/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.fullbuild.taskflow;

import com.google.common.collect.Maps;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.order.center.IParamContext;

import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestParamContext implements IParamContext {

    private final Map<String, String> params = Maps.newHashMap();

    public void set(String key, String value) {
        this.params.put(key, value);
    }

    @Override
    public ExecutePhaseRange getExecutePhaseRange() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getPartitionTimestampWithMillis() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(String key) {
        return params.get(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(params.get(key));
    }

    @Override
    public int getInt(String key) {
        return Integer.parseInt(params.get(key));
    }

    @Override
    public long getLong(String key) {
        return Long.parseLong(params.get(key));
    }
}
