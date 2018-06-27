package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T015_CatalystActivityWithoutPhysicalEntity extends MissingValueCheck {

    public T015_CatalystActivityWithoutPhysicalEntity() {
        super(ReactomeJavaConstants.CatalystActivity, ReactomeJavaConstants.physicalEntity);
    }

}
