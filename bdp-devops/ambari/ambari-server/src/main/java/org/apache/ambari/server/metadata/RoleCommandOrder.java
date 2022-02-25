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
package org.apache.ambari.server.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stageplanner.RoleGraphNode;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * This class is used to establish the order between two roles. This class
 * should not be used to determine the dependencies.
 */
public class RoleCommandOrder implements Cloneable {

  @Inject AmbariMetaInfo ambariMetaInfo;

  private final static Logger LOG =
			LoggerFactory.getLogger(RoleCommandOrder.class);

  /**
   * The section names used to add overides in addition to the
   * {@link #GENERAL_DEPS_KEY} section.
   */
  private LinkedHashSet<String> sectionKeys;

  private final static String GENERAL_DEPS_KEY = "general_deps";
  public final static String GLUSTERFS_DEPS_KEY = "optional_glusterfs";
  public final static String NO_GLUSTERFS_DEPS_KEY = "optional_no_glusterfs";
  public final static String NAMENODE_HA_DEPS_KEY = "namenode_optional_ha";
  public final static String RESOURCEMANAGER_HA_DEPS_KEY = "resourcemanager_optional_ha";
  public final static String COMMENT_STR = "_comment";

  /**
   * Commands that are independent, role order matters
   */
  private static final Set<RoleCommand> independentCommands = Sets.newHashSet(RoleCommand.START,
      RoleCommand.EXECUTE, RoleCommand.SERVICE_CHECK);

  /**
   * key -> blocked role command value -> set of blocker role commands.
   */
  private Map<RoleCommandPair, Set<RoleCommandPair>> dependencies = new HashMap<>();

  /**
   * Add a pair of tuples where the tuple defined by the first two parameters are blocked on
   * the tuple defined by the last two pair.
   * @param blockedRole Role that is blocked
   * @param blockedCommand The command on the role that is blocked
   * @param blockerRole The role that is blocking
   * @param blockerCommand The command on the blocking role
   */
  private void addDependency(Role blockedRole,
      RoleCommand blockedCommand, Role blockerRole, RoleCommand blockerCommand,
      boolean overrideExisting) {
    RoleCommandPair rcp1 = new RoleCommandPair(blockedRole, blockedCommand);
    RoleCommandPair rcp2 = new RoleCommandPair(blockerRole, blockerCommand);

    if (dependencies.get(rcp1) == null || overrideExisting) {
      dependencies.put(rcp1, new HashSet<>());
    }

    dependencies.get(rcp1).add(rcp2);
  }

