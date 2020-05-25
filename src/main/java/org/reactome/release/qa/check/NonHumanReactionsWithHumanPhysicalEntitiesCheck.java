package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

import java.util.*;

/**
 * Flags all human PhysicalEntities that are participants in a non-human ReactionlikeEvent.
 */

@SliceQATest
public class NonHumanReactionsWithHumanPhysicalEntitiesCheck extends AbstractQACheck {

    private static List<String> skiplistDbIds = new ArrayList<>();

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        QACheckerHelper.setHumanSpeciesInst(dba);
        skiplistDbIds = QACheckerHelper.getNonHumanPathwaySkipList();

        Collection<GKInstance> reactions = dba.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        for (GKInstance reaction : reactions) {
            // Many Events have multiple species. Cases where there are multiple species and one of them is Human are also excluded.
            if (QACheckerHelper.hasNonHumanSpecies(reaction) && !QACheckerHelper.memberSkipListPathway(reaction, skiplistDbIds)) {
                for (GKInstance humanPE : findAllHumanPhysicalEntitiesInReaction(reaction)) {
                    report.addLine(getReportLine(humanPE, reaction));
                }
            }
        }
        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    /**
     * Finds all distinct human PhysicalEntities that exist in the incoming ReactionlikeEvent.
     * @param reaction GKInstance -- ReactionlikeEvent with non-human species.
     * @return Set<GKInstance> -- Any human PhysicalEntities that exist in the ReactionlikeEvent.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    private Set<GKInstance> findAllHumanPhysicalEntitiesInReaction(GKInstance reaction) throws Exception {
        Set<GKInstance> humanPEs = new HashSet<>();
        for (GKInstance physicalEntity: QACheckerHelper.findAllPhysicalEntitiesInReaction(reaction)) {
            if (QACheckerHelper.isHumanDatabaseObject(physicalEntity)) {
                humanPEs.add(physicalEntity);
            }
        }
        return humanPEs;
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
        return "NonHuman_Reactions_Containing_Human_PhysicalEntities";
    }

    private String[] getColumnHeaders() {
        return new String[] {"DB_ID_RlE", "DisplayName_RlE", "DB_ID_PE", "DisplayName_PE", "Class_PE", "Species_PE", "Created_PE"};
    }
}
