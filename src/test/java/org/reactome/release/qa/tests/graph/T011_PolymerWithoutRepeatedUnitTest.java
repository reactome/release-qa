package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T011_PolymerWithoutRepeatedUnit;

public class T011_PolymerWithoutRepeatedUnitTest extends QACheckReportComparisonTester {

    public T011_PolymerWithoutRepeatedUnitTest() {
        super(new T011_PolymerWithoutRepeatedUnit(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass entityCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
        GKInstance entity = new GKInstance(entityCls);
        entity.setDbAdaptor(dba);
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.Polymer,
                ReactomeJavaConstants.repeatedUnit);
        return factory.createTestFixture(entity);
     }

}
