package org.reactome.release.qa.check;

import org.gk.persistence.MySQLAdaptor;

/**
 * Interface for a check that checks two database.
 * Any check that implements this interface should implement a method to set a second MySQLDBAdaptor for the prior database
 * (the name "prior" assumes you're comparing a <i>current</i> database to a <i>previous</i> database).
 * @author sshorser
 *
 */
public interface ChecksTwoDatabases
{
	public void setOtherDBAdaptor(MySQLAdaptor adaptor);
}
