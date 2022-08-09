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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.AmbariRuntimeException;
import org.apache.ambari.server.agent.stomp.dto.Hashable;
import org.apache.ambari.server.events.STOMPEvent;
import org.apache.ambari.server.events.STOMPHostEvent;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Is used to saving and updating last version of event in host scope
 * @param <T> event with hash to control version
 */
public abstract class AgentHostDataHolder<T extends STOMPHostEvent & Hashable> extends AgentDataHolder<T> {
  public static final Logger LOG = LoggerFactory.getLogger(AgentHostDataHolder.class);

  @Inject
  private STOMPUpdatePublisher STOMPUpdatePublisher;

  private final ConcurrentHashMap<Long, T> data = new ConcurrentHashMap<>();

  protected abstract T getCurrentData(Long hostId) throws AmbariException;
  protected abstract T handleUpdate(T current, T update) throws AmbariException;

  public T getUpdateIfChanged(String agentHash, Long hostId) throws AmbariException {
    T hostData = initializeDataIfNeeded(hostId, true);
    return !Objects.equals(agentHash, hostData.getHash()) ? hostData : getEmptyData();
  }

  public T initializeDataIfNeeded(Long hostId, boolean regenerateHash) throws AmbariException {
    try {
      return data.computeIfAbsent(hostId, id -> initializeData(hostId, regenerateHash));
    } catch (AmbariRuntimeException e) {
      throw new AmbariException(e.getMessage(), e);
    }
  }

  private T initializeData(Long hostId, boolean regenerateHash) {
    T hostData;
    try {
      hostData = getCurrentData(hostId);
    } catch (AmbariException e) {
      LOG.error("Error during retrieving initial value for host: {} and class {}", hostId, getClass().getName(), e);
      throw new AmbariRuntimeException("Error during retrieving initial value for host: " + hostId + " and class: " + getClass().getName(), e);
    }
    if (regenerateHash) {
      regenerateDataIdentifiers(hostData);
    }
    return hostData;
  }

  /**
   * Apply an incremental update to the data (host-specific), and publish the
   * event to listeners.
   */
  public void updateData(T update) throws AmbariException {
    try {
      data.compute(update.getHostId(), (id, current) -> {
        if (current == null) {
          current = initializeData(id, true);
        }
        T updated;
        try {
          updated = handleUpdate(current, update);
        } catch (AmbariException e) {
          LOG.error("Error during handling update for host: {} and class {}", id, getClass().getName(), e);
          throw new AmbariRuntimeException("Error during handling update for host: " + id + " and class: " + getClass().getName(), e);
        }
        if (updated == null) {
          return current;
        } else {
          regenerateDataIdentifiers(updated);
          setIdentifiersToEventUpdate(update, updated);
          return updated;
        }
      });
    } catch(AmbariRuntimeException e) {
      throw new AmbariException(e.getMessage(), e);
    }
    if (isIdentifierValid(update)) {
      if (update.getType().equals(STOMPEvent.Type.AGENT_CONFIGS)) {
        LOG.info("Configs update with hash {} will be sent to host {}", update.getHash(), update.getHostId());
      }
      STOMPUpdatePublisher.publish(update);
    }
  }

  protected void setIdentifiersToEventUpdate(T update, T hostData) {
    update.setHash(hostData.getHash());
  }

  /**
   * Reset data for the given host.  Used if changes are complex and it's easier to re-create data from scratch.
   */
  public final void resetData(Long hostId) throws AmbariException {
    T newData = getCurrentData(hostId);
    data.replace(hostId, newData);
    STOMPUpdatePublisher.publish(newData);
  }

  /**
   * Remove data for the given host.
   */
  public final void onHostRemoved(Long hostId) {
    data.remove(hostId);
  }

  public Map<Long, T> getData() {
    return data;
  }

  public T getData(Long hostId) {
    return data.get(hostId);
  }

  public void setData(T data, Long hostId) {
    this.data.put(hostId, data);
  }
}
