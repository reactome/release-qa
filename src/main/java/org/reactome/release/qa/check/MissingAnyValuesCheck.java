package org.reactome.release.qa.check;

import java.util.Collection;
import org.gk.model.Instance;

abstract public class MissingAnyValuesCheck extends MissingValuesCheck {

    public MissingAnyValuesCheck(String clsName, String ...attNames) {
        super(clsName, attNames);
    }

    @Override
    public String getDisplayName() {
        return getDescription("any");
    }

    @Override
    public void testCheck() {
        // All but one of the fixture instances is missing at least
        // one value.
        super.compareInvalidCountToExpected(fixture.size() - 1);
    }

    @Override
    protected Collection<Instance> fetchMissing() {
        return super.fetchInstancesMissingAnyAttributes(
                getSchemaClassName(),
                getAttributeNames());
    }

}
