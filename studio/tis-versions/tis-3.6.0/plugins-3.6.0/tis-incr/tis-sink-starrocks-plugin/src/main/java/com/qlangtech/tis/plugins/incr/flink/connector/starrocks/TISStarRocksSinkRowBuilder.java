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

package com.qlangtech.tis.plugins.incr.flink.connector.starrocks;

import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.starrocks.connector.flink.row.sink.StarRocksSinkOP;
import com.starrocks.connector.flink.row.sink.StarRocksSinkRowBuilder;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-05 13:58
 **/
public class TISStarRocksSinkRowBuilder implements StarRocksSinkRowBuilder<RowData> {
    private final List<FlinkCol> cols;

    public TISStarRocksSinkRowBuilder(List<FlinkCol> cols) {
        this.cols = cols;
    }

    @Override
    public void accept(Object[] slots, RowData rowData) {
        FlinkCol cmeta = null;
        for (int i = 0; i < cols.size(); i++) {
            cmeta = cols.get(i);
            ;
//                        cmeta.getType()
//
//                        streamRowData.get
//
//                        cmeta.process.apply()

//                        slots[i] = (RowKind.DELETE == streamRowData.getRowKind())
//                                ? streamRowData.getBefore().get(fieldKeys[i])
//                                : streamRowData.getAfter().get(fieldKeys[i]);
            slots[i] = cmeta.getRowDataVal(rowData);//  getRowDataColVal(rowData, i, cmeta);
        }
        StarRocksSinkOP sinkOp = getSinkOP(rowData.getRowKind());
        slots[cols.size()] = sinkOp.ordinal();
    }


    private static StarRocksSinkOP getSinkOP(RowKind evt) {
        switch (evt) {
            case DELETE:
                return StarRocksSinkOP.DELETE;
            case UPDATE_AFTER:
            default:
                return StarRocksSinkOP.UPSERT;
        }
    }
}
