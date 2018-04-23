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
	 * The name to be used to generate the file name.
	 * @return
	 */
	public String getDisplayName();
	
}
