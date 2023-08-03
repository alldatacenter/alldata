package com.linkedin.feathr.exception;

/**
  * This exception is thrown when the feature join is incorrect.
  */
public class FrameFeatureJoinException extends FeathrException {

  public FrameFeatureJoinException(ErrorLabel errorLabel, String msg, Throwable cause) {
    super(errorLabel, msg, cause);
  }

  public FrameFeatureJoinException(ErrorLabel errorLabel, String msg) {
    super(errorLabel, msg);
  }
}