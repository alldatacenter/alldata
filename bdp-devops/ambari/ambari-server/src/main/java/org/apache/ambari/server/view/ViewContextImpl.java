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

package org.apache.ambari.server.view;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.configuration.ParameterConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.events.EventImpl;
import org.apache.ambari.server.view.persistence.DataStoreImpl;
import org.apache.ambari.server.view.persistence.DataStoreModule;
import org.apache.ambari.server.view.validation.ValidationException;
import org.apache.ambari.view.AmbariStreamProvider;
import org.apache.ambari.view.ClusterType;
import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.ImpersonatorSetting;
import org.apache.ambari.view.MaskException;
import org.apache.ambari.view.Masker;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.SecurityException;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.URLConnectionProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewController;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.ambari.view.events.Event;
import org.apache.ambari.view.events.Listener;
import org.apache.directory.api.util.Strings;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

/**
 * View context implementation.
 */
public class ViewContextImpl implements ViewContext, ViewController {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(ViewContextImpl.class);

  public static final String HADOOP_SECURITY_AUTH_TO_LOCAL = "hadoop.security.auth_to_local";
  public static final String CORE_SITE = "core-site";
  public static final String HDFS_AUTH_TO_LOCAL = "hdfs.auth_to_local";

  /**
   * The associated view definition.
   */
  private final ViewEntity viewEntity;

  /**
   * The associated view definition.
   */
  private final ViewInstanceEntity viewInstanceEntity;

  /**
   * The view registry.
   */
  private final ViewRegistry viewRegistry;

  /**
   * The data store.
   */
  private DataStore dataStore = null;

  /**
   * Masker for properties.
   */
  private Masker masker;

  /**
   * The velocity context used to evaluate property templates.
   */
  private final VelocityContext velocityContext;


  // ---- Constructors -------------------------------------------------------

  /**
   * Construct a view context from the given view instance entity.
   *
   * @param viewInstanceEntity  the view entity
   * @param viewRegistry        the view registry
   */
  public ViewContextImpl(ViewInstanceEntity viewInstanceEntity, ViewRegistry viewRegistry) {
    this(viewInstanceEntity.getViewEntity(), viewInstanceEntity, viewRegistry);
  }

  /**
   * Construct a view context from the given view entity.
   *
   * @param viewEntity    the view entity
   * @param viewRegistry  the view registry
   */
  public ViewContextImpl(ViewEntity viewEntity, ViewRegistry viewRegistry) {
    this(viewEntity, null, viewRegistry);
  }

  private ViewContextImpl(ViewEntity viewEntity, ViewInstanceEntity viewInstanceEntity, ViewRegistry viewRegistry) {
    this.viewEntity         = viewEntity;
    this.viewInstanceEntity = viewInstanceEntity;
    this.viewRegistry       = viewRegistry;
    this.masker             = getMasker(viewEntity.getClassLoader(), viewEntity.getConfiguration());
    this.velocityContext    = initVelocityContext();
  }

  // ----- ViewContext -------------------------------------------------------

  @Override
  public String getViewName() {
    return viewEntity.getCommonName();
  }

  @Override
  public ViewDefinition getViewDefinition() {
    return viewEntity;
  }

  @Override
  public String getInstanceName() {
    return viewInstanceEntity == null ? null : viewInstanceEntity.getName();
  }

  @Override
  public ViewInstanceDefinition getViewInstanceDefinition() {
    return viewInstanceEntity;
  }

  @Override
  public Map<String, String> getProperties() {
    if (viewInstanceEntity == null) {
      return null;
    } else {
      return Collections.unmodifiableMap(getPropertyValues());
    }
  }

