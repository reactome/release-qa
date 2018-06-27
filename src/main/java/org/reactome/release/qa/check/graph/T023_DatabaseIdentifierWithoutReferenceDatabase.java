package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T023_DatabaseIdentifierWithoutReferenceDatabase extends MissingValueCheck {

    public T023_DatabaseIdentifierWithoutReferenceDatabase() {
        super(ReactomeJavaConstants.DatabaseIdentifier, ReactomeJavaConstants.referenceDatabase);
    }

}
