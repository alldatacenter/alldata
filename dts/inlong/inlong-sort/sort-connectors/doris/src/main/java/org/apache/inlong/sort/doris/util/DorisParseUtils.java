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

package org.apache.inlong.sort.doris.util;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.RowData.FieldGetter;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.types.RowKind;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class primarily serving DorisDynamicSchemaOutputFormat
 */
public class DorisParseUtils {

    /**
     * Pattern of escape mode for hexadecimal characters, such as "hi\\x33hi\\x44hello".
     */
    private static final Pattern HEX_PATTERN = Pattern.compile("\\\\x(\\d{2})");

    /**
     * A utility function used to determine the DORIS_DELETE_SIGN for a row change.
     *
     * @param rowKind the row change
     * @return the doris delete sign corresponding to the change
     */
    public static String parseDeleteSign(RowKind rowKind) {
        if (RowKind.INSERT.equals(rowKind) || RowKind.UPDATE_AFTER.equals(rowKind)) {
            return "0";
        } else if (RowKind.DELETE.equals(rowKind) || RowKind.UPDATE_BEFORE.equals(rowKind)) {
            return "1";
        } else {
            throw new RuntimeException("Unrecognized row kind: " + rowKind.toString());
        }
    }

    /**
     * A utility function used to handle special fieldGetters for specific
     *
     * @param type the logical type of the field getter array
     * @param pos the index of the corresponding row
     * @return the fieldGetter created
     */
    public static FieldGetter createFieldGetter(LogicalType type, int pos) {
        FieldGetter getter;
        if (type.toString().equalsIgnoreCase(LogicalTypeEnum.DATE.getType())) {
            getter = row -> {
                if (row.isNullAt(pos)) {
                    return null;
                }
                return DorisParseUtils.epochToDate(row.getInt(pos));
            };
        } else {
            getter = RowData.createFieldGetter(type, pos);
        }
        return getter;
    }

    /**
     * A utility used to parse a string according to the given hexadecimal escape sequence.
     * <p/>
     * Example input: ""hi\\x33hi\\x44hello"" , where \x33 is '!', \x44 is ','
     * Example output: "hi!hi,hello"
     *
     * @param hexStr hex string before parsing
     * @return the parsed string
     */
    public static String escapeString(String hexStr) {
        Matcher matcher = HEX_PATTERN.matcher(hexStr);
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buf, String.format("%s", (char) Integer.parseInt(matcher.group(1))));
        }
        matcher.appendTail(buf);

        return buf.toString();
    }

    /**
     * A utility used to change epoch dates into normal dates
     * <p/>
     * Example input: 0
     * Example output: 1970-01-01
     *
     * @param obj the epoch date that is either long or int
     * @return the transformed local date
     */
    public static LocalDate epochToDate(Object obj) {
        if (obj instanceof Long) {
            return LocalDate.ofEpochDay((Long) obj);
        }
        if (obj instanceof Integer) {
            return LocalDate.ofEpochDay((Integer) obj);
        }
        throw new IllegalArgumentException(
                "Convert to LocalDate failed from unexpected value '" + obj + "' of type " + obj.getClass().getName());
    }

    private enum LogicalTypeEnum {

        DATE("DATE");

        private final String logicalType;

        LogicalTypeEnum(String logicalType) {
            this.logicalType = logicalType;
        }

        public String getType() {
            return logicalType;
        }
    }

}
