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
package com.qlangtech.tis.order.center;

import com.google.common.collect.Maps;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.fullbuild.taskflow.IFlatTableBuilder;

import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-05-20 12:10
 */
public class TestJoinTaskContext implements IJoinTaskContext {
    private final ExecutePhaseRange execPhaseRange;

    public TestJoinTaskContext() {
        this(ExecutePhaseRange.fullRange());
    }

    public TestJoinTaskContext(ExecutePhaseRange execPhaseRange) {
        this.execPhaseRange = execPhaseRange;
    }

//    @Override
//    public IFlatTableBuilder getFlatTableBuilder() {
//        return null;
//    }

    @Override
    public IAppSourcePipelineController getPipelineController() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ExecutePhaseRange getExecutePhaseRange() {
        return this.execPhaseRange;
    }

    @Override
    public int getTaskId() {
        return 0;
    }

    private Map<String, Object> attrs = Maps.newHashMap();

    @Override
    public <T> T getAttribute(String key) {
        return (T) attrs.get(key);
    }

    @Override
    public void setAttribute(String key, Object v) {
        attrs.put(key, v);
    }

    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public boolean getBoolean(String key) {
        return false;
    }

    @Override
    public int getInt(String key) {
        return 0;
    }

    @Override
    public long getLong(String key) {
        return 0;
    }

    @Override
    public String getPartitionTimestamp() {
        return null;
    }

    @Override
    public String getIndexName() {
        return null;
    }

    @Override
    public boolean hasIndexName() {
        return false;
    }

    @Override
    public int getIndexShardCount() {
        return 0;
    }
}
