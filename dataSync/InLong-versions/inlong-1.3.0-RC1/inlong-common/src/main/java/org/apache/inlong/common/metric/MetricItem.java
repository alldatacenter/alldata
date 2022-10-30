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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MetricItem
 */
public abstract class MetricItem implements MetricItemMBean {

    public static final Logger LOGGER = LoggerFactory.getLogger(MetricItem.class);

    private String key;
    private Map<String, String> dimensions;
    private Map<String, AtomicLong> countMetrics;
    private Map<String, AtomicLong> gaugeMetrics;

    /**
     * Get declare fields.
     */
    public static List<Field> getDeclaredFieldsIncludingInherited(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        // check whether parent exists
        while (clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * getDimensionsKey
     */
    @Override
    public String getDimensionsKey() {
        if (key != null) {
            return key;
        }
        Map<String, String> dimensionMap = this.getDimensions();
        this.key = MetricUtils.getDimensionsKey(dimensionMap);
        return key;
    }

    /**
     * getDimensions
     */
    @Override
    public Map<String, String> getDimensions() {
        if (dimensions != null) {
            return dimensions;
        }
        dimensions = new HashMap<>();

        // get all fields
        List<Field> fields = getDeclaredFieldsIncludingInherited(this.getClass());

        // filter dimension fields
        for (Field field : fields) {
            field.setAccessible(true);
            for (Annotation fieldAnnotation : field.getAnnotations()) {
                if (fieldAnnotation instanceof Dimension) {
                    Dimension dimension = (Dimension) fieldAnnotation;
                    String name = dimension.name();
                    name = (name != null && name.length() > 0) ? name : field.getName();
                    try {
                        Object fieldValue = field.get(this);
                        String value = (fieldValue == null) ? "" : fieldValue.toString();
                        dimensions.put(name, value);
                    } catch (Throwable t) {
                        LOGGER.error(t.getMessage(), t);
                    }
                    break;
                }
            }
        }
        return dimensions;
    }

    /**
     * set dimensions
     *
     * @param dimensions the dimensions to set
     */
    public void setDimensions(Map<String, String> dimensions) {
        this.dimensions = new HashMap<String, String>();
        this.dimensions.putAll(dimensions);
    }

    /**
     * snapshot
     */
    @Override
    public Map<String, MetricValue> snapshot() {
        if (this.countMetrics == null || this.gaugeMetrics == null) {
            this.initMetricField();
        }
        //
        Map<String, MetricValue> metrics = new HashMap<>();
        this.countMetrics.forEach((key, value) -> {
            metrics.put(key, MetricValue.of(key, value.getAndSet(0)));
        });
        this.gaugeMetrics.forEach((key, value) -> {
            metrics.put(key, MetricValue.of(key, value.get()));
        });
        return metrics;
    }

    /**
     * initMetricField
     */
    protected void initMetricField() {
        this.countMetrics = new HashMap<>();
        this.gaugeMetrics = new HashMap<>();

        // get all fields
        List<Field> fields = getDeclaredFieldsIncludingInherited(this.getClass());

        // filter metric fields
        for (Field field : fields) {
            field.setAccessible(true);
            for (Annotation fieldAnnotation : field.getAnnotations()) {
                if (fieldAnnotation instanceof CountMetric) {
                    CountMetric countMetric = (CountMetric) fieldAnnotation;
                    String name = countMetric.name();
                    name = (name != null && name.length() > 0) ? name : field.getName();
                    try {
                        Object fieldValue = field.get(this);
                        if (fieldValue instanceof AtomicLong) {
                            this.countMetrics.put(name, (AtomicLong) fieldValue);
                        }
                    } catch (Throwable t) {
                        LOGGER.error(t.getMessage(), t);
                    }
                    break;
                } else if (fieldAnnotation instanceof GaugeMetric) {
                    GaugeMetric gaugeMetric = (GaugeMetric) fieldAnnotation;
                    String name = gaugeMetric.name();
                    name = (name != null && name.length() > 0) ? name : field.getName();
                    try {
                        Object fieldValue = field.get(this);
                        if (fieldValue instanceof AtomicLong) {
                            this.gaugeMetrics.put(name, (AtomicLong) fieldValue);
                        }
                    } catch (Throwable t) {
                        LOGGER.error(t.getMessage(), t);
                    }
                    break;
                }
            }
        }
    }
}
