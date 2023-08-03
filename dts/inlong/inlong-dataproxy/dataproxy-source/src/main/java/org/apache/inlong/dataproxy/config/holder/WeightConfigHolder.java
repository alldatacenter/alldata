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

package org.apache.inlong.dataproxy.config.holder;

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Save weight configure info
 */
public class WeightConfigHolder extends PropertiesHolder {

    private static final String weightConfigFileName = "weight.properties";
    private static final Logger LOG = LoggerFactory.getLogger(WeightConfigHolder.class);

    private static final String KEY_WEIGHT_CPU = "cpuWeight";
    private static final double VAL_DEF_WEIGHT_CPU = 1;
    private static final String KEY_WEIGHT_NET_IN = "netinWeight";
    private static final double VAL_DEF_WEIGHT_NET_IN = 0.5;
    private static final String KEY_WEIGHT_NET_OUT = "netoutWeight";
    private static final double VAL_DEF_WEIGHT_NET_OUT = 0.5;
    private static final String KEY_WEIGHT_TCP = "tcpWeight";
    private static final double VAL_DEF_WEIGHT_TCP = 0;
    private static final String KEY_WEIGHT_CPU_THRESHOLD = "cpuThreshold";
    private static final double VAL_DEF_WEIGHT_CPU_THRESHOLD = 85;
    // cache configure
    private final AtomicDouble cachedCpuWeight = new AtomicDouble(VAL_DEF_WEIGHT_CPU);
    private final AtomicDouble cachedNetInWeight = new AtomicDouble(VAL_DEF_WEIGHT_NET_IN);
    private final AtomicDouble cachedNetOutWeight = new AtomicDouble(VAL_DEF_WEIGHT_NET_OUT);
    private final AtomicDouble cachedTcpWeight = new AtomicDouble(VAL_DEF_WEIGHT_TCP);
    private final AtomicDouble cachedCpuThreshold = new AtomicDouble(VAL_DEF_WEIGHT_CPU_THRESHOLD);

    public WeightConfigHolder() {
        super(weightConfigFileName);
    }

    public double getCachedCpuWeight() {
        return cachedCpuWeight.get();
    }

    public double getCachedNetInWeight() {
        return cachedNetInWeight.get();
    }

    public double getCachedNetOutWeight() {
        return cachedNetOutWeight.get();
    }

    public double getCachedTcpWeight() {
        return cachedTcpWeight.get();
    }

    public double getCachedCpuThreshold() {
        return cachedCpuThreshold.get();
    }

    @Override
    protected Map<String, String> filterInValidRecords(Map<String, String> configMap) {
        Map<String, String> filteredMap = new HashMap<>(configMap.size());
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            if (entry == null
                    || StringUtils.isBlank(entry.getKey())
                    || StringUtils.isBlank(entry.getValue())) {
                continue;
            }
            try {
                Double.parseDouble(entry.getValue());
            } catch (Throwable e) {
                continue;
            }
            filteredMap.put(entry.getKey().trim(), entry.getValue().trim());
        }
        return filteredMap;
    }

    @Override
    protected boolean updateCacheData() {
        // get cpu weight
        double newVal = VAL_DEF_WEIGHT_CPU;
        String tmpStrVal = confHolder.get(KEY_WEIGHT_CPU);
        if (StringUtils.isNotBlank(tmpStrVal)) {
            try {
                newVal = Double.parseDouble(tmpStrVal);
            } catch (Throwable e) {
                //
            }
        }
        cachedCpuWeight.set(newVal);
        // get net-in weight
        newVal = VAL_DEF_WEIGHT_NET_IN;
        tmpStrVal = confHolder.get(KEY_WEIGHT_NET_IN);
        if (StringUtils.isNotBlank(tmpStrVal)) {
            try {
                newVal = Double.parseDouble(tmpStrVal);
            } catch (Throwable e) {
                //
            }
        }
        cachedNetInWeight.set(newVal);
        // get net-out weight
        newVal = VAL_DEF_WEIGHT_NET_OUT;
        tmpStrVal = confHolder.get(KEY_WEIGHT_NET_OUT);
        if (StringUtils.isNotBlank(tmpStrVal)) {
            try {
                newVal = Double.parseDouble(tmpStrVal);
            } catch (Throwable e) {
                //
            }
        }
        cachedNetOutWeight.set(newVal);
        // get tcp weight
        newVal = VAL_DEF_WEIGHT_TCP;
        tmpStrVal = confHolder.get(KEY_WEIGHT_TCP);
        if (StringUtils.isNotBlank(tmpStrVal)) {
            try {
                newVal = Double.parseDouble(tmpStrVal);
            } catch (Throwable e) {
                //
            }
        }
        cachedTcpWeight.set(newVal);
        // get cpu threshold weight
        newVal = VAL_DEF_WEIGHT_CPU_THRESHOLD;
        tmpStrVal = confHolder.get(KEY_WEIGHT_CPU_THRESHOLD);
        if (StringUtils.isNotBlank(tmpStrVal)) {
            try {
                newVal = Double.parseDouble(tmpStrVal);
            } catch (Throwable e) {
                //
            }
        }
        cachedCpuThreshold.set(newVal);
        return true;
    }
}
