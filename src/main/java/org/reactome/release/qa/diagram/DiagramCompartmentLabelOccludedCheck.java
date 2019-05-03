package org.reactome.release.qa.diagram;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathwaylayout.PathwayDiagramGeneratorViaAT;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckProperties;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * Check for diagram Compartment nodes whose label is significantly
 * occluded by another node. Only non-reaction database entity nodes
 * are checked. A compartment node A is checked for overlapping the
 * label of another compartment node B if and only if A is contained
 * in B or if A and B are different nodes for the same database
 * compartment object.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramCompartmentLabelOccludedCheck extends AbstractDiagramQACheck {

    private static final String TOLERANCE_PROP = "diagram.compartment.label.overlap.tolerance";

    private final static Integer TOLERANCE = QACheckProperties.getInteger(TOLERANCE_PROP);

    private static final String TOLERANCE_TOP_PROP = "diagram.compartment.label.overlap.tolerance.top";

    private final static Integer TOLERANCE_TOP = QACheckProperties.getInteger(TOLERANCE_TOP_PROP);

    private static final List<String> HEADERS = Arrays.asList(
            "PathwayDiagram_DBID",
            "Pathway_DisplayName",
            "Pathway_DBID",
            "Compartment_DBID",
            "Compartment_DisplayName",
            "Overlap_DBID",
            "Overlap_DisplayName",
            "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return "Diagram_Compartment_Label_Occluded";
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Collection<GKInstance> pathwayDiagrams = getPathwayDiagrams();
        DiagramGKBReader reader = new DiagramGKBReader();
        for (GKInstance pd: pathwayDiagrams) {
            checkPathwayDiagram(pd, reader, report);
        }
        report.setColumnHeaders(HEADERS);

        return report;
    }
    
    private void checkPathwayDiagram(GKInstance diagram, DiagramGKBReader reader, QAReport report)
            throws Exception {
        PathwayEditor pathwayEditor = new PathwayEditor();
        RenderablePathway pathway = reader.openDiagram(diagram);
        pathwayEditor.setRenderable(pathway);
        PathwayDiagramGeneratorViaAT helper = new PathwayDiagramGeneratorViaAT();
        BufferedImage img = helper.paintOnImage(pathwayEditor);
        Graphics g = img.createGraphics();
        @SuppressWarnings("unchecked")
        List<Renderable> componentNodes = pathway.getComponents();
        GKInstance pathwayInst =
                (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        // Check for a non-empty, non-occluded label.
        for (Renderable componentNode: componentNodes) {
            if (!(componentNode instanceof RenderableCompartment)) {
                continue;
            }
            Long dbId = componentNode.getReactomeId();
            if (dbId == null) {
                continue;
            }
            GKInstance compartmentInst = dba.fetchInstance(dbId);
            if (isEscaped(compartmentInst)) {
                continue;
            }
            if (componentNode.getDisplayName() == null) {
                // This is covered by another check.
                continue;
            }
            Renderable overlappingNode = findTextOcclusion(diagram, g, componentNode, componentNodes);
            if (overlappingNode != null) {
                String mod = QACheckerHelper.getLastModificationAuthor(diagram);
                long overlappingDbId = overlappingNode.getReactomeId();
                GKInstance overlappingInst = dba.fetchInstance(overlappingNode.getReactomeId());
                String overlappingDisplayNm = overlappingInst.getDisplayName();
                String componentDisplayNm = compartmentInst.getDisplayName();
                long compartmentDbId = compartmentInst.getDBID();
                if (overlappingDbId == compartmentDbId) {
                    overlappingDisplayNm += " (node id " + overlappingNode.getID() + ")";
                    componentDisplayNm += " (node id " + componentNode.getID() + ")";
                }
                report.addLine(diagram.getDBID().toString(),
                        pathwayInst.getDisplayName(),
                        pathwayInst.getDBID().toString(),
                        Long.toString(compartmentDbId),
                        componentDisplayNm,
                        Long.toString(overlappingDbId),
                        overlappingDisplayNm,
                        mod);
            }
        }
    }
    
    /**
     * @param diagram 
     * @param g 
     * @param compartment the diagram compartment to check
     * @param components all of the diagram components
     * @return a component that occludes the compartment label,
     *      or null if none
     * @throws Exception 
     */
    private Renderable findTextOcclusion(GKInstance diagram, Graphics g, Renderable compartment,
            List<Renderable> components) throws Exception {
        String label = compartment.getDisplayName();
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D layoutBnds = fm.getStringBounds(label, g);
        Rectangle textBnds = compartment.getTextBounds();
        int width = (int)layoutBnds.getWidth();
        int height = (int)layoutBnds.getHeight();
        Rectangle labelBnds = new Rectangle(textBnds.x, textBnds.y, width, height);
        Rectangle compartmentBnds = compartment.getBounds();
        if (!compartmentBnds.contains(labelBnds)) {
            // Since the reconsituted speculative label bounds does not fit within
            // the compartment, the label text must be wrapped. Without reconstructing
            // intricate rendering details, we can't know how it is wrapped. Therefore,
            // ignore this case.
            return null;
        }
        for (Renderable other : components) {
            // Don't check the compartment against itself, non-database objects
            // or reactions.
            if (other == compartment || other.getReactomeId() == null || other instanceof RenderableReaction) {
                continue;
            }
            Rectangle otherBnds = other.getBounds();
            // Don't check the compartment against another compartment
            // unless it is a contained compartment or both compartments
            // are different nodes for the same database compartment object.
            if (other instanceof RenderableCompartment) {
                if (compartment.getReactomeId() != other.getReactomeId()) {
                    if (!compartmentBnds.contains(otherBnds)) {
                        continue;
                    }
                }
            }
            
            // Shrink the other node's bounds by the tolerance.
            int inset = TOLERANCE_TOP == null ? 0 : TOLERANCE;
            int insetBottom =  TOLERANCE_TOP == null ? inset : TOLERANCE_TOP;
            Rectangle otherBndsAdjusted =
                    new Rectangle(otherBnds.x + inset, otherBnds.y + inset,
                            nonnegativeDifference(otherBnds.width, inset * 2),
                            nonnegativeDifference(otherBnds.height, inset + insetBottom));

            // Test for an overlap.
            if (otherBndsAdjusted.intersects(labelBnds)) {
                return other;
            }
        }
        
        return null;
    }

    private int nonnegativeDifference(int m, int n) {
        return m < n ? 0 : m - n;
    }

}
