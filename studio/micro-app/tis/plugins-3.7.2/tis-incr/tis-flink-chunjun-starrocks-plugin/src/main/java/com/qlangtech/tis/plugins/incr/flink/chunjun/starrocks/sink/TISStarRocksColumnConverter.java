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

package com.qlangtech.tis.plugins.incr.flink.chunjun.starrocks.sink;

import com.dtstack.chunjun.connector.jdbc.TableCols;
import com.dtstack.chunjun.connector.starrocks.converter.StarRocksColumnConverter;
import com.dtstack.chunjun.connector.starrocks.streamload.StarRocksSinkOP;
import com.dtstack.chunjun.converter.IDeserializationConverter;
import com.dtstack.chunjun.converter.ISerializationConverter;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugins.incr.flink.cdc.AbstractRowDataMapper;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.LogicalType;

import java.util.List;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-12-15 22:12
 **/
public class TISStarRocksColumnConverter extends StarRocksColumnConverter {
    public TISStarRocksColumnConverter(int fieldCount
            , List<IDeserializationConverter> toInternalConverters
            , List<Pair<ISerializationConverter<Map<String, Object>>, LogicalType>> toExternalConverters
            , List<String> columnList) {
        super(fieldCount, toInternalConverters, toExternalConverters, columnList);
    }

    public static TISStarRocksColumnConverter create(TableCols<IColMetaGetter> sinkTabCols
    ) {

        // Map<String, Integer> col2ordMap = Maps.newHashMap();
        List<String> columns = Lists.newArrayList();
        List<Pair<ISerializationConverter<Map<String, Object>>, LogicalType>>
                toExternalConverters = Lists.newArrayList();
        List<IDeserializationConverter> toInternalConverters = Lists.newArrayList();
        // ISerializationConverter<Map<String, Object>> extrnalColConerter = null;

        int fieldCount = 0;
        List<FlinkCol> flinkCols = AbstractRowDataMapper.getAllTabColsMeta(sinkTabCols.getCols());
        for (FlinkCol col : flinkCols) {
            columns.add(col.name);
            toExternalConverters.add(Pair.of(getSerializationConverter(col), col.type.getLogicalType()));
            fieldCount++;
        }
        return new TISStarRocksColumnConverter(fieldCount, toInternalConverters, toExternalConverters, columns);
    }

    @Override
    public RowData toInternal(Object[] input) throws Exception {
        // return super.toInternal(input);
        throw new UnsupportedOperationException(" end type as source is not supported");
    }

    @Override
    public Map<String, Object> toExternal(RowData rowData, Map<String, Object> output) throws Exception {
        int index = 0;
        for (ISerializationConverter colSeri : toExternalConverters) {
            colSeri.serialize(rowData, index++, output, -1);
        }
        output.put(
                StarRocksSinkOP.COLUMN_KEY, StarRocksSinkOP.parse(rowData.getRowKind()).ordinal());
        return output;
    }

    private static ISerializationConverter<Map<String, Object>> getSerializationConverter(FlinkCol col) {
        return new StarRocksSerializationConverter(col);
    }

    public static class StarRocksSerializationConverter implements ISerializationConverter<Map<String, Object>> {
        private final FlinkCol col;

        public StarRocksSerializationConverter(FlinkCol col) {
            this.col = col;
        }

        @Override
        public void serialize(RowData rowData, int index, Map<String, Object> vals, int outPos) throws Exception {
            Object val = (rowData.isNullAt(index)) ? null : col.getRowDataVal(rowData);
//            joiner.add(
//                    val == null ? NULL_VALUE : String.valueOf(val));
            vals.put(col.name, val);
        }
    }
}
