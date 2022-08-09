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

package org.apache.ambari.server.agent;

import java.util.Arrays;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.AmbariRuntimeException;
import org.apache.ambari.server.security.SecurePasswordHelper;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreType;

/**
 * The encryption key which is used to encrypt sensitive data in agent config updates.
 * This key is shared with the agent so that it can decrypt the encrypted values.
 */
public class AgentEncryptionKey implements Credential {
  private static final String ALIAS = "agent.encryption.key";
  private static final SecurePasswordHelper securePasswordHelper = new SecurePasswordHelper();
  private final char[] key;

  public static AgentEncryptionKey random() {
    return new AgentEncryptionKey(securePasswordHelper.createSecurePassword().toCharArray());
  }

  public static AgentEncryptionKey loadFrom(CredentialStoreService credentialStoreService, boolean createIfNotFound) {
    try {
      if (!credentialStoreService.containsCredential(null, AgentEncryptionKey.ALIAS)) {
        if (createIfNotFound) {
          AgentEncryptionKey encryptionKey = AgentEncryptionKey.random();
          encryptionKey.saveToCredentialStore(credentialStoreService);
          return loadKey(credentialStoreService); // load it again because after saving the key is cleared
        } else {
          throw new AmbariRuntimeException("AgentEncryptionKey with alias: " + ALIAS + " doesn't exist in credential store.");
        }
      } else {
        return loadKey(credentialStoreService);
      }
    } catch (AmbariException e) {
      throw new AmbariRuntimeException("Cannot load agent encryption key: " + e.getMessage(), e);
    }
  }

  private static AgentEncryptionKey loadKey(CredentialStoreService credentialStoreService) throws AmbariException {
    return new AgentEncryptionKey(credentialStoreService.getCredential(null, ALIAS).toValue());
  }

  public AgentEncryptionKey(char[] key) {
    this.key = Arrays.copyOf(key, key.length);
  }

  @Override
  public String toString() {
    return new String(key);
  }

  @Override
  public char[] toValue() {
    return key;
  }

  public void saveToCredentialStore(CredentialStoreService credentialStoreService) {
    try {
      credentialStoreService.setCredential(null, ALIAS, this, CredentialStoreType.PERSISTED);
    } catch (AmbariException e) {
      throw new AmbariRuntimeException("Cannot save agent encryption key: " + e.getMessage(), e);
    }
  }
}
