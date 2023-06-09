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

package com.ververica.cdc.connectors.mysql.testutils;

import com.google.common.collect.Lists;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.TableInDB;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.lifecycle.Startables;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-06 15:16
 **/
public class TestMySqlContainer {
    // private static Logger LOG = LoggerFactory.getLogger(TestMySqlContainer.class);
    protected static final MySqlContainer MYSQL_CONTAINER = MySqlContainer.MYSQL5_CONTAINER; //MySqlContainer.createMysqlContainer("docker/server-gtids/my.cnf", "docker/setup.sql");
//            (MySqlContainer)
//                    new MySqlContainer()
//                            .withConfigurationOverride("docker/server-gtids/my.cnf")
//                            .withSetupSQL("docker/setup.sql")
//                            .withDatabaseName("flink-test")
//                            .withUsername("flinkuser")
//                            .withPassword("flinkpw")
//                            .withLogConsumer(new TISLoggerConsumer(LOG));

    @Test
    public void testContainer() {
        Startables.deepStart(Stream.of(MYSQL_CONTAINER)).join();
        DataSourceFactory dsFactory = MYSQL_CONTAINER.createMySqlDataSourceFactory(new TargetResName("x"));// MySqlContainer.createMySqlDataSourceFactory(new TargetResName("x"), MYSQL_CONTAINER);
        dsFactory.visitFirstConnection((conn) -> {
            TableInDB tabs = dsFactory.getTablesInDB();//  TableInDB.create();
            // dsFactory.refectTableInDB(tabs);
            List<String> examples = Lists.newArrayList("base", "instancedetail", "stu");
            for (String tab : examples) {
                Assert.assertTrue("tab:" + tab, tabs.contains(tab));
            }

            System.out.println(tabs.getTabs().stream().collect(Collectors.joining(",")));
        });
    }
}
