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
package org.apache.ambari.server.upgrade;

import java.sql.SQLException;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link UpgradeCatalog261} upgrades Ambari from 2.6.0 to 2.6.1.
 */
public class UpgradeCatalog261 extends AbstractUpgradeCatalog {
  private static final String CORE_SITE = "core-site";
  private static final String COMPRESSION_CODECS_PROPERTY = "io.compression.codecs";
  private static final String COMPRESSION_CODECS_LZO = "com.hadoop.compression.lzo";
  private static final String LZO_ENABLED_JSON_KEY = "lzo_enabled";

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog261.class);

  /**
   * Constructor.
   *
   * @param injector
   */
  @Inject
  public UpgradeCatalog261(Injector injector) {
    super(injector);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
      return "2.6.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
      return "2.6.1";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    // TODO: make this map with clusterids as keys
    this.getUpgradeJsonOutput().put(LZO_ENABLED_JSON_KEY, isLzoEnabled().toString());
  }

  protected Boolean isLzoEnabled() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {

          Config coreSite = cluster.getDesiredConfigByType(CORE_SITE);
          if (coreSite != null) {
            Map<String, String> coreSiteProperties = coreSite.getProperties();

            if (coreSiteProperties.containsKey(COMPRESSION_CODECS_PROPERTY)) {
              String compressionCodecs = coreSiteProperties.get(COMPRESSION_CODECS_PROPERTY);

              if(compressionCodecs.contains(COMPRESSION_CODECS_LZO)) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }
}
