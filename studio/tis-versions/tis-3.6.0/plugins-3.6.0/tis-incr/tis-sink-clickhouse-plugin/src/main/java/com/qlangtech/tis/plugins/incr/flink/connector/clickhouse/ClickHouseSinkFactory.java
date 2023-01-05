///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.qlangtech.tis.plugins.incr.flink.connector.clickhouse;
//
//import com.google.common.collect.Maps;
//import com.qlangtech.tis.compiler.incr.ICompileAndPackage;
//import com.qlangtech.tis.compiler.streamcode.CompileAndPackage;
//import com.qlangtech.tis.datax.IDataXPluginMeta;
//import com.qlangtech.tis.datax.IDataxProcessor;
//import com.qlangtech.tis.datax.IDataxReader;
//import com.qlangtech.tis.extension.TISExtension;
//import com.qlangtech.tis.plugin.annotation.FormField;
//import com.qlangtech.tis.plugin.annotation.FormFieldType;
//import com.qlangtech.tis.plugin.annotation.Validator;
//import com.qlangtech.tis.plugin.datax.DataXClickhouseWriter;
//import com.qlangtech.tis.plugin.ds.ColumnMetaData;
//import com.qlangtech.tis.plugin.ds.DBConfig;
//import com.qlangtech.tis.plugin.ds.ISelectedTab;
//import com.qlangtech.tis.plugin.ds.clickhouse.ClickHouseDataSourceFactory;
//import com.qlangtech.tis.realtime.BasicTISSinkFactory;
//import com.qlangtech.tis.realtime.TabSinkFunc;
//import com.qlangtech.tis.realtime.transfer.DTO;
//import org.apache.commons.collections.CollectionUtils;
//import org.apache.commons.compress.utils.Lists;
//import org.apache.commons.lang.StringUtils;
//import org.apache.flink.annotation.Public;
//import org.apache.flink.streaming.api.functions.sink.SinkFunction;
//import ru.ivi.opensource.flinkclickhousesink.model.ClickHouseClusterSettings;
//import ru.ivi.opensource.flinkclickhousesink.model.ClickHouseSinkConst;
//
//import java.util.*;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.stream.Collectors;
//
///**
// * https://github.com/ivi-ru/flink-clickhouse-sink#usage
// *
// * @author: 百岁（baisui@qlangtech.com）
// * @create: 2021-11-18 12:05
// **/
//@Public
//public class ClickHouseSinkFactory extends BasicTISSinkFactory<DTO> {
//    public static final String DISPLAY_NAME_FLINK_SINK = "Flink-ClickHouse-Sink";
//
//
//    //    public static final String TARGET_TABLE_NAME = "clickhouse.sink.target-table";
////    public static final String MAX_BUFFER_SIZE = "clickhouse.sink.max-buffer-size";
//    @FormField(ordinal = 0, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
//    public Integer maxBufferSize;
//    //    public static final String NUM_WRITERS = "clickhouse.sink.num-writers";
//    @FormField(ordinal = 1, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
//    public Integer numWriters;
//    //    public static final String QUEUE_MAX_CAPACITY = "clickhouse.sink.queue-max-capacity";
//    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
//    public Integer queueMaxCapacity;
//    //    public static final String TIMEOUT_SEC = "clickhouse.sink.timeout-sec";
//    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
//    public Integer timeout;
//    //    public static final String NUM_RETRIES = "clickhouse.sink.retries";
//    @FormField(ordinal = 4, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
//    public Integer numRetries;
//    //    public static final String FAILED_RECORDS_PATH = "clickhouse.sink.failed-records-path";
////    @FormField(ordinal = 5, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
////    public Integer failedRecordsPath;
//    //    public static final String IGNORING_CLICKHOUSE_SENDING_EXCEPTION_ENABLED = "clickhouse.sink.ignoring-clickhouse-sending-exception-enabled";
//    @FormField(ordinal = 5, type = FormFieldType.ENUM, validate = {Validator.require})
//    public Boolean ignoringSendingException;
//
//    private SinkFunction<DTO> createSinkFunction(
//            String dbName, final String targetTabName, ISelectedTab tab, String jdbcUrl, ClickHouseDataSourceFactory dsFactory) {
//
//        // create props for sink
//        Properties props = new Properties();
//        props.put(ClickHouseSinkConst.TARGET_TABLE_NAME, targetTabName);
//        Objects.requireNonNull(maxBufferSize, "maxBufferSize can not be null");
//        props.put(ClickHouseSinkConst.MAX_BUFFER_SIZE, String.valueOf(maxBufferSize));
//
//        Map<String, String> globalParams = Maps.newHashMap();
//
//        // ClickHouse cluster properties
//        // "http://" + clickHouse.getContainerIpAddress() + ":" + dockerActualPort
//        List<String> clickhouseHosts = Lists.newArrayList();
//        DBConfig dbCfg = dsFactory.getDbConfig();
//        try {
//            dbCfg.vistDbName((cfg, ip, _dbName) -> {
//                clickhouseHosts.add("http://" + ip + ":" + dsFactory.port + "/?database=" + _dbName);
//                return false;
//            });
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        if (clickhouseHosts.size() < 1) {
//            throw new IllegalStateException("clickhouseHosts size can not small than 1");
//        }
//        globalParams.put(ClickHouseClusterSettings.CLICKHOUSE_HOSTS
//                , clickhouseHosts.stream().collect(Collectors.joining(",")));
//        globalParams.put(ClickHouseClusterSettings.CLICKHOUSE_USER, dsFactory.getUserName());
//        globalParams.put(ClickHouseClusterSettings.CLICKHOUSE_PASSWORD, StringUtils.trimToEmpty(dsFactory.getPassword()));
//
//        // sink common
//        globalParams.put(ClickHouseSinkConst.TIMEOUT_SEC, String.valueOf(this.timeout));
//        globalParams.put(ClickHouseSinkConst.FAILED_RECORDS_PATH, "/tmp/tis-clickhouse-sink");
//        globalParams.put(ClickHouseSinkConst.NUM_WRITERS, String.valueOf(this.numWriters));
//        globalParams.put(ClickHouseSinkConst.NUM_RETRIES, String.valueOf(this.numRetries));
//        globalParams.put(ClickHouseSinkConst.QUEUE_MAX_CAPACITY, String.valueOf(this.queueMaxCapacity));
//        globalParams.put(ClickHouseSinkConst.IGNORING_CLICKHOUSE_SENDING_EXCEPTION_ENABLED
//                , String.valueOf(this.ignoringSendingException));
//
//        List<ColumnMetaData> colsMeta = dsFactory.getTableMetadata(targetTabName);
//        if (CollectionUtils.isEmpty(colsMeta)) {
//            throw new IllegalStateException("targetTabName relevant colsMeta can not be empty");
//        }
//        return new TISClickHouseSink(globalParams, props
//                , colsMeta.stream().map((c) -> new ColMeta(c.getKey(), c.getType())).collect(Collectors.toList()));
//    }
//
//    @Override
//    public Map<TableAlias, TabSinkFunc<DTO>> createSinkFunction(IDataxProcessor dataxProcessor) {
//        Map<TableAlias, TabSinkFunc<DTO>> sinkFuncs = Maps.newHashMap();
//        TableAlias tableName = null;
//        DataXClickhouseWriter dataXWriter = (DataXClickhouseWriter) dataxProcessor.getWriter(null);
//        Objects.requireNonNull(dataXWriter, "dataXWriter can not be null");
//        IDataxReader reader = dataxProcessor.getReader(null);
//        List<ISelectedTab> tabs = reader.getSelectedTabs();
//
//        ClickHouseDataSourceFactory dsFactory = dataXWriter.getDataSourceFactory();
//        DBConfig dbConfig = dsFactory.getDbConfig();
//
//        for (Map.Entry<String, TableAlias> tabAliasEntry : dataxProcessor.getTabAlias().entrySet()) {
//            tableName = tabAliasEntry.getValue();
//
//            Objects.requireNonNull(tableName, "tableName can not be null");
//            if (StringUtils.isEmpty(tableName.getFrom())) {
//                throw new IllegalStateException("tableName.getFrom() can not be empty");
//            }
//
//            AtomicReference<SinkFunction<DTO>> sinkFuncRef = new AtomicReference<>();
//            final TableAlias tabName = tableName;
//            AtomicReference<Object[]> exceptionLoader = new AtomicReference<>();
//            final String targetTabName = tableName.getTo();
//            dbConfig.vistDbURL(false, (dbName, jdbcUrl) -> {
//                try {
//                    Optional<ISelectedTab> selectedTab = tabs.stream()
//                            .filter((tab) -> StringUtils.equals(tabName.getFrom(), tab.getName())).findFirst();
//                    if (!selectedTab.isPresent()) {
//                        throw new IllegalStateException("target table:" + tabName.getFrom()
//                                + " can not find matched table in:["
//                                + tabs.stream().map((t) -> t.getName()).collect(Collectors.joining(",")) + "]");
//                    }
//                    /**
//                     * 需要先初始化表ClickHouse目标库中的表
//                     */
//                    dataXWriter.initWriterTable(targetTabName, Collections.singletonList(jdbcUrl));
//
//                    sinkFuncRef.set(createSinkFunction(dbName, targetTabName, selectedTab.get(), jdbcUrl, dsFactory));
//
//                } catch (Throwable e) {
//                    exceptionLoader.set(new Object[]{jdbcUrl, e});
//                }
//            });
//            if (exceptionLoader.get() != null) {
//                Object[] error = exceptionLoader.get();
//                throw new RuntimeException((String) error[0], (Throwable) error[1]);
//            }
//            Objects.requireNonNull(sinkFuncRef.get(), "sinkFunc can not be null");
//            sinkFuncs.put(tableName, new DTOSinkFunc(tableName, sinkFuncRef.get()));
//        }
//
//
//        if (sinkFuncs.size() < 1) {
//            throw new IllegalStateException("size of sinkFuncs can not be small than 1");
//        }
//        return sinkFuncs;
//    }
//
//    @Override
//    public ICompileAndPackage getCompileAndPackageManager() {
//        return new CompileAndPackage();
//    }
//
//    @TISExtension
//    public static class DefaultSinkFunctionDescriptor extends BaseSinkFunctionDescriptor {
//        @Override
//        public String getDisplayName() {
//            return DISPLAY_NAME_FLINK_SINK;
//        }
//
//        @Override
//        protected IEndTypeGetter.EndType getTargetType() {
//            return IEndTypeGetter.EndType.Clickhouse;
//        }
//    }
//}
