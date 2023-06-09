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
package org.apache.drill.exec.store.elasticsearch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.drill.common.logical.AbstractSecuredStoragePluginConfig;
import org.apache.drill.exec.store.security.CredentialProviderUtils;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonTypeName(ElasticsearchStorageConfig.NAME)
public class ElasticsearchStorageConfig extends AbstractSecuredStoragePluginConfig {
  public static final String NAME = "elastic";

  private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writerFor(List.class);

  private static final String HOSTS = "hosts";

  private static final String PATH_PREFIX = "pathPrefix";

  private static final String USERNAME = "username";

  private static final String PASSWORD = "password";

  public static final String CREDENTIALS_PROVIDER = "credentialsProvider";

  private static final String EMPTY_STRING = "";

  private final List<String> hosts;
  private final String pathPrefix;

  @JsonCreator
  public ElasticsearchStorageConfig(
      @JsonProperty(HOSTS) List<String> hosts,
      @JsonProperty(USERNAME) String username,
      @JsonProperty(PASSWORD) String password,
      @JsonProperty(PATH_PREFIX) String pathPrefix,
      @JsonProperty(CREDENTIALS_PROVIDER) CredentialsProvider credentialsProvider) {
    super(CredentialProviderUtils.getCredentialsProvider(username, password, credentialsProvider), credentialsProvider == null);
    this.hosts = hosts;
    this.pathPrefix = pathPrefix;
  }

  public List<String> getHosts() {
    return hosts;
  }

  public String getPathPrefix() {
    return pathPrefix;
  }

  public String getUsername() {
    if (directCredentials) {
      return getUsernamePasswordCredentials().getUsername();
    }
    return null;
  }

  public String getPassword() {
    if (directCredentials) {
      return getUsernamePasswordCredentials().getPassword();
    }
    return null;
  }

  @JsonIgnore
  public UsernamePasswordCredentials getUsernamePasswordCredentials() {
    return new UsernamePasswordCredentials(credentialsProvider);
  }

  @JsonIgnore
  public Map<String, Object> toConfigMap()
      throws JsonProcessingException {
    Map<String, String> credentials = new HashMap<>(credentialsProvider.getCredentials());
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    builder.put(HOSTS, OBJECT_WRITER.writeValueAsString(hosts));
    builder.put(PATH_PREFIX, pathPrefix != null ? pathPrefix : EMPTY_STRING);
    builder.put(USERNAME, credentials.getOrDefault(USERNAME, EMPTY_STRING));
    builder.put(PASSWORD, credentials.getOrDefault(PASSWORD, EMPTY_STRING));

    credentials.remove(USERNAME);
    credentials.remove(PASSWORD);
    builder.putAll(credentials);
    return builder.build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElasticsearchStorageConfig that = (ElasticsearchStorageConfig) o;
    return Objects.equals(hosts, that.hosts)
        && Objects.equals(credentialsProvider, that.credentialsProvider);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hosts, credentialsProvider);
  }
}
