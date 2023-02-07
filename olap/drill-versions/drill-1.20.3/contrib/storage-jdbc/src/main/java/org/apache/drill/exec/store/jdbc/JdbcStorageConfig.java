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
package org.apache.drill.exec.store.jdbc;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.AbstractSecuredStoragePluginConfig;
import org.apache.drill.exec.store.security.CredentialProviderUtils;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;

@JsonTypeName(JdbcStorageConfig.NAME)
@JsonFilter("passwordFilter")
public class JdbcStorageConfig extends AbstractSecuredStoragePluginConfig {

  public static final String NAME = "jdbc";
  public static final int DEFAULT_MAX_WRITER_BATCH_SIZE = 10000;

  private final String driver;
  private final String url;
  private final boolean caseInsensitiveTableNames;
  private final boolean writable;
  private final Map<String, Object> sourceParameters;
  private final int writerBatchSize;

  @JsonCreator
  public JdbcStorageConfig(
      @JsonProperty("driver") String driver,
      @JsonProperty("url") String url,
      @JsonProperty("username") String username,
      @JsonProperty("password") String password,
      @JsonProperty("caseInsensitiveTableNames") boolean caseInsensitiveTableNames,
      @JsonProperty("writable") boolean writable,
      @JsonProperty("sourceParameters") Map<String, Object> sourceParameters,
      @JsonProperty("credentialsProvider") CredentialsProvider credentialsProvider,
      @JsonProperty("writerBatchSize") int writerBatchSize) {
    super(CredentialProviderUtils.getCredentialsProvider(username, password, credentialsProvider), credentialsProvider == null);
    this.driver = driver;
    this.url = url;
    this.writable = writable;
    this.caseInsensitiveTableNames = caseInsensitiveTableNames;
    this.sourceParameters = sourceParameters == null ? Collections.emptyMap() : sourceParameters;
    this.writerBatchSize = writerBatchSize == 0 ? writerBatchSize = DEFAULT_MAX_WRITER_BATCH_SIZE : writerBatchSize;
  }

  public String getDriver() {
    return driver;
  }

  public String getUrl() {
    return url;
  }

  public boolean isWritable() { return writable; }

  public int getWriterBatchSize() { return writerBatchSize; }

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

  @JsonProperty("caseInsensitiveTableNames")
  public boolean areTableNamesCaseInsensitive() {
    return caseInsensitiveTableNames;
  }

  public Map<String, Object> getSourceParameters() {
    return sourceParameters;
  }

  @JsonIgnore
  public UsernamePasswordCredentials getUsernamePasswordCredentials() {
    return new UsernamePasswordCredentials(credentialsProvider);
  }

  @Override
  public int hashCode() {
    return Objects.hash(driver, url, caseInsensitiveTableNames, sourceParameters, credentialsProvider, writable, writerBatchSize);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JdbcStorageConfig that = (JdbcStorageConfig) o;
    return caseInsensitiveTableNames == that.caseInsensitiveTableNames &&
        Objects.equals(driver, that.driver) &&
        Objects.equals(url, that.url) &&
        Objects.equals(writable, that.writable) &&
        Objects.equals(sourceParameters, that.sourceParameters) &&
        Objects.equals(credentialsProvider, that.credentialsProvider) &&
        Objects.equals(writerBatchSize, that.writerBatchSize);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("driver", driver)
      .field("url", url)
      .field("writable", writable)
      .field("writerBatchSize", writerBatchSize)
      .field("caseInsensitiveTableNames", caseInsensitiveTableNames)
      .toString();
  }
}
