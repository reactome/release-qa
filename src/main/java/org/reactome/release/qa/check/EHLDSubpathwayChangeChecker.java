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
public class EHLDSubpathwayChangeChecker extends AbstractQACheck implements ChecksTwoDatabases
{
	private MySQLAdaptor olderDatabase;

	@Override
	public QAReport executeQACheck() throws Exception
	{
		QAReport report = new QAReport();
		report.setColumnHeaders(
			"EHLD Pathway ID",
			"HasEvent DB_IDs for " + this.dba.getDBName(),
			"HasEvent DB_IDs for " + getOtherDBAdaptor().getDBName()
		);
		
		List<Long> pathwayIds = new ArrayList<>(getPathwayIDsWithEHLD());
		List<EHLDPathway> oldPathways = getEHLDPathways(pathwayIds, getOtherDBAdaptor());
		List<EHLDPathway> newPathways = getEHLDPathways(pathwayIds, this.dba);

		for (EHLDPathway oldPathway : oldPathways) {
			Optional<EHLDPathway> newPathway = findEHLDPathway(newPathways, oldPathway.getDatabaseId());

			if (oldPathway.subPathwaysAreDifferent(newPathway)) {
				report.addLine(
					oldPathway.getDatabaseId().toString(),
					newPathway.map(EHLDPathway::getSubPathwayIdsAsString).orElse(""),
					oldPathway.getSubPathwayIdsAsString()
				);
			}
		}

		return report;
	}

	@Override
	public String getDisplayName() { return "EHLD_Subpathway_Change_Check"; }

	@Override
	public void setOtherDBAdaptor(MySQLAdaptor olderDatabase)	{ this.olderDatabase = olderDatabase; }

	public MySQLAdaptor getOtherDBAdaptor() { return this.olderDatabase; };

	private List<Long> getPathwayIDsWithEHLD() {
		List<Long> pathwayIds = new ArrayList<>();
		try {
			BufferedReader in = new BufferedReader(
				new InputStreamReader(new URL("https://reactome.org/download/current/ehld/").openStream())
			);

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
		pathwayIds.sort(Comparator.comparing(Long::longValue));
		return pathwayIds;
	}

	private List<EHLDPathway> getEHLDPathways(Collection<Long> dbIds, MySQLAdaptor database) {
		List<EHLDPathway> ehldPathways = new ArrayList<>();
		try {
			ehldPathways.addAll(
				asGKInstanceCollection(database.fetchInstance(dbIds))
				.stream()
				.filter(this::isPathway)
				.sorted(Comparator.comparing(GKInstance::getDBID))
				.map(EHLDPathway::new)
				.collect(Collectors.toList())
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ehldPathways;
	}

	private Optional<EHLDPathway> findEHLDPathway(List<EHLDPathway> pathways, Long databaseId) {
		return pathways
				.stream()
				.filter(pathway -> pathway.getDatabaseId().equals(databaseId))
				.findFirst();
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

	private class EHLDPathway {
		private GKInstance pathway;
		private List<GKInstance> subPathways;

		public EHLDPathway(GKInstance pathway) {
			if (pathway == null) {
				throw new NullPointerException("A pathway of type GKInstance must be provided");
			}

			this.pathway = pathway;
			this.subPathways = retrieveSubPathways(pathway);
		}

		public GKInstance getPathway() {
			return this.pathway;
		}

		public List<GKInstance> getSubPathways() {
			return this.subPathways;
		}

		public List<Long> getSubPathwayIds() {
			return	getSubPathways()
					.stream()
					.map(GKInstance::getDBID)
					.collect(Collectors.toList());
		}

		private String getSubPathwayIdsAsString() {
			return getSubPathwayIds()
					.stream()
					.map(String::valueOf)
					.collect(Collectors.joining("|"));
		}

		public Long getDatabaseId() {
			return getPathway().getDBID();
		}

		@SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
		public boolean subPathwaysAreDifferent(Optional<EHLDPathway> secondPathway) {
			return secondPathway.map(this::subPathwaysAreDifferent).orElse(true);
		}

		private boolean subPathwaysAreDifferent(EHLDPathway secondPathway) {
			return !(getSubPathwayIds().equals(secondPathway.getSubPathwayIds()));
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof EHLDPathway)) {
				return false;
			}

			return Objects.equals(this.getDatabaseId(), ((EHLDPathway) obj).getDatabaseId());
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.getDatabaseId());
		}

		@Override
		public String toString() {
			return getPathway().toString();
		}

		private List<GKInstance> retrieveSubPathways(GKInstance pathway) {
			List<GKInstance> subPathways = new ArrayList<>();
			try {
				subPathways.addAll(
					asGKInstanceCollection(pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent))
				);
			} catch (Exception e) {
				e.printStackTrace();
			}

			subPathways.sort(Comparator.comparing(GKInstance::getDBID));
			return subPathways;
		}
	}
}