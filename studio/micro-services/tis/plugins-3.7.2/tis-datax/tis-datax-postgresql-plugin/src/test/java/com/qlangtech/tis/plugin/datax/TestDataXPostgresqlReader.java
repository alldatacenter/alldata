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
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.datax.test.TestSelectedTabs;
import com.qlangtech.tis.plugin.ds.postgresql.PGDataSourceFactory;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXPostgresqlReader {
    @Test
    public void testGetDftTemplate() {
        String dftTemplate = DataXPostgresqlReader.getDftTemplate();
        Assert.assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    @Test
    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXPostgresqlReader.class);
        Assert.assertTrue(extraProps.isPresent());
    }

    @Test
    public void testDescriptorsJSONGenerate() {
        DataXPostgresqlReader esWriter = new DataXPostgresqlReader();
        DescriptorsJSON descJson = new DescriptorsJSON(esWriter.getDescriptor());

        JsonUtil.assertJSONEqual(DataXPostgresqlReader.class
                , "postgres-datax-reader-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    Assert.assertEquals(m, e, a);
                });
    }

    @Test
    public void testTemplateGenerate() throws Exception {

        PGDataSourceFactory dsFactory = createDataSource();

        DataXPostgresqlReader reader = new DataXPostgresqlReader() {
            @Override
            public PGDataSourceFactory getDataSourceFactory() {
                return dsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXPostgresqlReader.class;
            }
        };
        reader.setSelectedTabs(TestSelectedTabs.createSelectedTabs(1));
        reader.fetchSize = 333;
        reader.splitPk = true;
        reader.template = DataXPostgresqlReader.getDftTemplate();

        String dataXName = "dataxName";

        ReaderTemplate.validateDataXReader("postgres-datax-reader-assert.json", dataXName, reader);


        reader.fetchSize = null;
        reader.splitPk = null;

        ReaderTemplate.validateDataXReader("postgres-datax-reader-assert-without-option.json", dataXName, reader);

    }

    public static PGDataSourceFactory createDataSource() {
        PGDataSourceFactory dsFactory = new PGDataSourceFactory();
        dsFactory.dbName = "order1";
        dsFactory.password = "123455*^";
        dsFactory.userName = "admin";
        dsFactory.port = 5432;
        dsFactory.encode = "utf8";
        dsFactory.extraParams = "aa=bb&cc=xxx";
        dsFactory.nodeDesc = "192.168.28.201";
        return dsFactory;
    }
}
