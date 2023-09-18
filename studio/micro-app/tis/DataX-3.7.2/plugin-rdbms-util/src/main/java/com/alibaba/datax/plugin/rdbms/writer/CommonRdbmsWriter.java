package com.alibaba.datax.plugin.rdbms.writer;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.job.IJobContainerContext;
import com.alibaba.datax.plugin.rdbms.util.DBUtil;
import com.alibaba.datax.plugin.rdbms.util.DBUtilErrorCode;
import com.alibaba.datax.plugin.rdbms.util.DataBaseType;
import com.alibaba.datax.plugin.rdbms.util.RdbmsException;
import com.alibaba.datax.plugin.rdbms.writer.util.*;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
import com.qlangtech.tis.web.start.TisAppLaunch;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommonRdbmsWriter {

    public static class Job {
        private DataBaseType dataBaseType;
        private IDataSourceFactoryGetter dataSourceFactoryGetter;
        private final IJobContainerContext containerContext;

        private static final Logger LOG = LoggerFactory
                .getLogger(Job.class);

        public Job(DataBaseType dataBaseType, IJobContainerContext containerContext) {
            this.dataBaseType = dataBaseType;
            this.containerContext = containerContext;
            OriginalConfPretreatmentUtil.DATABASE_TYPE = this.dataBaseType;
        }

        public void init(Configuration originalConfig) {
            this.dataSourceFactoryGetter = DBUtil.getWriterDataSourceFactoryGetter(originalConfig, this.containerContext);
            OriginalConfPretreatmentUtil.doPretreatment(originalConfig, this.dataSourceFactoryGetter, this.dataBaseType);

            LOG.debug("After job init(), originalConfig now is:[\n{}\n]",
                    originalConfig.toJSON());
        }


        /*目前只支持MySQL Writer跟Oracle Writer;检查PreSQL跟PostSQL语法以及insert，delete权限*/
        public void writerPreCheck(Configuration originalConfig, DataBaseType dataBaseType) {
            /*检查PreSql跟PostSql语句*/
            prePostSqlValid(originalConfig, dataBaseType);
            /*检查insert 跟delete权限*/
            privilegeValid(originalConfig, dataBaseType);
        }

        public void prePostSqlValid(Configuration originalConfig, DataBaseType dataBaseType) {
            /*检查PreSql跟PostSql语句*/
            WriterUtil.preCheckPrePareSQL(originalConfig, dataBaseType, this.dataSourceFactoryGetter.getDBReservedKeys());
            WriterUtil.preCheckPostSQL(originalConfig, dataBaseType, this.dataSourceFactoryGetter.getDBReservedKeys());
        }

        public void privilegeValid(Configuration originalConfig, DataBaseType dataBaseType) {
            /*检查insert 跟delete权限*/
            String username = originalConfig.getString(Key.USERNAME);
            String password = originalConfig.getString(Key.PASSWORD);
            List<Object> connections = originalConfig.getList(Constant.CONN_MARK,
                    Object.class);

            for (int i = 0, len = connections.size(); i < len; i++) {
                Configuration connConf = Configuration.from(connections.get(i).toString());
                String jdbcUrl = connConf.getString(Key.JDBC_URL);
                List<String> expandedTables = connConf.getList(Key.TABLE, String.class);
                boolean hasInsertPri = DBUtil.checkInsertPrivilege(dataSourceFactoryGetter, jdbcUrl, username, password, expandedTables);

                if (!hasInsertPri) {
                    throw RdbmsException.asInsertPriException(dataBaseType, originalConfig.getString(Key.USERNAME), jdbcUrl);
                }

                if (DBUtil.needCheckDeletePrivilege(originalConfig)) {
                    boolean hasDeletePri = DBUtil.checkDeletePrivilege(dataSourceFactoryGetter, jdbcUrl, username, password, expandedTables);
                    if (!hasDeletePri) {
                        throw RdbmsException.asDeletePriException(dataBaseType, originalConfig.getString(Key.USERNAME), jdbcUrl);
                    }
                }
            }
        }

        // 一般来说，是需要推迟到 task 中进行pre 的执行（单表情况例外）
        public void prepare(Configuration originalConfig) {
            int tableNumber = originalConfig.getInt(Constant.TABLE_NUMBER_MARK);
            if (tableNumber == 1) {
                String username = originalConfig.getString(Key.USERNAME);
                String password = originalConfig.getString(Key.PASSWORD);

                List<Object> conns = originalConfig.getList(Constant.CONN_MARK,
                        Object.class);
                Configuration connConf = Configuration.from(conns.get(0)
                        .toString());

                // 这里的 jdbcUrl 已经 append 了合适后缀参数
                String jdbcUrl = connConf.getString(Key.JDBC_URL);
                originalConfig.set(Key.JDBC_URL, jdbcUrl);

                SelectTable table = SelectTable.create(originalConfig, this.dataSourceFactoryGetter.getDBReservedKeys());

                // String table = connConf.getList(Key.TABLE, String.class).get(0);
                originalConfig.set(Key.TABLE, table.getUnescapeTabName());
                if (table.isContainEscapeChar()) {
                    originalConfig.set(Key.ESCAPE_CHAR, table.getEscapeChar());
                }

                List<String> preSqls = originalConfig.getList(Key.PRE_SQL,
                        String.class);
                List<String> renderedPreSqls = WriterUtil.renderPreOrPostSqls(preSqls, table);

                originalConfig.remove(Constant.CONN_MARK);
                if (null != renderedPreSqls && !renderedPreSqls.isEmpty()) {
                    // 说明有 preSql 配置，则此处删除掉
                    originalConfig.remove(Key.PRE_SQL);

                    Connection conn = DBUtil.getConnection(dataSourceFactoryGetter, jdbcUrl, username, password);
                    LOG.info("Begin to execute preSqls:[{}]. context info:{}.",
                            StringUtils.join(renderedPreSqls, ";"), jdbcUrl);

                    WriterUtil.executeSqls(conn, renderedPreSqls, jdbcUrl, dataBaseType);
                    DBUtil.closeDBResources(null, null, conn);
                }
            }

            LOG.debug("After job prepare(), originalConfig now is:[\n{}\n]",
                    originalConfig.toJSON());
        }

        public List<Configuration> split(Configuration originalConfig,
                                         int mandatoryNumber) {

            return WriterUtil.doSplit(originalConfig, mandatoryNumber, this.dataSourceFactoryGetter.getDBReservedKeys());
        }

        // 一般来说，是需要推迟到 task 中进行post 的执行（单表情况例外）

        public void post(Configuration originalConfig) {
            int tableNumber = originalConfig.getInt(Constant.TABLE_NUMBER_MARK);
            if (tableNumber == 1) {
                String username = originalConfig.getString(Key.USERNAME);
                String password = originalConfig.getString(Key.PASSWORD);

                // 已经由 prepare 进行了appendJDBCSuffix处理
                String jdbcUrl = originalConfig.getString(Key.JDBC_URL);

                SelectTable table = SelectTable.createInTask(
                        originalConfig, this.dataSourceFactoryGetter.getDBReservedKeys());//.getString(Key.TABLE);

                List<String> postSqls = originalConfig.getList(Key.POST_SQL,
                        String.class);
                List<String> renderedPostSqls = WriterUtil.renderPreOrPostSqls(
                        postSqls, table);

                if (null != renderedPostSqls && !renderedPostSqls.isEmpty()) {
                    // 说明有 postSql 配置，则此处删除掉
                    originalConfig.remove(Key.POST_SQL);

                    Connection conn = DBUtil.getConnection(this.dataSourceFactoryGetter, jdbcUrl, username, password);

                    LOG.info(
                            "Begin to execute postSqls:[{}]. context info:{}.",
                            StringUtils.join(renderedPostSqls, ";"), jdbcUrl);
                    WriterUtil.executeSqls(conn, renderedPostSqls, jdbcUrl, dataBaseType);
                    DBUtil.closeDBResources(null, null, conn);
                }
            }
        }

        public void destroy(Configuration originalConfig) {
        }

    }

    public static class Task {
        protected static final Logger LOG = LoggerFactory
                .getLogger(Task.class);

        protected DataBaseType dataBaseType;
        private IDataSourceFactoryGetter dataSourceFactoryGetter;
        private static final String VALUE_HOLDER = "?";

        protected String username;
        protected String password;
        protected String jdbcUrl;
        protected SelectTable table;
        protected SelectCols columns;
        protected List<String> preSqls;
        protected List<String> postSqls;
        protected int batchSize;
        protected int batchByteSize;
        protected int columnNumber = 0;
        protected TaskPluginCollector taskPluginCollector;


        // 作为日志显示信息时，需要附带的通用信息。比如信息所对应的数据库连接等信息，针对哪个表做的操作
        protected static String BASIC_MESSAGE;

        protected static String INSERT_OR_REPLACE_TEMPLATE;

        protected String writeRecordSql;
        protected String writeMode;
        protected boolean emptyAsNull;
        protected List<Pair<ColumnMetaData, IStatementSetter>> resultSetMetaData;
        private final IJobContainerContext containerContext;

        public Task(DataBaseType dataBaseType, IJobContainerContext containerContext) {
            this.dataBaseType = dataBaseType;
            this.containerContext = containerContext;
        }

        public void init(Configuration writerSliceConfig) {
            this.username = writerSliceConfig.getString(Key.USERNAME);
            this.password = writerSliceConfig.getString(Key.PASSWORD);
            this.jdbcUrl = writerSliceConfig.getString(Key.JDBC_URL);

            //ob10的处理
            if (this.jdbcUrl.startsWith(Constant.OB10_SPLIT_STRING) && this.dataBaseType == DataBaseType.MySql) {
                String[] ss = this.jdbcUrl.split(Constant.OB10_SPLIT_STRING_PATTERN);
                if (ss.length != 3) {
                    throw DataXException
                            .asDataXException(
                                    DBUtilErrorCode.JDBC_OB10_ADDRESS_ERROR, "JDBC OB10格式错误，请联系askdatax");
                }
                LOG.info("this is ob1_0 jdbc url.");
                this.username = ss[1].trim() + ":" + this.username;
                this.jdbcUrl = ss[2];
                LOG.info("this is ob1_0 jdbc url. user=" + this.username + " :url=" + this.jdbcUrl);
            }
            this.dataSourceFactoryGetter = DBUtil.getWriterDataSourceFactoryGetter(writerSliceConfig, this.containerContext);
            this.table = SelectTable.createInTask(writerSliceConfig, this.dataSourceFactoryGetter.getDBReservedKeys());


            this.preSqls = writerSliceConfig.getList(Key.PRE_SQL, String.class);
            this.postSqls = writerSliceConfig.getList(Key.POST_SQL, String.class);
            this.batchSize = writerSliceConfig.getInt(Key.BATCH_SIZE, Constant.DEFAULT_BATCH_SIZE);
            this.batchByteSize = writerSliceConfig.getInt(Key.BATCH_BYTE_SIZE, Constant.DEFAULT_BATCH_BYTE_SIZE);

            writeMode = writerSliceConfig.getString(Key.WRITE_MODE, "INSERT");
            emptyAsNull = writerSliceConfig.getBool(Key.EMPTY_AS_NULL, true);
            INSERT_OR_REPLACE_TEMPLATE = writerSliceConfig.getString(Constant.INSERT_OR_REPLACE_TEMPLATE_MARK);
            this.writeRecordSql = String.format(INSERT_OR_REPLACE_TEMPLATE, this.table);
            BASIC_MESSAGE = String.format("jdbcUrl:[%s], table:[%s]", this.jdbcUrl, this.table);


            this.columns = SelectCols.createSelectCols(writerSliceConfig, this.dataSourceFactoryGetter.getDataSourceFactory());
            this.columnNumber = this.columns.size();
        }

        public void prepare(Configuration writerSliceConfig) {
            Connection connection = DBUtil.getConnection(this.dataSourceFactoryGetter, this.jdbcUrl, username, password);

            DBUtil.dealWithSessionConfig(connection, writerSliceConfig, this.dataBaseType, BASIC_MESSAGE);

            int tableNumber = writerSliceConfig.getInt(Constant.TABLE_NUMBER_MARK);
            if (tableNumber != 1) {
                LOG.info("Begin to execute preSqls:[{}]. context info:{}.",
                        StringUtils.join(this.preSqls, ";"), BASIC_MESSAGE);
                WriterUtil.executeSqls(connection, this.preSqls, BASIC_MESSAGE, dataBaseType);
            }

            DBUtil.closeDBResources(null, null, connection);
        }


        protected IStatementSetter parseColSetter(ColumnMetaData cm) {

            switch (cm.getType().type) {
                case Types.CHAR:
                case Types.NCHAR:
                case Types.CLOB:
                case Types.NCLOB:
                case Types.VARCHAR:
                case Types.LONGVARCHAR:
                case Types.NVARCHAR:
                case Types.LONGNVARCHAR:
//                    preparedStatement.setString(columnIndex + 1, column
//                            .asString());
                    return (stat, colIndex, column) -> {
                        stat.setString(colIndex, column.asString());
                    };

                case Types.SMALLINT:
                case Types.INTEGER:
                    return (stat, colIndex, column) -> {
                        // long l =   18446744073709551615l;
                        stat.setLong(colIndex, column.asLong());
                    };
                case Types.BIGINT:
                    return (stat, colIndex, column) -> {
                        // long l =   18446744073709551615l;
                        // column.asBigInteger();
                        stat.setBigDecimal(colIndex, column.asBigDecimal());
                    };
                case Types.NUMERIC:
                case Types.DECIMAL:
                case Types.REAL:
                    return (stat, colIndex, column) -> {

                        // String strValue = column.asString();
                        if (emptyAsNull && column.getRawData() == null) {
                            stat.setNull(colIndex, cm.getType().type);
                        } else {
                            stat.setBigDecimal(colIndex, column.asBigDecimal());
                        }
                    };
                case Types.FLOAT:
                case Types.DOUBLE:
                    return (stat, colIndex, column) -> {

                        //  String strValue = column.asString();
                        if (emptyAsNull && column.getRawData() == null) {
                            stat.setNull(colIndex, cm.getType().type);
                        } else {
                            stat.setDouble(colIndex, column.asDouble());
                        }
                    };
                //tinyint is a little special in some database like mysql {boolean->tinyint(1)}
                case Types.TINYINT:

                    return (stat, colIndex, column) -> {
//                        Long longValue = column.asLong();
//                        if (null == longValue) {
//                            stat.setString(colIndex, null);
//                        } else {
                        stat.setShort(colIndex, column.asLong().shortValue());
                        //}
                    };

                // for mysql bug, see http://bugs.mysql.com/bug.php?id=35115
                case Types.DATE:

                    return (stat, colIndex, column) -> {
//                    if (typeName == null) {
//                        typeName = cm.getType().typeName; //this.resultSetMetaData.getRight().get(columnIndex);
//                    }
                        java.util.Date utilDate;
                        if (cm.getType().typeName.equalsIgnoreCase("year")) {
//                        if (column.asBigInteger() == null) {
//                            stat.setString(columnIndex + 1, null);
//                        } else {
                            stat.setInt(colIndex, column.asBigInteger().intValue());
                            //}
                        } else {
                            java.sql.Date sqlDate = null;
                            try {
                                utilDate = column.asDate();
                            } catch (DataXException e) {
                                throw new SQLException(String.format(
                                        "Date 类型转换错误：[%s]", column));
                            }

                            if (null != utilDate) {
                                sqlDate = new java.sql.Date(utilDate.getTime());
                            }
                            stat.setDate(colIndex, sqlDate);
                        }
                    };
                //  break;

                case Types.TIME:
                    return (stat, colIndex, column) -> {
                        java.sql.Time sqlTime = null;
                        java.util.Date utilDate;
                        try {
                            utilDate = column.asDate();
                        } catch (DataXException e) {
                            throw new SQLException(String.format(
                                    "TIME 类型转换错误：[%s]", column));
                        }

                        if (null != utilDate) {
                            sqlTime = new java.sql.Time(utilDate.getTime());
                        }
                        stat.setTime(colIndex, sqlTime);
                    };

                case Types.TIMESTAMP:
                    return (stat, colIndex, column) -> {
                        java.sql.Timestamp sqlTimestamp = null;
                        java.util.Date utilDate;
                        try {
                            utilDate = column.asDate();
                        } catch (DataXException e) {
                            throw new SQLException(String.format(
                                    "TIMESTAMP 类型转换错误：[%s]", column));
                        }

                        if (null != utilDate) {
                            sqlTimestamp = new java.sql.Timestamp(
                                    utilDate.getTime());
                        }
                        stat.setTimestamp(colIndex, sqlTimestamp);
                    };

                case Types.BINARY:
                case Types.VARBINARY:
                case Types.BLOB:
                case Types.LONGVARBINARY:
                    return (stat, colIndex, column) -> {
                        stat.setBytes(colIndex, column
                                .asBytes());
                    };
                case Types.BOOLEAN:
                    return (stat, colIndex, column) -> {
                        stat.setBoolean(colIndex, column.asBoolean());
                    };
                // warn: bit(1) -> Types.BIT 可使用setBoolean
                // warn: bit(>1) -> Types.VARBINARY 可使用setBytes
                case Types.BIT:
                    return (stat, colIndex, column) -> {
                        if (this.dataBaseType == DataBaseType.MySql) {
                            stat.setBoolean(colIndex, column.asBoolean());
                        } else {
                            stat.setString(colIndex, column.asString());
                        }
                    };

                //break;
                default:
                    throw DataXException
                            .asDataXException(
                                    DBUtilErrorCode.UNSUPPORTED_TYPE,
                                    String.format(
                                            "您的配置文件中的列配置信息有误. 因为DataX 不支持数据库写入这种字段类型. 字段名:[%s], 字段类型:[%d], 字段Java类型:[%s]. 请修改表中该字段的类型或者不同步该字段.",
                                            cm.getName(),
                                            cm.getType().type,
                                            cm.getType().typeName));
            }

        }


        public void startWriteWithConnection(RecordReceiver recordReceiver, TaskPluginCollector taskPluginCollector, DataSourceMeta.JDBCConnection connection) {
            this.taskPluginCollector = taskPluginCollector;

            // 用于写入数据的时候的类型根据目的表字段类型转换
//            this.resultSetMetaData = DBUtil.getColumnMetaData(connection,
//                    this.table, StringUtils.join(this.columns, ","));

            this.resultSetMetaData = DBUtil.getColumnMetaData(Optional.of(connection), true, this.dataSourceFactoryGetter, this.table, columns)
                    .stream().map((c) -> Pair.of(c, parseColSetter(c))).collect(Collectors.toList());


            // 写数据库的SQL语句
            calcWriteRecordSql();

            List<Record> writeBuffer = new ArrayList<Record>(this.batchSize);
            int bufferBytes = 0;
            try {
                Record record;
                while ((record = recordReceiver.getFromReader()) != null) {
                    if (record.getColumnNumber() != this.columnNumber) {
                        // 源头读取字段列数与目的表字段写入列数不相等，直接报错
                        throw DataXException
                                .asDataXException(
                                        DBUtilErrorCode.CONF_ERROR,
                                        String.format(
                                                "列配置信息有错误. 因为您配置的任务中，源头读取字段数:%s 与 目的表要写入的字段数:%s 不相等. 请检查您的配置并作出修改.",
                                                record.getColumnNumber(),
                                                this.columnNumber));
                    }

                    writeBuffer.add(record);
                    bufferBytes += record.getMemorySize();

                    if (writeBuffer.size() >= batchSize || bufferBytes >= batchByteSize) {
                        doBatchInsert(connection, writeBuffer);
                        writeBuffer.clear();
                        bufferBytes = 0;
                    }
                }
                if (!writeBuffer.isEmpty()) {
                    doBatchInsert(connection, writeBuffer);
                    writeBuffer.clear();
                    bufferBytes = 0;
                }
            } catch (Exception e) {
                throw DataXException.asDataXException(
                        DBUtilErrorCode.WRITE_DATA_ERROR, e);
            } finally {
                writeBuffer.clear();
                bufferBytes = 0;
                DBUtil.closeDBResources(null, null, connection.getConnection());
            }
        }

        // TODO 改用连接池，确保每次获取的连接都是可用的（注意：连接可能需要每次都初始化其 session）
        public void startWrite(RecordReceiver recordReceiver,
                               Configuration writerSliceConfig,
                               TaskPluginCollector taskPluginCollector) {
            Connection connection = DBUtil.getConnection(this.dataSourceFactoryGetter,
                    this.jdbcUrl, username, password);
            DBUtil.dealWithSessionConfig(connection, writerSliceConfig,
                    this.dataBaseType, BASIC_MESSAGE);
            startWriteWithConnection(recordReceiver, taskPluginCollector, new DataSourceMeta.JDBCConnection(connection, this.jdbcUrl));
        }


        public void post(Configuration writerSliceConfig) {
            int tableNumber = writerSliceConfig.getInt(
                    Constant.TABLE_NUMBER_MARK);

            boolean hasPostSql = (this.postSqls != null && this.postSqls.size() > 0);
            if (tableNumber == 1 || !hasPostSql) {
                return;
            }

            Connection connection = DBUtil.getConnection(this.dataSourceFactoryGetter,
                    this.jdbcUrl, username, password);

            LOG.info("Begin to execute postSqls:[{}]. context info:{}.",
                    StringUtils.join(this.postSqls, ";"), BASIC_MESSAGE);
            WriterUtil.executeSqls(connection, this.postSqls, BASIC_MESSAGE, dataBaseType);
            DBUtil.closeDBResources(null, null, connection);
        }

        public void destroy(Configuration writerSliceConfig) {
        }

        private static final int MAX_BATCH_INSERT_COUNT = 30;
        int batchInsertFaildCount = 0;

        protected void doBatchInsert(DataSourceMeta.JDBCConnection conn, List<Record> buffer)
                throws SQLException {
            Connection connection = conn.getConnection();
            PreparedStatement preparedStatement = null;
            try {
                connection.setAutoCommit(false);
                preparedStatement = connection
                        .prepareStatement(this.writeRecordSql);

                for (Record record : buffer) {
                    preparedStatement = fillPreparedStatement(
                            preparedStatement, record);
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                if (++batchInsertFaildCount > MAX_BATCH_INSERT_COUNT) {
                    throw e;
                }
                LOG.warn("回滚此次写入, 采用每次写入一行方式提交. 因为:" + e.getMessage());
                connection.rollback();
                doOneInsert(connection, buffer);
            } catch (Exception e) {
                throw DataXException.asDataXException(
                        DBUtilErrorCode.WRITE_DATA_ERROR, e);
            } finally {
                DBUtil.closeDBResources(preparedStatement, null);
            }
        }

        protected void doOneInsert(Connection connection, List<Record> buffer) {
            PreparedStatement preparedStatement = null;
            try {
                connection.setAutoCommit(true);
                preparedStatement = connection.prepareStatement(this.writeRecordSql);

                for (Record record : buffer) {
                    try {
                        preparedStatement = fillPreparedStatement(preparedStatement, record);
                        preparedStatement.execute();
                    } catch (SQLException e) {

                        if (TisAppLaunch.isTestMock()) {
                            // 试图找到哪一列有问题
                            final int allCols = this.columnNumber;
                            for (int tryColNumer = allCols; tryColNumer > 0; tryColNumer--) {
                                try {
                                    preparedStatement = connection
                                            .prepareStatement(this.writeRecordSql);
                                    preparedStatement = fillPreparedStatement(
                                            preparedStatement, record);
                                    for (int startNullIndex = (tryColNumer); startNullIndex < allCols; startNullIndex++) {
                                        preparedStatement.setNull(startNullIndex + 1, this.resultSetMetaData.get(startNullIndex).getLeft().getType().type);
                                    }
                                    preparedStatement.execute();
                                    LOG.info("success columnNumber:" + columnNumber + "," + this.resultSetMetaData.get(columnNumber).getLeft());
                                    break;
                                } catch (Throwable ex) {
                                    // ex.printStackTrace();
                                } finally {
                                    this.columnNumber--;
                                }
                            }
                        }
                        LOG.debug(e.toString());

                        this.taskPluginCollector.collectDirtyRecord(record, e);
                    } finally {
                        // 最后不要忘了关闭 preparedStatement
                        preparedStatement.clearParameters();
                    }
                }
            } catch (Exception e) {
                throw DataXException.asDataXException(
                        DBUtilErrorCode.WRITE_DATA_ERROR, e);
            } finally {
                DBUtil.closeDBResources(preparedStatement, null);
            }
        }

        // 直接使用了两个类变量：columnNumber,resultSetMetaData
        protected PreparedStatement fillPreparedStatement(PreparedStatement preparedStatement, Record record)
                throws SQLException {
            Pair<ColumnMetaData, IStatementSetter> col = null;
            // DataType type = null;
            for (int i = 0; i < this.columnNumber; i++) {
                col = this.resultSetMetaData.get(i);
                //  type = col.getType();
//                int columnSqltype = this.resultSetMetaData.getMiddle().get(i);
//                String typeName = this.resultSetMetaData.getRight().get(i);
                preparedStatement = fillPreparedStatementColumnType(preparedStatement, col.getRight(), i, col.getKey(), record.getColumn(i));
            }

            return preparedStatement;
        }

//        protected PreparedStatement fillPreparedStatementColumnType(PreparedStatement preparedStatement, IStatementSetter col, ColumnMetaData cm,
//                                                                    Column column) throws SQLException {
//            return fillPreparedStatementColumnType(preparedStatement, col, cm, column);
//        }

        protected PreparedStatement fillPreparedStatementColumnType(PreparedStatement preparedStatement, IStatementSetter col, int columnIndex,
                                                                    ColumnMetaData cm, Column column) throws SQLException {

            if (column.getRawData() == null) {
                preparedStatement.setNull(columnIndex + 1, cm.getType().type);
            } else {
                col.set(preparedStatement, columnIndex + 1, column);
            }
            return preparedStatement;

//            ColumnMetaData cm = this.resultSetMetaData.get(columnIndex);
//
//            java.util.Date utilDate;
//            switch (columnSqltype) {
//                case Types.CHAR:
//                case Types.NCHAR:
//                case Types.CLOB:
//                case Types.NCLOB:
//                case Types.VARCHAR:
//                case Types.LONGVARCHAR:
//                case Types.NVARCHAR:
//                case Types.LONGNVARCHAR:
//                    preparedStatement.setString(columnIndex + 1, column
//                            .asString());
//                    break;
//
//                case Types.SMALLINT:
//                case Types.INTEGER:
//                case Types.BIGINT:
//                case Types.NUMERIC:
//                case Types.DECIMAL:
//                case Types.FLOAT:
//                case Types.REAL:
//                case Types.DOUBLE:
//                    String strValue = column.asString();
//                    if (emptyAsNull && "".equals(strValue)) {
//                        preparedStatement.setString(columnIndex + 1, null);
//                    } else {
//                        preparedStatement.setString(columnIndex + 1, strValue);
//                    }
//                    break;
//
//                //tinyint is a little special in some database like mysql {boolean->tinyint(1)}
//                case Types.TINYINT:
//                    Long longValue = column.asLong();
//                    if (null == longValue) {
//                        preparedStatement.setString(columnIndex + 1, null);
//                    } else {
//                        preparedStatement.setString(columnIndex + 1, longValue.toString());
//                    }
//                    break;
//
//                // for mysql bug, see http://bugs.mysql.com/bug.php?id=35115
//                case Types.DATE:
//                    if (typeName == null) {
//                        typeName = cm.getType().typeName; //this.resultSetMetaData.getRight().get(columnIndex);
//                    }
//
//                    if (typeName.equalsIgnoreCase("year")) {
//                        if (column.asBigInteger() == null) {
//                            preparedStatement.setString(columnIndex + 1, null);
//                        } else {
//                            preparedStatement.setInt(columnIndex + 1, column.asBigInteger().intValue());
//                        }
//                    } else {
//                        java.sql.Date sqlDate = null;
//                        try {
//                            utilDate = column.asDate();
//                        } catch (DataXException e) {
//                            throw new SQLException(String.format(
//                                    "Date 类型转换错误：[%s]", column));
//                        }
//
//                        if (null != utilDate) {
//                            sqlDate = new java.sql.Date(utilDate.getTime());
//                        }
//                        preparedStatement.setDate(columnIndex + 1, sqlDate);
//                    }
//                    break;
//
//                case Types.TIME:
//                    java.sql.Time sqlTime = null;
//                    try {
//                        utilDate = column.asDate();
//                    } catch (DataXException e) {
//                        throw new SQLException(String.format(
//                                "TIME 类型转换错误：[%s]", column));
//                    }
//
//                    if (null != utilDate) {
//                        sqlTime = new java.sql.Time(utilDate.getTime());
//                    }
//                    preparedStatement.setTime(columnIndex + 1, sqlTime);
//                    break;
//
//                case Types.TIMESTAMP:
//                    java.sql.Timestamp sqlTimestamp = null;
//                    try {
//                        utilDate = column.asDate();
//                    } catch (DataXException e) {
//                        throw new SQLException(String.format(
//                                "TIMESTAMP 类型转换错误：[%s]", column));
//                    }
//
//                    if (null != utilDate) {
//                        sqlTimestamp = new java.sql.Timestamp(
//                                utilDate.getTime());
//                    }
//                    preparedStatement.setTimestamp(columnIndex + 1, sqlTimestamp);
//                    break;
//
//                case Types.BINARY:
//                case Types.VARBINARY:
//                case Types.BLOB:
//                case Types.LONGVARBINARY:
//                    preparedStatement.setBytes(columnIndex + 1, column
//                            .asBytes());
//                    break;
//
//                case Types.BOOLEAN:
//                    preparedStatement.setString(columnIndex + 1, column.asString());
//                    break;
//
//                // warn: bit(1) -> Types.BIT 可使用setBoolean
//                // warn: bit(>1) -> Types.VARBINARY 可使用setBytes
//                case Types.BIT:
//                    if (this.dataBaseType == DataBaseType.MySql) {
//                        preparedStatement.setBoolean(columnIndex + 1, column.asBoolean());
//                    } else {
//                        preparedStatement.setString(columnIndex + 1, column.asString());
//                    }
//                    break;
//                default:
//                    throw DataXException
//                            .asDataXException(
//                                    DBUtilErrorCode.UNSUPPORTED_TYPE,
//                                    String.format(
//                                            "您的配置文件中的列配置信息有误. 因为DataX 不支持数据库写入这种字段类型. 字段名:[%s], 字段类型:[%d], 字段Java类型:[%s]. 请修改表中该字段的类型或者不同步该字段.",
//                                            cm.getName(),
//                                            cm.getType().type,
//                                            cm.getType().typeName));
//            }
//            return preparedStatement;
        }

        private void calcWriteRecordSql() {
            if (!VALUE_HOLDER.equals(calcValueHolder(""))) {
                List<String> valueHolders = new ArrayList<String>(columnNumber);
                for (int i = 0; i < columns.size(); i++) {
                    String type = resultSetMetaData.get(i).getLeft().getType().typeName;
                    valueHolders.add(calcValueHolder(type));
                }

                boolean forceUseUpdate = false;
                //ob10的处理
                if (dataBaseType != null && dataBaseType == DataBaseType.MySql && OriginalConfPretreatmentUtil.isOB10(jdbcUrl)) {
                    forceUseUpdate = true;
                }

                INSERT_OR_REPLACE_TEMPLATE = WriterUtil.getWriteTemplate(columns, valueHolders, writeMode, dataBaseType, forceUseUpdate);
                writeRecordSql = String.format(INSERT_OR_REPLACE_TEMPLATE, this.table);
            }
        }

        protected String calcValueHolder(String columnType) {
            return VALUE_HOLDER;
        }
    }


}
