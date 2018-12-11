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
 * This is the implementation of the diagram-converter DT112 check for
 * diagram Compartment nodes whose displayName is missing or is
 * significantly occluded by another node.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramCompartmentLabelCheck extends AbstractDiagramQACheck {

    private static final String TOLERANCE_PROP = "diagram.compartment.label.tolerance";

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
                // TODO - The occlusion check detects false positive overlap,
                // even with a high tolerance. Comment out for now.
                //Renderable overlap = findTextOcclusion(cmpnt, cmpnts);
                Renderable overlap = null;
                if (cmpnt.getDisplayName() == null || overlap != null) {
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
    @SuppressWarnings("unused")
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
                return other;
            }
        }
        return null;
    }

}