  void addDependencies(Map<String, Object> jsonSection) {
    if(jsonSection == null) {
      return;
    }

    for (Object blockedObj : jsonSection.keySet()) {
      String blocked = (String) blockedObj;
      if (COMMENT_STR.equals(blocked)) {
        continue; // Skip comments
      }
      ArrayList<String> blockers = (ArrayList<String>) jsonSection.get(blocked);
      for (String blocker : blockers) {
        String [] blockedTuple = blocked.split("-");
        String blockedRole = blockedTuple[0];
        String blockedCommand = blockedTuple[1];

        // 3rd position is -OVERRIDE
        boolean overrideExisting = blockedTuple.length == 3;

        String [] blockerTuple = blocker.split("-");
        String blockerRole = blockerTuple[0];
        String blockerCommand = blockerTuple[1];

        addDependency(Role.valueOf(blockedRole), RoleCommand.valueOf(blockedCommand),
            Role.valueOf(blockerRole), RoleCommand.valueOf(blockerCommand), overrideExisting);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void initialize(Cluster cluster, LinkedHashSet<String> sectionKeys) {

    // in the event that initialize is called twice, ensure that we start with a
    // clean RCO instance
    this.sectionKeys = sectionKeys;
    dependencies.clear();

    Set<StackId> stackIds = new HashSet<>();
    for (Service service : cluster.getServices().values()) {
      stackIds.add(service.getDesiredStackId());
    }

    for (StackId stackId : stackIds) {
      StackInfo stack;
      try {
        stack = ambariMetaInfo.getStack(stackId.getStackName(),
          stackId.getStackVersion());
      } catch (AmbariException ignored) {
        throw new NullPointerException("Stack not found: " + stackId);
      }

      Map<String,Object> userData = stack.getRoleCommandOrder().getContent();
      Map<String,Object> generalSection =
        (Map<String, Object>) userData.get(GENERAL_DEPS_KEY);

      addDependencies(generalSection);

      for (String sectionKey : sectionKeys) {
        Map<String, Object> section = (Map<String, Object>) userData.get(sectionKey);

        addDependencies(section);
      }
    }

    extendTransitiveDependency();
    addMissingRestartDependencies();
  }

  /**
   * Returns the dependency order. -1 => rgn1 before rgn2, 0 => they can be
   * parallel 1 => rgn2 before rgn1
   *
   * @param rgn1 roleGraphNode1
   * @param rgn2 roleGraphNode2
   */
  public int order(RoleGraphNode rgn1, RoleGraphNode rgn2) {
    RoleCommandPair rcp1 = new RoleCommandPair(rgn1.getRole(),
        rgn1.getCommand());
    RoleCommandPair rcp2 = new RoleCommandPair(rgn2.getRole(),
        rgn2.getCommand());
    if ((dependencies.get(rcp1) != null)
        && (dependencies.get(rcp1).contains(rcp2))) {
      return 1;
    } else if ((dependencies.get(rcp2) != null)
        && (dependencies.get(rcp2).contains(rcp1))) {
      return -1;
    } else if (!rgn2.getCommand().equals(rgn1.getCommand())) {
      return compareCommands(rgn1, rgn2);
    }
    return 0;
  }

  /**
   * Returns transitive dependencies as a services list
   * @param service to check if it depends on another services
   * @return tramsitive services
   */
  public Set<Service> getTransitiveServices(Service service, RoleCommand cmd)
    throws AmbariException {

    Set<Service> transitiveServices = new HashSet<>();
    Cluster cluster = service.getCluster();

    Set<RoleCommandPair> allDeps = new HashSet<>();
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      RoleCommandPair rcp = new RoleCommandPair(Role.valueOf(sc.getName()), cmd);
      Set<RoleCommandPair> deps = dependencies.get(rcp);
      if (deps != null) {
        allDeps.addAll(deps);
      }
    }

    for (Service s : cluster.getServices().values()) {
      for (RoleCommandPair rcp : allDeps) {
        ServiceComponent sc = s.getServiceComponents().get(rcp.getRole().toString());
        if (sc != null) {
          transitiveServices.add(s);
          break;
        }
      }
    }

    return transitiveServices;
  }

  /**
   * Adds transitive dependencies to each node.
   * A => B and B => C implies A => B,C and B => C
   */
  private void extendTransitiveDependency() {
    for (Map.Entry<RoleCommandPair, Set<RoleCommandPair>> roleCommandPairSetEntry : dependencies.entrySet()) {
      HashSet<RoleCommandPair> visited = new HashSet<>();
      HashSet<RoleCommandPair> transitiveDependencies = new HashSet<>();
      for (RoleCommandPair directlyBlockedOn : dependencies.get(roleCommandPairSetEntry.getKey())) {
        visited.add(directlyBlockedOn);
        identifyTransitiveDependencies(directlyBlockedOn, visited, transitiveDependencies);
      }
      if (transitiveDependencies.size() > 0) {
        dependencies.get(roleCommandPairSetEntry.getKey()).addAll(transitiveDependencies);
      }
    }
  }

  private void identifyTransitiveDependencies(RoleCommandPair rcp, HashSet<RoleCommandPair> visited,
                                                     HashSet<RoleCommandPair> transitiveDependencies) {
    if (dependencies.get(rcp) != null) {
      for (RoleCommandPair blockedOn : dependencies.get(rcp)) {
        if (!visited.contains(blockedOn)) {
          visited.add(blockedOn);
          transitiveDependencies.add(blockedOn);
          identifyTransitiveDependencies(blockedOn, visited, transitiveDependencies);
        }
      }
    }
  }

  /**
   * RoleCommand.RESTART dependencies that are missing from role_command_order.json
   * will be added to the RCO graph to make sure RESTART ALL type of
   * operations respect the RCO. Only those @{@link RoleCommandPair} will be
   * added which do not have any RESTART definition in the role_command_order.json
   * by copying dependencies from the START operation.
   */
  private void addMissingRestartDependencies() {
    Map<RoleCommandPair, Set<RoleCommandPair>> missingDependencies = new HashMap<>();
    for (Map.Entry<RoleCommandPair, Set<RoleCommandPair>> roleCommandPairSetEntry : dependencies.entrySet()) {
      RoleCommandPair roleCommandPair = roleCommandPairSetEntry.getKey();
      if (roleCommandPair.getCmd().equals(RoleCommand.START)) {
        RoleCommandPair restartPair = new RoleCommandPair(roleCommandPair.getRole(), RoleCommand.RESTART);
        if (!dependencies.containsKey(restartPair)) {
          // Assumption that if defined the RESTART deps are complete
          Set<RoleCommandPair> roleCommandDeps = new HashSet<>();
          for (RoleCommandPair rco : roleCommandPairSetEntry.getValue()) {
            // Change dependency Role to match source
            roleCommandDeps.add(new RoleCommandPair(rco.getRole(), RoleCommand.RESTART));
          }

          if (LOG.isDebugEnabled()) {
            LOG.debug("Adding dependency for {}, dependencies => {}", restartPair, roleCommandDeps);
          }
          missingDependencies.put(restartPair, roleCommandDeps);
        }
      }
    }
    if (!missingDependencies.isEmpty()) {
      dependencies.putAll(missingDependencies);
    }
  }

  private int compareCommands(RoleGraphNode rgn1, RoleGraphNode rgn2) {
    // TODO: add proper order comparison support for RoleCommand.ACTIONEXECUTE

    RoleCommand rc1 = rgn1.getCommand();
    RoleCommand rc2 = rgn2.getCommand();
    if (rc1.equals(rc2)) {
      //If its coming here means roles have no dependencies.
      return 0;
    }

    if (independentCommands.contains(rc1) && independentCommands.contains(rc2)) {
      return 0;
    }

    if (rc1.equals(RoleCommand.INSTALL)) {
      return -1;
    } else if (rc2.equals(RoleCommand.INSTALL)) {
      return 1;
    } else if (rc1.equals(RoleCommand.START) || rc1.equals(RoleCommand.EXECUTE)
            || rc1.equals(RoleCommand.SERVICE_CHECK)) {
      return -1;
    } else if (rc2.equals(RoleCommand.START) || rc2.equals(RoleCommand.EXECUTE)
            || rc2.equals(RoleCommand.SERVICE_CHECK)) {
      return 1;
    } else if (rc1.equals(RoleCommand.STOP)) {
      return -1;
    } else if (rc2.equals(RoleCommand.STOP)) {
      return 1;
    }
    return 0;
  }

  public int compareDeps(RoleCommandOrder rco) {
    Set<RoleCommandPair> v1;
    Set<RoleCommandPair> v2;
    if (this == rco) {
      return 0;
    }

    // Check for key set match
    if (!dependencies.keySet().equals(rco.dependencies.keySet())){
      LOG.debug("dependency keysets differ");
      return 1;
    }
    LOG.debug("dependency keysets match");

    // So far so good.  Since the keysets match, let's check the
    // actual entries against each other
    for (Map.Entry<RoleCommandPair, Set<RoleCommandPair>> roleCommandPairSetEntry : dependencies.entrySet()) {
      v1 = dependencies.get(roleCommandPairSetEntry.getKey());
      v2 = rco.dependencies.get(roleCommandPairSetEntry.getKey());
      if (!v1.equals(v2)) {
        LOG.debug("different entry found for key ({}, {})", roleCommandPairSetEntry.getKey().getRole(), roleCommandPairSetEntry.getKey().getCmd());
        return 1;
      }
    }
    LOG.debug("dependency entries match");
    return 0;
  }

  /**
   * Gets the collection of section names that was used to initialize this
   * {@link RoleCommandOrder} instance. If this instance has not been
   * initialized, this will be {@code null}.
   * <p/>
   * The ordering of this collection is maintained.
   *
   * @return the section keys used to initialize this instance or {@code null}
   *         if it has not been initialized.
   */
  public LinkedHashSet<String> getSectionKeys() {
    return sectionKeys;
  }

  /**
   * For test purposes
   */
  public Map<RoleCommandPair, Set<RoleCommandPair>> getDependencies() {
    return dependencies;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    RoleCommandOrder clone = (RoleCommandOrder) super.clone();
    clone.sectionKeys = new LinkedHashSet<>(sectionKeys);
    clone.dependencies = new HashMap<>(dependencies);

    return clone;
  }
}
