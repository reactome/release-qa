package org.reactome.release.qa.check;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;

public class QACheckerHelper {
    
	static final String IS_NOT_NULL = "IS NOT NULL";
	static final String IS_NULL = "IS NULL";
	
	/**
	 * Filter a list of GKInstance objects by the DB IDs in skipList.
	 * @param skipList - the skipList.
	 * @param instances - Objects from the database. Any object whose DB_ID is in skipList will *not* be in the output.
	 * @return
	 */
	static List<GKInstance> filterBySkipList(List<Long> skipList, List<GKInstance> instances)
	{
		if (skipList != null && !skipList.isEmpty())
		{
			return instances.parallelStream().filter(inst -> !skipList.contains(inst.getDBID())).collect(Collectors.toList());
		}
		else
		{
			return instances;
		}
	}
	
    public static boolean isChimeric(GKInstance rle) throws Exception {
        if (!rle.getSchemClass().isValidAttribute(ReactomeJavaConstants.isChimeric))
            return false;
        Boolean value = (Boolean) rle.getAttributeValue(ReactomeJavaConstants.isChimeric);
        if (value == null || !value)
            return false;
        return true;
    }
	
	@SuppressWarnings("unchecked")
	public static GKInstance getHuman(MySQLAdaptor dba) throws Exception {
	    Collection<GKInstance> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Species,
	            ReactomeJavaConstants._displayName,
	            "=", 
	            "Homo sapiens");
	    if (c == null || c.size() == 0)
	        throw new IllegalStateException("Cannot find species Homo sapiens in the database, " + 
	                                        dba.getDBName() + "@" + dba.getDBHost());
	    return c.iterator().next();
	}
	
	static List<Long> getSkipList(String filePath) throws IOException
	{
		List<Long> skipList = new ArrayList<Long>();
		if (filePath == null)
		    return skipList;
		Files.readAllLines(Paths.get(filePath)).forEach(line -> {
			Long dbId = Long.parseLong(line.split("\t")[0]);
			skipList.add(dbId);
		});
		return skipList;
	}
	
	public static String getLastModificationAuthor(GKInstance instance)
	{
		final String noAuthor = "No modification or creation author";
		
		GKInstance mostRecentMod = null;
		try
		{
			@SuppressWarnings("unchecked")
			List<GKInstance> modificationInstances = (List<GKInstance>) instance.getAttributeValuesList("modified");
			if (modificationInstances.size() > 0)
			{
				for (int index = modificationInstances.size() - 1; index >= 0; index--)
				{
					GKInstance modificationInstance = modificationInstances.get(index);
					GKInstance author = (GKInstance) modificationInstance.getAttributeValue("author");
					// Skip modification instance for Solomon, Joel, or Guanming
					if (author == null || Arrays.asList("8939149", "1551959", "140537").contains(author.getDBID().toString()))
					{
						continue;
					}
					mostRecentMod = modificationInstance;
					break;
				}
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
	
	static int componentsHaveSpecies(GKInstance physicalEntity) throws Exception
	{
		Set<GKInstance> speciesSet = QACheckerHelper.grepAllSpeciesInPE(physicalEntity, true);
		//return !speciesSet.isEmpty();
		return !speciesSet.isEmpty() ? speciesSet.size() : 0;
	}
	
	static Set<GKInstance> grepAllSpeciesInPE(GKInstance pe, boolean needRecursion) throws Exception
	{
		Set<GKInstance> speciesSet = new HashSet<>();
		if (pe.getSchemClass().isValidAttribute(ReactomeJavaConstants.species))
		{
			@SuppressWarnings("unchecked")
			List<GKInstance> speciesList = (List<GKInstance>)pe.getAttributeValuesList(ReactomeJavaConstants.species);
			if (speciesList != null && speciesList.size() > 0)
			{
				speciesSet.addAll(speciesList);
			}
		}
		if (speciesSet.size() == 0 && needRecursion)
		{
			QACheckerHelper.grepAllSpeciesInPE(pe, speciesSet);
		}
		return speciesSet;
	}

	static void grepAllSpeciesInPE(GKInstance pe, Set<GKInstance> speciesSet) throws Exception
	{
		Set<GKInstance> wrappedPEs = InstanceUtilities.getContainedInstances(pe,
				ReactomeJavaConstants.hasComponent,
				ReactomeJavaConstants.hasCandidate,
				ReactomeJavaConstants.hasMember,
				ReactomeJavaConstants.repeatedUnit);
		for (GKInstance wrappedPE : wrappedPEs)
		{
			Set<GKInstance> wrappedSpecies = QACheckerHelper.grepAllSpeciesInPE(wrappedPE, true);
			speciesSet.addAll(wrappedSpecies);
		}
	}
	
	public static List<GKInstance> getInstancesWithNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, List<Long> skipList)
	{
		return getInstances(dba, schemaClass, attribute, IS_NULL, skipList);
	}
	
	public static List<GKInstance> getInstancesWithNonNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, List<Long> skipList) 
	{
		return getInstances(dba, schemaClass, attribute, IS_NOT_NULL, skipList);
	}
	
	@SuppressWarnings("unchecked")
	public static List<GKInstance> getInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		List<GKInstance> instances = new ArrayList<GKInstance>();
		try
		{
			instances.addAll(dba.fetchInstanceByAttribute(schemaClass, attribute, operator, null));
			return QACheckerHelper.filterBySkipList(skipList, instances);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return instances;
	}
}
