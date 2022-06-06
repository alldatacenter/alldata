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

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.collections.PredicateUtils;
import org.apache.ambari.server.stack.StackDirectory;
import org.apache.ambari.server.stack.Validable;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.apache.ambari.server.state.stack.StackRoleCommandOrder;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonFilter;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

@XmlAccessorType(XmlAccessType.FIELD)
@JsonFilter("propertiesfilter")
public class ServiceInfo implements Validable {

  public static final AbstractMap.SimpleEntry<String, String> DEFAULT_SERVICE_INSTALLABLE_PROPERTY = new AbstractMap.SimpleEntry<>("installable", "true");
  public static final AbstractMap.SimpleEntry<String, String> DEFAULT_SERVICE_MANAGED_PROPERTY = new AbstractMap.SimpleEntry<>("managed", "true");
  public static final AbstractMap.SimpleEntry<String, String> DEFAULT_SERVICE_MONITORED_PROPERTY = new AbstractMap.SimpleEntry<>("monitored", "true");
  public static final String HADOOP_COMPATIBLE_FS = "HCFS";
  /**
   * Format version. Added at schema ver 2
   */
  @XmlTransient
  private String schemaVersion;

  private String name;
  private String displayName;
  private String version;
  private String comment;
  private String serviceType;
  private Selection selection;
  private String maintainer;

  /**
   * Default to Python if not specified.
   */

  @XmlEnum
  public enum ServiceAdvisorType {
    @XmlEnumValue("PYTHON")
    PYTHON,
    @XmlEnumValue("JAVA")
    JAVA
  }

  @XmlElement(name = "service_advisor_type")
  private ServiceAdvisorType serviceAdvisorType = ServiceAdvisorType.PYTHON;

  @XmlTransient
  private List<PropertyInfo> properties;

  @XmlElementWrapper(name = "components")
  @XmlElements(@XmlElement(name = "component"))
  private List<ComponentInfo> components;

  @XmlElement(name = "deleted")
  private boolean isDeleted = false;

  @XmlElement(name = "supportDeleteViaUI")
  private Boolean supportDeleteViaUIField;

  private boolean supportDeleteViaUIInternal = true;

  @JsonIgnore
  @XmlTransient
  private volatile Map<String, Set<String>> configLayout = null;

  @XmlElementWrapper(name = "configuration-dependencies")
  @XmlElement(name = "config-type")
  private List<String> configDependencies;

  @XmlElementWrapper(name = "excluded-config-types")
  @XmlElement(name = "config-type")
  private Set<String> excludedConfigTypes = new HashSet<>();

  @XmlTransient
  private Map<String, Map<String, Map<String, String>>> configTypes;

  @JsonIgnore
  private Boolean monitoringService;

  @JsonIgnore
  @XmlElement(name = "restartRequiredAfterChange")
  private Boolean restartRequiredAfterChange;

  @JsonIgnore
  @XmlElement(name = "restartRequiredAfterRackChange")
  private Boolean restartRequiredAfterRackChange;

  @XmlElement(name = "extends")
  private String parent;

  @XmlElement(name = "widgetsFileName")
  private String widgetsFileName = AmbariMetaInfo.WIDGETS_DESCRIPTOR_FILE_NAME;

  @XmlElement(name = "metricsFileName")
  private String metricsFileName = StackDirectory.SERVICE_METRIC_FILE_NAME;

  @XmlTransient
  private volatile Map<String, PropertyInfo> requiredProperties;

  /**
   * Credential store information
   */
  @XmlElements(@XmlElement(name = "credential-store"))
  private CredentialStoreInfo credentialStoreInfo;

  /**
   * The configuration that can be used to determine if Kerberos has been enabled for this service.
   * <p>
   * It is expected that this value is in the form of a valid JSON predicate ({@link PredicateUtils#fromJSON(String)}
   */
  @XmlElement(name = "kerberosEnabledTest")
  private String kerberosEnabledTest = null;

  /**
   * Single Sign-on support information
   */
  @XmlElements(@XmlElement(name = "sso"))
  private SingleSignOnInfo singleSignOnInfo;

  /**
   * LDAP support information
   */
  @XmlElements(@XmlElement(name = "ldap"))
  private ServiceLdapInfo ldapInfo;

  public Boolean isRestartRequiredAfterChange() {
    return restartRequiredAfterChange;
  }

  public void setRestartRequiredAfterChange(Boolean restartRequiredAfterChange) {
    this.restartRequiredAfterChange = restartRequiredAfterChange;
  }

  @XmlTransient
  private File metricsFile = null;

  @XmlTransient
  private Map<String, Map<String, List<MetricDefinition>>> metrics = null;

