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
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.audit.event.kerberos.CreateKeyTabKerberosAuditEvent;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;
import org.apache.ambari.server.serveraction.ActionLog;
import org.apache.ambari.server.serveraction.kerberos.stageutils.KerberosKeytabController;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * CreateKeytabFilesServerAction is a ServerAction implementation that creates keytab files as
 * instructed.
 * <p/>
 * This class mainly relies on the KerberosServerAction to iterate through metadata identifying
 * the Kerberos keytab files that need to be created. For each identity in the metadata, this
 * implementation's
 * {@link KerberosServerAction#processIdentity(ResolvedKerberosPrincipal, KerberosOperationHandler, Map, boolean, Map)}
 * is invoked attempting the creation of the relevant keytab file.
 */
public class CreateKeytabFilesServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(CreateKeytabFilesServerAction.class);

  /**
   * KerberosPrincipalDAO used to set and get Kerberos principal details
   */
  @Inject
  private KerberosPrincipalDAO kerberosPrincipalDAO;

  /**
   * Configuration used to get the configured properties such as the keytab file cache directory
   */
  @Inject
  private Configuration configuration;

  /**
   * HostDAO used to retrieveHost Entity object
   */
  @Inject
  private HostDAO hostDAO;

  @Inject
  private KerberosKeytabController kerberosKeytabController;

  /**
   * A map of data used to track what has been processed in order to optimize the creation of keytabs
   * such as knowing when to create a cached keytab file or use a cached keytab file.
   */
  Map<String, Set<String>> visitedIdentities = new HashMap<>();

  /**
   * Called to execute this action.  Upon invocation, calls
   * {@link org.apache.ambari.server.serveraction.kerberos.KerberosServerAction#processIdentities(java.util.Map)} )}
   * to iterate through the Kerberos identity metadata and call
   * {@link org.apache.ambari.server.serveraction.kerberos.CreateKeytabFilesServerAction#processIdentities(java.util.Map)}
   * for each identity to process.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport indicating the result of this action
   * @throws AmbariException
   * @throws InterruptedException
   */
  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws
      AmbariException, InterruptedException {
    return processIdentities(requestSharedDataContext);
  }


  /**
   * For each identity, create a keytab and append to a new or existing keytab file.
   * <p/>
   * It is expected that the {@link org.apache.ambari.server.serveraction.kerberos.CreatePrincipalsServerAction}
   * (or similar) has executed before this action and a set of passwords has been created, map to
   * their relevant (evaluated) principals and stored in the requestSharedDataContext.
   * <p/>
   * If a password exists for the current evaluatedPrincipal, use a
   * {@link org.apache.ambari.server.serveraction.kerberos.KerberosOperationHandler} to generate
   * the keytab file. To help avoid filename collisions and to build a structure that is easy to
   * discover, each keytab file is stored in host-specific directory using the SHA1 hash of its destination file path.
   * <p/>
   * <pre>
   *   data_directory
   *   |- host1
   *   |  |- 16a054404c8826cd604a27ac970e8cc4b9c7a3fa   (keytab file)
   *   |  |- ...                                        (keytab files)
   *   |  |- a3c09cae73406912e8c55296d1c85b674d24f576   (keytab file)
   *   |- host2
   *   |  |- ...
   * </pre>
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
   * @throws AmbariException if an error occurs while processing the identity record
   */
  @Override
  protected CommandReport processIdentity(ResolvedKerberosPrincipal resolvedPrincipal,
                                          KerberosOperationHandler operationHandler,
                                          Map<String, String> kerberosConfiguration,
                                          boolean includedInFilter,
                                          Map<String, Object> requestSharedDataContext)
      throws AmbariException {


    CreateKeyTabKerberosAuditEvent.CreateKeyTabKerberosAuditEventBuilder auditEventBuilder = CreateKeyTabKerberosAuditEvent.builder();
    auditEventBuilder.withTimestamp(System.currentTimeMillis());
    // in case this is called directly from TopologyManager there's no HostRoleCommand
    auditEventBuilder.withRequestId(getHostRoleCommand() != null ? getHostRoleCommand().getRequestId() : -1);
    auditEventBuilder.withTaskId(getHostRoleCommand() != null ? getHostRoleCommand().getTaskId() : -1);

    CommandReport commandReport = null;
    String message = null;

    Set<ResolvedKerberosKeytab> keytabsToCreate = kerberosKeytabController.getFromPrincipal(resolvedPrincipal);

    try {
      String dataDirectory = getDataDirectoryPath();

      if (operationHandler == null) {
        message = String.format("Failed to create keytab file for %s, missing KerberosOperationHandler", resolvedPrincipal.getPrincipal());
        actionLog.writeStdErr(message);
        LOG.error(message);
        commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
      } else if (dataDirectory == null) {
        message = "The data directory has not been set. Generated keytab files can not be stored.";
        LOG.error(message);
        commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
      } else {
        Map<String, String> principalPasswordMap = getPrincipalPasswordMap(requestSharedDataContext);
        Map<String, Integer> principalKeyNumberMap = getPrincipalKeyNumberMap(requestSharedDataContext);
        for (ResolvedKerberosKeytab rkk : keytabsToCreate) {
          String hostName = resolvedPrincipal.getHostName();
          String keytabFilePath = rkk.getFile();

          if ((hostName != null) && !hostName.isEmpty() && (keytabFilePath != null) && !keytabFilePath.isEmpty()) {
            Set<String> visitedPrincipalKeys = visitedIdentities.get(resolvedPrincipal.getPrincipal());
            String visitationKey = String.format("%s|%s", hostName, keytabFilePath);

            if ((visitedPrincipalKeys == null) || !visitedPrincipalKeys.contains(visitationKey)) {
              // Look up the current evaluatedPrincipal's password.
              // If found create the keytab file, else try to find it in the cache.
              String password = principalPasswordMap.get(resolvedPrincipal.getPrincipal());
              Integer keyNumber = principalKeyNumberMap.get(resolvedPrincipal.getPrincipal());

              message = String.format("Creating keytab file for %s on host %s", resolvedPrincipal.getPrincipal(), hostName);
              LOG.info(message);
              actionLog.writeStdOut(message);
              auditEventBuilder.withPrincipal(resolvedPrincipal.getPrincipal()).withHostName(hostName).withKeyTabFilePath(keytabFilePath);

              // Determine where to store the keytab file.  It should go into a host-specific
              // directory under the previously determined data directory.
              File hostDirectory = new File(dataDirectory, hostName);

              // Ensure the host directory exists...
              if (!hostDirectory.exists() && hostDirectory.mkdirs()) {
                // Make sure only Ambari has access to this directory.
                ensureAmbariOnlyAccess(hostDirectory);
              }

              if (hostDirectory.exists()) {
                File destinationKeytabFile = new File(hostDirectory, DigestUtils.sha256Hex(keytabFilePath));

                boolean regenerateKeytabs = getOperationType(getCommandParameters()) == OperationType.RECREATE_ALL;

                if(!includedInFilter) {
                  // If this principal is to be filtered out, skip... unless is has not yet been created...
                  regenerateKeytabs = false;
                }

                KerberosPrincipalEntity principalEntity = kerberosPrincipalDAO.find(resolvedPrincipal.getPrincipal());
                String cachedKeytabPath = (principalEntity == null) ? null : principalEntity.getCachedKeytabPath();

                if (password == null) {
                  if (!regenerateKeytabs && hostName.equalsIgnoreCase(KerberosHelper.AMBARI_SERVER_HOST_NAME)) {
                    // There is nothing to do for this since it must already exist and we don't want to
                    // regenerate the keytab
                    message = String.format("Skipping keytab file for %s, missing password indicates nothing to do", resolvedPrincipal.getPrincipal());
                    LOG.info(message);
                  } else {
                    if (cachedKeytabPath == null) {
                      message = String.format("Failed to create keytab for %s, missing cached file", resolvedPrincipal.getPrincipal());
                      actionLog.writeStdErr(message);
                      LOG.error(message);
                      commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
                    } else {
                      try {
                        operationHandler.createKeytabFile(new File(cachedKeytabPath), destinationKeytabFile);
                      } catch (KerberosOperationException e) {
                        message = String.format("Failed to create keytab file for %s - %s", resolvedPrincipal.getPrincipal(), e.getMessage());
                        actionLog.writeStdErr(message);
                        LOG.error(message, e);
                        commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
                      }
                    }
                  }
                } else {
                  Keytab keytab = createKeytab(resolvedPrincipal.getPrincipal(), password, keyNumber, operationHandler, visitedPrincipalKeys != null, true, actionLog);

                  if (keytab != null) {
                    try {
                      if (operationHandler.createKeytabFile(keytab, destinationKeytabFile)) {
                        ensureAmbariOnlyAccess(destinationKeytabFile);

                        message = String.format("Successfully created keytab file for %s at %s", resolvedPrincipal.getPrincipal(), destinationKeytabFile.getAbsolutePath());
                        LOG.info(message);
                        auditEventBuilder.withPrincipal(resolvedPrincipal.getPrincipal()).withHostName(hostName).withKeyTabFilePath(destinationKeytabFile.getAbsolutePath());
                      } else {
                        message = String.format("Failed to create keytab file for %s at %s", resolvedPrincipal.getPrincipal(), destinationKeytabFile.getAbsolutePath());
                        actionLog.writeStdErr(message);
                        LOG.error(message);
                        commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
                      }
                    } catch (KerberosOperationException e) {
                      message = String.format("Failed to create keytab file for %s - %s", resolvedPrincipal.getPrincipal(), e.getMessage());
                      actionLog.writeStdErr(message);
                      LOG.error(message, e);
                      commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
                    }
                  } else {
                    commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
                  }

                  if (visitedPrincipalKeys == null) {
                    visitedPrincipalKeys = new HashSet<>();
                    visitedIdentities.put(resolvedPrincipal.getPrincipal(), visitedPrincipalKeys);
                  }

                  visitedPrincipalKeys.add(visitationKey);
                }
              } else {
                message = String.format("Failed to create keytab file for %s, the container directory does not exist: %s",
                    resolvedPrincipal.getPrincipal(), hostDirectory.getAbsolutePath());
                actionLog.writeStdErr(message);
                LOG.error(message);
                commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
              }
            } else {
              LOG.debug("Skipping previously processed keytab for {} on host {}", resolvedPrincipal.getPrincipal(), hostName);
            }
          }
        }
      }
    } finally {
      if (commandReport != null && HostRoleStatus.FAILED.toString().equals(commandReport.getStatus())) {
        auditEventBuilder.withReasonOfFailure(message == null ? "Unknown error" : message);
      }
      if (commandReport != null || auditEventBuilder.hasPrincipal()) {
        auditLog(auditEventBuilder.build());
      }
    }
    return commandReport;
  }

  /**
   * Creates the keytab or gets one from the cache for a principal.
   *
   * @param principal        the principal name for the Keytab to create
   * @param password         the password for the Keytab to create
   * @param keyNumber        the key number for the Keytab to create
   * @param operationHandler the KerberosOperationHandler for the relevant KDC
   * @param checkCache       true to check the cache for an existing Keytab; otherwise false
   * @param canCache         true to cache the resulting keytab (if generated); otherwise false
   * @param actionLog        the logger (may be null if no logging is desired)
   * @return a Keytab
   * @throws AmbariException
   */
  public Keytab createKeytab(String principal, String password, Integer keyNumber,
                             KerberosOperationHandler operationHandler, boolean checkCache,
                             boolean canCache, ActionLog actionLog) throws AmbariException {
    LOG.debug("Creating keytab for {} with kvno {}", principal, keyNumber);
    Keytab keytab = null;

    // Possibly get the keytab from the cache
    if (checkCache) {
      // Attempt to pull the keytab from the cache...
      KerberosPrincipalEntity principalEntity = kerberosPrincipalDAO.find(principal);
      String cachedKeytabPath = (principalEntity == null) ? null : principalEntity.getCachedKeytabPath();

      if (cachedKeytabPath != null) {
        try {
          keytab = Keytab.read(new File(cachedKeytabPath));
        } catch (IOException e) {
          String message = String.format("Failed to read the cached keytab for %s, recreating if possible - %s",
              principal, e.getMessage());

          if (LOG.isDebugEnabled()) {
            LOG.warn(message, e);
          } else {
            LOG.warn(message);
          }
        }
      }
    }

    // If the keytab was not retrieved from the cache... create it.
    if (keytab == null) {
      try {
        keytab = operationHandler.createKeytab(principal, password, keyNumber);

        // If the current identity does not represent a service, copy it to a secure location
        // and store that location so it can be reused rather than recreate it.
        KerberosPrincipalEntity principalEntity = kerberosPrincipalDAO.find(principal);
        if (principalEntity != null) {
          if (canCache) {
            File cachedKeytabFile = cacheKeytab(principal, keytab);
            String previousCachedFilePath = principalEntity.getCachedKeytabPath();
            String cachedKeytabFilePath = (!cachedKeytabFile.exists())
                ? null
                : cachedKeytabFile.getAbsolutePath();

            principalEntity.setCachedKeytabPath(cachedKeytabFilePath);
            kerberosPrincipalDAO.merge(principalEntity);

            if (previousCachedFilePath != null) {
              if (!new File(previousCachedFilePath).delete()) {
                LOG.debug("Failed to remove orphaned cache file {}", previousCachedFilePath);
              }
            }
          }
        }
      } catch (KerberosOperationException e) {
        String message = String.format("Failed to create keytab file for %s - %s", principal, e.getMessage());
        if (actionLog != null) {
          actionLog.writeStdErr(message);
        }
        LOG.error(message, e);
      }
    }

    return keytab;
  }


  /**
   * Cache a keytab given its relative principal name and the keytab data.
   * <p/>
   * The specified keytab is stored in a file in a location derived using the configured keytab
   * cache directory and the seeded hash of the principal name - this is to add a slight level
   * of obscurity so that it cannot be determined what keytab data is in the file based on its name.
   * The file is the set readable by only the Ambari server process owner.
   *
   * @param principal the principal name related to the keytab data
   * @param keytab    the keytab data to cache
   * @return a File pointing to the cached keytab file
   * @throws AmbariException if a failure occurs while creating the cache file containing the the keytab data
   */
  private File cacheKeytab(String principal, Keytab keytab) throws AmbariException {
    File cacheDirectory = configuration.getKerberosKeytabCacheDir();

    if (cacheDirectory == null) {
      String message = "The Kerberos keytab cache directory is not configured in the Ambari properties";
      LOG.error(message);
      throw new AmbariException(message);
    }

    if (!cacheDirectory.exists()) {
      // If the cache directory does not exist, create it and ensure only Ambari has access to it
      if (cacheDirectory.mkdirs()) {
        ensureAmbariOnlyAccess(cacheDirectory);

        if (!cacheDirectory.exists()) {
          String message = String.format("Failed to create the keytab cache directory %s",
              cacheDirectory.getAbsolutePath());
          LOG.error(message);
          throw new AmbariException(message);
        }
      }
    }

    File cachedKeytabFile = new File(cacheDirectory, DigestUtils.sha256Hex(principal + String.valueOf(System.currentTimeMillis())));

    try {
      keytab.write(cachedKeytabFile);
    } catch (IOException e) {
      String message = String.format("Failed to write the keytab for %s to the cache location (%s)",
          principal, cachedKeytabFile.getAbsolutePath());
      LOG.error(message, e);
      throw new AmbariException(message, e);
    }

    ensureAmbariOnlyAccess(cachedKeytabFile);

    return cachedKeytabFile;
  }

  /**
   * Ensures that the owner of the Ambari server process is the only local user account able to
   * read and write to the specified file or read, write to, and execute the specified directory.
   *
   * @param file the file or directory for which to modify access
   */
  protected void ensureAmbariOnlyAccess(File file) throws AmbariException {
    if (file.exists()) {
      if (!file.setReadable(false, false) || !file.setReadable(true, true)) {
        String message = String.format("Failed to set %s readable only by Ambari", file.getAbsolutePath());
        LOG.warn(message);
        throw new AmbariException(message);
      }

      if (!file.setWritable(false, false) || !file.setWritable(true, true)) {
        String message = String.format("Failed to set %s writable only by Ambari", file.getAbsolutePath());
        LOG.warn(message);
        throw new AmbariException(message);
      }

      if (file.isDirectory()) {
        if (!file.setExecutable(false, false) || !file.setExecutable(true, true)) {
          String message = String.format("Failed to set %s executable by Ambari", file.getAbsolutePath());
          LOG.warn(message);
          throw new AmbariException(message);
        }
      } else {
        if (!file.setExecutable(false, false)) {
          String message = String.format("Failed to set %s not executable", file.getAbsolutePath());
          LOG.warn(message);
          throw new AmbariException(message);
        }
      }
    }
  }
}
