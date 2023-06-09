/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.drill.exec.store.splunk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.AbstractSecuredStoragePluginConfig;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.store.security.CredentialProviderUtils;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;

import java.util.Objects;

@JsonTypeName(SplunkPluginConfig.NAME)
public class SplunkPluginConfig extends AbstractSecuredStoragePluginConfig {

  public static final String NAME = "splunk";
  public static final int DISABLED_RECONNECT_RETRIES = 1;

  private final String hostname;
  private final String earliestTime;
  private final String latestTime;

  private final int port;
  private final Integer reconnectRetries;

  @JsonCreator
  public SplunkPluginConfig(@JsonProperty("username") String username,
                            @JsonProperty("password") String password,
                            @JsonProperty("hostname") String hostname,
                            @JsonProperty("port") int port,
                            @JsonProperty("earliestTime") String earliestTime,
                            @JsonProperty("latestTime") String latestTime,
                            @JsonProperty("credentialsProvider") CredentialsProvider credentialsProvider,
                            @JsonProperty("reconnectRetries") Integer reconnectRetries) {
    super(CredentialProviderUtils.getCredentialsProvider(username, password, credentialsProvider),
        credentialsProvider == null);
    this.hostname = hostname;
    this.port = port;
    this.earliestTime = earliestTime;
    this.latestTime = latestTime == null ? "now" : latestTime;
    this.reconnectRetries = reconnectRetries;
  }

  @JsonIgnore
  public UsernamePasswordCredentials getUsernamePasswordCredentials() {
    return new UsernamePasswordCredentials(credentialsProvider);
  }

  @JsonProperty("username")
  public String getUsername() {
    if (directCredentials) {
      return getUsernamePasswordCredentials().getUsername();
    }
    return null;
  }

  @JsonProperty("password")
  public String getPassword() {
    if (directCredentials) {
      return getUsernamePasswordCredentials().getPassword();
    }
    return null;
  }

  @JsonProperty("hostname")
  public String getHostname() {
    return hostname;
  }

  @JsonProperty("port")
  public int getPort() {
    return port;
  }

  @JsonProperty("earliestTime")
  public String getEarliestTime() {
    return earliestTime;
  }

  @JsonProperty("latestTime")
  public String getLatestTime() {
    return latestTime;
  }

  @JsonProperty("reconnectRetries")
  public int getReconnectRetries() {
    return reconnectRetries != null ? reconnectRetries : DISABLED_RECONNECT_RETRIES;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    } else if (that == null || getClass() != that.getClass()) {
      return false;
    }
    SplunkPluginConfig thatConfig = (SplunkPluginConfig) that;
    return Objects.equals(credentialsProvider, thatConfig.credentialsProvider) &&
      Objects.equals(hostname, thatConfig.hostname) &&
      Objects.equals(port, thatConfig.port) &&
      Objects.equals(earliestTime, thatConfig.earliestTime) &&
      Objects.equals(latestTime, thatConfig.latestTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentialsProvider, hostname, port, earliestTime, latestTime);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("credentialsProvider", credentialsProvider)
      .field("hostname", hostname)
      .field("port", port)
      .field("earliestTime", earliestTime)
      .field("latestTime", latestTime)
      .toString();
  }
}
