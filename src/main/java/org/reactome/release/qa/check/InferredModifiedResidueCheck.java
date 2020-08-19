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
        String modifiedResidueCreatedName = QACheckerHelper.getInstanceAttributeNameForOutputReport(modifiedResidue, ReactomeJavaConstants.created);
        return String.join(
                modifiedResidue.getDBID().toString(),
                modifiedResidue.getDisplayName(),
                modifiedResidueCreatedName
        );
    }

    private String[] getColumnHeaders() {
        return new String[]{"DB_ID_MR", "DisplayName_MR", "Created_MR"};
    }

    @Override
    public String getDisplayName() {
        return "Inferred_Modified_Residues";
    }
}
