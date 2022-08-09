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
package org.apache.ambari.server.stack.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.serveraction.upgrades.ConfigureAction;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.ConfigurationKeyValue;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.Insert;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.Replace;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.Transfer;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;
import org.apache.ambari.server.state.Cluster;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link ConfigureTask} represents a configuration change. This task
 * contains id of change. Change definitions are located in a separate file (config
 * upgrade pack). IDs of change definitions share the same namespace within all
 * stacks
 * <p/>
 *
 * <pre>
 * {@code
 * <task xsi:type="configure" id="hdp_2_3_0_0-UpdateHiveConfig"/>
 * }
 * </pre>
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="configure")
public class ConfigureTask extends ServerSideActionTask {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigureTask.class);

  /**
   * The key that represents the configuration type to change (ie hdfs-site).
   */
  public static final String PARAMETER_CONFIG_TYPE = "configure-task-config-type";

  /**
   * Setting key/value pairs can be several per task, so they're passed in as a
   * json-ified list of objects.
   */
  public static final String PARAMETER_KEY_VALUE_PAIRS = "configure-task-key-value-pairs";

  /**
   * Transfers can be several per task, so they're passed in as a json-ified
   * list of objects.
   */
  public static final String PARAMETER_TRANSFERS = "configure-task-transfers";

  /**
   * Replacements can be several per task, so they're passed in as a json-ified list of
   * objects.
   */
  public static final String PARAMETER_REPLACEMENTS = "configure-task-replacements";

  /**
   * Insertions can be several per task, so they're passed in as a json-ified
   * list of objects.
   */
  public static final String PARAMETER_INSERTIONS = "configure-task-insertions";

  /**
   * The associated service for the config task
   */
  public static final String PARAMETER_ASSOCIATED_SERVICE = "configure-task-associated-service";

  public static final String actionVerb = "Configuring";

  /**
   * Gson
   */
  private Gson m_gson = new Gson();

  /**
   * Constructor.
   */
  public ConfigureTask() {
    implClass = ConfigureAction.class.getName();
  }

  @XmlAttribute(name = "id")
  public String id;

  @XmlAttribute(name="supports-patch")
  public boolean supportsPatch = false;

  /**
   * The associated service is the service where this config task is specified
   */
  @XmlTransient
  public String associatedService;

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getType() {
    return Task.Type.CONFIGURE;
  }

  @Override
  public StageWrapper.Type getStageWrapperType() {
    return StageWrapper.Type.SERVER_SIDE_ACTION;
  }

  @Override
  public String getActionVerb() {
    return actionVerb;
  }

  /**
   * This getter is intended to be used only from tests. In production,
   * getConfigurationChanges() logic should be used instead
   * @return id of config upgrade change definition as defined in upgrade pack
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the summary of the task or {@code null}.
   *
   * @return the task summary or {@code null}.
   */
  public String getSummary(ConfigUpgradePack configUpgradePack) {
    if(StringUtils.isNotBlank(id) && null != configUpgradePack){
      ConfigUpgradeChangeDefinition definition = configUpgradePack.enumerateConfigChangesByID().get(id);
      if (null != definition && StringUtils.isNotBlank(definition.summary)) {
          return definition.summary;
      }
    }

    return super.getSummary();
  }

  /**
   * Gets a map containing the following properties pertaining to the
   * configuration value to change:
   * <ul>
   * <li>{@link #PARAMETER_CONFIG_TYPE} - the configuration type (ie hdfs-site)</li>
   * <li>{@link #PARAMETER_KEY_VALUE_PAIRS} - key/value pairs for the
   * configurations</li>
   * <li>{@link #PARAMETER_KEY_VALUE_PAIRS} - key/value pairs for the
   * configurations</li>
   * <li>{@link #PARAMETER_TRANSFERS} - COPY/MOVE/DELETE changes</li>
   * <li>{@link #PARAMETER_REPLACEMENTS} - value replacements</li>
   * </ul>
   *
   * @param cluster
   *          the cluster to use when retrieving conditional properties to test
   *          against (not {@code null}).
   * @return the a map containing the changes to make. This could potentially be
   *         an empty map if no conditions are met. Callers should decide how to
   *         handle a configuration task that is unable to set any configuration
   *         values.
   */
  public Map<String, String> getConfigurationChanges(Cluster cluster,
                                                     ConfigUpgradePack configUpgradePack) {
    Map<String, String> configParameters = new HashMap<>();

    if (id == null || id.isEmpty()) {
      LOG.warn("Config task id is not defined, skipping config change");
      return configParameters;
    }

    if (configUpgradePack == null) {
      LOG.warn("Config upgrade pack is not defined, skipping config change");
      return configParameters;
    }

    // extract config change definition, referenced by current ConfigureTask
    ConfigUpgradeChangeDefinition definition = configUpgradePack.enumerateConfigChangesByID().get(id);
    if (definition == null) {
      LOG.warn(String.format("Can not resolve config change definition by id %s, " +
              "skipping config change", id));
      return configParameters;
    }

    // this task is not a condition task, so process the other elements normally
    if (null != definition.getConfigType()) {
      configParameters.put(PARAMETER_CONFIG_TYPE, definition.getConfigType());
    }

    // for every <set key=foo value=bar/> add it to this list
    if (null != definition.getKeyValuePairs() && !definition.getKeyValuePairs().isEmpty()) {
      List<ConfigurationKeyValue> allowedSets = getValidSets(cluster, definition.getConfigType(), definition.getKeyValuePairs());
      configParameters.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS,
          m_gson.toJson(allowedSets));
    }

    // transfers
    List<Transfer> transfers = definition.getTransfers();
    if (null != transfers && !transfers.isEmpty()) {
      List<Transfer> allowedTransfers = getValidTransfers(cluster, definition.getConfigType(), definition.getTransfers());
      configParameters.put(ConfigureTask.PARAMETER_TRANSFERS, m_gson.toJson(allowedTransfers));
    }

    // replacements

    List<Replace> replacements = new ArrayList<>();
    replacements.addAll(definition.getReplacements());
    //Fetch the replacements that used regex to find a string
    replacements.addAll(definition.getRegexReplacements(cluster));

    if( !replacements.isEmpty() ){
      List<Replace> allowedReplacements = getValidReplacements(cluster, definition.getConfigType(), replacements);
      configParameters.put(ConfigureTask.PARAMETER_REPLACEMENTS, m_gson.toJson(allowedReplacements));
    }

    // inserts
    List<Insert> insertions = definition.getInsertions();
    if (!insertions.isEmpty()) {
      configParameters.put(ConfigureTask.PARAMETER_INSERTIONS, m_gson.toJson(insertions));
    }

    if (StringUtils.isNotEmpty(associatedService)) {
      configParameters.put(ConfigureTask.PARAMETER_ASSOCIATED_SERVICE, associatedService);
    }

    return configParameters;
  }

  private List<Replace> getValidReplacements(Cluster cluster, String configType, List<Replace> replacements){
    List<Replace> allowedReplacements= new ArrayList<>();

    for(Replace replacement: replacements){
      if(isValidConditionSettings(cluster, configType, replacement.key,
          replacement.ifKey, replacement.ifType, replacement.ifValue, replacement.ifKeyState)) {
        allowedReplacements.add(replacement);
      }
    }

    return allowedReplacements;
  }

  private List<ConfigurationKeyValue> getValidSets(Cluster cluster, String configType, List<ConfigurationKeyValue> sets){
    List<ConfigurationKeyValue> allowedSets = new ArrayList<>();

    for(ConfigurationKeyValue configurationKeyValue: sets){
      if(isValidConditionSettings(cluster, configType, configurationKeyValue.key,
          configurationKeyValue.ifKey, configurationKeyValue.ifType, configurationKeyValue.ifValue, configurationKeyValue.ifKeyState)) {
        allowedSets.add(configurationKeyValue);
      }
    }

    return allowedSets;
  }

  private List<Transfer> getValidTransfers(Cluster cluster, String configType, List<Transfer> transfers){
    List<Transfer> allowedTransfers = new ArrayList<>();
    for (Transfer transfer : transfers) {
      String key = "";
      if(transfer.operation == TransferOperation.DELETE) {
        key = transfer.deleteKey;
      } else {
        key = transfer.fromKey;
      }

      if(isValidConditionSettings(cluster, configType, key,
          transfer.ifKey, transfer.ifType, transfer.ifValue, transfer.ifKeyState)) {
        allowedTransfers.add(transfer);
      }
    }

    return allowedTransfers;
  }

  /**
   * Sanity check for invalid attribute settings on if-key, if-value, if-key-state, if-site
   * Regardless whether it's set, transfer, or replace, the condition attributes are the same
   * So the same logic can be used to determine if the operation is allowed or not.
   * */
  private boolean isValidConditionSettings(Cluster cluster, String configType, String targetPropertyKey,
      String ifKey, String ifType, String ifValue, PropertyKeyState ifKeyState){

    //Operation is always valid if there are no conditions specified
    boolean isValid = false;

    boolean ifKeyIsNotBlank = StringUtils.isNotBlank(ifKey);
    boolean ifTypeIsNotBlank = StringUtils.isNotBlank(ifType);
    boolean ifValueIsNotNull = (null != ifValue);
    boolean ifKeyStateIsValid = (PropertyKeyState.PRESENT == ifKeyState || PropertyKeyState.ABSENT == ifKeyState);

    if(ifKeyIsNotBlank && ifTypeIsNotBlank && (ifValueIsNotNull || ifKeyStateIsValid)) {
      // allow if the condition has ifKey, ifType and either ifValue or ifKeyState
      isValid = true;
    } else if (!ifKeyIsNotBlank && !ifTypeIsNotBlank && !ifValueIsNotNull &&  !ifKeyStateIsValid) {
      //no condition, allow
      isValid = true;
    }

    return isValid;
  }
}
