package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T091_ReactionsWithoutLiteratureReference
extends AbstractQACheck {

    private static final String ISSUE = "No literature reference";

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    private static final String[] RLE_LOAD_ATTS = {
            ReactomeJavaConstants.summation,
            ReactomeJavaConstants.literatureReference
    };

    private static final String[] SUMMATION_LOAD_ATTS = {
            ReactomeJavaConstants.literatureReference
    };

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> rles =
                QACheckerHelper.getInstancesWithNullAttribute(dba,
                        ReactomeJavaConstants.ReactionlikeEvent,
                        ReactomeJavaConstants.inferredFrom, null);
        dba.loadInstanceAttributeValues(rles, RLE_LOAD_ATTS);
        for (GKInstance rle: rles) {
            if (!isValid(rle)) {
                addReportLine(report, rle);
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }

    @SuppressWarnings("unchecked")
    private boolean isValid(GKInstance rle) throws Exception {
        List<GKInstance> litRefs =
                rle.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
        if (!litRefs.isEmpty()) {
            return true;
        }
        List<GKInstance> summations =
                rle.getAttributeValuesList(ReactomeJavaConstants.summation);
        dba.loadInstanceAttributeValues(summations, SUMMATION_LOAD_ATTS);
        for (GKInstance summation: summations) {
            List<GKInstance> summationLitRefs =
                    summation.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
            if (!summationLitRefs.isEmpty()) {
                return true;
            }
        }
        
        return false;
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
