package org.reactome.qa.nullcheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.qa.QACheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

public class PhysicalEntityChecker implements QACheck
{
	private MySQLAdaptor dba;
	
	public void setAdaptor(MySQLAdaptor adaptor)
	{
		this.dba = adaptor;
	}

	@Override
	public Report executeQACheck()
	{
		Report report = new DelimitedTextReport();
		List<GKInstance> physicalEntities = new ArrayList<GKInstance>();
		for (String schemaClass : Arrays.asList("Complex", "EntitySet", "Polymer"))
		{	
			physicalEntities.addAll(NullCheckHelper.getInstancesWithNullAttribute(this.dba, schemaClass, "species", null));
		}
		
		for (GKInstance physicalEntity : physicalEntities)
		{
			int numComponents = 0;
			try
			{
				numComponents = NullCheckHelper.componentsHaveSpecies(physicalEntity);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			if(numComponents > 0)
			{
				//physicalEntitySpeciesReportLines.add(getReportLine(physicalEntity, "Null species but components with species"));
				report.addLine(Arrays.asList());
				report.addLine(Arrays.asList(physicalEntity.getDBID().toString(), physicalEntity.getDisplayName(), physicalEntity.getSchemClass().getName(), "NULL species but "+numComponents+" components have a species", NullCheckHelper.getLastModificationAuthor(physicalEntity) ));
			}
		}
		report.setColumnHeaders(Arrays.asList("DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor"));
		return report;
	}

}
