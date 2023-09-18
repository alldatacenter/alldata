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

import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.datax.test.TestSelectedTabs;
import com.qlangtech.tis.plugin.ds.sqlserver.SqlServerDatasourceFactory;
import junit.framework.TestCase;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXSqlserverReader extends TestCase {
    public void testGetDftTemplate() {
        String dftTemplate = DataXSqlserverReader.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXSqlserverReader.class);
        assertTrue(extraProps.isPresent());
    }

    public void testDescGenerate() {

      //   com.qlangtech.tis.plugin.common.ContextDesc.descBuild(DataXSqlserverReader.class);

        PluginDesc.testDescGenerate(DataXSqlserverReader.class, "sqlserver-datax-reader-descriptor.json");
    }

    public void testTemplateGenerate() throws Exception {
        SqlServerDatasourceFactory dsFactory = TestDataXSqlserverWriter.getSqlServerDSFactory();
        DataXSqlserverReader reader = new DataXSqlserverReader() {
            @Override
            public SqlServerDatasourceFactory getDataSourceFactory() {
                return dsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXSqlserverReader.class;
            }
        };
        reader.splitPk = true;
        reader.fetchSize = 1024;
        reader.selectedTabs = TestSelectedTabs.createSelectedTabs(1);
        reader.template = DataXSqlserverReader.getDftTemplate();
        String dataXName = "dataXName";
        ReaderTemplate.validateDataXReader("sqlserver-datax-reader-assert.json", dataXName, reader);
    }

}
