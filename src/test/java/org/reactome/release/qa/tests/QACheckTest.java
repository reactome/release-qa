package org.reactome.release.qa.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;
import org.reactome.release.qa.check.*;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.MySQLAdaptorManager;
import org.reactome.release.qa.common.QAReport;
import org.reactome.release.qa.graph.CatalystActivityWherePhysicalEntityAndActiveUnitPointToComplex;
import org.reactome.release.qa.graph.InferredFromInOtherAttributeCheck;
import org.reactome.release.qa.graph.InstanceDuplicationCheck;
import org.reactome.release.qa.graph.MultipleAttributesCrossClassesMissingCheck;
import org.reactome.release.qa.graph.MultipleAttributesMissingCheck;
import org.reactome.release.qa.graph.OneHopCircularReferenceCheck;
import org.reactome.release.qa.graph.OrphanEvents;
import org.reactome.release.qa.graph.OtherRelationsThatPointToTheSameEntry;
import org.reactome.release.qa.graph.PhysicalEntitiesWithMoreThanOneCompartment;
import org.reactome.release.qa.graph.PrecedingEventOutputsNotUsedInReaction;
import org.reactome.release.qa.graph.ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatch;
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
    public void testReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatch() throws Exception {
        ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatch checker = new ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatch();
        runTest(checker);
    }
    
    @Test
    public void testOrphanEvents() throws Exception {
        OrphanEvents checker = new OrphanEvents();
        runTest(checker);
    }
    
    @Test
    public void testPrecedingEventOutputsNotUsedInReaction() throws Exception {
        PrecedingEventOutputsNotUsedInReaction checker = new PrecedingEventOutputsNotUsedInReaction();
        runTest(checker);
    }
    
    @Test
    public void testCatalystActivityWherePhysicalEntityAndActiveUnitPointToComplex() throws Exception {
        CatalystActivityWherePhysicalEntityAndActiveUnitPointToComplex checker = new CatalystActivityWherePhysicalEntityAndActiveUnitPointToComplex();
        runTest(checker);
    }
    
    @Test
    public void testPhysicalEntitiesWithMoreThanOneCompartment() throws Exception {
        PhysicalEntitiesWithMoreThanOneCompartment checker = new PhysicalEntitiesWithMoreThanOneCompartment();
        runTest(checker);
    }
    
    @Test
    public void testOtherRelationsThatPointToTheSameEntry() throws Exception {
        OtherRelationsThatPointToTheSameEntry checker = new OtherRelationsThatPointToTheSameEntry();
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
    public void testChimericInstancesChecker() throws Exception {
        AbstractQACheck checker = new ChimericInstancesChecker();
        runTest(checker);
    }
    
    @Test
    public void testHumanEventNotInHierarchyChecker() throws Exception {
        AbstractQACheck checker = new HumanEventNotInHierarchyChecker();
        runTest(checker);
    }
    
    @Test
    public void testHumanStableIdentifierChecker() throws Exception {
        AbstractQACheck checker = new StableIdentifierCheck();
        runTest(checker);
    }
    
    @Test
    public void testSpeciesInPrecedingRelationChecker() throws Exception {
        AbstractQACheck checker = new SpeciesInPrecedingRelationChecker();
        runTest(checker);
    }
    
    @Test
    public void testCompareSpeciesByClasses() throws Exception {
        AbstractQACheck checker = new CompareSpeciesByClasses();
        runTest(checker);
    }
        
    @Test
    public void testPathwayDiagramRenderableTypeChecker() throws Exception {
        AbstractQACheck checker = new PathwayDiagramRenderableTypeChecker();
        runTest(checker);
    }

    @Test
    public void testEHLDSubPathwayChangeChecker() throws Exception {
        AbstractQACheck checker = new EHLDSubpathwayChangeChecker();
        runTest(checker);
    }
}