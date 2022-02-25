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

package org.apache.ambari.server.controller;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.serveraction.kerberos.Component;
import org.apache.ambari.server.serveraction.kerberos.KerberosAdminAuthenticationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileWriter;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationException;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;

public interface KerberosHelper {
  /**
   * directive used to override the behavior of the kerberos-env/manage_identities property
   */
  String DIRECTIVE_MANAGE_KERBEROS_IDENTITIES = "manage_kerberos_identities";
  /**
   * directive used to indicate that the enable Kerberos operation is only to regenerate keytab files
   */
  String DIRECTIVE_REGENERATE_KEYTABS = "regenerate_keytabs";
  /**
   * directive used to pass host list to regenerate keytabs on
   */
  String DIRECTIVE_HOSTS = "regenerate_hosts";
  /**
   * directive used to pass list of services and their components to regenerate keytabs for
   */
  String DIRECTIVE_COMPONENTS = "regenerate_components";
  /**
   * directive used to indicate configurations are not to be updated (if set to "true") when regenerating
   * keytab files
   * @deprecated use {@link #DIRECTIVE_CONFIG_UPDATE_POLICY}
   */
  String DIRECTIVE_IGNORE_CONFIGS = "ignore_config_updates";
  /**
   * directive used to indicate how to handle configuration updates when regenerating keytab files
   *
   * expected values:
   * <ul>
   * <li>none</li>
   * <li>identities_only</li>
   * <li>new_and_identities</li>
   * <li>all</li>
   * </ul>
   *
   * @see UpdateConfigurationPolicy
   */
  String DIRECTIVE_CONFIG_UPDATE_POLICY = "config_update_policy";
  /**
   * directive used to indicate that the enable Kerberos operation should proceed even if the
   * cluster's security type is not changing
   */
  String DIRECTIVE_FORCE_TOGGLE_KERBEROS = "force_toggle_kerberos";
  /**
   * config type which contains the property used to determine if Kerberos is enabled
   */
  String SECURITY_ENABLED_CONFIG_TYPE = "cluster-env";
  /**
   * name of property which states whether kerberos is enabled
   */
  String SECURITY_ENABLED_PROPERTY_NAME = "security_enabled";
  /**
   * The alias to assign to the KDC administrator credential Keystore item
   */
  String KDC_ADMINISTRATOR_CREDENTIAL_ALIAS = "kdc.admin.credential";
  /**
   * The hostname used to hold the place of the actual hostname of the host that the Ambari server
   * is on.
   */
  String AMBARI_SERVER_HOST_NAME = "ambari_server";
  /**
   * The name of the Kerberos environment configuration type
   */
  String KERBEROS_ENV = "kerberos-env";
  /**
   * The name of the Ambari server's Kerberos identities as defined in the Kerberos descriptor
   */
  String AMBARI_SERVER_KERBEROS_IDENTITY_NAME = "ambari-server";
  /**
   * The kerberos-env property name declaring whether Ambari should manage its own required
   * identities or not
   */
  String CREATE_AMBARI_PRINCIPAL = "create_ambari_principal";
  /**
   * The kerberos-env property name declaring whether Ambari should manage the cluster's required
   * identities or not
   */
  String MANAGE_IDENTITIES = "manage_identities";
  /**
   * The kerberos-env property name declaring the default realm
   */
  String DEFAULT_REALM = "realm";
  /**
   * The kerberos-env property name declaring the kdc_type
   *
   * @see org.apache.ambari.server.serveraction.kerberos.KDCType
   */
  String KDC_TYPE = "kdc_type";
  /**
   * The kerberos-env property name declaring whether to manage auth-to-local rules or not
   */
  String MANAGE_AUTH_TO_LOCAL_RULES = "manage_auth_to_local";
  /**
   * The kerberos-env property name declaring whether the Hadoop auth_to_local rules should be included for all components of an installed service even if the component itself is not installed
   */
  String INCLUDE_ALL_COMPONENTS_IN_AUTH_TO_LOCAL_RULES = "include_all_components_in_auth_to_local_rules";
  /**
   * The kerberos-env property name declaring whether auth-to-local rules should be case-insensitive or not
   */
  String CASE_INSENSITIVE_USERNAME_RULES = "case_insensitive_username_rules";
  /**
   * The kerberos-env property name declaring how to preprocess services.
   * <p>
   * Expected values are <code>"ALL"</code>, <code>"DEFAULT"</code>, <code>"NONE"</code>
   *
   * @see org.apache.ambari.server.serveraction.kerberos.PreconfigureServiceType
   */
  String PRECONFIGURE_SERVICES = "preconfigure_services";

  /**
   * If {@code true}, then this will create the stages and tasks as being
   * retry-able. A failure during Kerberos operations will not cause the entire
   * request to be aborted.
   */
  String ALLOW_RETRY = "allow_retry_on_failure";

