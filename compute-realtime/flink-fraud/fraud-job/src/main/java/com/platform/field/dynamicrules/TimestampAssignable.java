package com.platform.field.dynamicrules;

public interface TimestampAssignable<T> {
  void assignIngestionTimestamp(T timestamp);
}
