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

package com.qlangtech.plugins.incr.flink.cdc.valconvert;

import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import io.debezium.spi.converter.CustomConverter;
import io.debezium.spi.converter.RelationalColumn;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * copy from:https://github.com/holmofy/debezium-datetime-converter/blob/master/src/main/java/com/darcytech/debezium/converter/MySqlDateTimeConverter.java <br/>
 * <p>
 * 处理Debezium时间转换的问题
 * Debezium默认将MySQL中datetime类型转成UTC的时间戳({@link io.debezium.time.Timestamp})，时区是写死的没法儿改，
 * 导致数据库中设置的UTC+8，到kafka中变成了多八个小时的long型时间戳
 * Debezium默认将MySQL中的timestamp类型转成UTC的字符串。
 * | mysql                               | mysql-binlog-connector                   | debezium                          |
 * | ----------------------------------- | ---------------------------------------- | --------------------------------- |
 * | date<br>(2021-01-28)                | LocalDate<br/>(2021-01-28)               | Integer<br/>(18655)               |
 * | time<br/>(17:29:04)                 | Duration<br/>(PT17H29M4S)                | Long<br/>(62944000000)            |
 * | timestamp<br/>(2021-01-28 17:29:04) | ZonedDateTime<br/>(2021-01-28T09:29:04Z) | String<br/>(2021-01-28T09:29:04Z) |
 * | Datetime<br/>(2021-01-28 17:29:04)  | LocalDateTime<br/>(2021-01-28T17:29:04)  | Long<br/>(1611854944000)          |
 *
 * @see io.debezium.connector.mysql.converters.TinyIntOneToBooleanConverter
 */

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-13 22:50
 **/
public abstract class DateTimeConverter implements CustomConverter<SchemaBuilder, RelationalColumn> {

    private static final Logger logger = LoggerFactory.getLogger(DateTimeConverter.class);
    protected DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;
    protected DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_TIME;
    public DateTimeFormatter datetimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
    protected DateTimeFormatter timestampFormatter = DateTimeFormatter.ISO_DATE_TIME;

    protected static ZoneId timestampZoneId = ZoneId.systemDefault();


    public static void setDatetimeConverters(String convertType, Properties debeziumProperties) {
        debeziumProperties.put("converters", "datetime");
        debeziumProperties.put("datetime.type", convertType);
        debeziumProperties.put("datetime.format.date", "yyyy-MM-dd");
        debeziumProperties.put("datetime.format.time", "HH:mm:ss");
        debeziumProperties.put("datetime.format.datetime", "yyyy-MM-dd HH:mm:ss");
        debeziumProperties.put("datetime.format.timestamp", "yyyy-MM-dd HH:mm:ss");
        debeziumProperties.put("datetime.format.timestamp.zone"
                , DataSourceFactory.DEFAULT_SERVER_TIME_ZONE.getId());
    }


    @Override
    public void configure(Properties props) {
        readProps(props, "format.date", p -> dateFormatter = DateTimeFormatter.ofPattern(p));
        readProps(props, "format.time", p -> timeFormatter = DateTimeFormatter.ofPattern(p));
        readProps(props, "format.datetime", p -> datetimeFormatter = DateTimeFormatter.ofPattern(p));
        readProps(props, "format.timestamp", p -> timestampFormatter = DateTimeFormatter.ofPattern(p));
        readProps(props, "format.timestamp.zone", z -> timestampZoneId = ZoneId.of(z));
    }

    private void readProps(Properties properties, String settingKey, Consumer<String> callback) {
        String settingValue = (String) properties.get(settingKey);
        if (settingValue == null || settingValue.length() == 0) {
            return;
        }
        try {
            callback.accept(settingValue.trim());
        } catch (IllegalArgumentException | DateTimeException e) {
            logger.error("The \"{}\" setting is illegal:{}", settingKey, settingValue);
            throw e;
        }
    }

    @Override
    public void converterFor(RelationalColumn column, ConverterRegistration<SchemaBuilder> registration) {
        String sqlType = column.typeName().toUpperCase();
        SchemaBuilder schemaBuilder = null;
        Converter converter = null;
        if ("DATE".equals(sqlType)) {
            schemaBuilder = SchemaBuilder.string().optional().name("com.darcytech.debezium.date.string");
            converter = this::convertDate;
        }
        if ("TIME".equals(sqlType)) {
            schemaBuilder = SchemaBuilder.string().optional().name("com.darcytech.debezium.time.string");
            converter = this::convertTime;
        }
        if ("DATETIME".equals(sqlType)) {
            schemaBuilder = SchemaBuilder.string().optional().name("com.darcytech.debezium.datetime.string");
            converter = this::convertDateTime;
        }
        if ("TIMESTAMP".equals(sqlType)) {
            schemaBuilder = SchemaBuilder.string().optional().name("com.darcytech.debezium.timestamp.string");
            converter = this::convertTimestamp;
        }
        if (schemaBuilder != null) {
            registration.register(schemaBuilder, converter);
            logger.info("register converter for sqlType {} to schema {}", sqlType, schemaBuilder.name());
        }
    }

    protected abstract String convertDate(Object input);
//    {
//        if (input instanceof LocalDate) {
//            return dateFormatter.format((LocalDate) input);
//        }
//        if (input instanceof Integer) {
//            LocalDate date = LocalDate.ofEpochDay((Integer) input);
//            return dateFormatter.format(date);
//        }
//        return null;
//    }

    protected abstract String convertTime(Object input);
//    {
//        if (input instanceof Duration) {
//            Duration duration = (Duration) input;
//            long seconds = duration.getSeconds();
//            int nano = duration.getNano();
//            LocalTime time = LocalTime.ofSecondOfDay(seconds).withNano(nano);
//            return timeFormatter.format(time);
//        }
//        return null;
//    }

    protected abstract String convertDateTime(Object input);
//    {
//        if (input instanceof LocalDateTime) {
//            return datetimeFormatter.format((LocalDateTime) input);
//        }
//        return null;
//    }

    protected abstract String convertTimestamp(Object input); //{
//        if (input instanceof ZonedDateTime) {
//            // mysql的timestamp会转成UTC存储，这里的zonedDatetime都是UTC时间
//            ZonedDateTime zonedDateTime = (ZonedDateTime) input;
//            LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(timestampZoneId).toLocalDateTime();
//            return timestampFormatter.format(localDateTime);
//        }
//        return null;
//    }

}