  /**
   * Used as key in config maps to store component to host assignment mapping.
   */
  String CLUSTER_HOST_INFO = "clusterHostInfo";

  /**
   * Toggles Kerberos security to enable it or remove it depending on the state of the cluster.
   * <p/>
   * The cluster "security_type" property is used to determine the security state of the cluster.
   * If the declared security type is KERBEROS, than an attempt will be make to enable Kerberos; if
   * the security type is NONE, an attempt will be made to disable Kerberos; otherwise, no operations
   * will be performed.
   * <p/>
   * It is expected tht the "kerberos-env" configuration type is available.   It is used to obtain
   * information about the Kerberos configuration, generally specific to the KDC being used.
   * <p/>
   * This process is idempotent such that it may be called several times, each time only affecting
   * items that need to be brought up to date.
   *
   * @param cluster               the relevant Cluster
   * @param securityType          the SecurityType to handle; this value is expected to be either
   *                              SecurityType.KERBEROS or SecurityType.NONE
   * @param requestStageContainer a RequestStageContainer to place generated stages, if needed -
   *                              if null a new RequestStageContainer will be created.
   * @param manageIdentities      a Boolean value indicating how to override the configured behavior
   *                              of managing Kerberos identities; if null the configured behavior
   *                              will not be overridden
   * @return the updated or a new RequestStageContainer containing the stages that need to be
   * executed to complete this task; or null if no stages need to be executed.
   * @throws AmbariException
   * @throws KerberosInvalidConfigurationException if an issue occurs trying to get the
   *                                               Kerberos-specific configuration details
   * @throws KerberosOperationException
   */
  RequestStageContainer toggleKerberos(Cluster cluster, SecurityType securityType,
                                       RequestStageContainer requestStageContainer,
                                       Boolean manageIdentities)
      throws AmbariException, KerberosOperationException;

  /**
   * Used to execute custom security operations which are sent as directives in URI
   *
   * @param cluster               the relevant Cluster
   * @param requestProperties     this structure is expected to hold already supported and validated directives
   *                              for the 'Cluster' resource. See ClusterResourceDefinition#getUpdateDirectives
   * @param requestStageContainer a RequestStageContainer to place generated stages, if needed -
   *                              if null a new RequestStageContainer will be created.
   * @param manageIdentities      a Boolean value indicating how to override the configured behavior
   *                              of managing Kerberos identities; if null the configured behavior
   *                              will not be overridden
   * @return the updated or a new RequestStageContainer containing the stages that need to be
   * executed to complete this task; or null if no stages need to be executed.
   * @throws AmbariException
   * @throws KerberosOperationException
   * @throws KerberosInvalidConfigurationException if an issue occurs trying to get the
   *                                               Kerberos-specific configuration details
   */
  RequestStageContainer executeCustomOperations(Cluster cluster, Map<String, String> requestProperties,
                                                RequestStageContainer requestStageContainer,
                                                Boolean manageIdentities)
      throws AmbariException, KerberosOperationException;

  /**
   * Ensures the set of filtered principals and keytabs exist on the cluster.
   * <p/>
   * No configurations will be altered as a result of this operation, however principals and keytabs
   * may be updated or created.
   * <p/>
   * It is expected that the "kerberos-env" configuration type is available.  It is used to obtain
   * information about the relevant KDC.  For example, the "kdc_type" property is used to determine
   * what type of KDC is being used so that the appropriate actions maybe taken in order interact
   * with it properly.
   * <p/>
   * It is expected tht the "kerberos-env" configuration type is available.   It is used to obtain
   * information about the Kerberos configuration, generally specific to the KDC being used.
   *
   * @param cluster                        the relevant Cluster
   * @param serviceComponentFilter         a Map of service names to component names indicating the
   *                                       relevant set of services and components - if null, no
   *                                       filter is relevant; if empty, the filter indicates no
   *                                       relevant services or components
   * @param hostFilter                     a set of hostname indicating the set of hosts to process -
   *                                       if null, no filter is relevant; if empty, the filter
   *                                       indicates no relevant hosts
   * @param identityFilter                 a Collection of identity names indicating the relevant
   *                                       identities - if null, no filter is relevant; if empty,
   *                                       the filter indicates no relevant identities
   * @param hostsToForceKerberosOperations a set of host names on which it is expected that the
   *                                       Kerberos client is or will be in the INSTALLED state by
   *                                       the time the operations targeted for them are to be
   *                                       executed - if empty or null, this no hosts will be
   *                                       "forced"
   * @param requestStageContainer          a RequestStageContainer to place generated stages, if
   *                                       needed - if null a new RequestStageContainer will be
   *                                       created.
   * @param manageIdentities               a Boolean value indicating how to override the configured behavior
   *                                       of managing Kerberos identities; if null the configured behavior
   *                                       will not be overridden
   * @return the updated or a new RequestStageContainer containing the stages that need to be
   * executed to complete this task; or null if no stages need to be executed.
   * @throws AmbariException
   * @throws KerberosOperationException
   * @throws KerberosInvalidConfigurationException if an issue occurs trying to get the
   *                                               Kerberos-specific configuration details
   */
  RequestStageContainer ensureIdentities(Cluster cluster, Map<String, ? extends Collection<String>> serviceComponentFilter,
                                         Set<String> hostFilter, Collection<String> identityFilter,
                                         Set<String> hostsToForceKerberosOperations, RequestStageContainer requestStageContainer,
                                         Boolean manageIdentities)
      throws AmbariException, KerberosOperationException;

