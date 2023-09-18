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
package com.qlangtech.tis.plugin.ds;

import com.qlangtech.tis.BasicTestCase;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.offline.DbScope;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-04-25 11:09
 */
public class TestDataSourceFactoryPluginStore extends BasicTestCase {

    private static final String DB_NAME = "order1";
    private static final String DB_EMPLOYEE_NAME = "employees";
    private static final String TABLE_NAME = "totalpayinfo";

    public void testLoadTableMeta() throws Exception {
        DataSourceFactory dbPluginStore = TIS.getDataBasePlugin(new PostedDSProp(DBIdentity.parseId(DB_NAME)));
        //dbPluginStore.getPlugin()
        //  assertNotNull("db:" + DB_NAME + " relevant plugin config", dbPluginStore.getPlugin());
        List<ColumnMetaData> cols = dbPluginStore.getTableMetadata(false, EntityName.parse(TABLE_NAME));
//        TISTable tab = dbPluginStore.loadTableMeta(TABLE_NAME);
//        assertNotNull(tab);
        assertEquals(5, cols.size());
    }


    public void testLoadFacadeTableMeta() throws Exception {
        DataSourceFactory employeesPluginStore
                = TIS.getDataBasePlugin(new PostedDSProp(DBIdentity.parseId(DB_EMPLOYEE_NAME), DbScope.FACADE));
        assertNotNull(employeesPluginStore);

        Class<?> aClass = Class.forName("com.qlangtech.tis.plugin.ds.DBConfig");
        System.out.println(aClass);

        // DataSourceFactory plugin = employeesPluginStore.getPlugin();
        assertNotNull(employeesPluginStore);
        // plugin.createFacadeDataSource();

    }
}
