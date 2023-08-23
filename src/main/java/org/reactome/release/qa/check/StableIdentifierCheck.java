package org.reactome.release.qa.check;

import java.util.*;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport; 

/**
 * Need to consider a database level enforcement.
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
@SliceQACheck
public class StableIdentifierCheck extends AbstractQACheck {

	public StableIdentifierCheck() {
	}
	
	@Override
	public QAReport executeQACheck() throws Exception {
		QAReport report = new QAReport();
		report.setColumnHeaders("DBID", "DisplayName", "Issue", "LastAuthor");

		Collection<GKInstance> stableIds = dba.fetchInstancesByClass(ReactomeJavaConstants.StableIdentifier);
		dba.loadInstanceAttributeValues(stableIds, new String[] {ReactomeJavaConstants.identifier});
		Map<String, Set<GKInstance>> idToInsts = new HashMap<>();
		for (GKInstance stableId : stableIds) {
			if (isEscaped(stableId)) {
				continue;
			}
			String id = (String) stableId.getAttributeValue(ReactomeJavaConstants.identifier);
			if (id == null) {
				report.addLine(stableId.getDBID().toString(),
							   stableId.getDisplayName(),
							   "Missing identifier",
							   QACheckerHelper.getLastModificationAuthor(stableId));
				continue;
			}
			idToInsts.compute(id, (key, set) -> {
				if (set == null)
					set = new HashSet<>();
				set.add(stableId);
				return set;
			});
			// Check if this stableId is used by an existing instance or a deleted instance
			Collection<GKInstance> instanceReferrers = stableId.getReferers(ReactomeJavaConstants.stableIdentifier);
			Collection<GKInstance> deletedInstanceReferrers =
				stableId.getReferers("deletedStableIdentifier");
			Collection<GKInstance> referrers = new ArrayList<>();
			referrers.addAll(instanceReferrers);
			referrers.addAll(deletedInstanceReferrers);

			if (referrers.size() == 0) {
				report.addLine(stableId.getDBID().toString(),
						stableId.getDisplayName(),
						"Not used",
						QACheckerHelper.getLastModificationAuthor(stableId));
			}
			else if (referrers.size() > 1) {
				report.addLine(stableId.getDBID().toString(),
						stableId.getDisplayName(),
						"Referred more than once",
						QACheckerHelper.getLastModificationAuthor(stableId));
			}
		}

		for (Collection<GKInstance> instances: idToInsts.values()) {
			if (instances.size() != 1) {
				for (GKInstance stableId: instances) {
					if (!isEscaped(stableId)) {
						report.addLine(stableId.getDBID().toString(),
								stableId.getDisplayName(),
								"Duplicated identifier",
								QACheckerHelper.getLastModificationAuthor(stableId));
					}
				}
			}
		}

		return report;
	}

}
