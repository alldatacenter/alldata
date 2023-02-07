/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MetricObserver
 */
public class MetricObserver {

    public static final Logger LOG = LoggerFactory.getLogger(MetricObserver.class);
    private static final AtomicBoolean isInited = new AtomicBoolean(false);
    private static ScheduledExecutorService statExecutor = Executors.newScheduledThreadPool(5);

    /**
     * init
     */
    public static void init(Map<String, String> commonProperties) {
        if (!isInited.compareAndSet(false, true)) {
            return;
        }
        // get domain name list
        String metricDomains = commonProperties.get(MetricListener.KEY_METRIC_DOMAINS);
        if (StringUtils.isBlank(metricDomains)) {
            return;
        }
        // split domain name
        String[] domains = metricDomains.split("\\s+");
        for (String domain : domains) {
            // get domain parameters
            Map<String, String> domainMap = getSubProperties(commonProperties,
                    MetricListener.KEY_METRIC_DOMAINS + "." + domain + ".");
            List<MetricListener> listenerList = parseDomain(domainMap);
            // no listener
            if (listenerList == null || listenerList.size() <= 0) {
                continue;
            }
            // get snapshot interval
            long snapshotInterval = Long.parseLong(
                    domainMap.getOrDefault(MetricListener.KEY_SNAPSHOT_INTERVAL, "60000"));
            LOG.info("begin to register domain:{}, MetricListeners:{}, snapshotInterval:{}",
                    domain, listenerList, snapshotInterval);
            statExecutor.scheduleWithFixedDelay(new MetricListenerRunnable(domain, listenerList), snapshotInterval,
                    snapshotInterval, TimeUnit.MILLISECONDS);
        }

    }

    /**
     * parseDomain
     */
    private static List<MetricListener> parseDomain(Map<String, String> domainMap) {
        String listeners = domainMap.get(MetricListener.KEY_DOMAIN_LISTENERS);
        if (StringUtils.isBlank(listeners)) {
            return null;
        }
        String[] listenerTypes = listeners.split("\\s+");
        List<MetricListener> listenerList = new ArrayList<>();
        for (String listenerType : listenerTypes) {
            // new listener object
            try {
                Class<?> listenerClass = ClassUtils.getClass(listenerType);
                Object listenerObject = listenerClass.getDeclaredConstructor().newInstance();
                if (listenerObject == null || !(listenerObject instanceof MetricListener)) {
                    LOG.error("{} is not instance of MetricListener.", listenerType);
                    continue;
                }
                final MetricListener listener = (MetricListener) listenerObject;
                listenerList.add(listener);
            } catch (Throwable t) {
                LOG.error("Fail to init MetricListener:{},error:{}",
                        listenerType, t.getMessage());
            }
        }
        return listenerList;
    }

    /**
     * Get properties.
     */
    public static ImmutableMap<String, String> getSubProperties(Map<String, String> commonProperties,
            String prefix) {
        Preconditions
                .checkArgument(prefix.endsWith("."),
                        "The given prefix does not end with a period (" + prefix + ")");
        Map<String, String> result = Maps.newHashMap();
        synchronized (commonProperties) {
            Iterator var4 = commonProperties.entrySet().iterator();
            while (var4.hasNext()) {
                Entry<String, String> entry = (Entry) var4.next();
                String key = (String) entry.getKey();
                if (key.startsWith(prefix)) {
                    String name = key.substring(prefix.length());
                    result.put(name, entry.getValue());
                }
            }

            return ImmutableMap.copyOf(result);
        }
    }
}
