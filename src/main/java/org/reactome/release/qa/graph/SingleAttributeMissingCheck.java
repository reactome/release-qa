package org.reactome.release.qa.graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.reactome.release.qa.annotations.GraphQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This class is used to check if a value is missing in an attribute. It should be used
 * together with mandatory and required attribute check.
 * @author wug
 *
 */
@GraphQACheck
public class SingleAttributeMissingCheck extends MultipleAttributesMissingCheck {

    /**
     * The default report column headers
     */
    private static final String[] DEF_COL_HDRS = {
            "DBID",
            "DisplayName",
            "Class",
            "Attribute",
            "MostRecentAuthor"
    };
    
    public SingleAttributeMissingCheck() {
    }
    
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Map<String, List<String>> clsToAttributes = loadConfiguration();
        if (clsToAttributes == null || clsToAttributes.size() == 0)
            return report; // Nothing to be checked
        // Will be sorted based on cls names
        List<String> clsList = clsToAttributes.keySet().stream().sorted().collect(Collectors.toList());
        for (String cls : clsList) {
            List<String> attributes = clsToAttributes.get(cls);
            for (String att : attributes)
                executeQACheck(cls, att, report);
        }
        
        report.setColumnHeaders(getColumnHeaders());
        
        return report;
    }

    /**
     * This base implementation returns the standard column headers
     * <code>
     *   ["DBID", "DisplayName", "Class", "Attribute", "MostRecentAuthor"]
     * </code>
     * 
     * @return the report column headers
     */
    protected String[] getColumnHeaders() {
        return DEF_COL_HDRS;
    }
    
    protected void executeQACheck(String clsName, String attName, QAReport report) throws Exception {
        Collection<GKInstance> instances = QACheckerHelper.getInstancesWithNullAttribute(dba,
                                                                                   clsName,
                                                                                   attName,
                                                                                   null);
        for (GKInstance instance : instances) {
            if (isEscaped(instance)) {
                continue;
            }
            report.addLine(instance.getDBID() + "",
                           instance.getDisplayName(),
                           instance.getSchemClass().getName(),
                           attName,
                           QACheckerHelper.getLastModificationAuthor(instance));
        }
    }

    @Override
    public String getDisplayName() {
        return "Attribute_Value_Missing";
    }
    
}
