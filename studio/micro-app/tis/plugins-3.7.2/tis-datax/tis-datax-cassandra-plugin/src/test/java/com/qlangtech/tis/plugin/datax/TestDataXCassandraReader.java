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

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.ds.cassandra.CassandraDatasourceFactory;
import com.qlangtech.tis.plugin.ds.cassandra.TestCassandraDatasourceFactory;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import com.qlangtech.tis.util.UploadPluginMeta;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXCassandraReader extends TestCase {
    public void testGetDftTemplate() {
        String dftTemplate = DataXCassandraReader.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {

        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXCassandraReader.class);
        assertTrue(extraProps.isPresent());
    }

    public void testDescriptorsJSONGenerate() {
        DataXCassandraReader reader = new DataXCassandraReader();
        DescriptorsJSON descJson = new DescriptorsJSON(reader.getDescriptor());
        //System.out.println(descJson.getDescriptorsJSON().toJSONString());

        JsonUtil.assertJSONEqual(DataXCassandraReader.class, "cassandra-datax-reader-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    assertEquals(m, e, a);
                });


        UploadPluginMeta pluginMeta
                = UploadPluginMeta.parse("dataxReader:require,targetDescriptorName_"+DataXCassandraReader.DATAX_NAME+",subFormFieldName_selectedTabs,dataxName_baisuitestTestcase");

        JSONObject subFormDescriptorsJSON = descJson.getDescriptorsJSON(pluginMeta.getSubFormFilter());

        JsonUtil.assertJSONEqual(DataXCassandraReader.class, "cassandra-datax-reader-selected-tabs-subform-descriptor.json"
                , subFormDescriptorsJSON, (m, e, a) -> {
                    assertEquals(m, e, a);
                });
    }


    public void testTemplateGenerate() throws Exception {

        CassandraDatasourceFactory dsFactory = TestCassandraDatasourceFactory.getDS();
        dsFactory.useSSL = true;
        String dataxName = "testDataX";
        String tabUserDtl = "user_dtl";
        DataXCassandraReader reader = new DataXCassandraReader() {
            @Override
            public CassandraDatasourceFactory getDataSourceFactory() {
                return dsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXCassandraReader.class;
            }
        };
        reader.template = DataXCassandraReader.getDftTemplate();
        reader.allowFiltering = true;
        reader.consistancyLevel = "QUORUM";


        SelectedTab selectedTab = new SelectedTab();
        selectedTab.setCols(Lists.newArrayList("col2", "col1", "col3"));
        selectedTab.setWhere("delete = 0");
        selectedTab.name = tabUserDtl;
        reader.setSelectedTabs(Collections.singletonList(selectedTab));

        ReaderTemplate.validateDataXReader("cassandra-datax-reader-assert.json", dataxName, reader);

        dsFactory.useSSL = null;
        reader.allowFiltering = null;
        reader.consistancyLevel = null;
        selectedTab.setWhere(null);
        ReaderTemplate.validateDataXReader("cassandra-datax-reader-assert-without-option.json", dataxName, reader);
    }
}
