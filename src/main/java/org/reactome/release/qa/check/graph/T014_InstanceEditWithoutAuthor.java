package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T014_InstanceEditWithoutAuthor extends MissingValueCheck {

    public T014_InstanceEditWithoutAuthor() {
        super(ReactomeJavaConstants.InstanceEdit, ReactomeJavaConstants.author);
    }

}
