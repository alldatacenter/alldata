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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

/**
 * AbstractKerberosDescriptor is the base class for all Kerberos*Descriptor and associated classes.
 * <p/>
 * It provides storage and management for the parent and name values on behalf of implementing classes.
 * It also provides utility and helper methods.
 */
public abstract class AbstractKerberosDescriptor {

  static final String KEY_NAME = "name";

  /**
   * An AbstractKerberosDescriptor serving as the parent (or container) for this
   * AbstractKerberosDescriptor.
   * <p/>
   * This value may be null in the event no parent has been identified.
   */
  private AbstractKerberosDescriptor parent = null;

  /**
   * A String declaring the name of this AbstractKerberosDescriptor.
   * <p/>
   * This value may be null in the event a name (or identifier) is not relevant.
   */
  private String name = null;

  /**
   * Generates a Map of data that represents this AbstractKerberosDescriptor implementation.
   * <p/>
   * This method should be overwritten by AbstractKerberosDescriptor implementations to generate a
   * Map of data specific to it.
   * <p/>
   * It is not necessary to call this method from the overriding method.
   * <p/>
   * The map of data generated should be setup so that it can be fed back into the implementation
   * class to build a copy of it. For example:
   * <p/>
   * <pre>
   *  descriptor1 = AbstractKerberosDescriptorImpl(...)
   *  map = descriptor1.toMap()
   *  descriptor2 = AbstractKerberosDescriptor(map)
   *  descriptor1 should have the same data as descriptor2
   * </pre>
   *
   * @return a Map of date representing this AbstractKerberosDescriptor implementation
   */
  public Map<String, Object> toMap() {
    TreeMap<String, Object> dataMap = new TreeMap<>();
    String name = getName();

    if (name != null) {
      dataMap.put(KEY_NAME, name);
    }

    return dataMap;
  }

  /**
   * Returns the name of this descriptor
   *
   * @return a String indicating the name of this descriptor
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of this descriptor
   *
   * @param name a String indicating the name of this descriptor
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the parent (or container) of this descriptor
   *
   * @return an AbstractKerberosDescriptor representing the parent (or container) of this descriptor
   * or null if no parent was set
   */
  public AbstractKerberosDescriptor getParent() {
    return parent;
  }

  /**
   * Sets the parent (or container) of this descriptor
   *
   * @param parent an AbstractKerberosDescriptor representing the parent (or container) of this
   *               descriptor or null to clear the value
   */
  public void setParent(AbstractKerberosDescriptor parent) {
    this.parent = parent;
  }

  /**
   * Test this AbstractKerberosDescriptor to see if it is a container.
   * <p/>
   * The default implementation always returns false.  Implementing classes should override this
   * method to return something different, like true.
   *
   * @return true if this AbstractKerberosDescriptor is a container, false otherwise
   */
  public boolean isContainer() {
    return false;
  }

  /**
   * Safely retrieves the requested value from the supplied Map
   *
   * @param map a Map containing the relevant data
   * @param key a String declaring the item to retrieve
   * @return an Object representing the requested data; or null if not found
   */
  protected static Object getValue(Map<?, ?> map, String key) {
    return ((map == null) || key == null) ? null : map.get(key);
  }

  /**
   * Safely retrieves the requested value (converted to a String) from the supplied Map
   * <p/>
   * The found value will be converted to a String using the {@link Object#toString()} method.
   *
   * @param map a Map containing the relevant data
   * @param key a String declaring the item to retrieve
   * @return a String representing the requested data; or null if not found
   */
  protected static String getStringValue(Map<?, ?> map, String key) {
    Object value = getValue(map, key);
    return (value == null) ? null : value.toString();
  }

  /**
   * Safely retrieves the requested value (converted to a Boolean) from the supplied Map
   * <p/>
   * The found value will be converted to a Boolean using {@link Boolean#valueOf(String)}.
   * If not found, <code>null</code> will be returned
   *
   * @param map a Map containing the relevant data
   * @param key a String declaring the item to retrieve
   * @return a Boolean representing the requested data; or null if not found
   * @see Boolean#valueOf(String)
   * @see #getBooleanValue(Map, String, Boolean)
   */
  protected static Boolean getBooleanValue(Map<?, ?> map, String key) {
    return getBooleanValue(map, key, null);
  }

