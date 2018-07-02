package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T018_DatabaseObjectsWithoutCreated;

public class T018_DatabaseObjectsWithoutCreatedTest extends QACheckReportComparisonTester {

    /**
     * The classes which don't required a created slot.
     */
    private static final String[] OPTIONAL = {
            ReactomeJavaConstants.InstanceEdit,
            ReactomeJavaConstants.DatabaseIdentifier,
            ReactomeJavaConstants.Taxon,
            ReactomeJavaConstants.Person,
            ReactomeJavaConstants.ReferenceEntity
    };

    public T018_DatabaseObjectsWithoutCreatedTest() {
        super(new T018_DatabaseObjectsWithoutCreated(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        SchemaClass ieCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.InstanceEdit);
        GKInstance ie = new GKInstance(ieCls);
        ie.setDbAdaptor(dba);
        
        // One valid and one invalid pathway.
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.Pathway,
                ReactomeJavaConstants.created);
        List<Instance> fixture = factory.createTestFixture(ie);
        
        // Valid optional instances.
        Schema schema = dba.getSchema();
        for (String optClsName : OPTIONAL) {
            SchemaClass optCls = schema.getClassByName(optClsName);
            GKInstance instance = new GKInstance(optCls);
            instance.setDbAdaptor(dba);
            fixture.add(instance);
        }
        
        return fixture;
     }

}
