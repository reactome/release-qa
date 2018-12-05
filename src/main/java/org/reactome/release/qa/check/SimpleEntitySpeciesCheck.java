package org.reactome.release.qa.check;

import java.util.Arrays;
import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * Checks for a SimpleEntity with a species.
 */
@SuppressWarnings("unchecked")
@SliceQACheck
public class SimpleEntitySpeciesCheck extends AbstractQACheck {

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
	    report.setColumnHeaders("DBID","DisplayName","SchemaClass","Issue","MostRecentAuthor");
	    
	    return report;
	}

    @Override
    public String getDisplayName() {
        return "SimpleEntity_Has_Species";
    }
	
}
