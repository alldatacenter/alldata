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
package org.apache.ambari.server.orm.dao;

/**
 * Used when getting per host alert summary in bulk where a single database call will 
 * return alert summaries for multiple hosts. In addition to the information
 * returned by {@link AlertSummaryDTO}, a host name is provided.
 */
public class HostAlertSummaryDTO extends AlertSummaryDTO {

  private String hostName;

  /**
   * Constructor, used by JPA. JPA invokes this constructor, even if there are no 
   * records in the resultset. In that case, all arguments are {@code null}.
   * 
   * @param hostName
   * @param ok
   * @param warning
   * @param critical
   * @param unknown
   * @param maintenance
   */
  public HostAlertSummaryDTO(String hostName, Number ok, Number warning, Number critical, Number unknown, Number maintenance) {
    super(ok, warning, critical, unknown, maintenance);
    this.setHostName(hostName);
  }

  /**
   * Provide host name for this alerts summary
   * 
   * @return
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Set host name for this alerts summary
   * 
   * @param hostName
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

}
