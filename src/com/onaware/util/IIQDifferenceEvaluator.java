package com.onaware.util;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DifferenceEvaluator;


/**
 * Evaluator to determine if in certain conditions that an outcome can become Different,Similar or Equal. 
 * @author johnk
 *
 */
public class IIQDifferenceEvaluator implements DifferenceEvaluator {

	private static final Logger log = Logger.getLogger("com.onaware.plugin");


	public IIQDifferenceEvaluator() {
		
	}
	
	@Override
	public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
		
		
		if (outcome == ComparisonResult.EQUAL)
			return outcome; // only evaluate differences.
		
		final Node controlNode = comparison.getControlDetails().getTarget();
		final Node testNode = comparison.getTestDetails().getTarget();
		
		/*
		 * if two elements are boolean tags & while one boolean's TextContent is null & other is "false".
		 * Both must be considered EQUAL.
		 */
		if (controlNode instanceof Element && testNode instanceof Element) {

			
			String testNodeName = testNode.getNodeName(); 
			String controlNodeName = controlNode.getNodeName();

			if (testNodeName.equalsIgnoreCase("Boolean") && controlNodeName.equalsIgnoreCase("Boolean")) {
				log.debug("........ELEMENT COMPARISON TEST........");
				
				log.debug("testNodeName: " + testNodeName);
				log.debug("controlNodeName: " + controlNodeName);
				
				String testValue = testNode.getTextContent();
				String controlValue = controlNode.getTextContent();

				log.debug("testValue: " + testValue.toString() + "\nControlValue: " + controlValue);
				if (testValue.equalsIgnoreCase("true") && controlValue.equalsIgnoreCase("true")) {
					return ComparisonResult.EQUAL;// pass test if testValue="True" while controlValue="true"
					
				}else {
					if (testValue.equalsIgnoreCase("false") || controlValue.equalsIgnoreCase("false")) {
						log.debug("Both Elements are: EQUAL");

						return ComparisonResult.EQUAL;

					}
				}
			}
		}
		
		/*
		 * This if statement will be executed as long as one boolean text value is null while the other has #text.
		 * Text within Node tags are considered separate nodes. Therefore these nodes are instanceof Text
		 */
		if ((testNode instanceof Text || controlNode instanceof Text) && (testNode == null || controlNode == null)) {
			log.debug("........TEXT COMPARISON TEST........");
			boolean isBooleanNode = false;//checking if the node is a boolean tag
			
			/*
			 * Either testNode or controlNode is null, 
			 * whichever is not null is checked for the boolean tag.
			 */
			if (testNode != null) {
				
				Element testElement = (Element) testNode.getParentNode();
				if (testElement.getNodeName().equalsIgnoreCase("boolean")) {
					isBooleanNode = true;
					log.debug("testNodeName is: " + testElement.getNodeName());
				}
			
			}else{
				log.debug("testNode is: NULL");
			}

			if (controlNode != null) {
				
				log.debug("controlNode: " + controlNode);
				Element controlElement = (Element) controlNode.getParentNode();
				if (controlElement.getNodeName().equalsIgnoreCase("boolean")) {
					isBooleanNode = true;
					log.debug("controlElement is: " + controlElement.getNodeName());
				}
				
			}else{
				log.debug("controlNode is: NULL");
			}

			/*
			 * If isBooleanNode = true, check the values of testNode & controlNode.
			 */
			if (isBooleanNode) {
				log.debug("Boolean tag present");
				boolean testNodeValue = false;
				boolean controlNodeValue = false;
				
				
				/*
				 * Either testNode or controlNode is null, 
				 * whichever is not null is checked for the boolean value.
				 */
				if (testNode == null) {
					testNodeValue = false; //stating null value within a boolean tag = false
				}else{
					if(testNode.getTextContent().equalsIgnoreCase("false")) {
						testNodeValue = false; //stating string "false" within a boolean tag = false
					}else{
						testNodeValue = true;
					}
				}

				
				
				if (controlNode == null) {
						controlNodeValue = false; //stating null value within a boolean tag = false
				}else{
					if (controlNode.getTextContent().equalsIgnoreCase("false")) {
						controlNodeValue = false; //stating null value within a boolean tag = false
					}else{
						controlNodeValue = true;
					}
				}
				
				
				
				if(testNodeValue == controlNodeValue) {
					log.debug("both Values equal");
					return ComparisonResult.EQUAL;
				}
				
			}

		}else {
			//Text in controlNode = "True" while Text in testNode = "true"
		if((testNode instanceof Text || controlNode instanceof Text) && (testNode.getTextContent().equalsIgnoreCase("true")  
				&& controlNode.getTextContent().equalsIgnoreCase("true"))) {
			return ComparisonResult.EQUAL;
		}
		
		}
		return outcome;

	}

}