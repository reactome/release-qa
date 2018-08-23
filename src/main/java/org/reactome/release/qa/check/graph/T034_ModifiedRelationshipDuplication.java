package org.reactome.release.qa.check.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import org.gk.model.GKInstance;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T034_ModifiedRelationshipDuplication extends AbstractQACheck {
    
    private static final String SQL =
            "SELECT DB_ID, modified" +
            " FROM DatabaseObject_2_modified" +
            " GROUP BY DB_ID, modified" +
            " HAVING COUNT(*) > 1";

    private final static String ISSUE = "Duplicate modified entry ";
     
    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        
        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(SQL);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Long dbId = new Long(rs.getLong(1));
            GKInstance instance = dba.fetchInstance(dbId);
            Long otherDbId = new Long(rs.getLong(2));
            GKInstance other = dba.fetchInstance(otherDbId);
            addReportLine(report, instance, other);
        }
        
        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance, GKInstance other) {
        String issue = ISSUE + other;
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        issue,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}