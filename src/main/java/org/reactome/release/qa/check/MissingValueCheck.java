package org.reactome.release.qa.check;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.gk.model.Instance;

public class MissingValueCheck extends ClassBasedQACheck {

    private String attName;

    protected MissingValueCheck(String clsName, String attName) {
        super(clsName);
        this.attName = attName;
    }

    public String getSchemaAttributeName() {
        return attName;
    }

    @Override
    public String getDescription() {
        return getSchemaClassName() + " instances without a " + getSchemaAttributeName();
    }

    @Override
    protected String getIssue(QueryResult result) {
        return "Missing a" + getSchemaAttributeName();
    }

    @Override
    protected Collection<QueryResult> fetchInvalid() {
        return stream(fetchMissing()).collect(Collectors.toList());
    }

    protected Collection<Instance> fetchMissing() {
        return fetchInstancesMissingAttribute(getSchemaClassName(), getSchemaAttributeName());
    }

}
