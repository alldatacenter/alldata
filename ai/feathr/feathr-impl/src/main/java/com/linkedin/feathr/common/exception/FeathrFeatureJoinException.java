package com.linkedin.feathr.common.exception;

/**
 * This exception is thrown when the feature definition is invalid.
 */
public class FeathrFeatureJoinException extends FeathrException {

    public FeathrFeatureJoinException(ErrorLabel errorLabel, String msg, Throwable cause) {
        super(errorLabel, msg, cause);
    }

    public FeathrFeatureJoinException(ErrorLabel errorLabel, String msg) {
        super(errorLabel, msg);
    }
}