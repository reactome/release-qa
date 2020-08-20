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
import java.util.List;
import java.util.Set;

@SliceQACheck
public class CoV2EntityCheck extends AbstractQACheck {

    private static final long cov1SpeciesDbId = 9678119L;
    private static final long cov2InfectionPathwayDbId = 9694516L;

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        GKInstance cov2InfectionInst = dba.fetchInstance(cov2InfectionPathwayDbId);

        Collection<GKInstance> cov2Events = InstanceUtilities.getContainedEvents(cov2InfectionInst);
        cov2Events.add(cov2InfectionInst);

        for (GKInstance cov2Event : cov2Events) {
            Set<GKInstance> cov2Instances = new HashSet<>();
            cov2Instances.add(cov2Event);
            if (cov2Event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                cov2Instances.addAll(InstanceUtilities.getReactionParticipants(cov2Event));
            }

            for (GKInstance inst : cov2Instances) {
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
                if (speciesDbIds.contains(cov1SpeciesDbId)) {
                    report.addLine(getReportLine(cov2Event, inst, "CoV-1 species found in CoV-2 instance"));
                }

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
