/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.Map;

import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.state.Cluster;

/**
 * Resource manager specific HTTP property request.
 */
public class ResourceManagerHttpPropertyRequest extends JsonHttpPropertyRequest {

  private static final String CONFIG_YARN_SITE = "yarn-site";
  private static final String CONFIG_CORE_SITE = "core-site";

  private static final String PROPERTY_YARN_HTTP_POLICY = "yarn.http.policy";
  private static final String PROPERTY_HADOOP_SSL_ENABLED = "hadoop.ssl.enabled";
  private static final String PROPERTY_YARN_HTTP_POLICY_VALUE_HTTPS_ONLY = "HTTPS_ONLY";
  private static final String PROPERTY_HADOOP_SSL_ENABLED_VALUE_TRUE = "true";

  // resource manager properties
  private static final String PROPERTY_WEBAPP_ADDRESS = "yarn.resourcemanager.webapp.address";
  private static final String PROPERTY_WEBAPP_HTTPS_ADDRESS = "yarn.resourcemanager.webapp.https.address";

  // resource manager HA properties
  private static final String PROPERTY_HA_RM_IDS = "yarn.resourcemanager.ha.rm-ids";
  private static final String PROPERTY_HOSTNAME_TEMPLATE = "yarn.resourcemanager.hostname.%s";
  private static final String PROPERTY_WEBAPP_ADDRESS_TEMPLATE = "yarn.resourcemanager.webapp.address.%s";
  private static final String PROPERTY_WEBAPP_HTTPS_ADDRESS_TEMPLATE = "yarn.resourcemanager.webapp.https.address.%s";

  private static final String URL_TEMPLATE = "%s://%s:%s/ws/v1/cluster/info";

  private static final Map<String, String> PROPERTY_MAPPINGS =
      Collections.singletonMap("clusterInfo/haState", "HostRoles/ha_state");


  // ----- Constructors ----------------------------------------------------

  public ResourceManagerHttpPropertyRequest() {
    super(PROPERTY_MAPPINGS);
  }


  // ----- PropertyRequest -------------------------------------------------

  @Override
  public String getUrl(Cluster cluster, String hostName)
      throws SystemException {
    Map<String, String> yarnConfig = cluster.getDesiredConfigByType(CONFIG_YARN_SITE).getProperties();
    Map<String, String> coreConfig = cluster.getDesiredConfigByType(CONFIG_CORE_SITE).getProperties();

    String yarnHttpPolicy = yarnConfig.get(PROPERTY_YARN_HTTP_POLICY);
    String hadoopSslEnabled = coreConfig.get(PROPERTY_HADOOP_SSL_ENABLED);

    boolean useHttps =
        (yarnHttpPolicy != null && yarnHttpPolicy.equals(PROPERTY_YARN_HTTP_POLICY_VALUE_HTTPS_ONLY)) ||
            (hadoopSslEnabled != null && hadoopSslEnabled.equals(PROPERTY_HADOOP_SSL_ENABLED_VALUE_TRUE));

    return String.format(URL_TEMPLATE, getProtocol(useHttps), hostName, getPort(hostName, yarnConfig, useHttps));
  }


  // ----- helper methods --------------------------------------------------

  // get the protocal - http or https
  private String getProtocol(boolean useHttps) {
    return useHttps ? "https" : "http";
  }

  // get the port for the HTTP request from the given config
  private String getPort(String hostName, Map<String, String> yarnConfig, boolean useHttps) {
    if (yarnConfig.containsKey(PROPERTY_HA_RM_IDS)) {
      // ha mode
      String rmId = getConfigResourceManagerId(yarnConfig, hostName);
      return useHttps ?
          getConfigPortValue(yarnConfig, String.format(PROPERTY_WEBAPP_HTTPS_ADDRESS_TEMPLATE, rmId), "8090") :
          getConfigPortValue(yarnConfig, String.format(PROPERTY_WEBAPP_ADDRESS_TEMPLATE, rmId), "8088");
    }
    //non ha mode
    return useHttps ?
        getConfigPortValue(yarnConfig, PROPERTY_WEBAPP_HTTPS_ADDRESS, "8090") :
        getConfigPortValue(yarnConfig, PROPERTY_WEBAPP_ADDRESS, "8088");
  }

  // get the resource manager id from the given config
  private String getConfigResourceManagerId(Map<String, String> yarnConfig, String hostName) {

    for (String id : yarnConfig.get(PROPERTY_HA_RM_IDS).split(",")) {

      String hostNameProperty = String.format(PROPERTY_HOSTNAME_TEMPLATE, id);
      String hostNameById = yarnConfig.get(hostNameProperty);

      if (hostNameById.equals(hostName)) {
        return id;
      }
    }
    return null;
  }

  // get the port for the HTTP request by splitting off of the property specifed from the given config
  private String getConfigPortValue(Map<String, String> yarnConfig, String property, String defaultValue) {
    return yarnConfig.containsKey(property) ? yarnConfig.get(property).split(":")[1] : defaultValue;
  }
}
