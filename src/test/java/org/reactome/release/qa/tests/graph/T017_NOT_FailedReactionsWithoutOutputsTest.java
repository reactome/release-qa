package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T017_NOT_FailedReactionsWithoutOutputs;

public class T017_NOT_FailedReactionsWithoutOutputsTest extends QACheckReportComparisonTester {

    public T017_NOT_FailedReactionsWithoutOutputsTest() {
        super(new T017_NOT_FailedReactionsWithoutOutputs(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass entityCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
        // Add one valid and one invalid RLE.
        GKInstance entity = new GKInstance(entityCls);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.BlackBoxEvent,
                ReactomeJavaConstants.output);
        List<Instance> fixture = factory.createTestFixture(entity);

       // Add a valid empty failed output.
        SchemaClass frCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.FailedReaction);
        GKInstance fr = new GKInstance(frCls);
        fr.setDbAdaptor(dba);
        fixture.add(fr);
        
        return fixture;
     }

}
