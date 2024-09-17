package org.reactome.release.qa.check;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * These QA checks were mostly used during the CoV-1-to-CoV-2 inference process. For now they are being kept, but may
 * be removed, given they are for very specific scenarios (August 2020).
 *
 * This QA check class evaluates various properties of the Events contained within the 'SARS-CoV-2 Infection' Pathway.
 * This Pathway was created using a modified version of orthoinference to expedite Reactome's annotations of CoV-2
 * data.
 * Reports: 1a) Event Summation instances with discrepancy between their 'created' and modified' dateTimes
 * or 1b) Summations without a modified instance - they need to have been modified at least once to reflect an update
 * to the Summation text. 2) Events that don't contain at least 1 2020 (or later) literatureReference. Events where
 * the summation format is not correct, also have the following checks: 3a) Events where the inferredFrom slot is
 * empty despite having received the correct COVID inference summation text. 3b) Events where the inferredFrom is
 * populated but the updated summation text hasn't been updated. These are complementary cases, and just served to
 * notify the curator of a possible mistake with CoV-2 instances.
 *
 * @author jcook
 */

@SliceQACheck
public class CoV2InfectionPathwayEventCheck extends AbstractQACheck {

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();

        GKInstance cov2InfectionPathwayInst = dba.fetchInstance(QACheckerHelper.COV_2_INFECTION_PATHWAY_DB_ID);

