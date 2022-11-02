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
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.UpdateConfigurationPolicy;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.serveraction.AbstractServerAction;
import org.apache.ambari.server.serveraction.kerberos.stageutils.KerberosKeytabController;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

/**
 * KerberosServerAction is an abstract class to be implemented by Kerberos-related
 * {@link org.apache.ambari.server.serveraction.ServerAction} implementations.
 * <p/>
 * This class provides helper methods used to get common properties from the command parameters map
 * and iterate through the Kerberos identity metadata file
 * (see {@link org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileReader}).
 */
public abstract class KerberosServerAction extends AbstractServerAction {
  /**
   * A (command parameter) property name used to hold the authenticated user's name for use in
   * operations that record the acting user.
   */
  public static final String AUTHENTICATED_USER_NAME = "authenticated_user_name";

  /**
   * A (command parameter) property name used to hold the absolute path to the directory that is to
   * be used to store transient data while the request is being processed.  This is expected to be
   * a temporary directory.
   */
  public static final String DATA_DIRECTORY = "data_directory";

  /**
   * A (command parameter) property name used to hold the default Kerberos realm value.
   */
  public static final String DEFAULT_REALM = "default_realm";

  /**
   * A (command parameter) property name used to hold the (serialized) service/component filter map.
   */
  public static final String SERVICE_COMPONENT_FILTER = "service_component_filter";

  /**
   * A (command parameter) property name used to hold the (serialized) host filter list.
   */
  public static final String HOST_FILTER = "host_filter";

  /**
   * A (command parameter) property name used to hold the (serialized) identity filter list.
   */
  public static final String IDENTITY_FILTER = "identity_filter";

  /**
   * A (command parameter) property name used to hold the relevant KDC type value.  See
   * {@link org.apache.ambari.server.serveraction.kerberos.KDCType} for valid values
   */
  public static final String KDC_TYPE = "kdc_type";

  /**
   * A (command parameter) property name used to hold the value indicating how to process
   * configurations updates. One of the of the following values is expected:
   * <dl>
   * <dt>none</dt>
   * <dd>No configurations will be updated</dd>
   * <dt>identities_only</dt>
   * <dd>New and updated configurations related to Kerberos identity information - principal, keytab file, and auth-to-local rule properties</dd>
   * <dt>new_and_identities</dt>
   * <dd>Only new configurations declared by the Kerberos descriptor and stack advisor as well as the identity-related changes</dd>
   * <dt>all</dt>
   * <dd>All configuration changes (default)</dd>
   * </dl>
   */
  public static final String UPDATE_CONFIGURATION_POLICY = "update_configuration_policy";

  /**
   * A (command parameter) property name used to hold the note to set when applying any
   * configuration changes
   */
  public static final String UPDATE_CONFIGURATION_NOTE = "update_configuration_note";

  /**
   * The prefix to use for the data directory name.
   */
  public static final String DATA_DIRECTORY_PREFIX = ".ambari_";

  /**
   * Kerberos action shared data entry name for the principal-to-password map
   */
  private static final String PRINCIPAL_PASSWORD_MAP = "principal_password_map";

  /**
   * Kerberos action shared data entry name for the principal-to-key_number map
   */
  private static final String PRINCIPAL_KEY_NUMBER_MAP = "principal_key_number_map";

  /**
   * Key used in kerberosCommandParams in ExecutionCommand for base64 encoded keytab content
   */
  public static final String KEYTAB_CONTENT_BASE64 = "keytab_content_base64";

  /**
   * Key used in kerberosCommandParams in ExecutionCommand to indicate why type of creation operation to perform.
   *
   * @see OperationType
   */
  public static final String OPERATION_TYPE = "operation_type";

  /**
   * Key used in kerberosCommandParams in ExecutionCommand to indicate whether to include Ambari server indetity
   * ("true") or ignore it ("false")
   */
  public static final String INCLUDE_AMBARI_IDENTITY = "include_ambari_identity";

  /**
   * Keys used in CommandParams from ExecutionCommand to declare how to pre-configure services.
   * Expected values are, "ALL", "DEFAULT", and "NONE".
   */
  public static final String PRECONFIGURE_SERVICES = "preconfigure_services";

  private static final Logger LOG = LoggerFactory.getLogger(KerberosServerAction.class);

  /**
   * The Cluster that this ServerAction implementation is executing on
   */
  @Inject
  private Clusters clusters = null;

  /**
   * The KerberosOperationHandlerFactory to use to obtain KerberosOperationHandler instances
   * <p/>
   * This is needed to help with test cases to mock a KerberosOperationHandler
   */
  @Inject
  private KerberosOperationHandlerFactory kerberosOperationHandlerFactory;

