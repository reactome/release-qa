package org.reactome.release.qa.check;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.common.QAReport;

/**
 * Consider to add some new attributes to avoid skip lists: e.g. allowInputNull etc. 
 *
 */
public class ReactionLikeEventChecker extends AbstractQACheck {
	
	private String rleCompartmentSkipList;
	private String rleInputSkipList;
	private String rleOutputSkipList;
	
	public ReactionLikeEventChecker() {
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
		return NullCheckHelper.getSkipList(filePath);
	}
	
	private List<Long> getRLEOutputSkipList(String filePath) throws IOException
	{
		return NullCheckHelper.getSkipList(filePath);
	}
	
	private QAReport report(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		QAReport r = new QAReport();
		
		List<GKInstance> instances = new ArrayList<GKInstance>();
		instances.addAll(NullCheckHelper.getInstances(this.dba, schemaClass, attribute, operator, skipList));

		for (GKInstance instance : instances)
		{
			r.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), instance.getSchemClass().getName() + " with NULL " + attribute, NullCheckHelper.getLastModificationAuthor(instance)));
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
	    rlesWithNormalReaction.addAll(NullCheckHelper.getInstancesWithNonNullAttribute(currentDBA, "ReactionlikeEvent", "normalReaction", null));
	    for (GKInstance RLEWithNormalReaction : rlesWithNormalReaction)
	    {
	        GKInstance diseaseInstance = (GKInstance) RLEWithNormalReaction.getAttributeValue("disease");
	        if (diseaseInstance == null)
	        {
	            normalReactionWithoutDiseaseReport.addLine(Arrays.asList(RLEWithNormalReaction.getDBID().toString(), RLEWithNormalReaction.getDisplayName(), RLEWithNormalReaction.getSchemClass().getName(), "RLE with normal reaction but disease is null", NullCheckHelper.getLastModificationAuthor(RLEWithNormalReaction) ));
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
