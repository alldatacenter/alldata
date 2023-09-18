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

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.datax.*;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.extension.impl.RootFormProperties;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.datax.test.TestSelectedTabs;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.plugin.ds.mysql.MySQLDataSourceFactory;
import com.qlangtech.tis.plugin.test.BasicTest;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.IPluginContext;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author: baisui 百岁
 * @create: 2021-04-15 16:10
 **/
public class TestDataxMySQLWriter extends BasicTest {
    public static String mysqlJdbcUrl = "jdbc:mysql://192.168.28.200:3306/baisuitestWriterdb?useUnicode=yes&characterEncoding=utf8";
    public static String dbWriterName = "baisuitestWriterdb";

    public static String dataXName = "testDataXName";

    @Test
    public void testFieldCount() throws Exception {
        DataxMySQLWriter mySQLWriter = new DataxMySQLWriter();
        Descriptor<DataxWriter> descriptor = mySQLWriter.getDescriptor();
        PluginFormProperties pluginFormPropertyTypes = descriptor.getPluginFormPropertyTypes();

        Assert.assertTrue(pluginFormPropertyTypes instanceof RootFormProperties);
        Assert.assertEquals(8, pluginFormPropertyTypes.getKVTuples().size());

    }

    @Test
    public void testGenerateCreateDDL() {
        DataxMySQLWriter writer = new DataxMySQLWriter();
        writer.autoCreateTable = true;
        DataxReader.dataxReaderThreadLocal.set(new DataxReader() {
            @Override
            public <T extends ISelectedTab> List<T> getSelectedTabs() {
                return null;
            }

            @Override
            public IGroupChildTaskIterator getSubTasks(Predicate<ISelectedTab> filter) {
                return null;
            }

            @Override
            public String getTemplate() {
                return null;
            }
        });

        CreateTableSqlBuilder.CreateDDL ddl = writer.generateCreateDDL(getTabApplication((cols) -> {
            CMeta col = new CMeta();
            col.setPk(true);
            col.setName("id3");
            col.setType(DataXReaderColType.Long.dataType);
            cols.add(col);

            col = new CMeta();
            col.setName("col4");
            col.setType(DataXReaderColType.STRING.dataType);
            cols.add(col);

            col = new CMeta();
            col.setName("col5");
            col.setType(DataXReaderColType.STRING.dataType);
            cols.add(col);


            col = new CMeta();
            col.setPk(true);
            col.setName("col6");
            col.setType(DataXReaderColType.STRING.dataType);
            cols.add(col);
        }));

        Assert.assertNotNull(ddl);
        // System.out.println(ddl);

        Assert.assertEquals(
                StringUtils.trimToEmpty(IOUtils.loadResourceFromClasspath(DataxMySQLWriter.class, "create-application-ddl.sql"))
                , StringUtils.trimToEmpty(ddl.getDDLScript().toString()));
    }

    private IDataxProcessor.TableMap getTabApplication(
            Consumer<List<CMeta>>... colsProcess) {

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

        for (Consumer<List<CMeta>> p : colsProcess) {
            p.accept(sourceCols);
        }
        IDataxProcessor.TableMap tableMap = new IDataxProcessor.TableMap(sourceCols);
        tableMap.setFrom("application");
        tableMap.setTo("application_alias");
        //tableMap.setSourceCols(sourceCols);
        return tableMap;
    }

