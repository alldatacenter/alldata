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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.test.TestSelectedTabs;
import com.qlangtech.tis.plugin.ds.sqlserver.SqlServerDatasourceFactory;
import junit.framework.TestCase;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXSqlserverWriter extends TestCase {
    public void testGetDftTemplate() {
        String dftTemplate = DataXSqlserverWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXSqlserverWriter.class);
        assertTrue(extraProps.isPresent());
    }


    public void testDescGenerate() {
        PluginDesc.testDescGenerate(DataXSqlserverWriter.class, "sqlserver-datax-writer-descriptor.json");
    }

    public void testTemplateGenerate() throws Exception {




        DataXSqlserverWriter writer = getDataXSqlserverWriter();

        Optional<IDataxProcessor.TableMap> tableMap = TestSelectedTabs.createTableMapper();

        WriterTemplate.valiateCfgGenerate("sqlserver-datax-writer-assert.json", writer, tableMap.get());
    }

    protected DataXSqlserverWriter getDataXSqlserverWriter() {
        SqlServerDatasourceFactory dsFactory = getSqlServerDSFactory();
        DataXSqlserverWriter writer = new DataXSqlserverWriter() {
//            @Override
//            protected SqlServerDatasourceFactory getDataSourceFactory() {
//                return dsFactory;
//            }

            @Override
            public SqlServerDatasourceFactory getDataSourceFactory() {
                return dsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXSqlserverWriter.class;
            }
        };
        writer.template = DataXSqlserverWriter.getDftTemplate();
        writer.batchSize = 1234;
        writer.postSql = "drop table @table";
        writer.preSql = "drop table @table";
        writer.dbName = "testdb";
        return writer;
    }

    public void testGenerateCreateDDL() {

        DataXSqlserverWriter writer = getDataXSqlserverWriter();
        Optional<IDataxProcessor.TableMap> tableMap = TestSelectedTabs.createTableMapper();
        CreateTableSqlBuilder.CreateDDL createDDL = writer.generateCreateDDL(tableMap.get());
        assertNull(createDDL);

        writer.autoCreateTable = true;
        createDDL = writer.generateCreateDDL(tableMap.get());
        assertNotNull(createDDL);

        assertEquals("CREATE TABLE orderinfo_new\n" +
                "(\n" +
                "    \"col1\"   varchar(100),\n" +
                "    \"col2\"   varchar(100),\n" +
                "    \"col3\"   varchar(100)\n" +
                ")\n", String.valueOf(createDDL));

        System.out.println(createDDL);
    }

    public static SqlServerDatasourceFactory getSqlServerDSFactory() {
        SqlServerDatasourceFactory dsFactory = new SqlServerDatasourceFactory();
        dsFactory.dbName = "tis";
        dsFactory.password = "Hello1234!";
        dsFactory.userName = "sa";
        dsFactory.port = 1433;
        dsFactory.nodeDesc = "192.168.28.201";
        return dsFactory;
    }
}
