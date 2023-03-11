package com.hw.lineage.common.exception;

/**
 * @description: LineageRuntimeException
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class LineageException extends RuntimeException {

    private static final long serialVersionUID = 193141189399279147L;

    /**
     * Creates a new Exception with the given message and null as the cause.
     *
     * @param message The exception message
     */
    public LineageException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a null message and the given cause.
     *
     * @param cause The exception that caused this exception
     */
    public LineageException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message The exception message
     * @param cause   The exception that caused this exception
     */
    public LineageException(String message, Throwable cause) {
        super(message, cause);
    }
}
