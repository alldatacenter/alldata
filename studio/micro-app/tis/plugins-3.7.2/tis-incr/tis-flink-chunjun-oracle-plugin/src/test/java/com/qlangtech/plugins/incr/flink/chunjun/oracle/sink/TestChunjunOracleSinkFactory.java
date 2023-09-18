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

        OracleDSFactoryContainer.initialize(false);
        oracleContainer = OracleDSFactoryContainer.oracleContainer;
        oracleDS = OracleDSFactoryContainer.oracleDS;
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
