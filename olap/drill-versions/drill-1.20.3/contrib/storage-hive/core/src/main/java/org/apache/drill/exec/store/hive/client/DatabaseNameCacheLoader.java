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
package org.apache.drill.exec.store.hive.client;

import java.util.List;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CacheLoader that synchronized on client and tries to reconnect when
 * client fails. Used by {@link HiveMetadataCache}.
 */
final class DatabaseNameCacheLoader extends CacheLoader<String, List<String>> {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseNameCacheLoader.class);

  private final DrillHiveMetaStoreClient client;

  DatabaseNameCacheLoader(DrillHiveMetaStoreClient client) {
    this.client = client;
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public List<String> load(String key) throws Exception {
    synchronized (client) {
      try {
        return client.getAllDatabases();
      } catch (MetaException e) {
      /*
         HiveMetaStoreClient is encapsulating both the MetaException/TExceptions inside MetaException.
         Since we don't have good way to differentiate, we will close older connection and retry once.
         This is only applicable for getAllTables and getAllDatabases method since other methods are
         properly throwing correct exceptions.
      */
        logger.warn("Failure while attempting to get hive databases. Retries once.", e);
        AutoCloseables.closeSilently(client::close);
        try {
          /*
             Attempt to reconnect. If this is a secure connection, this will fail due
             to the invalidation of the security token. In that case, throw the original
             exception and let a higher level clean up. Ideally we'd get a new token
             here, but doing so requires the use of a different connection, and that
             one has also become invalid. This code needs a rework; this is just a
             work-around.
          */
          client.reconnect();
        } catch (Exception e1) {
          throw e;
        }
        return client.getAllDatabases();
      }
    }
  }

}
