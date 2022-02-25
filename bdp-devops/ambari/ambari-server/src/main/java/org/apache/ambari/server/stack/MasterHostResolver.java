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

package org.apache.ambari.server.stack;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.ExecuteHostType;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.utils.HTTPUtils;
import org.apache.ambari.server.utils.HostAndPort;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

/**
 * Class that determines hosts that are active or standby for services that
 * support HA
 */
@Experimental(feature=ExperimentalFeature.REFACTOR_TO_SPI)
public class MasterHostResolver {

  private static final Logger LOG = LoggerFactory.getLogger(MasterHostResolver.class);

  private final UpgradeContext m_upgradeContext;
  private final Cluster m_cluster;
  private final ConfigHelper m_configHelper;

  public enum Service {
    HDFS,
    HBASE,
    YARN,
    OTHER;

    public static Service fromString(String serviceName) {
      try {
        return valueOf(serviceName.toUpperCase());
      } catch (Exception ignore) {
        return OTHER;
      }
    }
  }

  /**
   * Union of status for several services.
   */
  protected enum Status {
    ACTIVE,
    STANDBY
  }

  /**
   * Constructor.
   *
   * @param configHelper
   *          Configuration Helper
   * @param upgradeContext
   *          the upgrade context
   */
  public MasterHostResolver(Cluster cluster, ConfigHelper configHelper, UpgradeContext upgradeContext) {
    m_configHelper = configHelper;
    m_upgradeContext = upgradeContext;
    m_cluster = cluster;
  }

  /**
   * Gets the cluster that this instance of the {@link MasterHostResolver} is
   * initialized with.
   *
   * @return the cluster (not {@code null}).
   */
  public Cluster getCluster() {
    return m_cluster;
  }

  /**
   * Get the master hostname of the given service and component.
   * @param serviceName Service
   * @param componentName Component
   * @return The hostname that is the master of the service and component if successful, null otherwise.
   */
  public HostsType getMasterAndHosts(String serviceName, String componentName) {
    if (serviceName == null || componentName == null) {
      return null;
    }
    LinkedHashSet<String> componentHosts = new LinkedHashSet<>(m_cluster.getHosts(serviceName, componentName));
    if (componentHosts.isEmpty()) {
      return null;
    }

    HostsType hostsType = HostsType.normal(componentHosts);

    try {
      switch (Service.fromString(serviceName)) {
        case HDFS:
          if (componentName.equalsIgnoreCase("NAMENODE") && componentHosts.size() >= 2) {
            try {
              hostsType = HostsType.federated(nameSpaces(componentHosts), componentHosts);
            } catch (ClassifyNameNodeException | IllegalArgumentException e) {
              if (componentHosts.size() == 2) { // in HA mode guess if cannot determine active/standby
                hostsType = HostsType.guessHighAvailability(componentHosts);
                LOG.warn(
                  "Could not determine the active/standby states from NameNodes {}. Using {} as active and {} as standbys.",
                  componentHosts, hostsType.getMasters(), hostsType.getSecondaries());
              } else {
                // XXX fallback to HostsType.normal unsure how to handle this
                LOG.warn("Could not determine the active/standby states of federated NameNode from NameNodes {}.", componentHosts);
              }
            }
          }
          break;
        case YARN:
          if (componentName.equalsIgnoreCase("RESOURCEMANAGER")) {
            hostsType = resolveResourceManagers(getCluster(), componentHosts);
          }
          break;
        case HBASE:
          if (componentName.equalsIgnoreCase("HBASE_MASTER")) {
            hostsType = resolveHBaseMasters(getCluster(), componentHosts);
          }
          break;
        default:
          break;
      }
    } catch (Exception err) {
      LOG.error("Unable to get master and hosts for Component " + componentName + ". Error: " + err.getMessage(), err);
    }
    return filterHosts(hostsType, serviceName, componentName);
  }

