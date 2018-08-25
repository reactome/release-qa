package org.reactome.release.qa.tests.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.graph.T027_EntriesWithOtherCyclicRelations;

public class T027_EntriesWithOtherCyclicRelationsTest extends QACheckReportComparisonTester {

    public T027_EntriesWithOtherCyclicRelationsTest() {
        super(new T027_EntriesWithOtherCyclicRelations(), 2);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        List<Instance> fixture = new ArrayList<Instance>();
        Schema schema = dba.getSchema();
        
        // A valid empty entity set.
        SchemaClass esCls = schema.getClassByName(ReactomeJavaConstants.EntitySet);
        GKInstance es = new GKInstance(esCls);
        es.setDbAdaptor(dba);
        fixture.add(es);
        
        // A valid entity set with references to different entity sets.
        es = new GKInstance(esCls);
        es.setDbAdaptor(dba);
        fixture.add(es);
        GKInstance other = new GKInstance(esCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        es.addAttributeValue(ReactomeJavaConstants.hasMember, other);
        GKInstance another = new GKInstance(esCls);
        another.setDbAdaptor(dba);
        fixture.add(another);
        es.addAttributeValue(ReactomeJavaConstants.hasMember, another);
        
        // An invalid entity set with a cycle to itself.
        es = new GKInstance(esCls);
        es.setDbAdaptor(dba);
        fixture.add(es);
        other = new GKInstance(esCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        es.addAttributeValue(ReactomeJavaConstants.hasMember, other);
        other.addAttributeValue(ReactomeJavaConstants.hasMember, es);
        
        // An invalid GO term with a cycle to itself.
        SchemaClass mfCls = schema.getClassByName(ReactomeJavaConstants.GO_MolecularFunction);
        GKInstance mf = new GKInstance(mfCls);
        mf.setDbAdaptor(dba);
        fixture.add(mf);
        other = new GKInstance(mfCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        mf.addAttributeValue(ReactomeJavaConstants.componentOf, other);
        other.addAttributeValue(ReactomeJavaConstants.instanceOf, mf);

        return fixture;
    }

}
