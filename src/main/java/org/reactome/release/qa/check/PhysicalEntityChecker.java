package org.reactome.release.qa.check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gk.model.GKInstance;
import org.reactome.release.qa.common.QAReport;

/**
 * This is a check for a case like this: A container PE (e.g. Complex, EntitySet, and Polymer)
 * doesn't have a species value, but its contained PEs have. The reason why this should be checked
 * is because species in these classes is a required slot, not mandatory. Should we make sure it is
 * mandatory? Probably we cannot, since the use of species in SimpleEntity is optional.
 *
 */
public class PhysicalEntityChecker extends AbstractQACheck {
	
	@Override
    public String getDisplayName() {
        return "PhysicalEntity_Container_Species";
    }

    @Override
	public QAReport executeQACheck() throws Exception {
		QAReport report = new QAReport();
		List<GKInstance> physicalEntities = new ArrayList<GKInstance>();
		for (String schemaClass : Arrays.asList("Complex", "EntitySet", "Polymer")) {	
			physicalEntities.addAll(QACheckerHelper.getInstancesWithNullAttribute(this.dba, schemaClass, "species", null));
		}
		
		for (GKInstance physicalEntity : physicalEntities) {
			int numComponents = QACheckerHelper.componentsHaveSpecies(physicalEntity);
			if(numComponents > 0) {
				report.addLine(Arrays.asList());
				report.addLine(Arrays.asList(physicalEntity.getDBID().toString(), 
				        physicalEntity.getDisplayName(), 
				        physicalEntity.getSchemClass().getName(), 
				        "NULL species but " + numComponents + " components have species",  
				        QACheckerHelper.getLastModificationAuthor(physicalEntity)));
			}
		}
		report.setColumnHeaders(Arrays.asList("DBID", "DisplayName", "SchemaClass", "Issue", "MostRecentAuthor"));
		return report;
	}

}