  /**
   * Deletes the set of filtered principals and keytabs from the cluster.
   * <p/>
   * No configurations will be altered as a result of this operation, however principals and keytabs
   * may be removed.
   * <p/>
   * It is expected that the "kerberos-env" configuration type is available.  It is used to obtain
   * information about the relevant KDC.  For example, the "kdc_type" property is used to determine
   * what type of KDC is being used so that the appropriate actions maybe taken in order interact
   * with it properly.
   * <p/>
   * It is expected tht the "kerberos-env" configuration type is available.   It is used to obtain
   * information about the Kerberos configuration, generally specific to the KDC being used.
   *
   * @param cluster                the relevant Cluster
   * @param serviceComponentFilter a Map of service names to component names indicating the relevant
   *                               set of services and components - if null, no filter is relevant;
   *                               if empty, the filter indicates no relevant services or components
   * @param hostFilter             a set of hostname indicating the set of hosts to process -
   *                               if null, no filter is relevant; if empty, the filter
   *                               indicates no relevant hosts
   * @param identityFilter         a Collection of identity names indicating the relevant identities -
   *                               if null, no filter is relevant; if empty, the filter indicates no
   *                               relevant identities
   * @param requestStageContainer  a RequestStageContainer to place generated stages, if needed -
   *                               if null a new RequestStageContainer will be created.
   * @param manageIdentities       a Boolean value indicating how to override the configured behavior
   *                               of managing Kerberos identities; if null the configured behavior
   *                               will not be overridden
   * @return the updated or a new RequestStageContainer containing the stages that need to be
   * executed to complete this task; or null if no stages need to be executed.
   * @throws AmbariException
   * @throws KerberosOperationException
   * @throws KerberosInvalidConfigurationException if an issue occurs trying to get the
   *                                               Kerberos-specific configuration details
   */
  RequestStageContainer deleteIdentities(Cluster cluster, Map<String, ? extends Collection<String>> serviceComponentFilter,
                                         Set<String> hostFilter, Collection<String> identityFilter,
                                         RequestStageContainer requestStageContainer, Boolean manageIdentities)
      throws AmbariException, KerberosOperationException;

  void deleteIdentities(Cluster cluster, List<Component> components, Set<String> identities) throws AmbariException, KerberosOperationException;

  /**
   * Updates the relevant configurations for the components specified in the service filter.
   * <p/>
   * If <code>null</code> is passed in as the service filter, all installed services and components
   * will be affected.  If an empty map is passed in, no services or components will be affected.
   * <p/>
   * If the relevant services and components have Kerberos descriptors, configuration values from
   * the descriptors are used to update the relevant configuration sets.
   *
   * @param cluster       the relevant Cluster
   * @param serviceFilter a Map of service names to component names indicating the
   *                      relevant set of services and components - if null, no
   *                      filter is relevant; if empty, the filter indicates no
   *                      relevant services or components
   * @throws AmbariException
   */
  void configureServices(Cluster cluster, Map<String, Collection<String>> serviceFilter)
      throws AmbariException, KerberosInvalidConfigurationException;

  /**
   * Returns the updates configurations that are expected when the given set of services are configured
   * for Kerberos.
   *
   * @param cluster                    the cluster
   * @param existingConfigurations     the cluster's existing configurations
   * @param installedServices          the map of services and relevant components to process
   * @param serviceFilter              a Map of service names to component names indicating the
   *                                   relevant set of services and components - if null, no
   *                                   filter is relevant; if empty, the filter indicates no
   *                                   relevant services or components
   * @param previouslyExistingServices a set of previously existing service names - null or a subset of installedServices
   * @param kerberosEnabled            true if kerberos is (to be) enabled; otherwise false
   * @param applyStackAdvisorUpdates   true to invoke the stack advisor to validate property updates;
   *                                   false to skip
   * @return a map of configuration updates
   * @throws AmbariException
   * @throws KerberosInvalidConfigurationException if an issue occurs trying to get the
   *                                               Kerberos-specific configuration details
   */
  Map<String, Map<String, String>> getServiceConfigurationUpdates(Cluster cluster,
                                                                  Map<String, Map<String, String>> existingConfigurations,
                                                                  Map<String, Set<String>> installedServices,
                                                                  Map<String, Collection<String>> serviceFilter,
                                                                  Set<String> previouslyExistingServices,
                                                                  boolean kerberosEnabled,
                                                                  boolean applyStackAdvisorUpdates)
      throws KerberosInvalidConfigurationException, AmbariException;

