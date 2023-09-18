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

import com.alibaba.datax.plugin.writer.hdfswriter.HdfsColMeta;
import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.common.WriterJson;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.test.TestSelectedTabs;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugin.ds.oracle.OracleDSFactoryContainer;
import com.qlangtech.tis.plugin.ds.oracle.OracleDataSourceFactory;
import com.qlangtech.tis.plugin.ds.oracle.TestOracleDataSourceFactory;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXOracleWriter {
    @Test
    public void testGetDftTemplate() {
        String dftTemplate = DataXOracleWriter.getDftTemplate();
        Assert.assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    @Test
    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXOracleWriter.class);
        Assert.assertTrue(extraProps.isPresent());
    }

    @Test
    public void testDescGenerate() throws Exception {

        PluginDesc.testDescGenerate(DataXOracleWriter.class, "oracle-datax-writer-descriptor.json");
    }

    @Test
    public void testTemplateGenerate() throws Exception {
        DataXOracleWriter writer = getOracleWriter();
        writer.template = DataXOracleWriter.getDftTemplate();
        writer.batchSize = 1235;
        writer.postSql = "drop table @table";
        writer.preSql = "drop table @table";
        writer.dbName = "testdb";
        writer.session = "[\n" +
                "            \"alter session set nls_date_format = 'dd.mm.yyyy hh24:mi:ss';\"\n" +
                "            \"alter session set NLS_LANG = 'AMERICAN';\"\n" +
                "]";

        Optional<IDataxProcessor.TableMap> tableMap = TestSelectedTabs.createTableMapper();


        WriterTemplate.valiateCfgGenerate("oracle-datax-writer-assert.json", writer, tableMap.get());

        writer.session = null;
        writer.preSql = null;
        writer.postSql = null;
        writer.batchSize = null;

        WriterTemplate.valiateCfgGenerate("oracle-datax-writer-assert-without-option.json", writer, tableMap.get());
    }

    private DataXOracleWriter getOracleWriter() {
        OracleDataSourceFactory dsFactory = TestOracleDataSourceFactory.createOracleDataSourceFactory(true);

        DataXOracleWriter writer = new DataXOracleWriter() {
            @Override
            public OracleDataSourceFactory getDataSourceFactory() {
                return dsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXOracleWriter.class;
            }
        };
        writer.autoCreateTable = true;
        return writer;
    }
