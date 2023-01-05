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

import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.mysql.MySQLDataSourceFactory;
import com.qlangtech.tis.plugin.test.BasicTest;
import com.ververica.cdc.connectors.mysql.testutils.MySqlContainer;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.lifecycle.Startables;

import java.io.File;
import java.util.stream.Stream;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-06 19:38
 **/
public class TestDataxMySQLReaderDump extends BasicTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected static final MySqlContainer MYSQL_CONTAINER =
            MySqlContainer.createMysqlContainer("/docker/server-gtids/my.cnf", "/docker/setup.sql");
    static BasicDataSourceFactory dsFactory;

    private static BasicDataSourceFactory createDataSource(TargetResName dataxName) {
        return MYSQL_CONTAINER.createMySqlDataSourceFactory(dataxName);
        //  return MySqlContainer.createMySqlDataSourceFactory(dataxName, MYSQL_CONTAINER);
    }

    @BeforeClass
    public static void initialize() {
        Startables.deepStart(Stream.of(MYSQL_CONTAINER)).join();
        dsFactory = createDataSource(new TargetResName(TestDataxMySQLReader.dataXName));
    }


    @Test
    public void testRealDump() throws Exception {
        File dataxReaderResult = folder.newFile("mysql-datax-reader-result.txt");
        DataxMySQLReader dataxReader = createReader(TestDataxMySQLReader.dataXName);
        DataxReader.dataxReaderGetter = (name) -> {
            assertEquals(TestDataxMySQLReader.dataXName, name);
            return dataxReader;
        };

        ReaderTemplate.realExecute("mysql-datax-reader-test-cfg.json", dataxReaderResult, dataxReader);
        System.out.println(FileUtils.readFileToString(dataxReaderResult, TisUTF8.get()));
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
