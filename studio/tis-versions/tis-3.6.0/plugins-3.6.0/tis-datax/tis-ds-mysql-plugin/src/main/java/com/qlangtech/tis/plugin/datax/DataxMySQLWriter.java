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

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.plugin.ds.mysql.MySQLDataSourceFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * @author: baisui 百岁
 * @create: 2021-04-07 15:30
 * @see com.alibaba.datax.plugin.writer.mysqlwriter.TISMysqlWriter
 **/
@Public
public class DataxMySQLWriter extends BasicDataXRdbmsWriter {
    private static final String DATAX_NAME = "MySQL";

    @FormField(ordinal = 1, type = FormFieldType.ENUM, validate = {Validator.require})
    public String writeMode;

    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataxMySQLReader.class, "mysql-writer-tpl.json");
    }

//    @Override
//    public void initWriterTable(String targetTabName, List jdbcUrls) throws Exception {
//        InitWriterTable.process(this.dataXName, this, targetTabName, jdbcUrls);
//    }

    @Override
    public IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap) {
        if (!tableMap.isPresent()) {
            throw new IllegalArgumentException("param tableMap shall be present");
        }
        MySQLDataSourceFactory dsFactory = (MySQLDataSourceFactory) this.getDataSourceFactory();
        IDataxProcessor.TableMap tm = tableMap.get();
        if (CollectionUtils.isEmpty(tm.getSourceCols())) {
            throw new IllegalStateException("tablemap " + tm + " source cols can not be null");
        }
        TISTable table = new TISTable();
        table.setTableName(tm.getTo());
        DataDumpers dataDumpers = dsFactory.getDataDumpers(table);
        if (dataDumpers.splitCount > 1) {
            // 写入库还支持多组路由的方式分发，只能向一个目标库中写入
            throw new IllegalStateException("dbSplit can not max than 1");
        }
        MySQLWriterContext context = new MySQLWriterContext(this.dataXName);
        if (dataDumpers.dumpers.hasNext()) {
            IDataSourceDumper next = dataDumpers.dumpers.next();
            context.jdbcUrl = next.getDbHost();
            context.password = dsFactory.password;
            context.username = dsFactory.userName;
            context.tabName = table.getTableName();
            context.cols = IDataxProcessor.TabCols.create(tm);
            context.dbName = this.dbName;
            context.writeMode = this.writeMode;
            context.preSql = this.preSql;
            context.postSql = this.postSql;
            context.session = session;
            context.batchSize = batchSize;
            return context;
        }

        throw new RuntimeException("dbName:" + dbName + " relevant DS is empty");
    }

    @Override
    public CreateTableSqlBuilder.CreateDDL generateCreateDDL(IDataxProcessor.TableMap tableMapper) {
        if (!this.autoCreateTable) {
            return null;
        }
        StringBuffer script = new StringBuffer();
        DataxReader threadBingDataXReader = DataxReader.getThreadBingDataXReader();
        Objects.requireNonNull(threadBingDataXReader, "getThreadBingDataXReader can not be null");
        if (threadBingDataXReader instanceof DataxMySQLReader
                // 没有使用别名
                && tableMapper.hasNotUseAlias()) {
            DataxMySQLReader mySQLReader = (DataxMySQLReader) threadBingDataXReader;
            MySQLDataSourceFactory dsFactory = mySQLReader.getDataSourceFactory();
            dsFactory.visitFirstConnection((conn) -> {
                try (Statement statement = conn.createStatement()) {
                    try (ResultSet resultSet = statement.executeQuery("show create table " + tableMapper.getFrom())) {
                        if (!resultSet.next()) {
                            throw new IllegalStateException("table:" + tableMapper.getFrom() + " can not exec show create table script");
                        }
                        String ddl = resultSet.getString(2);
                        script.append(ddl);
                    }
                }
            });
            return new CreateTableSqlBuilder.CreateDDL(script, null) {
                @Override
                public String getSelectAllScript() {
                    //return super.getSelectAllScript();
                    throw new UnsupportedOperationException();
                }
            };
        }

        // ddl中timestamp字段个数不能大于1个要控制，第二个的时候要用datetime
        final AtomicInteger timestampCount = new AtomicInteger();

        final CreateTableSqlBuilder createTableSqlBuilder = new CreateTableSqlBuilder(tableMapper, this.getDataSourceFactory()) {
            @Override
            protected void appendExtraColDef(List<ColWrapper> pks) {
                if (!pks.isEmpty()) {
                    script.append(" , PRIMARY KEY (").append(pks.stream().map((pk) -> "`" + pk.getName() + "`")
                            .collect(Collectors.joining(","))).append(")").append("\n");
                }
            }

            @Override
            protected void appendTabMeta(List<ColWrapper> pks) {
                script.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci").append("\n");
            }

            @Override
            protected ColWrapper createColWrapper(CMeta c) {
                return new ColWrapper(c) {
                    @Override
                    public String getMapperType() {
                        return convertType(this.meta);
                    }
                };
            }

            //            @Override
//            protected String convertType(CMeta col) {
//                switch (col.getType()) {
//                    case Long:
//                        return "bigint(20)";
//                    case INT:
//                        return "int(11)";
//                    case Double:
//                        return "decimal(18,2)";
//                    case Date:
//                        return "date";
//                    case STRING:
//                    case Boolean:
//                    case Bytes:
//                    default:
//                        return "varchar(50)";
//                }
//            }

            /**
             * https://www.runoob.com/mysql/mysql-data-types.html
             * @param col
             * @return
             */
            private String convertType(CMeta col) {
                DataType type = col.getType();
                switch (type.type) {
                    case Types.CHAR: {
                        String keyChar = "CHAR";
                        if (type.columnSize < 1) {
                            return keyChar;
                        }
                        return keyChar + "(" + type.columnSize + ")";
                    }
                    case Types.BIT:
                    case Types.BOOLEAN:
                        return "BOOLEAN";
                    case Types.REAL:
                        return "REAL";
                    case Types.TINYINT: {
                        return "TINYINT(" + type.columnSize + ") " + type.getUnsignedToken();
                    }
                    case Types.SMALLINT: {
                        return "SMALLINT(" + type.columnSize + ") " + type.getUnsignedToken();
                    }
                    case Types.INTEGER:
                        return "int(11)";
                    case Types.BIGINT: {
//                        if (type.columnSize < 1) {
//                            throw new IllegalStateException("col:" + col.getName() + type + " colsize can not small than 1");
//                        }
                       // return "BIGINT(" + type.columnSize + ") " + type.getUnsignedToken();

                        return "BIGINT " + type.getUnsignedToken();
                    }
                    case Types.FLOAT:
                        return "FLOAT";
                    case Types.DOUBLE:
                        return "DOUBLE";
                    case Types.DECIMAL:
                    case Types.NUMERIC: {
                        if (type.columnSize > 0) {
                            return "DECIMAL(" + type.columnSize + "," + type.getDecimalDigits() + ")";
                        } else {
                            return "DECIMAL";
                        }
                    }
                    case Types.DATE:
                        return "DATE";
                    case Types.TIME:
                        return "TIME";
                    case Types.TIMESTAMP: {
                        if (timestampCount.getAndIncrement() < 1) {
                            return "TIMESTAMP";
                        } else {
                            return "DATETIME";
                        }
                    }
                    case Types.BLOB:
                    case Types.BINARY:
                    case Types.LONGVARBINARY:
                    case Types.VARBINARY:
                        return "BLOB";
                    case Types.VARCHAR: {
                        if (type.columnSize > Short.MAX_VALUE) {
                            return "TEXT";
                        }
                        return "VARCHAR(" + type.columnSize + ")";
                    }
                    default:
                        return "TINYTEXT";
                }
            }


        };
        return createTableSqlBuilder.build();
    }


    public static class MySQLWriterContext extends RdbmsDataxContext implements IDataxContext {

        public MySQLWriterContext(String dataXName) {
            super(dataXName);
        }

        private String dbName;
        private String writeMode;
        private String preSql;
        private String postSql;
        private String session;
        private Integer batchSize;

        public String getDbName() {
            return dbName;
        }

        public String getWriteMode() {
            return writeMode;
        }

        public String getPreSql() {
            return preSql;
        }

        public String getPostSql() {
            return postSql;
        }

        public String getSession() {
            return session;
        }

        public boolean isContainPreSql() {
            return StringUtils.isNotBlank(preSql);
        }

        public boolean isContainPostSql() {
            return StringUtils.isNotBlank(postSql);
        }

        public boolean isContainSession() {
            return StringUtils.isNotBlank(session);
        }

        public Integer getBatchSize() {
            return batchSize;
        }
    }


    @TISExtension()
    public static class DefaultDescriptor extends RdbmsWriterDescriptor //implements DataxWriter.IRewriteSuFormProperties
    {
        public DefaultDescriptor() {
            super();
        }


        @Override
        public boolean isSupportIncr() {
            return true;
        }

        @Override
        public boolean isSupportTabCreate() {
            return true;
        }

        @Override
        public IEndTypeGetter.EndType getEndType() {
            return EndType.MySQL;
        }

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }

//        @Override
//        public SuFormProperties overwriteSubPluginFormPropertyTypes(SuFormProperties subformProps) throws Exception {
//
//            final String targetClass = MySQLSelectedTab.class.getName();
//
//            Descriptor newSubDescriptor = Objects.requireNonNull(TIS.get().getDescriptor(targetClass)
//                    , "subForm clazz:" + targetClass + " can not find relevant Descriptor");
//
//            SuFormProperties rewriteSubFormProperties = SuFormProperties.copy(
//                    filterFieldProp(buildPropertyTypes(Optional.of(newSubDescriptor), MySQLSelectedTab.class))
//                    , MySQLSelectedTab.class
//                    , newSubDescriptor
//                    , subformProps);
//            return rewriteSubFormProperties;
//
//        }
    }
}
