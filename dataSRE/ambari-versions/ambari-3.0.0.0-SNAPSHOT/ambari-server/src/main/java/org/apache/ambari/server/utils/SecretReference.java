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

package org.apache.ambari.server.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.PropertyInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;


@StaticallyInject
public class SecretReference {
  private static final String secretPrefix = "SECRET";
  private String configType;
  private Long version;
  private String value;

  private final static String PASSWORD_TEXT = "password";
  private final static String PASSWD_TEXT = "passwd";

  @Inject
  private static Gson gson;

  public SecretReference(String reference, Cluster cluster) throws AmbariException{
    String[] values = reference.split(":");

    configType = values[1];
    version = Long.valueOf(values[2]);

    String propertyName = values[3];
    String clusterName = cluster.getClusterName();
    Config refConfig = cluster.getConfigByVersion(configType, version);

    if(refConfig == null)
      throw new AmbariException(String.format("Error when parsing secret reference. Cluster: %s does not contain ConfigType: %s ConfigVersion: %s",
          clusterName, configType, version));
    Map<String, String> refProperties = refConfig.getProperties();
    if(!refProperties.containsKey(propertyName))
      throw new AmbariException(String.format("Error when parsing secret reference. Cluster: %s ConfigType: %s ConfigVersion: %s does not contain property '%s'",
          clusterName, configType, version, propertyName));

    this.value = refProperties.get(propertyName);
  }

  public void setConfigType(String configType) {
    this.configType = configType;
  }

  public Long getVersion() {
    return version;
  }

  public String getValue() {
    return value;
  }

  public static boolean isSecret(String value) {
    String[] values = value.split(":");
    return values.length == 4 && values[0].equals(secretPrefix);
  }

  public static String generateStub(String configType, Long configVersion, String propertyName) {
    return secretPrefix + ":" + configType + ":" + configVersion + ":" + propertyName;
  }

  /**
   * Helper function to mask a string of property bags that may contain a property with a password.
   * @param propertyMap Property map to mask by replacing any passwords with the text "SECRET"
   * @return New string with the passwords masked, or null if the property map is null.
   */
  public static String maskPasswordInPropertyMap(String propertyMap) {
    if (null == propertyMap) {
      return null;
    }
    final Map<String, String> map = gson.fromJson(propertyMap, new TypeToken<Map<String, String>>() {}.getType());
    return gson.toJson(maskPasswordInPropertyMap(map));
  }

  /**
   * Helper function to mask a string of properties that may contain a property with a password.
   * @param propertyMap Property map to mask by replacing any passwords with the text "SECRET"
   * @return a new map with the passwords masked, or null if the <code>propertyMap</code> is null.
   */
  public static Map<String, String> maskPasswordInPropertyMap(Map<String, String> propertyMap) {
    if (null == propertyMap) {
      return null;
    }
    final Map<String, String> maskedMap = new HashMap<>();
    for (Map.Entry<String, String> property : propertyMap.entrySet()) {
      String value = isPassword(property.getKey()) ? secretPrefix : property.getValue();
      maskedMap.put(property.getKey(), value);
    }
    return maskedMap;
  }

  private final static boolean isPassword(String propertyName) {
    return propertyName.toLowerCase().contains(PASSWORD_TEXT) || propertyName.toLowerCase().contains(PASSWD_TEXT);
  }

  /**
   * Replace secret references with appropriate real passwords.
   * @param targetMap map in which replacement will be performed
   * @param cluster current cluster
   * @throws AmbariException
   */
  public static void replaceReferencesWithPasswords(Map<String, String> targetMap, Cluster cluster)
      throws AmbariException {
    if(cluster != null) {
      for (Map.Entry<String, String> propertyValueEntry : targetMap.entrySet()) {
        String key = propertyValueEntry.getKey();
        String value = propertyValueEntry.getValue();
        if (value != null && SecretReference.isSecret(value)) {
          SecretReference ref = new SecretReference(value, cluster);
          targetMap.put(key, ref.getValue());
        }
      }
    }
  }

  /**
   * Replace real passwords with secret references
   * @param propertiesTypes map with properties types
   * @param propertiesMap map with properties in which replacement will be performed
   * @param configType configuration type
   * @param configVersion configuration version
   */
  public static void replacePasswordsWithReferences(Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes,
                                                    Map<String, String> propertiesMap,
                                                    String configType,
                                                    Long configVersion){
    if(propertiesTypes != null && propertiesTypes.containsKey(PropertyInfo.PropertyType.PASSWORD)) {
      for(String pwdPropertyName: propertiesTypes.get(PropertyInfo.PropertyType.PASSWORD)) {
        if(propertiesMap.containsKey(pwdPropertyName)){
          if(!propertiesMap.get(pwdPropertyName).equals("")) {
            String stub = SecretReference.generateStub(configType, configVersion, pwdPropertyName);
            propertiesMap.put(pwdPropertyName, stub);
          }
        }
      }
    }
  }

  /**
   * Replace real passwords with secret references
   * @param configAttributes map with config attributes containing properties types as part of their content
   * @param propertiesMap map with properties in which replacement will be performed
   * @param configType configuration type
   * @param configVersion configuration version
   */
  public static void replacePasswordsWithReferencesForCustomProperties(Map<String, Map<String, String>> configAttributes,
                                                    Map<String, String> propertiesMap,
                                                    String configType,
                                                    Long configVersion){
    if(configAttributes != null && configAttributes.containsKey("password")) {
      for(String pwdPropertyName: configAttributes.get("password").keySet()) {
        if(propertiesMap.containsKey(pwdPropertyName)){
          if(!propertiesMap.get(pwdPropertyName).equals("")) {
            String stub = SecretReference.generateStub(configType, configVersion, pwdPropertyName);
            propertiesMap.put(pwdPropertyName, stub);
          }
        }
      }
    }
  }
}
