package org.reactome.release.qa.check;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;
import org.reactome.release.qa.common.SkipList;

/**
 * Reports ReactionlikeEvent and Complexes for which one of the following conditions hold:
 * <ul>
 * <li>Chimeric but less than two species</li>
 * <li>Chimeric but not used for inference</li>
 * <li>Not chimeric but more than one species</li>
 * <li>Not chimeric but with chimeric participant</li>
 * </ul>
 * 
 * This class is ported from chimeric_qa.pl by Joel. However, the check here is expanded
 * for both ReactionlikeEvent and Complex, two classes that have isChimeric.
 * 
 * @author wug
 */
@SuppressWarnings("unchecked")
@SliceQACheck
public class ChimericInstancesCheck extends AbstractQACheck{

    private static final Logger logger = LogManager.getLogger();
    private SkipList skipList;

    @Override
    public String getDisplayName() {
        return "Chimerism_Reference_Constraint_Violations";
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        try {
            skipList = new SkipList(this.getDisplayName());
        } catch (Exception e) {
            logger.info(e.getMessage());
            e.printStackTrace();

        }

        String[] clsNames = {ReactomeJavaConstants.ReactionlikeEvent,
                             ReactomeJavaConstants.Complex};
        for (String cls : clsNames) {
            checkChimericInstances(report, cls);
        }
        
        report.setColumnHeaders("DBID",
                                "DisplayName",
                                "Class",
                                "Issue",
                                "MostRecentAuthor");
        return report;
    }

    protected void checkChimericInstances(QAReport report, String clsName) throws Exception {
        Collection<GKInstance> rles = dba.fetchInstancesByClass(clsName);
        dba.loadInstanceAttributeValues(rles,
                                        new String[]{ReactomeJavaConstants.isChimeric, 
                                                     ReactomeJavaConstants.species,
                                                     ReactomeJavaConstants.input,
                                                     ReactomeJavaConstants.output,
                                                     "regulatedBy",
                                                     ReactomeJavaConstants.catalystActivity,
                                                     ReactomeJavaConstants.hasComponent});
        for (GKInstance rle : rles) {
            if (isEscaped(rle)) {
                continue;
            }
            if (!skipList.containsInstanceDbId(rle)) {
                if (QACheckerHelper.isChimeric(rle)) {
                    if (!hasMultipleSpecies(rle))
                        report.addLine(rle.getDBID() + "",
                                rle.getDisplayName(),
                                rle.getSchemClass().getName(),
                                "Chimeric but one or null species",
                                QACheckerHelper.getLastModificationAuthor(rle));
                    if (isNotUsedForInference(rle))
                        report.addLine(rle.getDBID() + "",
                                rle.getDisplayName(),
                                rle.getSchemClass().getName(),
                                "Chimeric but not used for inference",
                                QACheckerHelper.getLastModificationAuthor(rle));
                } else {
                    if (hasMultipleSpecies(rle))
                        report.addLine(rle.getDBID() + "",
                                rle.getDisplayName(),
                                rle.getSchemClass().getName(),
                                "Not chimeric but with multiple species",
                                QACheckerHelper.getLastModificationAuthor(rle));
                    if (isParticipantChimeric(rle)) {
                        report.addLine(rle.getDBID() + "",
                                rle.getDisplayName(),
                                rle.getSchemClass().getName(),
                                "Not chimeric but with chimeric participant",
                                QACheckerHelper.getLastModificationAuthor(rle));
                    }
                }
            }
        }
    }
    
    private boolean isParticipantChimeric(GKInstance rle) throws Exception {
        Set<GKInstance> participants = grepChimericEntities(rle);
        for (GKInstance participant : participants) {
            if (QACheckerHelper.isChimeric(participant))
                return true;
        }
        return false;
    }
    
    private Set<GKInstance> grepChimericEntities(GKInstance container) throws Exception {
        Set<GKInstance> current = new HashSet<>();
        boolean isForComplex = false;
        if (container.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
            Set<GKInstance> pes = InstanceUtilities.getReactionParticipants(container);
            current.addAll(pes);
        }
        else if (container.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
            current.add(container);
            isForComplex = true;
        }
        Set<GKInstance> rtn = new HashSet<>();
        for (GKInstance pe : current) {
            if (!isForComplex)
                rtn.add(pe);
            Set<GKInstance> tmp = InstanceUtilities.getContainedInstances(pe,
                                                                          ReactomeJavaConstants.hasComponent,
                                                                          ReactomeJavaConstants.hasMember);
            rtn.addAll(tmp);
        }
        return rtn.stream()
                  .filter(inst -> inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.isChimeric))
                  .collect(Collectors.toSet());
    }
    
    /**
     * This check should be used for ReactionlikeEvent only.
     * @param rle
     * @return
     * @throws Exception
     */
    private boolean isNotUsedForInference(GKInstance rle) throws Exception {
        if (!rle.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
            return false;
        Collection<GKInstance> referrers = rle.getReferers(ReactomeJavaConstants.inferredFrom);
        if (referrers == null || referrers.size() == 0)
            return true;
        return false;
    }
    
    private boolean hasMultipleSpecies(GKInstance rle) throws Exception {
        List<GKInstance> species = rle.getAttributeValuesList(ReactomeJavaConstants.species);
        if (species.size() > 1)
            return true;
        return false;
    }
    
}
