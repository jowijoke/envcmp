package com.onaware.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import sailpoint.plugin.PluginContext;
import sailpoint.server.Exporter.Cleaner;
import sailpoint.tools.GeneralException;


/**
 * @author john Kennedy
 *
 */
public class ExportService {
	private static final String iiqIP = "controlUrl";
	private static final String testiiqIP = "testUrl";

	private static String iiqUser = "controlUsername";
	private static String iiqPass = "controlPassword";
	
	private static String exportLoc = "exportLocation";


	private static final Logger log = Logger.getLogger("com.onaware.plugin");
	
	static Collection<HashMap<String,String>> controlDiffObjects = new ArrayList<HashMap<String,String>>(); 
	static Collection<HashMap<String,String>> testDiffObjects = new ArrayList<HashMap<String,String>>();
	
	static Collection<HashMap<String,String>> extraObjects = new ArrayList<HashMap<String,String>>();
	static Collection<HashMap<String,String>> missingObjects = new ArrayList<HashMap<String,String>>(); 

	
	/**
	 * The plugin context.
	 */
	private PluginContext pluginContext;
	
	/**
	 * Constructor.
	 *
	 * @param pluginContext
	 * The plugin context.
	 */
	public ExportService(PluginContext pluginContext) {
		log.debug("plugin");
		this.pluginContext = pluginContext;
	}
	
	
	public HashMap<String, String> exportObjects(Map<String, ArrayList<Map>> data) throws GeneralException, SAXException, ParserConfigurationException, IOException, TransformerException {
		
		controlDiffObjects.clear();
    	testDiffObjects.clear();
    	extraObjects.clear();
    	missingObjects.clear();	
    	
    	for (String key : data.keySet()) {
			ArrayList<Map> maps = data.get(key);
			for (Map map : maps) {
				String type = map.get("type").toString();
				String name = map.get("name").toString();
				if(type.equalsIgnoreCase("Report") || type.equalsIgnoreCase("Life Cycle Event") || type.equalsIgnoreCase("Role"))
				{	log.debug("getting IIQ name for: " + type);
					String restType = getIIQType(type);
					populateHashMap(key, name, restType);
				}else {
					populateHashMap(key, name, type);
				}
			}
    	}
    	cleanXml(controlDiffObjects,testDiffObjects,extraObjects,missingObjects);
    	return downloadXml(controlDiffObjects,testDiffObjects,extraObjects,missingObjects);

	}
	
	/**
	 * Sets up each HashMap and populates the right XMl objects from each table to each HashMap Collection
	 * e.g XML objects from table extra are placed into extraObjects HashMap Collection.
	 * @param table: Table names difference, extra or missing. 
	 * @param name: The object name.
	 * @param type: Object type.
	 * @throws GeneralException
	 */
	private void populateHashMap(String table, String name, String type) throws GeneralException {
		
		if(table.equalsIgnoreCase("difference")) {
			if(checkObject(name) == false) 
			{
				log.debug(name + " not found in HashMap");
				String xmlRequest = getIIQDefaultURL();
				controlDiffObjects.addAll(getXml(xmlRequest, name, type));
				String xmlTestRequest = getIIQDefaultTestURL();
				testDiffObjects.addAll(getXml(xmlTestRequest, name, type));
			}
			log.debug(name +" found in HashMap");
			
		}
		
		else if(table.equalsIgnoreCase("extra")) {
			String xmlTestRequest = getIIQDefaultTestURL();
			extraObjects.addAll(getXml(xmlTestRequest, name, type));
		}
    	
		else if(table.equalsIgnoreCase("missing")) {
			String xmlRequest = getIIQDefaultURL();
			missingObjects.addAll(getXml(xmlRequest, name, type));
    	}
	}
	
	
	/**
	 * Executes Rest call to IIQ instance to retireve xml objects are placed into a HashMap 
	 * @param xmlRequest: The IIQ Url
	 * @param name: object name
	 * @param type: object type
	 * @return objects Arraylist
	 * @throws GeneralException
	 */
	public ArrayList<HashMap<String,String>> getXml(String xmlRequest, String name, String type) throws GeneralException {
		ArrayList<HashMap<String,String>> objects = new ArrayList<HashMap<String,String>>();
		try {
			HashMap<String, String> objMap = new HashMap();
			String i = name.replaceAll(" ", "%20");
			String xml = xmlRequest + "/rest/debug/" + type + "/" + i;
			HttpGet request = new HttpGet(xml);
			HttpResponse response = getConnection().execute(request);
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			String line = "";
			while ((line = rd.readLine()) != null) {
				JSONObject jsonObject2 = new JSONObject(line);
				JSONArray xmlArray = (JSONArray) jsonObject2.get("objects");
				for (int x = 0; x < xmlArray.length(); x++) {
					objMap.put("name", name);
					objMap.put("xml", xmlArray.getJSONObject(x).getString("xml"));
					if (type.equals("IdentityTrigger")) {
						objMap.put("type", "LifeCycleEvent");
						
					}else if (type.equals("Bundle")) {
						objMap.put("type", "Role");
						
					}else if (type.equals("TaskDefinition")) {
						
						objMap.put("type", "Report");
						
					}else{
						objMap.put("type", type);
					}
					objects.add(objMap);
					log.debug("Have XML for: " + objMap.get("name"));
					
				}
			}	

			return objects;
			
		}catch (Exception e) {
			log.debug("Exception:" + e.toString());
		}
		return null;
	}

