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

package com.qlangtech.plugins.incr.flink.chunjun.doris.table;

import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-11-13 10:37
 * @see com.dtstack.chunjun.connector.doris.sink.DorisDynamicTableSink
 * https://blog.csdn.net/daijiguo/article/details/112853827
 **/
public class TISDorisDynamicTableSink implements DynamicTableSink {

    private final TableSchema physicalSchema;
    private final SinkFunction<RowData> sinkFunction;
    private final Integer parallelism;

    public TISDorisDynamicTableSink(TableSchema physicalSchema, SinkFunction<RowData> sinkFunction, Integer parallelism) {
        this.physicalSchema = physicalSchema;
        this.sinkFunction = sinkFunction;
        if (parallelism < 1) {
            throw new IllegalArgumentException("param parallelism can not small than 1");
        }
        this.parallelism = parallelism;
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode changelogMode) {
        return changelogMode;
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {

//        SyncConf syncConf = null;
//
//        ChunjunSinkFactory.CreateChunjunSinkFunctionResult r = ChunjunDorisSinkFactory.createDorisSinkFunctionResult(syncConf);
//        r.initialize();

        return SinkFunctionProvider.of(sinkFunction, parallelism);
    }

    @Override
    public DynamicTableSink copy() {
        return new TISDorisDynamicTableSink(physicalSchema, sinkFunction, parallelism);
    }

    @Override
    public String asSummaryString() {
        return "TIS Doris Sink";
    }


}
