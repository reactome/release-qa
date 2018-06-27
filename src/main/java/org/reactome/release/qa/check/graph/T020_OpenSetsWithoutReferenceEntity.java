package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T020_OpenSetsWithoutReferenceEntity extends MissingValueCheck {

    public T020_OpenSetsWithoutReferenceEntity() {
        super(ReactomeJavaConstants.OpenSet, ReactomeJavaConstants.referenceEntity);
    }

}
