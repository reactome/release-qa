package org.reactome.release.qa.check;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * Reports non-failed RLEs with a normalReaction value but without a disease value.
 * 
 * This check replaces the disease aspect of the former ReactionLikeEventChecker.java.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@SliceQACheck
public class ReactionlikeEventDiseaseCheck extends AbstractQACheck {
	
    private static final String[] LOAD_ATTS = { ReactomeJavaConstants.disease };
    
	public ReactionlikeEventDiseaseCheck() {
	}
	
	@Override
    public String getDisplayName() {
        return "ReactionlikeEvent_Not_Failed_With_Normal_Without_Disease";
    }

    @Override
	public QAReport executeQACheck() throws Exception {
	    QAReport report = new QAReport();
        // The RLEs which refer to a normal reaction.
	    Collection<GKInstance> normalReferers =
	            QACheckerHelper.getInstancesWithNonNullAttribute(dba,
	                    ReactomeJavaConstants.ReactionlikeEvent,
	                    ReactomeJavaConstants.normalReaction, null);
	    // Prep the RLEs for accessing the disease attribute.
	    dba.loadInstanceAttributeValues(normalReferers, LOAD_ATTS);
	    for (GKInstance rle: normalReferers) {
	        if (isEscaped(rle)) {
	            continue;
	        }
	        // Ignore failed reactions.
            if (!rle.getSchemClass().isa(ReactomeJavaConstants.FailedReaction)) {
                // Report non-disease RLEs.
                if (rle.getAttributeValue(ReactomeJavaConstants.disease) == null) {
                    List<String> line = Arrays.asList(
                            rle.getDBID().toString(),
                            rle.getDisplayName(),
                            rle.getSchemClass().getName(),
                            QACheckerHelper.getLastModificationAuthor(rle));
                    report.addLine(line);
                }
	        }
	    }
	    report.setColumnHeaders("DBID","DisplayName", "Class", "MostRecentAuthor");
	    
	    return report;
	}

}
