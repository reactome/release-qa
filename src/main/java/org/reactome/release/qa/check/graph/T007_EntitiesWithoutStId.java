package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * Checks for physical entities and events without a stable id.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
public class T007_EntitiesWithoutStId extends AbstractQACheck {
   
    private static final String ISSUE = "No stable id";

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    private static final String[] CLASS_NAMES = {
            ReactomeJavaConstants.PhysicalEntity,
            ReactomeJavaConstants.Event
    };
    
    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        for (String clsName: CLASS_NAMES) {
            List<GKInstance> invalid = QACheckerHelper.getInstancesWithNullAttribute(dba,
                    clsName, ReactomeJavaConstants.stableIdentifier, null);
    
            for (GKInstance instance: invalid) {
                addReportLine(report, instance);
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
