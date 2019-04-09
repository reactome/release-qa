package org.reactome.release.qa.graph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaAttribute;
import org.reactome.release.qa.annotations.GraphQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This class is used to check if two or more instances in the same class are duplicated.
 * The implementation of this check is based on the list of defined attributes listed
 * in the schema, and is different from the implementation from graph QA check in some
 * cases (e.g. EntitySet duplication).
 * 
 * Note: a skip list is supported but not recommended for this check.
 * 
 * @author wug
 */
@GraphQACheck
public class InstanceDuplicationCheck extends AbstractQACheck {
    private static Logger logger = Logger.getLogger(InstanceDuplicationCheck.class);

    public InstanceDuplicationCheck() {
    }

    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        List<String> classes = loadConfiguration();
        if (classes == null || classes.size() == 0)
            return report; // Nothing to be checked
        for (String cls : classes) {
            logger.info("Check " + cls + "...");
            executeQACheck(cls, report);
        }
        report.setColumnHeaders("Class",
                                "Duplicated_DBIDs",
                                "Duplicated_DisplayNames",
                                "MostRecentAuthor");
        
        return report;
    }
    
    @SuppressWarnings("unchecked")
    private void executeQACheck(String clsName, QAReport report) throws Exception {
        GKSchemaClass cls = (GKSchemaClass) dba.fetchSchema().getClassByName(clsName);
        // There are three types of defined attributes. In this check, NONE is not considered
        // ANY is treated as ALL. The following statement returns both ALL and ANY.
        // In most of ANY cases, there are only one value (all ANY is used for name in a variety
        // of places). 
        // Note: The following check may miss some duplications if multiple values
        // existing in ANY slot!
        Collection<SchemaAttribute> definedAttributes = cls.getDefiningAttributes();
        // Key instances by a string of defined attribute values to simple check
        Collection<GKInstance> instances = dba.fetchInstancesByClass(cls);
        dba.loadInstanceAttributeValues(instances, definedAttributes);
        Map<String, Set<GKInstance>> keyToInsts = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        for (GKInstance instance : instances) {
            if (isEscaped(instance)) {
                continue;
            }
            builder.setLength(0);
            // Since the check may be run again subclass, which may have different
            // defined attributes as the super class, we need to get the defined attributes
            // directly from instance
            GKSchemaClass instCls = (GKSchemaClass) instance.getSchemClass();
            Collection<SchemaAttribute> instDefinedAttributes = instCls.getDefiningAttributes();
            List<SchemaAttribute> sorted = instDefinedAttributes.stream()
                    .sorted((att1, att2) -> att1.getName().compareTo(att2.getName()))
                    .collect(Collectors.toList());
            for (SchemaAttribute att : sorted) {
                // att may be defined in the superclass and should not be used for query
                List<?> values = instance.getAttributeValuesList(att.getName());
                generateKeyFromValues(values, att, builder);
                builder.append("||");
            }
            keyToInsts.compute(builder.toString(), (key, set) -> {
                if (set == null)
                    set = new HashSet<>();
                set.add(instance);
                return set;
            });
        }
        // Check duplication
        for (Set<GKInstance> duplicates : keyToInsts.values()) {
            Collection<GKInstance> unescapedDups = new ArrayList<GKInstance>();
            for (GKInstance duplicate: duplicates) {
                if (!isEscaped(duplicate)) {
                    unescapedDups.add(duplicate);
                }
            }
            if (unescapedDups.size() < 2)
                continue;
            // Sort dups by db id.
            List<GKInstance> sortedDups = unescapedDups.stream()
                    .sorted((inst1, inst2) -> inst1.getDBID().compareTo(inst2.getDBID()))
                    .collect(Collectors.toList());
            // Create report
            String dbIds = sortedDups.stream()
                    .map(inst -> inst.getDBID() + "")
                    .collect(Collectors.joining("|"));
            String names = sortedDups.stream()
                    .map(inst -> inst.getDisplayName())
                    .collect(Collectors.joining("|"));
            String authors = sortedDups.stream()
                    .map(inst -> QACheckerHelper.getLastModificationAuthor(inst))
                    .collect(Collectors.joining("|"));
            report.addLine(clsName, dbIds, names, authors);
        }
    }
    
    private void generateKeyFromValues(List<?> values,
                                       SchemaAttribute att,
                                       StringBuilder builder) {
        if (att.isInstanceTypeAttribute()) {
            // Copy to avoid override the instance
            List<?> copy = new ArrayList<>(values);
            InstanceUtilities.sortInstances(copy);
            copy.forEach(inst -> builder.append(((GKInstance)inst).getDBID()).append("|"));
        }
        else {
            List<String> copy = new ArrayList<>();
            values.forEach(o -> copy.add(o.toString()));
            copy.stream().sorted().forEach(s -> builder.append(copy).append("|"));
        }
    }
    
    private List<String> loadConfiguration() throws IOException {
        File file = getConfigurationFile();
        if (file == null)
            return null;
        try (Stream<String> lines = Files.lines(Paths.get(file.getAbsolutePath()))) {
            List<String> names = lines.filter(line -> !line.startsWith("#"))
                                      .filter(line -> line.trim().length() > 0)
                                      .sorted()
                                      .collect(Collectors.toList());
            return names;
        }
    }
    
}
