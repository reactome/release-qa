package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.annotations.ReleaseQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QAReport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class will find pathways with EHLDs whose list of subpathways differ between two databases
 * @author weiserj
 *
 */
@ReleaseQATest
public class EHLDSubpathwayChangeCheck extends AbstractQACheck implements ChecksTwoDatabases
{
	private MySQLAdaptor olderDatabase;

	@Override
	public QAReport executeQACheck() throws Exception
	{
		QAReport report = new QAReport();
		report.setColumnHeaders(
			"EHLD Pathway ID",
			"HasEvent DB_IDs for " + this.dba.getDBName(),
			"HasEvent DB_IDs for " + this.olderDatabase.getDBName()
		);

		Collection<Long> pathwayIds = new ArrayList<>(getPathwayIDsWithEHLD());

		List<GKInstance> oldPathways = getPathways(pathwayIds, this.olderDatabase);
		List<GKInstance> newPathways = getPathways(pathwayIds, this.dba);

		Map<Long, List<Long>> newPathwayIdsToSubPathwayIds = getPathwayIdToSubPathwayIdMap(newPathways);

		for (Map.Entry<GKInstance, List<Long>> pathwayToSubPathwayIds : getPathwayInstanceToSubPathwayIdsMap(oldPathways).entrySet()) {
			GKInstance pathway = pathwayToSubPathwayIds.getKey();
			List<Long> oldSubPathwayIds = new ArrayList<>(pathwayToSubPathwayIds.getValue());
			List<Long> newSubPathwayIds = new ArrayList<>(newPathwayIdsToSubPathwayIds.get(pathway.getDBID()));

			if (!(oldSubPathwayIds.equals(newSubPathwayIds))) {
				report.addLine(
					pathway.getDBID().toString(),
					join(newSubPathwayIds),
					join(oldSubPathwayIds)
				);
			}
		}

		return report;
	}

	private Map<GKInstance, List<Long>> getPathwayInstanceToSubPathwayIdsMap(List<GKInstance> pathways) {
		Map<GKInstance, List<Long>> pathwayToSubPathwayIdsMap = new HashMap<>();
		for (GKInstance pathway : pathways) {
			try {
				@SuppressWarnings("unchecked")
				List<Long> subPathways =
						((List<GKInstance>) pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent))
								.stream()
								.map(GKInstance::getDBID)
								.sorted()
								.collect(Collectors.toList());

				pathwayToSubPathwayIdsMap.put(pathway, subPathways);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return pathwayToSubPathwayIdsMap;
	}

	private Map<Long, List<Long>> getPathwayIdToSubPathwayIdMap(List<GKInstance> pathways) {
		return getPathwayInstanceToSubPathwayIdsMap(pathways).entrySet()
			.stream()
			.collect(
				Collectors.toMap(
					pathway -> pathway.getKey().getDBID(),
					Map.Entry::getValue
				)
			);
	}

	@Override
	public String getDisplayName() { return "EHLD_Subpathway_Change_Check"; }

	@Override
	public void setOtherDBAdaptor(MySQLAdaptor olderDatabase)
	{
		this.olderDatabase = olderDatabase;
	}

	private List<Long> getPathwayIDsWithEHLD() {
		List<Long> pathwayIds = new ArrayList<>();
		try {
			BufferedReader in = new BufferedReader(
					new InputStreamReader(new URL("https://reactome.org/download/current/ehld/").openStream()));

			Pattern pattern = Pattern.compile("\"(\\d+)\\.svg\"");
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				Matcher matcher = pattern.matcher(inputLine);
				if (matcher.find()) {
					Long pathwayId = Long.parseLong(matcher.group(1));
					pathwayIds.add(pathwayId);
				}
			}

			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pathwayIds;
	}

	private List<GKInstance> getPathways(Collection<Long> dbIds, MySQLAdaptor database) {
		List<GKInstance> pathways = new ArrayList<>();
		try {
			pathways.addAll(
				asGKInstanceCollection(database.fetchInstance(dbIds))
				.stream()
				.filter(this::isPathway)
				.collect(Collectors.toList())
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pathways;
	}

	// Cast moved to its own method to restrict scope for suppression of unchecked warning
	// https://stackoverflow.com/questions/31737288/how-to-suppress-unchecked-typecast-warning-with-generics-not-at-declaration
	@SuppressWarnings("unchecked")
	private Collection<GKInstance> asGKInstanceCollection(Collection instances) {
		return ((Collection<GKInstance>) instances);
	}

	private boolean isPathway(GKInstance instance) {
		return instance.getSchemClass().getName().equals(ReactomeJavaConstants.Pathway);
	}

	private String join(List<?> objectsToJoin) {
		return objectsToJoin.stream().map(String::valueOf).collect(Collectors.joining("|"));
	}
}