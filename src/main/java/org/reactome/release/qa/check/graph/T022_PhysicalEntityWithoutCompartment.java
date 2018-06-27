package org.reactome.release.qa.check.graph;

import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T022_PhysicalEntityWithoutCompartment extends MissingValueCheck {

    public T022_PhysicalEntityWithoutCompartment() {
        super(ReactomeJavaConstants.PhysicalEntity, ReactomeJavaConstants.compartment);
    }

}
