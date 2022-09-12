/**
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

package org.apache.inlong.sort.standalone.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.flume.Context;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * MetricObserver
 */
public class MetricObserver {

    public static final Logger LOG = InlongLoggerFactory.getLogger(MetricObserver.class);
    private static final AtomicBoolean isInited = new AtomicBoolean(false);
    private static ScheduledExecutorService statExecutor = Executors.newScheduledThreadPool(5);

    /**
     * init
     * 
     * @param commonProperties
     */
    public static void init(Map<String, String> commonProperties) {
        if (!isInited.compareAndSet(false, true)) {
            return;
        }
        // init
        Context context = new Context(commonProperties);
        // get domain name list
        String metricDomains = context.getString(MetricListener.KEY_METRIC_DOMAINS);
        if (StringUtils.isBlank(metricDomains)) {
            return;
        }
        // split domain name
        String[] domains = metricDomains.split("\\s+");
        for (String domain : domains) {
            // get domain parameters
            Context domainContext = new Context(
                    context.getSubProperties(MetricListener.KEY_METRIC_DOMAINS + "." + domain + "."));
            List<MetricListener> listenerList = parseDomain(domain, domainContext);
            // no listener
            if (listenerList == null || listenerList.size() <= 0) {
                continue;
            }
            // get snapshot interval
            long snapshotInterval = domainContext.getLong(MetricListener.KEY_SNAPSHOT_INTERVAL, 60000L);
            LOG.info("begin to register domain:{},MetricListeners:{},snapshotInterval:{}", domain, listenerList,
                    snapshotInterval);
            statExecutor.scheduleWithFixedDelay(new MetricListenerRunnable(domain, listenerList), snapshotInterval,
                    snapshotInterval, TimeUnit.MILLISECONDS);
        }

    }

    /**
     * parseDomain
     * 
     * @param  domain
     * @param  context
     * @return
     */
    private static List<MetricListener> parseDomain(String domain, Context domainContext) {
        String listeners = domainContext.getString(MetricListener.KEY_DOMAIN_LISTENERS);
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
                continue;
            }
        }
        return listenerList;
    }
}
