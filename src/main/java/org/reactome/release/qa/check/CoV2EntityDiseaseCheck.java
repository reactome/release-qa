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

@SliceQACheck
public class CoV2EntityDiseaseCheck extends AbstractQACheck {

    private static final long cov2InfectionPathwayDbId = 9694516L;
    private static final long cov2DiseaseDbId = 9683912L;
    private static final long cov2SpeciesDbId = 9681683L;

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        GKInstance cov2InfectionInst = dba.fetchInstance(cov2InfectionPathwayDbId);

        Collection<GKInstance> cov2Events = InstanceUtilities.getContainedEvents(cov2InfectionInst);
        cov2Events.add(cov2InfectionInst);

        Set<GKInstance> cov2InstancesWithoutCov2Disease = new HashSet<>();
        for (GKInstance cov2Event : cov2Events) {
            Set<GKInstance> cov2Instances = new HashSet<>();
            cov2Instances.add(cov2Event);
            if (cov2Event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                cov2Instances.addAll(InstanceUtilities.getReactionParticipants(cov2Event));
            }
//            System.out.println(cov2Event.getAttributeValuesList(ReactomeJavaConstants.disease) + "\t" + cov2Event);
            for (GKInstance inst : cov2Instances) {
                Set<Long> speciesDbIds = new HashSet<>();
                if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
                    for (GKInstance speciesInst : (Collection<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.species)) {
                        speciesDbIds.add(speciesInst.getDBID());
                    }
                }
                if (speciesDbIds.contains(cov2SpeciesDbId)) {
                    Set<Long> diseaseInstanceDbIds = new HashSet<>();
                    for (GKInstance disease : (Collection<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.disease)) {
                        diseaseInstanceDbIds.add(disease.getDBID());
                    }
                    if (!diseaseInstanceDbIds.contains(cov2DiseaseDbId)) {
                        cov2InstancesWithoutCov2Disease.add(inst);
                    }
//                } else {
////                    Set<Long> diseaseInstanceDbIds = new HashSet<>();
////                    for (GKInstance disease : (Collection<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.disease)) {
////                        diseaseInstanceDbIds.add(disease.getDBID());
////                    }
////                    if (!diseaseInstanceDbIds.contains(cov2DiseaseDbId)) {
////                        System.out.println(inst);
////                        cov2InstancesWithoutCov2Disease.add(inst);
////                    }
                }
            }
        }

        for (GKInstance cov2Inst : cov2InstancesWithoutCov2Disease) {
            report.addLine(getReportLine(cov2Inst));
        }

        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    private String getReportLine(GKInstance cov2Inst) {
        return String.join("\t",
                cov2Inst.getDBID().toString(),
                cov2Inst.getDisplayName(),
                cov2Inst.getSchemClass().getName(),
                QACheckerHelper.getLastModificationAuthor(cov2Inst)
        );
    }

    private String[] getColumnHeaders() {
        return new String[]{"DB_ID_Entity", "DisplayName_Entity", "ClassName_Entity", "MostRecentAuthor_Entity"};
    }

    @Override
    public String getDisplayName() {
        return "CoV-2_Entities_Without_COVID-19_Disease";
    }
}
