package org.reactome.release.qa.tests.graph;

import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T014_InstanceEditWithoutAuthor;

public class T014_InstanceEditWithoutAuthorTest extends QACheckReportComparisonTester {

    public T014_InstanceEditWithoutAuthorTest() {
        super(new T014_InstanceEditWithoutAuthor(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass personCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Person);
        GKInstance author = new GKInstance(personCls);
        author.setDbAdaptor(dba);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.InstanceEdit,
                ReactomeJavaConstants.author);
        return factory.createTestFixture(author);
     }

}
