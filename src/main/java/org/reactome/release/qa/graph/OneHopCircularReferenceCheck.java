package org.reactome.release.qa.graph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.schema.GKSchema;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.annotations.GraphQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This class is used to check if a one-hop circular reference existing between two instances. 
 * 
 * This check's escape list can include both preceding and following RLEs.

 * @author wug
 */
@GraphQACheck
@SuppressWarnings("unchecked")
public class OneHopCircularReferenceCheck extends AbstractQACheck {
    private final static Logger logger = Logger.getLogger(OneHopCircularReferenceCheck.class);
    
    public OneHopCircularReferenceCheck() {
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        
        // Collect all original attributes
        Schema schema = dba.getSchema();
        Set<SchemaAttribute> attributes = ((GKSchema)schema).getOriginalAttributes();
        List<SchemaAttribute> attributeList = new ArrayList<>(attributes);
        Collections.sort(attributeList, (att1, att2) -> att1.getName().compareTo(att2.getName()));
        Set<String[]> escapedAttributes = loadEscapedAttributes();
        for (int i = 0; i < attributeList.size(); i++) {
            SchemaAttribute att1 = attributeList.get(i);
            if (!att1.isInstanceTypeAttribute())
                continue;
            for (int j = i; j < attributeList.size(); j++) { // We want to check the same attribute too (e.g. precedingEvent)
                SchemaAttribute att2 = attributeList.get(j);
                if (!att2.isInstanceTypeAttribute())
                    continue;
                if (escape(att1, att2, escapedAttributes))
                    continue;
                if (!isCompatible(att1, att2))
                    continue; // These two attributes should be able to use the same type of instance at least.
                check(att1, att2, report);
            }
        }
        
        report.setColumnHeaders("DB_ID_1",
                "DisplayName_1",
                "Class_1",
                "Attribute_1",
                "MostRecentAuthor_1",
                "DB_ID_2",
                "DisplayName_2",
                "Class_2",
                "Attribute_2",
                "MostRecentAuthor_2");
        
        return report;
    }
    
    private boolean escape(SchemaAttribute att1, SchemaAttribute att2, Set<String[]> escape) {
        for (String[] escapeNames : escape) {
            if (att1.getName().equals(escapeNames[0]) && att2.getName().equals(escapeNames[1]))
                return true;
        }
        return false;
    }
    
    private boolean isCompatible(SchemaAttribute att1, SchemaAttribute att2) {
        SchemaClass cls1 = att1.getOrigin();
        // Make sure cls1 can be used in att2
        Collection<SchemaClass> allowedClses = att2.getAllowedClasses();
        boolean first = false;
        for (SchemaClass cls : allowedClses) {
            if (cls1.isa(cls) || cls.isa(cls1)) {
                first = true; // This is a relaxed constrain
                break;
            }
        }
        SchemaClass cls2 = att2.getOrigin();
        allowedClses = att1.getAllowedClasses();
        boolean second = false;
        for (SchemaClass cls : allowedClses) {
            if (cls2.isa(cls) || cls.isa(cls2)) {
                second = true;
                break;
            }
        }
        return first && second;
    }
    
    // The following code is modified from Fred's implementation: T027.
    private void check(SchemaAttribute att1, SchemaAttribute att2, QAReport report) throws Exception {
        logger.info("Check " + att1.getName() + " and " + att2.getName() + "...");
        String table1 = QACheckerHelper.getAttributeTableName(att1.getOrigin().getName(), att1.getName(), dba);
        String table2 = QACheckerHelper.getAttributeTableName(att2.getOrigin().getName(), att2.getName(), dba);
        String sql = "SELECT a.DB_ID, a." + att1.getName() +
                " FROM " + table1 + " a, " + table2 + " b" +
                " WHERE a.DB_ID = b." + att2.getName() +
                " AND b.DB_ID = a." + att1.getName();
        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Long dbId = new Long(rs.getLong(1));
            GKInstance instance = dba.fetchInstance(dbId);
            if (isEscaped(instance)) {
                continue;
            }
            Long otherDbId = new Long(rs.getLong(2));
            GKInstance other = dba.fetchInstance(otherDbId);
            if (isEscaped(other)) {
                continue;
            }
            report.addLine(dbId + "",
                           instance.getDisplayName(),
                           instance.getSchemClass().getName(),
                           att1.getName(),
                           QACheckerHelper.getLastModificationAuthor(instance),
                           other.getDBID() + "",
                           other.getDisplayName(),
                           other.getSchemClass().getName(),
                           att2.getName(),
                           QACheckerHelper.getLastModificationAuthor(other));
        }
        rs.close();
        ps.close();
        
    }

    @Override
    public String getDisplayName() {
        return "One_Hop_Circular_Reference";
    }

    private Set<String[]> loadEscapedAttributes() throws IOException {
        File file = getConfigurationFile();
        if (file == null)
            return new HashSet<>();
        try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()))) {
            Set<String[]> attributes = stream.filter(line -> !line.startsWith("#"))
                                           .map(line -> line.trim().split("\t"))
                                           .collect(Collectors.toSet());
            return attributes;
        }
    }
}