  /**
   * Gets hosts which match the supplied criteria.
   *
   * @param cluster
   * @param executeHostType
   * @param serviceName
   * @param componentName
   * @return
   */
  public static Collection<Host> getCandidateHosts(Cluster cluster, ExecuteHostType executeHostType,
      String serviceName, String componentName) {
    Collection<Host> candidates = cluster.getHosts();
    if (StringUtils.isNotBlank(serviceName) && StringUtils.isNotBlank(componentName)) {
      List<ServiceComponentHost> schs = cluster.getServiceComponentHosts(serviceName,componentName);
      candidates = schs.stream().map(sch -> sch.getHost()).collect(Collectors.toList());
    }

    if (candidates.isEmpty()) {
      return candidates;
    }

    // figure out where to add the new component
    List<Host> winners = Lists.newArrayList();
    switch (executeHostType) {
      case ALL:
        winners.addAll(candidates);
        break;
      case FIRST:
        winners.add(candidates.iterator().next());
        break;
      case MASTER:
        winners.add(candidates.iterator().next());
        break;
      case ANY:
        winners.add(candidates.iterator().next());
        break;
    }

    return winners;
  }

  /**
   * Filters the supplied list of hosts in the following ways:
   * <ul>
   * <li>Compares the versions of a HostComponent to the version for the
   * resolver. Only versions that do not match are retained.</li>
   * <li>Removes unhealthy hosts in maintenance mode from the list of healthy
   * hosts</li>
   * </ul>
   *
   * @param hostsType
   *          the hosts to resolve
   * @param service
   *          the service name
   * @param component
   *          the component name
   * @return the modified hosts instance with filtered and unhealthy hosts
   *         filled
   */
  private HostsType filterHosts(HostsType hostsType, String service, String component) {
    try {
      org.apache.ambari.server.state.Service svc = m_cluster.getService(service);
      ServiceComponent sc = svc.getServiceComponent(component);

      // !!! not really a fan of passing these around
      List<ServiceComponentHost> unhealthyHosts = new ArrayList<>();
      LinkedHashSet<String> upgradeHosts = new LinkedHashSet<>();

      for (String hostName : hostsType.getHosts()) {
        ServiceComponentHost sch = sc.getServiceComponentHost(hostName);
        Host host = sch.getHost();
        MaintenanceState maintenanceState = host.getMaintenanceState(sch.getClusterId());

        // !!! FIXME: only rely on maintenance state once the upgrade endpoint
        // is using the pre-req endpoint for determining if an upgrade is
        // possible
        if (maintenanceState != MaintenanceState.OFF) {
          unhealthyHosts.add(sch);
          continue;
        }

        if (sch.getUpgradeState() == UpgradeState.FAILED) {
          upgradeHosts.add(hostName);
          continue;
        }

        if(m_upgradeContext.getDirection() == Direction.UPGRADE){
          RepositoryVersionEntity targetRepositoryVersion = m_upgradeContext.getRepositoryVersion();
          if (!StringUtils.equals(targetRepositoryVersion.getVersion(), sch.getVersion())) {
            upgradeHosts.add(hostName);
          }

          continue;
        }

        // it's a downgrade ...
        RepositoryVersionEntity downgradeToRepositoryVersion = m_upgradeContext.getTargetRepositoryVersion(service);
        String downgradeToVersion = downgradeToRepositoryVersion.getVersion();
        if (!StringUtils.equals(downgradeToVersion, sch.getVersion())) {
          upgradeHosts.add(hostName);
          continue;
        }
      }

      hostsType.unhealthy = unhealthyHosts;
      hostsType.setHosts(upgradeHosts);

      return hostsType;
    } catch (AmbariException e) {
      // !!! better not
      LOG.warn("Could not determine host components to upgrade. Defaulting to saved hosts.", e);
      return hostsType;
    }
  }

