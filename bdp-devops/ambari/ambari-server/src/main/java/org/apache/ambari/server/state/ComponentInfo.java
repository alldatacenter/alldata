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

package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

@XmlAccessorType(XmlAccessType.FIELD)
public class ComponentInfo {
  private String name;
  private String displayName;
  private String category;
  private boolean deleted;
  private String cardinality;
  
  @XmlElement(name="versionAdvertised")
  private Boolean versionAdvertisedField;
  
  /**
   * Technically, no component is required to advertise a version. In practice, 
   * Components should advertise a version through a mechanism like hdp-select.
   * The version must be present the structured output.in the {"version": "#.#.#.#-###"}
   * For example, Masters will typically advertise the version upon a RESTART.
   * Whereas clients will advertise the version when INSTALLED.
   * Some components do not need to advertise a version because it is either redundant, or they don't have a mechanism
   * at the moment. For instance, ZKFC has the same version as NameNode, while AMBARI_METRICS and KERBEROS do not have a mechanism.
   *
   * This is the translation of the xml element ["true", "false", null] (note that if a value is not specified,
   * it will inherit from the parent) into a boolean after actually resolving it.
   */
  private boolean versionAdvertisedInternal = false;

  /**
   * Used to determine if decommission is allowed
   * */
  @XmlElements(@XmlElement(name = "decommissionAllowed"))
  private String decommissionAllowed;

  @XmlElement(name="unlimitedKeyJCERequired")
  private UnlimitedKeyJCERequirement unlimitedKeyJCERequired;

  /**
   * Used to determine if rolling restart is supported
   * */
  @XmlElements(@XmlElement(name = "rollingRestartSupported"))
  private boolean rollingRestartSupported;

  /**
  * Added at schema ver 2
  */
  private CommandScriptDefinition commandScript;

  /**
   * List of the logs that the component writes
   */
  @XmlElementWrapper(name = "logs")
  @XmlElements(@XmlElement(name = "log"))
  private List<LogDefinition> logs;

  /**
   * List of clients which configs are updated with master component.
   * If clientsToUpdateConfigs is not specified all clients are considered to be updated.
   * If clientsToUpdateConfigs is empty no clients are considered to be updated
   */
  @XmlElementWrapper(name = "clientsToUpdateConfigs")
  @XmlElements(@XmlElement(name = "client"))
  private List<String> clientsToUpdateConfigs;

  /**
   * Client configuration files
   * List of files to download in client configuration tar
   */
  @XmlElementWrapper(name = "configFiles")
  @XmlElements(@XmlElement(name = "configFile"))
  private List<ClientConfigFileDefinition> clientConfigFiles;

  /**
   * Added at schema ver 2
   */
  @XmlElementWrapper(name="customCommands")
  @XmlElements(@XmlElement(name="customCommand"))
  private List<CustomCommandDefinition> customCommands;

  /**
   * bulk commands shown in the Hosts actions
   * */
  @XmlElement(name="bulkCommands")
  private BulkCommandDefinition bulkCommandDefinition;

  /**
   * Component dependencies to other components.
   */
  @XmlElementWrapper(name="dependencies")
  @XmlElements(@XmlElement(name="dependency"))
  private List<DependencyInfo> dependencies = new ArrayList<>();

  @XmlElementWrapper(name="configuration-dependencies")
  @XmlElements(@XmlElement(name="config-type"))
  private List<String> configDependencies;

  /**
   * Auto-deployment information.
   * If auto-deployment is enabled and the component doesn't meet the cardinality requirement,
   * the component is auto-deployed to the cluster topology.
   */
  @XmlElement(name="auto-deploy")
  private AutoDeployInfo autoDeploy;

  @XmlElements(@XmlElement(name = "recovery_enabled"))
  private boolean recoveryEnabled = false;

  /**
   * Used to determine if reassign is allowed
   * */
  @XmlElements(@XmlElement(name = "reassignAllowed"))
  private String reassignAllowed;

