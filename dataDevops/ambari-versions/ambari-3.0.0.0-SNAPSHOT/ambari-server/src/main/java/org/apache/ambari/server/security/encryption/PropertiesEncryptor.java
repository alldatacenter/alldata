/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.security.encryption;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.TextEncoding;
import org.apache.commons.collections.CollectionUtils;

/**
 * A common base class for various encryptor implementations
 */
public class PropertiesEncryptor {
  private final Map<Long, Map<StackId, Map<String, Set<String>>>> clusterPasswordProperties = new ConcurrentHashMap<>(); //Map<clusterId, <Map<stackId, Map<configType, Set<passwordPropertyKeys>>>>;
  protected final EncryptionService encryptionService;

  public PropertiesEncryptor(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  protected void encrypt(Map<String, String> configProperties, Cluster cluster, String configType, String encryptionKey) {
    encrypt(configProperties, cluster, configType, value -> encryptAndDecoratePropertyValue(value, encryptionKey));
  }

  protected void encrypt(Map<String, String> configProperties, Cluster cluster, String configType) {
    encrypt(configProperties, cluster, configType, value -> encryptAndDecoratePropertyValue(value));
  }

  protected void encrypt(Map<String, String> configProperties, Cluster cluster, String configType, Function<String,String> encryption) {
    final Set<String> passwordProperties = getPasswordProperties(cluster, configType);
    if (CollectionUtils.isNotEmpty(passwordProperties)) {
      for (Map.Entry<String, String> property : configProperties.entrySet()) {
        if (shouldEncrypt(property, passwordProperties)) {
          configProperties.put(property.getKey(), encryption.apply(property.getValue()));
        }
      }
    }
  }

  private boolean shouldEncrypt(Map.Entry<String, String> property, Set<String> passwordProperties) {
    return passwordProperties.contains(property.getKey()) && !isEncryptedPassword(property.getValue());
  }

  private boolean isEncryptedPassword(String password) {
    return password != null && password.startsWith(Encryptor.ENCRYPTED_PROPERTY_PREFIX); // assuming previous encryption by this class
  }

  private Set<String> getPasswordProperties(Cluster cluster, String configType) {
    //in case of normal configuration change on the UI - or via the API - the current and desired stacks are equal
    //in case of an upgrade they are different; in this case we want to get password properties from the desired stack
    if (cluster.getCurrentStackVersion().equals(cluster.getDesiredStackVersion())) {
      return getPasswordProperties(cluster, cluster.getCurrentStackVersion(), configType);
    } else {
      return getPasswordProperties(cluster, cluster.getDesiredStackVersion(), configType);
    }
  }

  private Set<String> getPasswordProperties(Cluster cluster, StackId stackId, String configType) {
    final long clusterId = cluster.getClusterId();
    clusterPasswordProperties.computeIfAbsent(clusterId, v -> new ConcurrentHashMap<>()).computeIfAbsent(stackId, v -> new ConcurrentHashMap<>())
        .computeIfAbsent(configType, v -> cluster.getConfigPropertiesTypes(configType, stackId).getOrDefault(PropertyInfo.PropertyType.PASSWORD, new HashSet<>()));
    return clusterPasswordProperties.get(clusterId).get(stackId).getOrDefault(configType, new HashSet<>());
  }

  private String encryptAndDecoratePropertyValue(String propertyValue) {
    final String encrypted = encryptionService.encrypt(propertyValue, TextEncoding.BIN_HEX);
    return String.format(Encryptor.ENCRYPTED_PROPERTY_SCHEME, encrypted);
  }

  private String encryptAndDecoratePropertyValue(String propertyValue, String encryptionKey) {
    final String encrypted = encryptionService.encrypt(propertyValue, encryptionKey, TextEncoding.BIN_HEX);
    return String.format(Encryptor.ENCRYPTED_PROPERTY_SCHEME, encrypted);
  }

  protected void decrypt(Map<String, String> configProperties) {
    for (Map.Entry<String, String> property : configProperties.entrySet()) {
      if (isEncryptedPassword(property.getValue())) {
        configProperties.put(property.getKey(), decryptProperty(property.getValue()));
      }
    }
  }

  private String decryptProperty(String property) {
    // sample value: ${enc=aes256_hex, value=5248...303d}
    final String encrypted = property.substring(Encryptor.ENCRYPTED_PROPERTY_PREFIX.length(), property.indexOf('}'));
    return encryptionService.decrypt(encrypted, TextEncoding.BIN_HEX);
  }
}
