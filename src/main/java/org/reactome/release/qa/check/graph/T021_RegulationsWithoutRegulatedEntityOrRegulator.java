package org.reactome.release.qa.check.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T021_RegulationsWithoutRegulatedEntityOrRegulator extends MissingValueCheck {

    public T021_RegulationsWithoutRegulatedEntityOrRegulator() {
        super(ReactomeJavaConstants.Regulation,
              ReactomeJavaConstants.regulator);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " or regulatedBy referral";
    }

    @Override
    public void testCheck() {
        compareInvalidCountToExpected(3);
    }

    @Override
    protected Collection<Instance> fetchMissing() {
        // The default missing include Regulations without a regulator.
        Collection<Instance> missingRegulator = super.fetchMissing();
        
        // Get the Regulations without a regulatedBy referral.
        Collection<QueryResult> missingRegulatedBy = fetchUnreferenced(
                ReactomeJavaConstants.ReactionlikeEvent,
                ReactomeJavaConstants.regulatedBy);
        
        // Collect both missing collections into a set
        // to avoid duplication.
        Stream<Instance> regByStream =
                missingRegulatedBy.stream().map(result -> result.instance);
        return Stream.concat(missingRegulator.stream(), regByStream)
                .collect(Collectors.toSet());
    }

    @Override
    /**
     * Creates five regulations, two of which are valid.
     */
    protected List<Instance> createTestFixture() {
        // The default fixture has an empty Regulation.
        List<Instance> fixture = super.createTestFixture();
        
        // An invalid regulation without a regulator.
        Instance event = createInstance(ReactomeJavaConstants.BlackBoxEvent);
        fixture.add(event);
        Instance regulation = createInstance(ReactomeJavaConstants.PositiveRegulation);
        setAttributeValue(event, ReactomeJavaConstants.regulatedBy, regulation);
        fixture.add(regulation);
        
        // An invalid regulation without a regulatedBy referral.
        Instance regulator = createInstance(ReactomeJavaConstants.SimpleEntity);
        fixture.add(regulator);
        regulation = createInstance(ReactomeJavaConstants.PositiveRegulation);
        setAttributeValue(regulation, ReactomeJavaConstants.regulator, regulator);
        fixture.add(regulation);
        
        // A valid positive regulation.
        event = createInstance(ReactomeJavaConstants.BlackBoxEvent);
        fixture.add(event);
        regulator = createInstance(ReactomeJavaConstants.SimpleEntity);
        fixture.add(regulator);
        regulation = createInstance(ReactomeJavaConstants.PositiveRegulation);
        setAttributeValue(regulation, ReactomeJavaConstants.regulator, regulator);
        setAttributeValue(event, ReactomeJavaConstants.regulatedBy, regulation);
        fixture.add(regulation);
        
        // A valid negative regulation.
        event = createInstance(ReactomeJavaConstants.BlackBoxEvent);
        fixture.add(event);
        regulator = createInstance(ReactomeJavaConstants.SimpleEntity);
        fixture.add(regulator);
        regulation = createInstance(ReactomeJavaConstants.NegativeRegulation);
        setAttributeValue(regulation, ReactomeJavaConstants.regulator, regulator);
        setAttributeValue(event, ReactomeJavaConstants.regulatedBy, regulation);
        fixture.add(regulation);
        
        return fixture;
    }

}
