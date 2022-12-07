/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.platform;


import com.platform.explainer.lineage.*;

/**
 * Lineage Build
 * @author AllDataDC
 * @date 2022/12/1
 */
public class FlinkLineageBuild {


    public static void main(String[] args) {
        String sql = fetchInsertDDL();
        buildLineage(sql);
    }

    private static String fetchInsertDDL() {

        String sourceTableDDL = "CREATE TABLE data_gen (\n" +
                "    amount BIGINT\n" +
                ") WITH (\n" +
                "    'connector' = 'datagen'," +
                "    'rows-per-second' = '1'," +
                "    'number-of-rows' = '3'," +
                "    'fields.amount.kind' = 'random'," +
                "    'fields.amount.min' = '10'," +
                "    'fields.amount.max' = '11');";
        String sinkTableDDL = "CREATE TABLE mysql_sink (\n" +
                "  amount BIGINT,\n" +
                "  PRIMARY KEY (amount) NOT ENFORCED\n" +
                ") WITH (\n" +
                "    'connector' = 'jdbc',\n" +
                "    'url' = 'jdbc:mysql://localhost:3306/test_db',\n" +
                "    'table-name' = 'test_table',\n" +
                "    'username' = 'root',\n" +
                "    'password' = '123456',\n" +
                "    'lookup.cache.max-rows' = '5000',\n" +
                "    'lookup.cache.ttl' = '10min'\n" +
                ");";
        String insertSQL = "INSERT INTO mysql_sink SELECT amount as amount FROM data_gen;\n";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(sourceTableDDL).append("\n").append(sinkTableDDL).append("\n").append(insertSQL).append("\n");
        return stringBuilder.toString();
    }

    /**
     *
     */
    public static void buildLineage(String sql) {
        LineageResult result = LineageBuilder.getLineage(sql);

        System.out.println("1、Flink血缘构建结果-表:\n" + result.getTables());
        for (int i = 0; i < result.getTables().size(); i++) {
            LineageTable lineageTable = result.getTables().get(i);
            System.out.println("表ID: " + lineageTable.getId());
            System.out.println("表Name" + lineageTable.getName());
            for (int j = 0; j < lineageTable.getColumns().size(); j++) {
                LineageColumn lineageColumn = lineageTable.getColumns().get(j);
                System.out.println("表ID: " + lineageTable.getId() + "\n表Name" + lineageTable.getName() + "\n表-列" + lineageColumn + "\n");
            }
        }
        System.out.println("2、Flink血缘构建结果-边:\n" + result.getRelations());
        for (int i = 0; i < result.getRelations().size(); i++) {
            LineageRelation lineageRelation = result.getRelations().get(i);
            System.out.println("表-边: " + lineageRelation);
        }

    }

}
