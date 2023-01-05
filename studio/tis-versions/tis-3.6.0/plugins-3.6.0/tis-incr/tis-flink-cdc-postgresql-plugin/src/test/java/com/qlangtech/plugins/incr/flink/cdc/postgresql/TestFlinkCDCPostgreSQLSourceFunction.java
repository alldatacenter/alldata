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

package com.qlangtech.plugins.incr.flink.cdc.postgresql;

import com.qlangtech.plugins.incr.flink.cdc.CDCTestSuitParams;
import com.qlangtech.plugins.incr.flink.cdc.CUDCDCTestSuit;
import com.qlangtech.plugins.incr.flink.junit.TISApplySkipFlinkClassloaderFactoryCreation;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-11-02 11:03
 **/
public class TestFlinkCDCPostgreSQLSourceFunction extends PostgresTestBase {
    private static final String schemaName = "tis";

    @ClassRule(order = 100)
    public static TestRule name = new TISApplySkipFlinkClassloaderFactoryCreation();

    @Before
    public void before() {
        this.initializePostgresTable("tis");
    }


    @Test
    public void testBinlogConsume() throws Exception {
        FlinkCDCPostreSQLSourceFactory pgCDCFactory = new FlinkCDCPostreSQLSourceFactory();

        final String tabName = "base";

        CDCTestSuitParams suitParam = CDCTestSuitParams.createBuilder().setTabName(tabName).build();

        CUDCDCTestSuit cdcTestSuit = new CUDCDCTestSuit(suitParam) {
            @Override
            protected BasicDataSourceFactory createDataSourceFactory(TargetResName dataxName) {
                return createPGDataSourceFactory(dataxName);
            }

            @Override
            protected int executePreparedStatement(Connection connection, PreparedStatement statement) throws SQLException {
                int updateCount = super.executePreparedStatement(connection, statement);
                connection.commit();
                return updateCount;
            }

            @Override
            protected String createTableName(String tabName) {
                return schemaName + "." + tabName;
            }

            @Override
            protected int executeStatement(Connection connection, java.sql.Statement statement, String sql) throws SQLException {
                int updateCount = super.executeStatement(connection, statement, sql);
                connection.commit();
                return updateCount;
            }

            @Override
            protected void startProcessConn(Connection conn) throws SQLException {
                super.startProcessConn(conn);
                conn.setAutoCommit(false);
            }
        };

        cdcTestSuit.startTest(pgCDCFactory);

    }


    protected BasicDataSourceFactory createPGDataSourceFactory(TargetResName dataxName) {
        Descriptor pgDataSourceFactory = TIS.get().getDescriptor("PGDataSourceFactory");
        Assert.assertNotNull(pgDataSourceFactory);

        Descriptor.FormData formData = new Descriptor.FormData();
        formData.addProp("name", "pg");
        formData.addProp("dbName", POSTGERS_CONTAINER.getDatabaseName());
        formData.addProp("nodeDesc", POSTGERS_CONTAINER.getContainerIpAddress());
        formData.addProp("password", POSTGERS_CONTAINER.getPassword());
        formData.addProp("userName", POSTGERS_CONTAINER.getUsername());
        formData.addProp("port", String.valueOf(POSTGERS_CONTAINER.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
        formData.addProp("tabSchema", schemaName);
        formData.addProp("encode", "utf8");
        //formData.addProp("useCompression", "true");

        Descriptor.ParseDescribable<BasicDataSourceFactory> parseDescribable
                = pgDataSourceFactory.newInstance(dataxName.getName(), formData);
        Assert.assertNotNull(parseDescribable.getInstance());

        return parseDescribable.getInstance();
    }

}
