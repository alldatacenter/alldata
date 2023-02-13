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

import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.tis.async.message.client.consumer.Tab2OutputTag;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.plugin.datax.hudi.HudiTableMeta;
import com.qlangtech.tis.plugin.datax.hudi.IDataXHudiWriter;
import com.qlangtech.tis.plugins.incr.flink.cdc.DTO2RowDataMapper;
import com.qlangtech.tis.plugins.incr.flink.connector.hudi.HudiSinkFactory;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.commons.collections.MapUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.apache.flink.table.types.logical.RowType;
import org.apache.hudi.common.model.HoodieRecord;
import org.apache.hudi.common.model.WriteOperationType;
import org.apache.hudi.common.util.Option;
import org.apache.hudi.configuration.FlinkOptions;
import org.apache.hudi.sink.transform.Transformer;
import org.apache.hudi.sink.utils.Pipelines;
import org.apache.hudi.streamer.FlinkStreamerConfig;
import org.apache.hudi.util.AvroSchemaConverter;
import org.apache.hudi.util.StreamerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-23 11:18
 **/
public abstract class HoodieFlinkSourceHandle extends BasicFlinkSourceHandle<DTO> {
    private static final Logger logger = LoggerFactory.getLogger(HoodieFlinkSourceHandle.class);

    @Override
    protected final void processTableStream(StreamExecutionEnvironment env
            , Tab2OutputTag<DTOStream> tab2OutputTag, SinkFuncs<DTO> sinkFunction) {
        FlinkStreamerConfig flinkCfg = null;
        Map<String, FlinkStreamerConfig> tabStreamerCfg = createTabStreamerCfg();
        if (MapUtils.isEmpty(tabStreamerCfg)) {
            throw new IllegalStateException("tabStreamerCfg can not be null");
        }
        HudiSinkFactory sinkFunc = (HudiSinkFactory) this.getSinkFuncFactory();
        IDataXHudiWriter dataXHudiWriter = HudiSinkFactory.getDataXHudiWriter(sinkFunc);

        ITISFileSystem fs = dataXHudiWriter.getFileSystem();
        try {
            for (Map.Entry<TableAlias, DTOStream> entry : tab2OutputTag.entrySet()) {
                flinkCfg = Objects.requireNonNull(tabStreamerCfg.get(entry.getKey().getFrom())
                        , "tab:" + entry.getKey() + " relevant instance of 'FlinkStreamerConfig' can not be null,exist keys:"
                                + tabStreamerCfg.keySet().stream().collect(Collectors.joining(",")));
                this.createSchema(entry.getKey(), flinkCfg, sinkFunc, fs);
                this.registerTable(env, flinkCfg
                        , entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected List<FlinkCol> getTabColMetas(TargetResName dataxName, String tabName) {
        return DTO2RowDataMapper.getAllTabColsMeta(dataxName, tabName);
    }

    /**
     * @param tableName
     * @param flinkCfg
     * @param sinkFunc
     * @param fs
     */
    private void createSchema(TableAlias tableName, FlinkStreamerConfig flinkCfg, HudiSinkFactory sinkFunc, ITISFileSystem fs) {
        IPath schemaSourcePath = fs.getPath(flinkCfg.sourceAvroSchemaPath);
        if (fs.exists(schemaSourcePath)) {
            logger.info("schemaSourcePath has been create,shall not be create again,path:{}", schemaSourcePath);
            return;
        }
        HudiTableMeta.createSourceSchema(
                fs, tableName.getTo()
                , schemaSourcePath, sinkFunc.getTableMeta(tableName.getTo()).getLeft());
    }


    /**
     * velocity 模版中会用到
     *
     * @param operationType
     * @return
     */
    protected FlinkStreamerConfig createHudiCfg(String operationType) {
        FlinkStreamerConfig cfg = new FlinkStreamerConfig();
//        PluginFirstClassLoader cl1 = (PluginFirstClassLoader) cfg.operation.getClass().getClassLoader();
//        PluginFirstClassLoader cl2 = (PluginFirstClassLoader) WriteOperationType.BULK_INSERT.getClass().getClassLoader();
//
//        System.out.println("cl1:" + cl1.hashCode() + "->" + cl1.getURLs().stream().map((url) -> String.valueOf(url)).collect(Collectors.joining(",")));
//        System.out.println("cl2:" + cl2.hashCode() + "->" + cl2.getURLs().stream().map((url) -> String.valueOf(url)).collect(Collectors.joining(",")));
        cfg.operation = WriteOperationType.fromValue(operationType);
        return cfg;
    }

    //FlinkStreamerConfig
    protected abstract Map<String, org.apache.hudi.streamer.FlinkStreamerConfig> createTabStreamerCfg();

    private void registerTable(StreamExecutionEnvironment env, org.apache.hudi.streamer.FlinkStreamerConfig tabStreamerCfg
            , TableAlias tabName, DTOStream dtoDataStream) throws Exception {
        int parallelism = env.getParallelism();

        final org.apache.hudi.streamer.FlinkStreamerConfig cfg = tabStreamerCfg;

        RowType rowType =
                (RowType) AvroSchemaConverter.convertToDataType(StreamerUtil.getSourceSchema(cfg))
                        .getLogicalType();
        DTO2RowDataMapper toRowMapper
                = new DTO2RowDataMapper(this.getTabColMetas(new TargetResName(this.getDataXName()), tabName.getTo()));

        DataStream<RowData> dataStream = Objects.requireNonNull(dtoDataStream.getStream(), "source stream can not be null")
                .map(toRowMapper, InternalTypeInfo.of(rowType))
                .name(tabName.getTo()).uid("uid_" + tabName.getTo());


        if (cfg.transformerClassNames != null && !cfg.transformerClassNames.isEmpty()) {
            Option<Transformer> transformer = StreamerUtil.createTransformer(cfg.transformerClassNames);
            if (transformer.isPresent()) {
                dataStream = transformer.get().apply(dataStream);
            }
        }

        Configuration conf = org.apache.hudi.streamer.FlinkStreamerConfig.toFlinkConfig(cfg);
        // 额外参数也要添加进来
        conf.addAll(cfg);
        long ckpTimeout = env.getCheckpointConfig().getCheckpointTimeout();
        conf.setLong(FlinkOptions.WRITE_COMMIT_ACK_TIMEOUT, ckpTimeout);


        DataStream<HoodieRecord> hoodieRecordDataStream = Pipelines.bootstrap(conf, rowType, parallelism, dataStream);
        DataStream<Object> pipeline = Pipelines.hoodieStreamWrite(conf, parallelism, hoodieRecordDataStream);
        if (StreamerUtil.needsAsyncCompaction(conf)) {
            Pipelines.compact(conf, pipeline);
        } else {
            Pipelines.clean(conf, pipeline);
        }
    }


}
