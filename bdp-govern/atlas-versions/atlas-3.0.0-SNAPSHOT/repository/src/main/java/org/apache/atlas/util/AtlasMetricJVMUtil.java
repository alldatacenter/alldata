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
package org.apache.atlas.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class AtlasMetricJVMUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasMetricJVMUtil.class);

    private static final RuntimeMXBean RUNTIME;
    private static final OperatingSystemMXBean OS;
    private static final MemoryMXBean memBean;

    static {
        RUNTIME = ManagementFactory.getRuntimeMXBean();
        OS = ManagementFactory.getOperatingSystemMXBean();
        memBean = ManagementFactory.getMemoryMXBean();
    }

    /**
     * Collect general runtime information.
     */
    public static Map<String, Object> getRuntimeInfo() {
        Map<String, Object> vmDetails = new LinkedHashMap<>();
        vmDetails.put("name", RUNTIME.getVmName());
        vmDetails.put("version", RUNTIME.getSystemProperties().get("java.version"));
        return vmDetails;
    }

    /**
     * Add memory details
     */
    public static Map<String, Object> getMemoryDetails() {
        Map<String, Object> memory = new LinkedHashMap<>();
        heapDetails(memory);
        pooldivision(memory);
        return memory;
    }

    /**
     * Collect system information.
     */
    public static Map<String, Object> getSystemInfo() {
        Map<String, Object> values = new LinkedHashMap<>();
        String[] osInfo = {OS.getName(), OS.getArch(), OS.getVersion()};
        values.put("os.spec", String.join(", ", osInfo));
        values.put("os.vcpus", String.valueOf(OS.getAvailableProcessors()));
        return values;
    }

    /**
     * collect the pool division of java
     */
    private static void pooldivision(Map<String, Object> memory) {
        Map<String, Object> poolDivisionValues = new LinkedHashMap<>();
        for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (mpBean.getType() == MemoryType.HEAP) {
                poolDivisionValues.put(mpBean.getName(), mpBean.getUsage());
            }
        }
        memory.put("memory_pool_usages", poolDivisionValues);
    }

    /**
     * Collect java heap details
     */
    private static void heapDetails(Map<String, Object> memory) {
        MemoryUsage memHeapUsage = memBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memBean.getNonHeapMemoryUsage();
        memory.put("heapInit", String.valueOf(memHeapUsage.getInit()));
        memory.put("heapMax", String.valueOf(memHeapUsage.getMax()));
        memory.put("heapCommitted", String.valueOf(memHeapUsage.getCommitted()));
        memory.put("heapUsed", String.valueOf(memHeapUsage.getUsed()));
        memory.put("nonHeapInit", String.valueOf(nonHeapUsage.getInit()));
        memory.put("nonHeapMax", String.valueOf(nonHeapUsage.getMax()));
        memory.put("nonHeapCommitted", String.valueOf(nonHeapUsage.getCommitted()));
        memory.put("nonHeapUsed", String.valueOf(nonHeapUsage.getUsed()));
    }
}