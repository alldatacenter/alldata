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
//package com.qlangtech.plugins.incr.flink.cdc.mysql;
//
//import com.qlangtech.plugins.incr.flink.cdc.ISourceValConvert;
//import com.qlangtech.plugins.incr.flink.cdc.TabColIndexer;
//import com.qlangtech.tis.plugin.ds.ColumnMetaData;
//import com.qlangtech.tis.plugin.ds.ISelectedTab;
//import com.qlangtech.tis.realtime.transfer.DTO;
//import org.apache.kafka.connect.data.Field;
//import org.apache.kafka.connect.data.Schema;
//
//import java.io.Serializable;
//import java.sql.Types;
//import java.text.SimpleDateFormat;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.util.Date;
//
///**
// * https://debezium.io/documentation/reference/1.8/connectors/mysql.html#mysql-temporal-types
// *
// * @author: 百岁（baisui@qlangtech.com）
// * @create: 2021-12-20 11:06
// **/
//public class MySQLSourceValConvert implements ISourceValConvert, Serializable {
//
//    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
//        @Override
//        protected SimpleDateFormat initialValue() {
//            return new SimpleDateFormat("yyyy-MM-dd");
//        }
//    };
//    //2021-12-18T04:51:24Z
//    public static final ThreadLocal<SimpleDateFormat> TIME_FORMAT = new ThreadLocal<SimpleDateFormat>() {
//        @Override
//        protected SimpleDateFormat initialValue() {
//            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//           // return SimpleDateFormat.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
//        }
//    };
//    private static final ZoneId zof = ZoneId.of("Z");
//    // private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
//
//    private TabColIndexer colIndexer;
//
//    public MySQLSourceValConvert(TabColIndexer colIndexer) {
//        this.colIndexer = colIndexer;
//    }
//
//    @Override
//    public Object convert(DTO dto, Field field, Object val) {
//        if (val == null) {
//            return null;
//        }
//        CMeta colMeta = colIndexer.getColMeta(dto.getTableName(), field.name());
//        if (colMeta == null) {
//            return val;
//        }
//        DataType t = colMeta.getType();
//        switch (t.type) {
//            case Types.DECIMAL:
//                return val;
//            case Types.DATE: {
////                Date d = org.apache.kafka.connect.data.Date.toLogical(org.apache.kafka.connect.data.Date.SCHEMA, (Integer) val);
////                return DATE_FORMAT.get().format(d);
//            }
//            case Types.TIME: {
//                Date d = new Date((Long) val);
//                //  ZonedTimestamp.toIsoString(d);
//                //LocalDateTime localDateTime = LocalDateTime.ofInstant(d.toInstant(), zof);
//                return TIME_FORMAT.get().format(d);
//            }
//            case Types.TIMESTAMP:
//                if (Schema.Type.STRING == field.schema().type()) {
//                    return val;
//                } else {
//                    Date d = new Date((Long) val);
//                    //return TIME_FORMAT.get().format(d);
////                    LocalDateTime localDateTime = LocalDateTime.ofInstant(d.toInstant(), zof);
////                    return localDateTime.format(TIME_FORMAT.get());
//                    return TIME_FORMAT.get().format(d);
//                }
//
//            case Types.BIT:
//            case Types.BOOLEAN:
//            case Types.BLOB:
//            case Types.BINARY:
//            case Types.LONGVARBINARY:
//            case Types.VARBINARY:
//            case Types.INTEGER:
//            case Types.TINYINT:
//            case Types.SMALLINT:
//            case Types.BIGINT:
//            case Types.FLOAT:
//            case Types.DOUBLE:
//            default:
//                return val;
//        }
//    }
//}
