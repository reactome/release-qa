package org.reactome.release.qa.check;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.util.GKApplicationUtilities;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckUtilities;
import org.reactome.release.qa.common.QAReport;

/**
 * This check is to make sure the ReviewStatus setting for Events follow the following:
 *  1). Only events having three, four, and five stars are released. Otherwise, blocked error.
    2). Three stars: internal review is more than 6 months. Otherwise, warning (not blocked)
 * @author wug
 *
 */
@SuppressWarnings("unchecked")
@SliceQACheck
public class ReviewStatusCheck extends AbstractQACheck {
    
    public ReviewStatusCheck() {
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        report.setColumnHeaders("DB_ID", "DisplayName", "Issue", "LastIE");
        Collection<GKInstance> events = this.dba.fetchInstancesByClass(ReactomeJavaConstants.Event);
        for (GKInstance event : events) {
            String[] line = validateReviewStatus(event);
            if (line == null)
                continue;
            report.addLine(line);
        }
        return report;
    }

    
    /**
     * Make sure only 3 stars or update reviewed Events are released. This validation bypassed
     * Events that don't have reviewStatus assigned. Their reviewStatus should be filled later on.
     */
    private String[] validateReviewStatus(GKInstance inst) throws Exception {
        // Just in case
        if (!inst.getSchemClass().isValidAttribute("reviewStatus"))
            return null;
        GKInstance reviewStatus = (GKInstance) inst.getAttributeValue("reviewStatus");
        if (reviewStatus == null) {
            return null; // Treat as old release
        }
        String[] line = null;
        if (reviewStatus.getDisplayName().equals("one star")) {
            line = createQALine("one star event", inst);
        }
        else if (reviewStatus.getDisplayName().equals("two stars")) {
            line = createQALine("two stars event", inst);
        }
        else if (reviewStatus.getDisplayName().equals("three stars")) {
            // Check the time between the last reviewed
            List<GKInstance> internalReviewed = inst.getAttributeValuesList("internalReviewed");
            if (internalReviewed == null || internalReviewed.size() == 0) {
                line = createQALine("three stars without internal review", inst);
            }
            else {
                GKInstance lastIE = internalReviewed.get(internalReviewed.size() - 1);
                // Check if the datetime is 6 months old
                Date date = getDateTimeInInstanceEdit(lastIE);
                if (date == null) {
                    line = createQALine("three stars without dateTime in last internal review", inst);
                }
                else {
                    // Now
                    Calendar sixMonthAgo = GKApplicationUtilities.getCalendar();
                    // Six months ago
                    sixMonthAgo.set(Calendar.MONTH, sixMonthAgo.get(Calendar.MONTH) - 6);
                    if (date.after(sixMonthAgo.getTime())) {
                        line = createQALine("three stars with internal review shorter than 6 months", inst);
                    }
                }
            }
        }
        return line;
    }
    
    private String[] createQALine(String issue, GKInstance inst)  throws Exception {
        String[] line = new String[4];
        line[0] = inst.getDBID() + "";
        line[1] = inst.getDisplayName();
        line[2] = issue;
        line[3] = QACheckUtilities.getLatestCuratorIEFromInstance(inst) + "";
        return line;
    }
    
    /**
     * The following code is copied from the latest version of InstanceUtilities in the curator tool codebase.
     * It should be removed after the maven update for the new reactome-base API.
     * @param ie
     * @return
     * @throws Exception
     */
    private Date getDateTimeInInstanceEdit(GKInstance ie) throws Exception {
        // The type actually should be Java's Date in either java.util or java.sql. However, currently
        // it is converted from TimeStamp in MySQL to String. 
        String dateTime = (String) ie.getAttributeValue(ReactomeJavaConstants.dateTime);
        if (dateTime == null || dateTime.trim().length() == 0)
            return null;
        // All are in GMT
        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        if (dateTime.matches("(\\d){14}")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            format.setTimeZone(timeZone);
            return format.parse(dateTime);
        }
        else if (dateTime.matches("(\\d){4}-(\\d){2}-(\\d){2} (\\d){2}:(\\d){2}:(\\d){2}.(\\d)*")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
            format.setTimeZone(timeZone);
            return format.parse(dateTime);
        }
        else if (dateTime.matches("(\\d){4}-(\\d){2}-(\\d){2} (\\d){2}:(\\d){2}:(\\d){2}")) { // For MySQL 8
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            format.setTimeZone(timeZone);
            return format.parse(dateTime);
        }
        throw new IllegalArgumentException(ie + " has a wrongly formatted dateTime: " + dateTime);
    }
    
}
