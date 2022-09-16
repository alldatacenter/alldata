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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MetricUtils
 */
public class MetricUtils {

    public static final Logger LOGGER = LoggerFactory.getLogger(MetricUtils.class);

    /**
     * getDomain
     */
    public static String getDomain(Class<?> cls) {
        for (Annotation annotation : cls.getAnnotations()) {
            if (annotation instanceof MetricDomain) {
                MetricDomain domain = (MetricDomain) annotation;
                String name = domain.name();
                name = (name != null && name.length() > 0) ? name : cls.getName();
                return name;
            }
        }
        return cls.getName();
    }

    /**
     * getDimensionsKey
     */
    public static String getDimensionsKey(Map<String, String> dimensionMap) {
        StringBuilder builder = new StringBuilder();
        if (dimensionMap.size() <= 0) {
            return "";
        }
        //
        Set<String> fieldKeySet = dimensionMap.keySet();
        List<String> fieldKeyList = new ArrayList<>(fieldKeySet.size());
        fieldKeyList.addAll(fieldKeySet);
        Collections.sort(fieldKeyList);
        for (String fieldKey : fieldKeyList) {
            String fieldValue = dimensionMap.get(fieldKey);
            fieldValue = (fieldValue == null) ? "" : fieldValue;
            builder.append(fieldKey).append(MetricItemMBean.PROPERTY_EQUAL).append(fieldValue)
                    .append(MetricItemMBean.PROPERTY_SEPARATOR);
        }
        return builder.substring(0, builder.length() - 1);
    }

    /**
     * sleepOneInterval
     */
    public static void sleepOneInterval() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
