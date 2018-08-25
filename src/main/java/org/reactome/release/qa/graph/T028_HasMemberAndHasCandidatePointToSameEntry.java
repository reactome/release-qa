package org.reactome.release.qa.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T028_HasMemberAndHasCandidatePointToSameEntry extends AbstractQACheck {
    
    private static String ISSUE = "Candidate is also a member: ";
    
    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> candidateSets =
                dba.fetchInstancesByClass(ReactomeJavaConstants.CandidateSet);
        String[] attNames = {
                ReactomeJavaConstants.hasMember,
                ReactomeJavaConstants.hasCandidate
        };
        dba.loadInstanceAttributeValues(candidateSets, attNames);
        for (GKInstance candidateSet: candidateSets) {
            List<GKInstance> candidates =
                    candidateSet.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
            Set<Long> candidateDbIds = candidates.stream()
                    .map(inst -> inst.getDBID())
                    .collect(Collectors.toSet());
            List<GKInstance> members =
                    candidateSet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            for (GKInstance member: members) {
                if (candidateDbIds.contains(member.getDBID())) {
                    addReportLine(report, candidateSet, member);
                }
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance candidateSet, GKInstance duplicate) {
        String issue = ISSUE + duplicate.getDisplayName() + "(DBID " + duplicate.getDBID() + ")";
        report.addLine(
                Arrays.asList(candidateSet.getDBID().toString(), 
                        candidateSet.getDisplayName(), 
                        candidateSet.getSchemClass().getName(), 
                        issue,  
                        QACheckerHelper.getLastModificationAuthor(candidateSet)));
    }

}
