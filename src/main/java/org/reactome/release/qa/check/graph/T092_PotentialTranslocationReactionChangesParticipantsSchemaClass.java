package org.reactome.release.qa.check.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.check.AbstractQACheck;
import org.reactome.release.qa.check.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

public class T092_PotentialTranslocationReactionChangesParticipantsSchemaClass
extends AbstractQACheck {

    private static final String ISSUE = "Changes participant class";

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor");

    private static final String[] RLE_LOAD_ATTS = {
            ReactomeJavaConstants.catalystActivity,
            ReactomeJavaConstants.input,
            ReactomeJavaConstants.output
    };

    private static final String[] ENTITY_LOAD_ATTS = {
            ReactomeJavaConstants.compartment
    };

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    /**
     * A RLE is reported as invalid if and only if the following conditions hold:
     * * the RLE is not a black-box event
     * * the RLE does not have a catalyst activity
     * * there is exactly one input and one output
     * * the input compartment differs from the output compartment
     * * the input schema class differs from the output schema class
     */
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        Collection<GKInstance> rles =
                QACheckerHelper.getInstancesWithNullAttribute(dba,
                        ReactomeJavaConstants.ReactionlikeEvent,
                        ReactomeJavaConstants.inferredFrom, null);
        dba.loadInstanceAttributeValues(rles, RLE_LOAD_ATTS);
        for (GKInstance rle: rles) {
            if (!isValid(rle)) {
                addReportLine(report, rle);
            }
        }

        report.setColumnHeaders(HEADERS);

        return report;
    }

    @SuppressWarnings("unchecked")
    private boolean isValid(GKInstance rle) throws Exception {
        // A black-box event is not subject to this check.
        if (ReactomeJavaConstants.BlackBoxEvent.equals(rle.getSchemClass().getName())) {
            return true;
        }

        // A catalyst event is not subject to this check.
        List<GKInstance> catActs =
                rle.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (!catActs.isEmpty()) {
            return true;
        }
        
        // The participants.
        List<GKInstance> inputs =
                rle.getAttributeValuesList(ReactomeJavaConstants.input);
        dba.loadInstanceAttributeValues(inputs, ENTITY_LOAD_ATTS);
        List<GKInstance> outputs =
                rle.getAttributeValuesList(ReactomeJavaConstants.output);
        dba.loadInstanceAttributeValues(outputs, ENTITY_LOAD_ATTS);
        
        // The check only applies to singleton inputs and outputs.
        if (inputs.size() != 1 || outputs.size() != 1) {
            return true;
        }
        
        // The sole input and output.
        GKInstance input = inputs.get(0);
        GKInstance output = outputs.get(0);
        
        // The reaction is valid if the participant classes are the same.
        if (input.getSchemClass() == output.getSchemClass()) {
            return true;
        }
        
        // The reaction is valid if at least one input compartment is also
        // an output compartment.
        List<GKInstance> inCmpts =
                input.getAttributeValuesList(ReactomeJavaConstants.compartment);
        List<GKInstance> outCmpts =
                output.getAttributeValuesList(ReactomeJavaConstants.compartment);
        for (GKInstance inCmpt: inCmpts) {
            if (outCmpts.contains(inCmpt)) {
                return true;
            }
        }
        
        // All checks on the RLE failed.
        return false;
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
