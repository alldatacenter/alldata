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
//import com.qlangtech.tis.realtime.transfer.DTO;
//import org.apache.flink.configuration.Configuration;
//import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import ru.ivi.opensource.flinkclickhousesink.ClickHouseSink;
//import ru.ivi.opensource.flinkclickhousesink.applied.ClickHouseSinkManager;
//import ru.ivi.opensource.flinkclickhousesink.applied.Sink;
//
//import java.sql.Types;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Properties;
//
///**
// * @author: 百岁（baisui@qlangtech.com）
// * @create: 2021-12-01 14:23
// **/
//public class TISClickHouseSink extends RichSinkFunction<DTO> {
//
//    private static final Logger logger = LoggerFactory.getLogger(ClickHouseSink.class);
//
//    private static final Object DUMMY_LOCK = new Object();
//
//    private final Properties localProperties;
//    private final Map<String, String> globalParams;
//
//    private volatile static transient ClickHouseSinkManager sinkManager;
//    private transient Sink sink;
//
//    private final List<ColMeta> colsMeta;
//
//    public TISClickHouseSink(Map<String, String> globalParams, Properties properties, List<ColMeta> colsMeta) {
//        this.localProperties = properties;
//        this.globalParams = globalParams;
//        this.colsMeta = colsMeta;
//        if (globalParams == null || globalParams.isEmpty()) {
//            throw new IllegalArgumentException("globalParams can not be empty");
//        }
//    }
//
//    @Override
//    public void open(Configuration config) {
//        if (sinkManager == null) {
//            synchronized (DUMMY_LOCK) {
//                if (sinkManager == null) {
////                    Map<String, String> params = getRuntimeContext()
////                            .getExecutionConfig()
////                            .getGlobalJobParameters()
////                            .toMap();
//
//                    sinkManager = new ClickHouseSinkManager(globalParams);
//                }
//            }
//        }
//
//        sink = sinkManager.buildSink(localProperties);
//    }
//
//
//    @Override
//    public void invoke(DTO dto, Context context) {
//        if (dto.getEventType() == DTO.EventType.DELETE) {
//            // 删除操作先忽略
//            return;
//        }
//        String recordAsCSV = convertCSV(dto);
//        try {
//            sink.put(recordAsCSV);
//        } catch (Exception e) {
//            logger.error("Error while sending data to ClickHouse, record = {}", recordAsCSV, e);
//            throw new RuntimeException(recordAsCSV, e);
//        }
//    }
//
//    private void strVal(final Map<String, Object> afterVals, ColMeta cm, StringBuffer result) {
//        Object val = afterVals.get(cm.getKey());
//        if (val != null) {
//            result.append("'").append(val).append("'");
//        } else {
//            result.append("null");
//        }
//    }
//
//    private void numericVal(final Map<String, Object> afterVals, ColMeta cm, StringBuffer result) {
//        Object val = afterVals.get(cm.getKey());
//        if (val != null) {
//            result.append(val);
//        } else {
//            result.append("null");
//        }
//    }
//
//    private String convertCSV(DTO dto) {
//        final Map<String, Object> afterVals = dto.getAfter();
//        Objects.requireNonNull(afterVals, "afterVals can not be null");
//        StringBuffer result = new StringBuffer("(");
//        int size = colsMeta.size();
//        int index = 0;
//        for (ColMeta cm : colsMeta) {
//            switch (cm.getType().type) {
//                case Types.INTEGER:
//                case Types.TINYINT:
//                case Types.SMALLINT:
//                case Types.BIGINT:
//                case Types.FLOAT:
//                case Types.DOUBLE:
//                case Types.DECIMAL:
//                case Types.DATE:
//                case Types.TIME:
//                case Types.TIMESTAMP:
//                case Types.BIT:
//                case Types.BOOLEAN:
//                    numericVal(afterVals, cm, result);
//                    break;
//                case Types.BLOB:
//                case Types.BINARY:
//                case Types.LONGVARBINARY:
//                case Types.VARBINARY:
//                default:
//                    strVal(afterVals, cm, result);
//            }
//            if (index++ < (size - 1)) {
//                result.append(",");
//            }
//        }
//        result.append(")");
//        return result.toString();
//
//    }
//
//    @Override
//    public void close() throws Exception {
//        if (sink != null) {
//            sink.close();
//        }
//
//        if (sinkManager != null) {
//            if (!sinkManager.isClosed()) {
//                synchronized (DUMMY_LOCK) {
//                    if (!sinkManager.isClosed()) {
//                        sinkManager.close();
//                    }
//                }
//            }
//        }
//
//        super.close();
//    }
//}
