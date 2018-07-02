package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T017_NOT_FailedReactionsWithoutOutputs extends AbstractQACheck {

    private static final String ISSUE = "No output";

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        List<GKInstance> invalid = QACheckerHelper.getInstancesWithNullAttribute(dba,
                ReactomeJavaConstants.ReactionlikeEvent, ReactomeJavaConstants.output, null);
        // Filter out failed reactions.
        for (GKInstance instance: invalid) {
            if (!instance.getSchemClass().isa(ReactomeJavaConstants.FailedReaction)) {
                addReportLine(report, instance);
            }
        }
        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        ISSUE,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}
