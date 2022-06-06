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

package org.apache.ambari.server.upgrade;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Final upgrade catalog which simply updates database version (in case if no db changes between releases)
 */
public class FinalUpgradeCatalog extends AbstractFinalUpgradeCatalog {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(FinalUpgradeCatalog.class);

  @Inject
  public FinalUpgradeCatalog(Injector injector) {
    super(injector);
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    updateClusterEnv();
  }

  /**
   * Updates {@code cluster-env} in the following ways:
   * <ul>
   * <li>Adds/Updates {@link ConfigHelper#CLUSTER_ENV_STACK_FEATURES_PROPERTY} from stack</li>
   * <li>Adds/Updates {@link ConfigHelper#CLUSTER_ENV_STACK_TOOLS_PROPERTY} from stack</li>
   * <li>Adds/Updates {@link ConfigHelper#CLUSTER_ENV_STACK_PACKAGES_PROPERTY} from stack</li>
   * </ul>
   *
   * Note: Config properties stack_features and stack_tools should always be updated to latest values as defined
   * in the stack on an Ambari upgrade.
   */
  protected void updateClusterEnv() throws AmbariException {

    AmbariManagementController ambariManagementController = injector.getInstance(
        AmbariManagementController.class);
    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);

    LOG.info("Updating stack_features and stack_tools config properties.");
    Clusters clusters = ambariManagementController.getClusters();
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {

      Set<StackId> stackIds = new HashSet<>();
      for (Service service : cluster.getServices().values()) {
        stackIds.add(service.getDesiredStackId());
      }

      for (StackId stackId : stackIds) {
        Map<String, String> propertyMap = new HashMap<>();
        StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
        List<PropertyInfo> properties = stackInfo.getProperties();
        for(PropertyInfo property : properties) {
          if(property.getName().equals(ConfigHelper.CLUSTER_ENV_STACK_FEATURES_PROPERTY) ||
              property.getName().equals(ConfigHelper.CLUSTER_ENV_STACK_TOOLS_PROPERTY) ||
              property.getName().equals(ConfigHelper.CLUSTER_ENV_STACK_PACKAGES_PROPERTY)) {
            propertyMap.put(property.getName(), property.getValue());
          }
        }
        updateConfigurationPropertiesForCluster(cluster, ConfigHelper.CLUSTER_ENV, propertyMap, true, true);
      }
    }
  }

}
