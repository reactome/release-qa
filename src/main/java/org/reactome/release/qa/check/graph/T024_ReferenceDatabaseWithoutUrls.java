package org.reactome.release.qa.check.graph;

import java.util.List;

import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.MissingAnyValuesCheck;

public class T024_ReferenceDatabaseWithoutUrls extends MissingAnyValuesCheck {

    public T024_ReferenceDatabaseWithoutUrls() {
        super(ReactomeJavaConstants.ReferenceDatabase,
              ReactomeJavaConstants.accessUrl,
              ReactomeJavaConstants.url);
    }

    @Override
    protected List<Instance> createTestFixture() {
        return createTestFixture("Test", "Test");
    }

}
