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

package com.qlangtech.tis.plugin.datax.starrocks;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.common.WriterJson;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.test.TestSelectedTabs;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataXReaderColType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;

import com.qlangtech.tis.plugin.ds.starrocks.StarRocksSourceFactory;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import com.starrocks.connector.datax.plugin.writer.starrockswriter.row.StarRocksDelimiterParser;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-07 12:04
 **/
public class TestDataXStarRocksWriter extends TestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CenterResource.setNotFetchFromCenterRepository();
    }

    private static final String DataXName = "mysql_doris";

    public void testGetDftTemplate() {
        String dftTemplate = DataXStarRocksWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

//    public void test() {
//        System.out.println("xxx:"+StarRocksDelimiterParser.parse("\\x01", null).length());
//    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXStarRocksWriter.class);
        assertTrue(extraProps.isPresent());
        PluginExtraProps props = extraProps.get();
        PluginExtraProps.Props dbNameProp = props.getProp("dbName");
        assertNotNull(dbNameProp);
        JSONObject creator = dbNameProp.getProps().getJSONObject("creator");
        assertNotNull(creator);

    }

    public void testDescriptorsJSONGenerate() {
        DataxReader dataxReader = EasyMock.createMock("dataxReader", DataxReader.class);

        List<ISelectedTab> selectedTabs = TestSelectedTabs.createSelectedTabs(1)
                .stream().map((t) -> t).collect(Collectors.toList());

        for (ISelectedTab tab : selectedTabs) {
            for (CMeta cm : tab.getCols()) {
                cm.setType(DataXReaderColType.STRING.dataType);
            }
        }
        DataxReader.dataxReaderThreadLocal.set(dataxReader);
        EasyMock.replay(dataxReader);
        DataXStarRocksWriter writer = new DataXStarRocksWriter();
        DescriptorsJSON descJson = new DescriptorsJSON(writer.getDescriptor());

        JsonUtil.assertJSONEqual(DataXStarRocksWriter.class, "starrocks-datax-writer-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    assertEquals(m, e, a);
                });

        JsonUtil.assertJSONEqual(DataXStarRocksWriter.class, "starrocks-datax-writer-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    assertEquals(m, e, a);
                });
        EasyMock.verify(dataxReader);
    }

    public void testTemplateGenerate() throws Exception {


        CreateStarRocksWriter createDorisWriter = new CreateStarRocksWriter().invoke();
        StarRocksSourceFactory dsFactory = createDorisWriter.getDsFactory();
        DataXStarRocksWriter writer = createDorisWriter.getWriter();

        List<CMeta> sourceCols = Lists.newArrayList();
        CMeta col = new CMeta();
        col.setPk(true);
        col.setName("user_id");
        col.setType(DataXReaderColType.Long.dataType);
        sourceCols.add(col);

        col = new CMeta();
        col.setName("user_name");
        col.setType(DataXReaderColType.STRING.dataType);
        sourceCols.add(col);
        IDataxProcessor.TableMap tableMap = new IDataxProcessor.TableMap(sourceCols);
        tableMap.setFrom("application");
        tableMap.setTo("application");
        // tableMap.setSourceCols();

        WriterTemplate.valiateCfgGenerate(
                "starrocks-datax-writer-assert.json", writer, tableMap);

        dsFactory.password = null;
        writer.preSql = null;
        writer.postSql = null;
        writer.loadProps = null;

        writer.maxBatchRows = null;
        writer.batchSize = null;

        WriterTemplate.valiateCfgGenerate(
                "starrocks-datax-writer-assert-without-optional.json", writer, tableMap);


    }

    public void testRealDump() throws Exception {

        String targetTableName = "customer_order_relation";
        String testDataXName = "mysql_doris";

        CreateStarRocksWriter createDorisWriter = new CreateStarRocksWriter().invoke();
        createDorisWriter.dsFactory.password = "";
        // createDorisWriter.dsFactory.nodeDesc = "192.168.28.201";
        createDorisWriter.dsFactory.nodeDesc = "localhost";

        createDorisWriter.writer.autoCreateTable = true;

        DataxProcessor dataXProcessor = EasyMock.mock("dataXProcessor", DataxProcessor.class);
        File createDDLDir = new File(".");
        File createDDLFile = null;
        try {
            createDDLFile = new File(createDDLDir, targetTableName + IDataxProcessor.DATAX_CREATE_DDL_FILE_NAME_SUFFIX);
            FileUtils.write(createDDLFile
                    , com.qlangtech.tis.extension.impl.IOUtils.loadResourceFromClasspath(
                            DataXStarRocksWriter.class, "create_ddl_customer_order_relation.sql"), TisUTF8.get());

            EasyMock.expect(dataXProcessor.getDataxCreateDDLDir(null)).andReturn(createDDLDir);
            DataxWriter.dataxWriterGetter = (dataXName) -> {
                return createDorisWriter.writer;
            };
            DataxProcessor.processorGetter = (dataXName) -> {
                assertEquals(testDataXName, dataXName);
                return dataXProcessor;
            };
            EasyMock.replay(dataXProcessor);

            WriterTemplate.realExecuteDump(WriterJson.path("starrocks_writer_real_dump.json"), createDorisWriter.writer);

            EasyMock.verify(dataXProcessor);
        } finally {
            FileUtils.deleteQuietly(createDDLFile);
        }
    }

    public static StarRocksSourceFactory geSourceFactory() {
        StarRocksSourceFactory dataSourceFactory = new StarRocksSourceFactory() {
        };

        dataSourceFactory.dbName = "employees";
        dataSourceFactory.password = "123456";
        dataSourceFactory.userName = "root";
        dataSourceFactory.nodeDesc = "localhost";
        dataSourceFactory.port = 9030;
        dataSourceFactory.encode = "utf8";
        dataSourceFactory.loadUrl = "[\"localhost:8040\", \"localhost:8040\"]";
        return dataSourceFactory;
    }

    private class CreateStarRocksWriter {
        private StarRocksSourceFactory dsFactory;
        private DataXStarRocksWriter writer;

        public StarRocksSourceFactory getDsFactory() {
            return dsFactory;
        }

        public DataXStarRocksWriter getWriter() {
            return writer;
        }

        public CreateStarRocksWriter invoke() {
            dsFactory = geSourceFactory();
            writer = new DataXStarRocksWriter() {
                @Override
                public StarRocksSourceFactory getDataSourceFactory() {
                    return dsFactory;
                }

                @Override
                public Class<?> getOwnerClass() {
                    return DataXStarRocksWriter.class;
                }
            };
            writer.dataXName = DataXName;// .collectionName = "employee";

            //writer..column = IOUtils.loadResourceFromClasspath(this.getClass(), "mongodb-reader-column.json");
            writer.template = DataXStarRocksWriter.getDftTemplate();
            writer.dbName = "order1";
            writer.preSql = "drop table @table";
            writer.postSql = "drop table @table";
            writer.loadProps = "{\n" +
                    "    \"column_separator\": \"\\\\x01\",\n" +
                    "    \"row_delimiter\": \"\\\\x02\"\n" +
                    "}";

            writer.maxBatchRows = 999;
            writer.batchSize = 1001;
            return this;
        }
    }
}
