package org.reactome.release.qa.graph;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.reactome.release.qa.annotations.GraphQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

@GraphQATest
public class DatabaseObjectsWithoutCreated extends AbstractQACheck {
    private final static Logger logger = Logger.getLogger(DatabaseObjectsWithoutCreated.class);

    /**
     * The classes which don't required a created slot.
     */
    private static final String[] OPTIONAL = {
            ReactomeJavaConstants.InstanceEdit,
            ReactomeJavaConstants.DatabaseIdentifier,
            ReactomeJavaConstants.Taxon,
            ReactomeJavaConstants.Person,
            ReactomeJavaConstants.ReferenceEntity
    };

    private static final List<String> HEADERS = Arrays.asList(
            "DBID", "DisplayName", "SchemaClass", "MostRecentAuthor");

    @Override
    public String getDisplayName() {
        return "DatabaseObject_Without_Created";
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        // The root class.
        Schema schema = dba.getSchema();
        SchemaClass root =
                schema.getClassByName(ReactomeJavaConstants.DatabaseObject);
        // The optional schema classes.
        List<SchemaClass> optional = Stream.of(OPTIONAL)
                .map(clsName -> schema.getClassByName(clsName))
                .collect(Collectors.toList());
        // All schema classes.
        @SuppressWarnings("unchecked")
        Collection<SchemaClass> classes = schema.getClasses();

        // Build the report.
        QAReport report = new QAReport();
        for (SchemaClass cls: classes) {
            if (cls.getSuperClasses().contains(root) &&
                    !optional.stream().anyMatch(optCls -> cls.isa(optCls))) {
                logger.info("Checking " + cls + "...");
                List<GKInstance> invalid = QACheckerHelper.getInstancesWithNullAttribute(dba,
                        cls.getName(), 
                        ReactomeJavaConstants.created, 
                        null);
                for (GKInstance instance: invalid) {
                    addReportLine(report, instance);
                }
            }
        }
        report.setColumnHeaders(HEADERS);

        return report;
    }

    private void addReportLine(QAReport report, GKInstance instance) {
        report.addLine(instance.getDBID().toString(), 
                       instance.getDisplayName(), 
                       instance.getSchemClass().getName(), 
                       QACheckerHelper.getLastModificationAuthor(instance));
    }
}
