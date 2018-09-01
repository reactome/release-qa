package org.reactome.release.qa.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;
import org.reactome.release.qa.check.ChimericInstancesChecker;
import org.reactome.release.qa.check.CompareSpeciesByClasses;
import org.reactome.release.qa.check.HumanEventNotInHierarchyChecker;
import org.reactome.release.qa.check.PathwayDiagramRenderableTypeChecker;
import org.reactome.release.qa.check.SpeciesInPrecedingRelationChecker;
import org.reactome.release.qa.check.StableIdentifierCheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.MySQLAdaptorManager;
import org.reactome.release.qa.common.QAReport;
import org.reactome.release.qa.graph.InstanceDuplicationCheck;
import org.reactome.release.qa.graph.MultipleAttributesCrossClassesMissingCheck;
import org.reactome.release.qa.graph.MultipleAttributesMissingCheck;
import org.reactome.release.qa.graph.SingleAttributeDuplicationCheck;
import org.reactome.release.qa.graph.SingleAttributeMissingCheck;
import org.reactome.release.qa.graph.SingleAttributeSoleValueCheck;

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
        if (checker instanceof CompareSpeciesByClasses)
        {
        	((CompareSpeciesByClasses) checker).setOtherDBAdaptor(manager.getAlternateDBA());
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

}
