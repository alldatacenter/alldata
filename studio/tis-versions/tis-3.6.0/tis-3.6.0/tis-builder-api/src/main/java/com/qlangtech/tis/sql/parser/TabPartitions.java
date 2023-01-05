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

package com.qlangtech.tis.sql.parser;

import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
import org.apache.commons.lang.StringUtils;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: baisui 百岁
 * @create: 2020-10-02 07:36
 **/
public class TabPartitions {
    private final Map<IDumpTable, ITabPartition> tabPartition;

    public TabPartitions(Map<IDumpTable, ITabPartition> tabPartition) {
        this.tabPartition = tabPartition;
    }

    public int size() {
        return tabPartition.size();
    }

    public final Optional<Map.Entry<IDumpTable, ITabPartition>> findTablePartition(String dbName, String tableName) {
        return findTablePartition(true, dbName, tableName);
    }

    public void putPt(IDumpTable table, ITabPartition pt) {
        this.tabPartition.put(table, pt);
    }

    public Optional<ITabPartition> getMinTablePartition() {
        Optional<ITabPartition> min = tabPartition.values().stream().min(Comparator.comparing((r) -> Long.parseLong(r.getPt())));
        return min;
    }


    protected Optional<Map.Entry<IDumpTable, ITabPartition>> findTablePartition(boolean dbNameCriteria, String dbName, String tableName) {
        return tabPartition.entrySet().stream().filter((r) -> {
            return (!dbNameCriteria || StringUtils.equals(r.getKey().getDbName(), dbName))
                    && StringUtils.equals(r.getKey().getTableName(), tableName);
        }).findFirst();
    }

    public final Optional<Map.Entry<IDumpTable, ITabPartition>> findTablePartition(String tableName) {
        return this.findTablePartition(false, null, tableName);
    }

    public String joinFullNames() {
        return tabPartition.keySet().stream().map((r) -> r.getFullName()).collect(Collectors.joining(","));
    }

    @Override
    public String toString() {
        return this.tabPartition.entrySet().stream().map((ee) -> "[" + ee.getKey() + "->" + ee.getValue().getPt() + "]").collect(Collectors.joining(","));
    }
}
