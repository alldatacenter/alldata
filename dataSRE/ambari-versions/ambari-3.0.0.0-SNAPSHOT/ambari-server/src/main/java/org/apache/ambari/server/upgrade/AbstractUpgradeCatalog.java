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

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.PropertyUpgradeBehavior;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.stack.WidgetLayout;
import org.apache.ambari.server.state.stack.WidgetLayoutInfo;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

public abstract class AbstractUpgradeCatalog implements UpgradeCatalog {
  @Inject
  protected DBAccessor dbAccessor;
  @Inject
  protected Configuration configuration;
  @Inject
  protected AmbariManagementControllerImpl ambariManagementController;

  protected Injector injector;

  // map and list with constants, for filtration like in stack advisor

  /**
   * Override variable in child's if table name was changed
   */
  protected String ambariSequencesTable = "ambari_sequences";

  /**
   * The user name to use as the authenticated user when perform authenticated tasks or operations
   * that require the name of the authenticated user
   */
  protected static final String AUTHENTICATED_USER_NAME = "ambari-upgrade";

  private static final String CONFIGURATION_TYPE_HDFS_SITE = "hdfs-site";
  public static final String CONFIGURATION_TYPE_RANGER_HBASE_PLUGIN_PROPERTIES = "ranger-hbase-plugin-properties";
  public static final String CONFIGURATION_TYPE_RANGER_KNOX_PLUGIN_PROPERTIES = "ranger-knox-plugin-properties";
  public static final String CONFIGURATION_TYPE_RANGER_HIVE_PLUGIN_PROPERTIES = "ranger-hive-plugin-properties";

  private static final String PROPERTY_DFS_NAMESERVICES = "dfs.nameservices";
  public static final String PROPERTY_RANGER_HBASE_PLUGIN_ENABLED = "ranger-hbase-plugin-enabled";
  public static final String PROPERTY_RANGER_KNOX_PLUGIN_ENABLED = "ranger-knox-plugin-enabled";
  public static final String PROPERTY_RANGER_HIVE_PLUGIN_ENABLED = "ranger-hive-plugin-enabled";

  public static final String YARN_SCHEDULER_CAPACITY_ROOT_QUEUE = "yarn.scheduler.capacity.root";
  public static final String YARN_SCHEDULER_CAPACITY_ROOT_QUEUES = "yarn.scheduler.capacity.root.queues";
  public static final String QUEUES = "queues";

  public static final String ALERT_URL_PROPERTY_CONNECTION_TIMEOUT = "connection_timeout";

  private static final Logger LOG = LoggerFactory.getLogger
    (AbstractUpgradeCatalog.class);
  private static final Map<String, UpgradeCatalog> upgradeCatalogMap =
    new HashMap<>();

  protected String ambariUpgradeConfigUpdatesFileName;
  private Map<String,String> upgradeJsonOutput = new HashMap<>();

  @Inject
  public AbstractUpgradeCatalog(Injector injector) {
    this.injector = injector;
    injector.injectMembers(this);
    registerCatalog(this);
  }

  /**
   * Every subclass needs to register itself
   */
  protected void registerCatalog(UpgradeCatalog upgradeCatalog) {
    upgradeCatalogMap.put(upgradeCatalog.getTargetVersion(), upgradeCatalog);
  }

  /**
   * Add new sequence to <code>ambariSequencesTable</code>.
   * @param seqName name of sequence to be inserted
   * @param seqDefaultValue initial value for the sequence
   * @param ignoreFailure true to ignore insert sql errors
   * @throws SQLException
   */
   protected final void addSequence(String seqName, Long seqDefaultValue, boolean ignoreFailure) throws SQLException{
     // check if sequence is already in the database
     Statement statement = null;
     ResultSet rs = null;
     try {
       statement = dbAccessor.getConnection().createStatement();
       if (statement != null) {
         rs = statement.executeQuery(String.format("SELECT COUNT(*) from %s where sequence_name='%s'", ambariSequencesTable, seqName));

         if (rs != null) {
           if (rs.next() && rs.getInt(1) == 0) {
             dbAccessor.executeQuery(String.format("INSERT INTO %s(sequence_name, sequence_value) VALUES('%s', %d)", ambariSequencesTable, seqName, seqDefaultValue), ignoreFailure);
           } else {
             LOG.warn("Sequence {} already exists, skipping", seqName);
           }
         }
       }
     } finally {
       if (rs != null) {
         rs.close();
       }
       if (statement != null) {
         statement.close();
       }
     }
  }

  /**
   * Add several new sequences to <code>ambariSequencesTable</code>.
   * @param seqNames list of sequences to be inserted
   * @param seqDefaultValue initial value for the sequence
   * @param ignoreFailure true to ignore insert sql errors
   * @throws SQLException
   *
   */
  protected final void addSequences(List<String> seqNames, Long seqDefaultValue, boolean ignoreFailure) throws SQLException{
    // ToDo: rewrite function to use one SQL call per select/insert for all items
    for (String seqName: seqNames){
      addSequence(seqName, seqDefaultValue, ignoreFailure);
    }
  }

