package org.reactome.release.qa.tests.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.graph.T025_EntriesWithCyclicInferredToRelations;

public class T025_EntriesWithCyclicInferredToRelationsTest extends QACheckReportComparisonTester {

    public T025_EntriesWithCyclicInferredToRelationsTest() {
        super(new T025_EntriesWithCyclicInferredToRelations(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        List<Instance> fixture = new ArrayList<Instance>();
        Schema schema = dba.getSchema();
        
        // A valid empty entity.
        SchemaClass complexCls = schema.getClassByName(ReactomeJavaConstants.Complex);
        GKInstance complex = new GKInstance(complexCls);
        complex.setDbAdaptor(dba);
        fixture.add(complex);
        
        // A valid inferred entity with a reference to a different entity.
        complex = new GKInstance(complexCls);
        complex.setDbAdaptor(dba);
        fixture.add(complex);
        SchemaClass entityCls = schema.getClassByName(ReactomeJavaConstants.SimpleEntity);
        GKInstance entity = new GKInstance(entityCls);
        entity.setDbAdaptor(dba);
        fixture.add(entity);
        complex.addAttributeValue(ReactomeJavaConstants.hasComponent, entity);
        GKInstance inferralSource = new GKInstance(entityCls);
        inferralSource.setDbAdaptor(dba);
        fixture.add(inferralSource);
        inferralSource.addAttributeValue(ReactomeJavaConstants.inferredTo, complex);
        
        // An invalid entity with a reference to its inferral source.
        complex = new GKInstance(complexCls);
        complex.setDbAdaptor(dba);
        fixture.add(complex);
        inferralSource = new GKInstance(entityCls);
        inferralSource.setDbAdaptor(dba);
        fixture.add(inferralSource);
        inferralSource.addAttributeValue(ReactomeJavaConstants.inferredTo, complex);
        complex.addAttributeValue(ReactomeJavaConstants.hasComponent, inferralSource);
        
        return fixture;
    }

}
