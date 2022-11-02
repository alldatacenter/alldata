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

package org.apache.ambari.server.orm.entities;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.view.ViewSubResourceDefinition;
import org.apache.ambari.server.view.configuration.ParameterConfig;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.view.View;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.validation.Validator;

/**
 * Entity representing a View.
 */
@Table(name = "viewmain")
@NamedQuery(name = "allViews",
    query = "SELECT view FROM ViewEntity view")
@Entity
public class ViewEntity implements ViewDefinition {

  public static final String AMBARI_ONLY = "AMBARI-ONLY";

  /**
   * The unique view name.
   */
  @Id
  @Column(name = "view_name", nullable = false, insertable = true,
      updatable = false, unique = true, length = 100)
  private String name;

  /**
   * The public view name.
   */
  @Column
  @Basic
  private String label;

  /**
   * The view description.
   */
  @Column
  @Basic
  private String description;

  /**
   * The icon path.
   */
  @Column
  @Basic
  private String icon;

  /**
   * The big icon path.
   */
  @Column
  @Basic
  private String icon64;

  /**
   * The view version.
   */
  @Column
  @Basic
  private String version;

  /**
   * The view build number.
   */
  @Column
  @Basic
  private String build;

  /**
   * The view archive.
   */
  @Column
  @Basic
  private String archive;

  /**
   * The masker class for parameters.
   */
  @Column
  @Basic
  private String mask;

  /**
   * Indicates whether or not this is a system view.
   */
  @Column(name = "system_view")
  @Basic
  private Integer system;

