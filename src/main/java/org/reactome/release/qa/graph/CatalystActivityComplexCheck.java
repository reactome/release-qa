package org.reactome.release.qa.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.GraphQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

@GraphQACheck
public class CatalystActivityComplexCheck extends AbstractQACheck {
    
    private static final List<String> HEADERS = Arrays.asList("DBID", "DisplayName", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return "CatalystActivity_PhysicalEntity_ActivityUnit_Refers_To_Same_Complex";
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        String table1 = QACheckerHelper.getAttributeTableName(ReactomeJavaConstants.CatalystActivity,
                                                              ReactomeJavaConstants.physicalEntity, 
                                                              dba);
        String table2 = QACheckerHelper.getAttributeTableName(ReactomeJavaConstants.CatalystActivity,
                                                           ReactomeJavaConstants.activeUnit,
                                                           dba);
        String sql = "SELECT a.DB_ID FROM " + table1 + " a, " + table2 + " b " + 
                     "WHERE a.DB_ID = b.DB_ID AND a." + ReactomeJavaConstants.physicalEntity + 
                     " = b." + ReactomeJavaConstants.activeUnit + " AND a." + ReactomeJavaConstants.physicalEntity + 
                     "_class = 'Complex'";
        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Long dbId = new Long(rs.getLong(1));
            GKInstance catAct = dba.fetchInstance(dbId);
            if (!isEscaped(catAct)) {
                report.addLine(catAct.getDBID().toString(), 
                        catAct.getDisplayName(), 
                        QACheckerHelper.getLastModificationAuthor(catAct));
            }
        }
        rs.close();
        ps.close();
        
        report.setColumnHeaders(HEADERS);

        return report;
    }

}