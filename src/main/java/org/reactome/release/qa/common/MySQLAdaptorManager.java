package org.reactome.release.qa.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.gk.persistence.MySQLAdaptor;

public class MySQLAdaptorManager {
    
    private MySQLAdaptor dba;
    private MySQLAdaptor altDba;
    private Map<String, Object> cmdOpts;
    private static MySQLAdaptorManager manager;

    public static MySQLAdaptorManager getManager() {
        if (manager == null)
            manager = new MySQLAdaptorManager();
        return manager;
    }

    public static MySQLAdaptorManager getManager(Map<String, Object> cmdOpts) {
        return new MySQLAdaptorManager(cmdOpts);
    }
    
    public MySQLAdaptorManager() {
        this(null);
    }
    
    private MySQLAdaptorManager(Map<String, Object> cmdOpt) {
        this.cmdOpts = cmdOpt;
    }

    public MySQLAdaptor getDBA() throws Exception {
        if (dba == null)
            initDBA();
        return dba;
    }
    
    /**
     * Some checks might compare the current database to a different database (usually the database
     * from the previous release). The "alternate DBA" is a secondary DBA that can be used to get
     * a connection to another database.
     * @return
     * @throws Exception
     */
    public MySQLAdaptor getAlternateDBA() throws Exception {
        if (altDba == null)
        	initAlternateDBA();
        return altDba;
    }
    
    private void initDBA() throws Exception {
        Properties prop = getAuthProperties();
        // The command line options augment or override the auth.properties values.
        if (cmdOpts != null) {
            prop.putAll(cmdOpts);
        }
        dba = new MySQLAdaptor(prop.getProperty("dbHost"),
                               prop.getProperty("dbName"),
                               prop.getProperty("dbUser"),
                               prop.getProperty("dbPwd"));
    }

    protected Properties getAuthProperties() throws Exception {
        InputStream is = getAuthConfig();
        Properties prop = new Properties();
        prop.load(is);
        return prop;
    }
    
    private void initAlternateDBA() throws Exception {
        Properties prop = getAuthProperties();
        if (cmdOpts != null) {
            prop.putAll(cmdOpts);
        }
        altDba = new MySQLAdaptor(prop.getProperty("altDbHost"),
                               prop.getProperty("altDbName"),
                               prop.getProperty("altDbUser"),
                               prop.getProperty("altDbPwd"));
    }

    
    public InputStream getAuthConfig() throws IOException {
        // Have to add "/" before the file name. Otherwise, the file cannot be found.
        String fileName = File.separator + "auth.properties";
        // Try to get the configuration file first
        File file = new File("resources" + fileName);
        if (file.exists())
            return new FileInputStream(file);
        return getClass().getResourceAsStream(fileName);
    }
    
}
