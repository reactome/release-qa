package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@SliceQACheck
public class CoVEntityDiseaseCheck extends AbstractQACheck {

    private static final long cov1SpeciesDbId = 9678119L;
    private static final long cov1DiseaseDbId = 9678120L;

    private static final long cov2SpeciesDbId = 9681683L;
    private static final long cov2DiseaseDbId = 9683912L;

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> eventsAndPhysicalEntities = new ArrayList<>();
        eventsAndPhysicalEntities.addAll(dba.fetchInstancesByClass(ReactomeJavaConstants.Event));
        eventsAndPhysicalEntities.addAll(dba.fetchInstancesByClass(ReactomeJavaConstants.PhysicalEntity));

        for (GKInstance instance : eventsAndPhysicalEntities) {
            Set<Long> speciesDbIds = new HashSet<>();
            Set<Long> diseaseDbIds = new HashSet<>();
            if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
                for (GKInstance species : (Collection<GKInstance>) instance.getAttributeValuesList(ReactomeJavaConstants.species)) {
                    speciesDbIds.add(species.getDBID());
                }
            }
            if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.relatedSpecies)) {
                for (GKInstance relSpecies : (Collection<GKInstance>) instance.getAttributeValuesList(ReactomeJavaConstants.relatedSpecies)) {
                    speciesDbIds.add(relSpecies.getDBID());
                }
            }
            if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.disease)) {
                for (GKInstance disease : (Collection<GKInstance>) instance.getAttributeValuesList(ReactomeJavaConstants.disease)) {
                    diseaseDbIds.add(disease.getDBID());
                }
            }
            if (speciesDbIds.contains(cov2SpeciesDbId) && !diseaseDbIds.contains(cov2DiseaseDbId)) {
                report.addLine(getReportLine(instance, "COV-2 species without COVID-19 disease"));
            }
            if (speciesDbIds.contains(cov1SpeciesDbId) && !diseaseDbIds.contains(cov1DiseaseDbId)) {
                report.addLine(getReportLine(instance, "COV-1 species without severe acute respiratory syndrome disease"));
            }
        }

        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    private String getReportLine(GKInstance instance, String issue) {
        return String.join("\t",
                instance.getDBID().toString(),
                instance.getDisplayName(),
                instance.getSchemClass().getName(),
                QACheckerHelper.getLastModificationAuthor(instance),
                issue
        );
    }

    private String[] getColumnHeaders() {
        return new String[]{"DB_ID_Entity", "DisplayName_Entity", "ClassName_Entity", "MostRecentAuthor_Entity, Issue"};
    }

    @Override
    public String getDisplayName() {
        return "Entities_With_COV_Species_Without_Corresponding_Disease";
    }
}
