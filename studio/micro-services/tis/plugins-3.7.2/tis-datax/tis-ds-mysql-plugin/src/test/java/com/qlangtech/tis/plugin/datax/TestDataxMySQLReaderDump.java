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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.datax.common.util.Configuration;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.DataXJobInfo;
import com.qlangtech.tis.datax.DataxExecutor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.DSKey;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.DataSourceFactoryPluginStore;
import com.qlangtech.tis.plugin.ds.mysql.MySQLDataSourceFactory;
import com.qlangtech.tis.plugin.ds.split.DefaultSplitTableStrategy;
import com.qlangtech.tis.plugin.ds.split.NoneSplitTableStrategy;
import com.ververica.cdc.connectors.mysql.testutils.MySqlContainer;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.lifecycle.Startables;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-06 19:38
 **/
public class TestDataxMySQLReaderDump {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    protected static final MySqlContainer MYSQL_CONTAINER =
            MySqlContainer.createMysqlContainer("/docker/server-gtids/my.cnf", "/docker/setup.sql");
    static BasicDataSourceFactory dsFactory;

    private static BasicDataSourceFactory createDataSource(TargetResName dataxName) {
        return (BasicDataSourceFactory) MYSQL_CONTAINER.createMySqlDataSourceFactory(dataxName);
    }

    @BeforeClass
    public static void initialize() {
        System.setProperty(Config.SYSTEM_KEY_LOGBACK_PATH_KEY, "logback-test.xml");
        CenterResource.setNotFetchFromCenterRepository();
        HttpUtils.addMockGlobalParametersConfig();

        Startables.deepStart(Stream.of(MYSQL_CONTAINER)).join();
        dsFactory = createDataSource(new TargetResName(TestDataxMySQLReader.dataXName));
    }


    @Test
    public void testRealDump() throws Exception {

        MySQLDataSourceFactory mysqlDs = (MySQLDataSourceFactory) dsFactory;
        mysqlDs.splitTableStrategy = new NoneSplitTableStrategy();

        TIS.dsFactoryPluginStoreGetter = (p) -> {
            DSKey key = new DSKey(TIS.DB_GROUP_NAME, p, DataSourceFactory.class);
            return new DataSourceFactoryPluginStore(key, false) {
                @Override
                public DataSourceFactory getPlugin() {
                    return mysqlDs;
                }
            };
        };
        File dataxReaderResult = folder.newFile("mysql-datax-reader-result.txt");
        DataxMySQLReader dataxReader = createReader(TestDataxMySQLReader.dataXName);
        DataxReader.dataxReaderGetter = (name) -> {
            Assert.assertEquals(TestDataxMySQLReader.dataXName, name);
            return dataxReader;
        };

        Configuration readerConf = IOUtils.loadResourceFromClasspath(
                dataxReader.getClass(), "mysql-datax-reader-test-cfg.json", true, (writerJsonInput) -> {
                    return Configuration.from(writerJsonInput);
                });
        readerConf.set("parameter.connection[0].jdbcUrl[0]", dsFactory.getJdbcUrls().get(0));
        readerConf.set(DataxExecutor.connectKeyParameter + "." + DataxUtils.DATASOURCE_FACTORY_IDENTITY, dsFactory.identityValue());
        ReaderTemplate.realExecute(TestDataxMySQLReader.dataXName, readerConf, dataxReaderResult, dataxReader);
        System.out.println(FileUtils.readFileToString(dataxReaderResult, TisUTF8.get()));
    }

    /**
     * 基于分表的数据导入方式
     *
     * @throws Exception
     */
    @Test
    public void testRealDumpWithSplitTabs() throws Exception {

        MySQLDataSourceFactory mysqlDs = (MySQLDataSourceFactory) dsFactory;
        mysqlDs.splitTableStrategy = new DefaultSplitTableStrategy();
        DataXJobInfo.parse("base_1.json/base_01,base_02");
        Assert.assertNotNull(DataXJobInfo.getCurrent());

        File dataxReaderResult = folder.newFile("mysql-datax-reader-result.txt");
        DataxMySQLReader dataxReader = createReader(TestDataxMySQLReader.dataXName);
        DataxReader.dataxReaderGetter = (name) -> {
            Assert.assertEquals(TestDataxMySQLReader.dataXName, name);
            return dataxReader;
        };

        Configuration conf = IOUtils.loadResourceFromClasspath(
                dataxReader.getClass(), "mysql-datax-reader-test-cfg-tab-split.json"
                , true, (writerJsonInput) -> {
                    return Configuration.from(writerJsonInput);
                });
        conf.set("parameter.connection[0].jdbcUrl[0]", dsFactory.getJdbcUrls().get(0));
        ReaderTemplate.realExecute(TestDataxMySQLReader.dataXName, conf, dataxReaderResult, dataxReader);
        System.out.println("content as below:\n");
        List<String> lines = FileUtils.readLines(dataxReaderResult, TisUTF8.get());
        Assert.assertEquals(String.join(",", lines), 2, lines.size());
//        System.out.println(FileUtils.readFileToString(dataxReaderResult, TisUTF8.get()));
//        StringU

        DataXJobInfo.parse("base_1.json/base_01");
        Assert.assertNotNull(DataXJobInfo.getCurrent());

        ReaderTemplate.realExecute(TestDataxMySQLReader.dataXName, conf, dataxReaderResult, dataxReader);
        lines = FileUtils.readLines(dataxReaderResult, TisUTF8.get());
        // System.out.println("content as below:\n");
        // System.out.println(FileUtils.readFileToString(dataxReaderResult, TisUTF8.get()));
        Assert.assertEquals(String.join(",", lines), 1, lines.size());
    }

    protected DataxMySQLReader createReader(String dataXName) {

        DataxMySQLReader dataxReader = new DataxMySQLReader() {
            @Override
            public Class<?> getOwnerClass() {
                return DataxMySQLReader.class;
            }

            @Override
            public MySQLDataSourceFactory getDataSourceFactory() {
                // return super.getDataSourceFactory();
                return (MySQLDataSourceFactory) dsFactory;
            }
        };
        dataxReader.fetchSize = 2000;
        dataxReader.dataXName = dataXName;
        dataxReader.template = DataxMySQLReader.getDftTemplate();

        return dataxReader;
    }
}
