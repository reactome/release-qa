package org.reactome.qa.nullcheck;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.gk.persistence.MySQLAdaptor;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.exception.ReportException;

/**
 * This QA check mostly checks for entities with attributes that are NULL but should not be null.
 * It also checks a few other issues that are related, but a bit more complicated.
 * @author sshorser
 *
 */
public class NullCheck
{
	private static String rleCompartmentSkipList;
	private static String rleInputSkipList;
	private static String rleOutputSkipList;

	public final static void main(String[] args)
	{
		String pathToResources = "src/main/resources/auth.properties";
		if (args !=null && args.length > 0)
		{
			pathToResources = args[0];
		}
		
		MySQLAdaptor currentDBA = null;
		try
		{
			InputStream input = new FileInputStream(pathToResources);
			Properties prop = new Properties();
			prop.load(input);
			String user = prop.getProperty("user");
			String password = prop.getProperty("password");
			String host = prop.getProperty("host");
			String database = prop.getProperty("database");
			NullCheck.rleCompartmentSkipList = prop.getProperty("rleCompartmentSkipList");
			NullCheck.rleInputSkipList = prop.getProperty("rleInputSkipList");
			NullCheck.rleOutputSkipList = prop.getProperty("rleOutputSkipList");
			int port = Integer.parseInt(prop.getProperty("port", "3306"));
		
			currentDBA = new MySQLAdaptor(host, database, user, password, port);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("--------\nSimple Entity Report\n--------");
		SimpleEntityChecker simpleEntityChecker = new SimpleEntityChecker();
		simpleEntityChecker.setAdaptor(currentDBA);
		DelimitedTextReport simpleEntityReport = (DelimitedTextReport) simpleEntityChecker.executeQACheck();
		try
		{
			simpleEntityReport.print("\t", System.out);
		}
		catch (IOException | ReportException e1)
		{
			e1.printStackTrace();
		}

		System.out.println("--------\nPhysical Entity Report\n--------");
		PhysicalEntityChecker physicalEntityChecker = new PhysicalEntityChecker();
		physicalEntityChecker.setAdaptor(currentDBA);
		DelimitedTextReport physicalEntityReport = (DelimitedTextReport) physicalEntityChecker.executeQACheck();
		try
		{
			physicalEntityReport.print("\t", System.out);
		}
		catch (IOException | ReportException e1)
		{
			e1.printStackTrace();
		}


		System.out.println("--------\nReaction-Like Event Report\n--------");
		ReactionLikeEventChecker rleChecker = new ReactionLikeEventChecker();
		rleChecker.setAdaptor(currentDBA);
		rleChecker.setRleCompartmentSkipList(NullCheck.rleCompartmentSkipList);
		rleChecker.setRleInputSkipList(NullCheck.rleInputSkipList);
		rleChecker.setRleOutputSkipList(NullCheck.rleOutputSkipList);
		DelimitedTextReport rleReport = (DelimitedTextReport) rleChecker.executeQACheck();
		try
		{
			rleReport.print("\t", System.out);
		}
		catch (IOException | ReportException e)
		{
			e.printStackTrace();
		}
		
		System.out.println("--------\nFailed Reaction Report\n--------");
		FailedReactionChecker failedReactionChecker = new FailedReactionChecker();
		failedReactionChecker.setAdaptor(currentDBA);
		DelimitedTextReport failedReactionReport = (DelimitedTextReport) failedReactionChecker.executeQACheck();
		try
		{
			failedReactionReport.print("\t", System.out);
		}
		catch (IOException | ReportException e)
		{
			e.printStackTrace();
		}
		
		NewEventChecker newEventChecker = new NewEventChecker();
		newEventChecker.setAdaptor(currentDBA);
		DelimitedTextReport newEventReport = (DelimitedTextReport) newEventChecker.executeQACheck();
		try
		{
			newEventReport.print("\t", System.out);
		}
		catch (IOException | ReportException e)
		{
			e.printStackTrace();
		}
		
	}

}
