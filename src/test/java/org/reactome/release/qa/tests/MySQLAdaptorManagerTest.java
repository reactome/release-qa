package org.reactome.release.qa.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;
import org.reactome.release.qa.common.MySQLAdaptorManager;

public class MySQLAdaptorManagerTest {
	
	private static final Logger logger = LogManager.getLogger();

    @Test
    public void testGetDBA() throws Exception {
        MySQLAdaptorManager manager = MySQLAdaptorManager.getManager();
        MySQLAdaptor dba = manager.getDBA();
        logger.info("dbName: " + dba.getDBName() + "@" + dba.getDBHost());
    }


}
