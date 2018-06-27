package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T016_CatalystActivityWithoutActivity extends MissingValueCheck {

    public T016_CatalystActivityWithoutActivity() {
        super(ReactomeJavaConstants.CatalystActivity, ReactomeJavaConstants.activity);
    }

}
