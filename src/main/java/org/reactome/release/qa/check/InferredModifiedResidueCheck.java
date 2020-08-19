package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

import java.util.Collection;

@SliceQACheck
public class InferredModifiedResidueCheck extends AbstractQACheck {

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> modifiedResidues = dba.fetchInstancesByClass(ReactomeJavaConstants.AbstractModifiedResidue);
        for (GKInstance modifiedResidue : modifiedResidues) {
            if (modifiedResidue.getDisplayName().contains("[INFERRED]")) {
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
