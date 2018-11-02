package org.reactome.release.qa.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.annotations.GraphQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

@GraphQACheck
public class DatabaseObjectSelfLoopCheck extends AbstractQACheck {
    
    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Attribute", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return "DatabaseObject_With_Self_Loop";
    }

    @SuppressWarnings("unchecked")
    @Override
    public QAReport executeQACheck() throws Exception {
        // All schema classes.
        Collection<SchemaClass> classes = dba.getSchema().getClasses();

        // Build the report. Check each attribute which can take an instance
        // of its defining class as a value.
        QAReport report = new QAReport();
        for (SchemaClass cls: classes) {
            Collection<SchemaAttribute> attributes = cls.getAttributes();
            for (SchemaAttribute att: attributes) {
                if (att.getOrigin() == cls) {
                    Collection<SchemaClass> allowedClasses = att.getAllowedClasses();
                    for (SchemaClass valueCls: allowedClasses) {
                        if (cls.isa(valueCls)) {
                            String attName = att.getName();
                            String table = att.isMultiple() ?
                                    cls.getName() + "_2_" + attName : cls.getName();
                            String sql = "SELECT DB_ID from " + table +
                                    " WHERE DB_ID = " + attName;
                            Connection conn = dba.getConnection();
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ResultSet rs = ps.executeQuery();
                            while (rs.next()) {
                                Long dbId = new Long(rs.getLong(1));
                                GKInstance instance = dba.fetchInstance(dbId);
                                if (!isEscaped(instance)) {
                                    addReportLine(report, instance, attName);
                                }
                            }
                            rs.close();
                            ps.close();
                        }
                    }
                }
            }
        }
        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance, String attribute) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        attribute,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}