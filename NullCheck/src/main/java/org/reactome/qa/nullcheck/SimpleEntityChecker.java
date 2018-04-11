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

public class SimpleEntityChecker implements QACheck
{
	private MySQLAdaptor dba;
	
	private List<GKInstance> getInstancesWithNonNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, List<Long> skipList) 
	{
		return getInstances(dba, schemaClass, attribute, "IS NOT NULL", skipList);
	}
	
	private static List<GKInstance> getInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		List<GKInstance> instances = new ArrayList<GKInstance>();
		try
		{
			instances.addAll(dba.fetchInstanceByAttribute(schemaClass, attribute, operator, null));
			
			if (skipList != null && !skipList.isEmpty())
			{
				//List<GKInstance> filteredList = instances.parallelStream().filter(inst -> skipList.contains(inst.getDBID())).collect(Collectors.toList());
				return instances.parallelStream().filter(inst -> !skipList.contains(inst.getDBID())).collect(Collectors.toList());
			}
			else
			{
				return instances;
			}
			
//			if (skipList != null && !skipList.isEmpty()) {
//				Iterator<GKInstance> instanceIterator = instances.iterator();
//				
//				while(instanceIterator.hasNext()) {
//					GKInstance instance = instanceIterator.next();
//					if (skipList.contains(instance.getDBID())) {
//						//instances.remove(instance);
//						instanceIterator.remove();
//					}
//				}
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return instances;
	}
	
	public void setAdaptor(MySQLAdaptor adaptor)
	{
		this.dba = adaptor;
	}

	private String getLastModificationAuthor(GKInstance instance)
	{
		final String noAuthor = "No modification or creation author";
		
		GKInstance mostRecentMod = null;
		try
		{
			List<GKInstance> modificationInstances = (List<GKInstance>) instance.getAttributeValuesList("modified");
			for (int index = modificationInstances.size() - 1; index > 0; index--)
			{
				GKInstance modificationInstance = modificationInstances.get(index);
				GKInstance author = (GKInstance) modificationInstance.getAttributeValue("author");
				// Skip modification instance for Solomon, Joel, or Guanming
				if (Arrays.asList("8939149", "1551959", "140537").contains(author.getDBID().toString()))
				{
					continue;
				}
				mostRecentMod = modificationInstance;
				break;
			}
		}
		catch (InvalidAttributeException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		if (mostRecentMod == null)
		{
			GKInstance created = null;
			try
			{
				created = (GKInstance) instance.getAttributeValue("created");
			}
			catch (InvalidAttributeException e)
			{
				e.printStackTrace();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
			if (created != null)
			{ 
				return created.getDisplayName();
			}
			else
			{
				return noAuthor;
			}
		}
		return mostRecentMod.getDisplayName();	
	}
	
	@Override
	public Report executeQACheck()
	{
		Report report = new DelimitedTextReport();
		List<GKInstance> instances = this.getInstancesWithNonNullAttribute(dba, "SimpleEntity", "species", null);
		for (GKInstance simpleEntity : instances)
		{
			try
			{
				GKInstance speciesInstance = (GKInstance) simpleEntity.getAttributeValue("species");
				report.addLine(Arrays.asList(simpleEntity.getDBID().toString(), simpleEntity.getDisplayName(), simpleEntity.getSchemClass().getName(), "Simple entity with non-null species" + speciesInstance.toString(), getLastModificationAuthor(simpleEntity) ));
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
