package com.onaware.util;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmlunit.util.Predicate;

public class IIQNodeFilter implements Predicate<Node> {

	/*
	 * NodeFilter to remove any specific "key" attributes in IIQ matching acctAggregationStart or 
	 * acctAggregationEnd after an iiq aggregation for Application objects.
	 */
	@Override
	public boolean test(Node node) {
	if(node instanceof Element) {
		if(((Element) node).hasAttribute("key") && 
				(((Element) node).getAttribute("key").equals("acctAggregationEnd")|| 
						((Element) node).getAttribute("key").equals("acctAggregationStart")))  {
			return false;
		}
		
	}
	return true;
	}

}
