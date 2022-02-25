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

package org.apache.ambari.server.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyConditionInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.utils.SecretReference;
import org.apache.ambari.server.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Default blueprint validator.
 */
@StaticallyInject
public class BlueprintValidatorImpl implements BlueprintValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintValidatorImpl.class);
  private final Blueprint blueprint;
  private final Stack stack;

  public static final String LZO_CODEC_CLASS_PROPERTY_NAME = "io.compression.codec.lzo.class";
  public static final String CODEC_CLASSES_PROPERTY_NAME = "io.compression.codecs";
  public static final String LZO_CODEC_CLASS = "com.hadoop.compression.lzo.LzoCodec";

  @Inject
  private static org.apache.ambari.server.configuration.Configuration configuration;

  public BlueprintValidatorImpl(Blueprint blueprint) {
    this.blueprint = blueprint;
    this.stack = blueprint.getStack();
  }

  @Override
  public void validateTopology() throws InvalidTopologyException {
    LOGGER.info("Validating topology for blueprint: [{}]", blueprint.getName());
    Collection<HostGroup> hostGroups = blueprint.getHostGroups().values();
    Map<String, Map<String, Collection<DependencyInfo>>> dependenciesValidationIssues = new HashMap<>();

    for (HostGroup group : hostGroups) {
      Map<String, Collection<DependencyInfo>> groupDependenciesValidationIssues =
          validateHostGroup(group, "inclusive");

      groupDependenciesValidationIssues.putAll(validateHostGroup(group, "exclusive"));
      if (!groupDependenciesValidationIssues.isEmpty()) {
        dependenciesValidationIssues.put(group.getName(), groupDependenciesValidationIssues);
      }
    }

    Collection<String> cardinalityFailures = new HashSet<>();
    Collection<String> services = blueprint.getServices();

    for (String service : services) {
      for (String component : stack.getComponents(service)) {
        Cardinality cardinality = stack.getCardinality(component);
        AutoDeployInfo autoDeploy = stack.getAutoDeployInfo(component);
        if (cardinality.isAll()) {
          cardinalityFailures.addAll(verifyComponentInAllHostGroups(component, autoDeploy));
        } else {
          cardinalityFailures.addAll(verifyComponentCardinalityCount(
            component, cardinality, autoDeploy));
        }
      }
    }

    if (!dependenciesValidationIssues.isEmpty() || !cardinalityFailures.isEmpty()) {
      generateInvalidTopologyException(dependenciesValidationIssues, cardinalityFailures);
    }
  }

  @Override
  public void validateRequiredProperties() throws InvalidTopologyException, GPLLicenseNotAcceptedException {

    // we don't want to include default stack properties so we can't just use hostGroup full properties
    Map<String, Map<String, String>> clusterConfigurations = blueprint.getConfiguration().getProperties();

    // we need to have real passwords, not references
    if (clusterConfigurations != null) {

      // need to reject blueprints that have LZO enabled if the Ambari Server hasn't been configured for it
      boolean gplEnabled = configuration.getGplLicenseAccepted();

      StringBuilder errorMessage = new StringBuilder();
      boolean containsSecretReferences = false;
      for (Map.Entry<String, Map<String, String>> configEntry : clusterConfigurations.entrySet()) {
        String configType = configEntry.getKey();
        if (configEntry.getValue() != null) {
          for (Map.Entry<String, String> propertyEntry : configEntry.getValue().entrySet()) {
            String propertyName = propertyEntry.getKey();
            String propertyValue = propertyEntry.getValue();
            if (propertyValue != null) {
              if (!gplEnabled && configType.equals("core-site")
                  && (propertyName.equals(LZO_CODEC_CLASS_PROPERTY_NAME) || propertyName.equals(CODEC_CLASSES_PROPERTY_NAME))
                  && propertyValue.contains(LZO_CODEC_CLASS)) {
                throw new GPLLicenseNotAcceptedException("Your Ambari server has not been configured to download LZO GPL software. " +
                    "Please refer to documentation to configure Ambari before proceeding.");
              }
              if (SecretReference.isSecret(propertyValue)) {
                errorMessage.append("  Config:" + configType + " Property:" + propertyName + "\n");
                containsSecretReferences = true;
              }
            }
          }
        }
      }
      if (containsSecretReferences) {
        throw new InvalidTopologyException("Secret references are not allowed in blueprints, " +
          "replace following properties with real passwords:\n" + errorMessage);
      }
    }


    for (HostGroup hostGroup : blueprint.getHostGroups().values()) {
      Map<String, Map<String, String>> operationalConfiguration = new HashMap<>(clusterConfigurations);

      operationalConfiguration.putAll(hostGroup.getConfiguration().getProperties());
      for (String component : hostGroup.getComponentNames()) {
        //check that MYSQL_SERVER component is not available while hive is using existing db
        if (component.equals("MYSQL_SERVER")) {
          Map<String, String> hiveEnvConfig = clusterConfigurations.get("hive-env");
          if (hiveEnvConfig != null && !hiveEnvConfig.isEmpty() && hiveEnvConfig.get("hive_database") != null
            && hiveEnvConfig.get("hive_database").startsWith("Existing")) {
            throw new InvalidTopologyException("Incorrect configuration: MYSQL_SERVER component is available but hive" +
              " using existing db!");
          }
        }
        if (ClusterTopologyImpl.isNameNodeHAEnabled(clusterConfigurations) && component.equals("NAMENODE")) {
            Map<String, String> hadoopEnvConfig = clusterConfigurations.get("hadoop-env");
            if(hadoopEnvConfig != null && !hadoopEnvConfig.isEmpty() && hadoopEnvConfig.containsKey("dfs_ha_initial_namenode_active") && hadoopEnvConfig.containsKey("dfs_ha_initial_namenode_standby")) {
              ArrayList<HostGroup> hostGroupsForComponent = new ArrayList<>(blueprint.getHostGroupsForComponent(component));
              Set<String> givenHostGroups = new HashSet<>();
              givenHostGroups.add(hadoopEnvConfig.get("dfs_ha_initial_namenode_active"));
              givenHostGroups.add(hadoopEnvConfig.get("dfs_ha_initial_namenode_standby"));
              if(givenHostGroups.size() != hostGroupsForComponent.size()) {
                 throw new IllegalArgumentException("NAMENODE HA host groups mapped incorrectly for properties 'dfs_ha_initial_namenode_active' and 'dfs_ha_initial_namenode_standby'. Expected Host groups are :" + hostGroupsForComponent);
              }
              if(HostGroup.HOSTGROUP_REGEX.matcher(hadoopEnvConfig.get("dfs_ha_initial_namenode_active")).matches() && HostGroup.HOSTGROUP_REGEX.matcher(hadoopEnvConfig.get("dfs_ha_initial_namenode_standby")).matches()){
                for (HostGroup hostGroupForComponent : hostGroupsForComponent) {
                   Iterator<String> itr = givenHostGroups.iterator();
                   while(itr.hasNext()){
                      if(itr.next().contains(hostGroupForComponent.getName())){
                         itr.remove();
                      }
                   }
                 }
                 if(!givenHostGroups.isEmpty()){
                    throw new IllegalArgumentException("NAMENODE HA host groups mapped incorrectly for properties 'dfs_ha_initial_namenode_active' and 'dfs_ha_initial_namenode_standby'. Expected Host groups are :" + hostGroupsForComponent);
                 }
                }
              }
          }

        if (component.equals("HIVE_METASTORE")) {
          Map<String, String> hiveEnvConfig = clusterConfigurations.get("hive-env");
          if (hiveEnvConfig != null && !hiveEnvConfig.isEmpty() && hiveEnvConfig.get("hive_database") != null
            && hiveEnvConfig.get("hive_database").equals("Existing SQL Anywhere Database")
            && VersionUtils.compareVersions(stack.getVersion(), "2.3.0.0") < 0
            && stack.getName().equalsIgnoreCase("HDP")) {
            throw new InvalidTopologyException("Incorrect configuration: SQL Anywhere db is available only for stack HDP-2.3+ " +
              "and repo version 2.3.2+!");
          }
        }

        if (component.equals("OOZIE_SERVER")) {
          Map<String, String> oozieEnvConfig = clusterConfigurations.get("oozie-env");
          if (oozieEnvConfig != null && !oozieEnvConfig.isEmpty() && oozieEnvConfig.get("oozie_database") != null
            && oozieEnvConfig.get("oozie_database").equals("Existing SQL Anywhere Database")
            && VersionUtils.compareVersions(stack.getVersion(), "2.3.0.0") < 0
            && stack.getName().equalsIgnoreCase("HDP")) {
            throw new InvalidTopologyException("Incorrect configuration: SQL Anywhere db is available only for stack HDP-2.3+ " +
              "and repo version 2.3.2+!");
          }
        }
      }
    }
  }

  /**
   * Verify that a component is included in all host groups.
   * For components that are auto-install enabled, will add component to topology if needed.
   *
   * @param component   component to validate
   * @param autoDeploy  auto-deploy information for component
   *
   * @return collection of missing component information
   */
  private Collection<String> verifyComponentInAllHostGroups(String component, AutoDeployInfo autoDeploy) {

    Collection<String> cardinalityFailures = new HashSet<>();
    int actualCount = blueprint.getHostGroupsForComponent(component).size();
    Map<String, HostGroup> hostGroups = blueprint.getHostGroups();
    if (actualCount != hostGroups.size()) {
      if (autoDeploy != null && autoDeploy.isEnabled()) {
        for (HostGroup group : hostGroups.values()) {
          group.addComponent(component);
        }
      } else {
        cardinalityFailures.add(component + "(actual=" + actualCount + ", required=ALL)");
      }
    }
    return cardinalityFailures;
  }

  private Map<String, Collection<DependencyInfo>> validateHostGroup(HostGroup group, String dependencyValidationType) {
    LOGGER.info("Validating hostgroup: {}", group.getName());
    Map<String, Collection<DependencyInfo>> dependenciesIssues = new HashMap<>();

    for (String component : new HashSet<>(group.getComponentNames())) {
      LOGGER.debug("Processing component: {}", component);

      for (DependencyInfo dependency : stack.getDependenciesForComponent(component)) {
        LOGGER.debug("Processing dependency [{}] for component [{}]", dependency.getName(), component);

        String conditionalService = stack.getConditionalServiceForDependency(dependency);
        if (conditionalService != null && !blueprint.getServices().contains(conditionalService)) {
          LOGGER.debug("Conditional service  [{}] is missing from the blueprint, skipping dependency [{}]",
              conditionalService, dependency.getName());
          continue;
        }

        // dependent components from the stack definitions are only added if related services are explicitly added to the blueprint!
        ComponentInfo dependencyComponent = stack.getComponentInfo(dependency.getComponentName());
        if (dependencyComponent == null) {
          LOGGER.debug("The component [{}] is not associated with any known services, skipping dependency", dependency.getComponentName());
          continue;
        }

        boolean isClientDependency = dependencyComponent.isClient();
        if (isClientDependency && !blueprint.getServices().contains(dependency.getServiceName())) {
          LOGGER.debug("The service [{}] for component [{}] is missing from the blueprint [{}], skipping dependency",
              dependency.getServiceName(), dependency.getComponentName(), blueprint.getName());
          continue;
        }

        String         dependencyScope = dependency.getScope();
        String         dependencyType  = dependency.getType();
        String         componentName   = dependency.getComponentName();
        AutoDeployInfo autoDeployInfo  = dependency.getAutoDeploy();
        boolean        resolved        = false;

        if (dependencyValidationType != null && !dependencyValidationType.equals(dependencyType)) {
          continue;
        }

        //check if conditions are met, if any
        if(dependency.hasDependencyConditions()) {
          boolean conditionsSatisfied = true;
          for (DependencyConditionInfo dependencyCondition : dependency.getDependencyConditions()) {
            if (!dependencyCondition.isResolved(blueprint.getConfiguration().getFullProperties())) {
              conditionsSatisfied = false;
              break;
            }
          }
          if(!conditionsSatisfied){
            continue;
          }
        }
        if (dependencyScope.equals("cluster")) {
          Collection<String> missingDependencyInfo = verifyComponentCardinalityCount(
              componentName, new Cardinality("1+"), autoDeployInfo);

          resolved = missingDependencyInfo.isEmpty();
          if (dependencyType.equals("exclusive")) {
            resolved = !resolved;
          }
        } else if (dependencyScope.equals("host")) {
          if (dependencyType.equals("exclusive")) {
            if (!group.getComponentNames().contains(componentName)) {
              resolved = true;
            }
          } else {
            if (group.getComponentNames().contains(componentName) || (autoDeployInfo != null && autoDeployInfo.isEnabled())) {
              resolved = true;
              group.addComponent(componentName);
            }
          }
        }

        if (! resolved) {
          Collection<DependencyInfo> compDependenciesIssues = dependenciesIssues.get(component);
          if (compDependenciesIssues == null) {
            compDependenciesIssues = new HashSet<>();
            dependenciesIssues.put(component, compDependenciesIssues);
          }
          compDependenciesIssues.add(dependency);
        }
      }
    }
    return dependenciesIssues;
  }

  /**
   * Verify that a component meets cardinality requirements.  For components that are
   * auto-install enabled, will add component to topology if needed.
   *
   * @param component    component to validate
   * @param cardinality  required cardinality
   * @param autoDeploy   auto-deploy information for component
   *
   * @return collection of missing component information
   */
  public Collection<String> verifyComponentCardinalityCount(String component,
                                                            Cardinality cardinality,
                                                            AutoDeployInfo autoDeploy) {

    Map<String, Map<String, String>> configProperties = blueprint.getConfiguration().getProperties();
    Collection<String> cardinalityFailures = new HashSet<>();
    //todo: don't hard code this HA logic here
    if (ClusterTopologyImpl.isNameNodeHAEnabled(configProperties) &&
        (component.equals("SECONDARY_NAMENODE"))) {
      // override the cardinality for this component in an HA deployment,
      // since the SECONDARY_NAMENODE should not be started in this scenario
      cardinality = new Cardinality("0");
    }

    int actualCount = blueprint.getHostGroupsForComponent(component).size();
    if (! cardinality.isValidCount(actualCount)) {
      boolean validated = ! isDependencyManaged(stack, component, configProperties);
      if (! validated && autoDeploy != null && autoDeploy.isEnabled() && cardinality.supportsAutoDeploy()) {
        String coLocateName = autoDeploy.getCoLocate();
        if (coLocateName != null && ! coLocateName.isEmpty()) {
          Collection<HostGroup> coLocateHostGroups = blueprint.getHostGroupsForComponent(coLocateName.split("/")[1]);
          if (! coLocateHostGroups.isEmpty()) {
            validated = true;
            HostGroup group = coLocateHostGroups.iterator().next();
            group.addComponent(component);

          }
        }
      }
      if (! validated) {
        cardinalityFailures.add(component + "(actual=" + actualCount + ", required=" +
            cardinality.getValue() + ")");
      }
    }
    return cardinalityFailures;
  }

  /**
   * Determine if a component is managed, meaning that it is running inside of the cluster
   * topology.  Generally, non-managed dependencies will be database components.
   *
   * @param stack          stack instance
   * @param component      component to determine if it is managed
   * @param clusterConfig  cluster configuration
   *
   * @return true if the specified component managed by the cluster; false otherwise
   */
  protected boolean isDependencyManaged(Stack stack, String component, Map<String, Map<String, String>> clusterConfig) {
    boolean isManaged = true;
    String externalComponentConfig = stack.getExternalComponentConfig(component);
    if (externalComponentConfig != null) {
      String[] toks = externalComponentConfig.split("/");
      String externalComponentConfigType = toks[0];
      String externalComponentConfigProp = toks[1];
      Map<String, String> properties = clusterConfig.get(externalComponentConfigType);
      if (properties != null && properties.containsKey(externalComponentConfigProp)) {
        if (properties.get(externalComponentConfigProp).startsWith("Existing")) {
          isManaged = false;
        }
      }
    }
    return isManaged;
  }

  /**
   * Generate an exception for topology validation failure.
   *
   * @param dependenciesIssues  dependency issues information,
   *                            like component needs to be co-hosted,
   *                            or components can't be installed on the same host
   * @param cardinalityFailures  missing service component information
   *
   * @throws IllegalArgumentException  Always thrown and contains information regarding the topology validation failure
   *                                   in the msg
   */
  private void generateInvalidTopologyException(Map<String, Map<String, Collection<DependencyInfo>>> dependenciesIssues,
                                                Collection<String> cardinalityFailures) throws InvalidTopologyException {

    //todo: encapsulate some of this in exception?
    String msg = "Cluster Topology validation failed.";
    if (! cardinalityFailures.isEmpty()) {
      msg += "  Invalid service component count: " + cardinalityFailures;
    }
    if (! dependenciesIssues.isEmpty()) {
      msg += " Component dependencies issues: " + dependenciesIssues;
    }
    msg += ".  To disable topology validation and create the blueprint, " +
        "add the following to the end of the url: '?validate_topology=false'";
    throw new InvalidTopologyException(msg);
  }
}
