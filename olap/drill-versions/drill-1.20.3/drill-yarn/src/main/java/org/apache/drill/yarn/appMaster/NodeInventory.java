/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.yarn.appMaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.NodeReport;

/**
 * Creates an AM-side inventory of cluster nodes. Used to track node
 * reservations (container allocations) to prevent requesting multiple
 * containers on the same node. Tracks blacklisted nodes that have failed too
 * often. Since YARN will discard our blacklist if we add to many nodes, tracks
 * when a container is allocated on a blacklisted node and signals that the
 * cluster is in a bad state.
 */

public class NodeInventory {
  private static final Log LOG = LogFactory.getLog(NodeInventory.class);

  /**
   * Indicates the case in which we've failed so many nodes that YARN has
   * cancelled some of our blacklist entries and we've received a container for
   * a blacklisted node. At this point, we should stop adding new tasks else
   * we'll get into a nasty loop.
   */
  private boolean failed;

  private Map<String, String> nodeMap = new HashMap<>();

  /**
   * The set of nodes available that YARN reports are available.
   * Not clear if these are all nodes in the cluster, or just those usable
   * by the current app (when the app is associated to a queue that
   * uses node labels.)
   */

  private Map<String, NodeReport> yarnNodes = new HashMap<>();

  /**
   * The set of nodes in use by Drill. Includes both nodes on which the AM
   * has requested to run Drillbits, and those nodes found to be running
   * "stray" Drillbits started outside of DoY.
   */

  private Set<String> nodesInUse = new HashSet<>();

  /**
   * Nodes that have failed (typically due to mis-configuration) and
   * are to be excluded from future container requests.
   */

  private Set<String> blacklist = new HashSet<>();
  private final AMYarnFacade yarn;

  public NodeInventory(AMYarnFacade yarn) throws YarnFacadeException {
    this.yarn = yarn;
    buildNodeMap();
  }

  private void buildNodeMap() throws YarnFacadeException {
    List<NodeReport> nodes = yarn.getNodeReports();
    for (NodeReport node : nodes) {
      String hostName = node.getNodeId().getHost();
      nodeMap.put(hostName, node.getHttpAddress());
      yarnNodes.put(hostName, node);
    }
    if (LOG.isInfoEnabled()) {
      LOG.info("YARN Node report");
      for (NodeReport node : nodes) {
        LOG.info("Node: " + node.getHttpAddress() + ", Rack: "
            + node.getRackName() + " has " + node.getCapability().getMemory()
            + " MB, " + node.getCapability().getVirtualCores()
            + " vcores, labels: " + node.getNodeLabels());
      }
    }
  }

  public boolean isFailed() {
    return failed;
  }

  public void reserve(Container container) {
    reserve(container.getNodeId().getHost());
  }

  public void reserve(String hostName) {
    if (blacklist.contains(hostName)) {
      LOG.error( "Node to be reserved is in the blacklist: " + hostName );
      failed = true;
    }
    if (nodesInUse.contains(hostName)) {
      LOG.error( "Node to be reserved is already in use: " + hostName );
      return;
    }
    if (!yarnNodes.containsKey(hostName)) {
      LOG.warn( "Node to be reserved was not in YARN node inventory: " + hostName );
    }
    nodesInUse.add(hostName);
    yarn.blacklistNode(hostName);
  }

  public void release(Container container) {
    release(container.getNodeId().getHost());
  }

  public void release(String hostName) {
    if (!yarnNodes.containsKey(hostName)) {
      return;
    }
    nodesInUse.remove(hostName);
    yarn.removeBlacklist(hostName);
  }

  public void blacklist(String hostName) {
    if (!yarnNodes.containsKey(hostName)) {
      return;
    }
    assert !nodesInUse.contains(hostName);
    blacklist.add(hostName);
    yarn.blacklistNode(hostName);
    LOG.info("Node blacklisted: " + hostName);
  }

  /**
   * Determine the number of free nodes in the YARN cluster. The free set is the
   * set of all YARN nodes minus those that are allocated and those that are
   * blacklisted. Note that a node might be both in use and blacklisted if
   * DoY blacklists a node, but then the user starts a "stray" Drillbit on
   * that same node.
   * <p>
   * This number is an approximation: the set of nodes managed by YARN can
   * change any time, and in-flight container requests will consume a node,
   * but since the request is not yet completed, we don't know which node
   * will be assigned, so the node does not yet appear in the in-use list.
   *
   * @return an approximation of the free node count
   */

  public int getFreeNodeCount() {
    Set<String> free = new HashSet<>( );
    free.addAll( yarnNodes.keySet() );
    free.removeAll( nodesInUse );
    free.removeAll( blacklist );
    return free.size( );
  }

  /**
   * Return a copy of the blacklist (list of failed nodes) for use in display
   * to the user or similar purpose.
   *
   * @return a copy of the blacklist.
   */

  public List<String> getBlacklist() {
    List<String> copy = new ArrayList<>( );
    copy.addAll(blacklist);
    return copy;
  }

  /**
   * Report if the given host name is in use.
   *
   * @param hostName
   * @return true if the host is reserved (in use by a container) or
   * blacklisted (failed.)
   */

  public boolean isInUse(String hostName) {
    return blacklist.contains(hostName) || nodesInUse.contains(hostName);
  }
}
