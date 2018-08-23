package org.reactome.release.qa.check.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.JavaConstants;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T030_PhysicalEntitiesWithMoreThanOneCompartment extends AbstractQACheck {
    
    private final static String ISSUE = "More than one compartment";

    private final static String LOAD_ATTS[] = { ReactomeJavaConstants.inferredFrom };

    private final static String LOAD_REV_ATTS[] = { JavaConstants.entityOnOtherCell };
    
    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    @SuppressWarnings("unchecked")
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        String e2c = ReactomeJavaConstants.PhysicalEntity + "_2_" + ReactomeJavaConstants.compartment;
        String sql = "SELECT DB_ID" +
                " FROM " + e2c +
                " GROUP BY DB_ID" +
                " HAVING COUNT(" + ReactomeJavaConstants.compartment + ") > 1";
        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        List<Long> dbIds = new ArrayList<Long>();
        while (rs.next()) {
            dbIds.add(rs.getLong(1));
        }
        Collection<GKInstance> entities =
                dba.fetchInstances(ReactomeJavaConstants.PhysicalEntity, dbIds);
        dba.loadInstanceAttributeValues(entities, LOAD_ATTS);
        dba.loadInstanceReverseAttributeValues(entities, LOAD_REV_ATTS);
        for (GKInstance entity: entities) {
            Collection<GKInstance> inferred =
                    entity.getAttributeValuesList(ReactomeJavaConstants.inferredFrom);
            if (inferred ==  null || inferred.size() == 0) {
                Collection<GKInstance> onOtherCell =
                        entity.getReferers(JavaConstants.entityOnOtherCell);
                if (onOtherCell == null || onOtherCell.size() == 0) {
                    addReportLine(report, entity);
                }
            }
        }
        
        report.setColumnHeaders(HEADERS);

        return report;
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