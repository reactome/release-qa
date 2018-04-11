package org.reactome.qa.nullcheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;

class NullCheckHelper
{
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
	
	
	static String getLastModificationAuthor(GKInstance instance)
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
	
	static int componentsHaveSpecies(GKInstance physicalEntity) throws Exception
	{
		Set<GKInstance> speciesSet = NullCheckHelper.grepAllSpeciesInPE(physicalEntity, true);
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
			NullCheckHelper.grepAllSpeciesInPE(pe, speciesSet);
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
			Set<GKInstance> wrappedSpecies = NullCheckHelper.grepAllSpeciesInPE(wrappedPE, true);
			speciesSet.addAll(wrappedSpecies);
		}
	}
	
	static List<GKInstance> getInstancesWithNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, List<Long> skipList)
	{
		return getInstances(dba, schemaClass, attribute, "IS NULL", skipList);
	}
	
	static List<GKInstance> getInstancesWithNonNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, List<Long> skipList) 
	{
		return getInstances(dba, schemaClass, attribute, "IS NOT NULL", skipList);
	}
	
	static List<GKInstance> getInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		List<GKInstance> instances = new ArrayList<GKInstance>();
		try
		{
			instances.addAll(dba.fetchInstanceByAttribute(schemaClass, attribute, operator, null));
			return NullCheckHelper.filterBySkipList(skipList, instances);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return instances;
	}
}
