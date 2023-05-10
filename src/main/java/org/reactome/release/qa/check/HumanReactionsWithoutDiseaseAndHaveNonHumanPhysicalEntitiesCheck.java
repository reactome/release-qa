package org.reactome.release.qa.check;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;
import org.reactome.release.qa.common.SkipList;

import java.util.*;

/**
 * Flags human ReactionlikeEvents that do not have a populated disease attribute, but have non-human participants.
 */

@SliceQACheck
public class HumanReactionsWithoutDiseaseAndHaveNonHumanPhysicalEntitiesCheck extends AbstractQACheck {

    private static final Logger logger = LogManager.getLogger();
    private SkipList skipList;

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        try {
            skipList = new SkipList(this.getDisplayName());
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }

        Collection<GKInstance> reactions = dba.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        for (GKInstance reaction : reactions) {
            // isHumanDatabaseObject checks that the species attribute only contains a Homo sapiens species instance. Multi-species RlEs are excluded.
            if (!QACheckerHelper.memberSkipListPathway(reaction, skipList.getSkipListDbIds())
                    && QACheckerHelper.isHumanDatabaseObject(reaction)
                    && !QACheckerHelper.hasDisease(reaction)) {

                for (GKInstance nonHumanPE : findAllNonHumanPhysicalEntitiesInReaction(reaction)) {
                    report.addLine(getReportLine(nonHumanPE, reaction));
                }
            }
        }
        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    /**
     * Finds all distinct non-human PhysicalEntities that exist in the incoming human ReactionlikeEvent.
     * @param reaction GKInstance -- ReactionlikeEvent with Homo sapiens species.
     * @return Set<GKInstance> -- Any non-human PhysicalEntities that exist in the human ReactionlikeEvent.
     * @throws Exception -- Thrown by MySQLAdaptor.
     */
    private Set<GKInstance> findAllNonHumanPhysicalEntitiesInReaction(GKInstance reaction) throws Exception {
        Set<GKInstance> nonHumanPEs = new HashSet<>();
        for (GKInstance physicalEntity : QACheckerHelper.getAllReactionParticipantsIncludingActiveUnits(reaction)) {
            if (QACheckerHelper.hasOnlyNonHumanSpecies(physicalEntity)) {
                nonHumanPEs.add(physicalEntity);
            }
        }
        return nonHumanPEs;
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
        return "Human_Reactions_Without_Disease_And_Have_NonHuman_PhysicalEntities";
    }

    private String[] getColumnHeaders() {
        return new String[] {"DB_ID_RlE", "DisplayName_RlE", "DB_ID_PE", "DisplayName_PE", "Class_PE", "Species_PE", "Created_PE"};
    }
}