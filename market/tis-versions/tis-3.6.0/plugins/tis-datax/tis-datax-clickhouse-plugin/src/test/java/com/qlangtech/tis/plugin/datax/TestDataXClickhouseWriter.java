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

import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.common.WriterJson;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataXReaderColType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.plugin.ds.clickhouse.ClickHouseDataSourceFactory;
import org.apache.commons.io.FileUtils;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.easymock.EasyMock;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXClickhouseWriter extends com.qlangtech.tis.plugin.test.BasicTest {
    public void testGetDftTemplate() {
        String dftTemplate = DataXClickhouseWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXClickhouseWriter.class);
        assertTrue(extraProps.isPresent());
        assertEquals(9, extraProps.get().size());
    }

    private static final String testDataXName = "clickhouseWriter";
    private static final String clickhouse_datax_writer_assert_without_optional = "clickhouse-datax-writer-assert-without-optional.json";
    private static final String targetTableName = "customer_order_relation";

    public void testDescriptorsJSONGenerate() {
//        DataXClickhouseWriter writer = new DataXClickhouseWriter();
//        DescriptorsJSON descJson = new DescriptorsJSON(writer.getDescriptor());
//        //System.out.println(descJson.getDescriptorsJSON().toJSONString());
//
//        JsonUtil.assertJSONEqual(DataXClickhouseWriter.class, "clickhouse-datax-writer-descriptor.json"
//                , descJson.getDescriptorsJSON(), (m, e, a) -> {
//                    assertEquals(m, e, a);
//                });

        PluginDesc.testDescGenerate(DataXClickhouseWriter.class, "clickhouse-datax-writer-descriptor.json");
    }

    public void testConfigGenerate() throws Exception {
//        String dbName = "tis";
//        ClickHouseDataSourceFactory dsFactory = new ClickHouseDataSourceFactory();
//        dsFactory.nodeDesc = "192.168.28.201";
//        dsFactory.password = "123456";
//        dsFactory.userName = "default";
//        dsFactory.dbName = dbName;
//        dsFactory.port = 8123;
//        dsFactory.name = dbName;
//        IDataxProcessor.TableMap tableMap = new IDataxProcessor.TableMap();
//        tableMap.setFrom("application");
//        tableMap.setTo("customer_order_relation");
//
//        CMeta cm = null;
//        List<CMeta> cmetas = Lists.newArrayList();
//        cm = new CMeta();
//        cm.setName("customerregister_id");
//        cm.setType(DataXReaderColType.STRING);
//        cmetas.add(cm);
//
//        cm = new CMeta();
//        cm.setName("waitingorder_id");
//        cm.setType(DataXReaderColType.STRING);
//        cmetas.add(cm);
//
//        cm = new CMeta();
//        cm.setName("kind");
//        cm.setType(DataXReaderColType.INT);
//        cmetas.add(cm);
//
//        cm = new CMeta();
//        cm.setName("create_time");
//        cm.setType(DataXReaderColType.Long);
//        cmetas.add(cm);
//
//        cm = new CMeta();
//        cm.setName("last_ver");
//        cm.setType(DataXReaderColType.INT);
//        cmetas.add(cm);
//
//        tableMap.setSourceCols(cmetas);
        ClickHouseTest forTest = createDataXWriter(); //new DataXClickhouseWriter() {
//            @Override
//            public Class<?> getOwnerClass() {
//                return DataXClickhouseWriter.class;
//            }
//
//            @Override
//            public ClickHouseDataSourceFactory getDataSourceFactory() {
//                // return super.getDataSourceFactory();
//                return dsFactory;
//            }
//        };
//        writer.template = DataXClickhouseWriter.getDftTemplate();
//        writer.batchByteSize = 3456;
//        writer.batchSize = 9527;
//        writer.dbName = dbName;
//        writer.writeMode = "insert";
//        // writer.autoCreateTable = true;
//        writer.postSql = "drop table @table";
//        writer.preSql = "drop table @table";
//
//        writer.dataXName = testDataXName;
//        writer.dbName = dbName;

        WriterTemplate.valiateCfgGenerate("clickhouse-datax-writer-assert.json", forTest.writer, forTest.tableMap);

        forTest.writer.postSql = null;
        forTest.writer.preSql = null;
        forTest.writer.batchSize = null;
        forTest.writer.batchByteSize = null;
        forTest.writer.writeMode = null;


        WriterTemplate.valiateCfgGenerate(clickhouse_datax_writer_assert_without_optional, forTest.writer, forTest.tableMap);
    }

    private static class ClickHouseTest {
        final DataXClickhouseWriter writer;
        final IDataxProcessor.TableMap tableMap;

        public ClickHouseTest(DataXClickhouseWriter writer, IDataxProcessor.TableMap tableMap) {
            this.writer = writer;
            this.tableMap = tableMap;
        }
    }

    private static ClickHouseTest createDataXWriter() {

        String dbName = "tis";
        ClickHouseDataSourceFactory dsFactory = new ClickHouseDataSourceFactory();
        dsFactory.nodeDesc = "192.168.28.201";
        dsFactory.password = "123456";
        dsFactory.userName = "default";
        dsFactory.dbName = dbName;
        dsFactory.port = 8123;
        dsFactory.name = dbName;
        List<CMeta> cmetas = Lists.newArrayList();
        IDataxProcessor.TableMap tableMap = new IDataxProcessor.TableMap(cmetas);
        tableMap.setFrom("application");
        tableMap.setTo(targetTableName);

        CMeta cm = null;

        cm = new CMeta();
        cm.setPk(true);
        cm.setName("customerregister_id");
        cm.setType(DataXReaderColType.STRING.dataType);
        cmetas.add(cm);

        cm = new CMeta();
        cm.setName("waitingorder_id");
        cm.setType(DataXReaderColType.STRING.dataType);
        cmetas.add(cm);

        cm = new CMeta();
        cm.setName("kind");
        cm.setType(DataXReaderColType.INT.dataType);
        cmetas.add(cm);

        cm = new CMeta();
        cm.setName("create_time");
        cm.setType(DataXReaderColType.Long.dataType);
        cmetas.add(cm);

        cm = new CMeta();
        cm.setName("last_ver");
        cm.setType(DataXReaderColType.INT.dataType);
        cmetas.add(cm);

        //tableMap.setSourceCols(cmetas);
        DataXClickhouseWriter writer = new DataXClickhouseWriter() {
            @Override
            public Class<?> getOwnerClass() {
                return DataXClickhouseWriter.class;
            }

            @Override
            public ClickHouseDataSourceFactory getDataSourceFactory() {
                // return super.getDataSourceFactory();
                return dsFactory;
            }
        };
        writer.template = DataXClickhouseWriter.getDftTemplate();
        writer.batchByteSize = 3456;
        writer.batchSize = 9527;
        writer.dbName = dbName;
        writer.writeMode = "insert";
        // writer.autoCreateTable = true;
        writer.postSql = "drop table @table";
        writer.preSql = "drop table @table";

        writer.dataXName = testDataXName;
        writer.dbName = dbName;
        return new ClickHouseTest(writer, tableMap);
    }


    public void testRealDump() throws Exception {

        ClickHouseTest houseTest = createDataXWriter();

        houseTest.writer.autoCreateTable = true;

        DataxProcessor dataXProcessor = EasyMock.mock("dataXProcessor", DataxProcessor.class);
        File createDDLDir = new File(".");
        File createDDLFile = null;
        try {
            createDDLFile = new File(createDDLDir, targetTableName + IDataxProcessor.DATAX_CREATE_DDL_FILE_NAME_SUFFIX);
            FileUtils.write(createDDLFile
                    , com.qlangtech.tis.extension.impl.IOUtils.loadResourceFromClasspath(DataXClickhouseWriter.class
                            , "create_ddl_customer_order_relation.sql"), TisUTF8.get());

            EasyMock.expect(dataXProcessor.getDataxCreateDDLDir(null)).andReturn(createDDLDir);
            DataxWriter.dataxWriterGetter = (dataXName) -> {
                return houseTest.writer;
            };
            DataxProcessor.processorGetter = (dataXName) -> {
                assertEquals(testDataXName, dataXName);
                return dataXProcessor;
            };
            EasyMock.replay(dataXProcessor);
            DataXClickhouseWriter writer = new DataXClickhouseWriter();
            WriterTemplate.realExecuteDump( WriterJson.path( clickhouse_datax_writer_assert_without_optional), writer);

            EasyMock.verify(dataXProcessor);
        } finally {
            FileUtils.forceDelete(createDDLFile);
        }
    }

    public void testGenerateCreateDDL() {

        ClickHouseTest dataXWriter = createDataXWriter();

        CreateTableSqlBuilder.CreateDDL createDDL = dataXWriter.writer.generateCreateDDL(dataXWriter.tableMap);
        assertNull(createDDL);

        dataXWriter.writer.autoCreateTable = true;
        createDDL = dataXWriter.writer.generateCreateDDL(dataXWriter.tableMap);
        assertNotNull(createDDL);

        assertEquals("CREATE TABLE customer_order_relation\n" +
                "(\n" +
                "    `customerregister_id`   String,\n" +
                "    `waitingorder_id`       String,\n" +
                "    `kind`                  Int32,\n" +
                "    `create_time`           Int64,\n" +
                "    `last_ver`              Int32\n" +
                "   ,`__cc_ck_sign` Int8 DEFAULT 1\n" +
                ")\n" +
                " ENGINE = CollapsingMergeTree(__cc_ck_sign)\n" +
                " ORDER BY `customerregister_id`\n" +
                " SETTINGS index_granularity = 8192", String.valueOf(createDDL));

        System.out.println(createDDL);
    }

    public void testMySql2CKIncr() {

    }
}
