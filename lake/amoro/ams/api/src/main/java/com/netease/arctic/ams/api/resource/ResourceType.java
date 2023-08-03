package com.netease.arctic.ams.api.resource;

public enum ResourceType {

  OPTIMIZER(0);

  private int type;

  ResourceType(int type) {
    this.type = type;
  }

  public int getType() {
    return type;
  }
}
