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
import org.apache.ambari.view.pig.utils.MisconfigurationFormattedException;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Persistent storage engine for storing java beans to
 * properties file
 * Path to file should be in 'dataworker.storagePath' parameter
 */
@Deprecated
public class LocalKeyValueStorage extends KeyValueStorage {
  private final static Logger LOG =
      LoggerFactory.getLogger(LocalKeyValueStorage.class);

  private PersistentConfiguration config = null;

  /**
   * Constructor
   * @param context View Context instance
   */
  public LocalKeyValueStorage(ViewContext context) {
    super(context);
  }

  /**
   * Returns config instance
   * @return config instance
   */
  @Override
  protected synchronized PersistentConfiguration getConfig() {
    if (config == null) {
      String fileName = context.getProperties().get("dataworker.storagePath");
      if (fileName == null) {
        String msg = "dataworker.storagePath is not configured!";
        LOG.error(msg);
        throw new MisconfigurationFormattedException("dataworker.storagePath");
      }
      try {
        config = new PersistentConfiguration(fileName);
      } catch (ConfigurationException e) {
        e.printStackTrace();
      }
    }
    return config;
  }

}
