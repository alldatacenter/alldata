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
package org.apache.drill.exec.store.dfs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;

import org.apache.drill.common.logical.AbstractSecuredStoragePluginConfig;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap.Builder;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;

@JsonTypeName(FileSystemConfig.NAME)
public class FileSystemConfig extends AbstractSecuredStoragePluginConfig {
  private static final List<String> FS_CREDENTIAL_KEYS =
      Arrays.asList(
          CommonConfigurationKeysPublic.HADOOP_SECURITY_CREDENTIAL_PROVIDER_PATH,
          "fs.s3a.access.key",
          "fs.s3a.secret.key");

  public static final String NAME = "file";

  private final String connection;
  private final Map<String, String> config;
  private final Map<String, WorkspaceConfig> workspaces;
  private final Map<String, FormatPluginConfig> formats;

  @JsonCreator
  public FileSystemConfig(@JsonProperty("connection") String connection,
                          @JsonProperty("config") Map<String, String> config,
                          @JsonProperty("workspaces") Map<String, WorkspaceConfig> workspaces,
                          @JsonProperty("formats") Map<String, FormatPluginConfig> formats,
                          @JsonProperty("credentialsProvider") CredentialsProvider credentialsProvider) {
    super(getCredentialsProvider(config, credentialsProvider), credentialsProvider == null);
    this.connection = connection;

    // Force creation of an empty map so that configs compare equal
    Builder<String, String> builder = ImmutableMap.builder();
    if (config != null) {
      builder.putAll(config);
    }
    this.config = builder.build();
    Map<String, WorkspaceConfig> caseInsensitiveWorkspaces = CaseInsensitiveMap.newHashMap();
    Optional.ofNullable(workspaces).ifPresent(caseInsensitiveWorkspaces::putAll);
    this.workspaces = caseInsensitiveWorkspaces;
    this.formats = formats != null ? formats : new LinkedHashMap<>();
  }

  @JsonProperty
  public String getConnection() {
    return connection;
  }

  @JsonProperty
  public Map<String, String> getConfig() {
    return config;
  }

  @JsonProperty
  public Map<String, WorkspaceConfig> getWorkspaces() {
    return workspaces;
  }

  @JsonProperty
  public Map<String, FormatPluginConfig> getFormats() {
    return formats;
  }

  @Override
  public String getValue(String key) {
    return config == null ? null : config.get(key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connection, config, formats, workspaces);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FileSystemConfig other = (FileSystemConfig) obj;
    return Objects.equals(connection, other.connection) &&
           Objects.equals(config, other.config) &&
           Objects.equals(formats, other.formats) &&
           Objects.equals(workspaces, other.workspaces);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
        .field("connection", connection)
        .field("config", config)
        .field("formats", formats)
        .field("workspaces", workspaces)
        .toString();
  }

  /**
   * Copy the file system configuration. This <b>must</b> be done prior
   * to modifying a config already stored in the registry. The registry
   * maintains a key based on config value.
   * @return a copy of this config which may be modified
   */
  public FileSystemConfig copy() {
    return copyWithFormats(null);
  }

  /**
   * Copy this file system config with the set of new/replaced formats.
   * This <b>must</b> be done if the file system config is already stored
   * in the plugin registry
   * @param newFormats optional new formats to add
   * @return copy with the new formats
   */
  public FileSystemConfig copyWithFormats(Map<String, FormatPluginConfig> newFormats) {
    // Must make copies of structures. Turns out that the constructor already
    // copies workspaces, so we need not copy it here.
    Map<String, String> configCopy = config == null ? null : new HashMap<>(config);
    Map<String, FormatPluginConfig> formatsCopy =
        formats == null ? null : new LinkedHashMap<>(formats);
    if (newFormats != null) {
      formatsCopy = formatsCopy == null ? new LinkedHashMap<>() : formatsCopy;
      formatsCopy.putAll(newFormats);
    }
    FileSystemConfig newConfig =
        new FileSystemConfig(connection, configCopy, workspaces, formatsCopy, credentialsProvider);
    newConfig.setEnabled(isEnabled());
    return newConfig;
  }

  private static CredentialsProvider getCredentialsProvider(
      Map<String, String> config,
      CredentialsProvider credentialsProvider) {
    if (credentialsProvider != null) {
      return credentialsProvider;
    }

    if (config != null) {
      Map<String, String> credentials = FS_CREDENTIAL_KEYS.stream()
          .filter(config::containsKey)
          .collect(Collectors.toMap(fsCredentialKey -> fsCredentialKey, config::get));

      return new PlainCredentialsProvider(credentials);
    }
    return PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER;
  }
}
