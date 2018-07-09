package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T064_DuplicatedEntitySets extends AbstractQACheck {

    private static final String ISSUE = "Same members as ";

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
        String[] loadAtts = {
                ReactomeJavaConstants.compartment,
                ReactomeJavaConstants.hasMember
        };
        dba.loadInstanceAttributeValues(entitySets, loadAtts);
        Map<GKInstance, Set<GKInstance>> compartmentEntitySets =
                new HashMap<GKInstance, Set<GKInstance>>();
        for (GKInstance entitySet: entitySets) {
            List<GKInstance> membersList =
                    entitySet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            Set<GKInstance> members = new HashSet<GKInstance>(membersList);
            Collection<GKInstance> compartments =
                    entitySet.getAttributeValuesList(ReactomeJavaConstants.compartment);
            for (GKInstance compartment: compartments) {
                Set<GKInstance> cmptEntitySets = compartmentEntitySets.get(compartment);
                if (cmptEntitySets == null) {
                    cmptEntitySets = new HashSet<GKInstance>();
                    compartmentEntitySets.put(compartment, cmptEntitySets);
                } else {
                    for (GKInstance other: cmptEntitySets) {
                        List<GKInstance> otherMembers =
                                other.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                        if (members.size() != otherMembers.size()) {
                            continue;
                        }
                        boolean differs = false;
                        for (GKInstance member: otherMembers) {
                            if (!members.contains(member)) {
                                differs = true;
                                break;
                            }
                        }
                        if (!differs) {
                            addReportLine(report, entitySet, other);
                        }
                    }
                }
                cmptEntitySets.add(entitySet);
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
