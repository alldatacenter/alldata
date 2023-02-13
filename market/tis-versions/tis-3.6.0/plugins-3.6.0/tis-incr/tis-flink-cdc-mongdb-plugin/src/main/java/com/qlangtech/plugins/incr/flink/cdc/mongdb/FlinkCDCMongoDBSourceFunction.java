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

package com.qlangtech.plugins.incr.flink.cdc.mongdb;

import com.google.common.collect.Lists;
import com.qlangtech.plugins.incr.flink.cdc.SourceChannel;
import com.qlangtech.plugins.incr.flink.cdc.TISDeserializationSchema;
import com.qlangtech.tis.async.message.client.consumer.IAsyncMsgDeserialize;
import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.async.message.client.consumer.MQConsumeException;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.plugin.datax.DataXMongodbReader;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.mangodb.MangoDBDataSourceFactory;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.ReaderSource;
import com.qlangtech.tis.realtime.transfer.DTO;
import com.ververica.cdc.connectors.mongodb.MongoDBSource;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.List;

/**
 * https://ververica.github.io/flink-cdc-connectors/master/content/connectors/mongodb-cdc.html
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-11-02 11:40
 **/
public class FlinkCDCMongoDBSourceFunction implements IMQListener<JobExecutionResult> {
    private final FlinkCDCMongoDBSourceFactory sourceFactory;

    public FlinkCDCMongoDBSourceFunction(FlinkCDCMongoDBSourceFactory sourceFactory) {
        this.sourceFactory = sourceFactory;
    }

    @Override
    public JobExecutionResult start(TargetResName dataxName, IDataxReader dataSource, List<ISelectedTab> tabs, IDataxProcessor dataXProcessor) throws MQConsumeException {

        try {

            DataXMongodbReader mongoReader = (DataXMongodbReader) dataSource;

            MangoDBDataSourceFactory dsFactory = mongoReader.getDsFactory();

            List<ReaderSource> sourceFunctions = Lists.newArrayList();

            MongoDBSource.Builder<DTO> builder = MongoDBSource.<DTO>builder()
                    .hosts(dsFactory.address)
                    .database(dsFactory.dbName)
                    .collection(mongoReader.collectionName)
                    .connectionOptions(sourceFactory.connectionOptions)
                    .errorsTolerance(sourceFactory.errorsTolerance)
                    .username(dsFactory.getUserName())
                    .password(dsFactory.getPassword())
                    .deserializer(new TISDeserializationSchema());

            //  builder.

            if (sourceFactory.errorsLogEnable != null) {
                builder.errorsLogEnable(sourceFactory.errorsLogEnable);
            }
            if (sourceFactory.copyExisting != null) {
                builder.copyExisting(sourceFactory.copyExisting);
            }
            if (sourceFactory.copyExistingMaxThreads != null) {
                builder.copyExistingMaxThreads(sourceFactory.copyExistingMaxThreads);
            }
            if (sourceFactory.copyExistingQueueSize != null) {
                builder.copyExistingMaxThreads(sourceFactory.copyExistingQueueSize);
            }
            if (sourceFactory.pollMaxBatchSize != null) {
                builder.copyExistingMaxThreads(sourceFactory.pollMaxBatchSize);
            }
            if (sourceFactory.pollAwaitTimeMillis != null) {
                builder.copyExistingMaxThreads(sourceFactory.pollAwaitTimeMillis);
            }
            if (sourceFactory.heartbeatIntervalMillis != null) {
                builder.copyExistingMaxThreads(sourceFactory.heartbeatIntervalMillis);
            }

            SourceFunction<DTO> source = builder.build();

//                    MongoDBSource.<DTO>builder()
//                    .hosts(dsFactory.address)
//                    .database(dsFactory.dbName)
//                    .collection(mongoReader.collectionName)
//                    .connectionOptions(sourceFactory.connectionOptions)
//                    .errorsTolerance(sourceFactory.errorsTolerance)
//                    .errorsLogEnable(sourceFactory.errorsLogEnable)
//                    .copyExisting(sourceFactory.copyExisting)
//                    .copyExistingPipeline(sourceFactory.copyExistingPipeline)
//                    .copyExistingMaxThreads(sourceFactory.copyExistingMaxThreads)
//                    .copyExistingQueueSize(sourceFactory.copyExistingQueueSize)
//                    .pollMaxBatchSize(sourceFactory.pollMaxBatchSize)
//                    .pollAwaitTimeMillis(sourceFactory.pollAwaitTimeMillis)
//                    .heartbeatIntervalMillis(sourceFactory.heartbeatIntervalMillis)
//                    //.port(dsFactory.port)
//                    // .databaseList(dbs.toArray(new String[dbs.size()])) // monitor all tables under inventory database
////                              .tableList(tbs.toArray(new String[tbs.size()]))
//                    .username(dsFactory.getUserName())
//                    .password(dsFactory.getPassword())
////                              .startupOptions(sourceFactory.getStartupOptions())
//                    //.debeziumProperties(debeziumProperties)
//                    .deserializer(new TISDeserializationSchema()) // converts SourceRecord to JSON String
//                    .build();

            sourceFunctions.add(ReaderSource.createDTOSource(dsFactory.address + "_" + dsFactory.dbName + "_" + mongoReader.collectionName, source));

            SourceChannel sourceChannel = new SourceChannel(sourceFunctions);
            //  for (ISelectedTab tab : tabs) {
            sourceChannel.setFocusTabs(tabs, dataXProcessor.getTabAlias(), DTOStream::createDispatched);
            //}
            return (JobExecutionResult) getConsumerHandle().consume(dataxName, sourceChannel, dataXProcessor);
        } catch (Exception e) {
            throw new MQConsumeException(e.getMessage(), e);
        }
    }

    @Override
    public String getTopic() {
        return null;
    }

    @Override
    public void setDeserialize(IAsyncMsgDeserialize deserialize) {

    }

    @Override
    public IConsumerHandle getConsumerHandle() {
        return this.sourceFactory.getConsumerHander();
    }
}
