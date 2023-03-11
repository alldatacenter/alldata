package com.hw.lineage.common.util.function;

import com.hw.lineage.common.util.ExceptionUtils;

import java.util.function.Function;

/**
 * @description: Utility class for Flink's functions.
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class FunctionUtils {

    private FunctionUtils() {
        throw new UnsupportedOperationException("This class should never be instantiated.");
    }

    /**
     * Convert at {@link FunctionWithException} into a {@link Function}.
     *
     * @param functionWithException function with exception to convert into a function
     * @param <A>                   input type
     * @param <B>                   output type
     * @return {@link Function} which throws all checked exception as an unchecked exception.
     */
    public static <A, B> Function<A, B> uncheckedFunction(
            FunctionWithException<A, B, ?> functionWithException) {
        return (A value) -> {
            try {
                return functionWithException.apply(value);
            } catch (Throwable t) {
                ExceptionUtils.rethrow(t);
                // we need this to appease the compiler :-(
                return null;
            }
        };
    }
}
