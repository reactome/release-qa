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
import org.reactome.qa.QACheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

public class PhysicalEntityChecker implements QACheck
{
	private MySQLAdaptor dba;
	
	public void setAdaptor(MySQLAdaptor adaptor)
	{
		this.dba = adaptor;
	}

	private int componentsHaveSpecies(GKInstance physicalEntity) throws Exception {
		Set<GKInstance> speciesSet = grepAllSpeciesInPE(physicalEntity, true);
		//return !speciesSet.isEmpty();
		return !speciesSet.isEmpty() ? speciesSet.size() : 0;
	}
	
	private Set<GKInstance> grepAllSpeciesInPE(GKInstance pe, boolean needRecursion) throws Exception
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
		if (speciesSet.size() == 0 && needRecursion) {
			grepAllSpeciesInPE(pe, speciesSet);
		}
		return speciesSet;
	}

	private void grepAllSpeciesInPE(GKInstance pe, Set<GKInstance> speciesSet) throws Exception {
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
	
	private String getLastModificationAuthor(GKInstance instance)
	{
		final String noAuthor = "No modification or creation author";
		GKInstance mostRecentMod = null;
		try
		{
			@SuppressWarnings("unchecked")
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
			} catch (Exception e) {
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

	@SuppressWarnings("unchecked")
	private List<GKInstance> getInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		List<GKInstance> instances = new ArrayList<GKInstance>();
		try
		{
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
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return instances;
	}
	
	private List<GKInstance> getInstancesWithNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, List<Long> skipList)
	{
		return getInstances(dba, schemaClass, attribute, "IS NULL", skipList);
	}
	
	
	@Override
	public Report executeQACheck()
	{
		Report report = new DelimitedTextReport();
		List<GKInstance> physicalEntities = new ArrayList<GKInstance>();
		for (String schemaClass : Arrays.asList("Complex", "EntitySet", "Polymer"))
		{	
			physicalEntities.addAll(getInstancesWithNullAttribute(this.dba, schemaClass, "species", null));
		}
		
		for (GKInstance physicalEntity : physicalEntities)
		{
			int numComponents = 0;
			try
			{
				numComponents = componentsHaveSpecies(physicalEntity);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			if(numComponents > 0)
			{
				//physicalEntitySpeciesReportLines.add(getReportLine(physicalEntity, "Null species but components with species"));
				report.addLine(Arrays.asList());
				report.addLine(Arrays.asList(physicalEntity.getDBID().toString(), physicalEntity.getDisplayName(), physicalEntity.getSchemClass().getName(), "NULL species but "+numComponents+" components have a species", getLastModificationAuthor(physicalEntity) ));
			}
		}
		report.setColumnHeaders(Arrays.asList("DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor"));
		return report;
	}

}
