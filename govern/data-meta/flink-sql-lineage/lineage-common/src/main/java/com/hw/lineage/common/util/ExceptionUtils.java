package com.hw.lineage.common.util;

/**
 * @description: A collection of utility functions for dealing with exceptions and exception workflows.
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class ExceptionUtils {

    private ExceptionUtils() {
    }


    /**
     * Throws the given {@code Throwable} in scenarios where the signatures do not allow you to
     * throw an arbitrary Throwable. Errors and RuntimeExceptions are thrown directly, other
     * exceptions are packed into runtime exceptions
     *
     * @param t The throwable to be thrown.
     */
    public static void rethrow(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        } else if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else {
            throw new RuntimeException(t);
        }
    }
}
