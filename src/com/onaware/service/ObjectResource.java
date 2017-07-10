
package com.onaware.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.onaware.service.ExportService;
import com.onaware.service.ObjectService;
import com.onaware.util.CompareUtil;

import sailpoint.rest.plugin.AllowAll;
import sailpoint.rest.plugin.BasePluginResource;
import sailpoint.tools.GeneralException;

/**
 * The REST resource for CRUD operations on objects.
 *
 *
 */
@Path("envcom")
@Produces("application/json")
@Consumes("application/json")
public class ObjectResource extends BasePluginResource {
	
	private static final Logger log = Logger.getLogger("com.onaware");  


	 /**
     * {@inheritDoc}
     */
    @Override
    public String getPluginName() {
        return CompareUtil.PLUGIN_NAME;
    }
   
    
    /**
     * Gets the selected objects from the control & test iiq instances.
     * @return The HashMap containing the objects.
     * @throws GeneralException
     */
    @GET
    @Path("objects")
    @AllowAll
    public HashMap<String,HashMap> getObjects(@QueryParam("selection") final List<String> selection) throws GeneralException {
    	log.debug("Object Resource: getting objects");
    	log.debug("selection: " + selection);
    	ObjectService objService = getObjectService();
    	HashMap<String,HashMap> objects = objService.retrieveObjects(selection);
        log.debug("Objects: " + objects);
        return objects;
        
    }
    
    /**
     * Export object Tables to File location.
     *
     * @param data The object data.
     * @return The object data.
     * @throws GeneralException
     * @throws TransformerException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     */
    @POST
    @Path("export")
    @AllowAll
    public HashMap<String, String> exportTable(Map<String, ArrayList<Map>> data) throws GeneralException, SAXException, ParserConfigurationException, IOException, TransformerException {
        log.debug("ObjectResource data: " + data);
        ExportService expService = getExportService();
        return expService.exportObjects(data);
    }

    /**
     * Gets an instance of the ObjectService.
     *
     * @return The service.
     */
    private ObjectService getObjectService() {
        return new ObjectService(this);
    }
    
    /**
     * Gets an instance of the ObjectService.
     *
     * @return The service.
     */
    private ExportService getExportService() {
        return new ExportService(this);
    }

}

