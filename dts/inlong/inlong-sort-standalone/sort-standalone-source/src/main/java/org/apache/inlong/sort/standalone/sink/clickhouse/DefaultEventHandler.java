/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.standalone.sink.clickhouse;

import org.apache.commons.math3.util.Pair;
import org.apache.inlong.sdk.commons.protocol.EventConstants;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.utils.UnescapeHelper;
import org.apache.pulsar.shade.org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DefaultEventHandler
 */
public class DefaultEventHandler implements IEventHandler {

    public static final Logger LOG = LoggerFactory.getLogger(DefaultEventHandler.class);

    public static final String KEY_EXTINFO = "extinfo";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * parse
     * @param idConfig
     * @param event
     * @return
     */
    @Override
    public Map<String, String> parse(ClickHouseIdConfig idConfig, ProfileEvent event) {
        final Map<String, String> resultMap = new HashMap<>();
        // parse fields
        String delimeter = idConfig.getSeparator();
        char cDelimeter = delimeter.charAt(0);
        String strContext = null;
        // for tab separator
        byte[] bodyBytes = event.getBody();
        int msgLength = event.getBody().length;
        int contentOffset = idConfig.getContentOffset();
        if (contentOffset > 0 && msgLength >= 1) {
            strContext = new String(bodyBytes, contentOffset, msgLength - contentOffset, Charset.defaultCharset());
        } else {
            strContext = new String(bodyBytes, Charset.defaultCharset());
        }
        // unescape
        List<String> columnValues = UnescapeHelper.toFiledList(strContext, cDelimeter);
        // column size
        List<String> contentFieldList = idConfig.getContentFieldList();
        int matchSize = Math.min(contentFieldList.size(), columnValues.size());
        for (int i = 0; i < matchSize; i++) {
            resultMap.put(contentFieldList.get(i), columnValues.get(i));
        }

        // ftime
        String ftime = dateFormat.format(new Date(event.getRawLogTime()));
        resultMap.put("ftime", ftime);
        // extinfo
        String extinfo = getExtInfo(event);
        resultMap.put("extinfo", extinfo);
        return resultMap;
    }

    /**
     * getExtInfo
     * 
     * @param  event
     * @return
     */
    public static String getExtInfo(ProfileEvent event) {
        String extinfoValue = event.getHeaders().get(KEY_EXTINFO);
        if (extinfoValue != null) {
            return KEY_EXTINFO + "=" + extinfoValue;
        }
        extinfoValue = KEY_EXTINFO + "=" + event.getHeaders().get(EventConstants.HEADER_KEY_SOURCE_IP);
        return extinfoValue;
    }

    /**
     * setValue
     * @param idConfig
     * @param columnValueMap
     * @param pstat
     * @throws SQLException 
     */
    public void setValue(ClickHouseIdConfig idConfig, Map<String, String> columnValueMap, PreparedStatement pstat)
            throws SQLException {
        List<Pair<String, Integer>> dbFieldList = idConfig.getDbFieldList();
        for (int i = 1; i <= dbFieldList.size(); i++) {
            Pair<String, Integer> pair = dbFieldList.get(i - 1);
            String fieldValue = columnValueMap.getOrDefault(pair.getKey(), "");
            int fieldType = pair.getValue();
            switch (fieldType) {
                // Int8 - [-128 : 127]
                case Types.TINYINT:
                    // TINYINT = -6;
                    pstat.setByte(i, NumberUtils.toByte(fieldValue, (byte) 0));
                    break;
                // Int16 - [-32768 : 32767]
                case Types.SMALLINT:
                    // SMALLINT= 5;
                    pstat.setShort(i, NumberUtils.toShort(fieldValue, (short) 0));
                    break;
                // Int32 - [-2147483648 : 2147483647]
                case Types.INTEGER:
                    // INTEGER = 4;
                    pstat.setInt(i, NumberUtils.toInt(fieldValue, 0));
                    break;
                // Int64 - [-9223372036854775808 : 9223372036854775807]
                // UInt8 - [0 : 255]
                // UInt16 - [0 : 65535]
                // UInt32 - [0 : 4294967295]
                // UInt64 - [0 : 18446744073709551615]
                case Types.BIGINT:
                    // BIGINT = -5;
                    pstat.setLong(i, NumberUtils.toLong(fieldValue, 0));
                    break;
                // Float32 - float
                case Types.FLOAT:
                    // FLOAT = 6;
                    pstat.setFloat(i, NumberUtils.toFloat(fieldValue, 0));
                    break;
                // Float64 – double
                case Types.DOUBLE:
                    // DOUBLE = 8;
                    pstat.setDouble(i, NumberUtils.toDouble(fieldValue, 0));
                    break;
                // Decimal32(s)
                // Decimal64(s)
                // Decimal128(s)
                case Types.NUMERIC:
                    // NUMERIC = 2;
                    pstat.setBigDecimal(i, NumberUtils.toScaledBigDecimal(fieldValue));
                    break;
                // String
                // FixedString(N)
                // Enum8
                // Enum16
                case Types.VARCHAR:
                    // VARCHAR = 12;
                case Types.LONGVARCHAR:
                    // LONGVARCHAR = -1;
                    pstat.setString(i, fieldValue);
                    break;
                // Date
                case Types.DATE:
                    // DATE= 91;
                    pstat.setDate(i, this.parseDate(fieldValue));
                    break;
                // Datetime
                // Datetime64
                case Types.TIMESTAMP:
                    // TIMESTAMP = 93;
                    pstat.setTimestamp(i, new Timestamp(this.parseDate(fieldValue).getTime()));
                    break;

                case Types.TIME:
                    // TIME= 92;
                    pstat.setTime(i, new Time(this.parseDate(fieldValue).getTime()));
                    break;
                default:
                    pstat.setString(i, fieldValue);
                    break;
            }
        }
    }

    /**
     * parseDate
     * @param fieldValue
     * @return
     */
    private Date parseDate(String fieldValue) {
        try {
            return new Date(dateFormat.parse(fieldValue).getTime());
        } catch (Exception e) {
            return new Date(0);
        }
    }
}
