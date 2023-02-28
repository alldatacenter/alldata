/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.enums;

import io.datavines.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum DataVinesDataType {

    /**
     * String Data Type
     */
    STRING_TYPE() {
        @Override
        public List<String> getMetricList() {
            return new ArrayList<>(Arrays.asList(
                            "column_null",
                            "column_not_null",
                            "column_blank",
//                            "Values with Leading Whitespace",
//                            "Values with Trailing Whitespace",
//                            "Values with Both Whitespace",
                            "column_avg_length",
                            "column_max_length",
                            "column_max",
                            "column_min_length",
                            "column_min",
//                          "Unique Values",
                            "column_unique",
//                            "column_duplicate",
                            "column_distinct",
                            "column_histogram"
                    ));
        }

        @Override
        public String getName() {
            return "string";
        }
    },

    NUMERIC_TYPE() {
        @Override
        public List<String> getMetricList() {
            return new ArrayList<>(Arrays.asList(
                    "column_null",
                    "column_not_null",
                    "column_max",
                    "column_min",
                    "column_avg",
                    "column_sum",
                    "column_std_dev",
                    "column_variance",
                    "column_unique",
//                    "column_duplicate",
                    "column_distinct",
//                    "Unique Values",
//                    "25th Percentile",
//                    "50th Percentile",
//                    "75th Percentile",
                    "column_histogram"
//                    "Margin of Error"
            ));
        }

        @Override
        public String getName() {
            return "numeric";
        }
    },

    DATE_TIME_TYPE() {
        @Override
        public List<String> getMetricList() {
            return new ArrayList<>(Arrays.asList(
                    "column_null",
                    "column_not_null",
                    "column_histogram",
                    "column_max",
                    "column_min",
                    "column_unique",
//                    "column_duplicate",
                    "column_distinct"
//                    "Unique Values"
            ));
        }

        @Override
        public String getName() {
            return "date_time";
        }

    };

    public abstract List<String> getMetricList();

    public abstract String getName();

    public static DataVinesDataType getType(String type) {
        if (StringUtils.isEmpty(type)) {
            return null;
        }

        type = type.toLowerCase();
        if (type.contains("int") || "decimal".equalsIgnoreCase(type) || "float".equalsIgnoreCase(type) || "double".equalsIgnoreCase(type)) {
            return NUMERIC_TYPE;
        } else if (type.contains("char") || "blob".equalsIgnoreCase(type) || type.contains("text")) {
            return STRING_TYPE;
        } else if (type.contains("time")) {
            return DATE_TIME_TYPE;
        } else {
            return STRING_TYPE;
        }
    }
}
