package org.reactome.release.qa.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.GraphQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

@GraphQATest
public class OrphanEvents extends AbstractQACheck {

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return "Orphan_Events";
    }

    @Override
    @SuppressWarnings("unchecked")
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        
        //The top-level events.
        Set<GKInstance> tlps = getTopLevelPathways();
        // Get the human events.
        GKInstance human = QACheckerHelper.getHuman(dba);
        Collection<GKInstance> events = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Event,
                        ReactomeJavaConstants.species,
                        "=",
                        human);
        String[] loadAtts = { ReactomeJavaConstants.hasEvent }; 
        dba.loadInstanceReverseAttributeValues(events, loadAtts);
        // Check for events which are not referenced by another event.
        for (GKInstance event: events) {
            if (isEscaped(event)) {
                continue;
            }
            // A chimeric is used for inference, which doesn't need to be in the hierachy.
            if (QACheckerHelper.isChimeric(event))
                continue;
            Collection<GKInstance> referers = event.getReferers(ReactomeJavaConstants.hasEvent);
            if ((referers == null || referers.isEmpty()) && !tlps.contains(event)) {
                addReportLine(report, event);
            }
        }
        report.setColumnHeaders(HEADERS);

        return report;
    }
    
    @SuppressWarnings("unchecked")
    private Set<GKInstance> getTopLevelPathways() throws Exception {
        Set<GKInstance> topLevelPathways = new HashSet<>();
        // There should be only one frontPage instance
        Collection<GKInstance> frontPages = dba.fetchInstancesByClass(ReactomeJavaConstants.FrontPage);
        for (GKInstance frontPage : frontPages) {
            List<GKInstance> frontPageItems = frontPage.getAttributeValuesList(ReactomeJavaConstants.frontPageItem);
            if (frontPageItems != null)
                topLevelPathways.addAll(frontPageItems); 
        }
        return topLevelPathways;
    }

    private void addReportLine(QAReport report, GKInstance instance) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}
