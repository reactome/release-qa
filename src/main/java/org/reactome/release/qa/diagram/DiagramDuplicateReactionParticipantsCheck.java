package org.reactome.release.qa.diagram;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the slice QA adaptation of the diagram-converter T104 duplicate
 * participants diagram check.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramDuplicateReactionParticipantsCheck extends AbstractDiagramQACheck {
    
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Collection<GKInstance> pathwayDiagrams = getPathwayDiagrams();
        DiagramGKBReader reader = new DiagramGKBReader();
        for (GKInstance diagram : pathwayDiagrams) {
            checkPathwayDiagram(diagram, reader, report);
        }
        report.setColumnHeaders("PathwayDiagram_DBID",
                "Pathway_DisplayName",
                "Pathway_DBID",
                "Reaction_DBID",
                "Reaction_DisplayName",
                "Entity_DBID",
                "Entity_DisplayName",
                "MostRecentAuthor");
        
        return report;
    }

    private void checkPathwayDiagram(GKInstance diagram, DiagramGKBReader reader,
            QAReport report) throws Exception {
        RenderablePathway pathway = reader.openDiagram(diagram);
        @SuppressWarnings("unchecked")
        List<Renderable> components = (List<Renderable>) pathway.getComponents();
        for (Renderable component: components) {
            if (component instanceof RenderableReaction) {
                RenderableReaction reaction = (RenderableReaction) component;
                List<Node> inputs = reaction.getInputNodes();
                checkForDuplicates(diagram, reaction, inputs, report);
                List<Node> outputs = reaction.getOutputNodes();
                checkForDuplicates(diagram, reaction, outputs, report);
            }
        }
    }

    private void checkForDuplicates(GKInstance diagram, RenderableReaction reaction,
            List<Node> nodes, QAReport report) throws Exception {
        Set<Renderable> unique = new HashSet<Renderable>();
        Set<Renderable> dups = nodes.stream()
                .filter(node -> !unique.add(node))
                .collect(Collectors.toSet());
        if (!dups.isEmpty()) {
            GKInstance pathway =
                    (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
            for (Renderable dup: dups) {
                report.addLine(diagram.getDBID().toString(),
                        pathway.getDisplayName(),
                        pathway.getDBID().toString(),
                        reaction.getReactomeId().toString(),
                        reaction.getDisplayName(),
                        dup.getReactomeId().toString(),
                        dup.getDisplayName(),
                        QACheckerHelper.getLastModificationAuthor(diagram));
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Diagram_Duplicate_Reaction_Participants";
    }

}
