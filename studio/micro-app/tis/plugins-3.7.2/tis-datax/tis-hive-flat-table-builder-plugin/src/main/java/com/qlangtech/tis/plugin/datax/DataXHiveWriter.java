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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.config.hive.IHiveConnGetter;
import com.qlangtech.tis.datax.IDataXBatchPost;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.dump.hive.BindHiveTableTool;
import com.qlangtech.tis.dump.hive.HiveDBUtils;
import com.qlangtech.tis.exec.ExecChainContextUtils;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.ExecuteResult;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.fs.*;
import com.qlangtech.tis.fullbuild.indexbuild.DftTabPartition;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.fullbuild.phasestatus.IJoinTaskStatus;
import com.qlangtech.tis.fullbuild.taskflow.DataflowTask;
import com.qlangtech.tis.fullbuild.taskflow.IFlatTableBuilder;
import com.qlangtech.tis.fullbuild.taskflow.IFlatTableBuilderDescriptor;
import com.qlangtech.tis.fullbuild.taskflow.hive.JoinHiveTask;
import com.qlangtech.tis.hdfs.impl.HdfsPath;
import com.qlangtech.tis.hive.HdfsFormat;
import com.qlangtech.tis.hive.HiveColumn;
import com.qlangtech.tis.hive.Hiveserver2DataSourceFactory;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.ISqlTask;
import com.qlangtech.tis.sql.parser.TabPartitions;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-23 14:48
 * @see com.qlangtech.tis.plugin.datax.TisDataXHiveWriter
 **/
@Public
public class DataXHiveWriter extends BasicFSWriter implements IFlatTableBuilder, IDataSourceFactoryGetter, IDataXBatchPost {
    private static final String DATAX_NAME = "Hive";

    @FormField(identity = false, ordinal = 0, type = FormFieldType.ENUM, validate = {Validator.require})
    public String dbName;


