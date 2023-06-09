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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.ESTableAlias;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.aliyun.UsernamePassword;
import com.qlangtech.tis.plugin.common.WriterJson;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.elastic.ElasticEndpoint;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataXReaderColType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.test.BasicTest;
import com.qlangtech.tis.solrdao.ISchema;
import com.qlangtech.tis.solrdao.SchemaMetaContent;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;

import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXElasticsearchWriter extends BasicTest {

    public void testDescriptorsJSONGenerate() {

        //  DataxReader dataxReader = EasyMock.createMock("dataxReader", DataxReader.class);
        //List<ISelectedTab> selectedTabs = createSelectedTabs(dataxReader);
        //EasyMock.expect(dataxReader.getSelectedTabs()).andReturn(selectedTabs);


        // EasyMock.replay(dataxReader);
        DataXElasticsearchWriter esWriter = new DataXElasticsearchWriter();
        DescriptorsJSON descJson = new DescriptorsJSON(esWriter.getDescriptor());
//        descJson.getDescriptorsJSON().toJSONString();

        JsonUtil.assertJSONEqual(DataXElasticsearchWriter.class, "es-datax-writer-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    assertEquals(m, e, a);
                });


        // EasyMock.verify(dataxReader);
    }

//    protected List<ISelectedTab> createSelectedTabs(DataxReader dataxReader) {
//        List<ISelectedTab> selectedTabs = Lists.newArrayList();
//        SelectedTab tab = new SelectedTab();
//        List<String> cols = Lists.newArrayList();
//        cols.add("app_id");
//        cols.add("project_name");
//        tab.setCols(cols);
//        List<CMeta> cols1 = tab.getCols();
//        cols1.get(0).setType(DataXReaderColType.INT);
//        cols1.get(1).setType(DataXReaderColType.STRING);
//        selectedTabs.add(tab);
//        return selectedTabs;
//    }

    public void testGetDftTemplate() {
        String dftTemplate = DataXElasticsearchWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXElasticsearchWriter.class);
        assertTrue(extraProps.isPresent());
    }

    public void testTemplateGenerate() throws Exception {

        DataxReader dataxReader = EasyMock.createMock("dataxReader", DataxReader.class);
//        List<ISelectedTab> selectedTabs = createSelectedTabs(dataxReader);
//        EasyMock.expect(dataxReader.getSelectedTabs()).andReturn(selectedTabs);

        final TestAliyunToken token = new TestAliyunToken("xxxxxxxxxxx", "accessKeykkkkkkkkkkkkkk");

        DataXElasticsearchWriter dataXWriter = new DataXElasticsearchWriter() {
            @Override
            public ElasticEndpoint getToken() {
                return token;
            }

            @Override
            public String getTemplate() {
                return DataXElasticsearchWriter.getDftTemplate();
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXElasticsearchWriter.class;
            }
        };
        EasyMock.replay(dataxReader);
        dataXWriter.endpoint = "aliyun-bj-endpoint";
        dataXWriter.alias = "application2";
        dataXWriter.index = "application";
        // dataXWriter.type = "specific_type";
        dataXWriter.cleanup = true;
        dataXWriter.batchSize = 9999;
        dataXWriter.trySize = 22;
        dataXWriter.timeout = 6666;
        dataXWriter.discovery = true;
        dataXWriter.compression = true;
        dataXWriter.multiThread = true;
        dataXWriter.ignoreParseError = true;
        dataXWriter.aliasMode = "append";
        dataXWriter.settings = "{\"index\" :{\"number_of_shards\": 1, \"number_of_replicas\": 0}}";
        dataXWriter.splitter = ",";
        dataXWriter.dynamic = true;


        ESTableAlias tableMap = new ESTableAlias();
        String esSchema = IOUtils.loadResourceFromClasspath(DataXElasticsearchWriter.class, "es-schema-content.json");
        tableMap.setSchemaContent(esSchema);


        WriterTemplate.valiateCfgGenerate("es-datax-writer-assert.json", dataXWriter, tableMap);


        token.authToken = null;
        // token.sccessKeySecret = null;

        WriterTemplate.valiateCfgGenerate("es-datax-writer-assert-without-option.json", dataXWriter, tableMap);


        EasyMock.verify(dataxReader);
    }

    public void testRealDump() throws Exception {
        DataXElasticsearchWriter dataXWriter = new DataXElasticsearchWriter();
        WriterTemplate.realExecuteDump(WriterJson.path("es-datax-writer-real-dump.json"), dataXWriter);
    }

    public void testMergeFromStupidModel() {
        DataXElasticsearchWriter dataXWriter = new DataXElasticsearchWriter();

        ESField field = null;
        ESSchema schema = new ESSchema();
        field = new ESField();
        field.setName("aaa");
        field.setType(EsTokenizerType.visualTypeMap.get("long"));
        field.setIndexed(true);
        field.setStored(true);
        field.setDocValue(true);
        schema.fields.add(field);

        field = new ESField();
        field.setName("bbb");
        field.setType(EsTokenizerType.visualTypeMap.get("text"));
        field.setTokenizerType(EsTokenizerType.NULL.getKey());
        field.setIndexed(true);
        field.setStored(true);
        field.setDocValue(true);
        schema.fields.add(field);

        JSONObject mergeTarget = JSON.parseObject("{\"column\":[]}");

        JSONObject expertContent = dataXWriter.mergeFromStupidModel(schema, mergeTarget);
        // System.out.println(JsonUtil.toString(expertContent));
        JsonUtil.assertJSONEqual(DataXElasticsearchWriter.class, "mergeFromStupidModel_assert.json"
                , expertContent, (m, e, a) -> {
                    assertEquals(m, e, a);
                });
    }

    public void testProjectionFromExpertModel() {
        DataXElasticsearchWriter dataXWriter = new DataXElasticsearchWriter();

        JSONObject body = new JSONObject();
        body.put("content", IOUtils.loadResourceFromClasspath(DataXElasticsearchWriter.class, "mergeFromStupidModel_assert.json"));

        ISchema schema = dataXWriter.projectionFromExpertModel(body);
        assertNotNull(schema);

        List<ESField> fields = schema.getSchemaFields();
        assertEquals(2, fields.size());

        ESField aaa = fields.get(0);
        assertEquals("aaa", aaa.getName());
        assertEquals(EsTokenizerType.visualTypeMap.get("long").type, aaa.getTisFieldTypeName());
        assertTrue(aaa.isDocValue());
        assertTrue(aaa.isIndexed());
        assertTrue(aaa.isStored());

        ESField bbb = fields.get(1);
        assertEquals("bbb", bbb.getName());
        assertEquals(DataXElasticsearchWriter.ES_TYPE_TEXT.getType(), bbb.getTisFieldTypeName());
        assertEquals(EsTokenizerType.NULL.getKey(), bbb.getTokenizerType());
        assertTrue(bbb.isDocValue());
        assertTrue(bbb.isIndexed());
        assertTrue(bbb.isStored());

        SchemaMetaContent schemaContent = new SchemaMetaContent();
        schemaContent.content = StringUtils.EMPTY.getBytes(TisUTF8.get());
        schemaContent.parseResult = schema;
        JSONObject schemaContentJson = schemaContent.toJSON();
        // System.out.println(JsonUtil.toString(schemaContentJson));
        JsonUtil.assertJSONEqual(DataXElasticsearchWriter.class, "projectionFromExpertModel_assert.json"
                , schemaContentJson, (m, e, a) -> {
                    assertEquals(m, e, a);
                });
    }

    public void testInitSchemaMetaContent() {

        DataXElasticsearchWriter dataXWriter = new DataXElasticsearchWriter();

        ISelectedTab selectedTab = EasyMock.createMock("selectedTab", ISelectedTab.class);
        // ISelectedTab
        List<CMeta> cols = Lists.newArrayList();
        CMeta col = null;
        col = new CMeta();
        col.setName(null);
        col.setType(DataXReaderColType.STRING.dataType);
        cols.add(col);
        col = new CMeta();
        col.setName(null);
        col.setType(DataXReaderColType.Long.dataType);
        cols.add(col);
        EasyMock.expect(selectedTab.getCols()).andReturn(cols);

        EasyMock.replay(selectedTab);

        SchemaMetaContent metaContent = dataXWriter.initSchemaMetaContent(selectedTab);

        // System.out.println(JsonUtil.toString(metaContent.toJSON()));
        JsonUtil.assertJSONEqual(DataXElasticsearchWriter.class, "initSchemaMetaContent_assert.json", metaContent.toJSON(), (m, e, a) -> {
            assertEquals(m, e, a);
        });
        EasyMock.verify(selectedTab);

    }


    private static class TestAliyunToken extends ElasticEndpoint {


        public TestAliyunToken(String userName, String password) {
            UsernamePassword auth = new UsernamePassword();
            auth.userName = userName;
            auth.password = password;
            this.authToken = auth;
        }

        @Override
        public String identityValue() {
            return null;
        }


        @Override
        public String getEndpoint() {
            return "http://oss-cn-beijing.aliyuncs.com";
        }
    }
}
