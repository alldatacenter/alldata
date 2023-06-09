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
package org.apache.drill.exec.store.httpd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonTypeName(HttpdLogFormatPlugin.DEFAULT_NAME)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class HttpdLogFormatConfig implements FormatPluginConfig {

  public static final String DEFAULT_TS_FORMAT = "dd/MMM/yyyy:HH:mm:ss ZZ";
  public final String logFormat;
  public final String timestampFormat;
  public final List<String> extensions;
  public final int maxErrors;
  public final boolean flattenWildcards;
  public final boolean parseUserAgent;
  public final String logParserRemapping;

  @JsonCreator
  public HttpdLogFormatConfig(
      @JsonProperty("extensions") List<String> extensions,
      @JsonProperty("logFormat") String logFormat,
      @JsonProperty("timestampFormat") String timestampFormat,
      @JsonProperty("maxErrors") int maxErrors,
      @JsonProperty("flattenWildcards") boolean flattenWildcards,
      @JsonProperty("parseUserAgent") boolean parseUserAgent,
      @JsonProperty("logParserRemapping") String logParserRemapping
  ) {

    this.extensions = extensions == null
      ? Collections.singletonList("httpd")
      : ImmutableList.copyOf(extensions);
    this.logFormat = logFormat;
    this.timestampFormat = timestampFormat;
    this.maxErrors = maxErrors;
    this.flattenWildcards = flattenWildcards;
    this.parseUserAgent = parseUserAgent;
    this.logParserRemapping = logParserRemapping;
  }

  /**
   * @return the log formatting string. This string is the config string from
   *         httpd.conf or similar config file.
   */
  public String getLogFormat() {
    return logFormat;
  }

  /**
   * @return the timestampFormat
   */
  public String getTimestampFormat() {
    return timestampFormat;
  }

  public List<String> getExtensions() {
    return extensions;
  }

  public int getMaxErrors() {
    return maxErrors;
  }

  public boolean getFlattenWildcards () {
    return flattenWildcards;
  }

  public boolean getParseUserAgent() {
    return parseUserAgent;
  }

  public String getLogParserRemapping() {
    return logParserRemapping;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
            logFormat,
            timestampFormat,
            maxErrors,
            flattenWildcards,
            parseUserAgent,
            logParserRemapping);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    HttpdLogFormatConfig other = (HttpdLogFormatConfig) obj;
    return Objects.equals(logFormat, other.logFormat)
      && Objects.equals(timestampFormat, other.timestampFormat)
      && Objects.equals(maxErrors, other.maxErrors)
      && Objects.equals(flattenWildcards, other.flattenWildcards)
      && Objects.equals(parseUserAgent, other.parseUserAgent)
      && Objects.equals(logParserRemapping, other.logParserRemapping);
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
        .field("log format", logFormat)
        .field("timestamp format", timestampFormat)
        .field("max errors", maxErrors)
        .field("flattenWildcards", flattenWildcards)
        .field("parseUserAgent", parseUserAgent)
        .field("logParserRemapping", logParserRemapping)
        .toString();
  }
}
