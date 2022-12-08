package com.platform.field.config;

import lombok.Getter;

@Getter
public class Param<T> {

  private String name;
  private Class<T> type;
  private T defaultValue;

  Param(String name, T defaultValue, Class<T> type) {
    this.name = name;
    this.type = type;
    this.defaultValue = defaultValue;
  }

  public static Param<String> string(String name, String defaultValue) {
    return new Param<>(name, defaultValue, String.class);
  }

  public static Param<Integer> integer(String name, Integer defaultValue) {
    return new Param<>(name, defaultValue, Integer.class);
  }

  public static Param<Boolean> bool(String name, Boolean defaultValue) {
    return new Param<>(name, defaultValue, Boolean.class);
  }
}
