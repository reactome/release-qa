package org.reactome.release.qa.graph;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.GraphQACheck;

/**
 * This check reports instances with exactly one value in a specified
 * attribute. For example, hasMember in DefinedSet or hasComponent in
 * Complex should have more than one value. Checking for no value of a
 * non-mandatory attribute can be done in the
 * {@link SingleAttributeMissingCheck}.
 *
 * The attributes are specified in the resources file corresponding to
 * this class.
 * 
 * @author wug
 */
@GraphQACheck
public class SingleAttributeSoleValueCheck extends SingleAttributeCardinalityCheck {
    
    public SingleAttributeSoleValueCheck() {
        super("= 1");
    }

    private static final String SKIP_LIST_FILE_PATH = "resources/attribute_has_only_one_value_skip_list.txt";
    /**
     * Escapes <code>Pathway.hasEvent</code> check non-disease instances.
     */
    @Override
    protected boolean isEscaped(GKInstance inst, String attName) throws Exception {
        if (super.isEscaped(inst, attName)) {
            return true;
        }
        // Check if instance DbId is in SKIP_LIST_FILE_PATH.
        if (super.inSkipList(inst, SKIP_LIST_FILE_PATH)) {
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
