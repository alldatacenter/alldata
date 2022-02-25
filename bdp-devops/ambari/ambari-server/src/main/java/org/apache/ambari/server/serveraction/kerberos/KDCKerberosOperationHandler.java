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
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.utils.HTTPUtils;
import org.apache.ambari.server.utils.HostAndPort;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KDCKerberosOperationHandler is an implementation of a KerberosOperationHandler providing
 * functionality KDC-based Kerberos providers.
 * <p>
 * This implementation provides kinit functionality and keytab file caching utilities for classes.
 */
abstract class KDCKerberosOperationHandler extends KerberosOperationHandler {
  private static final Logger LOG = LoggerFactory.getLogger(KDCKerberosOperationHandler.class);

  /**
   * The FQDN of the host where KDC administration server is
   */
  private String adminServerHost = null;

  /**
   * The FQDN and port where KDC administration server is
   */
  private String adminServerHostAndPort = null;

  /**
   * A map of principal names to {@link Keytab} entries to ensure a Keyab file is not created/exported
   * for the same principal more than once.
   */
  private HashMap<String, Keytab> cachedKeytabs = null;

  /**
   * A String containing the resolved path to the kinit executable
   */
  private String executableKinit = null;

  /**
   * The absolute path to the KDC administrator's Kerberos ticket cache.
   * <p>
   * This path is created as as temporary file with a randomized name when this {@link KerberosOperationHandler}
   * is open.  It is destoryed when this {@link KerberosOperationHandler} is closed.
   */
  private File credentialsCacheFile = null;

  /**
   * A Map of environmet values to send to system command invocations.
   * <p>
   * This map is to be appened to any map of environment values passed in when
   * invoking {@link KerberosOperationHandler#executeCommand(String[], Map, ShellCommandUtil.InteractiveHandler)}
   */
  private Map<String, String> environmentMap = null;

  @Override
  public void open(PrincipalKeyCredential administratorCredentials, String realm, Map<String, String> kerberosConfiguration)
      throws KerberosOperationException {

    super.open(administratorCredentials, realm, kerberosConfiguration);

    if (kerberosConfiguration != null) {
      // Spit the host and port from the the admin_server_host value
      String value = kerberosConfiguration.get(KERBEROS_ENV_ADMIN_SERVER_HOST);
      HostAndPort hostAndPort = HTTPUtils.getHostAndPortFromProperty(value);

      // hostAndPort will be null if the value is not in the form of host:port
      if (hostAndPort == null) {
        // host-only and host and port values are the same since there is no port
        // both are equal to the value from the property
        adminServerHost = value;
        adminServerHostAndPort = value;
      } else {
        // host-only is the split value;
        // host and port value is the value from the property
        adminServerHost = hostAndPort.host;
        adminServerHostAndPort = value;
      }
    }

    // Pre-determine the paths to relevant Kerberos executables
    executableKinit = getExecutable("kinit");

    setOpen(init(kerberosConfiguration));
  }

  @Override
  public void close() throws KerberosOperationException {

    if (credentialsCacheFile != null) {
      if (credentialsCacheFile.delete()) {
        LOG.debug("Failed to remove the cache file, {}", credentialsCacheFile.getAbsolutePath());
      }
      credentialsCacheFile = null;
    }

    environmentMap = null;
    executableKinit = null;
    cachedKeytabs = null;
    adminServerHost = null;
    adminServerHostAndPort = null;

    super.close();
  }

  /**
   * Updates the password for an existing user principal in a previously configured IPA KDC
   * <p/>
   * This implementation creates a query to send to the ipa shell command and then interrogates
   * the exit code to determine if the operation executed successfully.
   *
   * @param principal a String containing the principal to update
   * @param password  a String containing the password to set
   * @param service   a boolean value indicating whether the principal is for a service or not
   * @return an Integer declaring the new key number
   * @throws KerberosOperationException if an unexpected error occurred
   */
  @Override
  public Integer setPrincipalPassword(String principal, String password, boolean service) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    // It is expected that KerberosPrincipalDoesNotExistException is thrown if the principal does not exist.
    // The caller expects so that it can attempt to set the password for a principal without checking
    // to see if it exists first. If the principal does not exist and is required, the caller will
    // create it.  This saves a potentially unnecessary round trip to the KDC and back.
    if(!principalExists(principal, service)) {
      throw new KerberosPrincipalDoesNotExistException(String.format("Principal does not exist while attempting to set its password: %s", principal));
    }

