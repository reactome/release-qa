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

public class T059_DuplicatedCandidateSets extends AbstractQACheck {

    private static final String ISSUE = "Same members and candidates as ";

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

        Collection<GKInstance> candidateSets = dba.fetchInstancesByClass(ReactomeJavaConstants.CandidateSet);
        String[] loadAtts = {
                ReactomeJavaConstants.compartment,
                ReactomeJavaConstants.hasMember,
                ReactomeJavaConstants.hasCandidate
        };
        dba.loadInstanceAttributeValues(candidateSets, loadAtts);
        Map<GKInstance, Set<GKInstance>> compartmentCandidateSets =
                new HashMap<GKInstance, Set<GKInstance>>();
        for (GKInstance candidateSet: candidateSets) {
            List<GKInstance> members =
                    candidateSet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            Set<GKInstance> entities = new HashSet<GKInstance>(members);
            List<GKInstance> candidates =
                    candidateSet.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
            entities.addAll(candidates);
            Collection<GKInstance> compartments =
                    candidateSet.getAttributeValuesList(ReactomeJavaConstants.compartment);
            for (GKInstance compartment: compartments) {
                Set<GKInstance> cses = compartmentCandidateSets.get(compartment);
                if (cses == null) {
                    cses = new HashSet<GKInstance>();
                    compartmentCandidateSets.put(compartment, cses);
                } else {
                    for (GKInstance other: cses) {
                        List<GKInstance> otherMembers =
                                other.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                        List<GKInstance> otherCandidates =
                                other.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
                        if (entities.size() != otherMembers.size() + otherCandidates.size()) {
                            continue;
                        }
                        boolean differs = false;
                        for (GKInstance member: otherMembers) {
                            if (!entities.contains(member)) {
                                differs = true;
                                break;
                            }
                        }
                        if (!differs) {
                            for (GKInstance candidate: otherCandidates) {
                                if (!entities.contains(candidate)) {
                                    differs = true;
                                    break;
                                }
                            }
                        }
                        if (!differs) {
                            addReportLine(report, candidateSet, other);
                        }
                    }
                }
                cses.add(candidateSet);
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
