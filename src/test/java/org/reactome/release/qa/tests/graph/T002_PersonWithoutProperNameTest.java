package org.reactome.release.qa.tests.graph;

import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T002_PersonWithoutProperName;

public class T002_PersonWithoutProperNameTest extends QACheckReportComparisonTester {

    public T002_PersonWithoutProperNameTest() {
        super(new T002_PersonWithoutProperName(), 5);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        // The factory creates a fixture with four persons, none of which
        // has a surname.
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.Person,
                ReactomeJavaConstants.firstname,
                ReactomeJavaConstants.initial);
        Collection<Instance> fixture = factory.createTestFixture("Test", "T");
        
        // A valid person with surname and first name.
        // Make the instance.
        SchemaClass schemaCls = dba.getSchema().getClassByName(ReactomeJavaConstants.Person);
        GKInstance person = new GKInstance(schemaCls);
        person.setDbAdaptor(dba);
        person.setAttributeValue(ReactomeJavaConstants.surname, "Valid");
        person.setAttributeValue(ReactomeJavaConstants.firstname, "Valid");
        fixture.add(person);
        
        // A valid person with surname and initial.
        person = new GKInstance(schemaCls);
        person.setDbAdaptor(dba);
        person.setAttributeValue(ReactomeJavaConstants.surname, "Valid");
        person.setAttributeValue(ReactomeJavaConstants.initial, "V");
        fixture.add(person);
        
        // An invalid person with neither a first name nor an initial.
        person = new GKInstance(schemaCls);
        person.setDbAdaptor(dba);
        person.setAttributeValue(ReactomeJavaConstants.surname, "Invalid");
        fixture.add(person);
        
        return fixture;
    }

}
