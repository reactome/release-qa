package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T013_ReferenceEntityWithoutIdentifier4 extends MissingValueCheck {

    public T013_ReferenceEntityWithoutIdentifier4() {
        super(ReactomeJavaConstants.ReferenceEntity, ReactomeJavaConstants.identifier);
    }

}
