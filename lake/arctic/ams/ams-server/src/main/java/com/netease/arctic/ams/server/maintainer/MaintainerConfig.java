package com.netease.arctic.ams.server.maintainer;

public class MaintainerConfig {

  private String thriftUrl;
  private String catalogName;

  public MaintainerConfig(String thriftUrl, String catalogName) {
    this.thriftUrl = thriftUrl;
    this.catalogName = catalogName;
  }

  public String getThriftUrl() {
    return thriftUrl;
  }

  public String getCatalogName() {
    return catalogName;
  }

}