  /**
   * KerberosHelper
   */
  @Inject
  private KerberosHelper kerberosHelper;

  @Inject
  private HostDAO hostDAO;

  @Inject
  private KerberosKeytabController kerberosKeytabController;

  /**
   * Given a (command parameter) Map and a property name, attempts to safely retrieve the requested
   * data.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @param propertyName      a String declaring the name of the item from commandParameters to retrieve
   * @return a String or null, depending on the property value and if it existed in commandParameters
   */
  protected static String getCommandParameterValue(Map<String, String> commandParameters, String propertyName) {
    return ((commandParameters == null) || (propertyName == null)) ? null : commandParameters.get(propertyName);
  }

  /**
   * Given a (command parameter) Map, attempts to safely retrieve the "update_configuration_policy" property.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @return a UpdateConfigurationPolicy
   */
  protected static UpdateConfigurationPolicy getUpdateConfigurationPolicy(Map<String, String> commandParameters) {
    String stringValue = getCommandParameterValue(commandParameters, UPDATE_CONFIGURATION_POLICY);
    UpdateConfigurationPolicy value = UpdateConfigurationPolicy.translate(stringValue);

    // Return UpdateConfigurationPolicy.ALL as a default value
    return (value == null) ? UpdateConfigurationPolicy.ALL : value;
  }

  /**
   * Given a (command parameter) Map, attempts to safely retrieve the "default_realm" property.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @return a String indicating the default realm or null (if not found or set)
   */
  protected static String getDefaultRealm(Map<String, String> commandParameters) {
    return getCommandParameterValue(commandParameters, DEFAULT_REALM);
  }

  /**
   * Given a (command parameter) Map, attempts to safely retrieve the "kdc_type" property.
   * <p/>
   * If not found, {@link org.apache.ambari.server.serveraction.kerberos.KDCType#MIT_KDC} will be
   * returned as a default value.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @return a KDCType or null (if not found or set)
   */
  protected static KDCType getKDCType(Map<String, String> commandParameters) {
    String kdcType = getCommandParameterValue(commandParameters, KDC_TYPE);

    return ((kdcType == null) || kdcType.isEmpty())
        ? KDCType.NONE
        : KDCType.translate(kdcType);
  }

  /**
   * Given a (command parameter) Map, attempts to safely retrieve the "data_directory" property.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @return a String indicating the data directory or null (if not found or set)
   */
  protected static String getDataDirectoryPath(Map<String, String> commandParameters) {
    return getCommandParameterValue(commandParameters, DATA_DIRECTORY);
  }

  /**
   * Given a (command parameter) Map, attempts to safely retrieve the "operation_type" property.
   *
   * @param commandParameters a Map containing the dictionary of data to interrogate
   * @return an OperationType
   */
  protected static OperationType getOperationType(Map<String, String> commandParameters) {
    String value = getCommandParameterValue(commandParameters, OPERATION_TYPE);
    if (StringUtils.isEmpty(value)) {
      return OperationType.DEFAULT;
    } else {
      return OperationType.valueOf(value.toUpperCase());
    }
  }

  /**
   * Sets the shared principal-to-password Map used to store principals and generated password for
   * use within the current request context.
   *
   * @param requestSharedDataContext a Map to be used as shared data among all ServerActions related
   *                                 to a given request
   * @param principalPasswordMap     A Map of principals and password to store
   */
  protected static void setPrincipalPasswordMap(Map<String, Object> requestSharedDataContext,
                                                Map<String, String> principalPasswordMap) {
    if (requestSharedDataContext != null) {
      requestSharedDataContext.put(PRINCIPAL_PASSWORD_MAP, principalPasswordMap);
    }
  }

  /**
   * Gets the shared principal-to-password Map used to store principals and generated password for
   * use within the current request context.
   * <p/>
   * If the requested Map is not found in requestSharedDataContext, one will be created and stored,
   * ensuring that a Map will always be returned, assuming requestSharedDataContext is not null.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return A Map of principals-to-password
   */
  protected static Map<String, String> getPrincipalPasswordMap(Map<String, Object> requestSharedDataContext) {
    if (requestSharedDataContext == null) {
      return null;
    } else {
      Object map = requestSharedDataContext.get(PRINCIPAL_PASSWORD_MAP);

      if (map == null) {
        map = new HashMap<String, String>();
        requestSharedDataContext.put(PRINCIPAL_PASSWORD_MAP, map);
      }

      return (Map<String, String>) map;
    }
  }

