package org.reactome.release.qa;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;
import org.reactome.release.qa.check.ChecksTwoDatabases;
import org.reactome.release.qa.common.MySQLAdaptorManager;
import org.reactome.release.qa.common.QACheck;
import org.reactome.release.qa.common.QAReport;
import org.reflections.Reflections;
	 
/**
 * The entry point to run all tests.
 * @author wug
 *
 */
public class Main {

    private static final Logger logger = LogManager.getLogger();
    
    private static final String QA_PROP_FILE = "resources/qa.properties";

    private static final String CHECKS_OPT = "checks";
    
    public static void main(String[] args) throws Exception {
        // Parse command line arguments.
        Map<String, Object> cmdOpts = parseCommandArguments(args);
        File output = prepareOutput();
        // Make the SQL adapter.
        MySQLAdaptorManager manager = MySQLAdaptorManager.getManager(cmdOpts);
        MySQLAdaptor dba = manager.getDBA();
        MySQLAdaptor altDBA = null;
        
        // Get the list of QAs from packages
        Reflections reflections = new Reflections("org.reactome.release.qa.check",
                                                  "org.reactome.release.qa.graph");

        // The QA checks to run must have at least one annotation and
        // be instantiable. Every runnable QA check must have an annotation
        // that can be used as an inclusion critieria.
        Set<Class<? extends QACheck>> allQAClasses = reflections.getSubTypesOf(QACheck.class);
        Set<Class<? extends QACheck>> instantiable = allQAClasses.stream()
                .filter(cls -> cls.getAnnotations().length > 0 &&
                               !Modifier.isAbstract(cls.getModifiers()) &&
                               !cls.isInterface())
                .collect(Collectors.toSet());

//        // Deployment aid:
//        // Uncomment and run to print the class display names.
//        for (Class<? extends QACheck> cls: instantiable) {
//            System.out.println(cls.getSimpleName() + "," + cls.newInstance().getDisplayName());
//        }
//        System.exit(0);
        
        // The properties file.
        Properties qaProps = new Properties();
        File qaPropsFile = new File(QA_PROP_FILE);
        if (qaPropsFile.exists()) {
            qaProps.load(new FileInputStream(qaPropsFile));
        }
        // The instance escape cut-off date.
        String cutoffDateStr = qaProps.getProperty("cutoffDate");
        Date cutoffDate = null;
        if (cutoffDateStr != null) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            cutoffDate = df.parse(cutoffDateStr);
            logger.info("Skip list cut-off date: " + cutoffDate);
        }
        
        // The optional QA checks to include.
        final Set<String> includes;
        // The optional QA checks to exclude.
        final Set<String> excludes;
        // The QA checks to run.
        Set<Class<? extends QACheck>> selected;
        
        // If checks were specified on the command line, then filter
        // for those checks. Otherwise, check the configuration.
        // The command line checks take precedence over the
        // configuration.
       @SuppressWarnings("unchecked")
        Set<String> cmdIncludes = (Set<String>) cmdOpts.get(CHECKS_OPT);
        if (cmdIncludes != null && !cmdIncludes.isEmpty()) {
            selected = instantiable.stream()
                    .filter(check -> cmdIncludes.contains(check.getSimpleName()))
                    .collect(Collectors.toSet());
        } else {
            // The optional QA checks to include.
            includes = getIncludedQAs();
            // The optional QA checks to exclude.
            excludes = getExcludedQAs();
            // Excludes take precedence.
            includes.removeAll(excludes);
            if (!includes.isEmpty()) {
                logger.info("Included QA check types: " + includes);        
            }
            if (!excludes.isEmpty()) {
                logger.info("Excluded QA check types: " + excludes);        
            }
            selected = instantiable.stream()
                        .filter(check -> isPicked(check, includes, excludes))
                        .collect(Collectors.toSet());
        }
        
