package org.reactome.release.qa;

import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.common.DelimitedTextReport;
import org.reactome.release.qa.common.QACheck;
import org.reactome.release.qa.common.QAReport;

/**
 * Not sure what this check is for.
 *
 */
public class SimpleEntityChecker implements QACheck
{
	private MySQLAdaptor dba;
	
	
	public void setAdaptor(MySQLAdaptor adaptor)
	{
		this.dba = adaptor;
	}

	@Override
	public QAReport executeQACheck()
	{
		QAReport report = new DelimitedTextReport();
		List<GKInstance> instances = NullCheckHelper.getInstancesWithNonNullAttribute(dba, "SimpleEntity", "species", null);
		for (GKInstance simpleEntity : instances)
		{
			try
			{
				GKInstance speciesInstance = (GKInstance) simpleEntity.getAttributeValue("species");
				report.addLine(Arrays.asList(simpleEntity.getDBID().toString(), simpleEntity.getDisplayName(), simpleEntity.getSchemClass().getName(), "Simple entity with non-null species" + speciesInstance.toString(), NullCheckHelper.getLastModificationAuthor(simpleEntity) ));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		report.setColumnHeaders(Arrays.asList("DBID","DisplayName","SchemaClass","Issue","MostRecentAuthor"));
		return report;
	}

}
