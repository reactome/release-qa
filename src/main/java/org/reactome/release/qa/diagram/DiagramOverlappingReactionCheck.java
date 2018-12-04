package org.reactome.release.qa.diagram;

import java.awt.Point;
import java.awt.Rectangle;
import org.gk.render.DefaultRenderConstants;
import org.gk.render.Renderable;
import org.gk.render.RenderableReaction;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckProperties;

/**
 * This is the slice QA adaptation of the diagram-converter T109 overlaping
 * reaction check.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramOverlappingReactionCheck extends DiagramOverlapCheck {

    private static final String TOLERANCE_PROP = "diagram.reaction.overlap.tolerance";

    private final static Float TOLERANCE = QACheckProperties.getFloat(TOLERANCE_PROP);
     
    public DiagramOverlappingReactionCheck() {
        super(cmpnt -> cmpnt instanceof RenderableReaction, TOLERANCE);
    }

    @Override
    public String getDisplayName() {
        return "Diagram_Overlapping_Reactions";
    }

    @Override
    protected Rectangle getBounds(Renderable renderable) {
        int w = DefaultRenderConstants.EDGE_TYPE_WIDGET_WIDTH;
        Point pos = renderable.getPosition();
        Rectangle bounds = new Rectangle(pos.x - w / 2, pos.y - w / 2, w, w);
        return bounds;
    }

}
