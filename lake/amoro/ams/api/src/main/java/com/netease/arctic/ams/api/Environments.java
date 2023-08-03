package com.netease.arctic.ams.api;

public class Environments {

  public static final String SYSTEM_ARCTIC_HOME = "ARCTIC_HOME";

  public static String getArcticHome() {
    String arcticHome = System.getenv(SYSTEM_ARCTIC_HOME);
    if (arcticHome != null) {
      return arcticHome;
    }
    return System.getProperty(SYSTEM_ARCTIC_HOME);
  }
}
