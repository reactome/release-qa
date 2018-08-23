package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T007_EntitiesWithoutStId;

public class T007_EntitiesWithoutStIdTest extends QACheckReportComparisonTester {

    public T007_EntitiesWithoutStIdTest() {
        super(new T007_EntitiesWithoutStId(), 2);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        
        // The factory creates a fixture with one valid and one invalid instance.
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.SimpleEntity,
                ReactomeJavaConstants.stableIdentifier);
        SchemaClass schemaCls = dba.getSchema().getClassByName(ReactomeJavaConstants.StableIdentifier);
        GKInstance stId = new GKInstance(schemaCls);
        Collection<Instance> entities = factory.createTestFixture(stId);

        factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.BlackBoxEvent,
                ReactomeJavaConstants.stableIdentifier);
        stId = new GKInstance(schemaCls);
        Collection<Instance> bbes = factory.createTestFixture(stId);
        
        return Stream.concat(entities.stream(), bbes.stream())
                .collect(Collectors.toList());
    }

}
