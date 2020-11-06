package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

import java.util.Collection;

/**
 *  This QA check was mostly used during the CoV-1-to-CoV-2 inference process. For now it is being kept, but may
 *  be removed, given it was for a specific curations (August 2020).
 *
 *  Since CoV-1-to-CoV-2 inference involved using a modified version of orthoinference, this check was created to find
 *  instances that had a 'Inferred from Electronic Annotation' EvidenceType, so Curators could prioritize their work.
 *
 *  @author jcook
 */
@SliceQACheck
public class InferredEvidenceTypeCheck extends AbstractQACheck {

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> events = dba.fetchInstancesByClass(ReactomeJavaConstants.Event);
        for (GKInstance event : events) {
            // Orthoinference adds a typical EvidenceType instance to projected instances that have
            // a displayName of 'Inferred from Electronic Annotation'.
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
