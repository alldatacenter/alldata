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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.SecurityHelperImpl;
import org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter;
import org.apache.ambari.server.view.ViewContextImpl;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.validation.InstanceValidationResultImpl;
import org.apache.ambari.server.view.validation.ValidationException;
import org.apache.ambari.server.view.validation.ValidationResultImpl;
import org.apache.ambari.view.ClusterType;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.migration.ViewDataMigrationContext;
import org.apache.ambari.view.migration.ViewDataMigrator;
import org.apache.ambari.view.validation.ValidationResult;
import org.apache.ambari.view.validation.Validator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Represents an instance of a View.
 */
@Table(name = "viewinstance", uniqueConstraints =
  @UniqueConstraint(
    name = "UQ_viewinstance_name", columnNames = {"view_name", "name"}
  )
)
@NamedQueries({ @NamedQuery(
    name = "allViewInstances",
    query = "SELECT viewInstance FROM ViewInstanceEntity viewInstance"),
    @NamedQuery(
        name = "viewInstanceByResourceId",
        query = "SELECT viewInstance FROM ViewInstanceEntity viewInstance "
            + "WHERE viewInstance.resource.id=:resourceId"),
    @NamedQuery(
        name = "getResourceIdByViewInstance",
        query = "SELECT viewInstance.resource FROM ViewInstanceEntity viewInstance "
            + "WHERE viewInstance.viewName = :viewName AND viewInstance.name = :instanceName"), })

@TableGenerator(name = "view_instance_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "view_instance_id_seq"
  , initialValue = 1
)
@Entity
public class ViewInstanceEntity implements ViewInstanceDefinition {

  @Id
  @Column(name = "view_instance_id", nullable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "view_instance_id_generator")
  private Long viewInstanceId;

  @Column(name = "view_name", nullable = false, insertable = false, updatable = false)
  private String viewName;

  /**
   * The instance name.
   */
  @Column(name = "name", nullable = false, insertable = true, updatable = false)
  private String name;

  /**
   * The public view instance name.
   */
  @Column
  @Basic
  private String label;

  /**
   * The description.
   */
  @Column
  @Basic
  private String description;

  /**
   * The associated cluster handle.
   */
  @Column(name = "cluster_handle", nullable = true)
  private Long clusterHandle;

  /**
   *  Cluster Type for cluster Handle
   */
  @Enumerated(value = EnumType.STRING)
  @Column(name = "cluster_type", nullable = false, length = 100)
  private ClusterType clusterType = ClusterType.LOCAL_AMBARI;

  /**
   * Visible flag.
   */
  @Column
  @Basic
  private char visible;

  /**
   * The icon path.
   */
  @Column
  @Basic
  private String icon;


  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumns({
          @JoinColumn(name = "short_url", referencedColumnName = "url_id", nullable = true)
  })
  private ViewURLEntity viewUrl;

  /**
   * The big icon path.
   */
  @Column
  @Basic
  private String icon64;

  /**
   * The XML driven instance flag.
   */
  @Column(name="xml_driven")
  @Basic
  private char xmlDriven = 'N';

  /**
   * Indicates whether or not to alter the names of the data store entities to
   * avoid db reserved word conflicts.
   */
  @Column(name = "alter_names", nullable = false)
  @Basic
  private Integer alterNames;

  /**
   * The instance properties.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "viewInstance")
  private Collection<ViewInstancePropertyEntity> properties = new HashSet<>();

  /**
   * The instance data.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "viewInstance")
  private Collection<ViewInstanceDataEntity> data = new HashSet<>();

  /**
   * The list of view entities.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "viewInstance")
  private Collection<ViewEntityEntity> entities = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "view_name", referencedColumnName = "view_name", nullable = false)
  private ViewEntity view;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumns({
      @JoinColumn(name = "resource_id", referencedColumnName = "resource_id", nullable = false)
  })
  private ResourceEntity resource;


  // ----- transient data ----------------------------------------------------

  /**
   * The associated configuration.  This will be null if the instance was not
   * defined in the archive.
   */
  @Transient
  private final InstanceConfig instanceConfig;

