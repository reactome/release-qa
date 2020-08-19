package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SliceQACheck
public class CoV2InfectionPathwayEventCheck extends AbstractQACheck {

    private static final long cov2InfectionPathwayDbId = 9694516L;

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        GKInstance cov2InfectionInst = dba.fetchInstance(cov2InfectionPathwayDbId);
        for (GKInstance cov2Event : InstanceUtilities.getContainedEvents(cov2InfectionInst)) {
            List<String> issues = new ArrayList<>();
            if (!hasRecentlyModifiedSummation(cov2Event)) {
                issues.add("Summation instance has not been recently modified");
            }
            if (cov2Event.getAttributeValue(ReactomeJavaConstants.inferredFrom) == null && !hasRecentLiteratureReference(cov2Event)) {
                issues.add("Does not contain a 2020 literature reference");
            }
            if (!hasCorrectSummationFormat(cov2Event)) {
                if (cov2Event.getAttributeValue(ReactomeJavaConstants.inferredFrom) == null) {
                    issues.add("InferredFrom is empty but instance has COVID inference summation text");
                } else {
                    issues.add("InferredFrom is populated but instance does NOT have COVID inference summation text.");
                }
            }
            if (issues.size() > 0) {
                report.addLine(getReportLine(cov2Event, String.join("|",issues)));
            }
        }
        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    private boolean hasRecentlyModifiedSummation(GKInstance cov2Event) throws Exception {
       for (GKInstance summation : (Collection<GKInstance>) cov2Event.getAttributeValuesList(ReactomeJavaConstants.summation)) {
            GKInstance createdInst = (GKInstance) summation.getAttributeValue(ReactomeJavaConstants.created);
            List<GKInstance> modifieds = summation.getAttributeValuesList(ReactomeJavaConstants.modified);
            if (modifieds.size() > 0) {
                // At time of writing (August 2020), nothing is reported from this clause, but it is a good check.
                GKInstance mostRecentModifiedInst = modifieds.get(modifieds.size() - 1);
                // Only check Year/Month/Day since the changes took place more than a day before Curators got to look at them in gk_central.
                int createdDateTime = Integer.valueOf(createdInst.getAttributeValue(ReactomeJavaConstants.dateTime).toString().split(" ")[0].replaceAll("-", ""));
                int modifiedDateTime = Integer.valueOf(mostRecentModifiedInst.getAttributeValue(ReactomeJavaConstants.dateTime).toString().split(" ")[0].replaceAll("-", ""));
                if (createdDateTime > modifiedDateTime) {
                    return false;
                }
            } else {
                return false;
            }
       }
        return true;
    }

    private boolean hasRecentLiteratureReference(GKInstance cov2Event) throws Exception {

        boolean has2020LiteratureReference = false;

        for (GKInstance literatureReference : (Collection<GKInstance>) cov2Event.getAttributeValuesList(ReactomeJavaConstants.literatureReference)) {
            if (literatureReference.getSchemClass().isa(ReactomeJavaConstants.LiteratureReference)) {
                if (literatureReference.getAttributeValue(ReactomeJavaConstants.year).toString().equals("2020")) {
                    has2020LiteratureReference = true;
                }
            } else if (literatureReference.getSchemClass().isa(ReactomeJavaConstants.URL)){
                // Only 10 URL-type LiteratureReferences currently exist in the Database for CoV-2 instances (August 2020).
                // All URLs are to pre-prints, and contain the string 'yyyy.mm.dd' in the URL.
                // Example: https://www.biorxiv.org/content/10.1101/2020.04.26.061705v1.full
                // This just simply checks for the existence of '2020.' in the URL string.
                String url = literatureReference.getAttributeValue(ReactomeJavaConstants.uniformResourceLocator).toString();
                if (url.contains("2020.")) {
                    has2020LiteratureReference = true;
                }
            }
        }
        return has2020LiteratureReference;
    }

    private boolean hasCorrectSummationFormat(GKInstance cov2Event) throws Exception {
        // Can't do full 'text' of the COVID inference message since displayNames get truncated.
        String inferredEventCovidText = "This COVID-19 " + cov2Event.getSchemClass().getName() + " instance was generated";
        for (GKInstance summation : (Collection<GKInstance>) cov2Event.getAttributeValuesList(ReactomeJavaConstants.summation)) {
            String displayName = summation.getDisplayName();
            String text = summation.getAttributeValue(ReactomeJavaConstants.text).toString();
            if (cov2Event.getAttributeValue(ReactomeJavaConstants.inferredFrom) != null) {
                //Inferred COVID summation should be there
                if (!displayName.contains(inferredEventCovidText) || !text.contains(inferredEventCovidText)) {
                    return false;
                }
            } else {
                // Inferred COVID summation should NOT be there
                if (displayName.contains(inferredEventCovidText) || text.contains(inferredEventCovidText)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getReportLine(GKInstance cov2Event, String issues) {
        return String.join("\t",
                cov2Event.getDBID().toString(),
                cov2Event.getDisplayName(),
                QACheckerHelper.getLastModificationAuthor(cov2Event),
                issues
        );
    }

    private String[] getColumnHeaders() {
        return new String[]{"DB_ID_Event", "DisplayName_Event", "MostRecentAuthor_Event", "Issue(s)"};
    }

    @Override
    public String getDisplayName() {
        return "CoV-2_Infection_Pathway_Events_With_Summation_And_Literature_Reference_Issues";
    }

}
