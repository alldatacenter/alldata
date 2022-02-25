/**
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

package org.apache.ambari.view.pig.persistence;

import com.google.gson.Gson;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.pig.persistence.utils.Indexed;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;
import org.apache.ambari.view.pig.persistence.utils.OnlyOwnersFilteringStrategy;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine for storing objects to key-value storage
 */
public abstract class KeyValueStorage implements Storage {
  private final static Logger LOG =
      LoggerFactory.getLogger(KeyValueStorage.class);
  protected final Gson gson = new Gson();
  protected ViewContext context;

  /**
   * Constructor
   * @param context View Context instance
   */
  public KeyValueStorage(ViewContext context) {
    this.context = context;
  }

  /**
   * Returns config instance, adapter to Persistence API
   * @return config instance
   */
  protected abstract Configuration getConfig();

  @Override
  public synchronized void store(Indexed obj) {
    String modelIndexingPropName = getIndexPropertyName(obj.getClass());

    if (obj.getId() == null) {
      int lastIndex = getConfig().getInt(modelIndexingPropName, 0);
      lastIndex ++;
      getConfig().setProperty(modelIndexingPropName, lastIndex);
      obj.setId(Integer.toString(lastIndex));
    }

    String modelPropName = getItemPropertyName(obj.getClass(), Integer.parseInt(obj.getId()));
    String json = serialize(obj);
    write(modelPropName, json);
  }

  @Override
  public <T extends Indexed> T load(Class<T> model, int id) throws ItemNotFound {
    String modelPropName = getItemPropertyName(model, id);
    LOG.debug(String.format("Loading %s", modelPropName));
    if (getConfig().containsKey(modelPropName)) {
      String json = read(modelPropName);
      LOG.debug(String.format("json: %s", json));
      return deserialize(model, json);
    } else {
      throw new ItemNotFound();
    }
  }

  /**
   * Write json to storage
   * @param modelPropName key
   * @param json value
   */
  protected void write(String modelPropName, String json) {
    getConfig().setProperty(modelPropName, json);
  }

  /**
   * Read json from storage
   * @param modelPropName key
   * @return value
   */
  protected String read(String modelPropName) {
    return getConfig().getString(modelPropName);
  }

  /**
   * Remove line from storage
   * @param modelPropName key
   */
  protected void clear(String modelPropName) {
    getConfig().clearProperty(modelPropName);
  }

  protected String serialize(Indexed obj) {
    return gson.toJson(obj);
  }

  protected <T extends Indexed> T deserialize(Class<T> model, String json) {
    return gson.fromJson(json, model);
  }

  @Override
  public synchronized <T extends Indexed> List<T> loadAll(Class<T> model, FilteringStrategy filter) {
    ArrayList<T> list = new ArrayList<T>();
    String modelIndexingPropName = getIndexPropertyName(model);
    LOG.debug(String.format("Loading all %s-s", model.getName()));
    int lastIndex = getConfig().getInt(modelIndexingPropName, 0);
    for(int i=1; i<=lastIndex; i++) {
      try {
        T item = load(model, i);
        if ((filter == null) || filter.isConform(item)) {
          list.add(item);
        }
      } catch (ItemNotFound ignored) {
      }
    }
    return list;
  }

  @Override
  public synchronized <T extends Indexed> List<T> loadAll(Class<T> model) {
    return loadAll(model, new OnlyOwnersFilteringStrategy(this.context.getUsername()));
  }

  @Override
  public synchronized void delete(Class model, int id) {
    LOG.debug(String.format("Deleting %s:%d", model.getName(), id));
    String modelPropName = getItemPropertyName(model, id);
    clear(modelPropName);
  }

  @Override
  public boolean exists(Class model, int id) {
    return getConfig().containsKey(getItemPropertyName(model, id));
  }

  private String getIndexPropertyName(Class model) {
    return String.format("%s:index", model.getName());
  }

  private String getItemPropertyName(Class model, int id) {
    return String.format("%s.%d", model.getName(), id);
  }
}
