package org.reactome.qa.contributorscheck;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import org.gk.persistence.MySQLAdaptor;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;
import org.reactome.qa.report.exception.ReportException;

/**
 * Checks contributors.
 *
 */
public class ContributorsCheck // implements QACheck
{
	private static MySQLAdaptor currentDBA;
	private static MySQLAdaptor previousDBA;
	private static String inputFile;
	
	public static final void main( String[] args )
	{
		String pathToResources = "src/main/resources/auth.properties";
		
		if (args.length > 0 && !args[0].equals(""))
		{
			pathToResources = args[0];
		}

		try
		{
			Report r = checkNewContributors(pathToResources);
			((DelimitedTextReport)r).print("\t",System.out);
		}
		catch (IOException | ReportException | SQLException e)
		{
			e.printStackTrace();
		}
    }

    /**
     * Checks contributors.
     * @param pathToResources - The path to the properties resource file, which should contain all information needed to connected to the databases. It should look like this:
     * <pre>
user=${USER}
password=${PASSWORD}

currentDBHost=${CURRENT RELEASE DATABASE HOST}
oldDBHost=${PRIOR RELEASE DATABASE HOST}

currentDatabase=${CURRENT RELEASE DATABASE}
currentDatabasePort=${CURRENT RELEASE DATABASE PORT}

oldDatabase=${PRIOR RELEASE DATABASE}
oldDatabasePort=${PRIOR RELEASE DATABASE PORT}

inputFile=path/to/contributors-check-input.txt
     * </pre>
     * @throws SQLException
     * @throws IOException
     */
	public static Report checkNewContributors(String pathToResources) throws IOException, SQLException
	{
		CheckNewContributors checker = new CheckNewContributors();

		InputStream input = new FileInputStream("src/main/resources/auth.properties");
		Properties prop = new Properties();
		prop.load(input);
		String user = prop.getProperty("user");
		String password = prop.getProperty("password");
		String currentDatabaseHost = prop.getProperty("currentDBHost");
		String currentDatabase = prop.getProperty("currentDatabase");
		String currentDatabasePort = prop.getProperty("currentDatabasePort");
		String oldDatabaseHost = prop.getProperty("oldDBHost");
		String oldDatabase = prop.getProperty("oldDatabase");
		String oldDatabasePort = prop.getProperty("oldDatabasePort");
		inputFile = prop.getProperty("inputFile");
		currentDBA = new MySQLAdaptor(currentDatabaseHost, currentDatabase, user, password, Integer.valueOf(currentDatabasePort));
		previousDBA = new MySQLAdaptor(oldDatabaseHost, oldDatabase, user, password, Integer.valueOf(oldDatabasePort));

		checker.setCurrentDBAdaptor(currentDBA);
		checker.setPreviousDBAdaptor(previousDBA);
		checker.setInputFilePath(inputFile);
		return checker.executeQACheck();
	}
}
