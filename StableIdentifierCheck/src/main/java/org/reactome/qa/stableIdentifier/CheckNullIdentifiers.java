package org.reactome.qa.stableIdentifier;

import java.util.Arrays;
import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.PersistenceAdaptor;
import org.reactome.qa.QACheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

public class CheckNullIdentifiers implements QACheck
{

	private PersistenceAdaptor adaptor;
	
	public void setDataAdaptor(PersistenceAdaptor adaptor)
	{
		this.adaptor = adaptor;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Report executeQACheck()
	{
		StringBuilder sb = new StringBuilder();
		Report report = new DelimitedTextReport();
		// Find DatabaseIdentifier objects with a "null" identifier.
		Collection<GKInstance> identifiers;
		try
		{
			identifiers = this.adaptor.fetchInstanceByAttribute("DatabaseIdentifier", "identifier", "IS NULL", null);
			if (!identifiers.isEmpty())
			{
				sb.append("The following DatabaseIdentifiers have NULL \"identifier\" values: \n");
				for (GKInstance identifier : identifiers)
				{
					report.addLine(Arrays.asList(identifier.getDBID().toString(), identifier.getDisplayName()));
				}
			}
			else
			{
				sb.append("All DatabaseIdentifiers have a value for \"identifier\"! :)\n");
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
