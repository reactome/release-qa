package org.reactome.release.qa.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.annotations.GraphQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

@GraphQACheck
public class ReactionsSingleInputOutputSchemaClassCheck extends AbstractQACheck {

    private static final Logger logger =
            Logger.getLogger(ReactionsSingleInputOutputSchemaClassCheck.class);

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "Input_Schema_Class", "Output_Schema_Class", "MostRecentAuthor");

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
            if (isEscaped(rle)) {
                continue;
            }
            SchemaClass cls = rle.getSchemClass();
            if (cls.isa(ReactomeJavaConstants.Polymerisation) || 
                cls.isa(ReactomeJavaConstants.Depolymerisation) ||
                cls.isa(ReactomeJavaConstants.BlackBoxEvent))
                continue;
            List<GKInstance> inputs =
                    rle.getAttributeValuesList(ReactomeJavaConstants.input);
            if (inputs.size() == 1) {
                List<GKInstance> outputs =
                        rle.getAttributeValuesList(ReactomeJavaConstants.output);
                if (outputs.size() == 1) {
                    GKInstance input = inputs.get(0);
                    GKInstance output = outputs.get(0);
                    SchemaClass inputSchemaCls = input.getSchemClass();
                    SchemaClass outputSchemaCls = output.getSchemClass();
                    if (inputSchemaCls != outputSchemaCls) {
                        // Work around a possible schema corruption bug.
                        // The Maven single jar package assembler adds
                        // two GKSchemaClass.class files, which can sporadically
                        // result in two SchemaClass instances with the same name.
                        //
                        // This problem was first noticed for the 2018-12-17
                        // weekly QA check run. At that time, the problem occured
                        // when this QA check is run along with other QA checks, but
                        // not when it is run alone. Oddly, the problem did not occur
                        // when the continue; line below was commented out. All of which
                        // indicates that the problem is sporadic and unpredictable.
                        //
                        // The work-around is to log the error and continue.
                        // Oddly, only one or two issues were reported without the
                        // logging but about 20 error messages were logged.
                        //
                        // TODO - consider using the maven shading package, although
                        // that is a complex, ugly solution.
                        if (Objects.equals(inputSchemaCls.getName(), outputSchemaCls.getName())) {
                            logger.error("Two schema class instances have the same name: " +
                                    inputSchemaCls + " and " + outputSchemaCls);
                            continue;
                        }
                        addReportLine(report, rle, inputSchemaCls, outputSchemaCls);
                    }
                }
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance rle, SchemaClass inputSchemaCls, SchemaClass outputSchemaCls) {
        report.addLine(
                Arrays.asList(rle.getDBID().toString(),
                        rle.getDisplayName(),
                        inputSchemaCls.getName(),
                        outputSchemaCls.getName(),
                        QACheckerHelper.getLastModificationAuthor(rle)));
    }

}
