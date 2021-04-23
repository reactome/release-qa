package org.reactome.release.qa.check;

import java.util.ArrayList;
import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.GKSchemaClass;
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
			Collection<GKInstance> eventInstances = this.dba.fetchInstancesByClass(schemaClass);
			for (GKInstance eventInstance : eventInstances)
			{
				GKInstance stableIdentifier = (GKInstance) eventInstance.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
				Boolean released = (Boolean) stableIdentifier.getAttributeValue(ReactomeJavaConstants.released);
				if (released == null || released != true)
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

						// Report on this instance.
						report.addLine(eventInstance.getDBID().toString(), (String)eventInstance.getAttributeValue(ReactomeJavaConstants.name),
											editedIsNull ? NO : YES, authoredIsNull ? NO : YES, reviewedIsNull ? NO : YES,
											creator.getAttributeValue(ReactomeJavaConstants._displayName).toString());
					}
				}
			}
		}
		return report;
	}

}
