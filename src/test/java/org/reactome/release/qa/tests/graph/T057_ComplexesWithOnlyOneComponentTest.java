package org.reactome.release.qa.tests.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.graph.T057_ComplexesWithOnlyOneComponent;

public class T057_ComplexesWithOnlyOneComponentTest extends QACheckReportComparisonTester {

    public T057_ComplexesWithOnlyOneComponentTest() {
        super(new T057_ComplexesWithOnlyOneComponent(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        List<Instance> fixture = new ArrayList<Instance>();
        Schema schema = dba.getSchema();
        
        // A valid empty complex.
        SchemaClass complexCls = schema.getClassByName(ReactomeJavaConstants.Complex);
        GKInstance complex = new GKInstance(complexCls);
        complex.setDbAdaptor(dba);
        fixture.add(complex);
        
        // A valid complex with two entities.
        complex = new GKInstance(complexCls);
        complex.setDbAdaptor(dba);
        fixture.add(complex);
        SchemaClass entityCls = schema.getClassByName(ReactomeJavaConstants.SimpleEntity);
        GKInstance entity = new GKInstance(entityCls);
        entity.setDbAdaptor(dba);
        fixture.add(entity);
        complex.addAttributeValue(ReactomeJavaConstants.hasComponent, entity);
        GKInstance other = new GKInstance(entityCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        complex.addAttributeValue(ReactomeJavaConstants.hasComponent, other);
        
        // A valid complex with one duplicated entity.
        complex = new GKInstance(complexCls);
        complex.setDbAdaptor(dba);
        fixture.add(complex);
        entity = new GKInstance(entityCls);
        entity.setDbAdaptor(dba);
        fixture.add(entity);
        complex.addAttributeValue(ReactomeJavaConstants.hasComponent, entity);
        complex.addAttributeValue(ReactomeJavaConstants.hasComponent, entity);
        
        // An invalid complex.
        complex = new GKInstance(complexCls);
        complex.setDbAdaptor(dba);
        fixture.add(complex);
        entity = new GKInstance(entityCls);
        entity.setDbAdaptor(dba);
        fixture.add(entity);
        complex.addAttributeValue(ReactomeJavaConstants.hasComponent, entity);
        
        return fixture;
    }

}
