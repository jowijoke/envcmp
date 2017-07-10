package com.onaware.util;

import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.DifferenceEvaluator;

/**
 * ignore Child_NodeList_Sequence if node order is different.
 * @author johnk
 *
 */
public class IIQChildListEvaluator implements DifferenceEvaluator {

	@Override
	public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
		if (outcome == ComparisonResult.DIFFERENT && 
                comparison.getType() == ComparisonType.CHILD_NODELIST_SEQUENCE) {
			
			return ComparisonResult.EQUAL;
            }

            return outcome;
	}

}
