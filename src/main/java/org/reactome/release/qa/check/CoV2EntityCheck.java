package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This QA check was mostly used during the CoV-1-to-CoV-2 inference process. For now it is being kept, but may
 * be removed, given it was for a specific curations (August 2020).
 *
 * This checks for CoV-1 species/displayNames within all Events/PhysicalEntities contained within the 'SARS-CoV-2 Infection' Pathway.
 *
 * @author jcook
 */

@SliceQACheck
public class CoV2EntityCheck extends AbstractQACheck {

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        // Get all contained events within 'SARS-CoV-2 Infection' Pathway, including 'SARS-CoV-2 Infection'.
        GKInstance cov2InfectionInst = dba.fetchInstance(QACheckerHelper.getCoV2InfectionPathwayDbId());
        Collection<GKInstance> cov2Events = InstanceUtilities.getContainedEvents(cov2InfectionInst);
        cov2Events.add(cov2InfectionInst);

        for (GKInstance cov2Event : cov2Events) {
            Set<GKInstance> cov2Instances = new HashSet<>();
            cov2Instances.add(cov2Event);
            // Find all PhysicalEntities within Event
            if (cov2Event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                cov2Instances.addAll(InstanceUtilities.getReactionParticipants(cov2Event));
            }
            // Iterate through all Events/PhysicalEntities
            for (GKInstance inst : cov2Instances) {
                // Get all instances within 'species' and 'relatedSpecies' attributes
                Set<Long> speciesDbIds = new HashSet<>();
                if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
                    for (GKInstance speciesInst : (Collection<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.species)) {
                        speciesDbIds.add(speciesInst.getDBID());
                    }
                }
                if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.relatedSpecies)) {
                    for (GKInstance relatedSpeciesInst : (Collection<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.relatedSpecies)) {
                        speciesDbIds.add(relatedSpeciesInst.getDBID());
                    }
                }
                // Check if any species instance DbIds match the CoV-1 species DbId
                if (speciesDbIds.contains(QACheckerHelper.getCoV1SpeciesDbId())) {
                    report.addLine(getReportLine(cov2Event, inst, "CoV-1 species found in CoV-2 instance"));
                }
                // Check displayName as well
                if (inst.getDisplayName().contains("V-1")
                        || inst.getDisplayName().contains("v-1")
                        || inst.getDisplayName().contains("v1")
                        || inst.getDisplayName().contains("V1")) {
                    report.addLine(getReportLine(cov2Event, inst, "CoV-1 displayName found in CoV-2 instance"));
                }
            }
        }
        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    private String getReportLine(GKInstance cov2Event, GKInstance entity, String issue) {
        return String.join("\t",
                cov2Event.getDBID().toString(),
                cov2Event.getDisplayName(),
                QACheckerHelper.getLastModificationAuthor(cov2Event),
                entity.getDBID().toString(),
                entity.getDisplayName(),
                QACheckerHelper.getLastModificationAuthor(entity),
                issue
        );
    }

    private String[] getColumnHeaders() {
        return new String[]{"DB_ID_Event", "DisplayName_Event", "MostRecentAuthor_Event", "DB_ID_Entity", "DisplayName_Entity", "MostRecentAuthor_Entity", "Issue"};
    }

    @Override
    public String getDisplayName() {
        return "CoV-2_Entities_With_CoV-1_Species_Or_DisplayName";
    }
}
