package com.linkedin.feathr.common;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Marks the target of this annotation as an experimental feature that may evolve in future, or may be removed
 * entirely.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface Experimental {
}
