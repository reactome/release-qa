package org.reactome.release.qa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactome.release.qa.common.QAReport;
	 
/**
 * Notifies the responsible curators of weekly QA reports.
 * 
 * Note: notification requires two configuration files in the working directory:
 * <ul>
 * <li><code>curators.csv</code> - the list of potential curators</li>
 * <li><code>mail.properties</code> - the JavaMail properties</li>
 * </ul>
 * <p>
 * <code>curators.csv</code> has columns Coordinator, Surname, First Name
 * and Email. <em>Coordinator</em> is the release coordinator flag. Each
 * coordinator is notified of all QA reports. Non-coordinators are notified
 * of only those reports where they are listed as the last modifier.
 * </p>
 * <p>
 * <code>mail.properties</code> is the JavaMail properties. The following
 * properties are recommended:
 * <ul>
 * <li><code>mail.from</code> - the required mail sender
 * <li><code>mail.smtp.host</code> - the optional mail host name
 *     (default <code>localhost</code>)
 * </li>
 * <li><code>mail.smtp.port</code> - the optional mail port (default 25)</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * 
 * @author Fred Loney <loneyf@ohsu.edu>
 */
public class Notify {

    private static final String INSTANCE_BROWSER_URL = "cgi-bin/instancebrowser?DB=gk_central&ID=";

    private static final String NL = System.getProperty("line.separator");

    private static final String PROTOCOL = "http";

    private static final String EMAIL_LOOKUP_FILE = "curators.csv";
    
    private static final String MAIL_CONFIG_FILE = "mail.properties";
    
    // The release coordinators.
    private static final Collection<String> COORDINATOR_NAMES =
            new HashSet<String>(2);
    private static final Collection<String> COORDINATOR_EMAILS =
            new HashSet<String>(2);
    
    private static final String COORDINATOR_PRELUDE =
            "The weekly QA checks issued the following reports:";
    
    private static final String NONCOORDINATOR_PRELUDE =
            "You are listed as the most recent author in the following weekly QA reports:";

    private static final String[] AUTHOR_HEADERS = {
            "MostRecentAuthor",
            "LastAuthor"
    };
    
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
        
        // The mail properties.
        Properties props = loadProperties();
        
        // The {curator:email} lookup map.
        Map<String, String> emailLookup = null;
        try {
            emailLookup = getCuratorEmailLookup();
        } catch (Exception e) {
            System.err.println("Could not read the curator email file: ");
            System.err.println(e);
            System.exit(1);
        }
        
        // The {curator: {report file: html file}} map.
        Map<String, Map<File, File>> notifications = new HashMap<String, Map<File, File>>();
        // Coordinators always receive notification.
        for (String coordinator: COORDINATOR_NAMES) {
            notifications.put(coordinator, new HashMap<File, File>());
        }

        // The prefix to prepend to URLs.
        String host = InetAddress.getLocalHost().getCanonicalHostName();
        String hostPrefix = PROTOCOL + "://" + host;

        // The QA reports directory.
        File rptsDir = new File(rptsDirArg);
        if (!rptsDir.exists()) {
            System.err.println("Reports directory not found: " + rptsDir);
            System.exit(1);
        }
        // Iterator over each reports subdirectory.
        for (File dir : rptsDir.listFiles()) {
            for (File file: dir.listFiles()) {
                addNotifications(file, emailLookup.keySet(), hostPrefix, notifications);
            }
        }

