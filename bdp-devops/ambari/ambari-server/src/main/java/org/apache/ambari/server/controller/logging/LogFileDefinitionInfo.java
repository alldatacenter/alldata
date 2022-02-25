/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.logging;


import org.codehaus.jackson.annotate.JsonProperty;

public class LogFileDefinitionInfo {

  private String logFileName;

  private LogFileType logFileType;

  private String searchEngineURL;

  private String logFileTailURL;

  // default, no args constructor, required for Jackson usage
  public LogFileDefinitionInfo() {
  }

  protected LogFileDefinitionInfo(String logFileName, LogFileType logFileType, String searchEngineURL, String logFileTailURL) {
    this.logFileName = logFileName;
    this.logFileType = logFileType;
    this.searchEngineURL = searchEngineURL;
    this.logFileTailURL = logFileTailURL;
  }

  @JsonProperty("name")
  public String getLogFileName() {
    return logFileName;
  }

  @JsonProperty("name")
  public void setLogFileName(String logFileName) {
    this.logFileName = logFileName;
  }

  @JsonProperty("type")
  public LogFileType getLogFileType() {
    return logFileType;
  }

  @JsonProperty("type")
  public void setLogFileType(LogFileType logFileType) {
    this.logFileType = logFileType;
  }

  @JsonProperty("searchEngineURL")
  public String getSearchEngineURL() {
    return searchEngineURL;
  }

  @JsonProperty("searchEngineURL")
  public void setSearchEngineURL(String searchEngineURL) {
    this.searchEngineURL = searchEngineURL;
  }

  @JsonProperty("logFileTailURL")
  public String getLogFileTailURL() {
    return logFileTailURL;
  }

  @JsonProperty("logFileTailURL")
  public void setLogFileTailURL(String logFileTailURL) {
    this.logFileTailURL = logFileTailURL;
  }
}
