package org.reactome.qa.stableIdentifier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.gk.model.PersistenceAdaptor;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.qa.QACheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

public class CheckDuplicateIdentifiers implements QACheck
{

	private static final String QUERY_STRING = "select identifier, count(*) as identifier_count\n" + 
											"from StableIdentifier\n" + 
											"group by identifier\n" + 
											"having count(*) <> 1\n" + 
											"order by identifier_count desc;";
	private PersistenceAdaptor adaptor;
	
	public void setDataAdaptor(PersistenceAdaptor adaptor)
	{
		this.adaptor = adaptor;
	}
	@Override
	public Report executeQACheck()
	{
		Report report = new DelimitedTextReport();
		// The API doesn't really have the ability to find duplicated identifiers, but it 
		// is pretty easy to do this with plain SQL. You could also do this query a bit simpler
		// with self-joins, but those seem to be quite slow in MySQL.
		ResultSet results;
		try {
			results = ((MySQLAdaptor)adaptor).executeQuery(QUERY_STRING, null);
			while (results.next())
			{
				report.addLine(Arrays.asList(results.getString("identifier"), String.valueOf(results.getInt("identifier_count")) ));
			}
		}
		catch (SQLException e)
		{
			System.err.println("There was an error executing the query. Error was: "+e.getMessage()+", Query was: "+QUERY_STRING);
			e.printStackTrace();
		}
		
		report.setColumnHeaders(Arrays.asList("Identifier", "Number of repetitions"));
		return report;
	}

}
