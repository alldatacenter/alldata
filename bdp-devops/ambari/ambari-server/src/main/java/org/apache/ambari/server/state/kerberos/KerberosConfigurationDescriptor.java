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


import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * KerberosConfigurationDescriptor is an implementation of an AbstractKerberosDescriptor that
 * encapsulates data related to an Ambari configuration block.
 * <p/>
 * A KerberosConfigurationDescriptor has the following properties:
 * <ul>
 * <li>type</li>
 * <li>properties</li>
 * </ul>
 * <p/>
 * The following is an example of a JSON structure that will yield a valid KerberosConfigurationDescriptor
 * <pre>
 *   {
 *    "core-site": {
 *      "hadoop.security.authentication": "kerberos",
 *      "hadoop.rpc.protection": "authentication; integrity; privacy",
 *      "hadoop.security.authorization": "true"
 *    }
 *   }
 * </pre>
 * <p/>
 * In this implementation,
 * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor#name} will hold the
 * KerberosConfigurationDescriptor#type value
 */
public class KerberosConfigurationDescriptor extends AbstractKerberosDescriptor {
  /**
   * A Map of the properties in this KerberosConfigurationDescriptor
   */
  private Map<String, String> properties = null;

  /**
   * Creates a new KerberosConfigurationDescriptor
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor} for an
   * example JSON structure that may be used to generate this map.
   *
   * @param data a Map of values use to populate the data for the new instance
   * @see org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor
   */
  public KerberosConfigurationDescriptor(Map<?, ?> data) {

    if (data != null) {
      Set<?> keySet = data.keySet();

      if (!keySet.isEmpty()) {
        // Only a single entry is expected...
        Object key = keySet.iterator().next();
        if (key != null) {
          Object object = data.get(key);

          setType(key.toString());

          if (object instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
              Object value = entry.getValue();
              putProperty(entry.getKey().toString(), (value == null) ? null : value.toString());
            }
          }
        }
      }
    }
  }

  /**
   * Returns the type of the configuration data represented by this KerberosConfigurationDescriptor
   *
   * @return a String declaring the configuration type, i.e., core-site
   */
  public String getType() {
    return getName();
  }

  /**
   * Sets the type of the configuration data represented by this KerberosConfigurationDescriptor
   *
   * @param type a String declaring the configuration group type, i.e., core-site
   */
  public void setType(String type) {
    setName(type);
  }

  /**
   * Set the properties of the configuration data represented by this KerberosConfigurationDescriptor
   *
   * @param properties a Map of properties
   */
  public void setProperties(Map<String, String> properties) {
    if (properties == null) {
      this.properties = null;
    } else {
      this.properties = new TreeMap<>(properties);
    }
  }

  /**
   * Gets the properties of the configuration data represented by this KerberosConfigurationDescriptor
   *
   * @return a Map of properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Gets the value of the configuration property with the specified name
   *
   * @param name a String declaring the name of the property to retrieve
   * @return a String or null if the property value is not found
   */
  public String getProperty(String name) {
    return ((name == null) || (properties == null)) ? null : properties.get(name);
  }

  /**
   * Adds or updates the value of the configuration property with the specified name
   * <p/>
   * If the property exists, it will be overwritten; else a new entry will be created.
   *
   * @param name  a String declaring the name of the property to set
   * @param value a String containing the value of the property
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
   * Updates this KerberosConfigurationDescriptor with data from another KerberosConfigurationDescriptor
   * <p/>
   * Properties will be updated if the relevant updated values are not null.
   *
   * @param updates the KerberosConfigurationDescriptor containing the updated values
   */
  public void update(KerberosConfigurationDescriptor updates) {
    if (updates != null) {
      setType(updates.getType());

      Map<String, String> updatedProperties = updates.getProperties();
      if (updatedProperties != null) {
        for (Map.Entry<String, String> entry : updatedProperties.entrySet()) {
          putProperty(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  /**
   * Creates a Map of values that can be used to create a copy of this KerberosConfigurationDescriptor
   * or generate the JSON structure described in
   * {@link org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor}
   *
   * @return a Map of values for this KerberosConfigurationDescriptor
   * @see org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor
   */
  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = new TreeMap<>();
    map.put(getName(), (properties == null) ? null : new TreeMap<String, Object>(properties));
    return map;
  }

  @Override
  public int hashCode() {
    return super.hashCode() +
        ((getProperties() == null)
            ? 0
            : getProperties().hashCode());
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    } else if (object == this) {
      return true;
    } else if (object.getClass() == KerberosConfigurationDescriptor.class) {
      KerberosConfigurationDescriptor descriptor = (KerberosConfigurationDescriptor) object;
      return super.equals(object) &&
          (
              (getProperties() == null)
                  ? (descriptor.getProperties() == null)
                  : getProperties().equals(descriptor.getProperties())
          );
    } else {
      return false;
    }
  }
}
