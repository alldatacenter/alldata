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

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IPAKerberosOperationHandler is an implementation of a KerberosOperationHandler providing
 * functionality specifically for IPA managed KDC. See http://www.freeipa.org
 * <p/>
 * It is assumed that the IPA admin tools are installed and that the ipa shell command is
 * available
 */
public class IPAKerberosOperationHandler extends KDCKerberosOperationHandler {
  private final static Logger LOG = LoggerFactory.getLogger(IPAKerberosOperationHandler.class);

  /**
   * This is where user principals are members of. Important as the password should not expire
   * and thus a separate password policy should apply to this group
   */
  private String userPrincipalGroup = null;

  /**
   * A String containing the resolved path to the ipa executable
   */
  private String executableIpaGetKeytab = null;

  /**
   * A String containing the resolved path to the ipa executable
   */
  private String executableIpa = null;

  /**
   * Prepares and creates resources to be used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used before this call.
   * <p/>
   * The kerberosConfiguration Map is not being used.
   *
   * @param administratorCredentials a KerberosCredential containing the administrative credentials
   *                                 for the relevant IPA KDC
   * @param realm                    a String declaring the default Kerberos realm (or domain)
   * @param kerberosConfiguration    a Map of key/value pairs containing data from the kerberos-env configuration set
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  @Override
  public void open(PrincipalKeyCredential administratorCredentials, String realm, Map<String, String> kerberosConfiguration)
      throws KerberosOperationException {

    if (kerberosConfiguration != null) {
      userPrincipalGroup = kerberosConfiguration.get(KERBEROS_ENV_USER_PRINCIPAL_GROUP);
    }

    // Pre-determine the paths to relevant Kerberos executables
    executableIpa = getExecutable("ipa");
    executableIpaGetKeytab = getExecutable("ipa-getkeytab");

    super.open(administratorCredentials, realm, kerberosConfiguration);
  }

  @Override
  public void close() throws KerberosOperationException {
    userPrincipalGroup = null;
    executableIpa = null;
    executableIpaGetKeytab = null;

    super.close();
  }

  /**
   * Test to see if the specified principal exists in a previously configured IPA KDC
   * <p/>
   * This implementation creates a query to send to the ipa shell command and then interrogates
   * the result from STDOUT to determine if the presence of the specified principal.
   *
   * @param principal a String containing the principal to test
   * @param service   a boolean value indicating whether the principal is for a service or not
   * @return true if the principal exists; false otherwise
   * @throws KerberosOperationException if an unexpected error occurred
   */
  @Override
  public boolean principalExists(String principal, boolean service)
      throws KerberosOperationException {

    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (!StringUtils.isEmpty(principal)) {
      DeconstructedPrincipal deconstructedPrincipal = createDeconstructPrincipal(principal);
      String principalName = deconstructedPrincipal.getPrincipalName();

      String[] ipaCommand = new String[]{
          (service) ? "service-show" : "user-show",
          principalName
      };

      ShellCommandUtil.Result result = invokeIpa(ipaCommand);
      if (result.isSuccessful()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Creates a new principal in a previously configured KDC.
   * <p>
   * This implementation uses the ipa shell to create either a user or service account.  No password
   * will be set for either account type.  The password (or key) will be automatically generated by
   * the IPA server when exporting the keytab entry.  Upon success, this method will always return
   * <code>0</code> as the key number since the value is not generated until the keytab entry is
   * exported.
   *
   * @param principal a String containing the principal to add
   * @param password  a String containing the password to use when creating the principal (ignored)
   * @param service   a boolean value indicating whether the principal is to be created as a service principal or not
   * @return an Integer declaring the generated key number (always 0)
   * @throws KerberosOperationException
   */
  @Override
  public Integer createPrincipal(String principal, String password, boolean service)
      throws KerberosOperationException {

    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (StringUtils.isEmpty(principal)) {
      throw new KerberosOperationException("Failed to create new principal - no principal specified");
    }

    DeconstructedPrincipal deconstructedPrincipal = createDeconstructPrincipal(principal);
    String normalizedPrincipal = deconstructedPrincipal.getNormalizedPrincipal();

    String[] ipaCommand;
    if (service) {
      ipaCommand = new String[]{
          "service-add",
          normalizedPrincipal
      };
    } else {
      String principalName = deconstructedPrincipal.getPrincipalName();

      if (!principalName.equals(principalName.toLowerCase())) {
        LOG.warn("{} is not in lowercase. FreeIPA does not recognize user " +
            "principals that are not entirely in lowercase. This can lead to issues with kinit and keytabs. Make " +
            "sure users are in lowercase.", principalName);
      }

      ipaCommand = new String[]{
          "user-add",
          deconstructedPrincipal.getPrimary(),
          "--principal",
          principalName,
          "--first",
          deconstructedPrincipal.getPrimary(),
          "--last",
          deconstructedPrincipal.getPrimary(),
          "--cn",
          deconstructedPrincipal.getPrimary()
      };
    }

    ShellCommandUtil.Result result = invokeIpa(ipaCommand);
    if (!result.isSuccessful()) {
      String message = String.format("Failed to create principal for %s\n%s\nSTDOUT: %s\nSTDERR: %s",
          normalizedPrincipal, StringUtils.join(ipaCommand, " "), result.getStdout(), result.getStderr());
      LOG.error(message);

      String stdErr = result.getStderr();

      if ((stdErr != null) &&
          ((service && stdErr.contains(String.format("service with name \"%s\" already exists", normalizedPrincipal))) ||
          (!service && stdErr.contains(String.format("user with name \"%s\" already exists", deconstructedPrincipal.getPrimary()))))) {
        throw new KerberosPrincipalAlreadyExistsException(principal);
      } else {
        throw new KerberosOperationException(String.format("Failed to create principal for %s\nSTDOUT: %s\nSTDERR: %s",
            normalizedPrincipal, result.getStdout(), result.getStderr()));
      }
    }

    if ((!service) && !StringUtils.isEmpty(userPrincipalGroup)) {
      result = invokeIpa(new String[]{"group-add-member", userPrincipalGroup, "--users", deconstructedPrincipal.getPrimary()});
      if (!result.isSuccessful()) {
        LOG.warn("Failed to add account for {} to group {}: \nSTDOUT: {}\nSTDERR: {}",
            normalizedPrincipal, userPrincipalGroup, result.getStdout(), result.getStderr());
      }
    }

    // Always return 0 since we do not have a key to get a key number for.
    return 0;
  }

  /**
   * Removes an existing principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to remove
   * @param service   a boolean value indicating whether the principal is for a service or not
   * @return true if the principal was successfully removed; otherwise false
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  @Override
  public boolean removePrincipal(String principal, boolean service) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (StringUtils.isEmpty(principal)) {
      throw new KerberosOperationException("Failed to remove principal - no principal specified");
    }

    DeconstructedPrincipal deconstructedPrincipal = createDeconstructPrincipal(principal);

    String[] ipaCommand = (service)
        ? new String[]{"service-del", deconstructedPrincipal.getNormalizedPrincipal()}
        : new String[]{"user-del", deconstructedPrincipal.getPrincipalName()};

    return invokeIpa(ipaCommand).isSuccessful();
  }

  @Override
  protected String[] getKinitCommand(String executableKinit, PrincipalKeyCredential credentials, String credentialsCache, Map<String, String> kerberosConfiguration) {
    return new String[]{
        executableKinit,
        "-c",
        credentialsCache,
        credentials.getPrincipal()
    };
  }

  @Override
  protected void exportKeytabFile(String principal, String keytabFileDestinationPath, Set<EncryptionType> keyEncryptionTypes) throws KerberosOperationException {
    String encryptionTypeSpec = null;
    if (!CollectionUtils.isEmpty(keyEncryptionTypes)) {
      StringBuilder encryptionTypeSpecBuilder = new StringBuilder();
      for (EncryptionType encryptionType : keyEncryptionTypes) {
        if (encryptionTypeSpecBuilder.length() > 0) {
          encryptionTypeSpecBuilder.append(',');
        }
        encryptionTypeSpecBuilder.append(encryptionType.getName());
      }

      encryptionTypeSpec = encryptionTypeSpecBuilder.toString();
    }

    String[] createKeytabFileCommand = (StringUtils.isEmpty(encryptionTypeSpec))
        ? new String[]{executableIpaGetKeytab, "-s", getAdminServerHost(true), "-p", principal, "-k", keytabFileDestinationPath}
        : new String[]{executableIpaGetKeytab, "-s", getAdminServerHost(true), "-e", encryptionTypeSpec, "-p", principal, "-k", keytabFileDestinationPath};

    ShellCommandUtil.Result result = executeCommand(createKeytabFileCommand);
    if (!result.isSuccessful()) {
      String message = String.format("Failed to export the keytab file for %s:\n\tExitCode: %s\n\tSTDOUT: %s\n\tSTDERR: %s",
          principal, result.getExitCode(), result.getStdout(), result.getStderr());
      LOG.warn(message);
      throw new KerberosOperationException(message);
    }
  }

  /**
   * Invokes the ipa shell command with administrative credentials to issue queries
   *
   * @param query a String containing the query to send to the ipa command
   * @return a ShellCommandUtil.Result containing the result of the operation
   * @throws KerberosOperationException if an unexpected error occurred
   */
  private ShellCommandUtil.Result invokeIpa(String[] query) throws KerberosOperationException {

    if ((query == null) || (query.length == 0)) {
      throw new KerberosOperationException("Missing ipa query");
    }

    if (StringUtils.isEmpty(executableIpa)) {
      throw new KerberosOperationException("No path for ipa is available - this KerberosOperationHandler may not have been opened.");
    }

    String[] command = new String[query.length + 1];
    command[0] = executableIpa;
    System.arraycopy(query, 0, command, 1, query.length);

    ShellCommandUtil.Result result = executeCommand(command);

    if (result.isSuccessful()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Executed the following command:\n{}\nSTDOUT: {}\nSTDERR: {}",
            StringUtils.join(command, " "), result.getStdout(), result.getStderr());
      }
    } else {
      LOG.error("Failed to execute the following command:\n{}\nSTDOUT: {}\nSTDERR: {}",
          StringUtils.join(command, " "), result.getStdout(), result.getStderr());
    }

    return result;
  }
}
