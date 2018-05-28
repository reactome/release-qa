package org.reactome.release.qa.check;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.common.QAReport;

/**
 * This will compare the number of instances for each species between two databases.
 * The counts are broken down by instance class.
 * @author sshorser
 *
 */
public class CompareSpeciesByClasses extends AbstractQACheck
{
	private MySQLAdaptor priorAdaptor;
	
	/**
	 * Set the adaptor for the prior database. This check will use the inherited adaptor for the current database.
	 * @param adaptor
	 */
	public void setPriorDBAdaptor(MySQLAdaptor adaptor)
	{
		this.priorAdaptor = adaptor;
	}
	
	@Override
	public String getDisplayName()
	{
		return "CompareSpeciesByClasses";
	}
	
	@Override
	public QAReport executeQACheck() throws SQLException
	{
		QAReport report = new QAReport();

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
				// Get all things that refer to the species. This is how it was done in the old Perl code for Orthoinference.
				@SuppressWarnings("unchecked")
				Collection<GKInstance> currentReferrers = species.getReferers(ReactomeJavaConstants.species);
				@SuppressWarnings("unchecked")
				// Get the prior species by name, or NULL of it's not in prior.
				GKInstance priorSpecies = ((HashSet<GKInstance>) this.priorAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species, ReactomeJavaConstants.name, "=", species.getDisplayName())).stream().findFirst().orElse(null);
				// It's possible that a species in current is new and is not in prior, so just print a message and keep going.
				if (priorSpecies == null)
				{
					System.out.println("WARNING: Could not find "+species.getDisplayName()+ " in prior database.\n");
				}
				else
				{
					if (currentReferrers != null && !currentReferrers.isEmpty())
					{
						// These maps will track how often a class name is seen.
						Map<String, Integer> currentClassCounts = Collections.synchronizedMap( new HashMap<String, Integer>() );
						Map<String, Integer> priorClassCounts = Collections.synchronizedMap( new HashMap<String, Integer>() );
						
						// Process the list of all referrers: use a hashmap that is keyed by classname and whose values
						// are the number of times that classname has been seen.
						// Determine counts by 
						currentReferrers.stream().parallel()
										.map( inst -> inst.getSchemClass().getName() )
										.forEach( populateClassCountMap(currentClassCounts) );
						
						// Get all things that refer to the species, this time in prior release database.
						@SuppressWarnings("unchecked")
						Collection<GKInstance> priorReferrers = priorSpecies.getReferers(ReactomeJavaConstants.species);
						priorReferrers.stream().parallel()
										.map( inst -> inst.getSchemClass().getName() )
										.forEach( populateClassCountMap(priorClassCounts) );
						
						// For each class in the map of classes from current database...
						List<String> combinedKeys = (new ArrayList<String>(currentClassCounts.keySet()));
						//Add any keys from priorClassCounts that are not in currentClassCounts
						combinedKeys.addAll(priorClassCounts.keySet().stream().filter(k -> !currentClassCounts.containsKey(k)).collect(Collectors.toList()));
						combinedKeys.sort(Comparator.comparing(String::toString));
						for (String className : combinedKeys )
						{
							int currentCount = 0;
							int priorCount = 0;
							
							if (currentClassCounts.containsKey(className))
							{
								currentCount = currentClassCounts.get(className);
							}
							else
							{
								System.out.println("WARNING: Class \""+className+"\" is not in current database for species "+species.getDisplayName());
							}
							
							if (priorClassCounts.containsKey(className))
							{
								priorCount = priorClassCounts.get(className);
							}
							else
							{
								System.out.println("WARNING: Class \""+className+"\" is not in prior database for species "+species.getDisplayName());
							}

							double percentDiff = (((double)currentCount - (double)priorCount) / (double)priorCount) * 100.0d;
							
							String percentDiffString = String.valueOf(percentDiff);
							// TODO: parameterize these thresholds? 10% and 1000 feel a bit arbitrary, someone else might want different numbers later.
							if (Math.abs(percentDiff) > 10.0 && priorCount - currentCount > 1000)
							{
								percentDiffString = "*** Difference is "+percentDiff+"% ***";
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
			System.out.println( "Time (seconds): " + TimeUnit.MILLISECONDS.toSeconds(endTime - startTime) );

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