  @XmlTransient
  private File advisorFile = null;

  @XmlTransient
  private String advisorName = null;

  @XmlTransient
  private File alertsFile = null;

  @XmlTransient
  private File kerberosDescriptorFile = null;

  @XmlTransient
  private File widgetsDescriptorFile = null;

  private StackRoleCommandOrder roleCommandOrder;

  @XmlTransient
  private boolean valid = true;

  @XmlElementWrapper(name = "properties")
  @XmlElement(name = "property")
  private List<ServicePropertyInfo> servicePropertyList = Lists.newArrayList();

  @XmlTransient
  private Map<String, String> servicePropertyMap = ImmutableMap.copyOf(ensureMandatoryServiceProperties(Maps.newHashMap()));

  /**
   * @return valid xml flag
   */
  @Override
  public boolean isValid() {
    return valid;
  }

  /**
   * @param valid set validity flag
   */
  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  @XmlTransient
  private Set<String> errorSet = new HashSet<>();

  @Override
  public void addError(String error) {
    errorSet.add(error);
  }

  @Override
  public Collection<String> getErrors() {
    return errorSet;
  }

  @Override
  public void addErrors(Collection<String> errors) {
    errorSet.addAll(errors);
  }

  /**
   * Internal list of os-specific details (loaded from xml). Added at schema ver 2
   */
  @JsonIgnore
  @XmlElementWrapper(name = "osSpecifics")
  @XmlElements(@XmlElement(name = "osSpecific"))
  private List<ServiceOsSpecific> serviceOsSpecifics;

  @JsonIgnore
  @XmlElement(name = "configuration-dir")
  private String configDir = StackDirectory.SERVICE_CONFIG_FOLDER_NAME;

  @JsonIgnore
  @XmlElement(name = "themes-dir")
  private String themesDir = StackDirectory.SERVICE_THEMES_FOLDER_NAME;

  @JsonIgnore
  @XmlElementWrapper(name = "themes")
  @XmlElements(@XmlElement(name = "theme"))
  private List<ThemeInfo> themes;

  @XmlTransient
  private volatile Map<String, ThemeInfo> themesMap;

  @JsonIgnore
  @XmlElement(name = "quickLinksConfigurations-dir")
  private String quickLinksConfigurationsDir = StackDirectory.SERVICE_QUICKLINKS_CONFIGURATIONS_FOLDER_NAME;

  @JsonIgnore
  @XmlElementWrapper(name = "quickLinksConfigurations")
  @XmlElements(@XmlElement(name = "quickLinksConfiguration"))
  private List<QuickLinksConfigurationInfo> quickLinksConfigurations;

  @XmlTransient
  private volatile Map<String, QuickLinksConfigurationInfo> quickLinksConfigurationsMap;

  /**
   * Map of of os-specific details that is exposed (and initialised from list)
   * at getter.
   * Added at schema ver 2
   */
  private volatile Map<String, ServiceOsSpecific> serviceOsSpecificsMap;

  /**
   * This is used to add service check actions for services.
   * Added at schema ver 2
   */
  private CommandScriptDefinition commandScript;

  /**
   * Added at schema ver 2
   */
  @XmlElementWrapper(name = "customCommands")
  @XmlElements(@XmlElement(name = "customCommand"))
  private List<CustomCommandDefinition> customCommands;

  @XmlElementWrapper(name = "requiredServices")
  @XmlElement(name = "service")
  private List<String> requiredServices = new ArrayList<>();

  /**
   * Meaning: stores subpath from stack root to exact directory, that contains
   * service scripts and templates. Since schema ver 2,
   * we may have multiple service metadata inside folder.
   * Added at schema ver 2
   */
  @XmlTransient
  private String servicePackageFolder;

  /**
   * Stores the path to the upgrades folder which contains the upgrade xmls for the given service.
   */
  @XmlTransient
  private File serviceUpgradesFolder;

  /**
   * Stores the path to the checks folder which contains prereq check jars for the given service.
   */
  @XmlTransient
  private File checksFolder;

  /**
   * Stores the path to the server actions folder which contains server actions jars for the given service.
   */
  @XmlTransient
  private File serverActionsFolder;

