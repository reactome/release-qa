package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T024_ReferenceDatabaseWithoutUrls extends AbstractQACheck {

    private static final String ISSUE = "Missing accessUrl or url";

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

        SchemaClass refDbCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDatabase);
        Collection<GKInstance> refDbs = dba.fetchInstancesByClass(refDbCls);
        String[] loadAtts = {
                ReactomeJavaConstants.accessUrl,
                ReactomeJavaConstants.url
        };
        dba.loadInstanceAttributeValues(refDbs, loadAtts);

        for (GKInstance refDb: refDbs) {
            if (refDb.getAttributeValue(ReactomeJavaConstants.accessUrl) == null ||
                    refDb.getAttributeValue(ReactomeJavaConstants.url) == null) {
                addReportLine(report, refDb);
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
