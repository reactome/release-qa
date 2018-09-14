package org.reactome.release.qa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactome.release.qa.common.QAReport;
	 
/**
 * The entry point to run all tests.
 * @author wug
 *
 */
public class Notify {
    
    private static final String NL = System.getProperty("line.separator");

    private static final String EMAIL_LOOKUP_FILE = "curators.csv";
    
    // The release coordinators.
    private static final Collection<String> COORDINATOR_NAMES =
            new HashSet<String>(1);
    private static final Collection<String> COORDINATOR_EMAILS =
            new HashSet<String>(1);
    
    private static final String COORDINATOR_PRELUDE =
            "The weekly QA checks issued the following QA reports:";
    
    private static final String NONCOORDINATOR_PRELUDE =
            "You are listed as the most recent author in the following weekly QA reports:";

    private static final String LAST_AUTHOR_HEADER = "MostRecentAuthor";
    
    private static final String MAIL_HOST = "localhost"; //"smtp.oicr.ca.on";
    
    private static final int MAIL_PORT = 8025; //25;
    
    private static final String MAIL_FROM = "donotreply@reactome.org";
    
    private static final Map<String, String> RPT_DIR_DESCRIPTIONS =
            new HashMap<String, String>();
    
    static {
        RPT_DIR_DESCRIPTIONS.put("CuratorQA", "gk_central");
        RPT_DIR_DESCRIPTIONS.put("SliceQA", "Trial slice");
    }
    
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws Exception {
        // Parse command line arguments.
        if (args.length == 0) {
            System.err.println("Missing the reports directory command argument.");
            System.exit(1);
        }
        if (args.length > 1) {
            String extraneous =
                    String.join(", ", Arrays.asList(args).subList(1, args.length));
            System.err.println("Extraneous arguments: " + extraneous);
            System.exit(1);
        }
        String rptsDirArg = args[0];
        // The {curator:email} lookup map.
        Map<String, String> emailLookup = null;
        try {
            emailLookup = getCuratorEmailLookup();
        } catch (Exception e) {
            System.err.println("Could not read the curator email file: ");
            System.err.println(e);
            System.exit(1);
        }
        // The {author: reports} map.
        Map<String, Collection<File>> notifications =
                new HashMap<String, Collection<File>>();
        // The QA reports directory.
        File rptsDir = new File(rptsDirArg);
        if (!rptsDir.exists()) {
            System.err.println("Reports directory not found: " + rptsDir);
            System.exit(1);
        }
        // Iterator over each reports subdirectory.
        for (File dir : rptsDir.listFiles()) {
            for (File file: dir.listFiles()) {
                addNotifications(file, notifications);
            }
        }
        // Notify the modifiers.
        sendNotifications(notifications, emailLookup, rptsDir);
    }

    private static Map<String, String> getCuratorEmailLookup() throws IOException {
        File file = new File("resources" + File.separator + EMAIL_LOOKUP_FILE);
        InputStream is;
        if (file.exists()) {
             is = new FileInputStream(file);
        } else {
            is = Notify.class.getResourceAsStream(EMAIL_LOOKUP_FILE);
            if (is == null) {
                String msg = "The curator email lookup configuration was not found: " + file;
                throw new FileNotFoundException(msg);
            }
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        Map<String, String> map = new HashMap<String, String>();
        String line;
        boolean isFirstLine = true;
        while ((line = br.readLine()) != null) {
            // The first line is a header.
            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }
            String[] content = line.split(",");
            // Reports sometimes, but not always, list the author as
            // last name, first initial. Compensate for this by
            // truncating the first name and always matching on
            // the truncated form.
            String name = content[1] + "," + content[2].charAt(0);
            String email = content[3];
            map.put(name, email);
            // The first column is the coordinator flag.
            boolean isCoordinator = Boolean.parseBoolean(content[0]);
            if (isCoordinator) {
                COORDINATOR_NAMES.add(name);
                COORDINATOR_EMAILS.add(email);
            }
        }
        br.close();
        is.close();
        
        return map;
   }

    private static QAReport getQAReport(File file) throws IOException {
        InputStream is;
        is = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        boolean isFirstLine = true;
        QAReport report = new QAReport();
        while ((line = br.readLine()) != null) {
            String[] content = line.split("\t");
            // The first line is a header.
            if (isFirstLine) {
                isFirstLine = false;
                report.setColumnHeaders(content);
            } else {
                report.addLine(content);
            }
        }
        br.close();
        is.close();
        
        return report;
   }

   private static void addNotifications(File file,
           Map<String, Collection<File>> notifications) throws IOException {
       QAReport report = getQAReport(file);
       List<String> headers = report.getHeaders();
        int authorNdx = headers.indexOf(LAST_AUTHOR_HEADER);
        if (authorNdx == -1) {
            return;
        }
        // Coordinators always receive notification.
        Set<String> authors = new HashSet<String>(COORDINATOR_NAMES);
        for (List<String> line: report.getReportLines()) {
            if (authorNdx >= line.size()) {
                continue;
            }
            String reportedAuthor = line.get(authorNdx);
            if (reportedAuthor == null) {
                continue;
            }
            // Convert the author string on the report to the
            // standardized last,initial format for matching
            // against the author email map.
            String[] authorFields = reportedAuthor.split(", *");
            if (authorFields.length > 1) {
                String last = authorFields[0];
                String firstOrInitial = authorFields[1];
                Character initial = firstOrInitial.charAt(0);
                String author = last + "," + initial;
                authors.add(author);
             }
        }
        for (String author: authors) {
            Collection<File> reports = notifications.get(author);
            if (reports == null) {
                reports = new ArrayList<File>();
                notifications.put(author, reports);
            }
            reports.add(file);
        }
    }

    private static void sendNotifications(Map<String, Collection<File>> notifications,
            Map<String, String> emailLookup, File output)
            throws Exception {
        String host = InetAddress.getLocalHost().getHostName();
        String url = "https://" + host + "/QAReports/" + output.getName();
        for (Entry<String, Collection<File>> notification: notifications.entrySet()) {
            String author = notification.getKey();
            String recipient = emailLookup.get(author);
            if (recipient != null) {
                notify(recipient, url, notification.getValue());
            }
        }
    }
    
    private static void notify(String recipient, String url, Collection<File> reports)
            throws Exception {
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", MAIL_HOST);
        properties.setProperty("mail.smtp.port", Integer.toString(MAIL_PORT));
        Session session = Session.getDefaultInstance(properties);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(MAIL_FROM));
        
        message.setSubject("Reactome Weekly QA");
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        StringBuffer sb = new StringBuffer();
        if (COORDINATOR_EMAILS.contains(recipient)) {
            sb.append(COORDINATOR_PRELUDE + NL);
        } else {
            sb.append(NONCOORDINATOR_PRELUDE + NL);
        }
        Function<File, String> classifier = f -> f.getParentFile().getName();
        Map<String, List<File>> groups =reports.stream()
                .collect(Collectors.groupingBy(classifier));
        for (Entry<String, List<File>> entry: groups.entrySet()) {
            String dir = entry.getKey();
            String dirDesc = RPT_DIR_DESCRIPTIONS.getOrDefault(dir, dir);
            sb.append(NL + dirDesc + " QA checks:"+ NL + NL);
            for (File file: entry.getValue()) {
                sb.append(url + dir + "/" + file.getName() + NL);
            }
        }
        message.setText(sb.toString());
        message.addRecipients(Message.RecipientType.TO, recipient);
        Transport.send(message);
        logger.info("Sent notification to " + recipient);
   }
   
}
