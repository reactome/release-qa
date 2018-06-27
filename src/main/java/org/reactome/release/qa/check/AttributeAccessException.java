package org.reactome.release.qa.check;

import org.gk.model.Instance;
import org.gk.schema.SchemaClass;

public class AttributeAccessException extends PersistenceException {

    static final long serialVersionUID = -8394970808086258962L;

    private static String formatMessage(SchemaClass cls, String attName) {
        // cls should never be null, but play it safe.
        String clsName = cls == null ? "?" : cls.getName();
        return "Error accessing " + clsName + "." + attName;
    }

    private static String formatMessage(Instance instance, String attName) {
        // instance should never be null, but play it safe.
        Long dbId = instance == null ? null : instance.getDBID();
        return formatMessage(instance.getSchemClass(), attName) +
                " (DB_ID " + dbId + ")";
    }

    public AttributeAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public AttributeAccessException(SchemaClass cls, String attName, Throwable cause) {
        super(formatMessage(cls, attName), cause);
    }

    public AttributeAccessException(Instance instance, String attName, Throwable cause) {
        this(formatMessage(instance, attName), cause);
    }

}
