package org.reactome.release.qa.diagram;

import java.awt.Rectangle;
import java.util.function.Predicate;

import org.gk.render.HyperEdge;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderableGene;
import org.gk.render.RenderablePathway;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckProperties;

/**
 * This is the slice QA adaptation of the diagram-converter T110 overlaping
 * entity check.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramOverlappingEntityCheck extends DiagramOverlapCheck {
    
    private static final String TOLERANCE_PROP = "diagram.entity.overlap.tolerance";
    
    protected static final Double TOLERANCE = QACheckProperties.getDouble(TOLERANCE_PROP);
    
    private static Predicate<? super Renderable> createFilter() {
        Predicate<? super Renderable> skip =
                cmpnt -> cmpnt.getReactomeId() == null ||
                         cmpnt instanceof HyperEdge ||
                         cmpnt instanceof RenderableCompartment || // We want to check complexes too
                         cmpnt instanceof RenderablePathway;
        return skip.negate();
    }
     
    public DiagramOverlappingEntityCheck() {
        super(createFilter(), TOLERANCE);
    }

    @Override
    public String getDisplayName() {
        return "Diagram_Overlapping_Entities";
    }

    @Override
    protected Rectangle getBounds(Renderable renderable) {
        Rectangle bounds = renderable.getBounds();
        // Genes have a peculiar shape such that most of the
        // bounding box above the text is empty. Therefore,
        // only check the text area. Reverse-engineering
        // DefaultGeneRenderer.drawGeneSymbol(), the empty
        // portion (i.e., the ullage) extends upward by half
        // of the gene symbol width. Note that "symbol" in
        // that method refers to the glyph drawn to represent
        // the gene, not the gene name. Note also that the
        // method's bounds parameter height is not the gene
        // symbol bounds height. The bounds height is not used
        // in the method. Rather, the ullage described above is
        // the true gene symbol height. The text label is drawn
        // below the gene symbol. Therefore, the text bounds
        // are what is left over from the bounds after the ullage
        // is removed.
        if (renderable instanceof RenderableGene) {
            int ullage = (int) (RenderableGene.GENE_SYMBOL_WIDTH / 2.0);
            int height = bounds.height - ullage;
            return new Rectangle(bounds.x, bounds.y, bounds.width, height);
        }

        return bounds;
    }

}
