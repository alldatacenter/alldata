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
package org.apache.ambari.server.agent.stomp;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class AgentConfigsHolder extends AgentHostDataHolder<AgentConfigsUpdateEvent> {
  public static final Logger LOG = LoggerFactory.getLogger(AgentConfigsHolder.class);
  private final Encryptor<AgentConfigsUpdateEvent> encryptor;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private Provider<Clusters> clusters;

  @Inject
  public AgentConfigsHolder(AmbariEventPublisher ambariEventPublisher, @Named("AgentConfigEncryptor") Encryptor<AgentConfigsUpdateEvent> encryptor) {
    this.encryptor = encryptor;
    ambariEventPublisher.register(this);
  }

  @Override
  public AgentConfigsUpdateEvent getCurrentData(Long hostId) throws AmbariException {
    return configHelper.getHostActualConfigs(hostId);
  }

  public AgentConfigsUpdateEvent getCurrentDataExcludeCluster(Long hostId, Long clusterId) throws AmbariException {
    return configHelper.getHostActualConfigsExcludeCluster(hostId, clusterId);
  }

  @Override
  protected AgentConfigsUpdateEvent handleUpdate(AgentConfigsUpdateEvent current, AgentConfigsUpdateEvent update) {
    return update;
  }

  public void updateData(Long clusterId, List<Long> hostIds) throws AmbariException {
    if (CollectionUtils.isEmpty(hostIds)) {
      // TODO cluster configs will be created before hosts assigning
      if (CollectionUtils.isEmpty(clusters.get().getCluster(clusterId).getHosts())) {
        hostIds = clusters.get().getHosts().stream().map(Host::getHostId).collect(Collectors.toList());
      } else {
        hostIds = clusters.get().getCluster(clusterId).getHosts().stream().map(Host::getHostId).collect(Collectors.toList());
      }
    }

    for (Long hostId : hostIds) {
      AgentConfigsUpdateEvent agentConfigsUpdateEvent = configHelper.getHostActualConfigs(hostId);
      updateData(agentConfigsUpdateEvent);
    }
  }

  @Override
  public AgentConfigsUpdateEvent getUpdateIfChanged(String agentHash, Long hostId) throws AmbariException {
    AgentConfigsUpdateEvent update = super.getUpdateIfChanged(agentHash, hostId);
    if (update.getClustersConfigs() == null) {
      update.setTimestamp(getData(hostId).getTimestamp());
    }
    return update;
  }

  @Override
  protected void regenerateDataIdentifiers(AgentConfigsUpdateEvent data) {
    data.setHash(getHash(data, encryptor.getEncryptionKey()));
    encryptor.encryptSensitiveData(data);
    data.setTimestamp(System.currentTimeMillis());
  }

  @Override
  protected boolean isIdentifierValid(AgentConfigsUpdateEvent data) {
    return StringUtils.isNotEmpty(data.getHash()) && data.getTimestamp() != null;
  }

  @Override
  protected void setIdentifiersToEventUpdate(AgentConfigsUpdateEvent update, AgentConfigsUpdateEvent hostData) {
    super.setIdentifiersToEventUpdate(update, hostData);
    update.setTimestamp(hostData.getTimestamp());
  }

  @Override
  protected AgentConfigsUpdateEvent getEmptyData() {
    return AgentConfigsUpdateEvent.emptyUpdate();
  }
}
