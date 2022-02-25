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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.commons.lang.StringUtils;

/**
 * KerberosDescriptor is an implementation of an AbstractKerberosDescriptorContainer that
 * encapsulates an entire Kerberos descriptor hierarchy.
 * <p/>
 * A KerberosDescriptor has the following properties:
 * <ul>
 * <li>services</li>
 * <li>properties</li>
 * <li>identities ({@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer})</li>
 * <li>configurations ({@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer})</li>
 * </ul>
 * <p/>
 * The following (pseudo) JSON Schema will yield a valid KerberosDescriptor
 * <pre>
 *   {
 *      "$schema": "http://json-schema.org/draft-04/schema#",
 *      "title": "KerberosDescriptor",
 *      "description": "Describes a Kerberos descriptor",
 *      "type": "object",
 *      "properties": {
 *        "services": {
 *          "description": "A list of service descriptors",
 *          "type": "array",
 *          "items": {
 *            "title": "KerberosServiceDescriptor"
 *            "type": "{@link org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor}"
 *          }
 *        },
 *        "properties": {
 *          "description": "Global properties that can be used in variable replacements",
 *          "type": "object",
 *          "patternProperties": {
 *            "^[\w\.\_]?$": {}"
 *          }
 *        }
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
 * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor#name} will be null
 */
public class KerberosDescriptor extends AbstractKerberosDescriptorContainer {

  public static final String KEY_PROPERTIES = "properties";
  public static final String KEY_SERVICES = Type.SERVICE.getDescriptorPluralName();

  /**
   * A Map of the "global" properties contained within this KerberosDescriptor
   */
  private Map<String, String> properties = null;

  /**
   * A Map of the services contained within this KerberosDescriptor
   */
  private Map<String, KerberosServiceDescriptor> services = null;


  /**
   * Creates an empty KerberosDescriptor
   */
  public KerberosDescriptor() {
    this(null);
  }

