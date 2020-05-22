package org.reactome.release.qa.diagram;

import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.DefaultRenderConstants;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableEntitySet;
import org.gk.render.RenderablePathway;
import org.reactome.release.qa.annotations.ReleaseQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * QA check to flag any oddities in drug coloring.
 */
@ReleaseQACheck
public class DiagramDrugColorCheck extends AbstractDiagramQACheck {

    public DiagramDrugColorCheck() {
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

            // Check correctness of drug background.
            if (component instanceof RenderableEntitySet ||
                component instanceof RenderableComplex)
                addLine = checkDrug(component);

            // Continue if there is no line to be added.
            if (!addLine) continue;

            // Otherwise, add line to report.
            report.addLine(diagram.getDBID().toString(),
                           pathway.getDisplayName(),
                           pathway.getReactomeDiagramId().toString(),
                           component.getDisplayName(),
                           component.getReactomeId().toString(),
                           modDate);
        }
    }

    /**
     * Check if a component has the appropriate background color for its drug status.
     *
     * Returns true is color matches the status.
     * Returns false if there is a mismatch (error).
     *
     * @param component
     * @return boolean
     * @throws Exception
     */
    private boolean checkDrug(Renderable component) throws Exception {
        GKInstance instance = dba.fetchInstance(component.getReactomeId());
        if (instance == null)
            return false;

        // Drug background check.
        if (InstanceUtilities.hasDrug(instance)) {
            if (component.getBackgroundColor() != DefaultRenderConstants.DEFAULT_DRUG_BACKGROUND)
                return true;
        }
        else {
            if (component.getBackgroundColor() != DefaultRenderConstants.DEFAULT_BACKGROUND)
                return true;
        }
        return false;
    }

}
