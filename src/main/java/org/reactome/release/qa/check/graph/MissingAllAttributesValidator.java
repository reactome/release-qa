package org.reactome.release.qa.check.graph;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.check.QACheckerHelper;

public class MissingAllAttributesValidator implements Validator {

    private static final String MISSING_ATTRIBUTE =
            "%s Missing Attributes check is missing the attribute arguments";

    private String clsName;
    
    private String[] attNames;
    
    public MissingAllAttributesValidator(String clsName, String... attNames) {
        super();
        if (attNames.length == 0) {
            String message = String.format(MISSING_ATTRIBUTE, clsName);
            throw new UnsupportedOperationException(message);
        }
        this.clsName = clsName;
        this.attNames = attNames;
    }

    @Override
    public String getName() {
        String suffix = Stream.of(attNames)
                .map(MissingAllAttributesValidator::capitalize)
                .collect(Collectors.joining("And"));
        return clsName + "Missing" + suffix;
    }
    
    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? null :
            s.substring(0, 1).toUpperCase() + s.substring(1);
    }
    
    @Override
    public void validate(MySQLAdaptor dba, Consumer<Invalid> consumer)
            throws Exception {
        List<GKInstance> instances = QACheckerHelper.getInstancesWithNullAttribute(dba,
                clsName, attNames[0], null);
        String issue = getIssue();
        for (GKInstance instance: instances) {
            boolean isMissing = true;
            for (int i = 1; i < attNames.length; i++) {
                String attName = attNames[i];
                if (instance.getSchemClass().isValidAttribute(attName)) {
                    if (instance.getAttributeValue(attName) != null) {
                        isMissing = false;
                        break;
                    }
                }
            }
            if (isMissing) {
                Invalid invalid = new Validator.Invalid(instance, issue);
                consumer.accept(invalid);
            }
        }
    }

    private String getIssue() {
        return "Missing " + String.join(" and ", attNames);
    }

}
