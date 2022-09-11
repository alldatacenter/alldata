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

package org.apache.inlong.tubemq.corerpc;

import java.util.HashMap;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;

public class RpcConfig {

    private final Map<String, Object> params = new HashMap<>();

    public RpcConfig() {

    }

    public void put(String key, Object value) {
        params.put(key, value);
    }

    public String getString(String key) {
        Object value = params.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public String getString(final String key, final String defaultValue) {
        String value = getString(key);
        if (TStringUtils.isNotBlank(value)) {
            return value.trim();
        } else {
            return defaultValue;
        }
    }

    public int getInt(final String key, final int defaultValue) {
        String value = getString(key);
        if (TStringUtils.isNotBlank(value)) {
            return Integer.parseInt(value.trim());
        } else {
            return defaultValue;
        }
    }

    public double getDouble(final String key, final double defaultValue) {
        String value = getString(key);
        if (TStringUtils.isNotBlank(value)) {
            return Double.parseDouble(value.trim());
        } else {
            return defaultValue;
        }
    }

    public long getLong(final String key, final long defaultValue) {
        String value = getString(key);
        if (TStringUtils.isNotBlank(value)) {
            return Long.parseLong(value.trim());
        } else {
            return defaultValue;
        }
    }

    public boolean getBoolean(final String key, final boolean defaultValue) {
        String value = getString(key);
        if (TStringUtils.isNotBlank(value)) {
            return Boolean.parseBoolean(value.trim());
        } else {
            return defaultValue;
        }
    }
}
