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
        }
        // Compare contents of the 'regulatedBy' attribute of the current and previous versions of the RlE.
        boolean sameRegulatedByAttrs = hasEquivalentAttributeValues(
                currentRlE.getAttributeValuesList(ReactomeJavaConstants.regulatedBy),
                previousRlE.getAttributeValuesList(ReactomeJavaConstants.regulatedBy)
        );

        boolean sameReviewedAttrs = hasEquivalentAttributeValues(
                currentRlE.getAttributeValuesList(ReactomeJavaConstants.reviewed),
                previousRlE.getAttributeValuesList(ReactomeJavaConstants.reviewed)
        );

        // The actual QA check
        return !sameRegulatedByAttrs && sameReviewedAttrs;
    }

    /**
     * Takes two lists from the same attribute in current and previous versions of a ReactionlikeEvent and
     * compares their contents, returning false if they differ.
     * @param attributeValuesCurrent List<GKInstance> -- Contents of currentRlE's attribute
     * @param attributeValuesPrevious List<GKInstance> -- Contents of previousRlE's attribute
     * @return boolean -- true if lists are equal, false if not.
     */
    private boolean hasEquivalentAttributeValues(List<GKInstance> attributeValuesCurrent, List<GKInstance> attributeValuesPrevious) {
        if (attributeValuesCurrent.size() != attributeValuesPrevious.size()) {
            return false;
        }
        Set<Long> list1DbIds = getInstanceListDBIDs(attributeValuesCurrent);
        Set<Long> list2DbIds = getInstanceListDBIDs(attributeValuesPrevious);
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
               null);
    }
}