  /**
   * Invokes the Stack Advisor to help determine relevant configuration changes when enabling or
   * disabling Kerberos. If kerberosEnabled = true, recommended properties are inserted into kerberosConfigurations,
   * while properties to remove in propertiesToRemove map. In case kerberosEnabled = false, recommended properties
   * are inserted into propertiesToInsert and properties to remove in propertiesToInsert map. This is because in
   * first case properties in kerberosConfigurations are going to be set, while in second case going to be
   * removed from cluster config.
   *
   * @param cluster                a cluster
   * @param services               a set of services that are being configured to enabled or disable Kerberos
   * @param existingConfigurations the cluster's existing configurations
   * @param kerberosConfigurations the configuration updates to make
   * @param propertiesToIgnore     the configuration properties that should be ignored when applying recommendations
   * @param propertiesToRemove     the configuration properties that must be removed from cluster config are inserted
   *                               into this map in case if provided (not null) and kerberosEnabled
   * @param kerberosEnabled        true if kerberos is (to be) enabled; otherwise false
   * @return the configuration updates
   * @throws AmbariException
   */
  Map<String, Map<String, String>> applyStackAdvisorUpdates(Cluster cluster, Set<String> services,
                                                            Map<String, Map<String, String>> existingConfigurations,
                                                            Map<String, Map<String, String>> kerberosConfigurations,
                                                            Map<String, Set<String>> propertiesToIgnore,
                                                            Map<String, Set<String>> propertiesToRemove,
                                                            boolean kerberosEnabled)
      throws AmbariException;

  /**
   * Ensures that the relevant headless (or user) Kerberos identities are created and cached.
   * <p/>
   * This can be called any number of times and only the missing identities will be created.
   *
   * @param cluster                the cluster
   * @param existingConfigurations the cluster's existing configurations
   * @param services               the set of services to process
   * @return
   * @throws AmbariException
   * @throws KerberosInvalidConfigurationException if an issue occurs trying to get the
   *                                               Kerberos-specific configuration details
   */
  boolean ensureHeadlessIdentities(Cluster cluster,
                                   Map<String, Map<String, String>> existingConfigurations,
                                   Set<String> services)
      throws KerberosInvalidConfigurationException, AmbariException;

  /**
   * Create a unique identity to use for testing the general Kerberos configuration.
   *
   * @param cluster               the relevant Cluster
   * @param commandParamsStage    the command parameters map that will be sent to the agent side command
   * @param requestStageContainer a RequestStageContainer to place generated stages, if needed -
   *                              if null a new RequestStageContainer will be created.
   * @return the updated or a new RequestStageContainer containing the stages that need to be
   * executed to complete this task; or null if no stages need to be executed.
   */
  RequestStageContainer createTestIdentity(Cluster cluster, Map<String, String> commandParamsStage,
                                           RequestStageContainer requestStageContainer)
      throws KerberosOperationException, AmbariException;

  /**
   * Deletes the unique identity to use for testing the general Kerberos configuration.
   *
   * @param cluster               the relevant Cluster
   * @param commandParamsStage    the command parameters map that will be sent to the agent side command
   * @param requestStageContainer a RequestStageContainer to place generated stages, if needed -
   *                              if null a new RequestStageContainer will be created.
   * @return the updated or a new RequestStageContainer containing the stages that need to be
   * executed to complete this task; or null if no stages need to be executed.
   */
  RequestStageContainer deleteTestIdentity(Cluster cluster, Map<String, String> commandParamsStage,
                                           RequestStageContainer requestStageContainer)
      throws KerberosOperationException, AmbariException;

  /**
   * Validate the KDC admin credentials.
   *
   * @param cluster associated cluster
   * @throws AmbariException if any other error occurs while trying to validate the credentials
   */
  void validateKDCCredentials(Cluster cluster) throws KerberosMissingAdminCredentialsException,
      KerberosAdminAuthenticationException,
      KerberosInvalidConfigurationException,
      AmbariException;

