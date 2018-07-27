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

public class T057_ComplexesWithOnlyOneComponent extends AbstractQACheck {
    
    private static final String SQL =
            "SELECT DB_ID" +
            " FROM Complex_2_hasComponent" +
            " GROUP BY DB_ID" +
            " HAVING COUNT(*) = 1";

    private final static String ISSUE = "Only one component";
     
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
            addReportLine(report, instance);
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