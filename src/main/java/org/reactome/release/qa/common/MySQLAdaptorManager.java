package org.reactome.release.qa.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.gk.persistence.MySQLAdaptor;

public class MySQLAdaptorManager {
    
    private MySQLAdaptor dba;
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
    
    private void initDBA() throws Exception {
        InputStream is = getAuthConfig();
        Properties prop = new Properties();
        prop.load(is);
        dba = new MySQLAdaptor(prop.getProperty("dbHost"),
                               prop.getProperty("dbName"),
                               prop.getProperty("dbUser"),
                               prop.getProperty("dbPwd"));
    }
    
    private InputStream getAuthConfig() throws Exception {
        // Have to add "/" before the file name. Otherwise, the file cannot be found.
        String fileName = File.separator + "auth.properties";
        // Try to get the configuration file first
        File file = new File("resources" + fileName);
        if (file.exists())
            return new FileInputStream(file);
        return getClass().getResourceAsStream(fileName);
    }
    
}
