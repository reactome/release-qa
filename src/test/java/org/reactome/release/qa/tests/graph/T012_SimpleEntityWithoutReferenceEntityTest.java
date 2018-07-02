package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T012_SimpleEntityWithoutReferenceEntity;

public class T012_SimpleEntityWithoutReferenceEntityTest extends QACheckReportComparisonTester {

    public T012_SimpleEntityWithoutReferenceEntityTest() {
        super(new T012_SimpleEntityWithoutReferenceEntity(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass refCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.ReferenceMolecule);
        GKInstance refMolecule = new GKInstance(refCls);
        refMolecule.setDbAdaptor(dba);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.SimpleEntity,
                ReactomeJavaConstants.referenceEntity);
        return factory.createTestFixture(refMolecule);
     }

}