  /**
   * Gets the shared principal-to-key_number Map used to store principals and key numbers for
   * use within the current request context.
   * <p/>
   * If the requested Map is not found in requestSharedDataContext, one will be created and stored,
   * ensuring that a Map will always be returned, assuming requestSharedDataContext is not null.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return A Map of principals-to-key_numbers
   */
  protected static Map<String, Integer> getPrincipalKeyNumberMap(Map<String, Object> requestSharedDataContext) {
    if (requestSharedDataContext == null) {
      return null;
    } else {
      Object map = requestSharedDataContext.get(PRINCIPAL_KEY_NUMBER_MAP);

      if (map == null) {
        map = new HashMap<String, String>();
        requestSharedDataContext.put(PRINCIPAL_KEY_NUMBER_MAP, map);
      }

      return (Map<String, Integer>) map;
    }
  }

  /**
   * Returns the relevant cluster's name
   * <p/>
   * Using the data from the execution command, retrieve the relevant cluster's name.
   *
   * @return a String declaring the relevant cluster's name
   * @throws AmbariException if the cluster's name is not available
   */
  protected String getClusterName() throws AmbariException {
    ExecutionCommand executionCommand = getExecutionCommand();
    String clusterName = (executionCommand == null) ? null : executionCommand.getClusterName();

    if ((clusterName == null) || clusterName.isEmpty()) {
      throw new AmbariException("Failed to retrieve the cluster name from the execution command");
    }

    return clusterName;
  }

  /**
   * Returns the relevant Cluster object
   *
   * @return the relevant Cluster
   * @throws AmbariException if the Cluster object cannot be retrieved
   */
  protected Cluster getCluster() throws AmbariException {
    Cluster cluster = clusters.getCluster(getClusterName());

    if (cluster == null) {
      throw new AmbariException(String.format("Failed to retrieve cluster for %s", getClusterName()));
    }

    return cluster;
  }

  /**
   * The Clusters object for this KerberosServerAction
   *
   * @return a Clusters object
   */
  protected Clusters getClusters() {
    return clusters;
  }

  /**
   * Attempts to safely retrieve the "data_directory" property from the this action's relevant
   * command parameters Map.
   *
   * @return a String indicating the data directory or null (if not found or set)
   */
  protected String getDataDirectoryPath() {
    return getDataDirectoryPath(getCommandParameters());
  }

  /**
   * Returns preconfigure type passed to current action.
   *
   * @return PreconfigureServiceType
   */
  protected PreconfigureServiceType getCommandPreconfigureType() {
    String preconfigureServices = getCommandParameterValue(getCommandParameters(), PRECONFIGURE_SERVICES);
    PreconfigureServiceType type = null;
    if (!StringUtils.isEmpty(preconfigureServices)) {
      try {
        type = PreconfigureServiceType.valueOf(preconfigureServices.toUpperCase());
      } catch (Throwable t) {
        LOG.warn("Invalid preconfigure_services value, assuming DEFAULT: {}", preconfigureServices);
        type = PreconfigureServiceType.DEFAULT;
      }
    }
    return type;
  }