  /**
   * Sets the relevant auth-to-local rule configuration properties using the services installed on
   * the cluster and their relevant Kerberos descriptors to determine the rules to be created.
   *
   * @param cluster                 cluster instance
   * @param kerberosDescriptor      the current Kerberos descriptor
   * @param realm                   the default realm
   * @param installedServices       the map of services and relevant components to process
   * @param existingConfigurations  a map of the current configurations
   * @param kerberosConfigurations  a map of the configurations to update, this where the generated
   *                                auth-to-local values will be stored
   * @param includePreconfigureData <code>true</code> to include the preconfigure data; <code>false</code> otherwise
   * @throws AmbariException
   */
  void setAuthToLocalRules(Cluster cluster, KerberosDescriptor kerberosDescriptor,
                           String realm,
                           Map<String, Set<String>> installedServices,
                           Map<String, Map<String, String>> existingConfigurations,
                           Map<String, Map<String, String>> kerberosConfigurations,
                           boolean includePreconfigureData)
      throws AmbariException;

  /**
   * @param cluster                the cluster
   * @param kerberosDescriptor     the current Kerberos descriptor
   * @param serviceComponentFilter a Map of service names to component names indicating the
   *                               relevant set of services and components - if null, no
   *                               filter is relevant; if empty, the filter indicates no
   *                               relevant services or components
   * @param hostFilter             a set of hostname indicating the set of hosts to process -
   *                               if null, no filter is relevant; if empty, the filter
   *                               indicates no relevant hosts
   * @return a list of ServiceComponentHost instances and should be processed during the relevant
   * Kerberos operation.
   * @throws AmbariException
   */
  List<ServiceComponentHost> getServiceComponentHostsToProcess(Cluster cluster,
                                                               KerberosDescriptor kerberosDescriptor,
                                                               Map<String, ? extends Collection<String>> serviceComponentFilter,
                                                               Collection<String> hostFilter)
      throws AmbariException;

  Set<String> getHostsWithValidKerberosClient(Cluster cluster) throws AmbariException;

  /**
   * Builds a composite Kerberos descriptor using the default Kerberos descriptor and a user-specified
   * Kerberos descriptor, if it exists.
   * <p/>
   * The default Kerberos descriptor is built from the kerberos.json files in the stack. It can be
   * retrieved via the <code>stacks/:stackName/versions/:version/artifacts/kerberos_descriptor</code>
   * endpoint
   * <p/>
   * The user-specified Kerberos descriptor was registered to the
   * <code>cluster/:clusterName/artifacts/kerberos_descriptor</code> endpoint.
   * <p/>
   * If the user-specified Kerberos descriptor exists, it is used to update the default Kerberos
   * descriptor and the composite is returned.  If not, the default cluster descriptor is returned
   * as-is.
   *
   * @param cluster                 cluster instance
   * @param includePreconfigureData <code>true</code> to include the preconfigure data; <code>false</code> otherwise
   * @return the kerberos descriptor associated with the specified cluster
   * @throws AmbariException if unable to obtain the descriptor
   * @see #getKerberosDescriptor(KerberosDescriptorType, Cluster, boolean, Collection, boolean)
   */
  KerberosDescriptor getKerberosDescriptor(Cluster cluster, boolean includePreconfigureData) throws AmbariException;

  /**
   * Gets the requested Kerberos descriptor.
   * <p>
   * One of the following Kerberos descriptors will be returned - with or without pruning identity
   * definitions based on the evaluation of their <code>when</code> clauses:
   * <dl>
   * <dt>{@link KerberosDescriptorType#STACK}</dt>
   * <dd>A Kerberos descriptor built using data from the current stack definition, only</dd>
   * <dt>{@link KerberosDescriptorType#USER}</dt>
   * <dd>A Kerberos descriptor built using user-specified data stored as an artifact of the cluster, only</dd>
   * <dt>{@link KerberosDescriptorType#COMPOSITE}</dt>
   * <dd>A Kerberos descriptor built using data from the current stack definition with user-specified data stored as an artifact of the cluster applied
   * - see {@link #getKerberosDescriptor(Cluster, boolean)}</dd>
   * </dl>
   *
   * @param kerberosDescriptorType  the type of Kerberos descriptor to retrieve - see {@link KerberosDescriptorType}
   * @param cluster                 the relevant Cluster
   * @param evaluateWhenClauses     true to evaluate Kerberos identity <code>when</code> clauses and
   *                                prune if necessary; false otherwise.
   * @param additionalServices      an optional collection of service names declaring additional
   *                                services to add to the set of currently installed services to use
   *                                while evaluating <code>when</code> clauses
   * @param includePreconfigureData <code>true</code> to include the preconfigure data; <code>false</code> otherwise
   * @return a Kerberos descriptor
   * @throws AmbariException
   */
  KerberosDescriptor getKerberosDescriptor(KerberosDescriptorType kerberosDescriptorType, Cluster cluster,
                                           boolean evaluateWhenClauses, Collection<String> additionalServices, boolean includePreconfigureData)
      throws AmbariException;

