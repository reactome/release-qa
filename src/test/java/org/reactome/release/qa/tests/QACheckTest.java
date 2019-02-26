package org.reactome.release.qa.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;
import org.reactome.release.qa.check.ChecksTwoDatabases;
import org.reactome.release.qa.check.ChimericInstancesCheck;
import org.reactome.release.qa.check.EHLDSubpathwayChangeCheck;
import org.reactome.release.qa.check.ReactionlikeEventDiseaseCheck;
import org.reactome.release.qa.check.SpeciesInstanceCountCheck;
import org.reactome.release.qa.check.SpeciesPrecedingRelationCheck;
import org.reactome.release.qa.check.StableIdentifierCheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.MySQLAdaptorManager;
import org.reactome.release.qa.common.QAReport;
import org.reactome.release.qa.diagram.DiagramDuplicateReactionParticipantsCheck;
import org.reactome.release.qa.diagram.DiagramEmptyCheck;
import org.reactome.release.qa.diagram.DiagramExtraReactionlikeEventsCheck;
import org.reactome.release.qa.diagram.DiagramOverlappingEntityCheck;
import org.reactome.release.qa.diagram.DiagramOverlappingReactionCheck;
import org.reactome.release.qa.diagram.DiagramReactionShapeCheck;
import org.reactome.release.qa.diagram.DiagramRenderableTypeCheck;
import org.reactome.release.qa.diagram.DiagramReactionBranchCheck;
import org.reactome.release.qa.diagram.DiagramCompartmentCheck;
import org.reactome.release.qa.graph.CatalystActivityComplexCheck;
import org.reactome.release.qa.graph.InferredFromInOtherAttributeCheck;
import org.reactome.release.qa.graph.InstanceDuplicationCheck;
import org.reactome.release.qa.graph.MultipleAttributesCrossClassesMissingCheck;
import org.reactome.release.qa.graph.MultipleAttributesMissingCheck;
import org.reactome.release.qa.graph.OneHopCircularReferenceCheck;
import org.reactome.release.qa.graph.OrphanEventsCheck;
import org.reactome.release.qa.graph.RelationsReferToSameInstanceCheck;
import org.reactome.release.qa.graph.PhysicalEntitiesCompartmentCheck;
import org.reactome.release.qa.graph.PrecedingEventUsageCheck;
import org.reactome.release.qa.graph.ReactionsSingleInputOutputSchemaClassCheck;
import org.reactome.release.qa.graph.SingleAttributeDuplicationCheck;
import org.reactome.release.qa.graph.SingleAttributeMissingCheck;
import org.reactome.release.qa.graph.SingleAttributeSoleValueCheck;
import org.reactome.release.qa.graph.TwoAttributesReferToSameCheck;

/**
 * Make sure the class name ends with "Test" to be included in maven test automatically.
 * Note: To run newly added test methods, need to run maven install first!!! Otherwise, an error
 * may be generated.
 * @author wug
 *
 */
public class QACheckTest {
    private static final Logger logger = LogManager.getLogger();
    
    public QACheckTest() {
    }
    
    private void runTest(AbstractQACheck checker) throws Exception {
        MySQLAdaptorManager manager = MySQLAdaptorManager.getManager();
        MySQLAdaptor dba = manager.getDBA();
        checker.setMySQLAdaptor(dba);
        if (checker instanceof ChecksTwoDatabases) {
            ((ChecksTwoDatabases) checker).setOtherDBAdaptor(manager.getAlternateDBA());
        }
        logger.info("Test " + checker.getDisplayName());
        QAReport report = checker.executeQACheck();
        if (report.isEmpty()) {
            logger.info("All is fine. Nothing needs to report!");
            return;
        }
        report.output(report.getReportLines().size());
    }
    
    @Test
    public void testOneHopCircularReferenceCheck() throws Exception {
        OneHopCircularReferenceCheck checker = new OneHopCircularReferenceCheck();
        runTest(checker);
    }
    
    @Test
    public void testReactionsSingleInputOutputSchemaClassCheck() throws Exception {
        ReactionsSingleInputOutputSchemaClassCheck checker = new ReactionsSingleInputOutputSchemaClassCheck();
        runTest(checker);
    }
    
    @Test
    public void testOrphanEventsCheck() throws Exception {
        OrphanEventsCheck checker = new OrphanEventsCheck();
        runTest(checker);
    }
    
    @Test
    public void testPrecedingEventUsageCheck() throws Exception {
        PrecedingEventUsageCheck checker = new PrecedingEventUsageCheck();
        runTest(checker);
    }
    
    @Test
    public void testCatalystActivityComplexCheck() throws Exception {
        AbstractQACheck checker = new CatalystActivityComplexCheck();
        runTest(checker);
    }
    
    @Test
    public void testPhysicalEntitiesCompartmentCheck() throws Exception {
        PhysicalEntitiesCompartmentCheck checker = new PhysicalEntitiesCompartmentCheck();
        runTest(checker);
    }
    
