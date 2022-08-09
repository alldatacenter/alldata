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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.serveraction.kerberos.DeconstructedPrincipal;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link org.apache.ambari.server.upgrade.UpgradeCatalog252} upgrades Ambari from 2.5.1 to 2.5.2.
 */
public class UpgradeCatalog252 extends AbstractUpgradeCatalog {

  static final String CLUSTERCONFIG_TABLE = "clusterconfig";
  static final String SERVICE_DELETED_COLUMN = "service_deleted";
  static final String UNMAPPED_COLUMN = "unmapped";

  private static final String UPGRADE_TABLE = "upgrade";
  private static final String UPGRADE_TABLE_FROM_REPO_COLUMN = "from_repo_version_id";
  private static final String UPGRADE_TABLE_TO_REPO_COLUMN = "to_repo_version_id";
  private static final String CLUSTERS_TABLE = "clusters";
  private static final String SERVICE_COMPONENT_HISTORY_TABLE = "servicecomponent_history";
  private static final String UPGRADE_GROUP_TABLE = "upgrade_group";
  private static final String UPGRADE_ITEM_TABLE = "upgrade_item";
  private static final String UPGRADE_ID_COLUMN = "upgrade_id";

  private static final String CLUSTER_ENV = "cluster-env";

  private static final String HIVE_ENV = "hive-env";
  private static final String MARIADB_REDHAT_SUPPORT = "mariadb_redhat_support";

