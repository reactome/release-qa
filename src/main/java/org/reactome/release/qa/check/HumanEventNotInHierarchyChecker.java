package org.reactome.release.qa.check;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.common.QAReport;

/**
 * Check if a human event is in the pathway hierrachy built based on the sole FontPageItem instance.
 * @author wug
 */
@SuppressWarnings("unchecked")
public class HumanEventNotInHierarchyChecker extends AbstractQACheck {
    
    public HumanEventNotInHierarchyChecker() {
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        GKInstance human = QACheckerHelper.getHuman(dba);
        Collection<GKInstance> events = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Event,
                                                                     ReactomeJavaConstants.species,
                                                                     "=",
                                                                     human);
        dba.loadInstanceAttributeValues(events, new String[]{ReactomeJavaConstants.hasEvent});
        // Get the top level events
        List<GKInstance> frontPageItems = getFrontPageItems();
        Set<GKInstance> itemsInTree = InstanceUtilities.getContainedEvents(frontPageItems);
        itemsInTree.addAll(frontPageItems);
        events.removeAll(itemsInTree);
        QAReport report = new QAReport();
        if (events.size() == 0)
            return report; // Return an empty report
        for (GKInstance event : events) {
            // A chimeric is used for inference, which doesn't need to be in the hierachy
            if (QACheckerHelper.isChimeric(event))
                continue;
            report.addLine(event.getDBID().toString(),
                           event.getDisplayName(),
                           QACheckerHelper.getLastModificationAuthor(event));
        }
        report.setColumnHeaders("DB_ID", "DisplayName", "LastAuthor");
        return report;
    }
    
    private List<GKInstance> getFrontPageItems() throws Exception {
        Collection<GKInstance> c = dba.fetchInstancesByClass(ReactomeJavaConstants.FrontPage);
        GKInstance frontPage = c.iterator().next();
        if (frontPage == null)
            throw new IllegalStateException("Cannot find FrontPage instance in " + dba.getDBName() + "@" + dba.getDBHost());
        return frontPage.getAttributeValuesList(ReactomeJavaConstants.frontPageItem);
    }

    @Override
    public String getDisplayName() {
        return "Human_Event_Not_In_Hierarchy";
    }
    
    

}
