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

package com.qlangtech.plugins.incr.flink.cdc.mysql;

import com.google.common.collect.ImmutableMap;
import com.qlangtech.plugins.incr.flink.cdc.CDCTestSuitParams;
import com.qlangtech.plugins.incr.flink.junit.TISApplySkipFlinkClassloaderFactoryCreation;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.testutils.MySQL8;
import com.ververica.cdc.connectors.mysql.testutils.MySqlContainer;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.test.util.AbstractTestBase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.lifecycle.Startables;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Basic class for testing {@link MySqlSource}.
 */
public abstract class MySqlSourceTestBase extends AbstractTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(MySqlSourceTestBase.class);

    @ClassRule(order = 100)
    public static TestRule name = new TISApplySkipFlinkClassloaderFactoryCreation();

    protected abstract JdbcDatabaseContainer getMysqlContainer();

    private BasicDataSourceFactory dsFactory;

    public BasicDataSourceFactory createDataSource(TargetResName dataxName) {
        if (this.dsFactory != null) {
            return this.dsFactory;
        }
        JdbcDatabaseContainer container = this.getMysqlContainer();
        Startables.deepStart(Stream.of(container)).join();

        if (container instanceof MySqlContainer) {
            return this.dsFactory = (BasicDataSourceFactory) ((MySqlContainer) container).createMySqlDataSourceFactory(dataxName);
        } else {
            this.dsFactory = (BasicDataSourceFactory) MySqlContainer.getBasicDataSourceFactory(dataxName, MySQL8.VERSION_8, container, false);
            this.dsFactory.initializeDB(StringUtils.substring(MySqlContainer.INITIAL_DB_SQL, 1));
            return dsFactory;
        }
    }

    public static String tabStu = "stu";
    public static String tabBase = "base";
    public static String fullTypes = "full_types";
    public static final String tabInstanceDetail = "instancedetail";

    public Map<String, CDCTestSuitParams> tabParamMap;

    @Before
    public void initializeTabParamMap() {

        ImmutableMap.Builder<String, CDCTestSuitParams> builder = ImmutableMap.builder();
        builder.put(tabStu, suitParamBuilder(tabStu) //.setIncrColumn("id")
                .setTabName(tabStu).build());

        builder.put(tabBase, suitParamBuilder(tabBase)
                //.setIncrColumn("update_time")
                .setTabName(tabBase) //
                .build());

        builder.put(tabInstanceDetail, suitParamBuilder(tabInstanceDetail)
                //.setIncrColumn("modify_time")
                .setTabName(tabInstanceDetail).build());
        builder.put(fullTypes
                , suitParamBuilder(fullTypes)
                        .setTabName(fullTypes).build());

        tabParamMap = builder.build();

    }

    protected abstract CDCTestSuitParams.Builder suitParamBuilder(String tableName);

    @BeforeClass
    public static void startContainers() {

    }


}
