package org.reactome.release.qa.graph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.release.qa.annotations.SliceQACheck;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckUtilities;
import org.reactome.release.qa.common.QAReport;

@SliceQACheck
public class UniquenessCheck extends AbstractQACheck { 
    private static Logger logger = Logger.getLogger(UniquenessCheck.class);
    
    public UniquenessCheck() {
    }
    
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        Map<String, List<String>> clsToAttributes = loadConfiguration();
        if (clsToAttributes == null || clsToAttributes.size() == 0)
            return report; // Nothing to be checked
        // Will be sorted based on cls names
        List<String> clsList = clsToAttributes.keySet().stream().sorted().collect(Collectors.toList());
        for (String cls : clsList) {
            logger.info("Checking " + cls + "...");
            List<String> attributes = clsToAttributes.get(cls);
            for (String attNm: attributes) {
                executeQACheck(cls, attNm, report);
            }
        }
        
        report.setColumnHeaders("DBID",
                "DisplayName",
                "Class",
                "Attribute",
                "Instance",
                "Duplicates",
                "Created",
                "Modified");
        
        return report;
    }
    
    private void executeQACheck(String clsName, String attName, QAReport report) throws Exception {
        @SuppressWarnings("unchecked")
        Collection<GKInstance> instances = dba.fetchInstancesByClass(clsName);
        dba.loadInstanceAttributeValues(instances, new String[] { attName });
        Map<Object, List<GKInstance>> valueToInstances =
                new HashMap<Object, List<GKInstance>>(instances.size());
        // First pass: group by value.
        for (GKInstance instance: instances) {
            if (isEscaped(instance)) {
                continue;
            }
            Object value = instance.getAttributeValue(attName);
            List<GKInstance> valInsts = valueToInstances.get(value);
            if (valInsts == null) {
                valInsts = new ArrayList<GKInstance>(1);
                valueToInstances.put(value, valInsts);
            }
            valInsts.add(instance);
        }
        // Second pass: report duplicates.
        for (Entry<Object, List<GKInstance>> valInstsEntry: valueToInstances.entrySet()) {
            List<GKInstance> valInsts = valInstsEntry.getValue();
            if (valInsts.size() > 1) {
                GKInstance latest = null;
                String latestModDateValue = null;
                for (GKInstance valInst: valInsts) {
                    GKInstance modified = QACheckUtilities.getLatestCuratorIEFromInstance(valInst);
                    String modDateValue =
                            (String) modified.getAttributeValue(ReactomeJavaConstants.dateTime);
                    if (modDateValue == null) {
                        continue;
                    }
                    if (latest == null || modDateValue.compareTo(latestModDateValue) < 0) {
                        latest = valInst;
                    }
                }
                final GKInstance instance = latest;
                String dupDbIds = valInsts.stream()
                        .filter(dup -> dup != instance)
                        .map(GKInstance::getDBID)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                GKInstance created = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.created);
                GKInstance modified = QACheckUtilities.getLatestCuratorIEFromInstance(instance);
                report.addLine(instance.getDBID().toString(),
                        instance.getDisplayName(),
                        instance.getSchemClass().getName(),
                        attName,
                        dupDbIds,
                        created.getDisplayName(),
                        modified.getDisplayName());
            }
        }
   }
    
    protected Map<String, List<String>> loadConfiguration() throws IOException {
        File file = getConfigurationFile();
        if (file == null)
            return null;
        try (Stream<String> lines = Files.lines(Paths.get(file.getAbsolutePath()))) {
            Map<String, List<String>> clsToAttributes = new HashMap<>();
            lines.filter(line -> !line.startsWith("#"))
                 .filter(line -> line.trim().length() > 0)
                 .map(line -> line.split("\t"))
                 .forEach(tokens -> {
                     List<String> attributes = Stream.of(tokens).skip(1).collect(Collectors.toList());
                     clsToAttributes.put(tokens[0], attributes);
                 });
            return clsToAttributes;
        }
    }

    @Override
    public String getDisplayName() {
        return "Values_Not_Unique";
    }

}
