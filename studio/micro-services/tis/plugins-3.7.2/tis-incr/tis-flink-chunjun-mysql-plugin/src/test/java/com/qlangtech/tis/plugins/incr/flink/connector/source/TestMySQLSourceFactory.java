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

package com.qlangtech.tis.plugins.incr.flink.connector.source;

import com.qlangtech.plugins.incr.flink.cdc.mysql.BasicMySQLCDCTest;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.TableInDB;
import com.ververica.cdc.connectors.mysql.testutils.MySqlContainer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-28 14:21
 **/
@RunWith(Parameterized.class)
public class TestMySQLSourceFactory extends BasicMySQLCDCTest {

    private final JdbcDatabaseContainer mySqlContainer;

    @Override
    protected MQListenerFactory createMySQLCDCFactory() {
        return new MySQLSourceFactory();
    }

    public TestMySQLSourceFactory(JdbcDatabaseContainer mySqlContainer) {
        this.mySqlContainer = mySqlContainer;
    }

    @Override
    protected JdbcDatabaseContainer getMysqlContainer() {
        return this.mySqlContainer;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][]{ //
                {MySqlContainer.MYSQL5_CONTAINER},
                // {MySQL8.createMySqlContainer()},
                // {MySqlContainer.MYSQL8_CONTAINER},
        };
    }


    @Test
    @Override
    public void testStuBinlogConsume() throws Exception {

        System.out.println(this.getClass().getResource("/org/testcontainers/containers/JdbcDatabaseContainer.class"));

        BasicDataSourceFactory dataSourceFactory = createDataSource(new TargetResName("x"));

        Assert.assertNotNull(dataSourceFactory);

        dataSourceFactory.visitFirstConnection((conn) -> {
            //  TableInDB.create();
            TableInDB tabs = dataSourceFactory.getTablesInDB();//.refectTableInDB(tabs, conn);

            System.out.println("refectTableInDB:" + tabs.getTabs().stream().collect(Collectors.joining(",")));
        });
    }

    @Test
    @Override
    public void testBinlogConsume() throws Exception {
        super.testBinlogConsume();
    }
}
