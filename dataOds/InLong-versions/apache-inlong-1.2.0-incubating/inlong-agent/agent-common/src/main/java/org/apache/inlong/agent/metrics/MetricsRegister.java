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

import org.apache.inlong.agent.metrics.counter.CounterInt;
import org.apache.inlong.agent.metrics.counter.CounterLong;
import org.apache.inlong.agent.metrics.gauge.GaugeInt;
import org.apache.inlong.agent.metrics.gauge.GaugeLong;
import org.apache.inlong.agent.metrics.meta.MetricMeta;
import org.apache.inlong.agent.metrics.meta.MetricsMeta;
import org.apache.inlong.agent.utils.AgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * register for metrics.
 */
public class MetricsRegister {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsRegister.class);

    private static final String DOMAIN_PREFIX = "Agent:";
    private static final String MODULE_PREFIX = "module=";
    private static final String ASPECT_PREFIX = "aspect=";
    private static final String COMMA_SPLITTER = ",";

    // object name should be uniq
    private static final ConcurrentHashMap<String, ObjectName> CACHED_NAME = new ConcurrentHashMap<>();

    private MetricsRegister() {
    }

    /**
     * register object name for metric
     *
     * @param agentDynamicMBean agent mbean
     */
    private static void innerRegister(AgentDynamicMBean agentDynamicMBean) {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String nameStr = DOMAIN_PREFIX + MODULE_PREFIX + agentDynamicMBean.getModule()
                + COMMA_SPLITTER + ASPECT_PREFIX + agentDynamicMBean.getAspect();
        try {
            ObjectName tmpName = new ObjectName(nameStr);
            ObjectName objectName = CACHED_NAME.putIfAbsent(nameStr, tmpName);
            if (objectName == null) {
                mbs.registerMBean(agentDynamicMBean, tmpName);
            }
        } catch (Exception ex) {
            LOGGER.error("exception while register mbean", ex);
        }
    }

    /**
     * register metric for object
     */
    public static void register(String module, String aspect, String desc, Object source) {
        List<MetricMeta> metricMetaList = handleFieldAnnotation(source);
        MetricsMeta metricsMeta = handleClassAnnotation(source, metricMetaList);
        if (metricsMeta != null) {
            innerRegister(new AgentDynamicMBean(module, aspect, desc, metricsMeta, source));
        } else {
            LOGGER.error("Cannot find Metrics annotation in {}, invalid metric", source);
        }
    }

    /**
     * handle class level annotation
     */
    private static MetricsMeta handleClassAnnotation(Object source,
            List<MetricMeta> metricMetaList) {
        for (Annotation annotation : source.getClass().getAnnotations()) {
            if (annotation instanceof Metrics) {
                return MetricsMeta.build((Metrics) annotation, metricMetaList);
            }
        }
        return null;
    }

    private static boolean initFieldByType(Object source, Field field) {
        try {
            if (field.getType() == CounterInt.class) {
                field.set(source, new CounterInt());
                return true;
            } else if (field.getType() == CounterLong.class) {
                field.set(source, new CounterLong());
                return true;
            } else if (field.getType() == GaugeInt.class) {
                field.set(source, new GaugeInt());
                return true;
            } else if (field.getType() == GaugeLong.class) {
                field.set(source, new GaugeLong());
                return true;
            } else if (field.getType() == Tag.class) {
                field.set(source, new Tag());
                return true;
            } else {
                throw new MetricException("field type error " + field.getType().toString());
            }
        } catch (MetricException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MetricException("Error setting field " + field
                    + " annotated with metric", ex);
        }
    }

    /**
     * handle field annotation
     */
    private static List<MetricMeta> handleFieldAnnotation(Object source) {
        List<MetricMeta> result = new ArrayList<>();
        for (Field field : AgentUtils.getDeclaredFieldsIncludingInherited(source.getClass())) {
            field.setAccessible(true);
            for (Annotation fieldAnnotation : field.getAnnotations()) {
                if (fieldAnnotation instanceof Metric) {
                    if (initFieldByType(source, field)) {
                        result.add(MetricMeta.build((Metric) fieldAnnotation, field));
                    }
                    break;
                }
            }
        }
        return result;
    }
}

