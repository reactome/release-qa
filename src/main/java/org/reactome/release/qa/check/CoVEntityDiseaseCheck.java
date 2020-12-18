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

        for (GKInstance eventOrPhysicalEntityInst : eventsAndPhysicalEntities) {
            // Get all species, relatedSpecies, and disease instances within instance being checked
            Set<Long> speciesDbIds = QACheckerHelper.getSpeciesAndRelatedSpeciesDbIds(eventOrPhysicalEntityInst);

            Set<Long> diseaseDbIds = new HashSet<>();
            if (eventOrPhysicalEntityInst.getSchemClass().isValidAttribute(ReactomeJavaConstants.disease)) {
                for (GKInstance disease : (Collection<GKInstance>) eventOrPhysicalEntityInst.getAttributeValuesList(ReactomeJavaConstants.disease)) {
                    diseaseDbIds.add(disease.getDBID());
                }
            }

            // Check for missing disease attributes for CoV instances.
            if (hasCoVSpeciesWithoutCoVDisease(speciesDbIds, diseaseDbIds)) {
                report.addLine(getReportLine(eventOrPhysicalEntityInst, speciesDbIds));
            }
        }

        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    private boolean hasCoVSpeciesWithoutCoVDisease(Set<Long> speciesDbIds, Set<Long> diseaseDbIds) {
        return hasCoV1SpeciesWithoutCoV1Disease(speciesDbIds, diseaseDbIds) || hasCoV2SpeciesWithoutCoV2Disease(speciesDbIds, diseaseDbIds);

    }

    private boolean hasCoV1SpeciesWithoutCoV1Disease(Set<Long> speciesDbIds, Set<Long> diseaseDbIds) {
        return speciesDbIds.contains(QACheckerHelper.COV_1_SPECIES_DB_ID) && !diseaseDbIds.contains(QACheckerHelper.COV_1_DISEASE_DB_ID);
    }

    private boolean hasCoV2SpeciesWithoutCoV2Disease(Set<Long> speciesDbIds, Set<Long> diseaseDbIds) {
        return speciesDbIds.contains(QACheckerHelper.COV_2_SPECIES_DB_ID) && !diseaseDbIds.contains(QACheckerHelper.COV_2_DISEASE_DB_ID);
    }

    private String getReportLine(GKInstance instance, Set<Long> speciesDbIds) {
        String issue = hasCoV1Species(speciesDbIds) ?
                "COV-1 species without severe acute respiratory syndrome disease" :
                "COV-2 species without COVID-19 disease";

        return String.join("\t",
                instance.getDBID().toString(),
                instance.getDisplayName(),
                instance.getSchemClass().getName(),
                QACheckerHelper.getLastModificationAuthor(instance),
                issue
        );
    }

    private boolean hasCoV1Species(Set<Long> speciesDbIds) {
        return speciesDbIds.contains(QACheckerHelper.COV_1_SPECIES_DB_ID);
    }

    private String[] getColumnHeaders() {
        return new String[]{"DB_ID_Entity", "DisplayName_Entity", "ClassName_Entity", "MostRecentAuthor_Entity, Issue"};
    }

    @Override
    public String getDisplayName() {
        return "Entities_With_CoV_Species_Without_Corresponding_Disease";
    }
}
