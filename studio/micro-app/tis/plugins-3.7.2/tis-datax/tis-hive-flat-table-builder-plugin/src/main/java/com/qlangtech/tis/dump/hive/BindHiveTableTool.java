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
package com.qlangtech.tis.dump.hive;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.hive.HiveColumn;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 将hdfs上的数据和hive database中的表绑定
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年12月15日 下午4:49:42
 */
public class BindHiveTableTool {
    private static final Logger logger = LoggerFactory.getLogger(HiveTableBuilder.class);

    public static void bindHiveTables(DataSourceMeta engine, DataSourceMeta.JDBCConnection hiveConn, ITISFileSystem fileSystem, Map<EntityName
            , Callable<HiveBindConfig>> hiveTables, String timestamp) {

        Objects.nonNull(fileSystem);
        Objects.nonNull(timestamp);
        try {
            // Dump 任务结束,开始绑定hive partition
            HiveTableBuilder hiveTableBuilder = new HiveTableBuilder(timestamp);
            hiveTableBuilder.setFileSystem(fileSystem);
            hiveTableBuilder.bindHiveTables(engine, hiveTables, hiveConn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void bindHiveTables(DataSourceMeta engine, DataSourceMeta.JDBCConnection hiveConn, ITISFileSystem fileSystem
            , Map<EntityName, Callable<HiveBindConfig>> hiveTables
            , String timestamp, HiveTableBuilder.IsTableSchemaSame isTableSchemaSame
            , HiveTableBuilder.CreateHiveTableAndBindPartition createHiveTableAndBindPartition) {
        Objects.nonNull(fileSystem);
        Objects.nonNull(timestamp);
        try {
            // Dump 任务结束,开始绑定hive partition
            HiveTableBuilder hiveTableBuilder = new HiveTableBuilder(timestamp);
            hiveTableBuilder.setFileSystem(fileSystem);
            hiveTableBuilder.bindHiveTables(engine, hiveTables, hiveConn, isTableSchemaSame, createHiveTableAndBindPartition);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("all")
    public static List<HiveColumn> getColumns(ITISFileSystem fs, EntityName hiveTable, String timestamp) throws IOException {
        String hivePath = hiveTable.getNameWithPath();
        InputStream input = null;
        List<HiveColumn> cols = new ArrayList<>();
        try {
            input = fs.open(fs.getPath(fs.getRootDir() + "/" + hivePath + "/all/" + timestamp + "/" + ColumnMetaData.KEY_COLS_METADATA));
            // input = fileSystem.open(path);
            String content = IOUtils.toString(input, TisUTF8.getName());
            JSONArray array = (JSONArray) JSON.parse(content);
            for (Object anArray : array) {
                JSONObject o = (JSONObject) anArray;
                HiveColumn col = new HiveColumn();
                col.setName(o.getString("key"));
                col.setIndex(o.getIntValue("index"));
                // col.setType(getHiveType(o.getIntValue("type")).name());
                col.setDataType(new DataType(o.getIntValue("type")));
                cols.add(col);
            }
        } finally {
            IOUtils.closeQuietly(input);
        }
        return cols;
    }

//    private static SupportHiveDataType getHiveType(int type) {
//        // java.sql.Types
//        // https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTableCreate/Drop/TruncateTable
//        switch (type) {
//            case BIT:
//            case TINYINT:
//            case SMALLINT:
//            case INTEGER:
//                return SupportHiveDataType.INT;
//            case BIGINT:
//                return SupportHiveDataType.BIGINT;
//            case FLOAT:
//            case REAL:
//            case DOUBLE:
//            case NUMERIC:
//            case DECIMAL:
//                return SupportHiveDataType.DOUBLE;
//            case TIMESTAMP:
//                return SupportHiveDataType.TIMESTAMP;
//            case DATE:
//                return SupportHiveDataType.DATE;
//            default:
//                return SupportHiveDataType.STRING; //HiveColumn.HIVE_TYPE_STRING;
//        }
//    }

    public static class HiveBindConfig {
        public final List<HiveColumn> colsMeta;
        public final Path tabDumpParentPath;

        public HiveBindConfig(List<HiveColumn> colsMeta, Path tabDumpParentPath) {
            this.colsMeta = colsMeta;
            this.tabDumpParentPath = tabDumpParentPath;
        }
    }
}
