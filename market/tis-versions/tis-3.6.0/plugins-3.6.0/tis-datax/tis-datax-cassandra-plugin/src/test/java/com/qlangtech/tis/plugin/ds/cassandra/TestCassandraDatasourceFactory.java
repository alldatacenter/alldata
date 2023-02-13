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

package com.qlangtech.tis.plugin.ds.cassandra;

import com.google.common.collect.Sets;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import junit.framework.TestCase;

import java.util.List;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-21 13:14
 **/
public class TestCassandraDatasourceFactory extends TestCase {

    public void testGetTablesInDB() {
        CassandraDatasourceFactory dsFactory = getDS();
        // dsFactory.useSSL = true;

        List<String> tablesInDB = dsFactory.getTablesInDB();
        assertTrue(tablesInDB.contains("user_dtl"));
    }

    public static CassandraDatasourceFactory getDS() {
        CassandraDatasourceFactory dsFactory = new CassandraDatasourceFactory();
        dsFactory.dbName = "test_ks";
        dsFactory.nodeDesc = "192.168.28.201";
        dsFactory.password = "cassandra";
        dsFactory.userName = "cassandra";
        dsFactory.port = 9042;
        return dsFactory;
    }

    public void testGetTableMetadata() {
        Set<String> keys = Sets.newHashSet("city", "user_id", "user_name");
        CassandraDatasourceFactory ds = getDS();
        List<ColumnMetaData> colsMeta = ds.getTableMetadata(EntityName.parse("user_dtl"));
        assertEquals(3, colsMeta.size());
        for (ColumnMetaData col : colsMeta) {
            assertTrue(keys.contains(col.getKey()));
        }
    }
}
