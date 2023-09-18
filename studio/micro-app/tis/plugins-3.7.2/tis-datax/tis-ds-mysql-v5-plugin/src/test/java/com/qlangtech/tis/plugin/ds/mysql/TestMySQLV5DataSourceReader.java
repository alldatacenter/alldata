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

package com.qlangtech.tis.plugin.ds.mysql;

import com.alibaba.datax.plugin.rdbms.util.DBUtil;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-05-08 10:41
 **/
public class TestMySQLV5DataSourceReader extends TestCase {
    public void testReader() {
        MySQLV5DataSourceFactory dataSourceFactory = new MySQLV5DataSourceFactory();
        dataSourceFactory.useCompression = false;
        dataSourceFactory.password = "123456";
        dataSourceFactory.dbName = "item_center";
        dataSourceFactory.encode = "utf8";
        dataSourceFactory.port = 3306;
        dataSourceFactory.userName = "root";
        dataSourceFactory.nodeDesc = "192.168.28.200";


        IDataSourceFactoryGetter dsGetter = new IDataSourceFactoryGetter() {
            @Override
            public DataSourceFactory getDataSourceFactory() {
                return dataSourceFactory;
            }

            @Override
            public Integer getRowFetchSize() {
                return 2000;
            }
        };


        dataSourceFactory.visitFirstConnection((conn) -> {
            Connection connection = conn.getConnection();
//com.alibaba.datax.plugin.reader.mysqlreader.MysqlReader
            String querySql = "select * from item";

            Pair<Statement, ResultSet> query = DBUtil.query(connection, querySql, 2000, dsGetter);
            try {
                int all = 0;
                int count = 0;
                try (ResultSet rs = query.getRight()) {
                    while (rs.next()) {
                        count++;
                        all++;
                        if (count > 50000) {
                            count = 0;
                            System.out.println(all);
                        }
                    }
                }
            } finally {
                query.getLeft().close();
            }


        });

    }
}
