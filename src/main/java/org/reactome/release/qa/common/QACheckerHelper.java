package org.reactome.release.qa.common;

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
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

public class QACheckerHelper {
    
	public static final String IS_NOT_NULL = "IS NOT NULL";
	public static final String IS_NULL = "IS NULL";
    
    /**
     * Filter a list of DB ids by the DB ids in skipList.
     * Any object whose DB_ID is in skipList will *not* be in the output.
     * 
     * @param skipList the skipList
     * @param dbIds thd DB ids to filter
     * @return
     */
    public static Collection<Long> filterDbIdsBySkipList(Collection<Long> skipList, Collection<Long> dbIds)
    {
        if (skipList == null || skipList.isEmpty()) {
            return dbIds;
        } else {
            return dbIds.parallelStream()
                    .filter(dbId -> !skipList.contains(dbId))
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * Filter a list of GKInstance objects by the DB IDs in skipList.
     * @param skipList - the skipList.
     * @param instances - Objects from the database. Any object whose DB_ID is in skipList will *not* be in the output.
     * @return
     */
    public static Collection<GKInstance> filterBySkipList(Collection<Long> skipList, Collection<GKInstance> instances)
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
	
	public static List<Long> getSkipList(String filePath) throws IOException
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
					// Skip modification instance for developers.
					List<Long> developers = QACheckProperties.getDeveloperDbIds();
					if (author == null || developers.contains(author.getDBID()))
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
    
    public static GKInstance getLastModification(GKInstance instance)
    {
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
                    return modificationInstance;
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
        
        return created == null ? null : created;
    }
	
	public static int componentsHaveSpecies(GKInstance physicalEntity) throws Exception
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
	
	public static Collection<GKInstance> getInstancesWithNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, Collection<Long> skipList)
	{
		return getInstances(dba, schemaClass, attribute, IS_NULL, skipList);
	}
	
	public static Collection<GKInstance> getInstancesWithNonNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, Collection<Long> skipList) 
	{
		return getInstances(dba, schemaClass, attribute, IS_NOT_NULL, skipList);
	}
	
	@SuppressWarnings("unchecked")
	public static Collection<GKInstance> getInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, Collection<Long> skipList)
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
	
	/**
	 * A generic method to get the table for an attribute in a specified class.
	 * @param clsName
	 * @param attributeName
	 * @param dba
	 * @return
	 * @throws Exception
	 */
	public static String getAttributeTableName(String clsName, 
	                                           String attributeName,
	                                           MySQLAdaptor dba) throws Exception {
	    Schema schema = dba.fetchSchema();
	    SchemaClass cls = schema.getClassByName(clsName);
	    SchemaAttribute attribute = cls.getAttribute(attributeName);
	    SchemaClass originCls = attribute.getOrigin();
	    if (attribute.isMultiple())
	        return originCls.getName() + "_2_" + attribute.getName();
	    else
	        return originCls.getName();
	}
	
}
