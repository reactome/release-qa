package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T029_ReactionsLikeEventWithoutInput;

public class T029_ReactionsLikeEventWithoutInputTest extends QACheckReportComparisonTester {

    public T029_ReactionsLikeEventWithoutInputTest() {
        super(new T029_ReactionsLikeEventWithoutInput(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass entityCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
        GKInstance input = new GKInstance(entityCls);
        input.setDbAdaptor(dba);
        SchemaClass reactionCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Reaction);
        GKInstance inferredFrom = new GKInstance(reactionCls);
        inferredFrom.setDbAdaptor(dba);
        // Make this inferral a valid event by adding an input.
        // Otherwise, this event would be detected as invalid.
        inferredFrom.setAttributeValue(ReactomeJavaConstants.input, input);

        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.Reaction,
                ReactomeJavaConstants.input,
                ReactomeJavaConstants.inferredFrom);
        
        // Three valid and one invalid candidate sets.
        return factory.createTestFixture(input, inferredFrom);
      }

}
