/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.plugin.ds.clickhouse;

import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.ds.TableInDB;
import junit.framework.TestCase;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-18 09:34
 **/
public class TestClickHouseDataSourceFactory extends TestCase {

    public void testDescGenerate() {

        PluginDesc.testDescGenerate(ClickHouseDataSourceFactory.class, "ClickHouseDataSourceFactory-desc.json");
    }

    public void testGetTablesInDB() {
        ClickHouseDataSourceFactory dsFactory = new ClickHouseDataSourceFactory();
        dsFactory.nodeDesc = "192.168.28.201";
        dsFactory.port = 8123;
        dsFactory.dbName = "tis";
        dsFactory.name = "tis-clickhouse";
        dsFactory.userName = "default";
        dsFactory.password = "123456";

        TableInDB tablesInDB = dsFactory.getTablesInDB();
        assertFalse(tablesInDB.isEmpty());
    }

}
