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
        SchemaClass regCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Regulation);
        SchemaAttribute regAtt =
                regCls.getAttribute(ReactomeJavaConstants.regulator);
        SchemaClass rleCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.ReactionlikeEvent);
        SchemaAttribute regByAtt =
                rleCls.getAttribute(ReactomeJavaConstants.regulatedBy);
        dba.loadInstanceAttributeValues(regulations, regAtt);
        dba.loadInstanceReverseAttributeValues(regulations, regByAtt);
        for (GKInstance regulation: regulations) {
            // The regulations without a regulator.
            // Note: we cannot call getAttributeValue(regAtt) since
            // regAtt is the Regulation.regulator attribute whereas
            // the regulation instance class is a subclass of Regulation.
            // The SchemaClass creates a separate SchemaAttribute instance
            // for an inherited attribute and therefore does not recognize
            // the superclass SchemaAttribute as a valid attribute. 
            if (regulation.getAttributeValue(regAtt.getName()) == null) {
                addReportLine(report, regulation, REGULATOR_ISSUE);
            }
            // The regulations without a regulatedBy referral.
            Collection<GKInstance> referers =
                    regulation.getReferers(regByAtt.getName());
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
