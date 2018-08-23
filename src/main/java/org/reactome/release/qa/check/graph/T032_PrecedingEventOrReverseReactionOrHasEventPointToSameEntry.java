package org.reactome.release.qa.check.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T032_PrecedingEventOrReverseReactionOrHasEventPointToSameEntry extends AbstractQACheck {
    
    private final static String ISSUE = "Activity is the same as the active unit ";
    
    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        String c2a = ReactomeJavaConstants.CatalystActivity + "_2_" + ReactomeJavaConstants.activeUnit;
        String sql = "SELECT c.DB_ID, c." + ReactomeJavaConstants.physicalEntity +
                " FROM " + ReactomeJavaConstants.CatalystActivity +
                " c, " + c2a + " c2a" +
                " WHERE c.DB_ID = c2a.DB_ID" +
                " AND c." + ReactomeJavaConstants.physicalEntity + " = c2a." + ReactomeJavaConstants.activeUnit;
        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Long dbId = new Long(rs.getLong(1));
            GKInstance catAct = dba.fetchInstance(dbId);
            Long unitDbId = new Long(rs.getLong(2));
            GKInstance unit = dba.fetchInstance(unitDbId);
            addReportLine(report, catAct, unit);
        }
        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance, GKInstance unit) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        ISSUE + unit.toString(), 
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}