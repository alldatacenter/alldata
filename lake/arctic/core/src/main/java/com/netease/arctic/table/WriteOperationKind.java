package com.netease.arctic.table;

public enum WriteOperationKind {
  APPEND,
  OVERWRITE,
  MINOR_OPTIMIZE,
  MAJOR_OPTIMIZE,
  FULL_OPTIMIZE
}
