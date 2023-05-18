package org.reactome.release.qa.check;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
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
public class ReviewStatusCheck extends AbstractQACheck implements ChecksTwoDatabases {
    private MySQLAdaptor priorDBA;
    
    public ReviewStatusCheck() {
    }

    @Override
    public void setOtherDBAdaptor(MySQLAdaptor adaptor) {
        this.priorDBA = adaptor;
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        if (priorDBA == null)
            throw new IllegalStateException("Need to specify the prior database for " + getClass().getName());
        QAReport report = new QAReport();
        report.setColumnHeaders("DB_ID", "DisplayName", "Issue", "LastIE", "Note");
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
        String[] line = null;
        if (reviewStatus == null) {
            line = createQALine("no review status assigned", inst);
        }
        else if (reviewStatus.getDisplayName().equals("one star")) {
            line = createQALine("one star event", inst);
        }
        else if (reviewStatus.getDisplayName().equals("two stars")) {
            // Get note for two stars
            String note = getNoteForTwoStars(inst);
            line = createQALine("two stars event", note, inst);
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
    
    
    /**
     * Per request from Lisa, generate the structure changes for two star Events.
     * @param inst
     * @return
     * @throws Exception
     */
    private String getNoteForTwoStars(GKInstance inst) throws Exception {
        GKInstance oldInst = priorDBA.fetchInstance(inst.getDBID());
        if (oldInst == null)
            return "Cannot find the instance in the prior database";
        if (!inst.getSchemClass().getName().equals(oldInst.getSchemClass().getName()))
            return "The class of the instance in the prior database is different";
        if (inst.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
            return getNodeForTwoStars(inst, oldInst, ReactomeJavaConstants.hasEvent);
        }
        if (inst.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
            String[] attNames = {ReactomeJavaConstants.input, 
                                 ReactomeJavaConstants.output,
                                 ReactomeJavaConstants.catalystActivity,
                                 ReactomeJavaConstants.regulatedBy};
            StringBuilder notes = new StringBuilder();
            for (String attName : attNames) {
                String note = getNodeForTwoStars(inst, oldInst, attName);
                if (note.startsWith("Same"))
                    continue;
                if (notes.length() > 0)
                    notes.append("; ");
                notes.append(note);
            }
            if (notes.length() == 0)
                notes.append("No structural update found");
            return notes.toString();
        }
        return null;
    }

    private String getNodeForTwoStars(GKInstance inst, 
                                      GKInstance oldInst,
                                      String attName) throws Exception {
        List<GKInstance> current = inst.getAttributeValuesList(attName);
        List<GKInstance> prior = oldInst.getAttributeValuesList(attName);
        List<Long> currentIDs = current.stream().map(i -> i.getDBID()).collect(Collectors.toList());
        List<Long> priorIDs = prior.stream().map(i -> i.getDBID()).collect(Collectors.toList());
        if (currentIDs.size() > priorIDs.size()) {
            priorIDs.removeAll(currentIDs);
            if (priorIDs.size() == 0)
                return "Added " + attName;
            else
                return "Added/Removed " + attName;
        }
        else if (currentIDs.size() == priorIDs.size()) {
            priorIDs.removeAll(currentIDs);
            if (priorIDs.size() == 0)
                return "Same " + attName;
            else
                return "Added/Removed " + attName;
        }
        else {
            currentIDs.removeAll(priorIDs);
            if (currentIDs.size() == 0)
                return "Removed " + attName;
            else
                return "Added/Removed " + attName;
        }
    }
    
    private String[] createQALine(String issue, GKInstance inst)  throws Exception {
        return createQALine(issue, null, inst);
    }
    
    private String[] createQALine(String issue, 
                                  String note,
                                  GKInstance inst)  throws Exception {
        String[] line = new String[5];
        line[0] = inst.getDBID() + "";
        line[1] = inst.getDisplayName();
        line[2] = issue;
        line[3] = QACheckUtilities.getLatestCuratorIEFromInstance(inst) + "";
        // Add some note if needed
        line[4] = note == null ? "" : note;
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
