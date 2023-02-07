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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.drill.yarn.core.ContainerRequestSpec;
import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.apache.drill.yarn.core.LaunchSpec;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync.CallbackHandler;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.proto.YarnServiceProtos.SchedulerResourceTypes;
import org.apache.hadoop.yarn.util.ConverterUtils;

/**
 * Wrapper around the asynchronous versions of the YARN AM-RM and AM-NM
 * interfaces. Allows strategy code to factor out the YARN-specific bits so that
 * strategy code is simpler. Also allows replacing the actual YARN code with a
 * mock for unit testing.
 */

public class AMYarnFacadeImpl implements AMYarnFacade {
  private static final Log LOG = LogFactory.getLog(AMYarnFacadeImpl.class);

  private YarnConfiguration conf;
  private AMRMClientAsync<ContainerRequest> resourceMgr;
  private NMClientAsync nodeMgr;
  private RegisterApplicationMasterResponse registration;
  private YarnClient client;
  private int pollPeriodMs;

  private String appMasterTrackingUrl;

  private ApplicationId appId;

  private ApplicationReport appReport;

  private String amHost;

  private boolean supportsDisks;

  public AMYarnFacadeImpl(int pollPeriodMs) {
    this.pollPeriodMs = pollPeriodMs;
  }

  @Override
  public void start(CallbackHandler resourceCallback,
      org.apache.hadoop.yarn.client.api.async.NMClientAsync.CallbackHandler nodeCallback ) {

    conf = new YarnConfiguration();

    resourceMgr = AMRMClientAsync.createAMRMClientAsync(pollPeriodMs, resourceCallback);
    resourceMgr.init(conf);
    resourceMgr.start();

    // Create the asynchronous node manager client

    nodeMgr = NMClientAsync.createNMClientAsync(nodeCallback);
    nodeMgr.init(conf);
    nodeMgr.start();

    client = YarnClient.createYarnClient();
    client.init(conf);
    client.start();

    String appIdStr = System.getenv(DrillOnYarnConfig.APP_ID_ENV_VAR);
    if (appIdStr != null) {
      appId = ConverterUtils.toApplicationId(appIdStr);
      try {
        appReport = client.getApplicationReport(appId);
      } catch (YarnException | IOException e) {
        LOG.error(
            "Failed to get YARN applicaiton report for App ID: " + appIdStr, e);
      }
    }
  }

  @Override
  public void register(String trackingUrl) throws YarnFacadeException {
    String thisHostName = NetUtils.getHostname();
    LOG.debug("Host Name from YARN: " + thisHostName);
    if (trackingUrl != null) {
      // YARN seems to provide multiple names: MACHNAME.local/10.250.56.235
      // The second seems to be the IP address, which is what we want.
      String names[] = thisHostName.split("/");
      amHost = names[names.length - 1];
      appMasterTrackingUrl = trackingUrl.replace("<host>", amHost);
      LOG.info("Tracking URL: " + appMasterTrackingUrl);
    }
    try {
      LOG.trace("Registering with YARN");
      registration = resourceMgr.registerApplicationMaster(thisHostName, 0,
          appMasterTrackingUrl);
    } catch (YarnException | IOException e) {
      throw new YarnFacadeException("Register AM failed", e);
    }

    // Some distributions (but not the stock YARN) support Disk
    // resources. Since Drill compiles against Apache YARN, without disk
    // resources, we have to use an indirect mechnanism to look for the
    // disk enum at runtime when we don't have that enum value at compile time.

    for (SchedulerResourceTypes type : registration
        .getSchedulerResourceTypes()) {
      if (type.name().equals("DISK")) {
        supportsDisks = true;
      }
    }
  }

  @Override
  public String getTrackingUrl( ) { return appMasterTrackingUrl; }

  @Override
  public boolean supportsDiskResource( ) { return supportsDisks; }

  @Override
  public ContainerRequest requestContainer(ContainerRequestSpec containerSpec) {
    ContainerRequest request = containerSpec.makeRequest();
    resourceMgr.addContainerRequest(containerSpec.makeRequest());
    return request;
  }

