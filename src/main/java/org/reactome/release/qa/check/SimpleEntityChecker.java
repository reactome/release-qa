package org.reactome.release.qa.check;

import java.util.Arrays;
import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * Not sure what this check is for.
 */
@SuppressWarnings("unchecked")
@SliceQATest
public class SimpleEntityChecker extends AbstractQACheck {

	@Override
	public QAReport executeQACheck() throws Exception {
	    QAReport report = new QAReport();
	    Collection<GKInstance> instances = dba.fetchInstanceByAttribute(ReactomeJavaConstants.SimpleEntity,
	                                                                    ReactomeJavaConstants.species,
	                                                                    "IS NOT NULL",
	                                                                    null);
	    for (GKInstance simpleEntity : instances) {
	        GKInstance speciesInstance = (GKInstance) simpleEntity.getAttributeValue(ReactomeJavaConstants.species);
	        report.addLine(Arrays.asList(simpleEntity.getDBID().toString(), 
	                                     simpleEntity.getDisplayName(), 
	                                     simpleEntity.getSchemClass().getName(), 
	                                     "Simple entity with non-null species: " + speciesInstance.toString(), 
	                                     QACheckerHelper.getLastModificationAuthor(simpleEntity)));
	    }
	    report.setColumnHeaders(Arrays.asList("DBID","DisplayName","SchemaClass","Issue","MostRecentAuthor"));
	    return report;
	}

    @Override
    public String getDisplayName() {
        return "SimpleEntity_With_Species";
    }
	
}
