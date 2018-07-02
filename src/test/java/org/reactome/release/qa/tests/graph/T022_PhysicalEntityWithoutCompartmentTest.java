package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T022_PhysicalEntityWithoutCompartment;

public class T022_PhysicalEntityWithoutCompartmentTest extends QACheckReportComparisonTester {

    public T022_PhysicalEntityWithoutCompartmentTest() {
        super(new T022_PhysicalEntityWithoutCompartment(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass compartmentCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Compartment);
        GKInstance compartment = new GKInstance(compartmentCls);
        compartment.setDbAdaptor(dba);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.SimpleEntity,
                ReactomeJavaConstants.compartment);
        
        return factory.createTestFixture(compartment);
      }

}
