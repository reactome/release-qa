package org.reactome.qa.nullcheck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.reactome.qa.QACheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

/**
 * Consider to add some new attributes to avoid skip lists: e.g. allowInputNull etc. 
 *
 */
public class ReactionLikeEventChecker implements QACheck
{

	MySQLAdaptor dba;
	
	private String rleCompartmentSkipList;
	private String rleInputSkipList;
	private String rleOutputSkipList;
	
	
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


	public void setAdaptor(MySQLAdaptor dba)
	{
		this.dba = dba;
	}

	private List<Long> getRLECompartmentSkipList(String filePath) throws IOException
	{
		return NullCheckHelper.getSkipList(filePath);
	}
	
	private List<Long> getRLEInputSkipList(String filePath) throws IOException
	{
		return NullCheckHelper.getSkipList(filePath);
	}
	
	private List<Long> getRLEOutputSkipList(String filePath) throws IOException
	{
		return NullCheckHelper.getSkipList(filePath);
	}
	
	private Report report(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		Report r = new DelimitedTextReport();
		
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
	private Report getNormalReactionWithoutDiseaseReportLines(MySQLAdaptor currentDBA)
	{
		Report normalReactionWithoutDiseaseReport = new DelimitedTextReport();
		List<GKInstance> RLEsWithNormalReaction = new ArrayList<GKInstance>();
		try
		{
			RLEsWithNormalReaction.addAll(NullCheckHelper.getInstancesWithNonNullAttribute(currentDBA, "ReactionlikeEvent", "normalReaction", null));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		for (GKInstance RLEWithNormalReaction : RLEsWithNormalReaction)
		{
			try
			{
				GKInstance diseaseInstance = (GKInstance) RLEWithNormalReaction.getAttributeValue("disease");
				if (diseaseInstance == null)
				{
					normalReactionWithoutDiseaseReport.addLine(Arrays.asList(RLEWithNormalReaction.getDBID().toString(), RLEWithNormalReaction.getDisplayName(), RLEWithNormalReaction.getSchemClass().getName(), "RLE with normal reaction but disease is null", NullCheckHelper.getLastModificationAuthor(RLEWithNormalReaction) ));
				}
			}
			catch (InvalidAttributeException e)
			{
				e.printStackTrace();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return normalReactionWithoutDiseaseReport;
	}
	
	@Override
	public Report executeQACheck()
	{
		Report reactionLikeEventReport = new DelimitedTextReport();
		
		try
		{
			Report r = report(this.dba, "ReactionlikeEvent", "compartment", "IS NULL", getRLECompartmentSkipList(this.rleCompartmentSkipList));
			reactionLikeEventReport.addLines(r.getReportLines());
		}
		catch (IOException e)
		{
			System.err.println("Unable to get RLE compartment skip list");
			e.printStackTrace();
		}
		
		try
		{
			Report r = report(this.dba, "ReactionlikeEvent", "input", "IS NULL", getRLEInputSkipList(this.rleInputSkipList));
			reactionLikeEventReport.addLines(r.getReportLines());
		}
		catch (IOException e)
		{
			System.err.println("Unable to get RLE input skip list");
			e.printStackTrace();
		}
		
		try
		{
			Report r = report(this.dba, "ReactionlikeEvent", "output", "IS NULL", getRLEOutputSkipList(this.rleOutputSkipList));
			reactionLikeEventReport.addLines(r.getReportLines());
		}
		catch (IOException e)
		{
			System.err.println("Unable to get RLE output skip list");
			e.printStackTrace();
		}
		
		reactionLikeEventReport.addLines(getNormalReactionWithoutDiseaseReportLines(this.dba).getReportLines());
		
		reactionLikeEventReport.setColumnHeaders(Arrays.asList("DBID","DisplayName", "SchemaClass", "Issue", "MostRecentAuthor"));
		return reactionLikeEventReport;
	}

}