  /**
   * Creates a new KerberosDescriptor
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.KerberosDescriptor} for the JSON
   * Schema that may be used to generate this map.
   *
   * @param data a Map of values use to populate the data for the new instance
   * @see org.apache.ambari.server.state.kerberos.KerberosDescriptor
   */
  KerberosDescriptor(Map<?, ?> data) {
    super(data);

    if (data != null) {
      Object list = data.get(KEY_SERVICES);
      if (list instanceof Collection) {
        for (Object item : (Collection) list) {
          if (item instanceof Map) {
            putService(new KerberosServiceDescriptor((Map<?, ?>) item));
          }
        }
      }

      Object map = data.get(KEY_PROPERTIES);
      if (map instanceof Map) {
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
          Object value = entry.getValue();
          putProperty(entry.getKey().toString(), (value == null) ? null : value.toString());
        }
      }
    }
  }

  @Override
  public Collection<? extends AbstractKerberosDescriptorContainer> getChildContainers() {
    return (services == null) ? null : Collections.unmodifiableCollection(services.values());
  }

  @Override
  public AbstractKerberosDescriptorContainer getChildContainer(String name) {
    return getService(name);
  }

  /**
   * Set the KerberosServiceDescriptors in this KerberosDescriptor
   *
   * @param services a Map of String to KerberosServiceDescriptor
   */
  public void setServices(Map<String, KerberosServiceDescriptor> services) {
    this.services = (services == null)
        ? null
        : new TreeMap<>(services);
  }

  /**
   * Returns a Map of the KerberosServiceDescriptors in this KerberosDescriptor
   *
   * @return a Map of String to KerberosServiceDescriptor
   */
  public Map<String, KerberosServiceDescriptor> getServices() {
    return services;
  }

  /**
   * Gets the KerberosServiceDescriptor with the specified name
   *
   * @param name a String declaring the name of the KerberosServiceDescriptor to retrieve
   * @return the requested KerberosServiceDescriptor or null if not found
   */
  public KerberosServiceDescriptor getService(String name) {
    return ((name == null) || (services == null)) ? null : services.get(name);
  }

  /**
   * Adds, replaces, or updates a KerberosServiceDescriptor
   * <p/>
   * If a KerberosServiceDescriptor with the same name does not exist in the services Map, a new
   * entry will be added; else if it one already exists and <code>overwrite</code> is
   * <code>true</code>, it will be replaced; else the exsting entry will be updated.
   *
   * @param service the KerberosServiceDescriptor to put
   */
  public void putService(KerberosServiceDescriptor service) {
    if (service != null) {
      String name = service.getName();

      if (name == null) {
        throw new IllegalArgumentException("The service name must not be null");
      }

      if (services == null) {
        services = new TreeMap<String, KerberosServiceDescriptor>();
      }

      KerberosServiceDescriptor existing = services.get(name);
      if (existing == null) {
        services.put(name, service);
        // Set the service's parent to this KerberosDescriptor
        service.setParent(this);
      } else {
        existing.update(service);
      }
    }
  }

  /**
   * Set the Map of properties for this KerberosDescriptor
   *
   * @param properties a Map of String to String values
   */
  public void setProperties(Map<String, String> properties) {
    this.properties = (properties == null)
        ? null
        : new TreeMap<>(properties);
  }

  /**
   * Gets the Map of properties for this KerberosDescriptor
   *
   * @return a Map of String to String values
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Gets the value of the property with the specified name
   *
   * @param name a String declaring the name of the property to retrieve
   * @return a String or null if the property was not found
   */
  public String getProperty(String name) {
    return ((name == null) || (properties == null)) ? null : properties.get(name);
  }

  /**
   * Adds or updates a property value
   * <p/>
   * If a property exists with the specified name, replaces its value; else adds a new entry.
   *
   * @param name  a String declaring the name of the property to put
   * @param value a String containing the value of the property to put
   */
  public void putProperty(String name, String value) {
    if (name == null) {
      throw new IllegalArgumentException("The property name must not be null");
    }

    if (properties == null) {
      properties = new TreeMap<>();
    }

    properties.put(name, value);
  }

  /**
   * Updates this KerberosDescriptor with data from another KerberosDescriptor
   * <p/>
   * Properties will be updated if the relevant updated values are not null.
   *
   * @param updates the KerberosDescriptor containing the updated values
   * @return this {@code KerberosDescriptor} for convenience
   */
  public KerberosDescriptor update(KerberosDescriptor updates) {
    if (updates != null) {
      Map<String, KerberosServiceDescriptor> updatedServiceDescriptors = updates.getServices();
      if (updatedServiceDescriptors != null) {
        for (Map.Entry<String, KerberosServiceDescriptor> entry : updatedServiceDescriptors.entrySet()) {
          putService(entry.getValue());
        }
      }

      Map<String, String> updatedProperties = updates.getProperties();
      if (updatedProperties != null) {
        for (Map.Entry<String, String> entry : updatedProperties.entrySet()) {
          putProperty(entry.getKey(), entry.getValue());
        }
      }
    }

    super.update(updates);

    return this;
  }

  /**
   * Gets the requested AbstractKerberosDescriptor implementation using a type name and a relevant
   * descriptor name.
   * <p/>
   * This implementation handles service descriptors and delegates to the AbstractKerberosDescriptor
   * implementation to handle component and identity types.
   *
   * @param type a String indicating the type of the requested descriptor
   * @param name a String indicating the name of the requested descriptor
   * @return a AbstractKerberosDescriptor representing the requested descriptor or null if not found
   */
  @Override
  protected AbstractKerberosDescriptor getDescriptor(Type type, String name) {
    if (Type.SERVICE == type) {
      return getService(name);
    } else {
      return super.getDescriptor(type, name);
    }
  }

  /**
   * Creates a Map of values that can be used to create a copy of this KerberosDescriptor
   * or generate the JSON structure described in
   * {@link org.apache.ambari.server.state.kerberos.KerberosDescriptor}
   *
   * @return a Map of values for this KerberosDescriptor
   * @see org.apache.ambari.server.state.kerberos.KerberosDescriptor
   */
  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();

    if (services != null) {
      List<Map<String, Object>> list = new ArrayList<>();
      for (KerberosServiceDescriptor service : services.values()) {
        list.add(service.toMap());
      }
      map.put(KEY_SERVICES, list);
    }

    if (properties != null) {
      map.put(KEY_PROPERTIES, new TreeMap<>(properties));
    }

    return map;
  }

  /**
   * Sets the parent (or container) of this descriptor
   * <p/>
   * This implementation prevents the parent from being externally set by always throwing an
   * UnsupportedOperationException.
   *
   * @param parent an AbstractKerberosDescriptor representing the parent (or container) of this
   */
  @Override
  public void setParent(AbstractKerberosDescriptor parent) {
    throw new UnsupportedOperationException("This KerberosDescriptor may not have a parent assigned to it.");
  }

  @Override
  public int hashCode() {
    return super.hashCode() +
        ((getProperties() == null)
            ? 0
            : getProperties().hashCode()) +
        ((getServices() == null)
            ? 0
            : getServices().hashCode());
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    } else if (object == this) {
      return true;
    } else if (object.getClass() == KerberosDescriptor.class) {
      KerberosDescriptor descriptor = (KerberosDescriptor) object;
      return super.equals(object) &&
          (
              (getProperties() == null)
                  ? (descriptor.getProperties() == null)
                  : getProperties().equals(descriptor.getProperties())
          ) &&
          (
              (getServices() == null)
                  ? (descriptor.getServices() == null)
                  : getServices().equals(descriptor.getServices())
          );
    } else {
      return false;
    }
  }

  /**
   * Recursively gets the entire set of <code>auth_to_local</code> property names contain within this
   * KerberosDescriptor.
   *
   * @return a Set of String values where each value is in the form of config-type/property_name
   */
  public Set<String> getAllAuthToLocalProperties() {
    Set<String> authToLocalProperties = new HashSet<>();

    Set<String> set;

    set = getAuthToLocalProperties();
    if (set != null) {
      authToLocalProperties.addAll(set);
    }

    if (services != null) {
      for (KerberosServiceDescriptor service : services.values()) {
        Map<String, KerberosComponentDescriptor> components = service.getComponents();

        if (components != null) {
          for (KerberosComponentDescriptor component : components.values()) {
            set = component.getAuthToLocalProperties();
            if (set != null) {
              authToLocalProperties.addAll(set);
            }
          }
        }

        set = service.getAuthToLocalProperties();
        if (set != null) {
          authToLocalProperties.addAll(set);
        }
      }
    }

    return authToLocalProperties;
  }

  /**
   * Get a map of principals, where the key is the principal path (SERVICE/COMPONENT/principal_name or SERVICE/principal_name) and the value is the principal.
   * <p>
   * For example if the kerberos principal of the HISTORYSERVER is defined in the kerberos.json:
   * "name": "history_server_jhs",
   * "principal": {
   * "value": "jhs/_HOST@${realm}",
   * "type" : "service",
   * },
   * Then "jhs/_HOST@EXAMPLE.COM" will be put into the map under the "MAPREDUCE2/HISTORYSERVER/history_server_jhs" key.
   */
  public Map<String, String> principals() throws AmbariException {
    Map<String, String> result = new HashMap<>();
    for (AbstractKerberosDescriptorContainer each : nullToEmpty(getChildContainers())) {
      if ((each instanceof KerberosServiceDescriptor)) {
        collectFromComponents(each.getName(), nullToEmpty(((KerberosServiceDescriptor) each).getComponents()).values(), result);
        collectFromIdentities(each.getName(), "", nullToEmpty(each.getIdentities()), result);
      }
    }
    return result;
  }

  private static void collectFromComponents(String service, Collection<KerberosComponentDescriptor> components, Map<String, String> result) {
    for (KerberosComponentDescriptor each : components) {
      collectFromIdentities(service, each.getName(), nullToEmpty(each.getIdentities()), result);
    }
  }

  private static void collectFromIdentities(String service, String component, Collection<KerberosIdentityDescriptor> identities, Map<String, String> result) {
    for (KerberosIdentityDescriptor each : identities) {
      if (each.getPrincipalDescriptor() != null && !each.getReferencedServiceName().isPresent() &&
          !each.getName().startsWith("/")) {
        String path = StringUtils.isBlank(component)
            ? String.format("%s/%s", service, each.getName())
            : String.format("%s/%s/%s", service, component, each.getName());
        result.put(path, each.getPrincipalDescriptor().getName());
      }
    }
  }
}