  /**
  * The list of view parameters.
  */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "view")
  private Collection<ViewParameterEntity> parameters = new HashSet<>();

  /**
   * The list of view resources.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "view")
  private Collection<ViewResourceEntity> resources = new HashSet<>();

   /**
   * The list of view instances.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "view")
  private Collection<ViewInstanceEntity> instances = new HashSet<>();

  /**
   * The list of view permissions.
   */
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumns({
      @JoinColumn(name = "resource_type_id", referencedColumnName = "resource_type_id", nullable = false)
  })
  private Collection<PermissionEntity> permissions = new HashSet<>();

  /**
   * The resource type.
   */
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumns({
      @JoinColumn(name = "resource_type_id", referencedColumnName = "resource_type_id", nullable = false)
  })
  private ResourceTypeEntity resourceType;

  // ----- Transient data ----------------------------------------------------

  /**
   * The associated view configuration.
   */
  @Transient
  private ViewConfig configuration;

  /**
   * The Ambari configuration properties.
   */
  @Transient
  private final Configuration ambariConfiguration;

  /**
   * The external resource type for the view.
   */
  @Transient
  private final Resource.Type externalResourceType;

  /**
   * The classloader used to load the view.
   */
  @Transient
  private ClassLoader classLoader = null;

  /**
   * The mapping of resource type to resource provider.
   */
  @Transient
  private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<>();

  /**
   * The mapping of resource type to resource definition.
   */
  @Transient
  private final Map<Resource.Type, ViewSubResourceDefinition> resourceDefinitions = new HashMap<>();

  /**
   * The mapping of resource type to resource configuration.
   */
  @Transient
  private final Map<Resource.Type, ResourceConfig> resourceConfigurations = new HashMap<>();

  /**
   * The name of the view shared across versions.
   */
  @Transient
  private String commonName = null;

  /**
   * The view.
   */
  @Transient
  private View view = null;

  /**
   * The view validator.
   */
  @Transient
  private Validator validator = null;

  /**
   * The view status.
   */
  @Transient
  private ViewStatus status = ViewStatus.PENDING;

  /**
   * The view status detail.
   */
  @Transient
  private String statusDetail;

  /**
   * Indicates whether or not this view is configurable through cluster association.
   */
  @Transient
  private boolean clusterConfigurable;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view entity.
   */
  public ViewEntity() {
    this.configuration        = null;
    this.ambariConfiguration  = null;
    this.archive              = null;
    this.externalResourceType = null;
    this.system               = 0;
    this.clusterConfigurable  = false;
  }

  /**
   * Construct a view entity from the given configuration.
   *
   * @param configuration        the view configuration
   * @param ambariConfiguration  the Ambari configuration
   * @param archivePath          the path of the view archive
   */
  public ViewEntity(ViewConfig configuration, Configuration ambariConfiguration,
                    String archivePath) {
    setConfiguration(configuration);

    this.ambariConfiguration = ambariConfiguration;
    this.archive             = archivePath;

    String version = configuration.getVersion();

    this.name        = getViewName(configuration.getName(), version);
    this.label       = configuration.getLabel();
    this.description = configuration.getDescription();
    this.version     = version;
    this.build       = configuration.getBuild();

    this.mask        = configuration.getMasker();
    this.icon        = configuration.getIcon();
    this.icon64      = configuration.getIcon64();
    this.system      = configuration.isSystem() ? 1 : 0;

    this.externalResourceType =
        new Resource.Type(getQualifiedResourceTypeName(ResourceConfig.EXTERNAL_RESOURCE_PLURAL_NAME));
  }


  // ----- ViewDefinition ----------------------------------------------------

  @Override
  public String getViewName() {
    return getCommonName();
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getBuild() {
    return build;
  }

  @Override
  public ViewStatus getStatus() {
    return status;
  }

  @Override
  public String getStatusDetail() {
    return statusDetail;
  }

  @Override
  public String getMask() {
    return mask;
  }


  // ----- ViewEntity --------------------------------------------------------

  /**
   * Get the view name.
   *
   * @return the view name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the view name.
   *
   * @param name the view name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the common name of the view.
   * This name is shared across versions of the view.
   *
   * @return the common name
   */
  public synchronized String getCommonName() {
    if (commonName == null) {
      // Strip version from the internal name
      commonName = name.replaceAll("\\{(.+)\\}", "");
    }
    return commonName;
  }

  /**
   * Set the view label (display name).
   *
   * @param label the view label
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Set the view description.
   *
   * @param description the view description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Set the view version.
   *
   * @param version the version
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Set the view build number.
   *
   * @param build the build
   */
  public void setBuild(String build) {
    this.build = build;
  }

  /**
   * Get the icon path.
   *
   * @return the icon path
   */
  public String getIcon() {
    return icon;
  }

  /**
   * Set the icon path.
   *
   * @param icon  the icon path
   */
  public void setIcon(String icon) {
    this.icon = icon;
  }

  /**
   * Get the big icon path.
   *
   * @return the big icon path
   */
  public String getIcon64() {
    return icon64;
  }

  /**
   * Set the big icon path.
   *
   * @param icon64  the big icon path
   */
  public void setIcon64(String icon64) {
    this.icon64 = icon64;
  }

  /**
   * Get the view parameters.
   *
   * @return the view parameters
   */
  public Collection<ViewParameterEntity> getParameters() {
    return parameters;
  }

  /**
   * Set the view parameters.
   *
   * @param parameters  the view parameters
   */
  public void setParameters(Collection<ViewParameterEntity> parameters) {
    this.parameters = parameters;
  }

  /**
   * Get the view custom permissions.
   *
   * @return the view permissions
   */
  public Collection<PermissionEntity> getPermissions() {
    return permissions;
  }

  /**
   * Set the custom view permissions.
   *
   * @param permissions  the permissions
   */
  public void setPermissions(Collection<PermissionEntity> permissions) {
    this.permissions = permissions;
  }

  /**
   * Get the permission entity for the given permission name.
   *
   * @param permissionName  the permission name
   *
   * @return the matching permission entity or null
   */
  public PermissionEntity getPermission(String permissionName) {

    for (PermissionEntity permissionEntity : permissions) {
      if (permissionEntity.getPermissionName().equals(permissionName)) {
        return permissionEntity;
      }
    }
    return null;
  }

  /**
   * Get the view resources.
   *
   * @return the view resources
   */
  public Collection<ViewResourceEntity> getResources() {
    return resources;
  }

  /**
   * Set the view resources.
   *
   * @param resources  the view resources
   */
  public void setResources(Collection<ViewResourceEntity> resources) {
    this.resources = resources;
  }

  /**
   * Get the view instances.
   *
   * @return the view instances
   */
  public Collection<ViewInstanceEntity> getInstances() {
    return instances;
  }

  /**
   * Set the view instances.
   *
   * @param instances  the instances
   */
  public void setInstances(Collection<ViewInstanceEntity> instances) {
    this.instances = instances;
  }

  /**
   * Add an instance definition.
   *
   * @param viewInstanceDefinition  the instance definition
   */
  public void addInstanceDefinition(ViewInstanceEntity viewInstanceDefinition) {
    removeInstanceDefinition(viewInstanceDefinition.getName());
    instances.add(viewInstanceDefinition);
  }

  /**
   * Remove an instance definition.
   *
   * @param instanceName  the instance name
   */
  public void removeInstanceDefinition(String instanceName) {
    ViewInstanceEntity entity = getInstanceDefinition(instanceName);
    if (entity != null) {
      instances.remove(entity);
    }
  }

  /**
   * Get an instance definition for the given name.
   *
   * @param instanceName  the instance name
   *
   * @return the instance definition
   */
  public ViewInstanceEntity getInstanceDefinition(String instanceName) {
    for (ViewInstanceEntity viewInstanceEntity : instances) {
      if (viewInstanceEntity.getName().equals(instanceName)) {
        return viewInstanceEntity;
      }
    }
    return null;
  }

  /**
   * Get the path of the view archive.
   *
   * @return  the path of the view archive
   */
  public String getArchive() {
    return archive;
  }

  /**
   * Set the view archive path.
   *
   * @param archive  the view archive path
   */
  public void setArchive(String archive) {
    this.archive = archive;
  }

  /**
   * Get a property for the given key from the ambari configuration.
   *
   * @param key  the property key
   *
   * @return the property value; null indicates that the configuration contains no mapping for the key
   */
  public String getAmbariProperty(String key) {
    return ambariConfiguration.getProperty(key);
  }

  /**
   * Get the Ambari configuration.
   *
   * @return the Ambari configuration
   */
  public Configuration getAmbariConfiguration() {
    return ambariConfiguration;
  }

  /**
   * Get a resource name qualified by the associated view name.
   *
   * @param resourceTypeName  the resource type name
   *
   * @return the qualified resource name
   */
  public String getQualifiedResourceTypeName(String resourceTypeName) {
    return getName() + "/" + resourceTypeName;
  }

  /**
   * Get the external resource type for the view.
   *
   * @return the external resource type
   */
  public Resource.Type getExternalResourceType() {
    return externalResourceType;
  }

  /**
   * Get the class loader used to load the view classes.
   *
   * @return the class loader
   */
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  /**
   * Set the class loader.
   *
   * @param classLoader  the class loader
   */
  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Add a resource provider for the given type.
   *
   * @param type      the resource type
   * @param provider  the resource provider
   */
  public void addResourceProvider(Resource.Type type, ResourceProvider provider) {
    resourceProviders.put(type, provider);
  }

  /**
   * Get the resource provider for the given type.
   *
   * @param type  the resource type
   *
   * @return the resource provider associated with the given type
   */
  public ResourceProvider getResourceProvider(Resource.Type type) {
    return resourceProviders.get(type);
  }

  /**
   * Add a resource definition.
   *
   * @param definition  the resource definition
   */
  public void addResourceDefinition(ViewSubResourceDefinition definition) {
    resourceDefinitions.put(definition.getType(), definition);
  }

  /**
   * Get the resource definition for the given type.
   *
   * @param type  the resource type
   *
   * @return the resource definition associated with the given type
   */
  public ViewSubResourceDefinition getResourceDefinition(Resource.Type type) {
    return resourceDefinitions.get(type);
  }

  /**
   * Get the mapping of resource type to resource definitions.
   *
   * @return the mapping of resource type to resource definitions
   */
  public Map<Resource.Type, ViewSubResourceDefinition> getResourceDefinitions() {
    return resourceDefinitions;
  }

  /**
   * Add a resource configuration for the given type.
   *
   * @param type    the resource type
   * @param config  the configuration
   */
  public void addResourceConfiguration(Resource.Type type, ResourceConfig config) {
    resourceConfigurations.put(type, config);
  }

  /**
   * Get the mapping of resource type to resource configurations.
   *
   * @return the mapping of resource types to resource configurations
   */
  public Map<Resource.Type, ResourceConfig> getResourceConfigurations() {
    return resourceConfigurations;
  }

  /**
   * Get the set of resource types for this view.
   *
   * @return the set of resource type
   */
  public Set<Resource.Type> getViewResourceTypes() {
    return resourceProviders.keySet();
  }

  /**
   * Set the view configuration.
   *
   * @param configuration  the view configuration
   */
  public void setConfiguration(ViewConfig configuration) {
    this.configuration       = configuration;
    this.clusterConfigurable = false;

    if(configuration.getClusterConfigOptions() != null &&
       configuration.getClusterConfigOptions().equals(AMBARI_ONLY)){
      this.clusterConfigurable = true;
      return;
    }

    // if any of the parameters contain a cluster config element then the view is cluster configurable
    for (ParameterConfig parameterConfig : configuration.getParameters()) {
      String clusterConfig = parameterConfig.getClusterConfig();
      if (clusterConfig != null && !clusterConfig.isEmpty()) {
        this.clusterConfigurable = true;
        return;
      }
    }
  }

  /**
   * Get the associated view configuration.
   *
   * @return the configuration
   */
  public ViewConfig getConfiguration() {
    return configuration;
  }

  /**
   * Set the view.
   *
   * @param view  the view
   */
  public void setView(View view) {
    this.view = view;
  }

  /**
   * Get the associated view.
   *
   * @return the view
   */
  public View getView() {
    return view;
  }

  /**
   * Set the view validator.
   *
   * @param validator  the view validator
   */
  public void setValidator(Validator validator) {
    this.validator = validator;
  }

  /**
   * Get the associated view validator.
   *
   * @return the view validator
   */
  public Validator getValidator() {
    return validator;
  }

  /**
   * Determine whether or not a validator has been specified for this view.
   *
   * @return true if this view has a validator
   */
  public boolean hasValidator() {
    return validator != null;
  }

  /**
   * Set the mask class name.
   *
   * @param mask the mask class name
   */
  public void setMask(String mask) {
    this.mask = mask;
  }

  /**
   * Determine whether or not the view is a system view.
   *
   * @return true if the view is a system view
   */
  public boolean isSystem() {
    return system == 1;
  }

  /**
   * Set the flag which indicates whether or not the view is a system view.
   *
   * @param required  the system flag; true if the view is a system view
   */
  public void setSystem(boolean required) {
    this.system = required ? 1 : 0;
  }

  /**
   * Get the admin resource type entity.
   *
   * @return the resource type entity
   */
  public ResourceTypeEntity getResourceType() {
    return resourceType;
  }

  /**
   * Set the admin resource type entity.
   *
   * @param resourceType  the resource type entity
   */
  public void setResourceType(ResourceTypeEntity resourceType) {
    this.resourceType = resourceType;
  }

  /**
   * Set the status of the view.
   *
   * @param status  the view status
   */
  public void setStatus(ViewStatus status) {
    this.status = status;
  }

  /**
   * Set the status detail for the view.
   *
   * @param statusDetail  the status detail
   */
  public void setStatusDetail(String statusDetail) {
    this.statusDetail = statusDetail;
  }

  /**
   * Determine whether or not this view is configurable through a cluster association.
   *
   * @return true if this view is cluster configurable
   */
  public boolean isClusterConfigurable() {
    return clusterConfigurable;
  }

  /**
   * Determine whether or not the entity is deployed.
   *
   * @return true if the entity is deployed
   */
  public boolean isDeployed() {
    return status.equals(ViewStatus.DEPLOYED);
  }

  /**
   * Get the internal view name from the given common name and version.
   *
   * @param name     the view common name
   * @param version  the version
   *
   * @return the view name
   */
  public static String getViewName(String name, String version) {
    return name + "{" + version + "}";
  }

  @Override
  public String toString() {
    return "ViewEntity{" +
        "name='" + name + '\'' +
        ", label='" + label + '\'' +
        ", description='" + description + '\'' +
        '}';
  }
}
