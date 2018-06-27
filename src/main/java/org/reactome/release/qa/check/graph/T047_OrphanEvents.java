package org.reactome.release.qa.check.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.ClassBasedQACheck;

public class T047_OrphanEvents extends ClassBasedQACheck {

    private static final String DESCRIPTION = "Events that cannot be reached through the events hierarchy";
    private static final String ISSUE = "Event is orphaned";

    public T047_OrphanEvents() {
        super(ReactomeJavaConstants.Event);
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected String getIssue(QueryResult result) {
        return ISSUE;
    }

    @Override
    protected Collection<QueryResult> fetchInvalid() {
        // The events which are not referenced by a BBE.
        Collection<QueryResult> unreferencedByBBE = fetchUnreferenced(
                ReactomeJavaConstants.BlackBoxEvent,
                ReactomeJavaConstants.hasEvent);
        // The events to exclude based on the BBE criterion.
        Set<Instance> excludes = unreferencedByBBE.stream()
                .map(result -> result.instance)
                .collect(Collectors.toSet());
        //The top-level events.
        Set<GKInstance> tles = getTopLevelPathways();

        // The events which are not referenced by another pathway.
        Collection<QueryResult> unreferencedByPathway = fetchUnreferenced(
                ReactomeJavaConstants.Pathway,
                ReactomeJavaConstants.hasEvent);
        // The exclusion criterion.
        Predicate<QueryResult> isInvalid =
                result -> !(tles.contains(result.instance) ||
                        excludes.contains(result.instance));
        // Filter for the events unreferenced by either another
        // pathway or a BBE.
        return unreferencedByPathway.stream()
                .filter(isInvalid)
                .collect(Collectors.toList());
    }

    @Override
    public void testCheck() {
        // There is one invalid fixture instance.
        compareInvalidCountToExpected(1);
    }

    @Override
    protected List<Instance> createTestFixture() {
        List<Instance> fixture = new ArrayList<Instance>();

        // An invalid empty event.
        Instance event = createInstance(ReactomeJavaConstants.Reaction);
        fixture.add(event);
        
        // A valid event referenced by only a pathway.
        event = createInstance(ReactomeJavaConstants.Reaction);
        fixture.add(event);
        Instance pathway = createInstance(ReactomeJavaConstants.Pathway);
        setAttributeValue(pathway, ReactomeJavaConstants.hasEvent, event);
        fixture.add(pathway);
        
        // A valid event referenced by only a BBE.
        event = createInstance(ReactomeJavaConstants.Reaction);
        fixture.add(event);
        Instance bbe = createInstance(ReactomeJavaConstants.BlackBoxEvent);
        setAttributeValue(bbe, ReactomeJavaConstants.hasEvent, event);
        fixture.add(bbe);
        
        // A valid event referenced by both a pathway and a BBE.
        event = createInstance(ReactomeJavaConstants.Reaction);
        fixture.add(event);
        pathway = createInstance(ReactomeJavaConstants.Pathway);
        setAttributeValue(pathway, ReactomeJavaConstants.hasEvent, event);
        fixture.add(pathway);
        bbe = createInstance(ReactomeJavaConstants.BlackBoxEvent);
        setAttributeValue(bbe, ReactomeJavaConstants.hasEvent, event);
        fixture.add(bbe);
        
        return fixture;
    }

}
