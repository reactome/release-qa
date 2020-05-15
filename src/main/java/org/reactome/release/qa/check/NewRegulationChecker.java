package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.annotations.SliceQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 *  This QA check finds any ReactionlikeEvents that have received a new, unreviewed Regulation instance (via 'regulatedBy' attribute).
 * @author jcook
 */

import java.util.*;

import static java.util.stream.Collectors.toSet;

@SliceQATest
public class NewRegulationChecker extends AbstractQACheck implements ChecksTwoDatabases
{

    private MySQLAdaptor priorAdaptor;

    @Override
    public String getDisplayName()
    {
        // Output report file name.
        return "ReactionlikeEvent_Regulations_Not_Reviewed";
    }

    @Override
    public void setOtherDBAdaptor(MySQLAdaptor adaptor)
    {
        this.priorAdaptor = adaptor;
    }

    @Override
    public QAReport executeQACheck() throws Exception
    {
        QAReport report = new QAReport();
        report.setColumnHeaders("DBID","DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");
        // Find all ReactionlikeEvents that have a filled 'regulatedBy' attribute in current slice.
        Set<GKInstance> currentRlEsWithRegulations = getRlEsWithRegulationInstances(dba);

        for (GKInstance currentRlE : currentRlEsWithRegulations) {
            GKInstance previousRlE = priorAdaptor.fetchInstance(currentRlE.getDBID());
            // QA check
            if (changedRegulatedByWithoutNewReviewed(currentRlE, previousRlE)){
                report.addLine(
                        currentRlE.getDBID().toString(),
                        currentRlE.getDisplayName(),
                        currentRlE.getSchemClass().getName(),
                        "ReactionlikeEvent with new regulatedBy instance but has not yet been reviewed",
                        QACheckerHelper.getLastModificationAuthor(currentRlE)
                );
            }
        }
        return report;
    }

    /**
     * This method checks to see if a new regulation instance has been added to the ReactionlikeEvent by comparing between slices.
     * If it does, it then compares the number of reviewed instances between slices. If the number of regulation instances is greater AND
     * the number of reviewed instances is the same, it is flagged to indicate that this ReactionlikeEvent still needs to reviewed before
     * it can be released.
     * @param currentRlE GKInstance -- ReactionlikeEvent from current slice.
     * @param previousRlE GKInstance -- Same ReactionlikeEvent, but from previous slice.
     * @return boolean -- True if a new, unreviewed regulation instance has been added.
     * @throws Exception -- DBA exceptions.
     */
    private boolean changedRegulatedByWithoutNewReviewed(GKInstance currentRlE, GKInstance previousRlE) throws Exception {

        // If previousRlE does not exist, we ignore it since that does not pertain to this particular QA check.
        if (previousRlE == null) {
            return false;
        } else {
            // Compare contents of the 'regulatedBy' attribute of the current and previous versions of the RlE.
            boolean sameRegulatedBy = isSameRegulatedByValues(
                    currentRlE.getAttributeValuesList(ReactomeJavaConstants.regulatedBy),
                    previousRlE.getAttributeValuesList(ReactomeJavaConstants.regulatedBy)
            );

            int currentRlEReviewed = currentRlE.getAttributeValuesList(ReactomeJavaConstants.reviewed).size();
            int previousRlEReviewed = previousRlE.getAttributeValuesList(ReactomeJavaConstants.reviewed).size();

            // The actual QA check
            return !sameRegulatedBy && currentRlEReviewed == previousRlEReviewed;
        }
    }

    /**
     * Adapted from private InstanceUtilities.compareAttributes method. Takes the two 'regulatedBy' lists from the
     * current and previous versions of a ReactionlikeEvent, and compares their contents, returning false if they differ.
     * @param regulatedByInstancesCurrent List<GKInstance> -- Contents of currentRlE's 'regulatedBy' attribute
     * @param regulatedByInstancesPrevious List<GKInstance> -- Contents of previousRlE's 'regulatedBy' attribute
     * @return boolean -- true if lists are equal, false if not.
     */
    private boolean isSameRegulatedByValues(List<GKInstance> regulatedByInstancesCurrent, List<GKInstance> regulatedByInstancesPrevious) {
        List<GKInstance> list1Copy = new ArrayList(regulatedByInstancesCurrent);
        List<GKInstance> list2Copy = new ArrayList(regulatedByInstancesPrevious);
        GKInstance instance1;
        GKInstance instance2;
        for (Iterator it = list1Copy.iterator(); it.hasNext();) {
            instance1 = (GKInstance) it.next();
            for (Iterator it1 = list2Copy.iterator(); it1.hasNext();) {
                instance2 = (GKInstance) it1.next();
                if (instance2.getDBID().equals(instance1.getDBID())) {
                    it1.remove();
                    it.remove();
                    break;
                }
            }
        }
        return equivalentLists(list1Copy, list2Copy);
    }

    // Even if Lists are the same size, it could mask unequal contents.
    // This checks that the Lists and their contents are in fact equal.
    private boolean equivalentLists(List<GKInstance> list1, List<GKInstance> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        Set<Long> list1DbIds = getInstanceListDBIDs(list1);
        Set<Long> list2DbIds = getInstanceListDBIDs(list2);
        return list1DbIds.equals(list2DbIds);
    }

    // Return all unique DBIDs from instances in the list
    private Set<Long> getInstanceListDBIDs(List<GKInstance> list) {
        return list.stream().map(GKInstance::getDBID).collect(toSet());
    }

    private Set<GKInstance> getRlEsWithRegulationInstances(MySQLAdaptor dba) throws Exception {
       return (HashSet<GKInstance>) dba.fetchInstanceByAttribute(
               ReactomeJavaConstants.ReactionlikeEvent,
               ReactomeJavaConstants.regulatedBy,
               "IS NOT NULL",
               "null");
    }
}
