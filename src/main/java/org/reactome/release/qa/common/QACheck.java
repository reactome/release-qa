package org.reactome.release.qa.common;

/**
 * Interface for a runnable QA check.
 * @author sshorser
 *
 */
public interface QACheck
{
	public QAReport executeQACheck();
	
}
