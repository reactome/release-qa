package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T024_ReferenceDatabaseWithoutUrls;

public class T024_ReferenceDatabaseWithoutUrlsTest extends QACheckReportComparisonTester {

    public T024_ReferenceDatabaseWithoutUrlsTest() {
        super(new T024_ReferenceDatabaseWithoutUrls(), 3);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.ReferenceDatabase,
                ReactomeJavaConstants.accessUrl,
                ReactomeJavaConstants.url);
        
        return factory.createTestFixture("Test", "Test");
      }

}
