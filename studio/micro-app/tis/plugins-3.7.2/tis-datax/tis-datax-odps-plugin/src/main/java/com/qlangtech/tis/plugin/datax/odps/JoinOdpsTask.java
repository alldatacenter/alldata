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

package com.qlangtech.tis.plugin.datax.odps;

import com.aliyun.odps.*;
import com.aliyun.odps.task.SQLTask;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.phasestatus.IJoinTaskStatus;
import com.qlangtech.tis.fullbuild.taskflow.HiveTask;
import com.qlangtech.tis.hive.AbstractInsertFromSelectParser;
import com.qlangtech.tis.hive.HiveColumn;
import com.qlangtech.tis.plugin.datax.CreateTableSqlBuilder;
import com.qlangtech.tis.plugin.datax.DataXOdpsWriter;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
import com.qlangtech.tis.sql.parser.ISqlTask;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-21 16:33
 **/
public class JoinOdpsTask extends HiveTask {

    private static final Logger logger = LoggerFactory.getLogger(JoinOdpsTask.class);
    private final DataXOdpsWriter odpsWriter;

    public JoinOdpsTask(DataXOdpsWriter odpsWriter, IDataSourceFactoryGetter dsFactoryGetter,
                        ISqlTask nodeMeta, boolean isFinalNode
            , Supplier<IPrimaryTabFinder> erRules, IJoinTaskStatus joinTaskStatus) {
        super(dsFactoryGetter, nodeMeta, isFinalNode, erRules, joinTaskStatus);
        this.odpsWriter = odpsWriter;
    }

    @Override
    protected void executeSql(String sql, DataSourceMeta.JDBCConnection conn) throws SQLException {

        OdpsDataSourceFactory dsFactory
                = (OdpsDataSourceFactory) dsFactoryGetter.getDataSourceFactory();
        Instance ist = null;
        // Instance instance = null;
        Instance.Status status = null;
        Instance.TaskStatus ts = null;
        try {
            Odps odps = dsFactory.createOdps();
            if (!StringUtils.endsWith(StringUtils.trim(sql), ";")) {
                sql = (sql + ";");
            }
            ist = SQLTask.run(odps, sql);

            // Instances instances = odps.instances();
            TaskStatusCollection taskInfo = new TaskStatusCollection();

            while (true) {
                //instance = instances.get(dsFactory.project, ist.getId());
                taskInfo.update(ist);
                status = ist.getStatus();

                if (status == Instance.Status.TERMINATED) {
                    if (taskInfo.isFaild()) {
                        this.joinTaskStatus.setFaild(true);
                    }
                    break;
                } else {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {

                    }
                }
            }

            logger.info("logViewUrl:{}", odps.logview().generateLogView(ist, 7 * 24));

        } catch (OdpsException e) {
            this.joinTaskStatus.setFaild(true);
            throw new SQLException(sql, e);
        } finally {
            this.joinTaskStatus.setComplete(true);
        }
    }

    private static class TaskStatusCollection {
        Map<String, TaskStatusInfo> taskInfo = new HashedMap();

        public void update(Instance instance) throws OdpsException {
            Instance.TaskStatus ts = null;
            TaskStatusInfo tskStatInfo = null;
            Map<String, Instance.TaskStatus> tStatus = instance.getTaskStatus();

            for (Map.Entry<String, Instance.TaskStatus> s : tStatus.entrySet()) {
                ts = s.getValue();
                if ((tskStatInfo = taskInfo.get(s.getKey())) != null) {
                    if (tskStatInfo.change(ts)) {
                        Instance.TaskSummary summary = null;
                        if ((summary = instance.getTaskSummary(s.getKey())) != null) {
                            logger.info(summary.getSummaryText());
                        }
                    }
                } else {
                    taskInfo.put(s.getKey(), new TaskStatusInfo(ts));
                }
            }
        }

