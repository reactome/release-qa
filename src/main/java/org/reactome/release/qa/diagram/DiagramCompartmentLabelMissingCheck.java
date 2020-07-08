package org.reactome.release.qa.diagram;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderablePathway;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the implementation of the diagram-converter DT112 check for
 * diagram Compartment nodes whose displayName is missing.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramCompartmentLabelMissingCheck extends AbstractDiagramQACheck {

    
    private static final List<String> HEADERS = Arrays.asList(
            "PathwayDiagram_DBID",
            "Pathway_DisplayName",
            "Pathway_DBID",
            "Compartment_DBID",
            "Compartment_DisplayName",
            "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return "Diagram_Compartment_Label_Missing";
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
                if (cmpntInst == null) {
                    // This is a serious issue
                    String mod = QACheckerHelper.getLastModificationAuthor(diagram);
                    report.addLine(diagram.getDBID().toString(),
                                   pathwayInst.getDisplayName(),
                                   pathwayInst.getDBID().toString(),
                                   dbId + "",
                                   "Compartment cannot find in db",
                                   mod);
                    continue;
                }
                if (isEscaped(cmpntInst)) {
                    continue;
                }
                if (cmpnt.getDisplayName() == null) {
                    String mod = QACheckerHelper.getLastModificationAuthor(diagram);
                    report.addLine(diagram.getDBID().toString(),
                            pathwayInst.getDisplayName(),
                            pathwayInst.getDBID().toString(),
                            cmpntInst.getDBID().toString(),
                            cmpntInst.getDisplayName(),
                            mod);
                }
            }
        }
    }
}
