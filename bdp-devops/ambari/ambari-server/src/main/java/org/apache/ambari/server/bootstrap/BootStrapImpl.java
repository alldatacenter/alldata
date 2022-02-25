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

package org.apache.ambari.server.bootstrap;

import static org.apache.ambari.server.utils.VersionUtils.DEV_VERSION;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.bootstrap.BSResponse.BSRunStat;
import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BootStrapImpl {
  private File bootStrapDir;
  private String bootScript;
  private String bootSetupAgentScript;
  private String bootSetupAgentPassword;
  private BSRunner bsRunner;
  private String masterHostname;
  long timeout;

  private static final Logger LOG = LoggerFactory.getLogger(BootStrapImpl.class);

  /* Monotonically increasing requestid for the bootstrap api to query on */
  int requestId = 0;
  private FifoLinkedHashMap<Long, BootStrapStatus> bsStatus;
  private final String clusterOsType;
  private final String clusterOsFamily;
  private String projectVersion;
  private int serverPort;

  @Inject
  public BootStrapImpl(Configuration conf, AmbariMetaInfo ambariMetaInfo) throws IOException {
    bootStrapDir = conf.getBootStrapDir();
    bootScript = conf.getBootStrapScript();
    bootSetupAgentScript = conf.getBootSetupAgentScript();
    bootSetupAgentPassword = conf.getBootSetupAgentPassword();
    bsStatus = new FifoLinkedHashMap<>();
    masterHostname = conf.getMasterHostname(
        InetAddress.getLocalHost().getCanonicalHostName());
    clusterOsType = conf.getServerOsType();
    clusterOsFamily = conf.getServerOsFamily();
    projectVersion = ambariMetaInfo.getServerVersion();
    projectVersion = (projectVersion.equals(DEV_VERSION)) ? DEV_VERSION.replace("$", "") : projectVersion;
    serverPort = (conf.getApiSSLAuthentication())? conf.getClientSSLApiPort() : conf.getClientApiPort();
  }

  /**
   * Return {@link BootStrapStatus} for a given responseId.
   * @param requestId the responseId for which the status needs to be returned.
   * @return status for a specific response id. A response Id of -1 means the
   * latest responseId.
   */
  public synchronized BootStrapStatus getStatus(long requestId) {
    if (! bsStatus.containsKey(Long.valueOf(requestId))) {
      return null;
    }
    return bsStatus.get(Long.valueOf(requestId));
  }

  /**
   * update status of a request. Mostly called by the status collector thread.
   * @param requestId the request id.
   * @param status the status of the update.
   */
  synchronized void updateStatus(long requestId, BootStrapStatus status) {
    bsStatus.put(Long.valueOf(requestId), status);
  }


  public synchronized void init() throws IOException {
    if (!bootStrapDir.exists()) {
      boolean mkdirs = bootStrapDir.mkdirs();
      if (!mkdirs) {
        throw new IOException("Unable to make directory for " +
            "bootstrap " + bootStrapDir);
      }
    }
  }

  public  synchronized BSResponse runBootStrap(SshHostInfo info) {
    BSResponse response = new BSResponse();
    /* Run some checks for ssh host */
    LOG.info("BootStrapping hosts " + info.hostListAsString());
    if (bsRunner != null) {
      response.setLog("BootStrap in Progress: Cannot Run more than one.");
      response.setStatus(BSRunStat.ERROR);

      return response;
    }
    requestId++;

    if (info.getHosts() == null || info.getHosts().isEmpty()) {
      BootStrapStatus status = new BootStrapStatus();
      status.setLog("Host list is empty.");
      status.setHostsStatus(new ArrayList<>());
      status.setStatus(BootStrapStatus.BSStat.ERROR);
      updateStatus(requestId, status);

      response.setStatus(BSRunStat.OK);
      response.setLog("Host list is empty.");
      response.setRequestId(requestId);
      return response;
    } else {
      bsRunner = new BSRunner(this, info, bootStrapDir.toString(),
          bootScript, bootSetupAgentScript, bootSetupAgentPassword, requestId, 0L,
          masterHostname, info.isVerbose(), clusterOsFamily, projectVersion, serverPort);
      bsRunner.start();
      response.setStatus(BSRunStat.OK);
      response.setLog("Running Bootstrap now.");
      response.setRequestId(requestId);
      return response;
    }
  }

  /**
   * @param hosts
   * @return
   */
  public synchronized List<BSHostStatus> getHostInfo(List<String> hosts) {
    List<BSHostStatus> statuses = new ArrayList<>();

    if (null == hosts || 0 == hosts.size() || (hosts.size() == 1 && hosts.get(0).equals("*"))) {
      for (BootStrapStatus status : bsStatus.values()) {
        if (null != status.getHostsStatus()) {
          statuses.addAll(status.getHostsStatus());
        }
      }
    } else {
      // TODO make bootstrapping a bit more robust then stop looping
      for (BootStrapStatus status : bsStatus.values()) {
        for (BSHostStatus hostStatus : status.getHostsStatus()) {
          if (-1 != hosts.indexOf(hostStatus.getHostName())) {
            statuses.add(hostStatus);
          }
        }
      }
    }

    return statuses;
  }

  /**
   *
   */
  public synchronized void reset() {
    bsRunner = null;
  }

}
