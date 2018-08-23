package org.reactome.release.qa.check.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T033_OtherRelationsThatPointToTheSameEntry extends AbstractQACheck {
    
    private final static String ISSUE = "References to the same instance";
    
    private final static String[] SKIP_ATTS = {
            "author",
            "created",
            "edited",
            "modified",
            "revised",
            "reviewed",
            "input",
            "output",
            "entityOnOtherCell",
            "hasComponent",
            "requiredInputComponent",
            "physicalEntity",
            "activeUnit",
            "reverseReaction",
            "precedingEvent",
            "hasEvent",
            "goCellularComponent",
            "compartment",
            "referenceSequence",
            "secondReferenceSequence",
            "hasCandidate",
            "hasMember"
    };
    
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

        // First pass: map value classes to attributes.
        Collection<SchemaClass> classes = schema.getClasses();
        SchemaClass dbObjCls =
                schema.getClassByName(ReactomeJavaConstants.DatabaseObject);
        Map<SchemaClass, Set<SchemaAttribute>> valueClsAtts =
                new HashMap<SchemaClass, Set<SchemaAttribute>>();
        Set<String> skipAtts = Stream.of(SKIP_ATTS).collect(Collectors.toSet());
        for (SchemaClass cls: classes) {
            Collection<SchemaAttribute> attributes = cls.getAttributes();
            for (SchemaAttribute att: attributes) {
                if (att.getOrigin() == cls && !skipAtts.contains(att.getName())) {
                    Collection<SchemaClass> valueClasses = att.getAllowedClasses();
                    for (SchemaClass valueCls: valueClasses) {
                        if (valueCls.isa(dbObjCls) && valueCls != dbObjCls) {
                            Set<SchemaAttribute> atts = valueClsAtts.get(valueCls);
                            if (atts == null) {
                                atts = new HashSet<SchemaAttribute>();
                                valueClsAtts.put(valueCls, atts);
                            }
                            atts.add(att);
                        }
                    }
                }
            }
        }

        // Second pass: check compatible reference attributes.
        SchemaClass[] valueClasses =
                valueClsAtts.keySet().toArray(new SchemaClass[valueClsAtts.size()]);
        for (int i=0; i < valueClasses.length; i++) {
            SchemaClass valueCls = valueClasses[i];
            Set<SchemaAttribute> attSet = valueClsAtts.get(valueCls);
            SchemaAttribute[] atts = attSet.toArray(new SchemaAttribute[attSet.size()]);
            for (int ia=0; ia < atts.length; ia++) {
                SchemaAttribute att = atts[ia];
                for (int j=i; j < valueClasses.length; j++) {
                    SchemaClass otherValueCls = valueClasses[j];
                    if (valueCls.isa(otherValueCls)) {
                        Set<SchemaAttribute> otherAttSet = valueClsAtts.get(otherValueCls);
                        SchemaAttribute[] otherAtts =
                                attSet.toArray(new SchemaAttribute[otherAttSet.size()]);
                        int ja = valueCls == otherValueCls ? ia + 1 : 0;
                        for (; ja < otherAtts.length; ja++) {
                            SchemaAttribute otherAtt = otherAtts[ja];
                            SchemaClass cls = att.getOrigin();
                            SchemaClass otherCls = otherAtt.getOrigin();
                            if (cls.isa(otherCls) || otherCls.isa(cls)) {
                                check(report, att, otherAtt);
                            }
                        }
                    }
                }
            }
        }
        
        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void check(QAReport report, SchemaAttribute att, SchemaAttribute otherAtt)
            throws Exception {
        SchemaClass cls = att.getOrigin();
        String attName = att.getName();
        String table = att.isMultiple() ?
                cls.getName() + "_2_" + attName : cls.getName();
        SchemaClass otherCls = otherAtt.getOrigin();
        String otherAttName = otherAtt.getName();
        String otherTable = otherAtt.isMultiple() ?
                otherCls.getName() + "_2_" + otherAttName : otherCls.getName();
        String sql = "SELECT a.DB_ID, a." + attName +
                " FROM " + table + " a, " + otherTable + " b" +
                " WHERE a.DB_ID = b.DB_ID " +
                " AND a." + attName + " = b." + otherAttName;
        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Long dbId = new Long(rs.getLong(1));
            GKInstance instance = dba.fetchInstance(dbId);
            Long otherDbId = new Long(rs.getLong(2));
            GKInstance other = dba.fetchInstance(otherDbId);
            addReportLine(report, instance, attName, otherAttName, other);
        }
    }

    private void addReportLine(QAReport report, GKInstance instance,
            String attribute, String otherAttribute, GKInstance other) {
        String issue = ISSUE + " from " + attribute + " and" + otherAttribute +
                " to " + other;
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        issue,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}