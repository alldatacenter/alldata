package com.netease.arctic.ams.api;

public final class ErrorCodes {

  public static final int UNDEFINED_ERROR_CODE = -1;

  public static final int PERSISTENCE_ERROR_CODE = 1000;
  public static final int OBJECT_NOT_EXISTS_ERROR_CODE = 1001;
  public static final int ALREADY_EXISTS_ERROR_CODE = 1002;
  public static final int ILLEGAL_METADATA_ERROR_CODE = 1003;

  public static final int TASK_NOT_FOUND_ERROR_CODE = 2001;
  public static final int DUPLICATED_TASK_ERROR_CODE = 2002;
  public static final int OPTIMIZING_CLOSED_ERROR_CODE = 2003;
  public static final int ILLEGAL_TASK_STATE_ERROR_CODE = 2004;
  public static final int PLUGIN_AUTH_ERROR_CODE = 2005;
  public static final int PLUGIN_RETRY_AUTH_ERROR_CODE = 2006;

  public static final int BLOCKER_CONFLICT_ERROR_CODE = 3001;
}
