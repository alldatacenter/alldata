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
package com.qlangtech.tis.fullbuild.taskflow.hive;
//

import com.qlangtech.tis.dump.hive.HiveDBUtils;
import com.qlangtech.tis.dump.hive.HiveRemoveHistoryDataTask;
import com.qlangtech.tis.dump.hive.HiveTableBuilder;
import com.qlangtech.tis.fs.FSHistoryFileUtils;
import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fullbuild.phasestatus.IJoinTaskStatus;
import com.qlangtech.tis.fullbuild.taskflow.HiveTask;
import com.qlangtech.tis.hive.AbstractInsertFromSelectParser;
import com.qlangtech.tis.hive.HdfsFormat;
import com.qlangtech.tis.hive.HiveColumn;
import com.qlangtech.tis.hive.HiveInsertFromSelectParser;
import com.qlangtech.tis.plugin.datax.MREngine;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
import com.qlangtech.tis.sql.parser.ISqlTask;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年12月22日 下午7:14:24
 */
public class JoinHiveTask extends HiveTask {
    //

    //
    private static final Logger log = LoggerFactory.getLogger(JoinHiveTask.class);

    private final ITISFileSystem fileSystem;

    // private final IFs2Table fs2Table;
    private final MREngine mrEngine;

    public JoinHiveTask(ISqlTask nodeMeta, boolean isFinalNode, Supplier<IPrimaryTabFinder> erRules, IJoinTaskStatus joinTaskStatus
            , ITISFileSystem fileSystem, MREngine mrEngine, IDataSourceFactoryGetter dsFactoryGetter) {
        super(dsFactoryGetter, nodeMeta, isFinalNode, erRules, joinTaskStatus);
        this.fileSystem = fileSystem;
        this.mrEngine = mrEngine;
    }


    @Override
    protected void executeSql(String sql, DataSourceMeta.JDBCConnection conn) throws SQLException {
        HiveDBUtils.execute(conn, sql, joinTaskStatus);
    }

    @Override
    protected List<String> getHistoryPts(
            DataSourceMeta mrEngine, DataSourceMeta.JDBCConnection conn, EntityName table) throws Exception {
        return HiveRemoveHistoryDataTask.getHistoryPts(mrEngine, conn, table);
    }


    /**
     * @param insertParser
     * @param conn
     * @param dumpTable
     * @param partitionRetainNum 保留多少个分区
     * @throws Exception
     */
    @Override
    protected void initializeTable(DataSourceMeta ds
            , ColsParser insertParser
            , DataSourceMeta.JDBCConnection conn, EntityName dumpTable, Integer partitionRetainNum) throws Exception {

        final String path = FSHistoryFileUtils.getJoinTableStorePath(fileSystem.getRootDir(), dumpTable);
        if (fileSystem == null) {
            throw new IllegalStateException("fileSys can not be null");
        }
        ITISFileSystem fs = fileSystem;
        IPath parentPath = fs.getPath(path);
        HdfsFormat fsFormat = HdfsFormat.DEFAULT_FORMAT;

        initializeTable(ds, conn, dumpTable,
                new IHistoryTableProcessor() {
                    @Override
                    public void cleanHistoryTable() throws IOException {
                        JoinHiveTask.cleanHistoryTable(fs, parentPath, ds, conn, dumpTable, partitionRetainNum);
                    }
                } //
                , () -> {
                    try {
                        return HiveTableBuilder.isTableSame(ds, conn, insertParser.getAllCols(), dumpTable);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, () -> {
                    insertParser.reflectColsType();
                    createHiveTable(fileSystem, fsFormat, ds, dumpTable, insertParser.getColsExcludePartitionCols(), conn);
                });
    }


    public static void cleanHistoryTable(ITISFileSystem fileSystem, IPath parentPath
            , DataSourceMeta mrEngine, DataSourceMeta.JDBCConnection conn, EntityName dumpTable, Integer partitionRetainNum) throws IOException {
        // 表结构没有变化，需要清理表中的历史数据 清理历史hdfs数据
        //this.fs2Table.deleteHistoryFile(dumpTable, this.getTaskContext());
        // 清理hive数据
        List<FSHistoryFileUtils.PathInfo> deletePts =
                (new HiveRemoveHistoryDataTask(MREngine.HIVE, fileSystem, mrEngine)).dropHistoryHiveTable(dumpTable, conn, partitionRetainNum);
        // 清理Hdfs数据
        FSHistoryFileUtils.deleteOldHdfsfile(fileSystem, parentPath, deletePts, partitionRetainNum);
    }


    @Override
    protected AbstractInsertFromSelectParser createInsertSQLParser(String sql, Function<ISqlTask.RewriteSql, List<ColumnMetaData>> sqlColMetaGetter) {
        return new HiveInsertFromSelectParser(sql, sqlColMetaGetter);
    }

    /**
     * 创建hive表
     */
    public static void createHiveTable(ITISFileSystem fileSystem, HdfsFormat fsFormat
            , DataSourceMeta sourceMeta, EntityName dumpTable, List<HiveColumn> cols, DataSourceMeta.JDBCConnection conn) {
        try {
            HiveTableBuilder tableBuilder = new HiveTableBuilder("0", fsFormat);
            tableBuilder.createHiveTableAndBindPartition(sourceMeta, conn, dumpTable, cols, (hiveSQl) -> {
                hiveSQl.append("\n LOCATION '").append(
                        FSHistoryFileUtils.getJoinTableStorePath(fileSystem.getRootDir(), dumpTable)
                ).append("'");
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
//    // 索引数据: /user/admin/search4totalpay/all/0/output/20160104003306
//    // dump数据: /user/admin/search4totalpay/all/0/20160105003307
}
