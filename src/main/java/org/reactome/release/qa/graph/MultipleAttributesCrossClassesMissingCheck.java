package org.reactome.release.qa.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.reactome.release.qa.annotations.GraphQATest;
import org.reactome.release.qa.common.AbstractQACheck;
import org.reactome.release.qa.common.QACheckerHelper;
import org.reactome.release.qa.common.QAReport;

/**
 * This class is used to check several attributes across two or more classes to make sure
 * at least one attribute has a non-null value. For example, a ReactionlikeEvent instance
 * should have a value in inferredFrom, literatureReference, and its Summation's
 * literartureReference. The check is configured by MultipleAttributesCrossClassesMissingCheck.txt
 * in the resources folder.
 * @author wug
 *
 */
@GraphQATest
public class MultipleAttributesCrossClassesMissingCheck extends AbstractQACheck { 
    private static Logger logger = Logger.getLogger(MultipleAttributesCrossClassesMissingCheck.class);
    
    public MultipleAttributesCrossClassesMissingCheck() {
    }
    
    @Override
    public QAReport executeQACheck() throws Exception {
        QAReport report = new QAReport();
        List<CheckConfiguration> configurations = loadConfiguration();
        if (configurations == null || configurations.size() == 0)
            return report; // Nothing to be checked
        // Will be sorted based on cls names
        Collections.sort(configurations, (c1, c2) -> c1.clsName.compareTo(c2.clsName));        
        for (CheckConfiguration config : configurations) {
            logger.info("Check " + config.clsName + " for " + config.toString() + "...");
            executeQACheck(config, report);
        }
        report.setColumnHeaders("DB_ID",
                "DisplayName",
                "Class",
                "Attributes",
                "MostRecentAuthor");
        
        return report;
    }
    
    @SuppressWarnings("unchecked")
    private void executeQACheck(CheckConfiguration config, QAReport report) throws Exception {
        Collection<GKInstance> instances = dba.fetchInstancesByClass(config.clsName);
        for (String attName : config.attributes)
            dba.loadInstanceAttributeValues(instances, new String[]{attName});
        boolean isGood = false;
        for (GKInstance instance : instances) {
            if (isEscaped(instance)) {
                continue;
            }
            isGood = false;
            for (String attName : config.attributes) {
                Object value = instance.getAttributeValue(attName);
                if (value != null) {
                    isGood = true;
                    break;
                }
            }
            if (isGood)
                continue; // Check next
            // Need to check refer attributes
            for (String refer : config.referToAttributes.keySet()) {
                // It should be instance
                List<GKInstance> values = instance.getAttributeValuesList(refer);
                if (values == null || values.size() == 0) {
                    continue;
                }
                for (GKInstance value : values) {
                    for (String refAtt : config.referToAttributes.get(refer)) {
                        Object refAttValue = value.getAttributeValue(refAtt);
                        if (refAttValue != null) {
                            isGood = true;
                            break;
                        }
                    }
                }
                // This is a less stringent check: as long as there is a value in a referred instance, it is fine.
                if (isGood) 
                    break;
            }
            if (!isGood)
                report.addLine(instance.getDBID() + "",
                               instance.getDisplayName(),
                               instance.getSchemClass().getName(),
                               config.toString(),
                               QACheckerHelper.getLastModificationAuthor(instance));
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<CheckConfiguration> loadConfiguration() throws Exception {
        String fileName = "resources" + File.separator + getClass().getSimpleName() + ".xml";
        File file = new File(fileName);
        if (!file.exists()) {
            logger.warn("This is no configuration file available for " + 
                         getClass().getSimpleName() + 
                        ": " + file.getAbsolutePath());
            return null;
        }
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(file);
        List<CheckConfiguration> configs = new ArrayList<>();
        List<Element> clsElms = document.getRootElement().getChildren("class");
        for (Element clsElm : clsElms) {
            CheckConfiguration config = new CheckConfiguration();
            String clsName = clsElm.getAttributeValue("name");
            config.clsName = clsName;
            List<Element> attributeElms = clsElm.getChildren("attribute");
            List<String> attributes = new ArrayList<>();
            for (Element attributeElm : attributeElms) {
                String name = attributeElm.getAttributeValue("name");
                attributes.add(name);
            }
            config.attributes = attributes;
            Map<String, List<String>> referToAttributes = new HashMap<>();
            List<Element> referElms = clsElm.getChildren("refer");
            for (Element referElm : referElms) {
                String name = referElm.getAttributeValue("name");
                List<Element> referAttElms = referElm.getChildren("attribute");
                for (Element referAttElm : referAttElms) {
                    String attName = referAttElm.getAttributeValue("name");
                    referToAttributes.compute(name, (key, list) -> {
                        if (list == null)
                            list = new ArrayList<>();
                        list.add(attName);
                        return list;
                    });
                }
            }
            config.referToAttributes = referToAttributes;
            configs.add(config);
        }
        return configs;
    }

    @Override
    public String getDisplayName() {
        return "Multiple_Attributes_Cross_Classes_Missing_Simultaneously";
    }
    
    private class CheckConfiguration {
        String clsName;
        List<String> attributes;
        Map<String, List<String>> referToAttributes;
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.join(",", attributes));
            builder.append(",");
            referToAttributes.forEach((refer, attributes) -> {
                builder.append(refer).append(":");
                builder.append(String.join(",", attributes));
            });
            return builder.toString();
        }
    }

}
