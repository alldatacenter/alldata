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

package org.apache.ambari.server.serveraction.kerberos;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.audit.event.kerberos.DestroyPrincipalKerberosAuditEvent;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.orm.dao.KerberosKeytabDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.KerberosKeytabEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabServiceMappingEntity;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * DestroyPrincipalsServerAction is a ServerAction implementation that destroys principals as instructed.
 * <p/>
 * This class mainly relies on the KerberosServerAction to iterate through metadata identifying
 * the Kerberos principals that need to be removed from the relevant KDC. For each identity in the
 * metadata, this implementation's
 * {@link KerberosServerAction#processIdentity(ResolvedKerberosPrincipal, KerberosOperationHandler, Map, boolean, Map)}
 * is invoked attempting the removal of the relevant principal.
 */
public class DestroyPrincipalsServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(DestroyPrincipalsServerAction.class);

  /**
   * The KerberosOperationHandlerFactory to use to obtain KerberosOperationHandler instances
   * <p/>
   * This is needed to help with test cases to mock a KerberosOperationHandler
   */
  @Inject
  private KerberosOperationHandlerFactory kerberosOperationHandlerFactory;

  @Inject
  private KerberosHelper kerberosHelper;

  @Inject
  private KerberosPrincipalDAO kerberosPrincipalDAO;

  @Inject
  private KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO;

  @Inject
  private KerberosKeytabDAO kerberosKeytabDAO;

  /**
   * A set of visited principal names used to prevent unnecessary processing on already processed
   * principal names
   */
  private Set<String> seenPrincipals = new HashSet<>();

  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link KerberosServerAction#processIdentities(java.util.Map)}
   * to iterate through the Kerberos identity metadata and call
   * {@link org.apache.ambari.server.serveraction.kerberos.DestroyPrincipalsServerAction#processIdentities(java.util.Map)}
   * for each identity to process.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport indicating the result of this action
   * @throws org.apache.ambari.server.AmbariException
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws
      AmbariException, InterruptedException {

    Map<String, String> commandParameters = getCommandParameters();
    KDCType kdcType = getKDCType(commandParameters);
    PrincipalKeyCredential administratorCredential = kerberosHelper.getKDCAdministratorCredentials(getClusterName());
    String defaultRealm = getDefaultRealm(commandParameters);

    KerberosOperationHandler operationHandler = kerberosOperationHandlerFactory.getKerberosOperationHandler(kdcType);
    Map<String, String> kerberosConfiguration = getConfigurationProperties("kerberos-env");

    try {
      operationHandler.open(administratorCredential, defaultRealm, kerberosConfiguration);
    } catch (KerberosOperationException e) {
      String message = String.format("Failed to process the identities, could not properly open the KDC operation handler: %s",
          e.getMessage());
      actionLog.writeStdErr(message);
      LOG.error(message);
      throw new AmbariException(message, e);
    }

    actionLog.writeStdOut("Cleaning up Kerberos identities.");

    Map<String, ? extends Collection<String>> serviceComponentFilter = getServiceComponentFilter();
    Set<String> hostFilter = getHostFilter();
    Collection<String> principalNameFilter = getIdentityFilter();

    List<KerberosKeytabPrincipalEntity> kerberosKeytabPrincipalEntities;

    if (MapUtils.isEmpty(serviceComponentFilter) && CollectionUtils.isEmpty(hostFilter) && CollectionUtils.isEmpty(principalNameFilter)) {
      // Clean up all... this is probably a disable Kerberos operation
      kerberosKeytabPrincipalEntities = kerberosKeytabPrincipalDAO.findAll();
    } else {
      // Build the search filters
      ArrayList<KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter> filters = new ArrayList<>();

      if (MapUtils.isEmpty(serviceComponentFilter)) {
        filters.add(KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter.createFilter(
            null,
            null,
            hostFilter,
            principalNameFilter));
      } else {
        for (Map.Entry<String, ? extends Collection<String>> entry : serviceComponentFilter.entrySet()) {
          filters.add(KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter.createFilter(
              entry.getKey(),
              entry.getValue(),
              hostFilter,
              principalNameFilter));
        }
      }

      // Get only the entries we care about...
      kerberosKeytabPrincipalEntities = kerberosKeytabPrincipalDAO.findByFilters(filters);
    }

    if (kerberosKeytabPrincipalEntities != null) {
      try {
        Set<Long> visitedKKPID = new HashSet<>();

        for (KerberosKeytabPrincipalEntity kerberosKeytabPrincipalEntity : kerberosKeytabPrincipalEntities) {
          // Do not re-process duplicate entries
          if (!visitedKKPID.contains(kerberosKeytabPrincipalEntity.getKkpId())) {

            visitedKKPID.add(kerberosKeytabPrincipalEntity.getKkpId());

            KerberosKeytabEntity kerberosKeytabEntity = kerberosKeytabPrincipalEntity.getKerberosKeytabEntity();
            KerberosPrincipalEntity kerberosPrincipalEntity = kerberosKeytabPrincipalEntity.getKerberosPrincipalEntity();

            if (serviceComponentFilter == null) {
              // All service and components "match" in this case... thus all mapping records are to be
              // removed.  The KerberosKeytabServiceMappingEntity has already been selected to be removed
              // based on the host and identity filters.
              kerberosKeytabPrincipalEntity.setServiceMapping(null);
            } else {
              // It is possible that this KerberosKeytabPrincipalEntity needs to stick around since other
              // services and components rely on it.  So remove only the relevant service mapping records
              List<KerberosKeytabServiceMappingEntity> serviceMapping = kerberosKeytabPrincipalEntity.getServiceMapping();

              if (CollectionUtils.isNotEmpty(serviceMapping)) {
                // Prune off the relevant service mappings...
                Iterator<KerberosKeytabServiceMappingEntity> iterator = serviceMapping.iterator();
                while (iterator.hasNext()) {
                  KerberosKeytabServiceMappingEntity entity = iterator.next();

                  if (serviceComponentFilter.containsKey(entity.getServiceName())) {
                    Collection<String> components = serviceComponentFilter.get(entity.getServiceName());

                    if ((CollectionUtils.isEmpty(components)) || components.contains(entity.getComponentName())) {
                      iterator.remove();
                    }
                  }
                }

                kerberosKeytabPrincipalEntity.setServiceMapping(serviceMapping);
              }
            }

            // Apply changes indicated above...
            kerberosKeytabPrincipalEntity = kerberosKeytabPrincipalDAO.merge(kerberosKeytabPrincipalEntity);

            // If there are no services or components relying on this KerberosKeytabPrincipalEntity, it
            // should be removed...
            if (CollectionUtils.isEmpty(kerberosKeytabPrincipalEntity.getServiceMapping())) {
              kerberosKeytabPrincipalDAO.remove(kerberosKeytabPrincipalEntity);

              if (LOG.isDebugEnabled()) {
                LOG.debug("Cleaning up keytab/principal entry: {}:{}:{}:{}",
                    kerberosKeytabPrincipalEntity.getKkpId(), kerberosKeytabEntity.getKeytabPath(), kerberosPrincipalEntity.getPrincipalName(), kerberosKeytabPrincipalEntity.getHostName());
              } else {
                LOG.info("Cleaning up keytab/principal entry: {}:{}:{}",
                    kerberosKeytabEntity.getKeytabPath(), kerberosPrincipalEntity.getPrincipalName(), kerberosKeytabPrincipalEntity.getHostName());
              }

              // Remove the KerberosKeytabPrincipalEntity reference from the relevant KerberosKeytabEntity
              kerberosKeytabEntity.getKerberosKeytabPrincipalEntities().remove(kerberosKeytabPrincipalEntity);
              kerberosKeytabEntity = kerberosKeytabDAO.merge(kerberosKeytabEntity);

              // Remove the KerberosKeytabPrincipalEntity reference from the relevant KerberosPrincipalEntity
              kerberosPrincipalEntity.getKerberosKeytabPrincipalEntities().remove(kerberosKeytabPrincipalEntity);
              kerberosPrincipalEntity = kerberosPrincipalDAO.merge(kerberosPrincipalEntity);
            }

            // If there are no more KerberosKeytabPrincipalEntity items that reference this, the keytab
            // file is no longer needed.
            if (kerberosKeytabDAO.removeIfNotReferenced(kerberosKeytabEntity)) {
              String message = String.format("Cleaning up keytab entry: %s", kerberosKeytabEntity.getKeytabPath());
              LOG.info(message);
              actionLog.writeStdOut(message);
            }

            // If there are no more KerberosKeytabPrincipalEntity items that reference this, the principal
            // is no longer needed.
            if (kerberosPrincipalDAO.removeIfNotReferenced(kerberosPrincipalEntity)) {
              String message = String.format("Cleaning up principal entry: %s", kerberosPrincipalEntity.getPrincipalName());
              LOG.info(message);
              actionLog.writeStdOut(message);

              destroyIdentity(operationHandler, kerberosPrincipalEntity);
            }
          }
        }
      } finally {
        // The KerberosOperationHandler needs to be closed, if it fails to close ignore the
        // exception since there is little we can or care to do about it now.
        try {
          operationHandler.close();
        } catch (KerberosOperationException e) {
          // Ignore this...
        }
      }
    }

    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
  }

  @Override
  protected boolean pruneServiceFilter() {
    return false;
  }

  /**
   * For each identity, remove the principal from the configured KDC.
   *
   * @param resolvedPrincipal        a ResolvedKerberosPrincipal object to process
   * @param operationHandler         a KerberosOperationHandler used to perform Kerberos-related
   *                                 tasks for specific Kerberos implementations
   *                                 (MIT, Active Directory, etc...)
   * @param kerberosConfiguration    a Map of configuration properties from kerberos-env
   * @param includedInFilter         a Boolean value indicating whather the principal is included in
   *                                 the current filter or not
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request  @return a CommandReport, indicating an error
   *                                 condition; or null, indicating a success condition
   * @throws org.apache.ambari.server.AmbariException if an error occurs while processing the identity record
   */
  @Override
  protected CommandReport processIdentity(ResolvedKerberosPrincipal resolvedPrincipal,
                                          KerberosOperationHandler operationHandler,
                                          Map<String, String> kerberosConfiguration,
                                          boolean includedInFilter,
                                          Map<String, Object> requestSharedDataContext)
      throws AmbariException {
    throw new UnsupportedOperationException();
  }

  private void destroyIdentity(KerberosOperationHandler operationHandler, KerberosPrincipalEntity kerberosPrincipalEntity) {
    String principalName = kerberosPrincipalEntity.getPrincipalName();
    String message = String.format("Destroying identity, %s", principalName);
    LOG.info(message);
    actionLog.writeStdOut(message);
    DestroyPrincipalKerberosAuditEvent.DestroyPrincipalKerberosAuditEventBuilder auditEventBuilder = DestroyPrincipalKerberosAuditEvent.builder()
        .withTimestamp(System.currentTimeMillis())
        .withRequestId(getHostRoleCommand().getRequestId())
        .withTaskId(getHostRoleCommand().getTaskId())
        .withPrincipal(principalName);

    try {
      try {
        operationHandler.removePrincipal(principalName, kerberosPrincipalEntity.isService());
      } catch (KerberosOperationException e) {
        message = String.format("Failed to remove identity for %s from the KDC - %s", principalName, e.getMessage());
        LOG.warn(message, e);
        actionLog.writeStdErr(message);
        auditEventBuilder.withReasonOfFailure(message);
      }

      try {
        KerberosPrincipalEntity principalEntity = kerberosPrincipalDAO.find(principalName);

        if (principalEntity != null) {
          String cachedKeytabPath = principalEntity.getCachedKeytabPath();

          // If a cached  keytabs file exists for this principal, delete it.
          if (cachedKeytabPath != null) {
            if (!new File(cachedKeytabPath).delete()) {
              LOG.debug("Failed to remove cached keytab for {}", principalName);
            }
          }
        }
      } catch (Throwable t) {
        message = String.format("Failed to remove identity for %s from the Ambari database - %s", principalName, t.getMessage());
        LOG.warn(message, t);
        actionLog.writeStdErr(message);
        auditEventBuilder.withReasonOfFailure(message);
      }
    } finally {
      auditLog(auditEventBuilder.build());
    }
  }
}