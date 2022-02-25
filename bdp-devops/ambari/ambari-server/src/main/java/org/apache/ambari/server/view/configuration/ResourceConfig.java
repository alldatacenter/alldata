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

import org.apache.ambari.view.ResourceProvider;

/**
 * View resource configuration.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceConfig {

  /**
   * Constants.
   */
  public static final String EXTERNAL_RESOURCE_PLURAL_NAME = "resources";

  /**
   * The resource name.
   */
  private String name;

  /**
   * The plural name of the resource.
   */
  @XmlElement(name="plural-name")
  private String pluralName;

  /**
   * The id property of the resource.
   */
  @XmlElement(name="id-property")
  private String idProperty;

  /**
   * The list of sub resource names.
   */
  @XmlElement(name="sub-resource-name")
  private List<String> subResourceNames;

  /**
   * The resource provider class name.
   */
  @XmlElement(name="provider-class")
  private String provider;

  /**
   * The resource provider class.
   */
  private Class<? extends ResourceProvider> providerClass = null;

  /**
   * The resource service class name.
   */
  @XmlElement(name="service-class")
  private String service;

  /**
   * The resource service class.
   */
  private Class<?> serviceClass = null;

  /**
   * The resource service class name.
   */
  @XmlElement(name="resource-class")
  private String resource;

  /**
   * The resource service class.
   */
  private Class<?> resourceClass = null;

  /**
   * Get the resource name.
   *
   * @return the resource name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the resource plural name.
   *
   * @return the plural name
   */
  public String getPluralName() {
    return pluralName;
  }

  /**
   * Get the id property of the resource.
   *
   * @return the id property
   */
  public String getIdProperty() {
    return idProperty;
  }

  /**
   * Get the list of sub-resource names.
   *
   * @return the sub-resource names
   */
  public List<String> getSubResourceNames() {
    return subResourceNames == null ? Collections.emptyList() : subResourceNames;
  }

  /**
   * Get the provider class name.
   *
   * @return the provider class name
   */
  public String getProvider() {
    return provider;
  }

  /**
   * Get the service class name.
   *
   * @return the service class name.
   */
  public String getService() {
    return service;
  }

  /**
   * Get the resource class name
   *
   * @return the resource classname
   */
  public String getResource() {
    return resource;
  }

  /**
   * Get the resource provider class.
   *
   * @param cl  the class loader
   *
   * @return the resource provider class
   *
   * @throws ClassNotFoundException if the class can not be loaded
   */
  public Class<? extends ResourceProvider> getProviderClass(ClassLoader cl) throws ClassNotFoundException {
    if (providerClass == null) {
      providerClass = cl.loadClass(provider).asSubclass(ResourceProvider.class);
    }
    return providerClass;
  }

  /**
   * Get the resource service class.
   *
   * @param cl  the class loader
   *
   * @return the resource service class
   *
   * @throws ClassNotFoundException if the class can not be loaded
   */
  public Class<?> getServiceClass(ClassLoader cl) throws ClassNotFoundException {
    if (serviceClass == null) {
      serviceClass = cl.loadClass(service);
    }
    return serviceClass;
  }

  /**
   * Get the resource class.
   *
   * @param cl  the class loader
   *
   * @return the resource class
   *
   * @throws ClassNotFoundException if the class can not be loaded
   */
  public Class<?> getResourceClass(ClassLoader cl) throws ClassNotFoundException {
    if (resourceClass == null) {
      resourceClass = cl.loadClass(resource);
    }
    return resourceClass;
  }

  /**
   * Determine whether the resource is external (does not use the API framework).
   *
   * @return true if the resource is external.
   */
  public boolean isExternal() {
    return resource == null || provider == null;
  }
}
