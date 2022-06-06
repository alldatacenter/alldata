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

package org.apache.ambari.server.controller.utilities;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;


/*This class should not be used anymore
* now we will use DatabaseConsistencyChecker*/
public class DatabaseChecker {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseChecker.class);

  @Inject
  static Injector injector;
  static AmbariMetaInfo ambariMetaInfo;
  static MetainfoDAO metainfoDAO;

  public static void checkDBConsistency() throws AmbariException {
    LOG.info("Checking DB consistency");

    boolean checkPassed = true;
    if (ambariMetaInfo == null) {
      ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    }

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    List<ClusterEntity> clusters = clusterDAO.findAll();
    for (ClusterEntity clusterEntity: clusters) {
      StackId stackId = new StackId(clusterEntity.getDesiredStack());

      Collection<ClusterServiceEntity> serviceEntities =
        clusterEntity.getClusterServiceEntities();
      for (ClusterServiceEntity clusterServiceEntity : serviceEntities) {

        ServiceDesiredStateEntity serviceDesiredStateEntity =
          clusterServiceEntity.getServiceDesiredStateEntity();
        if (serviceDesiredStateEntity == null) {
          checkPassed = false;
          LOG.error(String.format("ServiceDesiredStateEntity is null for " +
              "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s ",
            clusterEntity.getClusterName(), clusterServiceEntity.getServiceName()));
        }
        Collection<ServiceComponentDesiredStateEntity> scDesiredStateEntities =
          clusterServiceEntity.getServiceComponentDesiredStateEntities();
        if (scDesiredStateEntities == null ||
          scDesiredStateEntities.isEmpty()) {
          checkPassed = false;
          LOG.error(String.format("serviceComponentDesiredStateEntities is null or empty for " +
              "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s ",
            clusterEntity.getClusterName(), clusterServiceEntity.getServiceName()));
        } else {
          for (ServiceComponentDesiredStateEntity scDesiredStateEnity : scDesiredStateEntities) {

            Collection<HostComponentDesiredStateEntity> schDesiredStateEntities =
              scDesiredStateEnity.getHostComponentDesiredStateEntities();
            Collection<HostComponentStateEntity> schStateEntities =
              scDesiredStateEnity.getHostComponentStateEntities();

            ComponentInfo componentInfo = ambariMetaInfo.getComponent(
              stackId.getStackName(), stackId.getStackVersion(),
              scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName());

            boolean zeroCardinality = componentInfo.getCardinality() == null
              || componentInfo.getCardinality().startsWith("0")
              || scDesiredStateEnity.getComponentName().equals("SECONDARY_NAMENODE"); // cardinality 0 for NameNode HA

            boolean componentCheckFailed = false;

            if (schDesiredStateEntities == null) {
              componentCheckFailed = true;
              LOG.error(String.format("hostComponentDesiredStateEntities is null for " +
                  "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s, componentName=%s ",
                clusterEntity.getClusterName(), scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName()));
            } else if (!zeroCardinality && schDesiredStateEntities.isEmpty()) {
              componentCheckFailed = true;
              LOG.error(String.format("hostComponentDesiredStateEntities is empty for " +
                  "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s, componentName=%s ",
                clusterEntity.getClusterName(), scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName()));
            }

            if (schStateEntities == null) {
              componentCheckFailed = true;
              LOG.error(String.format("hostComponentStateEntities is null for " +
                  "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s, componentName=%s ",
                clusterEntity.getClusterName(), scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName()));
            } else if (!zeroCardinality && schStateEntities.isEmpty()) {
              componentCheckFailed = true;
              LOG.error(String.format("hostComponentStateEntities is empty for " +
                  "ServiceComponentDesiredStateEntity, clusterName=%s, serviceName=%s, componentName=%s ",
                clusterEntity.getClusterName(), scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName()));
            }

            if (!componentCheckFailed &&
              schDesiredStateEntities.size() != schStateEntities.size()) {
              checkPassed = false;
              LOG.error(String.format("HostComponentStateEntities and HostComponentDesiredStateEntities " +
                  "tables must contain equal number of rows mapped to ServiceComponentDesiredStateEntity, " +
                  "(clusterName=%s, serviceName=%s, componentName=%s) ", clusterEntity.getClusterName(),
                scDesiredStateEnity.getServiceName(), scDesiredStateEnity.getComponentName()));
            }
            checkPassed = checkPassed && !componentCheckFailed;
          }
        }
      }
    }
    if (checkPassed) {
      LOG.info("DB consistency check passed.");
    } else {
      String errorMessage = "DB consistency check failed. Run \"ambari-server start --skip-database-validation\" to skip validation.";
      LOG.error(errorMessage);
      throw new AmbariException(errorMessage);
    }
  }

  private static boolean clusterConfigsContainTypeAndTag(Collection<ClusterConfigEntity> clusterConfigEntities,
                                                         String typeName, String tag) {
    for (ClusterConfigEntity clusterConfigEntity : clusterConfigEntities) {
      if (typeName.equals(clusterConfigEntity.getType()) && tag.equals(clusterConfigEntity.getTag())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Validates configuration consistency by ensuring that configuration types
   * only have a single entry in {@link ClusterConfigEntity} which is enabled.
   * Additionally will check to ensure that every installed service's
   * configurations are present in the configuration table.
   *
   * @throws AmbariException
   *           if check failed
   * @see ClusterConfigEntity#isSelected()
   */
  public static void checkDBConfigsConsistency() throws AmbariException {
    LOG.info("Checking DB configs consistency");

    boolean checkPassed = true;

    if (ambariMetaInfo == null) {
      ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    }

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    List<ClusterEntity> clusters = clusterDAO.findAll();
    if (clusters != null) {
      for (ClusterEntity clusterEntity : clusters) {
        Collection<ClusterConfigEntity> clusterConfigEntities = clusterEntity.getClusterConfigEntities();
        if (null == clusterConfigEntities) {
          return;
        }

        Map<String, Integer> selectedCountForType = new HashMap<>();
        for (ClusterConfigEntity configEntity : clusterConfigEntities) {
          String typeName = configEntity.getType();
          if (configEntity.isSelected()) {
            int selectedCount = selectedCountForType.get(typeName) != null
                ? selectedCountForType.get(typeName) : 0;
            selectedCountForType.put(typeName, selectedCount + 1);
          } else {
            if (!selectedCountForType.containsKey(typeName)) {
              selectedCountForType.put(typeName, 0);
            }
          }
        }

        // Check that every config type from stack is present
        Collection<ClusterServiceEntity> clusterServiceEntities = clusterEntity.getClusterServiceEntities();
        ClusterStateEntity clusterStateEntity = clusterEntity.getClusterStateEntity();
        if (clusterStateEntity != null) {
          StackEntity currentStack = clusterStateEntity.getCurrentStack();
          StackInfo stack = ambariMetaInfo.getStack(currentStack.getStackName(),
              currentStack.getStackVersion());

          for (ClusterServiceEntity clusterServiceEntity : clusterServiceEntities) {
            if (!State.INIT.equals(
                clusterServiceEntity.getServiceDesiredStateEntity().getDesiredState())) {
              String serviceName = clusterServiceEntity.getServiceName();
              ServiceInfo serviceInfo = ambariMetaInfo.getService(stack.getName(),
                  stack.getVersion(), serviceName);

              for (String configTypeName : serviceInfo.getConfigTypeAttributes().keySet()) {
                if (selectedCountForType.get(configTypeName) == null) {
                  checkPassed = false;
                  LOG.error("Configuration {} is missing for service {}", configTypeName,
                      serviceName);
                } else {
                  // Check that for each config type exactly one is selected
                  if (selectedCountForType.get(configTypeName) == 0) {
                    checkPassed = false;
                    LOG.error("Configuration {} has no enabled entries for service {}",
                        configTypeName, serviceName);
                  } else if (selectedCountForType.get(configTypeName) > 1) {
                    checkPassed = false;
                    LOG.error("Configuration {} has more than 1 enabled entry for service {}",
                        configTypeName, serviceName);
                  }
                }
              }
            }
          }
        }
      }
    }

    if (checkPassed) {
      LOG.info("DB configs consistency check passed.");
    } else {
      String errorMessage = "DB configs consistency check failed. Run \"ambari-server start --skip-database-validation\" to skip validation.";
      LOG.error(errorMessage);
      throw new AmbariException(errorMessage);
    }
  }

  public static void checkDBVersion() throws AmbariException {

    LOG.info("Checking DB store version");
    if (metainfoDAO == null) {
      metainfoDAO = injector.getInstance(MetainfoDAO.class);
    }

    MetainfoEntity schemaVersionEntity = metainfoDAO.findByKey(Configuration.SERVER_VERSION_KEY);
    String schemaVersion = null;

    if (schemaVersionEntity != null) {
      schemaVersion = schemaVersionEntity.getMetainfoValue();
    }

    Configuration conf = injector.getInstance(Configuration.class);
    File versionFile = new File(conf.getServerVersionFilePath());
    if (!versionFile.exists()) {
      throw new AmbariException("Server version file does not exist.");
    }
    String serverVersion = null;
    try (Scanner scanner = new Scanner(versionFile)) {
      serverVersion = scanner.useDelimiter("\\Z").next();

    } catch (IOException ioe) {
      throw new AmbariException("Unable to read server version file.");
    }

    if (schemaVersionEntity==null || VersionUtils.compareVersions(schemaVersion, serverVersion, 3) != 0) {
      String error = "Current database store version is not compatible with " +
        "current server version"
        + ", serverVersion=" + serverVersion
        + ", schemaVersion=" + schemaVersion;
      LOG.warn(error);
      throw new AmbariException(error);
    }

    LOG.info("DB store version is compatible");
  }


}
