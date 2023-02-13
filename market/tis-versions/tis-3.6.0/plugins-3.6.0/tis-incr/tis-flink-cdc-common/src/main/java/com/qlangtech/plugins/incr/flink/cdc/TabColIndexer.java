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

package com.qlangtech.plugins.incr.flink.cdc;

import com.google.common.collect.ImmutableMap;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-12-18 22:03
 **/
public class TabColIndexer implements Serializable {
    final Map<String, Map<String, CMeta>> tabColsMapper;

    public TabColIndexer(List<ISelectedTab> tabs) {
        ImmutableMap.Builder<String, Map<String, CMeta>> tabColsMapperBuilder = ImmutableMap.builder();
        for (ISelectedTab tab : tabs) {
            tabColsMapperBuilder.put(tab.getName()
                    , tab.getCols().stream().collect(Collectors.toMap((c) -> c.getName(), (c) -> c)));
        }
        this.tabColsMapper = tabColsMapperBuilder.build();
    }

    public CMeta getColMeta(String tableName, String colName) {
        Map<String, CMeta> tabCols = tabColsMapper.get(tableName);
        if (tabCols == null) {
            throw new IllegalStateException("table:" + tableName + " relevant tabCols can not be null");
        }
        return tabCols.get(colName);
    }
}