    // This operation does nothing since a new key will be created when exporting the keytab file...
    return 0;
  }

  /**
   * Creates a key tab by using the ipa commandline utilities. It ignores key number and password
   * as this will be handled by IPA
   *
   * @param principal a String containing the principal to test
   * @param password  (IGNORED) a String containing the password to use when creating the principal
   * @param keyNumber (IGNORED) a Integer indicating the key number for the keytab entries
   * @return the created Keytab
   * @throws KerberosOperationException
   */
  @Override
  protected Keytab createKeytab(String principal, String password, Integer keyNumber)
      throws KerberosOperationException {

    if ((principal == null) || principal.isEmpty()) {
      throw new KerberosOperationException("Failed to create keytab file, missing principal");
    }

    // use cache if available
    if (cachedKeytabs.containsKey(principal)) {
      return cachedKeytabs.get(principal);
    }

    File keytabFile = null;

    try {
      try {
        keytabFile = File.createTempFile("ambari_tmp", ".keytab");

        // Remove the file else the command will fail...
        if (!keytabFile.delete()) {
          LOG.warn("Failed to remove temporary file to hold keytab.  Exporting the keytab file for {} may fail.", principal);
        }
      } catch (IOException e) {
        throw new KerberosOperationException(String.format("Failed to create the temporary file needed to hold the exported keytab file for %s: %s", principal, e.getLocalizedMessage()), e);
      }


      exportKeytabFile(principal, keytabFile.getAbsolutePath(), getKeyEncryptionTypes());

      Keytab keytab = readKeytabFile(keytabFile);
      cachedKeytabs.put(principal, keytab);
      return keytab;
    } finally {
      if ((keytabFile != null) && keytabFile.exists()) {
        if (!keytabFile.delete()) {
          LOG.debug("Failed to remove the temporary keytab file, {}", keytabFile.getAbsolutePath());
        }
      }
    }
  }

  /**
   * Executes a shell command in a credentials context
   * <p/>
   * See {@link ShellCommandUtil#runCommand(String[])}
   * <p>
   * This implementation sets the proper environment for the custom <code>KRB5CCNAME </code> value.
   *
   * @param command            an array of String value representing the command and its arguments
   * @param envp               a map of string, string of environment variables
   * @param interactiveHandler a handler to provide responses to queries from the command,
   *                           or null if no queries are expected
   * @return a ShellCommandUtil.Result declaring the result of the operation
   * @throws KerberosOperationException
   */
  @Override
  protected ShellCommandUtil.Result executeCommand(String[] command, Map<String, String> envp, ShellCommandUtil.InteractiveHandler interactiveHandler)
      throws KerberosOperationException {

    Map<String, String> _envp;

    if (MapUtils.isEmpty(environmentMap)) {
      _envp = envp;
    } else if (MapUtils.isEmpty(envp)) {
      _envp = environmentMap;
    } else {
      _envp = new HashMap<>();
      _envp.putAll(envp);
      _envp.putAll(environmentMap);
    }

    return super.executeCommand(command, _envp, interactiveHandler);
  }

  /**
   * Returns the KDC administration server host value (with or without the port)
   *
   * @param includePort <code>true</code> to include the port (if available); <code>false</code> to exclude the port
   * @return the KDC administration server host value (with or without the port)
   */
  String getAdminServerHost(boolean includePort) {
    return (includePort) ? adminServerHostAndPort : adminServerHost;
  }

  String getCredentialCacheFilePath() {
    return (credentialsCacheFile == null) ? null : credentialsCacheFile.getAbsolutePath();
  }

  /**
   * Return an array of Strings containing the command and the relavant arguments needed authenticate
   * with the KDC and create the Kerberos ticket/credential cache.
   *
   * @param executableKinit  the absolute path to the kinit executable
   * @param credentials      the KDC adminisrator's credentials
   * @param credentialsCache the absolute path to the expected location of the Kerberos ticket/credential cache file
   * @param kerberosConfigurations  a Map of key/value pairs containing data from the kerberos-env configuration set
   * @throws KerberosOperationException in case there was any error during kinit command creation
   * @return an array of Strings containing the command to execute
   */
  protected abstract String[] getKinitCommand(String executableKinit, PrincipalKeyCredential credentials, String credentialsCache, Map<String, String> kerberosConfigurations) throws KerberosOperationException;

  /**
   * Export the requested keytab entries for a given principal into the specified file.
   *
   * @param principal                 the principal name
   * @param keytabFileDestinationPath the absolute path to the keytab file
   * @param keyEncryptionTypes        a collection of encrption algorithm types indicating which ketyab entries are requested
   * @throws KerberosOperationException
   */
  protected abstract void exportKeytabFile(String principal, String keytabFileDestinationPath, Set<EncryptionType> keyEncryptionTypes) throws KerberosOperationException;

  /**
   * Initialize the Kerberos ticket cache using the supplied KDC administrator's credentials.
   * <p>
   * A randomly named temporary file is created to store the Kerberos ticket cache for this {@link KerberosOperationHandler}'s
   * session. The file will be removed upon closing when the session is complete.  The geneated ticket cache
   * filename is set in the environment variable map using the variable name "KRB5CCNAME". This will be passed
   * in for all relevant-system commands.
   *
   * @return
   * @throws KerberosOperationException
   */
  protected boolean init(Map<String, String> kerberosConfiguration) throws KerberosOperationException {
    if (credentialsCacheFile != null) {
      if (!credentialsCacheFile.delete()) {
        LOG.debug("Failed to remove the orphaned cache file, {}", credentialsCacheFile.getAbsolutePath());
      }
      credentialsCacheFile = null;
    }

    try {
      credentialsCacheFile = File.createTempFile("ambari_krb_", "cc");
      credentialsCacheFile.deleteOnExit();
      ensureAmbariOnlyAccess(credentialsCacheFile);
    } catch (IOException e) {
      throw new KerberosOperationException(String.format("Failed to create the temporary file needed to hold the administrator ticket cache: %s", e.getLocalizedMessage()), e);
    }

    String credentialsCache = String.format("FILE:%s", credentialsCacheFile.getAbsolutePath());

    environmentMap = new HashMap<>();
    environmentMap.put("KRB5CCNAME", credentialsCache);

    PrincipalKeyCredential credentials = getAdministratorCredential();

    ShellCommandUtil.Result result = executeCommand(getKinitCommand(executableKinit, credentials, credentialsCache, kerberosConfiguration),
        environmentMap,
        new InteractivePasswordHandler(String.valueOf(credentials.getKey()), null));

    if (!result.isSuccessful()) {
      String message = String.format("Failed to kinit as the KDC administrator user, %s:\n\tExitCode: %s\n\tSTDOUT: %s\n\tSTDERR: %s",
          credentials.getPrincipal(), result.getExitCode(), result.getStdout(), result.getStderr());
      LOG.warn(message);
      throw new KerberosAdminAuthenticationException(message);
    }

    cachedKeytabs = new HashMap<>();

    return true;
  }

  /**
   * Ensures that the owner of the Ambari server process is the only local user account able to
   * read and write to the specified file or read, write to, and execute the specified directory.
   *
   * @param file the file or directory for which to modify access
   */
  private void ensureAmbariOnlyAccess(File file) throws AmbariException {
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

  /**
   * InteractivePasswordHandler is a {@link ShellCommandUtil.InteractiveHandler}
   * implementation that answers queries from kadmin or kdamin.local command for the admin and/or user
   * passwords.
   */
  protected static class InteractivePasswordHandler implements ShellCommandUtil.InteractiveHandler {
    /**
     * The queue of responses to return
     */
    private LinkedList<String> responses;
    private Queue<String> currentResponses;

    /**
     * Constructor.
     *
     * @param adminPassword the KDC administrator's password (optional)
     * @param userPassword  the user's password (optional)
     */
    InteractivePasswordHandler(String adminPassword, String userPassword) {
      responses = new LinkedList<>();

      if (adminPassword != null) {
        responses.offer(adminPassword);
      }

      if (userPassword != null) {
        responses.offer(userPassword);
        responses.offer(userPassword);  // Add a 2nd time for the password "confirmation" request
      }

      currentResponses = new LinkedList<>(responses);
    }

    @Override
    public boolean done() {
      return currentResponses.size() == 0;
    }

    @Override
    public String getResponse(String query) {
      return currentResponses.poll();
    }

    @Override
    public void start() {
      currentResponses = new LinkedList<>(responses);
    }
  }
}
