package org.reactome.release.qa.graph;

import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * Reports failed reactions which are not used as an inferredFrom target
 * and do not have a normalReaction value. 
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
public class FailedReactionMissingNormalCheck extends AbstractQACheck {

    private static final String[] LOAD_ATTS = { ReactomeJavaConstants.inferredFrom };
    
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Collection<GKInstance> instances = QACheckerHelper.getInstancesWithNullAttribute(dba,
                ReactomeJavaConstants.FailedReaction,
                ReactomeJavaConstants.normalReaction,
                null);
        dba.loadInstanceReverseAttributeValues(instances, LOAD_ATTS);
        for (GKInstance instance: instances) {
            if (isEscaped(instance)) {
                continue;
            }
            if (instance.getReferers(ReactomeJavaConstants.inferredFrom).isEmpty()) {
                report.addLine(instance.getDBID() + "",
                        instance.getDisplayName(),
                        QACheckerHelper.getLastModificationAuthor(instance));
            }
        }
        report.setColumnHeaders("DBID",
                "DisplayName",
                "MostRecentAuthor");
        
        return report;
     }

}
