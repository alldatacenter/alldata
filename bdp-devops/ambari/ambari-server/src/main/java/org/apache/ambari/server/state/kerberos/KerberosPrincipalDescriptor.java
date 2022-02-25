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
import java.util.TreeMap;

/**
 * KerberosPrincipalDescriptor is an implementation of an AbstractKerberosDescriptor that
 * encapsulates data related to a Kerberos principal.  This class is typically associated with the
 * KerberosKeytabDescriptor via a KerberosIdentityDescriptor.
 * <p/>
 * A KerberosPrincipalDescriptor has the following properties:
 * <ul>
 * <li>value</li>
 * <li>type</li>
 * <li>configuration</li>
 * <li>local_username</li>
 * </ul>
 * <p/>
 * The following JSON Schema will yield a valid KerberosPrincipalDescriptor
 * <pre>
 *   {
 *      "$schema": "http://json-schema.org/draft-04/schema#",
 *      "title": "KerberosIdentityDescriptor",
 *      "description": "Describes a Kerberos principal and associated details",
 *      "type": "object",
 *      "properties": {
 *        "value": {
 *          "description": "The pattern to use to generate the principal",
 *          "type": "string"
 *        },
 *        "type": {
 *          "description": "The type of principal - either 'service' or 'user'",
 *          "type": "string"
 *        },
 *        "configuration": {
 *          "description": "The configuration type and property name indicating the property to be
 *                          updated with the generated principal - format: config-type/property.name",
 *          "type": "string"
 *        },
 *        "local_username": {
 *          "description": "The local username this principal maps to - optional if no mapping is needed",
 *          "type": "string"
 *        }
 *      }
 *   }
 * </pre>
 * <p/>
 * In this implementation,
 * {@link org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor#name} will hold the
 * KerberosPrincipalDescriptor#value value
 */
public class KerberosPrincipalDescriptor extends AbstractKerberosDescriptor {

  static final String KEY_VALUE = "value";
  static final String KEY_TYPE = "type";
  static final String KEY_CONFIGURATION = "configuration";
  static final String KEY_LOCAL_USERNAME = "local_username";

  /**
   * A string declaring the type of principal this KerberosPrincipalDescriptor represents.
   * <p/>
   * Expecting either "service" or "user"
   */
  private KerberosPrincipalType type = null;

  /**
   * A string declaring configuration type and property name indicating the property to be updated
   * with the generated principal
   * <p/>
   * This String is expected to be in the following format: configuration-type/property.name, where
   * <ul>
   * <li>configuration-type is the configuration file type where the property exists</li>
   * <li>property.value is the name of the relevant property within the configuration</li>
   * </ul>
   * <p/>
   * Example: hdfs-site/dfs.namenode.kerberos.principal
   */
  private String configuration = null;

  /**
   * a String indicating the local username related to this principal, or null of no local mapping is
   * needed or available.
   * <p/>
   * This value may be using in generating auth_to_local configuration settings.
   */
  private String localUsername = null;

  /**
   * Creates a new KerberosPrincipalDescriptor
   *
   * @param principal     the principal name
   * @param type          the principal type (user, service, etc...)
   * @param configuration the configuration used to store the principal name
   * @param localUsername the local username to map to the principal
   */
  public KerberosPrincipalDescriptor(String principal, KerberosPrincipalType type, String configuration, String localUsername) {
    // The name for this KerberosPrincipalDescriptor is stored in the "value" entry in the map
    // This is not automatically set by the super classes.
    setName(principal);
    setType(type);
    setConfiguration(configuration);
    setLocalUsername(localUsername);
  }

  /**
   * Creates a new KerberosPrincipalDescriptor
   * <p/>
   * See {@link org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor} for the JSON
   * Schema that may be used to generate this map.
   *
   * @param data a Map of values use to populate the data for the new instance
   * @see org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor
   */
  public KerberosPrincipalDescriptor(Map<?, ?> data) {
    this(getStringValue(data, KEY_VALUE),
        getKerberosPrincipalTypeValue(data, KEY_TYPE),
        getStringValue(data, KEY_CONFIGURATION),
        getStringValue(data, KEY_LOCAL_USERNAME)
    );
  }

  /**
   * Gets the value (or principal name pattern) for this KerberosPrincipalDescriptor
   * <p/>
   * The value may include variable placeholders to be replaced as needed
   * <ul>
   * <li>
   * ${variable} placeholders are replaced on the server - see
   * {@link VariableReplacementHelper#replaceVariables(String, Map)}
   * </li>
   * <li>the _HOST placeholder is replaced on the hosts to dynamically populate the relevant hostname</li>
   * </ul>
   *
   * @return a String declaring this principal's value
   * @see VariableReplacementHelper#replaceVariables(String, Map)
   */
  public String getValue() {
    return getName();
  }

