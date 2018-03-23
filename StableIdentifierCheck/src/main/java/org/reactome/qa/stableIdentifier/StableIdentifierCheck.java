package org.reactome.qa.stableIdentifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/**
 * Checks for DatabaseIdentifiers that have a NULL identifier. Also checks for DatabaseIdentifiers with duplicated identifiers.
 * @author sshorser
 *
 */
public class StableIdentifierCheck
{
	public static String executeStableIdentifierCheck()
	{
		StringBuilder sb = new StringBuilder();
		try
		{
			//TODO: Get real DB settings from a file - this is just for local testing.
			MySQLAdaptor adaptor = new MySQLAdaptor("localhost", "test_reactome_64", "root", "root", 3306);
			
			Collection<GKInstance> identifiers = adaptor.fetchInstanceByAttribute("DatabaseIdentifier", "identifier", "IS NULL", null);
			
			if (!identifiers.isEmpty())
			{
				sb.append("The following DatabaseIdentifiers have NULL \"identifier\" values: ");
				for (GKInstance identifier : identifiers)
				{
					sb.append("DBID: ").append(identifier.getDBID()).append(" DisplayName: ").append(identifier.getDisplayName()).append("\n");
				}
			}
			else
			{
				sb.append("All DatabaseIdentifiers have a value for \"identifier\"! :)\n");
			}
			
			// The API doesn't really have the ability to find duplicated identifiers, but it 
			// is pretty easy to do this with plain SQL. You could also do this query a bit simpler
			// with self-joins, but those seem to be quite slow in MySQL.
			ResultSet results = adaptor.executeQuery("select identifier, count(*) as identifier_count\n" + 
													"from StableIdentifier\n" + 
													"group by identifier\n" + 
													"having count(*) <> 1\n" + 
													"order by identifier_count desc;", null);
			
			while (results.next())
			{
				sb.append("Identifier: ").append(results.getString("identifier")).append(" Number of reptitions: ").append(results.getInt("identifier_count")).append("\n");
			}
			
			sb.append("Done!").append("\n");
		}
		catch (SQLException e) 
		{
			
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return sb.toString();
				
	}
	
	public static void main(String[] args)
	{
		String report = executeStableIdentifierCheck();
		System.out.println(report);
	}
}
