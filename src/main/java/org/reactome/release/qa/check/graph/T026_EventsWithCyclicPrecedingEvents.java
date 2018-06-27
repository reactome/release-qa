package org.reactome.release.qa.check.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.ClassBasedQACheck;

public class T026_EventsWithCyclicPrecedingEvents extends ClassBasedQACheck {

    private static String DESCRIPTION =
            "Events with a preceding event cyclical dependency";

    private static QueryAttribute PRECEDING =
            new QueryAttribute("Event_2_precedingEvent", "ep", "precedingEvent");
    
    private static String SUBQUERY =
            "EXISTS (" + 
            "  SELECT 1 FROM Pathway_2_hasEvent pe" + 
            "  WHERE pe.DB_ID = ep.precedingEvent" + 
            "  AND pe.hasEvent = ep.DB_ID" + 
            ")" + 
            "OR EXISTS (" + 
            "  SELECT 1 FROM Event_2_inferredFrom pi" + 
            "  WHERE pi.DB_ID = ep.precedingEvent" + 
            "  AND pi.inferredFrom = ep.DB_ID" + 
            ")" + 
            "OR EXISTS (" + 
            "  SELECT 1 FROM Event_2_orthologousEvent po" + 
            "  WHERE po.DB_ID = ep.precedingEvent" + 
            "  AND po.orthologousEvent = ep.DB_ID" + 
            ")";

    public T026_EventsWithCyclicPrecedingEvents() {
        super(ReactomeJavaConstants.Event);
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected String getIssue(QueryResult result) {
        Long precedingDbID = (Long) result.values[0];
        Instance preceding = fetch(ReactomeJavaConstants.Event, precedingDbID);
        
        return "Used by preceding event " + format(preceding);
    }

    @Override
    public void testCheck() {
        // There are four invalid events in the test fixture.
        compareInvalidCountToExpected(4);
    }

    @Override
    protected Collection<QueryResult> fetchInvalid() {
        // TODO - this takes too long. Change the query.
        //return fetch(SUBQUERY, PRECEDING);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    /**
     * Creates five upsteam and five downstream instances, of which
     * four of the downstream instances should be reported as invalid.
     */
    protected List<Instance> createTestFixture() {
        List<Instance> fixture = new ArrayList<Instance>();
        
        // A valid downstream event.
        Instance upstream = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(upstream);
        Instance downstream = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(downstream);
        setAttributeValue(downstream, ReactomeJavaConstants.precedingEvent, upstream);
        
        // An invalid downstream event with an upstream hasEvent dependency.
        upstream = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(upstream);
        downstream = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(downstream);
        setAttributeValue(downstream, ReactomeJavaConstants.precedingEvent, upstream);
        setAttributeValue(upstream, ReactomeJavaConstants.hasEvent, downstream);
        
        // An invalid downstream event with an upstream inferredFrom dependency.
        upstream = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(upstream);
        downstream = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(downstream);
        setAttributeValue(downstream, ReactomeJavaConstants.precedingEvent, upstream);
        setAttributeValue(upstream, ReactomeJavaConstants.inferredFrom, downstream);
        
        // An invalid downstream event with an upstream inferredFrom dependency.
        upstream = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(upstream);
        downstream = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(downstream);
        setAttributeValue(downstream, ReactomeJavaConstants.precedingEvent, upstream);
        setAttributeValue(upstream, ReactomeJavaConstants.orthologousEvent, downstream);
        
        // An invalid downstream event with all three upstream dependencies.
        // This ensures that the invalid instance is not reported redundantly.
        upstream = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(upstream);
        downstream = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(downstream);
        setAttributeValue(downstream, ReactomeJavaConstants.precedingEvent, upstream);
        setAttributeValue(upstream, ReactomeJavaConstants.hasEvent, downstream);
        setAttributeValue(upstream, ReactomeJavaConstants.inferredFrom, downstream);
        setAttributeValue(upstream, ReactomeJavaConstants.orthologousEvent, downstream);
        
        return fixture;
    }

}
