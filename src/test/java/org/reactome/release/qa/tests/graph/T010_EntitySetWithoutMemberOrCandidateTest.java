package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T010_EntitySetWithoutMemberOrCandidate;

public class T010_EntitySetWithoutMemberOrCandidateTest extends QACheckReportComparisonTester {

    public T010_EntitySetWithoutMemberOrCandidateTest() {
        super(new T010_EntitySetWithoutMemberOrCandidate(), 2);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass entityCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
        GKInstance member = new GKInstance(entityCls);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.DefinedSet,
                ReactomeJavaConstants.hasMember);
        List<Instance> definedSets = factory.createTestFixture(member);
        
        member = new GKInstance(entityCls);
        GKInstance candidate = new GKInstance(entityCls);
        factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.CandidateSet,
                ReactomeJavaConstants.hasMember,
                ReactomeJavaConstants.hasCandidate);
        List<Instance> candidates = factory.createTestFixture(member, candidate);

        return Stream.concat(definedSets.stream(), candidates.stream())
                .collect(Collectors.toList());
    }

}
