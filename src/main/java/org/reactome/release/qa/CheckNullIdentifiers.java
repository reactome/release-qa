package org.reactome.release.qa;

import java.util.Arrays;
import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.PersistenceAdaptor;
import org.reactome.release.qa.common.DelimitedTextReport;
import org.reactome.release.qa.common.QACheck;
import org.reactome.release.qa.common.QAReport;

/**
 * identifier is a mandatory slot. The error should be caught during
 * the mandatory check. 
 *
 */
public class CheckNullIdentifiers implements QACheck
{

	private PersistenceAdaptor adaptor;
	
	public void setDataAdaptor(PersistenceAdaptor adaptor)
	{
		this.adaptor = adaptor;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public QAReport executeQACheck()
	{
		QAReport report = new DelimitedTextReport();
		// Find DatabaseIdentifier objects with a "null" identifier.
		Collection<GKInstance> identifiers;
		try
		{
			identifiers = this.adaptor.fetchInstanceByAttribute("DatabaseIdentifier", "identifier", "IS NULL", null);
			if (!identifiers.isEmpty())
			{
				for (GKInstance identifier : identifiers)
				{
					report.addLine(Arrays.asList(identifier.getDBID().toString(), identifier.getDisplayName()));
				}
			}
			report.setColumnHeaders(Arrays.asList("DBID", "Display Name"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return report;
	}

}
