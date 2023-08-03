package com.linkedin.feathr.common.exception;

public class FeathrFeatureTransformationException extends FeathrException {

    public FeathrFeatureTransformationException(ErrorLabel errorLabel, String msg, Throwable cause) {
        super(errorLabel, msg, cause);
    }

    public FeathrFeatureTransformationException(ErrorLabel errorLabel, String msg) {
        super(errorLabel, msg);
    }
}