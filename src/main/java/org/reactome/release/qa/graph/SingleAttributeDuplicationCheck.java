package org.reactome.release.qa.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This class is to check if duplications appears in an attribute.
 * @author wug
 *
 */
public class SingleAttributeDuplicationCheck extends MultipleAttributesMissingCheck {
    private static final Logger logger = Logger.getLogger(SingleAttributeDuplicationCheck.class);
    
    public SingleAttributeDuplicationCheck() {
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Map<String, List<String>> clsToAttributes = loadConfiguration();
        if (clsToAttributes == null || clsToAttributes.size() == 0)
            return report; // Nothing to be checked
        // Will be sorted based on cls names
        List<String> clsList = clsToAttributes.keySet().stream().sorted().collect(Collectors.toList());
        for (String cls : clsList) {
            logger.info("Checking " + cls + "...");
            List<String> attributes = clsToAttributes.get(cls);
            executeQACheck(cls, attributes, report);
        }
        
        report.setColumnHeaders("DB_ID",
                "DisplayName",
                "Class",
                "Duplication_DBID",
                "Duplication_DisplayName",
                "MostRecentAuthor");
        
        return report;
    }
    
    @SuppressWarnings("unchecked")
    private void executeQACheck(String clsName, 
                                List<String> attributes, 
                                QAReport report) throws Exception {
        Collection<GKInstance> instances = dba.fetchInstancesByClass(clsName);
        // Based on Fred's direct SQL query like the following. Otherwise, it takes huge time to check something
        // like DatabaseObject.created.
        // SQL =
        //         "SELECT DB_ID, modified" +
        //        " FROM DatabaseObject_2_modified" +
        //        " GROUP BY DB_ID, modified" +
        //        " HAVING COUNT(*) > 1";
        SchemaClass cls = dba.fetchSchema().getClassByName(clsName);
        for (String attName : attributes) {
            logger.info("Checking " + attName + "...");
            SchemaAttribute att = cls.getAttribute(attName);
            SchemaClass origin = att.getOrigin();
            StringBuilder builder = new StringBuilder();
            builder.append("SELECT DB_ID, ").append(attName).append(" FROM ");
            builder.append(origin.getName());
            if (att.isMultiple())
                builder.append("_2_").append(attName);
            builder.append(" GROUP BY DB_ID, ").append(attName);
            builder.append(" HAVING COUNT(*) > 1");

            Connection conn = dba.getConnection();
            PreparedStatement ps = conn.prepareStatement(builder.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Long dbId = rs.getLong(1);
                GKInstance instance = dba.fetchInstance(dbId);
                Long valueDbId = rs.getLong(2);
                GKInstance value = dba.fetchInstance(valueDbId);
                report.addLine(instance.getDBID() + "",
                        instance.getDisplayName(),
                        instance.getSchemClass().getName(),
                        value.getDBID() + "",
                        value.getDisplayName(),
                        QACheckerHelper.getLastModificationAuthor(instance));
            }
            rs.close();
            ps.close();
        }
    }

    @Override
    public String getDisplayName() {
        return "Attrinute_Value_Duplication";
    }
}
