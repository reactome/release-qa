package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This QA check was mostly used during the CoV-1-to-CoV-2 inference process. For now it is being kept, but may
 * be removed, given it was for a specific curations (August 2020).
 *
 * This checks for instances that have either a CoV-1 or CoV-2 species without the corresponding disease attribute.
 * For CoV-1 instances, they would be missing 'severe acute respiratory disease' (9678120). For CoV-2 instances, they
 * would be missing 'COVID-19' (9683912).
 *
 * @author jcook
 */

@SliceQACheck
public class CoVEntityDiseaseCheck extends AbstractQACheck {

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        // Fetches all Events and PhysicalEntities in DB
        Collection<GKInstance> eventsAndPhysicalEntities = new ArrayList<>();
        eventsAndPhysicalEntities.addAll(dba.fetchInstancesByClass(ReactomeJavaConstants.Event));
        eventsAndPhysicalEntities.addAll(dba.fetchInstancesByClass(ReactomeJavaConstants.PhysicalEntity));

        for (GKInstance instance : eventsAndPhysicalEntities) {
            Set<Long> speciesDbIds = new HashSet<>();
            Set<Long> diseaseDbIds = new HashSet<>();
            // Get all species, relatedSpecies, and disease instances within instance being checked
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
            // Check for missing disease attributes for CoV instances.
            if (speciesDbIds.contains(QACheckerHelper.getCoV1SpeciesDbId()) && !diseaseDbIds.contains(QACheckerHelper.getCoV1DiseaseDbId())) {
                report.addLine(getReportLine(instance, "COV-1 species without severe acute respiratory syndrome disease"));
            }
            if (speciesDbIds.contains(QACheckerHelper.getCoV2SpeciesDbId()) && !diseaseDbIds.contains(QACheckerHelper.getCoV2DiseaseDbId())) {
                report.addLine(getReportLine(instance, "COV-2 species without COVID-19 disease"));
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
        return "Entities_With_CoV_Species_Without_Corresponding_Disease";
    }
}
