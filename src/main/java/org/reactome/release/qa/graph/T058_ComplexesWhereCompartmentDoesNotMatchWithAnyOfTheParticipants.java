package org.reactome.release.qa.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T058_ComplexesWhereCompartmentDoesNotMatchWithAnyOfTheParticipants extends AbstractQACheck {

    private static final String ISSUE = "No matching participant component ";

    private static final String SQL =
            "SELECT DISTINCT pc.DB_ID" + 
            " FROM Complex c, PhysicalEntity_2_compartment pc" + 
            " WHERE c.DB_ID = pc.DB_ID AND NOT EXISTS (" + 
            "   SELECT 1 FROM Complex_2_hasComponent child, PhysicalEntity_2_compartment cc" + 
            "   WHERE pc.DB_ID = child.DB_ID" + 
            "     AND child.hasComponent = cc.DB_ID" + 
            "     AND pc.compartment = cc.compartment" + 
            " )"; 

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(SQL);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Long dbId = new Long(rs.getLong(1));
            GKInstance complex = dba.fetchInstance(dbId);
            // Check for a catalyst entity or regulator match.
            if (!usesCompartmentIndirectly(complex)) {
                addReportLine(report, complex);
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }
    
    private boolean usesCompartmentIndirectly(GKInstance complex)
            throws InvalidAttributeException, Exception {
        // Delegate to a recursive visitor.
        IndirectCompartmentChecker checker =
                new IndirectCompartmentChecker(complex);
        return checker.usesCompartment();
    }
    
    private void addReportLine(QAReport report, GKInstance complex) {
        report.addLine(
                Arrays.asList(complex.getDBID().toString(), 
                        complex.getDisplayName(), 
                        complex.getSchemClass().getName(), 
                        ISSUE,  
                        QACheckerHelper.getLastModificationAuthor(complex)));
    }
    
    private static class IndirectCompartmentChecker {
        
        private static final String[] EXPANSION_ATTS = {
                ReactomeJavaConstants.hasComponent,
                ReactomeJavaConstants.hasMember,
                ReactomeJavaConstants.hasCandidate,
                ReactomeJavaConstants.repeatedUnit
        };
        
        Set<GKInstance> compartments;
        
        Set<GKInstance> visited;

        private GKInstance complex;

        @SuppressWarnings({ "unchecked" })
        IndirectCompartmentChecker(GKInstance complex) throws Exception {
           this.complex = complex;
           List<GKInstance> compartmentsList =
                   complex.getAttributeValuesList(ReactomeJavaConstants.compartment);
           compartments = new HashSet<GKInstance>(compartmentsList);
           visited = new HashSet<GKInstance>();
        }
        
        /**
         * @return whether at least one of this checker's complex
         *   compartments is used as a compartment of the complex's
         *   children
         * @throws Exception
         */
        boolean usesCompartment() throws Exception {
            visited.add(complex);
            return childUsesCompartment(complex);
        }
        
        /**
         * @param entity the instance to check
         * @return whether there is at least one complex compartment
         *   that is used as a compartment of the given entity or
         *   its children
         * @throws Exception
         */
        @SuppressWarnings("unchecked")
        private boolean usesCompartment(GKInstance entity) throws Exception {
            // Forestall an infinite loop by verifying that we haven't yet seen
            // the instance. This would occur in the situation when an ancestor
            // is a child in a hierarchy. That situation is rare but could occur
            // and would be caught in a separate QA check.
            if (visited.contains(entity)) {
                return false;
            }
            visited.add(entity);
            
            // Check the direct entity compartments.
            List<GKInstance> cmpts =
                    entity.getAttributeValuesList(ReactomeJavaConstants.compartment);
            for (GKInstance cmpt: cmpts) {
                if (compartments.contains(cmpt)) {
                    return true;
                }
            }
            
            // Check children recursively.
            return childUsesCompartment(entity);
        }

        private boolean childUsesCompartment(GKInstance entity)
                throws Exception {
            for (String att: EXPANSION_ATTS) {
                if (childUsesCompartment(entity, att)) {
                    return true;
                }
            }

            return false;
        }

        @SuppressWarnings("unchecked")
        private boolean childUsesCompartment(GKInstance entity, String attribute)
                throws Exception {
            if (entity.getSchemClass().isValidAttribute(attribute)) {
                List<GKInstance> members =
                        entity.getAttributeValuesList(attribute);
                for (GKInstance member: members) {
                    if (usesCompartment(member)) {
                        return true;
                    }
                }
            }

            return false;
        }

    }

}
