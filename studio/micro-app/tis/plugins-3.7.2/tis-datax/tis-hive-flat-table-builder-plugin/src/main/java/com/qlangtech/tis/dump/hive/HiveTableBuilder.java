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

import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.hdfs.impl.HdfsPath;
import com.qlangtech.tis.hive.HdfsFormat;
import com.qlangtech.tis.hive.HiveColumn;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 在hive中生成新表，和在新表上创建创建Partition
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @date: 2015年10月31日 下午3:43:14
 **/
public class HiveTableBuilder {
    private static final Logger logger = LoggerFactory.getLogger(HiveTableBuilder.class);
    private final String timestamp;

    protected ITISFileSystem fileSystem;
    private final HdfsFormat fsFormat;


    public HiveTableBuilder(String timestamp) {
        this(timestamp, HdfsFormat.DEFAULT_FORMAT);
    }

    public HiveTableBuilder(String timestamp, HdfsFormat fsFileFormat) {
        super();
        if (StringUtils.isEmpty(timestamp)) {
            throw new IllegalArgumentException("param timestamp can not be null");
        }
        this.timestamp = timestamp;
        this.fsFormat = fsFileFormat;
    }

//        /**
//         * @param
//         * @return
//         * @throws Exception
//         */
//        public static List<String> getExistTables(Connection conn) throws Exception {
//            final List<String> tables = new ArrayList<>();
//            HiveDBUtils.query(conn, "show tables", new HiveDBUtils.ResultProcess() {
//                @Override
//                public boolean callback(ResultSet result) throws Exception {
//                    tables.add(result.getString(1));
//                    return true;
//                }
//            });
//            return tables;
//        }

//        /**
//         * @param
//         * @return
//         * @throws Exception
//         */
//        public static List<String> getExistTables(MREngine mrEngine, DataSourceMeta.JDBCConnection conn, String dbName) throws Exception {
//            final List<String> tables = new ArrayList<>();
//            if (!HiveTask.isDBExists(mrEngine, conn, dbName)) {
//                // DB都不存在，table肯定就不存在啦
//                return tables;
//            }
//            conn.query("show tables from " + dbName, result -> tables.add(result.getString(2)));
//            return tables;
//        }

    /**
     * Reference:https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTableCreate/Drop/TruncateTable
     *
     * @param conn
     * @param table
     * @param cols
     * @param sqlCommandTailAppend
     * @throws Exception
     */
    public StringBuffer createHiveTableAndBindPartition(
            DataSourceMeta sourceMeta, DataSourceMeta.JDBCConnection conn, EntityName table, List<HiveColumn> cols, SQLCommandTailAppend sqlCommandTailAppend) throws Exception {
        if (StringUtils.isEmpty(table.getDbName())) {
            throw new IllegalArgumentException("table.getDbName can not be empty");
        }

        int maxColLength = 0;
        int tmpLength = 0;
        for (HiveColumn c : cols) {
            tmpLength = StringUtils.length(c.getName());
            if (tmpLength < 1) {
                throw new IllegalStateException("col name length can not small than 1,cols size:" + cols.size());
            }
            if (tmpLength > maxColLength) {
                maxColLength = tmpLength;
            }
        }
        HiveColumn o = null;
        String colformat = "%-" + (++maxColLength) + "s";
        StringBuffer hiveSQl = new StringBuffer();
        HiveDBUtils.executeNoLog(conn, "CREATE DATABASE IF NOT EXISTS " + table.getDbName() + "");
        hiveSQl.append("CREATE EXTERNAL TABLE IF NOT EXISTS " + table.getFullName((sourceMeta.getEscapeChar())) + " (\n");
        final int colsSize = cols.size();
        for (int i = 0; i < colsSize; i++) {
            o = cols.get(i);
            if (i != o.getIndex()) {
                throw new IllegalStateException("i:" + i + " shall equal with index:" + o.getIndex());
            }

            hiveSQl.append("  ").append(String.format(colformat, sourceMeta.getEscapedEntity(sourceMeta.removeEscapeChar(o.getName()))))
                    .append(" ").append(o.getDataType());

//                hiveSQl.append("  ").append(sourceMeta.getEscapeChar())
//                        .append(String.format(colformat, sourceMeta.removeEscapeChar(o.getName()) + sourceMeta.getEscapeChar()))
//                        .append(" ").append(o.getDataType());
            if ((i + 1) < colsSize) {
                hiveSQl.append(",");
            }
            hiveSQl.append("\n");
        }
        hiveSQl.append(") COMMENT 'tis_tmp_" + table + "' PARTITIONED BY(" + IDumpTable.PARTITION_PT + " string," + IDumpTable.PARTITION_PMOD + " string)   ");
        hiveSQl.append(this.fsFormat.getRowFormat());
        hiveSQl.append("STORED AS " + this.fsFormat.getFileType().getType());
        sqlCommandTailAppend.append(hiveSQl);
        logger.info(hiveSQl.toString());
        HiveDBUtils.executeNoLog(conn, hiveSQl.toString());
        return hiveSQl;
    }

