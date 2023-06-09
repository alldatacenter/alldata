/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.sys.store.provider;

import java.io.IOException;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.StoreException;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;
import org.apache.drill.exec.store.sys.PersistentStoreRegistry;
import org.apache.drill.exec.store.sys.VersionedPersistentStore;
import org.apache.drill.exec.store.sys.store.LocalPersistentStore;
import org.apache.drill.exec.store.sys.store.VersionedDelegatingStore;
import org.apache.drill.exec.testing.store.NoWriteLocalStore;
import org.apache.hadoop.fs.Path;

/**
 * A really simple provider that stores data in the local file system, one value per file.
 */
public class LocalPersistentStoreProvider extends BasePersistentStoreProvider {
//  private static final Logger logger = LoggerFactory.getLogger(LocalPersistentStoreProvider.class);

  private final Path path;
  private final DrillFileSystem fs;
  // This flag is used in testing. Ideally, tests should use a specific PersistentStoreProvider that knows
  // how to handle this flag.
  private final boolean enableWrite;

  public LocalPersistentStoreProvider(final PersistentStoreRegistry<?> registry) throws StoreException {
    this(registry.getConfig());
  }

  public LocalPersistentStoreProvider(final DrillConfig config) throws StoreException {
    this.path = new Path(config.getString(ExecConstants.SYS_STORE_PROVIDER_LOCAL_PATH));
    this.enableWrite = config.getBoolean(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE);
    try {
      this.fs = LocalPersistentStore.getFileSystem(config, path);
    } catch (IOException e) {
      throw new StoreException("unable to get filesystem", e);
    }
  }

  @Override
  public <V> PersistentStore<V> getOrCreateStore(PersistentStoreConfig<V> storeConfig) {
    switch(storeConfig.getMode()){
    case BLOB_PERSISTENT:
    case PERSISTENT:
      if (enableWrite) {
        return new LocalPersistentStore<>(fs, path, storeConfig);
      }
      return new NoWriteLocalStore<>();
    default:
      throw new IllegalStateException();
    }
  }

  @Override
  public <V> VersionedPersistentStore<V> getOrCreateVersionedStore(PersistentStoreConfig<V> config) {
    return new VersionedDelegatingStore<>(getOrCreateStore(config));
  }

  @Override
  public void close() throws Exception {
    fs.close();
  }
}