  /**
   * Iterates through the Kerberos identity metadata from the
   * {@link org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileReader} and calls
   * the implementing class to handle each identity found.
   * <p/>
   * Using {@link #getHostFilter()}, {@link #getIdentityFilter()} and {@link #getServiceComponentFilter()} it retrieve
   * list of filtered keytabs and their principals and process each principal using
   * {@link #processIdentity(ResolvedKerberosPrincipal, KerberosOperationHandler, Map, boolean, Map)}.
   *
   * @param requestSharedDataContext a Map to be used a shared data among all ServerActions related
   *                                 to a given request
   * @return a CommandReport indicating the result of this operation
   * @throws AmbariException
   */
  protected CommandReport processIdentities(Map<String, Object> requestSharedDataContext)
      throws AmbariException {
    CommandReport commandReport = null;
    Map<String, String> commandParameters = getCommandParameters();

    actionLog.writeStdOut("Processing identities...");
    LOG.info("Processing identities...");

    if (commandParameters != null) {
      // Grab the relevant data from this action's command parameters map
      PrincipalKeyCredential administratorCredential = kerberosHelper.getKDCAdministratorCredentials(getClusterName());
      KDCType kdcType = getKDCType(commandParameters);
      String defaultRealm = getDefaultRealm(commandParameters);

      KerberosOperationHandler handler = kerberosOperationHandlerFactory.getKerberosOperationHandler(kdcType);
      Map<String, String> kerberosConfiguration = getConfigurationProperties("kerberos-env");

      try {
        handler.open(administratorCredential, defaultRealm, kerberosConfiguration);
      } catch (KerberosOperationException e) {
        String message = String.format("Failed to process the identities, could not properly open the KDC operation handler: %s",
            e.getMessage());
        actionLog.writeStdErr(message);
        LOG.error(message);
        throw new AmbariException(message, e);
      }

      try {
        Map<String, Collection<String>> serviceComponentFilter = getServiceComponentFilter();
        if (serviceComponentFilter != null && pruneServiceFilter()) {
          kerberosKeytabController.adjustServiceComponentFilter(clusters.getCluster(getClusterName()), true, serviceComponentFilter);
        }
        final Collection<KerberosIdentityDescriptor> serviceIdentities = serviceComponentFilter == null ? null : kerberosKeytabController.getServiceIdentities(getClusterName(), serviceComponentFilter.keySet());
        for (ResolvedKerberosKeytab rkk : kerberosKeytabController.getFilteredKeytabs(serviceIdentities, getHostFilter(),getIdentityFilter())) {
          for (ResolvedKerberosPrincipal principal : rkk.getPrincipals()) {
            commandReport = processIdentity(principal, handler, kerberosConfiguration, isRelevantIdentity(serviceIdentities, principal), requestSharedDataContext);
            // If the principal processor returns a CommandReport, than it is time to stop
            // since an error condition has probably occurred, else all is assumed to be well.
            if (commandReport != null) {
              break;
            }
          }
        }
      } finally {
        // The KerberosOperationHandler needs to be closed, if it fails to close ignore the
        // exception since there is little we can or care to do about it now.
        try {
          handler.close();
        } catch (KerberosOperationException e) {
          // Ignore this...
        }
      }

    }

    actionLog.writeStdOut("Processing identities completed.");
    LOG.info("Processing identities completed.");

    // If commandReport is null, we can assume this operation was a success, so return a successful
    // CommandReport; else return the previously created CommandReport.
    return (commandReport == null)
        ? createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr())
        : commandReport;
  }

  protected boolean pruneServiceFilter() {
    return true;
  }

  private boolean isRelevantIdentity(Collection<KerberosIdentityDescriptor> serviceIdentities, ResolvedKerberosPrincipal principal) {
    if (serviceIdentities != null) {
      boolean hasValidIdentity = false;
      for (KerberosIdentityDescriptor serviceIdentity : serviceIdentities) {
        if (principal.getPrincipal().equals(serviceIdentity.getPrincipalDescriptor().getName()) && StringUtils.isBlank(serviceIdentity.getReference())) {
          hasValidIdentity = true;
          break;
        }
      }
      return hasValidIdentity;
    }

    return true;
  }

  /**
   * Processes an identity as necessary.
   * <p/>
   * This method is called from {@link #processIdentities(Map)} for each
   * principal found by specified filter. After processing, it is expected
   * that the return value is null on success and a CommandReport (indicating the error) on failure.
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
  protected abstract CommandReport processIdentity(ResolvedKerberosPrincipal resolvedPrincipal,
                                                   KerberosOperationHandler operationHandler,
                                                   Map<String, String> kerberosConfiguration,
                                                   boolean includedInFilter,
                                                   Map<String, Object> requestSharedDataContext)
      throws AmbariException;

  protected void deleteDataDirectory(String dataDirectoryPath) {
    // Make sure this is a relevant directory. We don't want to accidentally allow _ANY_ directory
    // to be deleted.
    if ((dataDirectoryPath != null) && dataDirectoryPath.contains("/" + DATA_DIRECTORY_PREFIX)) {
      File dataDirectory = new File(dataDirectoryPath);
      File dataDirectoryParent = dataDirectory.getParentFile();

      // Make sure this directory has a parent and it is writeable, else we wont be able to
      // delete the directory
      if ((dataDirectoryParent != null) && dataDirectory.isDirectory() &&
          dataDirectoryParent.isDirectory() && dataDirectoryParent.canWrite()) {
        try {
          FileUtils.deleteDirectory(dataDirectory);
        } catch (IOException e) {
          // We should log this exception, but don't let it fail the process since if we got to this
          // KerberosServerAction it is expected that the the overall process was a success.
          String message = String.format("The data directory (%s) was not deleted due to an error condition - {%s}",
              dataDirectory.getAbsolutePath(), e.getMessage());
          LOG.warn(message, e);
        }
      }
    }
  }


  protected Set<String> getHostFilter() {
    String serializedValue = getCommandParameterValue(HOST_FILTER);

    if (serializedValue != null) {
      Type type = new TypeToken<Set<String>>() {
      }.getType();
      return StageUtils.getGson().fromJson(serializedValue, type);
    } else {
      return null;
    }
  }

  protected boolean hasHostFilters() {
    Set<String> hostFilers = getHostFilter();
    return hostFilers != null && hostFilers.size() > 0;
  }


  protected Map<String, Collection<String>> getServiceComponentFilter() {
    String serializedValue = getCommandParameterValue(SERVICE_COMPONENT_FILTER);

    if (serializedValue != null) {
      Type type = new TypeToken<Map<String, ? extends Collection<String>>>() {
      }.getType();
      return StageUtils.getGson().fromJson(serializedValue, type);
    } else {
      return null;
    }
  }

  protected Collection<String> getIdentityFilter() {
    String serializedValue = getCommandParameterValue(IDENTITY_FILTER);

    if (serializedValue != null) {
      Type type = new TypeToken<Collection<String>>() {
      }.getType();
      return StageUtils.getGson().fromJson(serializedValue, type);
    } else {
      return null;
    }
  }

  protected Long ambariServerHostID() {
    String ambariServerHostName = StageUtils.getHostName();
    HostEntity ambariServerHostEntity = hostDAO.findByName(ambariServerHostName);
    return (ambariServerHostEntity == null)
        ? null
        : ambariServerHostEntity.getHostId();
  }

  /**
   * Retrieve the current set of properties for the requested config type for the relevant cluster.
   *
   * @return a Map of property names to property values for the requested config type; or null if no data is found
   * @throws AmbariException if an error occurs retrieving the relevant cluster details
   */
  protected Map<String, String> getConfigurationProperties(String configType) throws AmbariException {
    if (StringUtils.isNotEmpty(configType)) {
      Cluster cluster = getCluster();
      Config config = (cluster == null) ? null : cluster.getDesiredConfigByType(configType);
      Map<String, String> properties = (config == null) ? null : config.getProperties();

      if (properties == null) {
        LOG.warn("The '{}' configuration data is not available:" +
                "\n\tcluster: {}" +
                "\n\tconfig: {}" +
                "\n\tproperties: null",
            configType,
            (cluster == null) ? "null" : "not null",
            (config == null) ? "null" : "not null");
      }

      return properties;
    } else {
      return null;
    }
  }

  public static class KerberosCommandParameters {
    private Map<String, String> params;

    public KerberosCommandParameters(ExecutionCommand ec) {
      params = ec.getCommandParams();
    }

    public KerberosCommandParameters(AbstractServerAction serverAction) {
      this(serverAction.getExecutionCommand());
    }

    public Set<String> getHostFilter() {
      String serializedValue = getCommandParameterValue(HOST_FILTER);

      if (serializedValue != null) {
        Type type = new TypeToken<Set<String>>() {
        }.getType();
        return StageUtils.getGson().fromJson(serializedValue, type);
      } else {
        return null;
      }
    }

    public boolean hasHostFilters() {
      Set<String> hostFilers = getHostFilter();
      return hostFilers != null && hostFilers.size() > 0;
    }

    public Map<String, ? extends Collection<String>> getServiceComponentFilter() {
      String serializedValue = getCommandParameterValue(SERVICE_COMPONENT_FILTER);

      if (serializedValue != null) {
        Type type = new TypeToken<Map<String, ? extends Collection<String>>>() {
        }.getType();
        return StageUtils.getGson().fromJson(serializedValue, type);
      } else {
        return null;
      }
    }

    public Collection<String> getIdentityFilter() {
      String serializedValue = getCommandParameterValue(IDENTITY_FILTER);

      if (serializedValue != null) {
        Type type = new TypeToken<Collection<String>>() {
        }.getType();
        return StageUtils.getGson().fromJson(serializedValue, type);
      } else {
        return null;
      }
    }

    public String getCommandParameterValue(String propertyName) {
      Map<String, String> commandParameters = params;
      return (commandParameters == null) ? null : commandParameters.get(propertyName);
    }
  }

  /**
   * A Kerberos operation type
   * <ul>
   * <li>RECREATE_ALL - regenerate keytabs for all principals</li>
   * <li>CREATE_MISSING - generate keytabs for only those that are missing</li>
   * <li>DEFAULT - generate needed keytabs for new components</li>
   * </ul>
   */
  public enum OperationType {
    /**
     * Regenerate keytabs for all principals
     */
    RECREATE_ALL,

    /**
     * Generate keytabs for only those that are missing
     */
    CREATE_MISSING,

    /**
     * Generate needed keytabs for new components
     */
    DEFAULT
  }
}
