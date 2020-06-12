package org.reactome.release.qa.check;

import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QAReport;

public class OrcidCrossreferenceCheck extends AbstractQACheck {

    @Override
    public QAReport executeQACheck() throws Exception {
	    QAReport report = new QAReport();
	    report.setColumnHeaders("Person_DBID",
                                "Person_DisplayName",
                                "CrossReference_DBID",
                                "CrossReference_DisplayName");
	    /*
	    Create a map between ORCID and person list.

	    Fetch all Person instances from database where crossReference attribute 'IS NOT NULL'.

	    For all person instances:
	        Get ORCID for given person.
	        person list = map.get(ORCID)

	        If person list is null:
                Put (ORCID, Arrays.asList(person)) into map.

            Otherwise, map contains ORCID:
                Put (ORCID, list.add(person)) into map.

         For entry in map:
             list = entry.getValue()

             If list.size < 1:
                 continue

             ORCID = entry.getKey()
             For person in list:
                 Create report row.
                 report.addLine(person DBID, person display name, ORCID DBID, ORCID display name)
	     */

	    return report;
    }

}
