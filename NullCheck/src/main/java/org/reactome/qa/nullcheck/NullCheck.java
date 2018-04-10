package org.reactome.qa.nullcheck;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.exception.ReportException;

/**
 * This QA check mostly checks for entities with attributes that are NULL but should not be null.
 * It also checks a few other issues that are related, but a bit more complicated.
 * @author sshorser
 *
 */
public class NullCheck {
	public static void main(String[] args) {
		MySQLAdaptor currentDBA = null;
		try {
			InputStream input = new FileInputStream("src/main/resources/auth.properties");
			Properties prop = new Properties();
			prop.load(input);
			String user = prop.getProperty("user");
			String password = prop.getProperty("password");
			String host = prop.getProperty("host");
			String database = prop.getProperty("database");
			int port = Integer.parseInt(prop.getProperty("port", "3306"));
		
			currentDBA = new MySQLAdaptor(host, database, user, password, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		SimpleEntityChecker simpleEntityChecker = new SimpleEntityChecker();
		simpleEntityChecker.setAdaptor(currentDBA);
		DelimitedTextReport simpleEntityReport = (DelimitedTextReport) simpleEntityChecker.executeQACheck();
		try
		{
			simpleEntityReport.print("\t", System.out);
		}
		catch (IOException | ReportException e1)
		{
			e1.printStackTrace();
		}
//		List<String> simpleEntityReportLines = getSimpleEntityReportLines(currentDBA);
//		if (!simpleEntityReportLines.isEmpty()) {
//			//TODO: Switch to better logging framework than stdout.
//			System.out.println("There are "+simpleEntityReportLines.size()+" SimpleEntities with a non-null species. Details are:");
//			for (String line : simpleEntityReportLines)
//			{
//				System.out.println(line);
//			}			
//		} else {	
//			System.out.println("SimpleEntities with non-null species: there are none! :)");
//		}

		
//		report(currentDBA, "PhysicalEntity", "compartment", "IS NULL", null);
//		
//		try {
//			List<String> physicalEntitySpeciesLines = getPhysicalEntitySpeciesReportLines(currentDBA);
//			if(!physicalEntitySpeciesLines.isEmpty()) {
//				System.out.println("There are " + physicalEntitySpeciesLines.size() + " PhysicalEntities with a null species but "
//						+ "having components with species");
//				for (String line : physicalEntitySpeciesLines) {
//					System.out.println(line);
//				}
//			} else {
//				System.out.println("PhysicalEntities with null species having components with species: there are none! :)");
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		PhysicalEntityChecker physicalEntityChecker = new PhysicalEntityChecker();
		physicalEntityChecker.setAdaptor(currentDBA);
		DelimitedTextReport physicalEntityReport = (DelimitedTextReport) physicalEntityChecker.executeQACheck();
		try
		{
			physicalEntityReport.print("\t", System.out);
		}
		catch (IOException | ReportException e1)
		{
			e1.printStackTrace();
		}
		
//		try {
//			report(currentDBA, "ReactionlikeEvent", "compartment", "IS NULL", getRLECompartmentSkipList());
//		} catch (IOException e) {
//			System.err.println("Unable to get RLE compartment skip list");
//			e.printStackTrace();
//		}
//		try {
//			report(currentDBA, "ReactionlikeEvent", "input", "IS NULL", getRLEInputSkipList());
//		} catch (IOException e) {
//			System.err.println("Unable to get RLE input skip list");
//			e.printStackTrace();
//		}
//		try {
//			report(currentDBA, "ReactionlikeEvent", "output", "IS NULL", getRLEOutputSkipList());
//		} catch (IOException e) {
//			System.err.println("Unable to get RLE output skip list");
//			e.printStackTrace();
//		}
		
		ReactionLikeEventChecker rleChecker = new ReactionLikeEventChecker();
		rleChecker.setAdaptor(currentDBA);
		DelimitedTextReport rleReport = (DelimitedTextReport) rleChecker.executeQACheck();
		try
		{
			rleReport.print("\t", System.out);
		}
		catch (IOException | ReportException e)
		{
			e.printStackTrace();
		}
		
//		List<String> normalReactionWithoutDiseaseReportLines = getNormalReactionWithoutDiseaseReportLines(currentDBA);
//		if (!normalReactionWithoutDiseaseReportLines.isEmpty()) {
//			System.out.println("There are "+normalReactionWithoutDiseaseReportLines.size()+" RLEs with a normal reaction but null disease");
//			for (String line : normalReactionWithoutDiseaseReportLines)
//			{
//				System.out.println(line);
//			}			
//		} else {	
//			System.out.println("RLEs with a normal reaction but null disease: there are none! :)");
//		}
		report(currentDBA, "FailedReaction", "normalReaction", "IS NULL", null);
		report(currentDBA, "FailedReaction", "output", "IS NOT NULL", null);
		
		List<GKInstance> newEvents = getNewEvents(currentDBA);
		reportNullAttribute(newEvents, "edited");
		reportNullAttribute(newEvents, "authored");
		reportNullAttribute(newEvents, "reviewed");
		List<String> RLENotInferredAndNoLiteratureReferenceReportLines = getRLENotInferredAndNoLiteratureReferenceReportLines(newEvents);
		if (!RLENotInferredAndNoLiteratureReferenceReportLines.isEmpty()) {
			System.out.println("There are "+RLENotInferredAndNoLiteratureReferenceReportLines.size()+" non-inferred RLEs without a literature reference");
			for (String line : RLENotInferredAndNoLiteratureReferenceReportLines)
			{
				System.out.println(line);
			}			
		} else {	
			System.out.println("Non-inferred RLEs without a literature reference: there are none! :)");
		}		
		reportNullAttribute(newEvents, "summation");
		reportNullAttribute(newEvents, "species");
	}
	
//	private static List<String> getSimpleEntityReportLines(MySQLAdaptor currentDBA) {
//		List<String> simpleEntityReportLines = new ArrayList<String>();
//		
//		List<GKInstance> simpleEntities = new ArrayList<GKInstance>();
//		simpleEntities.addAll(getInstancesWithNonNullAttribute(currentDBA, "SimpleEntity", "species", null));
//			
//		for (GKInstance simpleEntity : simpleEntities) {
//			try {
//				GKInstance speciesInstance = (GKInstance) simpleEntity.getAttributeValue("species");
//				if (speciesInstance == null) {
//					continue;
//				}
//				simpleEntityReportLines.add(getReportLine(simpleEntity, "Simple entity with non-null species"));
//			} catch (InvalidAttributeException e) {
//				e.printStackTrace();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		
//		return simpleEntityReportLines;
//	}
	
	private static void report(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList) {
		List<String> reportLines = getReportLines(dba, schemaClass, attribute, operator, skipList);
		
		if (!reportLines.isEmpty()) {
			System.out.println("There are " + reportLines.size() + " " + schemaClass + " instances with a null " + attribute);
			for (String line : reportLines) {
				System.out.println(line);
			}
		} else {
			System.out.println(schemaClass + " instances with null " + attribute + ": there are none! :)");
		}
	}
	
	private static void reportNullAttribute(List<GKInstance> instances, String attribute) {
		List<String> reportLines = getReportLines(instances, attribute);
		
		if (!reportLines.isEmpty()) {
			System.out.println("There are " + reportLines.size() + " instances with a null " + attribute);
			for (String line : reportLines) {
				System.out.println(line);
			}
		} else {
			System.out.println("Instances with null " + attribute + ": there are none! :)");
		}
	}
	
	private static List<String> getReportLines(MySQLAdaptor currentDBA, String schemaClass, String attribute, String operator, List<Long> skipList) {
		List<String> reportLines = new ArrayList<String>();
		
		List<GKInstance> instances = new ArrayList<GKInstance>();
		instances.addAll(getInstances(currentDBA, schemaClass, attribute, operator, skipList));
	
		for (GKInstance instance : instances) {
			reportLines.add(getReportLine(instance, schemaClass + " with null " + attribute));
		}
		
		return reportLines;
	}
	
	private static List<String> getReportLines(List<GKInstance> instances, String attribute) {
		List<String> reportLines = new ArrayList<String>();
		
		for (GKInstance instance : instances) {
			try {
				Object attributeValue = instance.getAttributeValue(attribute);
				if (attributeValue == null) {
					reportLines.add(getReportLine(instance, "Instance with null " + attribute));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return reportLines;
	}

//	private static List<String> getPhysicalEntitySpeciesReportLines(MySQLAdaptor currentDBA) throws Exception {
//		List<String> physicalEntitySpeciesReportLines = new ArrayList<String>();
//		
//		List<GKInstance> physicalEntities = new ArrayList<GKInstance>();
//		for (String schemaClass : Arrays.asList("Complex", "EntitySet", "Polymer")) {	
//			physicalEntities.addAll(getInstancesWithNullAttribute(currentDBA, schemaClass, "species", null));
//		}
//		
//		for (GKInstance physicalEntity : physicalEntities) {
//			if(componentsHaveSpecies(physicalEntity)) {
//				physicalEntitySpeciesReportLines.add(getReportLine(physicalEntity, "Null species but components with species"));
//			}
//		}
//		
//		return physicalEntitySpeciesReportLines;
//	}
	
	private static List<String> getNormalReactionWithoutDiseaseReportLines(MySQLAdaptor currentDBA) {
		List<String> normalReactionWithoutDiseaseReportLines = new ArrayList<String>();
		List<GKInstance> RLEsWithNormalReaction = new ArrayList<GKInstance>();
		try {
			RLEsWithNormalReaction.addAll(getInstancesWithNonNullAttribute(currentDBA, "ReactionlikeEvent", "normalReaction", null));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for (GKInstance RLEWithNormalReaction : RLEsWithNormalReaction) {
			try {
				GKInstance diseaseInstance = (GKInstance) RLEWithNormalReaction.getAttributeValue("disease");
				if (diseaseInstance == null) {
					normalReactionWithoutDiseaseReportLines.add(getReportLine(RLEWithNormalReaction, "RLE with normal reaction but disease is null"));
				}
			} catch (InvalidAttributeException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return normalReactionWithoutDiseaseReportLines;
	}
	
	private static List<GKInstance> getNewEvents(MySQLAdaptor currentDBA) {
		List<GKInstance> newEvents = new ArrayList<GKInstance>();
		try {
			List<GKInstance> reactionLikeEvents = getInstances(currentDBA, "ReactionlikeEvent", "stableIdentifier", "IS NOT NULL", null);
			for (GKInstance reactionLikeEvent : reactionLikeEvents) {
				GKInstance RLEStableIdentifier = (GKInstance) reactionLikeEvent.getAttributeValue("stableIdentifier");
				Boolean releasedAttribute = (Boolean) RLEStableIdentifier.getAttributeValue("released");
				if (releasedAttribute == null || !releasedAttribute) {
					newEvents.add(reactionLikeEvent);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newEvents;
	}
	
	private static List<String> getRLENotInferredAndNoLiteratureReferenceReportLines(List<GKInstance> RLEs) {
		List<String> RLENotInferredAndNoLiteratureReferenceReportLines = new ArrayList<String>();
		
		for (GKInstance RLE : RLEs) {
			try {
				GKInstance inferredFrom = (GKInstance) RLE.getAttributeValue("inferredFrom");
				GKInstance literatureReference = (GKInstance) RLE.getAttributeValue("literatureReference");
				
				if (inferredFrom == null && literatureReference == null) {
					RLENotInferredAndNoLiteratureReferenceReportLines.add(getReportLine(RLE, "RLE not inferred and no literature reference"));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return RLENotInferredAndNoLiteratureReferenceReportLines;
	}
	
	private static List<GKInstance> getInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList) {
		List<GKInstance> instances = new ArrayList<GKInstance>();
		try {
			instances.addAll(dba.fetchInstanceByAttribute(schemaClass, attribute, operator, null));
			
			if (skipList != null && !skipList.isEmpty())
			{
				//List<GKInstance> filteredList = instances.parallelStream().filter(inst -> skipList.contains(inst.getDBID())).collect(Collectors.toList());
				return instances.parallelStream().filter(inst -> skipList.contains(inst.getDBID())).collect(Collectors.toList());
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
	
//	private static List<GKInstance> getInstancesWithNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, List<Long> skipList) {
//		return getInstances(dba, schemaClass, attribute, "IS NULL", skipList);
//	}
	
	private static List<GKInstance> getInstancesWithNonNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, List<Long> skipList)  {
		return getInstances(dba, schemaClass, attribute, "IS NOT NULL", skipList);
	}
	
//	private static boolean componentsHaveSpecies(GKInstance physicalEntity) throws Exception {
//		Set<GKInstance> speciesSet = grepAllSpeciesInPE(physicalEntity, true);
//		return !speciesSet.isEmpty();		
//	}
	
	private static Set<GKInstance> grepAllSpeciesInPE(GKInstance pe, boolean needRecursion) throws Exception {
		Set<GKInstance> speciesSet = new HashSet<>();
		if (pe.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
			List<GKInstance> speciesList = pe.getAttributeValuesList(ReactomeJavaConstants.species);
			if (speciesList != null && speciesList.size() > 0) {
				speciesSet.addAll(speciesList);
			}
		}
		if (speciesSet.size() == 0 && needRecursion) {
			grepAllSpeciesInPE(pe, speciesSet);
		}
		return speciesSet;
	}

	private static void grepAllSpeciesInPE(GKInstance pe, Set<GKInstance> speciesSet) throws Exception {
		Set<GKInstance> wrappedPEs = InstanceUtilities.getContainedInstances(pe,
				ReactomeJavaConstants.hasComponent,
				ReactomeJavaConstants.hasCandidate,
				ReactomeJavaConstants.hasMember,
				ReactomeJavaConstants.repeatedUnit);
		for (GKInstance wrappedPE : wrappedPEs) {
			Set<GKInstance> wrappedSpecies = grepAllSpeciesInPE(wrappedPE, true);
			speciesSet.addAll(wrappedSpecies);
		}
	}
	
	private static String getLastModificationAuthor(GKInstance instance) {
		final String noAuthor = "No modification or creation author";
		
		GKInstance mostRecentMod = null;
		try {
			List<GKInstance> modificationInstances = (List<GKInstance>) instance.getAttributeValuesList("modified");
			for (int index = modificationInstances.size() - 1; index > 0; index--) {
				GKInstance modificationInstance = modificationInstances.get(index);
				GKInstance author = (GKInstance) modificationInstance.getAttributeValue("author");
				// Skip modification instance for Solomon, Joel, or Guanming
				if (Arrays.asList("8939149", "1551959", "140537").contains(author.getDBID().toString())) {
					continue;
				}
				mostRecentMod = modificationInstance;
				break;
			}
		} catch (InvalidAttributeException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (mostRecentMod == null) {
			GKInstance created = null;
			try {
				created = (GKInstance) instance.getAttributeValue("created");
			} catch (InvalidAttributeException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (created != null) { 
				return created.getDisplayName();
			} else {
				return noAuthor;
			}
		}
		
		return mostRecentMod.getDisplayName();	
	}
	
	private static String getReportLine(GKInstance instance, String instanceIssue) {
		return String.join("\t", Arrays.asList(
			instance.getDBID().toString(),
			instance.getDisplayName(),
			instance.getSchemClass().getName(),
			instanceIssue,
			getLastModificationAuthor(instance)
		));
	}
	
//	private static List<Long> getRLECompartmentSkipList() throws IOException {
//		final String filePath = "src/main/resources/reaction_like_event_compartment_skip_list.txt";
//		
//		return getSkipList(filePath);
//	}
//	
//	private static List<Long> getRLEInputSkipList() throws IOException {
//		final String filePath = "src/main/resources/reaction_like_event_input_skip_list.txt";
//		
//		return getSkipList(filePath);
//	}
//	
//	private static List<Long> getRLEOutputSkipList() throws IOException {
//		final String filePath = "src/main/resources/reaction_like_event_output_skip_list.txt";
//	
//		return getSkipList(filePath);
//	}
//	
//	private static List<Long> getSkipList(String filePath) throws IOException {
//		List<Long> skipList = new ArrayList<Long>();
//		Files.readAllLines(Paths.get(filePath)).forEach(line -> {
//			Long dbId = Long.parseLong(line.split("\t")[0]);
//			skipList.add(dbId);
//		});
//		return skipList;
//	}
}
