package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T029_ReactionsLikeEventWithoutInput extends AbstractQACheck {

    private static final String ISSUE = "No input";

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        List<GKInstance> rles = QACheckerHelper.getInstancesWithNullAttribute(dba,
                ReactomeJavaConstants.ReactionlikeEvent, ReactomeJavaConstants.input, null);
        SchemaClass rleCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.ReactionlikeEvent);
        // Inferred RLEs need not have input.
        SchemaAttribute inferredFromAtt =
                rleCls.getAttribute(ReactomeJavaConstants.inferredFrom);
        dba.loadInstanceAttributeValues(rles, inferredFromAtt);
        for (GKInstance rle: rles) {
            if (rle.getAttributeValue(ReactomeJavaConstants.inferredFrom) == null) {
                addReportLine(report, rle);
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