  /**
   * Sets the value (or principal name pattern) for this KerberosPrincipalDescriptor
   *
   * @param value a String declaring this principal's value
   * @see #getValue()
   */
  public void setValue(String value) {
    setName(value);
  }

  /**
   * Gets the type of this KerberosPrincipalDescriptor
   *
   * @return a KerberosPrincipalType declaring the type of this KerberosPrincipalDescriptor
   */
  public KerberosPrincipalType getType() {
    return type;
  }

  /**
   * Sets the type of this KerberosPrincipalDescriptor
   * <p/>
   * The value should be either "service" or "user"
   *
   * @param type a KerberosPrincipalType declaring the type of this KerberosPrincipalDescriptor
   */
  public void setType(KerberosPrincipalType type) {
    this.type = type;
  }

  /**
   * Gets the configuration type and property name indicating the property to be updated with the
   * generated principal
   *
   * @return a String declaring the configuration type and property name indicating the property to
   * be updated with the generated principal
   * #see #configuration
   */
  public String getConfiguration() {
    return configuration;
  }

  /**
   * Sets the configuration type and property name indicating the property to be updated with the
   * generated principal
   *
   * @param configuration a String declaring the configuration type and property name indicating the
   *                      property to be updated with the generated principal
   * @see #configuration
   */
  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  /**
   * Gets the local username associated with this principal
   *
   * @return a String indicating the local username related to this principal, or null of not local
   * mapping is needed/available
   */
  public String getLocalUsername() {
    return localUsername;
  }

  /**
   * Sets the local username associated with this principal
   *
   * @param localUsername a String indicating the local username related to this principal, or null
   *                      of no local mapping is needed/available
   */
  public void setLocalUsername(String localUsername) {
    this.localUsername = localUsername;
  }

  /**
   * Updates this KerberosPrincipalDescriptor with data from another KerberosPrincipalDescriptor
   * <p/>
   * Properties will be updated if the relevant updated values are not null.
   *
   * @param updates the KerberosPrincipalDescriptor containing the updated values
   */
  public void update(KerberosPrincipalDescriptor updates) {
    if (updates != null) {
      String updatedValue;

      updatedValue = updates.getValue();
      if (updatedValue != null) {
        setValue(updatedValue);
      }

      KerberosPrincipalType updatedType = updates.getType();
      if (updatedType != null) {
        setType(updatedType);
      }

      updatedValue = updates.getConfiguration();
      if (updatedValue != null) {
        setConfiguration(updatedValue);
      }

      updatedValue = updates.getLocalUsername();
      if (updatedValue != null) {
        setLocalUsername(updatedValue);
      }
    }
  }

  /**
   * Creates a Map of values that can be used to create a copy of this KerberosPrincipalDescriptor
   * or generate the JSON structure described in
   * {@link org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor}
   *
   * @return a Map of values for this KerberosPrincipalDescriptor
   * @see org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor
   */
  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = new TreeMap<>();

    map.put(KEY_VALUE, getValue());
    map.put(KEY_TYPE, KerberosPrincipalType.translate(getType()));
    map.put(KEY_CONFIGURATION, getConfiguration());
    map.put(KEY_LOCAL_USERNAME, getLocalUsername());

    return map;
  }

  @Override
  public int hashCode() {
    return super.hashCode() +
        ((getConfiguration() == null)
            ? 0
            : getConfiguration().hashCode()) +
        ((getType() == null)
            ? 0
            : getType().hashCode());
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    } else if (object == this) {
      return true;
    } else if (object.getClass() == KerberosPrincipalDescriptor.class) {
      KerberosPrincipalDescriptor descriptor = (KerberosPrincipalDescriptor) object;
      return super.equals(object) &&
          (
              (getConfiguration() == null)
                  ? (descriptor.getConfiguration() == null)
                  : getConfiguration().equals(descriptor.getConfiguration())
          ) &&
          (
              (getType() == null)
                  ? (descriptor.getType() == null)
                  : getType().equals(descriptor.getType())
          );
    } else {
      return false;
    }
  }

  /**
   * Translates a string value representing a principal type to a KerberosPrincipalType.
   *
   * @param map a Map containing the relevant data
   * @param key a String declaring the item to retrieve
   * @return a KerberosPrincipalType, or null is not specified in the map
   * @throws IllegalArgumentException if the principal type value is not one of the expected types.
   */
  private static KerberosPrincipalType getKerberosPrincipalTypeValue(Map<?, ?> map, String key) {
    String type = getStringValue(map, key);
    if ((type == null) || type.isEmpty()) {
      return null;
    } else {
      return KerberosPrincipalType.valueOf(type.toUpperCase());
    }
  }
}
