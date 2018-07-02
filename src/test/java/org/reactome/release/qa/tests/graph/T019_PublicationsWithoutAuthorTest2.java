package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T019_PublicationsWithoutAuthor;

public class T019_PublicationsWithoutAuthorTest2 extends QACheckReportComparisonTester {

    public T019_PublicationsWithoutAuthorTest2() {
        super(new T019_PublicationsWithoutAuthor(), 2);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass personCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Person);
        GKInstance author = new GKInstance(personCls);
        author.setDbAdaptor(dba);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.Book,
                ReactomeJavaConstants.author);
        List<Instance> books = factory.createTestFixture(author);
       
        author = new GKInstance(personCls);
        author.setDbAdaptor(dba);
        factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.URL,
                ReactomeJavaConstants.author);
        List<Instance> urls = factory.createTestFixture(author);
        
        return Stream.concat(books.stream(), urls.stream())
                .collect(Collectors.toList());
     }

}
