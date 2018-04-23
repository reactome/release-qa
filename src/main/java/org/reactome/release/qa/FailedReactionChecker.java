package org.reactome.release.qa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.common.DelimitedTextReport;
import org.reactome.release.qa.common.QACheck;
import org.reactome.release.qa.common.QAReport;

/**
 * 
 * This class is to check two things for FailedReaction: 1). A FailedReaction must have
 * a nornalReaction (Mandatory); 2). A FaiedReaction cannot have output (Note: We may have
 * to change the data model to enforce this!)
 *
 */
public class FailedReactionChecker implements QACheck
{
	private static final String schemaClassName = "FailedReaction";
	private MySQLAdaptor adaptor;
	
	public void setAdaptor (MySQLAdaptor dba)
	{
		this.adaptor = dba;
	}

	private QAReport report(String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		QAReport r = new DelimitedTextReport();
		
		List<GKInstance> instances = new ArrayList<GKInstance>();
		instances.addAll(NullCheckHelper.getInstances(this.adaptor, schemaClass, attribute, operator, skipList));

		for (GKInstance instance : instances)
		{
			r.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), attribute + " " + operator  , NullCheckHelper.getLastModificationAuthor(instance)));
		}

		return r;
	}
	
	@Override
	public QAReport executeQACheck()
	{
		QAReport r;
		
		r = report(FailedReactionChecker.schemaClassName, "normalReaction", NullCheckHelper.IS_NULL, null);
		r.addLines( report(FailedReactionChecker.schemaClassName, "output", NullCheckHelper.IS_NOT_NULL, null).getReportLines() );
		r.setColumnHeaders(Arrays.asList("DBID","DisplayName","SchemaClass","Issue","MostRecentAuthor"));
		return r;
	}

}
