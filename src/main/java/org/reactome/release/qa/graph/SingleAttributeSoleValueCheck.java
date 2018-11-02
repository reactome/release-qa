package org.reactome.release.qa.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.GraphQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This check is to make sure more than one value should be provided in a specific attribute.
 * For example, hasMember in DefinedSet or hasComponent in Complex should have more than one value.
 * @author wug
 *
 */
@GraphQACheck
public class SingleAttributeSoleValueCheck extends SingleAttributeMissingCheck {
    private static final Logger logger = Logger.getLogger(SingleAttributeSoleValueCheck.class);
    
    public SingleAttributeSoleValueCheck() {
    }
    
    private boolean shouldEscape(GKInstance inst, String attName) throws Exception {
        if (isEscaped(inst)) {
            return true;
        }
        if (inst.getSchemClass().isa(ReactomeJavaConstants.Pathway) && 
            attName.equals(ReactomeJavaConstants.hasEvent)) {
            GKInstance disease = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.disease);
            if (disease != null)
                return true;
        }
        return false;
    }

    @Override
    protected void executeQACheck(String clsName, String attName, QAReport report) throws Exception {
        // For quick performance, we will use SQL query directly
        String tableName = QACheckerHelper.getAttributeTableName(clsName, attName, dba);
        if (!tableName.contains("_2_")) {
            logger.error("SingleAttributeSoleValueCheck should be used for multiple-valued attributes only. " + attName + " in " + clsName + " is not!");
            return;
        }
        String query = "SELECT DB_ID FROM " + tableName + " GROUP BY DB_ID HAVING COUNT(*) = 1";
        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Long dbId = rs.getLong(1);
            GKInstance instance = dba.fetchInstance(dbId);
            // Since the attribute may be defined in a superclass, the above SQL query may pick out
            // instances in another class (e.g. an CandidateSet for DefinedSet checking). Therefore
            // the following check.
            if (!instance.getSchemClass().isa(clsName))
                continue;
            // Escape the special case
            if (shouldEscape(instance, attName))
                continue;
            report.addLine(instance.getDBID() + "",
                           instance.getDisplayName(),
                           clsName,
                           attName,
                           QACheckerHelper.getLastModificationAuthor(instance));
        }
        rs.close();
        ps.close();
    }

    @Override
    public String getDisplayName() {
        return "Single_Attribute_Sole_Value";
    }

}
