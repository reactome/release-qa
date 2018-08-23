package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T050_DuplicatedLiteratureReferences;

public class T050_DuplicatedLiteratureReferencesTest extends QACheckReportComparisonTester {

    public T050_DuplicatedLiteratureReferencesTest() {
        super(new T050_DuplicatedLiteratureReferences(), 2);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();
        SchemaClass litRefCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.LiteratureReference);
        
        // A valid literature reference.
        GKInstance litRef = new GKInstance(litRefCls);
        litRef.setDbAdaptor(dba);
        litRef.setAttributeValue(ReactomeJavaConstants.pubMedIdentifier, new Integer(-1));
        fixture.add(litRef);
        
        // An invalid literature reference.
        litRef = new GKInstance(litRefCls);
        litRef.setDbAdaptor(dba);
        litRef.setAttributeValue(ReactomeJavaConstants.pubMedIdentifier, new Integer(-1));
        fixture.add(litRef);
        
        // Another invalid literature reference.
        litRef = new GKInstance(litRefCls);
        litRef.setDbAdaptor(dba);
        litRef.setAttributeValue(ReactomeJavaConstants.pubMedIdentifier, new Integer(-1));
        fixture.add(litRef);
        
        return fixture;
      }

}
