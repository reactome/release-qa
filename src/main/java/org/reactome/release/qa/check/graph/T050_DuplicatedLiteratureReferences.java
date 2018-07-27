package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T050_DuplicatedLiteratureReferences extends AbstractQACheck {

    private static final String ISSUE = "Same PubMed identifier as ";

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    @SuppressWarnings("unchecked")
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        
        Collection<GKInstance> litRefs =
                dba.fetchInstancesByClass(ReactomeJavaConstants.LiteratureReference);
        String[] loadAtts = { ReactomeJavaConstants.pubMedIdentifier };
        dba.loadInstanceAttributeValues(litRefs, loadAtts);
        Map<Integer, Instance> pubMedRefs = new HashMap<Integer, Instance>(litRefs.size());
        for (GKInstance litRef: litRefs) {
            Integer pubMedId =
                    (Integer) litRef.getAttributeValue(ReactomeJavaConstants.pubMedIdentifier);
            Instance other = pubMedRefs.get(pubMedId);
            if (other == null) {
                pubMedRefs.put(pubMedId, litRef);
            } else {
                addReportLine(report, litRef, other);
            }
        }
        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance, Instance other) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        ISSUE + other.getDisplayName() + " (DBID " + other.getDBID() + ")",  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}
