package com.linkedin.feathr.common.exception;

/**
 * This exception is thrown when the data output cannot be written successfully.
 */
public class FeathrDataOutputException extends FeathrException {

    public FeathrDataOutputException(ErrorLabel errorLabel, String msg, Throwable cause) {
        super(errorLabel, msg, cause);
    }

    public FeathrDataOutputException(ErrorLabel errorLabel, String msg) {
        super(errorLabel, msg);
    }
}