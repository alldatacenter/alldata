/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.fullbuild.taskflow.hive;
//
import com.qlangtech.tis.dump.hive.BindHiveTableTool;
import com.qlangtech.tis.dump.hive.HiveDBUtils;
import com.qlangtech.tis.dump.hive.HiveRemoveHistoryDataTask;
import com.qlangtech.tis.fs.FSHistoryFileUtils;
import com.qlangtech.tis.fs.IFs2Table;
import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fullbuild.phasestatus.IJoinTaskStatus;
import com.qlangtech.tis.hive.HdfsFormat;
import com.qlangtech.tis.hive.HiveColumn;
import com.qlangtech.tis.hive.HiveInsertFromSelectParser;
import com.qlangtech.tis.order.dump.task.ITableDumpConstant;
import com.qlangtech.tis.plugin.datax.MREngine;
import com.qlangtech.tis.sql.parser.ISqlTask;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.parse.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2015年12月22日 下午7:14:24
// */
public class JoinHiveTask  //extends HiveTask
{
//
//    private static final MessageFormat SQL_INSERT_TABLE = new MessageFormat("INSERT OVERWRITE TABLE {0} PARTITION (pt,pmod) \n {1}");
//
    private static final Logger log = LoggerFactory.getLogger(JoinHiveTask.class);
//
//    private final ITISFileSystem fileSystem;
//
//    private final IFs2Table fs2Table;
//    private final MREngine mrEngine;
//
//    public JoinHiveTask(ISqlTask nodeMeta, boolean isFinalNode, IPrimaryTabFinder erRules, IJoinTaskStatus joinTaskStatus
//            , ITISFileSystem fileSystem, IFs2Table fs2Table, MREngine mrEngine) {
//        super(nodeMeta, isFinalNode, erRules, joinTaskStatus);
//        this.fileSystem = fileSystem;
//        Objects.nonNull(fs2Table);
//        this.fs2Table = fs2Table;
//        this.mrEngine = mrEngine;
//    }
//
//
//    @Override
//    protected void executeSql(String taskName, String rewritedSql) {
//        // 处理历史表，多余的partition要删除，表不同了需要删除重建
//        processJoinTask(rewritedSql);
//        final EntityName newCreateTab = EntityName.parse(this.nodeMeta.getExportName());
//        final String insertSql = SQL_INSERT_TABLE.format(new Object[]{newCreateTab.getFullName(), rewritedSql});
//        super.executeSql(taskName, insertSql);
//    }
//
//    /**
//     * 处理join表，是否需要自动创建表或者删除重新创建表
//     *
//     * @param sql
//     */
//    private void processJoinTask(String sql) {
//        try {
//            final HiveInsertFromSelectParser insertParser = getSQLParserResult(sql);
//            final Connection conn = this.getTaskContext().getObj();
//            // final DumpTable dumpTable =
//            // DumpTable.createTable(insertParser.getTargetTableName());
//            final EntityName dumpTable = EntityName.parse(this.getName());
//
//            final String path = FSHistoryFileUtils.getJoinTableStorePath(fileSystem.getRootDir(), dumpTable).replaceAll("\\.", Path.SEPARATOR);
//            if (fileSystem == null) {
//                throw new IllegalStateException("fileSys can not be null");
//            }
//            ITISFileSystem fs = fileSystem;
//            IPath parent = fs.getPath(path);
//            initializeHiveTable(this.fileSystem, parent, mrEngine, HdfsFormat.DEFAULT_FORMAT, insertParser.getCols()
//                    , insertParser.getColsExcludePartitionCols(), conn, dumpTable, ITableDumpConstant.MAX_PARTITION_SAVE);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
    /**
     * @param fileSystem
     * @param fsFormat
     * @param cols
     * @param colsExcludePartitionCols
     * @param conn
     * @param dumpTable
     * @param partitionRetainNum       保留多少个分区
     * @throws Exception
     */
    public static void initializeHiveTable(ITISFileSystem fileSystem, IPath parentPath, MREngine mrEngine, HdfsFormat fsFormat
            , List<HiveColumn> cols, List<HiveColumn> colsExcludePartitionCols
            , Connection conn, EntityName dumpTable, Integer partitionRetainNum) throws Exception {
        if (partitionRetainNum == null || partitionRetainNum < 1) {
            throw new IllegalArgumentException("illegal param partitionRetainNum ");
        }
        if (BindHiveTableTool.HiveTableBuilder.isTableExists(mrEngine, conn, dumpTable)) {
            if (BindHiveTableTool.HiveTableBuilder.isTableSame(conn, cols, dumpTable)) {
                log.info("Start clean up history file '{}'", dumpTable);

                // 表结构没有变化，需要清理表中的历史数据 清理历史hdfs数据
                //this.fs2Table.deleteHistoryFile(dumpTable, this.getTaskContext());
                // 清理hive数据
                List<FSHistoryFileUtils.PathInfo> deletePts =
                        (new HiveRemoveHistoryDataTask(fileSystem,mrEngine)).dropHistoryHiveTable(dumpTable, conn, partitionRetainNum);
                // 清理Hdfs数据
                FSHistoryFileUtils.deleteOldHdfsfile(fileSystem, parentPath, deletePts, 0);
                //  RemoveJoinHistoryDataTask.deleteHistoryJoinTable(dumpTable, fileSystem, partitionRetainNum);
            } else {
                HiveDBUtils.execute(conn, "drop table " + dumpTable);
                createHiveTable(fileSystem, fsFormat, dumpTable, colsExcludePartitionCols, conn);
            }
        } else {
            // 说明原表并不存在 直接创建
            log.info("table " + dumpTable + " doesn't exist");
            log.info("create table " + dumpTable);
            createHiveTable(fileSystem, fsFormat, dumpTable, colsExcludePartitionCols, conn);
        }
    }
//
//    public HiveInsertFromSelectParser getSQLParserResult() throws Exception {
//        return this.getSQLParserResult(mergeVelocityTemplate(Collections.emptyMap()));
//    }
//
//    private HiveInsertFromSelectParser getSQLParserResult(String sql) throws ParseException {
//        final HiveInsertFromSelectParser insertParser = new HiveInsertFromSelectParser();
//        insertParser.start(sql);
//        return insertParser;
//    }
//
//
//    /**
//     * 创建hive表
//     */
    public static void createHiveTable(ITISFileSystem fileSystem, HdfsFormat fsFormat, EntityName dumpTable, List<HiveColumn> cols, Connection conn) throws Exception {
        // final String user = this.getContext().joinTaskContext().getContextUserName();
        BindHiveTableTool.HiveTableBuilder tableBuilder = new BindHiveTableTool.HiveTableBuilder("0", fsFormat);
        tableBuilder.createHiveTableAndBindPartition(conn, dumpTable, cols, (hiveSQl) -> {
            hiveSQl.append("\n LOCATION '").append(
                    FSHistoryFileUtils.getJoinTableStorePath(fileSystem.getRootDir(), dumpTable)
                            .replaceAll("\\.", "/")).append("'");
        });
    }
//    // 索引数据: /user/admin/search4totalpay/all/0/output/20160104003306
//    // dump数据: /user/admin/search4totalpay/all/0/20160105003307
}
