package org.reactome.release.qa.diagram;

import java.awt.Rectangle;
import java.util.function.Predicate;

import org.gk.render.ContainerNode;
import org.gk.render.HyperEdge;
import org.gk.render.Renderable;
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
    
    private static final Float TOLERANCE = QACheckProperties.getFloat(TOLERANCE_PROP);
    
    private static Predicate<? super Renderable> createFilter() {
        Predicate<? super Renderable> skip =
                cmpnt -> cmpnt.getReactomeId() == null ||
                        cmpnt instanceof HyperEdge ||
                        cmpnt instanceof ContainerNode;
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
        return bounds;
    }

}