//    @Test
//    public void test() {
//        String sql = "INSERT INTO \"full_types\" (\"id\",\"tiny_c\",\"tiny_un_c\",\"small_c\",\"small_un_c\",\"medium_c\",\"medium_un_c\",\"int_c\",\"int_un_c\",\"int11_c\",\"big_c\",\"big_un_c\",\"varchar_c\",\"char_c\",\"real_c\",\"float_c\",\"double_c\",\"decimal_c\",\"numeric_c\",\"big_decimal_c\",\"bit1_c\",\"tiny1_c\",\"boolean_c\",\"date_c\",\"time_c\",\"datetime3_c\",\"datetime6_c\",\"timestamp_c\",\"file_uuid\",\"bit_c\",\"text_c\",\"tiny_blob_c\",\"blob_c\",\"medium_blob_c\",\"long_blob_c\",\"year_c\",\"enum_c\",\"set_c\",\"json_c\") VALUES(:1 ,:2 ,:3 ,:4 ,:5 ,:6 ,:7 ,:8 ,:9 ,:10 ,:11 ,:12 ,:13 ,:14 ,:15 ,:16 ,:17 ,:18 ,:19 ,:20 ,:21 ,:22 ,:23 ,:24 ,:25 ,:26 ,:27 ,:28 ,:29 ,:30 ,:31 ,:32 ,:33 ,:34 ,:35 ,:36 ,:37 ,:38 ,:39 ), OriginalSql = INSERT INTO \"full_types\" (\"id\",\"tiny_c\",\"tiny_un_c\",\"small_c\",\"small_un_c\",\"medium_c\",\"medium_un_c\",\"int_c\",\"int_un_c\",\"int11_c\",\"big_c\",\"big_un_c\",\"varchar_c\",\"char_c\",\"real_c\",\"float_c\",\"double_c\",\"decimal_c\",\"numeric_c\",\"big_decimal_c\",\"bit1_c\",\"tiny1_c\",\"boolean_c\",\"date_c\",\"time_c\",\"datetime3_c\",\"datetime6_c\",\"timestamp_c\",\"file_uuid\",\"bit_c\",\"text_c\",\"tiny_blob_c\",\"blob_c\",\"medium_blob_c\",\"long_blob_c\",\"year_c\",\"enum_c\",\"set_c\",\"json_c\") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?), \n";
//        System.out.println(StringUtils.substring(sql, 547));
//    }

    @Test
    public void testRealDump() throws Exception {

        final String targetTableName = "customer_order_relation";
        String testDataXName = "mysql_oracle";

        final DataXOracleWriter writer = getOracleWriter();
        writer.dataXName = testDataXName;
        List<IColMetaGetter> colMetas = Lists.newArrayList();

//                "customerregister_id",
//                "waitingorder_id",
//                "kind",
//                "create_time",
//                "last_ver"
        // DataType
        HdfsColMeta cmeta = null;
        // String colName, Boolean nullable, Boolean pk, DataType dataType
        cmeta = new HdfsColMeta("customerregister_id", false
                , true, new DataType(Types.VARCHAR, "VARCHAR", 150));
        colMetas.add(cmeta);

        cmeta = new HdfsColMeta("waitingorder_id", false, true
                , new DataType(Types.VARCHAR, "VARCHAR", 150));
        colMetas.add(cmeta);

        cmeta = new HdfsColMeta("kind"
                , true, false, new DataType(Types.BIGINT));
        colMetas.add(cmeta);

        cmeta = new HdfsColMeta("create_time"
                , true, false, new DataType(Types.BIGINT));
        colMetas.add(cmeta);

        cmeta = new HdfsColMeta("last_ver"
                , true, false, new DataType(Types.BIGINT));
        colMetas.add(cmeta);

        IDataxProcessor.TableMap tabMap = IDataxProcessor.TableMap.create(targetTableName, colMetas);
        CreateTableSqlBuilder.CreateDDL ddl = writer.generateCreateDDL(tabMap);

//        CreateStarRocksWriter createDorisWriter = new CreateStarRocksWriter().invoke();
//        createDorisWriter.dsFactory.password = "";
//        // createDorisWriter.dsFactory.nodeDesc = "192.168.28.201";
//        createDorisWriter.dsFactory.nodeDesc = "localhost";
//
//        createDorisWriter.writer.autoCreateTable = true;

        DataxProcessor dataXProcessor = EasyMock.mock("dataXProcessor", DataxProcessor.class);
        File createDDLDir = new File(".");
        File createDDLFile = null;
        try {
            createDDLFile = new File(createDDLDir, targetTableName + IDataxProcessor.DATAX_CREATE_DDL_FILE_NAME_SUFFIX);
            FileUtils.write(createDDLFile, ddl.getDDLScript(), TisUTF8.get());

            EasyMock.expect(dataXProcessor.getDataxCreateDDLDir(null)).andReturn(createDDLDir);
            DataxWriter.dataxWriterGetter = (dataXName) -> {
                return writer;
            };
            DataxProcessor.processorGetter = (dataXName) -> {
                Assert.assertEquals(testDataXName, dataXName);
                return dataXProcessor;
            };
            EasyMock.replay(dataXProcessor);
            String[] jdbcUrl = new String[1];
            OracleDSFactoryContainer.oracleDS.getDbConfig().vistDbURL(false, (a, b, url) -> {
                jdbcUrl[0] = url;
            });
            WriterJson wjson = WriterJson.path("oracle_writer_real_dump.json");
            wjson.addCfgSetter((cfg) -> {
                cfg.set("parameter.connection[0].jdbcUrl", jdbcUrl[0]);
                return cfg;
            });
            WriterTemplate.realExecuteDump(wjson, writer);

            EasyMock.verify(dataXProcessor);
        } finally {
            FileUtils.deleteQuietly(createDDLFile);
        }
    }

}
