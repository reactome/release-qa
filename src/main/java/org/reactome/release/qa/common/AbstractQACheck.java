package org.reactome.release.qa.common;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.FileUtilities;

public abstract class AbstractQACheck implements QACheck {

    private final static Logger logger = Logger.getLogger(AbstractQACheck.class);

    private static final Pattern CHECK_SUFFIX_PAT = Pattern.compile("Check(er)?$");

    protected MySQLAdaptor dba;

    private Set<Long> escDbIds;

    private Date cutoffDate;
    
    @Override
    abstract public QAReport executeQACheck() throws Exception;
    
    @Override
    public void setMySQLAdaptor(MySQLAdaptor dba) {
        this.dba = dba;
    }
    
    @Override
    public void setCutoffDate(Date cutoffDate) {
        this.cutoffDate = cutoffDate;
    }

    protected File getConfigurationFile() {
        String fileName = "resources" + File.separator + getClass().getSimpleName() + ".txt";
        File file = new File(fileName);
        if (!file.exists()) {
            logger.warn("This is no configuration file available for " + 
                         getClass().getSimpleName() + 
                        ": " + file.getAbsolutePath());
            return null;
        }
        return file;
    }
    
    @Override
    /**
     * The default display name is the simple class name with
     * capitalized words delimited by underscore.
     */
    public String getDisplayName() {
        Matcher match = CHECK_SUFFIX_PAT.matcher(getClass().getSimpleName());
        String baseName = match.replaceAll("");
        List<String> words = splitCamelCase(baseName);
        return String.join("_", words);
    }
    
    /**
     * Determines whether the given instance should not be reported.
     * Returns true if both of the following conditions hold:
     * <ul>
     * <li>the instance DB id is in the escape list</li>
     * <li>there is no cut-off date or there is no instance most
     *     recent modification date or the cut-off date precedes
     *     the modification date</li>
     * </ul>
     * Otherwise, this method returns false.
     * 
     * @param instance the instance to check
     * @return whether to escape the instance
     * @throws Exception
     */
    protected boolean isEscaped(GKInstance instance) throws Exception {
        // Load the skip list on demand.
        if (escDbIds == null) {
            escDbIds = loadEscapedDbIds();
        }
        // First check: DB id is in the escape list.
        if (!escDbIds.contains(instance.getDBID())) {
            return false;
        }
        // Second check: if there is a cut-off date, then the instance
        // was most recently modified on or before the cut-off date.
        if (cutoffDate == null) {
            return true;
        }
        GKInstance ie = InstanceUtilities.getLatestIEFromInstance(instance);
        if (ie == null) {
            // Probably an error, but the error should be detected
            // elsewhere and the most reasonable interpretation here
            // is that the instance should not be escaped.
            return true;
        }
        String ieDateValue = (String) ie.getAttributeValue(ReactomeJavaConstants.dateTime);
        if (ieDateValue == null) {
            return true;
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        Date ieDate = df.parse(ieDateValue);
        return !ieDate.after(cutoffDate);
    }
    
    /**
     * Opens the file consisting of escaped instance DB ids.
     * @param class the QA class
     * @throws IOException
     */
    private Set<Long> loadEscapedDbIds() throws IOException {
        File file = new File("QA_SkipList" + File.separator + getDisplayName() + ".txt");
        HashSet<Long> escapedDbIds = new HashSet<Long>();
        if (file.exists()) {
            logger.info("Loading the " + getClass().getSimpleName() + " skip list " + file + "...");
            FileUtilities fu = new FileUtilities();
            fu.setInput(file.getAbsolutePath());
            String line = null;
            while ((line = fu.readLine()) != null) {
                if (line.startsWith("#"))
                    continue; // Escape comment line
                String[] tokens = line.split("\t");
                // Make sure only number will be got
                if (tokens[0].matches("\\d+")) {
                    Long dbId = new Long(tokens[0]);
                    escapedDbIds.add(dbId);
                }
            }
            fu.close();
        }
        
        return escapedDbIds;
    }
    
    /**
     * Converts a camelCase string into capitalized words, e.g.
     * <code>AbCDEfG</code> is converted to
     * <code>[Ab, CD, Ef, G]</code>.
     *
     * @param s the input camelCase string
     * @return the word array
     */
    private static List<String> splitCamelCase(String s) {
        String[] caps = s.split("(?=\\p{Upper})");
        // Combine single-letter splits.
        List<String> words = new ArrayList<String>();
        StringBuffer allCaps = new StringBuffer();
        for (String cap: caps) {
            if (cap.length() == 1) {
                // Build up the all caps word.
                allCaps.append(cap);
            } else {
                // Flush the concatenated all caps word to the list.
                if (allCaps.length() > 0) {
                    words.add(allCaps.toString());
                    allCaps.setLength(0);
                }
                // Add the current word.
                words.add(cap);
            }
        }
        // Check for a final all caps word.
        if (allCaps.length() > 0) {
            words.add(allCaps.toString());
        }

        return words;
    }

}
