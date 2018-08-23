package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T021_RegulationsWithoutRegulatedEntityOrRegulator;

public class T021_RegulationsWithoutRegulatedEntityOrRegulatorTest
extends QACheckReportComparisonTester {

    public T021_RegulationsWithoutRegulatedEntityOrRegulatorTest() {
        super(new T021_RegulationsWithoutRegulatedEntityOrRegulator(), 4);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Schema schema = dba.getSchema();
        SchemaClass entityCls =
                schema.getClassByName(ReactomeJavaConstants.SimpleEntity);
        GKInstance regulator = new GKInstance(entityCls);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.PositiveRegulation,
                ReactomeJavaConstants.regulator);
        // Two invalid regulations, both missing a regulated by referral.
        // The regulation without both is reported twice.
        List<Instance> fixture = factory.createTestFixture(regulator);
        
        // An invalid regulation with a regulated by referral but no regulator.
        SchemaClass bbeCls = schema.getClassByName(ReactomeJavaConstants.BlackBoxEvent);
        GKInstance bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        SchemaClass regCls = schema.getClassByName(ReactomeJavaConstants.PositiveRegulation);
        GKInstance regulation = new GKInstance(regCls);
        regulation.setDbAdaptor(dba);
        fixture.add(regulation);
        bbe.setAttributeValue(ReactomeJavaConstants.regulatedBy, regulation);
        
        // The sole valid regulation with both a regulated by referral
        // and a regulator.
        bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        regulation = new GKInstance(regCls);
        regulation.setDbAdaptor(dba);
        fixture.add(regulation);
        bbe.setAttributeValue(ReactomeJavaConstants.regulatedBy, regulation);
        regulator = new GKInstance(entityCls);
        regulator.setDbAdaptor(dba);
        fixture.add(regulator);
        regulation.setAttributeValue(ReactomeJavaConstants.regulator, regulator);
        
        return fixture;
     }

}
