package com.linkedin.feathr.compute;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * An annotation indicating that the target is is part of a module-private "internal API" and should not be used by
 * external modules.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface InternalApi {
}