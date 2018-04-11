package org.reactome.qa.nullcheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.qa.QACheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

public class FailedReactionChecker implements QACheck
{
	private static final String schemaClassName = "FailedReaction";
	private MySQLAdaptor adaptor;
	
	public void setAdaptor (MySQLAdaptor dba)
	{
		this.adaptor = dba;
	}

	private Report report(String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		Report r = new DelimitedTextReport();
		
		List<GKInstance> instances = new ArrayList<GKInstance>();
		instances.addAll(NullCheckHelper.getInstances(this.adaptor, schemaClass, attribute, operator, skipList));

		for (GKInstance instance : instances)
		{
			r.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), attribute + " " + operator  , NullCheckHelper.getLastModificationAuthor(instance)));
		}

		return r;
	}
	
	@Override
	public Report executeQACheck()
	{
		Report r;
		
		r = report(FailedReactionChecker.schemaClassName, "normalReaction", NullCheckHelper.IS_NULL, null);
		r.addLines( report(FailedReactionChecker.schemaClassName, "output", NullCheckHelper.IS_NOT_NULL, null).getReportLines() );
		r.setColumnHeaders(Arrays.asList("DBID","DisplayName","SchemaClass","Issue","MostRecentAuthor"));
		return r;
	}

}
