package org.reactome.release.qa.diagram;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the slice QA adaptation of the diagram-converter T101 empty
 * pathway diagram check.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramEmptyCheck extends AbstractDiagramQACheck {

    private final static Logger logger = Logger.getLogger(DiagramEmptyCheck.class);

    public DiagramEmptyCheck() {
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Collection<GKInstance> pathwayDiagrams = getPathwayDiagrams();
        DiagramGKBReader reader = new DiagramGKBReader();
        for (GKInstance diagram : pathwayDiagrams) {
            if (isHuman(diagram)) {
                checkPathwayDiagram(diagram, reader, report);
            }
        }
        report.setColumnHeaders("PathwayDiagram_DBID",
                "Pathway_DisplayName",
                "Pathway_DBID",
                "MostRecentAuthor");
        return report;
    }

    private void checkPathwayDiagram(GKInstance pathwayDiagram,
            DiagramGKBReader reader,
            QAReport report) throws Exception {
        logger.info("Checking " + pathwayDiagram.getDisplayName() + "...");
        RenderablePathway pathway = reader.openDiagram(pathwayDiagram);
        @SuppressWarnings("unchecked")
        List<Renderable> components = pathway.getComponents();
        if (components == null || components.size() == 0) {
            GKInstance pathwayInst =
                    (GKInstance) pathwayDiagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
            report.addLine(pathwayDiagram.getDBID().toString(),
                    pathwayInst.getDisplayName(),
                    pathwayInst.getDBID().toString(),
                    QACheckerHelper.getLastModificationAuthor(pathwayDiagram));
        }
    }

}
