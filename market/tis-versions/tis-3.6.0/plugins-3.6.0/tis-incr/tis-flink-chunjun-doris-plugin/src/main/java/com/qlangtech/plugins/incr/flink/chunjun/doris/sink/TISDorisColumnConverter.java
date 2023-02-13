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

package com.qlangtech.plugins.incr.flink.chunjun.doris.sink;

import com.dtstack.chunjun.connector.jdbc.TableCols;
import com.dtstack.chunjun.converter.AbstractRowConverter;
import com.dtstack.chunjun.converter.IDeserializationConverter;
import com.dtstack.chunjun.converter.ISerializationConverter;
import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugins.incr.flink.cdc.AbstractRowDataMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.table.data.RowData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-18 11:03
 **/
public class TISDorisColumnConverter
        extends AbstractRowConverter<RowData, RowData, List<String>, DataType> {

    private List<String> fullColumn;
    private List<String> columnNames;
    // private final DorisConf options;

    private static final String NULL_VALUE = "\\N";

    private final Map<String, Integer> col2ordMap;

    private TISDorisColumnConverter(//DorisConf options,
                                    Map<String, Integer> col2ordMap,
                                    int fieldCount, List<IDeserializationConverter> toInternalConverters
            , List<Pair<ISerializationConverter<List<String>>, DataType>> toExternalConverters) {
        super(fieldCount, toInternalConverters, toExternalConverters);
        //   this.options = options;
        this.col2ordMap = col2ordMap;
    }


    public static TISDorisColumnConverter create(TableCols<IColMetaGetter> sinkTabCols  //DorisConf options
    ) {
        // FieldConf col = null;
        //DataType dataType = null;
        Map<String, Integer> col2ordMap = Maps.newHashMap();

        List<Pair<ISerializationConverter<List<String>>, DataType>>
                toExternalConverters = Lists.newArrayList();
        List<IDeserializationConverter> toInternalConverters = Lists.newArrayList();
        ISerializationConverter extrnalColConerter = null;

        int fieldCount = 0;
        List<FlinkCol> flinkCols = AbstractRowDataMapper.getAllTabColsMeta(sinkTabCols.getCols());
        for (FlinkCol col : flinkCols) {

            // dataType = cmeta.getType();
            col2ordMap.put(col.name, fieldCount);
            extrnalColConerter = wrapNullableExternalConverter(getSerializationConverter(col));
            toExternalConverters.add(Pair.of(extrnalColConerter, col.colType));
            fieldCount++;

        }


//        for (IColMetaGetter cmeta : sinkTabCols.getCols()) {
//
//            dataType = cmeta.getType();
//            col2ordMap.put(cmeta.getName(), fieldCount);
//            extrnalColConerter = wrapNullableExternalConverter(getSerializationConverter(dataType));
//            toExternalConverters.add(Pair.of(extrnalColConerter, dataType));
//            fieldCount++;
//        }

//        int fieldCount = options.getColumn().size();
//        for (int i = 0; i < options.getColumn().size(); i++) {
//            col = options.getColumn().get(i);
//            dorisType = col.getType();
//            col2ordMap.put(col.getName(), i);
//            extrnalColConerter = wrapNullableExternalConverter(getSerializationConverter(dorisType));
//            toExternalConverters.add(Pair.of(extrnalColConerter, dorisType));
//        }
        return new TISDorisColumnConverter(col2ordMap, fieldCount, toInternalConverters, toExternalConverters);
    }

//    public TISDorisColumnConverter(DorisConf options) {
//        super(options.getColumn().size());
//        this.options = options;
//
//    }


    @Override
    public List<ColVal> getValByColName(RowData value, List<String> cols) {
        if (CollectionUtils.isEmpty(cols)) {
            throw new IllegalArgumentException("param cols can not be empty");
        }
        try {
            List<ColVal> result = Lists.newArrayList();
            Integer ord = null;
            List<String> val = new ArrayList<>(1);
            for (String col : cols) {
                ord = col2ordMap.get(col);
                val.clear();
                toExternalConverters.get(ord).serialize(value, ord, val);
                result.add(new ColVal(col, val.get(0)));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public RowData toInternal(RowData input) {
        return null;
    }

    @Override
    public List<String> toExternal(RowData rowData, List<String> joiner) throws Exception {
//        if (fullColumn.size() == options.getColumn().size()) {
        for (int index = 0; index < rowData.getArity(); index++) {
            toExternalConverters.get(index).serialize(rowData, index, joiner);
        }
//        } else {
//        for (String columnName : fullColumn) {
//            if (columnNames.contains(columnName)) {
//                int index = columnNames.indexOf(columnName);
//                toExternalConverters.get(index).serialize(rowData, index, joiner);
//            } else {
//                joiner.add(NULL_VALUE);
//            }
//        }
        //}
        return joiner;
    }

    @Override
    protected ISerializationConverter<List<String>> wrapIntoNullableExternalConverter(
            ISerializationConverter<List<String>> serializeConverter, DataType type) {
        return wrapNullableExternalConverter(serializeConverter);
    }

    private static ISerializationConverter<List<String>>
    wrapNullableExternalConverter(ISerializationConverter<List<String>> serializeConverter) {
        return ((rowData, index, joiner) -> {
            if (rowData == null || rowData.isNullAt(index)) {
                joiner.add(NULL_VALUE);
            } else {
                serializeConverter.serialize(rowData, index, joiner);
            }
        });
    }

//    @Override
//    protected ISerializationConverter<List<String>> createExternalConverter(final BasicDorisStarRocksWriter.DorisType type) {
//        return getSerializationConverter(type);
//    }

    private static ISerializationConverter<List<String>> getSerializationConverter(FlinkCol col) {
        return new DorisSerializationConverter(col.getRowDataValGetter());
//        return (rowData, index, joiner) -> {
//            Object val = (rowData.isNullAt(index)) ? null : col.getRowDataValGetter().getFieldOrNull(rowData);
//            joiner.add(
//                    val == null ? NULL_VALUE : String.valueOf(val));
//        };
    }

    public static class DorisSerializationConverter implements ISerializationConverter<List<String>> {
        private final RowData.FieldGetter valGetter;

        public DorisSerializationConverter(RowData.FieldGetter valGetter) {
            this.valGetter = valGetter;
        }

        @Override
        public void serialize(RowData rowData, int index, List<String> joiner) throws Exception {
            Object val = (rowData.isNullAt(index)) ? null : valGetter.getFieldOrNull(rowData);
            joiner.add(
                    val == null ? NULL_VALUE : String.valueOf(val));
        }
    }

    public void setFullColumn(List<String> fullColumn) {
        this.fullColumn = fullColumn;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

}
