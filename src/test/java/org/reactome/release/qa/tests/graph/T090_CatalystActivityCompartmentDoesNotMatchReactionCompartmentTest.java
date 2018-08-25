package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.graph.T090_CatalystActivityCompartmentDoesNotMatchReactionCompartment;

public class T090_CatalystActivityCompartmentDoesNotMatchReactionCompartmentTest
extends QACheckReportComparisonTester {

    public T090_CatalystActivityCompartmentDoesNotMatchReactionCompartmentTest() {
        super(new T090_CatalystActivityCompartmentDoesNotMatchReactionCompartment(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();

        // A valid RLE.
         SchemaClass rleCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.Reaction);
         GKInstance rle = new GKInstance(rleCls);
         rle.setDbAdaptor(dba);
         fixture.add(rle);
         SchemaClass cmptCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.Compartment);
         GKInstance cmpt = new GKInstance(cmptCls);
         cmpt.setDbAdaptor(dba);
         fixture.add(cmpt);
         rle.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         SchemaClass catCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.CatalystActivity);
         GKInstance catActivity = new GKInstance(catCls);
         catActivity.setDbAdaptor(dba);
         fixture.add(catActivity);
         rle.addAttributeValue(ReactomeJavaConstants.catalystActivity, catActivity);
         SchemaClass entityCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
         GKInstance entity = new GKInstance(entityCls);
         entity.setDbAdaptor(dba);
         fixture.add(entity);
         entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         catActivity.addAttributeValue(ReactomeJavaConstants.physicalEntity, entity);

         // A RLE without a catalyst is valid.
          rle = new GKInstance(rleCls);
          rle.setDbAdaptor(dba);
          fixture.add(rle);

          // An invalid RLE without a common compartment.
           rle = new GKInstance(rleCls);
           rle.setDbAdaptor(dba);
           fixture.add(rle);
           cmpt = new GKInstance(cmptCls);
           cmpt.setDbAdaptor(dba);
           fixture.add(cmpt);
           rle.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
           catActivity = new GKInstance(catCls);
           catActivity.setDbAdaptor(dba);
           fixture.add(catActivity);
           rle.addAttributeValue(ReactomeJavaConstants.catalystActivity, catActivity);
           entity = new GKInstance(entityCls);
           entity.setDbAdaptor(dba);
           fixture.add(entity);
           entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
           catActivity.addAttributeValue(ReactomeJavaConstants.physicalEntity, entity);
           catActivity = new GKInstance(catCls);
           catActivity.setDbAdaptor(dba);
           fixture.add(catActivity);
           rle.addAttributeValue(ReactomeJavaConstants.catalystActivity, catActivity);
           entity = new GKInstance(entityCls);
           entity.setDbAdaptor(dba);
           fixture.add(entity);
           cmpt = new GKInstance(cmptCls);
           cmpt.setDbAdaptor(dba);
           fixture.add(cmpt);
           entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
           catActivity.addAttributeValue(ReactomeJavaConstants.physicalEntity, entity);

           // A valid RLE with a common compartment but non-identical compartments.
           rle = new GKInstance(rleCls);
           rle.setDbAdaptor(dba);
           fixture.add(rle);
           cmpt = new GKInstance(cmptCls);
           cmpt.setDbAdaptor(dba);
           fixture.add(cmpt);
           rle.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
           cmpt = new GKInstance(cmptCls);
           cmpt.setDbAdaptor(dba);
           fixture.add(cmpt);
           rle.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
           catActivity = new GKInstance(catCls);
           catActivity.setDbAdaptor(dba);
           fixture.add(catActivity);
           rle.addAttributeValue(ReactomeJavaConstants.catalystActivity, catActivity);
           entity = new GKInstance(entityCls);
           entity.setDbAdaptor(dba);
           fixture.add(entity);
           entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
           catActivity.addAttributeValue(ReactomeJavaConstants.physicalEntity, entity);

            // Another valid RLE with a common compartment but non-identical compartments.
           rle = new GKInstance(rleCls);
           rle.setDbAdaptor(dba);
           fixture.add(rle);
           cmpt = new GKInstance(cmptCls);
           cmpt.setDbAdaptor(dba);
           fixture.add(cmpt);
           rle.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
           cmpt = new GKInstance(cmptCls);
           cmpt.setDbAdaptor(dba);
           fixture.add(cmpt);
           rle.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
           catActivity = new GKInstance(catCls);
           catActivity.setDbAdaptor(dba);
           fixture.add(catActivity);
           rle.addAttributeValue(ReactomeJavaConstants.catalystActivity, catActivity);
           catActivity = new GKInstance(catCls);
           catActivity.setDbAdaptor(dba);
           fixture.add(catActivity);
           entity = new GKInstance(entityCls);
           entity.setDbAdaptor(dba);
           fixture.add(entity);
           entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
           catActivity.addAttributeValue(ReactomeJavaConstants.physicalEntity, entity);
           rle.addAttributeValue(ReactomeJavaConstants.catalystActivity, catActivity);
           entity = new GKInstance(entityCls);
           entity.setDbAdaptor(dba);
           fixture.add(entity);
           cmpt = new GKInstance(cmptCls);
           cmpt.setDbAdaptor(dba);
           fixture.add(cmpt);
           entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
           cmpt = new GKInstance(cmptCls);
           cmpt.setDbAdaptor(dba);
           fixture.add(cmpt);
           entity.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);

        return fixture;
      }

}
