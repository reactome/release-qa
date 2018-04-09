package org.reactome.qa;

import org.reactome.qa.report.Report;

/**
 * Interface for a runnable QA check.
 * @author sshorser
 *
 */
public interface QACheck
{
	public Report executeQACheck();
	
}
