package com.onaware.util;

import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.Comparison.Detail;
import org.xmlunit.diff.ComparisonFormatter;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.DefaultComparisonFormatter;

import com.onaware.util.diff_match_patch.Diff;
import com.onaware.util.diff_match_patch.Operation;


/**
 * Formatter used for evaluating BeanShell code.
 * @author johnk
 *
 */
public class IIQComparisonFormatter implements ComparisonFormatter{

private static final Logger log = Logger.getLogger("com.onaware.plugin");

	
	@Override
	public String getDescription(Comparison diff) {
		log.debug("starting formatter");
		
		final Node controlNode = diff.getControlDetails().getTarget();
		final Node testNode = diff.getTestDetails().getTarget();

		if(controlNode != null && testNode != null ) {
			Element testElement = (Element) testNode.getParentNode();
			Element controlElement = (Element) controlNode.getParentNode();
			if(testElement !=null && controlElement != null) {
				//Only handle Source tags where beanshell is present.
				if ((testElement.getNodeName().equalsIgnoreCase("Source") && 
						controlElement.getNodeName().equalsIgnoreCase("Source"))) {
					log.debug("testNodeName is: " + testElement.getNodeName());
					log.debug("controlElement is: " + controlElement.getNodeName());
					String test = testNode.getTextContent();
					String control = controlNode.getTextContent();
				
					if(!control.equals(test)) {
						try {
							Assert.assertEquals(control,test);
						}catch (AssertionError e) {
					
					
							log.debug("starting diff test ");
							diff_match_patch difference = new diff_match_patch();
							LinkedList<Diff> deltas = difference.diff_main(control, test);
							//	log.debug("deltas: " + deltas);
							//	Reconstruct texts from the deltas
							//  text1 = all deletion (-1) and equality (0).
							//  text2 = all insertion (1) and equality (0).
							int Del = 0;
							int Ins = 0;
							 String text1 = "";
							 String text2 = "";
							for(Diff d: deltas)
							{
								if(d.operation==Operation.DELETE)
								{  
									text1 += d.text;
									Del ++;
									
								}else if(d.operation==Operation.INSERT)
								{ 
									text2 += d.text;
									Ins ++;
								}else{
									text1 += d.text;
									text2 += d.text;
								}
						      
							}
					
						log.debug("Del: " + Del);
				    	log.debug("Insert: " + Ins);
				    	
				    	difference.diff_cleanupSemantic(deltas);
				    	String result = difference.diff_prettyHtml(deltas);
				    	
						String count = "Beanshell source different - " +"\n" + Del + " Deletions "+"\n"+ Ins + " Insertions";
						return count;
					}

				
				}
			}
		}

	}
		log.debug("Result: " + diff.toString());
		return diff.toString();
	}

	
	@Override
	public String getDetails(Detail arg0, ComparisonType arg1, boolean arg2) {
		DefaultComparisonFormatter defaultFormatter = new DefaultComparisonFormatter();
		return  defaultFormatter.getDetails(arg0, arg1, arg2);
	}

}
