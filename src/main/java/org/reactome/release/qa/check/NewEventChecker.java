package org.reactome.release.qa.check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.MySQLAdaptor.AttributeQueryRequest;
import org.gk.persistence.MySQLAdaptor.QueryRequestList;
import org.gk.schema.InvalidAttributeException;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/** 
 * 
 * A set of QAs related to new ReactionLikeEvents, which are collected
 * based on inferredFrom = null and stableIdentifier is not null and its released
 * is null or false.
 *
 */
@SuppressWarnings("unchecked")
public class NewEventChecker extends AbstractQACheck {
    
	@Override
    public String getDisplayName() {
        return "New_Event_QA";
    }

    /**
	 * Gets new Events that have no value for "inferredFrom" and also have no value for "literatureReference" 
	 * @param dba
	 * @param skipList
	 * @return
	 */
	private List<GKInstance> getNewEventsWithNoInferredFromAndNoLitRef(MySQLAdaptor dba, List<Long> skipList) throws Exception {
	    AttributeQueryRequest stIdIsNotNullRequest = dba.new AttributeQueryRequest(ReactomeJavaConstants.ReactionlikeEvent,
	            "stableIdentifier",
	            QACheckerHelper.IS_NOT_NULL,
	            null);
	    AttributeQueryRequest noInferredFrom = this.dba.new AttributeQueryRequest(ReactomeJavaConstants.ReactionlikeEvent,
	            "inferredFrom",
	            QACheckerHelper.IS_NULL,
	            null);
	    AttributeQueryRequest noLitRef = this.dba.new AttributeQueryRequest(ReactomeJavaConstants.ReactionlikeEvent, 
	            "literatureReference",
	            QACheckerHelper.IS_NULL,
	            null);
	    QueryRequestList queryReqestList = this.dba.new QueryRequestList();
	    queryReqestList.add(stIdIsNotNullRequest);
	    queryReqestList.add(noLitRef);
	    queryReqestList.add(noInferredFrom);

	    Collection<GKInstance> newEvents = dba.fetchInstance(queryReqestList);
	    List<GKInstance> instances = new ArrayList<GKInstance>();
	    filterToUnreleasedEvents(instances, newEvents);
	    return QACheckerHelper.filterBySkipList(skipList, instances);
	}

	/**
	 * Gets Events that are unreleased - They have a stableIdentifier and the stableIdentifier's "released" attribute is NULL/false
	 * @param instances
	 * @param newEvents
	 * @throws InvalidAttributeException
	 * @throws Exception
	 */
	private void filterToUnreleasedEvents(List<GKInstance> instances, Collection<GKInstance> newEvents) throws InvalidAttributeException, Exception
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
	private List<GKInstance> getNewEventInstances(MySQLAdaptor dba,
	                                              String schemaClass,
	                                              String attribute, 
	                                              String operator, 
	                                              List<Long> skipList) throws Exception {
	    AttributeQueryRequest stIdIsNotNullRequest = dba.new AttributeQueryRequest(schemaClass, ReactomeJavaConstants.stableIdentifier, QACheckerHelper.IS_NOT_NULL, null);
	    AttributeQueryRequest queryRequest = dba.new AttributeQueryRequest(schemaClass, attribute, operator, null);

	    QueryRequestList queryReqestList = dba.new QueryRequestList();
	    queryReqestList.add(stIdIsNotNullRequest);
	    queryReqestList.add(queryRequest);

	    Collection<GKInstance> newEvents = dba.fetchInstance(queryReqestList);
	    List<GKInstance> instances = new ArrayList<>();
	    filterToUnreleasedEvents(instances, newEvents);
	    // filter by skip list
	    return QACheckerHelper.filterBySkipList(skipList, instances);
	}

	@Override
	public QAReport executeQACheck() throws Exception {
		QAReport newEventReport = new QAReport();

		List<GKInstance> reactionLikeEventsSummationIsNull = getNewEventInstances(dba,
		                                                                          ReactomeJavaConstants.ReactionlikeEvent, 
		                                                                          ReactomeJavaConstants.summation, 
		                                                                          QACheckerHelper.IS_NULL, 
		                                                                          null);
		for (GKInstance instance : reactionLikeEventsSummationIsNull) {
		    newEventReport.addLine(Arrays.asList(instance.getDBID().toString(), 
		                                         instance.getDisplayName(), 
		                                         instance.getSchemClass().getName(), 
		                                         "Attribute \"summation\" is NULL", 
		                                         QACheckerHelper.getLastModificationAuthor(instance)));
		}

		List<GKInstance> noInferredFromAndNoLitRef = this.getNewEventsWithNoInferredFromAndNoLitRef(dba, null);
		for (GKInstance instance : noInferredFromAndNoLitRef) {
			newEventReport.addLine(Arrays.asList(instance.getDBID().toString(),
			        instance.getDisplayName(), 
			        instance.getSchemClass().getName(), 
			        "Attributes \"inferredFrom\" and \"literatureReferences\" are NULL", 
			        QACheckerHelper.getLastModificationAuthor(instance)));
		}
		
		newEventReport.setColumnHeaders(Arrays.asList("DBID","DisplayName","SchemaClass","Issue","MostRecentAuthor"));
		
		return newEventReport;
	}

}
