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
package org.apache.drill.exec.store.phoenix;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.AbstractSecuredStoragePluginConfig;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.store.security.CredentialProviderUtils;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(PhoenixStoragePluginConfig.NAME)
public class PhoenixStoragePluginConfig extends AbstractSecuredStoragePluginConfig {

  public static final String NAME = "phoenix";
  public static final String THIN_DRIVER_CLASS = "org.apache.phoenix.queryserver.client.Driver";
  public static final String FAT_DRIVER_CLASS = "org.apache.phoenix.jdbc.PhoenixDriver";

  private final String host;
  private final int port;
  private final String jdbcURL; // (options) Equal to host + port
  private final Map<String, Object> props; // (options) See also http://phoenix.apache.org/tuning.html

  @JsonCreator
  public PhoenixStoragePluginConfig(
      @JsonProperty("host") String host,
      @JsonProperty("port") int port,
      @JsonProperty("userName") String userName,
      @JsonProperty("password") String password,
      @JsonProperty("jdbcURL") String jdbcURL,
      @JsonProperty("credentialsProvider") CredentialsProvider credentialsProvider,
      @JsonProperty("props") Map<String, Object> props) {
    super(CredentialProviderUtils.getCredentialsProvider(userName, password, credentialsProvider), credentialsProvider == null);
    this.host = host;
    this.port = port == 0 ? 8765 : port;
    this.jdbcURL = jdbcURL;
    this.props = props == null ? Collections.emptyMap() : props;
  }

  @JsonIgnore
  public UsernamePasswordCredentials getUsernamePasswordCredentials() {
    return new UsernamePasswordCredentials(credentialsProvider);
  }

  @JsonProperty("host")
  public String getHost() {
    return host;
  }

  @JsonProperty("port")
  public int getPort() {
    return port;
  }

  @JsonProperty("userName")
  public String getUsername() {
    if (directCredentials) {
      return getUsernamePasswordCredentials().getUsername();
    }
    return null;
  }

  @JsonIgnore
  @JsonProperty("password")
  public String getPassword() {
    if (directCredentials) {
      return getUsernamePasswordCredentials().getPassword();
    }
    return null;
  }

  @JsonProperty("jdbcURL")
  public String getJdbcURL() {
    return jdbcURL;
  }

  @JsonProperty("props")
  public Map<String, Object> getProps() {
    return props;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null || !(o instanceof PhoenixStoragePluginConfig)) {
      return false;
    }
    PhoenixStoragePluginConfig config = (PhoenixStoragePluginConfig) o;
    // URL first
    if (StringUtils.isNotBlank(config.getJdbcURL())) {
      return Objects.equals(this.jdbcURL, config.getJdbcURL());
    }
    // Then the host and port
    return Objects.equals(this.host, config.getHost()) && Objects.equals(this.port, config.getPort());
  }

  @Override
  public int hashCode() {
    if (StringUtils.isNotBlank(jdbcURL)) {
     return Objects.hash(jdbcURL);
    }
    return Objects.hash(host, port);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(PhoenixStoragePluginConfig.NAME)
        .field("driverName", THIN_DRIVER_CLASS)
        .field("host", host)
        .field("port", port)
        .field("userName", getUsername())
        .maskedField("password", getPassword()) // will set to "*******"
        .field("jdbcURL", jdbcURL)
        .field("props", props)
        .toString();
  }
}
