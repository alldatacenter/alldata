/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.incr.flink;

import com.qlangtech.plugins.incr.flink.cdc.BiFunction;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;

import java.io.Serializable;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-26 11:24
 **/
public class FlinkColMapper implements Serializable {
    private final Map<String, FlinkCol> colMapper;

    public FlinkColMapper(Map<String, FlinkCol> colMapper) {
        this.colMapper = colMapper;
    }

    public BiFunction getSourceDTOColValProcess(String colName) {
        FlinkCol fcol = colMapper.get(colName);
        if (fcol == null) {
            return null;
        }
        return fcol.getSourceDTOColValProcess();
    }
}
