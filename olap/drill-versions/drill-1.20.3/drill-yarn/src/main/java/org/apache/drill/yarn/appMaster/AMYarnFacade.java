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
package org.apache.drill.yarn.appMaster;

import java.util.List;

import org.apache.drill.yarn.core.ContainerRequestSpec;
import org.apache.drill.yarn.core.LaunchSpec;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;

/**
 * Defines the interface between the Application Master and YARN. This interface
 * enables the use of a mock implementation for testing as well as the actual
 * implementation that works with YARN.
 */

public interface AMYarnFacade {
  /**
   * Provides a collection of web UI links for the YARN Resource Manager and the
   * Node Manager that is running the Drill-on-YARN AM. This information is
   * primarily for use in the AM's own web UI.
   */

  public static class YarnAppHostReport {
    public String appId;
    public String amHost;
    public String rmHost;
    public String rmUrl;
    public String rmAppUrl;
    public String nmHost;
    public String nmUrl;
    public String nmAppUrl;
  }

  void start(AMRMClientAsync.CallbackHandler resourceCallback,
      NMClientAsync.CallbackHandler nodeCallback);

  void register(String trackingUrl) throws YarnFacadeException;

  String getTrackingUrl();

  ContainerRequest requestContainer(ContainerRequestSpec containerSpec);

  void removeContainerRequest(ContainerRequest containerRequest);

  void launchContainer(Container container, LaunchSpec taskSpec)
      throws YarnFacadeException;

  void finish(boolean success, String msg) throws YarnFacadeException;

  void releaseContainer(Container container);

  void killContainer(Container container);

  int getNodeCount();

  Resource getResources();

  RegisterApplicationMasterResponse getRegistrationResponse();

  void blacklistNode(String nodeName);

  void removeBlacklist(String nodeName);

  List<NodeReport> getNodeReports() throws YarnFacadeException;

  YarnAppHostReport getAppHostReport();

  boolean supportsDiskResource();
}