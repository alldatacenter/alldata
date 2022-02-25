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


import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class HostComponentLoggingInfo {

  private String componentName;

  private List<LogFileDefinitionInfo> listOfLogFileDefinitions;

  private List<NameValuePair> listOfLogLevels;


  public HostComponentLoggingInfo() {
  }

  @JsonProperty("name")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("name")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  @JsonProperty("logs")
  public List<LogFileDefinitionInfo> getListOfLogFileDefinitions() {
    return listOfLogFileDefinitions;
  }

  @JsonProperty("logs")
  public void setListOfLogFileDefinitions(List<LogFileDefinitionInfo> listOfLogFileDefinitions) {
    this.listOfLogFileDefinitions = listOfLogFileDefinitions;
  }

  @JsonProperty("log_level_counts")
  public List<NameValuePair> getListOfLogLevels() {
    return listOfLogLevels;
  }

  @JsonProperty("log_level_counts")
  public void setListOfLogLevels(List<NameValuePair> listOfLogLevels) {
    this.listOfLogLevels = listOfLogLevels;
  }
}
