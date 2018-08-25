package org.reactome.release.qa.tests.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.graph.T031_CatalystActivityWherePhysicalEntityAndActiveUnitPointToComplex;

public class T031_CatalystActivityWherePhysicalEntityAndActiveUnitPointToComplexTest
extends QACheckReportComparisonTester {

    public T031_CatalystActivityWherePhysicalEntityAndActiveUnitPointToComplexTest() {
        super(new T031_CatalystActivityWherePhysicalEntityAndActiveUnitPointToComplex(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        List<Instance> fixture = new ArrayList<Instance>();
        Schema schema = dba.getSchema();
        
        // A valid empty catalyst.
        SchemaClass catActCls = schema.getClassByName(ReactomeJavaConstants.CatalystActivity);
        GKInstance catAct = new GKInstance(catActCls);
        catAct.setDbAdaptor(dba);
        fixture.add(catAct);
        
        // A valid catalyst with activity different from active unit.
        catAct = new GKInstance(catActCls);
        catAct.setDbAdaptor(dba);
        fixture.add(catAct);
        SchemaClass complexCls = schema.getClassByName(ReactomeJavaConstants.Complex);
        GKInstance complex = new GKInstance(complexCls);
        complex.setDbAdaptor(dba);
        fixture.add(complex);
        catAct.addAttributeValue(ReactomeJavaConstants.physicalEntity, complex);
        GKInstance unit = new GKInstance(complexCls);
        unit.setDbAdaptor(dba);
        fixture.add(unit);
        catAct.addAttributeValue(ReactomeJavaConstants.activeUnit, unit);
        
        // An invalid catalyst with activity the same as active unit.
        catAct = new GKInstance(catActCls);
        catAct.setDbAdaptor(dba);
        fixture.add(catAct);
        complex = new GKInstance(complexCls);
        complex.setDbAdaptor(dba);
        fixture.add(complex);
        catAct.addAttributeValue(ReactomeJavaConstants.physicalEntity, complex);
        catAct.addAttributeValue(ReactomeJavaConstants.activeUnit, complex);
        
        return fixture;
    }

}
