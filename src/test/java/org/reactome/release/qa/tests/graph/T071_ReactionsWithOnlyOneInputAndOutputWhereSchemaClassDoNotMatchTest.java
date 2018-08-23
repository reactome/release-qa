package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T071_ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatch;

public class T071_ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatchTest
extends QACheckReportComparisonTester {

    public T071_ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatchTest() {
        super(new T071_ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatch(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();

        // A valid RLE with one input and one output of the same class.
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
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         GKInstance output = new GKInstance(entityCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         rle.addAttributeValue(ReactomeJavaConstants.output, output);
         
         // An invalid RLE with one input and one output of different classes.
         rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         SchemaClass ewasCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.EntityWithAccessionedSequence);
         output = new GKInstance(ewasCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         rle.addAttributeValue(ReactomeJavaConstants.output, output);
         
         // A valid RLE with two inputs and one output.
         rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         output = new GKInstance(ewasCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         rle.addAttributeValue(ReactomeJavaConstants.output, output);
         
         // All polymerizations pass this check.
         SchemaClass polyCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.Polymerisation);
         rle = new GKInstance(polyCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         output = new GKInstance(ewasCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         rle.addAttributeValue(ReactomeJavaConstants.output, output);
         
         // All depolymerizations pass this check.
         SchemaClass depolyCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.Depolymerisation);
         rle = new GKInstance(depolyCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         input = new GKInstance(entityCls);
         input.setDbAdaptor(dba);
         fixture.add(input);
         rle.addAttributeValue(ReactomeJavaConstants.input, input);
         output = new GKInstance(ewasCls);
         output.setDbAdaptor(dba);
         fixture.add(output);
         rle.addAttributeValue(ReactomeJavaConstants.output, output);

        return fixture;
      }

}
