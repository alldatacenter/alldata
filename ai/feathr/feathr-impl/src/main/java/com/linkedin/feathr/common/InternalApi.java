package com.linkedin.feathr.common;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * An annotation indicating that the target is part of a module-private "internal API" and SHOULD NOT be used by
 * external modules. These APIs are not guaranteed to be stable across releases.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface InternalApi {
}
