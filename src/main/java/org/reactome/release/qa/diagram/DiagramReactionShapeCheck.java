package org.reactome.release.qa.diagram;

import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.property.SearchDBTypeHelper;
import org.gk.render.ReactionType;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.annotations.DiagramQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This is the implementation of the diagram-converter T113 check for
 * diagram reaction shape type consistency with the represented
 * database Reaction reactionType value.
 *
 * The reaction type is determined as follows:
 * <ol>
 * <li>BlackBoxEvent => <code>omitted</code>
 * <li>Polymerisation or Depolymerisation or has catalystActivity => <code>transition</code>
 * <li>More inputs than outputs and has a Complex output => <code>association</code>
 * <li>More outputs than inputs and has a Complex input => <code>dissociation</code>
 * <li>Otherwise => <code>transition</code>
 * </ol>
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@DiagramQACheck
public class DiagramReactionShapeCheck extends AbstractQACheck {

    private final static Logger logger =
            Logger.getLogger(DiagramReactionShapeCheck.class);

    private static final String[] LOAD_ATTS = {
            ReactomeJavaConstants.representedPathway
    };

    @Override
    public String getDisplayName() {
        return "Diagram_Reaction_Shape_Mismatch";
    }

    @SuppressWarnings("unchecked")
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Collection<GKInstance> pathwayDiagrams =
                dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        dba.loadInstanceAttributeValues(pathwayDiagrams, LOAD_ATTS);
        DiagramGKBReader reader = new DiagramGKBReader();
        SearchDBTypeHelper typeHelper = new SearchDBTypeHelper();
        for (GKInstance diagram : pathwayDiagrams) {
            if (!isEscaped(diagram)) {
                checkPathwayDiagram(diagram, reader, typeHelper, report);
            }
        }
        report.setColumnHeaders("PathwayDiagram_DBID",
                "Pathway_DisplayName",
                "Pathway_DBID",
                "Reaction_DBID",
                "Reaction_DisplayName",
                "Correct_Reaction_Type",
                "Found_Reaction_Type",
                "MostRecentAuthor");
        
        return report;
    }
    
    private void checkPathwayDiagram(GKInstance diagram, DiagramGKBReader reader,
            SearchDBTypeHelper typeHelper, QAReport report) throws Exception {
        logger.debug("Checking " + diagram.getDisplayName() + "...");
        if (isEscaped(diagram)) {
            logger.info("Pathway diagram is on the skip list: " + diagram.getDisplayName());
            return;
        }
        GKInstance pathwayInst =
                (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        RenderablePathway pathway = reader.openDiagram(diagram);
        @SuppressWarnings("unchecked")
        List<Renderable> components = pathway.getComponents();
        if (components == null || components.size() == 0)
            return;
        for (Renderable r : components) {
            if (!(r instanceof RenderableReaction)) {
                continue;
            }
            RenderableReaction rxn = (RenderableReaction) r;
            Long dbId = rxn.getReactomeId();
            if (dbId == null)
                continue;
            GKInstance dbInst = dba.fetchInstance(dbId);
            if (dbInst == null) {
                logger.warn("Diagram references DB id not found in database: " + dbId);
                continue;
            }
            // The diagram reaction type (default transition).
            ReactionType diagRxnType = rxn.getReactionType();
            if (diagRxnType == null) {
                diagRxnType = ReactionType.TRANSITION;
            }
            // The reaction type inferred from the database reaction object.
            ReactionType dbRxnType = inferReactionType(dbInst);
            if (diagRxnType != dbRxnType) {
                report.addLine(diagram.getDBID().toString(),
                        pathwayInst.getDisplayName(),
                        pathwayInst.getDBID().toString(),
                        dbId.toString(),
                        dbInst.getDisplayName(),
                        dbRxnType.toString(),
                        diagRxnType.toString(),
                        QACheckerHelper.getLastModificationAuthor(diagram));
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private ReactionType inferReactionType(GKInstance instance) throws Exception {
        SchemaClass schemaCls = instance.getSchemClass();
        if (!schemaCls.isa(ReactomeJavaConstants.ReactionlikeEvent)) {
            String msg = "The instance must be a ReactionlikeEvent; found: " +
                    schemaCls.getName() + " for " + instance.getDisplayName() +
                    " (DB_ID " + instance.getDBID() + ")";
            throw new IllegalArgumentException(msg);
        }
        // BBE => omitted.
        if (schemaCls.isa(ReactomeJavaConstants.BlackBoxEvent)) {
            return ReactionType.OMITTED_PROCESS;
        }
        // (de)polymerisation or has catalyst => association.
        if (schemaCls.isa(ReactomeJavaConstants.Polymerisation) ||
                schemaCls.isa(ReactomeJavaConstants.Depolymerisation) ||
                instance.getAttributeValue(ReactomeJavaConstants.catalystActivity) != null) {
            return ReactionType.TRANSITION;
        }
        List<GKInstance> inputs =
                instance.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = 
                instance.getAttributeValuesList(ReactomeJavaConstants.output);
        int netProductCnt = outputs.size() - inputs.size();
        // More inputs than outputs and has a Complex output => association.
        // More outputs than inputs and has a Complex input => dissociation.
        if (netProductCnt < 0 && hasComplex(outputs)) {
            return ReactionType.ASSOCIATION;
        } else if (netProductCnt > 0 && hasComplex(inputs)) {
            return ReactionType.DISSOCIATION;
        }
        
        // Default is transition.
        return ReactionType.TRANSITION;
    }
    
    /**
     * Returns whether any of the given instances or its constituents
     * is a Complex. An EntitySet's members and candidates are
     * checked recursively.
     * 
     * @param instances the instances to inspect
     * @return whether an instance is or contains a Complex
     * @throws Exception
     */
    private boolean hasComplex(Collection<GKInstance> instances) throws Exception {
        for (GKInstance instance: instances) {
            if (hasComplex(instance)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns whether the given instance or any of its constituents
     * is a Complex. An EntitySet's members and candidates are
     * checked recursively.
     * 
     * @param instance
     * @return whether the instance is or contains a Complex
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private boolean hasComplex(GKInstance instance) throws Exception {
        SchemaClass schemaCls = instance.getSchemClass();
        if (schemaCls.isa(ReactomeJavaConstants.EntitySet)) {
            List<GKInstance> constituents =
                    instance.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (schemaCls.isa(ReactomeJavaConstants.CandidateSet)) {
                List<GKInstance> candidates =
                        instance.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
                constituents.addAll(candidates);
            }
            return hasComplex(constituents);
        } else {
            return schemaCls.isa(ReactomeJavaConstants.Complex);
        }
    }
 
}
