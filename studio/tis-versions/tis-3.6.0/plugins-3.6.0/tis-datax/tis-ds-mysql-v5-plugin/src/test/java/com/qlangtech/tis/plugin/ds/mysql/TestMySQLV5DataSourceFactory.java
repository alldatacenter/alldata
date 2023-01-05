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

import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.trigger.util.JsonUtil;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-12-30 10:56
 **/
public class TestMySQLV5DataSourceFactory extends TestCase {
    public void testGetTableMetadata() {
        MySQLV5DataSourceFactory dataSourceFactory = new MySQLV5DataSourceFactory();
        dataSourceFactory.useCompression = true;
        dataSourceFactory.password = "123456";
        dataSourceFactory.dbName = "order2";
        dataSourceFactory.encode = "utf8";
        dataSourceFactory.port = 3306;
        dataSourceFactory.userName = "root";
        dataSourceFactory.nodeDesc = "192.168.28.200";


        List<ColumnMetaData> baseColsMeta = dataSourceFactory.getTableMetadata( EntityName.parse( "base"));
        assertEquals(8, baseColsMeta.size());


        JsonUtil.assertJSONEqual(TestMySQLV5DataSourceFactory.class, "base-cols-meta.json"
                , JsonUtil.toString(Collections.singletonMap("cols", baseColsMeta)), (msg, e, a) -> {
                    assertEquals(msg, e, a);
                });
    }
}