    /**
     * 和hdfs上已经导入的数据进行绑定
     *
     * @param hiveTables
     * @throws Exception
     */
    public void bindHiveTables(DataSourceMeta engine, Map<EntityName, Callable<BindHiveTableTool.HiveBindConfig>> hiveTables, DataSourceMeta.JDBCConnection conn) throws Exception {
        bindHiveTables(engine, hiveTables, conn,
                (columns, hiveTable) -> {
                    return isTableSame(engine, conn, columns.colsMeta, hiveTable);
                },
                (columns, hiveTable) -> {
                    this.createHiveTableAndBindPartition(engine, conn, columns, hiveTable);
                });
    }


    /**
     * 和hdfs上已经导入的数据进行绑定
     *
     * @param hiveTables
     * @throws Exception
     */
    public void bindHiveTables(DataSourceMeta engine, Map<EntityName, Callable<BindHiveTableTool.HiveBindConfig>> hiveTables, DataSourceMeta.JDBCConnection conn
            , IsTableSchemaSame isTableSchemaSame, CreateHiveTableAndBindPartition createHiveTableAndBindPartition) throws Exception {

        try {
            EntityName hiveTable = null;

            for (Map.Entry<EntityName, Callable<BindHiveTableTool.HiveBindConfig>> entry : hiveTables.entrySet()) {
                // String hivePath = hiveTable.getNameWithPath();
                hiveTable = entry.getKey();

                final List<String> tables = engine.getTablesInDB().getTabs();
                BindHiveTableTool.HiveBindConfig columns = entry.getValue().call();
                if (tables.contains(hiveTable.getTableName())) {
                    //isTableSame(conn, columns.colsMeta, hiveTable)
                    if (!isTableSchemaSame.isSame(columns, hiveTable)) {
                        // 原表有改动，需要把表drop掉
                        logger.info("table:" + hiveTable.getTableName() + " exist but table col metadta has been changed");
                        HiveDBUtils.execute(conn, "DROP TABLE " + hiveTable);
                        createHiveTableAndBindPartition.run(columns, hiveTable);
                        // this.createHiveTableAndBindPartition(conn, columns, hiveTable);
                        //return;
                    } else {
                        logger.info("table:" + hiveTable.getTableName() + " exist will bind new partition");
                    }
                } else {
                    logger.info("table not exist,will create now:" + hiveTable.getTableName() + ",exist table:["
                            + tables.stream().collect(Collectors.joining(",")) + "]");
                    //  this.createHiveTableAndBindPartition(conn, columns, hiveTable);
                    createHiveTableAndBindPartition.run(columns, hiveTable);
                    //return;
                }

                // 生成 hive partitiion
                this.bindPartition(conn, columns, hiveTable, 0);
            }
        } finally {
//                try {
//                    conn.close();
//                } catch (Throwable e) {
//                }
        }
    }

    public interface CreateHiveTableAndBindPartition {
        void run(BindHiveTableTool.HiveBindConfig columns, EntityName hiveTable) throws Exception;
    }

    public interface IsTableSchemaSame {
        boolean isSame(BindHiveTableTool.HiveBindConfig columns, EntityName hiveTable) throws Exception;
    }


