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
package org.apache.drill.exec.store.http;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.logical.AbstractSecuredStoragePluginConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.exec.store.security.CredentialProviderUtils;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.store.security.oauth.OAuthTokenCredentials;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@JsonTypeName(HttpStoragePluginConfig.NAME)
public class HttpStoragePluginConfig extends AbstractSecuredStoragePluginConfig {
  private static final Logger logger = LoggerFactory.getLogger(HttpStoragePluginConfig.class);
  public static final String NAME = "http";

  public final Map<String, HttpApiConfig> connections;
  public final boolean cacheResults;
  public final String proxyHost;
  public final int proxyPort;
  public final String proxyType;
  public final HttpOAuthConfig oAuthConfig;

  /**
   * Timeout in {@link TimeUnit#SECONDS}.
   */
  public final int timeout;

  @JsonCreator
  public HttpStoragePluginConfig(@JsonProperty("cacheResults") Boolean cacheResults,
                                 @JsonProperty("connections") Map<String, HttpApiConfig> connections,
                                 @JsonProperty("timeout") Integer timeout,
                                 @JsonProperty("proxyHost") String proxyHost,
                                 @JsonProperty("proxyPort") Integer proxyPort,
                                 @JsonProperty("proxyType") String proxyType,
                                 @JsonProperty("proxyUsername") String proxyUsername,
                                 @JsonProperty("proxyPassword") String proxyPassword,
                                 @JsonProperty("oAuthConfig") HttpOAuthConfig oAuthConfig,
                                 @JsonProperty("credentialsProvider") CredentialsProvider credentialsProvider
                                 ) {
    super(CredentialProviderUtils.getCredentialsProvider(
        getClientID(new OAuthTokenCredentials(credentialsProvider)),
        getClientSecret(new OAuthTokenCredentials(credentialsProvider)),
        getTokenURL(new OAuthTokenCredentials(credentialsProvider)),
        normalize(proxyUsername),
        normalize(proxyPassword),
        credentialsProvider),
        credentialsProvider == null);
    this.cacheResults = cacheResults != null && cacheResults;

    this.connections = CaseInsensitiveMap.newHashMap();
    if (connections != null) {
      this.connections.putAll(connections);
    }

    this.timeout = timeout == null ? 0 : timeout;
    this.proxyHost = normalize(proxyHost);
    this.proxyPort = proxyPort == null ? 0 : proxyPort;
    this.oAuthConfig = oAuthConfig;

    proxyType = normalize(proxyType);
    this.proxyType = proxyType == null
        ? "direct" : proxyType.trim().toLowerCase();

    // Validate Proxy Type
    switch (this.proxyType) {
      case "direct":
      case "http":
      case "socks":
        break;
      default:
        throw UserException
          .validationError()
          .message("Invalid Proxy Type: %s.  Drill supports 'direct', 'http' and 'socks' proxies.", proxyType)
          .build(logger);
    }
  }

  /**
   * Clone constructor used for updating OAuth tokens
   * @param that The current HTTP Plugin Config
   * @param oAuthConfig The updated OAuth config
   */
  public HttpStoragePluginConfig(HttpStoragePluginConfig that, HttpOAuthConfig oAuthConfig) {
    super(CredentialProviderUtils.getCredentialsProvider(that.proxyUsername(), that.proxyPassword(), that.credentialsProvider),
      that.credentialsProvider == null);

    this.cacheResults = that.cacheResults;
    this.connections = that.connections;
    this.timeout = that.timeout;
    this.proxyHost = that.proxyHost;
    this.proxyPort = that.proxyPort;
    this.proxyType = that.proxyType;
    this.oAuthConfig = oAuthConfig;
  }

  private static String normalize(String value) {
    if (value == null) {
      return value;
    }
    value = value.trim();
    return value.isEmpty() ? null : value;
  }

  /**
   * Create a copy of the plugin config with only the indicated connection.
   * The copy is used in the query plan to avoid including unnecessary information.
   */
  public HttpStoragePluginConfig copyForPlan(String connectionName) {
    return new HttpStoragePluginConfig(
        cacheResults, configFor(connectionName), timeout,
        proxyHost, proxyPort, proxyType, null, null, oAuthConfig, credentialsProvider);
  }

  private Map<String, HttpApiConfig> configFor(String connectionName) {
    Map<String, HttpApiConfig> single = new HashMap<>();
    single.put(connectionName, getConnection(connectionName));
    return single;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    } else if (that == null || getClass() != that.getClass()) {
      return false;
    }
    HttpStoragePluginConfig thatConfig = (HttpStoragePluginConfig) that;
    return Objects.equals(connections, thatConfig.connections) &&
           Objects.equals(cacheResults, thatConfig.cacheResults) &&
           Objects.equals(proxyHost, thatConfig.proxyHost) &&
           Objects.equals(proxyPort, thatConfig.proxyPort) &&
           Objects.equals(proxyType, thatConfig.proxyType) &&
           Objects.equals(oAuthConfig, thatConfig.oAuthConfig) &&
           Objects.equals(credentialsProvider, thatConfig.credentialsProvider);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("connections", connections)
      .field("cacheResults", cacheResults)
      .field("timeout", timeout)
      .field("proxyHost", proxyHost)
      .field("proxyPort", proxyPort)
      .field("credentialsProvider", credentialsProvider)
      .field("oauthConfig", oAuthConfig)
      .field("proxyType", proxyType)
      .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(connections, cacheResults, timeout,
        proxyHost, proxyPort, proxyType, oAuthConfig, credentialsProvider);
  }

  @JsonProperty("cacheResults")
  public boolean cacheResults() { return cacheResults; }

  @JsonProperty("connections")
  public Map<String, HttpApiConfig> connections() { return connections; }

  @JsonProperty("timeout")
  public int timeout() { return timeout;}

  @JsonProperty("proxyHost")
  public String proxyHost() { return proxyHost; }

  @JsonProperty("proxyPort")
  public int proxyPort() { return proxyPort; }

  @JsonProperty("oAuthConfig")
  public HttpOAuthConfig oAuthConfig() {
    return oAuthConfig;
  }

  @JsonProperty("proxyUsername")
  public String proxyUsername() {
    if (directCredentials) {
      return getUsernamePasswordCredentials().getUsername();
    }
    return null;
  }

  @JsonProperty("proxyPassword")
  public String proxyPassword() {
    if (directCredentials) {
      return getUsernamePasswordCredentials().getPassword();
    }
    return null;
  }

  @JsonIgnore
  private static String getClientID(OAuthTokenCredentials credentials) {
    return credentials.getClientID();
  }

  @JsonIgnore
  private static String getClientSecret(OAuthTokenCredentials credentials) {
    return credentials.getClientSecret();
  }

  @JsonIgnore
  private static String getTokenURL(OAuthTokenCredentials credentials) {
    return credentials.getTokenUri();
  }

  @JsonProperty("proxyType")
  public String proxyType() { return proxyType; }

  @JsonIgnore
  public HttpApiConfig getConnection(String connectionName) {
    return connections.get(connectionName);
  }

  @JsonIgnore
  public UsernamePasswordCredentials getUsernamePasswordCredentials() {
    return new UsernamePasswordCredentials(credentialsProvider);
  }

  @JsonIgnore
  public static OAuthTokenCredentials getOAuthCredentials(CredentialsProvider credentialsProvider) {
    return new OAuthTokenCredentials(credentialsProvider);
  }
}
