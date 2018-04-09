package org.reactome.qa.contributorscheck;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.InvalidAttributeException;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

/**
 * Hello world!
 *
 */
public class ContributorsCheck 
{
    public static void main( String[] args )
    {
		String pathToResources = "src/main/resources/auth.properties";
		
		if (args.length > 0 && !args[0].equals(""))
		{
			pathToResources = args[0];
		}
		ContributorsCheck.checkContributors(pathToResources);
    }
    
    public static void checkContributors(String pathToResources) {
		try {
			InputStream input = new FileInputStream("src/main/resources/auth.properties");
			Properties prop = new Properties();
			prop.load(input);
			String user = prop.getProperty("user");
			String password = prop.getProperty("password");
			String currentDatabaseHost = prop.getProperty("currentDBHost");
			String currentDatabase = prop.getProperty("currentDatabase");
			String oldDatabaseHost = prop.getProperty("oldDBHost");
			String oldDatabase = prop.getProperty("oldDatabase");
			String inputFile = prop.getProperty("inputFile");
			MySQLAdaptor currentDBA = new MySQLAdaptor(currentDatabaseHost, currentDatabase, user, password, 3306);
			MySQLAdaptor previousDBA = new MySQLAdaptor(oldDatabaseHost, oldDatabase, user, password, 3306);
			Report report = new DelimitedTextReport();
			report.setColumnHeaders(Arrays.asList("Pathway Name", "Pathway DB_Id", "HasEvent Name", "HasEvent DB_ID", "Contributors"));
			Files.readAllLines(Paths.get(inputFile)).forEach( line -> {
				Pattern pattern = Pattern.compile("R-HSA-(\\d+)");
				Matcher matcher = pattern.matcher(line);
				if(matcher.find()) {
					long pathwayDbId = Long.parseLong(matcher.group(1));
				
					GKInstance currentPathway;
					try {
						currentPathway = currentDBA.fetchInstance(pathwayDbId);
					} catch (Exception e) {
						e.printStackTrace();
						return; // Need pathway instance to report contributors
					}
					List<GKInstance> currentPathwayChildren = getChildEvents(currentPathway);
					for (GKInstance currentPathwayChild : currentPathwayChildren) {
						GKInstance previousPathwayChild;
						try {
							previousPathwayChild = previousDBA.fetchInstance(currentPathwayChild.getDBID());
						} catch (Exception e) {
							// No instance in old database to compare contributors so take all contributors as new
							previousPathwayChild = null;
						}
						
						List<String> allAuthorNames = new ArrayList<String>();
						try
						{
							allAuthorNames = getAllAuthorNames(currentPathwayChild, previousPathwayChild);	
						}
						catch (Exception e)
						{
							e.getStackTrace();
						}
						report.addLine( Arrays.asList(currentPathway.toString() , currentPathwayChild.toString() , "\""+String.join("; ", allAuthorNames)+"\"" )  );
					}
				}
			});
			((DelimitedTextReport)report).print("\t",System.out);
		} catch (FileNotFoundException e){
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private static List<GKInstance> getChildEvents(GKInstance event) {
    	List<GKInstance> childEvents = new ArrayList<GKInstance>();
    	if (event != null)
    	{
	    	try {
				for (Object childEvent : event.getAttributeValuesList("hasEvent")) {
					childEvents.add((GKInstance) childEvent);
					if (((GKInstance) childEvent).getSchemClass().getName().equals("Reaction")) {
						continue;
					}
					
					List<GKInstance> grandChildren = getChildEvents((GKInstance) childEvent);
					if (!grandChildren.isEmpty()) {
						childEvents.addAll(grandChildren);
					}
				}
			} catch (InvalidAttributeException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	return childEvents;
    }
    
    private static List<String> getAllAuthorNames(GKInstance currentEvent, GKInstance previousEvent) throws InvalidAttributeException, Exception {
		List <String> allAuthorNames = new ArrayList<String>();

		List<GKInstance> newAuthorInstances = getNewInstances("authored", currentEvent, previousEvent);
		List<String> newAuthorNames = getAuthorNames(newAuthorInstances);

		List<GKInstance> newReviewedInstances = getNewInstances("reviewed", currentEvent, previousEvent);
		List<String> newReviewedAuthorNames = getAuthorNames(newReviewedInstances);

		List<GKInstance> newRevisedInstances = getNewInstances("revised", currentEvent, previousEvent);
		List<String> newRevisedAuthorNames = getAuthorNames(newRevisedInstances);
	
		allAuthorNames.addAll(newAuthorNames);
		allAuthorNames.addAll(newReviewedAuthorNames);
		allAuthorNames.addAll(newRevisedAuthorNames);

    	return allAuthorNames;
    }
    
    private static List<GKInstance> getNewInstances(String attribute, GKInstance currentInstance, GKInstance previousInstance) throws InvalidAttributeException, Exception
    {
    	List<GKInstance> currentAttributeValues = new ArrayList<GKInstance>();
		if (currentInstance.getSchemaAttributes().stream().filter(attr -> ((GKSchemaAttribute)attr).getName().equals(attribute) ).count() > 0)
		{
    		currentAttributeValues = (ArrayList<GKInstance>) (currentInstance.getAttributeValuesList(attribute));
    		if (previousInstance == null)
    		{
    			return currentAttributeValues;
    		}
		}
		else
		{
			System.err.println(currentInstance.toString() + " Does not have " + attribute);
		}
		if (previousInstance.getSchemaAttributes().stream().filter(attr -> ((GKSchemaAttribute)attr).getName().equals(attribute) ).count() > 0)
		{
			List<GKInstance> previousAttributeValues = (ArrayList<GKInstance>) (previousInstance.getAttributeValuesList(attribute));
			Map<Long, GKInstance> dbIdToPreviousAttributeValues = getDbIdsToInstance(previousAttributeValues);
			Map<Long, GKInstance> dbIdToCurrentAttributeValues = getDbIdsToInstance(currentAttributeValues);
		
			List<GKInstance> newInstances = new ArrayList<GKInstance>();
			for (Long dbId : dbIdToCurrentAttributeValues.keySet()) {
				if (!dbIdToPreviousAttributeValues.containsKey(dbId)) {
					GKInstance currentAttributeValue = dbIdToCurrentAttributeValues.get(dbId);
					newInstances.add(currentAttributeValue);
				}
			}
			return newInstances;
		}
		else
		{
			System.err.println(previousInstance.toString() + " Does not have " + attribute);
		}
		return new ArrayList<GKInstance>();
    }
    
    private static Map<Long, GKInstance> getDbIdsToInstance(List<GKInstance> instances) {
    	Map<Long, GKInstance> dbIdToInstance = new HashMap<Long, GKInstance>();
    	for (GKInstance instance : instances) {
    		dbIdToInstance.put(instance.getDBID(), instance);
    	}    	
    	return dbIdToInstance;
    }
    
    private static List<String> getAuthorNames(List<GKInstance> instanceEdits) {
    	List<String> authorNames = new ArrayList<String>();
    	for (GKInstance instanceEdit : instanceEdits) {
    		try {
				for (Object person : instanceEdit.getAttributeValuesList("author")) {
					String personName = ((GKInstance) person).getDisplayName();
					authorNames.add(personName);
				}
			} catch (InvalidAttributeException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	return authorNames;
    }
}
