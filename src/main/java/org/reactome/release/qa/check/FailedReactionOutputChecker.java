package org.reactome.release.qa.check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This class reports FailedReactions that have output.
 * (Note: We may have to change the data model to enforce this!)
 */
@SliceQATest
public class FailedReactionOutputChecker extends AbstractQACheck {

    private static final List<String> HEADERS =
            Arrays.asList("DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

	@Override
    public String getDisplayName() {
        return "FailedReaction_Output";
    }
	
	@Override
	public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        report.setColumnHeaders(HEADERS);
        List<List<String>> missingNormal = check(ReactomeJavaConstants.FailedReaction,
                ReactomeJavaConstants.normalReaction, QACheckerHelper.IS_NULL);
        report.addLines(missingNormal);
        List<List<String>> hasOutput = check(ReactomeJavaConstants.FailedReaction,
                ReactomeJavaConstants.output, QACheckerHelper.IS_NOT_NULL);
        report.addLines(hasOutput);
		
        return report;
	}
    
    private List<List<String>> check(String schemaClass, String attribute, String operator)
            throws Exception {
        Collection<GKInstance> instances =
                QACheckerHelper.getInstances(dba, schemaClass, attribute, operator, null);
        List<List<String>> lines = new ArrayList<List<String>>();
        for (GKInstance instance: instances) {
            if (!isEscaped(instance)) {
                List<String> line = toReportLine(instance, attribute, operator);
                lines.add(line);
            }
        }
        
        return lines;
    }
    
    private List<String> toReportLine(GKInstance instance, String attribute, String operator) {
        return Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(),
                instance.getSchemClass().getName(), attribute + " " + operator,
                QACheckerHelper.getLastModificationAuthor(instance));
    }

}
