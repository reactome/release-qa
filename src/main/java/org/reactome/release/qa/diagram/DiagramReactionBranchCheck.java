package org.reactome.release.qa.diagram;

import java.awt.Point;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckProperties;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the slice QA adaptation of the diagram-converter T107
 * reaction branch participant/hub overlap check.
 *
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramReactionBranchCheck extends AbstractDiagramQACheck {

    private static final Logger logger = Logger.getLogger(DiagramOverlappingEntityCheck.class);

    private static final String TOLERANCE_PROP = "diagram.reaction.participant.hub.min.distance";

    private final static Float TOLERANCE = QACheckProperties.getFloat(TOLERANCE_PROP);

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
        GKInstance instance = (GKInstance)
                pathwayDiagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        GKInstance created = (GKInstance) pathwayDiagram.getAttributeValue("created");
        GKInstance modified = InstanceUtilities.getLatestCuratorIEFromInstance(pathwayDiagram);
        for (RenderableReaction rxn: rxns) {
            // Report the overlaps.
            if (isOverlapping(rxn)) {
                report.addLine(pathwayDiagram.getDBID().toString(),
                        instance.getDisplayName(),
                        instance.getDBID().toString(),
                        rxn.getReactomeId().toString(),
                        rxn.getDisplayName(),
                        created.getDisplayName(),
                        modified.getDisplayName());
            }
        }
    }

    private boolean isOverlapping(RenderableReaction rxn) {
        return isOverlapping(rxn.getInputHub(), rxn.getInputPoints()) ||
                isOverlapping(rxn.getOutputHub(), rxn.getOutputPoints()) ||
                isOverlapping(rxn.getPosition(), rxn.getActivatorPoints()) ||
                isOverlapping(rxn.getPosition(), rxn.getInhibitorPoints());
    }

    private boolean isOverlapping(Point hub, List<List<Point>> branches) {
        return branches != null &&
                branches.stream().anyMatch(branch -> isOverlapping(hub, branch.get(0)));
    }

    private boolean isOverlapping(Point hub, Point point) {
        if (TOLERANCE == null) {
            return hub.equals(point);
        } else {
            return hub.distance(point) <= TOLERANCE;
        }
    }

}