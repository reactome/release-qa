package org.reactome.release.qa.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.annotations.GraphQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

@GraphQATest
public class ReactionsWithOnlyOneInputAndOutputWhereSchemaClassDoNotMatch extends AbstractQACheck {

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return "Reaction_Single_Input_Output_Schema_Not_Matched";
    }

    @SuppressWarnings("unchecked")
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> rles = dba.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        String[] loadAtts = {
                ReactomeJavaConstants.input,
                ReactomeJavaConstants.output
        };
        dba.loadInstanceAttributeValues(rles, loadAtts);
        for (GKInstance rle: rles) {
            SchemaClass cls = rle.getSchemClass();
            if (cls.isa(ReactomeJavaConstants.Polymerisation) || 
                cls.isa(ReactomeJavaConstants.Depolymerisation) ||
                cls.isa(ReactomeJavaConstants.BlackBoxEvent))
                continue;
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
                        QACheckerHelper.getLastModificationAuthor(instance)));
    }

}
