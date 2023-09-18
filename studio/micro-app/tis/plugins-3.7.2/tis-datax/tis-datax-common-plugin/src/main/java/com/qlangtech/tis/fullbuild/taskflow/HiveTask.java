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
package com.qlangtech.tis.fullbuild.taskflow;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.phasestatus.IJoinTaskStatus;
import com.qlangtech.tis.fullbuild.taskflow.hive.AbstractResultSet;
import com.qlangtech.tis.hive.AbstractInsertFromSelectParser;
import com.qlangtech.tis.hive.HiveColumn;
import com.qlangtech.tis.order.dump.task.ITableDumpConstant;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.sql.parser.IAliasTable;
import com.qlangtech.tis.sql.parser.ISqlTask;
import com.qlangtech.tis.sql.parser.TabPartitions;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年11月1日 下午10:58:29
 */
public abstract class HiveTask extends AdapterTask {

    private static final Logger logger = LoggerFactory.getLogger(HiveTask.class);


    protected final IJoinTaskStatus joinTaskStatus;

    protected final ISqlTask nodeMeta;
    protected final boolean isFinalNode;
    protected final IDataSourceFactoryGetter dsFactoryGetter;

    private final Supplier<IPrimaryTabFinder> erRules;

    /**
     * @param joinTaskStatus
     */
    protected HiveTask(IDataSourceFactoryGetter dsFactoryGetter, ISqlTask nodeMeta, boolean isFinalNode
            , Supplier<IPrimaryTabFinder> erRules, IJoinTaskStatus joinTaskStatus) {
        super(nodeMeta.getId());
        if (joinTaskStatus == null) {
            throw new IllegalStateException("param joinTaskStatus can not be null");
        }
        Objects.requireNonNull(erRules, "param erRule can not be null");
        this.erRules = erRules;
        this.joinTaskStatus = joinTaskStatus;
        this.nodeMeta = nodeMeta;
        this.isFinalNode = isFinalNode;
        this.dsFactoryGetter = dsFactoryGetter;
    }

    /**
     * 判断表是否存在
     *
     * @param connection
     * @param dumpTable
     * @return
     * @throws Exception
     */
    public static boolean isTableExists(DataSourceMeta ds
            , DataSourceMeta.JDBCConnection connection, EntityName dumpTable) throws Exception {
        // 判断表是否存在
//        if (!isDBExists(mrEngine, connection, dumpTable.getDbName())) {
//            // DB都不存在，table肯定就不存在啦
//            logger.error("dumpTable'DB is not exist:{}", dumpTable);
//            // return false;
//            throw new IllegalStateException("database:" + dumpTable.getDbName() + " is not exist in " + connection.getUrl());
//        }

        try {
            ds.getTableMetadata(connection, true, dumpTable);
            return true;
        } catch (TableNotFoundException e) {
            return false;
        }

        //  final List<String> tables = mrEngine.getTablesInDB().getTabs();// mrEngine.getTabs(connection, dumpTable);// new ArrayList<>();
        // SPARK:
        //        +-----------+---------------+--------------+--+
        //        | database  |   tableName   | isTemporary  |
        //        +-----------+---------------+--------------+--+
        //        | order     | totalpayinfo  | false        |
        //        +-----------+---------------+--------------+--+
        // Hive
//            HiveDBUtils.query(connection, "show tables in " + dumpTable.getDbName()
//                    , result -> tables.add(result.getString(2)));
//            boolean contain = tables.contains(dumpTable.getTableName());
//            if (!contain) {
//                logger.debug("table:{} is not exist in[{}]", dumpTable.getTableName()
//                        , tables.stream().collect(Collectors.joining(",")));
//            }
//            return contain;
    }

//    public static boolean isDBExists(MREngine mrEngine, DataSourceMeta.JDBCConnection connection, String dbName) throws Exception {
//        AtomicBoolean dbExist = new AtomicBoolean(false);
//        connection.query(mrEngine.showDBSQL, result -> {
//            if (StringUtils.equals(result.getString(1), dbName)) {
//                dbExist.set(true);
//            }
//            return true;
//        });
//        return dbExist.get();
//    }

    @Override
    public final String getName() {
        return this.nodeMeta.getExportName();
    }

    @Override
    public FullbuildPhase phase() {
        return FullbuildPhase.JOIN;
    }

    @Override
    public String getIdentityName() {
        return nodeMeta.getExportName();
    }

    private ISqlTask.RewriteSql rewriteSql;


