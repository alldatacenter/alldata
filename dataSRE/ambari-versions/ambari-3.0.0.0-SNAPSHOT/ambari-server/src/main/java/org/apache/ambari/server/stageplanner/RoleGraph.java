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
package org.apache.ambari.server.stageplanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.CommandExecutionType;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class RoleGraph {

  private static final Logger LOG = LoggerFactory.getLogger(RoleGraph.class);

  Map<String, RoleGraphNode> graph = null;
  private RoleCommandOrder roleDependencies;
  private Stage initialStage = null;
  private boolean sameHostOptimization = true;
  private CommandExecutionType commandExecutionType = CommandExecutionType.STAGE;

  @Inject
  private StageFactory stageFactory;

  /**
   * Used for created {@link HostRoleCommand}s when building structures to
   * represent an ordered set of stages.
   */
  @Inject
  private HostRoleCommandFactory hrcFactory;

  @AssistedInject
  public RoleGraph() {
  }

  @AssistedInject
  public RoleGraph(@Assisted RoleCommandOrder rd) {
    roleDependencies = rd;
  }

  public CommandExecutionType getCommandExecutionType() {
    return commandExecutionType;
  }

  public void setCommandExecutionType(CommandExecutionType commandExecutionType) {
    this.commandExecutionType = commandExecutionType;
  }

  /**
   * Given a stage builds a DAG of all execution commands within the stage.
   *
   * @see #getStages()
   */
  public void build(Stage stage) {
    if (stage == null) {
      throw new IllegalArgumentException("Null stage");
    }

    if (commandExecutionType == CommandExecutionType.DEPENDENCY_ORDERED) {
      LOG.info("Build stage with DEPENDENCY_ORDERED commandExecutionType: {} ",
          stage.getRequestContext());
    }

    initialStage = stage;

    Map<String, Map<String, HostRoleCommand>> hostRoleCommands = stage.getHostRoleCommands();
    build(hostRoleCommands);
  }

  /**
   * Initializes {@link #graph} with the supplied unordered commands. The
   * commands specified are in the following format: Input:
   * 
   * <pre>
   * {c6401={NAMENODE=STOP}, c6402={DATANODE=STOP}, NODEMANAGER=STOP}}
   * </pre>
   *
   * @param hostRoleCommands
   *          the unordered commands to build a DAG from. The map is keyed first
   *          by host and the for each host it is keyed by {@link Role} to
   *          {@link RoleCommand}.
   */
  private void build(Map<String, Map<String, HostRoleCommand>> hostRoleCommands) {
    graph = new TreeMap<>();

    for (String host : hostRoleCommands.keySet()) {
      for (String role : hostRoleCommands.get(host).keySet()) {
        HostRoleCommand hostRoleCommand = hostRoleCommands.get(host).get(role);
        RoleGraphNode rgn;
        if (graph.get(role) == null) {
          rgn = new RoleGraphNode(hostRoleCommand.getRole(),
              getRoleCommand(hostRoleCommand));
          graph.put(role, rgn);
        }
        rgn = graph.get(role);
        rgn.addHost(host);
      }
    }

    // In case commandExecutionType == DEPENDENCY_ORDERED there will be only one stage, thus no need to add edges to
    // the graph
    if (commandExecutionType == CommandExecutionType.STAGE) {
      if (null != roleDependencies) {
        //Add edges
        for (String roleI : graph.keySet()) {
          for (String roleJ : graph.keySet()) {
            if (!roleI.equals(roleJ)) {
              RoleGraphNode rgnI = graph.get(roleI);
              RoleGraphNode rgnJ = graph.get(roleJ);
              int order = roleDependencies.order(rgnI, rgnJ);
              if (order == -1) {
                rgnI.addEdge(rgnJ);
              } else if (order == 1) {
                rgnJ.addEdge(rgnI);
              }
            }
          }
        }
      }
    }
  }

  /**
   * This method return more detailed RoleCommand type. For now, i've added code
   * only for RESTART name of CUSTOM COMMAND, but in future i think all other will be added too.
   * This method was implemented for fix in role_command_order.json, for RESTART commands.
   */
  private RoleCommand getRoleCommand(HostRoleCommand hostRoleCommand) {
    if (hostRoleCommand.getRoleCommand().equals(RoleCommand.CUSTOM_COMMAND)) {
      return hostRoleCommand.getCustomCommandName().equals("RESTART") ? RoleCommand.RESTART : RoleCommand.CUSTOM_COMMAND;
    }
    return hostRoleCommand.getRoleCommand();
  }

  /**
   * Returns a list of stages that need to be executed one after another
   * to execute the DAG generated in the last {@link #build(Stage)} call.
   */
  public List<Stage> getStages() throws AmbariException {
    long initialStageId = initialStage.getStageId();
    List<Stage> stageList = new ArrayList<>();
    List<RoleGraphNode> firstStageNodes = new ArrayList<>();
    if(!graph.isEmpty()){
      LOG.info("Detecting cycle graphs");
      LOG.info(stringifyGraph());
      breakCycleGraph();
    }
    while (!graph.isEmpty()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(stringifyGraph());
      }

      for (String role: graph.keySet()) {
        RoleGraphNode rgn = graph.get(role);
        if (rgn.getInDegree() == 0) {
          firstStageNodes.add(rgn);
        }
      }
      Stage aStage = getStageFromGraphNodes(initialStage, firstStageNodes);
      aStage.setStageId(++initialStageId);
      stageList.add(aStage);
      //Remove first stage nodes from the graph, we know that none of
      //these nodes have an incoming edges.
      for (RoleGraphNode rgn : firstStageNodes) {
        if (sameHostOptimization) {
          //Perform optimization
        }
        removeZeroInDegreeNode(rgn.getRole().toString());
      }
      firstStageNodes.clear();
    }
    return stageList;
  }

  /**
   * Gets a representation of the role ordering of the specified commands
   * without constructing {@link Stage} instances. The commands to order are
   * supplied as mapping of host to role/command. Each item of the returned list
   * represents a single stage. The map is of host to commands. For example:
   * <br/>
   * <br/>
   * Input:
   * <pre>
   * {c6401={NAMENODE=STOP}, c6402={DATANODE=STOP}, NODEMANAGER=STOP}}
   * </pre>
   *
   * Output:
   * <pre>
   * [{c6402=[NODEMANAGER/STOP, DATANODE-STOP]}, c6401=[NAMENODE/STOP]]
   *
   * <pre>
   *
   * @param unorderedCommands
   *          a mapping of {@link Role} to {@link HostRoleCommand} by host.
   * @return and ordered list where each item represents a single stage and each
   *         stage's commands are mapped by host.
   */
  public List<Map<String, List<HostRoleCommand>>> getOrderedHostRoleCommands(
      Map<String, Map<String, HostRoleCommand>> unorderedCommands) {
    build(unorderedCommands);

    // represents an ordered list of stages
    List<Map<String, List<HostRoleCommand>>> orderedCommands = new ArrayList<>();

    List<RoleGraphNode> firstStageNodes = new ArrayList<>();
    while (!graph.isEmpty()) {
      for (String role : graph.keySet()) {
        RoleGraphNode rgn = graph.get(role);
        if (rgn.getInDegree() == 0) {
          firstStageNodes.add(rgn);
        }
      }

      // represents a stage
      Map<String, List<HostRoleCommand>> commandsPerHost = new HashMap<>();

      for (RoleGraphNode rgn : firstStageNodes) {
        // for every host for this stage, create the ordered commands
        for (String host : rgn.getHosts()) {
          List<HostRoleCommand> commands = commandsPerHost.get(host);
          if (null == commands) {
            commands = new ArrayList<>();
            commandsPerHost.put(host, commands);
          }

          HostRoleCommand hrc = hrcFactory.create(host, rgn.getRole(), null, rgn.getCommand());
          commands.add(hrc);
        }
      }

      // add the stage to the list of stages
      orderedCommands.add(commandsPerHost);

      // Remove first stage nodes from the graph, we know that none of
      // these nodes have an incoming edges.
      for (RoleGraphNode rgn : firstStageNodes) {
        removeZeroInDegreeNode(rgn.getRole().toString());
      }

      firstStageNodes.clear();
    }

    return orderedCommands;
  }

  /**
   * Assumes there are no incoming edges.
   */
  private synchronized void removeZeroInDegreeNode(String role) {
    RoleGraphNode nodeToRemove = graph.remove(role);
    for (RoleGraphNode edgeNode : nodeToRemove.getEdges()) {
      edgeNode.decrementInDegree();
    }
  }

  private Stage getStageFromGraphNodes(Stage origStage,
      List<RoleGraphNode> stageGraphNodes) {

    Stage newStage = stageFactory.createNew(origStage.getRequestId(),
        origStage.getLogDir(), origStage.getClusterName(),
        origStage.getClusterId(),
        origStage.getRequestContext(),
        origStage.getCommandParamsStage(), origStage.getHostParamsStage());
    newStage.setSuccessFactors(origStage.getSuccessFactors());
    newStage.setSkippable(origStage.isSkippable());
    newStage.setAutoSkipFailureSupported(origStage.isAutoSkipOnFailureSupported());
    if (commandExecutionType != null) {
      newStage.setCommandExecutionType(commandExecutionType);
    }

    for (RoleGraphNode rgn : stageGraphNodes) {
      for (String host : rgn.getHosts()) {
        newStage.addExecutionCommandWrapper(origStage, host, rgn.getRole());
      }
    }
    return newStage;
  }

  public String stringifyGraph() {
    StringBuilder builder = new StringBuilder();
    builder.append("Graph:\n");
    for (String role : graph.keySet()) {
      builder.append(graph.get(role));
      for (RoleGraphNode rgn : graph.get(role).getEdges()) {
        builder.append(" --> ");
        builder.append(rgn);
      }
      builder.append("\n");
    }
    return builder.toString();
  }

  /**
   * Cycle graphs indicate circular dependencies such as the following example
   * that can cause Ambari enter an infinite loop while building stages.
   *   (DATANODE, START, 2) --> (NAMENODE, START, 2) --> (SECONDARY_NAMENODE, START, 3)
   *   (HDFS_CLIENT, INSTALL, 0) --> (DATANODE, START, 2) --> (NAMENODE, START, 2) --> (SECONDARY_NAMENODE, START, 3)
   *   (NAMENODE, START, 2) --> (DATANODE, START, 2) --> (SECONDARY_NAMENODE, START, 3)
   *   (SECONDARY_NAMENODE, START, 3)
   * It is important to safe guard against cycle graphs,
   * when Ambari supports mpacks, custom services and service level role command order.
   * */
  public void breakCycleGraph() throws AmbariException{
    List<String> edges = new ArrayList<>();
    for (String role : graph.keySet()){
      RoleGraphNode fromNode = graph.get(role);
      String fnRole = fromNode.getRole().name();
      String fnCommand = fromNode.getCommand().name();

      Iterator<RoleGraphNode> it = fromNode.getEdges().iterator();
      while(it.hasNext()){
        RoleGraphNode toNode = it.next();
        String tnRole = toNode.getRole().name();
        String tnCommand = toNode.getCommand().name();
        //Check if the reversed edge exists in the list already
        //If the edit exists, print an error message and break the edge
        String format = "%s:%s --> %s:%s";
        String edge = String.format(format, fnRole, fnCommand, tnRole, tnCommand);
        String reversedEdge = String.format(format, tnRole, tnCommand, fnRole, fnCommand);
        if (edges.contains(reversedEdge)){
          String msg = String.format(
              "Circular dependencies detected between %s and %s for %s. "
              + "%s already exists in the role command order.", fnRole, tnRole, edge, reversedEdge);
          LOG.error(msg);
          throw new AmbariException(msg);
        } else {
          edges.add(edge);
        }
      }
    }
  }
}
