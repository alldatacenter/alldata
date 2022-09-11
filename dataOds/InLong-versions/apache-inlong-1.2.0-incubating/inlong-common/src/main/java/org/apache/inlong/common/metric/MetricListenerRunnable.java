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

package org.apache.inlong.common.metric;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * MetricListenerRunnable
 */
public class MetricListenerRunnable implements Runnable {

    public static final Logger LOG = LoggerFactory.getLogger(MetricListenerRunnable.class);

    private String domain;
    private List<MetricListener> listenerList;

    /**
     * Constructor
     *
     * @param domain
     * @param listenerList
     */
    public MetricListenerRunnable(String domain, List<MetricListener> listenerList) {
        this.domain = domain;
        this.listenerList = listenerList;
    }

    /**
     * run
     */
    @Override
    public void run() {
        LOG.info("begin to snapshot metric:{}", domain);
        try {
            List<MetricItemValue> itemValues = this.getItemValues();
            LOG.info("snapshot metric:{},size:{}", domain, itemValues.size());
            this.listenerList.forEach((item) -> {
                item.snapshot(domain, itemValues);
            });
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
        LOG.info("end to snapshot metric:{}", domain);
    }

    /**
     * getItemValues
     *
     * @return                              MetricItemValue List
     * @throws InstanceNotFoundException
     * @throws AttributeNotFoundException
     * @throws ReflectionException
     * @throws MBeanException
     * @throws MalformedObjectNameException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public List<MetricItemValue> getItemValues() throws InstanceNotFoundException, AttributeNotFoundException,
            ReflectionException, MBeanException, MalformedObjectNameException, ClassNotFoundException {
        StringBuilder beanName = new StringBuilder();
        beanName.append(MetricRegister.JMX_DOMAIN).append(MetricItemMBean.DOMAIN_SEPARATOR)
                .append("type=").append(domain)
                .append(MetricItemMBean.PROPERTY_SEPARATOR)
                .append("*");
        ObjectName objName = new ObjectName(beanName.toString());
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectInstance> mbeans = mbs.queryMBeans(objName, null);
        LOG.info("getItemValues for domain:{},queryMBeans:{}", domain, mbeans);
        List<MetricItemValue> itemValues = new ArrayList<>();
        for (ObjectInstance mbean : mbeans) {
            String className = mbean.getClassName();
            Class<?> clazz = ClassUtils.getClass(className);
            if (ClassUtils.isAssignable(clazz, MetricItemMBean.class)) {
                ObjectName metricObjectName = mbean.getObjectName();
                String dimensionsKey = (String) mbs.getAttribute(metricObjectName,
                        MetricItemMBean.ATTRIBUTE_KEY);
                Map<String, String> dimensions = (Map<String, String>) mbs
                        .getAttribute(metricObjectName, MetricItemMBean.ATTRIBUTE_DIMENSIONS);
                Map<String, MetricValue> metrics = (Map<String, MetricValue>) mbs
                        .invoke(metricObjectName, MetricItemMBean.METHOD_SNAPSHOT, null, null);
                MetricItemValue itemValue = new MetricItemValue(dimensionsKey, dimensions, metrics);
                LOG.info("MetricItemMBean get itemValue:{}", itemValue);
                itemValues.add(itemValue);
            } else if (ClassUtils.isAssignable(clazz, MetricItemSetMBean.class)) {
                ObjectName metricObjectName = mbean.getObjectName();
                List<MetricItem> items =
                        (List<MetricItem>) mbs.invoke(metricObjectName,
                        MetricItemMBean.METHOD_SNAPSHOT, null, null);
                /*
                  * ut will throw classCaseException if use MetricItem without Object
                 */
                for (Object itemT : items) {
                    if (itemT instanceof MetricItem) {
                        MetricItem item = (MetricItem) itemT;
                        String dimensionsKey = item.getDimensionsKey();
                        Map<String, String> dimensions = item.getDimensions();
                        Map<String, MetricValue> metrics = item.snapshot();
                        MetricItemValue itemValue = new MetricItemValue(dimensionsKey, dimensions, metrics);
                        LOG.info("MetricItemSetMBean get itemValue:{}", itemValue);
                        itemValues.add(itemValue);
                    }
                }
            }
        }
        return itemValues;
    }
}
