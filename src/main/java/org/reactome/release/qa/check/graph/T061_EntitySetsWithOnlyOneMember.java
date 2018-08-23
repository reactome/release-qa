package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T061_EntitySetsWithOnlyOneMember extends AbstractQACheck {

    private static final String ISSUE = "Has exactly one member";

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

        Collection<GKInstance> definedSets =
                dba.fetchInstancesByClass(ReactomeJavaConstants.DefinedSet);
        String[] loadAtts = { ReactomeJavaConstants.hasMember };
        dba.loadInstanceAttributeValues(definedSets, loadAtts);
        for (GKInstance definedSet: definedSets) {
            List<GKInstance> members =
                    definedSet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (members.size() == 1) {
                addReportLine(report, definedSet);
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
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
