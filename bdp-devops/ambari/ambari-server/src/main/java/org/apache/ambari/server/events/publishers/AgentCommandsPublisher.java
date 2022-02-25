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

package org.apache.ambari.server.events.publishers;

import static org.apache.ambari.server.controller.KerberosHelperImpl.CHECK_KEYTABS;
import static org.apache.ambari.server.controller.KerberosHelperImpl.REMOVE_KEYTAB;
import static org.apache.ambari.server.controller.KerberosHelperImpl.SET_KEYTAB;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentCommand;
import org.apache.ambari.server.agent.CancelCommand;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.agent.stomp.dto.ExecutionCommandsCluster;
import org.apache.ambari.server.events.ExecutionCommandEvent;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileReader;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.stageutils.KerberosKeytabController;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AgentCommandsPublisher {
  private static final Logger LOG = LoggerFactory.getLogger(AgentCommandsPublisher.class);

  @Inject
  private KerberosKeytabController kerberosKeytabController;

  @Inject
  private Clusters clusters;

  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  @Inject
  private AgentConfigsHolder agentConfigsHolder;

  public void sendAgentCommand(Multimap<Long, AgentCommand> agentCommands) throws AmbariException {
    if (agentCommands != null && !agentCommands.isEmpty()) {
      Map<Long, TreeMap<String, ExecutionCommandsCluster>> executionCommandsClusters = new TreeMap<>();
      for (Map.Entry<Long, AgentCommand> acHostEntry : agentCommands.entries()) {
        Long hostId = acHostEntry.getKey();
        AgentCommand ac = acHostEntry.getValue();
        populateExecutionCommandsClusters(executionCommandsClusters, hostId, ac);
      }
      for (Map.Entry<Long, TreeMap<String, ExecutionCommandsCluster>> hostEntry : executionCommandsClusters.entrySet()) {
        Long hostId = hostEntry.getKey();
        ExecutionCommandEvent executionCommandEvent = new ExecutionCommandEvent(hostId,
            agentConfigsHolder
                .initializeDataIfNeeded(hostId, true).getTimestamp(),
            hostEntry.getValue());
        STOMPUpdatePublisher.publish(executionCommandEvent);
      }
    }
  }

  public void sendAgentCommand(Long hostId, AgentCommand agentCommand) throws AmbariException {
    Multimap<Long, AgentCommand> agentCommands = ArrayListMultimap.create();
    agentCommands.put(hostId, agentCommand);
    sendAgentCommand(agentCommands);
  }

  private void populateExecutionCommandsClusters(Map<Long, TreeMap<String, ExecutionCommandsCluster>> executionCommandsClusters,
                                            Long hostId, AgentCommand ac) throws AmbariException {
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Sending command string = " + StageUtils.jaxbToString(ac));
      }
    } catch (Exception e) {
      throw new AmbariException("Could not get jaxb string for command", e);
    }
    switch (ac.getCommandType()) {
      case BACKGROUND_EXECUTION_COMMAND:
      case EXECUTION_COMMAND: {
        ExecutionCommand ec = (ExecutionCommand) ac;
        LOG.info("AgentCommandsPublisher.sendCommands: sending ExecutionCommand for host {}, role {}, roleCommand {}, and command ID {}, task ID {}",
            ec.getHostname(), ec.getRole(), ec.getRoleCommand(), ec.getCommandId(), ec.getTaskId());
        Map<String, String> hlp = ec.getCommandParams();
        if (hlp != null) {
          String customCommand = hlp.get("custom_command");
          if (SET_KEYTAB.equalsIgnoreCase(customCommand) || REMOVE_KEYTAB.equalsIgnoreCase(customCommand) || CHECK_KEYTABS.equalsIgnoreCase(customCommand)) {
            LOG.info(String.format("%s called", customCommand));
            try {
              injectKeytab(ec, customCommand, clusters.getHostById(hostId).getHostName());
            } catch (IOException e) {
              throw new AmbariException("Could not inject keytab into command", e);
            }
          }
        }
        String clusterName = ec.getClusterName();
        String clusterId = "-1";
        if (clusterName != null) {
          clusterId = Long.toString(clusters.getCluster(clusterName).getClusterId());
        }
        ec.setClusterId(clusterId);
        prepareExecutionCommandsClusters(executionCommandsClusters, hostId, clusterId);
        executionCommandsClusters.get(hostId).get(clusterId).getExecutionCommands().add((ExecutionCommand) ac);
        break;
      }
      case CANCEL_COMMAND: {
        CancelCommand cc = (CancelCommand) ac;
        String clusterId = Long.toString(hostRoleCommandDAO.findByPK(cc.getTargetTaskId()).getStage().getClusterId());
        prepareExecutionCommandsClusters(executionCommandsClusters, hostId, clusterId);
        executionCommandsClusters.get(hostId).get(clusterId).getCancelCommands().add(cc);
        break;
      }
      default:
        LOG.error("There is no action for agent command ="
            + ac.getCommandType().name());
    }
  }

  private void prepareExecutionCommandsClusters(Map<Long, TreeMap<String, ExecutionCommandsCluster>> executionCommandsClusters,
                                                Long hostId, String clusterId) {
    if (!executionCommandsClusters.containsKey(hostId)) {
      executionCommandsClusters.put(hostId, new TreeMap<>());
    }
    if (!executionCommandsClusters.get(hostId).containsKey(clusterId)) {
      executionCommandsClusters.get(hostId).put(clusterId, new ExecutionCommandsCluster(new ArrayList<>(),
          new ArrayList<>()));
    }
  }

  /**
   * Insert Kerberos keytab details into the ExecutionCommand for the SET_KEYTAB custom command if
   * any keytab details and associated data exists for the target host.
   *
   * @param ec the ExecutionCommand to update
   * @param command a name of the relevant keytab command
   * @param targetHost a name of the host the relevant command is destined for
   * @throws AmbariException
   */
  private void injectKeytab(ExecutionCommand ec, String command, String targetHost) throws AmbariException {
    KerberosCommandParameterProcessor processor = KerberosCommandParameterProcessor.getInstance(command, clusters, ec, kerberosKeytabController);
    if (processor != null) {
      ec.setKerberosCommandParams(processor.process(targetHost));
    }
  }

  /**
   * KerberosCommandParameterProcessor is an abstract class providing common implementations for processing
   * the Kerberos command parameters.
   *
   * The Kerberos command parameters are processed differently depending on the operation
   * - set, check, or remove keytab files.
   */
  private static abstract class KerberosCommandParameterProcessor {
    protected final Clusters clusters;

    protected final ExecutionCommand executionCommand;

    protected final KerberosKeytabController kerberosKeytabController;

    protected List<Map<String, String>> kcp;

    protected KerberosCommandParameterProcessor(Clusters clusters, ExecutionCommand executionCommand, KerberosKeytabController kerberosKeytabController) {
      this.clusters = clusters;
      this.executionCommand = executionCommand;
      this.kerberosKeytabController = kerberosKeytabController;
      kcp = executionCommand.getKerberosCommandParams();
    }

    /**
     * Factory method to return the appropriate KerberosCommandParameterProcessor instance based
     * on the command being executed.
     *
     * @param command the command being executed
     * @param clusters the clusters helper class
     * @param executionCommand the execution command structure
     * @param kerberosKeytabController the keytab controller helper class
     * @return
     */
    public static KerberosCommandParameterProcessor getInstance(String command, Clusters clusters, ExecutionCommand executionCommand, KerberosKeytabController kerberosKeytabController) {
      if (SET_KEYTAB.equalsIgnoreCase(command)) {
        return new SetKeytabCommandParameterProcessor(clusters, executionCommand, kerberosKeytabController);
      }
      if (CHECK_KEYTABS.equalsIgnoreCase(command)) {
        return new CheckKeytabsCommandParameterProcessor(clusters, executionCommand, kerberosKeytabController);
      }

      if (REMOVE_KEYTAB.equalsIgnoreCase(command)) {
        return new RemoveKeytabCommandParameterProcessor(clusters, executionCommand, kerberosKeytabController);
      }

      return null;
    }

    /**
     * Performs the default behavior for processing the relevant Kerberos identities and generating the
     * Kerberos-specific command details to send to the agent.
     *
     * @param targetHost the hostname of the target host
     * @return a map of propoperties to set as the Kerberos command parameters
     * @throws AmbariException
     */
    public List<Map<String, String>> process(String targetHost) throws AmbariException {
      KerberosServerAction.KerberosCommandParameters kerberosCommandParameters = new KerberosServerAction.KerberosCommandParameters(executionCommand);

      try {
        Map<String, ? extends Collection<String>> serviceComponentFilter = getServiceComponentFilter(kerberosCommandParameters.getServiceComponentFilter());
        final Collection<KerberosIdentityDescriptor> serviceIdentities = serviceComponentFilter == null ? null : kerberosKeytabController.getServiceIdentities(executionCommand.getClusterName(), serviceComponentFilter.keySet());
        final Set<ResolvedKerberosKeytab> keytabsToInject = kerberosKeytabController.getFilteredKeytabs(serviceIdentities, kerberosCommandParameters.getHostFilter(), kerberosCommandParameters.getIdentityFilter());
        for (ResolvedKerberosKeytab resolvedKeytab : keytabsToInject) {
          for (ResolvedKerberosPrincipal resolvedPrincipal : resolvedKeytab.getPrincipals()) {
            String hostName = resolvedPrincipal.getHostName();

            if (targetHost.equalsIgnoreCase(hostName)) {
              process(targetHost, resolvedKeytab, resolvedPrincipal, serviceComponentFilter);
            }
          }
        }
      } catch (IOException e) {
        throw new AmbariException("Could not inject keytabs to enable kerberos");
      }

      return kcp;
    }

    /**
     * Performs the default behavior for processing the details of a particular Kerberos identity to
     * be added to the Kerberos command parameters.
     *
     * Implementations will override this method to perform specified tasks.
     *
     * @param hostName               the target hostname
     * @param resolvedKeytab         the relevant keytab file details
     * @param resolvedPrincipal      the relevant principal details
     * @param serviceComponentFilter the filter used to determine if the current Kerberos identity
     *                               should be processed
     * @throws IOException
     */
    protected void process(String hostName, ResolvedKerberosKeytab resolvedKeytab, ResolvedKerberosPrincipal resolvedPrincipal, Map<String, ? extends Collection<String>> serviceComponentFilter) throws IOException {
      Map<String, String> keytabMap = new HashMap<>();
      keytabMap.put(KerberosIdentityDataFileReader.HOSTNAME, hostName);
      keytabMap.put(KerberosIdentityDataFileReader.PRINCIPAL, resolvedPrincipal.getPrincipal());
      keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH, resolvedKeytab.getFile());
      kcp.add(keytabMap);
    }

    /**
     * Given a service/component filter, processes it as needed.
     * <p>
     * See overridden methods for more details.
     *
     * @param serviceComponentFilter a map of service to components indicate the services and
     *                               components to include in an operation
     * @return a map of service to components indicate the services and components to include in
     * the operation
     * @throws AmbariException
     */
    protected Map<String, ? extends Collection<String>> getServiceComponentFilter(Map<String, ? extends Collection<String>> serviceComponentFilter) throws AmbariException {
      return serviceComponentFilter;
    }
  }

  /**
   * SetKeytabCommandParameterProcessor is an implementation of {@link KerberosCommandParameterProcessor}
   * that handles the case for setting keytab files.
   * <p>
   * Specifically, this implementation add addition the keytab file details and its contents to the
   * command parameters. It also only performs operations only for services and components that are
   * known to be installed; therefore, the service/component filter may be altered to enforce this.
   */
  private static class SetKeytabCommandParameterProcessor extends KerberosCommandParameterProcessor {

    private final String dataDir;

    private SetKeytabCommandParameterProcessor(Clusters clusters, ExecutionCommand executionCommand, KerberosKeytabController kerberosKeytabController) {
      super(clusters, executionCommand, kerberosKeytabController);
      dataDir = executionCommand.getCommandParams().get(KerberosServerAction.DATA_DIRECTORY);
    }

    @Override
    protected void process(String hostName, ResolvedKerberosKeytab resolvedKeytab, ResolvedKerberosPrincipal resolvedPrincipal, Map<String, ? extends Collection<String>> serviceComponentFilter) throws IOException {
      if (dataDir != null) {
        String principal = resolvedPrincipal.getPrincipal();
        String keytabFilePath = resolvedKeytab.getFile();
        LOG.info("Processing principal {} for host {} and keytab file path {}", principal, hostName, keytabFilePath);

        if (keytabFilePath != null) {
          String sha1Keytab = DigestUtils.sha256Hex(keytabFilePath);
          File keytabFile = Paths.get(dataDir, hostName, sha1Keytab).toFile();

          if (keytabFile.canRead()) {
            Map<String, String> keytabMap = new HashMap<>();

            keytabMap.put(KerberosIdentityDataFileReader.HOSTNAME, hostName);
            keytabMap.put(KerberosIdentityDataFileReader.PRINCIPAL, principal);
            keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH, keytabFilePath);
            keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME, resolvedKeytab.getOwnerName());
            keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS, resolvedKeytab.getOwnerAccess());
            keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_NAME, resolvedKeytab.getGroupName());
            keytabMap.put(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS, resolvedKeytab.getGroupAccess());

            BufferedInputStream bufferedIn = new BufferedInputStream(new FileInputStream(keytabFile));
            byte[] keytabContent;
            try {
              keytabContent = IOUtils.toByteArray(bufferedIn);
            } finally {
              bufferedIn.close();
            }
            String keytabContentBase64 = Base64.encodeBase64String(keytabContent);
            keytabMap.put(KerberosServerAction.KEYTAB_CONTENT_BASE64, keytabContentBase64);

            kcp.add(keytabMap);
          } else {
            LOG.warn("Keytab file for principal {} and host {} can not to be read at path {}",
                principal, hostName, keytabFile.getAbsolutePath());
          }
        }
      }
    }

    @Override
    protected Map<String, ? extends Collection<String>> getServiceComponentFilter(Map<String, ? extends Collection<String>> serviceComponentFilter)
        throws AmbariException {
      return kerberosKeytabController.adjustServiceComponentFilter(clusters.getCluster(executionCommand.getClusterName()), false, serviceComponentFilter);
    }
  }

  /**
   * CheckKeytabsCommandParameterProcessor is an implementation of {@link KerberosCommandParameterProcessor}
   * that handles the case for checking the keytab files on the hosts of the cluster.
   */
  private static class CheckKeytabsCommandParameterProcessor extends KerberosCommandParameterProcessor {

    private CheckKeytabsCommandParameterProcessor(Clusters clusters, ExecutionCommand executionCommand, KerberosKeytabController kerberosKeytabController) {
      super(clusters, executionCommand, kerberosKeytabController);
    }
  }

  /**
   * RemoveKeytabCommandParameterProcessor is an implementation of {@link KerberosCommandParameterProcessor}
   * that handles the case for setting keytab files.
   * <p>
   * Specifically, performs operations any services and components; however only keytab files found
   * to no longer be needed are specified for removal.
   */
  private static class RemoveKeytabCommandParameterProcessor extends KerberosCommandParameterProcessor {

    private RemoveKeytabCommandParameterProcessor(Clusters clusters, ExecutionCommand executionCommand, KerberosKeytabController kerberosKeytabController) {
      super(clusters, executionCommand, kerberosKeytabController);
    }

    @Override
    protected void process(String hostName, ResolvedKerberosKeytab resolvedKeytab, ResolvedKerberosPrincipal resolvedPrincipal, Map<String, ? extends Collection<String>> serviceComponentFilter) throws IOException {
      if (shouldRemove(hostName, resolvedKeytab, resolvedPrincipal, serviceComponentFilter)) {
        super.process(hostName, resolvedKeytab, resolvedPrincipal, serviceComponentFilter);
      }
    }

    /**
     * Determines if the keytab file for a given Kerberos identitiy should be removed from the target
     * host.
     * <p>
     * This is determined by comparing the service/component filter with the metadata about the relavent
     * Kerberos identity. If it is determined that more components than the ones specified in the filer
     * are linked to the identity, than the keytab file will not be flagged for removal.
     *
     * @param hostname               the target hostname
     * @param resolvedKerberosKeytab the relevant keytab file details
     * @param resolvedPrincipal      the relevant principal details
     * @param serviceComponentFilter the filter used to determine if the current Kerberos identity
     *                               should be processed
     * @return <code>true</code>, if this keytab file should be removed; <code>false</code>, otherwise
     */
    private boolean shouldRemove(String hostname,
                                 ResolvedKerberosKeytab resolvedKerberosKeytab,
                                 ResolvedKerberosPrincipal resolvedPrincipal,
                                 Map<String, ? extends Collection<String>> serviceComponentFilter) {
      ResolvedKerberosKeytab existingResolvedKeytab = kerberosKeytabController.getKeytabByFile(resolvedKerberosKeytab.getFile());

      if (existingResolvedKeytab == null) {
        return true;
      }

      Set<ResolvedKerberosPrincipal> principals = existingResolvedKeytab.getPrincipals();
      for (ResolvedKerberosPrincipal principal : principals) {
        if (hostname.equals(principal.getHostName()) && principal.getPrincipal().equals(resolvedPrincipal.getPrincipal())) {
          Multimap<String, String> temp = principal.getServiceMapping();

          // Make a local copy so we do not edit the stored copy, since we do not know how it is stored...
          Map<String, Collection<String>> serviceMapping = (temp == null) ? new HashMap<>() : new HashMap<>(temp.asMap());

          // Prune off the services in the filter, or all if the filter it none.  If there are no
          // service mappings left, this keytab file can be removed...
          if (serviceComponentFilter == null) {
            serviceMapping.clear();
          } else {
            for (Map.Entry<String, ? extends Collection<String>> entry : serviceComponentFilter.entrySet()) {
              String service = entry.getKey();
              Collection<String> components = entry.getValue();

              if (serviceMapping.containsKey(service)) {

                if (CollectionUtils.isEmpty(components) || CollectionUtils.isEmpty(serviceMapping.get(service))) {
                  // Remove all entries for the service...
                  serviceMapping.remove(service);
                } else {
                  Collection<String> leftOver = new HashSet<String>(serviceMapping.get(service));
                  leftOver.removeAll(components);

                  if (CollectionUtils.isEmpty(leftOver)) {
                    serviceMapping.remove(service);
                  } else {
                    serviceMapping.put(service, leftOver);
                  }
                }
              }
            }
          }

          // There are still service mappings for this keytab files, we cannot remove it.
          if (serviceMapping.size() > 0) {
            return false;
          }
        }
      }

      return true;
    }
  }
}
