package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T021_RegulationsWithoutRegulatedEntityOrRegulator extends AbstractQACheck {

    private static final String REGULATOR_ISSUE = "No regulator";

    private static final String REGULATED_BY_ISSUE = "No regulated entity";
    
    private static final String[] REG_LOAD_ATTS = {
            ReactomeJavaConstants.regulator
    };
    
    private static final String[] REG_REVERSE_LOAD_ATTS = {
            ReactomeJavaConstants.regulatedBy
    };

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> regulations =
                dba.fetchInstancesByClass(ReactomeJavaConstants.Regulation);
        dba.loadInstanceAttributeValues(regulations, REG_LOAD_ATTS);
        dba.loadInstanceReverseAttributeValues(regulations, REG_REVERSE_LOAD_ATTS);
        for (GKInstance regulation: regulations) {
            GKInstance regulator =
                    (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
            if (regulator == null) {
                addReportLine(report, regulation, REGULATOR_ISSUE);
            }
            // The regulations without a regulatedBy referral.
            Collection<GKInstance> referers =
                    regulation.getReferers(ReactomeJavaConstants.regulatedBy);
            if (referers == null || referers.isEmpty()) {
                addReportLine(report, regulation, REGULATED_BY_ISSUE);
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance, String issue) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        issue,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}
