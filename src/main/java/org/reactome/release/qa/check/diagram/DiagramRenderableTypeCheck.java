package org.reactome.release.qa.check.diagram;

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
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the implementation of Antonio's DT102 for object consistence checking in pathway diagrams.
 * @author wug
 *
 */
@SliceQACheck
public class DiagramRenderableTypeChecker extends DiagramQACheck {
    private final static Logger logger = Logger.getLogger(DiagramRenderableTypeChecker.class);
    
    public DiagramRenderableTypeChecker() {
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Collection<GKInstance> pathwayDiagrams = getPathwayDiagrams();
        DiagramGKBReader reader = new DiagramGKBReader();
        SearchDBTypeHelper typeHelper = new SearchDBTypeHelper();
        for (GKInstance diagram : pathwayDiagrams) {
            if (isHuman(diagram)) {
                checkPathwayDiagram(diagram, reader, typeHelper, report);
            }
        }
        report.setColumnHeaders(Arrays.asList("PathwayDiagram_DBID",
                "Pathway_DisplayName",
                "Pathway_DBID",
                "Entity_DBID",
                "Entity_DisplayName",
                "Correct Renderable",
                "Wrong Renderable",
                "MostRecentAuthor"));
        return report;
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends Renderable> getRenderableType(GKInstance inst, SearchDBTypeHelper typeHelper) throws Exception {
        if (inst.getSchemClass().isa(ReactomeJavaConstants.Pathway))
            return ProcessNode.class;
        // In some cases GO_CellularComponent instances are used in drawing
        if (inst.getSchemClass().isa(ReactomeJavaConstants.GO_CellularComponent))
            return RenderableCompartment.class;
        return typeHelper.guessNodeType(inst);
    }
    
    private void checkPathwayDiagram(GKInstance pathwayDiagram,
            DiagramGKBReader reader,
            SearchDBTypeHelper typeHelper,
            QAReport report) throws Exception {
        logger.info("Checking " + pathwayDiagram.getDisplayName() + "...");
        GKInstance pathwayInst = (GKInstance) pathwayDiagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        RenderablePathway pathway = reader.openDiagram(pathwayDiagram);
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
            
            
            // DO NOT CHECK IN YET
            // TODO - Does this test need a skip list instead?
            if (dbInst == null) {
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
                report.addLine(pathwayDiagram.getDBID().toString(),
                        pathwayInst.getDisplayName(),
                        pathwayInst.getDBID().toString(),
                        dbId.toString(),
                        dbInst.getDisplayName(),
                        renderable.getSimpleName(),
                        r.getClass().getSimpleName(),
                        QACheckerHelper.getLastModificationAuthor(pathwayDiagram));
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Wrong_Renderables_In_Diagrams";
    }

}