  /**
   * Used to determine if rolling restart is supported
   * */
  @XmlElement(name = "rollingRestartSupported")
  private boolean rollingRestartSupported;

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean deleted) {
    isDeleted = deleted;
  }

  public Boolean getSupportDeleteViaUIField() {
    return supportDeleteViaUIField;
  }

  public void setSupportDeleteViaUIField(Boolean supportDeleteViaUIField) {
    this.supportDeleteViaUIField = supportDeleteViaUIField;
  }

  public boolean isSupportDeleteViaUI() {
    if (null != supportDeleteViaUIField) {
      return supportDeleteViaUIField.booleanValue();
    }
    // If set to null and has a parent, then the value would have already been resolved and set.
    // Otherwise, return the default value (true).
    return supportDeleteViaUIInternal;
  }

  public void setSupportDeleteViaUI(boolean supportDeleteViaUI) {
    supportDeleteViaUIInternal = supportDeleteViaUI;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setServiceAdvisorType(ServiceAdvisorType type) {
    serviceAdvisorType = type;
  }

  public ServiceAdvisorType getServiceAdvisorType() {
    return serviceAdvisorType;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Selection getSelection() {
    if (selection == null) {
      return Selection.DEFAULT;
    }
    return selection;
  }

  public void setSelection(Selection selection) {
    this.selection = selection;
  }

  /**
   * Check if selection was presented in xml. We need this for proper stack inheritance, because {@link ServiceInfo#getSelection}
   * by default returns {@link Selection#DEFAULT}, even if no value found in metainfo.xml.
   *
   * @return true, if selection not defined in metainfo.xml
   */
  public boolean isSelectionEmpty() {
    return selection == null;
  }

  public String getMaintainer() {
    return maintainer;
  }

  public void setMaintainer(String maintainer) {
    this.maintainer = maintainer;
  }

  public boolean isMaintainerEmpty() {
    return maintainer == null;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public List<String> getRequiredServices() {
    return requiredServices;
  }

  public String getWidgetsFileName() {
    return widgetsFileName;
  }

  public void setWidgetsFileName(String widgetsFileName) {
    this.widgetsFileName = widgetsFileName;
  }

  public String getMetricsFileName() {
    return metricsFileName;
  }

  public void setMetricsFileName(String metricsFileName) {
    this.metricsFileName = metricsFileName;
  }

  public void setRequiredServices(List<String> requiredServices) {
    this.requiredServices = requiredServices;
  }

  public List<PropertyInfo> getProperties() {
    if (properties == null) {
      properties = new ArrayList<>();
    }
    return properties;
  }

  public void setProperties(List<PropertyInfo> properties) {
    this.properties = properties;
  }

  public List<ComponentInfo> getComponents() {
    if (components == null) {
      components = new ArrayList<>();
    }
    return components;
  }

  /**
   * Finds ComponentInfo by component name
   *
   * @param componentName name of the component
   * @return ComponentInfo componentName or null
   */
  public ComponentInfo getComponentByName(String componentName) {
    for (ComponentInfo componentInfo : getComponents()) {
      if (componentInfo.getName().equals(componentName)) {
        return componentInfo;
      }
    }
    return null;
  }

  public boolean isClientOnlyService() {
    if (components == null || components.isEmpty()) {
      return false;
    }
    for (ComponentInfo compInfo : components) {
      if (!compInfo.isClient()) {
        return false;
      }
    }
    return true;
  }

  public ComponentInfo getClientComponent() {
    ComponentInfo client = null;

    if (components != null) {
      for (ComponentInfo compInfo : components) {
        if (compInfo.isClient()) {
          client = compInfo;
          break;
        }
      }
    }
    return client;
  }

  public File getAdvisorFile() {
    return advisorFile;
  }

  public void setAdvisorFile(File advisorFile) {
    this.advisorFile = advisorFile;
  }

  public String getAdvisorName() {
    return advisorName;
  }

  public void setAdvisorName(String advisorName) {
    this.advisorName = advisorName;
  }

  /**
   * Indicates if this service supports credential store.
   * False, it was not specified.
   *
   * @return true or false
   */
  public boolean isCredentialStoreSupported() {
    if (credentialStoreInfo != null) {
      if (credentialStoreInfo.isSupported() != null) {
        return credentialStoreInfo.isSupported();
      }
    }

    return false;
  }

  /**
   * Set a value indicating if this service supports credential store.
   *
   * @param credentialStoreSupported
   */
  public void setCredentialStoreSupported(boolean credentialStoreSupported) {
    if (credentialStoreInfo == null) {
      credentialStoreInfo = new CredentialStoreInfo();
    }
    credentialStoreInfo.setSupported(credentialStoreSupported);
  }

  /**
   * Indicates if this service is requires credential store.
   * False if it was not specified.
   *
   * @return true or false
   */
  public boolean isCredentialStoreRequired() {
    if (credentialStoreInfo != null) {
      if (credentialStoreInfo.isRequired() != null) {
        return credentialStoreInfo.isRequired();
      }
    }

    return false;
  }

  /**
   * Set a value indicating if this service requires credential store.
   *
   * @param credentialStoreRequired
   */
  public void setCredentialStoreRequired(boolean credentialStoreRequired) {
    if (credentialStoreInfo == null) {
      credentialStoreInfo = new CredentialStoreInfo();
    }
    credentialStoreInfo.setRequired(credentialStoreRequired);
  }

  /**
   * Indicates if this service is enabled for credential store use.
   * False if it was not specified.
   *
   * @return true or false
   */
  public boolean isCredentialStoreEnabled() {
    if (credentialStoreInfo != null) {
      if (credentialStoreInfo.isEnabled() != null) {
        return credentialStoreInfo.isEnabled();
      }
    }

    return false;
  }

  /**
   * Set a value indicating if this service is enabled for credential store use.
   *
   * @param credentialStoreEnabled
   */
  public void setCredentialStoreEnabled(boolean credentialStoreEnabled) {
    if (credentialStoreInfo == null) {
      credentialStoreInfo = new CredentialStoreInfo();
    }
    credentialStoreInfo.setEnabled(credentialStoreEnabled);
  }

  /**
   * Get the credential store information object.
   *
   * @return
   */
  public CredentialStoreInfo getCredentialStoreInfo() {
    return credentialStoreInfo;
  }

  /**
   * Set a new value for the credential store information.
   *
   * @param credentialStoreInfo
   */
  public void setCredentialStoreInfo(CredentialStoreInfo credentialStoreInfo) {
    this.credentialStoreInfo = credentialStoreInfo;
  }

  /**
   * Gets the JSON predicate ({@link PredicateUtils#fromJSON(String)} that can be used to determine
   * if Kerberos has been enabled for this service or not.
   *
   * @return a valid JSON predicate ({@link PredicateUtils#fromJSON(String)}
   */
  public String getKerberosEnabledTest() {
    return kerberosEnabledTest;
  }

  /**
   * Sets the JSON predicate ({@link PredicateUtils#fromJSON(String)} that can be used to determine
   * if Kerberos has been enabled for this service or not.
   *
   * @param kerberosEnabledTest a valid JSON predicate ({@link PredicateUtils#fromJSON(String)}
   */
  public void setKerberosEnabledTest(String kerberosEnabledTest) {
    this.kerberosEnabledTest = kerberosEnabledTest;
  }


  /**
   * Gets the value for the SSO integration support
   *
   * @return the {@link SingleSignOnInfo}
   */
  public SingleSignOnInfo getSingleSignOnInfo() {
    return singleSignOnInfo;
  }

  /**
   * Set a new value for the SSO integration support
   *
   * @param singleSignOnInfo a {@link SingleSignOnInfo}
   */
  public void setSingleSignOnInfo(SingleSignOnInfo singleSignOnInfo) {
    this.singleSignOnInfo = singleSignOnInfo;
  }

  /**
   * Indicates if this service supports single sign-on integration.
   *
   * @return true or false
   */
  public boolean isSingleSignOnSupported() {
    return (singleSignOnInfo != null) && singleSignOnInfo.isSupported();
  }

  /**
   * @deprecated Use {@link #getSingleSignOnEnabledTest()} instead
   */
  @Deprecated
  public String getSingleSignOnEnabledConfiguration() {
    return singleSignOnInfo != null ? singleSignOnInfo.getEnabledConfiguration() : null;
  }

  public String getSingleSignOnEnabledTest() {
    return singleSignOnInfo != null ? singleSignOnInfo.getSsoEnabledTest() : null;
  }

  /**
   * @return the boolean flag is Kerberos is required for SSO integration
   */
  public boolean isKerberosRequiredForSingleSignOnIntegration() {
    return singleSignOnInfo != null && singleSignOnInfo.isKerberosRequired();
  }

  /**
   * Gets a new value for LDAP integration support
   */
  public ServiceLdapInfo getLdapInfo() {
    return ldapInfo;
  }

  /**
   * Sets a new value for LDAP integration support
   *
   * @param ldapInfo
   *          a {@link ServiceLdapInfo}
   */
  public void setLdapInfo(ServiceLdapInfo ldapInfo) {
    this.ldapInfo = ldapInfo;
  }

  /**
   * @return whether this service supports single sign-on integration
   */
  public boolean isLdapSupported() {
    return (ldapInfo != null) && ldapInfo.isSupported();
  }

  /**
   * @return the configuration specification that can be used to determine if LDAP
   *         has been enabled or not.
   */
  public String getLdapEnabledTest() {
    return ldapInfo != null ? ldapInfo.getLdapEnabledTest() : null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Service name:");
    sb.append(name);
    sb.append("\nService type:");
    sb.append(serviceType);
    sb.append("\nversion:");
    sb.append(version);
    sb.append("\nKerberos enabled test:");
    sb.append(kerberosEnabledTest);
    sb.append("\ncomment:");
    sb.append(comment);

    //for (PropertyInfo property : getProperties()) {
    //  sb.append("\tProperty name=" + property.getName() +
    //"\nproperty value=" + property.getValue() + "\ndescription=" + property.getDescription());
    //}
    for (ComponentInfo component : getComponents()) {
      sb.append("\n\n\nComponent:\n");
      sb.append("name=");
      sb.append(component.getName());
      sb.append("\tcategory=");
      sb.append(component.getCategory());
    }

    return sb.toString();
  }

  /**
   * Obtain the config types associated with this service.
   * The returned map is an unmodifiable view.
   *
   * @return unmodifiable map of config types associated with this service
   */
  public synchronized Map<String, Map<String, Map<String, String>>> getConfigTypeAttributes() {
    Map<String, Map<String, Map<String, String>>> tmpConfigTypes = configTypes == null ?
        new HashMap<>() : configTypes;

    for (String excludedtype : excludedConfigTypes) {
      tmpConfigTypes.remove(excludedtype);
    }

    return Collections.unmodifiableMap(tmpConfigTypes);
  }

  /**
   * Add the given type and set it's attributes.
   * If the type is marked for exclusion, it will not be added.
   *
   * @param type           configuration type
   * @param typeAttributes attributes associated with the type
   */
  public synchronized void setTypeAttributes(String type, Map<String, Map<String, String>> typeAttributes) {
    if (configTypes == null) {
      configTypes = new HashMap<>();
    }
    configTypes.put(type, typeAttributes);
  }

  /**
   * Set all types and associated attributes.  Any previously existing types and
   * attributes are removed prior to setting the new values.
   *
   * @param types map of type attributes
   */
  public synchronized void setAllConfigAttributes(Map<String, Map<String, Map<String, String>>> types) {
    configTypes = new HashMap<>();
    for (Map.Entry<String, Map<String, Map<String, String>>> entry : types.entrySet()) {
      setTypeAttributes(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Determine of the service has a dependency on the provided configuration type.
   *
   * @param type the config type
   * @return <code>true</code> if the service defines a dependency on the provided type
   */
  public boolean hasConfigDependency(String type) {
    return configDependencies != null && configDependencies.contains(type);
  }

  /**
   * Determine if the service contains the specified config type
   *
   * @param type config type to check
   * @return true if the service has the specified config type; false otherwise
   */
  public boolean hasConfigType(String type) {
    return configTypes != null && configTypes.containsKey(type)
        && !excludedConfigTypes.contains(type);
  }

  /**
   * Determine if the service has a dependency on the provided type and contains any of the provided properties.
   * This can be used in determining if a property is stale.
   *
   * @param type     the config type
   * @param keyNames the names of all the config keys for the given type
   * @return <code>true</code> if the config is stale
   */
  public boolean hasDependencyAndPropertyFor(String type, Collection<String> keyNames) {
    if (!hasConfigDependency(type)) {
      return false;
    }

    buildConfigLayout();
    Set<String> keys = configLayout.get(type);

    for (String staleCheck : keyNames) {
      if (keys != null && keys.contains(staleCheck)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Builds the config map specific to this service.
   */
  private void buildConfigLayout() {
    if (null == configLayout) {
      synchronized (this) {
        if (null == configLayout) {
          configLayout = new HashMap<>();

          for (PropertyInfo pi : getProperties()) {
            String type = pi.getFilename();
            int idx = type.indexOf(".xml");
            type = type.substring(0, idx);

            if (!configLayout.containsKey(type)) {
              configLayout.put(type, new HashSet<>());
            }

            configLayout.get(type).add(pi.getName());
          }
        }
      }
    }
  }

  public List<String> getConfigDependencies() {
    return configDependencies;
  }

  public List<String> getConfigDependenciesWithComponents() {
    List<String> retVal = new ArrayList<>();
    if (configDependencies != null) {
      retVal.addAll(configDependencies);
    }
    if (components != null) {
      for (ComponentInfo c : components) {
        if (c.getConfigDependencies() != null) {
          retVal.addAll(c.getConfigDependencies());
        }
      }
    }
    return retVal.size() == 0 ? (configDependencies == null ? null : configDependencies) : retVal;
  }

  public void setConfigDependencies(List<String> configDependencies) {
    this.configDependencies = configDependencies;
  }

  public String getSchemaVersion() {
    if (schemaVersion == null) {
      return AmbariMetaInfo.SCHEMA_VERSION_2;
    } else {
      return schemaVersion;
    }
  }


  public void setSchemaVersion(String schemaVersion) {
    this.schemaVersion = schemaVersion;
  }


  public String getServicePackageFolder() {
    return servicePackageFolder;
  }

  public void setServicePackageFolder(String servicePackageFolder) {
    this.servicePackageFolder = servicePackageFolder;
  }

  public File getServiceUpgradesFolder() {
    return serviceUpgradesFolder;
  }

  public void setServiceUpgradesFolder(File serviceUpgradesFolder) {
    this.serviceUpgradesFolder = serviceUpgradesFolder;
  }

  public File getChecksFolder() {
    return checksFolder;
  }

  public void setChecksFolder(File checksFolder) {
    this.checksFolder = checksFolder;
  }

  public File getServerActionsFolder() {
    return serverActionsFolder;
  }

  public void setServerActionsFolder(File serverActionsFolder) {
    this.serverActionsFolder = serverActionsFolder;
  }

  /**
   * Exposes (and initializes on first use) map of os-specific details.
   *
   * @return map of OS specific details keyed by family
   */
  public Map<String, ServiceOsSpecific> getOsSpecifics() {
    if (serviceOsSpecificsMap == null) {
      synchronized (this) { // Double-checked locking pattern
        if (serviceOsSpecificsMap == null) {
          Map<String, ServiceOsSpecific> tmpMap =
              new TreeMap<>();
          if (serviceOsSpecifics != null) {
            for (ServiceOsSpecific osSpecific : serviceOsSpecifics) {
              tmpMap.put(osSpecific.getOsFamily(), osSpecific);
            }
          }
          serviceOsSpecificsMap = tmpMap;
        }
      }
    }
    return serviceOsSpecificsMap;
  }

  public void setOsSpecifics(Map<String, ServiceOsSpecific> serviceOsSpecificsMap) {
    this.serviceOsSpecificsMap = serviceOsSpecificsMap;
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

  public CommandScriptDefinition getCommandScript() {
    return commandScript;
  }

  public void setCommandScript(CommandScriptDefinition commandScript) {
    this.commandScript = commandScript;
  }

  /**
   * @param file the file containing the metrics definitions
   */
  public void setMetricsFile(File file) {
    metricsFile = file;
  }

  /**
   * @return the metrics file, or <code>null</code> if none exists
   */
  public File getMetricsFile() {
    return metricsFile;
  }

  /**
   * @return the metrics defined for this service
   */
  public Map<String, Map<String, List<MetricDefinition>>> getMetrics() {
    return metrics;
  }

  /**
   * @param map the metrics for this service
   */
  public void setMetrics(Map<String, Map<String, List<MetricDefinition>>> map) {
    metrics = map;
  }

  /**
   * @return the configuration directory name
   */
  public String getConfigDir() {
    return configDir;
  }

  /**
   * @return whether the service is a monitoring service
   */
  public Boolean isMonitoringService() {
    return monitoringService;
  }

  /**
   * @param monitoringService whether the service is a monitoring service
   */
  public void setMonitoringService(Boolean monitoringService) {
    this.monitoringService = monitoringService;
  }

  /**
   * @param file the file containing the alert definitions
   */
  public void setAlertsFile(File file) {
    alertsFile = file;
  }

  /**
   * @return the alerts file, or <code>null</code> if none exists
   */
  public File getAlertsFile() {
    return alertsFile;
  }

  /**
   * @param file the file containing the alert definitions
   */
  public void setKerberosDescriptorFile(File file) {
    kerberosDescriptorFile = file;
  }

  /**
   * @return the kerberos descriptor file, or <code>null</code> if none exists
   */
  public File getKerberosDescriptorFile() {
    return kerberosDescriptorFile;
  }

  public boolean isRollingRestartSupported() {
    return rollingRestartSupported;
  }

  public void setRollingRestartSupported(boolean rollingRestartSupported) {
    this.rollingRestartSupported = rollingRestartSupported;
  }

  /**
   * @return the widgets descriptor file, or <code>null</code> if none exists
   */
  public File getWidgetsDescriptorFile() {
    return widgetsDescriptorFile;
  }

  public void setWidgetsDescriptorFile(File widgetsDescriptorFile) {
    this.widgetsDescriptorFile = widgetsDescriptorFile;
  }

  public StackRoleCommandOrder getRoleCommandOrder() {
    return roleCommandOrder;
  }

  public void setRoleCommandOrder(StackRoleCommandOrder roleCommandOrder) {
    this.roleCommandOrder = roleCommandOrder;
  }

  /**
   * @return config types this service contains configuration for, but which are primarily related to another service
   */
  public Set<String> getExcludedConfigTypes() {
    return excludedConfigTypes;
  }

  public void setExcludedConfigTypes(Set<String> excludedConfigTypes) {
    this.excludedConfigTypes = excludedConfigTypes;
  }

  //todo: ensure that required properties are never modified...
  public Map<String, PropertyInfo> getRequiredProperties() {
    Map<String, PropertyInfo> result = requiredProperties;
    if (result == null) {
      synchronized (this) {
        result = requiredProperties;
        if (result == null) {
          requiredProperties = result = new HashMap<>();
          List<PropertyInfo> properties = getProperties();
          for (PropertyInfo propertyInfo : properties) {
            if (propertyInfo.isRequireInput()) {
              result.put(propertyInfo.getName(), propertyInfo);
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Determine whether or not a restart is required for this service after a host rack info change.
   *
   * @return true if a restart is required
   */
  public Boolean isRestartRequiredAfterRackChange() {
    return restartRequiredAfterRackChange;
  }

  /**
   * Set indicator for required restart after a host rack info change.
   *
   * @param restartRequiredAfterRackChange true if a restart is required
   */
  public void setRestartRequiredAfterRackChange(Boolean restartRequiredAfterRackChange) {
    this.restartRequiredAfterRackChange = restartRequiredAfterRackChange;
  }

  public String getThemesDir() {
    return themesDir;
  }

  public void setThemesDir(String themesDir) {
    this.themesDir = themesDir;
  }

  public List<ThemeInfo> getThemes() {
    return themes;
  }

  public void setThemes(List<ThemeInfo> themes) {
    this.themes = themes;
  }

  public Map<String, ThemeInfo> getThemesMap() {
    if (themesMap == null) {
      Map<String, ThemeInfo> tmp = new TreeMap<>();
      if (themes != null) {
        for (ThemeInfo theme : themes) {
          tmp.put(theme.getFileName(), theme);
        }
      }
      themesMap = tmp;
    }
    return themesMap;
  }

  public void setThemesMap(Map<String, ThemeInfo> themesMap) {
    this.themesMap = themesMap;
  }

  //Quick links configurations
  public String getQuickLinksConfigurationsDir() {
    return quickLinksConfigurationsDir;
  }

  public void setQuickLinksConfigurationsDir(String quickLinksConfigurationsDir) {
    this.quickLinksConfigurationsDir = quickLinksConfigurationsDir;
  }

  public List<QuickLinksConfigurationInfo> getQuickLinksConfigurations() {
    return quickLinksConfigurations;
  }

  public void setQuickLinksConfigurations(List<QuickLinksConfigurationInfo> quickLinksConfigurations) {
    this.quickLinksConfigurations = quickLinksConfigurations;
  }

  public Map<String, QuickLinksConfigurationInfo> getQuickLinksConfigurationsMap() {
    if (quickLinksConfigurationsMap == null) {
      Map<String, QuickLinksConfigurationInfo> tmp = new TreeMap<>();
      if (quickLinksConfigurations != null) {
        for (QuickLinksConfigurationInfo quickLinksConfiguration : quickLinksConfigurations) {
          tmp.put(quickLinksConfiguration.getFileName(), quickLinksConfiguration);
        }
      }
      quickLinksConfigurationsMap = tmp;
    }
    return quickLinksConfigurationsMap;
  }

  public void setQuickLinksConfigurationsMap(Map<String, QuickLinksConfigurationInfo> quickLinksConfigurationsMap) {
    this.quickLinksConfigurationsMap = quickLinksConfigurationsMap;
  }

  public List<ServicePropertyInfo> getServicePropertyList() {
    return servicePropertyList;
  }

  public void setServicePropertyList(List<ServicePropertyInfo> servicePropertyList) {
    this.servicePropertyList = servicePropertyList;
    afterServicePropertyListSet();
  }

  private void afterServicePropertyListSet() {
    validateServiceProperties();
    buildServiceProperties();
  }


  /**
   * Returns the service properties defined in the xml service definition.
   *
   * @return Service property map
   */
  public Map<String, String> getServiceProperties() {
    return servicePropertyMap;
  }

  /**
   * Constructs the map that stores the service properties defined in the xml service definition.
   * The keys are the property names and values the property values.
   * It ensures that missing required service properties are added with default values.
   */
  private void buildServiceProperties() {
    if (isValid()) {
      Map<String, String> properties = Maps.newHashMap();
      for (ServicePropertyInfo property : getServicePropertyList()) {
        properties.put(property.getName(), property.getValue());
      }
      servicePropertyMap = ImmutableMap.copyOf(ensureMandatoryServiceProperties(properties));
    } else {
      servicePropertyMap = ImmutableMap.of();
    }


  }

  private Map<String, String> ensureMandatoryServiceProperties(Map<String, String> properties) {
    return ensureVisibilityServiceProperties(properties);
  }

  private Map<String, String> ensureVisibilityServiceProperties(Map<String, String> properties) {
    if (!properties.containsKey(DEFAULT_SERVICE_INSTALLABLE_PROPERTY.getKey())) {
      properties.put(DEFAULT_SERVICE_INSTALLABLE_PROPERTY.getKey(), DEFAULT_SERVICE_INSTALLABLE_PROPERTY.getValue());
    }

    if (!properties.containsKey(DEFAULT_SERVICE_MANAGED_PROPERTY.getKey())) {
      properties.put(DEFAULT_SERVICE_MANAGED_PROPERTY.getKey(), DEFAULT_SERVICE_MANAGED_PROPERTY.getValue());
    }


    if (!properties.containsKey(DEFAULT_SERVICE_MONITORED_PROPERTY.getKey())) {
      properties.put(DEFAULT_SERVICE_MONITORED_PROPERTY.getKey(), DEFAULT_SERVICE_MONITORED_PROPERTY.getValue());
    }

    return properties;
  }

  void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    afterServicePropertyListSet();
  }


  private void validateServiceProperties() {
    // Verify if there are duplicate service properties by name
    Multimap<String, ServicePropertyInfo> servicePropsByName = Multimaps.index(
        getServicePropertyList(),
        new Function<ServicePropertyInfo, String>() {
          @Override
          public String apply(ServicePropertyInfo servicePropertyInfo) {
            return servicePropertyInfo.getName();
          }
        }

    );

    for (String propertyName : servicePropsByName.keySet()) {
      if (servicePropsByName.get(propertyName).size() > 1) {
        setValid(false);
        addError("Duplicate service property with name '" + propertyName + "' found in " + getName() + ":" + getVersion() + " service definition !");
      }
    }

    for (ComponentInfo component : getComponents()) {
      int primaryLogs = 0;
      for (LogDefinition log : component.getLogs()) {
        primaryLogs += log.isPrimary() ? 1 : 0;
      }

      if (primaryLogs > 1) {
        setValid(false);
        addError("More than one primary log exists for the component " + component.getName());
      }
    }

    // validate credential store information
    if (credentialStoreInfo != null) {
      // if both are specified, supported must be true if enabled is false or true.
      if (credentialStoreInfo.isSupported() != null && credentialStoreInfo.isEnabled() != null) {
        if (!credentialStoreInfo.isSupported() && credentialStoreInfo.isEnabled()) {
          setValid(false);
          addError("Credential store cannot be enabled for service " + getName() + " as it does not support it.");
        }
      }

      // Must be specified
      if (credentialStoreInfo.isSupported() == null) {
        setValid(false);
        addError("Credential store supported is not specified for service " + getName());
      }

      // Must be specified
      if (credentialStoreInfo.isEnabled() == null) {
        setValid(false);
        addError("Credential store enabled is not specified for service " + getName());
      }
    }

    // validate single sign-in support information
    if (singleSignOnInfo != null) {
      if (singleSignOnInfo.isSupported()) {
        if (StringUtils.isEmpty(singleSignOnInfo.getSsoEnabledTest()) && StringUtils.isEmpty(singleSignOnInfo.getEnabledConfiguration())) {
          setValid(false);
          addError("Single Sign-on support is indicated for service " + getName() + " but no test configuration has been set (enabledConfiguration or ssoEnabledTest).");
        }
      }
    }

    if (ldapInfo != null && ldapInfo.isSupported() && StringUtils.isBlank(ldapInfo.getLdapEnabledTest())) {
      setValid(false);
      addError("LDAP support is indicated for service " + getName() + " but no test configuration has been set by ldapEnabledTest.");
    }
  }

  /**
   * Gets whether this service advertises a version based on whether at least
   * one of its components advertises a version.
   *
   * @return {@code true} if at least 1 component of this service advertises a
   *         version, {@code false} otherwise.
   */
  public boolean isVersionAdvertised() {
    if (null == components) {
      return false;
    }

    for (ComponentInfo componentInfo : components) {
      if (componentInfo.isVersionAdvertised()) {
        return true;
      }
    }

    return false;
  }

  public enum Selection {
    DEFAULT,
    TECH_PREVIEW,
    MANDATORY,
    DEPRECATED
  }
}
