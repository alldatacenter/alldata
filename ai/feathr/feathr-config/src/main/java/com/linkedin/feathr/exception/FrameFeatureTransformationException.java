package com.linkedin.feathr.exception;

/**
  * This exception is thrown when something wrong happened during feature transformation.
  */
public class FrameFeatureTransformationException extends FeathrException {

  public FrameFeatureTransformationException(ErrorLabel errorLabel, String msg, Throwable cause) {
    super(errorLabel, msg, cause);
  }

  public FrameFeatureTransformationException(ErrorLabel errorLabel, String msg) {
    super(errorLabel, msg);
  }
}