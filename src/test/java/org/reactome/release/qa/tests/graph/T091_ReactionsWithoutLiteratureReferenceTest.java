package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T091_ReactionsWithoutLiteratureReference;

public class T091_ReactionsWithoutLiteratureReferenceTest
extends QACheckReportComparisonTester {

    public T091_ReactionsWithoutLiteratureReferenceTest() {
        super(new T091_ReactionsWithoutLiteratureReference(), 3);
    }

    @Override
    /**
     * @return ten reaction {inferredFrom, literature reference, summation}
     *      permutations, of which three are invalid
     */
    protected Collection<Instance> createTestFixture() throws Exception {
        // A reaction for the inferredFrom slot.
        SchemaClass rleCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Reaction);
        GKInstance basis = new GKInstance(rleCls);

        // A literature reference.
        SchemaClass litRefCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.LiteratureReference);
        GKInstance litRef = new GKInstance(litRefCls);
        
        // A summation with a literature reference.
        SchemaClass summationCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Summation);
        GKInstance summation = new GKInstance(summationCls);
        summation.addAttributeValue(ReactomeJavaConstants.literatureReference, litRef);
        
        // The permutations.
        MissingValuesFixtureFactory factory = new MissingValuesFixtureFactory(dba,
                ReactomeJavaConstants.Reaction,
                ReactomeJavaConstants.inferredFrom,
                ReactomeJavaConstants.literatureReference,
                ReactomeJavaConstants.summation);
        List<Instance> fixture = factory.createTestFixture(basis, litRef, summation);
        
        // A summation without a literature reference.
        summation = new GKInstance(summationCls);
        summation.setDbAdaptor(dba);
        fixture.add(summation);
        // A reaction with both literature reference and the empty summation.
        GKInstance rle = new GKInstance(rleCls);
        rle.setDbAdaptor(dba);
        rle.addAttributeValue(ReactomeJavaConstants.summation, summation);
        fixture.add(rle);
        litRef = new GKInstance(litRefCls);
        litRef.setDbAdaptor(dba);
        fixture.add(litRef);
        rle.addAttributeValue(ReactomeJavaConstants.literatureReference, litRef);

        // A reaction without literature reference and with the empty summation.
        rle = new GKInstance(rleCls);
        rle.setDbAdaptor(dba);
        fixture.add(rle);
        rle.addAttributeValue(ReactomeJavaConstants.summation, summation);
        
        return fixture;
    }

}
