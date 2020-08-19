package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

import java.util.Collection;

@SliceQACheck
public class InferredEvidenceTypeCheck extends AbstractQACheck {
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> events = dba.fetchInstancesByClass(ReactomeJavaConstants.Event);
        for (GKInstance event : events) {
            GKInstance evidenceType = (GKInstance) event.getAttributeValue(ReactomeJavaConstants.evidenceType);
            if (evidenceType != null && evidenceType.getDisplayName().contains("Inferred from Electronic Annotation")) {
                report.addLine(getReportLine(event, evidenceType));
            }

        }
        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    private String getReportLine(GKInstance event, GKInstance evidenceType) {
        return String.join("\t",
                event.getDBID().toString(),
                event.getDisplayName(),
                evidenceType.getDisplayName(),
                QACheckerHelper.getLastModificationAuthor(event)
        );
    }

    private String[] getColumnHeaders() {
        return new String[]{"DB_ID_Event", "DisplayName_Event", "DisplayName_ET", "MostRecentAuthor_Event"};
    }

    @Override
    public String getDisplayName() {
        return "Events_With_Electronic_Evidence_Types";
    }
}
