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

package org.apache.ambari.scom;

import org.apache.ambari.server.controller.spi.SystemException;

public class TestHostInfoProvider implements HostInfoProvider {

  private String clusterName;
  private String componentName;
  private String hostId;

  public String getClusterName() {
    return clusterName;
  }

  public String getComponentName() {
    return componentName;
  }

  public String getHostId() {
    return hostId;
  }

  @Override
  public String getHostName(String clusterName, String componentName) throws SystemException {
    this.clusterName = clusterName;
    this.componentName = componentName;
    return "host1";
  }

  @Override
  public String getHostName(String id) throws SystemException {
    this.hostId = id;
    return "host1";
  }

  @Override
  public String getHostAddress(String id) throws SystemException {
    return "127.0.0.1";
  }
}