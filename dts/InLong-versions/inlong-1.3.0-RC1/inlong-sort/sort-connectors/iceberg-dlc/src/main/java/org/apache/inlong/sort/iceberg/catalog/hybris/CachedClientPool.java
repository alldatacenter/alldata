/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.sort.iceberg.catalog.hybris;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.ClientPool;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.shaded.com.github.benmanes.caffeine.cache.Cache;
import org.apache.iceberg.shaded.com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedClientPool implements ClientPool<IMetaStoreClient, TException> {
    private static final Logger LOG  = LoggerFactory.getLogger(CachedClientPool.class);
    private static Cache<String, DLCWrappedHybrisClientPool> clientPoolCache;

    private final Configuration conf;
    private final String metastoreUri;
    private final int clientPoolSize;
    private final long evictionInterval;

    CachedClientPool(Configuration conf, Map<String, String> properties) {
        this.conf = conf;
        this.metastoreUri = conf.get(HiveConf.ConfVars.METASTOREURIS.varname, "");
        this.clientPoolSize = PropertyUtil.propertyAsInt(properties,
                CatalogProperties.CLIENT_POOL_SIZE,
                CatalogProperties.CLIENT_POOL_SIZE_DEFAULT);
        this.evictionInterval = PropertyUtil.propertyAsLong(properties,
                CatalogProperties.CLIENT_POOL_CACHE_EVICTION_INTERVAL_MS,
                CatalogProperties.CLIENT_POOL_CACHE_EVICTION_INTERVAL_MS_DEFAULT);
        init();
    }

    @VisibleForTesting
    DLCWrappedHybrisClientPool clientPool() {
        return clientPoolCache.get(metastoreUri, k -> new DLCWrappedHybrisClientPool(clientPoolSize, conf));
    }

    private synchronized void init() {
        if (clientPoolCache == null) {
            clientPoolCache = Caffeine.newBuilder().expireAfterAccess(evictionInterval, TimeUnit.MILLISECONDS)
                    .removalListener((key, value, cause) -> ((DLCWrappedHybrisClientPool) value).close())
                    .build();
        }
    }

    @VisibleForTesting
    static Cache<String, DLCWrappedHybrisClientPool> clientPoolCache() {
        return clientPoolCache;
    }

    @Override
    public <R> R run(Action<R, IMetaStoreClient, TException> action) throws TException, InterruptedException {
        return clientPool().run(action);
    }

    @Override
    public <R> R run(Action<R, IMetaStoreClient, TException> action, boolean retry)
            throws TException, InterruptedException {
        return clientPool().run(action, retry);
    }
}