  /**
   * Determine if HDFS is present and it has NameNode High Availability.
   * @return true if has NameNode HA, otherwise, false.
   */
  public boolean isNameNodeHA() throws AmbariException {
    Map<String, org.apache.ambari.server.state.Service> services = m_cluster.getServices();
    if (services != null && services.containsKey("HDFS")) {

      Set<String> secondaryNameNodeHosts = m_cluster.getHosts("HDFS", "SECONDARY_NAMENODE");
      Set<String> nameNodeHosts = m_cluster.getHosts("HDFS", "NAMENODE");

      if (secondaryNameNodeHosts.size() == 1 && nameNodeHosts.size() == 1) {
        return false;
      }
      if (nameNodeHosts.size() > 1) {
        return true;
      }

      throw new AmbariException("Unable to determine if cluster has NameNode HA.");
    }
    return false;
  }

  /**
   * Get the NameNode NameSpaces (master->secondaries hosts).
   * In each NameSpace there should be exactly 1 master and at least one secondary host.
   */
  private List<HostsType.HighAvailabilityHosts> nameSpaces(Set<String> componentHosts) {
    return NameService.fromConfig(m_configHelper, getCluster()).stream()
      .map(each -> findMasterAndSecondaries(each, componentHosts))
      .collect(Collectors.toList());

  }

  /**
   * Find Config value for current Cluster using configType and propertyName
   *
   * @param configType   Config Type
   * @param propertyName Property Name
   * @return Value of property if present else null
   */
  public String getValueFromDesiredConfigurations(final String configType, final String propertyName) {
    return m_configHelper.getValueFromDesiredConfigurations(m_cluster, configType, propertyName);
  }

  /**
   * Find the master and secondary namenode(s) based on JMX NameNodeStatus.
   */
  private HostsType.HighAvailabilityHosts findMasterAndSecondaries(NameService nameService, Set<String> componentHosts) throws ClassifyNameNodeException {
    String master = null;
    List<String> secondaries = new ArrayList<>();
    for (NameService.NameNode nameNode : nameService.getNameNodes()) {
      checkForDualNetworkCards(componentHosts, nameNode);
      String state = queryJmxBeanValue(nameNode.getHost(), nameNode.getPort(), "Hadoop:service=NameNode,name=NameNodeStatus", "State", true, nameNode.isEncrypted());
      if (Status.ACTIVE.toString().equalsIgnoreCase(state)) {
        master = nameNode.getHost();
      } else if (Status.STANDBY.toString().equalsIgnoreCase(state)) {
        secondaries.add(nameNode.getHost());
      } else {
        LOG.error(String.format("Could not retrieve state for NameNode %s from property %s by querying JMX.", nameNode.getHost(), nameNode.getPropertyName()));
      }
    }
    if (masterAndSecondariesAreFound(componentHosts, master, secondaries)) {
      return new HostsType.HighAvailabilityHosts(master, secondaries);
    }
    throw new ClassifyNameNodeException(nameService);
  }

  private static void checkForDualNetworkCards(Set<String> componentHosts, NameService.NameNode nameNode) {
    if (!componentHosts.contains(nameNode.getHost())) {
      //This may happen when NN HA is configured on dual network card machines with public/private FQDNs.
      LOG.error(
        MessageFormat.format(
          "Hadoop NameNode HA configuration {0} contains host {1} that does not exist in the NameNode hosts list {3}",
          nameNode.getPropertyName(), nameNode.getHost(), componentHosts.toString()));
    }
  }

  private static boolean masterAndSecondariesAreFound(Set<String> componentHosts, String master, List<String> secondaries) {
    return master != null && secondaries.size() + 1 == componentHosts.size() && !secondaries.contains(master);
  }

  private HostAndPort parseHostPort(Cluster cluster, String propertyName, String configType) throws MalformedURLException {
    String propertyValue = m_configHelper.getValueFromDesiredConfigurations(cluster, configType, propertyName);
    HostAndPort hp = HTTPUtils.getHostAndPortFromProperty(propertyValue);
    if (hp == null) {
      throw new MalformedURLException("Could not parse host and port from " + propertyValue);
    }
    return hp;
  }

