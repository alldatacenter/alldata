/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.encryption;

import java.util.Map;

import org.apache.ambari.server.state.Config;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * {@link Encryptor} implementation for encrypting/decrypting PASSWORD type
 * properties in {@link Config}'s properties
 */

@Singleton
public class ConfigPropertiesEncryptor extends PropertiesEncryptor implements Encryptor<Config> {

  @Inject
  public ConfigPropertiesEncryptor(EncryptionService encryptionService) {
    super(encryptionService);
  }

  @Override
  public void encryptSensitiveData(Config config) {
    Map<String, String> properties = config.getProperties();
    encrypt(properties, config.getCluster(), config.getType());
    config.setProperties(properties);
  }

  @Override
  public void decryptSensitiveData(Config config) {
    Map<String, String> properties = config.getProperties();
    decrypt(properties);
    config.setProperties(properties);
  }

  @Override
  public String getEncryptionKey() {
    return encryptionService.getAmbariMasterKey();
  }
}
