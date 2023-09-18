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

package com.qlangtech.tis.plugin.ds.oracle;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;

//import com.qlangtech.tis.plugin.ds.oracle.impl.SIDConnEntity;
//import com.qlangtech.tis.plugin.ds.oracle.impl.ServiceNameConnEntity;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-01 09:36
 **/
public class OracleDSFactoryContainer {
    public static final String drop_column_type_test = "drop_column_types";
    public static final String sqlfile_column_type_test = "column_type_test";

    public static final String tab_full_types = "full_types";

    public static final TargetResName dataName = new TargetResName("dataXName");

    // docker run -d -p 1521:1521 -e ORACLE_PASSWORD=test -e ORACLE_DATABASE=tis gvenzl/oracle-xe:18.4.0-slim
    public static final DockerImageName ORACLE_DOCKER_IMAGE_NAME = DockerImageName.parse(
            "gvenzl/oracle-xe:18.4.0-slim"
            // "registry.cn-hangzhou.aliyuncs.com/tis/oracle-xe:18.4.0-slim"
    );

    private static final Pattern COMMENT_PATTERN = Pattern.compile("^(.*)--.*$");

    public static BasicDataSourceFactory oracleDS;
    public static TISOracleContainer oracleContainer;

    private static BasicDataSourceFactory createOracleDataSourceFactory(TargetResName dataxName) {
        Assert.assertNotNull("oracleContainer can not be null", oracleContainer);
        Descriptor mySqlV5DataSourceFactory = TIS.get().getDescriptor("OracleDataSourceFactory");
        Assert.assertNotNull(mySqlV5DataSourceFactory);

        Descriptor.FormData formData = new Descriptor.FormData();
        formData.addProp("name", "oracle");
        // formData.addProp("dbName", oracleContainer.getDatabaseName());
        // formData.addProp("nodeDesc", mySqlContainer.getHost());

        if (oracleContainer.isUsingSid()) {
//            SIDConnEntity sidConn = new SIDConnEntity();
//            sidConn.sid = oracleContainer.getSid();
//            oracleDS.connEntity = sidConn;

            Descriptor.FormData subForm = new Descriptor.FormData();
            subForm.addProp("sid", oracleContainer.getSid());
            formData.addSubForm("connEntity", "com.qlangtech.tis.plugin.ds.oracle.impl.SIDConnEntity", subForm);

        } else {
//            ServiceNameConnEntity serviceConn = new ServiceNameConnEntity();
//            serviceConn.serviceName = oracleContainer.getDatabaseName();
//            oracleDS.connEntity = serviceConn;

            Descriptor.FormData subForm = new Descriptor.FormData();
            subForm.addProp("serviceName", oracleContainer.getDatabaseName());
            formData.addSubForm("connEntity", "com.qlangtech.tis.plugin.ds.oracle.impl.ServiceNameConnEntity", subForm);
        }

        formData.addProp("nodeDesc", oracleContainer.getHost());

        formData.addProp("password", oracleContainer.getPassword());
        formData.addProp("userName", oracleContainer.getUsername());
        formData.addProp("port", String.valueOf(oracleContainer.getOraclePort()));
     //   formData.addProp("allAuthorized", "true");

        formData.addSubForm("allAuthorized"
                , "com.qlangtech.tis.plugin.ds.oracle.auth.NoneAuth", new Descriptor.FormData());



//        formData.addProp("encode", "utf8");
//        formData.addProp("useCompression", "true");

        Descriptor.ParseDescribable<DataSourceFactory> parseDescribable
                = mySqlV5DataSourceFactory.newInstance(dataxName.getName(), formData);
        Assert.assertNotNull(parseDescribable.getInstance());

        return parseDescribable.getInstance();
    }

    public static final String testTabName = "testTab";

    //@BeforeClass
    public static DataSourceFactory initialize(boolean inSink) {
        oracleContainer = new TISOracleContainer();
        oracleContainer.usingSid();
        oracleContainer.start();
        oracleDS = createOracleDataSourceFactory(dataName);
        System.out.println(oracleContainer.getJdbcUrl());
        System.out.println(oracleDS.toString());

        oracleDS.visitAllConnection((c) -> {
            Connection conn = c.getConnection();
            try (Statement statement = conn.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("select 1,sysdate from dual")) {
                    Assert.assertTrue(resultSet.next());
                    Assert.assertEquals(1, resultSet.getInt(1));
                }
                statement.execute("create table \"" + testTabName + "\"( U_ID integer ,birthday DATE ,update_time TIMESTAMP ,U_NAME varchar(20),CONSTRAINT testTab_pk PRIMARY KEY (U_ID))");
            }

            ResultSet tableRs = conn.getMetaData().getTables(null, null, testTabName, null);
            // cataLog和schema需要为空，不然pg不能反射到表的存在
            // ResultSet tableRs = dbConn.getMetaData().getTables(null, null, tableName, null);
            if (!tableRs.next()) {
                throw new RuntimeException(String.format("table %s not found.", testTabName));
            }
            // conn.getMetaData().getTables()
            List<ColumnMetaData> cols = oracleDS.getTableMetadata(c, inSink, EntityName.parse(testTabName));
            for (ColumnMetaData col : cols) {
                System.out.println("key:" + col.getName() + ",type:" + col.getType());
            }

            // 创建新用户
            //try (Statement statement = conn.createStatement()) {
            //  statement.execute("CREATE TABLESPACE tbs_perm_01   DATAFILE 'tbs_perm_01.dat' SIZE 20M  ONLINE;");
            // statement.execute("create user \"baisui\" identified by 123456");
            // statement.execute(" grant connect,resource,dba to \"baisui\"");
            //  statement.execute("create table \"baisui\".\"" + testTabName + "\"( U_ID integer ,birthday DATE ,update_time TIMESTAMP ,U_NAME varchar(20),CONSTRAINT testTab_pk PRIMARY KEY (U_ID))");
            // }

        });
        return oracleDS;
    }


    /**
     * Executes a JDBC statement using the default jdbc config without autocommitting the
     * connection.
     */
    public static void initializeOracleTable(String... sqlFile) {
        final List<URL> ddlTestFile = Lists.newArrayList();
        for (String f : sqlFile) {
            final String ddlFile = String.format("ddl/%s.sql", f);
            final URL ddFile = OracleDSFactoryContainer.class.getClassLoader().getResource(ddlFile);
            assertNotNull("Cannot locate " + ddlFile, ddFile);
            ddlTestFile.add(ddFile);
        }

        oracleDS.visitAllConnection((c) -> {
            Connection connection = c.getConnection();
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

}
