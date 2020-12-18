package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * Flags all human Reactions that contain non-disease PhysicalEntities that are non-human, or are human PhysicalEntities with relatedSpecies.
 */


@SliceQACheck
public class HumanReactionsWithNonHumanPhysicalEntitiesWithoutDiseaseCheck extends AbstractQACheck {

    private static final long infectiousDiseasePathwayDbId = 5663205L;
    private static GKInstance infectiousDiseasePathway = null;

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        infectiousDiseasePathway = dba.fetchInstance(infectiousDiseasePathwayDbId);
        // This QA Check is only performed on human ReactionlikeEvents that do not have any inferredFrom referrals.
        for (GKInstance reaction : QACheckerHelper.findHumanReactionsNotUsedForManualInference(dba, EMPTY_SKIP_LIST)) {
            if (InstanceUtilities.isDescendentOf(reaction, infectiousDiseasePathway)) {
                for (GKInstance reactionPE : QACheckerHelper.getAllReactionParticipantsIncludingActiveUnits(reaction)) {
                    // Valid PhysicalEntities include those that have a non-human species OR have a human species AND have a relatedSpecies,
                    // and that do not have a populated disease attribute.
                    if ((QACheckerHelper.hasOnlyNonHumanSpecies(reactionPE) || hasHumanSpeciesWithRelatedSpecies(reactionPE))
                            && !QACheckerHelper.hasDisease(reactionPE)) {

                        report.addLine(getReportLine(reactionPE, reaction));
                    }
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
        String rleSpeciesName = QACheckerHelper.getInstanceAttributeNameForOutputReport(reaction, ReactomeJavaConstants.species);
        String rleRelatedSpeciesName = QACheckerHelper.getInstanceAttributeNameForOutputReport(reaction, ReactomeJavaConstants.relatedSpecies);
        String peSpeciesName = QACheckerHelper.getInstanceAttributeNameForOutputReport(physicalEntity, ReactomeJavaConstants.species);
        String peRelatedSpeciesName = QACheckerHelper.getInstanceAttributeNameForOutputReport(physicalEntity, ReactomeJavaConstants.relatedSpecies);
        String diseaseName = QACheckerHelper.getInstanceAttributeNameForOutputReport(reaction, ReactomeJavaConstants.disease);
        String createdName = QACheckerHelper.getInstanceAttributeNameForOutputReport(physicalEntity, ReactomeJavaConstants.created);
        return String.join("\t",
                reaction.getDBID().toString(),
                reaction.getDisplayName(),
                diseaseName,
                rleSpeciesName,
                rleRelatedSpeciesName,
                physicalEntity.getDBID().toString(),
                physicalEntity.getDisplayName(),
                physicalEntity.getSchemClass().getName(),
                peSpeciesName,
                peRelatedSpeciesName,
                createdName);
    }

    @Override
    public String getDisplayName() {
        return "Infectious_Disease_RLEs_Containing_PhysicalEntities_Without_Disease";
    }

    private String[] getColumnHeaders() {
        return new String[] {"DBID_RlE", "DisplayName_RlE", "Disease_RlE", "Species_RlE", "RelatedSpecies_RlE", "DBID_PE", "DisplayName_PE", "Class_PE", "Species_PE", "RelatedSpecies_RlE", "Created"};
    }
}