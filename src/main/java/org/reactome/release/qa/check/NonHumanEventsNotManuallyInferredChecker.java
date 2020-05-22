package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 *  Finds all non-human Events that are not used for manual inference (ie. inferredFrom referral is null)
 */

@SliceQATest
public class NonHumanEventsNotManuallyInferredChecker extends AbstractQACheck {

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        QACheckerHelper.setHumanSpeciesInst(dba);
        QACheckerHelper.setSkipList(QACheckerHelper.getNonHumanPathwaySkipList());

        // The actual method for finding Events that aren't manually inferred is used by multiple QA tests.
        for (GKInstance event : QACheckerHelper.findEventsNotUsedForManualInference(dba)) {
            // Many Events have multiple species. Cases where there are multiple species and one of them is human are also excluded.
            if (QACheckerHelper.hasNonHumanSpecies(event)) {
                report.addLine(getReportLine(event));
            }
        }
        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    private String getReportLine(GKInstance event) throws Exception {
        String speciesName = QACheckerHelper.getInstanceAttributeName(event, ReactomeJavaConstants.species);
        String createdName = QACheckerHelper.getInstanceAttributeName(event, ReactomeJavaConstants.created);
        return String.join("\t",
                event.getDBID().toString(),
                event.getDisplayName(),
                speciesName,
                createdName);
    }

    private String[] getColumnHeaders() {
        return new String[] {"DB_ID", "DisplayName", "Species", "Created"};
    }

    @Override
    public String getDisplayName() {
        return "NonHuman_Events_Not_Manually_Inferred";
    }
}
