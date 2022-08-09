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
import java.util.SortedMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.AmbariRuntimeException;
import org.apache.ambari.server.agent.AgentEncryptionKey;
import org.apache.ambari.server.agent.stomp.dto.ClusterConfigs;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * This encryptor encrypts the sensitive data in config updates sent to ambari-agent.
 * Decryption happens on the agent side.
 */
@Singleton
public class AgentConfigUpdateEncryptor extends PropertiesEncryptor implements Encryptor<AgentConfigsUpdateEvent> {
  private final AgentEncryptionKey encryptionKey;
  private final Provider<Clusters> clusters;

  @Inject
  public AgentConfigUpdateEncryptor(EncryptionService encryptionService, CredentialStoreService credentialStore, Provider<Clusters> clusters) {
    super(encryptionService);
    this.encryptionKey = AgentEncryptionKey.loadFrom(credentialStore, true);
    this.clusters = clusters;
  }

  @Override
  public void encryptSensitiveData(AgentConfigsUpdateEvent event) {
    for (Map.Entry<String, ClusterConfigs> each : event.getClustersConfigs().entrySet()) {
      Cluster cluster = getCluster(Long.parseLong(each.getKey()));
      ClusterConfigs clusterConfigs = each.getValue();
      for (Map.Entry<String, SortedMap<String, String>> clusterConfig : clusterConfigs.getConfigurations().entrySet()) {
        encrypt(
          clusterConfig.getValue(),
          cluster,
          clusterConfig.getKey(),
          encryptionKey.toString());
      }
    }
  }

  @Override
  public void decryptSensitiveData(AgentConfigsUpdateEvent event) {
    throw new UnsupportedOperationException("Not supported"); // Decryption happens on the agent side, this is not needed right now
  }

  private Cluster getCluster(long clusterId) throws AmbariRuntimeException {
    try {
      return clusters.get().getCluster(clusterId);
    } catch (AmbariException e) {
      throw new AmbariRuntimeException("Cannot load cluster: " + clusterId, e);
    }
  }

  @Override
  public String getEncryptionKey() {
    return encryptionKey.toString();
  }
}