  /**
   * The mapping of resource type to resource provider.  Calculated when the
   * instance is added.
   */
  @Transient
  private final Map<Resource.Type, ResourceProvider> resourceProviders = new HashMap<>();

  /**
   * The mapping of the resource plural name to service.  Calculated when the
   * instance is added.
   */
  @Transient
  private final Map<String, Object> services = new HashMap<>();

  /**
   * Helper class.
   */
  // TODO : we should @Inject this.
  @Transient
  private SecurityHelper securityHelper = SecurityHelperImpl.getInstance();

  /**
   * The view data migrator.
   */
  @Transient
  private ViewDataMigrator dataMigrator;

  // ----- Constructors ------------------------------------------------------

  public ViewInstanceEntity() {
    instanceConfig = null;
    alterNames = 1;
  }

  /**
   * Construct a view instance definition.
   *
   * @param view           the parent view definition
   * @param instanceConfig the associated configuration
   */
  public ViewInstanceEntity(ViewEntity view, InstanceConfig instanceConfig) {
    name = instanceConfig.getName();
    this.instanceConfig = instanceConfig;
    this.view = view;
    viewName = view.getName();
    description = instanceConfig.getDescription();
    clusterHandle = null;
    visible = instanceConfig.isVisible() ? 'Y' : 'N';
    alterNames = 1;
    clusterType = ClusterType.LOCAL_AMBARI;

    String label = instanceConfig.getLabel();
    this.label = (label == null || label.length() == 0) ? view.getLabel() : label;

    String icon = instanceConfig.getIcon();
    this.icon = (icon == null || icon.length() == 0) ? view.getIcon() : icon;

    String icon64 = instanceConfig.getIcon64();
    this.icon64 = (icon64 == null || icon64.length() == 0) ? view.getIcon64() : icon64;
  }

  /**
   * Construct a view instance definition.
   *
   * @param view the parent view definition
   * @param name the instance name
   */
  public ViewInstanceEntity(ViewEntity view, String name) {
    this(view, name, view.getLabel());
  }

  /**
   * Construct a view instance definition.
   *
   * @param view the parent view definition
   * @param name the instance name
   * @param label the instance label
   */
  public ViewInstanceEntity(ViewEntity view, String name, String label) {
    this.name = name;
    instanceConfig = null;
    this.view = view;
    viewName = view.getName();
    description = null;
    clusterHandle = null;
    visible = 'Y';
    alterNames = 1;
    this.label = label;
  }


  // ----- ViewInstanceDefinition --------------------------------------------

