package org.reactome.release.qa.diagram;

import java.awt.Point;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.ReactionType;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.util.DrawUtilities;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckProperties;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This QA check is a variation of the diagram-converter T107 check.
 * The T107 check detects an exact reaction participant/hub overlap.
 * This slice QA check detects overlaps within a tolerance which
 * would obfuscate the display. As of 01/2019, T107 reports issues
 * from the following diagram-converter bug:
 *
 * When there is only one output entity and no output hub different
 * from the reaction hub, CuratorTool formats the reaction type
 * symbol (e.g. association circle) at the midpoint of the input
 * hub-to-entity line segment, whereas diagram-converter abuts the
 * symbol against the output entity, thereby overlapping the output
 * arrow.
 * 
 * This bug is tracked as https://reactome.atlassian.net/browse/DEV-1788.
 *
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramReactionBranchCheck extends AbstractDiagramQACheck {

    /** The association/dissociation circle symbol radius. */
    private static final int CIRCLE_RADIUS = 2;

    private static final Logger logger = Logger.getLogger(DiagramOverlappingEntityCheck.class);

    private static final String TOLERANCE_PROP = "diagram.reaction.participant.hub.min.distance";

    private final static Integer TOLERANCE = QACheckProperties.getInteger(TOLERANCE_PROP);

    @Override
    public String getDisplayName() {
        return "Diagram_Reactions_With_Participant_Overlapping_Hub";
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Collection<GKInstance> pathwayDiagrams = getPathwayDiagrams();
        DiagramGKBReader reader = new DiagramGKBReader();
        for (GKInstance diagram : pathwayDiagrams) {
            checkPathwayDiagram(diagram, reader, report);
        }
        report.setColumnHeaders("PathwayDiagram_DBID",
                "Pathway_DisplayName",
                "Pathway_DBID",
                "Reaction_DBID",
                "Reaction_DisplayName",
                "Created",
                "Modified");
        
        return report;
    }

    private void checkPathwayDiagram(GKInstance pathwayDiagram, DiagramGKBReader reader, QAReport report)
            throws Exception {
        logger.debug("Checking " + pathwayDiagram.getDisplayName() + "...");
        RenderablePathway pathway = reader.openDiagram(pathwayDiagram);
        @SuppressWarnings("unchecked")
        List<Renderable> components = pathway.getComponents();
        List<RenderableReaction> rxns = components.stream()
                .filter(cmpnt -> cmpnt instanceof RenderableReaction)
                .map(renderable -> (RenderableReaction)renderable)
                .collect(Collectors.toList());
        GKInstance pathwayInst = (GKInstance)
                pathwayDiagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        GKInstance created = (GKInstance) pathwayDiagram.getAttributeValue("created");
        GKInstance modified = QACheckerHelper.getLastModification(pathwayDiagram);
        for (RenderableReaction rxn: rxns) {
            // Report the overlaps.
            if (isOverlapping(rxn)) {
                report.addLine(pathwayDiagram.getDBID().toString(),
                        pathwayInst.getDisplayName(),
                        pathwayInst.getDBID().toString(),
                        rxn.getReactomeId().toString(),
                        rxn.getDisplayName(),
                        created.getDisplayName(),
                        modified.getDisplayName());
            }
        }
    }

    private boolean isOverlapping(RenderableReaction rxn) {
        return isInputOverlapping(rxn) ||
                isOutputOverlapping(rxn) ||
                isOverlapping(rxn.getPosition(), rxn.getActivatorPoints()) ||
                isOverlapping(rxn.getPosition(), rxn.getInhibitorPoints());
    }

    private boolean isInputOverlapping(RenderableReaction rxn) {
        int tolerance = TOLERANCE == null ? 0 : TOLERANCE;
        if (isReactionTypeSymbolDrawn(rxn)) {
            tolerance += CIRCLE_RADIUS;
            if (rxn.isNeedInputArrow()) {
                tolerance += DrawUtilities.ARROW_LENGTH;
            }
        }
        
        return isOverlapping(rxn.getInputHub(), rxn.getInputPoints(), tolerance);
    }

    private boolean isOutputOverlapping(RenderableReaction rxn) {
        int tolerance = TOLERANCE == null ? 0 : TOLERANCE;
        if (isReactionTypeSymbolDrawn(rxn)) {
            tolerance += CIRCLE_RADIUS;
            if (rxn.isNeedOutputArrow()) {
                tolerance += DrawUtilities.ARROW_LENGTH;
            }
        }
        
        return isOverlapping(rxn.getOutputHub(), rxn.getOutputPoints(), tolerance);
    }

    private boolean isReactionTypeSymbolDrawn(RenderableReaction rxn) {
        ReactionType rxnType = rxn.getReactionType();
        return rxnType == ReactionType.ASSOCIATION || rxnType == ReactionType.DISSOCIATION;
    }

    private boolean isOverlapping(Point hub, List<List<Point>> branches) {
        return isOverlapping(hub, branches, TOLERANCE);
    }

    private boolean isOverlapping(Point hub, List<List<Point>> branches, Integer tolerance) {
        return branches != null &&
                branches.stream().anyMatch(branch -> isOverlapping(hub, branch.get(0), tolerance));
    }

    private boolean isOverlapping(Point hub, Point point, Integer tolerance) {
        if (tolerance == null) {
            return hub.equals(point);
        } else {
            double distance = hub.distance(point);
            return distance <= tolerance;
        }
    }

}