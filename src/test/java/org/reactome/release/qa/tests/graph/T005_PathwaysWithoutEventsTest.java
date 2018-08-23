package org.reactome.release.qa.tests.graph;

import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T005_PathwaysWithoutEvents;

public class T005_PathwaysWithoutEventsTest extends QACheckReportComparisonTester {

    public T005_PathwaysWithoutEventsTest() {
        super(new T005_PathwaysWithoutEvents(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass schemaCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Reaction);
        GKInstance event = new GKInstance(schemaCls);
        // The factory creates a fixture with one valid and one invalid instance.
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.Pathway,
                ReactomeJavaConstants.hasEvent);
        return factory.createTestFixture(event);
    }

}
