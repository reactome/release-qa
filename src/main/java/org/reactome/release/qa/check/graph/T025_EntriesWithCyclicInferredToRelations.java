package org.reactome.release.qa.check.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T025_EntriesWithCyclicInferredToRelations extends AbstractQACheck {
    
    private static final String INFERRED_TO_TABLE =
            ReactomeJavaConstants.PhysicalEntity + "_2_" + ReactomeJavaConstants.inferredTo;

    private final static String ISSUE = "%s refers to inferral source %s";
    
    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public QAReport executeQACheck() throws Exception {
        Schema schema = dba.getSchema();
        QAReport report = new QAReport();

        // Check inferred references.
        Collection<SchemaClass> classes = schema.getClasses();
        SchemaClass entityCls =
                schema.getClassByName(ReactomeJavaConstants.PhysicalEntity);
        for (SchemaClass cls: classes) {
            if (cls.isa(entityCls)) {
                Collection<SchemaAttribute> attributes = cls.getAttributes();
                for (SchemaAttribute att: attributes) {
                    if (att.getOrigin() == cls &&
                            !ReactomeJavaConstants.inferredTo.equals(att.getName())) {
                        Collection<SchemaClass> valueClasses = att.getAllowedClasses();
                        for (SchemaClass valueCls: valueClasses) {
                            if (valueCls.isa(entityCls)) {
                                check(report, att);
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void check(QAReport report, SchemaAttribute att) throws Exception {
        SchemaClass cls = att.getOrigin();
        String attName = att.getName();
        String table = att.isMultiple() ?
                cls.getName() + "_2_" + attName : cls.getName();
        String sql = "SELECT a.DB_ID, a." + attName +
                " FROM " + table + " a, " + INFERRED_TO_TABLE + " b" +
                " WHERE a.DB_ID = b." + ReactomeJavaConstants.inferredTo +
                " AND b.DB_ID = a." + attName;
        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Long dbId = new Long(rs.getLong(1));
            GKInstance instance = dba.fetchInstance(dbId);
            Long otherDbId = new Long(rs.getLong(2));
            GKInstance other = dba.fetchInstance(otherDbId);
            addReportLine(report, instance, attName, other);
        }
    }

    private void addReportLine(QAReport report, GKInstance instance,
            String attribute, GKInstance other) {
        String issue = String.format(ISSUE, attribute, other);
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        issue,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}