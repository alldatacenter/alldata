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
package org.apache.ambari.server.security.encryption;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.utils.TextEncoding;

import com.google.inject.Inject;

public class AmbariServerConfigurationEncryptor implements Encryptor<AmbariServerConfiguration> {

  private final Set<String> passwordConfigurations = AmbariServerConfigurationKey.findPasswordConfigurations();
  private final EncryptionService encryptionService;

  @Inject
  public AmbariServerConfigurationEncryptor(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  @Override
  public void encryptSensitiveData(AmbariServerConfiguration encryptible) {
    encryptible.toMap().entrySet().stream()
        .filter(f -> shouldEncrypt(f))
        .forEach(entry -> encryptible.setValueFor(entry.getKey(), encryptAndDecorateConfigValue(entry.getValue())));
  }

  private boolean shouldEncrypt(Map.Entry<String, String> config) {
    return passwordConfigurations.contains(config.getKey()) && !isEncryptedPassword(config.getValue());
  }

  private String encryptAndDecorateConfigValue(String propertyValue) {
    final String encrypted = encryptionService.encrypt(propertyValue, TextEncoding.BIN_HEX);
    return String.format(ENCRYPTED_PROPERTY_SCHEME, encrypted);
  }

  @Override
  public void decryptSensitiveData(AmbariServerConfiguration decryptible) {
    decryptible.toMap().entrySet().stream()
        .filter(f -> passwordConfigurations.contains(f.getKey()))
        .filter(f -> isEncryptedPassword(f.getValue()))
        .forEach(entry -> decryptible.setValueFor(entry.getKey(), decryptConfig(entry.getValue())));
  }

  private boolean isEncryptedPassword(String password) {
    return password != null && password.startsWith(Encryptor.ENCRYPTED_PROPERTY_PREFIX); // assuming previous encryption by this class
  }

  private String decryptConfig(String property) {
    // sample value: ${enc=aes256_hex, value=5248...303d}
    final String encrypted = property.substring(ENCRYPTED_PROPERTY_PREFIX.length(), property.indexOf('}'));
    return encryptionService.decrypt(encrypted, TextEncoding.BIN_HEX);
  }

  @Override
  public String getEncryptionKey() {
    return encryptionService.getAmbariMasterKey();
  }

}
