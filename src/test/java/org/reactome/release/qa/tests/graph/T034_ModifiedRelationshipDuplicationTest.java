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
import org.reactome.release.qa.check.graph.T034_ModifiedRelationshipDuplication;

public class T034_ModifiedRelationshipDuplicationTest extends QACheckReportComparisonTester {

    public T034_ModifiedRelationshipDuplicationTest() {
        super(new T034_ModifiedRelationshipDuplication(), 1);
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
        
        // A valid modification.
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        SchemaClass ieCls = schema.getClassByName(ReactomeJavaConstants.InstanceEdit);
        GKInstance ie = new GKInstance(ieCls);
        ie.setDbAdaptor(dba);
        fixture.add(ie);
        bbe.addAttributeValue(ReactomeJavaConstants.modified, ie);
        
        // An invalid duplicate modification.
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        ie = new GKInstance(ieCls);
        ie.setDbAdaptor(dba);
        fixture.add(ie);
        bbe.addAttributeValue(ReactomeJavaConstants.modified, ie);
        GKInstance other = new GKInstance(ieCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        bbe.addAttributeValue(ReactomeJavaConstants.modified, other);
        bbe.addAttributeValue(ReactomeJavaConstants.modified, other);
        
        return fixture;
    }

}
