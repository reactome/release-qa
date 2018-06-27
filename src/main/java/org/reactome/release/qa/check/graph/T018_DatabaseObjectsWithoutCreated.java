package org.reactome.release.qa.check.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.junit.Test;

public class T018_DatabaseObjectsWithoutCreated extends MissingValueCheck {

    private static String[] DISQUALIFIED = {
            ReactomeJavaConstants.InstanceEdit,
            ReactomeJavaConstants.DatabaseIdentifier,
            ReactomeJavaConstants.Taxon,
            ReactomeJavaConstants.Person,
            ReactomeJavaConstants.ReferenceEntity
    };

    public T018_DatabaseObjectsWithoutCreated() {
        super(ReactomeJavaConstants.DatabaseObject, ReactomeJavaConstants.created);
    }

    @Override
    public String getDescription() {
        return "Qualifying " + super.getDescription();
    }

    @Override
    @Test
    public void testCheck() {
        compareInvalidCountToExpected(1);
    }

    @Override
    protected Collection<Instance> fetchMissing() {
        Schema schema = dba.getSchema();
        // The root class.
        SchemaClass root =
                schema.getClassByName(ReactomeJavaConstants.DatabaseObject);
        // All schema classes.
        @SuppressWarnings("unchecked")
        Collection<SchemaClass> classes = schema.getClasses();
        // The disqualified schema classes.
        List<SchemaClass> disqualified = Stream.of(DISQUALIFIED)
                .map(clsName -> schema.getClassByName(clsName))
                .collect(Collectors.toList());

        // Return the qualified instances without a created slot.
        return classes.stream()
                // Root subclasses which are not disqualified.
                .filter(cls -> cls.getSuperClasses().contains(root) &&
                        !disqualified.stream().anyMatch(dq -> cls.isa(dq)))
                // The qualified class names.
                .map(cls -> cls.getName())
                // The qualified instances.
                .map(cls -> fetchInstancesMissingAttribute(cls, ReactomeJavaConstants.created))
                // Flatten the lists.
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    protected List<Instance> createTestFixture() {
        List<Instance> fixture = new ArrayList<Instance>();
        // Instances which need not have a created slot.
        for (String clsName: DISQUALIFIED) {
            Instance inst = createInstance(clsName);
            fixture.add(inst);
        }
        // An instance which must have a created slot.
        Instance inst = createInstance(ReactomeJavaConstants.Pathway);
        fixture.add(inst);
        
        return fixture;
    }

}
