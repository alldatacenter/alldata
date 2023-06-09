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

package com.qlangtech.tis.plugins.incr.flink.chunjun.table;

import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.script.ChunjunSqlType;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import com.qlangtech.tis.realtime.BasicTISSinkFactory;
import com.qlangtech.tis.realtime.dto.DTOStream;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.data.conversion.RowRowConverter;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.factories.StreamTableSinkFactory;
import org.apache.flink.table.sinks.StreamTableSink;
import org.apache.flink.table.sinks.TableSink;
import org.apache.flink.table.sinks.UpsertStreamTableSink;
import org.apache.flink.types.Row;

import java.util.List;
import java.util.Map;

import static org.apache.flink.table.descriptors.ConnectorDescriptorValidator.CONNECTOR_PROPERTY_VERSION;
import static org.apache.flink.table.descriptors.ConnectorDescriptorValidator.CONNECTOR_TYPE;
import static org.apache.flink.table.descriptors.Schema.SCHEMA;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-11-16 10:59
 **/
public class ChunjunTableSinkFactory implements StreamTableSinkFactory<Tuple2<Boolean, Row>> {

    private final IEndTypeGetter.EndType endType;

    public ChunjunTableSinkFactory(IEndTypeGetter.EndType endType) {
        this.endType = endType;
    }

    @Override
    public StreamTableSink<Tuple2<Boolean, Row>> createStreamTableSink(Map<String, String> properties) {
        String dataXName = (properties.get(StringUtils.lowerCase(DataxUtils.DATAX_NAME)));
        String sourceTableName = properties.get(StringUtils.lowerCase(TableAlias.KEY_FROM_TABLE_NAME));
        if (StringUtils.isEmpty(dataXName) || StringUtils.isEmpty(sourceTableName)) {
            throw new IllegalArgumentException("param dataXName or sourceTableName can not be null");
        }
        ChunjunSinkFactory sinKFactory = (ChunjunSinkFactory) TISSinkFactory.getIncrSinKFactory(dataXName);
        IDataxProcessor dataxProcessor = DataxProcessor.load(null, dataXName);

        BasicTISSinkFactory.RowDataSinkFunc rowDataSinkFunc = sinKFactory.createRowDataSinkFunc(dataxProcessor
                , dataxProcessor.getTabAlias(null).getWithCheckNotNull(sourceTableName), false);
        return new ChunjunStreamTableSink(false, endType, rowDataSinkFunc);
    }


    public static class ChunjunStreamTableSink implements UpsertStreamTableSink<Row> {

        private final BasicTISSinkFactory.RowDataSinkFunc rowDataSinkFunc;
        private String[] primaryKeys;
        private final IEndTypeGetter.EndType endType;
        /**
         * Flag that indicates that only inserts are accepted.
         */
        private final boolean isAppendOnly;

        public ChunjunStreamTableSink(boolean isAppendOnly, IEndTypeGetter.EndType endType
                , BasicTISSinkFactory.RowDataSinkFunc rowDataSinkFunc) {
            this.rowDataSinkFunc = rowDataSinkFunc;
            this.endType = endType;
            this.isAppendOnly = isAppendOnly;
        }

        @Override
        public void setKeyFields(String[] primaryKeys) {
            this.primaryKeys = primaryKeys;
            //throw new UnsupportedOperationException(Arrays.stream(strings).collect(Collectors.joining(",")));
        }

        @Override
        public void setIsAppendOnly(Boolean isAppendOnly) {
            if (this.isAppendOnly && !isAppendOnly) {
                throw new ValidationException(
                        "The given query is not supported by this sink because the sink is configured to "
                                + "operate in append mode only. Thus, it only support insertions (no queries "
                                + "with updating results).");
            }
        }

        @Override
        public TypeInformation<Row> getRecordType() {
            return getTableSchema().toRowType();
            //return null;
        }

        @Override
        public DataStreamSink<?> consumeDataStream(DataStream<Tuple2<Boolean, Row>> dataStream) {
            TableSchema schema = getTableSchema();
            RowRowConverter rConverter = RowRowConverter.create(schema.toRowDataType());
            DTOStream rowData = DTOStream.createRowData(dataStream.map((row) -> {
                return rConverter.toInternal(row.f1);
            }));

            return this.rowDataSinkFunc.add2Sink(rowData);
        }

        @Override
        public TableSink<Tuple2<Boolean, Row>> configure(String[] strings, TypeInformation<?>[] typeInformations) {
            // return null;
            throw new UnsupportedOperationException();
        }

        @Override
        public final TableSchema getTableSchema() {

            return createTableSchema(this.rowDataSinkFunc.getColsMeta());
        }

        public static TableSchema createTableSchema(List<FlinkCol> flinkCols) {
            TableSchema.Builder schemaBuilder = TableSchema.builder();
            List<String> pks = Lists.newArrayList();
            List<FlinkCol> cols = flinkCols; //this.rowDataSinkFunc.getColsMeta(); //AbstractRowDataMapper.getAllTabColsMeta(sinkColsMeta.getCols());
            for (FlinkCol col : cols) {
                schemaBuilder.field(col.name, col.type);
                if (col.isPk()) {
                    pks.add(col.name);
                }
            }
            if (pks.size() > 0) {
                schemaBuilder.primaryKey(pks.toArray(new String[pks.size()]));
            }
            return schemaBuilder.build();
        }


        @Override
        public String[] getFieldNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TypeInformation<?>[] getFieldTypes() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Map<String, String> requiredContext() {
        Map<String, String> context = Maps.newHashMap();
        context.put(CONNECTOR_TYPE, ChunjunSqlType.getTableSinkTypeName(this.endType));
        context.put(CONNECTOR_PROPERTY_VERSION, "1");
        return context;
    }

    @Override
    public List<String> supportedProperties() {
        List<String> props = Lists.newArrayList();
        props.add(DataxUtils.DATAX_NAME);
        props.add(TableAlias.KEY_FROM_TABLE_NAME);
        props.add("update-mode");

        // schema
        props.add(SCHEMA + ".#." + DescriptorProperties.TYPE);
        props.add(SCHEMA + ".#." + DescriptorProperties.DATA_TYPE);
        props.add(SCHEMA + ".#." + DescriptorProperties.NAME);
        props.add(SCHEMA + ".#." + DescriptorProperties.EXPR);
        // schema watermark
        props.add(SCHEMA + "." + DescriptorProperties.WATERMARK + ".*");
        return props;
    }
}
