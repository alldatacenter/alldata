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

import com.qlangtech.tis.sql.parser.meta.ColumnTransfer;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 表字段内存处理，例如时间格式化等，将来要做到可以自己扩充
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public class TabFieldProcessor {

    public final EntityName tabName;

    private final List<ColumnTransfer> colTransfers;

    private Map<String, ColumnTransfer> colTransfersMap;

    public TabFieldProcessor(EntityName tabName, List<ColumnTransfer> colTransfers) {
        this.tabName = tabName;
        this.colTransfers = colTransfers;
    }

    public String getName() {
        return this.tabName.getTabName();
    }

    // @JSONField(serialize = false)
    public Map<String, /**
     * col key
     */
    ColumnTransfer> colTransfersMap() {
        if (colTransfersMap == null) {
            colTransfersMap = this.colTransfers.stream().collect(Collectors.toMap((r) -> r.getColKey(), (r) -> r));
        }
        return colTransfersMap;
    }

    public List<ColumnTransfer> getColTransfers() {
        return this.colTransfers;
    }
}
