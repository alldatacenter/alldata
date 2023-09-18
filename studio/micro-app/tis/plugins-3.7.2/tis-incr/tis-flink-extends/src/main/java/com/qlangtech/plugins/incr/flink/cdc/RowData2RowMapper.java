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

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.conversion.RowRowConverter;
import org.apache.flink.types.Row;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-31 13:30
 **/
public class RowData2RowMapper implements MapFunction<RowData, Row> {
    //  private final List<FlinkCol> cols;
    private final RowRowConverter rowRowConverter;

    public RowData2RowMapper(List<FlinkCol> cols) {
        DataTypes.Field[] fields = new DataTypes.Field[cols.size()];
        int index = 0;
        for (FlinkCol cm : cols) {
            fields[index++] = DataTypes.FIELD(cm.name, cm.type);// new DataTypes.Field(cm.name,cm.type);
        }
        this.rowRowConverter = RowRowConverter.create(DataTypes.ROW(fields).notNull());
    }


    @Override
    public Row map(RowData r) throws Exception {
        return rowRowConverter.toExternal(r);
        //  throw new UnsupportedOperationException();
//        GenericRowData rr = (GenericRowData) r;
//
//        Row row = new Row(r.getRowKind(), cols.size());
//        int index = 0;
//        Object val = null;
//        for (FlinkCol col : cols) {
//            if (r.isNullAt(index)) {
//                row.setField(index, null);
//            } else {
//                val = rr.getField(index);
//                row.setField(index, col.processVal(val));
//            }
//            index++;
//        }
//        return row;

    }
}
