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

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.persistence.utils.*;
import org.apache.ambari.view.pig.utils.ServiceFormattedException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;


/**
 * Persistent storage engine for storing java beans to
 * instance data
 */
@Deprecated
public class InstanceKeyValueStorage extends KeyValueStorage {
  private final static Logger LOG =
      LoggerFactory.getLogger(InstanceKeyValueStorage.class);

  private ContextConfigurationAdapter config = null;
  private int VALUE_LENGTH_LIMIT = 254;

  /**
   * Constructor.
   * @param context View Context instance
   */
  public InstanceKeyValueStorage(ViewContext context) {
    super(context);
  }

  /**
   * Returns config instance, adapter to Persistence API
   * @return config instance
   */
  @Override
  protected synchronized Configuration getConfig() {
    if (config == null) {
      config = new ContextConfigurationAdapter(context);
    }
    return config;
  }

  /**
   * Value is limited to 256 symbols, this code splits value into chunks and saves them as <key>#<chunk_id>
   * @param modelPropName key
   * @param json value
   */
  protected void write(String modelPropName, String json) {
    int saved = 0;
    int page = 1;
    while (saved < json.length()) {
      int end = Math.min(saved + VALUE_LENGTH_LIMIT, json.length());
      String substring = json.substring(saved, end);
      getConfig().setProperty(modelPropName + "#" + page, substring);
      saved += VALUE_LENGTH_LIMIT;
      page += 1;
      LOG.debug("Chunk saved: " + modelPropName + "#" + page + "=" + substring);
    }
    getConfig().setProperty(modelPropName, page - 1);
    LOG.debug("Write finished: " + modelPropName + " pages:" + (page - 1));
  }

  /**
   * Read chunked value (keys format <key>#<chunk_id>)
   * @param modelPropName key
   * @return value
   */
  protected String read(String modelPropName) {
    StringBuilder result = new StringBuilder();
    int pages = getConfig().getInt(modelPropName);
    LOG.debug("Read started: " + modelPropName + " pages:" + pages);

    for(int page = 1; page <= pages; page++) {
      String substring = getConfig().getString(modelPropName + "#" + page);
      LOG.debug("Chunk read: " + modelPropName + "#" + page + "=" + substring);
      if (substring != null) {
        result.append(substring);
      }
    }

    return result.toString();
  }

  /**
   * Remove chunked value (keys format <key>#<chunk_id>)
   * @param modelPropName key
   */
  protected void clear(String modelPropName) {
    int pages = getConfig().getInt(modelPropName);
    LOG.debug("Clean started: " + modelPropName + " pages:" + pages);

    for(int page = 1; page <= pages; page++) {
      getConfig().clearProperty(modelPropName + "#" + page);
      LOG.debug("Chunk clean: " + modelPropName + "#" + page);
    }
    getConfig().clearProperty(modelPropName);
  }

  public static void storageSmokeTest(ViewContext context) {
    try {
      final String property = "test.smoke.property";
      context.putInstanceData(property, "42");
      boolean status = context.getInstanceData(property).equals("42");
      context.removeInstanceData(property);
      if (!status) throw new ServiceFormattedException("Ambari Views instance data DB doesn't work properly", null);
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }
}
