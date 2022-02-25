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

package org.apache.ambari.view.pig.persistence.utils;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.persistence.DataStoreStorage;
import org.apache.ambari.view.pig.persistence.InstanceKeyValueStorage;
import org.apache.ambari.view.pig.persistence.LocalKeyValueStorage;
import org.apache.ambari.view.pig.persistence.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Storage factory, creates storage of Local or Persistence API type.
 * Type depends on context configuration: if "dataworker.storagePath" is set,
 * storage of Local type will be created.  Otherwise, Persistence API will be used.
 *
 * Storage is singleton.
 */
public class StorageUtil {
  private Storage storageInstance = null;

  protected final static Logger LOG =
      LoggerFactory.getLogger(StorageUtil.class);


  private static Map<String, StorageUtil> viewSingletonObjects = new HashMap<String, StorageUtil>();
  public static StorageUtil getInstance(ViewContext context) {
    if (!viewSingletonObjects.containsKey(context.getInstanceName()))
      viewSingletonObjects.put(context.getInstanceName(), new StorageUtil(context));
    return viewSingletonObjects.get(context.getInstanceName());
  }

  public static void dropAllConnections() {
    viewSingletonObjects.clear();
  }

  private ViewContext context;

  /**
   * Constructor of storage util
   * @param context View Context instance
   */
  public StorageUtil(ViewContext context) {
    this.context = context;
  }

  /**
   * Get storage instance. If one is not created, creates instance.
   * @return storage instance
   */
  public synchronized Storage getStorage() {
    if (storageInstance == null) {
      String fileName = context.getProperties().get("dataworker.storagePath");
      if (fileName != null) {
        LOG.debug("Using local storage in " + fileName + " to store data");
        // If specifed, use LocalKeyValueStorage - key-value file based storage
        storageInstance = new LocalKeyValueStorage(context);
      } else {
        LOG.debug("Using Persistence API to store data");
        // If not specifed, use ambari-views Persistence API
        storageInstance = new DataStoreStorage(context);
      }
    }
    return storageInstance;
  }

  /**
   * Set storage to use across all application.
   * Used in unit tests.
   * @param storage storage instance
   */
  public void setStorage(Storage storage) {
    storageInstance = storage;
  }
}
