///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.qlangtech.plugins.incr.flink.cdc.mysql;
//
//import com.qlangtech.plugins.incr.flink.junit.TISApplySkipFlinkClassloaderFactoryCreation;
//import com.qlangtech.plugins.incr.flink.slf4j.TISLoggerConsumer;
//import com.ververica.cdc.connectors.mysql.source.MySqlSource;
//import com.ververica.cdc.connectors.mysql.testutils.MySqlContainer;
//import org.apache.flink.test.util.AbstractTestBase;
//import org.junit.BeforeClass;
//import org.junit.ClassRule;
//import org.junit.rules.TestRule;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.testcontainers.lifecycle.Startables;
//
//import java.util.stream.Stream;
//
///**
// * Basic class for testing {@link MySqlSource}.
// */
//public abstract class MySqlSourceTestBase extends AbstractTestBase {
//
//    private static final Logger LOG = LoggerFactory.getLogger(MySqlSourceTestBase.class);
//
//    @ClassRule(order = 100)
//    public static TestRule name = new TISApplySkipFlinkClassloaderFactoryCreation();
//
//    //protected static final int DEFAULT_PARALLELISM = 4;
//    protected static final MySqlContainer MYSQL_CONTAINER =
//            (MySqlContainer)
//                    new MySqlContainer()
//                            .withConfigurationOverride("docker/server-gtids/my.cnf")
//                            .withSetupSQL("docker/setup.sql")
//                            .withDatabaseName("flink-test")
//                            .withUsername("flinkuser")
//                            .withPassword("flinkpw")
//                            .withLogConsumer(new TISLoggerConsumer(LOG));
//
//    @BeforeClass
//    public static void startContainers() {
//        LOG.info("Starting containers...");
//        Startables.deepStart(Stream.of(MYSQL_CONTAINER)).join();
//        LOG.info("Containers are started.");
//    }
//
//
//}
