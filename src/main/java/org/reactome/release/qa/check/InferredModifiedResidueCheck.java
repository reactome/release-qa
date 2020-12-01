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
 *  Flags an ModifiedResidue instances that have the string '[INFERRED]' in their displayName.
 *
 * @author jcook
 */

@SliceQACheck
public class InferredModifiedResidueCheck extends AbstractQACheck {

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> modifiedResidues = dba.fetchInstancesByClass(ReactomeJavaConstants.AbstractModifiedResidue);
        for (GKInstance modifiedResidue : modifiedResidues) {
            if (modifiedResidue.getDisplayName() != null && modifiedResidue.getDisplayName().contains("[INFERRED]")) {
                report.addLine(getReportLine(modifiedResidue));
            }
        }
        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    private String getReportLine(GKInstance modifiedResidue) throws Exception {
        return String.join("\t",
                modifiedResidue.getDBID().toString(),
                modifiedResidue.getDisplayName(),
                QACheckerHelper.getLastModificationAuthor(modifiedResidue)
        );
    }

    private String[] getColumnHeaders() {
        return new String[]{"DB_ID_MR", "DisplayName_MR", "MostRecentAuthor_MR"};
    }

    @Override
    public String getDisplayName() {
        return "Inferred_Modified_Residues";
    }
}
