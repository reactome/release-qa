package org.reactome.qa.stableIdentifier;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.Properties;

import org.gk.persistence.MySQLAdaptor;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

/**
 * Checks for DatabaseIdentifiers that have a NULL identifier. Also checks for DatabaseIdentifiers with duplicated identifiers.
 * @author sshorser
 *
 */
public class StableIdentifierCheck 
{

	private static Report checkNullIdentifiers(MySQLAdaptor adaptor) throws Exception
	{
		CheckNullIdentifiers checker = new CheckNullIdentifiers();
		checker.setDataAdaptor(adaptor);
		return checker.executeQACheck();
	}
	
	private static Report checkDuplicatedIdentifiers(MySQLAdaptor adaptor) throws Exception 
	{
		CheckDuplicateIdentifiers checker = new CheckDuplicateIdentifiers();
		checker.setDataAdaptor(adaptor);
		return checker.executeQACheck();
	}
	
	/**
	 * Execute the Stable Identifier Check. This will check stable identifiers to see if there are any that are NULL or duplicated.
	 * Will output to System.out.
	 * @param pathToResources
	 */
	public static void executeStableIdentifierCheck(String pathToResources)
	{
		try
		{
			Properties props = new Properties();
			props.load(new FileInputStream(pathToResources) );
			
			String host = props.getProperty("host");
			String database = props.getProperty("database");
			String username = props.getProperty("username");
			String password = props.getProperty("password");
			int port = Integer.valueOf(props.getProperty("port"));
			
			MySQLAdaptor adaptor = new MySQLAdaptor(host, database, username, password, port);
			Report nullIdentifiersReport = checkNullIdentifiers(adaptor);

			Report duplicateIdentifiersReport = checkDuplicatedIdentifiers(adaptor);

			((DelimitedTextReport)nullIdentifiersReport).print(",", System.out);
			((DelimitedTextReport)duplicateIdentifiersReport).print(",", System.out);
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}	
	}
	
	public static final void main(String[] args)
	{
		String pathToResources = "src/main/resources/auth.properties";
		
		if (args.length > 0 && !args[0].equals(""))
		{
			pathToResources = args[0];
		}
		StableIdentifierCheck.executeStableIdentifierCheck(pathToResources);
		
	}

	
}
