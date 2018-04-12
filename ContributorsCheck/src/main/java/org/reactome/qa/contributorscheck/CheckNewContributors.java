package org.reactome.qa.contributorscheck;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.InvalidAttributeException;
import org.reactome.qa.QACheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

public class CheckNewContributors implements QACheck
{
	private MySQLAdaptor currentDBA;
	private MySQLAdaptor previousDBA;
	private String inputFile;
	
	public CheckNewContributors() {}
	
	
	public CheckNewContributors(MySQLAdaptor currentDB, MySQLAdaptor prevDB, String pathToInputFile)
	{
		this.currentDBA = currentDB;
		this.previousDBA = prevDB;
		this.inputFile = pathToInputFile;
	}
	
	public void setCurrentDBAdaptor(MySQLAdaptor dba)
	{
		this.currentDBA = dba;
	}
	
	public void setPreviousDBAdaptor(MySQLAdaptor dba)
	{
		this.previousDBA = dba;
	}
	
	public void setInputFilePath(String path)
	{
		this.inputFile = path;
	}
	
	private List<GKInstance> getNewInstances(String attribute, GKInstance currentInstance, GKInstance previousInstance) throws InvalidAttributeException, Exception
	{
		List<GKInstance> currentAttributeValues = new ArrayList<GKInstance>();
		//Check that the attribute is valid for the GKInstance.
		if (currentInstance.getSchemClass().isValidAttribute(attribute))
		{
			currentAttributeValues = (ArrayList<GKInstance>) (currentInstance.getAttributeValuesList(attribute));
			if (previousInstance == null)
			{
				return currentAttributeValues;
			}
		}
		else
		{
			System.err.println("DEBUG currentInstance: "+currentInstance.toString() + " Does not have " + attribute);
		}
		if (previousInstance.getSchemClass().isValidAttribute(attribute))
		{
			@SuppressWarnings("unchecked")
			List<GKInstance> previousAttributeValues = (ArrayList<GKInstance>) (previousInstance.getAttributeValuesList(attribute));
			Map<Long, GKInstance> dbIdToPreviousAttributeValues = getDbIdsToInstance(previousAttributeValues);
			Map<Long, GKInstance> dbIdToCurrentAttributeValues = getDbIdsToInstance(currentAttributeValues);
		
			List<GKInstance> newInstances = new ArrayList<GKInstance>();
			for (Long dbId : dbIdToCurrentAttributeValues.keySet())
			{
				if (!dbIdToPreviousAttributeValues.containsKey(dbId))
				{
					GKInstance currentAttributeValue = dbIdToCurrentAttributeValues.get(dbId);
					newInstances.add(currentAttributeValue);
				}
			}
			return newInstances;
		}
		else
		{
			System.err.println("DEBUG prevInstance: "+previousInstance.toString() + " Does not have " + attribute);
		}
		return new ArrayList<GKInstance>();
	}

	private static Map<Long, GKInstance> getDbIdsToInstance(List<GKInstance> instances)
	{
		Map<Long, GKInstance> dbIdToInstance = new HashMap<Long, GKInstance>();
		for (GKInstance instance : instances)
		{
			dbIdToInstance.put(instance.getDBID(), instance);
		}
		return dbIdToInstance;
	}

	private List<String> getAuthorNames(List<GKInstance> instanceEdits)
	{
		List<String> authorNames = new ArrayList<String>();
		for (GKInstance instanceEdit : instanceEdits)
		{
			try
			{
				for (Object person : instanceEdit.getAttributeValuesList("author"))
				{
					String personName = ((GKInstance) person).getDisplayName();
					authorNames.add(personName);
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
		}
		return authorNames;
	}

	private List<GKInstance> getChildEvents(GKInstance event)
	{
		List<GKInstance> childEvents = new ArrayList<GKInstance>();
		if (event != null)
		{
			try
			{
				for (Object childEvent : event.getAttributeValuesList("hasEvent"))
				{
					childEvents.add((GKInstance) childEvent);
					//if (((GKInstance) childEvent).getSchemClass().getName().equals("Reaction"))
					if (((GKInstance) childEvent).getSchemClass().isa("Reaction"))
					{
						continue;
					}
					
					List<GKInstance> grandChildren = getChildEvents((GKInstance) childEvent);
					if (!grandChildren.isEmpty())
					{
						childEvents.addAll(grandChildren);
					}
				}
			}
			catch (InvalidAttributeException e)
			{
				System.err.println("That was an invalid attribute!");
				e.printStackTrace();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return childEvents;
	}

	private List<String> getAllAuthorNames(GKInstance currentEvent, GKInstance previousEvent) throws InvalidAttributeException, Exception
	{
		List <String> allAuthorNames = new ArrayList<String>();

		List<GKInstance> newAuthorInstances = this.getNewInstances("authored", currentEvent, previousEvent);
		List<String> newAuthorNames = this.getAuthorNames(newAuthorInstances);

		List<GKInstance> newReviewedInstances = this.getNewInstances("reviewed", currentEvent, previousEvent);
		List<String> newReviewedAuthorNames = this.getAuthorNames(newReviewedInstances);

		List<GKInstance> newRevisedInstances = this.getNewInstances("revised", currentEvent, previousEvent);
		List<String> newRevisedAuthorNames = this.getAuthorNames(newRevisedInstances);
	
		allAuthorNames.addAll(newAuthorNames);
		allAuthorNames.addAll(newReviewedAuthorNames);
		allAuthorNames.addAll(newRevisedAuthorNames);

		return allAuthorNames;
	}

	@Override
	public Report executeQACheck()
	{
		Report report = new DelimitedTextReport();
		report.setColumnHeaders(Arrays.asList("Pathway Name", "Pathway DB_Id", "HasEvent Name", "HasEvent DB_ID", "Contributors"));
		try
		{
			Files.readAllLines(Paths.get(inputFile)).forEach( line -> {
				Pattern pattern = Pattern.compile("R-HSA-(\\d+)");
				Matcher matcher = pattern.matcher(line);
				if(matcher.find())
				{
					long pathwayDbId = Long.parseLong(matcher.group(1));
					GKInstance currentPathway;
					try
					{
						currentPathway = currentDBA.fetchInstance(pathwayDbId);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						return; // Need pathway instance to report contributors
					}
					List<GKInstance> currentPathwayChildren = this.getChildEvents(currentPathway);
					for (GKInstance currentPathwayChild : currentPathwayChildren)
					{
						GKInstance previousPathwayChild;
						try 
						{
							previousPathwayChild = previousDBA.fetchInstance(currentPathwayChild.getDBID());
						}
						catch (Exception e)
						{
							// No instance in old database to compare contributors so take all contributors as new
							previousPathwayChild = null;
						}
						
						List<String> allAuthorNames = new ArrayList<String>();
						try
						{
							allAuthorNames = this.getAllAuthorNames(currentPathwayChild, previousPathwayChild);	
						}
						catch (Exception e)
						{
							e.getStackTrace();
						}
						report.addLine( Arrays.asList(currentPathway.toString() , currentPathwayChild.toString() , "\""+String.join("; ", allAuthorNames)+"\"" )  );
					}
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return report;
	}
}
