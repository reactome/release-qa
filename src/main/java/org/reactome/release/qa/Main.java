package org.reactome.release.qa;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    
    private static final String CHECKS_OPT = "checks";

    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws Exception {
        // Parse command line arguments.
        Map<String, Object> cmdOpts = parseCommandArguments(args);
        File output = prepareOutput();
        // Make the SQL adapter.
        MySQLAdaptorManager manager = MySQLAdaptorManager.getManager(cmdOpts);
        MySQLAdaptor dba = manager.getDBA();
        MySQLAdaptor altDBA = null;
        
        Set<String> testTypes = getTestTypes();
        logger.info("Will execute tests of type: " + testTypes);
        
        // Get the list of QAs from packages
        Reflections reflections = new Reflections("org.reactome.release.qa.check",
                                                  "org.reactome.release.qa.graph");

        Set<Class<? extends QACheck>> releaseQAs = reflections.getSubTypesOf(QACheck.class)
                                                              .stream()
                                                              .filter(checker -> isPicked(checker, testTypes))
                                                              .collect(Collectors.toSet());
        
        // If checks were specified on the command line, then filter for those checks.
        @SuppressWarnings("unchecked")
        Collection<String> includes = (Collection<String>) cmdOpts.get(CHECKS_OPT);
        if (includes != null && !includes.isEmpty()) {
            releaseQAs = releaseQAs.stream()
                    .filter(check -> includes.contains(check.getSimpleName()))
                    .collect(Collectors.toSet());
        }
        
        // Omit checks in the check skip list, if necessary.
        File file = new File("resources/QASkipList.txt");
        if (file.exists()) {
            Set<String> skipList = Files.lines(Paths.get(file.getPath()))
                    .collect(Collectors.toSet());
            releaseQAs = releaseQAs.stream()
                    .filter(check -> !skipList.contains(check.getSimpleName()))
                    .collect(Collectors.toSet());
        }

        // Run the QA checks.
        for (Class<? extends QACheck> cls : releaseQAs) {
            QACheck check = cls.newInstance();
            logger.info("Perform " + check.getDisplayName() + "...");
            check.setMySQLAdaptor(dba);
            // Some checks might compare two databases to each other (usually test_reactome_## and test_reactome_##-1)
            // So far, this only happens with CompareSpeciesByClasses, but there could be other multi-database checks in the future.
            if (check instanceof ChecksTwoDatabases) {
                if (altDBA == null) {
                    // Let the exception thrown to the top level to stop the whole execution
                    altDBA = MySQLAdaptorManager.getManager().getAlternateDBA();
                }
                ((ChecksTwoDatabases)check).setOtherDBAdaptor(altDBA);
            }
            QAReport qaReport = check.executeQACheck();
            if (qaReport.isEmpty()) {
                logger.info("Nothing to report!");
                continue;
            }
            else {
                String fileName = check.getDisplayName();
                qaReport.output(fileName + ".tsv", output.getAbsolutePath());
                logger.info("Check "+ output.getAbsolutePath() + "/" + fileName + ".tsv for report details.");
            }
        }
    }
    
    private static boolean isPicked(Class<? extends QACheck> checker, Set<String> testTypes) {
        Annotation[] annotations = checker.getAnnotations();
        if (annotations == null)
            return false;
        for (Annotation annotation : annotations) {
            if (testTypes.contains(annotation.annotationType().getSimpleName()))
                return true;
        }
        return false;
    }
    
    /**
     * Get pre-configured test types from auth.properties.
     * @return
     * @throws IOException
     */
    private static Set<String> getTestTypes() throws IOException {
        Set<String> rtn = new HashSet<>();
        InputStream is = MySQLAdaptorManager.getManager().getAuthConfig();
        Properties prop = new Properties();
        prop.load(is);
        String testTypes = prop.getProperty("testTypes");
        if (testTypes == null) {
            rtn.add("SliceQATest");
            rtn.add("GraphQATest");
        }
        else {
            String[] tokens = testTypes.split(",");
            Stream.of(tokens).forEach(token -> rtn.add(token.trim()));
        }
        return rtn;
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
        List<String> checks = new ArrayList<String>();
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
