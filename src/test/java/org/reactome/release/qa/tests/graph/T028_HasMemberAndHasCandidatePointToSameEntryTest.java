package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T028_HasMemberAndHasCandidatePointToSameEntry;

public class T028_HasMemberAndHasCandidatePointToSameEntryTest extends QACheckReportComparisonTester {

    public T028_HasMemberAndHasCandidatePointToSameEntryTest() {
        super(new T028_HasMemberAndHasCandidatePointToSameEntry(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass entityCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
        GKInstance candidate = new GKInstance(entityCls);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.CandidateSet,
                ReactomeJavaConstants.hasCandidate,
                ReactomeJavaConstants.hasMember);
        
        // Three valid and one invalid candidate sets.
        return factory.createTestFixture(candidate, candidate);
      }

}
