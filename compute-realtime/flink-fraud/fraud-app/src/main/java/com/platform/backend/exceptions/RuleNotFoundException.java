package com.platform.backend.exceptions;

public class RuleNotFoundException extends RuntimeException {

  public RuleNotFoundException(Integer id) {
    super("Could not find employee " + id);
  }
}
