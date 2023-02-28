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

package com.qlangtech.plugins.incr.flink.cdc.postgresql;

import com.qlangtech.plugins.incr.flink.cdc.SourceChannel;
import com.qlangtech.plugins.incr.flink.cdc.TISDeserializationSchema;
import com.qlangtech.plugins.incr.flink.cdc.valconvert.DateTimeConverter;
import com.qlangtech.tis.async.message.client.consumer.IAsyncMsgDeserialize;
import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.async.message.client.consumer.MQConsumeException;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsReader;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.ReaderSource;
import com.qlangtech.tis.realtime.transfer.DTO;
import com.ververica.cdc.connectors.postgres.PostgreSQLSource;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.List;
import java.util.stream.Collectors;

/**
 * https://ververica.github.io/flink-cdc-connectors/master/content/connectors/postgres-cdc.html
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-27 15:17
 **/
public class FlinkCDCPostgreSQLSourceFunction implements IMQListener<JobExecutionResult> {

    private final FlinkCDCPostreSQLSourceFactory sourceFactory;

    //   private IDataxProcessor dataXProcessor;

    public FlinkCDCPostgreSQLSourceFunction(FlinkCDCPostreSQLSourceFactory sourceFactory) {
        this.sourceFactory = sourceFactory;
    }


    @Override
    public IConsumerHandle getConsumerHandle() {
        return this.sourceFactory.getConsumerHander();
    }

    @Override
    public JobExecutionResult start(TargetResName dataxName, IDataxReader dataSource
            , List<ISelectedTab> tabs, IDataxProcessor dataXProcessor) throws MQConsumeException {
        try {
            BasicDataXRdbmsReader rdbmsReader = (BasicDataXRdbmsReader) dataSource;
            final BasicDataSourceFactory dsFactory = (BasicDataSourceFactory) rdbmsReader.getDataSourceFactory();
            BasicDataSourceFactory.ISchemaSupported schemaSupported = (BasicDataSourceFactory.ISchemaSupported) dsFactory;
            if (StringUtils.isEmpty(schemaSupported.getDBSchema())) {
                throw new IllegalStateException("dsFactory:" + dsFactory.dbName + " relevant dbSchema can not be null");
            }
            SourceChannel sourceChannel = new SourceChannel(
                    SourceChannel.getSourceFunction(dsFactory, (tab) -> schemaSupported.getDBSchema() + "." + tab.getTabName()
                            , tabs
                            , (dbHost, dbs, tbs, debeziumProperties) -> {
                                DateTimeConverter.setDatetimeConverters(PGDateTimeConverter.class.getName(), debeziumProperties);

                                return dbs.stream().map((dbname) -> {
                                    SourceFunction<DTO> sourceFunction = PostgreSQLSource.<DTO>builder()
                                            //.debeziumProperties()
                                            .hostname(dbHost)
                                            .port(dsFactory.port)
                                            .database(dbname) // monitor postgres database
                                            .schemaList(schemaSupported.getDBSchema())  // monitor inventory schema
                                            .tableList(tbs.toArray(new String[tbs.size()])) // monitor products table
                                            .username(dsFactory.userName)
                                            .password(dsFactory.password)
                                            .debeziumProperties(debeziumProperties)
                                            .deserializer(new TISDeserializationSchema()) // converts SourceRecord to JSON String
                                            .build();
                                    return ReaderSource.createDTOSource(dbHost + ":" + dsFactory.port + "_" + dbname, sourceFunction);
                                }).collect(Collectors.toList());

                            }));
            // for (ISelectedTab tab : tabs) {
            sourceChannel.setFocusTabs(tabs, dataXProcessor.getTabAlias(), DTOStream::createDispatched);
            //}
            return (JobExecutionResult) getConsumerHandle().consume(dataxName, sourceChannel, dataXProcessor);
        } catch (Exception e) {
            throw new MQConsumeException(e.getMessage(), e);
        }
    }


//    //https://ververica.github.io/flink-cdc-connectors/master/
//    private List<SourceFunction<DTO>> getPostreSQLSourceFunction(DBConfigGetter dataSource, List<ISelectedTab> tabs) {
//
//        try {
//            BasicDataSourceFactory dsFactory = dataSource.getBasicDataSource();
//            List<SourceFunction<DTO>> sourceFuncs = Lists.newArrayList();
//            DBConfig dbConfig = dataSource.getDbConfig();
//            Map<String, List<String>> ip2dbs = Maps.newHashMap();
//            Map<String, List<ISelectedTab>> db2tabs = Maps.newHashMap();
//            dbConfig.vistDbName((config, ip, dbName) -> {
//                List<String> dbs = ip2dbs.get(ip);
//                if (dbs == null) {
//                    dbs = Lists.newArrayList();
//                    ip2dbs.put(ip, dbs);
//                }
//                dbs.add(dbName);
//
//                if (db2tabs.get(dbName) == null) {
//                    db2tabs.put(dbName, tabs);
//                }
//                return false;
//            });
//
//            for (Map.Entry<String /**ip*/, List<String>/**dbs*/> entry : ip2dbs.entrySet()) {
//
//
//                Set<String> tbs = entry.getValue().stream().flatMap(
//                        (dbName) -> db2tabs.get(dbName).stream().map((tab) -> dbName + "." + tab.getName())).collect(Collectors.toSet());
//
//
//                Properties debeziumProperties = new Properties();
//                debeziumProperties.put("snapshot.locking.mode", "none");// do not use lock
//
//                SourceFunction<DTO> sourceFunction = PostgreSQLSource.<DTO>builder()
//                        .hostname("localhost")
//                        .port(5432)
//                        .database("postgres") // monitor postgres database
//                        .schemaList("inventory")  // monitor inventory schema
//                        .tableList("inventory.products") // monitor products table
//                        .username("flinkuser")
//                        .password("flinkpw")
//                        .deserializer(null) // converts SourceRecord to JSON String
//                        .build();
//               // sourceFuncs.add(sourceFuncs);
//
////                sourceFuncs.add(MySqlSource.<DTO>builder()
////                        .hostname(entry.getKey())
////                        .port(dsFactory.port)
////                        .databaseList(entry.getValue().toArray(new String[entry.getValue().size()])) // monitor all tables under inventory database
////                        .tableList(tbs.toArray(new String[tbs.size()]))
////                        .username(dsFactory.getUserName())
////                        .password(dsFactory.getPassword())
////                        .startupOptions(sourceFactory.getStartupOptions())
////                        .debeziumProperties(debeziumProperties)
////                        //.deserializer(new JsonStringDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
////                        .deserializer(new TISDeserializationSchema()) // converts SourceRecord to JSON String
////                        .build());
//            }
//
//            return sourceFuncs;
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//
//    }

//    public IDataxProcessor getDataXProcessor() {
//        return dataXProcessor;
//    }
//
//    public void setDataXProcessor(IDataxProcessor dataXProcessor) {
//        this.dataXProcessor = dataXProcessor;
//    }

    @Override
    public String getTopic() {
        return null;
    }

    @Override
    public void setDeserialize(IAsyncMsgDeserialize deserialize) {

    }


}
