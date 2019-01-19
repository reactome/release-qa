package org.reactome.release.qa.diagram;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
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
 * occluded by another node.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramCompartmentLabelOccludedCheck extends AbstractDiagramQACheck {

    private static final String TOLERANCE_PROP = "diagram.compartment.overlap.tolerance";

    private final static Integer TOLERANCE = QACheckProperties.getInteger(TOLERANCE_PROP);
    
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
        return "Diagram_Compartment_Label_Missing_Or_Occluded";
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
        RenderablePathway pathway = reader.openDiagram(diagram);
        @SuppressWarnings("unchecked")
        List<Renderable> cmpnts = pathway.getComponents();
        GKInstance pathwayInst =
                (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        // Check for a non-empty, non-occluded label.
        for (Renderable cmpnt: cmpnts) {
            if (cmpnt instanceof RenderableCompartment) {
                Long dbId = cmpnt.getReactomeId();
                if (dbId == null) {
                    continue;
                }
                GKInstance cmpntInst = dba.fetchInstance(dbId);
                if (isEscaped(cmpntInst)) {
                    continue;
                }
                if (cmpnt.getDisplayName() == null) {
                    // This is covered by another check.
                    continue;
                }
                Renderable overlap = findTextOcclusion(cmpnt, cmpnts);
                if (overlap != null) {
                    String mod = QACheckerHelper.getLastModificationAuthor(diagram);
                    String overlapDbIdStr = "";
                    String overlapDisplayNm = "";
                    if (overlap != null) {
                        Long overlapDbId = overlap.getReactomeId();
                        if (overlapDbId != null) {
                            overlapDbIdStr = overlapDbId.toString();
                            GKInstance overlapInst = dba.fetchInstance(overlap.getReactomeId());
                            overlapDisplayNm = overlapInst.getDisplayName();
                        }
                    }
                    report.addLine(diagram.getDBID().toString(),
                            pathwayInst.getDisplayName(),
                            pathwayInst.getDBID().toString(),
                            cmpntInst.getDBID().toString(),
                            cmpntInst.getDisplayName(),
                            overlapDbIdStr,
                            overlapDisplayNm,
                            mod);
                }
            }
        }
    }
    
    /**
     * @param cmpnt the diagram component to check
     * @param cmpnts all of the diagram components
     * @return a component that occludes the text, or null if none
     */
    private Renderable findTextOcclusion(Renderable cmpnt, List<Renderable> cmpnts) {
        int tolerance = TOLERANCE == null ? 0 : TOLERANCE;
        Rectangle bnds = cmpnt.getBounds();
        Rectangle textBnds = cmpnt.getTextBounds();
        Point textOrigin = new Point((int)textBnds.getX() + tolerance, (int)textBnds.getY() + tolerance);
        for (Renderable other : cmpnts) {
            if (other == cmpnt || other.getReactomeId() == null || other instanceof RenderableReaction) {
                continue;
            }
            if (other instanceof RenderableCompartment) {
                if (!bnds.contains(other.getBounds()) || other.getReactomeId().equals(cmpnt.getReactomeId())) {
                    continue;
                }
            }
            if (other.getBounds().contains(textOrigin)) {
                if (other.getReactomeId() == 9009912L) {
                    System.out.println(">>");
                }
                return other;
            }
        }
        return null;
    }

}
