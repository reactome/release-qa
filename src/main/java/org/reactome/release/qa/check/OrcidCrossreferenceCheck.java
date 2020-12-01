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

    private final Long ORCID_DBID = 5334734L;
    private static final String SKIP_LIST_FILE_PATH = "resources/orcid_crossreference_skip_list.txt";

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
        // This does not verify that the ORCID database is being used for crossReference.
        // However, no other databases have been found, so a verification is not currently needed.
        @SuppressWarnings("unchecked")
        Collection<GKInstance> peopleWithCrossreference = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Person,
                                                                                       ReactomeJavaConstants.crossReference,
                                                                                       "IS NOT NULL",
                                                                                       null);
        // For all person instances:
        for (GKInstance person : peopleWithCrossreference) {
            // Get ORCID for given person.
            List<GKInstance> crossRefs = person.getAttributeValuesList(ReactomeJavaConstants.crossReference);
            if (crossRefs == null || crossRefs.size() == 0)
                continue;

            List<GKInstance> orcids = new ArrayList<GKInstance>();
            for (GKInstance crossRef : crossRefs) {
                if (inSkipList(crossRef, SKIP_LIST_FILE_PATH)) {
                    continue;
                }
                GKInstance refDb = (GKInstance) crossRef.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
                if (refDb.getDBID().equals(ORCID_DBID))
                    orcids.add(crossRef);
            }

            if (orcids.size() == 0) continue;

            for (GKInstance orcid : orcids) {
                orcidToPeople.compute(orcid, (key, list) -> {
                    if (list == null)
                        list = new ArrayList<GKInstance>();
                    list.add(person);
                    return list;
                });
            }
        }

        // For entry in map:
        for (Entry<GKInstance, List<GKInstance>> entry : orcidToPeople.entrySet()) {
            List<GKInstance> people = entry.getValue();

            // If there are no duplicates, continue.
            if (people.size() < 2)
                continue;

            // Person DBID's (e.g. "1168468,140934,26636").
            String peopleDBIDs = people.stream()
                                       .map(GKInstance::getDBID)
                                       .sorted()
                                       .map(dbid -> dbid.toString())
                                       .collect(Collectors.joining(","));
            // Create report row.
            GKInstance orcid = entry.getKey();
            report.addLine(orcid.getDBID() + "",
                           orcid.getDisplayName(),
                           orcid.getAttributeValue(ReactomeJavaConstants.identifier) + "",
                           peopleDBIDs);
        }

        return report;
    }

}
