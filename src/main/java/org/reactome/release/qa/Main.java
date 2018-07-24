package org.reactome.release.qa;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import java.util.stream.Collectors;

import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;
import org.reactome.release.qa.annotations.SliceQATest;
import org.reactome.release.qa.check.ChecksTwoDatabases;
import org.reactome.release.qa.common.MySQLAdaptorManager;
import org.reactome.release.qa.common.QACheck;
import org.reactome.release.qa.common.QAReport;
import org.reflections.Reflections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
	 
/**
 * The entry point to run all tests.
 * @author wug
 *
 */
public class Main {
    
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws Exception {
        File output = prepareOutput();
        // Make sure we have output
        MySQLAdaptorManager manager = MySQLAdaptorManager.getManager();
        MySQLAdaptor dba = manager.getDBA();
        
        String authFileName = File.separator + "auth.properties";
        // Try to get the configuration file first
        //File file = new File("resources" + authFileName);
        InputStream is = Main.class.getResourceAsStream(authFileName);
        Properties prop = new Properties();
        prop.load(is);
        
        // testType should come from auth.properties. For backwards compatibility reasons, this property defaults to SliceQATest.
        String testType = prop.getProperty("testType", SliceQATest.class.getSimpleName());
        logger.info("Will execute tests of type: " + testType);
        MySQLAdaptor altDBA = null;

        // Get the list of QAs from the package
        String packageName = "org.reactome.release.qa.check";
        Reflections reflections = new Reflections(packageName);

        // A predicate to filter to exclude Abstract classes, and anything annotated with @ReleaseQATest, since this Main is for Slice QA.
        //
        // For backwards compatibility, SliceQATests *will* run if there are no annotations and the test type is SliceQATest. This is because
        // the first QA classes did not have annotations applied to them, and they were ALL SliceQATests. This way, the original classes and
        // properties files don't need to be modified and this code should still work. In the future, this should be changed and ALL classes
        // should be annotated.
        Predicate<? super Class<? extends QACheck>> qaCheckClassFilter = c -> !Modifier.isAbstract(c.getModifiers())
                                                                              && (
                                                                                     (c.getAnnotations().length == 0
                                                                                       && testType.equals(SliceQATest.class.getSimpleName()))
                                                                                     || Arrays.stream(c.getAnnotations()).anyMatch(a -> a.annotationType().getSimpleName().equals(testType))
                                                                                 );

        Set<Class<? extends QACheck>> releaseQAs = reflections.getSubTypesOf(QACheck.class).stream()
                                                              .filter(qaCheckClassFilter)
                                                              .collect(Collectors.toSet());

        for (Class<? extends QACheck> cls : releaseQAs) {
            QACheck check = cls.newInstance();
            logger.info("Perform " + check.getDisplayName() + "...");
            check.setMySQLAdaptor(dba);
            // Some checks might compare two databases to each other (usually test_reactome_## and test_reactome_##-1)
            // So far, this only happens with CompareSpeciesByClasses, but there could be other multi-database checks in the future.
            if (check instanceof ChecksTwoDatabases)
            {
                if (altDBA == null)
                {
                    try
                    {
                        altDBA = MySQLAdaptorManager.getManager().getAlternateDBA();
                    }
                    catch (Exception e)
                    {
                    e.printStackTrace();
                    }
                }
                // make sure altDBA was initialized OK...
                if (altDBA != null)
                {
                    ((ChecksTwoDatabases)check).setOtherDBAdaptor(altDBA);
                }
                // ...print a message if it wasn't.
                else
                {
                	logger.warn("The check \"" +check.getDisplayName() +"\" is supposed to check two databases, but the alternate DBA was NULL! Please set the altDbHost, altDbName, altDbUser, altDbPass properties in your config file.");
                }
            }

            QAReport qaReport = check.executeQACheck();
            if (qaReport.isEmpty())
            {
            	logger.info("Nothing to report!");
                continue;
            }
            else
            {
                String fileName = check.getDisplayName();
                qaReport.output(fileName + ".txt", output.getAbsolutePath());
                logger.info("Check "+output.getAbsolutePath()+"/"+fileName+".txt for report details.");
            }
        }
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
    
}
