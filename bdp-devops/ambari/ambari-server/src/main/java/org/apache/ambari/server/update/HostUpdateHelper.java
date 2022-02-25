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
package org.apache.ambari.server.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.TopologyHostGroupDAO;
import org.apache.ambari.server.orm.dao.TopologyHostRequestDAO;
import org.apache.ambari.server.orm.dao.TopologyRequestDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.TopologyHostGroupEntity;
import org.apache.ambari.server.orm.entities.TopologyHostInfoEntity;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/*
* Class for host names update.
* Can be used by command "ambari-server update-host-names /hosts_changes.json"
*
* Simple example of hosts_changes.json
* {
*  "cluster1" : {
*     "c6400.ambari.apache.org" : "c6411.ambari.apache.org"
*  }
* }
*
*/
public class HostUpdateHelper {
  private static final Logger LOG = LoggerFactory.getLogger
          (HostUpdateHelper.class);

  private static final String AUTHENTICATED_USER_NAME = "ambari-host-update";
  public static final String TMP_PREFIX = "tmpvalue";


  private PersistService persistService;
  private Configuration configuration;
  private Injector injector;

  protected String hostChangesFile;
  protected Map<String, Map<String, String>> hostChangesFileMap;


  @Inject
  public HostUpdateHelper(PersistService persistService,
                          Configuration configuration,
                          Injector injector) {
    this.persistService = persistService;
    this.configuration = configuration;
    this.injector = injector;
  }

  public void startPersistenceService() {
    persistService.start();
  }

  public void stopPersistenceService() {
    persistService.stop();
  }

  public String getHostChangesFile() {
    return hostChangesFile;
  }

  public void setHostChangesFile(String hostChangesFile) {
    this.hostChangesFile = hostChangesFile;
  }

  public Map<String, Map<String, String>> getHostChangesFileMap() {
    return hostChangesFileMap;
  }

  public void setHostChangesFileMap(Map<String, Map<String, String>> hostChangesFileMap) {
    this.hostChangesFileMap = hostChangesFileMap;
  }

  /**
   * Extension of audit logger module
   */
  public static class CheckHelperAuditModule extends AuditLoggerModule {

    public CheckHelperAuditModule() throws Exception {
    }

    @Override
    protected void configure() {
      super.configure();
    }

  }

  /**
   * Extension of main controller module
   */
  public static class UpdateHelperModule extends ControllerModule {

    public UpdateHelperModule() throws Exception {
    }

    @Override
    protected void configure() {
      super.configure();
      EventBusSynchronizer.synchronizeAmbariEventPublisher(binder());
    }
  }

  /*
  * Method which validates json with host changes.
  * Check cluster and hosts existence.
  * Check on valid structure of json.
  * */
  void validateHostChanges() throws AmbariException {
    if (hostChangesFileMap == null || hostChangesFileMap.isEmpty()) {
      throw new AmbariException(String.format("File with host names changes is null or empty"));
    }

    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {

      for (Map.Entry<String, Map<String, String>> clusterHosts : hostChangesFileMap.entrySet()) {
        String clusterName = clusterHosts.getKey();
        Cluster cluster = clusters.getCluster(clusterName);

        if (cluster != null) {
          Collection<Host> hosts = cluster.getHosts();
          List<String> invalidHostNames = new ArrayList<>();
          List<String> hostNames = new ArrayList<>();

          for (Host host : hosts) {
            hostNames.add(host.getHostName());
          }

          for (Map.Entry<String,String> hostPair : clusterHosts.getValue().entrySet()) {
            if (!hostNames.contains(hostPair.getKey())) {
              invalidHostNames.add(hostPair.getKey());
            }
          }

          if (!invalidHostNames.isEmpty()) {
            throw new AmbariException(String.format("Hostname(s): %s was(were) not found.", StringUtils.join(invalidHostNames, ", ")));
          }
        } else {
          throw new AmbariException(String.format("Cluster %s was not found.", clusterName));
        }
      }
    }
  }

