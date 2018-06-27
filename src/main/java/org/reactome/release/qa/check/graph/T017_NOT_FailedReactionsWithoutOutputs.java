package org.reactome.release.qa.check.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;
import org.junit.Test;

public class T017_NOT_FailedReactionsWithoutOutputs extends MissingValueCheck {

    public T017_NOT_FailedReactionsWithoutOutputs() {
        super(ReactomeJavaConstants.ReactionlikeEvent,
              ReactomeJavaConstants.output);
    }

    @Override
    public String getDescription() {
        return "Non-failed " + super.getDescription();
    }

    @Override
    @Test
    public void testCheck() {
        super.compareInvalidCountToExpected(1);
    }

    @Override
    protected Collection<Instance> fetchMissing() {
        return super.fetchMissing()
                    .stream()
                    .filter(rle -> !rle.getSchemClass().isa(ReactomeJavaConstants.FailedReaction))
                    .collect(Collectors.toList());
    }

    @Override
    protected List<Instance> createTestFixture() {
        // An invalid empty event.
        List<Instance> fixture = new ArrayList<Instance>();
        Instance bbe = createInstance(ReactomeJavaConstants.BlackBoxEvent);
        fixture.add(bbe);
        
        // An empty but valid (for our purposes) failed reaction.
        Instance fr = createInstance(ReactomeJavaConstants.FailedReaction);
        fixture.add(fr);
        
        return fixture;
    }

}
