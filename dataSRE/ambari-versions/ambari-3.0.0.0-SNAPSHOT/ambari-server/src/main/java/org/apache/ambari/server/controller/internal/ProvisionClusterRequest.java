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
package org.apache.ambari.server.controller.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfileBuilder;
import org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfileEvaluationException;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.ConfigurationFactory;
import org.apache.ambari.server.topology.Credential;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.NoSuchBlueprintException;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

/**
 * Request for provisioning a cluster.
 */
@SuppressWarnings("unchecked")
public class ProvisionClusterRequest extends BaseClusterRequest {
  /**
   * host groups property name
   */
  public static final String HOSTGROUPS_PROPERTY = "host_groups";

  /**
   * host group name property name
   */
  public static final String HOSTGROUP_NAME_PROPERTY = "name";

  /**
   * host group host count property name
   */
  public static final String HOSTGROUP_HOST_COUNT_PROPERTY = "host_count";

  /**
   * host group host predicate property name
   */
  public static final String HOSTGROUP_HOST_PREDICATE_PROPERTY = "host_predicate";

  /**
   * host group host fqdn property name
   */
  public static final String HOSTGROUP_HOST_FQDN_PROPERTY = "fqdn";


  /**
   * rack info property name
   */
  public static final String HOSTGROUP_HOST_RACK_INFO_PROPERTY = "rack_info";

  /**
   * host group hosts property name
   */
  public static final String HOSTGROUP_HOSTS_PROPERTY = "hosts";

  /**
   * configurations property name
   */
  public static final String CONFIGURATIONS_PROPERTY = "configurations";

  /**
   * default password property name
   */
  public static final String DEFAULT_PASSWORD_PROPERTY = "default_password";

  /**
   * configuration recommendation strategy property name
   */
  public static final String CONFIG_RECOMMENDATION_STRATEGY = "config_recommendation_strategy";

  /**
   * The repo version to use
   */
  public static final String REPO_VERSION_PROPERTY = "repository_version";

  /**
   * The repo version id to use
   */
  public static final String REPO_VERSION_ID_PROPERTY = "repository_version_id";

  /**
   * The global quick link filters property
   */
  public static final String QUICKLINKS_PROFILE_FILTERS_PROPERTY = "quicklinks_profile/filters";

  /**
   * The service and component level quick link filters property
   */
  public static final String QUICKLINKS_PROFILE_SERVICES_PROPERTY = "quicklinks_profile/services";

  public static final String ALIAS = "alias";
  public static final String PRINCIPAL = "principal";
  public static final String KEY = "key";
  public static final String TYPE = "type";


  /**
   * configuration factory
   */
  private static ConfigurationFactory configurationFactory = new ConfigurationFactory();

  /**
   * cluster name
   */
  private String clusterName;

  /**
   * default password
   */
  private String defaultPassword;

  private Map<String, Credential> credentialsMap;

  /**
   * configuration recommendation strategy
   */
  private final ConfigRecommendationStrategy configRecommendationStrategy;

  private String repoVersion;

  private Long repoVersionId;

  private final String quickLinksProfileJson;

  private final static Logger LOG = LoggerFactory.getLogger(ProvisionClusterRequest.class);

