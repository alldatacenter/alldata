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

import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.ds.tidb.GetColsMeta;
import com.qlangtech.tis.plugin.ds.tidb.TestTiKVDataSourceFactory;
import com.qlangtech.tis.plugin.ds.tidb.TiKVDataSourceFactory;
import com.qlangtech.tis.plugin.test.BasicTest;

import java.io.File;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-05 20:48
 **/
public class TestDataXTiDBReader extends BasicTest {
    public void testGetDftTemplate() {
        String dftTemplate = DataXTiDBReader.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXTiDBReader.class);
        assertTrue(extraProps.isPresent());
    }

    public void testDescriptorsJSONGenerate() {
        PluginDesc.testDescGenerate(DataXTiDBReader.class, "tidb-datax-reader-descriptor.json");
    }

    public void testTemplateGenerate() throws Exception {

        final String dataXName = "dataXName";
        GetColsMeta getColsMeta = new GetColsMeta().invoke();
        final TiKVDataSourceFactory dsFactory = getColsMeta.getDataSourceFactory();
        DataXTiDBReader dataxReader = new DataXTiDBReader() {
            @Override
            public TiKVDataSourceFactory getDataSourceFactory() {
                return dsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXTiDBReader.class;
            }
        };

        dataxReader.template = DataXTiDBReader.getDftTemplate();

        dataxReader.setSelectedTabs(TestTiKVDataSourceFactory.createTabOfEmployees());

        ReaderTemplate.validateDataXReader("tidb-datax-reader-template-assert.json", dataXName, dataxReader);
    }


    public void testRealDumpEmployees() throws Exception {
        DataXTiDBReader dataxReader = new DataXTiDBReader();
        File cf = new File("tidb-datax-reader-content.txt");
        ReaderTemplate.realExecute("tidb-datax-reader-template-assert.json", cf, dataxReader);
    }

    public void testRealDumpTable1() throws Exception {
        DataXTiDBReader dataxReader = new DataXTiDBReader();
        File cf = new File("tidb-datax-reader-content.txt");
        ReaderTemplate.realExecute("tidb-datax-reader-cfg-tab1.json", cf, dataxReader);
    }
}
