package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.graph.T061_EntitySetsWithOnlyOneMember;

public class T061_EntitySetsWithOnlyOneMemberTest extends QACheckReportComparisonTester {

    public T061_EntitySetsWithOnlyOneMemberTest() {
        super(new T061_EntitySetsWithOnlyOneMember(), 1);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();

        // A valid defined set with two members.
         SchemaClass dsCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.DefinedSet);
         GKInstance ds = new GKInstance(dsCls);
         ds.setDbAdaptor(dba);
         fixture.add(ds);
         SchemaClass entityCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.SimpleEntity);
         GKInstance member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         ds.addAttributeValue(ReactomeJavaConstants.hasMember, member);
         member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         ds.addAttributeValue(ReactomeJavaConstants.hasMember, member);

         // An invalid defined set with one member.
         ds = new GKInstance(dsCls);
         ds.setDbAdaptor(dba);
         fixture.add(ds);
         member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         ds.addAttributeValue(ReactomeJavaConstants.hasMember, member);

         // A valid candidate set with one member.
         SchemaClass csCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.CandidateSet);
         GKInstance cs = new GKInstance(csCls);
         cs.setDbAdaptor(dba);
         fixture.add(cs);
         member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         cs.addAttributeValue(ReactomeJavaConstants.hasMember, member);

         // A valid open set with one member.
         SchemaClass osCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.OpenSet);
         GKInstance os = new GKInstance(osCls);
         os.setDbAdaptor(dba);
         fixture.add(os);
         member = new GKInstance(entityCls);
         member.setDbAdaptor(dba);
         fixture.add(member);
         os.addAttributeValue(ReactomeJavaConstants.hasMember, member);

        return fixture;
      }

}