  /**
   * Constructor.
   *
   * @param properties  request properties
   * @param securityConfiguration  security config related properties
   */
  public ProvisionClusterRequest(Map<String, Object> properties, SecurityConfiguration securityConfiguration) throws
    InvalidTopologyTemplateException {
    setClusterName(String.valueOf(properties.get(
      ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID)));

    if (properties.containsKey(REPO_VERSION_PROPERTY)) {
      repoVersion = properties.get(REPO_VERSION_PROPERTY).toString();
    }

    if (properties.containsKey(REPO_VERSION_ID_PROPERTY)) {
      repoVersionId = Long.parseLong(properties.get(REPO_VERSION_ID_PROPERTY).toString());
    }

    if (properties.containsKey(DEFAULT_PASSWORD_PROPERTY)) {
      defaultPassword = String.valueOf(properties.get(DEFAULT_PASSWORD_PROPERTY));
    }

    try {
      parseBlueprint(properties);
    } catch (NoSuchStackException e) {
      throw new InvalidTopologyTemplateException("The specified stack doesn't exist: " + e, e);
    } catch (NoSuchBlueprintException e) {
      throw new InvalidTopologyTemplateException("The specified blueprint doesn't exist: " + e, e);
    }

    this.securityConfiguration = securityConfiguration;

    Configuration configuration = configurationFactory.getConfiguration(
      (Collection<Map<String, String>>) properties.get(CONFIGURATIONS_PROPERTY));
    configuration.setParentConfiguration(blueprint.getConfiguration());
    setConfiguration(configuration);

    parseHostGroupInfo(properties);

    this.credentialsMap = parseCredentials(properties);

    this.configRecommendationStrategy = parseConfigRecommendationStrategy(properties);

    setProvisionAction(parseProvisionAction(properties));

    try {
      this.quickLinksProfileJson = processQuickLinksProfile(properties);
    } catch (QuickLinksProfileEvaluationException ex) {
      throw new InvalidTopologyTemplateException("Invalid quick links profile", ex);
    }
  }

  private String processQuickLinksProfile(Map<String, Object> properties) throws QuickLinksProfileEvaluationException {
    Object globalFilters = properties.get(QUICKLINKS_PROFILE_FILTERS_PROPERTY);
    Object serviceFilters = properties.get(QUICKLINKS_PROFILE_SERVICES_PROPERTY);
    return (null != globalFilters || null != serviceFilters) ?
      new QuickLinksProfileBuilder().buildQuickLinksProfile(globalFilters, serviceFilters) : null;
  }


  private Map<String, Credential> parseCredentials(Map<String, Object> properties) throws
    InvalidTopologyTemplateException {
    HashMap<String, Credential> credentialHashMap = new HashMap<>();
    Set<Map<String, String>> credentialsSet = (Set<Map<String, String>>) properties.get(ClusterResourceProvider.CREDENTIALS);
    if (credentialsSet != null) {
      for (Map<String, String> credentialMap : credentialsSet) {
        String alias = Strings.emptyToNull(credentialMap.get(ALIAS));
        if (alias == null) {
          throw new InvalidTopologyTemplateException("credential.alias property is missing.");
        }
        String principal = Strings.emptyToNull(credentialMap.get(PRINCIPAL));
        if (principal == null) {
          throw new InvalidTopologyTemplateException("credential.principal property is missing.");
        }
        String key = Strings.emptyToNull(credentialMap.get(KEY));
        if (key == null) {
          throw new InvalidTopologyTemplateException("credential.key is missing.");
        }
        String typeString = Strings.emptyToNull(credentialMap.get(TYPE));
        if (typeString == null) {
          throw new InvalidTopologyTemplateException("credential.type is missing.");
        }
        CredentialStoreType type = Enums.getIfPresent(CredentialStoreType.class, typeString.toUpperCase()).orNull();
        if (type == null) {
          throw new InvalidTopologyTemplateException(
              String.format("credential.type [%s] is invalid. acceptable values: %s", typeString.toUpperCase(),
                  Arrays.toString(CredentialStoreType.values())));
        }
        credentialHashMap.put(alias, new Credential(alias, principal, key, type));
      }
    }
    return credentialHashMap;
  }

