package org.reactome.release.qa.common;

import org.gk.persistence.MySQLAdaptor;

/**
 * Interface for a runnable QA check.
 * @author sshorser
 *
 */
public interface QACheck {
    
    public void setMySQLAdaptor(MySQLAdaptor dba);
    
	public QAReport executeQACheck() throws Exception;
    
    /**
     * @return the name used to generate the report title and file name
     */
    public String getDisplayName();
    
    /**
     * @return the report simple file name
     */
    default public String getFileName() {
        return getDisplayName() + ".tsv";
    }
	
}
