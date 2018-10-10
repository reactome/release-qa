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

    private static final String HUMAN_ABBREVIATION = "HSA";
    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return "OrphanEvents";
    }

    @Override
    @SuppressWarnings("unchecked")
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        
        //The top-level events.
        Set<GKInstance> tles = getTopLevelPathways();
        // Check for events which are not referenced by another event.
        Collection<GKInstance> events = dba.fetchInstancesByClass(ReactomeJavaConstants.Event);
        String[] loadAtts = { ReactomeJavaConstants.hasEvent }; 
        dba.loadInstanceReverseAttributeValues(events, loadAtts);
        for (GKInstance event: events) {
            Collection<GKInstance> referers = event.getReferers(ReactomeJavaConstants.hasEvent);
            if ((referers == null || referers.isEmpty()) && !tles.contains(event)) {
                // Escape the special case
                if (shouldEscape(event))
                    continue;
                addReportLine(report, event);
            }
        }
        report.setColumnHeaders(HEADERS);

        return report;
    }
    
    /**
     * Returns whether the given event is a non-human reaction used to
     * infer human events.
     * 
     * @param event
     * @return whether the event should <em>not</em> be reported
     * @throws Exception
     */
    private boolean shouldEscape(GKInstance event) throws Exception {
        GKInstance species = (GKInstance) event.getAttributeValue(ReactomeJavaConstants.species);
        String abbrev = (String) species.getAttributeValue(ReactomeJavaConstants.abbreviation);
        if (!HUMAN_ABBREVIATION.equals(abbrev)) {
            @SuppressWarnings("unchecked")
            Collection<GKInstance> inferrals =
                    (Collection<GKInstance>) event.getReferers(ReactomeJavaConstants.inferredFrom);
            if (inferrals == null) {
                return false;
            }
            for (GKInstance other: inferrals) {
                GKInstance otherSpecies =
                        (GKInstance) other.getAttributeValue(ReactomeJavaConstants.species);
                String otherAbbrev =
                        (String) otherSpecies.getAttributeValue(ReactomeJavaConstants.abbreviation);
                if (HUMAN_ABBREVIATION.equals(otherAbbrev)) {
                    return true;
                }
            }
        }
        return false;
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