  /**
   * Fetches the maximum value of the given ID column from the given table.
   *
   * @param tableName
   *          the name of the table to query the data from
   * @param idColumnName
   *          the name of the ID column you want to query the maximum value for.
   *          This MUST refer an existing numeric type column
   * @return the maximum value of the given column in the given table if any;
   *         <code>0L</code> otherwise.
   * @throws SQLException
   */
  protected final long fetchMaxId(String tableName, String idColumnName) throws SQLException {
    try (Statement stmt = dbAccessor.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery(String.format("SELECT MAX(%s) FROM %s", idColumnName, tableName))) {
      if (rs.next()) {
        return rs.getLong(1);
      }
      return 0L;
    }
  }

  @Override
  public String getSourceVersion() {
    return null;
  }

  protected static UpgradeCatalog getUpgradeCatalog(String version) {
    return upgradeCatalogMap.get(version);
  }

  protected static Document convertStringToDocument(String xmlStr) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    Document doc = null;

    try
    {
      builder = factory.newDocumentBuilder();
      doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) );
    } catch (Exception e) {
      LOG.error("Error during convertation from String \"" + xmlStr + "\" to Xml!", e);
    }
    return doc;
  }

  protected static boolean isConfigEnabled(Cluster cluster, String configType, String propertyName) {
    boolean isRangerPluginEnabled = false;
    if (cluster != null) {
      Config rangerPluginProperties = cluster.getDesiredConfigByType(configType);
      if (rangerPluginProperties != null) {
        String rangerPluginEnabled = rangerPluginProperties.getProperties().get(propertyName);
        if (StringUtils.isNotEmpty(rangerPluginEnabled)) {
          isRangerPluginEnabled =  "yes".equalsIgnoreCase(rangerPluginEnabled);
        }
      }
    }
    return isRangerPluginEnabled;
  }

  protected static class VersionComparator implements Comparator<UpgradeCatalog> {

    @Override
    public int compare(UpgradeCatalog upgradeCatalog1,
                       UpgradeCatalog upgradeCatalog2) {
      //make sure FinalUpgradeCatalog runs last
      if (upgradeCatalog1.isFinal() ^ upgradeCatalog2.isFinal()) {
        return Boolean.compare(upgradeCatalog1.isFinal(), upgradeCatalog2.isFinal());
      }

      return VersionUtils.compareVersions(upgradeCatalog1.getTargetVersion(),
        upgradeCatalog2.getTargetVersion(), 4);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String,String> getUpgradeJsonOutput() {
    return upgradeJsonOutput;
  }

  /**
   * Update metainfo to new version.
   */
  @Transactional
  public int updateMetaInfoVersion(String version) {
    int rows = 0;
    if (version != null) {
      MetainfoDAO metainfoDAO = injector.getInstance(MetainfoDAO.class);

      MetainfoEntity versionEntity = metainfoDAO.findByKey("version");

      if (versionEntity != null) {
        versionEntity.setMetainfoValue(version);
        metainfoDAO.merge(versionEntity);
      } else {
        versionEntity = new MetainfoEntity();
        versionEntity.setMetainfoName("version");
        versionEntity.setMetainfoValue(version);
        metainfoDAO.create(versionEntity);
      }

    }

    return rows;
  }

  /*
  * This method will check all Web and Metric alerts one by one.
  * Parameter connection_timeout will be added to every alert which
  * doesn't contain it.
  * */
  public void addConnectionTimeoutParamForWebAndMetricAlerts() {
    LOG.info("Updating alert definitions.");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();
    JsonParser jsonParser = new JsonParser();

    for (final Cluster cluster : getCheckedClusterMap(clusters).values()) {
      long clusterID = cluster.getClusterId();
      List<AlertDefinitionEntity> alertDefinitionList = alertDefinitionDAO.findAll(clusterID);

      for (AlertDefinitionEntity alertDefinitionEntity : alertDefinitionList) {
        SourceType sourceType = alertDefinitionEntity.getSourceType();
        if (sourceType == SourceType.METRIC || sourceType == SourceType.WEB) {
          String source = alertDefinitionEntity.getSource();
          JsonObject rootJson = jsonParser.parse(source).getAsJsonObject();

          JsonObject uriJson = rootJson.get("uri").getAsJsonObject();
          if (!uriJson.has(ALERT_URL_PROPERTY_CONNECTION_TIMEOUT)) {
            uriJson.addProperty(ALERT_URL_PROPERTY_CONNECTION_TIMEOUT, 5.0);
            alertDefinitionEntity.setSource(rootJson.toString());
            alertDefinitionDAO.merge(alertDefinitionEntity);
          }
        }
      }
    }
  }


  protected Provider<EntityManager> getEntityManagerProvider() {
    return injector.getProvider(EntityManager.class);
  }

  protected void executeInTransaction(Runnable func) {
    EntityManager entityManager = getEntityManagerProvider().get();
    if (entityManager.getTransaction().isActive()) { //already started, reuse
      func.run();
    } else {
      entityManager.getTransaction().begin();
      try {
        func.run();
        entityManager.getTransaction().commit();
        // This is required because some of the  entities actively managed by
        // the persistence context will remain unaware of the actual changes
        // occurring at the database level. Some UpgradeCatalogs perform
        // update / delete using CriteriaBuilder directly.
        entityManager.getEntityManagerFactory().getCache().evictAll();
      } catch (Exception e) {
        LOG.error("Error in transaction ", e);
        if (entityManager.getTransaction().isActive()) {
          entityManager.getTransaction().rollback();
        }
        throw new RuntimeException(e);
      }

    }
  }

  protected void changePostgresSearchPath() throws SQLException {
    String dbUser = configuration.getDatabaseUser();
    String schemaName = configuration.getServerJDBCPostgresSchemaName();

    if (null != dbUser && !dbUser.equals("") && null != schemaName && !schemaName.equals("")) {
      // Wrap username with double quotes to accept old username "ambari-server"
      if (!dbUser.contains("\"")) {
        dbUser = String.format("\"%s\"", dbUser);
      }

      dbAccessor.executeQuery(String.format("ALTER SCHEMA %s OWNER TO %s;", schemaName, dbUser));
      dbAccessor.executeQuery(String.format("ALTER ROLE %s SET search_path to '%s';", dbUser, schemaName));
    }
  }

  public void addNewConfigurationsFromXml() throws AmbariException {
    Clusters clusters = injector.getInstance(Clusters.class);
    if (clusters == null) {
      return;
    }

    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null && !clusterMap.isEmpty()) {
      for (Cluster cluster : clusterMap.values()) {
        Map<String, Set<String>> toAddProperties = new HashMap<>();
        Map<String, Set<String>> toUpdateProperties = new HashMap<>();
        Map<String, Set<String>> toRemoveProperties = new HashMap<>();


        Set<PropertyInfo> stackProperties = configHelper.getStackProperties(cluster);
        for(String serviceName: cluster.getServices().keySet()) {
          Set<PropertyInfo> properties = configHelper.getServiceProperties(cluster, serviceName);

          if (properties == null) {
            continue;
          }
          properties.addAll(stackProperties);

          for (PropertyInfo property : properties) {
            String configType = ConfigHelper.fileNameToConfigType(property.getFilename());
            PropertyUpgradeBehavior upgradeBehavior = property.getPropertyAmbariUpgradeBehavior();

            if (property.isDeleted()) {
              // Do nothing
            } else if (upgradeBehavior.isDelete()) {
              if (!toRemoveProperties.containsKey(configType)) {
                toRemoveProperties.put(configType, new HashSet<>());
              }
              toRemoveProperties.get(configType).add(property.getName());
            } else if (upgradeBehavior.isUpdate()) {
              if (!toUpdateProperties.containsKey(configType)) {
                toUpdateProperties.put(configType, new HashSet<>());
              }
              toUpdateProperties.get(configType).add(property.getName());
            } else if (upgradeBehavior.isAdd()) {
              if (!toAddProperties.containsKey(configType)) {
                toAddProperties.put(configType, new HashSet<>());
              }
              toAddProperties.get(configType).add(property.getName());
            }
          }
        }

        for (Entry<String, Set<String>> newProperty : toAddProperties.entrySet()) {
          String newPropertyKey = newProperty.getKey();
          updateConfigurationPropertiesWithValuesFromXml(newPropertyKey, newProperty.getValue(), false, true);
        }

        for (Entry<String, Set<String>> newProperty : toUpdateProperties.entrySet()) {
          String newPropertyKey = newProperty.getKey();
          updateConfigurationPropertiesWithValuesFromXml(newPropertyKey, newProperty.getValue(), true, false);
        }

        for (Entry<String, Set<String>> toRemove : toRemoveProperties.entrySet()) {
          String newPropertyKey = toRemove.getKey();
          updateConfigurationPropertiesWithValuesFromXml(newPropertyKey, Collections.emptySet(), toRemove.getValue(), false, true);
        }
      }
    }
  }

  protected boolean isNNHAEnabled(Cluster cluster) {
    Config hdfsSiteConfig = cluster.getDesiredConfigByType(CONFIGURATION_TYPE_HDFS_SITE);
    if (hdfsSiteConfig != null) {
      Map<String, String> properties = hdfsSiteConfig.getProperties();
      if (properties.containsKey("dfs.internal.nameservices")) {
        return true;
      }
      String nameServices = properties.get(PROPERTY_DFS_NAMESERVICES);
      if (!StringUtils.isEmpty(nameServices)) {
        for (String nameService : nameServices.split(",")) {
          String namenodes = properties.get(String.format("dfs.ha.namenodes.%s", nameService));
          if (!StringUtils.isEmpty(namenodes)) {
            return (namenodes.split(",").length > 1);
          }
        }
      }
    }
    return false;
  }

  /**
   * This method returns Map of clusters.
   * Map can be empty or with some objects, but never be null.
   */
  protected Map<String, Cluster> getCheckedClusterMap(Clusters clusters) {
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null) {
        return clusterMap;
      }
    }
    return new HashMap<>();
  }

  /**
   * Create a new cluster scoped configuration with the new properties added
   * with the values from the coresponding xml files.
   *
   * If xml owner service is not in the cluster, the configuration won't be added.
   *
   * @param configType Configuration type. (hdfs-site, etc.)
   * @param propertyNames Set property names.
   */
  protected void updateConfigurationPropertiesWithValuesFromXml(String configType,
      Set<String> propertyNames, boolean updateIfExists, boolean createNewConfigType) throws AmbariException {
    updateConfigurationPropertiesWithValuesFromXml(configType, propertyNames, null, updateIfExists, createNewConfigType);

  }

  protected void updateConfigurationPropertiesWithValuesFromXml(String configType,
                                                                Set<String> propertyNames,
                                                                Set<String> toRemove,
                                                                boolean updateIfExists,
                                                                boolean createNewConfigType) throws AmbariException {
    Clusters clusters = injector.getInstance(Clusters.class);
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    if (clusters == null) {
      return;
    }
    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null && !clusterMap.isEmpty()) {
      for (Cluster cluster : clusterMap.values()) {
        Map<String, String> properties = new HashMap<>();

        for(String propertyName:propertyNames) {
          String propertyValue = configHelper.getPropertyValueFromStackDefinitions(cluster, configType, propertyName);

          if(propertyValue == null) {
            LOG.info("Config " + propertyName + " from " + configType + " is not found in xml definitions." +
                "Skipping configuration property update");
            continue;
          }

          ServiceInfo propertyService = configHelper.getPropertyOwnerService(cluster, configType, propertyName);
          if(propertyService != null && !cluster.getServices().containsKey(propertyService.getName())) {
            LOG.info("Config " + propertyName + " from " + configType + " with value = " + propertyValue + " " +
                "Is not added due to service " + propertyService.getName() + " is not in the cluster.");
            continue;
          }

          properties.put(propertyName, propertyValue);
        }

        updateConfigurationPropertiesForCluster(cluster, configType,
            properties, toRemove, updateIfExists, createNewConfigType);
      }
    }
  }

  /**
   * Update properties for the cluster
   * @param cluster cluster object
   * @param configType config to be updated
   * @param properties properties to be added or updated. Couldn't be <code>null</code>, but could be empty.
   * @param removePropertiesList properties to be removed. Could be <code>null</code>
   * @param updateIfExists
   * @param createNewConfigType
   * @throws AmbariException
   */
  protected void updateConfigurationPropertiesForCluster(Cluster cluster, String configType,
        Map<String, String> properties, Set<String> removePropertiesList, boolean updateIfExists,
        boolean createNewConfigType) throws AmbariException {
    AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);
    String newTag = "version" + System.currentTimeMillis();

    if (properties != null) {
      Map<String, Config> all = cluster.getConfigsByType(configType);
      if (all == null || !all.containsKey(newTag) || properties.size() > 0) {
        Map<String, String> oldConfigProperties;
        Config oldConfig = cluster.getDesiredConfigByType(configType);

        if (oldConfig == null && !createNewConfigType) {
          LOG.info("Config " + configType + " not found. Assuming service not installed. " +
              "Skipping configuration properties update");
          return;
        } else if (oldConfig == null) {
          oldConfigProperties = new HashMap<>();
        } else {
          oldConfigProperties = oldConfig.getProperties();
        }

        Multimap<ConfigUpdateType, Entry<String, String>> propertiesToLog = ArrayListMultimap.create();
        String serviceName = cluster.getServiceByConfigType(configType);

        Map<String, String> mergedProperties =
          mergeProperties(oldConfigProperties, properties, updateIfExists, propertiesToLog);

        if (removePropertiesList != null) {
          mergedProperties = removeProperties(mergedProperties, removePropertiesList, propertiesToLog);
        }

        if (propertiesToLog.size() > 0) {
          try {
            configuration.writeToAmbariUpgradeConfigUpdatesFile(propertiesToLog, configType, serviceName, ambariUpgradeConfigUpdatesFileName);
          } catch(Exception e) {
            LOG.error("Write to config updates file failed:", e);
          }
        }

        if (!Maps.difference(oldConfigProperties, mergedProperties).areEqual()) {
          LOG.info("Applying configuration with tag '{}' and configType '{}' to " +
            "cluster '{}'", newTag, configType, cluster.getClusterName());

          Map<String, Map<String, String>> propertiesAttributes = null;
          if (oldConfig != null) {
            propertiesAttributes = oldConfig.getPropertiesAttributes();
          }

          // the contract of creating a configuration requires non-null
          // collections for attributes
          if (null == propertiesAttributes) {
            propertiesAttributes = Collections.emptyMap();
          }

          controller.createConfig(cluster, cluster.getDesiredStackVersion(), configType,
              mergedProperties, newTag, propertiesAttributes);

          Config baseConfig = cluster.getConfig(configType, newTag);
          if (baseConfig != null) {
            String authName = AUTHENTICATED_USER_NAME;

            String configVersionNote = String.format("Updated %s during Ambari Upgrade from %s to %s.", configType, getSourceVersion(), getTargetVersion());
            if (cluster.addDesiredConfig(authName, Collections.singleton(baseConfig), configVersionNote) != null) {
              String oldConfigString = (oldConfig != null) ? " from='" + oldConfig.getTag() + "'" : "";
              LOG.info("cluster '" + cluster.getClusterName() + "' "
                + "changed by: '" + authName + "'; "
                + "type='" + baseConfig.getType() + "' "
                + "tag='" + baseConfig.getTag() + "'"
                + oldConfigString);
            }

            ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
            configHelper.updateAgentConfigs(Collections.singleton(cluster.getClusterName()));
          }
        } else {
          LOG.info("No changes detected to config " + configType + ". Skipping configuration properties update");
        }
      }
    }
  }

  protected void updateConfigurationPropertiesForCluster(Cluster cluster, String configType,
        Map<String, String> properties, boolean updateIfExists, boolean createNewConfigType) throws AmbariException {
    updateConfigurationPropertiesForCluster(cluster, configType, properties, null, updateIfExists, createNewConfigType);
  }

  /**
   * Remove properties from the cluster
   * @param cluster cluster object
   * @param configType config to be updated
   * @param removePropertiesList properties to be removed. Could be <code>null</code>
   * @throws AmbariException
   */
  protected void removeConfigurationPropertiesFromCluster(Cluster cluster, String configType, Set<String> removePropertiesList)
      throws AmbariException {

    updateConfigurationPropertiesForCluster(cluster, configType, new HashMap<>(), removePropertiesList, false, true);
  }

  /**
   * Create a new cluster scoped configuration with the new properties added
   * to the existing set of properties.
   * @param configType Configuration type. (hdfs-site, etc.)
   * @param properties Map of key value pairs to add / update.
   */
  protected void updateConfigurationProperties(String configType,
        Map<String, String> properties, boolean updateIfExists, boolean createNewConfigType) throws
    AmbariException {
    AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);

    Clusters clusters = controller.getClusters();
    if (clusters == null) {
      return;
    }
    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null && !clusterMap.isEmpty()) {
      for (Cluster cluster : clusterMap.values()) {
        updateConfigurationPropertiesForCluster(cluster, configType,
            properties, updateIfExists, createNewConfigType);
      }
    }
  }

  private Map<String, String> mergeProperties(Map<String, String> originalProperties,
                               Map<String, String> newProperties,
                               boolean updateIfExists, Multimap<AbstractUpgradeCatalog.ConfigUpdateType, Entry<String, String>> propertiesToLog) {

    Map<String, String> properties = new HashMap<>(originalProperties);
    for (Map.Entry<String, String> entry : newProperties.entrySet()) {
      if (!properties.containsKey(entry.getKey())) {
        properties.put(entry.getKey(), entry.getValue());
        propertiesToLog.put(ConfigUpdateType.ADDED, entry);
      }
      if (updateIfExists)  {
        properties.put(entry.getKey(), entry.getValue());
        propertiesToLog.put(ConfigUpdateType.UPDATED, entry);
      }
    }
    return properties;
  }

  private Map<String, String> removeProperties(Map<String, String> originalProperties,
                                               Set<String> removeList, Multimap<AbstractUpgradeCatalog.ConfigUpdateType, Entry<String, String>> propertiesToLog){
    Map<String, String> properties = new HashMap<>();
    properties.putAll(originalProperties);
    for (String removeProperty: removeList){
      if (originalProperties.containsKey(removeProperty)){
        properties.remove(removeProperty);
        propertiesToLog.put(ConfigUpdateType.REMOVED, new AbstractMap.SimpleEntry<>(removeProperty, ""));
      }
    }
    return properties;
  }

  public enum ConfigUpdateType {
    ADDED("Added"),
    UPDATED("Updated"),
    REMOVED("Removed");


    private final String description;


    ConfigUpdateType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Iterates through a collection of AbstractKerberosDescriptorContainers to find and update
   * identity descriptor references.
   *
   * @param descriptorMap    a String to AbstractKerberosDescriptorContainer map to iterate trough
   * @param referenceName    the reference name to change
   * @param newReferenceName the new reference name
   */
  protected void updateKerberosDescriptorIdentityReferences(Map<String, ? extends AbstractKerberosDescriptorContainer> descriptorMap,
                                                          String referenceName,
                                                          String newReferenceName) {
    if (descriptorMap != null) {
      for (AbstractKerberosDescriptorContainer kerberosServiceDescriptor : descriptorMap.values()) {
        updateKerberosDescriptorIdentityReferences(kerberosServiceDescriptor, referenceName, newReferenceName);

        if (kerberosServiceDescriptor instanceof KerberosServiceDescriptor) {
          updateKerberosDescriptorIdentityReferences(((KerberosServiceDescriptor) kerberosServiceDescriptor).getComponents(),
              referenceName, newReferenceName);
        }
      }
    }
  }

  /**
   * Given an AbstractKerberosDescriptorContainer, iterates through its contained identity descriptors
   * to find ones matching the reference name to change.
   * <p/>
   * If found, the reference name is updated to the new name.
   *
   * @param descriptorContainer the AbstractKerberosDescriptorContainer to update
   * @param referenceName       the reference name to change
   * @param newReferenceName    the new reference name
   */
  protected void updateKerberosDescriptorIdentityReferences(AbstractKerberosDescriptorContainer descriptorContainer,
                                                          String referenceName,
                                                          String newReferenceName) {
    if (descriptorContainer != null) {
      KerberosIdentityDescriptor identity = descriptorContainer.getIdentity(referenceName);
      if (identity != null) {
        identity.setName(newReferenceName);
      }
    }
  }

  /**
   * Update the stored Kerberos Descriptor artifacts to conform to the new structure.
   * <p/>
   * Finds the relevant artifact entities and iterates through them to process each independently.
   */
  protected void updateKerberosDescriptorArtifacts() throws AmbariException {
    ArtifactDAO artifactDAO = injector.getInstance(ArtifactDAO.class);
    List<ArtifactEntity> artifactEntities = artifactDAO.findByName("kerberos_descriptor");

    if (artifactEntities != null) {
      for (ArtifactEntity artifactEntity : artifactEntities) {
        updateKerberosDescriptorArtifact(artifactDAO, artifactEntity);
      }
    }
  }

  /**
   * Retrieve the composite Kerberos Descriptor.
   * <p>
   * The composite Kerberos Descriptor is the cluster's stack-specific Kerberos Descriptor overlaid
   * with changes specified by the user via the cluster's Kerberos Descriptor artifact.
   *
   * @param cluster the relevant cluster
   * @return the composite Kerberos Descriptor
   * @throws AmbariException
   */
  protected KerberosDescriptor getKerberosDescriptor(Cluster cluster) throws AmbariException {
    // Get the Stack-defined Kerberos Descriptor (aka default Kerberos Descriptor)
    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);


    // !!! FIXME
    @Experimental(feature = ExperimentalFeature.PATCH_UPGRADES,
        comment = "can only take the first stack we find until we can support multiple with Kerberos")
    StackId stackId = getStackId(cluster);

    KerberosDescriptor defaultDescriptor = ambariMetaInfo.getKerberosDescriptor(stackId.getStackName(), stackId.getStackVersion(), false);

    // Get the User-set Kerberos Descriptor
    ArtifactDAO artifactDAO = injector.getInstance(ArtifactDAO.class);
    KerberosDescriptor artifactDescriptor = null;
    ArtifactEntity artifactEntity = artifactDAO.findByNameAndForeignKeys("kerberos_descriptor",
      new TreeMap<>(Collections.singletonMap("cluster", String.valueOf(cluster.getClusterId()))));
    if (artifactEntity != null) {
      Map<String, Object> data = artifactEntity.getArtifactData();

      if (data != null) {
        artifactDescriptor = new KerberosDescriptorFactory().createInstance(data);
      }
    }

    // Calculate and return the composite Kerberos Descriptor
    if (defaultDescriptor == null) {
      return artifactDescriptor;
    } else if (artifactDescriptor == null) {
      return defaultDescriptor;
    } else {
      defaultDescriptor.update(artifactDescriptor);
      return defaultDescriptor;
    }
  }

  /**
   * Add a new role authorization and optionally add it to 1 or more roles.
   * <p>
   * The collection of roles to add the new role authorization to may be null or empty, indicating
   * that no roles are to be altered. If set, though, each role entry in the collection must be a
   * colon-delimited string like:  <code>ROLE:RESOURCE TYPE</code>. Examples:
   * <ul>
   * <li>"AMBARI.ADMINISTRATOR:AMBARI"</li>
   * <li>"CLUSTER.ADMINISTRATOR:CLUSTER"</li>
   * <li>"SERVICE.OPERATOR:CLUSTER"</li>
   * </ul>
   *
   * @param roleAuthorizationID   the ID of the new authorization
   * @param roleAuthorizationName the (descriptive) name of the new authorization
   * @param applicableRoles       an optional collection of role specification to add the new authorization to
   * @throws SQLException
   */
  protected void addRoleAuthorization(String roleAuthorizationID, String roleAuthorizationName, Collection<String> applicableRoles) throws SQLException {
    if (!StringUtils.isEmpty(roleAuthorizationID)) {
      RoleAuthorizationDAO roleAuthorizationDAO = injector.getInstance(RoleAuthorizationDAO.class);
      RoleAuthorizationEntity roleAuthorization = roleAuthorizationDAO.findById(roleAuthorizationID);

      if (roleAuthorization == null) {
        roleAuthorization = new RoleAuthorizationEntity();
        roleAuthorization.setAuthorizationId(roleAuthorizationID);
        roleAuthorization.setAuthorizationName(roleAuthorizationName);
        roleAuthorizationDAO.create(roleAuthorization);
      }

      if ((applicableRoles != null) && (!applicableRoles.isEmpty())) {
        for (String role : applicableRoles) {
          String[] parts = role.split("\\:");
          addAuthorizationToRole(parts[0], parts[1], roleAuthorization);
        }
      }
    }
  }

  /**
   * Add a new authorization to the set of authorizations for a role
   *
   * @param roleName            the name of the role
   * @param resourceType        the resource type of the role (AMBARI, CLUSTER, VIEW, etc...)
   * @param roleAuthorizationID the ID of the authorization
   * @see #addAuthorizationToRole(String, String, RoleAuthorizationEntity)
   */
  protected void addAuthorizationToRole(String roleName, String resourceType, String roleAuthorizationID) {
    if (!StringUtils.isEmpty(roleAuthorizationID)) {
      RoleAuthorizationDAO roleAuthorizationDAO = injector.getInstance(RoleAuthorizationDAO.class);
      RoleAuthorizationEntity roleAuthorization = roleAuthorizationDAO.findById(roleAuthorizationID);

      if (roleAuthorization != null) {
        addAuthorizationToRole(roleName, resourceType, roleAuthorization);
      }
    }
  }

  /**
   * Add a new authorization to the set of authorizations for a role
   *
   * @param roleName          the name of the role
   * @param resourceType      the resource type of the role (AMBARI, CLUSTER, VIEW, etc...)
   * @param roleAuthorization the authorization to add
   */
  protected void addAuthorizationToRole(String roleName, String resourceType, RoleAuthorizationEntity roleAuthorization) {
    if ((roleAuthorization != null) && !StringUtils.isEmpty(roleName) && !StringUtils.isEmpty(resourceType)) {
      PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
      ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);

      PermissionEntity role = permissionDAO.findPermissionByNameAndType(roleName, resourceTypeDAO.findByName(resourceType));
      if (role != null) {
        role.addAuthorization(roleAuthorization);
        permissionDAO.merge(role);
      }
    }
  }

  /**
   * Add a new authorization to the set of authorizations for a role
   *
   * @param role                the role to add the authorization to
   * @param roleAuthorizationID the authorization to add
   */
  protected void addAuthorizationToRole(PermissionEntity role, String roleAuthorizationID) {
    if ((role != null) && !StringUtils.isEmpty(roleAuthorizationID)) {
      RoleAuthorizationDAO roleAuthorizationDAO = injector.getInstance(RoleAuthorizationDAO.class);
      RoleAuthorizationEntity roleAuthorization = roleAuthorizationDAO.findById(roleAuthorizationID);

      if (roleAuthorization != null) {
        PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
        role.getAuthorizations().add(roleAuthorization);
        permissionDAO.merge(role);
      }
    }
  }

  /**
   * Update the specified Kerberos Descriptor artifact to conform to the new structure.
   * <p/>
   * On ambari version update some of identities can be moved between scopes(e.g. from service to component), so
   * old identity need to be moved to proper place and all references for moved identity need to be updated.
   * <p/>
   * By default descriptor remains unchanged and this method must be overridden in child UpgradeCatalog to meet new
   * ambari version changes in kerberos descriptors.
   * <p/>
   * The supplied ArtifactEntity is updated in place a merged back into the database.
   *
   * @param artifactDAO    the ArtifactDAO to use to store the updated ArtifactEntity
   * @param artifactEntity the ArtifactEntity to update
   */
  protected void updateKerberosDescriptorArtifact(ArtifactDAO artifactDAO, ArtifactEntity artifactEntity) throws AmbariException {
    // NOOP
  }

  @Override
  public void upgradeSchema() throws AmbariException, SQLException {
    DatabaseType databaseType = configuration.getDatabaseType();

    if (databaseType == DatabaseType.POSTGRES) {
      changePostgresSearchPath();
    }

    executeDDLUpdates();
  }

  @Override
  public void preUpgradeData() throws AmbariException, SQLException {
    executePreDMLUpdates();
  }

  @Override
  public void setConfigUpdatesFileName(String ambariUpgradeConfigUpdatesFileName) {
    this.ambariUpgradeConfigUpdatesFileName = ambariUpgradeConfigUpdatesFileName;
  }

  @Override
  public void upgradeData() throws AmbariException, SQLException {
    executeDMLUpdates();
  }


  @Override
  public final void updateDatabaseSchemaVersion() {
    updateMetaInfoVersion(getTargetVersion());
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  protected abstract void executeDDLUpdates() throws AmbariException, SQLException;

  /**
   * Perform data insertion before running normal upgrade of data, requires started persist service
   * @throws AmbariException
   * @throws SQLException
   */
  protected abstract void executePreDMLUpdates() throws AmbariException, SQLException;

  protected abstract void executeDMLUpdates() throws AmbariException, SQLException;

  @Override
  public String toString() {
    return "{ upgradeCatalog: sourceVersion = " + getSourceVersion() + ", " +
      "targetVersion = " + getTargetVersion() + " }";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onPostUpgrade() throws AmbariException, SQLException {
    // NOOP
  }

  /**
   * Validate queueNameProperty exists for configType in cluster and corresponds to one of validLeafQueues
   * @param cluster cluster to operate with
   * @param validLeafQueues Set of YARN capacity-scheduler leaf queues
   * @param queueNameProperty queue name property to check and update
   * @param configType config type name
   * @return
   */
  protected boolean isQueueNameValid(Cluster cluster, Set<String> validLeafQueues, String queueNameProperty, String configType) {
    Config site = cluster.getDesiredConfigByType(configType);
    Map<String, String> properties = site.getProperties();
    boolean result = properties.containsKey(queueNameProperty) && validLeafQueues.contains(properties.get(queueNameProperty));
    if (!result){
      LOG.info("Queue name " + queueNameProperty + " in " + configType + " not defined or not corresponds to valid capacity-scheduler queue");
    }
    return result;
  }


  /**
   * Update property queueNameProperty from configType of cluster to first of validLeafQueues
   * @param cluster cluster to operate with
   * @param validLeafQueues Set of YARN capacity-scheduler leaf queues
   * @param queueNameProperty queue name property to check and update
   * @param configType config type name
   * @throws AmbariException if an error occurs while updating the configurations
   */
  protected void updateQueueName(Cluster cluster, Set<String> validLeafQueues, String queueNameProperty, String configType) throws AmbariException {
    String recommendQueue = validLeafQueues.iterator().next();
    LOG.info("Update " + queueNameProperty + " in " + configType + " set to " + recommendQueue);
    Map<String, String> updates = Collections.singletonMap(queueNameProperty, recommendQueue);
    updateConfigurationPropertiesForCluster(cluster, configType, updates, true, true);
  }

  /**
   * Pars Capacity Scheduler Properties and get all YARN Capacity Scheduler leaf queue names
   * @param capacitySchedulerMap capacity-scheduler properties map
   * @return all YARN Capacity Scheduler leaf queue names
   */
  protected Set<String> getCapacitySchedulerLeafQueues(Map<String, String> capacitySchedulerMap) {
    Set<String> leafQueues= new HashSet<>();
    Stack<String> toProcessQueues = new Stack<>();
    if (capacitySchedulerMap.containsKey(YARN_SCHEDULER_CAPACITY_ROOT_QUEUES)){
      StringTokenizer queueTokenizer = new StringTokenizer(capacitySchedulerMap.get(
          YARN_SCHEDULER_CAPACITY_ROOT_QUEUES), ",");
      while (queueTokenizer.hasMoreTokens()){
        toProcessQueues.push(queueTokenizer.nextToken());
      }
    }
    while (!toProcessQueues.empty()){
      String queue = toProcessQueues.pop();
      String queueKey = YARN_SCHEDULER_CAPACITY_ROOT_QUEUE + "." + queue + "." + QUEUES;
      if (capacitySchedulerMap.containsKey(queueKey)){
        StringTokenizer queueTokenizer = new StringTokenizer(capacitySchedulerMap.get(queueKey), ",");
        while (queueTokenizer.hasMoreTokens()){
          toProcessQueues.push(queue + "." + queueTokenizer.nextToken());
        }
      } else {
        if (!queue.endsWith(".")){
          String queueName = queue.substring(queue.lastIndexOf('.')+1);
          leafQueues.add(queueName);
        } else {
          LOG.warn("Queue " + queue + " is not valid");
        }
      }
    }
    return leafQueues;
  }

  /**
   *
   * @param serviceName
   * @param widgetMap
   * @param sectionLayoutMap
   * @throws AmbariException
   */
  protected void updateWidgetDefinitionsForService(String serviceName, Map<String, List<String>> widgetMap,
                                                   Map<String, String> sectionLayoutMap) throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    Type widgetLayoutType = new TypeToken<Map<String, List<WidgetLayout>>>(){}.getType();
    Gson gson = injector.getInstance(Gson.class);
    WidgetDAO widgetDAO = injector.getInstance(WidgetDAO.class);

    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      long clusterID = cluster.getClusterId();

      Service service = cluster.getServices().get(serviceName);
      if (null == service) {
        continue;
      }

      StackId stackId = service.getDesiredStackId();

      Map<String, Object> widgetDescriptor = null;
      StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
      ServiceInfo serviceInfo = stackInfo.getService(serviceName);
      if (serviceInfo == null) {
        LOG.info("Skipping updating widget definition, because " + serviceName +  " service is not present in cluster " +
            "cluster_name= " + cluster.getClusterName());
        continue;
      }

      for (String section : widgetMap.keySet()) {
        List<String> widgets = widgetMap.get(section);
        for (String widgetName : widgets) {
          List<WidgetEntity> widgetEntities = widgetDAO.findByName(clusterID,
              widgetName, "ambari", section);

          if (widgetEntities != null && widgetEntities.size() > 0) {
            WidgetEntity entityToUpdate = null;
            if (widgetEntities.size() > 1) {
              LOG.info("Found more that 1 entity with name = "+ widgetName +
                  " for cluster = " + cluster.getClusterName() + ", skipping update.");
            } else {
              entityToUpdate = widgetEntities.iterator().next();
            }
            if (entityToUpdate != null) {
              LOG.info("Updating widget: " + entityToUpdate.getWidgetName());
              // Get the definition from widgets.json file
              WidgetLayoutInfo targetWidgetLayoutInfo = null;
              File widgetDescriptorFile = serviceInfo.getWidgetsDescriptorFile();
              if (widgetDescriptorFile != null && widgetDescriptorFile.exists()) {
                try {
                  widgetDescriptor = gson.fromJson(new FileReader(widgetDescriptorFile), widgetLayoutType);
                } catch (Exception ex) {
                  String msg = "Error loading widgets from file: " + widgetDescriptorFile;
                  LOG.error(msg, ex);
                  widgetDescriptor = null;
                }
              }
              if (widgetDescriptor != null) {
                LOG.debug("Loaded widget descriptor: {}", widgetDescriptor);
                for (Object artifact : widgetDescriptor.values()) {
                  List<WidgetLayout> widgetLayouts = (List<WidgetLayout>) artifact;
                  for (WidgetLayout widgetLayout : widgetLayouts) {
                    if (widgetLayout.getLayoutName().equals(sectionLayoutMap.get(section))) {
                      for (WidgetLayoutInfo layoutInfo : widgetLayout.getWidgetLayoutInfoList()) {
                        if (layoutInfo.getWidgetName().equals(widgetName)) {
                          targetWidgetLayoutInfo = layoutInfo;
                        }
                      }
                    }
                  }
                }
              }
              if (targetWidgetLayoutInfo != null) {
                entityToUpdate.setMetrics(gson.toJson(targetWidgetLayoutInfo.getMetricsInfo()));
                entityToUpdate.setWidgetValues(gson.toJson(targetWidgetLayoutInfo.getValues()));
                entityToUpdate.setDescription(targetWidgetLayoutInfo.getDescription());
                widgetDAO.merge(entityToUpdate);
              } else {
                LOG.warn("Unable to find widget layout info for " + widgetName +
                    " in the stack: " + stackId);
              }
            }
          }
        }
      }
    }
  }

  @Experimental(feature = ExperimentalFeature.PATCH_UPGRADES,
      comment = "can only take the first stack we find until we can support multiple with Kerberos")
  private StackId getStackId(Cluster cluster) throws AmbariException {
    return cluster.getServices().values().iterator().next().getDesiredStackId();
  }
}