    //    @FormField(ordinal = 1, type = FormFieldType.SELECTABLE, validate = {Validator.require})
//    public String hiveConn;
    @FormField(ordinal = 6, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
    public Integer partitionRetainNum;

    @FormField(ordinal = 7, validate = {Validator.require})
    public TabNameDecorator tabDecorator;

    @FormField(ordinal = 8, type = FormFieldType.ENUM, validate = {Validator.require})
    public String partitionFormat;

    @FormField(ordinal = 15, type = FormFieldType.TEXTAREA, advance = false, validate = {Validator.require})
    public String template;

    @Override
    public String getTemplate() {
        return this.template;
    }

    public MREngine getEngineType() {
        return MREngine.HIVE;
    }

    @Override
    protected FSDataXContext getDataXContext(IDataxProcessor.TableMap tableMap) {
        return new HiveDataXContext("tishivewriter", tableMap, this.dataXName);
    }


    /**
     * ========================================================
     * implements: IFlatTableBuilder
     *
     * @see IFlatTableBuilder
     */
    @Override
    public ExecuteResult startTask(ITableBuildTask dumpTask) {

        try {
            try (DataSourceMeta.JDBCConnection conn = getConnection()) {
                HiveDBUtils.executeNoLog(conn, "SET hive.exec.dynamic.partition = true");
                HiveDBUtils.executeNoLog(conn, "SET hive.exec.dynamic.partition.mode = nonstrict");
                return dumpTask.process(new ITaskContext() {
                    @Override
                    public DataSourceMeta.JDBCConnection getObj() {
                        return conn;
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public DataflowTask createTask(ISqlTask nodeMeta, boolean isFinalNode, IExecChainContext execChainContext, ITaskContext tskContext
            , IJoinTaskStatus joinTaskStatus
            , IDataSourceFactoryGetter dsGetter, Supplier<IPrimaryTabFinder> primaryTabFinder) {
        JoinHiveTask joinHiveTask = new JoinHiveTask(nodeMeta, isFinalNode, primaryTabFinder
                , joinTaskStatus, this.getFs().getFileSystem(), getEngineType(), dsGetter);
        //  joinHiveTask.setTaskContext(tskContext);
        joinHiveTask.setContext(execChainContext, tskContext);
        return joinHiveTask;
    }

    /**
     * END implements: IFlatTableBuilder
     * ==================================================================
     */

    @Override
    public boolean isGenerateCreateDDLSwitchOff() {
        return false;
    }

    @Override
    public CreateTableSqlBuilder.CreateDDL generateCreateDDL(IDataxProcessor.TableMap tableMapper) {

        final ITISFileSystem fileSystem = this.getFs().getFileSystem();
        final CreateTableSqlBuilder createTableSqlBuilder
                = new CreateTableSqlBuilder(tableMapper, this.getDataSourceFactory()) {

            @Override
            protected CreateTableName getCreateTableName() {
                CreateTableName nameBuilder = super.getCreateTableName();
                nameBuilder.setCreateTablePredicate("CREATE EXTERNAL TABLE IF NOT EXISTS");
                return nameBuilder;
            }

            protected void appendTabMeta(List<ColWrapper> pks) {

                HdfsFormat fsFormat = parseFSFormat();

                script.appendLine("COMMENT 'tis_tmp_" + tableMapper.getTo() + "' PARTITIONED BY(" + IDumpTable.PARTITION_PT + " string," + IDumpTable.PARTITION_PMOD + " string)   ");
                script.appendLine(fsFormat.getRowFormat());
                script.appendLine("STORED AS " + fsFormat.getFileType().getType());

                script.appendLine("LOCATION '").append(
                        FSHistoryFileUtils.getJoinTableStorePath(fileSystem.getRootDir(), getDumpTab(tableMapper.getTo()))
                ).append("'");
            }

            private HdfsFormat parseFSFormat() {

                HdfsFormat fsFormat = new HdfsFormat();

                fsFormat.setFieldDelimiter(String.valueOf(fileType.getFieldDelimiter()));
                //  (String) TisDataXHiveWriter.jobFileType.get(this)
                fsFormat.setFileType(fileType.getType());
                return fsFormat;
            }

            @Override
            protected ColWrapper createColWrapper(CMeta c) {
                return new ColWrapper(c) {
                    @Override
                    public String getMapperType() {
                        return c.getType().accept(HiveColumn.hiveTypeVisitor);
                    }
                };
            }
        };

        return createTableSqlBuilder.build();
    }

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXHiveWriter.class, "DataXHiveWriter-tpl.json");
    }

    public DataSourceMeta.JDBCConnection getConnection() {
        Hiveserver2DataSourceFactory dsFactory = getDataSourceFactory();
        String jdbcUrl = dsFactory.getJdbcUrl();
        try {
            return dsFactory.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new RuntimeException(jdbcUrl, e);
        }
    }

    @Override
    public Hiveserver2DataSourceFactory getDataSourceFactory() {
        if (StringUtils.isBlank(this.dbName)) {
            throw new IllegalStateException("prop dbName can not be null");
        }
        return BasicDataXRdbmsWriter.getDs(this.dbName);
    }

    @Override
    public Integer getRowFetchSize() {
        throw new UnsupportedOperationException();
    }

    public final IHiveConnGetter getHiveConnGetter() {
        return this.getDataSourceFactory();
    }


    public class HiveDataXContext extends FSDataXContext {

        private final String dataxPluginName;

        public HiveDataXContext(String dataxPluginName, IDataxProcessor.TableMap tabMap, String dataXName) {
            super(tabMap, dataXName);
            this.dataxPluginName = dataxPluginName;
        }

        @Override
        public String getTableName() {
            return tabDecorator.decorate(super.getTableName());
        }

        public String getDataxPluginName() {
            return this.dataxPluginName;
        }

        public Integer getPartitionRetainNum() {
            return partitionRetainNum;
        }

        public String getPartitionFormat() {
            return partitionFormat;
        }
    }


    /**
     * ========================================================
     * impl:
     *
     * @see IDataXBatchPost
     */
    @Override
    public ExecutePhaseRange getPhaseRange() {
        return new ExecutePhaseRange(FullbuildPhase.FullDump, FullbuildPhase.JOIN);
    }

    @Override
    public IRemoteTaskTrigger createPreExecuteTask(IExecChainContext execContext, ISelectedTab tab) {
        Objects.requireNonNull(partitionRetainNum, "partitionRetainNum can not be null");
        return new IRemoteTaskTrigger() {
            @Override
            public String getTaskName() {
                return IDataXBatchPost.getPreExecuteTaskName(tab);
            }

            @Override
            public void run() {

                // 负责初始化表
                Hiveserver2DataSourceFactory ds = DataXHiveWriter.this.getDataSourceFactory();
                EntityName dumpTable = getDumpTab(tab);
                ITISFileSystem fs = getFs().getFileSystem();
                Path tabDumpParentPath = getTabDumpParentPath(execContext, tab);// new Path(fs.getRootDir().unwrap(Path.class), getHdfsSubPath(dumpTable));
                ds.visitFirstConnection((conn) -> {
                    try {
                        Objects.requireNonNull(tabDumpParentPath, "tabDumpParentPath can not be null");
                        Hiveserver2DataSourceFactory dsFactory = getDataSourceFactory();
                        final IPath parentPath = fs.getPath(new HdfsPath(tabDumpParentPath), "..");
                        JoinHiveTask.initializeTable(dsFactory
                                , conn, dumpTable,
                                new JoinHiveTask.IHistoryTableProcessor() {
                                    @Override
                                    public void cleanHistoryTable() throws IOException {
                                        JoinHiveTask.cleanHistoryTable(fs, parentPath, dsFactory, conn, dumpTable, partitionRetainNum);
                                    }
                                }
                                , () -> true, () -> {
                                    try {
                                        BasicDataXRdbmsWriter.process(dataXName, execContext.getProcessor(), DataXHiveWriter.this, DataXHiveWriter.this, conn, tab.getName());
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        };
    }

    @Override
    public IRemoteTaskTrigger createPostTask(
            IExecChainContext execContext, ISelectedTab tab, DataXCfgGenerator.GenerateCfgs cfgFileNames) {

        return new IRemoteTaskTrigger() {

            @Override
            public String getTaskName() {
                return getEngineType().getToken() + "_" + tab.getName() + "_bind";
            }

            @Override
            public void run() {
                bindHiveTables(execContext, tab);
            }
        };
    }

    private EntityName getDumpTab(ISelectedTab tab) {
        return getDumpTab(tab.getName());
    }

    private EntityName getDumpTab(String tabName) {
        return EntityName.create(this.getDataSourceFactory().getDbName(), tabName);
    }

    private String getHdfsSubPath(IExecChainContext execContext, EntityName dumpTable) {
        Objects.requireNonNull(dumpTable, "dumpTable can not be null");
        // return dumpTable.getNameWithPath() + "/" + DataxUtils.getDumpTimeStamp();
        return dumpTable.getNameWithPath() + "/"
                + TimeFormat.parse(this.partitionFormat).format(execContext.getPartitionTimestampWithMillis());
    }

    private Path getTabDumpParentPath(IExecChainContext execContext, ISelectedTab tab) {
        EntityName dumpTable = getDumpTab(tab);
        ITISFileSystem fs = getFs().getFileSystem();
        Path tabDumpParentPath = new Path(fs.getRootDir().unwrap(Path.class), getHdfsSubPath(execContext, dumpTable));
        return tabDumpParentPath;
    }


    private void bindHiveTables(IExecChainContext execContext, ISelectedTab tab) {
        try {

            EntityName dumpTable = getDumpTab(tab);
            String dumpTimeStamp = TimeFormat.parse(this.partitionFormat).format(execContext.getPartitionTimestampWithMillis());

            if (!execContext.isDryRun()) {
                Path tabDumpParentPath = getTabDumpParentPath(execContext, tab);
                Hiveserver2DataSourceFactory dsFactory = this.getDataSourceFactory();
                try (DataSourceMeta.JDBCConnection hiveConn = this.getConnection()) {
                    final Path dumpParentPath = tabDumpParentPath;
                    BindHiveTableTool.bindHiveTables(dsFactory, hiveConn, this.getFs().getFileSystem()
                            , Collections.singletonMap(dumpTable, () -> {
                                        return new BindHiveTableTool.HiveBindConfig(Collections.emptyList(), dumpParentPath);
                                    }
                            )
                            , dumpTimeStamp
                            , (columns, hiveTable) -> {
                                return true;
                            },
                            (columns, hiveTable) -> {
                                throw new UnsupportedOperationException("table " + hiveTable.getTabName() + " shall have create in 'createPreExecuteTask'");
                            }
                    );

                }
            }

            recordPt(execContext, dumpTable, dumpTimeStamp);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 添加当前任务的pt
     */
    private void recordPt(IExecChainContext execContext, EntityName dumpTable, String dumpTimeStamp) {
        if (StringUtils.isEmpty(dumpTimeStamp)) {
            throw new IllegalArgumentException("param dumpTimeStamp can not be empty");
        }
        TabPartitions dateParams = ExecChainContextUtils.getDependencyTablesPartitions(execContext);
        dateParams.putPt(dumpTable, new DftTabPartition(dumpTimeStamp));
    }


//    /**
//     * https://cwiki.apache.org/confluence/display/hive/languagemanual+ddl#LanguageManualDDL-CreateTableCreate/Drop/TruncateTable
//     *
//     * @return
//     */
//    private List<HiveColumn> getCols() {
//        List<Configuration> cols = null;//this.columns;
//        AtomicInteger index = new AtomicInteger();
//        return cols.stream().map((c) -> {
//            HiveColumn hivCol = new HiveColumn();
//            DataType colType = DataType.ds(c.getString(HdfsColMeta.KEY_TYPE));
//            //SupportHiveDataType columnType = DataType.convert2HiveType();
//            String name = StringUtils.remove(c.getString(HdfsColMeta.KEY_NAME), "`");
//            if (StringUtils.isBlank(name)) {
//                throw new IllegalStateException("col name can not be blank");
//            }
//            hivCol.setName(name);
//            hivCol.setDataType(colType);
//            //hivCol.setType(columnType.name());
//            hivCol.setIndex(index.getAndIncrement());
//            return hivCol;
//        }).collect(Collectors.toList());
//    }

    /**
     * impl End: IDataXBatchPost
     * ========================================================
     */
    @TISExtension()
    public static class DefaultDescriptor extends HdfsWriterDescriptor implements IFlatTableBuilderDescriptor {
        public DefaultDescriptor() {
            super();
            //this.registerSelectOptions(KEY_FIELD_NAME_HIVE_CONN, () -> ParamsConfig.getItems(IHiveConnGetter.PLUGIN_NAME));
        }

        @Override
        public boolean isSupportTabCreate() {
            return true;
        }

        public boolean validatePartitionRetainNum(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
            Integer retainNum = Integer.parseInt(value);
            if (retainNum < 1 || retainNum > 5) {
                msgHandler.addFieldError(context, fieldName, "数目必须为不小于1且不大于5之间");
                return false;
            }
            return true;
        }

        @Override
        public EndType getEndType() {
            return EndType.HiveMetaStore;
        }
//        @Override
//        protected boolean validate(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
//            return HiveFlatTableBuilder.validateHiveAvailable(msgHandler, context, postFormVals);
//        }

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }
    }
}
