package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.graph.T013_ReferenceEntityWithoutIdentifier;

public class T013_ReferenceEntityWithoutIdentifierTest extends QACheckReportComparisonTester {

    public T013_ReferenceEntityWithoutIdentifierTest() {
        super(new T013_ReferenceEntityWithoutIdentifier(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.ReferenceEntity,
                ReactomeJavaConstants.identifier);
        return factory.createTestFixture("Test");
     }

}
