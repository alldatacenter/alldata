package com.netease.arctic.optimizer.util;

import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.util.Map;

public class PropertyUtil {

  public static String checkAndGetProperty(Map<String, String> properties, String key) {
    Preconditions.checkState(properties != null && properties.containsKey(key),
        "Cannot find %s in properties", key);
    return properties.get(key);
  }
}
