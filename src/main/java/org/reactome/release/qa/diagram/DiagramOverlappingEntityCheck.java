package org.reactome.release.qa.diagram;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.ContainerNode;
import org.gk.render.HyperEdge;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QACheckProperties;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the slice QA adaptation of the diagram-converter T110 overlaping
 * entity check.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramOverlappingEntityCheck extends AbstractDiagramQACheck {
    
    private final static Logger logger = Logger.getLogger(DiagramOverlappingEntityCheck.class);

    private static final String TOLERANCE_PROP = "diagram.entity.overlap.tolerance";

    private final static Float TOLERANCE = QACheckProperties.getFloat(TOLERANCE_PROP);
    
    public DiagramOverlappingEntityCheck() {
    }

    @Override
    public String getDisplayName() {
        return "Diagram_Overlapping_Entities";
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Collection<GKInstance> pathwayDiagrams = getPathwayDiagrams();
        DiagramGKBReader reader = new DiagramGKBReader();
        for (GKInstance diagram : pathwayDiagrams) {
            checkPathwayDiagram(diagram, reader, report);
        }
        report.setColumnHeaders(Arrays.asList("PathwayDiagram_DBID",
                "Pathway_DisplayName",
                "Pathway_DBID",
                "Overlapping_DBIDs",
                "Overlapping_DisplayNames",
                "MostRecentAuthor"));
        return report;
    }

    private void checkPathwayDiagram(GKInstance pathwayDiagram,
            DiagramGKBReader reader,
            QAReport report) throws Exception {
        logger.info("Checking " + pathwayDiagram.getDisplayName() + "...");
        RenderablePathway pathway = reader.openDiagram(pathwayDiagram);
        @SuppressWarnings("unchecked")
        List<Renderable> components = pathway.getComponents();
        Predicate<? super Renderable> skip =
                cmpnt -> cmpnt.getReactomeId() == null ||
                        cmpnt instanceof HyperEdge ||
                        cmpnt instanceof ContainerNode;
        List<Renderable> filtered = components.stream()
                .filter(skip.negate())
                .collect(Collectors.toList());
        List<Renderable> overlaps = new ArrayList<Renderable>();
        for (int i = 0; i < filtered.size(); i++) {
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
                report.addLine(pathwayDiagram.getDBID().toString(),
                        overlapIds,
                        overlapDisplayNames,
                        QACheckerHelper.getLastModificationAuthor(pathwayDiagram));
            }
        }
    }

    private boolean isOverlapping(Renderable renderable, Renderable other) {
        Rectangle bounds = renderable.getBounds();
        Rectangle otherBounds = other.getBounds();

        if (bounds.intersects(otherBounds)) {
            if (TOLERANCE == null) {
                return true;
            }
            Rectangle intersection = bounds.intersection(otherBounds);
            double intArea = intersection.getHeight() * intersection.getWidth();
            double area = bounds.getHeight() * bounds.getWidth();
            double otherArea = otherBounds.getHeight() * otherBounds.getWidth();
            double toleratedArea = Math.min(area, otherArea) * TOLERANCE;
            return intArea > toleratedArea;
        }
        
        return false;
    }

}
