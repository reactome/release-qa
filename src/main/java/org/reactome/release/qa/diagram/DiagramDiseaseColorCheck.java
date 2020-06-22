package org.reactome.release.qa.diagram;

import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.DefaultRenderConstants;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableEntitySet;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * QA check to flag any oddities in disease reaction coloring.
 * The reaction edges of an RLE with a disease tag should be red.
 * Likewise if an RLE is not tagged with disease, reaction edges should be black.
 * Disease-tagged entities and/or entities with non-human species (or a relatedSpecies attribute) should colored red.
 * RLEs QA consideration: manually applied colors may differ. RGB comparison to make sure same color is applied.
 * Have predefined color for certain entities.
 */
@DiagramQACheck
public class DiagramDiseaseColorCheck extends AbstractDiagramQACheck {

    public DiagramDiseaseColorCheck() {
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
                                "Reaction_DisplayName",
                                "Reaction_DBID",
                                "MostRecentAuthor");
        return report;
    }

    private void checkPathwayDiagram(GKInstance diagram, DiagramGKBReader reader, QAReport report) throws Exception {
        RenderablePathway pathway = reader.openDiagram(diagram);
        String modDate = QACheckerHelper.getLastModificationAuthor(diagram);
        List<Renderable> components = pathway.getComponents();
        if (components == null || components.size() == 0)
            return;
        // For all rendered components.
        for (Renderable component : components) {
            boolean hasCorrectColors = true;

            // Check correctness of disease outline.
            if (component instanceof RenderableReaction || component instanceof RenderableEntitySet || component instanceof RenderableComplex)
                hasCorrectColors = hasCorrectColors(component);

            // Continue if there is no line to be added.
            if (hasCorrectColors) continue;

            // Otherwise, add line to report.
            report.addLine(diagram.getDBID().toString(),
                           pathway.getDisplayName(),
                           pathway.getReactomeDiagramId().toString(),
                           component.getDisplayName(),
                           component.getReactomeId() + "",
                           modDate);
        }
    }

    /**
     * Check if a component has the appropriate foreground color for its disease status.
     * Run against diagrams with no 'disease' attribute. Will need to check on model level.
     *
     * Returns true is color matches the status.
     * Returns false if there is a mismatch error.
     *
     * @param component
     * @return boolean
     * @throws Exception
     */
    private boolean hasCorrectColors(Renderable component) throws Exception {
        GKInstance instance = dba.fetchInstance(component.getReactomeId());
        if (component.getReactomeId() == null || instance == null)
            return true;

        // Disease outline check.
        Object isForDisease = instance.getAttributeValue(ReactomeJavaConstants.disease);
        if (isForDisease == null)
            return true;

        Object lineColor = component.getLineColor();
        if (lineColor == null)
            return false;

        return lineColor.equals(DefaultRenderConstants.DEFAULT_DISEASE_LINE_COLOR);
    }

}
