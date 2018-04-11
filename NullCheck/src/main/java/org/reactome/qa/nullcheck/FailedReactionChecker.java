package org.reactome.qa.nullcheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.reactome.qa.QACheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

public class FailedReactionChecker implements QACheck
{

	private MySQLAdaptor adaptor;
	
	public void setAdaptor (MySQLAdaptor dba)
	{
		this.adaptor = dba;
	}

//	private static String getLastModificationAuthor(GKInstance instance) {
//		final String noAuthor = "No modification or creation author";
//		
//		GKInstance mostRecentMod = null;
//		try {
//			List<GKInstance> modificationInstances = (List<GKInstance>) instance.getAttributeValuesList("modified");
//			for (int index = modificationInstances.size() - 1; index > 0; index--) {
//				GKInstance modificationInstance = modificationInstances.get(index);
//				GKInstance author = (GKInstance) modificationInstance.getAttributeValue("author");
//				// Skip modification instance for Solomon, Joel, or Guanming
//				if (Arrays.asList("8939149", "1551959", "140537").contains(author.getDBID().toString())) {
//					continue;
//				}
//				mostRecentMod = modificationInstance;
//				break;
//			}
//		} catch (InvalidAttributeException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		if (mostRecentMod == null) {
//			GKInstance created = null;
//			try {
//				created = (GKInstance) instance.getAttributeValue("created");
//			} catch (InvalidAttributeException e) {
//				e.printStackTrace();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			
//			if (created != null) { 
//				return created.getDisplayName();
//			} else {
//				return noAuthor;
//			}
//		}
//		
//		return mostRecentMod.getDisplayName();	
//	}
	
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
	
//	@SuppressWarnings("unchecked")
//	private List<GKInstance> getInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
//	{
//		List<GKInstance> instances = new ArrayList<GKInstance>();
//		try
//		{
//			instances.addAll(dba.fetchInstanceByAttribute(schemaClass, attribute, operator, null));
//			
//			if (skipList != null && !skipList.isEmpty())
//			{
//				return instances.parallelStream().filter(inst -> !skipList.contains(inst.getDBID())).collect(Collectors.toList());
//			}
//			else
//			{
//				return instances;
//			}
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//		return instances;
//	}
	
	@Override
	public Report executeQACheck()
	{
		Report r;
		r = report("FailedReaction", "normalReaction", "IS NULL", null);
		r.addLines( report("FailedReaction", "output", "IS NOT NULL", null).getReportLines() );
		r.setColumnHeaders(Arrays.asList("DBID","DisplayName","SchemaClass","Issue","MostRecentAuthor"));
		return r;
	}

}
