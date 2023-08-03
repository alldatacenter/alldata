package com.linkedin.feathr.exception;

/**
  * This exception is thrown when the data output is not not successful.
  */
public class FrameDataOutputException extends FeathrException {

  public FrameDataOutputException(ErrorLabel errorLabel, String msg, Throwable cause) {
    super(errorLabel, msg, cause);
  }

  public FrameDataOutputException(ErrorLabel errorLabel, String msg) {
    super(errorLabel, msg);
  }
}