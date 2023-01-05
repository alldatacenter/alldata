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

package com.qlangtech.plugins.incr.flink.chunjun.oracle.sink;

import com.google.common.collect.Lists;
import com.qlangtech.plugins.incr.flink.chunjun.doris.sink.TestFlinkSinkExecutor;
import com.qlangtech.tis.plugin.datax.DataXOracleWriter;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.oracle.OracleDSFactoryContainer;
import com.qlangtech.tis.plugin.ds.oracle.OracleDataSourceFactory;
import com.qlangtech.tis.plugin.ds.oracle.TISOracleContainer;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.UpdateMode;
import com.qlangtech.tis.plugins.incr.flink.connector.impl.InsertType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-24 13:39
 **/
public class TestChunjunOracleSinkFactory extends TestFlinkSinkExecutor {

//    // docker run -d -p 1521:1521 -e ORACLE_PASSWORD=test -e ORACLE_DATABASE=tis gvenzl/oracle-xe:18.4.0-slim
//    public static final DockerImageName ORACLE_DOCKER_IMAGE_NAME = DockerImageName.parse(
//            "gvenzl/oracle-xe:18.4.0-slim"
//            // "registry.cn-hangzhou.aliyuncs.com/tis/oracle-xe:18.4.0-slim"
//    );

    public static BasicDataSourceFactory oracleDS;
    private static TISOracleContainer oracleContainer;


    @BeforeClass
    public static void initialize() {

        OracleDSFactoryContainer.initialize();
        oracleContainer = OracleDSFactoryContainer.oracleContainer;
        oracleDS = OracleDSFactoryContainer.oracleDS;
//        oracleContainer = new TISOracleContainer();
//        oracleContainer.usingSid();
//        oracleContainer.start();
//        oracleDS = new OracleDataSourceFactory();
//        oracleDS.userName = oracleContainer.getUsername();
//        oracleDS.password = oracleContainer.getPassword();
//        oracleDS.port = oracleContainer.getOraclePort();
//
//        //oracleDS.asServiceName = !oracleContainer.isUsingSid();
//
//        if (oracleContainer.isUsingSid()) {
//            SIDConnEntity sidConn = new SIDConnEntity();
//            sidConn.sid = oracleContainer.getSid();
//            oracleDS.connEntity = sidConn;
//        } else {
//            ServiceNameConnEntity serviceConn = new ServiceNameConnEntity();
//            serviceConn.serviceName = oracleContainer.getDatabaseName();
//            oracleDS.connEntity = serviceConn;
//        }
//
//        // oracleDS.dbName = oracleDS.asServiceName ? oracleContainer.getDatabaseName() : oracleContainer.getSid();
//        oracleDS.nodeDesc = oracleContainer.getHost();//.getJdbcUrl()
//
//        oracleDS.allAuthorized = true;
//        System.out.println(oracleContainer.getJdbcUrl());
//        System.out.println(oracleDS.toString());
//        final String testTabName = "testTab";
//        oracleDS.visitAllConnection((conn) -> {
//            try (Statement statement = conn.createStatement()) {
//                try (ResultSet resultSet = statement.executeQuery("select 1,sysdate from dual")) {
//                    Assert.assertTrue(resultSet.next());
//                    Assert.assertEquals(1, resultSet.getInt(1));
//                }
//                statement.execute("create table \"" + testTabName + "\"( U_ID integer ,birthday DATE ,update_time TIMESTAMP ,U_NAME varchar(20),CONSTRAINT testTab_pk PRIMARY KEY (U_ID))");
//            }
//
//            ResultSet tableRs = conn.getMetaData().getTables(null, null, testTabName, null);
//            // cataLog和schema需要为空，不然pg不能反射到表的存在
//            // ResultSet tableRs = dbConn.getMetaData().getTables(null, null, tableName, null);
//            if (!tableRs.next()) {
//                throw new ChunJunRuntimeException(String.format("table %s not found.", testTabName));
//            }
//            // conn.getMetaData().getTables()
//            List<ColumnMetaData> cols = oracleDS.getTableMetadata(conn, testTabName);
//            for (ColumnMetaData col : cols) {
//                System.out.println("key:" + col.getName() + ",type:" + col.getType());
//            }
//        });


    }

    @AfterClass
    public static void stop() {

        oracleContainer.close();
    }

    @Override
    protected UpdateMode createIncrMode() {
        InsertType insertType = new InsertType();
        // UpdateType updateMode = new UpdateType();
        //  updateMode.updateKey = Lists.newArrayList(colId, updateTime);
        return insertType;
    }

    @Override
    protected ArrayList<String> getUniqueKey() {
        return Lists.newArrayList(colId, updateTime);
    }

    @Override
    protected BasicDataSourceFactory getDsFactory() {
        return oracleDS;
    }

    @Test
    @Override
    public void testSinkSync() throws Exception {
        super.testSinkSync();
    }

    @Override
    protected ChunjunSinkFactory getSinkFactory() {
        return new ChunjunOracleSinkFactory();
    }

    @Override
    protected BasicDataXRdbmsWriter createDataXWriter() {
        DataXOracleWriter writer = new DataXOracleWriter() {
            @Override
            public OracleDataSourceFactory getDataSourceFactory() {
                return (OracleDataSourceFactory) oracleDS;
            }
        };
        return writer;
    }

}