  @Override
  public String getInstanceName() {
    return name;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public Map<String, String> getPropertyMap() {
    Map<String, String> propertyMap = new HashMap<>();

    for (ViewInstancePropertyEntity viewInstancePropertyEntity : getProperties()) {
      propertyMap.put(viewInstancePropertyEntity.getName(), viewInstancePropertyEntity.getValue());
    }
    for (ViewParameterEntity viewParameterEntity : view.getParameters()) {
      String parameterName = viewParameterEntity.getName();
      if (!propertyMap.containsKey(parameterName)) {
        propertyMap.put(parameterName, viewParameterEntity.getDefaultValue());
      }
    }
    return propertyMap;
  }

  @Override
  public Map<String, String> getInstanceDataMap() {
    Map<String, String> applicationData = new HashMap<>();

    String user = getCurrentUserName();

    for (ViewInstanceDataEntity viewInstanceDataEntity : data) {
      if (viewInstanceDataEntity.getUser().equals(user)) {
        applicationData.put(viewInstanceDataEntity.getName(), viewInstanceDataEntity.getValue());
      }
    }
    return applicationData;
  }

  @Override
  public ViewDefinition getViewDefinition() {
    return view;
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
  public Long getClusterHandle() {
    return clusterHandle;
  }





  @Override
  public boolean isVisible() {
    return visible == 'y' || visible == 'Y';
  }


  // ----- ViewInstanceEntity ------------------------------------------------

  /**
   * Get the view instance id.
   *
   * @return the instance id
   */
  public Long getViewInstanceId() {
    return viewInstanceId;
  }

  /**
   * Set the given view instance id.
   *
   * @param viewInstanceId  the instance id
   */
  public void setViewInstanceId(Long viewInstanceId) {
    this.viewInstanceId = viewInstanceId;
  }

  /**
   * Set the view name.
   *
   * @param viewName the view name
   */
  public void setViewName(String viewName) {
    this.viewName = viewName;
  }


  /**
   * Get the name of this instance.
   *
   * @return the instance name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of this instance.
   *
   * @param name the instance name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Set the label.
   *
   * @param label the label
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Set the description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Set a cluster association for this view instance with the Ambari cluster
   * identified by the given cluster handle.  For a local cluster reference,
   * the cluster handle is simply the unique cluster id.
   *
   * @param clusterHandle  the cluster identifier
   */
  public void setClusterHandle(Long clusterHandle) {
    this.clusterHandle = clusterHandle;
  }

  /**
   *  Get the type of cluster the view instance is attached to
   *
   * @return clusterType the type of cluster for cluster handle
   */
  @Override
  public ClusterType getClusterType() {
    return clusterType;
  }

  /**
   * Set the type of cluster for cluster handle
   *
   * @param clusterType
   */
  public void setClusterType(ClusterType clusterType) {
    this.clusterType = clusterType;
  }

  /**
   * Set the visible flag.
   *
   * @param visible visible flag
   */
  public void setVisible(boolean visible) {
    this.visible = (visible ? 'Y' : 'N');
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
   * @param icon the icon path
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
   * @param icon64 the big icon path
   */
  public void setIcon64(String icon64) {
    this.icon64 = icon64;
  }

  /**
   * Get the xml driven flag.
   *
   * @return the xml driven flag
   */
  public boolean isXmlDriven() {
    return xmlDriven == 'y' || xmlDriven == 'Y';
  }

  /**
   * Set the xml driven flag.
   *
   * @param xmlDriven the xml driven flag
   */
  public void setXmlDriven(boolean xmlDriven) {
    this.xmlDriven = (xmlDriven) ? 'Y' : 'N';
  }

  /**
   * Determine whether or not to alter the names of the
   * data store entities to avoid db reserved word conflicts.
   *
   * @return true if the view is a system view
   */
  public boolean alterNames() {
    return alterNames == 1;
  }

  /**
   * Set the flag which indicates whether or not to alter the names of the
   * data store entities to avoid db reserved word conflicts.
   *
   * @param alterNames  the alterNames flag; true if the data store names should be altered
   */
  public void setAlterNames(boolean alterNames) {
    this.alterNames = alterNames ? 1 : 0;
  }

  /**
   * Get the instance properties.
   *
   * @return the instance properties
   */
  public Collection<ViewInstancePropertyEntity> getProperties() {
    return properties;
  }

  /**
   * Add a property value to this instance.
   *
   * @param key   the property key
   * @param value the property value
   */
  public void putProperty(String key, String value) {
    removeProperty(key);
    ViewInstancePropertyEntity viewInstancePropertyEntity = new ViewInstancePropertyEntity();
    viewInstancePropertyEntity.setViewName(viewName);
    viewInstancePropertyEntity.setViewInstanceName(name);
    viewInstancePropertyEntity.setName(key);
    viewInstancePropertyEntity.setValue(value);
    viewInstancePropertyEntity.setViewInstanceEntity(this);
    properties.add(viewInstancePropertyEntity);
  }

  /**
   * Remove the property identified by the given key from this instance.
   *
   * @param key the key
   */
  public void removeProperty(String key) {
    ViewInstancePropertyEntity entity = getProperty(key);
    if (entity != null) {
      properties.remove(entity);
    }
  }

  /**
   * Get the instance property entity for the given key.
   *
   * @param key the key
   * @return the instance property entity identified by the given key
   */
  public ViewInstancePropertyEntity getProperty(String key) {
    for (ViewInstancePropertyEntity viewInstancePropertyEntity : properties) {
      if (viewInstancePropertyEntity.getName().equals(key)) {
        return viewInstancePropertyEntity;
      }
    }
    return null;
  }

  /**
   * Set the collection of instance property entities.
   *
   * @param properties the collection of instance property entities
   */
  public void setProperties(Collection<ViewInstancePropertyEntity> properties) {
    this.properties = properties;
  }

  /**
   * Get the instance data.
   *
   * @return the instance data
   */
  public Collection<ViewInstanceDataEntity> getData() {
    return data;
  }

  /**
   * Set the collection of instance data entities.
   *
   * @param data the collection of instance data entities
   */
  public void setData(Collection<ViewInstanceDataEntity> data) {
    this.data = data;
  }

  /**
   * Get the view entities.
   *
   * @return the view entities
   */
  public Collection<ViewEntityEntity> getEntities() {
    return entities;
  }

  /**
   * Set the view entities.
   *
   * @param entities the view entities
   */
  public void setEntities(Collection<ViewEntityEntity> entities) {
    this.entities = entities;
  }

  /**
   * Associate the given instance data value with the given key.
   *
   * @param key   the key
   * @param value the value
   */
  public void putInstanceData(String key, String value) {
    removeInstanceData(key);
    ViewInstanceDataEntity viewInstanceDataEntity = new ViewInstanceDataEntity();
    viewInstanceDataEntity.setViewName(viewName);
    viewInstanceDataEntity.setViewInstanceName(name);
    viewInstanceDataEntity.setName(key);
    viewInstanceDataEntity.setUser(getCurrentUserName());
    viewInstanceDataEntity.setValue(value);
    viewInstanceDataEntity.setViewInstanceEntity(this);
    data.add(viewInstanceDataEntity);
  }

  /**
   * Remove the instance data entity associated with the given key.
   *
   * @param key the key
   */
  public void removeInstanceData(String key) {
    ViewInstanceDataEntity entity = getInstanceData(key);
    if (entity != null) {
      data.remove(entity);
    }
  }

  /**
   * Get the instance data entity for the given key.
   *
   * @param key the key
   * @return the instance data entity associated with the given key
   */
  public ViewInstanceDataEntity getInstanceData(String key) {
    String user = getCurrentUserName();

    for (ViewInstanceDataEntity viewInstanceDataEntity : data) {
      if (viewInstanceDataEntity.getName().equals(key) &&
        viewInstanceDataEntity.getUser().equals(user)) {
        return viewInstanceDataEntity;
      }
    }
    return null;
  }

  /**
   * Get the parent view entity.
   *
   * @return the parent view entity
   */
  public ViewEntity getViewEntity() {
    return view;
  }

  /**
   * Set the parent view entity.
   *
   * @param view the parent view entity
   */
  public void setViewEntity(ViewEntity view) {
    this.view = view;
  }

  /**
   * Get the associated configuration.
   *
   * @return the configuration
   */
  public InstanceConfig getConfiguration() {
    return instanceConfig;
  }

  /**
   * Add a resource provider for the given resource type.
   *
   * @param type     the resource type
   * @param provider the resource provider
   */
  public void addResourceProvider(Resource.Type type, ResourceProvider provider) {
    resourceProviders.put(type, provider);
  }

  /**
   * Get the resource provider for the given resource type.
   *
   * @param type the resource type
   * @return the resource provider
   */
  public ResourceProvider getResourceProvider(Resource.Type type) {
    return resourceProviders.get(type);
  }

  /**
   * Get the resource provider for the given resource type name (scoped to this view).
   *
   * @param type the resource type name
   * @return the resource provider
   */
  public ResourceProvider getResourceProvider(String type) {
    String typeName = view.getQualifiedResourceTypeName(type);
    return resourceProviders.get(Resource.Type.valueOf(typeName));
  }

  /**
   * Add a service for the given plural resource name.
   *
   * @param pluralName the plural resource name
   * @param service    the service
   */
  public void addService(String pluralName, Object service) {
    services.put(pluralName, service);
  }

  /**
   * Get the service associated with the given plural resource name.
   *
   * @param pluralName the plural resource name
   * @return the service associated with the given name
   */
  public Object getService(String pluralName) {
    return services.get(pluralName);
  }

  /**
   * Get the context path for the UI for this view.
   *
   * @return the context path
   */
  public String getContextPath() {
    return getContextPath(view.getCommonName(), view.getVersion(), getName());
  }

  /**
   * Get the context path for a view instance with the given names.
   *
   * @param viewName         the view name
   * @param viewInstanceName the instance name
   * @return the context path
   */
  public static String getContextPath(String viewName, String version, String viewInstanceName) {
    return AmbariAuthorizationFilter.VIEWS_CONTEXT_PATH_PREFIX + viewName + "/" + version + "/" + viewInstanceName;
  }

  /**
   * Get the current user name.
   *
   * @return the current user name; empty String if user is not known
   */
  public String getUsername() {
    return securityHelper.getCurrentUserName();
  }

  /**
   * Get the admin resource entity.
   *
   * @return the resource entity
   */
  public ResourceEntity getResource() {
    return resource;
  }

  /**
   * Set the admin resource entity.
   *
   * @param resource  the resource entity
   */
  public void setResource(ResourceEntity resource) {
    this.resource = resource;
  }

  /**
   * Get the data migrator instance for view instance.
   *
   * @param dataMigrationContext  the data migration context to inject into migrator instance.
   * @return  the data migrator.
   * @throws ClassNotFoundException  if class defined in the archive could not be loaded
   */
  public ViewDataMigrator getDataMigrator(ViewDataMigrationContext dataMigrationContext)
      throws ClassNotFoundException {
    if (view != null) {
      if (dataMigrator == null && view.getConfiguration().getDataMigrator() != null) {
        ClassLoader cl = view.getClassLoader();
        dataMigrator = getDataMigrator(view.getConfiguration().getDataMigratorClass(cl),
                                       new ViewContextImpl(view, ViewRegistry.getInstance()),
                                       dataMigrationContext);
      }
    }
    return dataMigrator;
  }

  // get the data migrator class; inject a migration and view contexts
  private static ViewDataMigrator getDataMigrator(Class<? extends ViewDataMigrator> clazz,
                                                  final ViewContext viewContext,
                                                  final ViewDataMigrationContext dataMigrationContext) {
    Injector viewInstanceInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ViewContext.class)
                .toInstance(viewContext);
        bind(ViewDataMigrationContext.class)
            .toInstance(dataMigrationContext);
      }
    });
    return viewInstanceInjector.getInstance(clazz);
  }

  /**
   * Validate the state of the instance.
   *
   * @param viewEntity the view entity to which this instance will be bound
   * @param context the validation context
   *
   * @throws ValidationException if the instance can not be validated
   */
  public void validate(ViewEntity viewEntity, Validator.ValidationContext context) throws ValidationException {
    InstanceValidationResultImpl result = getValidationResult(viewEntity, context);
    if (!result.isValid()) {
      throw new ValidationException(result.toJson());
    }
  }

  /**
   * Get the validation the state of the instance.
   *
   * @param viewEntity the view entity to which this instance will be bound
   * @param context the validation context
   *
   * @return the instance validation result
   */
  public InstanceValidationResultImpl getValidationResult(ViewEntity viewEntity, Validator.ValidationContext context)
      throws IllegalStateException {

    Map<String, ValidationResult> propertyResults = new HashMap<>();

    if (context.equals(Validator.ValidationContext.PRE_CREATE) ||
        context.equals(Validator.ValidationContext.PRE_UPDATE)) {

      // make sure that there is an instance property value defined
      // for each required view parameter
      Set<String> requiredParameterNames = new HashSet<>();
      for (ViewParameterEntity parameter : viewEntity.getParameters()) {
        if (parameter.isRequired()) {
          // Don't enforce 'required' validation for cluster config parameters since
          // the value will be obtained through cluster association, not user input
          if (parameter.getClusterConfig()== null) {
            requiredParameterNames.add(parameter.getName());
          }
        }
      }
      Map<String, String> propertyMap = getPropertyMap();
      for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
        if (entry.getValue() != null) {
          requiredParameterNames.remove(entry.getKey());
        }
      }
      // required but missing instance properties...
      for (String requiredParameterName : requiredParameterNames) {
        propertyResults.put(requiredParameterName,
            new ValidationResultImpl(false,
                "No property values exist for the required parameter " + requiredParameterName + "."));
      }
    }

    ValidationResult instanceResult = null;
    Validator         validator     = viewEntity.getValidator();

    // if the view provides its own validator, run it
    if (validator != null) {
      instanceResult = validator.validateInstance(this, context);
      for ( String property : getPropertyMap().keySet()) {
        if (!propertyResults.containsKey(property)) {
          propertyResults.put(property,
              ValidationResultImpl.create(validator.validateProperty(property, this, context)));
        }
      }
    }
    return new InstanceValidationResultImpl(ValidationResultImpl.create(instanceResult), propertyResults);
  }


  // ----- helper methods ----------------------------------------------------

  // get the current user name
  public String getCurrentUserName() {
    String currentUserName = getUsername();

    return currentUserName == null || currentUserName.length() == 0 ?
      " " : currentUserName;
  }

  /**
   * Set the security helper.
   *
   * @param securityHelper the helper
   */
  protected void setSecurityHelper(SecurityHelper securityHelper) {
    this.securityHelper = securityHelper;
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ViewInstanceEntity that = (ViewInstanceEntity) o;

    return name.equals(that.name) && viewName.equals(that.viewName);
  }

  @Override
  public int hashCode() {
    int result = viewName.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }

  /**
   * Get the view URL associated with the instance
   * @return
     */
  public ViewURLEntity getViewUrl() {
    return viewUrl;
  }

  /**
   * Set the view URL associated with the instance
   * @param viewUrl
     */
  public void setViewUrl(ViewURLEntity viewUrl) {
    this.viewUrl = viewUrl;
  }

  /**
   * Remove the URL associated with this entity
   */
  public void clearUrl() {
    viewUrl = null;
  }

  //----- ViewInstanceVersionDTO inner class --------------------------------------------------

  /**
   * Keeps information about view name, version and instance name.
   */
  public static class ViewInstanceVersionDTO {

    /**
     * View name.
     */
    private final String viewName;

    /**
     * View version.
     */
    private final String version;

    /**
     * View instance name.
     */
    private final String instanceName;

    /**
     * Constructor.
     *
     * @param viewName view name
     * @param version view version
     * @param instanceName view instance name
     */
    public ViewInstanceVersionDTO(String viewName, String version, String instanceName) {
      this.viewName = viewName;
      this.version = version;
      this.instanceName = instanceName;
    }

    /**
     * Get the view name.
     *
     * @return the view name
     */
    public String getViewName() {
      return viewName;
    }

    /**
     * Get the view version.
     *
     * @return the view version
     */
    public String getVersion() {
      return version;
    }

    /**
     * Get the view instance name.
     *
     * @return the view instance name
     */
    public String getInstanceName() {
      return instanceName;
    }
  }

  @Override
  public String toString() {
    return "ViewInstanceEntity{" +
        "viewInstanceId=" + viewInstanceId +
        ", viewName='" + viewName + '\'' +
        ", name='" + name + '\'' +
        ", label='" + label + '\'' +
        '}';
  }
}