	/**
	 * Iterates through each xml from the HashMap collections to remove all id,created and modified attributes. 
	 * @param controlDiffObjects
	 * @param testDiffObjects
	 * @param extraObjects
	 * @param missingObjects
	 */
	private void cleanXml(Collection<HashMap<String, String>> controlDiffObjects, Collection<HashMap<String, String>> testDiffObjects, Collection<HashMap<String, String>> extraObjects, Collection<HashMap<String, String>> missingObjects) {
		
		List attributesToRemove = new ArrayList();
		attributesToRemove.add("id");
		attributesToRemove.add("created");
		attributesToRemove.add("modified");
		Cleaner cleaner = new Cleaner(attributesToRemove);
		
		ArrayList<Collection<HashMap<String, String>>> objHashMaps = new ArrayList();
		
		if(!extraObjects.isEmpty()) {
			objHashMaps.add(extraObjects);
			
			
		}
		else if(!missingObjects.isEmpty()) {
			objHashMaps.add(missingObjects);
		}
		
		else if (!testDiffObjects.isEmpty() && !controlDiffObjects.isEmpty()) {
			objHashMaps.add(testDiffObjects);
			objHashMaps.add(controlDiffObjects);
		}
		
		log.debug("Added collections");
		for (Collection<HashMap<String, String>> map : objHashMaps) {
			for (HashMap<String, String> i : map) {
	    		String xml = i.get("xml");
	    		xml = cleaner.clean(xml);
	    		i.put("xml", xml);
	    	    
	    	}
		}
		
		log.debug("cleaned xml");
	
	}
	
	/**
	 * Create the file directories and export teh xmls to the right folder.
	 * 
	 * @param controlDiffObjects
	 * @param testDiffObjects
	 * @param extraObjects
	 * @param missingObjects
	 * @return result
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws TransformerException
	 */
	private HashMap<String, String> downloadXml(Collection<HashMap<String, String>> controlDiffObjects, Collection<HashMap<String, String>> testDiffObjects, Collection<HashMap<String, String>> extraObjects,	Collection<HashMap<String, String>> missingObjects) throws SAXException, ParserConfigurationException, IOException, TransformerException {
		HashMap<String, String> result = new HashMap();
		
		if(extraObjects.isEmpty() && missingObjects.isEmpty() && controlDiffObjects.isEmpty() && testDiffObjects.isEmpty()) {
			result.put("info","Tables are empty nothing to export");
			return result;
		}
		
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_hhmmss");
		String date = dateFormat.format(now);
		
		
		
		File fileLocation = new File(getExportLocation() + File.separator + "TableExport" + date);
		log.debug(fileLocation.getAbsolutePath());
		Path extra = Paths.get(  fileLocation + File.separator + "extra");
		Path missing = Paths.get( fileLocation + File.separator +"missing");
		Path diffControl = Paths.get( fileLocation + File.separator +"difference" + File.separator +"control");
		Path diffTest = Paths.get( fileLocation + File.separator +"difference" + File.separator +"test");
		
		try {
			if(!extraObjects.isEmpty()) {
				Files.createDirectories(extra);
				for (HashMap<String, String> x : extraObjects) {
					String name = x.get("name");	
					String xml = x.get("xml");
					String type = x.get("type");
					createFiles(xml, extra, type, name);
					
				}
				log.debug("extra xml created");
			}
			if(!missingObjects.isEmpty()) {
				Files.createDirectories(missing);
				for (HashMap<String, String> x : missingObjects) {
					String name = x.get("name");	
					String xml = x.get("xml");
					String type = x.get("type");
					createFiles(xml,missing, type, name);
					   
				}
				log.debug("missing xml created");
			}
			if(!controlDiffObjects.isEmpty() && !testDiffObjects.isEmpty()) {
				Files.createDirectories(diffControl);	
				for (HashMap<String, String> x : controlDiffObjects) {
					String name = x.get("name");	
					String xml = x.get("xml");
					String type = x.get("type");
					createFiles(xml,diffControl, type, name);
				     
				}
				log.debug("control Diff xml created");
			
					Files.createDirectories(diffTest);
					for (HashMap<String, String> x : testDiffObjects) {
						String name = x.get("name");	
						String xml = x.get("xml");
						String type = x.get("type");
						createFiles(xml,diffTest, type, name);
						
					}
				
					log.debug("test Diff xml created");
			}
			
		} catch (IOException e) {
			log.error("Cannot create directories - " + e);
		    result.put("error","Cannot create directories");
		    return result;
		}
	
		result.put("success", "XML objects successfully exported to " + fileLocation.getAbsolutePath());
		log.debug(result);
		
		return result;

		}