    @Test
    public void testRelationsReferToSameInstanceCheck() throws Exception {
        RelationsReferToSameInstanceCheck checker = new RelationsReferToSameInstanceCheck();
        runTest(checker);
    }
    
    @Test
    public void testInferredFromInOtherAttributeCheck() throws Exception {
        InferredFromInOtherAttributeCheck checker = new InferredFromInOtherAttributeCheck();
        runTest(checker);
    }
    
    @Test
    public void testTwoAttributesReferToSameCheck() throws Exception {
        TwoAttributesReferToSameCheck checker = new TwoAttributesReferToSameCheck();
        runTest(checker);
    }
    
    @Test
    public void testSingleAttributeSoleValueCheck() throws Exception {
        AbstractQACheck checker = new SingleAttributeSoleValueCheck();
        runTest(checker);
    }
    
    @Test
    public void testSingleAttributeDuplicationCheck() throws Exception {
        AbstractQACheck checker = new SingleAttributeDuplicationCheck();
        runTest(checker);
    }
    
    @Test
    public void testMultipleAttributesCrossClassesMissingCheck() throws Exception {
        AbstractQACheck checker = new MultipleAttributesCrossClassesMissingCheck();
        runTest(checker);
    }
    
    @Test
    public void testInstanceDuplicationCheck() throws Exception {
        AbstractQACheck checker = new InstanceDuplicationCheck();
        runTest(checker);
    }
    
    @Test
    public void testSingleAttributeMissingCheck() throws Exception {
        AbstractQACheck checker = new SingleAttributeMissingCheck();
        runTest(checker);
    }
    
    @Test
    public void testMultipleAttributesMissingCheck() throws Exception {
        AbstractQACheck checker = new MultipleAttributesMissingCheck();
        runTest(checker);
    }
    
    @Test
    public void testChimericInstancesCheck() throws Exception {
        AbstractQACheck checker = new ChimericInstancesCheck();
        runTest(checker);
    }
    
    @Test
    public void testStableIdentifierCheck() throws Exception {
        AbstractQACheck checker = new StableIdentifierCheck();
        runTest(checker);
    }
    
    @Test
    public void testSpeciesPrecedingRelationCheck() throws Exception {
        AbstractQACheck checker = new SpeciesPrecedingRelationCheck();
        runTest(checker);
    }
    
    @Test
    public void testSpeciesInstanceCountCheck() throws Exception {
        AbstractQACheck checker = new SpeciesInstanceCountCheck();
        runTest(checker);
    }
        
    @Test
    public void testDiagramRenderableTypeCheck() throws Exception {
        AbstractQACheck checker = new DiagramRenderableTypeCheck();
        runTest(checker);
    }
    
    @Test
    public void testDiagramEmptyCheck() throws Exception {
        AbstractQACheck checker = new DiagramEmptyCheck();
        runTest(checker);
    }
    
    @Test
    public void testDiagramDuplicateReactionParticipantsCheck() throws Exception {
        AbstractQACheck checker = new DiagramDuplicateReactionParticipantsCheck();
        runTest(checker);
    }
    
    @Test
    public void testDiagramExtraParticipantCheck() throws Exception {
        AbstractQACheck checker = new DiagramExtraReactionlikeEventsCheck();
        runTest(checker);
    }
    
    @Test
    public void testDiagramUnlabeledCompartmentCheck() throws Exception {
        AbstractQACheck checker = new DiagramCompartmentCheck();
        runTest(checker);
    }
    
    @Test
    public void testDiagramOverlappingEntityCheck() throws Exception {
        AbstractQACheck checker = new DiagramOverlappingEntityCheck();
        runTest(checker);
    }
    
    @Test
    public void testDiagramOverlappingReactionCheck() throws Exception {
        AbstractQACheck checker = new DiagramOverlappingReactionCheck();
        runTest(checker);
    }
    
    @Test
    public void testDiagramReactionBranchCheck() throws Exception {
        AbstractQACheck checker = new DiagramReactionBranchCheck();
        runTest(checker);
    }
    
    @Test
    public void testDiagramCompartmentCheck() throws Exception {
        AbstractQACheck checker = new DiagramCompartmentCheck();
        runTest(checker);
    }
    
    @Test
    public void testDiagramReactionShapeCheck() throws Exception {
        AbstractQACheck checker = new DiagramReactionShapeCheck();
        runTest(checker);
    }

    @Test
    public void testEHLDSubPathwayChangeCheck() throws Exception {
        AbstractQACheck checker = new EHLDSubpathwayChangeCheck();
        runTest(checker);
    }

    @Test
    public void testReactionlikeEventDiseaseCheck() throws Exception {
        AbstractQACheck checker = new ReactionlikeEventDiseaseCheck();
        runTest(checker);
    }

}