package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T006_EwasWithoutReferenceEntity extends MissingValueCheck {
    public T006_EwasWithoutReferenceEntity() {
        super(ReactomeJavaConstants.EntityWithAccessionedSequence,
                ReactomeJavaConstants.referenceEntity);
    }
}
