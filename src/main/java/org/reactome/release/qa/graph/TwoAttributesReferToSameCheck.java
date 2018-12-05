package org.reactome.release.qa.graph;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.annotations.GraphQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * Sometimes two attributes should not refer to the same instance. For example,
 * an CandidateSet should not refer to the same instance in hasMember and hasCandidate
 * attributes. This check tries to catch such a use based on a configuration file.
 * @author wug
 *
 */
@GraphQACheck
public class TwoAttributesReferToSameCheck extends AbstractQACheck {
    private static final Logger logger = Logger.getLogger(TwoAttributesReferToSameCheck.class);

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        List<CheckConfiguration> configurations = loadConfiguration();
        if (configurations == null || configurations.size() == 0)
            return report; // Nothing to be checked
        // Will be sorted based on cls names
        Collections.sort(configurations, (c1, c2) -> c1.clsName.compareTo(c2.clsName));        
        for (CheckConfiguration config : configurations) {
            logger.info("Check " + config.clsName + " for " + config.toString() + "...");
            executeQACheck(config, report);
        }
        report.setColumnHeaders("DBID",
                "DisplayName",
                "Class",
                "Attributes",
                "ValueDBId",
                "ValueDisplayName",
                "MostRecentAuthor");
        
        return report;
    }
    
    protected void executeQACheck(CheckConfiguration config, QAReport report) throws Exception {
        // Make sure only instance type can be checked here
        SchemaClass cls = dba.fetchSchema().getClassByName(config.clsName);
        if (!cls.getAttribute(config.attName1).isInstanceTypeAttribute()) {
            logger.error("Only instance type attribute can be checked. " + config.clsName + "." + config.attName1 + " is not!");
            return;
        }
        if (!cls.getAttribute(config.attName2).isInstanceTypeAttribute()) {
            logger.error("Only instance type attribute can be checked. " + config.clsName + "." + config.attName2 + " is not!");
            return;
        }
        // Build an SQL query for the check
        String table1 = QACheckerHelper.getAttributeTableName(config.clsName, config.attName1, dba);
        String table2 = QACheckerHelper.getAttributeTableName(config.clsName, config.attName2, dba);
        // In a rare case
        String query = null;
        if (table1.equals(table2)) {
            query = "SELECT DB_ID, " + config.attName1 + " FROM " + 
                    table1 + " WHERE " + config.attName1 + " = " + config.attName2 + 
                    " AND " + config.attName1 + " IS NOT NULL";
        }
        else { // Need to join two tables together
            query = "SELECT a.DB_ID, a." + config.attName1 + " FROM " + 
                    table1 + " a, " + table2 + " b WHERE a.DB_ID = b.DB_ID AND " + 
                    "a." + config.attName1 + " = " + "b." + config.attName2 + " AND a." + 
                    config.attName1 + " IS NOT NULL";
        }
//        System.out.println("SQL query: " + query);
        Connection connection = dba.getConnection();
        PreparedStatement stat = connection.prepareStatement(query);
        ResultSet result = stat.executeQuery();
        while (result.next()) {
            Long dbId = result.getLong(1);
            GKInstance inst = dba.fetchInstance(dbId);
            if (isEscaped(inst)) {
                continue;
            }
            Long valueId = result.getLong(2);
            GKInstance value = dba.fetchInstance(valueId);
            if (value == null)
                throw new IllegalStateException("Instance cannot be found for " + valueId + ".");
            report.addLine(inst.getDBID() + "",
                           inst.getDisplayName(),
                           inst.getSchemClass().getName(),
                           config.toString(),
                           value.getDBID() + "",
                           value.getDisplayName(),
                           QACheckerHelper.getLastModificationAuthor(inst));
        }
        result.close();
        stat.close();
    }

    @Override
    public String getDisplayName() {
        return "Two_Attributes_Refer_To_Same_Instance";
    }
    
    protected List<CheckConfiguration> loadConfiguration() throws Exception {
        File file = getConfigurationFile();
        try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()))) {
            List<CheckConfiguration> configrations = new ArrayList<>();
            stream.filter(line -> !line.startsWith("#"))
                  .map(line -> line.split("\t"))
                  .filter(tokens -> tokens.length > 2)
                  .forEach(tokens -> {
                      CheckConfiguration configuration = new CheckConfiguration();
                      configuration.clsName = tokens[0];
                      configuration.attName1 = tokens[1];
                      configuration.attName2 = tokens[2];
                      configrations.add(configuration);
                  });
            return configrations;
        }
    }
    
    /**
     * Use the same name as in class MultipleAttributesCrossClassesMissingCheck for future
     * generation.
     * @author wug
     *
     */
    class CheckConfiguration {
        String clsName;
        String attName1;
        String attName2;
        
        @Override
        public String toString() {
            return attName1 + ", " + attName2;
        }
    }

}
