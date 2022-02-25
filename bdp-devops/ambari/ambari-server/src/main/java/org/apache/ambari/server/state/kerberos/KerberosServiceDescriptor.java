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
package org.apache.ambari.server.state.kerberos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * KerberosServiceDescriptor is an implementation of an AbstractKerberosDescriptorContainer that
 * encapsulates an Ambari service and it components.
 * <p/>
 * A KerberosServiceDescriptor has the following properties:
 * <ul>
 * <li>name</li>
 * <li>components</li>
 * <li>identities ({@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer})</li>
 * <li>configurations ({@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer})</li>
 * </ul>
 * <p/>
 * The following (pseudo) JSON Schema will yield a valid KerberosServiceDescriptor
 * <pre>
 *   {
 *      "$schema": "http://json-schema.org/draft-04/schema#",
 *      "title": "KerberosServiceDescriptor",
 *      "description": "Describes an Ambari service",
 *      "type": "object",
 *      "preconfigure": "boolean",
 *      "properties": {
 *        "name": {
 *          "description": "An identifying name for this service descriptor.",
 *          "type": "string"
 *        },
 *        "components": {
 *          "description": "A list of Ambari component descriptors",
 *          "type": "array",
 *          "items": {
 *            "title": "KerberosComponentDescriptor"
 *            "type": "{@link org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor}"
 *          }
 *        },
 *        "identities": {
 *          "description": "A list of Kerberos identity descriptors",
 *          "type": "array",
 *          "items": {
 *            "title": "KerberosIdentityDescriptor"
 *            "type": "{@link org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor}"
 *          }
 *        },
 *        "configurations": {
 *          "description": "A list of relevant configuration blocks",
 *          "type": "array",
 *          "items": {
 *            "title": "KerberosConfigurationDescriptor"
 *            "type": "{@link org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor}"
 *          }
 *        }
 *      }
 *   }
 * </pre>
 * <p/>
 * In this implementation,
 * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor#name} will hold the
 * KerberosServiceDescriptor#name value.
 */
public class KerberosServiceDescriptor extends AbstractKerberosDescriptorContainer {

  public static final String KEY_NAME = "name";
  static final String KEY_PRECONFIGURE = "preconfigure";
  static final String KEY_COMPONENTS = Type.COMPONENT.getDescriptorPluralName();

  /**
   * A Map of the components contained within this KerberosServiceDescriptor
   */
  private Map<String, KerberosComponentDescriptor> components;

  /**
   * A Boolean value indicating whether this service should be pre-configured (true) or not.
   */
  private Boolean preconfigure = null;

  /**
   * Creates a new KerberosServiceDescriptor
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor} for the JSON
   * Schema that may be used to generate this map.
   *
   * @param data a Map of values use to populate the data for the new instance
   * @see org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor
   */
  KerberosServiceDescriptor(Map<?, ?> data) {
    // The name for this KerberosServiceDescriptor is stored in the "name" entry in the map
    // This is not automatically set by the super classes.
    this(getStringValue(data, KEY_NAME), data);
  }

  @Override
  public Collection<? extends AbstractKerberosDescriptorContainer> getChildContainers() {
    return (components == null) ? null : Collections.unmodifiableCollection(components.values());
  }

  @Override
  public AbstractKerberosDescriptorContainer getChildContainer(String name) {
    return getComponent(name);
  }

  /**
   * Creates a new KerberosServiceDescriptor
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor} for the JSON
   * Schema that may be used to generate this map.
   *
   * @param name a String declaring this service's name
   * @param data a Map of values use to populate the data for the new instance
   * @see org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor
   */
  KerberosServiceDescriptor(String name, Map<?, ?> data) {
    super(data);

    // This is not automatically set by the super classes.
    setName(name);

    if (data != null) {
      Object list = data.get(KEY_COMPONENTS);
      if (list instanceof Collection) {
        // Assume list is Collection<Map<String, Object>>
        for (Object item : (Collection) list) {
          if (item instanceof Map) {
            putComponent(new KerberosComponentDescriptor((Map<?, ?>) item));
          }
        }
      }

      setPreconfigure(getBooleanValue(data, KEY_PRECONFIGURE));
    }
  }

  /**
   * Returns a Map of the KerberosComponentDescriptors related to this KerberosServiceDescriptor
   *
   * @return a Map of String to KerberosComponentDescriptor
   */
  public Map<String, KerberosComponentDescriptor> getComponents() {
    return components;
  }

