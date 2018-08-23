package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T064_DuplicatedEntitySets;

public class T064_DuplicatedEntitySetsTest extends QACheckReportComparisonTester {

    public T064_DuplicatedEntitySetsTest() {
        super(new T064_DuplicatedEntitySets(), 3);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();

        // A valid entity set.
         SchemaClass entitySetCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.DefinedSet);
         GKInstance entitySet = new GKInstance(entitySetCls);
         entitySet.setDbAdaptor(dba);
         fixture.add(entitySet);
         SchemaClass cmptCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.Compartment);
         GKInstance cmpt = new GKInstance(cmptCls);
         cmpt.setDbAdaptor(dba);
         fixture.add(cmpt);
         SchemaClass entityCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
         GKInstance member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         entitySet.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         entitySet.addAttributeValue(ReactomeJavaConstants.hasMember, member);

         // A valid entity set in the same compartment but with a different
         // entity.
         entitySet = new GKInstance(entitySetCls);
         entitySet.setDbAdaptor(dba);
         fixture.add(entitySet);
         entitySet.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         GKInstance otherMember = new GKInstance(entityCls);
         otherMember.setDbAdaptor(dba);
         fixture.add(otherMember);
         entitySet.addAttributeValue(ReactomeJavaConstants.hasMember, otherMember);

         // A valid entity set in a different compartment.
         entitySet = new GKInstance(entitySetCls);
         entitySet.setDbAdaptor(dba);
         fixture.add(entitySet);
         GKInstance otherCmpt = new GKInstance(cmptCls);
         otherCmpt.setDbAdaptor(dba);
         fixture.add(otherCmpt);
         entitySet.addAttributeValue(ReactomeJavaConstants.compartment, otherCmpt);
         entitySet.addAttributeValue(ReactomeJavaConstants.hasMember, member);

         // An invalid entity set in the same compartment with the same
         // member.
         entitySet = new GKInstance(entitySetCls);
         entitySet.setDbAdaptor(dba);
         fixture.add(entitySet);
         entitySet.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         entitySet.addAttributeValue(ReactomeJavaConstants.hasMember, member);

         // Another invalid entity set in the same compartment with the same
         // member/entity set. This will report two QA report lines, one for
         // each of the preceding entity sets in the same compartment with
         // the same member/entity sets.
         entitySet = new GKInstance(entitySetCls);
         entitySet.setDbAdaptor(dba);
         fixture.add(entitySet);
         entitySet.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         entitySet.addAttributeValue(ReactomeJavaConstants.hasMember, member);

        return fixture;
      }

}
