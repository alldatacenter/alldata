package com.netease.arctic.server.exception;

public class UndefinedException extends ArcticRuntimeException {
  public UndefinedException(Throwable throwable) {
    super(throwable);
  }

  public UndefinedException(String message) {
    super(message);
  }
}
