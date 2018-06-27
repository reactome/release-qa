package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T005_PathwaysWithoutEvents extends MissingValueCheck {

    public T005_PathwaysWithoutEvents() {
        super(ReactomeJavaConstants.Pathway,
                ReactomeJavaConstants.hasEvent);
    }

}
