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

package org.apache.drill.exec.store.syslog;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;

import java.util.List;
import java.util.Objects;

@JsonTypeName("syslog")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class SyslogFormatConfig implements FormatPluginConfig {

  private final List<String> extensions;
  private final int maxErrors;
  private final boolean flattenStructuredData;

  @JsonCreator
  public SyslogFormatConfig(
      @JsonProperty("extensions") List<String> extensions,
      @JsonProperty("maxErrors") Integer maxErrors,
      @JsonProperty("flattenStructuredData") Boolean flattenStructuredData) {
    this.extensions = extensions == null ?
        ImmutableList.of() : ImmutableList.copyOf(extensions);
    this.maxErrors = maxErrors == null ? 10 : maxErrors;
    this.flattenStructuredData = flattenStructuredData == null ? false : flattenStructuredData;
  }

  @JsonProperty("flattenStructuredData")
  public boolean flattenStructuredData() {
    return flattenStructuredData;
  }

  @JsonProperty("maxErrors")
  public int getMaxErrors() {
    return maxErrors;
  }

  @JsonProperty("extensions")
  public List<String> getExtensions() {
    return extensions;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SyslogFormatConfig other = (SyslogFormatConfig) obj;
    return Objects.equals(extensions, other.extensions) &&
           Objects.equals(maxErrors, other.maxErrors) &&
           Objects.equals(flattenStructuredData, other.flattenStructuredData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxErrors, flattenStructuredData, extensions);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
        .field("extensions", extensions)
        .field("max errors", maxErrors)
        .field("flatten structured data", flattenStructuredData)
        .toString();
  }
}
