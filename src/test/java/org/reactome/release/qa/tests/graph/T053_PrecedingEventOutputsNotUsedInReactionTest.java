package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T053_PrecedingEventOutputsNotUsedInReaction;

public class T053_PrecedingEventOutputsNotUsedInReactionTest
extends QACheckReportComparisonTester {

    public T053_PrecedingEventOutputsNotUsedInReactionTest() {
        super(new T053_PrecedingEventOutputsNotUsedInReaction(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();

        // A valid empty RLE.
         SchemaClass rleCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.Reaction);
         GKInstance rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);

         // A valid RLE sharing its sole input shared as a preceding event output.
         rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         GKInstance preceding = new GKInstance(rleCls);
         preceding.setDbAdaptor(dba);
         fixture.add(preceding);
         rle.addAttributeValue(ReactomeJavaConstants.precedingEvent, preceding);
         SchemaClass entityCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
         GKInstance input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         preceding.addAttributeValue(ReactomeJavaConstants.output, input);

         // A valid RLE with an input shared as a preceding event output.
         rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         preceding = new GKInstance(rleCls);
         preceding.setDbAdaptor(dba);
         fixture.add(preceding);
         rle.addAttributeValue(ReactomeJavaConstants.precedingEvent, preceding);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         GKInstance output = new GKInstance(entityCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         preceding.addAttributeValue(ReactomeJavaConstants.output, output);
         preceding.addAttributeValue(ReactomeJavaConstants.output, input);

         // Another valid RLE with an input shared as a preceding event output.
         rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         preceding = new GKInstance(rleCls);
         preceding.setDbAdaptor(dba);
         fixture.add(preceding);
         rle.addAttributeValue(ReactomeJavaConstants.precedingEvent, preceding);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         GKInstance another = new GKInstance(entityCls);
         another.setDbAdaptor(dba);
         fixture.add(another);
         rle.addAttributeValue(ReactomeJavaConstants.input, another);
         preceding.addAttributeValue(ReactomeJavaConstants.output, input);

         // An invalid RLE with no input shared as a preceding event output.
         rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         preceding = new GKInstance(rleCls);
         preceding.setDbAdaptor(dba);
         fixture.add(preceding);
         rle.addAttributeValue(ReactomeJavaConstants.precedingEvent, preceding);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         output = new GKInstance(entityCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         preceding.addAttributeValue(ReactomeJavaConstants.output, output);

         // A valid RLE with an output shared with a catalyst entity.
         rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         preceding = new GKInstance(rleCls);
         preceding.setDbAdaptor(dba);
         fixture.add(preceding);
         rle.addAttributeValue(ReactomeJavaConstants.precedingEvent, preceding);
         output = new GKInstance(entityCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         preceding.addAttributeValue(ReactomeJavaConstants.output, output);
         SchemaClass catActCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.CatalystActivity);
         GKInstance catAct = new GKInstance(catActCls);
         catAct.setDbAdaptor(dba);
         fixture.add(catAct);
         rle.addAttributeValue(ReactomeJavaConstants.catalystActivity, catAct);
         catAct.addAttributeValue(ReactomeJavaConstants.physicalEntity, output);

         // A valid RLE with an output shared as a regulator.
         rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         preceding = new GKInstance(rleCls);
         preceding.setDbAdaptor(dba);
         fixture.add(preceding);
         rle.addAttributeValue(ReactomeJavaConstants.precedingEvent, preceding);
         output = new GKInstance(entityCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         preceding.addAttributeValue(ReactomeJavaConstants.output, output);
         SchemaClass regCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.Regulation);
         GKInstance regulation = new GKInstance(regCls);
         regulation.setDbAdaptor(dba);
         fixture.add(regulation);
         rle.addAttributeValue(ReactomeJavaConstants.regulatedBy, regulation);
         regulation.addAttributeValue(ReactomeJavaConstants.regulator, output);

         // A valid inferred RLE with no input shared with a preceding event.
         rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         GKInstance inferralSource = new GKInstance(rleCls);
         inferralSource.setDbAdaptor(dba);
         fixture.add(inferralSource);
         rle.addAttributeValue(ReactomeJavaConstants.inferredFrom, inferralSource);
         preceding = new GKInstance(rleCls);
         preceding.setDbAdaptor(dba);
         fixture.add(preceding);
         rle.addAttributeValue(ReactomeJavaConstants.precedingEvent, preceding);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         output = new GKInstance(entityCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         preceding.addAttributeValue(ReactomeJavaConstants.output, output);

         // A valid RLE with an output shared very indirectly.
         rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         SchemaClass entitySetCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.DefinedSet);
         GKInstance inputSet = new GKInstance(entitySetCls);
         inputSet.setDbAdaptor(dba);
         fixture.add(inputSet);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         inputSet.addAttributeValue(ReactomeJavaConstants.hasMember, input);
         rle.addAttributeValue(ReactomeJavaConstants.input, inputSet);
         preceding = new GKInstance(rleCls);
         preceding.setDbAdaptor(dba);
         fixture.add(preceding);
         rle.addAttributeValue(ReactomeJavaConstants.precedingEvent, preceding);
         GKInstance outputEntitySet = new GKInstance(entitySetCls);
         outputEntitySet.setDbAdaptor(dba);
         fixture.add(outputEntitySet);
         preceding.addAttributeValue(ReactomeJavaConstants.output, outputEntitySet);
         output = new GKInstance(entityCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         outputEntitySet.addAttributeValue(ReactomeJavaConstants.hasMember, output);
         regulation = new GKInstance(regCls);
         regulation.setDbAdaptor(dba);
         fixture.add(regulation);
         rle.addAttributeValue(ReactomeJavaConstants.regulatedBy, regulation);
         GKInstance entitySet = new GKInstance(entitySetCls);
         entitySet.setDbAdaptor(dba);
         fixture.add(entitySet);
         regulation.addAttributeValue(ReactomeJavaConstants.regulator, entitySet);
         SchemaClass candidateSetCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.CandidateSet);
         GKInstance candidateSet = new GKInstance(candidateSetCls);
         candidateSet.setDbAdaptor(dba);
         fixture.add(candidateSet);
         entitySet.addAttributeValue(ReactomeJavaConstants.hasMember, candidateSet);
         GKInstance candidate = new GKInstance(entityCls);
         candidate.setDbAdaptor(dba);
         fixture.add(candidate);
         candidateSet.addAttributeValue(ReactomeJavaConstants.hasCandidate, candidate);
         candidateSet.addAttributeValue(ReactomeJavaConstants.hasCandidate, output);

         return fixture;
      }

}
