package org.reactome.qa.nullcheck;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.MySQLAdaptor.AttributeQueryRequest;
import org.gk.persistence.MySQLAdaptor.QueryRequest;
import org.gk.persistence.MySQLAdaptor.QueryRequestList;
import org.reactome.qa.QACheck;
import org.reactome.qa.report.Report;

public class NewEventChecker implements QACheck {

	private MySQLAdaptor adaptor;
	
	public void setAdaptor(MySQLAdaptor dba)
	{
		this.adaptor = dba;
	}

	@SuppressWarnings("unchecked")
	private List<GKInstance> getInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		List<GKInstance> instances = new ArrayList<GKInstance>();
		try
		{
			AttributeQueryRequest stIdIsNotNullRequest = this.adaptor.new AttributeQueryRequest("ReactionlikeEvent", "stableIdentifier", "IS NOT NULL", null);
			AttributeQueryRequest editedIsNullRequest = this.adaptor.new AttributeQueryRequest("ReactionlikeEvent", "edited", "IS NULL", null);
			AttributeQueryRequest authoredIsNullRequest = this.adaptor.new AttributeQueryRequest("ReactionlikeEvent", "authored", "IS NULL", null);
			AttributeQueryRequest reviewedIsNullRequest = this.adaptor.new AttributeQueryRequest("ReactionlikeEvent", "reviewed", "IS NULL", null);
			AttributeQueryRequest summationIsNullRequest = this.adaptor.new AttributeQueryRequest("ReactionlikeEvent", "summation", "IS NULL", null);
			AttributeQueryRequest speciesIsNullRequest = this.adaptor.new AttributeQueryRequest("ReactionlikeEvent", "species", "IS NULL", null);

			QueryRequestList queryReqestListEdited = this.adaptor.new QueryRequestList();
			queryReqestListEdited.add(stIdIsNotNullRequest);
			queryReqestListEdited.add(editedIsNullRequest);
			
			QueryRequestList queryReuestListAuthored = this.adaptor.new QueryRequestList();
			queryReuestListAuthored.add(stIdIsNotNullRequest);
			queryReuestListAuthored.add(authoredIsNullRequest);
			
			QueryRequestList queryReqestListReviewed = this.adaptor.new QueryRequestList();
			queryReqestListReviewed.add(stIdIsNotNullRequest);
			queryReqestListReviewed.add(reviewedIsNullRequest);
			
			QueryRequestList queryReqestListSummation = this.adaptor.new QueryRequestList();
			queryReqestListSummation.add(stIdIsNotNullRequest);
			queryReqestListSummation.add(summationIsNullRequest);
			
			QueryRequestList queryReqestListSpecies = this.adaptor.new QueryRequestList();
			queryReqestListSpecies.add(stIdIsNotNullRequest);
			queryReqestListSpecies.add(speciesIsNullRequest);
			
			dba.fetchInstance(queryReqestListEdited);
			// Now, for each RLE, also need to check that the "released" on stableIdentifier is  attribute is NULL or false
			// TODO: Finish this code!
			instances.addAll(dba.fetchInstanceByAttribute(schemaClass, attribute, operator, null));
			
			if (skipList != null && !skipList.isEmpty())
			{
				return instances.parallelStream().filter(inst -> skipList.contains(inst.getDBID())).collect(Collectors.toList());
			}
			else
			{
				return instances;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return instances;
	}
	
	private List<GKInstance> getInstancesWithNullAttribute(MySQLAdaptor dba, String schemaClass, String attribute, List<Long> skipList)
	{
		return getInstances(dba, schemaClass, attribute, "IS NULL", skipList);
	}
	
	private List<GKInstance> getNewEvents(MySQLAdaptor currentDBA)
	{
		List<GKInstance> newEvents = new ArrayList<GKInstance>();
		try
		{
			List<GKInstance> reactionLikeEvents = getInstances(currentDBA, "ReactionlikeEvent", "stableIdentifier", "IS NOT NULL", null);
			for (GKInstance reactionLikeEvent : reactionLikeEvents)
			{
				GKInstance RLEStableIdentifier = (GKInstance) reactionLikeEvent.getAttributeValue("stableIdentifier");
				Boolean releasedAttribute = (Boolean) RLEStableIdentifier.getAttributeValue("released");
				if (releasedAttribute == null || !releasedAttribute)
				{
					newEvents.add(reactionLikeEvent);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return newEvents;
	}
	
	@Override
	public Report executeQACheck()
	{
		return null;
		
	}

}
