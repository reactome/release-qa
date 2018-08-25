package org.reactome.release.qa.check;

import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.annotations.ReleaseQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QAReport;

/**
 * This class will find Stable Identifiers whose version numbers differ between gk_central and test_reactome_XX
 * @author sshorser
 *
 */
@ReleaseQATest
public class FindMismatchedStableIdentifierVersions extends AbstractQACheck implements ChecksTwoDatabases
{
	private MySQLAdaptor otherAdaptor;

	@Override
	public QAReport executeQACheck() throws Exception
	{
		QAReport report = new QAReport();
		report.setColumnHeaders("StableIdentifier", this.dba.getDBName(), this.otherAdaptor.getDBName());
		@SuppressWarnings("unchecked")
		Set<GKInstance> gkCentralSTIDs = (Set<GKInstance>) this.otherAdaptor.fetchInstancesByClass(ReactomeJavaConstants.StableIdentifier);

		for (GKInstance gkCentralSTID : gkCentralSTIDs)
		{
			String identifier = (String) gkCentralSTID.getAttributeValue(ReactomeJavaConstants.identifier);
			String version = (String) gkCentralSTID.getAttributeValue(ReactomeJavaConstants.identifierVersion);
			
			// This assumes that the DBID from gk_central is used for the same Stable Identifier as in test_reactome_XX.
			GKInstance testReactomeSTID = this.dba.fetchInstance(gkCentralSTID.getDBID());
			
			if (testReactomeSTID!=null && testReactomeSTID.getSchemClass().getName().equals(ReactomeJavaConstants.StableIdentifier))
			{
				String testReactomeIdentifierString = (String) testReactomeSTID.getAttributeValue(ReactomeJavaConstants.identifier);
				String testReactomeIdentifierVersionString = (String) testReactomeSTID.getAttributeValue(ReactomeJavaConstants.identifierVersion);
				
				if (identifier.equals(testReactomeIdentifierString) && !version.equals(testReactomeIdentifierVersionString))
				{
					report.addLine(testReactomeIdentifierString, version, testReactomeIdentifierVersionString);
				}
			}
		}
		return report;
	}

	@Override
	public String getDisplayName()
	{
		return "Find_Mismatched_Stabled_Identifier_Versions";
	}

	@Override
	public void setOtherDBAdaptor(MySQLAdaptor adaptor)
	{
		this.otherAdaptor = adaptor;
	}

}