  public Map<String, Credential> getCredentialsMap() {
    return credentialsMap;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public ConfigRecommendationStrategy getConfigRecommendationStrategy() {
    return configRecommendationStrategy;
  }

  @Override
  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  @Override
  public Type getType() {
    return Type.PROVISION;
  }

  @Override
  public String getDescription() {
    return String.format("Provision Cluster '%s'", clusterName);
  }

  /**
   * Parse blueprint.
   *
   * @param properties  request properties
   *
   * @throws NoSuchStackException     if specified stack doesn't exist
   * @throws NoSuchBlueprintException if specified blueprint doesn't exist
   */
  private void parseBlueprint(Map<String, Object> properties) throws NoSuchStackException, NoSuchBlueprintException {
    String blueprintName = String.valueOf(properties.get(ClusterResourceProvider.BLUEPRINT));
    // set blueprint field
    setBlueprint(getBlueprintFactory().getBlueprint(blueprintName));

    if (blueprint == null) {
      throw new NoSuchBlueprintException(blueprintName);
    }
  }

  /**
   * Parse host group information.
   *
   * @param properties  request properties
   *
   * @throws InvalidTopologyTemplateException  if any validation checks on properties fail
   */
  private void parseHostGroupInfo(Map<String, Object> properties) throws InvalidTopologyTemplateException {
    Collection<Map<String, Object>> hostGroups =
      (Collection<Map<String, Object>>) properties.get(HOSTGROUPS_PROPERTY);

    if (hostGroups == null || hostGroups.isEmpty()) {
      throw new InvalidTopologyTemplateException("'host_groups' element must be included in cluster create body");
    }

    for (Map<String, Object> hostGroupProperties : hostGroups) {
      processHostGroup(hostGroupProperties);
    }
  }

  /**
   * Process host group properties.
   *
   * @param hostGroupProperties  host group properties
   *
   * @throws InvalidTopologyTemplateException if any validation checks on properties fail
   */
  private void processHostGroup(Map<String, Object> hostGroupProperties) throws InvalidTopologyTemplateException {
    String name = String.valueOf(hostGroupProperties.get(HOSTGROUP_NAME_PROPERTY));
    // String.valueOf() converts null to "null"
    if (name == null || name.equals("null") || name.isEmpty()) {
      throw new InvalidTopologyTemplateException("All host groups must contain a 'name' element");
    }

    HostGroupInfo hostGroupInfo = new HostGroupInfo(name);
    getHostGroupInfo().put(name, hostGroupInfo);

    processHostCountAndPredicate(hostGroupProperties, hostGroupInfo);
    processGroupHosts(name, (Collection<Map<String, String>>)
      hostGroupProperties.get(HOSTGROUP_HOSTS_PROPERTY), hostGroupInfo);

    // don't set the parent configuration
    hostGroupInfo.setConfiguration(configurationFactory.getConfiguration(
      (Collection<Map<String, String>>) hostGroupProperties.get(CONFIGURATIONS_PROPERTY)));
  }

  /**
   * Process host count and host predicate for a host group.
   *
   * @param hostGroupProperties  host group properties
   * @param hostGroupInfo        associated host group info instance
   *
   * @throws InvalidTopologyTemplateException  specified host group properties fail validation
   */
  private void processHostCountAndPredicate(Map<String, Object> hostGroupProperties, HostGroupInfo hostGroupInfo)
    throws InvalidTopologyTemplateException {

    if (hostGroupProperties.containsKey(HOSTGROUP_HOST_COUNT_PROPERTY)) {
      hostGroupInfo.setRequestedCount(Integer.parseInt(String.valueOf(
        hostGroupProperties.get(HOSTGROUP_HOST_COUNT_PROPERTY))));
      LOG.info("Stored expected hosts count {} for group {}",
        hostGroupInfo.getRequestedHostCount(), hostGroupInfo.getHostGroupName());
    }

    if (hostGroupProperties.containsKey(HOSTGROUP_HOST_PREDICATE_PROPERTY)) {
      if (hostGroupInfo.getRequestedHostCount() == 0) {
        throw new InvalidTopologyTemplateException(String.format(
          "Host group '%s' must not specify 'host_predicate' without 'host_count'",
          hostGroupInfo.getHostGroupName()));
      }

      String hostPredicate = String.valueOf(hostGroupProperties.get(HOSTGROUP_HOST_PREDICATE_PROPERTY));
      validateHostPredicateProperties(hostPredicate);
      try {
        hostGroupInfo.setPredicate(hostPredicate);
        LOG.info("Compiled host predicate {} for group {}", hostPredicate, hostGroupInfo.getHostGroupName());
      } catch (InvalidQueryException e) {
        throw new InvalidTopologyTemplateException(
          String.format("Unable to compile host predicate '%s': %s", hostPredicate, e), e);
      }
    }
  }

  /**
   * Process host group hosts.
   *
   * @param name           host group name
   * @param hosts          collection of host group host properties
   * @param hostGroupInfo  associated host group info instance
   *
   * @throws InvalidTopologyTemplateException specified host group properties fail validation
   */
  private void processGroupHosts(String name, Collection<Map<String, String>> hosts, HostGroupInfo hostGroupInfo)
    throws InvalidTopologyTemplateException {

    if (hosts != null) {
      if (hostGroupInfo.getRequestedHostCount() != 0) {
        throw new InvalidTopologyTemplateException(String.format(
          "Host group '%s' must not contain both a 'hosts' element and a 'host_count' value", name));
      }

      if (hostGroupInfo.getPredicate() != null) {
        throw new InvalidTopologyTemplateException(String.format(
          "Host group '%s' must not contain both a 'hosts' element and a 'host_predicate' value", name));
      }

      for (Map<String, String> hostProperties : hosts) {
        if (hostProperties.containsKey(HOSTGROUP_HOST_FQDN_PROPERTY)) {
          hostGroupInfo.addHost(hostProperties.get(HOSTGROUP_HOST_FQDN_PROPERTY));
        }

        if (hostProperties.containsKey(HOSTGROUP_HOST_RACK_INFO_PROPERTY)) {
          hostGroupInfo.addHostRackInfo(
            hostProperties.get(HOSTGROUP_HOST_FQDN_PROPERTY),
            hostProperties.get(HOSTGROUP_HOST_RACK_INFO_PROPERTY));
        }
      }
    }

    if (hostGroupInfo.getRequestedHostCount() == 0) {
      throw new InvalidTopologyTemplateException(String.format(
        "Host group '%s' must contain at least one 'hosts/fqdn' or a 'host_count' value", name));
    }
  }

  /**
   * Parse config recommendation strategy. Throws exception in case of the value is not correct.
   * The default value is {@link ConfigRecommendationStrategy#NEVER_APPLY}
   * @param properties request properties
   * @throws InvalidTopologyTemplateException specified config recommendation strategy property fail validation
   */
  private ConfigRecommendationStrategy parseConfigRecommendationStrategy(Map<String, Object> properties)
    throws InvalidTopologyTemplateException {
    if (properties.containsKey(CONFIG_RECOMMENDATION_STRATEGY)) {
      String configRecommendationStrategy = String.valueOf(properties.get(CONFIG_RECOMMENDATION_STRATEGY));
      Optional<ConfigRecommendationStrategy> configRecommendationStrategyOpt =
        Enums.getIfPresent(ConfigRecommendationStrategy.class, configRecommendationStrategy);
      if (!configRecommendationStrategyOpt.isPresent()) {
        throw new InvalidTopologyTemplateException(String.format(
          "Config recommendation strategy is not supported: %s", configRecommendationStrategy));
      }
      return configRecommendationStrategyOpt.get();
    } else {
      // default
      return ConfigRecommendationStrategy.NEVER_APPLY;
    }
  }

  /**
   * Parse Provision Action specified in RequestInfo properties.
   */
  private ProvisionAction parseProvisionAction(Map<String, Object> properties) throws InvalidTopologyTemplateException {
    if (properties.containsKey(PROVISION_ACTION_PROPERTY)) {
      String provisionActionStr = String.valueOf(properties.get(PROVISION_ACTION_PROPERTY));
      Optional<ProvisionAction> provisionActionOptional =
        Enums.getIfPresent(ProvisionAction.class, provisionActionStr);

      if (!provisionActionOptional.isPresent()) {
        throw new InvalidTopologyTemplateException(String.format(
          "Invalid provision_action specified in the template: %s", provisionActionStr));
      }
      return provisionActionOptional.get();
    } else {
      return ProvisionAction.INSTALL_AND_START;
    }
  }

  /**
   * @return the repository version, if any
   */
  public String getRepositoryVersion() {
    return repoVersion;
  }

  /**
   * @return the repository version id or {@code null}
   */
  public Long getRepositoryVersionId(){
    return repoVersionId;
  }

  /**
   * @return the quick links profile in Json string format
   */
  public String getQuickLinksProfileJson() {
    return quickLinksProfileJson;
  }

  public String getDefaultPassword() {
    return defaultPassword;
  }

}
