package org.reactome.release.qa.check.graph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.management.modelmbean.XMLParseException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;
import org.reactome.release.qa.common.MySQLAdaptorManager;
import org.reactome.release.qa.common.QACheck;
import org.reactome.release.qa.common.QAReport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The entry point to run the graph QA tests.
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
public class Runner {
    
    private static final String TYPE_ATT_NOT_FOUND =
            "QA Check %d is missing the type attribute";
    
    private static final String CLASS_ELT_NOT_FOUND =
            "QA Check %d %s class element not found";
    
    private static final String MULTIPLE_CLASS_ELTS =
            "QA Check %d %s multiple class elements is not supported";

    private static final String DB_OPT_VALUE_MISSING =
            "Database option value missing";
  
    private static MySQLAdaptor dba;
    
    private static class DBManager extends MySQLAdaptorManager {
        
        private String dbName;

        DBManager(String dbName) {
            this.dbName = dbName;
        }
        
        @Override
        protected Properties getAuthProperties() throws Exception {
            Properties prop = super.getAuthProperties();
            if (dbName != null) {
                prop.put("dbName", dbName);
            }
            return prop;
        }
    }

    public static void main(String[] args) throws Exception {
        int i = 0;
        // The required QA checks specification file.
        String checksFile = null;
        // The optional database name.
        String dbName = null;
        while (i < args.length) {
            if ("-d".equals(args[i]) || "--database".equals(args[i])) {
                i++;
                if (i == args.length) {
                    throw new IllegalArgumentException(DB_OPT_VALUE_MISSING);
                }
                checksFile = args[i];
            } else {
                dbName = args[i];
            }
            i++;
        }
        if (checksFile == null) {
            checksFile = "QAChecks.xml";
        }
        QACheck[] checks = parseChecksFile(checksFile);
        MySQLAdaptorManager manager = new DBManager(dbName);
        dba = manager.getDBA();
        Runner runner = new Runner(checks);
        runner.run();
    }
    
    private QACheck[] checks;
    
    public Runner(QACheck[] checks) {
        this.checks = checks;
    }

    public void run() throws Exception {
        File output = prepareOutput();
        for (QACheck check: checks) {
            check.setMySQLAdaptor(dba);
            QAReport report = check.executeQACheck();
            String fileName = check.getDisplayName();
            if (report.isEmpty()) {
                System.out.println(fileName + " had no issues.");
            } else {
                System.out.println(fileName + " had " +
                                   report.getReportLines().size() + " issues.");
                report.output(fileName + ".txt", output.getAbsolutePath());
            }
        }
    }
    
    private static QACheck[] parseChecksFile(String fileName) throws Exception {
        File file = new File(fileName);
        if (!file.exists())
            throw new IOException("QA Checks file does not exist: " + fileName);
        InputStream is = new FileInputStream(file);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(is);
        Element root = doc.getDocumentElement();
        NodeList checkNodes = root.getElementsByTagName("check");
        QACheck[] checks = new QACheck[checkNodes.getLength()];
        for (int i = 0; i < checkNodes.getLength(); i++) {
            Element checkElt = (Element) checkNodes.item(i);
            String type = checkElt.getAttribute("type");
            if (type == null || type.isEmpty()) {
                String message = String.format(TYPE_ATT_NOT_FOUND, i + 1);
                throw new XMLParseException(message);
            }
            NodeList clsNodes = checkElt.getElementsByTagName("class");
            if (clsNodes.getLength() == 0) {
                String message = String.format(CLASS_ELT_NOT_FOUND, i + 1, type);
                throw new XMLParseException(message);
            }
            if (clsNodes.getLength() > 1) {
                String message = String.format(MULTIPLE_CLASS_ELTS, i + 1, type);
                throw new XMLParseException(message);
            }
            String clsName = clsNodes.item(0).getTextContent();
            NodeList attNodes = root.getElementsByTagName("attribute");
            String[] atts = new String[attNodes.getLength()];
            for (int j = 0; j < attNodes.getLength(); j++) {
                atts[j] = attNodes.item(j).getTextContent();
            }
            QACheck check = QACheckFactory.create(type, clsName, atts);
            checks[i] = check;
        }
        is.close();
        
        return checks;
    }
    
    private File prepareOutput() throws IOException {
        String output = "output";
        File file = new File(output);
        if (file.exists()) {
            GKApplicationUtilities.delete(file);
        }
        file.mkdir();
        return file;
    }
    
}
