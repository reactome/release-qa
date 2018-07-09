package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T071_ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatch
extends AbstractQACheck {

    private static final String ISSUE = "Sole input class does not match the sole output class";

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

        Collection<GKInstance> rles = QACheckerHelper.getInstancesWithNullAttribute(dba,
                ReactomeJavaConstants.ReactionlikeEvent,
                ReactomeJavaConstants.inferredFrom, null);
        String[] loadAtts = {
                ReactomeJavaConstants.input,
                ReactomeJavaConstants.output
        };
        dba.loadInstanceAttributeValues(rles, loadAtts);
        SchemaClass polyCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Polymerisation);
        SchemaClass depolyCls =
                dba.getSchema().getClassByName(ReactomeJavaConstants.Depolymerisation);
        for (GKInstance rle: rles) {
            // Skip (de-)polymerisations.
            if (rle.getSchemClass().isa(polyCls) || rle.getSchemClass().isa(depolyCls)) {
                continue;
            }
            // Check for incompatible input/output singletons.
            List<GKInstance> inputs =
                    rle.getAttributeValuesList(ReactomeJavaConstants.input);
            if (inputs.size() == 1) {
                List<GKInstance> outputs =
                        rle.getAttributeValuesList(ReactomeJavaConstants.output);
                if (outputs.size() == 1) {
                    GKInstance input = inputs.get(0);
                    GKInstance output = outputs.get(0);
                    if (input.getSchemClass() != output.getSchemClass()) {
                        addReportLine(report, rle);
                    }
                }
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance) {
        report.addLine(
                Arrays.asList(instance.getDBID().toString(), 
                        instance.getDisplayName(), 
                        instance.getSchemClass().getName(), 
                        ISSUE,  
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}
