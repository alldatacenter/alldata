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

package com.qlangtech.tis.plugins.incr.flink.chunjun.source;

import com.dtstack.chunjun.conf.ContentConf;
import com.dtstack.chunjun.conf.JobConf;
import com.dtstack.chunjun.conf.OperatorConf;
import com.dtstack.chunjun.conf.SyncConf;
import com.dtstack.chunjun.connector.jdbc.source.JdbcSourceFactory;
import com.dtstack.chunjun.constants.ConfigConstant;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.SourceChannel;
import com.qlangtech.tis.async.message.client.consumer.IAsyncMsgDeserialize;
import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.async.message.client.consumer.MQConsumeException;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.IStreamTableMeataCreator;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsReader;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.ReaderSource;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.data.RowData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-10 11:59
 **/
public abstract class ChunjunSourceFunction
        implements IMQListener<JobExecutionResult> {
    final ChunjunSourceFactory sourceFactory;

    public ChunjunSourceFunction(ChunjunSourceFactory sourceFactory) {
        this.sourceFactory = sourceFactory;
    }


    @Override
    public IConsumerHandle getConsumerHandle() {
        return this.sourceFactory.getConsumerHandle();
    }

    private SourceFunction<RowData> createSourceFunction(String sourceTabName, SyncConf conf, BasicDataSourceFactory sourceFactory, BasicDataXRdbmsReader reader) {


        AtomicReference<SourceFunction<RowData>> sourceFunc = new AtomicReference<>();
        IStreamTableMeataCreator.IStreamTableMeta streamTableMeta = reader.getStreamTableMeta(sourceTabName);
        JdbcSourceFactory chunjunSourceFactory = createChunjunSourceFactory(conf, sourceFactory, streamTableMeta, sourceFunc);
        Objects.requireNonNull(chunjunSourceFactory, "chunjunSourceFactory can not be null");
        chunjunSourceFactory.createSource();
        return Objects.requireNonNull(sourceFunc.get(), "SourceFunction<RowData> shall present");
    }

    protected abstract JdbcSourceFactory //
    createChunjunSourceFactory( //
                                SyncConf conf, BasicDataSourceFactory sourceFactory, IStreamTableMeataCreator.IStreamTableMeta streamTableMeta, AtomicReference<SourceFunction<RowData>> sourceFunc);


    @Override
    public JobExecutionResult start(TargetResName name, IDataxReader dataSource
            , List<ISelectedTab> tabs, IDataxProcessor dataXProcessor) throws MQConsumeException {
        Objects.requireNonNull(dataXProcessor, "dataXProcessor can not be null");
        BasicDataXRdbmsReader reader = (BasicDataXRdbmsReader) dataSource;
        final BasicDataSourceFactory sourceFactory = (BasicDataSourceFactory) reader.getDataSourceFactory();
        List<ReaderSource> sourceFuncs = Lists.newArrayList();
        sourceFactory.getDbConfig().vistDbURL(false, (dbName, dbHost, jdbcUrl) -> {
            for (ISelectedTab tab : tabs) {
                SyncConf conf = createSyncConf(sourceFactory, jdbcUrl, dbName, (SelectedTab) tab);
                SourceFunction<RowData> sourceFunc = createSourceFunction(tab.getName(), conf, sourceFactory, reader);
                sourceFuncs.add(ReaderSource.createRowDataSource(
                        dbHost + ":" + sourceFactory.port + "_" + dbName, tab, sourceFunc));
            }
        });

        try {
            SourceChannel sourceChannel = new SourceChannel(sourceFuncs);
            sourceChannel.setFocusTabs(tabs, dataXProcessor.getTabAlias(), (tabName) -> DTOStream.createRowData());
            return (JobExecutionResult) getConsumerHandle().consume(name, sourceChannel, dataXProcessor);
        } catch (Exception e) {
            throw new MQConsumeException(e.getMessage(), e);
        }
    }

    private SyncConf createSyncConf(BasicDataSourceFactory sourceFactory, String jdbcUrl, String dbName, SelectedTab tab) {
        SyncConf syncConf = new SyncConf();

        JobConf jobConf = new JobConf();
        ContentConf content = new ContentConf();
        OperatorConf writer = new OperatorConf();
        Map<String, Object> writerParams = Maps.newHashMap();
        writer.setParameter(writerParams);

        OperatorConf reader = new OperatorConf();
        reader.setName("postgresqlreader");
        Map<String, Object> params = Maps.newHashMap();
        params.put("username", sourceFactory.getUserName());
        params.put("password", sourceFactory.getPassword());

        params.put("queryTimeOut", this.sourceFactory.queryTimeOut);
        params.put("fetchSize", this.sourceFactory.fetchSize);

        //tab.getIncrMode().set(params);

        List<Map<String, String>> cols = Lists.newArrayList();
        Map<String, String> col = null;
        // com.dtstack.chunjun.conf.FieldConf.getField(List)

        for (CMeta cm : tab.getCols()) {
            col = Maps.newHashMap();
            col.put("name", cm.getName());
            col.put("type", parseType(cm));
            cols.add(col);
        }

        params.put(ConfigConstant.KEY_COLUMN, cols);
        params.put("fullColumn", tab.getCols().stream().map((c) -> c.getName()).collect(Collectors.toList()));
        Map<String, Object> conn = Maps.newHashMap();
        conn.put("jdbcUrl", Collections.singletonList(jdbcUrl));
        conn.put("table", Lists.newArrayList(tab.getName()));
        //  conn.put("schema", dbName);
        params.put("connection", Lists.newArrayList(conn));

        SelectedTabPropsExtends tabExtend = tab.getIncrSourceProps();
        if (tabExtend == null) {
            throw new IllegalStateException("tab:" + tab.getName() + " relevant tabExtend can not be null");
        }
        // tabExtend.polling.setParams(params);
        tabExtend.setParams(params);

        // polling
//        params.put("polling", true);
//        params.put("pollingInterval", 3000);
//        params.put("increColumn", "id");
//        params.put("startLocation", 0);

        reader.setParameter(params);
        content.setReader(reader);
        content.setWriter(writer);
        jobConf.setContent(Lists.newLinkedList(Collections.singleton(content)));
        syncConf.setJob(jobConf);
        return syncConf;
    }

    protected final String parseType(CMeta cm) {
        return cm.getType().getS();
    }


    @Override
    public String getTopic() {
        return null;
    }

    @Override
    public void setDeserialize(IAsyncMsgDeserialize deserialize) {

    }
}
