package com.linkedin.feathr.common.exception;


/**
 * Base exception for Feathr
 */
public class FeathrException extends RuntimeException {
    public FeathrException(String msg) {
        super(msg);
    }

    public FeathrException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public FeathrException(ErrorLabel errorLabel, String msg, Throwable cause) {
        super(String.format("[%s]", errorLabel) + " " + msg, cause);
    }

    public FeathrException(ErrorLabel errorLabel, String msg) {
        super(String.format("[%s]", errorLabel) + " " + msg);
    }
}