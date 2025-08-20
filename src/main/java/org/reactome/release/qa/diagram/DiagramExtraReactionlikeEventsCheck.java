package org.reactome.release.qa.diagram;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This checks is to find extra ReactionLikeEvents that are drawn but should not drawn in a pathway 
 * diagram. This usually should not occur since the slicing tool should have take a check.
 * The output from this check is different from the diagram-converter T103 extra
 * reaction participant diagram check, which checks extra PhysicalEntities (Nodes) only and should
 * be handled by Reaction synchronization check in the CuratorTool codebase.
 * 
 * @author Fred Loney <loneyf@ohsu.edu> & Guanming Wu <wug@ohsu.edu>
 */
@DiagramQACheck
@SuppressWarnings("unchecked")
public class DiagramExtraReactionlikeEventsCheck extends AbstractDiagramQACheck {
    
    private static final List<String> HEADERS = Arrays.asList(
            "PathwayDiagram_DBID",
            "Pathway_DisplayName",
            "Pathway_DBID",
            "RLE_DisplayName",
            "RLE_DBID",
            "MostRecentAuthor");
    
    public DiagramExtraReactionlikeEventsCheck() {
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
            // All diagrams will be checked, including diagrams for non-human pathways
            checkPathwayDiagram(diagram, reader, report);
        }
        report.setColumnHeaders(HEADERS);
        return report;
    }
    
    private void checkPathwayDiagram(GKInstance diagram, 
                                     DiagramGKBReader reader, 
                                     QAReport report) throws Exception {
        RenderablePathway pathway = reader.openDiagram(diagram);
        List<GKInstance> pathwayInsts = diagram.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
        Set<Long> drawnIds = getRenderableReactionDbIds(pathway);
        checkPathwayDiagram(pathwayInsts, drawnIds);
        String modDate = QACheckerHelper.getLastModificationAuthor(diagram);
        for (Long dbId: drawnIds) {
            GKInstance pathwayInst = pathwayInsts.get(0);
            System.out.println("Reporting for RLE with dbId of " + dbId);
            GKInstance rle = dba.fetchInstance(dbId);
            report.addLine(diagram.getDBID().toString(),
                           pathwayInst.getDisplayName(),
                           pathwayInst.getDBID().toString(),
                           rle.getDisplayName(),
                           dbId.toString(),
                           modDate);
        }
    }
    
    private void checkPathwayDiagram(List<GKInstance> pathways,
                                     Set<Long> dbIds) throws Exception {
        Set<GKInstance> containedEvents = new HashSet<>();
        for (GKInstance pathway : pathways) {
            Set<GKInstance> tmp = InstanceUtilities.getContainedEvents(pathway);
            containedEvents.addAll(tmp);
        }
        containedEvents.addAll(pathways);
        // Check pathways that are not drawn as ProcessNodes. We expect all its reactions will be drawn
        for (GKInstance event : containedEvents) {
            if (!event.getSchemClass().isa(ReactomeJavaConstants.Pathway))
                continue;
            if (dbIds.contains(event.getDBID())) {
                dbIds.remove(event.getDBID()); // Drawn as ProcessNode
            }
            else {
                // Remove all first level RLEs
                List<GKInstance> hasEvents = event.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
                for (GKInstance hasEvent : hasEvents) {
                    if (hasEvent.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
                        dbIds.remove(hasEvent.getDBID());
                }
            }
        }
        // If we still have anything left, they will be extra
    }

    private Set<Long> getRenderableReactionDbIds(RenderablePathway pathway) {
        List<Renderable> components = (List<Renderable>) pathway.getComponents();
        System.out.println("Pathway: " + pathway);

        Set<Long> dbIds = components.stream()
                .filter(r -> r.getReactomeId() != null)
                .filter(RenderableReaction.class :: isInstance)
                .map(r -> r.getReactomeId())
                .collect(Collectors.toSet());
        return dbIds;
    }
    
    @Override
    public String getDisplayName() {
        return "Diagram_Extra_ReactionLikeEvents";
    }

}