  /**
   * Resolve the name of the Resource Manager master and convert the hostname to lowercase.
   */
  private HostsType resolveResourceManagers(Cluster cluster, Set<String> hosts) throws MalformedURLException {
    String master = null;
    LinkedHashSet<String> orderedHosts = new LinkedHashSet<>(hosts);

    // IMPORTANT, for RM, only the master returns jmx
    HostAndPort hp = parseHostPort(cluster, "yarn.resourcemanager.webapp.address", ConfigHelper.YARN_SITE);

    for (String hostname : hosts) {
      String value = queryJmxBeanValue(hostname, hp.port,
          "Hadoop:service=ResourceManager,name=RMNMInfo", "modelerType", true);

      if (null != value) {
        if (master != null) {
          master = hostname.toLowerCase();
        }

        // Quick and dirty to make sure the master is last in the list
        orderedHosts.remove(hostname.toLowerCase());
        orderedHosts.add(hostname.toLowerCase());
      }

    }
    return HostsType.from(master, null, orderedHosts);
  }

  /**
   * Resolve the HBASE master and convert the hostname to lowercase.
   */
  private HostsType resolveHBaseMasters(Cluster cluster, Set<String> hosts) throws AmbariException {
    String master = null;
    String secondary = null;
    String hbaseMasterInfoPortProperty = "hbase.master.info.port";
    String hbaseMasterInfoPortValue = m_configHelper.getValueFromDesiredConfigurations(cluster, ConfigHelper.HBASE_SITE, hbaseMasterInfoPortProperty);

    if (hbaseMasterInfoPortValue == null || hbaseMasterInfoPortValue.isEmpty()) {
      throw new AmbariException("Could not find property " + hbaseMasterInfoPortProperty);
    }

    final int hbaseMasterInfoPort = Integer.parseInt(hbaseMasterInfoPortValue);
    for (String hostname : hosts) {
      String value = queryJmxBeanValue(hostname, hbaseMasterInfoPort,
          "Hadoop:service=HBase,name=Master,sub=Server", "tag.isActiveMaster", false);

      if (null != value) {
        Boolean bool = Boolean.valueOf(value);
        if (bool.booleanValue()) {
          master = hostname.toLowerCase();
        } else {
          secondary = hostname.toLowerCase();
        }
      }
    }
    return HostsType.from(master, secondary, new LinkedHashSet<>(hosts));
  }

  protected String queryJmxBeanValue(String hostname, int port, String beanName, String attributeName,
                                  boolean asQuery) {
    return queryJmxBeanValue(hostname, port, beanName, attributeName, asQuery, false);
  }

  /**
   * Query the JMX attribute at http(s)://$server:$port/jmx?qry=$query or http(s)://$server:$port/jmx?get=$bean::$attribute
   * @param hostname host name
   * @param port port number
   * @param beanName if asQuery is false, then search for this bean name
   * @param attributeName if asQuery is false, then search for this attribute name
   * @param asQuery whether to search bean or query
   * @param encrypted true if using https instead of http.
   * @return The jmx value.
   */
  protected String queryJmxBeanValue(String hostname, int port, String beanName, String attributeName,
                                  boolean asQuery, boolean encrypted) {

    String protocol = encrypted ? "https://" : "http://";
    String endPoint = protocol + (asQuery ?
        String.format("%s:%s/jmx?qry=%s", hostname, port, beanName) :
        String.format("%s:%s/jmx?get=%s::%s", hostname, port, beanName, attributeName));

    String response = HTTPUtils.requestURL(endPoint);

    if (null == response || response.isEmpty()) {
      return null;
    }

    Type type = new TypeToken<Map<String, ArrayList<HashMap<String, String>>>>() {}.getType();

    try {
      Map<String, ArrayList<HashMap<String, String>>> jmxBeans =
          StageUtils.getGson().fromJson(response, type);

      return jmxBeans.get("beans").get(0).get(attributeName);
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Could not load JMX from {}/{} from {}", beanName, attributeName, hostname, e);
      } else {
        LOG.debug("Could not load JMX from {}/{} from {}", beanName, attributeName, hostname);
      }
    }

    return null;
  }
}
