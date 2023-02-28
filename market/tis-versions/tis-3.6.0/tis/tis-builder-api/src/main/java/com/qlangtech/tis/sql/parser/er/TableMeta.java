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
package com.qlangtech.tis.sql.parser.er;

import org.apache.commons.lang.StringUtils;

import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-06-03 18:09
 */
public class TableMeta {

    public static boolean hasValidPrimayTableSharedKey(Optional<TableMeta> ptab) {
        return ptab.isPresent() && StringUtils.isNotEmpty(ptab.get().getSharedKey());
    }

    // 主索引表名称
    private final String tabName;

    private final String sharedKey;

    public TableMeta(String tabName, String sharedKey) {
        this.tabName = tabName;
        this.sharedKey = sharedKey;
    }

    public String getTabName() {
        return tabName;
    }

    public String getSharedKey() {
        return sharedKey;
    }
}
