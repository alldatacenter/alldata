/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.common.metric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MetricItemSet
 */
public abstract class MetricItemSet<T extends MetricItem> implements MetricItemSetMBean {

    protected String name;

    protected Map<String, T> itemMap = new ConcurrentHashMap<>();

    /**
     * Constructor
     */
    public MetricItemSet(String name) {
        this.name = name;
    }

    /**
     * getName
     */
    public String getName() {
        return name;
    }

    /**
     * createItem
     */
    protected abstract T createItem();

    /**
     * findMetricItem
     */
    public T findMetricItem(Map<String, String> dimensions) {
        String key = MetricUtils.getDimensionsKey(dimensions);
        T currentItem = this.itemMap.get(key);
        if (currentItem != null) {
            return currentItem;
        }
        currentItem = createItem();
        currentItem.setDimensions(dimensions);
        T oldItem = this.itemMap.putIfAbsent(key, currentItem);
        return (oldItem == null) ? currentItem : oldItem;
    }

    /**
     * snapshot
     */
    @Override
    public List<MetricItem> snapshot() {
        Map<String, T> oldItemMap = itemMap;
        this.itemMap = new ConcurrentHashMap<>();
        MetricUtils.sleepOneInterval();
        List<MetricItem> result = new ArrayList<>(oldItemMap.size());
        result.addAll(oldItemMap.values());
        return result;
    }
}
