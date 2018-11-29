package org.reactome.release.qa.diagram;

import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathwaylayout.DiagramGeneratorFromDB;
import org.gk.pathwaylayout.PathwayDiagramXMLGenerator;
import org.reactome.release.qa.common.AbstractQACheck;

public abstract class DiagramQACheck extends AbstractQACheck {

    protected static final String[] REPRESENTED_PATHWAY_ATTS = {
            ReactomeJavaConstants.representedPathway
    };

    protected boolean isHuman(GKInstance diagram) throws Exception {
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

    /**
     * @return all PathwayDiagram instances with pre-loaded representedPathway attribute
     * @throws Exception
     */
    protected Collection<GKInstance> getPathwayDiagrams() throws Exception {
        @SuppressWarnings("unchecked")
        Collection<GKInstance> pathwayDiagrams = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        dba.loadInstanceAttributeValues(pathwayDiagrams, new String[]{ReactomeJavaConstants.representedPathway});
        
        return pathwayDiagrams;
    }
    
    protected String getPathwayDiagramXML(GKInstance diagram) throws Exception {
        GKInstance pathway = (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        PathwayDiagramXMLGenerator xmlGenerator = new PathwayDiagramXMLGenerator();
        
        return xmlGenerator.generateXMLForPathwayDiagram(diagram, pathway);
    }

}