        // Run the QA checks.
        for (Class<? extends QACheck> cls : selected) {
            QACheck check;
            try {
                check = cls.newInstance();
            } catch (InstantiationException e) {
                // Instantiation errors are remarkably uninformative.
                logger.error("Could not instantiate " + cls.getName());
                throw e;
            }
            logger.info("Perform " + check.getDisplayName() + "...");
            check.setMySQLAdaptor(dba);
            // Some checks compare two databases to each other
            // (usually test_reactome_## and test_reactome_##-1).
            // These checks require the alternate database authorization
            // properties.
            if (check instanceof ChecksTwoDatabases) {
                if (altDBA == null) {
                    // If the adaptor throws an exception, it will be passed
                    // up the call stack to the top level to stop process
                    // execution.
                    altDBA = MySQLAdaptorManager.getManager().getAlternateDBA();
                }
                ((ChecksTwoDatabases)check).setOtherDBAdaptor(altDBA);
            }
            // Set the common skip list cut-off date.
            check.setCutoffDate(cutoffDate);

            QAReport qaReport = check.executeQACheck();
            if (qaReport.isEmpty()) {
                logger.info("Nothing to report!");
                continue;
            }
            else {
                String fileName = check.getFileName();
                qaReport.output(fileName, output.getAbsolutePath());
                logger.info("Check "+ output.getAbsolutePath() + "/" + fileName + " for report details.");
            }
        }
    }

    private static Set<String> getIncludedQAs() throws IOException {
        File qaSkipListfile = new File("resources/IncludedChecks.txt");
        return loadQACheckList(qaSkipListfile);
    }

    private static Set<String> getExcludedQAs() throws IOException {
        File qaSkipListfile = new File("resources/ExcludedChecks.txt");
        return loadQACheckList(qaSkipListfile);
    }

    protected static Set<String> loadQACheckList(File qaSkipListfile) throws IOException {
        if (qaSkipListfile.exists()) {
            Path path = Paths.get(qaSkipListfile.getPath());
            return Files.lines(path)
                    .filter(line -> !line.isEmpty() && line.charAt(0) != '#')
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
    
    /**
     * Determines whether the given QA class should be run. A QA
     * class is run if and only if both of the following conditions
     * hold:
     * <ul>
     * <li>the includes is either empty or the includes contains
     *     the QA class or any of its annotations</li>
     * <li>the excludes does not contain the class or any of its
     *     annotations</li>
     * </ul>
     * 
     * @param check the QA check to examine
     * @param includes the QA check class and annotation names to include 
     * @param excludes the excluded QA check class and annotation names
     * @return whether the check should be run
     */
    private static boolean isPicked(Class<? extends QACheck> check,
            Set<String> includes, Set<String> excludes) {
        // Check the class.
        String checkName = check.getSimpleName();
        if (excludes.contains(checkName)) {
            return false;
        }
        if (includes.contains(checkName)) {
            return true;
        }
        // Check the annotations.
        Annotation[] annotations = check.getAnnotations();
        for (Annotation annotation : annotations) {
            String annName = annotation.annotationType().getSimpleName();
            if (excludes.contains(annName)) {
                return false;
            } else if (includes.contains(annName)) {
                return true;
            }
        }
        // Not included or excluded.
        // If there are includes, then only those are picked.
        return includes.isEmpty();
    }

    private static File prepareOutput() throws IOException {
        String output = "output";
        File file = new File(output);
        if (file.exists()) {
            GKApplicationUtilities.delete(file);
        }
        file.mkdir();
        return file;
    }

    /**
     * A simple roll-your-own command line parser.
     * Arguments are parsed as follows:
     * 
     * Arguments which follow a {@code --} option, e.g.
     * {@code -- FailedReactionChecker DatabaseObjectsWithSelfLoops},
     * or without a preceding option are added to the
     * {@code checks} option list value.
     * 
     * If the argument starts with {@code --}, then it is recognized
     * as an option. A following argument is processed as follows:
     * 
     * If there is no following argument or the following argument
     * is also an option, then the preceding option is assigned the
     * boolean value {code true}.
     * Otherwise, the preceding option is assigned the following
     * argument, e.g. {@code --dbName test_slice_99} is captured as
     * option {@code dbName} with value {@code test_slice_99}.
     * 
     * @param args the command line arguments
     * @return the {option: value} map
     * @throws IllegalArgumentException if an option other than
     *   {@code --notify} is followed by another option
     */
    private static Map<String, Object> parseCommandArguments(String[] args) {
        Map<String, Object> cmdOpts = new HashMap<String, Object>();
        String option = null;
        Set<String> checks = new HashSet<String>();
        for (String arg: args) {
            if (CHECKS_OPT.equals(option)) {
                checks.add(arg);
            } else if (arg.startsWith("--")) {
                if (option != null) {
                    // The option is assumed to be a flag.
                    cmdOpts.put(option, Boolean.TRUE);
                }
                if ("--".equals(arg)) {
                    option = CHECKS_OPT;
                } else {
                    option = arg.substring(2);
                }
            } else if (option == null) {
                checks.add(arg);
            } else {
                cmdOpts.put(option, arg);
                option = null;
            }
        }
        // A final option without an argument has value true.
        if (option != null && !CHECKS_OPT.equals(option)) {
            cmdOpts.put(option, Boolean.TRUE);
        }
        // If there are checks, then add the checks option.
        if (!checks.isEmpty()) {
            cmdOpts.put(CHECKS_OPT, checks);
        }
        
        return cmdOpts;
    }
  
}
