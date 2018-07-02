package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathwaylayout.Utils;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T047_OrphanEvents extends AbstractQACheck {

    private static final String ISSUE = "Event is orphaned";

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
        
        //The top-level events.
        Set<GKInstance> tles = new HashSet<GKInstance>(Utils.getTopLevelPathways(dba));
        // Check for events which are not referenced by another event.
        Collection<GKInstance> events = dba.fetchInstancesByClass(ReactomeJavaConstants.Event);
        String[] loadAtts = { ReactomeJavaConstants.hasEvent }; 
        dba.loadInstanceReverseAttributeValues(events, loadAtts);
        for (GKInstance event: events) {
            Collection<GKInstance> referers = event.getReferers(ReactomeJavaConstants.hasEvent);
            if ((referers == null || referers.isEmpty()) && !tles.contains(event)) {
                addReportLine(report, event);
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
