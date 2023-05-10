package org.reactome.release.qa.check;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * Consider to add some new attributes to avoid skip lists: e.g. allowInputNull etc.
 *
 */

/**
 * Note: This class was originally on the old 'develop' branch before 'master' was merged into it in December 2020.
 * It was added to 'master', but as it was an old QA test its utility was questionable, and so it was turned off. (JCook 2020)
 */
public class ReactionLikeEventCheck extends AbstractQACheck {

    private String rleCompartmentSkipList;
    private String rleInputSkipList;
    private String rleOutputSkipList;

    public ReactionLikeEventCheck() {
        // Automatically load skip lists if they are in the resources folder
        File file = new File("resources/reaction_like_event_compartment_skip_list.txt");
        if (file.exists())
            setRleCompartmentSkipList(file.getAbsolutePath());
        file = new File("resources/reaction_like_event_input_skip_list.txt");
        if (file.exists())
            setRleInputSkipList(file.getAbsolutePath());
        file = new File("resources/reaction_like_event_output_skip_list.txt");
        if (file.exists())
            setRleOutputSkipList(file.getAbsolutePath());
    }

    public String getRleCompartmentSkipList()
    {
        return rleCompartmentSkipList;
    }

    public void setRleCompartmentSkipList(String rleCompartmentSkipList)
    {
        this.rleCompartmentSkipList = rleCompartmentSkipList;
    }

    public String getRleInputSkipList()
    {
        return rleInputSkipList;
    }

    public void setRleInputSkipList(String rleInputSkipList)
    {
        this.rleInputSkipList = rleInputSkipList;
    }

    public String getRleOutputSkipList()
    {
        return rleOutputSkipList;
    }

    public void setRleOutputSkipList(String rleOutputSkipList)
    {
        this.rleOutputSkipList = rleOutputSkipList;
    }

    private List<Long> getRLEInputSkipList(String filePath) throws IOException
    {
        return QACheckerHelper.getSkipList(filePath);
    }

    private List<Long> getRLEOutputSkipList(String filePath) throws IOException
    {
        return QACheckerHelper.getSkipList(filePath);
    }

    private QAReport report(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
    {
        QAReport r = new QAReport();

        List<GKInstance> instances = new ArrayList<GKInstance>();
        instances.addAll(QACheckerHelper.getInstances(this.dba, schemaClass, attribute, operator, skipList));

        for (GKInstance instance : instances)
        {
            r.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), instance.getSchemClass().getName() + " with NULL " + attribute, QACheckerHelper.getLastModificationAuthor(instance)));
        }

        return r;
    }

    /**
     * A RLE having non-null normalReaction should have disease value not null. Can we enfore this
     * type of check in the data model?
     */
    private QAReport getNormalReactionWithoutDiseaseReportLines(MySQLAdaptor currentDBA) throws Exception {
        QAReport normalReactionWithoutDiseaseReport = new QAReport();
        List<GKInstance> rlesWithNormalReaction = new ArrayList<GKInstance>();
        rlesWithNormalReaction.addAll(QACheckerHelper.getInstancesWithNonNullAttribute(currentDBA, "ReactionlikeEvent", "normalReaction", null));
        for (GKInstance RLEWithNormalReaction : rlesWithNormalReaction)
        {
            GKInstance diseaseInstance = (GKInstance) RLEWithNormalReaction.getAttributeValue("disease");
            if (diseaseInstance == null)
            {
                normalReactionWithoutDiseaseReport.addLine(Arrays.asList(RLEWithNormalReaction.getDBID().toString(), RLEWithNormalReaction.getDisplayName(), RLEWithNormalReaction.getSchemClass().getName(), "RLE with normal reaction but disease is null", QACheckerHelper.getLastModificationAuthor(RLEWithNormalReaction) ));
            }
        }
        return normalReactionWithoutDiseaseReport;
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport reactionLikeEventReport = new QAReport();
        QAReport r = report(this.dba, "ReactionlikeEvent", "input", "IS NULL", getRLEInputSkipList(this.rleInputSkipList));
        reactionLikeEventReport.addLines(r.getReportLines());
        r = report(this.dba, "ReactionlikeEvent", "output", "IS NULL", getRLEOutputSkipList(this.rleOutputSkipList));
        reactionLikeEventReport.addLines(r.getReportLines());
        reactionLikeEventReport.addLines(getNormalReactionWithoutDiseaseReportLines(this.dba).getReportLines());
        reactionLikeEventReport.setColumnHeaders(Arrays.asList("DBID","DisplayName", "SchemaClass", "Issue", "MostRecentAuthor"));
        return reactionLikeEventReport;
    }

    public String getDisplayName() {
        return "ReactionLikeEvent_Multiple_Check";
    }

}