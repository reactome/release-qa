package org.reactome.release.qa.tests.graph;

import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T015_CatalystActivityWithoutPhysicalEntity;

public class T015_CatalystActivityWithoutPhysicalEntityTest extends QACheckReportComparisonTester {

    public T015_CatalystActivityWithoutPhysicalEntityTest() {
        super(new T015_CatalystActivityWithoutPhysicalEntity(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass entityCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
        GKInstance entity = new GKInstance(entityCls);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.CatalystActivity,
                ReactomeJavaConstants.physicalEntity);
        return factory.createTestFixture(entity);
     }

}
