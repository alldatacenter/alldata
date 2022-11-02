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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ambari.server.agent.stomp.dto.HashAndTimestampIgnoreMixIn;
import org.apache.ambari.server.agent.stomp.dto.HashIgnoreMixIn;
import org.apache.ambari.server.agent.stomp.dto.Hashable;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Is used to hash generating for event
 * @param <T> event with hash to control version
 */
public abstract class AgentDataHolder<T extends Hashable> {
  protected final ReentrantLock updateLock = new ReentrantLock();
  private final static ObjectMapper MAPPER = new ObjectMapper();
  static {
    MAPPER.addMixIn(Hashable.class, HashIgnoreMixIn.class);
    MAPPER.addMixIn(AgentConfigsUpdateEvent.class, HashAndTimestampIgnoreMixIn.class);
  }

  protected abstract T getEmptyData();

  protected void regenerateDataIdentifiers(T data) {
    data.setHash(getHash(data));
  }

  protected boolean isIdentifierValid(T data) {
    return StringUtils.isNotEmpty(data.getHash());
  }

  protected String getHash(T data) {
    return getHash(data, "");
  }

  protected String getHash(T data, String salt) {
    String json = null;
    try {
      json = MAPPER.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error during mapping message to calculate hash", e);
    }
    String generatedPassword = null;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      md.update(salt.getBytes("UTF-8"));
      byte[] bytes = md.digest(json.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder();
      for (byte b : bytes) {
        sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
      }
      generatedPassword = sb.toString();
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return generatedPassword;
  }
}
