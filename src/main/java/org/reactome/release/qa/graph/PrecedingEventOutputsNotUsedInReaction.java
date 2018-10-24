package org.reactome.release.qa.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.GraphQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

@GraphQATest
@SuppressWarnings("unchecked")
public class PrecedingEventOutputsNotUsedInReaction extends AbstractQACheck {
    private static final Logger logger = Logger.getLogger(PrecedingEventOutputsNotUsedInReaction.class);

    private static final String SQL =
            "SELECT DISTINCT ep.DB_ID, ep.precedingEvent" + 
            " FROM Event_2_precedingEvent ep" + 
            " WHERE NOT EXISTS (" + 
            "   SELECT 1 FROM ReactionlikeEvent_2_input ri, ReactionlikeEvent_2_output ro" + // Remove simple cases
            "   WHERE ep.DB_ID = ri.DB_ID AND ep.precedingEvent = ro.DB_ID" + 
            "   AND ri.input = ro.output" + 
            " )"; 

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "Class", "precedingDBID", "precedingDisplayName", "precedingClass", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return "PrecedingEvent_Output_Not_Used_In_Reaction";
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Connection conn = dba.getConnection();
        PreparedStatement ps = conn.prepareStatement(SQL);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Long dbId = new Long(rs.getLong(1));
            GKInstance following = dba.fetchInstance(dbId);
            if (isEscaped(following)) {
                continue;
            }
            Long precedingDbId = new Long(rs.getLong(2));
            GKInstance preceding = dba.fetchInstance(precedingDbId);
            if (preceding == null) {
                logger.error(precedingDbId + " for " + following + " doesn't exist in the DB!");
                continue;
            }
            // Check for a catalyst entity or regulator match.
            if (!usesOutputIndirectly(following, preceding)) {
                addReportLine(report, following, preceding);
            }
        }
        rs.close();
        ps.close();
        
        report.setColumnHeaders(HEADERS);

        return report;
    }
    
    private boolean usesOutputIndirectly(GKInstance following, GKInstance preceding) throws Exception {
        if (!preceding.getSchemClass().isValidAttribute(ReactomeJavaConstants.output)) {
            // No output, ergo not used.
            return false;
        }
        List<GKInstance> outputs = preceding.getAttributeValuesList(ReactomeJavaConstants.output);
        if (outputs == null || outputs.size() == 0)
            return false; // There is no possible
        Set<GKInstance> precedingOutputs = new HashSet<>(outputs);
        Set<GKInstance> followingLFHEntities = getLeftHandEntities(following);
        for (GKInstance precedingOutput : precedingOutputs) {
            for (GKInstance followingLFHEntity : followingLFHEntities) {
                if (isEquivalent(followingLFHEntity, precedingOutput))
                    return true;
            }
        }
        return false;
    }
    
    private boolean isEquivalent(GKInstance lfhEntity, GKInstance output) throws Exception {
        // If they are the same, return true
        if (lfhEntity == output)
            return true;
        if (output.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
            // If output is an EntitySet having lfhEntity as a member, return true
            Set<GKInstance> members = InstanceUtilities.getContainedInstances(output,
                                                                              ReactomeJavaConstants.hasMember,
                                                                              ReactomeJavaConstants.hasCandidate);
            if (members.contains(lfhEntity))
                return true;
            if (lfhEntity.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
                // If both are EntitySets having shared member, return true
                Set<GKInstance> lfhMembers = InstanceUtilities.getContainedInstances(lfhEntity,
                                                                                     ReactomeJavaConstants.hasMember,
                                                                                     ReactomeJavaConstants.hasCandidate);
                lfhMembers.retainAll(members);
                if (lfhMembers.size() > 0)
                    return true; // There is at least one shared member
            }
        }
        else if (lfhEntity.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
            // If both are EntitySets having shared member, return true
            Set<GKInstance> lfhMembers = InstanceUtilities.getContainedInstances(lfhEntity,
                                                                                 ReactomeJavaConstants.hasMember,
                                                                                 ReactomeJavaConstants.hasCandidate);
            if (lfhMembers.contains(output))
                return true;
        }
        return false;
    }

    private Set<GKInstance> getLeftHandEntities(GKInstance following) throws Exception {
        Set<GKInstance> rtn = new HashSet<GKInstance>();
        if (!following.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
            return rtn;
        List<GKInstance> input = following.getAttributeValuesList(ReactomeJavaConstants.input);
        if (input != null)
            rtn.addAll(input);
        GKInstance cas = (GKInstance) following.getAttributeValue(ReactomeJavaConstants.catalystActivity);
        if (cas != null) {
            GKInstance ca = (GKInstance) cas.getAttributeValue(ReactomeJavaConstants.physicalEntity);
            if (ca != null)
                rtn.add(ca);
        }
        Collection<GKInstance> regulations = following.getAttributeValuesList(ReactomeJavaConstants.regulatedBy);
        if (regulations != null && regulations.size() > 0) {
            for (GKInstance regulation : regulations) {
                // Want to use PositiveRegulation only
                if (!regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation))
                    continue;
                GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                if (regulator != null)
                    rtn.add(regulator);
            }
        }
        return rtn;
    }
    
    private void addReportLine(QAReport report, GKInstance instance, GKInstance preceding) {
        report.addLine(instance.getDBID().toString(), 
                instance.getDisplayName(), 
                instance.getSchemClass().getName(), 
                preceding.getDBID().toString(),
                preceding.getDisplayName(),
                preceding.getSchemClass().getName(),
                QACheckerHelper.getLastModificationAuthor(instance));
    }
}
