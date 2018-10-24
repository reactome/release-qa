package org.reactome.release.qa.graph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.reactome.release.qa.annotations.GraphQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

@GraphQATest
public class MultipleAttributesMissingCheck extends AbstractQACheck { 
    private static Logger logger = Logger.getLogger(MultipleAttributesMissingCheck.class);
    
    public MultipleAttributesMissingCheck() {
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
            executeQACheck(cls, attributes, report);
        }
        
        report.setColumnHeaders("DB_ID",
                "DisplayName",
                "Class",
                "Attributes",
                "MostRecentAuthor");
        
        return report;
    }
    
    private void executeQACheck(String clsName, List<String> attNames, QAReport report) throws Exception {
        Collection<GKInstance> instances = QACheckerHelper.getInstancesWithNullAttribute(dba,
                                                                                         clsName,
                                                                                         attNames.get(0),
                                                                                         null);
        for (GKInstance instance: instances) {
            if (isEscaped(instance)) {
                continue;
            }
            boolean isMissing = true;
            for (int i = 1; i < attNames.size(); i++) {
                String attName = attNames.get(i);
                if (instance.getSchemClass().isValidAttribute(attName)) {
                    if (instance.getAttributeValue(attName) != null) {
                        isMissing = false;
                        break;
                    }
                }
            }
            if (isMissing) {
                report.addLine(instance.getDBID() + "",
                               instance.getDisplayName(),
                               instance.getSchemClass().getName(),
                               String.join(",", attNames),
                               QACheckerHelper.getLastModificationAuthor(instance));
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
//                 .filter(tokens -> tokens.length > 2) // At least three tokens should be available: class and two attributes.
                 .forEach(tokens -> {
                     List<String> attributes = Stream.of(tokens).skip(1).collect(Collectors.toList());
                     clsToAttributes.put(tokens[0], attributes);
                 });
            return clsToAttributes;
        }
    }

    @Override
    public String getDisplayName() {
        return "Multiple_Attributes_Missing_Simultaneously";
    }

}
