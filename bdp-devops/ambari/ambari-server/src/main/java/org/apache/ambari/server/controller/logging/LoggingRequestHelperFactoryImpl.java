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

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


public class LoggingRequestHelperFactoryImpl implements LoggingRequestHelperFactory {

  private static final Logger LOG = Logger.getLogger(LoggingRequestHelperFactoryImpl.class);

  private static final String LOGSEARCH_PROPERTIES_CONFIG_TYPE_NAME = "logsearch-properties";

  private static final String LOGSEARCH_SERVICE_NAME = "LOGSEARCH";

  private static final String LOGSEARCH_SERVER_COMPONENT_NAME = "LOGSEARCH_SERVER";

  private static final String LOGSEARCH_HTTP_PORT_PROPERTY_NAME = "logsearch.http.port";

  private static final String LOGSEARCH_HTTPS_PORT_PROPERTY_NAME = "logsearch.https.port";

  private static final String LOGSEARCH_UI_PROTOCOL = "logsearch.protocol";

  private static final String LOGSEARCH_HTTPS_PROTOCOL_VALUE = "https";

  @Inject
  private Configuration ambariServerConfiguration;

  @Override
  public LoggingRequestHelper getHelper(AmbariManagementController ambariManagementController, String clusterName) {

    if (ambariServerConfiguration == null) {
      LOG.error("Ambari Server configuration object not available, cannot create request helper");
      return null;
    }

    Clusters clusters =
      ambariManagementController.getClusters();

    try {
      final LoggingRequestHelperImpl loggingRequestHelper;
      Cluster cluster = clusters.getCluster(clusterName);
      if (cluster != null) {
        final String logSearchHostName;
        final String logSearchPortNumber;
        final String logSearchProtocol;
        if (StringUtils.isNotBlank(ambariServerConfiguration.getLogSearchPortalExternalAddress())) {
          loggingRequestHelper = new LoggingRequestHelperImpl(null, null, null,
            ambariManagementController.getCredentialStoreService(), cluster, ambariServerConfiguration.getLogSearchPortalExternalAddress());
        } else {
          boolean isLogSearchEnabled =
            cluster.getServices().containsKey(LOGSEARCH_SERVICE_NAME);

          if (!isLogSearchEnabled) {
            // log search not enabled, just return null, since no helper impl is necessary
            return null;
          }

          Config logSearchEnvConfig =
            cluster.getDesiredConfigByType(LOGSEARCH_PROPERTIES_CONFIG_TYPE_NAME);

          List<ServiceComponentHost> listOfMatchingHosts =
            cluster.getServiceComponentHosts(LOGSEARCH_SERVICE_NAME, LOGSEARCH_SERVER_COMPONENT_NAME);


          if (listOfMatchingHosts.size() == 0) {
            LOG.warn("No matching LOGSEARCH_SERVER instances found, this may indicate a deployment problem.  Please verify that LogSearch is deployed and running.");
            return null;
          }

          if (listOfMatchingHosts.size() > 1) {
            LOG.warn("More than one LOGSEARCH_SERVER instance found, this may be a deployment error.  Only the first LOGSEARCH_SERVER instance will be used.");
          }

          ServiceComponentHost serviceComponentHost =
            listOfMatchingHosts.get(0);

          if (serviceComponentHost.getState() != State.STARTED) {
            // if the LOGSEARCH_SERVER is not started, don't attempt to connect
            return null;
          }

          logSearchHostName = serviceComponentHost.getHostName();

          logSearchProtocol =
            logSearchEnvConfig.getProperties().get(LOGSEARCH_UI_PROTOCOL);

          logSearchPortNumber = LOGSEARCH_HTTPS_PROTOCOL_VALUE.equalsIgnoreCase(logSearchProtocol)
            ? logSearchEnvConfig.getProperties().get(LOGSEARCH_HTTPS_PORT_PROPERTY_NAME)
            : logSearchEnvConfig.getProperties().get(LOGSEARCH_HTTP_PORT_PROPERTY_NAME);

          loggingRequestHelper = new LoggingRequestHelperImpl(logSearchHostName, logSearchPortNumber, logSearchProtocol,
            ambariManagementController.getCredentialStoreService(), cluster, null);
        }
        // set configured timeouts for the Ambari connection to the LogSearch Portal service
        loggingRequestHelper.setLogSearchConnectTimeoutInMilliseconds(ambariServerConfiguration.getLogSearchPortalConnectTimeout());
        loggingRequestHelper.setLogSearchReadTimeoutInMilliseconds(ambariServerConfiguration.getLogSearchPortalReadTimeout());
        return loggingRequestHelper;
      }
    } catch (AmbariException ambariException) {
      LOG.error("Error occurred while trying to obtain the cluster, cluster name = " + clusterName, ambariException);
    }


    return null;
  }

  /**
   * Package-level setter to facilitate simpler unit testing
   *
   * @param ambariServerConfiguration the Ambari Server configuration properties
   */
  void setAmbariServerConfiguration(Configuration ambariServerConfiguration) {
    this.ambariServerConfiguration = ambariServerConfiguration;
  }
}
