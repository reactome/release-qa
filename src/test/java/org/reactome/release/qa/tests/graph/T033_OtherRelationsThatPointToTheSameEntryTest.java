package org.reactome.release.qa.tests.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.JavaConstants;
import org.reactome.release.qa.check.graph.T033_OtherRelationsThatPointToTheSameEntry;

public class T033_OtherRelationsThatPointToTheSameEntryTest extends QACheckReportComparisonTester {

    public T033_OtherRelationsThatPointToTheSameEntryTest() {
        super(new T033_OtherRelationsThatPointToTheSameEntry(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        List<Instance> fixture = new ArrayList<Instance>();
        Schema schema = dba.getSchema();
        
        // A valid empty event.
        SchemaClass bbeCls = schema.getClassByName(ReactomeJavaConstants.BlackBoxEvent);
        GKInstance bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        
        // A valid event with references to different events.
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        GKInstance other = new GKInstance(bbeCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        bbe.addAttributeValue(JavaConstants.templateEvent, other);
        GKInstance another = new GKInstance(bbeCls);
        another.setDbAdaptor(dba);
        fixture.add(another);
        bbe.addAttributeValue(ReactomeJavaConstants.orthologousEvent, another);
        
        // An invalid event with references to the same event.
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        other = new GKInstance(bbeCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        bbe.addAttributeValue(JavaConstants.templateEvent, other);
        bbe.addAttributeValue(ReactomeJavaConstants.orthologousEvent, other);
        
        return fixture;
    }

}
