package org.reactome.qa.nullcheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.MySQLAdaptor.AttributeQueryRequest;
import org.gk.persistence.MySQLAdaptor.QueryRequestList;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidClassException;
import org.reactome.qa.QACheck;
import org.reactome.qa.report.DelimitedTextReport;
import org.reactome.qa.report.Report;

public class NewEventChecker implements QACheck
{
	private static final String IS_NOT_NULL = "IS NOT NULL";
	private static final String IS_NULL = "IS NULL";
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
			AttributeQueryRequest stIdIsNotNullRequest = this.adaptor.new AttributeQueryRequest(REACTIONLIKE_EVENT, "stableIdentifier", IS_NOT_NULL, null);
			AttributeQueryRequest noInferredFrom = this.adaptor.new AttributeQueryRequest(REACTIONLIKE_EVENT, "inferredFrom", IS_NULL, null);
			AttributeQueryRequest noLitRef = this.adaptor.new AttributeQueryRequest(REACTIONLIKE_EVENT, "literatureReference", IS_NULL, null);
			QueryRequestList queryReqestList = this.adaptor.new QueryRequestList();
			queryReqestList.add(stIdIsNotNullRequest);
			queryReqestList.add(noLitRef);
			queryReqestList.add(noInferredFrom);
			
			Collection<GKInstance> newEvents = dba.fetchInstance(queryReqestList);
			getUnreleasedEvents(instances, newEvents);
			return filterBySkipList(skipList, instances);
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
			AttributeQueryRequest stIdIsNotNullRequest = this.adaptor.new AttributeQueryRequest(schemaClass, "stableIdentifier", IS_NOT_NULL, null);
			AttributeQueryRequest queryRequest = this.adaptor.new AttributeQueryRequest(schemaClass, attribute, operator, null);

			QueryRequestList queryReqestList = this.adaptor.new QueryRequestList();
			queryReqestList.add(stIdIsNotNullRequest);
			queryReqestList.add(queryRequest);

			Collection<GKInstance> newEvents = dba.fetchInstance(queryReqestList);
			getUnreleasedEvents(instances, newEvents);
			// filter by skip list
			return filterBySkipList(skipList, instances);
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
	 * Filter a list of GKInstance objects by the DB IDs in skipList.
	 * @param skipList - the skipList.
	 * @param instances - Objects from the database. Any object whose DB_ID is in skipList will *not* be in the output.
	 * @return
	 */
	private List<GKInstance> filterBySkipList(List<Long> skipList, List<GKInstance> instances)
	{
		if (skipList != null && !skipList.isEmpty())
		{
			return instances.parallelStream().filter(inst -> !skipList.contains(inst.getDBID())).collect(Collectors.toList());
		}
		else
		{
			return instances;
		}
	}
	

	private String getLastModificationAuthor(GKInstance instance) {
		final String noAuthor = "No modification or creation author";
		
		GKInstance mostRecentMod = null;
		try {
			List<GKInstance> modificationInstances = (List<GKInstance>) instance.getAttributeValuesList("modified");
			for (int index = modificationInstances.size() - 1; index > 0; index--) {
				GKInstance modificationInstance = modificationInstances.get(index);
				GKInstance author = (GKInstance) modificationInstance.getAttributeValue("author");
				// Skip modification instance for Solomon, Joel, or Guanming
				if (Arrays.asList("8939149", "1551959", "140537").contains(author.getDBID().toString())) {
					continue;
				}
				mostRecentMod = modificationInstance;
				break;
			}
		} catch (InvalidAttributeException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (mostRecentMod == null) {
			GKInstance created = null;
			try {
				created = (GKInstance) instance.getAttributeValue("created");
			} catch (InvalidAttributeException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (created != null) { 
				return created.getDisplayName();
			} else {
				return noAuthor;
			}
		}
		
		return mostRecentMod.getDisplayName();	
	}
	
	@Override
	public Report executeQACheck()
	{
		Report newEventReport = new DelimitedTextReport();

		List<GKInstance> reactionLikeEventsEditedIsNull = getNewEventInstances(this.adaptor, REACTIONLIKE_EVENT, "edited", IS_NULL, null);
		for (GKInstance instance : reactionLikeEventsEditedIsNull)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attribute \"edited\" is NULL", getLastModificationAuthor(instance) ));
		}
		
		List<GKInstance> reactionLikeEventsAuthoredIsNull = getNewEventInstances(this.adaptor, REACTIONLIKE_EVENT, "authored", IS_NULL, null);
		for (GKInstance instance : reactionLikeEventsAuthoredIsNull)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attribute \"authored\" is NULL", getLastModificationAuthor(instance) ));
		}

		List<GKInstance> reactionLikeEventsReviewedIsNull = getNewEventInstances(this.adaptor, REACTIONLIKE_EVENT, "reviewed", IS_NULL, null);
		for (GKInstance instance : reactionLikeEventsReviewedIsNull)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attribute \"reviewed\" is NULL", getLastModificationAuthor(instance) ));
		}

		List<GKInstance> reactionLikeEventsSummationIsNull = getNewEventInstances(this.adaptor, REACTIONLIKE_EVENT, "summation", IS_NULL, null);
		for (GKInstance instance : reactionLikeEventsSummationIsNull)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attribute \"summation\" is NULL", getLastModificationAuthor(instance) ));
		}

		List<GKInstance> reactionLikeEventsSpeiesIsNull = getNewEventInstances(this.adaptor, REACTIONLIKE_EVENT, "species", IS_NULL, null);
		for (GKInstance instance : reactionLikeEventsSpeiesIsNull)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attribute \"species\" is NULL", getLastModificationAuthor(instance) ));
		}

		List<GKInstance> noInferredFromAndNoLitRef = this.getNewEventsWithNoInferredFromAndNoLitRef(this.adaptor, null);
		for (GKInstance instance : noInferredFromAndNoLitRef)
		{
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), instance.getDisplayName(), instance.getSchemClass().getName(), "Attributes \"inferredFrom\" and \"literatureReferences\" are NULL", getLastModificationAuthor(instance) ));
		}
		
		newEventReport.setColumnHeaders(Arrays.asList("DBID","DisplayName","SchemaClass","Issue","MostRecentAuthor"));
		
		return newEventReport;
	}

}
