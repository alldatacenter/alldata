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

package org.apache.ambari.server.api.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.ambari.server.orm.dao.KeyValueDAO;
import org.apache.ambari.server.orm.entities.KeyValueEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PersistKeyValueImpl {

  @Inject
  KeyValueDAO keyValueDAO;

  public String generateKey() {
    return UUID.randomUUID().toString();
  }

  public Collection<String> generateKeys(int number) {
    List<String> keys = new ArrayList<>(number);
    for (int i = 0; i < number; i++) {
      keys.add(generateKey());
    }
    return keys;
  }

  public synchronized String getValue(String key) {
    KeyValueEntity keyValueEntity = keyValueDAO.findByKey(key);
    if (keyValueEntity != null) {
      return keyValueEntity.getValue();
    }
    throw new WebApplicationException(Response.Status.NOT_FOUND);
  }

  public synchronized String put(String value) {
    String key = generateKey();
    put(key, value);
    return key;
  }

  public synchronized void put(String key, String value) {
    KeyValueEntity keyValueEntity = keyValueDAO.findByKey(key);
    if (keyValueEntity != null) {
      keyValueEntity.setValue(value);
      keyValueDAO.merge(keyValueEntity);
    } else {
      keyValueEntity = new KeyValueEntity();
      keyValueEntity.setKey(key);
      keyValueEntity.setValue(value);
      keyValueDAO.create(keyValueEntity);
    }
  }
  
  public synchronized Map<String, String> getAllKeyValues() {
    Map<String, String> map = new HashMap<>();
    for (KeyValueEntity keyValueEntity : keyValueDAO.findAll()) {
      map.put(keyValueEntity.getKey(), keyValueEntity.getValue());
    }
    return map;
  }
}
