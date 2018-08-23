package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T065_EntitySetsWithRepeatedMembers extends AbstractQACheck {

    private static final String ISSUE = "Contains member more than once: ";

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> entitySets = dba.fetchInstancesByClass(ReactomeJavaConstants.DefinedSet);
        Collection<GKInstance> openSets = dba.fetchInstancesByClass(ReactomeJavaConstants.OpenSet);
        entitySets.addAll(openSets);
        String[] loadAtts = { ReactomeJavaConstants.hasMember };
        dba.loadInstanceAttributeValues(entitySets, loadAtts);
        for (GKInstance entitySet: entitySets) {
            List<GKInstance> membersList =
                    entitySet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            Set<GKInstance> membersSet = new HashSet<GKInstance>();
            Set<GKInstance> repeated = new HashSet<GKInstance>();
            for (GKInstance member: membersList) {
                if (membersSet.contains(member)) {
                    if (!repeated.contains(member)) {
                        addReportLine(report, entitySet, member);
                        repeated.add(member);
                    }
                } else {
                    membersSet.add(member);
                }
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance, Instance other) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        ISSUE + other.getDisplayName() + " (DBID " + other.getDBID() + ")",  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}
