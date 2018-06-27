package org.reactome.release.qa.check;

import static org.junit.Assert.assertEquals;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathwaylayout.Utils;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchema;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidClassException;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactome.release.qa.common.MySQLAdaptorManager;
import org.reactome.release.qa.common.QAReport;

/**
 * Class-based QA check that provides common instance access and
 * test functions.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
abstract public class ClassBasedQACheck extends AbstractQACheck {

    /**
     * This data structure corresponds to the SQL:
     * <code>
     * SELECT alias_or_table.attribute ...
     * FROM alias_or_table
     * </code>
     * where <em>alias_or_table</em> is the alias, if it exists,
     * otherwise the table.
     */
    public static class QueryAttribute {

        public String table;
        public String alias;
        public String attributes[];
    
        public QueryAttribute(String table, String alias, String... attributes) {
            this.table = table;
            this.alias = alias;
            this.attributes = attributes;
        }
         
        private Stream<String> qualify() {
            return Stream.of(attributes).map(att -> qualify(att));
        }
        
        private String qualify(String attribute) {
            return String.join(".", tableDesignator(), attribute);
        }

        private String tableDefinition() {
            return alias == null ? table : String.join(" ", table, alias);
        }

        private String tableDesignator() {
            return alias == null ? table : alias;
        }
        
    }
    
    protected class QueryResult {
        public Instance instance;
        public Object[] values;
        
        public QueryResult(Instance instance, Object[] values) {
            this.instance = instance;
            this.values = values;
        }
        
        public QueryResult(Map.Entry<Instance, Object[]> entry) {
            this(entry.getKey(), entry.getValue());
        }
        
        public QueryResult(Instance instance) {
            this(instance, null);
        }
    }
    
    private String clsName;

    // Testing cruft.
    protected List<Instance> fixture;
    private int preTestFailureCnt;

    protected ClassBasedQACheck(String clsName) {
        this.clsName = clsName;
    }

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    abstract public String getDescription();
    
    protected Stream<QueryResult> stream(Collection<Instance> instances) {
        return instances.stream().map(QueryResult::new);
    }
    
    protected Stream<QueryResult> stream(Map<Instance, Object[]> map) {
        return map.entrySet().stream().map(QueryResult::new);
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        for (QueryResult invalid: fetchInvalid()) {
            Instance instance = invalid.instance;
            report.addLine(instance.getDBID() + "",
                    instance.getDisplayName(),
                    instance.getSchemClass().getName(),
                    getIssue(invalid),
                    QACheckerHelper.getLastModificationAuthor((GKInstance)instance));
        }

        report.setColumnHeaders("DB_ID",
                                "DisplayName",
                                "Class",
                                "Issue",
                                "MostRecentAuthor");
        return report;
    }

    public String getSchemaClassName() {
        return this.clsName;
    }

    @Before
    /**
     * Sets up testing for this QA check.
     * 
     * Testing requires a file <code>test/resources/auth.properties</code>
     * with entries <code>host</code> (default <code>localhost</code>),
     * <code>database</code>, <code>user</code> and <code>password</code>.
     * 
     * @throws FileNotFoundException if the test database authorization
     *      file is missing
     */
    public void setUp() throws Exception {
        MySQLAdaptorManager manager = MySQLAdaptorManager.getManager();
        MySQLAdaptor dba = manager.getDBA();
        setMySQLAdaptor(dba);
        Collection<QueryResult> failures = fetchInvalid();
        preTestFailureCnt = failures.size();
        fixture = createTestFixture();
        dba.storeLocalInstances(fixture);
        dba.commit();
    }

    @Test
    /**
     * Verifies this QA check.
     * 
     * @see #setUp()
     */
    public void testCheck() {
        compareInvalidCountToExpected(fixture.size());
    }

    @After
    /**
     * Deletes all test fixture instances.
     * 
     * @throws Exception if there is a database access error
     */
    public void tearDown() throws Exception {
        if (fixture == null) {
            return;
        }
        for (Instance inst: fixture) {
            // Note: this is the one place where it is assumed
            // that the instance is a GKInstance.
            dba.deleteInstance((GKInstance) inst);
        }
        dba.commit();
    }

    /**
     * Verifies whether the test fixture introduces new QA check failures.
     * 
     * @param expectedCount the expected number of test fixture failures
     */
    protected void compareInvalidCountToExpected(int expectedCount) {
        Collection<QueryResult> failures = fetchInvalid();
        assertEquals(getDescription() + " count incorrect",
                     expectedCount,
                     failures.size() - preTestFailureCnt);
    }
    
    /**
     * Returns the issue with the given invalid instance.
     * 
     * @param instance
     * @param values
     * @return
     */
    protected abstract String getIssue(QueryResult result);

    /**
     * Collects instances which do not pass this QA test.
     * 
     * @return the invalid check failure instances.
     */
    abstract protected Collection<QueryResult> fetchInvalid();

    /**
     * Creates a fixture consisting of a new empty instance
     * of this check's schema class.
     */
    protected List<Instance> createTestFixture() {
        List<Instance> fixture = new ArrayList<Instance>();
        Instance inst = createInstance(getSchemaClassName());
        fixture.add(inst);
        return fixture;
    }

    /**
     * Creates an instance of the given schema class.
     * The instance {@link GKInstance#getDbAdaptor() persistence adaptor}
     * is set to this quality check's {@link #getDatasource() data source}.
     * The instance has no attributes besides the schema class and is not
     * yet saved.
     * 
     * @param clsName the {@link SchemaClass} name
     * @return the new instance
     */
    protected Instance createInstance(String clsName) {
        Schema schema = dba.getSchema();
        GKInstance instance = new GKInstance(schema.getClassByName(clsName));
        instance.setDbAdaptor(dba);
        return instance;
    }

    /**
     * Fetches the instance from the database.
     * 
     * @param clsName the {@link SchemaClass} name
     * @return the new instance
     */
    protected Instance fetch(String clsName, Long dbId) {
        try {
            return dba.getInstance(clsName, dbId);
        } catch (Exception e) {
            String message = "Error fetching " + clsName + " dbID " + dbId;
            throw new PersistenceException(message, e);
        }
    }
    
    /**
     * Returns the string representation of the given instance in
     * the form:
     * <code>
     * <displayName>(<schemaClass> dbId <id>)
     * </code>
     * 
     * @param instance
     * @return the formatted string
     */
    protected String format(Instance instance) {
        return instance.getDisplayName() + "(" +
                instance.getSchemClass().getName() + " dbId " + instance.getDBID() + ")";
    }

    @SuppressWarnings("unchecked")
    /**
     * Returns the instances for which the given attribute is null.
     * This QA check's data source must be set to an adapter which
     * supports the <code>IS NULL</code> operator.
     * 
     * @param clsName the search class simple name
     * @param attName the search attribute name
     * @return the instances which satisfy the criterion
     * @throws NullPointerException if the data source is not set
     * @throws AttributeAccessException if there is a data access error
     */
    protected Collection<Instance> fetchInstancesMissingAttribute(
            String clsName, String attName) {
        try {
            return dba.fetchInstanceByAttribute(
                    clsName, attName, "IS NULL", null);
        } catch (Exception e) {
            String message = "Error accessing " + clsName + "," + attName;
            throw new AttributeAccessException(message, e);
        }
    }

    /**
     * Returns the instances for which any of the given attributes
     * are null.
     * 
     * @see #fetchInstancesMissingAttribute(String, String) fetchInstancesMissingAttribute
     */
    protected Collection<Instance> fetchInstancesMissingAnyAttributes(
            String clsName, String ...attNames) {
        // Collect the instances into a set rather than a list
        // in order to avoid duplicate reporting.
        Set<Instance> missing = new HashSet<Instance>();
        for (String attName: attNames) {
            Collection<Instance> instances = fetchInstancesMissingAttribute(
                    clsName, attName);
            for (Instance inst: instances) {
                missing.add(inst);
            }
        }
        return missing;
    }

    /**
     * Returns the instances for which all of the given attributes
     * are null.
     * 
     * @see #fetchInstancesMissingAttribute(String, String) fetchInstancesMissingAttribute
     */
    protected Collection<Instance> fetchInstancesMissingAllAttributes(
            String clsName, String ...attNames) {
        // Map the id to the instance. Use a map rather than a list
        // in order to avoid duplicate reporting.
        List<Instance> missing = new ArrayList<Instance>();
        // Split the attributes into the first and the others.
        // The database query is performed on the first.
        // Each fetched instance is then examined for the remaining
        // attributes.
        String first = attNames[0];
        String[] rest = Arrays.copyOfRange(attNames, 1, attNames.length);
        Collection<Instance> instances = fetchInstancesMissingAttribute(
                    clsName, first);
        for (Instance inst: instances) {
            if (isMissingValues(inst, rest)) {
                missing.add(inst);
            }
        }
        return missing;
    }

    /**
     * Fetches instances of the given class which are not referenced
     * by the given attribute.
     * 
     * @see MySQLAdaptor#fetchUnreferencedInstances(String, SchemaAttribute)
     * @param className the origin class name
     * @param attName the origin attribute name
     * @return the target instances
     * @throws ClassCastException if the data source is not a {@link MySQLAdaptor}
     * @throws AttributeAccessException if there is a query failure
     */
    protected Collection<QueryResult> fetchUnreferenced(String className, String attName) {
        SchemaClass originCls =
                dba.getSchema().getClassByName(className);
        SchemaAttribute originAtt;
        try {
            originAtt = originCls.getAttribute(attName);
        } catch (InvalidAttributeException e) {
            throw new AttributeAccessException(originCls, attName, e);
        }
        try {
            return fetchUnreferencedInstances(getSchemaClassName(), originAtt);
        } catch (Exception e) {
            SchemaClass cls =
                    dba.getSchema().getClassByName(getSchemaClassName());
            throw new AttributeAccessException(cls, attName, e);
        }
    }

    /**
     * Fetches instances of the given class filtered by the given
     * subquery SQL.
     * 
     * @see MySQLAdaptor#fetchInstancesIn(String, String)
     * @param className the origin class name
     * @param subquery the SQL condition subquery
     * @return the target instances
     * @throws ClassCastException if the data source is not a {@link MySQLAdaptor}
     * @throws AttributeAccessException if there is a query failure
     */
    protected Collection<QueryResult> fetch(String condition, QueryAttribute... attributes) {
        try {
            String fetchSql = createFetchInstancesByClassSQL(
                    getSchemaClassName(), condition, attributes);
            return fetchInstances(fetchSql);
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    protected Set<GKInstance> getTopLevelPathways() {
        // Make a set, consistent with other instance fetch methods.
        Set<GKInstance> tles;
        try {
            tles = new HashSet<GKInstance>(Utils.getTopLevelPathways(dba));
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
        return tles;
    }

    /**
     * Gets the given instance attribute value.
     * 
     * @param inst the instance to access
     * @param attName the attribute to access
     * @return the access value
     * @throws AttributeAccessException if there is a data access error
     */
    protected Object getAttributeValue(Instance inst, String attName) {
        try {
            return inst.getAttributeValue(attName);
        } catch (Exception e) {
            throw new AttributeAccessException(inst, attName, e);
        }
    }

    /**
     * Sets the given instance attribute value.
     * 
     * @param inst the instance to access
     * @param attName the attribute to set
     * @param value the attribute value
     * @throws AttributeAccessException if there is a data access error
     */
    protected void setAttributeValue(Instance inst, String attName, Object value) {
        try {
            inst.setAttributeValue(attName, value);
        } catch (Exception e) {
            throw new AttributeAccessException(inst, attName, e);
        }
    }

    protected boolean isNotInferred(Instance rle) {
        return getAttributeValue(rle, ReactomeJavaConstants.inferredFrom) == null;
    }

    /**
     * Fetches instances of the given class which are not referenced
     * by the given attribute. For example,
     * <code>
     * SchemaClass originCls = dba.getSchema().getClassByName("Pathway");
     * SchemaAttribute originAtt = originCls.getAttribute("hasEvent");
     * fetchUnreferencedInstances("Event", originAtt)
     * </code>
     * fetches the <code>Event</code> instances which are not
     * referenced by a <code>Pathway</code> <code>hasEvent</code>
     * relationship.
     * 
     * @param className the target class name
     * @param attribute the origin reference attribute
     * @return the query result
     */
    private Collection<QueryResult> fetchUnreferencedInstances(
            String className, SchemaAttribute attribute) {
        String notInSql = createReverseNotInSQL(className, attribute);
        return fetch(notInSql);
    }
    
    private static String select(QueryAttribute... attributes) {
        return Stream.of(attributes)
                .flatMap(att -> att.qualify())
                .collect(Collectors.joining(", "));
    }
    
    private static String from(QueryAttribute... attributes) {
        return Stream.of(attributes)
                .map(att -> att.tableDefinition())
                .collect(Collectors.joining(", "));
    }

    /**
     * Determines whether all of the given instance attributes are null-valued.
     * 
     * @param inst the instance to check
     * @param attNames the attributes to check
     * @return whether all attribute values are null
     */
    private boolean isMissingValues(Instance inst, String[] attNames) {
        for (String attName: attNames) {
            if (getAttributeValue(inst, attName) != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds the SQL to fetch all records in the given class
     * table. It is recommended that the query be qualified by
     * a WHERE clause.
     * 
     * @param className the target class name
     * @return the SQL query string
     * @throws InvalidClassException if the given class is not found in the schema
     */
    private String createFetchInstancesByClassSQL(String className) throws Exception {
        return createFetchInstancesByClassSQL(className, null);
    }

    /**
     * Builds the SQL to fetch all records in the given class
     * table. It is recommended that the query be qualified by
     * a WHERE clause.
     * 
     * @param className the target class name
     * @param condition the optional additional <code>WHERE</code> subcondition
     * @param attributes the optional additional attributes
     * @return the SQL query string
     * @throws InvalidClassException if the given class is not found in the schema
     */
    private String createFetchInstancesByClassSQL(
            String className, String condition, QueryAttribute ...attributes)
                    throws Exception {
        // This code block augments the CuratorTool MySQLAdaptor
        // SQL builder.
        Schema schema = dba.fetchSchema();
        schema.isValidClassOrThrow(className);
        String rootClassName = ((GKSchema) schema).getRootClass().getName();
        String baseSelect = "SELECT " + rootClassName + "." + MySQLAdaptor.DB_ID_NAME + ", " +
                        rootClassName + "._class, " + rootClassName + "._displayName";
        String select = attributes.length == 0 ? baseSelect :
            String.join(", ", baseSelect, select(attributes));
        String baseFrom = "FROM " + rootClassName;
        if (!className.equals(rootClassName)) {
            baseFrom = baseFrom + ", " + className;
        }
        String from = attributes.length == 0 ? baseFrom :
            String.join(", ", baseFrom, from(attributes));
        String where = null;
        if (className.equals(rootClassName)) {
            if (condition != null) {
                where = "WHERE " + condition;
            }
        } else {
            String baseWhere =
                    "WHERE " + rootClassName + "." + MySQLAdaptor.DB_ID_NAME +
                    " = " + className + "." + MySQLAdaptor.DB_ID_NAME;
            where = condition == null ? baseWhere :
                String.join(" AND ",  baseWhere, condition);
        }
        String baseSql = String.join("\n", select, from);
        
        return where == null ? baseSql : String.join("\n", baseSql, where);
    }

    /**
     * Builds the SQL clause to fetch records which are not referenced by
     * the given attribute. For example,
     * <code>
     * SchemaClass originCls = dba.getSchema().getClassByName("Pathway");
     * SchemaAttribute originAtt = originCls.getAttribute("hasEvent");
     * createReverseNotInSQL("Event", originAtt)
     * </code>
     * returns:
     * <code>Event.DB_ID NOT IN (SELECT hasEvent FROM Pathway_2_hasEvent)</code>
     * 
     * @param className the target class name
     * @param attribute the origin attribute
     * @return the SQL string
     */
    private String createReverseNotInSQL(String className, SchemaAttribute attribute) {
        String attName = attribute.getName();
        String originName = attribute.getOrigin().getName();
        String intersectionName = new String(originName + "_2_" + attName);
        String subselect = "(SELECT " + attName + " FROM " + intersectionName + ")";
        return className + ".DB_ID NOT IN " + subselect;
    }

    /**
     * Fetches instances based on the given SQL query.
     * 
     * @param sql the query string
     * @return the result instances
     */
    private Collection<QueryResult> fetchInstances(String sql, QueryAttribute... attributes)
            throws SQLException, InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        // This code block is adapted from the CuratorTool MySQLAdaptor.
        PreparedStatement ps = dba.getConnection().prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        Map<Instance, Object[]> result = new HashMap<Instance, Object[]>();
        Long dbId = new Long(0);
        Instance instance = null;
        Object[] values = new Object[attributes.length];
        while (rs.next()) {
            long newID = rs.getLong(1);
            if (newID != dbId.longValue()) {
                dbId = new Long(newID);
                String clsName = rs.getString(2);
                instance = dba.getInstance(clsName, dbId);
            }
            if (instance != null) {
                String displayName = rs.getString(3);
                instance.setDisplayName(displayName);
                for (int i=0; i < attributes.length; i++) {
                     values[i] = rs.getObject(i);
                }
                result.put(instance, values);
            }
        }
        return result.entrySet().stream()
                .map(QueryResult::new)
                .collect(Collectors.toList());
    }

}
