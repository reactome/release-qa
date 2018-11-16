package org.reactome.release.qa.check;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.property.SearchDBTypeHelper;
import org.gk.render.Node;
import org.gk.render.ProcessNode;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderablePathway;
import org.gk.schema.InvalidAttributeException;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the implementation of diagram-converter DT101 check for
 * empty pathway diagrams.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@SliceQACheck
public class DiagramEmptyCheck extends AbstractQACheck {

    private static final String[] LOAD_ATTS =
            new String[]{ReactomeJavaConstants.representedPathway};
    
    private final static Logger logger = Logger.getLogger(DiagramEmptyCheck.class);
    
    public DiagramEmptyCheck() {
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        @SuppressWarnings("unchecked")
        Collection<GKInstance> pathwayDiagrams =
                dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        dba.loadInstanceAttributeValues(pathwayDiagrams, LOAD_ATTS);
        DiagramGKBReader reader = new DiagramGKBReader();
        for (GKInstance diagram : pathwayDiagrams) {
            if (!isEscaped(diagram)) {
                checkPathwayDiagram(diagram, reader, report);
            }
        }
        report.setColumnHeaders(Arrays.asList("PathwayDiagram_DBID",
                "Pathway_DisplayName",
                "Pathway_DBID",
                "MostRecentAuthor"));
        return report;
    }
    
    private void checkPathwayDiagram(GKInstance diagram, DiagramGKBReader reader,
            QAReport report) throws Exception {
        logger.debug("Checking " + diagram.getDisplayName() + "...");
        if (isEscaped(diagram)) {
            String msg = "Pathway diagram is on the skip list: " + diagram.getDisplayName();
            logger.info(msg);
            return;
        }
        RenderablePathway pathway = reader.openDiagram(diagram);
        @SuppressWarnings("unchecked")
        List<Renderable> components = pathway.getComponents();
        if (components == null || components.size() == 0) {
            GKInstance pathwayInst =
                    (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
            report.addLine(diagram.getDBID().toString(),
                    pathwayInst.getDisplayName(),
                    pathwayInst.getDBID().toString(),
                    QACheckerHelper.getLastModificationAuthor(diagram));
        }
    }

}
