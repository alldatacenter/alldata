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

package com.qlangtech.plugins.incr.flink.chunjun.postgresql.sink;

import com.google.common.collect.Lists;
import com.qlangtech.plugins.incr.flink.chunjun.doris.sink.TestFlinkSinkExecutor;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.plugin.datax.DataXPostgresqlWriter;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.postgresql.PGDataSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.UpdateMode;
import com.qlangtech.tis.plugins.incr.flink.connector.impl.UpsertType;
import com.ververica.cdc.connectors.postgres.PostgresTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-22 20:35
 **/
public class TestChunjunPostgreSQLSinkFactory extends TestFlinkSinkExecutor {
    static BasicDataSourceFactory pgDSFactory;

    @BeforeClass
    public static void initialize() throws Exception {
        PostgresTestBase.startContainers();
        pgDSFactory = (BasicDataSourceFactory) PostgresTestBase.createPgSourceFactory(new TargetResName(dataXName));
    }

    @Override
    protected CMeta createUpdateTime() {
        CMeta cm = super.createUpdateTime();
        cm.setPk(false);
        return cm;
    }

    @Override
    protected BasicDataSourceFactory getDsFactory() {
        return pgDSFactory;
    }

    @Override
    protected UpdateMode createIncrMode() {
        // UpdateType updateMode = new UpdateType();

        UpsertType upsert = new UpsertType();
        return upsert;
        //  updateMode.updateKey = Lists.newArrayList(colId);
        // return updateMode;
    }

    @Override
    protected ArrayList<String> getUniqueKey() {
        return Lists.newArrayList(colId);
    }

    @Override
    protected ChunjunSinkFactory getSinkFactory() {
        return new ChunjunPostgreSQLSinkFactory();
    }

    @Override
    protected BasicDataXRdbmsWriter createDataXWriter() {

        DataXPostgresqlWriter pgDataXWriter = new DataXPostgresqlWriter() {
            @Override
            public PGDataSourceFactory getDataSourceFactory() {
                return (PGDataSourceFactory) pgDSFactory;
            }
        };
        pgDataXWriter.autoCreateTable = true;
        // pgDataXWriter.generateCreateDDL()
        return pgDataXWriter;
    }

    @Test
    @Override
    public void testSinkSync() throws Exception {
        super.testSinkSync();
    }
}
