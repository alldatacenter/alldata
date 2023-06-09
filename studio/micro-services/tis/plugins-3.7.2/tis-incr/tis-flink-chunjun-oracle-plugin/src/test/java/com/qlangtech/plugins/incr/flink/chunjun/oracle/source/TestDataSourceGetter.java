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

package com.qlangtech.plugins.incr.flink.chunjun.oracle.source;

import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.plugin.datax.DataXOracleReader;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.oracle.OracleDataSourceFactory;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-27 12:14
 **/
public class TestDataSourceGetter {

    @Test
    public void testDataSourceGetter() {
        IDataxProcessor processor = DataxProcessor.load(null, "oracle_mysql");

        DataXOracleReader reader = (DataXOracleReader) processor.getReader(null);
        OracleDataSourceFactory dsFactory = reader.getDataSourceFactory();

        dsFactory.visitFirstConnection((conn) -> {
            try (Statement statment = conn.createStatement()) {
                try (ResultSet resultSet = statment.executeQuery("select count(1) from SYSTEM.DMP_USER_")) {
                    while (resultSet.next()) {
                        System.out.println(resultSet.getInt(1));
                    }
                }
            }
        });


        List<ColumnMetaData> dmp_user_ = dsFactory.getTableMetadata(false, EntityName.parse("DMP_USER_"));
        for (ColumnMetaData col : dmp_user_) {
            System.out.println(col.getName() + "->" + col.getType().toString());
        }
    }
}