  /*
  * Method updates all properties in all configs,
  * which value contains hostname that should be updated
  * */
  void updateHostsInConfigurations() throws AmbariException {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {

      // going through all clusters with host pairs from file
      for (Map.Entry<String, Map<String,String>> clusterHosts : hostChangesFileMap.entrySet()) {
        boolean hostConfigsUpdated = false;
        String clusterName = clusterHosts.getKey();
        ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);
        Cluster cluster = clusters.getCluster(clusterName);
        Map<String, String> hostMapping = clusterHosts.getValue();
        List<String> currentHostNames = new ArrayList<>();
        String updatedPropertyValue;

        for (Map.Entry<String, String> hostPair : hostMapping.entrySet()) {
          currentHostNames.add(hostPair.getKey());
        }

        //******************************

        if (clusterEntity != null) {
          Collection<ClusterConfigEntity> clusterConfigEntities = clusterEntity.getClusterConfigEntities();
          boolean configUpdated;

          // going through all cluster configs and update property values
          ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
          for (ClusterConfigEntity clusterConfigEntity : clusterConfigEntities) {
            Config config = configFactory.createExisting(cluster, clusterConfigEntity);
            configUpdated = false;

            for (Map.Entry<String,String> property : config.getProperties().entrySet()) {
              updatedPropertyValue = replaceHosts(property.getValue(), currentHostNames, hostMapping);

              if (updatedPropertyValue != null) {
                Map<String,String> propertiesWithUpdates = config.getProperties();
                propertiesWithUpdates.put(property.getKey(), updatedPropertyValue);
                config.setProperties(propertiesWithUpdates);
                configUpdated = true;
              }
            }

            if (configUpdated) {
              hostConfigsUpdated = true;
              config.save();
            }
          }
        }
        if (hostConfigsUpdated) {
          AgentConfigsHolder agentConfigsHolder = injector.getInstance(AgentConfigsHolder.class);
          agentConfigsHolder.updateData(cluster.getClusterId(), currentHostNames.stream()
              .map(hm -> cluster.getHost(hm).getHostId()).collect(Collectors.toList()));
        }

        //******************************
      }
    }
  }

  /*
  * Method replaces current hosts with new hosts in propertyValue
  * */
  private String replaceHosts(String propertyValue, List<String> currentHostNames, Map<String,String> hostMapping) {
    List<String> hostListForReplace;
    String updatedPropertyValue = null;

    hostListForReplace = getHostNamesWhichValueIncludes(currentHostNames, propertyValue);

    if (!hostListForReplace.isEmpty() && hostMapping != null) {
      // sort included hosts
      Collections.sort(hostListForReplace, new StringComparator());

      updatedPropertyValue = propertyValue;
      // create map with replace codes, it will help us to replace every hostname only once
      // replace hosts in value with codes
      Map<String, String> hostNameCode = new HashMap<>();
      int counter = 0;
      for (String hostName : hostListForReplace) {
        String code = String.format("{replace_code_%d}", counter++);
        hostNameCode.put(hostName, code);
        updatedPropertyValue = updatedPropertyValue.replaceAll("(?i)"+ Pattern.quote(hostName), code);
      }

      // replace codes with new host names according to ald host names
      for (String hostName : hostListForReplace) {
        updatedPropertyValue = updatedPropertyValue.replace(hostNameCode.get(hostName), hostMapping.get(hostName));
      }

    }

    return updatedPropertyValue;
  }


  /*
  * Method return host names which are included in value
  * */
  private List<String> getHostNamesWhichValueIncludes(List<String> hostNames, String value) {
    List<String> includedHostNames = new ArrayList<>();

    if (value != null && hostNames != null && !value.isEmpty()) {
      for (String host : hostNames) {
        if (StringUtils.containsIgnoreCase(value, host)) {
          includedHostNames.add(host);
        }
      }
    }

    return includedHostNames;
  }

  /*
  * String comparator. For sorting collection of strings from longer to shorter..
  * */
  public class StringComparator implements Comparator<String> {

    @Override
    public int compare(String s1, String s2) {
      return s2.length() - s1.length();
    }
  }

  /*
  * Method check if security enabled for clusters from file.
  * If enabled for someone, then we will throw exception
  * and put message to log.
  * */
  void checkForSecurity() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    List<String> clustersInSecure = new ArrayList<>();

    if (clusters != null) {
      for (String clusterName : hostChangesFileMap.keySet()) {
        Cluster cluster = clusters.getCluster(clusterName);
        Config clusterEnv = cluster.getDesiredConfigByType(ConfigHelper.CLUSTER_ENV);
        if (clusterEnv != null) {
          String securityEnabled = clusterEnv.getProperties().get(KerberosHelper.SECURITY_ENABLED_PROPERTY_NAME);
          if (securityEnabled.toLowerCase().equals("true")) {
            clustersInSecure.add(clusterName);
          }
        }
      }

      if (!clustersInSecure.isEmpty()) {
        throw new AmbariException(String.format("Cluster(s) %s from file, is(are) in secure mode. Please, turn off security mode.",
                StringUtils.join(clustersInSecure, ", ")));
      }
    }

  }

  /*
  * Method initialize Map with json data from file
  * */
  protected void initHostChangesFileMap() throws AmbariException {
    JsonObject hostChangesJsonObject = configuration.getHostChangesJson(hostChangesFile);
    hostChangesFileMap = new HashMap<>();

    for (Map.Entry<String, JsonElement> clusterEntry : hostChangesJsonObject.entrySet()) {
      try {
        Gson gson = new Gson();
        hostChangesFileMap.put(clusterEntry.getKey(), gson.<Map<String, String>>fromJson(clusterEntry.getValue(), Map.class));
      } catch(Exception e) {
        throw new AmbariException("Error occurred during mapping Json to Map structure. Please check json structure in file.", e);
      }
    }

    // put current host names to lower case
    Map<String, Map<String,String>> newHostChangesFileMap = new HashMap<>();
    for (Map.Entry<String, Map<String,String>> clusterHosts : hostChangesFileMap.entrySet()) {
      Map<String,String> newHostPairs = new HashMap<>();
      for (Map.Entry<String, String> hostPair : clusterHosts.getValue().entrySet()) {
        newHostPairs.put(hostPair.getKey().toLowerCase(), hostPair.getValue().toLowerCase());
      }
      newHostChangesFileMap.put(clusterHosts.getKey(), newHostPairs);
    }
    hostChangesFileMap = newHostChangesFileMap;
  }

  /*
  * Method updates host names in db for hosts table..
  * */
  void updateHostsInDB() {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);

    for (Map.Entry<String, Map<String,String>> clusterHosts : hostChangesFileMap.entrySet()) {
      String clusterName = clusterHosts.getKey();
      Map<String, String> hostMapping = clusterHosts.getValue();
      Map<String, String> toTmpHostMapping = new HashMap<>();
      Map<String, String> fromTmpHostMapping = new HashMap<>();
      for (Map.Entry<String, String> hostPair : hostMapping.entrySet()) {
        toTmpHostMapping.put(hostPair.getKey(), TMP_PREFIX + hostPair.getValue());
        fromTmpHostMapping.put(TMP_PREFIX + hostPair.getValue(), hostPair.getValue());
      }
      ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);
      renameHostsInDB(hostDAO, toTmpHostMapping, clusterEntity);
      renameHostsInDB(hostDAO, fromTmpHostMapping, clusterEntity);
    }
  }

  private void renameHostsInDB(HostDAO hostDAO, Map<String, String> hostMapping, ClusterEntity clusterEntity) {
    List<String> currentHostNames = new ArrayList<>();

    for (Map.Entry<String, String> hostPair : hostMapping.entrySet()) {
      currentHostNames.add(hostPair.getKey());
    }

    if (clusterEntity != null) {
      Collection<HostEntity> hostEntities = clusterEntity.getHostEntities();
      for (HostEntity hostEntity : hostEntities) {
        if (currentHostNames.contains(hostEntity.getHostName())) {
          hostEntity.setHostName(hostMapping.get(hostEntity.getHostName()));
          hostDAO.merge(hostEntity);
        }
      }
    }
  }

  /*
  * Method updates Current Alerts host name and
  * regenerates hash for alert definitions(for alerts to be recreated)
  * */
  void updateHostsForAlertsInDB() {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertsDAO alertsDAO = injector.getInstance(AlertsDAO.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null) {

        for (Map.Entry<String, Map<String,String>> clusterHosts : hostChangesFileMap.entrySet()) {
          List<String> currentHostNames = new ArrayList<>();
          Map<String, String> hostMapping = clusterHosts.getValue();
          Long clusterId = clusterMap.get(clusterHosts.getKey()).getClusterId();

          for (Map.Entry<String, String> hostPair : hostMapping.entrySet()) {
            currentHostNames.add(hostPair.getKey());
          }

          List<AlertCurrentEntity> currentEntities = alertsDAO.findCurrentByCluster(clusterId);
          for (AlertCurrentEntity alertCurrentEntity : currentEntities) {
            AlertHistoryEntity alertHistoryEntity = alertCurrentEntity.getAlertHistory();
            if (currentHostNames.contains(alertHistoryEntity.getHostName())) {
              alertHistoryEntity.setHostName(hostMapping.get(alertHistoryEntity.getHostName()));
              alertsDAO.merge(alertHistoryEntity);
            }
          }

          List<AlertDefinitionEntity> alertDefinitionEntities = alertDefinitionDAO.findAll(clusterId);
          for (AlertDefinitionEntity alertDefinitionEntity : alertDefinitionEntities) {
            alertDefinitionEntity.setHash(UUID.randomUUID().toString());
            alertDefinitionDAO.merge(alertDefinitionEntity);
          }
        }
      }
    }
  }

  /*
  * Method updates hosts for Topology Requests (Blue Prints logic)
  * */
  void updateHostsForTopologyRequests() {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    TopologyRequestDAO topologyRequestDAO = injector.getInstance(TopologyRequestDAO.class);
    TopologyHostRequestDAO topologyHostRequestDAO = injector.getInstance(TopologyHostRequestDAO.class);
    TopologyHostGroupDAO topologyHostGroupDAO = injector.getInstance(TopologyHostGroupDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null) {

        for (Map.Entry<String, Map<String, String>> clusterHosts : hostChangesFileMap.entrySet()) {
          Long clusterId = clusterMap.get(clusterHosts.getKey()).getClusterId();
          List<TopologyRequestEntity> topologyRequestEntities = topologyRequestDAO.findByClusterId(clusterId);
          List<String> currentHostNames = new ArrayList<>();
          Map<String, String> hostMapping = clusterHosts.getValue();

          for (Map.Entry<String, String> hostPair : hostMapping.entrySet()) {
            currentHostNames.add(hostPair.getKey());
          }

          for (TopologyRequestEntity topologyRequestEntity : topologyRequestEntities) {
            TopologyLogicalRequestEntity topologyLogicalRequestEntity = topologyRequestEntity.getTopologyLogicalRequestEntity();
            Collection<TopologyHostGroupEntity> topologyHostGroupEntities = topologyRequestEntity.getTopologyHostGroupEntities();

            // update topology host infos
            if (topologyHostGroupEntities != null) {
              for (TopologyHostGroupEntity topologyHostGroupEntity : topologyHostGroupEntities) {
                Collection<TopologyHostInfoEntity> topologyHostInfoEntities = topologyHostGroupEntity.getTopologyHostInfoEntities();
                boolean updatesAvailable = false;

                for (TopologyHostInfoEntity topologyHostInfoEntity : topologyHostInfoEntities) {
                  if (currentHostNames.contains(topologyHostInfoEntity.getFqdn())) {
                    topologyHostInfoEntity.setFqdn(hostMapping.get(topologyHostInfoEntity.getFqdn()));
                    updatesAvailable = true;
                  }
                }
                if (updatesAvailable) {
                  topologyHostGroupDAO.merge(topologyHostGroupEntity);
                }
              }
            }

            // update topology host requests
            if (topologyLogicalRequestEntity != null) {
              Collection<TopologyHostRequestEntity> topologyHostRequestEntities = topologyLogicalRequestEntity.getTopologyHostRequestEntities();

              if (topologyHostRequestEntities != null) {
                for (TopologyHostRequestEntity topologyHostRequestEntity : topologyHostRequestEntities) {
                  if (currentHostNames.contains(topologyHostRequestEntity.getHostName())) {
                    topologyHostRequestEntity.setHostName(hostMapping.get(topologyHostRequestEntity.getHostName()));
                    topologyHostRequestDAO.merge(topologyHostRequestEntity);
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(new UpdateHelperModule(), new CheckHelperAuditModule(), new LdapModule());
    HostUpdateHelper hostUpdateHelper = injector.getInstance(HostUpdateHelper.class);
    try {
      LOG.info("Host names update started.");

      String hostChangesFile = args[0];

      if (hostChangesFile == null || hostChangesFile.isEmpty()) {
        throw new AmbariException("Path to file with host names changes is empty or null.");
      }
      hostUpdateHelper.setHostChangesFile(hostChangesFile);

      hostUpdateHelper.initHostChangesFileMap();

      hostUpdateHelper.startPersistenceService();

      hostUpdateHelper.validateHostChanges();

      hostUpdateHelper.checkForSecurity();

      hostUpdateHelper.updateHostsInDB();

      hostUpdateHelper.updateHostsForTopologyRequests();

      hostUpdateHelper.updateHostsForAlertsInDB();

      hostUpdateHelper.updateHostsInConfigurations();

      LOG.info("Host names update completed successfully.");

    } catch (AmbariException e) {
        LOG.error("Exception occurred during host names update, failed", e);
        throw e;
    } finally {
      hostUpdateHelper.stopPersistenceService();
    }
  }
}
