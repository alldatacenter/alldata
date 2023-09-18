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
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.test.TestSelectedTabs;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
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
public class TestDataXPostgresqlWriter  //extends TestCase
{

    @Test
    public void testGetDftTemplate() {
        String dftTemplate = DataXPostgresqlWriter.getDftTemplate();
        Assert.assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    @Test
    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXPostgresqlWriter.class);
        Assert.assertTrue(extraProps.isPresent());
    }

    @Test
    public void testDescriptorsJSONGenerate() {
        DataXPostgresqlWriter esWriter = new DataXPostgresqlWriter();
        DescriptorsJSON descJson = new DescriptorsJSON(esWriter.getDescriptor());

        JsonUtil.assertJSONEqual(DataXPostgresqlReader.class
                , "postgres-datax-writer-descriptor.json"
                , descJson.getDescriptorsJSON(), (m, e, a) -> {
                    Assert.assertEquals(m, e, a);
                });
    }


    @Test
    public void testTemplateGenerate() throws Exception {

        DataXPostgresqlWriter dataXWriter = getDataXPostgresqlWriter();

        dataXWriter.template = DataXPostgresqlWriter.getDftTemplate();

        dataXWriter.dbName = "order2";
        dataXWriter.postSql = "drop table @table";
        dataXWriter.preSql = "drop table @table";
        dataXWriter.batchSize = 967;
        //dataXWriter.session =""

        Optional<IDataxProcessor.TableMap> tableMapper = TestSelectedTabs.createTableMapper();

        WriterTemplate.valiateCfgGenerate("postgres-datax-writer-assert.json", dataXWriter, tableMapper.get());

        dataXWriter.postSql = null;
        dataXWriter.preSql = null;
        dataXWriter.batchSize = null;

        WriterTemplate.valiateCfgGenerate("postgres-datax-writer-assert-without-option.json", dataXWriter, tableMapper.get());
    }

    private DataXPostgresqlWriter getDataXPostgresqlWriter() {
        PGDataSourceFactory ds = TestDataXPostgresqlReader.createDataSource();
        return new DataXPostgresqlWriter() {
            @Override
            public PGDataSourceFactory getDataSourceFactory() {
                return ds;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXPostgresqlWriter.class;
            }
        };
    }


    @Test
    public void testAutoCreateDDL() {
        DataXPostgresqlWriter dataXPostgresqlWriter = getDataXPostgresqlWriter();
        dataXPostgresqlWriter.autoCreateTable = true;
        DataxWriter.BaseDataxWriterDescriptor writerDescriptor = dataXPostgresqlWriter.getWriterDescriptor();
        Assert.assertTrue("isSupportTabCreate", writerDescriptor.isSupportTabCreate());


        IDataxProcessor.TableMap tableMapper = WriterTemplate.createCustomer_order_relationTableMap();

        CreateTableSqlBuilder.CreateDDL createDDL = dataXPostgresqlWriter.generateCreateDDL(tableMapper);
        Assert.assertNotNull(createDDL);
        // 多主键
        Assert.assertEquals(IOUtils.loadResourceFromClasspath(TestDataXPostgresqlWriter.class, "multi-pks-create-ddl.txt"), createDDL.toString());

        Optional<CMeta> firstCustomerregisterId = tableMapper.getSourceCols().stream().filter((col) -> WriterTemplate.customerregisterId.equals(col.getName())).findFirst();
        Assert.assertTrue(firstCustomerregisterId.isPresent());
        firstCustomerregisterId.get().setPk(false);

        createDDL = dataXPostgresqlWriter.generateCreateDDL(tableMapper);
        Assert.assertNotNull(createDDL);
        // 单主键
        Assert.assertEquals(IOUtils.loadResourceFromClasspath(TestDataXPostgresqlWriter.class, "single-pks-create-ddl.txt"), createDDL.toString());
        //  System.out.println(createDDL.toString());
    }

}
