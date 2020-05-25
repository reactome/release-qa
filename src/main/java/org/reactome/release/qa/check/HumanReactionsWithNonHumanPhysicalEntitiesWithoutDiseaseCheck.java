package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags all human Reactions that contain non-disease PhysicalEntities that are non-human, or are human PhysicalEntities with relatedSpecies.
 */

@SliceQATest
public class HumanReactionsWithNonHumanPhysicalEntitiesWithoutDiseaseCheck extends AbstractQACheck {

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        QACheckerHelper.setHumanSpeciesInst(dba);

        // This QA Check is only performed on human ReactionlikeEvents that do not have any inferredFrom referrals.
        for (GKInstance reaction : QACheckerHelper.findHumanReactionsNotUsedForManualInference(dba, EMPTY_SKIP_LIST)) {
            for (GKInstance reactionPE : QACheckerHelper.findAllPhysicalEntitiesInReaction(reaction)) {
                // Valid PhysicalEntities include those that have a non-human species OR have a human species AND have a relatedSpecies,
                // and that do not have a populated disease attribute.
                if ((QACheckerHelper.hasNonHumanSpecies(reactionPE) || hasHumanSpeciesWithRelatedSpecies(reactionPE))
                        && !QACheckerHelper.hasDisease(reactionPE)) {

                    report.addLine(getReportLine(reactionPE, reaction));
                }
            }
        }
        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    /**
     * Checks if incoming PhysicalEntity is human AND has populated relatedSpecies.
     * @param reactionPE GKInstance -- PhysicalEntity to be checked for Homo sapiens species and populated relatedSpecies.
     * @return boolean -- true if human PhysicalEntity and populated relatedSpecies, false if not.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    private boolean hasHumanSpeciesWithRelatedSpecies(GKInstance reactionPE) throws Exception {
        return QACheckerHelper.isHumanDatabaseObject(reactionPE)
                && hasRelatedSpecies(reactionPE);
    }

    /**
     * Checks if incoming PhysicalEntity has populated relatedSpecies attribute.
     * @param reactionPE GKInstance -- PhysicalEntity to be checked for relatedSpecies.
     * @return boolean -- true if relatedSpecies is populated, false if not.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    private boolean hasRelatedSpecies(GKInstance reactionPE) throws Exception {
        return reactionPE.getSchemClass().isValidAttribute(ReactomeJavaConstants.relatedSpecies)
                && reactionPE.getAttributeValue(ReactomeJavaConstants.relatedSpecies) != null;
    }

    private String getReportLine(GKInstance physicalEntity, GKInstance reaction) throws Exception {
        String speciesName = QACheckerHelper.getInstanceAttributeNameForOutputReport(physicalEntity, ReactomeJavaConstants.species);
        String createdName = QACheckerHelper.getInstanceAttributeNameForOutputReport(physicalEntity, ReactomeJavaConstants.created);
        return String.join("\t",
                reaction.getDBID().toString(),
                reaction.getDisplayName(),
                physicalEntity.getDBID().toString(),
                physicalEntity.getDisplayName(),
                physicalEntity.getSchemClass().getName(),
                speciesName,
                createdName);
    }

    @Override
    public String getDisplayName() {
        return "Infectious_Disease_RLEs_Containing_PhysicalEntities_Without_Disease";
    }

    private String[] getColumnHeaders() {
        return new String[] {"DB_ID_RlE", "DisplayName_RlE", "DB_ID_PE", "DisplayName_PE", "Class_PE", "Species", "Created"};
    }
}
