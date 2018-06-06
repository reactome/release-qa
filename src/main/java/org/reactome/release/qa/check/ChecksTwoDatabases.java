package org.reactome.release.qa.check;

import org.gk.persistence.MySQLAdaptor;

/**
 * Interface for a check that checks two database.
 * Any check that implements this interface should implement a method to set a second MySQLDBAdaptor for another database.
 * For example: some QA checks might need to compare test_reactome_XX and test_reactome_XX -1, others might want to compare test_reactome_XX to gk_central.
 * @author sshorser
 *
 */
public interface ChecksTwoDatabases
{
	public void setOtherDBAdaptor(MySQLAdaptor adaptor);
}
