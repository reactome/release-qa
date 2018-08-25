package org.reactome.release.qa.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T053_PrecedingEventOutputsNotUsedInReaction extends AbstractQACheck {

    private static final String ISSUE = "No input is an output of the preceding event ";

    private static final String SQL =
            "SELECT DISTINCT ep.DB_ID, ep.precedingEvent" + 
            " FROM Event_2_precedingEvent ep" + 
            " WHERE NOT EXISTS (" + 
            "   SELECT 1 FROM Event_2_inferredFrom ei WHERE ei.DB_ID = ep.DB_ID" + 
            " )" + 
            " AND NOT EXISTS (" + 
            "   SELECT 1 FROM ReactionlikeEvent_2_input ri, ReactionlikeEvent_2_output ro" + 
            "   WHERE ep.DB_ID = ri.DB_ID AND ep.precedingEvent = ro.DB_ID" + 
            "   AND ri.input = ro.output" + 
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
            GKInstance instance = dba.fetchInstance(dbId);
            Long precedingDbId = new Long(rs.getLong(2));
            GKInstance preceding = dba.fetchInstance(precedingDbId);
            // Check for a catalyst entity or regulator match.
            if (!usesOutputIndirectly(instance, preceding)) {
                addReportLine(report, instance, preceding);
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }
    
    private boolean usesOutputIndirectly(GKInstance event, GKInstance preceding)
            throws InvalidAttributeException, Exception {
        if (!preceding.getSchemClass().isValidAttribute(ReactomeJavaConstants.output)) {
            // No output, ergo not used.
            return false;
        }
        // Delegate to a recursive visitor.
        IndirectOutputChecker checker = new IndirectOutputChecker(preceding);
        return checker.usesEvent(event);
    }
    
    private void addReportLine(QAReport report, GKInstance instance, GKInstance preceding) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        ISSUE + preceding,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }
    
    private static class IndirectOutputChecker {
        
        private static final String[] EXPANSION_ATTS = {
                ReactomeJavaConstants.hasMember,
                ReactomeJavaConstants.hasCandidate
        };
        
        Set<GKInstance> outputs;
        
        Set<GKInstance> visited;

        @SuppressWarnings({ "unchecked" })
        IndirectOutputChecker(GKInstance preceding) throws Exception {
            outputs = new HashSet<GKInstance>();
            Collection<GKInstance> directOutputs =
                    preceding.getAttributeValuesList(ReactomeJavaConstants.output);
           collectIndirectOutputs(directOutputs);
           this.visited = new HashSet<GKInstance>();
        }
        
        private void collectIndirectOutputs(Collection<GKInstance> instances)
                throws Exception {
            for (GKInstance instance: instances) {
                collectIndirectOutputs(instance);
            }
        }
        
        @SuppressWarnings("unchecked")
        private void collectIndirectOutputs(GKInstance instance) throws Exception {
            if (outputs.contains(instance)) {
                return;
            }
            outputs.add(instance);
            for (String attName: EXPANSION_ATTS) {
                if (instance.getSchemClass().isValidAttribute(attName)) {
                    List<GKInstance> children = instance.getAttributeValuesList(attName);
                    collectIndirectOutputs(children);
                }
            }
        }
        
        /**
         * @param event the instance to check
         * @return whether the instance is used directly or indirectly as an output
         * @throws Exception
         */
        boolean usesEvent(GKInstance event) throws Exception {
            // Forestall an infinite loop by verifying that we haven't yet seen
            // the instance. This would occur in the situation when an ancestor
            // is a child in a hierarchy. That situation is rare but could occur
            // and would be caught in a separate QA check.
            if (visited.contains(event)) {
                return false;
            }
            visited.add(event);
            
            return usesInput(event) ||
                    usesCatalyst(event) ||
                    usesRegulator(event);
        }
        
        /**
         * @param entity the instance to check
         * @return whether the instance is used directly or indirectly as an output
         * @throws Exception
         */
        boolean usesEntity(GKInstance entity) throws Exception {
            // Forestall an infinite loop by verifying that we haven't yet seen
            // the instance. This would occur in the situation when an ancestor
            // is a child in a hierarchy. That situation is rare but could occur
            // and would be caught in a separate QA check.
            if (visited.contains(entity)) {
                return false;
            }
            visited.add(entity);
            
            return outputs.contains(entity) ||
                    usesMember(entity) ||
                    usesCandidate(entity);
        }
        
        @SuppressWarnings({ "unchecked" })
        private boolean usesCatalyst(GKInstance event) throws Exception {
            if (!event.getSchemClass().isValidAttribute(ReactomeJavaConstants.catalystActivity)) {
                return false;
            }
            List<GKInstance> catActs = event.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
            for (GKInstance catAct: catActs) {
                GKInstance catEntity =
                        (GKInstance) catAct.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (usesEntity(catEntity) || usesEvent(event)) {
                    return true;
                }
            }
        
            return false;
        }
        
        @SuppressWarnings({ "unchecked" })
        private boolean usesRegulator(GKInstance event) throws Exception {
            if (!event.getSchemClass().isValidAttribute(ReactomeJavaConstants.regulatedBy)) {
                return false;
            }
            List<GKInstance> regulations = event.getAttributeValuesList(ReactomeJavaConstants.regulatedBy);
            for (GKInstance regulation: regulations) {
                GKInstance regulator =
                        (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                if (usesEntity(regulator) || usesEvent(event)) {
                    return true;
                }
            }
    
            return false;
        }
        
        @SuppressWarnings("unchecked")
        private boolean usesInput(GKInstance event) throws Exception {
            if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.input)) {
                List<GKInstance> inputs =
                        event.getAttributeValuesList(ReactomeJavaConstants.input);
                for (GKInstance input: inputs) {
                    if (usesEntity(input)) {
                       return true;
                    }
                }
            }
            
            return false;
        }
        
        @SuppressWarnings("unchecked")
        private boolean usesMember(GKInstance entity) throws Exception {
            if (entity.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
                List<GKInstance> members =
                        entity.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                for (GKInstance member: members) {
                    if (usesEntity(member)) {
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        @SuppressWarnings("unchecked")
        private boolean usesCandidate(GKInstance entity) throws Exception {
            if (entity.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
                List<GKInstance> candidates =
                        entity.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
                for (GKInstance candidate: candidates) {
                    if (usesEntity(candidate)) {
                        return true;
                    }
                }
            }
    
            return false;
        }

    }

}
