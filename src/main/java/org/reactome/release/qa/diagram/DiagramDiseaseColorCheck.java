package org.reactome.release.qa.diagram;

import java.awt.Color;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.DefaultRenderConstants;
import org.gk.render.Renderable;
import org.gk.render.RenderableEntity;
import org.gk.render.RenderableEntitySet;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.reactome.release.qa.annotations.ReleaseQACheck;
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
@ReleaseQACheck
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
                                "Reaction_DBID",
                                "Reaction_DisplayName",
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
            boolean addLine = false;

            if (component instanceof RenderableReaction)
                addLine = checkRLE(component);

            else if (component instanceof RenderableEntitySet)
                addLine = checkEntitySet(component);

            if (!addLine) continue;

            report.addLine(diagram.getDBID().toString(),
                           pathway.getDisplayName(),
                           pathway.getReactomeDiagramId().toString(),
                           component.getDisplayName(),
                           component.getReactomeId().toString(),
                           modDate);
        }
    }

    private boolean checkRLE(Renderable component) {
        // Return false if component is not an RLE.
        if (!(component instanceof RenderableReaction))
            return false;

        boolean addLine = false;

        // If RLE is tagged with a disease, check that all reaction edges are red.
        if (component.getIsForDisease()) {
            if (component.getLineColor() != DefaultRenderConstants.DEFAULT_DISEASE_LINE_COLOR)
                addLine = true;
        }

        // If RLE is not tagged with a disease, check that all reaction edges are black.
        else {
            if (component.getLineColor() != Color.black)
                addLine = true;
        }

        return addLine;
    }

    private boolean checkEntitySet(Renderable component) throws Exception {
        // Return false if component is not an Entity.
        if (!(component instanceof RenderableEntitySet))
            return false;

        boolean addLine = false;

        // If entity is tagged with a disease, check that it is colored red.
        if (component.getIsForDisease()) {
            if (component.getBackgroundColor() != DefaultRenderConstants.DEFAULT_DISEASE_LINE_COLOR)
                addLine = true;
        }

        // If entity is not tagged with a disease, check that it is colored with default background.
        else {
            if (component.getBackgroundColor() != DefaultRenderConstants.DEFAULT_BACKGROUND)
                addLine = true;
        }

        GKInstance instance = dba.fetchInstance(component.getReactomeId());
        if (instance == null)
            return false;

        GKInstance species = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
        GKInstance relatedSpecies = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.relatedSpecies);
        GKInstance human = dba.fetchInstance(48887L);
        if (species == null || relatedSpecies == null)
            return false;

        if (species.equals(human) || relatedSpecies.equals(human)) {
            if (component.getBackgroundColor() != DefaultRenderConstants.DEFAULT_DISEASE_LINE_COLOR)
                addLine = true;
        }
        else {
            if (component.getBackgroundColor() != DefaultRenderConstants.DEFAULT_BACKGROUND)
                addLine = true;
        }

        return addLine;
    }

}
