package org.reactome.release.qa.diagram;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.schema.InvalidAttributeException;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the slice QA adaptation of the diagram-converter T103 extra
 * reaction participant diagram check. This check reports db ids which
 * are represented as a reaction participant in a diagram but are not
 * a participant in the database pathway event hierarchy, exclusive of
 * subpathways which have there own diagram.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramExtraParticipantCheck extends AbstractDiagramQACheck {
    
    private static final List<String> HEADERS = Arrays.asList(
            "PathwayDiagram_DBID",
            "Pathway_DisplayName",
            "Pathway_DBID",
            "Entity_DBID",
            "MostRecentAuthor");

    private final static String[] PATHWAY_LOAD_ATTS = {
            ReactomeJavaConstants.hasEvent
    };

    private final static String[] PATHWAY_REVERSE_LOAD_ATTS = {
            ReactomeJavaConstants.normalPathway
    };

    private final static String[] REACTION_PARTICIPANT_ATTS = {
            ReactomeJavaConstants.input,
            ReactomeJavaConstants.output
    };
    
    private final static Logger logger = Logger.getLogger(DiagramExtraParticipantCheck.class);
    
    public DiagramExtraParticipantCheck() {
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Collection<GKInstance> pathwayDiagrams = getPathwayDiagrams();
        Collection<GKInstance> diagrammed = new HashSet<GKInstance>(pathwayDiagrams.size());
        for (GKInstance pd: pathwayDiagrams) {
            diagrammed.add((GKInstance) pd.getAttributeValue(ReactomeJavaConstants.representedPathway));
        }
        DiagramGKBReader reader = new DiagramGKBReader();
        for (GKInstance diagram : pathwayDiagrams) {
            if (isHuman(diagram)) {
                checkPathwayDiagram(diagram, diagrammed, reader, report);
            }
        }
        report.setColumnHeaders(HEADERS);
        return report;
    }
    
    private void checkPathwayDiagram(GKInstance diagram, Collection<GKInstance> diagrammed,
            DiagramGKBReader reader, QAReport report) throws Exception {
        RenderablePathway pathway = reader.openDiagram(diagram);
        GKInstance pathwayInst =
                (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        Set<Long> allowed = getPathwayParticipantDbIds(pathwayInst, diagrammed);
        Set<Long> actual = getRenderableParticipantDbIds(pathway);
        Set<Long> invalid = new HashSet<Long>(actual);
        invalid.removeAll(allowed);
        String modDate = QACheckerHelper.getLastModificationAuthor(diagram);
        for (Long dbId: invalid) {
            report.addLine(diagram.getDBID().toString(),
                    pathwayInst.getDisplayName(),
                    pathwayInst.getDBID().toString(),
                    dbId.toString(),
                    modDate);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Set<Long> getRenderableParticipantDbIds(RenderablePathway pathway) {
        Set<Long> dbIds = new HashSet<Long>();
        List<Renderable> components = (List<Renderable>) pathway.getComponents();
        List<RenderableReaction> reactions = components.stream()
                .filter(RenderableReaction.class::isInstance)
                .map(RenderableReaction.class::cast)
                .collect(Collectors.toList());
        for (RenderableReaction reaction: reactions) {
            collectRenderableParticipantDbIds(reaction, dbIds);
        }
        
        return dbIds;
    }
    
    private void collectRenderableParticipantDbIds(RenderableReaction reaction, Set<Long> dbIds) {
        Long dbId = reaction.getReactomeId();
        if (dbId == null) {
            // Should not occur in practice, but it doesn't hurt to check.
            String msg = "RenderableReaction missing Reactome id: " +
                    reaction.getDisplayName() + "(component id " +
                    reaction.getID() + ")";
            logger.warn(msg);
            return;
        }
        List<Node> inputs = reaction.getInputNodes();
        for (Node input: inputs) {
            dbIds.add(input.getReactomeId());
        }
        List<Node> outputs = reaction.getOutputNodes();
        for (Node output: outputs) {
            dbIds.add(output.getReactomeId());
        }
    }

    /**
     * Returns the DB ids of the pathway hierarchy event participants which
     * are not represented in a separate pathway diagram. Subpathways are
     * visited if and only if they do not have their own diagram.
     * 
     * @param pathway the pathway to traverse
     * @param diagrammed 
     * @return the participant DB ids
     * @throws Exception
     */
    private Set<Long> getPathwayParticipantDbIds(GKInstance pathway,
            Collection<GKInstance> diagrammed) throws Exception {
        Set<Long> dbIds = new HashSet<Long>();
        collectPathwayParticipantDbIds(pathway, diagrammed, dbIds);
        
        return dbIds;
    }

    /**
     * Returns the DB ids of event participants which are not represented
     * in a separate pathway diagram. Subpathways are visited if and only
     * if they do not have their own diagram.
     * @param diagrammed 
     * 
     * @param pathwayInst
     * @return the {DB id: DB instance} map
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void collectPathwayParticipantDbIds(GKInstance pathway,
            Collection<GKInstance> diagrammed, Collection<Long> dbIds) throws Exception {
        List<GKInstance> subevents =
                pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
        Map<String, List<GKInstance>> partition = subevents.stream()
                .collect(Collectors.groupingBy(ev -> ev.getSchemClass().getName()));
        for (Entry<String, List<GKInstance>> entry: partition.entrySet()) {
            String clsName = entry.getKey();
            List<GKInstance> group = entry.getValue();
            if (ReactomeJavaConstants.Pathway.equals(clsName)) {
                collectPathwayParticipantDbIds(group, diagrammed, dbIds);
            } else {
                collectRLEParticipantDbIds(group, dbIds);
            }
        }
        // A normal pathway can include disease pathway participants as well.
        Collection<GKInstance> diseasePathways =
                (Collection<GKInstance>) pathway.getReferers(ReactomeJavaConstants.normalPathway);
        if (diseasePathways != null) {
            for (GKInstance diseasePathway: diseasePathways) {
                // Recurse into the disease pathway.
                collectPathwayParticipantDbIds(diseasePathway, diagrammed, dbIds);
            }
        }
    }

    private void collectPathwayParticipantDbIds(List<GKInstance> pathways,
            Collection<GKInstance> diagrammed, Collection<Long> dbIds) throws Exception {
        // Recurse to the undiagrammed subpathways.
        List<GKInstance> undiagrammed = pathways.stream()
                .filter(subpathway -> !diagrammed.contains(subpathway))
                .collect(Collectors.toList());
        dba.loadInstanceAttributeValues(undiagrammed, PATHWAY_LOAD_ATTS);
        dba.loadInstanceReverseAttributeValues(undiagrammed, PATHWAY_REVERSE_LOAD_ATTS);
        for (GKInstance subpathway: undiagrammed) {
            collectPathwayParticipantDbIds(subpathway, diagrammed, dbIds);
        }
    }

    @SuppressWarnings("unchecked")
    private void collectRLEParticipantDbIds(List<GKInstance> group, Collection<Long> dbIds)
            throws Exception, InvalidAttributeException {
        // Add the participant ids.
        dba.loadInstanceAttributeValues(group, REACTION_PARTICIPANT_ATTS);
        for (GKInstance rle: group) {
            List<GKInstance> inputs =
                    rle.getAttributeValuesList(ReactomeJavaConstants.input);
            for (GKInstance input: inputs) {
                dbIds.add(input.getDBID());
            }
            List<GKInstance> outputs =
                    rle.getAttributeValuesList(ReactomeJavaConstants.output);
            for (GKInstance output: outputs) {
                dbIds.add(output.getDBID());
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Diagram_Extra_Participants";
    }

}
