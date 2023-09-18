/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.data.provider.optimize;

import datart.core.common.Cache;
import datart.core.common.CacheFactory;
import datart.core.data.provider.*;
import org.apache.commons.collections4.map.LRUMap;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public abstract class DataProviderExecuteOptimizer {

    private final Map<String, Dataframe> TEMP_RESULT = new LRUMap<>(100);

    public Dataframe runOptimize(String queryKey, DataProviderSource source, QueryScript queryScript, ExecuteParam param) throws Exception {

        ReentrantLock lock = DefaultLockFactory.getLock(queryKey);

        Dataframe dataframe;
        try {
            lock.lock();
            if (TEMP_RESULT.containsKey(queryKey)) {
                if (lock.getQueueLength() == 0) {
                    return TEMP_RESULT.remove(queryKey);
                } else {
                    return TEMP_RESULT.get(queryKey);
                }
            }
            dataframe = run(source, queryScript, param);
            if (lock.getQueueLength() > 0) {
                TEMP_RESULT.put(queryKey, dataframe);
            }
            return dataframe;
        } finally {
            lock.unlock();
        }
    }

    public Dataframe getFromCache(String queryKey) {
        try {
            Cache cache = CacheFactory.getCache();
            if (cache != null) {
                return cache.get(queryKey);
            }
        } catch (Exception e) {
        }
        return null;
    }

    public void setCache(String queryKey, Dataframe dataframe, int cacheExpires) {
        try {
            Cache cache = CacheFactory.getCache();
            if (cache != null) {
                cache.put(queryKey, dataframe, cacheExpires);
            }
        } catch (Exception e) {
        }
    }


    public abstract Dataframe run(DataProviderSource source, QueryScript queryScript, ExecuteParam param) throws Exception;


}
