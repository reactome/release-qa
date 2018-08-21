package org.reactome.release.qa.check.graph;

import java.util.function.Consumer;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

public interface Validator {
    
    public class Invalid {

        GKInstance instance;
        
        String issue;

        public Invalid(GKInstance instance, String issue) {
            this.instance = instance;
            this.issue = issue;
        }
        
    }

    String getName();

    void validate(MySQLAdaptor dba, Consumer<Invalid> consumer)
            throws Exception;

}
