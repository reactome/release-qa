package org.reactome.release.qa.check;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
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
			Collection<GKInstance> currentSpecies = this.dba.fetchInstancesByClass("Species");
			
			// To be used when sorting lists of classes.
			Comparator<GKInstance> classNameSorter = new Comparator<GKInstance>() {
				@Override
				public int compare(GKInstance o1, GKInstance o2)
				{
					return o1.getSchemClass().getName().compareTo(o2.getSchemClass().getName());
				}
			};
			// To be used when sorting lists of species. Sorts by displayName (as String). 
			Comparator<GKInstance> speciesNameSorter = new Comparator<GKInstance>() {
				@Override
				public int compare(GKInstance o1, GKInstance o2)
				{
					return o1.getDisplayName().compareTo(o2.getDisplayName());
				}
			};
			
			// For each species in the current database...
			for (GKInstance species : currentSpecies.stream().sorted(speciesNameSorter)
														//TODO: parameterize filter for species, so you can do a quick comparison on one species.
														//.filter(s -> s.getDisplayName().equals("Arabidopsis thaliana"))
														.collect(Collectors.toList()))
			{
				// Get all things that refer to the species. This is how it was done in the old Perl code for Orthoinference.
				@SuppressWarnings("unchecked")
				Collection<GKInstance> currentReferrers = species.getReferers("species");
				@SuppressWarnings("unchecked")
				// Get the prior species by name, or NULL of it's not in prior.
				GKInstance priorSpecies = ((HashSet<GKInstance>) this.priorAdaptor.fetchInstanceByAttribute("Species", "name", "=", species.getDisplayName())).stream().findFirst().orElse(null);
				// It's possible that a species in current is new and is not in prior, so just print a message and keep going.
				if (priorSpecies == null)
				{
					System.out.println("ERROR: Could not find "+species.getDisplayName()+ " in prior database.\n");
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
						currentReferrers.stream().sorted( classNameSorter )
												.map( inst -> inst.getSchemClass().getName() )
												.forEach( populateClassCountMap(currentClassCounts) );
						
						// Get all things that refer to the species, this time in prior release database.
						@SuppressWarnings("unchecked")
						Collection<GKInstance> priorReferrers = priorSpecies.getReferers("species");
						priorReferrers.stream().sorted( classNameSorter )
												.map( inst -> inst.getSchemClass().getName() )
												.forEach( populateClassCountMap(priorClassCounts) );
						
						// For each class in the map of classes from current database...
						for (String className : currentClassCounts.keySet())
						{
							// If a class isn't in prior, just set the counter to 0.
							// It would also be possible to print a warning here, but it seems
							// simpler to just let the count be 0, and that will *probably* 
							// trigger a "*** Difference is... ***" message.
							int currentCount = currentClassCounts.containsKey(className) ? currentClassCounts.get(className) : 0;
							int priorCount = priorClassCounts.containsKey(className) ? priorClassCounts.get(className) : 0;
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