        // Notify the modifiers.
        sendNotifications(notifications, hostPrefix, emailLookup, props, rptsDir);
    }
    
    private static Properties loadProperties() throws IOException {
        File file = new File("resources" + File.separator + MAIL_CONFIG_FILE);
        InputStream is;
        if (file.exists()) {
            is = new FileInputStream(file);
       } else {
            String msg = "The mail configuration file was not found: " + file;
            throw new FileNotFoundException(msg);
       }
        Properties properties = new Properties();
        properties.load(is);
        is.close();

        return properties;
    }

    private static Map<String, String> getCuratorEmailLookup() throws IOException {
        File file = new File("resources" + File.separator + EMAIL_LOOKUP_FILE);
        InputStream is;
        if (file.exists()) {
             is = new FileInputStream(file);
        } else {
            String msg = "The curator email lookup file was not found: " + file;
            throw new FileNotFoundException(msg);
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

   private static void addNotifications(File rptFile, Set<String> curators,
           String hostPrefix, Map<String, Map<File, File>> notifications) throws Exception {
       // The QA report.
       QAReport report = getQAReport(rptFile);
       // The column headers.
       List<String> headers = report.getHeaders();
       // The author headers begin with one of the author headers,
       // e.g. MostRecentAuthor_1 is an author header.
       List<Integer> authorIndexes = new ArrayList<Integer>();
       for (String hdr : AUTHOR_HEADERS) {
           int authorNdx = headers.indexOf(hdr);
           if (authorNdx != -1) {
               authorIndexes.add(authorNdx);
           }
       }
       // No author fields => nothing to do.
       if (authorIndexes.isEmpty()) {
           return;
       }
       
       if (!hostPrefix.endsWith("/")) {
           hostPrefix = hostPrefix + "/";
       }
       // The {curator: lines} map. 
       Map<String, List<String>> linesMap = new HashMap<String, List<String>>();
       // Coordinators are notified of every file.
       for (String coordinator: COORDINATOR_NAMES) {
           linesMap.put(coordinator, new ArrayList<String>());
       }
       // The DB_ID column index.
       int dbIdNdx = report.getHeaders().indexOf("DB_ID");
       // The DB ID link URL prefix.
       String instUrlPrefix = hostPrefix + INSTANCE_BROWSER_URL;
       // Apportion report lines to the curators.       
       for (List<String> line: report.getReportLines()) {
           Set<String> authors = new HashSet<String>();
           for (int authorNdx: authorIndexes) {
               if (authorNdx >= line.size()) {
                   continue;
               }
               String reportedAuthor = line.get(authorNdx);
               if (reportedAuthor != null) {
                   authors.add(reportedAuthor);
               }
           }

           // Convert the report line to HTML.
           String html = createHTMLTableRow(line, dbIdNdx, instUrlPrefix);

           // Coordinators get every line.
           for (String coordinator: COORDINATOR_NAMES) {
               List<String> lines = linesMap.get(coordinator);
               lines.add(html);
           }

           // Convert the author string on the report to the standardized
           // last,initial format for matching against the curators.
           for (String author: authors) {
               // The author field format pseudo-regex is:
               //   /last, first|initial(, date)?/
               String[] authorFields = author.split(", *");
               if (authorFields.length > 1) {
                   String last = authorFields[0];
                   String firstOrInitial = authorFields[1];
                   Character initial = firstOrInitial.charAt(0);
                   String canonicalAuthor = last + "," + initial;
                   if (curators.contains(canonicalAuthor)) {
                       List<String> lines = linesMap.get(canonicalAuthor);
                       if (lines == null) {
                           lines = new ArrayList<String>();
                           linesMap.put(canonicalAuthor, lines);
                       }
                       lines.add(html);
                   }
               }
           }
       }

       // The HTML table header.
       String header = createHTMLTableHeader(report);

       // Make the curator-specific HTML files.
       for (Entry<String, List<String>> entry: linesMap.entrySet()) {
           String curator = entry.getKey();
           List<String> lines = entry.getValue();
           File dir = rptFile.getParentFile();
           String fileName = rptFile.getName();
           // The report file base name before the extension.
           String prefix = fileName.split("\\.")[0];
           // Make the custom curator file name.
           String rptHdg = prefix.replace('_', ' ');
           String suffix = curator.replace(",", "").toLowerCase();
           String base = prefix + "_" + suffix;
           String curatorFileName = base + ".html";
           File curatorFile = new File(dir, curatorFileName);
           // Write the custom curator file.
           BufferedWriter bw = new BufferedWriter(new FileWriter(curatorFile));
           try {
               bw.write("<html>");
               bw.newLine();
               bw.write("<style>");
               bw.newLine();
               bw.write("table { border-collapse: collapse; }");
               bw.newLine();
               bw.write("table, th, td { border: 1px solid black; }");
               bw.newLine();
               bw.write("</style>");
               bw.write("<body>");
               bw.newLine();
               bw.write("<h1>");
               bw.write(rptHdg);
               bw.write("</h1>");
               bw.newLine();
               bw.write("<table>");
               bw.newLine();
               bw.write(" ");
               bw.write(header);
               bw.newLine();
               for (String line: lines) {
                   bw.write(" ");
                   bw.write(line);
                   bw.newLine();
               }
               bw.write("</table>");
               bw.newLine();
               bw.write("</body>");
               bw.write("</html>");
           } finally {
               bw.flush();  
               bw.close();  
           }
           // Add the custom curator file to the
           // {curator: {report file: curator file}} map.
           Map<File, File> curatorNtfs = notifications.get(curator);
           if (curatorNtfs == null) {
               curatorNtfs = new HashMap<File, File>();
               notifications.put(curator, curatorNtfs);
           }
           curatorNtfs.put(rptFile, curatorFile);
       }
    }

    private static String createHTMLTableHeader(QAReport report) {
        StringBuffer sb = new StringBuffer();
        sb.append("<tr>");
        for (String hdr: report.getHeaders()) {
            sb.append("<th>");
            sb.append(hdr);
            sb.append("</th>");
        }
        sb.append("</tr>");

        return sb.toString();
    }

    private static String createHTMLTableRow(List<String> line, int dbIdColNdx, String instUrlPrefix) {
        StringBuffer sb = new StringBuffer();
        sb.append("<tr>");
        for (int i = 0; i < line.size(); i++) {
            String col = line.get(i);
            sb.append("<td>");
            if (i == dbIdColNdx) {
                sb.append("<a href=");
                sb.append(instUrlPrefix);
                sb.append(col);
                sb.append(">");
                sb.append(col);
                sb.append("</a>");
            } else {
                sb.append(col);
            }
            sb.append("</td>");
        }
        sb.append("</tr>");
        sb.append(NL);

        return sb.toString();
    }

    private static void sendNotifications(Map<String, Map<File, File>> notifications,
            String hostPrefix, Map<String, String> emailLookup, Properties props, File rptsDir)
            throws Exception {
        if (!hostPrefix.endsWith("/")) {
            hostPrefix = hostPrefix + "/";
        }
        String dirUrl = hostPrefix + "QAReports/" + rptsDir.getName();
        for (Entry<String, Map<File, File>> ntf: notifications.entrySet()) {
            String author = ntf.getKey();
            String recipient = emailLookup.get(author);
            notify(recipient, dirUrl, props, ntf.getValue());
        }
    }
    
    private static void notify(String recipient, String dirUrl, Properties properties,
            Map<File, File> rptHtmlMap) throws Exception {
        Session session = Session.getDefaultInstance(properties);
        MimeMessage message = new MimeMessage(session);
        
        message.setSubject("Reactome Weekly QA");
        if (!dirUrl.endsWith("/")) {
            dirUrl = dirUrl + "/";
        }
        StringBuffer sb = new StringBuffer();
        if (COORDINATOR_EMAILS.contains(recipient)) {
            sb.append(COORDINATOR_PRELUDE + NL + NL);
        } else {
            sb.append(NONCOORDINATOR_PRELUDE + NL + NL);
        }
        Function<File, String> classifier = f -> f.getParentFile().getName();
        Map<String, List<File>> groups = rptHtmlMap.keySet().stream()
                .collect(Collectors.groupingBy(classifier));
        sb.append("<ul>");
        for (Entry<String, List<File>> entry: groups.entrySet()) {
            // The QA reports subdirectory.
            String dir = entry.getKey();
            // The report URL prefix.
            String prefix = dirUrl + dir + "/";
            // Format the link for each report.
            for (File rptFile: entry.getValue()) {
                String rptName = rptFile.getName().split("\\.")[0];
                File htmlFile = rptHtmlMap.get(rptFile);
                String htmlUrl = prefix + htmlFile.getName();
                String rptUrl = prefix + rptFile.getName();
                sb.append("<li>");
                sb.append("<a href='" + htmlUrl + "'>");
                sb.append(rptName);
                sb.append("</a>");
                // Coordinators get a link to the raw CSV file as well.
                if (COORDINATOR_EMAILS.contains(recipient)) {
                    sb.append(" (<a href='" + rptUrl + "'>");
                    sb.append("tsv</a>)");
                }
                sb.append("</li>");
                sb.append(NL);
            }
        }
        sb.append("</ul>");
        sb.append(NL);
        message.setContent(sb.toString(), "text/html");

        Address address = new InternetAddress(recipient);
        message.setRecipient(Message.RecipientType.TO, address);
        Transport.send(message);
        logger.info("Sent notification to " + recipient);
   }
   
}
