package org.reactome.release.qa.graph;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.GraphQACheck;

/**
 * This check is to make sure more than one value should be provided in a specific attribute.
 * For example, hasMember in DefinedSet or hasComponent in Complex should have more than one value.
 * @author wug
 *
 */
@GraphQACheck
public class SingleAttributeSoleValueCheck extends SingleAttributeCardinalityCheck {
    
    public SingleAttributeSoleValueCheck() {
        super(" = 1");
    }
    
    @Override
    protected boolean isEscaped(GKInstance inst, String attName) throws Exception {
        if (super.isEscaped(inst, attName)) {
            return true;
        }
        if (inst.getSchemClass().isa(ReactomeJavaConstants.Pathway) && 
                attName.equals(ReactomeJavaConstants.hasEvent)) {
            GKInstance disease = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.disease);
            if (disease != null)
                return true;
        }
        return false;
    }

    @Override
    public String getDisplayName() {
        return "Attribute_Has_Only_One_Value";
    }

}
