

package com.qlangtech.tis.plugin.datax;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.datax.IDataXBatchPost;
import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.exec.ExecChainContextUtils;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.ExecuteResult;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.fs.ITableBuildTask;
import com.qlangtech.tis.fs.ITaskContext;
import com.qlangtech.tis.fullbuild.indexbuild.DftTabPartition;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.fullbuild.phasestatus.IJoinTaskStatus;
import com.qlangtech.tis.fullbuild.taskflow.DataflowTask;
import com.qlangtech.tis.fullbuild.taskflow.IFlatTableBuilder;
import com.qlangtech.tis.fullbuild.taskflow.IFlatTableBuilderDescriptor;
import com.qlangtech.tis.plugin.aliyun.AccessKey;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.datax.odps.JoinOdpsTask;
import com.qlangtech.tis.plugin.datax.odps.OdpsDataSourceFactory;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.ISqlTask;
import com.qlangtech.tis.sql.parser.TabPartitions;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 **/
public class DataXOdpsWriter extends BasicDataXRdbmsWriter implements IFlatTableBuilder, IDataSourceFactoryGetter, IDataXBatchPost {
    private static final String DATAX_NAME = "Aliyun-ODPS";
    private static final Logger logger = LoggerFactory.getLogger(DataXOdpsWriter.class);

    @FormField(ordinal = 6, type = FormFieldType.ENUM, validate = {Validator.require})
    public Boolean truncate;

    @FormField(ordinal = 7, type = FormFieldType.INT_NUMBER, validate = {Validator.require})
    public Integer lifecycle;

