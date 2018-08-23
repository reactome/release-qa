package org.reactome.release.qa.check.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T062_DuplicatedReferenceEntities extends AbstractQACheck {

    private static final String ISSUE = "Same content as ";

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @SuppressWarnings("unchecked")
    @Override
    /**
     * Reports the reference entities with the same database name,
     * identifier and variant identifier.
     */
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> refEntities =
                dba.fetchInstancesByClass(ReactomeJavaConstants.ReferenceEntity);
        // First pass: collect suspects.
        String[] loadAtts = { ReactomeJavaConstants.identifier };
        dba.loadInstanceAttributeValues(refEntities, loadAtts);
        // Grouping of ref entities with the same identifer.
        Map<String, List<GKInstance>> suspects = new HashMap<String, List<GKInstance>>();
        // Map of each identifier to its first ref entity occurence.
        Map<String, GKInstance> idsToInstance = new HashMap<String, GKInstance>();
        // For each ref entity, if it maps to the same identifier then
        // add it to the suspects.
        for (GKInstance refEntity: refEntities) {
            String identifier =
                    (String) refEntity.getAttributeValue(ReactomeJavaConstants.identifier);
            GKInstance other = idsToInstance.get(identifier);
            if (other != null) {
                List<GKInstance> instances = suspects.get(identifier);
                if (instances == null) {
                    instances = new ArrayList<GKInstance>();
                    instances.add(other);
                    suspects.put(identifier, instances);
                }
                instances.add(refEntity);
            } else {
                idsToInstance.put(identifier, refEntity);
            }
        }
        // Second pass: vet the suspects.
        String[] loadMoreAtts = {
                ReactomeJavaConstants.referenceDatabase,
                ReactomeJavaConstants.variantIdentifier
        };
        for (List<GKInstance> instances: suspects.values()) {
            dba.loadInstanceAttributeValues(instances, loadMoreAtts);
            for (int i=0; i < instances.size(); i++) {
                GKInstance instance = instances.get(i);
                for (int j = i + 1; j < instances.size(); j++) {
                    GKInstance other = instances.get(j);
                    GKInstance refDb =
                            (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
                    GKInstance otherRefDb =
                            (GKInstance) other.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
                    if (refDb == otherRefDb) {
                        String varId = null;
                        if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier)) {
                            varId = (String) instance.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
                        }
                        String otherVarId = null;
                        if (other.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier)) {
                            otherVarId = (String) other.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
                        }
                        boolean isVarIdEqual = varId == null ? otherVarId == null : varId.equals(otherVarId);
                        if (isVarIdEqual) {
                            addReportLine(report, instance, other); 
                        }
                    }
                }
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance, GKInstance other) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        ISSUE + other.getDisplayName() + " (DBID " + other.getDBID() + ")",  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}
