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

package com.qlangtech.tis.realtime;

import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.DTO2RowMapper;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.plugins.incr.flink.cdc.RowData2RowMapper;
import com.qlangtech.tis.async.message.client.consumer.Tab2OutputTag;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IStreamTableMeataCreator;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DBConfig;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
import com.qlangtech.tis.plugin.ds.IInitWriterTableExecutor;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.cdc.AbstractRowDataMapper;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.transfer.DTO;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.descriptors.ConnectorDescriptor;
import org.apache.flink.table.types.utils.LegacyTypeInfoDataTypeConverter;
import org.apache.flink.types.Row;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 将源DataStream 转成Table
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-18 11:50
 **/
public abstract class TableRegisterFlinkSourceHandle extends BasicFlinkSourceHandle<DTO> {


    /**
     * @param env
     * @param tab2OutputTag source 部分
     * @param sinkFunction  sink 部分
     */
    @Override
    protected final void processTableStream(StreamExecutionEnvironment env
            , Tab2OutputTag<DTOStream> tab2OutputTag, SinkFuncs<DTO> sinkFunction) {

        StreamTableEnvironment tabEnv = StreamTableEnvironment.create(
                env, EnvironmentSettings.newInstance()
                        .useBlinkPlanner()
                        .inStreamingMode()
                        .build());

        for (Map.Entry<TableAlias, DTOStream> entry : tab2OutputTag.entrySet()) {
            this.registerSourceTable(tabEnv, entry.getKey().getFrom(), entry.getValue());
            if (shallRegisterSinkTable()) {
                this.registerSinkTable(tabEnv, entry.getKey());
            }
        }
        TISTableEnvironment tabEnvironment = new TISTableEnvironment(tabEnv);
        this.executeSql(tabEnvironment);
        tabEnvironment.executeMultiStatment();
    }

    @Override
    protected Map<TableAlias, TabSinkFunc<DTO>> createTabSinkFunc(IDataxProcessor dataXProcessor) {
        // return super.createTabSinkFunc(dataXProcessor);
        return Collections.emptyMap();
    }

    @Override
    protected JobExecutionResult executeFlinkJob(TargetResName dataxName, StreamExecutionEnvironment env) throws Exception {
//        JobExecutionResult execResult = super.executeFlinkJob(dataxName, env);
//
//        return execResult;
        return null;
    }

    /**
     * 是否要自动注册Sink表？
     *
     * @return
     */
    protected Boolean shallRegisterSinkTable() {
        return true;
    }

    abstract protected void executeSql(TISTableEnvironment tabEnv);

    @Override
    protected List<FlinkCol> getTabColMetas(TargetResName dataxName, String tabName) {

        return getAllTabColsMeta(this.getSinkFuncFactory(), tabName);
    }

    private void registerSinkTable(StreamTableEnvironment tabEnv, TableAlias alias) {
        final Map<String, String> connProps = Maps.newHashMap();
        connProps.put(DataxUtils.DATAX_NAME, this.getDataXName());
        //ChunjunSinkFactory.KEY_SOURCE_TABLE_NAME
        connProps.put(TableAlias.KEY_FROM_TABLE_NAME, alias.getFrom());
        org.apache.flink.table.descriptors.Schema sinkTabSchema
                = new org.apache.flink.table.descriptors.Schema();
        // 其实无作用骗骗校验器的

        DataxWriter dataXWriter = DataxWriter.load(null, this.getDataXName());
        BasicDataSourceFactory dsFactory
                = (BasicDataSourceFactory) ((IDataSourceFactoryGetter) dataXWriter).getDataSourceFactory();
        if (dsFactory == null) {
            throw new IllegalStateException("dsFactory can not be null");
        }
        DBConfig dbConfig = dsFactory.getDbConfig();
        dbConfig.vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
            try {
                /**
                 * 需要先初始化表MySQL目标库中的表
                 */
                ((IInitWriterTableExecutor) dataXWriter)
                        .initWriterTable(alias.getTo(), Collections.singletonList(jdbcUrl));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        List<FlinkCol> cols = this.getTabColMetas(new TargetResName(this.getDataXName()), alias.getTo());
        for (FlinkCol c : cols) {
            sinkTabSchema.field(c.name, c.type);
        }
        tabEnv.connect(new ConnectorDescriptor(this.getSinkTypeName(), 1, false) {
            @Override
            protected Map<String, String> toConnectorProperties() {
                return connProps;
            }
        }).withSchema(sinkTabSchema) //
                .inUpsertMode().createTemporaryTable(alias.getTo());
    }

    protected abstract String getSinkTypeName();


    protected void registerSourceTable(StreamTableEnvironment tabEnv
            , String tabName, DTOStream sourceStream) {


        Schema.Builder scmBuilder = Schema.newBuilder();
        List<FlinkCol> cols = AbstractRowDataMapper.getAllTabColsMeta(
                this.getSourceStreamTableMeta().getStreamTableMeta(tabName));
        String[] fieldNames = new String[cols.size()];
        TypeInformation<?>[] types = new TypeInformation<?>[cols.size()];
        int i = 0;

        for (FlinkCol col : cols) {
            scmBuilder.column(col.name, col.type);
            // TypeConversions.fromDataTypeToLegacyInfo()
            types[i] = LegacyTypeInfoDataTypeConverter.toLegacyTypeInfo(col.type);
            fieldNames[i++] = col.name;

        }
        List<String> pks = cols.stream().filter((c) -> c.isPk()).map((c) -> c.name).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(pks)) {
            scmBuilder.primaryKey(pks);
        }
        Schema schema = scmBuilder.build();

        TypeInformation<Row> outputType = Types.ROW_NAMED(fieldNames, types);


        SingleOutputStreamOperator<Row> rowStream = null;
        if (sourceStream.clazz == DTO.class) {
            rowStream = sourceStream.getStream()
                    .map(new DTO2RowMapper(cols), outputType)
                    .name(tabName).uid("uid_" + tabName);
        } else if (sourceStream.clazz == RowData.class) {
            rowStream = sourceStream.getStream()
                    .map(new RowData2RowMapper(cols), outputType)
                    .name(tabName).uid("uid_" + tabName);
        }
        Objects.requireNonNull(rowStream, "rowStream can not be null");

        Table table = tabEnv.fromChangelogStream(rowStream, schema, ChangelogMode.all());
        tabEnv.createTemporaryView(tabName + IStreamIncrGenerateStrategy.IStreamTemplateData.KEY_STREAM_SOURCE_TABLE_SUFFIX, table);
    }

    private static List<FlinkCol> getAllTabColsMeta(TISSinkFactory sinkFactory, String tabName) {
        IStreamTableMeataCreator.IStreamTableMeta streamTableMeta = getStreamTableMeta(sinkFactory, tabName);
        return AbstractRowDataMapper.getAllTabColsMeta(streamTableMeta);
    }


}
