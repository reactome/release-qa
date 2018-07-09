package org.reactome.release.qa.tests.graph;

import java.util.Collection;
import java.util.HashSet;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.graph.T059_DuplicatedCandidateSets;

public class T059_DuplicatedCandidateSetsTest extends QACheckReportComparisonTester {

    public T059_DuplicatedCandidateSetsTest() {
        super(new T059_DuplicatedCandidateSets(), 3);
    }

    @Override
    protected Collection<Instance> createTestFixture() throws Exception {
        Collection<Instance> fixture = new HashSet<Instance>();

        // A valid candidate set.
         SchemaClass csCls =
                 dba.getSchema().getClassByName(ReactomeJavaConstants.CandidateSet);
         GKInstance cs = new GKInstance(csCls);
         cs.setDbAdaptor(dba);
         fixture.add(cs);
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
         GKInstance candidate = new GKInstance(entityCls);
         candidate.setDbAdaptor(dba);
         fixture.add(candidate);
         cs.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         cs.addAttributeValue(ReactomeJavaConstants.hasMember, member);
         cs.addAttributeValue(ReactomeJavaConstants.hasCandidate, candidate);

         // A valid candidate set in the same compartment but without
         // the candidate.
         cs = new GKInstance(csCls);
         cs.setDbAdaptor(dba);
         fixture.add(cs);
         cs.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         cs.addAttributeValue(ReactomeJavaConstants.hasMember, member);

         // A valid candidate set in a different compartment.
         cs = new GKInstance(csCls);
         cs.setDbAdaptor(dba);
         fixture.add(cs);
         GKInstance otherCmpt = new GKInstance(cmptCls);
         otherCmpt.setDbAdaptor(dba);
         fixture.add(otherCmpt);
         cs.addAttributeValue(ReactomeJavaConstants.compartment, otherCmpt);
         cs.addAttributeValue(ReactomeJavaConstants.hasMember, member);
         cs.addAttributeValue(ReactomeJavaConstants.hasCandidate, candidate);

         // An invalid candidate set in the same compartment with the same
         // member/candidate set.
         cs = new GKInstance(csCls);
         cs.setDbAdaptor(dba);
         fixture.add(cs);
         cs.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         cs.addAttributeValue(ReactomeJavaConstants.hasMember, member);
         cs.addAttributeValue(ReactomeJavaConstants.hasCandidate, candidate);

         // Another invalid candidate set in the same compartment with the same
         // member/candidate set. This will report two QA report lines, one for
         // each of the preceding candidate sets in the same compartment with
         // the same member/candidate sets.
         cs = new GKInstance(csCls);
         cs.setDbAdaptor(dba);
         fixture.add(cs);
         cs.addAttributeValue(ReactomeJavaConstants.compartment, cmpt);
         cs.addAttributeValue(ReactomeJavaConstants.hasMember, member);
         cs.addAttributeValue(ReactomeJavaConstants.hasMember, candidate);

        return fixture;
      }

}