  /**
   * Gets the Kerberos descriptor for the requested stack.
   * <p>
   * One of the following Kerberos descriptors will be returned:
   * <dl>
   * <dt>{@link KerberosDescriptorType#STACK}</dt>
   * <dd>A Kerberos descriptor built using data from the current stack definition, only</dd>
   * <dt>{@link KerberosDescriptorType#USER}</dt>
   * <dd>A Kerberos descriptor built using user-specified data stored as an artifact of the cluster, only</dd>
   * <dt>{@link KerberosDescriptorType#COMPOSITE}</dt>
   * <dd>A Kerberos descriptor built using data from the current stack definition with user-specified data stored as an artifact of the cluster applied
   * - see {@link #getKerberosDescriptor(Cluster, boolean)}</dd>
   * </dl>
   *
   * @param kerberosDescriptorType  the type of Kerberos descriptor to retrieve - see {@link KerberosDescriptorType}
   * @param cluster                 the relevant Cluster
   * @param stackId                 the relevant stack id, used for <code>COMPOSITE</code> or <code>STACK</code> Kerberos descriptor requests
   * @param includePreconfigureData <code>true</code> to include the preconfigure data; <code>false</code> otherwise
   * @return a Kerberos descriptor
   * @throws AmbariException
   */
  KerberosDescriptor getKerberosDescriptor(KerberosDescriptorType kerberosDescriptorType, Cluster cluster, StackId stackId, boolean includePreconfigureData)
      throws AmbariException;

  /**
   * Merges configurations from a Map of configuration updates into a main configurations Map.
   * <p>
   * Each property in the updates Map is processed to replace variables using the replacement Map,
   * unless it has been filtered out using an optionally supplied configuration type filter
   * (configurationTypeFilter).
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.VariableReplacementHelper#replaceVariables(String, java.util.Map)}
   * for information on variable replacement.
   *
   * @param configurations          a Map of existing configurations (updated in-place)
   * @param updates                 a Map of configuration updates
   * @param replacements            a Map of (grouped) replacement values
   * @param configurationTypeFilter a Set of config types to filter from the map of updates;
   *                                <code>null</code> indicate no filter
   * @return the updated configurations map
   * @throws AmbariException if an issue occurs
   */
  Map<String, Map<String, String>> mergeConfigurations(Map<String, Map<String, String>> configurations,
                                                       Map<String, KerberosConfigurationDescriptor> updates,
                                                       Map<String, Map<String, String>> replacements,
                                                       Set<String> configurationTypeFilter)
      throws AmbariException;

  /**
   * Determines which services, not currently installed, should be preconfigured to aid in reducing
   * the number of service restarts when new services are added to a cluster where Kerberos is enabled.
   *
   * @param configurations     a Map of existing configurations (updated in-place)
   * @param replacements       a Map of (grouped) replacement values
   * @param cluster            the cluster
   * @param kerberosDescriptor the Kerberos Descriptor
   * @return the updated configurations map
   * @throws AmbariException if an issue occurs
   */
  Map<String, Map<String, String>> processPreconfiguredServiceConfigurations(Map<String, Map<String, String>> configurations,
                                                                             Map<String, Map<String, String>> replacements, Cluster cluster,
                                                                             KerberosDescriptor kerberosDescriptor)
      throws AmbariException;

  /**
   * Adds identities to the KerberosIdentityDataFileWriter.
   *
   * @param kerberosIdentityDataFileWriter a KerberosIdentityDataFileWriter to use for storing identity
   *                                       records
   * @param identities                     a List of KerberosIdentityDescriptors to add to the data
   *                                       file
   * @param identityFilter                 a Collection of identity names indicating the relevant identities -
   *                                       if null, no filter is relevant; if empty, the filter indicates no
   *                                       relevant identities
   * @param hostname                       the relevant hostname
   * @param serviceName                    the relevant service name
   * @param componentName                  the relevant component name
   * @param kerberosConfigurations         a map of the configurations to update with identity-specific
   *                                       values
   * @param configurations                 a Map of configurations to use a replacements for variables
   *                                       in identity fields
   * @return an integer indicating the number of identities added to the data file
   * @throws java.io.IOException if an error occurs while writing a record to the data file
   */
  int addIdentities(KerberosIdentityDataFileWriter kerberosIdentityDataFileWriter,
                    Collection<KerberosIdentityDescriptor> identities,
                    Collection<String> identityFilter, String hostname, Long hostId, String serviceName,
                    String componentName, Map<String, Map<String, String>> kerberosConfigurations,
                    Map<String, Map<String, String>> configurations,
                    Map<String, ResolvedKerberosKeytab> resolvedKeytabs, String realm)
      throws IOException;
  /**
   * Calculates the map of configurations relative to the cluster and host.
   * <p/>
   * Most of this was borrowed from {@link org.apache.ambari.server.actionmanager.ExecutionCommandWrapper#getExecutionCommand()}
   *
   * @param cluster                      the relevant Cluster
   * @param hostname                     the relevant hostname
   * @param kerberosDescriptor a map of general Kerberos descriptor properties
   * @param includePreconfigureData <code>true</code> to include the preconfigure data; otherwise false
   * @param calculateClusterHostInfo
   * @return a Map of calculated configuration types
   * @throws AmbariException
   */
  Map<String, Map<String, String>> calculateConfigurations(Cluster cluster, String hostname,
                                                           KerberosDescriptor kerberosDescriptor,
                                                           boolean includePreconfigureData,
                                                           boolean calculateClusterHostInfo)
      throws AmbariException;

