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

package org.apache.inlong.manager.service.resource.sink.hudi;

import org.apache.inlong.manager.pojo.sink.hudi.HudiColumnInfo;
import org.apache.inlong.manager.pojo.sink.hudi.HudiType;

import java.util.Optional;

/**
 * Converter between Java type and Hive type that reflects the behavior before This converter reflects the old behavior
 * that includes:
 * <ul>
 * <li>Use old java.sql.* time classes for time data types.
 * <li>Only support millisecond precision for timestamps or day-time intervals.
 * <li>Let variable precision and scale for decimal types pass through the planner.
 * </ul>
 * {@see org.apache.flink.table.types.utils.TypeInfoDataTypeConverter}
 */
public class HudiTypeConverter {

    /**
     * Converter field type of column to Hive field type.
     */
    public static String convert(HudiColumnInfo column) {
        return Optional.ofNullable(column)
                .map(col -> HudiType.forType(col.getType()))
                .map(hudiType -> {
                    if (HudiType.DECIMAL == hudiType) {
                        return String.format("decimal(%d, %d)", column.getPrecision(), column.getScale());
                    } else if (HudiType.FIXED == hudiType) {
                        return String.format("fixed(%d)", column.getLength());
                    } else {
                        return hudiType.getHiveType();
                    }
                })
                .orElseThrow(() -> new RuntimeException("Can not properly convert type of column: " + column));
    }

}
