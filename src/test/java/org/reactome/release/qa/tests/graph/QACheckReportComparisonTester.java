package org.reactome.release.qa.tests.graph;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.persistence.MySQLAdaptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.common.MySQLAdaptorManager;
import org.reactome.release.qa.common.QACheck;
import org.reactome.release.qa.common.QAReport;

/**
 * A QA check test superclass that creates valid and invalid instances
 * in the test database and compares the QA reports before and after
 * running the QA check for expected differences.
 * 
 * <em>Note</em>: because the MySQLAdaptorManager does not support
 * selecting a different authorization file for testing, these tests
 * run against the Reactome database defined in the standard location.
 * Although the teardowns delete the invalid instances created by the
 * setup, the tests should be run against a development or test
 * database (which is a good development practice anyway).
 * 
 * This test framework should not be deployed to a production environment.
 * The Maven package for distribution should not include this test framework
 * in the deployment jar (which is a good development practice anyway).
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
abstract public class QACheckReportComparisonTester {

    protected QACheck checker;
    protected MySQLAdaptor dba;
    private int beforeCnt;
    private Collection<Instance> fixture;
    private int expected;

    protected QACheckReportComparisonTester(AbstractQACheck checker, int expected) {
        this.checker = checker;
        this.expected = expected;
    }

    /**
     * Sets up testing for this QA check.
     * 
     * @throws FileNotFoundException if the test database authorization
     *      file is missing
     */
    @Before
    public void setUp() throws Exception {
        // Acquire the database adaptor.
        MySQLAdaptorManager manager = MySQLAdaptorManager.getManager();
        dba = manager.getDBA();
        checker.setMySQLAdaptor(dba);
        // Run without a fixture.
        QAReport report = checker.executeQACheck();
        // Capture the report line count.
        beforeCnt = report.getReportLines().size();
        // Make the fixture.
        fixture = createTestFixture();
        // Save the fixture. storeLocalInstances() overspecifies
        // a list argument rather than a collection, so first
        // convert the fixtures to a list.
        List<Instance> saves = fixture.stream().collect(Collectors.toList());
        dba.storeLocalInstances(saves);
        dba.commit();
    }

    @Test
    /**
     * Verifies this QA check.
     * 
     * @see #setUp()
     */
    public void testCheck() throws Exception {
        // Rerun the QA check.
        QAReport report = checker.executeQACheck();
        // Compare the line counts.
        int afterCnt = report.getReportLines().size();
        String message = checker.getDisplayName() + " report count incorrect";
        assertEquals(message, (int)expected, afterCnt - beforeCnt);
    }

    @After
    /**
     * Deletes all test fixture instances.
     * 
     * @throws Exception if there is a database access error
     */
    public void tearDown() throws Exception {
        if (fixture == null) {
            return;
        }
        for (Instance inst: fixture) {
            // Note: this is the one place where it is assumed
            // that the instance is a GKInstance.
            dba.deleteInstance((GKInstance) inst);
        }
        dba.commit();
    }

    /**
     * Creates a fixture of instances with known validity.
     * The <em>expected</em> constructor variable is the number
     * of invalid instances. The default is the number of
     * instances in the fixture. Therefore, if the subclass
     * creates a mixture of valid and invalid instances,
     * then it is the subclass responsibility to set the
     * expected invalid instance count in the constructor.
     */
    abstract protected Collection<Instance> createTestFixture()
            throws Exception;

}
