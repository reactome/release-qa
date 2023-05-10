package org.reactome.release.qa.diagram;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * The base class for checking diagram rendering overlap. Subclasses are
 * responsible for providing a renderable filter, a bounds factory and
 * an optional overlap tolerance. For each pathway diagram, the filter
 * selects which diagram components to compare. Then the chosen
 * components bounds are checked for overlap. If there is a tolerance,
 * the overlap is reported if and only if the overlap exceeds the
 * tolerance. The default is no tolerance, i.e. any overlap is reported.
 *
 * @author Fred Loney <loneyf@ohsu.edu>
 */
public abstract class DiagramOverlapCheck extends AbstractDiagramQACheck {

    private static final Logger logger = Logger.getLogger(DiagramOverlapCheck.class);
    
    private Predicate<? super Renderable> filter;

    private Double tolerance;
    
    protected DiagramOverlapCheck(Predicate<? super Renderable> filter, Double tolerance) {
        this.filter = filter;
        this.tolerance = tolerance;
    }
    
    protected DiagramOverlapCheck(Predicate<? super Renderable> filter) {
        this(filter, null);
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
                "Overlapping_DBIDs",
                "Overlapping_DisplayNames",
                "Created",
                "Modified");
        
        return report;
    }

    abstract protected Rectangle getBounds(Renderable renderable);

    private void checkPathwayDiagram(GKInstance pathwayDiagram, DiagramGKBReader reader, QAReport report)
            throws Exception {
        // Skip disease pathways.
        @SuppressWarnings("unchecked")
        List<GKInstance> pathwayInsts = (List<GKInstance>)
                pathwayDiagram.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
        for (GKInstance pathwayInst: pathwayInsts) {
            GKInstance normal =
                    (GKInstance) pathwayInst.getAttributeValue(ReactomeJavaConstants.normalPathway);
            if (normal != null || isEscaped(pathwayDiagram)) {
                return;
            }
        }
        // Not a disease pathway; take the first pathway as representative.
        GKInstance pathwayInst = pathwayInsts.get(0);
        logger.info("Checking " + pathwayDiagram.getDisplayName() + "...");
        RenderablePathway pathway = reader.openDiagram(pathwayDiagram);
        @SuppressWarnings("unchecked")
        List<Renderable> components = pathway.getComponents();
        List<Renderable> filtered = components.stream()
                .filter(filter)
                .collect(Collectors.toList());
        
        // Check for overlaps.
        List<Renderable> overlaps = new ArrayList<Renderable>();
        for (int i = 0; i < filtered.size() - 1; i++) {
            Renderable renderable = filtered.get(i);
            overlaps.clear();
            for (int j = i + 1; j < filtered.size(); j++) {
                Renderable other = filtered.get(j);
                if (isOverlapping(renderable, other)) {
                    overlaps.add(other);
                }
            }
            if (!overlaps.isEmpty()) {
                overlaps.add(0, renderable);
                String overlapIds = overlaps.stream()
                        .map(Renderable::getReactomeId)
                        .map(dbId -> dbId == null ? "unknown" :  dbId.toString())
                        .collect(Collectors.joining("|"));
                String overlapDisplayNames = overlaps.stream()
                        .map(Renderable::getDisplayName)
                        .collect(Collectors.joining("|"));
                GKInstance created = (GKInstance) pathwayDiagram.getAttributeValue("created");
                GKInstance modified = QACheckerHelper.getLastModification(pathwayDiagram);
                report.addLine(pathwayDiagram.getDBID().toString(),
                        pathwayInst.getDisplayName(),
                        pathwayInst.getDBID().toString(),
                        overlapIds,
                        overlapDisplayNames,
                        created.getDisplayName(),
                        modified.getDisplayName());
            }
        }
    }

    private boolean isOverlapping(Renderable renderable, Renderable other) {
        logger.debug("isOverlapping: " + renderable.getDisplayName() + " (" + renderable.getID() + ") and " +
                                         other.getDisplayName() + " (" + other.getID() + ").");
        Rectangle bounds = getBounds(renderable);
        Rectangle otherBounds = getBounds(other);
    
        if (bounds.intersects(otherBounds)) {
            if (tolerance == null) {
                return true;
            }
            Rectangle intersection = bounds.intersection(otherBounds);
            double intArea = intersection.getHeight() * intersection.getWidth();
            double area = bounds.getHeight() * bounds.getWidth();
            double otherArea = otherBounds.getHeight() * otherBounds.getWidth();
            double toleratedArea = Math.min(area, otherArea) * tolerance;
            return intArea > toleratedArea;
        }
        
        return false;
    }

}