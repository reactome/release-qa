package org.reactome.release.qa.check.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.ClassBasedQACheck;

public class T026_EventsWithCyclicPrecedingEvents extends ClassBasedQACheck {

    private static String DESCRIPTION =
            "Events with a preceding event cyclical dependency";

    private static QueryAttribute HAS_EVENT_ATTRS[] = {
            new QueryAttribute("Event_2_precedingEvent", "ep", "precedingEvent"),
            new QueryAttribute("Pathway_2_hasEvent", "pe", "hasEvent")
    };
    
    private static String HAS_EVENT_CONDITION =
            "Event.DB_ID = ep.DB_ID" + 
            " AND ep.precedingEvent = pe.DB_ID" +
            " AND ep.DB_ID = pe.hasEvent";

    private static QueryAttribute INFERRED_ATTRS[] = {
            new QueryAttribute("Event_2_precedingEvent", "ep", "precedingEvent"),
            new QueryAttribute("Event_2_inferredFrom", "ei", "inferredFrom")
    };
    
    private static String INFERRED_CONDITION =
            "Event.DB_ID = ep.DB_ID" + 
            " AND ep.precedingEvent = ei.DB_ID" +
            " AND ep.DB_ID = ei.inferredFrom";

    private static QueryAttribute ORTHOLOGOUS_ATTRS[] = {
            new QueryAttribute("Event_2_precedingEvent", "ep", "precedingEvent"),
            new QueryAttribute("Event_2_orthologousEvent", "eo", "orthologousEvent")
    };
    
    private static String ORTHOLOGOUS_CONDITION =
            "Event.DB_ID = ep.DB_ID" + 
            " AND ep.precedingEvent = eo.DB_ID" +
            " AND ep.DB_ID = eo.orthologousEvent";

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
        Collection<QueryResult> hasEventInvalid =
                fetch(HAS_EVENT_CONDITION, HAS_EVENT_ATTRS);
        Collection<QueryResult> inferredInvalid =
                fetch(INFERRED_CONDITION, INFERRED_ATTRS);
        Collection<QueryResult> orthoInvalid =
                fetch(ORTHOLOGOUS_CONDITION, ORTHOLOGOUS_ATTRS);

        Map<Instance, QueryResult> map = new HashMap<Instance, QueryResult>();
        for (QueryResult qr: hasEventInvalid) {
            map.put(qr.instance, qr);
        }
        for (QueryResult qr: inferredInvalid) {
            map.put(qr.instance, qr);
        }
        for (QueryResult qr: orthoInvalid) {
            map.put(qr.instance, qr);
        }
        
        return map.values();
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
