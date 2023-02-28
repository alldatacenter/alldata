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

import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.ds.mangodb.MangoDBDataSourceFactory;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import junit.framework.TestCase;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXMongodbReader extends TestCase {
    public void testGetDftTemplate() {
        String dftTemplate = DataXMongodbReader.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXMongodbReader.class);
        assertTrue(extraProps.isPresent());
    }

    public void testDescriptorsJSONGenerate() {
        DataXMongodbReader reader = new DataXMongodbReader();
        DescriptorsJSON descJson = new DescriptorsJSON(reader.getDescriptor());

        JsonUtil.assertJSONEqual(DataXMongodbReader.class, "mongdodb-datax-reader-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    assertEquals(m, e, a);
                });

    }

    public void testTemplateGenerate() throws Exception {
        String dataXName = "testDataXName";
        MangoDBDataSourceFactory dsFactory = getDataSourceFactory();


        DataXMongodbReader reader = new DataXMongodbReader() {
            @Override
            public MangoDBDataSourceFactory getDsFactory() {
                return dsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXMongodbReader.class;
            }
        };
        reader.column = IOUtils.loadResourceFromClasspath(this.getClass(), "mongodb-reader-column.json");
        reader.query = "this is my query";
        reader.collectionName = "employee";
        reader.template = DataXMongodbReader.getDftTemplate();


        ReaderTemplate.validateDataXReader("mongodb-datax-reader-assert.json", dataXName, reader);

        reader.query = null;
        dsFactory.password = null;
        ReaderTemplate.validateDataXReader("mongodb-datax-reader-assert-without-option.json", dataXName, reader);
    }

    public static MangoDBDataSourceFactory getDataSourceFactory() {
        MangoDBDataSourceFactory dsFactory = new MangoDBDataSourceFactory();
        dsFactory.dbName = "order1";
        dsFactory.address = "192.168.28.200:27017;192.168.28.201:27017";
        dsFactory.password = "123456";
        dsFactory.username = "root";
        return dsFactory;
    }


}
