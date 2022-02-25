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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.audit.event.kerberos.CreatePrincipalKerberosAuditEvent;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;
import org.apache.ambari.server.security.SecurePasswordHelper;
import org.apache.ambari.server.serveraction.ActionLog;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * CreatePrincipalsServerAction is a ServerAction implementation that creates principals as instructed.
 * <p/>
 * This class mainly relies on the KerberosServerAction to iterate through metadata identifying
 * the Kerberos principals that need to be created. For each identity in the metadata, this implementation's
 * {@link KerberosServerAction#processIdentity(ResolvedKerberosPrincipal, KerberosOperationHandler, Map, boolean, Map)}
 * is invoked attempting the creation of the relevant principal.
 */
public class CreatePrincipalsServerAction extends KerberosServerAction {
  private final static Logger LOG = LoggerFactory.getLogger(CreatePrincipalsServerAction.class);

  /**
   * KerberosPrincipalDAO used to set and get Kerberos principal details
   */
  @Inject
  private KerberosPrincipalDAO kerberosPrincipalDAO;

  /**
   * SecurePasswordHelper used to generate secure passwords for newly created principals
   */
  @Inject
  private SecurePasswordHelper securePasswordHelper;

  @Inject
  private KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO;

  /**
   * A set of visited principal names used to prevent unnecessary processing on already processed
   * principal names
   */
  private Set<String> seenPrincipals = new HashSet<>();

  /**
   * Called to execute this action. Upon invocation, calls
   * {@link org.apache.ambari.server.serveraction.kerberos.KerberosServerAction#processIdentities(java.util.Map)}
   * to iterate through the Kerberos identity metadata and call
   * {@link org.apache.ambari.server.serveraction.kerberos.CreatePrincipalsServerAction#processIdentities(java.util.Map)}
   * for each identity to process.
   *
   * @param requestSharedDataContext a Map to be used as shared data among all ServerActions related
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
   * For each identity, generate a unique password, and create a new or update an existing principal in
   * an assumed to be configured KDC.
   * <p/>
   * If a password has not been previously created for the current evaluatedPrincipal, create a "secure"
   * password using {@link SecurePasswordHelper#createSecurePassword()}. Then if the principal
   * does not exist in the KDC, create it using the generated password; else if it does exist update
   * its password.  Finally store the generated password in the shared principal-to-password map and
   * store the new key numbers in the shared principal-to-key_number map so that subsequent process
   * may use the data if necessary.
   *
   * @param resolvedPrincipal        a ResolvedKerberosPrincipal object to process
   * @param operationHandler         a KerberosOperationHandler used to perform Kerberos-related
   *                                 tasks for specific Kerberos implementations
   *                                 (MIT, Active Directory, etc...)
   * @param kerberosConfiguration    a Map of configuration properties from kerberos-env
   * @param includedInFilter         a Boolean value indicating whather the principal is included in
   *                                 the current filter or not
   * @param requestSharedDataContext a Map to be used as shared data among all ServerActions related
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
    CommandReport commandReport = null;

    //  Only process this principal name if we haven't already processed it
    // TODO optimize - split invalidation and principal creation to separate stages
    if (!seenPrincipals.contains(resolvedPrincipal.getPrincipal())) {
      seenPrincipals.add(resolvedPrincipal.getPrincipal());

      boolean processPrincipal;

      KerberosPrincipalEntity kerberosPrincipalEntity = kerberosPrincipalDAO.find(resolvedPrincipal.getPrincipal());

      boolean regenerateKeytabs = getOperationType(getCommandParameters()) == OperationType.RECREATE_ALL;
      boolean servicePrincipal = resolvedPrincipal.isService();

      if(!includedInFilter) {
        // If this principal is to be filtered out, skip... unless is has not yet been created...
        regenerateKeytabs = false;
      }

      if (regenerateKeytabs) {
        // force recreation of principal due to keytab regeneration
        // regenerate only service principals if request filtered by hosts
        processPrincipal = !hasHostFilters() || servicePrincipal;
      } else if (kerberosPrincipalEntity == null) {
        // This principal has not been processed before, process it.
        processPrincipal = true;
      } else if (!StringUtils.isEmpty(kerberosPrincipalEntity.getCachedKeytabPath())) {
        // This principal has been processed and a keytab file has been cached for it... do not process it.
        processPrincipal = false;
      } else {
        // This principal has been processed but a keytab file for it has not been distributed... process it.
        processPrincipal = true;
      }

      if (processPrincipal) {
        Map<String, String> principalPasswordMap = getPrincipalPasswordMap(requestSharedDataContext);

        String password = principalPasswordMap.get(resolvedPrincipal.getPrincipal());

        if (password == null) {
          CreatePrincipalResult result = createPrincipal(resolvedPrincipal.getPrincipal(), servicePrincipal, kerberosConfiguration, operationHandler, regenerateKeytabs, actionLog);
          if (result == null) {
            commandReport = createCommandReport(1, HostRoleStatus.FAILED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
          } else {
            Map<String, Integer> principalKeyNumberMap = getPrincipalKeyNumberMap(requestSharedDataContext);

            principalPasswordMap.put(resolvedPrincipal.getPrincipal(), result.getPassword());
            principalKeyNumberMap.put(resolvedPrincipal.getPrincipal(), result.getKeyNumber());
            // invalidate given principal for all keytabs to make them redistributed again
            for (KerberosKeytabPrincipalEntity kkpe: kerberosKeytabPrincipalDAO.findByPrincipal(resolvedPrincipal.getPrincipal())) {
              kkpe.setDistributed(false);
              kerberosKeytabPrincipalDAO.merge(kkpe);
            }
            // invalidate principal cache
            KerberosPrincipalEntity principalEntity = kerberosPrincipalDAO.find(resolvedPrincipal.getPrincipal());
            try {
              new File(principalEntity.getCachedKeytabPath()).delete();
            } catch (Exception e) {
              LOG.debug("Failed to delete cache file '{}'", principalEntity.getCachedKeytabPath());
            }
            principalEntity.setCachedKeytabPath(null);
            kerberosPrincipalDAO.merge(principalEntity);
          }
        }
      }
    }

    return commandReport;
  }

  /**
   * Creates a principal in the relevant KDC
   *
   * @param principal                the principal name to create
   * @param isServicePrincipal       true if the principal is a service principal; false if the
   *                                 principal is a user principal
   * @param kerberosConfiguration    the kerberos-env configuration properties
   * @param kerberosOperationHandler the KerberosOperationHandler for the relevant KDC
   * @param regenerateKeytabs        true if this was triggered in response to regenerating keytab files; false otherwise
   * @param actionLog                the logger (may be null if no logging is desired)
   * @return a CreatePrincipalResult containing the generated password and key number value
   */
  public CreatePrincipalResult createPrincipal(String principal, boolean isServicePrincipal,
                                               Map<String, String> kerberosConfiguration,
                                               KerberosOperationHandler kerberosOperationHandler,
                                               boolean regenerateKeytabs, ActionLog actionLog) {

    // in case this is called directly from TopologyManager there's no HostRoleCommand
    CreatePrincipalKerberosAuditEvent.CreatePrincipalKerberosAuditEventBuilder auditEventBuilder = CreatePrincipalKerberosAuditEvent.builder()
        .withTimestamp(System.currentTimeMillis())
        .withRequestId(getHostRoleCommand() != null ? getHostRoleCommand().getRequestId() : -1)
        .withTaskId(getHostRoleCommand() != null ? getHostRoleCommand().getTaskId() : -1)
        .withPrincipal(principal);
    CreatePrincipalResult result = null;
    String message = null;

    try {
      message = String.format("Processing principal, %s", principal);
      LOG.info(message);
      if (actionLog != null) {
        actionLog.writeStdOut(message);
      }

      Integer length;
      Integer minLowercaseLetters;
      Integer minUppercaseLetters;
      Integer minDigits;
      Integer minPunctuation;
      Integer minWhitespace;

      if (kerberosConfiguration == null) {
        length = null;
        minLowercaseLetters = null;
        minUppercaseLetters = null;
        minDigits = null;
        minPunctuation = null;
        minWhitespace = null;
      } else {
        length = toInt(kerberosConfiguration.get("password_length"));
        minLowercaseLetters = toInt(kerberosConfiguration.get("password_min_lowercase_letters"));
        minUppercaseLetters = toInt(kerberosConfiguration.get("password_min_uppercase_letters"));
        minDigits = toInt(kerberosConfiguration.get("password_min_digits"));
        minPunctuation = toInt(kerberosConfiguration.get("password_min_punctuation"));
        minWhitespace = toInt(kerberosConfiguration.get("password_min_whitespace"));
      }

      String password = securePasswordHelper.createSecurePassword(length, minLowercaseLetters, minUppercaseLetters, minDigits, minPunctuation, minWhitespace);

      try {
        /*
         * true indicates a new principal was created, false indicates an existing principal was updated
         */
        boolean created;
        Integer keyNumber;

        if (regenerateKeytabs) {
          try {
            keyNumber = kerberosOperationHandler.setPrincipalPassword(principal, password, isServicePrincipal);
            created = false;
          } catch (KerberosPrincipalDoesNotExistException e) {
            message = String.format("Principal, %s, does not exist, creating new principal", principal);
            LOG.warn(message);
            if (actionLog != null) {
              actionLog.writeStdOut(message);
            }

            keyNumber = kerberosOperationHandler.createPrincipal(principal, password, isServicePrincipal);
            created = true;
          }
        } else {
          try {
            keyNumber = kerberosOperationHandler.createPrincipal(principal, password, isServicePrincipal);
            created = true;
          } catch (KerberosPrincipalAlreadyExistsException e) {
            message = String.format("Principal, %s, already exists, setting new password", principal);
            LOG.warn(message);
            if (actionLog != null) {
              actionLog.writeStdOut(message);
            }

            keyNumber = kerberosOperationHandler.setPrincipalPassword(principal, password, isServicePrincipal);
            created = false;
          }
        }

        if (keyNumber != null) {
          result = new CreatePrincipalResult(principal, password, keyNumber);

          if (created) {
            message = String.format("Successfully created new principal, %s", principal);
          } else {
            message = String.format("Successfully set password for %s", principal);
          }
          LOG.debug(message);
        } else {
          if (created) {
            message = String.format("Failed to create principal, %s - unknown reason", principal);
          } else {
            message = String.format("Failed to set password for %s - unknown reason", principal);
          }
          LOG.error(message);
          if (actionLog != null) {
            actionLog.writeStdErr(message);
          }
        }

        if (!kerberosPrincipalDAO.exists(principal)) {
          kerberosPrincipalDAO.create(principal, isServicePrincipal);
        }

      } catch (KerberosOperationException e) {
        message = String.format("Failed to create principal, %s - %s", principal, e.getMessage());
        LOG.error(message, e);
        if (actionLog != null) {
          actionLog.writeStdErr(message);
        }
      }
    } finally {
      if (result == null) {
        auditEventBuilder.withReasonOfFailure(message == null ? "Unknown error" : message);
      }
      auditLog(auditEventBuilder.build());
    }

    return result;
  }

  /**
   * Translates a String containing an integer value to an Integer.
   * <p/>
   * If the string is null, empty or not a number, returns null; otherwise returns an Integer value
   * representing the integer value of the string.
   *
   * @param string the string to parse
   * @return an Integer or null
   */
  private static Integer toInt(String string) {
    if ((string == null) || string.isEmpty()) {
      return null;
    } else {
      try {
        return Integer.parseInt(string);
      } catch (NumberFormatException e) {
        return null;
      }
    }
  }

  /**
   * CreatePrincipalResult holds values created as a result of creating a principal in a KDC.
   */
  public static class CreatePrincipalResult {
    final private String principal;
    final private String password;
    final private Integer keyNumber;

    /**
     * Constructor
     *
     * @param principal a principal name
     * @param password  a password
     * @param keyNumber a key number
     */
    public CreatePrincipalResult(String principal, String password, Integer keyNumber) {
      this.principal = principal;
      this.password = password;
      this.keyNumber = keyNumber;
    }

    /**
     * Gets the principal name
     *
     * @return the principal name
     */
    public String getPrincipal() {
      return principal;
    }

    /**
     * Gets the principal's password
     *
     * @return the principal's password
     */
    public String getPassword() {
      return password;
    }

    /**
     * Gets the password's key number
     *
     * @return the password's key number
     */
    public Integer getKeyNumber() {
      return keyNumber;
    }
  }
}
