/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology.validators;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.TopologyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * Validates NAMENODE HA setup correctness.
 */
public class NameNodeHaValidator implements TopologyValidator {

  private static final Logger LOG = LoggerFactory.getLogger(NameNodeHaValidator.class);
  private static final Splitter SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {
    if( topology.isNameNodeHAEnabled() ) {

      if (!allHostNamesAreKnown(topology)) {
        LOG.warn("Provision cluster template contains hostgroup(s) without explicit hostnames. Cannot validate NAMENODE HA setup in this case.");
        return;
      }

      Collection<String> nnHosts = topology.getHostAssignmentsForComponent("NAMENODE");

      if (nnHosts.isEmpty()) {
        LOG.info("NAMENODE HA is enabled but there are no NAMENODE components in the cluster. Assuming external name nodes.");
        validateExternalNamenodeHa(topology);
      }
      else if (nnHosts.size() == 1) {
        throw new InvalidTopologyException("NAMENODE HA requires at least two hosts running NAMENODE but there is only one: " +
          nnHosts.iterator().next());
      }

      Map<String, String> hadoopEnvConfig = topology.getConfiguration().getFullProperties().get("hadoop-env");
      if(hadoopEnvConfig != null && !hadoopEnvConfig.isEmpty() && hadoopEnvConfig.containsKey("dfs_ha_initial_namenode_active") && hadoopEnvConfig.containsKey("dfs_ha_initial_namenode_standby")) {
        if((!HostGroup.HOSTGROUP_REGEX.matcher(hadoopEnvConfig.get("dfs_ha_initial_namenode_active")).matches() && !nnHosts.contains(hadoopEnvConfig.get("dfs_ha_initial_namenode_active")))
          || (!HostGroup.HOSTGROUP_REGEX.matcher(hadoopEnvConfig.get("dfs_ha_initial_namenode_standby")).matches() && !nnHosts.contains(hadoopEnvConfig.get("dfs_ha_initial_namenode_standby")))){
          throw new InvalidTopologyException("NAMENODE HA hosts mapped incorrectly for properties 'dfs_ha_initial_namenode_active' and 'dfs_ha_initial_namenode_standby'. Expected hosts are: " + nnHosts);
        }
      }
    }

  }

  private boolean allHostNamesAreKnown(ClusterTopology topology) {
    return topology.getHostGroupInfo().values().stream().allMatch(
      hg -> !hg.getHostNames().isEmpty());
  }

  /**
   * Verifies that all respective properties are set for an external NANENODE HA setup
   */
  public void validateExternalNamenodeHa(ClusterTopology topology) throws InvalidTopologyException {
    Map<String, String> hdfsSite = topology.getConfiguration().getFullProperties().get("hdfs-site");
    Set<String> hosts = topology.getAllHosts();

    for (Map.Entry<String, List<String>> entry: getNameServicesToNameNodes(hdfsSite).entrySet()) {
      String nameService = entry.getKey();
      List<String> nameNodes = entry.getValue();
      for (String nameNode: nameNodes) {
        String address = hdfsSite.get("dfs.namenode.rpc-address." + nameService + "." + nameNode);
        checkValidExternalNameNodeAddress(nameService, nameNode, address, hosts);
      }
    }
  }

  private void checkValidExternalNameNodeAddress(String nameService, String nameNode, String address, Set<String> hosts) throws InvalidTopologyException {
    final String errorMessage = "Illegal external HA NAMENODE address for name service [%s], namenode [%s]: [%s]. Address " +
      "must be in <host>:<port> format where host is external to the cluster.";

    checkInvalidTopology(
      Strings.isNullOrEmpty(address) || address.contains("localhost") || address.contains("%HOSTGROUP") || !address.contains(":"),
      errorMessage, nameService, nameNode, address);

    String hostName = address.substring(0, address.indexOf(':'));

    checkInvalidTopology(hostName.isEmpty() || hosts.contains(hostName),
      errorMessage, nameService, nameNode, address);
  }

  Map<String, List<String>> getNameServicesToNameNodes(Map<String, String> hdfsSite) throws InvalidTopologyException {
    String nameServices = null != hdfsSite.get("dfs.internal.nameservices") ?
      hdfsSite.get("dfs.internal.nameservices") : hdfsSite.get("dfs.nameservices");
    Map<String, List<String>> nameServicesToNameNodes = new HashMap<>();

    for (String ns: SPLITTER.splitToList(nameServices)) {
      String nameNodes = hdfsSite.get("dfs.ha.namenodes." + ns);
      checkInvalidTopology(Strings.isNullOrEmpty(nameNodes),
        "No namenodes specified for nameservice %s.", ns);
      nameServicesToNameNodes.put(ns, SPLITTER.splitToList(nameNodes));
    }

    return nameServicesToNameNodes;
  }


  private void checkInvalidTopology(boolean errorCondition, String errorMessageTemplate, Object... errorMessageParams) throws InvalidTopologyException {
    if (errorCondition) throw new InvalidTopologyException(String.format(errorMessageTemplate, errorMessageParams));
  }

}
