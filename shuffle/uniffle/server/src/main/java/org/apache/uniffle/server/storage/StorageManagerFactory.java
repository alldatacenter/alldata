/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.server.storage;

import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.util.StorageType;

public class StorageManagerFactory {

  private static class LazyHolder {
    static final StorageManagerFactory INSTANCE = new StorageManagerFactory();
  }

  public static StorageManagerFactory getInstance() {
    return LazyHolder.INSTANCE;
  }

  public StorageManager createStorageManager(ShuffleServerConf conf) {
    StorageType type = StorageType.valueOf(conf.get(ShuffleServerConf.RSS_STORAGE_TYPE));
    if (StorageType.LOCALFILE.equals(type) || StorageType.MEMORY_LOCALFILE.equals(type)) {
      return new LocalStorageManager(conf);
    } else if (StorageType.HDFS.equals(type) || StorageType.MEMORY_HDFS.equals(type)) {
      return new HdfsStorageManager(conf);
    } else if (StorageType.LOCALFILE_HDFS.equals(type)
        || StorageType.MEMORY_LOCALFILE_HDFS.equals(type)) {
      return new MultiStorageManager(conf);
    } else {
      throw new IllegalArgumentException("unknown storageType was found");
    }
  }
}
