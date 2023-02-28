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

package com.qlangtech.tis.plugin.ds.tidb;

import com.pingcap.tikv.meta.TiTableInfo;

/**
 * @author: baisui 百岁
 * @create: 2020-12-04 16:04
 **/
public class TiTableInfoWrapper {
    public final TiTableInfo tableInfo;
    boolean hasGetRowSize = false;

    public TiTableInfoWrapper(TiTableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    /**
     * 如果有多个子任务获取rowsize则只有一个partition会返回整个table的rowSize，其他子任务返回0
     *
     * @return
     */
    public int getRowSize() {
        if (!hasGetRowSize) {
            hasGetRowSize = true;
            return (int) tableInfo.getEstimatedRowSizeInByte();
        }
        return 0;
    }
}
