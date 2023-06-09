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

package com.qlangtech.tis.plugin.ds.tidb;

import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-23 19:16
 **/
public class GetColsMeta {
    private TiKVDataSourceFactory dataSourceFactory;
    private List<ColumnMetaData> employeesCols;

    public TiKVDataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    public List<ColumnMetaData> getEmployeesCols() {
        return employeesCols;
    }

    public GetColsMeta invoke() {
        return invoke(true);
    }

    public List<ColumnMetaData> getColsMeta(String tabName) {
        return dataSourceFactory.getTableMetadata( EntityName.parse(tabName));
    }

    public GetColsMeta invoke(boolean datetimeFormat) {
        dataSourceFactory = new TiKVDataSourceFactory();
        dataSourceFactory.dbName = TestTiKVDataSourceFactory.DB_NAME;
        dataSourceFactory.pdAddrs = "192.168.28.201:2399";
        //dataSourceFactory.datetimeFormat = datetimeFormat;
//        employeesCols = dataSourceFactory.getTableMetadata(TestTiKVDataSourceFactory.TABLE_NAME);
//        TestCase.assertNotNull(employeesCols);
//        TestCase.assertEquals(6, employeesCols.size());
        return this;
    }
}
