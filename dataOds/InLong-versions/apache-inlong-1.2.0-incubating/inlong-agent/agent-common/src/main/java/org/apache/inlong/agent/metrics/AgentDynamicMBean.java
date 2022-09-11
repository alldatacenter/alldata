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

package org.apache.inlong.agent.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import org.apache.inlong.agent.metrics.meta.MetricMeta;
import org.apache.inlong.agent.metrics.meta.MetricsMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic MBean for agent
 */
public class AgentDynamicMBean implements DynamicMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentDynamicMBean.class);

    private final ConcurrentHashMap<String, MetricSnapshot<?>> snapshotAttrs = new ConcurrentHashMap<>();
    private final MBeanInfo mBeanInfo;
    private final List<MBeanAttributeInfo> attrs;
    private final MetricsMeta metricsMeta;
    private final String module;
    private final String aspect;
    private final String desc;

    public AgentDynamicMBean(String module, String aspect, String desc,
        MetricsMeta metricsMeta, Object source) {
        this.module = module;
        this.aspect = aspect;
        this.desc = desc;
        this.metricsMeta = metricsMeta;
        this.attrs = new ArrayList<>();
        this.mBeanInfo = metricsMetaToInfo();
        formatSnapshotList(source);
    }

    private void formatSnapshotList(Object source) {
        for (MetricMeta metricMeta : this.metricsMeta.getMetricMetaList()) {
            try {
                snapshotAttrs.put(metricMeta.getName(),
                    (MetricSnapshot<?>) metricMeta.getField().get(source));
            } catch (Exception ex) {
                LOGGER.error("exception while adding snapshot list", ex);
            }
        }
    }

    private MBeanInfo metricsMetaToInfo() {
        // overwrite name, desc from MetricsMeta if not null.
        String name = this.module == null ? metricsMeta.getName() : this.module;
        String description = this.desc == null ? metricsMeta.getDesc() : this.desc;

        for (MetricMeta fieldMetricMeta : metricsMeta.getMetricMetaList()) {
            attrs.add(new MBeanAttributeInfo(fieldMetricMeta.getName(),
                fieldMetricMeta.getType(), fieldMetricMeta.getDesc(), true, false, false));
        }
        return new MBeanInfo(name, description, attrs.toArray(new MBeanAttributeInfo[0]),
            null, null, null);
    }

    @Override
    public Object getAttribute(String attribute) {
        MetricSnapshot<?> snapshot = snapshotAttrs.get(attribute);
        return new Attribute(attribute, snapshot.snapshot());
    }

    @Override
    public void setAttribute(Attribute attribute) {
        throw new UnsupportedOperationException("Metrics are read-only.");
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList attributeList = new AttributeList();
        for (String attrKey : attributes) {
            MetricSnapshot<?> snapshot = snapshotAttrs.get(attrKey);
            if (snapshot != null) {
                attributeList.add(new Attribute(attrKey, snapshot.snapshot()));
            }
        }
        return attributeList;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        throw new UnsupportedOperationException("Metrics are read-only.");
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mBeanInfo;
    }

    public String getModule() {
        return module;
    }

    public String getAspect() {
        return aspect;
    }
}
