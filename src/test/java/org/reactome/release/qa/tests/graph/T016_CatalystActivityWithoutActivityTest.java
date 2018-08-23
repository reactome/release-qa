package org.reactome.release.qa.tests.graph;

import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T016_CatalystActivityWithoutActivity;

public class T016_CatalystActivityWithoutActivityTest extends QACheckReportComparisonTester {

    public T016_CatalystActivityWithoutActivityTest() {
        super(new T016_CatalystActivityWithoutActivity(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass functionCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.GO_MolecularFunction);
        GKInstance function = new GKInstance(functionCls);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.CatalystActivity,
                ReactomeJavaConstants.activity);
        return factory.createTestFixture(function);
     }

}
