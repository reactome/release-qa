package org.reactome.release.qa.check.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;
import org.gk.schema.SchemaClass;

public class T010_EntitySetWithoutMemberOrCandidate extends MissingValueCheck {
 
    public T010_EntitySetWithoutMemberOrCandidate() {
        super(ReactomeJavaConstants.EntitySet,
                ReactomeJavaConstants.hasMember);
    }

    private static final String DESCRIPTION = "EntitySets with neither a member nor a candidate";

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public void testCheck() {
        compareInvalidCountToExpected(2);
    }

    public Collection<Instance> fetchMissing() {
        // Instances without members.
        Collection<Instance> missing = super.fetchMissing();
        // Filter out instances with a candidate.
        return missing.stream()
                .filter(es -> !hasCandidate(es))
                .collect(Collectors.toList());
    }

    private boolean hasCandidate(Instance entitySet) {
        SchemaClass klass = entitySet.getSchemClass();
        return klass.isValidAttribute(ReactomeJavaConstants.hasCandidate) &&
                getAttributeValue(entitySet, ReactomeJavaConstants.hasCandidate) != null;
    }

    @Override
    protected List<Instance> createTestFixture() {
        List<Instance> fixture = new ArrayList<Instance>();
        
        // An empty DefinedSet.
        Instance invalid = createInstance(ReactomeJavaConstants.DefinedSet);
        fixture.add(invalid);
        
        // An empty CandidateSet.
        invalid = createInstance(ReactomeJavaConstants.CandidateSet);
        fixture.add(invalid);
        
        // A valid EntitySet with a member.
        Instance entity = createInstance(ReactomeJavaConstants.SimpleEntity);
        Instance valid = createInstance(ReactomeJavaConstants.DefinedSet);
        setAttributeValue(valid, ReactomeJavaConstants.hasMember, entity);
        fixture.add(valid);

        // A valid CandidateSet with a candidate.
        valid = createInstance(ReactomeJavaConstants.CandidateSet);
        setAttributeValue(valid, ReactomeJavaConstants.hasCandidate, entity);
        fixture.add(valid);

        return fixture;
    }

}
