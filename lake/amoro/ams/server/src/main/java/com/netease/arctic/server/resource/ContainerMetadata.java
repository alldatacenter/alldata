package com.netease.arctic.server.resource;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;

public class ContainerMetadata {
  private final String name;
  private final String implClass;
  private Map<String, String> properties;

  public ContainerMetadata(String name, String implClass) {
    Preconditions.checkArgument(name != null && implClass != null,
        "Resource container name and implementation class can not be null");
    this.name = name;
    this.implClass = implClass;
    properties = Maps.newHashMap();
  }

  public String getName() {
    return name;
  }

  public String getImplClass() {
    return implClass;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
