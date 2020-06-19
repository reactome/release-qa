package org.reactome.release.qa.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.gk.persistence.MySQLAdaptor;

public abstract class AbstractQACheck implements QACheck {
    private final static Logger logger = Logger.getLogger(AbstractQACheck.class);

    protected MySQLAdaptor dba;
    protected final List<String> EMPTY_SKIP_LIST = Collections.emptyList();

    @Override
    public void setMySQLAdaptor(MySQLAdaptor dba) {
        this.dba = dba;
    }
    
    public File getConfigurationFile() {
        String fileName = "resources" + File.separator + getClass().getSimpleName() + ".txt";
        File file = new File(fileName);
        if (!file.exists()) {
            logger.warn("This is no configuration file available for " + 
                         getClass().getSimpleName() + 
                        ": " + file.getAbsolutePath());
            return null;
        }
        return file;
    }
    
}
