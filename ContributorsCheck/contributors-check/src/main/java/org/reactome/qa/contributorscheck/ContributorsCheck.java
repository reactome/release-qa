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
    	//ContributorsCheck.checkContributors("src/main/resources/update_doi.log", "test_slice_64", "reactomerelease.oicr.on.ca", "test_slice_63", "reactomerelease.oicr.on.ca");
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
			// System.out.println(String.join("\t", Arrays.asList("Pathway Name", "Pathway DB_ID", "HasEvent Name", "HasEvent DB_ID", "Contributors")));
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
						List<String> allAuthorNames = getAllAuthorNames(currentPathwayChild, previousPathwayChild);
								
						//String record = getRecord(currentPathway, currentPathwayChild, allAuthorNames);
						//System.out.println(record);
						report.addLine( Arrays.asList(currentPathway.toString() , currentPathwayChild.toString() ,String.join(",", allAuthorNames) )  );
					}
				}
			});
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
    	
    	return childEvents;
    }
    
    private static List<String> getAllAuthorNames(GKInstance currentEvent, GKInstance previousEvent) {
		List <String> allAuthorNames = new ArrayList<String>();
    	try {
			List<GKInstance> newAuthorInstances = getNewInstances("authored", currentEvent, previousEvent);
			List<String> newAuthorNames = getAuthorNames(newAuthorInstances);
			List<GKInstance> newReviewedInstances = getNewInstances("reviewed", currentEvent, previousEvent);
			List<String> newReviewedAuthorNames = getAuthorNames(newReviewedInstances);
			List<GKInstance> newRevisedInstances = getNewInstances("revised", currentEvent, previousEvent);
			List<String> newRevisedAuthorNames = getAuthorNames(newRevisedInstances);
		
			allAuthorNames.addAll(newAuthorNames);
			allAuthorNames.addAll(newReviewedAuthorNames);
			allAuthorNames.addAll(newRevisedAuthorNames);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return allAuthorNames;
    }
    
    private static List<GKInstance> getNewInstances(String attribute, GKInstance currentInstance, GKInstance previousInstance) throws InvalidAttributeException, Exception {
    		@SuppressWarnings("unchecked")
    		List<GKInstance> currentAttributeValues = currentInstance.getAttributeValuesList(attribute);
    		if (previousInstance == null) {
    			return currentAttributeValues;
    		}
    	
    		@SuppressWarnings("unchecked")
    		List<GKInstance> previousAttributeValues = previousInstance.getAttributeValuesList(attribute);
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
    
//    private static String getRecord(GKInstance pathway, GKInstance hasEvent, List<String> authorNames) {
//    	StringBuilder record = new StringBuilder();
//    	final String DELIMITER = "\t";
//    	
//    	record.append(pathway.getDisplayName());
//    	record.append(DELIMITER);
//    	record.append(pathway.getDBID());
//    	record.append(DELIMITER);
//    	record.append(hasEvent.getDisplayName());
//    	record.append(DELIMITER);
//    	record.append(hasEvent.getDBID());
//    	
//    	for (String authorName: authorNames) {
//    		record.append(DELIMITER);
//    		record.append(authorName);
//       	}
//    	
//    	return record.toString();
//    }
}
