package com.linkedin.feathr.exception;

/**
  * This exception is thrown when the feature definition is incorrect.
  */
public class FeathrConfigException extends FeathrException {

  public FeathrConfigException(ErrorLabel errorLabel, String msg, Throwable cause) {
    super(errorLabel, msg, cause);
  }

  public FeathrConfigException(ErrorLabel errorLabel, String msg) {
    super(errorLabel, msg);
  }
}