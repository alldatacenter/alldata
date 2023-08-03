package com.linkedin.feathr.exception;

/**
  * This exception is thrown when the data input is incorrect.
  */
public class FrameInputDataException extends FeathrException {

  public FrameInputDataException(ErrorLabel errorLabel, String msg, Throwable cause) {
    super(errorLabel, msg, cause);
  }

  public FrameInputDataException(ErrorLabel errorLabel, String msg) {
    super(errorLabel, msg);
  }
}