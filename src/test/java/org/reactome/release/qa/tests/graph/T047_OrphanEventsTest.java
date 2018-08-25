package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.graph.T047_OrphanEvents;

public class T047_OrphanEventsTest extends QACheckReportComparisonTester {

    public T047_OrphanEventsTest() {
        super(new T047_OrphanEvents(), 5);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();
        SchemaClass reactionCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Reaction);
        
        // An invalid empty event.
        GKInstance event = new GKInstance(reactionCls);
        event.setDbAdaptor(dba);
        fixture.add(event);
        
        // A valid event referenced by an invalid pathway.
        event = new GKInstance(reactionCls);
        event.setDbAdaptor(dba);
        fixture.add(event);
        SchemaClass pathwayCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Pathway);
        GKInstance pathway = new GKInstance(pathwayCls);
        pathway.setDbAdaptor(dba);
        pathway.setAttributeValue(ReactomeJavaConstants.hasEvent, event);
        fixture.add(pathway);
        
        // A valid event referenced by an invalid BBE.
        event = new GKInstance(reactionCls);
        event.setDbAdaptor(dba);
        fixture.add(event);
        SchemaClass bbeCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.BlackBoxEvent);
        GKInstance bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        bbe.setAttributeValue(ReactomeJavaConstants.hasEvent, event);
        fixture.add(bbe);
        
        // A valid event referenced by both an invalid pathway and an invalid BBE.
        event = new GKInstance(reactionCls);
        event.setDbAdaptor(dba);
        fixture.add(event);
        pathway = new GKInstance(pathwayCls);
        pathway.setDbAdaptor(dba);
        pathway.setAttributeValue(ReactomeJavaConstants.hasEvent, event);
        fixture.add(pathway);
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        bbe.setAttributeValue(ReactomeJavaConstants.hasEvent, event);
        fixture.add(bbe);
        
        return fixture;
      }

}
