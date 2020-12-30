package org.reactome.release.qa.check;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.annotations.ReleaseQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QAReport;

/**
 * This will compare the number of instances for each species between two databases.
 * The counts are broken down by instance class.
 * 
 * Skip lists do not apply to this check.
 * 
 * @author sshorser
 */
@ReleaseQACheck
public class SpeciesInstanceCountCheck extends AbstractQACheck implements ChecksTwoDatabases
{
	private static final Logger logger = LogManager.getLogger();
	
	private static final String INFERRED_EVENTS_BASED_ON_ENSEMBL_COMPARA = "inferred events based on ensembl compara";
	private MySQLAdaptor priorAdaptor;
	
	/**
	 * Set the adaptor for the prior database. This check will use the inherited adaptor for the current database.
	 * @param adaptor
	 */
	@Override
	public void setOtherDBAdaptor(MySQLAdaptor adaptor)
	{
		this.priorAdaptor = adaptor;
	}
	
	@Override
	public String getDisplayName()
	{
		return "Species_Instance_Counts_Comparison";
	}
	
	@Override
	public QAReport executeQACheck() throws SQLException
	{
		QAReport report = new QAReport();

		// This predicate will be used to filter out chimeric instances
		// when counting the number of instances per class.
		Predicate<? super GKInstance> filterToExcludeChimeras = inst -> {
			// check validity of attribute.
			if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.isChimeric))
			{
				try
				{
					Boolean isChimeric = (Boolean) inst.getAttributeValue(ReactomeJavaConstants.isChimeric);
						
					// return false to exclude anything that is chimeric.
					if (isChimeric != null && isChimeric)
					{
						return false;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			// if we got here, then this instance is not chimeric.
			return true;
		};
		
		// This predicate is used to filter for inferred objects.
		// We assume that objects created by the Orthoinference process will have a note in their
		// "Created" attribute that reads: "inferred events based on ensembl compara"
		// I'm concerned there could somehow be inferred objects that don't have this, though I 
		// haven't found them yet, so maybe I'm just overly paranoid...
		Predicate<? super GKInstance> filterOnlyInferredObjects = inst -> {
			// check validity of attribute.
			if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.created))
			{
				try
				{
					GKInstance modified = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.created);
					if (modified != null)
					{
						String note = (String) modified.getAttributeValue(ReactomeJavaConstants.note);
						// If the note on the "Created" InstanceEdit has the correct text,
						// then try the filter to exclude chimeric instances.
						if (note != null && note.equals(SpeciesInstanceCountCheck.INFERRED_EVENTS_BASED_ON_ENSEMBL_COMPARA))
						{
							return filterToExcludeChimeras.test(inst);
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			// if we got here, then the note we're looking for is not present.
			return false;
		};
		
		try
		{
			// start with a list of all species in the current database.
			@SuppressWarnings("unchecked")
			List<GKInstance> currentSpecies = new ArrayList<GKInstance>( this.dba.fetchInstancesByClass(ReactomeJavaConstants.Species) );
			InstanceUtilities.sortInstances(currentSpecies);
			
			Long startTime = System.currentTimeMillis();
			// For each species in the current database...
			for (GKInstance species : currentSpecies )
			{
				@SuppressWarnings("unchecked")
				// Get the prior species by name, or NULL of it's not in prior.
				GKInstance priorSpecies = ((HashSet<GKInstance>) this.priorAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species, ReactomeJavaConstants.name, "=", species.getDisplayName())).stream().findFirst().orElse(null);
				// It's possible that a species in current is new and is not in prior, so just print a message and keep going.
				if (priorSpecies == null)
				{
					logger.warn("Could not find Species \""+species.getDisplayName()+ "\" in prior database.");
				}
				else
				{
					// Get all things that refer to the species. This is how it was done in the old Perl code for Orthoinference.
					@SuppressWarnings("unchecked")
					Collection<GKInstance> currentReferrers = species.getReferers(ReactomeJavaConstants.species);
					
					if (currentReferrers != null && !currentReferrers.isEmpty())
					{
						// These maps will track how often a class name is seen.
						Map<String, Integer> currentClassCounts = new HashMap<String, Integer>();
						Map<String, Integer> priorClassCounts = new HashMap<String, Integer>();
						
						// Process the list of all referrers: use a hashmap that is keyed by classname and whose values
						// are the number of times that classname has been seen.
						// Determine counts by 
						currentReferrers.stream().sequential()
										.filter( filterOnlyInferredObjects )
										.map( inst -> inst.getSchemClass().getName() )
										.forEach( populateClassCountMap(currentClassCounts) );
						
						// Get all things that refer to the species, this time in prior release database.
						@SuppressWarnings("unchecked")
						Collection<GKInstance> priorReferrers = priorSpecies.getReferers(ReactomeJavaConstants.species);
						priorReferrers.stream().sequential()
										.filter( filterOnlyInferredObjects )
										.map( inst -> inst.getSchemClass().getName() )
										.forEach( populateClassCountMap(priorClassCounts) );
						
						// For each class in the map of classes from current database...
						List<String> combinedKeys = (new ArrayList<String>(currentClassCounts.keySet()));
						//Add any keys from priorClassCounts that are not in currentClassCounts
						combinedKeys.addAll(priorClassCounts.keySet().stream().filter(k -> !currentClassCounts.containsKey(k)).collect(Collectors.toList()));
						//Sort by class name (remember the class names ARE the keys)
						combinedKeys.sort(Comparator.comparing(String::toString));
						for (String className : combinedKeys)
						{
							int currentCount = 0;
							int priorCount = 0;
							
							if (currentClassCounts.containsKey(className))
							{
								currentCount = currentClassCounts.get(className);
							}
							else
							{
								logger.warn("Class \""+className+"\" is not in current database for species "+species.getDisplayName());
							}
							
							if (priorClassCounts.containsKey(className))
							{
								priorCount = priorClassCounts.get(className);
							}
							else
							{
								logger.warn("Class \""+className+"\" is not in prior database for species "+species.getDisplayName());
							}

							double percentDiff = (((double)currentCount - (double)priorCount) / (double)priorCount) * 100.0d;
							
							String percentDiffString = String.format("%.4f", percentDiff);
							// TODO: parameterize these thresholds? 10% and 1000 feel a bit arbitrary, someone else might want different numbers later.
							if (Math.abs(percentDiff) > 10.0 && Math.abs(priorCount - currentCount) > 1000)
							{
								percentDiffString = "*** Difference is "+ String.format("%.4f", percentDiff)+"% ***";
							}
							report.addLine(species.getDisplayName(), className, Integer.toString(priorCount), Integer.toString(currentCount), percentDiffString);
						}
					}
					else
					{
						report.addLine(species.getDisplayName(), "N/A", "THIS SPECIES NOT IN CURRENT RELEASE", "0", "N/A");
					}
				}
			}
			Long endTime = System.currentTimeMillis();
			logger.info( "Elapsed time (seconds): " + TimeUnit.MILLISECONDS.toSeconds(endTime - startTime) );

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		report.setColumnHeaders("Species", "Class", "Count for Prior Release", "Count for Current Release", "% Difference");
		
		return report;
	}

	/**
	 * Returns a function that will modify the input map, based on whether the input to the function
	 * is new to the map. The function will take a String as input.
	 * @param classCounts - The map that the function will modify.
	 * @return A Function that takes in a String as its input, and modifies <code>classCounts</code>
	 */
	private static Consumer<? super String> populateClassCountMap(Map<String, Integer> classCounts)
	{
		return className -> {
			if (classCounts.containsKey(className))
			{
				classCounts.put(className, classCounts.get(className) + 1);
			}
			else
			{
				classCounts.put(className, 1);
			}
		};
	}

}
