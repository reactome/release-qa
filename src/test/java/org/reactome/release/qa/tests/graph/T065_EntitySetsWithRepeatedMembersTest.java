package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T065_EntitySetsWithRepeatedMembers;

public class T065_EntitySetsWithRepeatedMembersTest extends QACheckReportComparisonTester {

    public T065_EntitySetsWithRepeatedMembersTest() {
        super(new T065_EntitySetsWithRepeatedMembers(), 1);
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

         // An invalid entity set with a repeated member.
         entitySet = new GKInstance(entitySetCls);
         entitySet.setDbAdaptor(dba);
         fixture.add(entitySet);
         entitySet.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         entitySet.addAttributeValue(ReactomeJavaConstants.hasMember, member);
         GKInstance repeated = new GKInstance(entityCls);
         repeated.setDbAdaptor(dba);
         fixture.add(repeated);
         entitySet.addAttributeValue(ReactomeJavaConstants.hasMember, repeated);
         entitySet.addAttributeValue(ReactomeJavaConstants.hasMember, repeated);

        return fixture;
      }

}
