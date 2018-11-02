package org.reactome.release.qa.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.annotations.GraphQACheck;

/**
 * An evidence used in the inferredFrom slot should not be used in other attributes semantically.
 * This check tries to catch such a use.
 * @author wug
 *
 */
@GraphQACheck
public class InferredFromInOtherAttributeCheck extends TwoAttributesReferToSameCheck {

    public InferredFromInOtherAttributeCheck() {
    }

    @Override
    public String getDisplayName() {
        return "InferredFrom_Used_In_Other_Attribute";
    }

    @Override
    protected List<CheckConfiguration> loadConfiguration() throws Exception {
        List<CheckConfiguration> configurations = new ArrayList<>();
        // InferredFrom has been used in two classes: PhysicalEntity and Event
        Schema schema = dba.fetchSchema();
        createConfigurations(configurations, ReactomeJavaConstants.PhysicalEntity, schema);
        createConfigurations(configurations, ReactomeJavaConstants.Event, schema);
        return configurations;
    }

    @SuppressWarnings("unchecked")
    private void createConfigurations(List<CheckConfiguration> configurations,
                                      String clsName,
                                      Schema schema) {
        SchemaClass parentCls = schema.getClassByName(clsName);
        // Check inferred references.
        Collection<SchemaClass> classes = schema.getClasses();
        for (SchemaClass cls: classes) {
            if (!cls.isa(parentCls))
                continue;
            Collection<SchemaAttribute> attributes = cls.getAttributes();
            for (SchemaAttribute att: attributes) {
                if (!att.isInstanceTypeAttribute()) // Should check instance type attribute 
                    continue;
                if (att.getName().equals(ReactomeJavaConstants.inferredFrom)) // Don't check itself
                    continue;
                if (att.getName().equals(ReactomeJavaConstants.orthologousEvent))
                    continue; // They are often filled together.
                if (att.getOrigin() != cls) // Make sure check only once
                    continue; 
                for (SchemaClass allowedCls : (Collection<SchemaClass>)att.getAllowedClasses()) {
                    if (!allowedCls.isa(parentCls))
                        continue;
                    // Only check when allowed class is a parent class since the inferredFrom
                    // and the current should be the same type of instances.
                    CheckConfiguration configuration = new CheckConfiguration();
                    configuration.clsName = cls.getName();
                    configuration.attName1 = ReactomeJavaConstants.inferredFrom;
                    configuration.attName2 = att.getName();
                    configurations.add(configuration);
                    break;
                }
            }
        }
    }
    
}
