package org.reactome.release.qa.check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * 
 * This class is to check two things for FailedReaction: 1). A FailedReaction must have
 * a nornalReaction (Mandatory); 2). A FaiedReaction cannot have output (Note: We may have
 * to change the data model to enforce this!)
 *
 */
public class FailedReactionChecker extends AbstractQACheck {

    private static final String schemaClassName = ReactomeJavaConstants.FailedReaction;
    
	@Override
    public String getDisplayName() {
        return "FailedReaction_NormalReaction_Output";
    }

    private QAReport report(String schemaClass, String attribute, String operator, List<Long> skipList) {
		QAReport r = new QAReport();
		
		List<GKInstance> instances = new ArrayList<GKInstance>();
		instances.addAll(QACheckerHelper.getInstances(dba, schemaClass, attribute, operator, skipList));

		for (GKInstance instance : instances)
		{
			r.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), attribute + " " + operator  , QACheckerHelper.getLastModificationAuthor(instance)));
		}
		return r;
	}
	
	@Override
	public QAReport executeQACheck() {
		QAReport r;
		
		r = report(FailedReactionChecker.schemaClassName, "normalReaction", QACheckerHelper.IS_NULL, null);
		r.addLines(report(FailedReactionChecker.schemaClassName, "output", QACheckerHelper.IS_NOT_NULL, null).getReportLines() );
		r.setColumnHeaders(Arrays.asList("DBID","DisplayName","SchemaClass","Issue","MostRecentAuthor"));
		return r;
	}

}