  /**
   * Safely retrieves the requested value (converted to a Boolean) from the supplied Map
   * <p/>
   * The found value will be converted to a Boolean using {@link Boolean#valueOf(String)}.
   *
   * @param map          a Map containing the relevant data
   * @param key          a String declaring the item to retrieve
   * @param defaultValue a Boolean value to return if the data is not found
   * @return a Boolean representing the requested data; or the specified default value if not found
   * @see Boolean#valueOf(String)
   */
  protected static Boolean getBooleanValue(Map<?, ?> map, String key, Boolean defaultValue) {
    String value = getStringValue(map, key);
    return (StringUtils.isEmpty(value)) ? defaultValue : Boolean.valueOf(value);
  }

  /**
   * Gets the requested AbstractKerberosDescriptor implementation using a type name and a relevant
   * descriptor name.
   * <p/>
   * Implementation classes should override this method to handle relevant descriptor types.
   *
   * @param type a String indicating the type of the requested descriptor
   * @param name a String indicating the name of the requested descriptor
   * @return a AbstractKerberosDescriptor representing the requested descriptor or null if not found
   */
  protected AbstractKerberosDescriptor getDescriptor(Type type, String name) {
    return null;
  }

  /**
   * Traverses up the hierarchy to find the "root" or "parent" container.
   * <p/>
   * The root AbstractKerberosDescriptor is the first descriptor in the hierarchy with a null parent.
   *
   * @return the AbstractKerberosDescriptor implementation that is found to be the root of the hierarchy.
   */
  protected AbstractKerberosDescriptor getRoot() {
    AbstractKerberosDescriptor root = this;

    while (root.getParent() != null) {
      root = root.getParent();
    }

    return root;
  }

  public static <T> Collection<T> nullToEmpty(Collection<T> collection) {
    return collection == null ? Collections.emptyList() : collection;
  }

  public static <T> List<T> nullToEmpty(List<T> list) {
    return list == null ? Collections.emptyList() : list;
  }

  public static <K, V> Map<K, V> nullToEmpty(Map<K, V> collection) {
    return collection == null ? Collections.emptyMap() : collection;
  }

  @Override
  public int hashCode() {
    return 37 *
        ((getName() == null)
            ? 0
            : getName().hashCode());
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    } else if (object == this) {
      return true;
    } else if (object instanceof AbstractKerberosDescriptor) {
      AbstractKerberosDescriptor descriptor = (AbstractKerberosDescriptor) object;
      return (
          (getName() == null)
              ? (descriptor.getName() == null)
              : getName().equals(descriptor.getName())
      );
    } else {
      return false;
    }
  }

  /**
   * Calculate the path to this identity descriptor for logging purposes.
   * Examples:
   * <ul>
   * <li>/</li>
   * <li>/SERVICE</li>
   * <li>/SERVICE/COMPONENT</li>
   * <li>/SERVICE/COMPONENT/identity_name</li>
   * </ul>
   *
   * @return a path
   */
  public String getPath() {
    //
    StringBuilder path = new StringBuilder();
    AbstractKerberosDescriptor current = this;
    while (current != null && (current.getName() != null)) {
      path.insert(0, current.getName());
      path.insert(0, '/');
      current = current.getParent();
    }

    return path.toString();
  }

  /**
   * An enumeration of the different Kerberos (sub)descriptors for internal use.
   */
  public enum Type {
    SERVICE("service", "services"),
    COMPONENT("component", "components"),
    IDENTITY("identity", "identities"),
    PRINCIPAL("principal", "principals"),
    KEYTAB("keytab", "keytabs"),
    CONFIGURATION("configuration", "configurations"),
    AUTH_TO_LOCAL_PROPERTY("auth_to_local_property", "auth_to_local_properties");

    private final String descriptorName;
    private final String descriptorPluralName;

    Type(String descriptorName, String descriptorPluralName) {
      this.descriptorName = descriptorName;
      this.descriptorPluralName = descriptorPluralName;
    }

    /**
     * Gets the identifying name for this Type
     *
     * @return a String declaring the identifying name for this Type
     */
    public String getDescriptorName() {
      return descriptorName;
    }

    /**
     * Gets the identifying name for a group of this Type
     *
     * @return a String declaring the identifying name for a group of this Type
     */
    public String getDescriptorPluralName() {
      return descriptorPluralName;
    }
  }
}
