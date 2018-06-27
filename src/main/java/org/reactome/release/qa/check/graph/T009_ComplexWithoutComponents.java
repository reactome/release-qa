package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T009_ComplexWithoutComponents extends MissingValueCheck {

    public T009_ComplexWithoutComponents() {
        super(ReactomeJavaConstants.Complex,
                ReactomeJavaConstants.hasComponent);
    }

}
