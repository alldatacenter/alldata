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

package com.qlangtech.tis.plugin.ds;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.db.parser.DBConfigParser;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-06 19:48
 **/
public abstract class BasicDataSourceFactory extends DataSourceFactory implements JdbcUrlBuilder {
    //
    public static final ZoneId DEFAULT_SERVER_TIME_ZONE = ZoneId.of("Asia/Shanghai");

    private static final Logger logger = LoggerFactory.getLogger(BasicDataSourceFactory.class);

    @FormField(identity = true, ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
    public String name;

    // 数据库名称
    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
    public String dbName;

    @FormField(ordinal = 5, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.user_name})
    public String userName;

    @FormField(ordinal = 7, type = FormFieldType.PASSWORD, validate = {Validator.none_blank})
    public String password;
    /**
     * 节点描述
     */
    @FormField(ordinal = 9, type = FormFieldType.TEXTAREA, validate = {Validator.require})
    public String nodeDesc;

    @FormField(ordinal = 11, type = FormFieldType.INT_NUMBER, validate = {Validator.require, Validator.integer})
    public int port;
    /**
     * 数据库编码
     */
    @FormField(ordinal = 13, type = FormFieldType.ENUM, validate = {Validator.require, Validator.identity})
    public String encode;
    /**
     * 附加参数
     */
    @FormField(ordinal = 11, type = FormFieldType.INPUTTEXT)
    public String extraParams;


    public String getUserName() {
        return this.userName;
    }

    public String getPassword() {
        return this.password;
    }

    @Override
    public String identityValue() {
        return this.name;
    }

