package org.reactome.release.qa.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the abstract class for attribute cardinality checks.
 * The subclass is responsible for providing a count comparison
 * condition right-hand side clause, e.g. "= 1".
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
abstract public class SingleAttributeCardinalityCheck extends SingleAttributeMissingCheck {
    private static final Logger logger = Logger.getLogger(SingleAttributeCardinalityCheck.class);
    
    private String comparison;
    
    /**
     * @param comparison the count comparison SQL clause, e.g. <code>= 1</code>.
     */
    protected SingleAttributeCardinalityCheck(String comparison) {
        this.comparison = comparison;
    }
    
    /**
     * This base class implementation ignores the attribute name
     * and delegates to the standard {@link #isEscaped(GKInstance)}
     * method.
     * 
     * Subclasses can override this method to refine the escape
     * criterion based on the attribute name.
     * 
     * @param inst the instance to check
     * @param attName the attribute to check
     * @return whether to escape the instance
     * @throws Exception
     */
    protected boolean isEscaped(GKInstance inst, String attName) throws Exception {
        return isEscaped(inst);
    }

    @Override
    protected void executeQACheck(String clsName, String attName, QAReport report) throws Exception {
        // For quick performance, we will use SQL query directly
        String tableName = QACheckerHelper.getAttributeTableName(clsName, attName, dba);
        if (!tableName.contains("_2_")) {
            logger.error("SingleAttributeCardinalityCheck should be used for" +
                    " multiple-valued attributes only. " + 
                    clsName + "." + attName + " is single-valued.");
            return;
        }
        String query = "SELECT DB_ID FROM " + tableName +
                " GROUP BY DB_ID HAVING COUNT(*) " + comparison;
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
            if (isEscaped(instance, attName))
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

}
