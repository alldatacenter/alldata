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

package org.apache.ambari.server.view.configuration;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ambari.server.view.DefaultMasker;
import org.apache.ambari.view.Masker;
import org.apache.ambari.view.View;
import org.apache.ambari.view.migration.ViewDataMigrator;
import org.apache.ambari.view.validation.Validator;
import org.apache.commons.lang.StringUtils;

/**
 * View configuration.
 */
@XmlRootElement(name="view")
@XmlAccessorType(XmlAccessType.FIELD)
public class ViewConfig {
  /**
   * The unique view name.
   */
  private String name;

  /**
   * The public view name.
   */
  private String label;

  /**
   * The view description.
   */
  private String description;

  /**
   * The view version.
   */
  private String version;

  /**
   * The view version.
   */
  private String build;

  /**
   * The minimum Ambari version.
   */
  @XmlElement(name="min-ambari-version")
  private String minAmbariVersion;

  /**
   * The maximum Ambari version.
   */
  @XmlElement(name="max-ambari-version")
  private String maxAmbariVersion;

  /**
   * The icon path in the view archive.
   */
  private String icon;

  /**
   * The big icon path in the view archive.
   */
  private String icon64;

  /**
   * Indicates whether or not this is a system view.
   */
  private boolean system;

  /**
   * The list of extra classpath elements.
   */
  @XmlElementWrapper
  @XmlElement(name="path")
  private List<String> classpath;

  /**
   * The main view class name.
   */
  @XmlElement(name="view-class")
  private String view;

  /**
   * The view class.
   */
  private Class<? extends View> viewClass = null;

  /**
   * The main view class name.
   */
  @XmlElement(name="data-migrator-class")
  private String dataMigrator;

  /**
   * The main view class name.
   */
  @XmlElement(name="data-version")
  private String dataVersion;

  /**
   * Enable views to display cluster options in case of no cluster config properties.
   */
  @XmlElement(name="cluster-config-options")
  private String clusterConfigOptions;

  /**
   * The view class.
   */
  private Class<? extends ViewDataMigrator> dataMigratorClass = null;

  /**
   * The main view class name.
   */
  @XmlElement(name="validator-class")
  private String validator;

  /**
   * The view validator class.
   */
  private Class<? extends Validator> validatorClass = null;

  /**
   * The masker class name for parameters.
   */
  @XmlElement(name="masker-class")
  private String masker;

  /**
   * The mask class.
   */
  private Class<? extends Masker> maskerClass = null;

  /**
   * The list of view parameters.
   */
  @XmlElement(name="parameter")
  private List<ParameterConfig> parameters;

  /**
   * The list of view resources.
   */
  @XmlElement(name="resource")
  private List<ResourceConfig> resources;

  /**
   * The view instance auto create configuration.
   */
  @XmlElement(name="auto-instance")
  private AutoInstanceConfig autoInstance;

  /**
   * The list of view instances.
   */
  @XmlElement(name="instance")
  private List<InstanceConfig> instances;

  /**
   * The view persistence configuration.
   */
  @XmlElement(name="persistence")
  private PersistenceConfig persistence;

  /**
   * The list of view parameters.
   */
  @XmlElement(name="permission")
  private List<PermissionConfig> permissions;

  /**
   * Get the unique name.
   *
   * @return the view name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the public view name.
   *
   * @return the view label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Get the view description.
   *
   * @return the view description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the view version.
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Get the view build number.
   *
   * @return the build number
   */
  public String getBuild() {
    return build;
  }

  /**
   * Get the minimum version of Ambari required to run this view.
   *
   * @return the minimum Ambari version
   */
  public String getMinAmbariVersion() {
    return minAmbariVersion;
  }

  /**
   * Get the maximum version of Ambari that can run this view.
   *
   * @return the maximum Ambari version
   */
  public String getMaxAmbariVersion() {
    return maxAmbariVersion;
  }

  /**
   * Get the icon path in the view archive.
   *
   * @return the icon path
   */
  public String getIcon() {
    return icon;
  }

  /**
   * Get the big icon path in the view archive.
   *
   * @return the big icon path
   */
  public String getIcon64() {
    return icon64;
  }

  /**
   * Determine whether or not this is a system view.
   *
   * @return true if this is a system view
   */
  public boolean isSystem() {
    return system;
  }