  private String timelineAppid;

  @XmlElement(name="customFolder")
  private String customFolder;

  /**
   * Optional component type like HCFS_CLIENT.
   * HCFS_CLIENT indicates compatibility with HDFS_CLIENT
   */
  private String componentType;

  public ComponentInfo() {
  }

  /**
   * Copy constructor.
   */
  public ComponentInfo(ComponentInfo prototype) {
    name = prototype.name;
    category = prototype.category;
    deleted = prototype.deleted;
    cardinality = prototype.cardinality;
    versionAdvertisedField = prototype.versionAdvertisedField;
    versionAdvertisedInternal = prototype.versionAdvertisedInternal;
    decommissionAllowed = prototype.decommissionAllowed;
    unlimitedKeyJCERequired = prototype.unlimitedKeyJCERequired;
    clientsToUpdateConfigs = prototype.clientsToUpdateConfigs;
    commandScript = prototype.commandScript;
    logs = prototype.logs;
    customCommands = prototype.customCommands;
    bulkCommandDefinition = prototype.bulkCommandDefinition;
    dependencies = prototype.dependencies;
    autoDeploy = prototype.autoDeploy;
    configDependencies = prototype.configDependencies;
    clientConfigFiles = prototype.clientConfigFiles;
    timelineAppid = prototype.timelineAppid;
    reassignAllowed = prototype.reassignAllowed;
    customFolder = prototype.customFolder;
    rollingRestartSupported = prototype.rollingRestartSupported;
    componentType = prototype.componentType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public boolean isClient() {
    return "CLIENT".equals(category);
  }

  public boolean isMaster() {
    return "MASTER".equals(category);
  }

  public boolean isSlave() {
    return "SLAVE".equals(category);
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public CommandScriptDefinition getCommandScript() {
    return commandScript;
  }

  public void setCommandScript(CommandScriptDefinition commandScript) {
    this.commandScript = commandScript;
  }

  public List<LogDefinition> getLogs() {
    if (logs == null) {
      logs = new ArrayList<>();
    }
    
    return logs;
  }

  public LogDefinition getPrimaryLog() {
    for (LogDefinition log : getLogs()) {
      if (log.isPrimary()) {
        return log;
      }
    }
    
    return null;
  }

  public void setLogs(List<LogDefinition> logs) {
    this.logs = logs;
  }

  public List<ClientConfigFileDefinition> getClientConfigFiles() {
    return clientConfigFiles;
  }

  public void setClientConfigFiles(List<ClientConfigFileDefinition> clientConfigFiles) {
    this.clientConfigFiles = clientConfigFiles;
  }

  public List<CustomCommandDefinition> getCustomCommands() {
    if (customCommands == null) {
      customCommands = new ArrayList<>();
    }
    return customCommands;
  }

  public void setCustomCommands(List<CustomCommandDefinition> customCommands) {
    this.customCommands = customCommands;
  }

  public boolean isCustomCommand(String commandName) {
    if (customCommands != null && commandName != null) {
      for (CustomCommandDefinition cc: customCommands) {
        if (commandName.equals(cc.getName())){
          return true;
        }
      }
    }
    return false;
  }
  public CustomCommandDefinition getCustomCommandByName(String commandName){
    for(CustomCommandDefinition ccd : getCustomCommands()){
      if (ccd.getName().equals(commandName)){
        return ccd;
      }
    }
    return null;
  }

  public BulkCommandDefinition getBulkCommandDefinition() {
    return bulkCommandDefinition;
  }

  public void setBulkCommands(BulkCommandDefinition bulkCommandDefinition) {
    this.bulkCommandDefinition = bulkCommandDefinition;
  }

  public List<DependencyInfo> getDependencies() {
    return dependencies;
  }

  public List<String> getConfigDependencies() {
    return configDependencies;
  }
  
  public void setConfigDependencies(List<String> configDependencies) {
    this.configDependencies = configDependencies;
  }
  public boolean hasConfigType(String type) {
    return configDependencies != null && configDependencies.contains(type);
  }

  public void setDependencies(List<DependencyInfo> dependencies) {
    this.dependencies = dependencies;
  }

  public void setAutoDeploy(AutoDeployInfo autoDeploy) {
    this.autoDeploy = autoDeploy;
  }

  public AutoDeployInfo getAutoDeploy() {
    return autoDeploy;
  }

  public void setCardinality(String cardinality) {
    this.cardinality = cardinality;
  }
  
  public String getCardinality() {
    return cardinality;
  }

  /**
   * WARNING: only call this method from unit tests to set the Boolean that would have been read from the xml file.
   * If you call this function, you must still call {@see org.apache.ambari.server.stack.ComponentModule#resolve()}.
   * @param versionAdvertisedField
   */
  public void setVersionAdvertisedField(Boolean versionAdvertisedField) {
    this.versionAdvertisedField = versionAdvertisedField;
  }

  /**
   * WARNING: only call this from ComponentModule to resolve the boolean (true|false).
   * In all other classes, use {@seealso isVersionAdvertised}
   * @return The Boolean for versionAdvertised from the xml file in order to resolve it into a boolean.
   */
  public Boolean getVersionAdvertisedField() {
    return this.versionAdvertisedField;
  }

  /**
   * WARNING: only call this from ComponentModule to resolve the boolean (true|false).
   * @param versionAdvertised Final resolution of whether version is advertised or not.
   */
  public void setVersionAdvertised(boolean versionAdvertised) {
    this.versionAdvertisedInternal = versionAdvertised;
  }

  /**
   * Determine if this component advertises a version. This Boolean has already resolved to true|false depending
   * on explicitly overriding the value or inheriting from an ancestor.
   * @return boolean of whether this component advertises a version.
   */
  public boolean isVersionAdvertised() {
    if (null != versionAdvertisedField) {
      return versionAdvertisedField.booleanValue();
    }
    // If set to null and has a parent, then the value would have already been resolved and set.
    // Otherwise, return the default value (false).
    return this.versionAdvertisedInternal;

  }

  public String getDecommissionAllowed() {
    return decommissionAllowed;
  }

  public void setDecommissionAllowed(String decommissionAllowed) {
    this.decommissionAllowed = decommissionAllowed;
  }

  public boolean getRollingRestartSupported() {
    return rollingRestartSupported;
  }

  public void setRollingRestartSupported(boolean rollingRestartSupported) {
    this.rollingRestartSupported = rollingRestartSupported;
  }

  public UnlimitedKeyJCERequirement getUnlimitedKeyJCERequired() {
    return unlimitedKeyJCERequired;
  }

  public void setUnlimitedKeyJCERequired(UnlimitedKeyJCERequirement unlimitedKeyJCERequired) {
    this.unlimitedKeyJCERequired = unlimitedKeyJCERequired;
  }

  public void setRecoveryEnabled(boolean recoveryEnabled) {
    this.recoveryEnabled = recoveryEnabled;
  }

  public boolean isRecoveryEnabled() {
    return recoveryEnabled;
  }

  public List<String> getClientsToUpdateConfigs() {
    return clientsToUpdateConfigs;
  }

  public void setClientsToUpdateConfigs(List<String> clientsToUpdateConfigs) {
    this.clientsToUpdateConfigs = clientsToUpdateConfigs;
  }

  public String getTimelineAppid() {
    return timelineAppid;
  }

  public void setTimelineAppid(String timelineAppid) {
    this.timelineAppid = timelineAppid;
  }

  public String getReassignAllowed() {
    return reassignAllowed;
  }

  public void setReassignAllowed(String reassignAllowed) {
    this.reassignAllowed = reassignAllowed;
  }

  public String getCustomFolder() {
    return customFolder;
  }

  public void setCustomFolder(String customFolder) {
    this.customFolder = customFolder;
  }

  public String getComponentType() {
    return componentType;
  }

  public void setComponentType(String componentType) {
    this.componentType = componentType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ComponentInfo that = (ComponentInfo) o;

    if (deleted != that.deleted) return false;
    if (autoDeploy != null ? !autoDeploy.equals(that.autoDeploy) : that.autoDeploy != null) return false;
    if (cardinality != null ? !cardinality.equals(that.cardinality) : that.cardinality != null) return false;
    if (versionAdvertisedField != null ? !versionAdvertisedField.equals(that.versionAdvertisedField) : that.versionAdvertisedField != null) return false;
    if (versionAdvertisedInternal != that.versionAdvertisedInternal) return false;
    if (decommissionAllowed != null ? !decommissionAllowed.equals(that.decommissionAllowed) : that.decommissionAllowed != null) return false;
    if (unlimitedKeyJCERequired != null ? !unlimitedKeyJCERequired.equals(that.unlimitedKeyJCERequired) : that.unlimitedKeyJCERequired != null) return false;
    if (reassignAllowed != null ? !reassignAllowed.equals(that.reassignAllowed) : that.reassignAllowed != null) return false;
    if (category != null ? !category.equals(that.category) : that.category != null) return false;
    if (clientConfigFiles != null ? !clientConfigFiles.equals(that.clientConfigFiles) : that.clientConfigFiles != null)
      return false;
    if (commandScript != null ? !commandScript.equals(that.commandScript) : that.commandScript != null) return false;
    if (logs != null ? !logs.equals(that.logs) : that.logs != null) return false;
    if (configDependencies != null ? !configDependencies.equals(that.configDependencies) : that.configDependencies != null)
      return false;
    if (customCommands != null ? !customCommands.equals(that.customCommands) : that.customCommands != null)
      return false;
    if (bulkCommandDefinition != null ? !bulkCommandDefinition.equals(that.bulkCommandDefinition) : that.bulkCommandDefinition != null)
      return false;
    if (dependencies != null ? !dependencies.equals(that.dependencies) : that.dependencies != null) return false;
    if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (clientConfigFiles != null ? !clientConfigFiles.equals(that.clientConfigFiles) :
        that.clientConfigFiles != null) return false;
    if (customFolder != null ? !customFolder.equals(that.customFolder) : that.customFolder != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (category != null ? category.hashCode() : 0);
    result = 31 * result + (deleted ? 1 : 0);
    result = 31 * result + (cardinality != null ? cardinality.hashCode() : 0);
    result = 31 * result + (decommissionAllowed != null ? decommissionAllowed.hashCode() : 0);
    result = 31 * result + (unlimitedKeyJCERequired != null ? unlimitedKeyJCERequired.hashCode() : 0);
    result = 31 * result + (reassignAllowed != null ? reassignAllowed.hashCode() : 0);
    result = 31 * result + (commandScript != null ? commandScript.hashCode() : 0);
    result = 31 * result + (logs != null ? logs.hashCode() : 0);
    result = 31 * result + (clientConfigFiles != null ? clientConfigFiles.hashCode() : 0);
    result = 31 * result + (customCommands != null ? customCommands.hashCode() : 0);
    result = 31 * result + (bulkCommandDefinition != null ? bulkCommandDefinition.hashCode(): 0);
    result = 31 * result + (dependencies != null ? dependencies.hashCode() : 0);
    result = 31 * result + (autoDeploy != null ? autoDeploy.hashCode() : 0);
    result = 31 * result + (configDependencies != null ? configDependencies.hashCode() : 0);
    result = 31 * result + (clientConfigFiles != null ? clientConfigFiles.hashCode() : 0);
    // NULL = 0, TRUE = 2, FALSE = 1
    result = 31 * result + (versionAdvertisedField != null ? (versionAdvertisedField.booleanValue() ? 2 : 1) : 0);
    result = 31 * result + (customFolder != null ? customFolder.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}
