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
package io.datavines.engine.spark.config;

import io.datavines.common.utils.StringUtils;
import io.datavines.metric.api.ColumnInfo;
import io.datavines.metric.api.MetricConstants;

import java.util.ArrayList;
import java.util.List;

public class SparkSinkSqlBuilder {

    public static String getActualValueSql() {

        List<String> columnList = new ArrayList<>();
        for (ColumnInfo columnInfo : MetricConstants.ACTUAL_COLUMN_LIST) {
            if (columnInfo.isNeedSingleQuotation()) {
                columnList.add(StringUtils.wrapperSingleQuotes("${"+columnInfo.getParameterName()+"}") + " as " + columnInfo.getName());
            } else {
                columnList.add("${"+columnInfo.getParameterName()+"}" + " as " + columnInfo.getName());
            }

        }

        return "select " + String.join(", ", columnList) + " from ${actual_table}";
    }

    private static String getBasicSql() {
        List<String> columnList = new ArrayList<>();
        for (ColumnInfo columnInfo : MetricConstants.RESULT_COLUMN_LIST) {

            if (columnInfo.isNeedSingleQuotation()) {
                columnList.add(StringUtils.wrapperSingleQuotes("${"+columnInfo.getParameterName()+"}") + " as " + columnInfo.getName());
            } else {
                columnList.add("${"+columnInfo.getParameterName()+"}" + " as " + columnInfo.getName());
            }

        }

        return "select " + String.join(", ", columnList);
    }

    public static String getDefaultSinkSql() {
        return getBasicSql() + " from ${actual_table} full join ${expected_table}";
    }

    public static String getMultiTableComparisonSinkSql() {
        return getBasicSql()
                + " from ( ${actual_execute_sql} ) tmp1 "
                + "join ( ${expected_execute_sql} ) tmp2";
    }

    public static String getSingleTableCustomSqlSinkSql() {
        return getBasicSql()
                + " from ( ${actual_table} ) tmp1 "
                + "join ${expected_table}";
    }
}
