package org.reactome.release.qa.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.GraphQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.JavaConstants;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

@GraphQATest
public class PhysicalEntitiesWithMoreThanOneCompartment extends AbstractQACheck {
    
    private final static String LOAD_ATTS[] = {ReactomeJavaConstants.inferredFrom, JavaConstants.entityOnOtherCell};

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "MostRecentAuthor");
    

    @Override
    public String getDisplayName() {
        return "Entity_With_More_Than_One_Compartment";
    }

    @Override
    @SuppressWarnings("unchecked")
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        String e2c = QACheckerHelper.getAttributeTableName(ReactomeJavaConstants.PhysicalEntity,
                                                           ReactomeJavaConstants.compartment,
                                                           dba);
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
        for (GKInstance entity: entities) {
            GKInstance entityOnOtherCell = null;
            if (entity.getSchemClass().isValidAttribute(JavaConstants.entityOnOtherCell))
                entityOnOtherCell = (GKInstance) entity.getAttributeValue(JavaConstants.entityOnOtherCell);
            if (entityOnOtherCell == null) {
                report.addLine(entity.getDBID().toString(), 
                               entity.getDisplayName(), 
                               entity.getSchemClass().getName(), 
                               QACheckerHelper.getLastModificationAuthor(entity));
            }
        }
        
        report.setColumnHeaders(HEADERS);

        return report;
    }

}