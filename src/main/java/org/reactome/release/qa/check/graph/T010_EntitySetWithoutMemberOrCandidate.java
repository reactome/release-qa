package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;
import org.gk.schema.SchemaClass;

public class T010_EntitySetWithoutMemberOrCandidate extends AbstractQACheck {

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        List<GKInstance> withoutMember = QACheckerHelper.getInstancesWithNullAttribute(dba,
                ReactomeJavaConstants.EntitySet, ReactomeJavaConstants.hasMember, null);
        List<GKInstance> withoutCandidate = QACheckerHelper.getInstancesWithNullAttribute(dba,
                ReactomeJavaConstants.CandidateSet, ReactomeJavaConstants.hasCandidate, null);
        Set<Long> withoutCandidateDbIds = withoutCandidate.stream()
                .map(inst -> inst.getDBID())
                .collect(Collectors.toSet());
        SchemaClass candidateSetCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.CandidateSet);
        for (GKInstance instance: withoutMember) {
            if (instance.getSchemClass() == candidateSetCls) {
                if (withoutCandidateDbIds.contains(instance.getDBID())) {
                    addReportLine(report, instance, "Missing both members and candidates");
                }
            } else {
                addReportLine(report, instance, "Missing members");
            }
        }
        
        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance, String issue) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        issue,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}