    protected ISqlTask.RewriteSql getRewriteSql() {
        if (this.rewriteSql == null) {
            this.rewriteSql = nodeMeta.getRewriteSql(this.getName(), this.getDumpPartition(), this.erRules, this.getExecContext(), this.isFinalNode);
        }
        return this.rewriteSql;
    }

    protected interface ValSupplier {
        Object get() throws SQLException;
    }


    private static class DependencyNodeStatus {

        final DependencyNode taskNode;

        final Boolean dependencyWorkStatus;

        public DependencyNodeStatus(DependencyNode taskNode, Boolean dependencyWorkStatus) {
            super();
            this.taskNode = taskNode;
            this.dependencyWorkStatus = dependencyWorkStatus;
        }
    }

    @Override
    protected final void executeTask(String taskname) {
        ISqlTask.RewriteSql rewriteSql = this.getRewriteSql();

        // 处理历史表，多余的partition要删除，表不同了需要删除重建
        boolean dryRun = this.getExecContext().isDryRun();
        TabPartitions dumpPartition = this.getDumpPartition();
        final EntityName newCreateTab = EntityName.parse(this.nodeMeta.getExportName());
        processJoinTask(rewriteSql);
        if (dryRun) {
            logger.info("task:{}, as DryRun mode skip remaining tasks", taskname);
            dumpPartition.putPt(newCreateTab, rewriteSql.primaryTable);
            joinTaskStatus.setComplete(true);
            return;
        }

        String insertSql = rewriteSql.convert2InsertIntoSQL(this.dsFactoryGetter.getDataSourceFactory(), this.nodeMeta.getExportName());

        this.validateDependenciesNode(taskname);
        final DataSourceMeta.JDBCConnection conn = this.getTaskContextObj();
        DataSourceFactory dsFactory = dsFactoryGetter.getDataSourceFactory();

        //final String newCreatePt = primaryTable.getTabPartition();
        this.getRewriteSql();
        List<String> allpts = null;
        try {
            logger.info("\n execute hive task:{} \n{}", taskname, insertSql);
            executeSql(insertSql, conn);
            // 将当前的join task的partition设置到当前上下文中

            dumpPartition.putPt(newCreateTab, rewriteSql.primaryTable);
            allpts = getHistoryPts(dsFactory, conn, newCreateTab);
        } catch (Exception e) {
            // TODO 一旦有异常要将整个链路执行都停下来
            throw new RuntimeException("taskname:" + taskname, e);
        }
        IAliasTable child = null;
        // 校验最新的Partition 是否已经生成
        if (!allpts.contains(this.rewriteSql.primaryTable.getPt())) {
            StringBuffer errInfo = new StringBuffer();
            errInfo.append("\ntable:" + newCreateTab + "," + IDumpTable.PARTITION_PT + ":" + this.rewriteSql.primaryTable
                    + " is not exist in exist partition set [" + Joiner.on(",").join(allpts) + "]");
            child = this.rewriteSql.primaryTable.getChild();
            if (child != null && !child.isSubQueryTable()) {
                try {
                    allpts = getHistoryPts(dsFactory, conn, (EntityName) child.getTable());
                } catch (Exception e) {
                    throw new RuntimeException(child.getTable().getFullName(), e);
                }
                errInfo.append("\n\t child table:").append(child.getTable()).append(",").append(IDumpTable.PARTITION_PT)
                        .append(":").append(this.rewriteSql.primaryTable)
                        .append(" is not exist in exist partition set [").append(Joiner.on(",").join(allpts)).append("]");
            }
            throw new IllegalStateException(errInfo.toString());
        }
    }

