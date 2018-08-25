package org.reactome.release.qa.graph;

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
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T027_EntriesWithOtherCyclicRelations extends AbstractQACheck {
    
    private final static String ISSUE = "Cycle to this instance";
    
    /**
     * The attributes to skip when checking for x == x.attribute.attribute.
     */
    private final static String[] SKIP_INVOLUTORY_ATTS = {
            "hasEncapsulatedEvent", "precedingEvent", "inferredTo"
    };
    
    /* The attributes which are bi-directional symmetric relations in the
     * curator database but uni-directional in the graph db.
     */ 
    private final static String[] SYMMETRIC_ATTS = {
            "equivalentTo", "reverseReaction"
    };
    
    /**
     * The attributes to skip when checking for x == x.attribute1.attribute2.
     */
    private final static String[] SKIP_NONINVOLUTORY_ATTS = {
            "author", "created", "edited", "modified", "revised", "reviewed",
            "inferredTo", "hasPart", "precedingEvent", "hasEncapsulatedEvent"
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
        for (SchemaClass cls: classes) {
            Collection<SchemaAttribute> attributes = cls.getAttributes();
            for (SchemaAttribute att: attributes) {
                if (att.getOrigin() == cls) {
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
        SchemaClass[] valueClasses =
                valueClsAtts.keySet().toArray(new SchemaClass[valueClsAtts.size()]);
        
        // Second pass: check for involution cycles, i.e.
        // x == x.attribute.attribute.
        Set<String> skipSingleAttSet = Stream.of(SKIP_INVOLUTORY_ATTS)
                .collect(Collectors.toSet());
        // Skip equivalences as well.
        skipSingleAttSet.addAll(Arrays.asList(SYMMETRIC_ATTS));
        for (int i=0; i < valueClasses.length; i++) {
            SchemaClass valueCls = valueClasses[i];
            for (SchemaAttribute att: valueClsAtts.get(valueCls)) {
                SchemaClass originCls = att.getOrigin();
                if (skipSingleAttSet.contains(att.getName())) {
                    continue;
                }
                if (originCls.isa(valueCls) || valueCls.isa(originCls)) {
                    check(report, att);
                }
            }
        }

        // Third pass: check for non-involution cycles, i.e.
        // x == x.attribute1.attribute2.
        Set<String> skipMultiAttSet = Stream.of(SKIP_NONINVOLUTORY_ATTS)
                .collect(Collectors.toSet());
        for (int i=0; i < valueClasses.length; i++) {
            SchemaClass valueCls = valueClasses[i];
            // Filter out the skipped attributes.
            List<SchemaAttribute> unskipped = valueClsAtts.get(valueCls).stream()
                    .filter(att -> !skipMultiAttSet.contains(att.getName()))
                    .collect(Collectors.toList());
            // For each unskipped attribute, check any other attribute whose value
            // is compatible with the value of that unskipped attribute.
            for (int ia=0; ia < unskipped.size(); ia++) {
                SchemaAttribute att = unskipped.get(ia);
                SchemaClass originCls = att.getOrigin();
                for (int j=i; j < valueClasses.length; j++) {
                    SchemaClass otherCls = valueClasses[j];
                    // A cycle could occur only if the other attribute returns
                    // an instance compatible with the the initial origin class. 
                    if (otherCls.isa(originCls) || originCls.isa(otherCls)) {
                        List<SchemaAttribute> otherAtts;
                        int ja;
                        if (valueCls == otherCls) {
                            otherAtts = unskipped;
                            // Only check the value class's attributes which have
                            // not yet been checked.
                            ja = ia + 1;
                        } else {
                            Set<SchemaAttribute> otherAttSet =
                                    valueClsAtts.get(otherCls);
                           otherAtts = otherAttSet.stream()
                                   .filter(oAtt -> !skipMultiAttSet.contains(oAtt.getName()))
                                   .collect(Collectors.toList());
                            // Check all of the other class's unskipped attributes.
                            ja = 0;
                        }
                        for (; ja < otherAtts.size(); ja++) {
                            SchemaAttribute otherAtt = otherAtts.get(ja);
                            SchemaClass otherOriginCls = otherAtt.getOrigin();
                            // If the first attribute return value is compatible with
                            // the second attribute origin class, then check for a cycle.
                            if (otherOriginCls.isa(valueCls) || valueCls.isa(otherOriginCls)) {
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

    private void check(QAReport report, SchemaAttribute att) throws Exception {
        // The multi-attribute check works for this single-attribute check
        // as well.
        check(report, att, att);
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
                " WHERE a.DB_ID = b." + otherAttName +
                " AND b.DB_ID = a." + attName;
        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        Set<Long> visited = new HashSet<Long>();
        while (rs.next()) {
            Long dbId = new Long(rs.getLong(1));
            GKInstance instance = dba.fetchInstance(dbId);
            Long otherDbId = new Long(rs.getLong(2));
            // If a single-attribute relationship reverse has already,
            // been captured, then skip this report line.
            if (att == otherAtt) {
                if (visited.contains(otherDbId)) {
                    continue;
                } else {
                    visited.add(dbId);
                }
            }
            GKInstance other = dba.fetchInstance(otherDbId);
            addReportLine(report, instance, attName, otherAttName, other);
       }
    }

    private void addReportLine(QAReport report, GKInstance instance,
            String attribute, String otherAttribute, GKInstance other) {
        String issue = ISSUE + " through " + attribute + " -> " + other +
                " -> " + otherAttribute;
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        issue,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}