  /**
   * Determine if a cluster has kerberos enabled.
   *
   * @param cluster cluster to test
   * @return true if the provided cluster has kerberos enabled; false otherwise
   */
  boolean isClusterKerberosEnabled(Cluster cluster);

  /**
   * Tests the request properties to check for directives that need to be acted upon
   * <p/>
   * It is required that the SecurityType from the request is either KERBEROS or NONE and that at
   * least one directive in the requestProperties map is supported.
   *
   * @param requestSecurityType the SecurityType from the request
   * @param requestProperties   A Map of request directives and their values
   * @return true if custom operations should be executed; false otherwise
   */
  boolean shouldExecuteCustomOperations(SecurityType requestSecurityType, Map<String, String> requestProperties);

  /**
   * Retrieves the value of the manage_kerberos_identities directive from the request properties,
   * if it exists.
   * <p/>
   * If manage_kerberos_identities does not exist in the map of request properties, null is returned
   * <p/>
   * If manage_kerberos_identities does exists in the map of request properties, a Boolean value
   * is returned indicating whether its value is "false" (Boolean.FALSE) or not (Boolean.TRUE).
   *
   * @param requestProperties a map of the request property name/value pairs
   * @return Boolean.TRUE or Boolean.FALSE if the manage_kerberos_identities property exists in the map;
   * otherwise false
   */
  Boolean getManageIdentitiesDirective(Map<String, String> requestProperties);

  /**
   * Retrieves the value of the force_toggle_kerberos directive from the request properties,
   * if it exists.
   * <p/>
   * If force_toggle_kerberos does not exist in the map of request properties, <code>false</code> is
   * returned.
   * <p/>
   * If force_toggle_kerberos does exists in the map of request properties and is equal to "true",
   * then <code>true</code> is returned; otherwise <code>false</code> is returned.
   *
   * @param requestProperties a map of the request property name/value pairs
   * @return true if force_toggle_kerberos is "true"; otherwise false
   */
  boolean getForceToggleKerberosDirective(Map<String, String> requestProperties);

  /**
   * Given a list of KerberosIdentityDescriptors, returns a Map fo configuration types to property
   * names and values.
   * <p/>
   * The property names and values are not expected to have any variable replacements done.
   *
   * @param identityDescriptors a List of KerberosIdentityDescriptor from which to retrieve configurations
   * @return a Map of configuration types to property name/value pairs (as a Map)
   */
  Map<String, Map<String, String>> getIdentityConfigurations(List<KerberosIdentityDescriptor> identityDescriptors);

  /**
   * Returns the active identities for the named cluster.  Results are filtered by host, service,
   * and/or component; and grouped by host.
   * <p/>
   * The cluster name is mandatory; however the active identities may be filtered by one or more of
   * host, service, or component. A <code>null</code> value for any of these filters indicates no
   * filter for that parameter.
   * <p/>
   * The return values are grouped by host and optionally <code>_HOST</code> in principals will be
   * replaced with the relevant hostname if specified to do so.
   *
   * @param clusterName      the name of the relevant cluster (mandatory)
   * @param hostName         the name of a host for which to find results, null indicates all hosts
   * @param serviceName      the name of a service for which to find results, null indicates all
   *                         services
   * @param componentName    the name of a component for which to find results, null indicates all
   *                         components
   * @param replaceHostNames if true, _HOST in principals will be replace with the relevant host
   *                         name
   * @return a map of host names to kerberos identities
   * @throws AmbariException if an error occurs processing the cluster's active identities
   */
  Map<String, Collection<KerberosIdentityDescriptor>> getActiveIdentities(String clusterName,
                                                                          String hostName,
                                                                          String serviceName,
                                                                          String componentName,
                                                                          boolean replaceHostNames)
      throws AmbariException;