  /**
   * Returns the KerberosComponentDescriptors with the specified name
   *
   * @param name the name of the component for which to retrieve a descriptor
   * @return the KerberosComponentDescriptor for the requested component or null if not found
   */
  public KerberosComponentDescriptor getComponent(String name) {
    return ((name == null) || (components == null)) ? null : components.get(name);
  }

  /**
   * Adds or replaces a KerberosComponentDescriptor
   * <p/>
   * If a KerberosComponentDescriptor with the same name already exists in the components Map, it
   * will be replaced; else a new entry will be made.
   *
   * @param component the KerberosComponentDescriptor to put
   */
  public void putComponent(KerberosComponentDescriptor component) {
    if (component != null) {
      String name = component.getName();

      if (name == null) {
        throw new IllegalArgumentException("The component name must not be null");
      }

      if (components == null) {
        components = new TreeMap<>();
      }

      components.put(name, component);
      component.setParent(this);
    }
  }

  /**
   * Updates this KerberosServiceDescriptor with data from another KerberosServiceDescriptor
   * <p/>
   * Properties will be updated if the relevant updated values are not null.
   *
   * @param updates the KerberosServiceDescriptor containing the updated values
   */
  public void update(KerberosServiceDescriptor updates) {
    if (updates != null) {
      Map<String, KerberosComponentDescriptor> updatedComponents = updates.getComponents();
      if (updatedComponents != null) {
        for (Map.Entry<String, KerberosComponentDescriptor> entry : updatedComponents.entrySet()) {
          KerberosComponentDescriptor existing = getComponent(entry.getKey());
          if (existing == null) {
            putComponent(entry.getValue());
          } else {
            existing.update(entry.getValue());
          }
        }
      }
    }

    super.update(updates);
  }

  /**
   * Indicate whether this service should be preconfigured when determining configurations.
   *
   * @return true, to preconfigure; false, otherwise
   */
  public boolean shouldPreconfigure() {
    return Boolean.TRUE.equals(preconfigure);
  }

  /**
   * Sets whether this service should be preconfigured when determining configurations or not.
   *
   * @param preconfigure true, to preconfigure; false, otherwise
   */
  public void setPreconfigure(Boolean preconfigure) {
    this.preconfigure = preconfigure;
  }

  /**
   * Gets the requested AbstractKerberosDescriptor implementation using a type name and a relevant
   * descriptor name.
   * <p/>
   * This implementation handles component descriptors and relies on the
   * AbstractKerberosDescriptorContainer implementation to handle other types.
   *
   * @param type a String indicating the type of the requested descriptor
   * @param name a String indicating the name of the requested descriptor
   * @return a AbstractKerberosDescriptor representing the requested descriptor or null if not found
   */
  @Override
  protected AbstractKerberosDescriptor getDescriptor(Type type, String name) {
    if (Type.COMPONENT == type) {
      return getComponent(name);
    } else {
      return super.getDescriptor(type, name);
    }
  }

  /**
   * Creates a Map of values that can be used to create a copy of this KerberosServiceDescriptor
   * or generate the JSON structure described in
   * {@link org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor}
   *
   * @return a Map of values for this KerberosServiceDescriptor
   * @see org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor
   */
  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();

    if (components != null) {
      List<Map<String, Object>> list = new ArrayList<>();
      for (KerberosComponentDescriptor component : components.values()) {
        list.add(component.toMap());
      }
      map.put(KEY_COMPONENTS, list);
    }

    if (preconfigure != null) {
      map.put(KEY_PRECONFIGURE, preconfigure.toString());
    }

    return map;
  }

  public List<KerberosIdentityDescriptor> getComponentIdentities(String componentName) {
    return getComponent(componentName) != null
        ? nullToEmpty(getComponent(componentName).getIdentities())
        : Collections.emptyList();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .appendSuper(super.hashCode())
        .append(components)
        .append(preconfigure)
        .toHashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }

    if (!(object instanceof KerberosServiceDescriptor)) {
      return false;
    }

    KerberosServiceDescriptor that = (KerberosServiceDescriptor) object;
    return new EqualsBuilder()
        .appendSuper(super.equals(object))
        .append(components, components)
        .append(preconfigure, that.preconfigure)
        .isEquals();
  }
}