    public static boolean isTableSame(DataSourceMeta mrEngine, DataSourceMeta.JDBCConnection conn, List<HiveColumn> columns, EntityName tableName) throws Exception {
        boolean isTableSame;
        final StringBuffer errMsg = new StringBuffer();
        final StringBuffer equalsCols = new StringBuffer("compar equals:");
        final AtomicBoolean compareOver = new AtomicBoolean(false);
        conn.query("desc " + tableName.getFullName((mrEngine.getEscapeChar())), new DataSourceMeta.ResultProcess() {

            int index = 0;

            @Override
            public boolean callback(ResultSet result) throws Exception {
                if (errMsg.length() > 0) {
                    return false;
                }
                final String keyName = result.getString(1);
                if (compareOver.get() || (StringUtils.isBlank(keyName) && compareOver.compareAndSet(false, true))) {
                    // 所有列都解析完成
                    return false;
                }
                if (IDumpTable.preservedPsCols.contains(keyName)) {
                    // 忽略pt和pmod
                    return true;
                }
                if (StringUtils.startsWith(keyName, "#")) {
                    return false;
                }
                if (index > (columns.size() - 1)) {
                    errMsg.append("create table " + tableName + " col:[" + keyName + "] is not exist");
                    return false;
                }
                HiveColumn column = columns.get(index++);
                if (column.getIndex() != (index - 1)) {
                    throw new IllegalStateException("col:" + column.getName() + " index shall be " + (index - 1) + " but is " + column.getIndex());
                }
                if (!StringUtils.equals(keyName, column.getName())) {
                    errMsg.append("target table keyName[" + index + "]:" + keyName + " is not equal with source table col:" + column.getName());
                    return false;
                } else {
                    equalsCols.append(keyName).append(",");
                }
                return true;
            }
        });
        if (errMsg.length() > 0) {
            isTableSame = false;
            logger.warn("create table has been modify,error:" + errMsg);
            logger.warn(equalsCols.toString());
        } else {
            // 没有改动，不过需要把元表清空
            isTableSame = true;
        }
        return isTableSame;
    }

    /**
     * 创建表分区
     *
     * @param
     * @throws Exception
     */
    void bindPartition(DataSourceMeta.JDBCConnection conn, BindHiveTableTool.HiveBindConfig columns, EntityName table, int startIndex) throws Exception {

        visitSubPmodPath(table, columns, startIndex, (pmod, path) -> {
            String sql = "alter table " + table + " add if not exists partition(" + IDumpTable.PARTITION_PT + "='"
                    + timestamp + "'," + IDumpTable.PARTITION_PMOD + "='" + pmod + "') location '" + path.toString() + "'";
            logger.info(sql);
            HiveDBUtils.executeNoLog(conn, sql);
            return true;
        });
    }

    private IPath visitSubPmodPath(EntityName table, BindHiveTableTool.HiveBindConfig colsMeta, int startIndex, FSPathVisitor pathVisitor) throws Exception {
        final String hivePath = table.getNameWithPath();
        ITISFileSystem fs = this.fileSystem;
        IPath path = null;

        while (true) {
            //path = fs.getPath(this.fileSystem.getRootDir() + "/" + hivePath + "/all/" + timestamp + "/" + (startIndex));
            path = fs.getPath(new HdfsPath(colsMeta.tabDumpParentPath), String.valueOf(startIndex));
            if (!fs.exists(path)) {
                if (startIndex < 1) {
                    // 说明一个分区都没有绑定到,可能绑定表的执行逻辑 跑到 dump逻辑前面去了
                    throw new IllegalStateException("path shall be exist:" + path);
                }
                return path;
            }
            if (!pathVisitor.process(startIndex, path)) { return path;}
            startIndex++;
        }
    }

    private interface FSPathVisitor {
        boolean process(int pmod, IPath path) throws Exception;
    }


    private void createHiveTableAndBindPartition(DataSourceMeta sourceMeta, DataSourceMeta.JDBCConnection conn, BindHiveTableTool.HiveBindConfig columns, EntityName tableName) throws Exception {
        createHiveTableAndBindPartition(sourceMeta, conn, tableName, columns.colsMeta, (hiveSQl) -> {
                    //final String hivePath = tableName.getNameWithPath();
                    int startIndex = 0;
                    //final StringBuffer tableLocation = new StringBuffer();
                    AtomicBoolean hasSubPmod = new AtomicBoolean(false);
                    IPath p = this.visitSubPmodPath(tableName, columns, startIndex, (pmod, path) -> {
                        // hiveSQl.append(" partition(pt='" + timestamp + "',pmod='" + pmod + "') location '" + path.toString() + "'");

                        hiveSQl.append(" location '" + path.toString() + "'");

                        hasSubPmod.set(true);
                        return false;
                    });

                    if (!hasSubPmod.get()) {
                        throw new IllegalStateException("dump table:" + tableName.getFullName() + " has any valid of relevant fs in timestamp path:" + p);
                    }
                }
        );
    }

    public void setFileSystem(ITISFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public interface SQLCommandTailAppend {
        public void append(StringBuffer hiveSQl) throws Exception;
    }
}
