package org.reactome.release.qa.check;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QAReport;

/**
 * WeeklyQA check to detect person instances that share the same ORCID id (crossreference).
 *
 * TODO Check false negative by creating two people with same ORCID.
 */
@SliceQACheck
public class OrcidCrossreferenceCheck extends AbstractQACheck {

    @Override
    public QAReport executeQACheck() throws Exception {
	    QAReport report = new QAReport();
	    report.setColumnHeaders("CrossReference_DBID",
                                "CrossReference_DisplayName",
                                "Identifier",
                                "Person_DBIDs");

	    // Create a map between ORCID and person list.
	    Map<GKInstance, List<GKInstance>> orcidToPeople = new HashMap<GKInstance, List<GKInstance>>();

	    // Fetch all Person instances from database where crossReference attribute 'IS NOT NULL'.
	    // Verify that ORCHID database id is being used.
	    @SuppressWarnings("unchecked")
        Collection<GKInstance> peopleWithOrcid = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Person,
                                                                              ReactomeJavaConstants.crossReference,
                                                                              "IS NOT NULL",
                                                                              null);
	    for (GKInstance person : peopleWithOrcid) {
	        GKInstance crossReference = (GKInstance) person.getAttributeValue(ReactomeJavaConstants.crossReference);
	        GKInstance refDatabase = (GKInstance) crossReference.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
	        if (!refDatabase.getDBID().equals(5334734L))
	            peopleWithOrcid.remove(person);
	    }
	    // For all person instances:
	    for (GKInstance person : peopleWithOrcid) {
	        // Get ORCID for given person.
	        GKInstance orcid = (GKInstance) person.getAttributeValue(ReactomeJavaConstants.crossReference);

	        List<GKInstance> people = orcidToPeople.get(orcid);

	        // If person list is null:
	        if (people == null) {
                // Put (ORCID, Arrays.asList(person)) into map.
	            List<GKInstance> newPeople = new ArrayList<GKInstance>();
	            newPeople.add(person);
	            orcidToPeople.put(orcid, newPeople);
	            continue;
	        }

            // Otherwise, map contains ORCID.
            // Add person to list.
	        people.add(person);
	    }

         // For entry in map:
	    for (Entry<GKInstance, List<GKInstance>> entry : orcidToPeople.entrySet()) {
             List<GKInstance> people = entry.getValue();

             // If there are no duplicates, continue.
             if (people.size() < 2)
                 continue;

             // Person DBID's (e.g. "1168468, 140934, 26636").
             String peopleDBIDs = people.stream()
                                        .map(GKInstance::getDBID)
                                        .sorted()
                                        .map(Object::toString)
                                        .collect(Collectors.joining(","));
             // Create report row.
             GKInstance orcid = entry.getKey();
             report.addLine(orcid.getDBID().toString(),
                            orcid.getDisplayName().toString(),
                            orcid.getAttributeValue(ReactomeJavaConstants.identifier).toString(),
                            peopleDBIDs);
	    }

	    return report;
    }

}
