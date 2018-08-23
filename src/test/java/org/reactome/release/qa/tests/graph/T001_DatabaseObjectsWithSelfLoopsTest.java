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
import org.reactome.release.qa.check.graph.T001_DatabaseObjectsWithSelfLoops;

public class T001_DatabaseObjectsWithSelfLoopsTest extends QACheckReportComparisonTester {

    public T001_DatabaseObjectsWithSelfLoopsTest() {
        super(new T001_DatabaseObjectsWithSelfLoops(), 5);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        List<Instance> fixture = new ArrayList<Instance>();
        Schema schema = dba.getSchema();
        
        // A valid empty EWAS.
        SchemaClass ewasCls = schema.getClassByName(ReactomeJavaConstants.EntityWithAccessionedSequence);
        GKInstance ewas = new GKInstance(ewasCls);
        ewas.setDbAdaptor(dba);
        fixture.add(ewas);
        
        // A valid EWAS with a reference to another event.
        ewas = new GKInstance(ewasCls);
        ewas.setDbAdaptor(dba);
        fixture.add(ewas);
        GKInstance other = new GKInstance(ewasCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        ewas.addAttributeValue(ReactomeJavaConstants.inferredFrom, other);
        
        // An invalid EWAS refering to itself.
        ewas = new GKInstance(ewasCls);
        ewas.setDbAdaptor(dba);
        fixture.add(ewas);
        ewas.addAttributeValue(ReactomeJavaConstants.inferredFrom, ewas);
        
        // A valid empty black box event.
        SchemaClass bbeCls = schema.getClassByName(ReactomeJavaConstants.BlackBoxEvent);
        GKInstance bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        
        // A valid black box event with a multi-valued reference to another event.
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        other = new GKInstance(bbeCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        bbe.addAttributeValue(ReactomeJavaConstants.precedingEvent, other);
        
        // An invalid black box event with a multi-valued reference to itself.
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        bbe.addAttributeValue(ReactomeJavaConstants.precedingEvent, bbe);
        
        // An invalid black box event refering to itself twice.
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        bbe.addAttributeValue(ReactomeJavaConstants.precedingEvent, bbe);
        bbe.addAttributeValue(ReactomeJavaConstants.orthologousEvent, bbe);
       
        // A valid black box event with a single-valued reference to another event.
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        other = new GKInstance(bbeCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        bbe.addAttributeValue(JavaConstants.templateEvent, other);
        
        // An invalid black box event with a single-valued reference to itself.
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        bbe.addAttributeValue(JavaConstants.templateEvent, bbe);
        
        return fixture;
    }

}
