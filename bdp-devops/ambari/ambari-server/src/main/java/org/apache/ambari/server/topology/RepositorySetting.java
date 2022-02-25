/**
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

package org.apache.ambari.server.topology;

public class RepositorySetting {
  /**
   * Settings for each repo setting sections
   */
  public static final String OVERRIDE_STRATEGY = "override_strategy";
  public static final String OVERRIDE_STRATEGY_ALWAYS_APPLY = "ALWAYS_APPLY";
  public static final String OVERRIDE_STRATEGY_APPLY_WHEN_MISSING = "APPLY_WHEN_MISSING";
  public static final String OPERATING_SYSTEM = "operating_system";
  public static final String REPO_ID = "repo_id";
  public static final String BASE_URL = "base_url";

  private String overrideStrategy;
  private String operatingSystem;
  private String repoId;
  private String baseUrl;

  /**
   * When specified under the "settings" section, it allows Ambari to overwrite existing repos stored
   * in the metainfo table in the Ambari server database.
   * Two override strategies
   * ALWAYS_APPLY will override the existing repo. If repo does not exists, add it.
   * APPLY_WHEN_MISSING will only add the repo info to the table if there is no such entries yet.
   *
   *   <pre>
   *     {@code
   *       "settings" : [
   *       {
   *            "repository_settings" : [
   *            {
   *              "override_strategy":"ALWAYS_APPLY",
   *              "operating_system":"redhat7",
   *              "repo_id":"HDP-2.6",
   *              "base_url":"http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos7/2.x/BUILDS/2.6.0.3-8"
   *            },
   *            {
   *              "override_strategy":"APPLY_WHEN_MISSING",
   *              "operating_system":"redhat7",
   *              "repo_id": "HDP-UTILS-1.1.0.21",
   *              "repo_name": "HDP-UTILS",
   *              "base_url": "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.21/repos/centos7"
   *            }
   *          ]
   *       }]
   *     }
   *   </pre>
   */
  public String getOverrideStrategy() {
    return overrideStrategy;
  }

  public void setOverrideStrategy(String overrideStrategy) {
    this.overrideStrategy = overrideStrategy;
  }

  /**
   * Get repository id
   * */
  public String getOperatingSystem() {
    return operatingSystem;
  }

  public void setOperatingSystem(String operatingSystem) {
    this.operatingSystem = operatingSystem;
  }

  /**
   * Get repository id
   * */
  public String getRepoId() {
    return repoId;
  }

  public void setRepoId(String repoId) {
    this.repoId = repoId;
  }

  /**
   * Get repository url
   * */
  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public String toString(){
    StringBuilder strBldr = new StringBuilder();
    strBldr.append(OVERRIDE_STRATEGY);strBldr.append(": ");strBldr.append(overrideStrategy);
    strBldr.append(OPERATING_SYSTEM);strBldr.append(": ");strBldr.append(operatingSystem);
    strBldr.append(REPO_ID);strBldr.append(": ");strBldr.append(repoId);
    strBldr.append(BASE_URL);strBldr.append(": ");strBldr.append(baseUrl);
    return strBldr.toString();
  }
}