        // Get all Events contained within 'SARS-CoV-2 Infection' pathway.
        for (GKInstance cov2Event : InstanceUtilities.getContainedEvents(cov2InfectionPathwayInst)) {
            List<String> issues = new ArrayList<>();
            // If does not have any modified instances, or if created instance predates most recent modified instance.
            if (!hasRecentlyModifiedSummation(cov2Event)) {
                issues.add("Summation instance has not been recently modified");
            }

            // inferredFrom being null is a proxy for curator being 'done' with instance. Therefore, if it is null and the
            // issue is being reported, it indicates they likely made an error.
            // If literatureReference attribute does not contain at least 1 litRef from 2020.
            if (cov2Event.getAttributeValue(ReactomeJavaConstants.inferredFrom) == null && !hasRecentLiteratureReference(cov2Event)) {
                issues.add("Does not contain a literature reference from 2020 or later");
            }
            // Summation text was updated near end of release cycle where CoV-1-to-CoV-2 projections happened.
            // If it didn't have the updated text, there are two reasons for it, outlined below.
            if (!hasCorrectSummationFormat(cov2Event)) {
                if (cov2Event.getAttributeValue(ReactomeJavaConstants.inferredFrom) == null) {
                    issues.add("InferredFrom is empty but instance has COVID inference summation text");
                } else {
                    issues.add("InferredFrom is populated but instance does NOT have COVID inference summation text.");
                }
            }
            if (issues.size() > 0) {
                report.addLine(getReportLine(cov2Event, String.join("|",issues)));
            }
        }
        report.setColumnHeaders(getColumnHeaders());
        return report;
    }

    /**
     * Checks if instance created dateTime is before most recent modified instance's dateTime. Also checks if
     * any modified instance is present -- it should be given that there was at least 1 required modification from the
     * CoV-1-to-CoV-2 merge.
     * @param cov2Event - GKInstance, Event contained within 'SARS-CoV-2 Infection' Pathway.
     * @return - boolean, true if modification property looks correct, false if not.
     * @throws Exception, thrown by MySQLAdaptor if unable to retrieve attribute values from Summation instance.
     */
    private boolean hasRecentlyModifiedSummation(GKInstance cov2Event) throws Exception {
       for (GKInstance summation : (Collection<GKInstance>) cov2Event.getAttributeValuesList(ReactomeJavaConstants.summation)) {
            GKInstance createdInst = (GKInstance) summation.getAttributeValue(ReactomeJavaConstants.created);
            List<GKInstance> modifieds = summation.getAttributeValuesList(ReactomeJavaConstants.modified);
            if (modifieds.size() > 0) {
                // At time of writing (August 2020), nothing is reported from this clause, but it is a good check.
                GKInstance mostRecentModifiedInst = modifieds.get(modifieds.size() - 1);
                // Only check Year/Month/Day since the changes took place more than a day before Curators got to look at them in gk_central.
                LocalDate createdDateTime = getDateTimeFromInstance(createdInst);
                LocalDate modifiedDateTime = getDateTimeFromInstance(mostRecentModifiedInst);
                if (createdDateTime.isAfter(modifiedDateTime)) {
                    return false;
                }
            // Should be at least 1 modified instance in CoV-1-to-CoV-2 instances.
            } else {
                return false;
            }
       }
        return true;
    }

    /**
     * Parses out the Date from dateTime attribute of the incoming InstanceEdit instance type.
     * This value is formatted as 'yyyy-MM-dd hh:mm:ss', and the 'date' portion (1st half) is all that is needed for this test.
     * @param instanceEditInst - GKInstance, from either the 'created' or 'modified' attribute of an instance.
     * @return - LocalDate, in the format 'yyyy-MM-dd'.
     * @throws Exception, thrown if the MySQLAdaptor throws an exception when trying to obtain the 'dateTime' attribute from an InstanceEdit.
     */
    private LocalDate getDateTimeFromInstance(GKInstance instanceEditInst) throws Exception {
        return LocalDate.parse(instanceEditInst.getAttributeValue(ReactomeJavaConstants.dateTime).toString().split(" ")[0]);
    }

    /**
     * Checks if Event has a 2020 or later literatureReference. This is because CoV-2 evidence was very recent when
     * this was being evaluated.
     * @param cov2Event - GKInstance, Event contained within 'SARS-CoV-2 Infection' Pathway.
     * @return - boolean, true if at least one 2020 or later literatureReference, false if not.
     * @throws Exception, thrown by MySQLAdaptor if unable to get attribute values from Event or LiteratureReference
     * instances.
     */
    private boolean hasRecentLiteratureReference(GKInstance cov2Event) throws Exception {
        // Iterate through all literatureReference instances, checking the 'year' attribute.
        Collection<GKInstance> literatureReferences =
            (Collection<GKInstance>) cov2Event.getAttributeValuesList(ReactomeJavaConstants.literatureReference);

        for (GKInstance literatureReference : literatureReferences) {
            if (literatureReferenceFromOrLaterThan2020(literatureReference) ||
                urlFromOrLaterThan2020(literatureReference)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the instance is a literature reference and is from 2020 or later
     * @param literatureReference - GKInstance, literatureReference to check when it publishes
     * @return - boolean, true if instance argument is a literature reference and from 2020 or later
     * @throws Exception, thrown by MySQLAdaptor if unable to get attribute values from the Literature Reference
     * instance
     */
    private boolean literatureReferenceFromOrLaterThan2020(GKInstance literatureReference) throws Exception {
        if (!literatureReference.getSchemClass().isa(ReactomeJavaConstants.LiteratureReference)) {
            return false;
        }

        String year = literatureReference.getAttributeValue(ReactomeJavaConstants.year).toString();

        return publicationYearFromOrLaterThan2020(year);
    }

    /**
     * Checks if the instance is a URL and is from 2020 or later
     * @param url - GKInstance, URL to check when it publishes
     * @return - boolean, true if instance argument is a URL and from 2020 or later
     * @throws Exception, thrown by MySQLAdaptor if unable to get attribute values from the URL instance
     */
    private boolean urlFromOrLaterThan2020(GKInstance url) throws Exception {
        if (!url.getSchemClass().isa(ReactomeJavaConstants.URL)) {
            return false;
        }

        // Only 10 URL-type LiteratureReferences currently exist in the Database for CoV-2 instances (August 2020).
        // All URLs are to pre-prints, and contain the string 'yyyy.mm.dd' in the URL.
        // Example: https://www.biorxiv.org/content/10.1101/2020.04.26.061705v1.full
        // This just simply checks for the existence of '2020.' in the URL string.
        String urlString = url.getAttributeValue(ReactomeJavaConstants.uniformResourceLocator).toString();

        Pattern dateInURLPattern = Pattern.compile("(\\d{4})\\.\\d{2}\\.\\d{2}");
        Matcher dateInURLMatcher = dateInURLPattern.matcher(urlString);

        if (!dateInURLMatcher.find()) {
            return false;
        }

        String year = dateInURLMatcher.group(1);
        return publicationYearFromOrLaterThan2020(year);
    }

    private boolean publicationYearFromOrLaterThan2020(String year) {
        return Integer.parseInt(year) >= 2020;
    }

    /**
     * Checks that the Event's Summation instance contains the updated Summation text, in both the 'text' and 'displayName'.
     * Since displayNames sometimes truncate summation names, only the first part of the sentence is checked.
     * @param cov2Event - GKInstance, Event contained within 'SARS-CoV-2 Infection' Pathway.
     * @return - boolean, true if contains updated Summation text, false if not.
     * @throws Exception, thrown by MySQLAdaptor.
     */
    private boolean hasCorrectSummationFormat(GKInstance cov2Event) throws Exception {
        // Can't do full 'text' of the COVID inference message since displayNames get truncated.
        String inferredEventCovidText = "This COVID-19 event has been created by a combination";
        for (GKInstance summation : (Collection<GKInstance>) cov2Event.getAttributeValuesList(ReactomeJavaConstants.summation)) {
            String displayName = summation.getDisplayName();
            String text = summation.getAttributeValue(ReactomeJavaConstants.text).toString();
            if (cov2Event.getAttributeValue(ReactomeJavaConstants.inferredFrom) != null) {
                //Inferred COVID summation should be there
                if (!displayName.contains(inferredEventCovidText) || !text.contains(inferredEventCovidText)) {
                    return false;
                }
            } else {
                // Inferred COVID summation should NOT be there
                if (displayName.contains(inferredEventCovidText) || text.contains(inferredEventCovidText)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getReportLine(GKInstance cov2Event, String issues) {
        return String.join("\t",
                cov2Event.getDBID().toString(),
                cov2Event.getDisplayName(),
                QACheckerHelper.getLastModificationAuthor(cov2Event),
                issues
        );
    }

    private String[] getColumnHeaders() {
        return new String[]{"DB_ID_Event", "DisplayName_Event", "MostRecentAuthor_Event", "Issue(s)"};
    }

    @Override
    public String getDisplayName() {
        return "CoV-2_Infection_Pathway_Events_With_Summation_And_Literature_Reference_Issues";
    }

}