    @Test
    public void testTempateGenerate() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataxMySQLWriter.class);
        Assert.assertTrue("DataxMySQLWriter extraProps shall exist", extraProps.isPresent());
        IPluginContext pluginContext = EasyMock.createMock("pluginContext", IPluginContext.class);
        Context context = EasyMock.createMock("context", Context.class);
        EasyMock.expect(context.hasErrors()).andReturn(false);
        MySQLDataSourceFactory mysqlDs = new MySQLDataSourceFactory() {
            @Override
            public JDBCConnection getConnection(String jdbcUrl) throws SQLException {
                return null;
            }
        };

        mysqlDs.dbName = dbWriterName;
        mysqlDs.port = 3306;
        mysqlDs.encode = "utf8";
        mysqlDs.userName = "root";
        mysqlDs.password = "123456";
        mysqlDs.nodeDesc = "192.168.28.200";
        Descriptor.ParseDescribable<DataSourceFactory> desc = new Descriptor.ParseDescribable<>(mysqlDs);
        pluginContext.addDb(desc, dbWriterName, context, true);
        EasyMock.replay(pluginContext, context);

        DataSourceFactoryPluginStore dbStore = TIS.getDataSourceFactoryPluginStore( PostedDSProp.parse(dbWriterName));

        Assert.assertTrue("save mysql db Config faild"
                , dbStore.setPlugins(pluginContext, Optional.of(context), Collections.singletonList(desc)).success);


        DataxMySQLWriter mySQLWriter = new DataxMySQLWriter();
        mySQLWriter.dataXName = dataXName;
        mySQLWriter.writeMode = "replace";
        mySQLWriter.dbName = dbWriterName;
        mySQLWriter.template = DataxMySQLWriter.getDftTemplate();
        mySQLWriter.batchSize = 1001;
        mySQLWriter.preSql = "delete from test";
        mySQLWriter.postSql = "delete from test1";
        mySQLWriter.session = "set session sql_mode='ANSI'";
        validateConfigGenerate("mysql-datax-writer-assert.json", mySQLWriter);
        //  System.out.println(mySQLWriter.getTemplate());


        // 将非必须输入的值去掉再测试一遍
        mySQLWriter.batchSize = null;
        mySQLWriter.preSql = null;
        mySQLWriter.postSql = null;
        mySQLWriter.session = null;
        validateConfigGenerate("mysql-datax-writer-assert-without-option-val.json", mySQLWriter);

        mySQLWriter.preSql = " ";
        mySQLWriter.postSql = " ";
        mySQLWriter.session = " ";
        validateConfigGenerate("mysql-datax-writer-assert-without-option-val.json", mySQLWriter);


    }

    private void validateConfigGenerate(String assertFileName, DataxMySQLWriter mySQLWriter) throws IOException {

        Optional<IDataxProcessor.TableMap> tableMap = TestSelectedTabs.createTableMapper();
        IDataxContext subTaskCtx = mySQLWriter.getSubTask(tableMap);
        Assert.assertNotNull(subTaskCtx);

        RdbmsDataxContext mySQLDataxContext = (RdbmsDataxContext) subTaskCtx;
        Assert.assertEquals("\"`col1`\",\"`col2`\",\"`col3`\"", mySQLDataxContext.getColsQuotes());
        Assert.assertEquals(mysqlJdbcUrl, mySQLDataxContext.getJdbcUrl());
        Assert.assertEquals("123456", mySQLDataxContext.getPassword());
        Assert.assertEquals("orderinfo_new", mySQLDataxContext.tabName);
        Assert.assertEquals("root", mySQLDataxContext.getUsername());

        IDataxProcessor processor = EasyMock.mock("dataxProcessor", IDataxProcessor.class);
        IDataxGlobalCfg dataxGlobalCfg = EasyMock.mock("dataxGlobalCfg", IDataxGlobalCfg.class);

        IDataxReader dataxReader = EasyMock.mock("dataxReader", IDataxReader.class);

        EasyMock.expect(processor.getReader(null)).andReturn(dataxReader);
        EasyMock.expect(processor.getWriter(null)).andReturn(mySQLWriter);
        EasyMock.expect(processor.getDataXGlobalCfg()).andReturn(dataxGlobalCfg);
        EasyMock.replay(processor, dataxGlobalCfg, dataxReader);


        DataXCfgGenerator dataProcessor = new DataXCfgGenerator(null, "testDataXName", processor) {
        };

        String cfgResult = dataProcessor.generateDataxConfig(null, mySQLWriter, dataxReader, tableMap);

        JsonUtil.assertJSONEqual(this.getClass(), assertFileName, cfgResult, (m, e, a) -> {
            Assert.assertEquals(m, e, a);
        });
        EasyMock.verify(processor, dataxGlobalCfg, dataxReader);
    }


    @Test
    public void testGetDftTemplate() {
        String dftTemplate = DataxMySQLWriter.getDftTemplate();
        Assert.assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    @Test
    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataxMySQLWriter.class);
        Assert.assertTrue(extraProps.isPresent());
    }

//    public void testCreateDDLParser() {
//
//        SqlParser sqlParser = new SqlParser();
//        String createSql = IOUtils.loadResourceFromClasspath(TestDataxMySQLWriter.class, "sql/create-instancedetail.sql");
//        Expression statement = sqlParser.createExpression(createSql, new ParsingOptions());
//        Objects.requireNonNull(statement, "statement can not be null");
//        System.out.println(   SqlFormatter.formatSql(statement, Optional.empty()) );
//    }

}
