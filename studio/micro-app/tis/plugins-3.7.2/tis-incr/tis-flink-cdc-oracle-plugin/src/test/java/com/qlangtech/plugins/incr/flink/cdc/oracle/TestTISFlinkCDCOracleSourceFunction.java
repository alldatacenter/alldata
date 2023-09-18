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

package com.qlangtech.plugins.incr.flink.cdc.oracle;

import com.qlangtech.plugins.incr.flink.cdc.CDCTestSuitParams;
import com.qlangtech.plugins.incr.flink.cdc.CUDCDCTestSuit;
import com.qlangtech.plugins.incr.flink.cdc.oracle.utils.OracleTestUtils;
import com.qlangtech.plugins.incr.flink.junit.TISApplySkipFlinkClassloaderFactoryCreation;
import com.qlangtech.plugins.incr.flink.slf4j.TISLoggerConsumer;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import org.apache.flink.test.util.AbstractTestBase;
import org.junit.*;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.lifecycle.Startables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;


/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-11 11:24
 **/
public class TestTISFlinkCDCOracleSourceFunction extends AbstractTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(TestTISFlinkCDCOracleSourceFunction.class);

    private OracleContainer oracleContainer =
            OracleTestUtils.ORACLE_CONTAINER.withLogConsumer(new TISLoggerConsumer(LOG));
    @ClassRule(order = 100)
    public static TestRule name = new TISApplySkipFlinkClassloaderFactoryCreation();

    @Before
    public void setUp() throws Exception {
        CenterResource.setNotFetchFromCenterRepository();
        LOG.info("Starting containers...");
        Startables.deepStart(Stream.of(oracleContainer)).join();
        LOG.info("Containers are started.");
    }

    @After
    public void teardown() {
        oracleContainer.stop();
    }

    @Test
    public void testBinlogConsume() throws Exception {
        FlinkCDCOracleSourceFactory mysqlCDCFactory = new FlinkCDCOracleSourceFactory();
        mysqlCDCFactory.startupOptions = "latest";
        // debezium
        final String tabName = "DEBEZIUM.BASE";

        CDCTestSuitParams suitParams = CDCTestSuitParams.createBuilder().setTabName(tabName).build();

        CUDCDCTestSuit cdcTestSuit = new CUDCDCTestSuit(suitParams) {
            @Override
            protected BasicDataSourceFactory createDataSourceFactory(TargetResName dataxName,boolean useSplitTabStrategy) {
                return createMySqlDataSourceFactory(dataxName);
            }

            @Override
            protected void startProcessConn(DataSourceMeta.JDBCConnection conn) throws SQLException {
                conn.getConnection().setAutoCommit(false);
            }

            @Override
            protected int executePreparedStatement(Connection connection, PreparedStatement statement) throws SQLException {
                int count = super.executePreparedStatement(connection, statement);
                connection.commit();
                return count;
            }

            @Override
            protected int executeStatement(Connection connection, Statement statement, String sql) throws SQLException {
                int count = super.executeStatement(connection, statement, sql);
                connection.commit();
                return count;
            }
        };

        cdcTestSuit.startTest(mysqlCDCFactory);

    }


    protected BasicDataSourceFactory createMySqlDataSourceFactory(TargetResName dataxName) {
        Descriptor mySqlV5DataSourceFactory = TIS.get().getDescriptor("OracleDataSourceFactory");
        Assert.assertNotNull(mySqlV5DataSourceFactory);

        Descriptor.FormData formData = new Descriptor.FormData();
        formData.addProp("name", "oracle");
        formData.addProp("dbName", oracleContainer.getSid());
        formData.addProp("nodeDesc", oracleContainer.getHost());
//        formData.addProp("password", OracleTestUtils.ORACLE_PWD);
//        formData.addProp("userName", OracleTestUtils.ORACLE_USER);
        formData.addProp("password", oracleContainer.getPassword());
        formData.addProp("userName", oracleContainer.getUsername());
        formData.addProp("port", String.valueOf(oracleContainer.getOraclePort()));
        formData.addProp("allAuthorized", "true");
        formData.addProp("asServiceName", "false");

        Descriptor.ParseDescribable<BasicDataSourceFactory> parseDescribable
                = mySqlV5DataSourceFactory.newInstance(dataxName.getName(), formData);
        Assert.assertNotNull(parseDescribable.getInstance());

        return parseDescribable.getInstance();
    }
}
