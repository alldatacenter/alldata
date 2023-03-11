package com.netease.arctic.ams.api.client;

import java.util.Objects;

public class AmsServerInfo {
  private String host;
  private Integer thriftBindPort;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getThriftBindPort() {
    return thriftBindPort;
  }

  public void setThriftBindPort(Integer thriftBindPort) {
    this.thriftBindPort = thriftBindPort;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AmsServerInfo that = (AmsServerInfo) o;
    return Objects.equals(host, that.host) && Objects.equals(thriftBindPort, that.thriftBindPort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, thriftBindPort);
  }
}
