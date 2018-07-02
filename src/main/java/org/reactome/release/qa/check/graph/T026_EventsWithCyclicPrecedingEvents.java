package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T026_EventsWithCyclicPrecedingEvents extends AbstractQACheck {

    private final static String CHECK_ATTRIBUTES[] = {
            "hasEvent",
            "inferredFrom",
            "orthologousEvent"
    };
    
    private final static String FETCH_ATTRIBUTES[] = {
            "precedingEvent",
            "hasEvent",
            "inferredFrom",
            "orthologousEvent"
    };

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        for (Entry<GKInstance, String> entry: fetchInvalid().entrySet()) {
            GKInstance instance = entry.getKey();
            String issue = entry.getValue();
            report.addLine(
                    Arrays.asList(instance.getDBID().toString(), 
                            instance.getDisplayName(), 
                            instance.getSchemClass().getName(), 
                            issue,  
                            QACheckerHelper.getLastModificationAuthor(instance)));
        }
        report.setColumnHeaders(Arrays.asList("DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor"));

        return report;
    }
    
    @SuppressWarnings("unchecked")
    protected Map<GKInstance, String> fetchInvalid() throws Exception {
        Map<GKInstance, String> invalid = new HashMap<GKInstance, String>();
        Collection<GKInstance> events =  (Collection<GKInstance>) dba.fetchInstancesByClass(ReactomeJavaConstants.Event);
        dba.loadInstanceAttributeValues(events, FETCH_ATTRIBUTES);
 
        for (GKInstance event: events) {
            for (GKInstance preceding: (List<GKInstance>)event.getAttributeValuesList("precedingEvent")) {
                for (String checkAtt: CHECK_ATTRIBUTES) {
                    if (preceding.getSchemClass().isValidAttribute(checkAtt)) {
                        for (GKInstance ref: (List<GKInstance>)preceding.getAttributeValuesList(checkAtt)) {
                            if (ref.getDBID() == preceding.getDBID()) {
                                String issue = "Used in " + checkAtt + " slot by preceding event " +
                                        preceding.getDisplayName();
                                invalid.put(event, issue);
                            }
                        }
                    }
                }
            }
        }
        
        return invalid;
    }

}