	/**
	 * Create new xml Documents 
	 * @param xml: String containg the xml content
	 * @param path: the location to save the xml
	 * @param type: Object type
	 * @param name: object Name
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws TransformerException
	 */
	private void createFiles(String xml, Path path, String type, String name) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    Document doc = builder.parse(new InputSource(new StringReader(xml)));

	    // Write the parsed document to an xml file
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer = transformerFactory.newTransformer();
	    DOMSource source = new DOMSource(doc);
	    
	    StreamResult result =  new StreamResult(new File(path + File.separator + type + "-" + toCamelCase(name) +  ".xml"));
	    transformer.transform(source, result);
	}

	/**
	 * Creates HttpClient object to be used to connect to  the iiq instance.
	 * @return client
	 */
	public HttpClient getConnection() {
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(getIIQDefaultUserName(),
				getIIQDefaultPassword());
		provider.setCredentials(AuthScope.ANY, credentials);
		HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

		return client;
	}

	

	/**
	 * Gets the IIQ Object Type for the REST Calls.
	 *
	 * @return The new IIQType.
	 */
	private String getIIQType(String type) {	
		if(type.equalsIgnoreCase("report")) {
			String iiqType = "TaskDefinition";
			log.debug("IIQType is: " + iiqType);
			return iiqType;
		}else if(type.equalsIgnoreCase("Life Cycle Event")) {
			String iiqType = "IdentityTrigger";
			log.debug("IIQType is: " + iiqType);
			return iiqType;
		}else if(type.equalsIgnoreCase("Role")) {
			String iiqType = "Bundle";
			log.debug("IIQType is: " + iiqType);
			return iiqType;
		}
		return type;
	}
	
	/**
	 * Convert Strings to CamelCase
	 * @param s
	 * @return result
	 */
	public static String toCamelCase(String s) {
	    final String ACTIONABLE_DELIMITERS = " _-";                                            
	    StringBuilder sb = new StringBuilder();
	    boolean capNext = true;
	    for (char c : s.toCharArray()) {
	        c = (capNext)
	                ? Character.toUpperCase(c)
	                : Character.toLowerCase(c);
	        sb.append(c);
	        capNext = (ACTIONABLE_DELIMITERS.indexOf(c) >= 0);
	    }
	    String result = sb.toString();
	    result = result.replaceAll(" ", "");
	    return result;
	}
	
	/**
	 * Check HashMap collections to see if the object already exists in the collection
	 * @param name: Object name
	 * @return boolean
	 */
	private Boolean checkObject(String name) {
		if(!controlDiffObjects.isEmpty() && !testDiffObjects.isEmpty()) {
			for (HashMap<String, String> i : controlDiffObjects) {
	    		//if key "name" matches String name
	    		if(i.get("name").equals(name)) {
	    			return true;
	    	    }
	    	}
			
			for (HashMap<String, String> i : testDiffObjects) {
	    		//if key "name" matches String name
	    		if(i.get("name").equals(name)) {
	    			return true;
	    	    }
	    	}
		}
		return false;
	}

	
	/**
	 * Gets the configured default UserName.
	 *
	 * @return The default name.
	 */
	private String getIIQDefaultUserName() {
		return pluginContext.getSettingString(iiqUser);
	}
	
	/**
	 * Gets the configured file location.
	 *
	 * @return The default location.
	 */
	private String getExportLocation() {
		 String path = pluginContext.getSettingString(exportLoc);
		 if(path == null || path.trim().equals("")) {
			 path = System.getProperty("user.dir");
		 }
		 return path;
	}

	/**
	 * Gets the configured default Password.
	 *
	 * @return The password.
	 */
	private String getIIQDefaultPassword() {
		return pluginContext.getSettingString(iiqPass);
	}

	/**
	 * Gets the configured default Control URL.
	 *
	 * @return The Control URL.
	 */
	private String getIIQDefaultURL() {
		return pluginContext.getSettingString(iiqIP);
	}

	/**
	 * Gets the configured default Test URL.
	 *
	 * @return The Test URL.
	 */
	private String getIIQDefaultTestURL() {
		return pluginContext.getSettingString(testiiqIP);
	}

}
