package com.linkedin.feathr.common.exception;

/**
 * This exception is thrown when the input data can't be read or is invalid.
 */
public class FeathrInputDataException extends FeathrException {

    public FeathrInputDataException(ErrorLabel errorLabel, String msg, Throwable cause) {
        super(errorLabel, msg, cause);
    }

    public FeathrInputDataException(ErrorLabel errorLabel, String msg) {
        super(errorLabel, msg);
    }
}