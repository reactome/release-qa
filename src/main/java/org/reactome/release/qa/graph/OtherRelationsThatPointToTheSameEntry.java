package org.reactome.release.qa.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.schema.GKSchema;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.annotations.GraphQATest;

@GraphQATest
public class OtherRelationsThatPointToTheSameEntry extends TwoAttributesReferToSameCheck {
    
    //TODO: Need to check with Fred how the following list was created.
    private final static String[] SKIP_ATTS = {
            "author",
            "created",
            "edited",
            "modified",
            "revised",
            "reviewed",
            "input",
            "output",
            "entityOnOtherCell",
            "hasComponent",
            "requiredInputComponent",
            "physicalEntity",
            "diseaseEntity",
            "activeUnit",
            "reverseReaction",
            "precedingEvent",
            "hasEvent",
            "goCellularComponent",
            "compartment",
            "referenceSequence",
            "secondReferenceSequence",
            "hasCandidate",
            "hasMember"
    };
    
    @Override
    public String getDisplayName() {
        return "Other_Relations_Refer_Same_Instance";
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<CheckConfiguration> loadConfiguration() throws Exception {
        // Collect all original attributes
        Schema schema = dba.getSchema();
        Set<SchemaAttribute> attributes = ((GKSchema)schema).getOriginalAttributes();
        Set<String> skipAttNames = Stream.of(SKIP_ATTS).collect(Collectors.toSet());
        List<SchemaAttribute> attributeList = attributes.stream()
                                                        .filter(att -> !skipAttNames.contains(att.getName()))
                                                        .collect(Collectors.toList());
        List<CheckConfiguration> configurations = new ArrayList<>();
        for (int i = 0; i < attributeList.size() - 1; i++) {
            SchemaAttribute att1 = attributeList.get(i);
            SchemaClass cls1 = att1.getOrigin();
            for (int j = i + 1; j < attributeList.size(); j++) {
                SchemaAttribute att2 = attributeList.get(j);
                SchemaClass cls2 = att2.getOrigin();
                if (!cls1.isa(cls2) && !cls2.isa(cls1))
                    continue; // att1 and att2 cannot be used in the same class
                // Make sure the same types of instances can be used in these two attributes
                if (!isCompatibleAttributes(att1, att2))
                    continue;
                // Find which class should be used
                CheckConfiguration config = new CheckConfiguration();
                if (cls1.isa(cls2))
                    config.clsName = cls1.getName(); // Should use the lower class since 
                                                     // an attribute in the lower class may not
                                                     // be valid in an supper class
                else
                    config.clsName = cls2.getName();
                config.attName1 = att1.getName();
                config.attName2 = att2.getName();
                configurations.add(config);
            }
        }
        return configurations;
    }
    
    /**
     * Check if two attributes can use the same type of instances so that it is possible
     * they may refer to the same instance or creating circular reference.
     * @param att1
     * @param att2
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean isCompatibleAttributes(SchemaAttribute att1, SchemaAttribute att2) {
        Collection<SchemaClass> clses1 = att1.getAllowedClasses();
        Collection<SchemaClass> clses2 = att2.getAllowedClasses();
        for (SchemaClass cls1 : clses1) {
            for (SchemaClass cls2 : clses2) {
                if (cls1.isa(cls2) || cls2.isa(cls1))
                    return true;
            }
        }
        return false;
    }

}
