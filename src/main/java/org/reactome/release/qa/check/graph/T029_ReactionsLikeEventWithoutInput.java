package org.reactome.release.qa.check.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.JavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T029_ReactionsLikeEventWithoutInput extends MissingValueCheck {

    public T029_ReactionsLikeEventWithoutInput() {
        super(JavaConstants.ReactionLikeEvent,
                ReactomeJavaConstants.input);
    }

    @Override
    protected Collection<Instance> fetchMissing() {
        Collection<Instance> missing = super.fetchMissing();
        // Filter out inferred instances.
        return missing.stream()
                .filter(instance -> isNotInferred(instance))
                .collect(Collectors.toList());
    }

    @Override
    protected List<Instance> createTestFixture() {
        List<Instance> fixture = super.createTestFixture();
        
        return fixture;
    }

}
