package org.reactome.release.qa.check.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingValueCheck;

public class T007_EntitiesWithoutStdId extends MissingValueCheck {

    public T007_EntitiesWithoutStdId() {
        super(ReactomeJavaConstants.PhysicalEntity,
                ReactomeJavaConstants.stableIdentifier);
    }

    private static final String DESCRIPTION = "Entities without a stable id";

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected Collection<Instance> fetchMissing() {
        Collection<Instance> missingEntities = super.fetchMissing();
        Collection<Instance> missingEvents = fetchInstancesMissingAttribute(
                ReactomeJavaConstants.Event,
                ReactomeJavaConstants.stableIdentifier);
        return Stream.concat(missingEntities.stream(), missingEvents.stream())
                     .collect(Collectors.toList());
    }

    @Override
    protected List<Instance> createTestFixture() {
        List<Instance> fixture = new ArrayList<Instance>();
        Instance entity = createInstance(ReactomeJavaConstants.SimpleEntity);
        fixture.add(entity);
        Instance event = createInstance(ReactomeJavaConstants.BlackBoxEvent);
        fixture.add(event);
        return fixture;
    }

}
