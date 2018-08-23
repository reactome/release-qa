package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T020_OpenSetsWithoutReferenceEntity;

public class T020_OpenSetsWithoutReferenceEntityTest extends QACheckReportComparisonTester {

    public T020_OpenSetsWithoutReferenceEntityTest() {
        super(new T020_OpenSetsWithoutReferenceEntity(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass refCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.ReferenceMolecule);
        GKInstance refMolecule = new GKInstance(refCls);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.OpenSet,
                ReactomeJavaConstants.referenceEntity);
        return factory.createTestFixture(refMolecule);
     }

}
