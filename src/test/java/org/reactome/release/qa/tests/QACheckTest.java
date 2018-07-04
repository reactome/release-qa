package org.reactome.release.qa.tests;

import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.ChimericInstancesChecker;
import org.reactome.release.qa.check.CompareSpeciesByClasses;
import org.reactome.release.qa.check.HumanEventNotInHierarchyChecker;
import org.reactome.release.qa.check.SpeciesInPrecedingRelationChecker;
import org.reactome.release.qa.check.StableIdentifierCheck;
import org.reactome.release.qa.common.MySQLAdaptorManager;
import org.reactome.release.qa.common.QAReport;

/**
 * Make sure the class name ends with "Test" to be included in maven test automatically.
 * @author wug
 *
 */
public class QACheckTest {
    
    private void runTest(AbstractQACheck checker) throws Exception {
        MySQLAdaptorManager manager = MySQLAdaptorManager.getManager();
        MySQLAdaptor dba = manager.getDBA();
        checker.setMySQLAdaptor(dba);
        if (checker instanceof CompareSpeciesByClasses)
        {
        	((CompareSpeciesByClasses) checker).setOtherDBAdaptor(manager.getAlternateDBA());
        }
        System.out.println("Test " + checker.getDisplayName());
        QAReport report = checker.executeQACheck();
        if (report.isEmpty()) {
            System.out.println("All is fine. Nothing needs to report!");
            return;
        }
        report.output(10);
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

}
