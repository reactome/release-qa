package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T002_PersonWithoutProperName extends AbstractQACheck {
    
    private final static String FETCH_ATTRIBUTES[] = {
            ReactomeJavaConstants.surname,
            ReactomeJavaConstants.firstname,
            ReactomeJavaConstants.initial
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
        Collection<GKInstance> invalid =
                (Collection<GKInstance>) dba.fetchInstancesByClass(ReactomeJavaConstants.Person);
        dba.loadInstanceAttributeValues(invalid, FETCH_ATTRIBUTES);
 
        for (GKInstance instance: invalid) {
            if (instance.getAttributeValue(ReactomeJavaConstants.surname) == null) {
                String issue = "Missing a " + ReactomeJavaConstants.surname;
                addReportLine(report, instance, issue);
            } else if (instance.getAttributeValue(ReactomeJavaConstants.firstname) == null &&
                    instance.getAttributeValue(ReactomeJavaConstants.initial) == null) {
                String issue = "Missing a " + ReactomeJavaConstants.firstname +
                        " and " + ReactomeJavaConstants.initial;
                addReportLine(report, instance, issue);
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