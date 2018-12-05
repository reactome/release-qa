package org.reactome.release.qa.graph;

import org.reactome.release.qa.annotations.GraphQACheck;

/**
 * This class checks for multi-valued attribute duplications.
 * 
 * Note: a skip list is supported but not recommended for this check.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@GraphQACheck
public class SingleAttributeDuplicationCheck extends SingleAttributeCardinalityCheck {
    
    public SingleAttributeDuplicationCheck() {
        super("> 1", true);
    }

    @Override
    public String getDisplayName() {
        return "Attribute_Value_Duplication";
    }

}
