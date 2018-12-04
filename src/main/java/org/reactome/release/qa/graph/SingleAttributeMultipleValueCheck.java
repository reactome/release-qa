package org.reactome.release.qa.graph;

import org.reactome.release.qa.annotations.GraphQACheck;

/**
 * This check reports instances which have more than one value
 * in a specified attribute. For example, a Complex should not
 * be assigned to multiple Compartments. Checking for no value
 * of a non-mandatory attribute can be done in the
 * {@link SingleAttributeMissingCheck}.
 * 
 * The attributes are specified in the resources file corresponding
 * to this class.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@GraphQACheck
public class SingleAttributeMultipleValueCheck extends SingleAttributeCardinalityCheck {
    
    public SingleAttributeMultipleValueCheck() {
        super("> 1");
    }

    @Override
    public String getDisplayName() {
        return "Attribute_Does_Not_Have_Exactly_One_Value";
    }

}
