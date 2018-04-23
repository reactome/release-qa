package org.reactome.release.qa.check;

import java.sql.ResultSet;
import java.util.Arrays;

import org.reactome.release.qa.common.QAReport; 

/**
 * Need to consider a database level enforcement.
 * @author wug
 *
 */
public class StableIdentifierCheck extends AbstractQACheck {

	private static final String QUERY_STRING = "select identifier, count(*) as identifier_count\n" + 
											"from StableIdentifier\n" + 
											"group by identifier\n" + 
											"having count(*) <> 1\n" + 
											"order by identifier_count desc;";
	
	public StableIdentifierCheck() {
    }
	
	@Override
	public QAReport executeQACheck() throws Exception {
	    QAReport report = new QAReport();
	    // The API doesn't really have the ability to find duplicated identifiers, but it 
	    // is pretty easy to do this with plain SQL. You could also do this query a bit simpler
	    // with self-joins, but those seem to be quite slow in MySQL.
	    ResultSet results = dba.executeQuery(QUERY_STRING, null);
	    while (results.next()) {
	        report.addLine(Arrays.asList(results.getString("identifier"), String.valueOf(results.getInt("identifier_count")) ));
	    }
	    results.close();
	    report.setColumnHeaders(Arrays.asList("Identifier", "Number of repetitions"));
	    return report;
	}

    @Override
    public String getDisplayName() {
        return "StableIdentifier_Identifier_Duplication";
    }

}
