package org.reactome.release.qa.check.graph;

import org.reactome.release.qa.common.QACheck;

public class QACheckFactory {

    private static final String MISSING_ALL_ATTS = "MissingAllAttributes";

    private static final String CHECK_UNSUPPORTED = "%s %s check is not supported";

    public static QACheck create(String check, String className, String... arguments) {
        Validator validator = null;
        if (MISSING_ALL_ATTS.equals(check)) {
            validator = new MissingAllAttributesValidator(className, arguments);
        }
        
        if (validator == null) {
            String message = String.format(CHECK_UNSUPPORTED, className, check);
            throw new UnsupportedOperationException(message);
        }
        
        return new ValidatorQACheck(validator);
    }

}
