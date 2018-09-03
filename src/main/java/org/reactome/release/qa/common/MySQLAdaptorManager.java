package org.reactome.release.qa.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.gk.persistence.MySQLAdaptor;

public class MySQLAdaptorManager {
    
    private MySQLAdaptor dba;
    private MySQLAdaptor altDba;
    private static MySQLAdaptorManager manager;
    
    public static MySQLAdaptorManager getManager() {
        if (manager == null)
            manager = new MySQLAdaptorManager();
        return manager;
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
        InputStream is = getAuthConfig();
        Properties prop = new Properties();
        prop.load(is);
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
