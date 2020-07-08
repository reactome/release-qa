package org.reactome.release.qa.diagram;

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
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the implementation of the diagram-converter T105 check for
 * diagram renderable class consistency with the represented object's
 * schema class.
 * 
 * @author wug
 */
@DiagramQACheck
public class DiagramRenderableTypeCheck extends AbstractQACheck {

    private final static Logger logger = Logger.getLogger(DiagramRenderableTypeCheck.class);
    
    public DiagramRenderableTypeCheck() {
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        @SuppressWarnings("unchecked")
        Collection<GKInstance> pathwayDiagrams = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        dba.loadInstanceAttributeValues(pathwayDiagrams, new String[]{ReactomeJavaConstants.representedPathway});
        DiagramGKBReader reader = new DiagramGKBReader();
        SearchDBTypeHelper typeHelper = new SearchDBTypeHelper();
        for (GKInstance diagram : pathwayDiagrams) {
            if (!isEscaped(diagram) && isSomePathwayHuman(diagram)) {
                checkPathwayDiagram(diagram, reader, typeHelper, report);
            }
        }
        report.setColumnHeaders("PathwayDiagram_DBID",
                "Pathway_DisplayName",
                "Pathway_DBID",
                "Entity_DBID",
                "Entity_DisplayName",
                "Correct Renderable",
                "Found Renderable",
                "MostRecentAuthor");
        return report;
    }
    
    @SuppressWarnings("unchecked")
    private Class getRenderableType(GKInstance inst, SearchDBTypeHelper typeHelper)
            throws Exception {
        if (inst.getSchemClass().isa(ReactomeJavaConstants.Pathway))
            return ProcessNode.class;
        // In some cases GO_CellularComponent instances are used in drawing
        if (inst.getSchemClass().isa(ReactomeJavaConstants.GO_CellularComponent))
            return RenderableCompartment.class;
        return typeHelper.guessNodeType(inst);
    }
    
    private boolean isSomePathwayHuman(GKInstance diagram) throws Exception {
        @SuppressWarnings("unchecked")
        List<GKInstance> pathways = diagram.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
        for (GKInstance pathway : pathways) {
            GKInstance species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
            if (species != null && species.getDisplayName().equals("Homo sapiens")) {
                return true;
            }
        }
        return false;
    }
    
    private void checkPathwayDiagram(GKInstance diagram, DiagramGKBReader reader,
            SearchDBTypeHelper typeHelper, QAReport report) throws Exception {
        logger.debug("Checking " + diagram.getDisplayName() + "...");
        if (isEscaped(diagram)) {
            logger.info("Pathway diagram is on the skip list: " + diagram.getDisplayName());
            return;
        }
        GKInstance pathwayInst = (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        RenderablePathway pathway = reader.openDiagram(diagram);
        @SuppressWarnings("unchecked")
        List<Renderable> components = pathway.getComponents();
        if (components == null || components.size() == 0)
            return;
        for (Renderable r : components) {
            if (!(r instanceof Node))
                continue; // This check is for Entity only
            Long dbId = r.getReactomeId();
            if (dbId == null)
                continue;
            GKInstance dbInst = dba.fetchInstance(dbId);
            if (dbInst == null) {
                // TODO - when could this occur? Should there be a skip list?
                logger.warn("Diagram references DB id not found in database: " + dbId);
                continue;
            }
            Class<? extends Renderable> renderable = getRenderableType(dbInst, typeHelper);
            // There are two types of errors
            // The saved schemaClass and the actual schemaClass
            // Schema class is not saved in the database. There is no need to check it.
//            String schemaClass = (String) r.getAttributeValue(RenderablePropertyNames.SCHEMA_CLASS);
//            if (schemaClass != null && !schemaClass.equals(dbInst.getSchemClass().getName())) {
//                report.addLine(pathwayDiagram.getDBID().toString(),
//                               pathwayInst.getDisplayName(),
//                               pathwayInst.getDBID().toString(),
//                               dbId.toString(),
//                               dbInst.getDisplayName(),
//                               dbInst.getSchemClass().getName(),
//                               schemaClass,
//                               "Schema class not matched",
//                               QACheckerHelper.getLastModificationAuthor(pathwayDiagram));
//            }
            // Used Renderable object is not matched to the correct one
            if (r.getClass() != renderable) {
                report.addLine(diagram.getDBID().toString(),
                        pathwayInst.getDisplayName(),
                        pathwayInst.getDBID().toString(),
                        dbId.toString(),
                        dbInst.getDisplayName(),
                        renderable.getSimpleName(),
                        r.getClass().getSimpleName(),
                        QACheckerHelper.getLastModificationAuthor(diagram));
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Diagram_Wrong_Renderable_Class";
    }

}