  @Transactional
  @Override
  public void putInstanceData(String key, String value) {
    checkInstance();

    ViewInstanceEntity updateInstance =
        viewRegistry.getViewInstanceEntity(viewInstanceEntity.getViewName(), viewInstanceEntity.getInstanceName());

    if (updateInstance != null) {
      updateInstance.putInstanceData(key, value);

      try {
        viewRegistry.updateViewInstance(updateInstance);
      } catch (SystemException e) {
        String msg = "Caught exception updating the view instance.";
        LOG.error(msg, e);
        throw new IllegalStateException(msg, e);
      } catch (ValidationException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
  }

  @Override
  public String getInstanceData(String key) {
    return viewInstanceEntity == null ? null :
        viewInstanceEntity.getInstanceDataMap().get(key);
  }

  @Override
  public Map<String, String> getInstanceData() {
    return viewInstanceEntity == null ? null :
        Collections.unmodifiableMap(viewInstanceEntity.getInstanceDataMap());
  }

  @Override
  public void removeInstanceData(String key) {
    checkInstance();
    viewRegistry.removeInstanceData(viewInstanceEntity, key);
  }

  @Override
  public String getAmbariProperty(String key) {
    return viewInstanceEntity == null ? null :
        viewInstanceEntity.getViewEntity().getAmbariProperty(key);
  }

  @Override
  public ResourceProvider<?> getResourceProvider(String type) {
    return viewInstanceEntity == null ? null :
        viewInstanceEntity.getResourceProvider(type);
  }

  @Override
  public String getUsername() {
    String shortName = getLoggedinUser();
    try {
      String authToLocalRules = getAuthToLocalRules();
      //Getting ambari server realm. Ideally this should come from user
      String defaultRealm = KerberosUtil.getDefaultRealm();
      if(Strings.isNotEmpty(authToLocalRules) && Strings.isNotEmpty(defaultRealm)){
        synchronized (KerberosName.class){
          KerberosName.setRules(authToLocalRules);
          shortName = new KerberosName(shortName+"@"+defaultRealm).getShortName();
        }
      }
    } catch (InvocationTargetException e) {
      LOG.debug("Failed to get default realm",e);
    }catch (Exception e){
      LOG.warn("Failed to apply auth_to_local rules. "+e.getMessage());
      LOG.debug("Failed to apply auth_to_local rules",e);
    }
    return shortName;
  }

  private String getAuthToLocalRules(){
    Cluster cluster = getCluster();
    String authToLocalRules = null;
    if (cluster != null) {
      authToLocalRules = cluster.getConfigurationValue(CORE_SITE, HADOOP_SECURITY_AUTH_TO_LOCAL);
    }else if(viewInstanceEntity != null) {
      authToLocalRules = viewInstanceEntity.getPropertyMap().get(HDFS_AUTH_TO_LOCAL);
    }
    return authToLocalRules;
  }

  @Override
  public String getLoggedinUser(){
    return viewInstanceEntity != null ? viewInstanceEntity.getUsername() : null;
  }

  @Override
  public void hasPermission(String userName, String permissionName) throws org.apache.ambari.view.SecurityException {

    if (userName == null || userName.length() == 0) {
      throw new SecurityException("No user name specified.");
    }

    if (permissionName == null || permissionName.length() == 0) {
      throw new SecurityException("No permission name specified.");
    }

    if (viewInstanceEntity == null) {
      throw new SecurityException("There is no instance associated with the view context");
    }

    PermissionEntity permissionEntity = viewEntity.getPermission(permissionName);

    if (permissionEntity == null) {
      throw new SecurityException("The permission " + permissionName + " is not defined for " + viewEntity.getName());
    }

    if (!viewRegistry.hasPermission(permissionEntity, viewInstanceEntity.getResource(), userName)) {
      throw new SecurityException("The user " + userName + " has not been granted permission " + permissionName);
    }
  }

  @Override
  public org.apache.ambari.view.URLStreamProvider getURLStreamProvider() {
    return viewRegistry.createURLStreamProvider(this);
  }

  @Override
  public URLConnectionProvider getURLConnectionProvider() {
    return viewRegistry.createURLStreamProvider(this);
  }

  @Override
  public synchronized AmbariStreamProvider getAmbariStreamProvider() {
    return viewRegistry.createAmbariStreamProvider();
  }

  @Override
  public AmbariStreamProvider getAmbariClusterStreamProvider() {

    Long clusterHandle = viewInstanceEntity.getClusterHandle();
    ClusterType clusterType = viewInstanceEntity.getClusterType();

    AmbariStreamProvider clusterStreamProvider = null;

    if(clusterHandle != null && clusterType == ClusterType.LOCAL_AMBARI){

      clusterStreamProvider = getAmbariStreamProvider();

    } else if(clusterHandle != null && clusterType == ClusterType.REMOTE_AMBARI){

      clusterStreamProvider = viewRegistry.createRemoteAmbariStreamProvider(clusterHandle);

    }

    return clusterStreamProvider;
  }

  @Override
  public synchronized DataStore getDataStore() {
    if (viewInstanceEntity != null) {
      if (dataStore == null) {
        Injector injector = Guice.createInjector(new DataStoreModule(viewInstanceEntity));
        dataStore = injector.getInstance(DataStoreImpl.class);
      }
    }
    return dataStore;
  }

  @Override
  public Collection<ViewDefinition> getViewDefinitions() {
    return Collections.unmodifiableCollection(viewRegistry.getDefinitions());
  }

  @Override
  public Collection<ViewInstanceDefinition> getViewInstanceDefinitions() {
    Collection<ViewInstanceEntity> instanceDefinitions = new HashSet<>();
    for (ViewEntity viewEntity : viewRegistry.getDefinitions()) {
      instanceDefinitions.addAll(viewRegistry.getInstanceDefinitions(viewEntity));
    }
    return Collections.unmodifiableCollection(instanceDefinitions);
  }

  @Override
  public ViewController getController() {
    return this;
  }

  @Override
  public HttpImpersonatorImpl getHttpImpersonator() {
    return new HttpImpersonatorImpl(this);
  }

  @Override
  public ImpersonatorSetting getImpersonatorSetting() {
    return new ImpersonatorSettingImpl(this);
  }

  @Override
  public Cluster getCluster() {
    return viewRegistry.getCluster(viewInstanceEntity);
  }


  // ----- ViewController ----------------------------------------------------

  @Override
  public void fireEvent(String eventId, Map<String, String> eventProperties) {
    Event event = viewInstanceEntity == null ?
        new EventImpl(eventId, eventProperties, viewEntity) :
        new EventImpl(eventId, eventProperties, viewInstanceEntity);

    viewRegistry.fireEvent(event);
  }

  @Override
  public void registerListener(Listener listener, String viewName) {
    viewRegistry.registerListener(listener, viewName, null);
  }

  @Override
  public void registerListener(Listener listener, String viewName, String viewVersion) {
    viewRegistry.registerListener(listener, viewName, viewVersion);
  }

  @Override
  public void unregisterListener(Listener listener, String viewName) {
    viewRegistry.unregisterListener(listener, viewName, null);
  }

  @Override
  public void unregisterListener(Listener listener, String viewName, String viewVersion) {
    viewRegistry.unregisterListener(listener, viewName, viewVersion);
  }


  // ----- helper methods ----------------------------------------------------

  // check for an associated instance
  private void checkInstance() {
    if (viewInstanceEntity == null) {
      throw new IllegalStateException("No instance is associated with the context.");
    }
  }

  private Masker getMasker(ClassLoader cl, ViewConfig viewConfig) {
    try {
      return viewConfig.getMaskerClass(cl).newInstance();
    } catch (Exception e) {
      throw new InstantiationError("Could not create masker instance.");
    }
  }

  /**
   * Get the property values for the associated view instance.
   *
   * @return the property values for the instance
   */
  private Map<String, String> getPropertyValues() {
    Map<String, String> properties = viewInstanceEntity.getPropertyMap();

    Map<String, ParameterConfig> parameters = new HashMap<>();

    for (ParameterConfig paramConfig : viewEntity.getConfiguration().getParameters()) {
      parameters.put(paramConfig.getName(), paramConfig);
    }

    Cluster cluster = getCluster();

    for (Entry<String, String> entry: properties.entrySet()) {
      String propertyName  = entry.getKey();
      String propertyValue = entry.getValue();

      ParameterConfig parameterConfig = parameters.get(propertyName);

      if (parameterConfig != null) {

        String clusterConfig = parameterConfig.getClusterConfig();
        if (clusterConfig != null && cluster != null) {
          propertyValue = getClusterConfigurationValue(cluster, clusterConfig);
        } else {
          if (parameterConfig.isMasked()) {
            try {
              propertyValue = masker.unmask(propertyValue);
            } catch (MaskException e) {
              LOG.error("Failed to unmask view property", e);
            }
          }
        }
      }
      properties.put(propertyName, evaluatePropertyTemplates(propertyValue));
    }
    return properties;
  }

  /**
   * Get a specified configuration value from the given cluster.
   *
   * @param cluster        the cluster
   * @param clusterConfig  the cluster configuration identifier
   *
   * @return the configuration value or <code>null</code> if the desired configuration can not be found
   */
  private static String getClusterConfigurationValue(Cluster cluster, String clusterConfig) {
    if (clusterConfig != null) {
      String[] parts = clusterConfig.split("/");
      if (parts.length == 2) {
        return cluster.getConfigurationValue(parts[0], parts[1]);
      }
    }
    return null;
  }

  /**
   * Evaluate any templates in the given property value.
   *
   * @param rawValue original string with parameters in formal or shorthand notation
   *
   * @return the evaluated property value
   */
  private String evaluatePropertyTemplates(String rawValue) {
    if (rawValue != null) {
      try {
        Writer templateWriter = new StringWriter();
        Velocity.evaluate(velocityContext, templateWriter, rawValue, rawValue);
        return templateWriter.toString();
      } catch (ParseErrorException e) {
        LOG.warn(String.format("Error during parsing '%s' parameter. Leaving original value.", rawValue));
      }
    }
    return rawValue;
  }

  /**
   * Instantiate and initialize context for parameters processing using Velocity.
   *
   * @return initialized context instance
   */
  private VelocityContext initVelocityContext() {
    VelocityContext context = new VelocityContext();
    context.put("username",
        new ParameterResolver() {
          @Override
          protected String getValue() {
            return viewContext.getUsername();
          }
        });
    context.put("viewName",
        new ParameterResolver() {
          @Override
          protected String getValue() {
            return viewContext.getViewName();
          }
        });
    context.put("instanceName",
        new ParameterResolver() {
          @Override
          protected String getValue() {
            return viewContext.getInstanceName();
          }
        });
    context.put("loggedinUser",
            new ParameterResolver() {
              @Override
              protected String getValue() {
                return viewContext.getLoggedinUser();
              }
            });
    return context;
  }

  // ----- Inner class : ParameterResolver -------------------------------

  /**
   * Represents basic parameter resolver to obtain fields of ViewContext at runtime.
   */
  private abstract class ParameterResolver {

    protected final ViewContext viewContext = ViewContextImpl.this;

    protected abstract String getValue();

    @Override
    public String toString() {
      String value = getValue();
      return value == null ? "" : value;
    }
  }
}
