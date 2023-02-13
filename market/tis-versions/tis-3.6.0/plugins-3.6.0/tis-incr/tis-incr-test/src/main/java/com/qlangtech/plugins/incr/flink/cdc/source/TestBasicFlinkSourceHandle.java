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

package com.qlangtech.plugins.incr.flink.cdc.source;

import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.IResultRows;
import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
import com.qlangtech.tis.async.message.client.consumer.Tab2OutputTag;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.realtime.BasicFlinkSourceHandle;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.SinkFuncs;
import com.qlangtech.tis.realtime.transfer.DTO;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-12-18 10:57
 **/
public class TestBasicFlinkSourceHandle extends BasicFlinkSourceHandle<DTO> implements Serializable, IResultRows {
    protected final String tabName;

    protected StreamTableEnvironment tabEnv;

    private final Map<String, TableResult> sourceTableQueryResult = Maps.newHashMap();

    public TestBasicFlinkSourceHandle(final String tabName) {
        this.tabName = tabName;
    }

    @Override
    public Object deColFormat(String tableName, String colName, Object val) {
        return val;
    }

    @Override
    protected StreamExecutionEnvironment getFlinkExecutionEnvironment() {
//        StreamExecutionEnvironment evn = super.getFlinkExecutionEnvironment();
//        // evn.enableCheckpointing(1000);
//        return evn;
        return StreamExecutionEnvironment.getExecutionEnvironment();
    }

    @Override
    protected JobExecutionResult executeFlinkJob(TargetResName dataxName, StreamExecutionEnvironment env) throws Exception {
        // return super.executeFlinkJob(dataxName, env);
        // 测试环境下不能执行 return env.execute(dataxName.getName()); 不然单元测试不会自动退出了
        return null;
    }

    @Override
    protected void processTableStream(StreamExecutionEnvironment env
            , Tab2OutputTag<DTOStream> streamMap, SinkFuncs<DTO> sinkFunction) {

//    @Override
//    protected void processTableStream(Map<String, DataStream<DTO>> streamMap, SinkFuncs sinkFunction) {
        sinkFunction.add2Sink(tabName, streamMap.getSourceMapper().get(tabName));
        if (tabEnv == null) {

            tabEnv = StreamTableEnvironment.create(
                    env,
                    EnvironmentSettings.newInstance()
                            .useBlinkPlanner()
                            .inStreamingMode()
                            .build());

            // tabEnv = StreamTableEnvironment.create(env);
        }

        createTemporaryView(streamMap);
        getSourceTableQueryResult().put(tabName, tabEnv.executeSql("SELECT * FROM " + tabName));
    }

    protected void createTemporaryView(Tab2OutputTag<DTOStream> streamMap) {
        tabEnv.createTemporaryView(tabName
                , Objects.requireNonNull(streamMap.getSourceMapper().get(tabName).getStream()
                        , "tabName:" + tabName + " relevant 'DataStream' can not be null"));
    }

    @Override
    public IConsumerHandle getConsumerHandle() {
        return this;
    }

//    @Override
//    public CloseableIterator<Row> getRowSnapshot(String tabName) {
//        TableResult tableResult = getSourceTableQueryResult().get(tabName);
//        Objects.requireNonNull(tableResult, "tabName:" + tabName + " relevant TableResult can not be null");
//        CloseableIterator<Row> collect = tableResult.collect();
//        return collect;
//    }

    public Map<String, TableResult> getSourceTableQueryResult() {
        return sourceTableQueryResult;
    }

//    /**
//     * 终止flink
//     */
//    @Override
//    public void cancel() {
//        try {
//            for (TableResult tabResult : getSourceTableQueryResult().values()) {
//                tabResult.getJobClient().get().cancel().get();
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
}