    @FormField(ordinal = 8, type = FormFieldType.ENUM, validate = {Validator.require})
    public String partitionFormat;


    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXOdpsWriter.class, "DataXOdpsWriter-tpl.json");
    }

    @Override
    public ExecutePhaseRange getPhaseRange() {
        return new ExecutePhaseRange(FullbuildPhase.FullDump, FullbuildPhase.JOIN);
    }

    @Override
    public IRemoteTaskTrigger createPreExecuteTask(IExecChainContext execContext, ISelectedTab tab) {
        return new IRemoteTaskTrigger() {
            @Override
            public String getTaskName() {
                return IDataXBatchPost.getPreExecuteTaskName(tab);
            }

            @Override
            public void run() {

                // 负责初始化表
                OdpsDataSourceFactory ds = DataXOdpsWriter.this.getDataSourceFactory();
                EntityName dumpTable = getDumpTab(tab);
                // ITISFileSystem fs = getFs().getFileSystem();
                // Path tabDumpParentPath = getTabDumpParentPath(tab);// new Path(fs.getRootDir().unwrap(Path.class), getHdfsSubPath(dumpTable));
                ds.visitFirstConnection((conn) -> {
                    try {
//                        Objects.requireNonNull(tabDumpParentPath, "tabDumpParentPath can not be null");
//                        JoinHiveTask.initializeHiveTable(fs
//                                , fs.getPath(new HdfsPath(tabDumpParentPath), "..")
//                                , getDataSourceFactory()
//                                , conn.getConnection(), dumpTable, partitionRetainNum, () -> true, () -> {
//                                    try {
//                                        BasicDataXRdbmsWriter.process(dataXName, execContext.getProcessor(), DataXOdpsWriter.this
//                                                , DataXOdpsWriter.this, conn, tab.getName());
//                                    } catch (Exception e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                });

                        try {
                            BasicDataXRdbmsWriter.process(dataXName, execContext.getProcessor(), DataXOdpsWriter.this
                                    , DataXOdpsWriter.this, conn, tab.getName());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        };
    }

    private EntityName getDumpTab(ISelectedTab tab) {
        return getDumpTab(tab.getName());
    }

    private EntityName getDumpTab(String tabName) {
        return EntityName.create(this.getDataSourceFactory().getDbName(), tabName);
    }


    @Override
    public OdpsDataSourceFactory getDataSourceFactory() {
        return (OdpsDataSourceFactory) super.getDataSourceFactory();
    }

    @Override
    public IRemoteTaskTrigger createPostTask(
            IExecChainContext execContext, ISelectedTab tab, DataXCfgGenerator.GenerateCfgs cfgFileNames) {

        return new IRemoteTaskTrigger() {
            @Override
            public String getTaskName() {
                return "odps_" + tab.getName() + "_bind";
            }

            @Override
            public void run() {
                bindHiveTables(execContext, tab);
            }
        };
    }

    private void bindHiveTables(IExecChainContext execContext, ISelectedTab tab) {
        String dumpTimeStamp = TimeFormat.parse(this.partitionFormat)
                .format(execContext.getPartitionTimestampWithMillis());

        recordPt(execContext, getDumpTab(tab.getName()), dumpTimeStamp);
    }


    /**
     * 添加当前任务的pt
     */
    private void recordPt(IExecChainContext execContext, EntityName dumpTable, String dumpTimeStamp) {
        //  Map<IDumpTable, ITabPartition> dateParams = ExecChainContextUtils.getDependencyTablesPartitions(execChainContext);
        if (StringUtils.isEmpty(dumpTimeStamp)) {
            throw new IllegalArgumentException("param dumpTimeStamp can not be empty");
        }
        TabPartitions dateParams = ExecChainContextUtils.getDependencyTablesPartitions(execContext);
        dateParams.putPt(dumpTable, new DftTabPartition(dumpTimeStamp));
    }


    @Override
    public DataflowTask createTask(ISqlTask nodeMeta, boolean isFinalNode, IExecChainContext tplContext
            , ITaskContext tskContext, IJoinTaskStatus joinTaskStatus, IDataSourceFactoryGetter dsGetter
            , Supplier<IPrimaryTabFinder> primaryTabFinder) {

        JoinOdpsTask odpsTask = new JoinOdpsTask(this, dsGetter, nodeMeta, isFinalNode, primaryTabFinder, joinTaskStatus);
        odpsTask.setContext(tplContext, tskContext);
        return odpsTask;
    }

    /**
     * https://help.aliyun.com/document_detail/73768.html#section-ixi-bgd-948
     *
     * @param tableMapper
     * @return
     */
    @Override
    public CreateTableSqlBuilder.CreateDDL generateCreateDDL(IDataxProcessor.TableMap tableMapper) {
        final CreateTableSqlBuilder createTableSqlBuilder
                = new CreateTableSqlBuilder(tableMapper, this.getDataSourceFactory()) {

            @Override
            protected CreateTableName getCreateTableName() {
                CreateTableName nameBuilder = super.getCreateTableName();
                // EXTERNAL
                nameBuilder.setCreateTablePredicate("CREATE TABLE IF NOT EXISTS");
                return nameBuilder;
            }

            protected void appendTabMeta(List<ColWrapper> pks) {

                // HdfsFormat fsFormat = parseFSFormat();
                script.appendLine("COMMENT 'tis_tmp_" + tableMapper.getTo() + "' PARTITIONED BY(" + IDumpTable.PARTITION_PT + " string," + IDumpTable.PARTITION_PMOD + " string)   ");
                script.append("lifecycle " + Objects.requireNonNull(lifecycle, "lifecycle can not be null"));

                // script.appendLine(fsFormat.getRowFormat());
                // script.appendLine("STORED AS " + fsFormat.getFileType().getType());

//                script.appendLine("LOCATION '").append(
//                        FSHistoryFileUtils.getJoinTableStorePath(fileSystem.getRootDir(), getDumpTab(tableMapper.getTo()))
//                ).append("'");
            }

            @Override
            protected ColWrapper createColWrapper(CMeta c) {
                return new ColWrapper(c) {
                    @Override
                    public String getMapperType() {
                        return c.getType().accept(typeTransfer);
                    }
                };
            }
        };

        return createTableSqlBuilder.build();
    }

    /**
     * https://help.aliyun.com/document_detail/159540.html?spm=a2c4g.11186623.0.0.6ae36f60bs6Lm2
     */
    public static DataType.TypeVisitor<String> typeTransfer = new DataType.TypeVisitor<String>() {
        @Override
        public String bigInt(DataType type) {
            return "BIGINT";
        }

        @Override
        public String decimalType(DataType type) {
            return "DECIMAL(" + type.columnSize + "," + type.getDecimalDigits() + ")";
        }

        @Override
        public String intType(DataType type) {
            return "INT";
        }

        @Override
        public String tinyIntType(DataType dataType) {
            return "TINYINT";
        }

        @Override
        public String smallIntType(DataType dataType) {
            return "SMALLINT";
        }

        @Override
        public String boolType(DataType dataType) {
            return "BOOLEAN";
        }

        @Override
        public String floatType(DataType type) {
            return "FLOAT";
        }

        @Override
        public String doubleType(DataType type) {
            return "DOUBLE";
        }

        @Override
        public String dateType(DataType type) {
            return "DATE";
        }

        @Override
        public String timestampType(DataType type) {
            return "TIMESTAMP";
        }

        @Override
        public String bitType(DataType type) {
            return "STRING";
        }

        @Override
        public String blobType(DataType type) {
            return "BINARY";
        }

        @Override
        public String varcharType(DataType type) {
            return "STRING";
        }
    };

    @Override
    public boolean isGenerateCreateDDLSwitchOff() {
        return false;
    }

    @Override
    public ExecuteResult startTask(ITableBuildTask dumpTask) {

        try (DataSourceMeta.JDBCConnection conn = this.getConnection()) {
            return dumpTask.process(new ITaskContext() {
                @Override
                public DataSourceMeta.JDBCConnection getObj() {
                    return conn;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public DataSourceMeta.JDBCConnection getConnection() {
        OdpsDataSourceFactory dsFactory = getDataSourceFactory();
        String jdbcUrl = dsFactory.getJdbcUrl();
        try {
            return dsFactory.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new RuntimeException(jdbcUrl, e);
        }
    }

    @Override
    public IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap) {
        if (!tableMap.isPresent()) {
            throw new IllegalArgumentException("param tableMap shall be present");
        }

        return new OdpsContext(this, tableMap.get());
    }

    @Override
    public Class<?> getOwnerClass() {
        return DataXOdpsWriter.class;
    }


    public static class OdpsContext implements IDataxContext {
        private final DataXOdpsWriter odpsWriter;
        private final IDataxProcessor.TableMap tableMapper;
        private final AccessKey accessKey;
        private final OdpsDataSourceFactory dsFactory;

        public OdpsContext(DataXOdpsWriter odpsWriter, IDataxProcessor.TableMap tableMapper) {
            this.odpsWriter = odpsWriter;
            this.tableMapper = tableMapper;

            this.dsFactory = odpsWriter.getDataSourceFactory();
            this.accessKey = this.dsFactory.getAccessKey();

        }

        public String getTable() {
            return this.tableMapper.getTo();
        }

        public String getProject() {
            return this.dsFactory.project;
        }

        public List<String> getColumn() {
            return this.tableMapper.getSourceCols()
                    .stream().map((col) -> col.getName()).collect(Collectors.toList());
        }

        public String getAccessId() {
            return this.accessKey.getAccessKeyId();
        }

        public String getAccessKey() {
            return this.accessKey.getAccessKeySecret();
        }

        public boolean isTruncate() {
            return this.odpsWriter.truncate;
        }

        public String getOdpsServer() {
            return this.dsFactory.odpsServer;
        }

        public String getTunnelServer() {
            return this.dsFactory.tunnelServer;
        }

        public String getPartitionTimeFormat() {
            return odpsWriter.partitionFormat;
        }
    }

    @TISExtension()
    public static class DefaultDescriptor extends RdbmsWriterDescriptor
            implements IFlatTableBuilderDescriptor {
        public DefaultDescriptor() {
            super();
        }


        public boolean validatePartitionFormat(IFieldErrorHandler msgHandler, Context context, String fieldName, String val) {

            try {
                TimeFormat.parse(val);
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                msgHandler.addFieldError(context, fieldName, e.getMessage());
                return false;
            }

            return true;
        }

        /**
         * @param msgHandler
         * @param context
         * @param fieldName
         * @param val
         * @return
         */
        public boolean validateLifecycle(IFieldErrorHandler msgHandler, Context context, String fieldName, String val) {
            int lifecycle = Integer.parseInt(val);
            final int MIN_Lifecycle = 3;
            if (lifecycle < MIN_Lifecycle) {
                msgHandler.addFieldError(context, fieldName, "不能小于" + MIN_Lifecycle + "天");
                return false;
            }
            return true;
        }


        @Override
        public boolean isSupportTabCreate() {
            return true;
        }

        @Override
        public boolean isSupportIncr() {
            return false;
        }

        @Override
        public EndType getEndType() {
            return EndType.AliyunODPS;
        }

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }
    }
}
