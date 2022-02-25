/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view.repo;

import java.util.Collection;
import java.util.Date;

import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.PersistenceException;
import org.apache.oozie.ambari.view.model.Indexed;
import org.apache.oozie.ambari.view.model.When;

public class BaseRepo<T> {

  protected final DataStore dataStore;
  private final Class type;

  public BaseRepo(Class type, DataStore dataStore) {
    this.type = type;
    this.dataStore = dataStore;
  }

  public String generateId() {
    return java.util.UUID.randomUUID().toString();
  }

  public Collection<T> findAll() {
    try {
      return dataStore.findAll(type, null);
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }

  public T findById(String id) {
    try {
      return (T) dataStore.find(type, id);
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }

  public T create(T obj) {
    try {
      if (obj instanceof Indexed) {
        Indexed idxObj = (Indexed) obj;
        if (idxObj.getId() == null) {
          idxObj.setId(this.generateId());
        } else {
          T findById = findById(idxObj.getId());
          if (findById != null) {
            throw new RuntimeException("Object already exist in db");
          }
        }
      }
      if (obj instanceof When) {
        Date now = new Date();
        When when = (When) obj;
        when.setCreatedAt(String.valueOf(now.getTime()));
        when.setUpdatedAt(String.valueOf(now.getTime()));
      }
      this.dataStore.store(obj);
      return obj;
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }

  public void update(T obj) {
    try {
      if (obj instanceof When) {
        Date now = new Date();
        When when = (When) obj;
        when.setUpdatedAt(String.valueOf(now.getTime()));
      }
      this.dataStore.store(obj);
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }

  public void delete(T obj) {
    try {
      this.dataStore.remove(obj);
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }

  public void deleteById(String id) {
    try {
      T findById = this.findById(id);
      this.dataStore.remove(findById);
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }
}
