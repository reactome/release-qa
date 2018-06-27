package org.reactome.release.qa.check.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingAllValuesCheck;

public class T002_PersonWithoutProperName extends MissingAllValuesCheck {

    private static final String DESCRIPTION = "Persons without a complete proper name";

    public T002_PersonWithoutProperName() {
        super(ReactomeJavaConstants.Person,
                ReactomeJavaConstants.firstname,
                ReactomeJavaConstants.initial);
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public void testCheck() {
        compareInvalidCountToExpected(5);
    }

    @Override
    protected Collection<Instance> fetchMissing() {
        // The persons missing a firstname and initial.
        Collection<Instance> missingRest = super.fetchMissing();
        // Add the persons missing a surname.
        Collection<Instance> missingSurname = fetchInstancesMissingAttribute(
                ReactomeJavaConstants.Person,
                ReactomeJavaConstants.surname);
        // Return a set to remove redundant persons without
        // a surname, first name or initial.
        return Stream.concat(missingRest.stream(), missingSurname.stream())
                .collect(Collectors.toSet());
    }

    /**
     * Creates seven new Person instances, two of which are valid.
     */
    @Override
    protected List<Instance> createTestFixture() {
        // Create a fixture with four persons, none of which
        // has a surname.
        List<Instance> fixture = createTestFixture("Test", "T");
        
        // A valid person with surname and first name.
        Instance person = createInstance(ReactomeJavaConstants.Person);
        setAttributeValue(person, ReactomeJavaConstants.surname, "Valid");
        setAttributeValue(person, ReactomeJavaConstants.firstname, "Valid");
        fixture.add(person);
        
        // A valid person with surname and initial.
        person = createInstance(ReactomeJavaConstants.Person);
        setAttributeValue(person, ReactomeJavaConstants.surname, "Valid");
        setAttributeValue(person, ReactomeJavaConstants.initial, "V");
        fixture.add(person);
        
        // An invalid person with neither a first name nor an initial.
        person = createInstance(ReactomeJavaConstants.Person);
        setAttributeValue(person, ReactomeJavaConstants.surname, "Invalid");
        fixture.add(person);
        
        return fixture;
    }

}