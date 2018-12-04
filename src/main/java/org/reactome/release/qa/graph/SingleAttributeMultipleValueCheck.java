package org.reactome.release.qa.graph;

import org.reactome.release.qa.annotations.GraphQACheck;

/**
 * This check is to make sure there is exactly one value provided in a
 * specific attribute. For example, a Complex should be assigned to one
 * and only one Compartment.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@GraphQACheck
public class SingleAttributeNotSoleValueCheck extends SingleAttributeCardinalityCheck {
    
    public SingleAttributeNotSoleValueCheck() {
        super(" <> 1");
    }

    @Override
    public String getDisplayName() {
        return "Attribute_Does_Not_Have_Exactly_One_Value";
    }

}
