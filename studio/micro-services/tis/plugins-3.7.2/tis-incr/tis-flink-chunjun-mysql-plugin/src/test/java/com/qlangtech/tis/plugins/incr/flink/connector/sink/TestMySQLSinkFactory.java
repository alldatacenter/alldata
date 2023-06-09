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

package com.qlangtech.tis.plugins.incr.flink.connector.sink;

import com.dtstack.chunjun.conf.SyncConf;
import com.qlangtech.plugins.incr.flink.cdc.mysql.MySqlSourceTestBase;
import com.qlangtech.plugins.incr.flink.chunjun.doris.sink.TestFlinkSinkExecutor;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.datax.DataxMySQLWriter;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsWriter;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import com.ververica.cdc.connectors.mysql.testutils.MySqlContainer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-17 14:54
 * @see MySqlSourceTestBase
 **/
public class TestMySQLSinkFactory extends TestFlinkSinkExecutor {

    @Test
    public void testMySQLWrite() throws Exception {
        super.testSinkSync();
    }

    static BasicDataSourceFactory mysqlDSFactory;

    @Override
    protected DataxReader createDataxReader() {
        DataxReader dataxReader = super.createDataxReader();
        DataxReader.dataxReaderThreadLocal.set(dataxReader);
        return dataxReader;
    }

    @BeforeClass
    public static void initialize() throws Exception {
        MySqlSourceTestBase.startContainers();
        mysqlDSFactory = (BasicDataSourceFactory) MySqlContainer.MYSQL5_CONTAINER.createMySqlDataSourceFactory(new TargetResName(dataXName));
    }


    @Test
    public void testConfParse() {
        SyncConf syncConf = SyncConf.parseJob(IOUtils.loadResourceFromClasspath(MySQLSinkFactory.class, "mysql_mysql_batch.json"));
        Assert.assertNotNull(syncConf);
    }

    @Override
    protected CMeta createUpdateTime() {
        CMeta updateTime = super.createUpdateTime();
        updateTime.setPk(false);
        return updateTime;
    }

    @Override
    protected BasicDataSourceFactory getDsFactory() {
        return mysqlDSFactory;
    }

    @Override
    protected ChunjunSinkFactory getSinkFactory() {
        MySQLSinkFactory sinkFactory = new MySQLSinkFactory();
        return sinkFactory;
    }

    @Override
    protected BasicDataXRdbmsWriter createDataXWriter() {
        DataxMySQLWriter dataXWriter = new DataxMySQLWriter() {
            @Override
            public DataSourceFactory getDataSourceFactory() {
                return mysqlDSFactory;
            }
        };

        return dataXWriter;
    }
}