  /**
   * Get the extra classpath as a comma separated path of filenames or URLs pointing to
   * directories or jar files.
   *
   * @return the extra classpath
   */
  public String getExtraClasspath() {
    return classpath == null ? null : StringUtils.join(classpath, ",");
  }

  /**
   * Get the view class name.
   *
   * @return the view class name
   */
  public String getView() {
    return view;
  }

  /**
   * Get the view class.
   *
   * @param cl the class loader
   *
   * @return the view class
   *
   * @throws ClassNotFoundException if the class can not be loaded
   */
  public Class<? extends View> getViewClass(ClassLoader cl) throws ClassNotFoundException {
    if (viewClass == null) {
      viewClass = cl.loadClass(view).asSubclass(View.class);
    }
    return viewClass;
  }

  /**
   * Get the view class name.
   *
   * @return the view class name
   */
  public String getDataMigrator() {
    return dataMigrator;
  }

  /**
   * Get the view class.
   *
   * @param cl the class loader
   *
   * @return the view class
   *
   * @throws ClassNotFoundException if the class can not be loaded
   */
  public Class<? extends ViewDataMigrator> getDataMigratorClass(ClassLoader cl) throws ClassNotFoundException {
    if (dataMigratorClass == null) {
      dataMigratorClass = cl.loadClass(dataMigrator).asSubclass(ViewDataMigrator.class);
    }
    return dataMigratorClass;
  }

  /**
   * Get the view data version. If not specified, data version is 0.
   *
   * @return the data version
   */
  public int getDataVersion() {
    return (dataVersion == null) ? 0 : Integer.parseInt(dataVersion);
  }

  /**
   * Get the view validator class name.
   *
   * @return the view validator class name
   */
  public String getValidator() {
    return validator;
  }

  /**
   * Get the view validator class.
   *
   * @param cl the class loader
   *
   * @return the view validator class
   *
   * @throws ClassNotFoundException if the class can not be loaded
   */
  public Class<? extends Validator> getValidatorClass(ClassLoader cl) throws ClassNotFoundException {
    if (validatorClass == null) {
      validatorClass = cl.loadClass(validator).asSubclass(Validator.class);
    }
    return validatorClass;
  }

  /**
   * Get the masker class name.
   * @return the masker class name
   */
  public String getMasker() {
    return masker;
  }

  /**
   * Get the masker class.
   *
   * @param cl the class loader
   *
   * @return the masker class
   *
   * @throws ClassNotFoundException if the class can not be loaded
   */
  public Class<? extends Masker> getMaskerClass(ClassLoader cl) throws ClassNotFoundException {
    if (maskerClass == null) {
      if (StringUtils.isBlank(masker)) {
        maskerClass = DefaultMasker.class;
      } else {
        maskerClass = cl.loadClass(masker).asSubclass(Masker.class);
      }
    }
    return maskerClass;
  }

  /**
   * Get the list of view parameters.
   *
   * @return the list of parameters
   */
  public List<ParameterConfig> getParameters() {
    return parameters == null ? Collections.emptyList() : parameters;
  }

  /**
   * Get the list of view resources.
   *
   * @return return the list of resources
   */
  public List<ResourceConfig> getResources() {
    return resources == null ? Collections.emptyList() : resources;
  }

  /**
   * Get the configuration for the view instance auto create.
   *
   * @return the view instance auto create; null if no auto instance is specified
   */
  public AutoInstanceConfig getAutoInstance() {
    return autoInstance;
  }

  /**
   * Get the list of view instances.
   *
   * @return the list of view instances
   */
  public List<InstanceConfig> getInstances() {
    return instances == null ? Collections.emptyList() : instances;
  }

  /**
   * Get the view persistence configuration.
   *
   * @return the view persistence configuration
   */
  public PersistenceConfig getPersistence() {
    return persistence;
  }

  /**
   * Get the list of custom permissions defined for the view.
   *
   * @return the list of custom permissions
   */
  public List<PermissionConfig> getPermissions() {
    return permissions == null ? Collections.emptyList() : permissions;
  }

  /**
   * Enables views to display cluster options in case of no cluster config properties
   *
   * @return clusterConfigOptions
   */
  public String getClusterConfigOptions() {
    return clusterConfigOptions;
  }
}