  /**
   * Gets the Ambari server Kerberos identities found in the Kerberos descriptor.
   *
   * @param kerberosDescriptor the kerberos descriptor
   */
  List<KerberosIdentityDescriptor> getAmbariServerIdentities(KerberosDescriptor kerberosDescriptor) throws AmbariException;

  /**
   * Determines if the Ambari identities should be created when enabling Kerberos.
   * <p>
   * If kerberos-env/create_ambari_principal is not set to false the identity should be calculated.
   *
   * @param kerberosEnvProperties the kerberos-env configuration properties
   * @return true if the Ambari identities should be created; otherwise false
   */
  boolean createAmbariIdentities(Map<String, String> kerberosEnvProperties);

  /**
   * Gets the previously stored KDC administrator credentials.
   *
   * @param clusterName the name of the relevant cluster
   * @return a PrincipalKeyCredential or null, if the KDC administrator credentials have not be set or
   * have been removed
   * @throws AmbariException if an error occurs while retrieving the credentials
   */
  PrincipalKeyCredential getKDCAdministratorCredentials(String clusterName) throws AmbariException;

  /**
   * Saves underlying entities in persistent storage.
   *
   * @param resolvedKerberosKeytab kerberos keytab to be persisted
   */
  void createResolvedKeytab(ResolvedKerberosKeytab resolvedKerberosKeytab);

  /**
   * Removes existent persisted keytabs if they are not in {@code expectedKeytabs} collection.
   *
   * @param expectedKeytabs collection to compare existent keytabs
   */
  void removeStaleKeytabs(Collection<ResolvedKerberosKeytab> expectedKeytabs);

  /**
   * Creates a temporary directory within the system temporary directory
   * <p/>
   * The resulting directory is to be removed by the caller when desired.
   *
   * @return a File pointing to the new temporary directory, or null if one was not created
   * @throws AmbariException if a new temporary directory cannot be created
   */
  File createTemporaryDirectory() throws AmbariException;

  /**
   * Translates a collection of configuration specifications (<code>config-type/property-name</code>)
   * to a map of configuration types to a set of property names.
   * <p>
   * For example:
   * <ul>
   * <li>config-type1/property-name1</li>
   * <li>config-type1/property-name2</li>
   * <li>config-type2/property-name3</li>
   * </ul>
   * Becomes
   * <ul>
   * <li>
   * config-type
   * <ul>
   * <li>property-name1</li>
   * <li>property-name2</li>
   * </ul>
   * </li>
   * <li>
   * config-type2
   * <ul>
   * <li>property-name3</li>
   * </ul>
   * </li>
   * </ul>
   *
   * @param configurationSpecifications a collection of configuration specifications (<code>config-type/property-name</code>)
   * @return a map of configuration types to sets of property names
   */
  Map<String, Set<String>> translateConfigurationSpecifications(Collection<String> configurationSpecifications);
  
  /**
   * Gathers the Kerberos-related data from configurations and stores it in a new KerberosDetails
   * instance.
   *
   * @param cluster          the relevant Cluster
   * @param manageIdentities a Boolean value indicating how to override the configured behavior
   *                         of managing Kerberos identities; if null the configured behavior
   *                         will not be overridden
   * @return a new KerberosDetails with the collected configuration data
   * @throws AmbariException
   */
  KerberosDetails getKerberosDetails(Cluster cluster, Boolean manageIdentities)
    throws KerberosInvalidConfigurationException, AmbariException;  

  /**
   * Types of Kerberos descriptors related to where the data is stored.
   * <dl>
   * <dt>STACK</dt>
   * <dd>A Kerberos descriptor built using data from a stack definition, only</dd>
   * <dt>USER</dt>
   * <dd>A Kerberos descriptor built using user-specified data stored as an artifact of a cluster, only</dd>
   * <dt>COMPOSITE</dt>
   * <dd>A Kerberos descriptor built using data from a stack definition with user-specified data stored as an artifact of a cluster applied</dd>
   * </dl>
   */
  enum KerberosDescriptorType {
    /**
     * A Kerberos descriptor built using data from a stack definition, only
     */
    STACK,

    /**
     * A Kerberos descriptor built using user-specified data stored as an artifact of a cluster, only
     */
    USER,

    /**
     * A Kerberos descriptor built using data from a stack definition with user-specified data stored as an artifact of a cluster applied
     */
    COMPOSITE
  }

  /**
   * Command to invoke against the Ambari backend.
   *
   * @param <T> the response type
   * @param <A> the argument type
   */
  interface Command<T, A> {
    /**
     * Invoke this command.
     *
     * @return the response
     * @throws AmbariException thrown if a problem occurred during invocation
     */
    T invoke(A arg) throws AmbariException;
  }
}
