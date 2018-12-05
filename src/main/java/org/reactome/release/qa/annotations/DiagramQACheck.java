package org.reactome.release.qa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to mark classes that are converted from the
 * diagram-converter QA checks.
 *
 * @author Fred Loney <loneyf@ohsu.edu>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DiagramQACheck {
}
