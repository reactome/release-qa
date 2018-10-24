package org.reactome.release.qa.common;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

public abstract class AbstractQACheck implements QACheck {
    private final static Logger logger = Logger.getLogger(AbstractQACheck.class);

    protected MySQLAdaptor dba;

    private Set<Long> escDbIds;

    private Date cutoffDate;

    @Override
    public void setMySQLAdaptor(MySQLAdaptor dba) {
        this.dba = dba;
    }
    
    protected Collection<Long> getEscapedDbIds() {
        return this.escDbIds;
    }
    
    @Override
    public void setEscapedDbIds(Set<Long> escDbIds) {
        this.escDbIds = escDbIds;
    }
    
    @Override
    public void setCutoffDate(Date cutoffDate) {
        this.cutoffDate = cutoffDate;
    }

    public File getConfigurationFile() {
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
        // First check: DB id is in the escape list.
        if (escDbIds == null || !escDbIds.contains(instance.getDBID())) {
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
    
}
