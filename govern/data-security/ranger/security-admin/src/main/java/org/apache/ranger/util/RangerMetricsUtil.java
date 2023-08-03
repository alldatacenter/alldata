/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.util;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;

/**
 * Connect Worker system and runtime information.
 */
@Component
public class RangerMetricsUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RangerMetricsUtil.class);
    private static final OperatingSystemMXBean OS;
    private static final MemoryMXBean MEM_BEAN;
    public static final String NL = System.getProperty("line.separator");

    static {
        OS = ManagementFactory.getOperatingSystemMXBean();
        MEM_BEAN = ManagementFactory.getMemoryMXBean();
    }

    public Map<String, Object> getValues() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerJVMMetricUtil.getValues()");
        }
        
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("os.spec", StringUtils.join(Arrays.asList(addSystemInfo()), ", "));
        values.put("os.vcpus", String.valueOf(OS.getAvailableProcessors()));
        values.put("memory", addMemoryDetails());

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerJVMMetricUtil.getValues()" + values);
        }

        return values;
    }

    /**
     * collect the pool division of java
     */
    protected Map<String, Object> getPoolDivision() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerJVMMetricUtil.getPoolDivision()");
        }

        Map<String, Object> poolDivisionValues = new LinkedHashMap<>();
        for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (mpBean.getType() == MemoryType.HEAP) {
                poolDivisionValues.put(mpBean.getName(), mpBean.getUsage());
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerJVMMetricUtil.getPoolDivision()" + poolDivisionValues);
        }

        return poolDivisionValues;
    }

    /**
     * Add memory details
     */
    protected Map<String, Object> addMemoryDetails() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerJVMMetricUtil.addMemoryDetails()");
        }

        Map<String, Object> memory  = new LinkedHashMap<>();
        MemoryUsage memHeapUsage = MEM_BEAN.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = MEM_BEAN.getNonHeapMemoryUsage();
        memory.put("heapInit", String.valueOf(memHeapUsage.getInit()));
        memory.put("heapMax", String.valueOf(memHeapUsage.getMax()));
        memory.put("heapCommitted", String.valueOf(memHeapUsage.getCommitted()));
        memory.put("heapUsed", String.valueOf(memHeapUsage.getUsed()));
        memory.put("nonHeapInit", String.valueOf(nonHeapUsage.getInit()));
        memory.put("nonHeapMax", String.valueOf(nonHeapUsage.getMax()));
        memory.put("nonHeapCommitted", String.valueOf(nonHeapUsage.getCommitted()));
        memory.put("nonHeapUsed", String.valueOf(nonHeapUsage.getUsed()));
        memory.put("memory_pool_usages", getPoolDivision());

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerJVMMetricUtil.addMemoryDetails()" + memory);
        }

        return memory;
    }

    /**
     * Collect system information.
     */
    protected String[] addSystemInfo() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerJVMMetricUtil.addSystemInfo()");
        }

        String[] osInfo = { OS.getName(), OS.getArch(), OS.getVersion() };
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerJVMMetricUtil.addSystemInfo()" + osInfo);
        }

        return osInfo;
    }
}
