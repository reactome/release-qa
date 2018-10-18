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
	final private String NOT_AVAILABLE = "N/A";
	private MySQLAdaptor olderDatabase;

	@Override
	public QAReport executeQACheck() throws Exception
	{
		QAReport report = new QAReport();
		report.setColumnHeaders(
			"EHLD Pathway Name",
			"EHLD Pathway ID",
			"Subpathways added in " + this.dba.getDBName(),
			"Subpathways removed in " + this.dba.getDBName()
		);

		List<Long> pathwayIds = new ArrayList<>(getPathwayIDsWithEHLD());
		List<EHLDPathway> oldPathways = getEHLDPathways(pathwayIds, getOtherDBAdaptor());
		List<EHLDPathway> newPathways = getEHLDPathways(pathwayIds, this.dba);

		for (EHLDPathway oldPathway : oldPathways) {
			Optional<EHLDPathway> potentialNewPathway = findEHLDPathway(newPathways, oldPathway.getDatabaseId());

			if (oldPathway.subPathwaysAreDifferent(potentialNewPathway)) {
				report.addLine(
					oldPathway.getPathwayName(),
					oldPathway.getDatabaseId().toString(),
					potentialNewPathway
						.map(newPathway -> newPathway.getAsStringSubPathwayIdsNotPresentIn(oldPathway))
						.orElse(NOT_AVAILABLE),
					oldPathway.getAsStringSubPathwayIdsNotPresentIn(potentialNewPathway)
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

	private List<Long> getPathwayIDsWithEHLD() throws EHLDPathwayIDRetrievalException {
		final String reactomeEHLDURL = "https://reactome.org/download/current/ehld/";
		List<Long> pathwayIds = new ArrayList<>();
		try {
			BufferedReader ehldWebSource = new BufferedReader(
				new InputStreamReader(new URL(reactomeEHLDURL).openStream())
			);

			pathwayIds.addAll(parsePathwayIds(ehldWebSource));
			pathwayIds.sort(Comparator.comparing(Long::longValue));

			ehldWebSource.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (pathwayIds.isEmpty()) {
			throw new EHLDPathwayIDRetrievalException("Unable to retrieve pathway ids from " + reactomeEHLDURL);
		}

		return pathwayIds;
	}

	private List<Long> parsePathwayIds(BufferedReader ehldWebSource) throws IOException {
		List<Long> pathwayIds = new ArrayList<>();

		Pattern svgFileName = getSVGFileNamePattern();
		String sourceLine;
		while ((sourceLine = ehldWebSource.readLine()) != null) {
			Matcher svgMatcher = svgFileName.matcher(sourceLine);
			if (svgMatcher.find()) {
				Long pathwayId = Long.parseLong(svgMatcher.group(1));
				pathwayIds.add(pathwayId);
			}
		}

		return pathwayIds;
	}

	private Pattern getSVGFileNamePattern() {
		return Pattern.compile("\"(\\d+)\\.svg\"");
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
		return  pathways
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
		return instance.getSchemClass().isa(ReactomeJavaConstants.Pathway);
	}

	private class EHLDPathwayIDRetrievalException extends Exception {
		public EHLDPathwayIDRetrievalException(String retrievalError) {
			super(retrievalError);
		}
	}

	private class EHLDPathway {
		final private GKInstance pathway;
		final private List<GKInstance> subPathways;
		final private Integer reactomeVersion;

		public EHLDPathway(GKInstance pathway) {
			if (pathway == null) {
				throw new NullPointerException("A pathway of type GKInstance must be provided");
			}
			if (isElectronicallyInferred(pathway)) {
				throw new IllegalArgumentException("The pathway provided cannot be electronically inferred");
			}

			this.pathway = pathway;
			this.subPathways = retrieveSubPathways(pathway);
			this.reactomeVersion = getReactomeVersion(pathway);
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

		public String getSubPathwayIdsAsString() {
			return	stringOrDefaultValue(
						asPipeDelimitedString(getSubPathwayIds()),
						EHLDSubpathwayChangeChecker.this.NOT_AVAILABLE
					);
		}

		@SuppressWarnings({"OptionalUsedAsFieldOrParameterType"})
		public String getAsStringSubPathwayIdsNotPresentIn(Optional<EHLDPathway> secondPathway) {
			return secondPathway.map(this::getAsStringSubPathwayIdsNotPresentIn).orElse(EHLDSubpathwayChangeChecker.this.NOT_AVAILABLE);
		}

		public String getAsStringSubPathwayIdsNotPresentIn(EHLDPathway secondPathway) {
			return	stringOrDefaultValue(
						asPipeDelimitedString(getSubPathwayIdsNotPresentIn(secondPathway)),
						EHLDSubpathwayChangeChecker.this.NOT_AVAILABLE
					);
		}

		private List<Long> getSubPathwayIdsNotPresentIn(EHLDPathway secondPathway) {
			Set<Long> otherSubPathwayIds = new HashSet<>(secondPathway.getSubPathwayIds());

			return	getSubPathwayIds()
					.stream()
					.filter(subPathwayId -> !otherSubPathwayIds.contains(subPathwayId))
					.collect(Collectors.toList());
		}

		private String asPipeDelimitedString(List<?> listElements) {
			return	listElements
					.stream()
					.map(String::valueOf)
					.collect(Collectors.joining("|"));
		}

		private String stringOrDefaultValue(String string, String defaultValue) {
			return !string.isEmpty() ? string : defaultValue;
		}

		public Long getDatabaseId() {
			return getPathway().getDBID();
		}

		public String getPathwayName() {
			return getPathway().getDisplayName();
		}

		public Integer getReactomeVersion() { return this.reactomeVersion; };

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

			return Objects.equals(this.getDatabaseId(), ((EHLDPathway) obj).getDatabaseId()) &&
					Objects.equals(this.getPathwayName(), ((EHLDPathway) obj).getPathwayName());
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.getDatabaseId(), this.getPathwayName());
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

		private boolean isElectronicallyInferred(GKInstance event) {
			GKInstance evidenceType;
			try {
				evidenceType = ((GKInstance) event.getAttributeValue(ReactomeJavaConstants.evidenceType));
			} catch (Exception e) {
				throw new IllegalArgumentException("The GKInstance provided must be an event (i.e. pathway or reaction like event)");
			}

			if (evidenceType == null) {
				return false;
			}

			return evidenceType
				.getDisplayName()
				.toLowerCase()
				.contains("electronic");
		}

		private Integer getReactomeVersion(GKInstance pathway) {
			try {
				return ((MySQLAdaptor) pathway.getDbAdaptor()).getReleaseNumber();
			} catch (Exception e) {
				return null;
			}
		}
	}
}