  private static final List<String> configTypesToEnsureSelected = Arrays.asList("spark2-javaopts-properties");

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog252.class);

  /**
   * Constructor.
   *
   * @param injector
   */
  @Inject
  public UpgradeCatalog252(Injector injector) {
    super(injector);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.5.1";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.5.2";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    addServiceDeletedColumnToClusterConfigTable();
    addRepositoryColumnsToUpgradeTable();
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
    addNewConfigurationsFromXml();
    resetStackToolsAndFeatures();
    updateKerberosDescriptorArtifacts();
    fixLivySuperusers();
  }

  /**
   * Adds the {@value #SERVICE_DELETED_COLUMN} column to the
   * {@value #CLUSTERCONFIG_TABLE} table.
   *
   * @throws java.sql.SQLException
   */
  private void addServiceDeletedColumnToClusterConfigTable() throws SQLException {
    if (!dbAccessor.tableHasColumn(CLUSTERCONFIG_TABLE, UNMAPPED_COLUMN)) {
      dbAccessor.addColumn(CLUSTERCONFIG_TABLE,
          new DBColumnInfo(SERVICE_DELETED_COLUMN, Short.class, null, 0, false));
    }
  }

  /**
   * Changes the following columns to {@value #UPGRADE_TABLE}:
   * <ul>
   * <li>{@value #UPGRADE_TABLE_FROM_REPO_COLUMN}
   * <li>{@value #UPGRADE_TABLE_TO_REPO_COLUMN}
   * <li>Removes {@code to_version}
   * <li>Removes {@code from_version}
   * </ul>
   *
   * @throws SQLException
   */
  private void addRepositoryColumnsToUpgradeTable() throws SQLException {
    dbAccessor.clearTableColumn(CLUSTERS_TABLE, UPGRADE_ID_COLUMN, null);
    dbAccessor.clearTable(SERVICE_COMPONENT_HISTORY_TABLE);
    dbAccessor.clearTable(SERVICE_COMPONENT_HISTORY_TABLE);
    dbAccessor.clearTable(UPGRADE_ITEM_TABLE);
    dbAccessor.clearTable(UPGRADE_GROUP_TABLE);
    dbAccessor.clearTable(UPGRADE_TABLE);

    dbAccessor.dropColumn(UPGRADE_TABLE, "to_version");
    dbAccessor.dropColumn(UPGRADE_TABLE, "from_version");

    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBColumnInfo(UPGRADE_TABLE_FROM_REPO_COLUMN, Long.class, null, null, false));

    dbAccessor.addFKConstraint(UPGRADE_TABLE, "FK_upgrade_from_repo_id",
        UPGRADE_TABLE_FROM_REPO_COLUMN, "repo_version", "repo_version_id", false);

    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBColumnInfo(UPGRADE_TABLE_TO_REPO_COLUMN, Long.class, null, null, false));

    dbAccessor.addFKConstraint(UPGRADE_TABLE, "FK_upgrade_to_repo_id",
        UPGRADE_TABLE_FROM_REPO_COLUMN, "repo_version", "repo_version_id", false);
  }

  /**
   * Resets the following properties in {@code cluster-env} to their new
   * defaults:
   * <ul>
   * <li>stack_root
   * <li>stack_tools
   * <li>stack_features
   * <ul>
   *
   * @throws AmbariException
   */
  private void resetStackToolsAndFeatures() throws AmbariException {
    Set<String> propertiesToReset = Sets.newHashSet("stack_tools", "stack_features", "stack_root");

    Clusters clusters = injector.getInstance(Clusters.class);
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);

    Map<String, Cluster> clusterMap = clusters.getClusters();
    for (Cluster cluster : clusterMap.values()) {
      Config clusterEnv = cluster.getDesiredConfigByType(CLUSTER_ENV);
      if (null == clusterEnv) {
        continue;
      }

      Map<String, String> newStackProperties = new HashMap<>();
      Set<PropertyInfo> stackProperties = configHelper.getStackProperties(cluster);
      if (null == stackProperties) {
        continue;
      }

      for (PropertyInfo propertyInfo : stackProperties) {
        String fileName = propertyInfo.getFilename();
        if (StringUtils.isEmpty(fileName)) {
          continue;
        }

        if (StringUtils.equals(ConfigHelper.fileNameToConfigType(fileName), CLUSTER_ENV)) {
          String stackPropertyName = propertyInfo.getName();
          if (propertiesToReset.contains(stackPropertyName)) {
            newStackProperties.put(stackPropertyName, propertyInfo.getValue());
          }
        }
      }

      updateConfigurationPropertiesForCluster(cluster, CLUSTER_ENV, newStackProperties, true, false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateKerberosDescriptorArtifact(ArtifactDAO artifactDAO, ArtifactEntity artifactEntity) throws AmbariException {
    if (artifactEntity != null) {
      Map<String, Object> data = artifactEntity.getArtifactData();

      if (data != null) {
        final KerberosDescriptor kerberosDescriptor = new KerberosDescriptorFactory().createInstance(data);

        if (kerberosDescriptor != null) {
          boolean updated = false;

          // Find and remove configuration specifications for <code>livy-conf/livy.superusers</code>
          // in SPARK since this logic has been moved to the relevant stack/service advisors
          if(removeConfigurationSpecifications(kerberosDescriptor.getService("SPARK"),
              Collections.<String, Collection<String>>singletonMap("livy-conf", Collections.singleton("livy.superusers")))) {
            updated = true;
          }

          // Find and remove configuration specifications for <code>livy-conf2/livy.superusers</code>
          // in SPARK2 since this logic has been moved to the relevant stack/service advisors
          if(removeConfigurationSpecifications(kerberosDescriptor.getService("SPARK2"),
              Collections.<String, Collection<String>>singletonMap("livy2-conf", Collections.singleton("livy.superusers")))) {
            updated = true;
          }

          // Find and remove configuration specifications for the following configurations in KNOX/KNOX_GATEWAY
          // since they are invalid due to static "knox" embedded in the property name:
          // * oozie-site/oozie.service.ProxyUserService.proxyuser.knox.groups
          // * oozie-site/oozie.service.ProxyUserService.proxyuser.knox.hosts
          // * webhcat-site/webhcat.proxyuser.knox.groups
          // * webhcat-site/webhcat.proxyuser.knox.hosts
          // * core-site/hadoop.proxyuser.knox.groups
          // * core-site/hadoop.proxyuser.knox.hosts
          // * falcon-runtime.properties/*.falcon.service.ProxyUserService.proxyuser.knox.groups
          // * falcon-runtime.properties/*.falcon.service.ProxyUserService.proxyuser.knox.hosts
          KerberosServiceDescriptor knoxKerberosDescriptor = kerberosDescriptor.getService("KNOX");
          if(knoxKerberosDescriptor != null) {
            KerberosComponentDescriptor knoxGatewayKerberosDescriptor = knoxKerberosDescriptor.getComponent("KNOX_GATEWAY");
            if (knoxGatewayKerberosDescriptor != null) {
              Map<String, Collection<String>> configsToRemove = new HashMap<>();
              configsToRemove.put("oozie-site",
                  Arrays.asList("oozie.service.ProxyUserService.proxyuser.knox.groups", "oozie.service.ProxyUserService.proxyuser.knox.hosts"));
              configsToRemove.put("webhcat-site",
                  Arrays.asList("webhcat.proxyuser.knox.groups", "webhcat.proxyuser.knox.hosts"));
              configsToRemove.put("core-site",
                  Arrays.asList("hadoop.proxyuser.knox.groups", "hadoop.proxyuser.knox.hosts"));
              configsToRemove.put("falcon-runtime.properties",
                  Arrays.asList("*.falcon.service.ProxyUserService.proxyuser.knox.groups", "*.falcon.service.ProxyUserService.proxyuser.knox.hosts"));
              if (removeConfigurationSpecifications(knoxGatewayKerberosDescriptor, configsToRemove)) {
                updated = true;
              }
            }
          }

          if (updated) {
            artifactEntity.setArtifactData(kerberosDescriptor.toMap());
            artifactDAO.merge(artifactEntity);
          }
        }
      }
    }
  }

  /**
   * Fixes the <code>livy.superusers</code> value in <code>livy-conf</code> and
   * <code>livy2-conf</code>.
   * <p>
   * When Kerberos is enabled, the values of <code>livy.superusers</code> in <code>livy-conf</code>
   * and <code>livy2-conf</code> are potentially incorrect due to an issue with the Spark and Spark2
   * kerberos.json files.  In Ambari 2.5.2, the logic to set <code>livy.superusers</code> has been
   * moved to the stack advisor and removed from the kerberos.json files.  The user-supplied Kerberos
   * descriptor is fixed in {@link #updateKerberosDescriptorArtifact(ArtifactDAO, ArtifactEntity)}.
   * <p>
   * If Zeppelin is installed and Kerberos is enabled, then <code>livy.superusers</code> should be
   * updated to contain the proper value for the Zeppelin user. If the incorrect value is there and
   * in the form of <code>zeppelin-clustername</code> then it will be removed.
   */
  void fixLivySuperusers() throws AmbariException {
    Clusters clusters = injector.getInstance(Clusters.class);
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Config zeppelinEnvProperties = cluster.getDesiredConfigByType("zeppelin-env");
          if (zeppelinEnvProperties != null) {
            Map<String, String> zeppelinProperties = zeppelinEnvProperties.getProperties();
            if (zeppelinProperties != null) {
              String zeppelinPrincipal = zeppelinProperties.get("zeppelin.server.kerberos.principal");

              if (!StringUtils.isEmpty(zeppelinPrincipal)) {
                // Parse the principal name component from the full principal. The default realm of
                // EXAMPLE.COM is used because we really don't care what the realm is.
                DeconstructedPrincipal deconstructedPrincipal = DeconstructedPrincipal.valueOf(zeppelinPrincipal, "EXAMPLE.COM");
                String newZeppelinPrincipalName = deconstructedPrincipal.getPrincipalName();
                String oldZeppelinPrincipalName = "zeppelin-" + cluster.getClusterName().toLowerCase();

                // Fix livy-conf/livy.supserusers
                updateListValues(cluster, "livy-conf", "livy.superusers",
                    Collections.singleton(newZeppelinPrincipalName), Collections.singleton(oldZeppelinPrincipalName));

                // Fix livy2-conf/livy.supserusers
                updateListValues(cluster, "livy2-conf", "livy.superusers",
                    Collections.singleton(newZeppelinPrincipalName), Collections.singleton(oldZeppelinPrincipalName));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Updates the contents of a configuration with comma-delimited list of values.
   * <p>
   * Items will be added and/or removed as needed. If changes are made to the value, the configuration
   * is updated in the cluster.
   *
   * @param cluster        the cluster
   * @param configType     the configuration type
   * @param propertyName   the property name
   * @param valuesToAdd    a set of values to add to the list
   * @param valuesToRemove a set of values to remove from the list
   * @throws AmbariException
   */
  private void updateListValues(Cluster cluster, String configType, String propertyName, Set<String> valuesToAdd, Set<String> valuesToRemove)
      throws AmbariException {
    Config config = cluster.getDesiredConfigByType(configType);
    if (config != null) {
      Map<String, String> properties = config.getProperties();
      if (properties != null) {
        String existingValue = properties.get(propertyName);
        String newValue = null;

        if (StringUtils.isEmpty(existingValue)) {
          if ((valuesToAdd != null) && !valuesToAdd.isEmpty()) {
            newValue = StringUtils.join(valuesToAdd, ',');
          }
        } else {
          Set<String> valueSet = new TreeSet<>(Arrays.asList(existingValue.split("\\s*,\\s*")));

          boolean removedValues = false;
          if (valuesToRemove != null) {
            removedValues = valueSet.removeAll(valuesToRemove);
          }

          boolean addedValues = false;
          if (valuesToAdd != null) {
            addedValues = valueSet.addAll(valuesToAdd);
          }

          if (removedValues || addedValues) {
            newValue = StringUtils.join(valueSet, ',');
          }
        }

        if (!StringUtils.isEmpty(newValue)) {
          updateConfigurationPropertiesForCluster(cluster, configType, Collections.singletonMap(propertyName, newValue), true, true);
        }
      }
    }
  }

  /**
   * Given an {@link AbstractKerberosDescriptorContainer}, attempts to remove the specified
   * configurations (<code>configType/propertyName</code> from it.
   *
   * @param kerberosDescriptorContainer the container to update
   * @param configurations              a map of configuration types to sets of property names.
   * @return true if changes where made to the container; false otherwise
   */
  private boolean removeConfigurationSpecifications(AbstractKerberosDescriptorContainer kerberosDescriptorContainer, Map<String, Collection<String>> configurations) {
    boolean updated = false;
    if (kerberosDescriptorContainer != null) {
      if (!MapUtils.isEmpty(configurations)) {
        for (Map.Entry<String, Collection<String>> entry : configurations.entrySet()) {
          String configType = entry.getKey();

          for (String propertyName : entry.getValue()) {
            Map<String, KerberosConfigurationDescriptor> configurationDescriptors = kerberosDescriptorContainer.getConfigurations(false);
            KerberosConfigurationDescriptor configurationDescriptor = (configurationDescriptors == null)
                ? null
                : configurationDescriptors.get(configType);
            if (configurationDescriptor != null) {
              Map<String, String> properties = configurationDescriptor.getProperties();
              if ((properties != null) && properties.containsKey(propertyName)) {
                properties.remove(propertyName);
                LOG.info("Removed {}/{} from the descriptor named {}", configType, propertyName, kerberosDescriptorContainer.getName());
                updated = true;

                // If there are no more properties in the configurationDescriptor, remove it from the container.
                if (properties.isEmpty()) {
                  configurationDescriptors.remove(configType);
                  kerberosDescriptorContainer.setConfigurations(configurationDescriptors);
                }
              }
            }
          }
        }
      }
    }

    return updated;
  }
}
