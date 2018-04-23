package org.reactome.release.qa.check;

import org.gk.persistence.MySQLAdaptor;
import org.reactome.release.qa.common.QACheck;

public abstract class AbstractQACheck implements QACheck {

    protected MySQLAdaptor dba;

    @Override
    public void setMySQLAdaptor(MySQLAdaptor dba) {
        this.dba = dba;
    }
    
}