  @Override
  public void launchContainer(Container container, LaunchSpec taskSpec)
      throws YarnFacadeException {
    ContainerLaunchContext context = createLaunchContext(taskSpec);
    startContainerAsync(container, context);
  }

  private ContainerLaunchContext createLaunchContext(LaunchSpec task)
      throws YarnFacadeException {
    try {
      return task.createLaunchContext(conf);
    } catch (IOException e) {
      throw new YarnFacadeException("Failed to create launch context", e);
    }
  }

  private void startContainerAsync(Container container,
      ContainerLaunchContext context) {
    nodeMgr.startContainerAsync(container, context);
  }

  @Override
  public void finish(boolean succeeded, String msg) throws YarnFacadeException {
    // Stop the Node Manager client.

    nodeMgr.stop();

    // Deregister the app from YARN.

    String appMsg = "Drill Cluster Shut-Down";
    FinalApplicationStatus status = FinalApplicationStatus.SUCCEEDED;
    if (!succeeded) {
      appMsg = "Drill Cluster Fatal Error - check logs";
      status = FinalApplicationStatus.FAILED;
    }
    if (msg != null) {
      appMsg = msg;
    }
    try {
      resourceMgr.unregisterApplicationMaster(status, appMsg, "");
    } catch (YarnException | IOException e) {
      throw new YarnFacadeException("Deregister AM failed", e);
    }

    // Stop the Resource Manager client

    resourceMgr.stop();
  }

  @Override
  public void releaseContainer(Container container) {
    resourceMgr.releaseAssignedContainer(container.getId());
  }

  @Override
  public void killContainer(Container container) {
    nodeMgr.stopContainerAsync(container.getId(), container.getNodeId());
  }

  @Override
  public int getNodeCount() {
    return resourceMgr.getClusterNodeCount();
  }

  @Override
  public Resource getResources() {
    return resourceMgr.getAvailableResources();
  }

  @Override
  public void removeContainerRequest(ContainerRequest containerRequest) {
    resourceMgr.removeContainerRequest(containerRequest);
  }

  @Override
  public RegisterApplicationMasterResponse getRegistrationResponse() {
    return registration;
  }

  @Override
  public void blacklistNode(String nodeName) {
    resourceMgr.updateBlacklist(Collections.singletonList(nodeName), null);
  }

  @Override
  public void removeBlacklist(String nodeName) {
    resourceMgr.updateBlacklist(null, Collections.singletonList(nodeName));
  }

  @Override
  public List<NodeReport> getNodeReports() throws YarnFacadeException {
    try {
      return client.getNodeReports(NodeState.RUNNING);
    } catch (Exception e) {
      throw new YarnFacadeException("getNodeReports failed", e);
    }
  }

  @Override
  public YarnAppHostReport getAppHostReport() {
    // Cobble together YARN links to simplify debugging.

    YarnAppHostReport hostRpt = new YarnAppHostReport();
    hostRpt.amHost = amHost;
    if (appId != null) {
      hostRpt.appId = appId.toString();
    }
    if (appReport == null) {
      return hostRpt;
    }
    try {
      String rmLink = appReport.getTrackingUrl();
      URL url = new URL(rmLink);
      hostRpt.rmHost = url.getHost();
      hostRpt.rmUrl = "http://" + hostRpt.rmHost + ":" + url.getPort() + "/";
      hostRpt.rmAppUrl = hostRpt.rmUrl + "cluster/app/" + appId.toString();
    } catch (MalformedURLException e) {
      return null;
    }

    hostRpt.nmHost = System.getenv("NM_HOST");
    String nmPort = System.getenv("NM_HTTP_PORT");
    if (hostRpt.nmHost != null || nmPort != null) {
      hostRpt.nmUrl = "http://" + hostRpt.nmHost + ":" + nmPort + "/";
      hostRpt.nmAppUrl = hostRpt.nmUrl + "node/application/" + hostRpt.appId;
    }
    return hostRpt;
  }
}