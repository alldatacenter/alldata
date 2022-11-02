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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.state.kerberos.VariableReplacementHelper;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * MITKerberosOperationHandler is an implementation of a KerberosOperationHandler providing
 * functionality specifically for an MIT KDC. See http://web.mit.edu/kerberos.
 * <p/>
 * It is assumed that a MIT Kerberos client is installed and that the kdamin shell command is
 * available
 */
public class MITKerberosOperationHandler extends KDCKerberosOperationHandler {

  private final static Logger LOG = LoggerFactory.getLogger(MITKerberosOperationHandler.class);

  @Inject
  private Configuration configuration;
  
  @Inject
  private VariableReplacementHelper variableReplacementHelper;

  /**
   * A String containing user-specified attributes used when creating principals
   */
  private String createAttributes = null;

  /**
   * A String containing the resolved path to the kdamin executable
   */
  private String executableKadmin = null;

  /**
   * Prepares and creates resources to be used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used before this call.
   * <p/>
   * The kerberosConfiguration Map is not being used.
   *
   * @param administratorCredentials a PrincipalKeyCredential containing the administrative credential
   *                                 for the relevant KDC
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
      createAttributes = kerberosConfiguration.get(KERBEROS_ENV_KDC_CREATE_ATTRIBUTES);
    }

    // Pre-determine the paths to relevant Kerberos executables
    executableKadmin = getExecutable("kadmin");

    super.open(administratorCredentials, realm, kerberosConfiguration);
  }

  @Override
  public void close() throws KerberosOperationException {
    createAttributes = null;
    executableKadmin = null;

    super.close();
  }

  /**
   * Test to see if the specified principal exists in a previously configured MIT KDC
   * <p/>
   * This implementation creates a query to send to the kadmin shell command and then interrogates
   * the result from STDOUT to determine if the presence of the specified principal.
   *
   * @param principal a String containing the principal to test
   * @param service   a boolean value indicating whether the principal is for a service or not
   * @return true if the principal exists; false otherwise
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  @Override
  public boolean principalExists(String principal, boolean service)
      throws KerberosOperationException {

    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    if (!StringUtils.isEmpty(principal)) {
      // Create the KAdmin query to execute:
      ShellCommandUtil.Result result = invokeKAdmin(String.format("get_principal %s", principal));

      // If there is data from STDOUT, see if the following string exists:
      //    Principal: <principal>
      String stdOut = result.getStdout();
      return (stdOut != null) && stdOut.contains(String.format("Principal: %s", principal));
    }

    return false;
  }

  /**
   * Creates a new principal in a previously configured MIT KDC
   * <p/>
   * This implementation creates a query to send to the kadmin shell command and then interrogates
   * the result from STDOUT to determine if the operation executed successfully.
   *
   * @param principal a String containing the principal add
   * @param password  a String containing the password to use when creating the principal
   * @param service   a boolean value indicating whether the principal is to be created as a service principal or not
   * @return an Integer declaring the generated key number
   * @throws KerberosKDCConnectionException          if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException    if the administrator credentials fail to authenticate
   * @throws KerberosRealmException                  if the realm does not map to a KDC
   * @throws KerberosPrincipalAlreadyExistsException if the principal already exists
   * @throws KerberosOperationException              if an unexpected error occurred
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

    // Create the kdamin query:  add_principal <-randkey|-pw <password>> [<options>] <principal>
    ShellCommandUtil.Result result = invokeKAdmin(String.format("add_principal -randkey %s %s",
        (createAttributes == null) ? "" : createAttributes, principal));

    // If there is data from STDOUT, see if the following string exists:
    //    Principal "<principal>" created
    String stdOut = result.getStdout();
    String stdErr = result.getStderr();
    if ((stdOut != null) && stdOut.contains(String.format("Principal \"%s\" created", principal))) {
      return 0;
    } else if ((stdErr != null) && stdErr.contains(String.format("Principal or policy already exists while creating \"%s\"", principal))) {
      throw new KerberosPrincipalAlreadyExistsException(principal);
    } else {
      LOG.error("Failed to execute kadmin query: add_principal -pw \"********\" {} {}\nSTDOUT: {}\nSTDERR: {}",
          (createAttributes == null) ? "" : createAttributes, principal, stdOut, result.getStderr());
      throw new KerberosOperationException(String.format("Failed to create service principal for %s\nSTDOUT: %s\nSTDERR: %s",
          principal, stdOut, result.getStderr()));
    }
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

    ShellCommandUtil.Result result = invokeKAdmin(String.format("delete_principal -force %s", principal));

    // If there is data from STDOUT, see if the following string exists:
    //    Principal "<principal>" created
    String stdOut = result.getStdout();
    return (stdOut != null) && !stdOut.contains("Principal does not exist");
  }

  /**
   * Invokes the kadmin shell command to issue queries
   *
   * @param query a String containing the query to send to the kdamin command
   * @return a ShellCommandUtil.Result containing the result of the operation
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  protected ShellCommandUtil.Result invokeKAdmin(String query)
      throws KerberosOperationException {
    if (StringUtils.isEmpty(query)) {
      throw new KerberosOperationException("Missing kadmin query");
    }

    if (StringUtils.isEmpty(executableKadmin)) {
      throw new KerberosOperationException("No path for kadmin is available - this KerberosOperationHandler may not have been opened.");
    }

    List<String> command = new ArrayList<>();
    command.add(executableKadmin);

    // Add the credential cache, if available
    String credentialCacheFilePath = getCredentialCacheFilePath();
    if (!StringUtils.isEmpty(credentialCacheFilePath)) {
      command.add("-c");
      command.add(credentialCacheFilePath);
    }

    // Add explicit KDC admin host, if available
    String adminSeverHost = getAdminServerHost(true);
    if (!StringUtils.isEmpty(adminSeverHost)) {
      command.add("-s");
      command.add(adminSeverHost);
    }

    // Add default realm clause, if available
    String defaultRealm = getDefaultRealm();
    if (!StringUtils.isEmpty(defaultRealm)) {
      command.add("-r");
      command.add(defaultRealm);
    }

    // Add kadmin query
    command.add("-q");
    command.add(query);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Executing: {}", command);
    }

    ShellCommandUtil.Result result = null;
    int retryCount = configuration.getKerberosOperationRetries();
    int tries = 0;

    while (tries <= retryCount) {
      try {
        result = executeCommand(command.toArray(new String[command.size()]));
      } catch (KerberosOperationException exception) {
        if (tries == retryCount) {
          throw exception;
        }
      }

      if (result != null && result.isSuccessful()) {
        break; // break on successful result
      }
      tries++;

      try {
        Thread.sleep(1000 * configuration.getKerberosOperationRetryTimeout());
      } catch (InterruptedException ignored) {
      }

      String message = String.format("Retrying to execute kadmin after a wait of %d seconds :\n\tCommand: %s",
          configuration.getKerberosOperationRetryTimeout(),
          command);
      LOG.warn(message);
    }


    if ((result == null) || !result.isSuccessful()) {
      int exitCode = (result == null) ? -999 : result.getExitCode();
      String stdOut = (result == null) ? "" : result.getStdout();
      String stdErr = (result == null) ? "" : result.getStderr();

      String message = String.format("Failed to execute kadmin:\n\tCommand: %s\n\tExitCode: %s\n\tSTDOUT: %s\n\tSTDERR: %s",
          command, exitCode, stdOut, stdErr);
      LOG.warn(message);

      // Test STDERR to see of any "expected" error conditions were encountered...
      // Did admin credentials fail?
      if (stdErr.contains("Client not found in Kerberos database")) {
        throw new KerberosAdminAuthenticationException(stdErr);
      } else if (stdErr.contains("Incorrect password while initializing")) {
        throw new KerberosAdminAuthenticationException(stdErr);
      }
      // Did we fail to connect to the KDC?
      else if (stdErr.contains("Cannot contact any KDC")) {
        throw new KerberosKDCConnectionException(stdErr);
      } else if (stdErr.contains("Cannot resolve network address for admin server in requested realm while initializing kadmin interface")) {
        throw new KerberosKDCConnectionException(stdErr);
      }
      // Was the realm invalid?
      else if (stdErr.contains("Missing parameters in krb5.conf required for kadmin client")) {
        throw new KerberosRealmException(stdErr);
      } else if (stdErr.contains("Cannot find KDC for requested realm while initializing kadmin interface")) {
        throw new KerberosRealmException(stdErr);
      } else {
        throw new KerberosOperationException(String.format("Unexpected error condition executing the kadmin command. STDERR: %s", stdErr));
      }
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Executed the following command:\n{}\nSTDOUT: {}\nSTDERR: {}",
            StringUtils.join(command, " "), result.getStdout(), result.getStderr());
      }
    }

    return result;
  }

  @Override
  protected String[] getKinitCommand(String executableKinit, PrincipalKeyCredential credentials, String credentialsCache, Map<String, String> kerberosConfiguration) throws KerberosOperationException {
    // kinit -c <path> -S kadmin/`hostname -f` <principal>
    try {
      final String kadminPrincipalName = variableReplacementHelper.replaceVariables(kerberosConfiguration.get(KERBEROS_ENV_KADMIN_PRINCIPAL_NAME), buildReplacementsMap(kerberosConfiguration));
      return new String[]{
          executableKinit,
          "-c",
          credentialsCache,
          "-S",
          kadminPrincipalName,
          credentials.getPrincipal()
      };
    } catch (AmbariException e) {
      throw new KerberosOperationException("Error while getting 'kinit' command", e);
    }
  }

  private Map<String, Map<String, String>> buildReplacementsMap(Map<String, String> kerberosConfiguration) {
    final Map<String, Map<String, String>> replacementsMap = new HashMap<>();
    replacementsMap.put("", kerberosConfiguration);
    return replacementsMap;
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
        encryptionTypeSpecBuilder.append(":normal");
      }

      encryptionTypeSpec = encryptionTypeSpecBuilder.toString();
    }

    String query = (StringUtils.isEmpty(encryptionTypeSpec))
        ? String.format("xst -k \"%s\" %s", keytabFileDestinationPath, principal)
        : String.format("xst -k \"%s\" -e %s %s", keytabFileDestinationPath, encryptionTypeSpec, principal);

    ShellCommandUtil.Result result = invokeKAdmin(query);

    if (!result.isSuccessful()) {
      String message = String.format("Failed to export the keytab file for %s:\n\tExitCode: %s\n\tSTDOUT: %s\n\tSTDERR: %s",
          principal, result.getExitCode(), result.getStdout(), result.getStderr());
      LOG.warn(message);
      throw new KerberosOperationException(message);
    }
  }
}
