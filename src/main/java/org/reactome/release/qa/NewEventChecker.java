package org.reactome.release.qa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.MySQLAdaptor.AttributeQueryRequest;
import org.gk.persistence.MySQLAdaptor.QueryRequestList;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidClassException;
import org.reactome.release.qa.common.DelimitedTextReport;
import org.reactome.release.qa.common.QACheck;
import org.reactome.release.qa.common.QAReport;

/** 
 * 
 * A set of QAs related to new ReactionLikeEvents, which are collected
 * based on inferredFrom = null and stableIdentifier is not null and its released
 * is null or false.
 *
 */
public class NewEventChecker implements QACheck
{

	private static final String REACTIONLIKE_EVENT = "ReactionlikeEvent";
	private MySQLAdaptor adaptor;
	
	public void setAdaptor(MySQLAdaptor dba)
	{
		this.adaptor = dba;
	}

	/**
	 * Gets new Events that have no value for "inferredFrom" and also have no value for "literatureReference" 
	 * @param dba
	 * @param skipList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<GKInstance> getNewEventsWithNoInferredFromAndNoLitRef(MySQLAdaptor dba, List<Long> skipList)
	{
		List<GKInstance> instances = new ArrayList<GKInstance>();
		try
		{
			AttributeQueryRequest stIdIsNotNullRequest = this.adaptor.new AttributeQueryRequest(REACTIONLIKE_EVENT, "stableIdentifier", NullCheckHelper.IS_NOT_NULL, null);
			AttributeQueryRequest noInferredFrom = this.adaptor.new AttributeQueryRequest(REACTIONLIKE_EVENT, "inferredFrom", NullCheckHelper.IS_NULL, null);
			AttributeQueryRequest noLitRef = this.adaptor.new AttributeQueryRequest(REACTIONLIKE_EVENT, "literatureReference", NullCheckHelper.IS_NULL, null);
			QueryRequestList queryReqestList = this.adaptor.new QueryRequestList();
			queryReqestList.add(stIdIsNotNullRequest);
			queryReqestList.add(noLitRef);
			queryReqestList.add(noInferredFrom);
			
			Collection<GKInstance> newEvents = dba.fetchInstance(queryReqestList);
			getUnreleasedEvents(instances, newEvents);
			return NullCheckHelper.filterBySkipList(skipList, instances);
		}
		catch (InvalidClassException | InvalidAttributeException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return instances;
	}

	/**
	 * Gets Events that are unreleased - They have a stableIdentifier and the stableIdentifier's "released" attribute is NULL/false
	 * @param instances
	 * @param newEvents
	 * @throws InvalidAttributeException
	 * @throws Exception
	 */
	private void getUnreleasedEvents(List<GKInstance> instances, Collection<GKInstance> newEvents) throws InvalidAttributeException, Exception
	{
		for (GKInstance inst : newEvents)
		{
			GKInstance stableIdentifier = (GKInstance) inst.getAttributeValue("stableIdentifier");
			Boolean released = (Boolean) stableIdentifier.getAttributeValue("released");
			if (released==null || released.booleanValue()==false)
			{
				instances.add(inst);
			}
			
		}
	}
	
	/**
	 * Gets new Events, based on whether a certain attribute is null or not.
	 * @param dba - the database adaptor.
	 * @param schemaClass - the class that the events will be.
	 * @param attribute - the attribute to check.
	 * @param operator - should be "IS NULL" or "IS NOT NULL" //TODO: change this to an enum. 
	 * @param skipList - a skiplist to filter out results, if you want.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<GKInstance> getNewEventInstances(MySQLAdaptor dba, String schemaClass, String attribute, String operator, List<Long> skipList)
	{
		List<GKInstance> instances = new ArrayList<GKInstance>();
		try
		{
			AttributeQueryRequest stIdIsNotNullRequest = this.adaptor.new AttributeQueryRequest(schemaClass, "stableIdentifier", NullCheckHelper.IS_NOT_NULL, null);
			AttributeQueryRequest queryRequest = this.adaptor.new AttributeQueryRequest(schemaClass, attribute, operator, null);

			QueryRequestList queryReqestList = this.adaptor.new QueryRequestList();
			queryReqestList.add(stIdIsNotNullRequest);
			queryReqestList.add(queryRequest);

			Collection<GKInstance> newEvents = dba.fetchInstance(queryReqestList);
			getUnreleasedEvents(instances, newEvents);
			// filter by skip list
			return NullCheckHelper.filterBySkipList(skipList, instances);
		}
		catch (InvalidClassException | InvalidAttributeException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return instances;
	}

	@Override
	public QAReport executeQACheck()
	{
		QAReport newEventReport = new DelimitedTextReport();

		List<GKInstance> reactionLikeEventsEditedIsNull = getNewEventInstances(this.adaptor, REACTIONLIKE_EVENT, "edited", NullCheckHelper.IS_NULL, null);
		for (GKInstance instance : reactionLikeEventsEditedIsNull)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attribute \"edited\" is NULL", NullCheckHelper.getLastModificationAuthor(instance) ));
		}
		
		List<GKInstance> reactionLikeEventsAuthoredIsNull = getNewEventInstances(this.adaptor, REACTIONLIKE_EVENT, "authored", NullCheckHelper.IS_NULL, null);
		for (GKInstance instance : reactionLikeEventsAuthoredIsNull)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attribute \"authored\" is NULL", NullCheckHelper.getLastModificationAuthor(instance) ));
		}

		List<GKInstance> reactionLikeEventsReviewedIsNull = getNewEventInstances(this.adaptor, REACTIONLIKE_EVENT, "reviewed", NullCheckHelper.IS_NULL, null);
		for (GKInstance instance : reactionLikeEventsReviewedIsNull)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attribute \"reviewed\" is NULL", NullCheckHelper.getLastModificationAuthor(instance) ));
		}

		List<GKInstance> reactionLikeEventsSummationIsNull = getNewEventInstances(this.adaptor, REACTIONLIKE_EVENT, "summation", NullCheckHelper.IS_NULL, null);
		for (GKInstance instance : reactionLikeEventsSummationIsNull)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attribute \"summation\" is NULL", NullCheckHelper.getLastModificationAuthor(instance) ));
		}

		List<GKInstance> reactionLikeEventsSpeiesIsNull = getNewEventInstances(this.adaptor, REACTIONLIKE_EVENT, "species", NullCheckHelper.IS_NULL, null);
		for (GKInstance instance : reactionLikeEventsSpeiesIsNull)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attribute \"species\" is NULL", NullCheckHelper.getLastModificationAuthor(instance) ));
		}

		List<GKInstance> noInferredFromAndNoLitRef = this.getNewEventsWithNoInferredFromAndNoLitRef(this.adaptor, null);
		for (GKInstance instance : noInferredFromAndNoLitRef)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attributes \"inferredFrom\" and \"literatureReferences\" are NULL", NullCheckHelper.getLastModificationAuthor(instance) ));
		}
		
		newEventReport.setColumnHeaders(Arrays.asList("DBID","DisplayName","SchemaClass","Issue","MostRecentAuthor"));
		
		return newEventReport;
	}

}
