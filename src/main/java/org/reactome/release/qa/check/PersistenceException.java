package org.reactome.release.qa.check;

/**
 * Wraps a low-level checked data access exception.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
public class PersistenceException extends RuntimeException {

    static final long serialVersionUID = 2437132780920966487L;

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistenceException(Exception e) {
        this("Access error: " + e.getMessage(), e);
    }

}
