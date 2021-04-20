package org.reactome.release.qa.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.annotations.GraphQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.JavaConstants;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

@GraphQACheck
public class PhysicalEntitiesCompartmentCheck extends AbstractQACheck {

    private final static String LOAD_ATTS[] = {ReactomeJavaConstants.inferredFrom, JavaConstants.entityOnOtherCell};

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "MostRecentAuthor");


    @Override
    public String getDisplayName() {
        return "PhysicalEntity_With_More_Than_One_Compartment";
    }

    @Override
    @SuppressWarnings("unchecked")
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();


        for (SchemaClass sc : ((Collection<SchemaClass>) this.dba.getSchema().getClasses()))
        {
            // we can't check the compartment of PhysicalEntity, but we can check all subclasses that have a compartment attribute.
            if (sc.isa(ReactomeJavaConstants.PhysicalEntity))
            {
                boolean hasCompartment = ((Collection<GKSchemaAttribute>)sc.getAttributes()).stream().anyMatch(a -> a.getName().equals(ReactomeJavaConstants.compartment));
                if (hasCompartment)
                {
                    String e2c = QACheckerHelper.getAttributeTableName(sc.getName(),
                                                                       ReactomeJavaConstants.compartment,
                                                                       dba);
                    String sql = "SELECT DB_ID" +
                            " FROM " + e2c +
                            " GROUP BY DB_ID" +
                            " HAVING COUNT(" + ReactomeJavaConstants.compartment + ") > 1";
                    Connection conn = dba.getConnection();
                    try(PreparedStatement ps = conn.prepareStatement(sql))
                    {
                        ResultSet rs = ps.executeQuery();
                        List<Long> dbIds = new ArrayList<>();
                        while (rs.next()) {
                            dbIds.add(rs.getLong(1));
                        }

                        Collection<GKInstance> entities =
                                dba.fetchInstances(ReactomeJavaConstants.PhysicalEntity, dbIds);
                        dba.loadInstanceAttributeValues(entities, LOAD_ATTS);

                        for (GKInstance entity: entities) {
                            if (isEscaped(entity)) {
                                continue;
                            }
                            // Only report complexes.
                            if (!entity.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
                                continue;
                            }
                            GKInstance entityOnOtherCell = null;
                            if (entity.getSchemClass().isValidAttribute(JavaConstants.entityOnOtherCell))
                                entityOnOtherCell = (GKInstance) entity.getAttributeValue(JavaConstants.entityOnOtherCell);
                            if (entityOnOtherCell == null) {
                                report.addLine(entity.getDBID().toString(),
                                               entity.getDisplayName(),
                                               entity.getSchemClass().getName(),
                                               QACheckerHelper.getLastModificationAuthor(entity));
                            }
                        }
                    }
                }
            }
        }


        report.setColumnHeaders(HEADERS);

        return report;
    }

}