        public boolean isFaild() {
            for (Map.Entry<String, TaskStatusInfo> status : taskInfo.entrySet()) {
                if (status.getValue().preState.get().getStatus() != Instance.TaskStatus.Status.SUCCESS) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class TaskStatusInfo {
        private final AtomicReference<Instance.TaskStatus> preState;

        public TaskStatusInfo(Instance.TaskStatus preState) {
            this.preState = new AtomicReference<>(preState);
        }

        /**
         * 状态有变化
         *
         * @param ts
         * @return
         */
        public boolean change(Instance.TaskStatus ts) {
            Instance.TaskStatus pre = preState.get();
            if (pre.getStatus() != ts.getStatus()) {
                preState.set(ts);
                return true;
            }
            return false;
        }
    }

    @Override
    protected List<String> getHistoryPts(
            DataSourceMeta mrEngine, DataSourceMeta.JDBCConnection conn, EntityName table) throws Exception {
        OdpsDataSourceFactory dsFactory = (OdpsDataSourceFactory) mrEngine;
        Table tab = dsFactory.getOdpsTable(table, Optional.empty());
        PartitionSpec ptSpec = null;
        Set<String> pts = Sets.newHashSet();
        for (Partition pt : tab.getPartitions()) {
            ptSpec = pt.getPartitionSpec();
            pts.add(ptSpec.get(IDumpTable.PARTITION_PT));
        }
        List<String> result = Lists.newArrayList(pts);
        Collections.sort(result);
        return result;

    }

    @Override
    protected void initializeTable(DataSourceMeta ds, ColsParser insertParser
            , DataSourceMeta.JDBCConnection conn, EntityName dumpTable, Integer partitionRetainNum) throws Exception {

        OdpsDataSourceFactory dsFactory = (OdpsDataSourceFactory) ds;
        initializeTable(ds, conn, dumpTable,
                new IHistoryTableProcessor() {
                    @Override
                    public void cleanHistoryTable() throws IOException {
                        // ODPS有lifecycle控制，不需要主动地去删除历史表
                    }
                } //
                , () -> {
                    // 判断和已经在ODPS中创建的表是否一致，如不一致需要删除重新创建
                    try {
                        return isTableSame(dsFactory, conn, insertParser, dumpTable);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, () -> {
                    insertParser.reflectColsType();
                    createTable(dsFactory, dumpTable, insertParser, conn);
                });

    }

    /**
     * 创建 ODPS表
     *
     * @param dsFactory
     * @param dumpTable
     * @param insertParser
     * @param conn
     */
    private void createTable(OdpsDataSourceFactory dsFactory, EntityName dumpTable
            , ColsParser insertParser, DataSourceMeta.JDBCConnection conn) {
        // ISqlTask.RewriteSql rewriteSql = insertParser.getSql();
//        String sql = rewriteSql.rewriteSql;
//        try {
//            conn.execute("CREATE TABLE IF NOT EXISTS " + dumpTable.getFullName((dsFactory.getEscapeChar()))
//                    + " lifecycle " + odpsWriter.lifecycle + " AS \n" + sql);
//            // 添加分区
//            Table ntab = dsFactory.getOdpsTable(dumpTable, dsFactory.getEscapeChar());
//            PartitionSpec spec = new PartitionSpec();
//            spec.set(IDumpTable.PARTITION_PT, rewriteSql.primaryTable.getPt());
//            spec.set(IDumpTable.PARTITION_PMOD, "0");
//            ntab.createPartition(spec, true);
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

        List<HiveColumn> colsExcludePartitionCols = insertParser.getColsExcludePartitionCols();

        List<IColMetaGetter> cols = colsExcludePartitionCols.stream()
                .map((c) -> IColMetaGetter.create(c.getName(), c.dataType)).collect(Collectors.toList());

        IDataxProcessor.TableMap tabMapper
                = IDataxProcessor.TableMap.create(dumpTable.getTabName(), cols);

        CreateTableSqlBuilder.CreateDDL createDDL = odpsWriter.generateCreateDDL(tabMapper);
        StringBuffer ddlScript = createDDL.getDDLScript();
        try {
            conn.execute(ddlScript.toString());
        } catch (Exception e) {
            throw new RuntimeException(ddlScript.toString(), e);
        }
    }

    private boolean isTableSame(OdpsDataSourceFactory dsFactory, DataSourceMeta.JDBCConnection conn
            , ColsParser allCols, EntityName dumpTable) {
        TableSchema tabSchema = dsFactory.getTableSchema(dumpTable);
        for (HiveColumn col : allCols.getColsExcludePartitionCols()) {
            if (!tabSchema.containsColumn(col.getName())) {
                logger.info("col:" + col.getName() + " is not exist in created table:" + dumpTable.getFullName()
                        + ",tableschema:" + tabSchema.getColumns().stream().map((c) -> c.getName()).collect(Collectors.joining(",")));
                return false;
            }
        }
        return true;
    }

    @Override
    protected AbstractInsertFromSelectParser createInsertSQLParser(String sql, Function<ISqlTask.RewriteSql, List<ColumnMetaData>> sqlColMetaGetter) {
        return new OdpsInsertFromSelectParser(sql, sqlColMetaGetter);
    }


}
