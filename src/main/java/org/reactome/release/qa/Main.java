package org.reactome.release.qa;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;
import org.reactome.release.qa.annotations.ReleaseQATest;
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
    
    public static void main(String[] args) throws Exception {
        File output = prepareOutput();
        // Make sure we have output
        MySQLAdaptorManager manager = MySQLAdaptorManager.getManager();
        MySQLAdaptor dba = manager.getDBA();
        // Get the list of QAs from the package
        String packageName = "org.reactome.release.qa.check";
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends QACheck>> releaseQAs = reflections.getSubTypesOf(QACheck.class).stream()
                                                        // Filter to exclude Abstract classes, and anything annotated with @ReleaseQATest, since this Main is for Slice QA.
                                                        .filter(c -> !Modifier.isAbstract(c.getModifiers())
                                                                     && Arrays.stream(c.getAnnotations()).noneMatch(a -> a.annotationType().getName().equals(ReleaseQATest.class.getName())) )
                                                        .collect(Collectors.toSet());
        for (Class<? extends QACheck> cls : releaseQAs) {
            QACheck check = cls.newInstance();
            System.out.println("Perform " + check.getDisplayName() + "...");
            check.setMySQLAdaptor(dba);
            QAReport qaReport = check.executeQACheck();
            if (qaReport.isEmpty())
                continue;
            String fileName = check.getDisplayName();
            qaReport.output(fileName + ".txt", output.getAbsolutePath());
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