    /**
     * 处理join表，是否需要自动创建表或者删除重新创建表
     *
     * @param sql
     */
    private void processJoinTask(ISqlTask.RewriteSql sql) {
        try {
            final DataSourceMeta.JDBCConnection conn = this.getTaskContextObj();
            final ColsParser insertParser = new ColsParser(sql, conn);

            DataSourceFactory dsFactory = this.dsFactoryGetter.getDataSourceFactory();

            final EntityName dumpTable = EntityName.create(dsFactory.getDbConfig().getName(), this.getName());

            initializeTable(this.dsFactoryGetter.getDataSourceFactory(), insertParser
                    , conn, dumpTable, ITableDumpConstant.MAX_PARTITION_SAVE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param insertParser
     * @param conn
     * @param dumpTable
     * @param partitionRetainNum 保留多少个分区
     * @throws Exception
     */
    protected abstract void initializeTable(DataSourceMeta mrEngine
            , ColsParser insertParser
            , DataSourceMeta.JDBCConnection conn, EntityName dumpTable, Integer partitionRetainNum) throws Exception;

    protected abstract void executeSql(String sql, DataSourceMeta.JDBCConnection conn) throws SQLException;

    protected abstract List<String> getHistoryPts(DataSourceMeta mrEngine, DataSourceMeta.JDBCConnection conn, final EntityName table) throws Exception;

    protected void validateDependenciesNode(String taskname) {
        Boolean dependencyWorkStatus = null;
        final List<DependencyNodeStatus> lackDependencies = Lists.newArrayList();
        for (DependencyNode depenency : this.nodeMeta.getDependencies()) {
            dependencyWorkStatus = this.getTaskWorkStatus().get(depenency.getId());
            if (dependencyWorkStatus == null || !dependencyWorkStatus) {
                lackDependencies.add(new DependencyNodeStatus(depenency, dependencyWorkStatus));
            }
        }
        if (!lackDependencies.isEmpty()) {
            // 说明有依赖到的node没有被执行
            throw new IllegalStateException("taskname:" + taskname + " lackDependencies:"
                    + lackDependencies.stream().map((r) -> "(" + r.taskNode.getId() + "," + r.taskNode.parseEntityName()
                    + ",status:" + (r.dependencyWorkStatus == null ? "notExecute" : r.dependencyWorkStatus) + ")")
                    .collect(Collectors.joining())
                    + "/n TaskWorkStatus:"
                    + this.getTaskWorkStatus().entrySet().stream()
                    .map((e) -> "[" + e.getKey() + "->" + e.getValue() + "]")
                    .collect(Collectors.joining(",")));
        }
    }

    protected static class ResultSetMetaDataDelegate extends AbstractResultSet {
        //private final ResultSetMetaData metaData;

        private int columnCursor;
        private final int colCount;

        private final Map<String, ValSupplier> valueSupplier;


        public ResultSetMetaDataDelegate(ResultSetMetaData metaData) throws SQLException {
            this.colCount = metaData.getColumnCount();
            ImmutableMap.Builder<String, ValSupplier> mapBuilder = ImmutableMap.builder();
            mapBuilder.put(DataSourceFactory.KEY_COLUMN_NAME, () -> {
                return metaData.getColumnName(this.columnCursor);
            });
            mapBuilder.put(DataSourceFactory.KEY_REMARKS, () -> {
                return StringUtils.EMPTY;
            });
            mapBuilder.put(DataSourceFactory.KEY_NULLABLE, () -> {
                return true;
            });
            mapBuilder.put(DataSourceFactory.KEY_DECIMAL_DIGITS, () -> {
                return metaData.getScale(this.columnCursor);
            });
            mapBuilder.put(DataSourceFactory.KEY_TYPE_NAME, () -> {
                return metaData.getColumnTypeName(this.columnCursor);
            });
            mapBuilder.put(DataSourceFactory.KEY_DATA_TYPE, () -> {
                return metaData.getColumnType(this.columnCursor);
            });
            mapBuilder.put(DataSourceFactory.KEY_COLUMN_SIZE, () -> {
                return metaData.getPrecision(this.columnCursor);
            });
            valueSupplier = mapBuilder.build();
        }

        @Override
        public void close() throws SQLException {

        }

        @Override
        public boolean next() throws SQLException {
            return (++this.columnCursor) <= colCount;
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            return (Integer) getValue(columnLabel);
        }

        @Override
        public String getString(String columnLabel) throws SQLException {
            return (String) getValue(columnLabel);
        }

        @Override
        public boolean getBoolean(String columnLabel) throws SQLException {
            return (Boolean) getValue(columnLabel);
        }

        private Object getValue(String columnLabel) throws SQLException {
            ValSupplier valSupplier = valueSupplier.get(columnLabel);
            Objects.requireNonNull(valSupplier, "label:" + columnLabel + " relevant supplier must be present");
            return valSupplier.get();
        }
    }

    protected class ColsParser {
        private final ISqlTask.RewriteSql sql;
        private final DataSourceMeta.JDBCConnection conn;
        private AbstractInsertFromSelectParser sqlParser;

        public ColsParser(ISqlTask.RewriteSql sql, DataSourceMeta.JDBCConnection conn) {
            this.sql = sql;
            this.conn = conn;
        }

        public ISqlTask.RewriteSql getSql() {
            return this.sql;
        }

        private AbstractInsertFromSelectParser getInserSqlParser() {
            if (sqlParser == null) {
                sqlParser = getSQLParserResult(sql.originSql, conn);
            }
            return sqlParser;
        }

        public List<HiveColumn> getAllCols() {
            return getInserSqlParser().getCols();
        }

        public void reflectColsType() {
            getInserSqlParser().reflectColsType();
        }

        /**
         * 除去ps列
         */
        public List<HiveColumn> getColsExcludePartitionCols() {
            return getInserSqlParser().getColsExcludePartitionCols();
        }
    }

    private AbstractInsertFromSelectParser getSQLParserResult(String sql, DataSourceMeta.JDBCConnection conn) {
        DataSourceFactory dsFactory = this.dsFactoryGetter.getDataSourceFactory();
        Function<ISqlTask.RewriteSql, List<ColumnMetaData>> sqlColMetaGetter = (rewriteSql) -> {
            List<ColMeta> cols = rewriteSql.getCols();
            try (Statement statement = conn.createStatement()) {
                try (ResultSet result = statement.executeQuery(rewriteSql.rewriteSql)) {
                    try (ResultSet metaData = convert2ResultSet(result.getMetaData())) {
                        // 取得结果集数据列类型
                        List<ColumnMetaData> columnMetas
                                = dsFactory.wrapColsMeta(true, metaData
                                , new DataSourceFactory.CreateColumnMeta(Collections.emptySet(), metaData) {
                                    @Override
                                    public ColumnMetaData create(String colName, int index) throws SQLException {
                                        ColMeta colMeta = cols.get(index);
                                        if (StringUtils.indexOf(colName, colMeta.getName()) < 0) {
                                            throw new IllegalStateException("colMeta.getName:"
                                                    + colMeta.getName() + " is not contain in colName:" + colName);
                                        }
                                        return super.create(colMeta.getName(), index);
                                    }
                                });


                        return columnMetas;
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        };
        return createInsertSQLParser(sql, sqlColMetaGetter);
    }

//    private AbstractInsertFromSelectParser getSQLParserResult(
//            String sql, DataSourceFactory dsFactory, DataSourceMeta.JDBCConnection conn, AbstractInsertFromSelectParser insertParser) {
//
//        TabPartitions tabPartition = new TabPartitions(Collections.emptyMap()) {
//            @Override
//            protected Optional<DumpTabPartition> findTablePartition(boolean dbNameCriteria, String dbName, String tableName) {
//                return Optional.of(new DumpTabPartition((dbNameCriteria ? EntityName.create(dbName, tableName) : EntityName.parse(tableName)), () -> "-1"));
//            }
//        };
//        insertParser.start(sql, tabPartition, );
//        return insertParser;
//    }


    private ResultSet convert2ResultSet(ResultSetMetaData metaData) throws SQLException {
        return new ResultSetMetaDataDelegate(metaData);
    }

    protected abstract AbstractInsertFromSelectParser createInsertSQLParser(String sql, Function<ISqlTask.RewriteSql, List<ColumnMetaData>> sqlColMetaGetter);


    public interface IHistoryTableProcessor {
        public void cleanHistoryTable() throws IOException;
    }

    /**
     * @param
     * @param
     * @param conn
     * @param dumpTable
     * @throws Exception
     */
    public static void initializeTable(DataSourceMeta ds
            , DataSourceMeta.JDBCConnection conn, EntityName dumpTable, IHistoryTableProcessor historyTableProcessor
            , Supplier<Boolean> tableSameJudgement, Runnable tableCreator) throws Exception {
//        if (partitionRetainNum == null || partitionRetainNum < 1) {
//            throw new IllegalArgumentException("illegal param partitionRetainNum ");
//        }
        if (isTableExists(ds, conn, dumpTable)) {
            if (tableSameJudgement.get()) {
                logger.info("Start clean up history file '{}'", dumpTable);
                // cleanHistoryTable(fileSystem, parentPath, mrEngine, conn, dumpTable, partitionRetainNum);

                historyTableProcessor.cleanHistoryTable();

                //  RemoveJoinHistoryDataTask.deleteHistoryJoinTable(dumpTable, fileSystem, partitionRetainNum);
            } else {
                conn.execute("drop table " + dumpTable);
                tableCreator.run();
                // createHiveTable(fileSystem, fsFormat, dumpTable, colsExcludePartitionCols, conn);
            }
        } else {
            // 说明原表并不存在 直接创建
            logger.info("table " + dumpTable + " doesn't exist");
            logger.info("create table " + dumpTable);
            tableCreator.run();
            //createHiveTable(fileSystem, fsFormat, dumpTable, colsExcludePartitionCols, conn);
        }
    }
}
