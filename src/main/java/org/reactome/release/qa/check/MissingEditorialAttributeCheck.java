package org.reactome.release.qa.check;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QAReport;

/**
 * This QA check was requested by Lisa.
 * Description:
 * "Weekly QA and release QA: Report new events in
 * slice (those where stableID released attribute
 * is != True) and edited, reviewed, or authored
 * attribute is null."
 * @author sshorser
 *
 */
@SliceQACheck
public class MissingEditorialAttributeCheck extends AbstractQACheck {

	private static final String YES = "YES";
	private static final String NO = "NO";
	private static Set<String> reportedEntities = new HashSet<>();

	@Override
	public QAReport executeQACheck() throws Exception
	{
		GKSchemaClass eventClass = (GKSchemaClass) this.dba.getSchema().getClassByName(ReactomeJavaConstants.Event);
		QAReport report = new QAReport();
		report.setColumnHeaders("DB_ID", "Name", "Edited?", "Authored?", "Reviewed?", "Created");
		Collection<GKSchemaClass> classesToCheck = (Collection<GKSchemaClass>) eventClass.getSubClasses();
		// make sure Event is in there with its subclasses.
		classesToCheck = new ArrayList<>(classesToCheck);
		classesToCheck.add(eventClass);
		for (GKSchemaClass schemaClass : classesToCheck)
		{
			Collection<GKInstance> eventInstances = (Collection<GKInstance>) this.dba.fetchInstancesByClass(schemaClass);
			for (GKInstance eventInstance : eventInstances)
			{
				String[] line = getReportLine(eventInstance);
				if (line.length > 0)
				{
					report.addLine(line);
				}
			}
		}
		return report;
	}

	/**
	 * Returns an array of Strings that will be added as a line to the report - IFF the eventInstance meets the criteria:
	 * StableIdentifier is NOT released; Any of: authored, edited, reviewed are null.
	 * @param eventInstance
	 * @return
	 * @throws InvalidAttributeException
	 * @throws Exception
	 */
	private String[] getReportLine(GKInstance eventInstance) throws InvalidAttributeException, Exception
	{
		ArrayList<String> line = new ArrayList<>();
		GKInstance stableIdentifier = (GKInstance) eventInstance.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
		Boolean released = (Boolean) stableIdentifier.getAttributeValue(ReactomeJavaConstants.released);
		String dbId = eventInstance.getDBID().toString();
		// Check that the instance hasn't already been reported. It _could_ happen, since we're traversing all subclasses of Event.
		if ((released == null || !released.booleanValue()) && !reportedEntities.contains(dbId))
		{
			// We have found an unreleased Event! Now we need to check the editorial attributes.
			GKInstance edited = (GKInstance) eventInstance.getAttributeValue(ReactomeJavaConstants.edited);
			GKInstance authored = (GKInstance) eventInstance.getAttributeValue(ReactomeJavaConstants.authored);
			GKInstance reviewed = (GKInstance) eventInstance.getAttributeValue(ReactomeJavaConstants.reviewed);

			boolean editedIsNull = edited == null;
			boolean authoredIsNull = authored == null;
			boolean reviewedIsNull = reviewed == null;
			if (editedIsNull || authoredIsNull || reviewedIsNull)
			{
				// We also need the creator info for the report.
				GKInstance creator = (GKInstance) eventInstance.getAttributeValue(ReactomeJavaConstants.created);
				line.add(dbId);
				line.add((String)eventInstance.getAttributeValue(ReactomeJavaConstants.name));
				line.add(editedIsNull ? NO : YES);
				line.add(authoredIsNull ? NO : YES);
				line.add(reviewedIsNull ? NO : YES);
				line.add(creator.getAttributeValue(ReactomeJavaConstants._displayName).toString());
				reportedEntities.add(dbId);
			}
		}
		return line.toArray(new String[0]);
	}

}
