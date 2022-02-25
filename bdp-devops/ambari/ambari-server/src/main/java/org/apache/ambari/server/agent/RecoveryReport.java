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
package org.apache.ambari.server.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;


public class RecoveryReport {

  /**
   * One of DISABLED, RECOVERABLE, UNRECOVERABLE, PARTIALLY_RECOVERABLE
   */
  private String summary = "DISABLED";
  private List<ComponentRecoveryReport> componentReports = new ArrayList<>();


  @JsonProperty("summary")
  @com.fasterxml.jackson.annotation.JsonProperty("summary")
  public String getSummary() {
    return summary;
  }

  @JsonProperty("summary")
  @com.fasterxml.jackson.annotation.JsonProperty("summary")
  public void setSummary(String summary) {
    this.summary = summary;
  }

  @JsonProperty("component_reports")
  @com.fasterxml.jackson.annotation.JsonProperty("component_reports")
  public List<ComponentRecoveryReport> getComponentReports() {
    return componentReports;
  }

  @JsonProperty("component_reports")
  @com.fasterxml.jackson.annotation.JsonProperty("component_reports")
  public void setComponentReports(List<ComponentRecoveryReport> componentReports) {
    this.componentReports = componentReports;
  }

  @Override
  public String toString() {
    String componentReportsStr = "[]";
    if (componentReports != null) {
      componentReportsStr = Arrays.toString(componentReports.toArray());
    }
    return "RecoveryReport{" +
           "summary='" + summary + '\'' +
           ", component_reports='" + componentReportsStr +
           "'}";
  }
}
