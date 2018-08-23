package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T092_PotentialTranslocationReactionChangesParticipantsSchemaClass;

public class T092_PotentialTranslocationReactionChangesParticipantsSchemaClassTest
extends QACheckReportComparisonTester {

    public T092_PotentialTranslocationReactionChangesParticipantsSchemaClassTest() {
        super(new T092_PotentialTranslocationReactionChangesParticipantsSchemaClass(), 1);
    }

    @Override
    /**
     * @return ten reaction {inferredFrom, literature reference, summation}
     *      permutations, of which three are invalid
     */
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();

        // Two compartments.
        SchemaClass cmptCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Compartment);
        GKInstance cmpt = new GKInstance(cmptCls);
        cmpt.setDbAdaptor(dba);
        fixture.add(cmpt);
        GKInstance otherCmpt = new GKInstance(cmptCls);
        otherCmpt.setDbAdaptor(dba);
        fixture.add(otherCmpt);
        
        // A valid reaction with participants of the same class.
        SchemaClass rleCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Reaction);
        GKInstance rle = new GKInstance(rleCls);
        rle.setDbAdaptor(dba);
        fixture.add(rle);
        SchemaClass entityCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
        GKInstance input = new GKInstance(entityCls);
        input.setDbAdaptor(dba);
        fixture.add(input);
        input.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        GKInstance output = new GKInstance(entityCls);
        output.setDbAdaptor(dba);
        fixture.add(output);
        output.addAttributeValue(ReactomeJavaConstants.compartment, otherCmpt);
        rle.addAttributeValue(ReactomeJavaConstants.input, input);
        rle.addAttributeValue(ReactomeJavaConstants.output, output);

        // An invalid reaction with with different participant classes.
        rle = new GKInstance(rleCls);
        rle.setDbAdaptor(dba);
        fixture.add(rle);
        SchemaClass ewasCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.EntityWithAccessionedSequence);
        input = new GKInstance(ewasCls);
        input.setDbAdaptor(dba);
        fixture.add(input);
        input.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        output = new GKInstance(entityCls);
        output.setDbAdaptor(dba);
        fixture.add(output);
        output.addAttributeValue(ReactomeJavaConstants.compartment, otherCmpt);
        rle.addAttributeValue(ReactomeJavaConstants.input, input);
        rle.addAttributeValue(ReactomeJavaConstants.output, output);

        // A valid reaction with with different stoichiometry.
        rle = new GKInstance(rleCls);
        rle.setDbAdaptor(dba);
        fixture.add(rle);
        input = new GKInstance(ewasCls);
        input.setDbAdaptor(dba);
        fixture.add(input);
        input.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        output = new GKInstance(entityCls);
        output.setDbAdaptor(dba);
        fixture.add(output);
        output.addAttributeValue(ReactomeJavaConstants.compartment, otherCmpt);
        rle.addAttributeValue(ReactomeJavaConstants.input, input);
        rle.addAttributeValue(ReactomeJavaConstants.input, input);
        rle.addAttributeValue(ReactomeJavaConstants.output, output);

        // A valid reaction with with the same participant compartment.
        rle = new GKInstance(rleCls);
        rle.setDbAdaptor(dba);
        fixture.add(rle);
        input = new GKInstance(ewasCls);
        input.setDbAdaptor(dba);
        fixture.add(input);
        input.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        output = new GKInstance(entityCls);
        output.setDbAdaptor(dba);
        fixture.add(output);
        output.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        rle.addAttributeValue(ReactomeJavaConstants.input, input);
        rle.addAttributeValue(ReactomeJavaConstants.output, output);

        // A BBE is always valid.
        SchemaClass bbeCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.BlackBoxEvent);
        GKInstance bbe = new GKInstance(bbeCls);
        bbe.setDbAdaptor(dba);
        fixture.add(bbe);
        input = new GKInstance(ewasCls);
        input.setDbAdaptor(dba);
        fixture.add(input);
        input.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        output = new GKInstance(entityCls);
        output.setDbAdaptor(dba);
        fixture.add(output);
        output.addAttributeValue(ReactomeJavaConstants.compartment, otherCmpt);
        bbe.addAttributeValue(ReactomeJavaConstants.input, input);
        bbe.addAttributeValue(ReactomeJavaConstants.output, output);

        // A catalyst reaction is always valid.
        SchemaClass catCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.CatalystActivity);
        GKInstance catActivity = new GKInstance(catCls);
        catActivity.setDbAdaptor(dba);
        fixture.add(catActivity);
        rle = new GKInstance(rleCls);
        rle.setDbAdaptor(dba);
        fixture.add(rle);
        input = new GKInstance(ewasCls);
        input.setDbAdaptor(dba);
        fixture.add(input);
        input.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        output = new GKInstance(entityCls);
        output.setDbAdaptor(dba);
        fixture.add(output);
        output.addAttributeValue(ReactomeJavaConstants.compartment, otherCmpt);
        rle.addAttributeValue(ReactomeJavaConstants.catalystActivity, catActivity);
        rle.addAttributeValue(ReactomeJavaConstants.input, input);
        rle.addAttributeValue(ReactomeJavaConstants.output, output);

        // An inferred reaction is always valid.
        GKInstance basis = new GKInstance(rleCls);
        basis.setDbAdaptor(dba);
        fixture.add(basis);
        rle = new GKInstance(rleCls);
        rle.setDbAdaptor(dba);
        fixture.add(rle);
        input = new GKInstance(ewasCls);
        input.setDbAdaptor(dba);
        fixture.add(input);
        input.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
        output = new GKInstance(entityCls);
        output.setDbAdaptor(dba);
        fixture.add(output);
        output.addAttributeValue(ReactomeJavaConstants.compartment, otherCmpt);
        rle.addAttributeValue(ReactomeJavaConstants.inferredFrom, basis);
        rle.addAttributeValue(ReactomeJavaConstants.input, input);
        rle.addAttributeValue(ReactomeJavaConstants.output, output);
        
        return fixture;
    }

}
