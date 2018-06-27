package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T012_SimpleEntityWithoutReferenceEntity extends MissingValueCheck {

    public T012_SimpleEntityWithoutReferenceEntity() {
        super(ReactomeJavaConstants.SimpleEntity,
              ReactomeJavaConstants.referenceEntity);
    }

}
