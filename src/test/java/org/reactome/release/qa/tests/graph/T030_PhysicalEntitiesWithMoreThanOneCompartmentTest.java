package org.reactome.release.qa.tests.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.common.JavaConstants;
import org.reactome.release.qa.graph.T030_PhysicalEntitiesWithMoreThanOneCompartment;

public class T030_PhysicalEntitiesWithMoreThanOneCompartmentTest
extends QACheckReportComparisonTester {

    public T030_PhysicalEntitiesWithMoreThanOneCompartmentTest() {
        super(new T030_PhysicalEntitiesWithMoreThanOneCompartment(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        List<Instance> fixture = new ArrayList<Instance>();
        Schema schema = dba.getSchema();
        
        // A valid empty entity.
        SchemaClass entityCls = schema.getClassByName(ReactomeJavaConstants.SimpleEntity);
        GKInstance entity = new GKInstance(entityCls);
        entity.setDbAdaptor(dba);
        fixture.add(entity);
        
        // A valid entity with one compartment.
        entity = new GKInstance(entityCls);
        entity.setDbAdaptor(dba);
        fixture.add(entity);
        SchemaClass cmptCls = schema.getClassByName(ReactomeJavaConstants.Compartment);
        GKInstance cmpt = new GKInstance(cmptCls);
        cmpt.setDbAdaptor(dba);
        fixture.add(cmpt);
        entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        
        // An invalid entity with two compartments.
        entity = new GKInstance(entityCls);
        entity.setDbAdaptor(dba);
        fixture.add(entity);
        cmpt = new GKInstance(cmptCls);
        cmpt.setDbAdaptor(dba);
        fixture.add(cmpt);
        entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        cmpt = new GKInstance(cmptCls);
        cmpt.setDbAdaptor(dba);
        fixture.add(cmpt);
        entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        
        // A valid entity on another cell with two compartments.
        entity = new GKInstance(entityCls);
        entity.setDbAdaptor(dba);
        fixture.add(entity);
        SchemaClass bbeCls = schema.getClassByName(ReactomeJavaConstants.BlackBoxEvent);
        GKInstance bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        bbe.addAttributeValue(JavaConstants.entityOnOtherCell, entity);
        cmpt = new GKInstance(cmptCls);
        cmpt.setDbAdaptor(dba);
        fixture.add(cmpt);
        entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        cmpt = new GKInstance(cmptCls);
        cmpt.setDbAdaptor(dba);
        fixture.add(cmpt);
        entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        
        // A valid inferred entity with two compartments.
        entity = new GKInstance(entityCls);
        entity.setDbAdaptor(dba);
        fixture.add(entity);
        GKInstance other = new GKInstance(entityCls);
        other.setDbAdaptor(dba);
        fixture.add(other);
        bbe.addAttributeValue(JavaConstants.entityOnOtherCell, entity);
        cmpt = new GKInstance(cmptCls);
        cmpt.setDbAdaptor(dba);
        fixture.add(cmpt);
        entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        cmpt = new GKInstance(cmptCls);
        cmpt.setDbAdaptor(dba);
        fixture.add(cmpt);
        entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        
        return fixture;
    }

}