    @Override
    public List<ColumnMetaData> getTableMetadata(final EntityName table) {
        if (table == null) {
            throw new IllegalArgumentException("param table can not be null");
        }
        List<ColumnMetaData> columns = new ArrayList<>();
        try {
            final DBConfig dbConfig = getDbConfig();
            dbConfig.vistDbName((config, ip, dbname) -> {
                columns.addAll(parseTableColMeta(table, config, ip, dbname));
                logger.info("tabmeta:{},colsSize:{},cols:{}"
                        , table
                        , columns.size()
                        , columns.stream().map((c) -> c.getName()).collect(Collectors.joining(",")));
                return true;
            });
            return columns;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ColumnMetaData> getTableMetadata(Connection conn, EntityName table) throws TableNotFoundException {
        try {
            return parseTableColMeta(conn, table);
        } catch (TableNotFoundException e) {
            throw e;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ColumnMetaData> parseTableColMeta(EntityName table, DBConfig config, String ip, String dbname) throws Exception {
        // List<ColumnMetaData> columns = Lists.newArrayList();
        String jdbcUrl = buidJdbcUrl(config, ip, dbname);

        return parseTableColMeta(table, jdbcUrl);
    }


    /**
     * 访问第一个JDBC Connection对象
     *
     * @param connProcessor
     */
    public final void visitFirstConnection(final IConnProcessor connProcessor) {
        this.visitConnection(connProcessor, false);
    }

    /**
     * 遍历所有conn
     *
     * @param connProcessor
     */
    public final void visitAllConnection(final IConnProcessor connProcessor) {
        this.visitConnection(connProcessor, true);
    }

    private final void visitConnection(final IConnProcessor connProcessor, final boolean visitAll) {
        try {
            final DBConfig dbConfig = getDbConfig();
            dbConfig.vistDbName((config, ip, databaseName) -> {
                visitConnection(config, ip, databaseName, connProcessor);
                return !visitAll;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public final List<String> getTablesInDB() {
        try {
            final List<String> tabs = new ArrayList<>();

            this.visitFirstConnection((conn) -> {
                refectTableInDB(tabs, conn);
            });

//            final DBConfig dbConfig = getDbConfig();
//            dbConfig.vistDbName((config, ip, databaseName) -> {
//                visitConnection(config, ip, databaseName, (conn) -> {
//                    refectTableInDB(tabs, conn);
//                });
//                return true;
//            });
            return tabs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Connection getConnection(String jdbcUrl) throws SQLException {
        return super.getConnection(jdbcUrl);
    }

    public void refectTableInDB(List<String> tabs, Connection conn) throws SQLException {
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            resultSet = statement.executeQuery(getRefectTablesSql());
            //   resultSet = statement.getResultSet();
            while (resultSet.next()) {
                tabs.add(resultSet.getString(1));
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
    }

    protected String getRefectTablesSql() {
        return "show tables";
    }

    public final DBConfig getDbConfig() {
        final DBConfig dbConfig = new DBConfig(this);
        dbConfig.setName(this.getDbName());
        dbConfig.setDbEnum(DBConfigParser.parseDBEnum(getDbName(), this.nodeDesc));
        return dbConfig;
    }


    public List<String> getJdbcUrls() {
        final DBConfig dbLinkMetaData = this.getDbConfig();
        List<String> jdbcUrls = Lists.newArrayList();
        dbLinkMetaData.vistDbURL(true, (dbName, dbHost, jdbcUrl) -> {
            jdbcUrls.add(jdbcUrl);
        }, false);
        return jdbcUrls;
    }

    @Override
    public DataDumpers getDataDumpers(TISTable table) {
        if (table == null) {
            throw new IllegalArgumentException("param table can not be null");
        }
        List<String> jdbcUrls = getJdbcUrls();

        return DataDumpers.create(jdbcUrls, table); // new DataDumpers(length, dsIt);
    }


    private void visitConnection(DBConfig db, String ip, String dbName
            , IConnProcessor p) throws Exception {
        if (db == null) {
            throw new IllegalStateException("param db can not be null");
        }
        if (StringUtils.isEmpty(ip)) {
            throw new IllegalArgumentException("param ip can not be null");
        }
        if (StringUtils.isEmpty(dbName)) {
            throw new IllegalArgumentException("param dbName can not be null");
        }

//        if (StringUtils.isEmpty(password)) {
//            throw new IllegalArgumentException("param password can not be null");
//        }
        if (p == null) {
            throw new IllegalArgumentException("param IConnProcessor can not be null");
        }
        //Connection conn = null;
        String jdbcUrl = buidJdbcUrl(db, ip, dbName);
        try {
            validateConnection(jdbcUrl, p);
        } catch (Exception e) {
            //MethodHandles.lookup().lookupClass()
            throw new TisException("请确认插件:" + this.getClass().getSimpleName() + "配置:" + this.identityValue() + ",jdbcUrl:" + jdbcUrl, e);
        }
    }


    @Override
    protected Class<BasicRdbmsDataSourceFactoryDescriptor> getExpectDesClass() {
        return BasicRdbmsDataSourceFactoryDescriptor.class;
    }

    public String getDbName() {
        return this.dbName;
    }

    public interface IConnProcessor {
        void vist(Connection conn) throws SQLException, TableNotFoundException;
    }

    public abstract static class BasicRdbmsDataSourceFactoryDescriptor extends BaseDataSourceFactoryDescriptor<BasicDataSourceFactory> {
        private static final Pattern urlParamsPattern = Pattern.compile("(\\w+?\\=\\w+?)(\\&\\w+?\\=\\w+?)*");


        public boolean validateExtraParams(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {

            Matcher matcher = urlParamsPattern.matcher(value);
            if (!matcher.matches()) {
                msgHandler.addFieldError(context, fieldName, "不符合格式：" + urlParamsPattern);
                return false;
            }
            return true;
        }
    }

    /**
     * 某些数据库是支持schema概念，如支持则实现该接口
     */
    @Public
    public interface ISchemaSupported {
        public String getDBSchema();
    }

    private static final Pattern COMMENT_PATTERN = Pattern.compile("^(.*)--.*$");

    /**
     * Executes a JDBC statement using the default jdbc config without autocommitting the
     * connection. <br/>
     * this method is for test
     */
    public void initializeDB(String... sqlFile) {
        if (sqlFile.length < 1) {
            throw new IllegalArgumentException("length of sqlFile length can not short than 1");
        }
        final List<URL> ddlTestFile = Lists.newArrayList();
        for (String f : sqlFile) {
            final String ddlFile = f;//String.format("ddl/%s.sql", f);
            final URL ddFile = BasicDataSourceFactory.class.getClassLoader().getResource(ddlFile);
            Objects.requireNonNull(ddFile, "Cannot locate " + ddlFile);
            ddlTestFile.add(ddFile);
        }

        this.visitAllConnection((connection) -> {
            for (URL ddl : ddlTestFile) {
                try (InputStream reader = ddl.openStream()) {
                    try (Statement statement = connection.createStatement()) {
                        final List<String> statements
                                = Arrays.stream(IOUtils.readLines(reader, TisUTF8.get()).stream().map(String::trim)
                                .filter(x -> !x.startsWith("--") && !x.isEmpty())
                                .map(
                                        x -> {
                                            final Matcher m =
                                                    COMMENT_PATTERN.matcher(x);
                                            return m.matches() ? m.group(1) : x;
                                        })
                                .collect(Collectors.joining("\n"))
                                .split(";"))
                                .collect(Collectors.toList());
                        for (String stmt : statements) {
                            statement.execute(stmt);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    public static void main(String[] args) {
        Matcher matcher = BasicRdbmsDataSourceFactoryDescriptor.urlParamsPattern.matcher("kkk=lll&bbb=lll");
        System.out.println(matcher.matches());
    }

}
