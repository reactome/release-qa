package org.reactome.release.qa.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T090_CatalystActivityCompartmentDoesNotMatchReactionCompartment
extends AbstractQACheck {

    private static final String ISSUE = "Compartment mismatch: ";

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    private static final String[] RLE_LOAD_ATTS = {
            ReactomeJavaConstants.compartment,
            ReactomeJavaConstants.catalystActivity
    };

    private static final String[] CATALYST_LOAD_ATTS = {
            ReactomeJavaConstants.physicalEntity
    };
    
    private static final String[] ENTITY_LOAD_ATTS = {
            ReactomeJavaConstants.compartment
    };

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> rles =
                QACheckerHelper.getInstancesWithNullAttribute(dba,
                        ReactomeJavaConstants.ReactionlikeEvent,
                        ReactomeJavaConstants.inferredFrom, null);
        dba.loadInstanceAttributeValues(rles, RLE_LOAD_ATTS);
        for (GKInstance rle: rles) {
            if (!isRLEValid(rle)) {
                addReportLine(report, rle);
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }

    @SuppressWarnings("unchecked")
    /**
     * Returns whether there is no catalyst or there is at least one
     * catalyst container in the RLE containers and there is at least
     * one RLE container in the catalyst containers.
     */
    private boolean isRLEValid(GKInstance rle) throws Exception {
        List<GKInstance> catActivities =
                rle.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (catActivities.isEmpty()) {
            return true;
        }
        List<GKInstance> rleCmptList =
                rle.getAttributeValuesList(ReactomeJavaConstants.compartment);
        Set<GKInstance> rleCmpts = new HashSet<GKInstance>(rleCmptList);
        dba.loadInstanceAttributeValues(catActivities, CATALYST_LOAD_ATTS);
        for (GKInstance catActivity: catActivities) {
            List<GKInstance> entities =
                    catActivity.getAttributeValuesList(ReactomeJavaConstants.physicalEntity);
            dba.loadInstanceAttributeValues(entities, ENTITY_LOAD_ATTS);
            for (GKInstance entity: entities) {
                List<GKInstance> catCmpts =
                        entity.getAttributeValuesList(ReactomeJavaConstants.compartment);
                for (GKInstance catCmpt: catCmpts) {
                    if (rleCmpts.contains(catCmpt)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addReportLine(QAReport report, GKInstance instance) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        ISSUE,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}
