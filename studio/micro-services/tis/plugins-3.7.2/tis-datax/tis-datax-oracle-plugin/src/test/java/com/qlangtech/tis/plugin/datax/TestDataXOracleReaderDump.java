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

import com.alibaba.datax.common.util.Configuration;
import com.qlangtech.plugins.incr.flink.cdc.TestSelectedTab;
import com.qlangtech.tis.datax.IDataxGlobalCfg;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxWriter;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugin.ds.oracle.OracleDSFactoryContainer;
import com.qlangtech.tis.plugin.ds.oracle.OracleDataSourceFactory;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-01 10:14
 **/
public class TestDataXOracleReaderDump {
    private static BasicDataSourceFactory dsFactory;
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void initialize() {
        dsFactory = (BasicDataSourceFactory) OracleDSFactoryContainer.initialize(false);
        OracleDSFactoryContainer.initializeOracleTable(
                OracleDSFactoryContainer.sqlfile_column_type_test, "insert_full_types");

    }

    /**
     * 测试读
     *
     * @throws Exception
     */
    @Test
    public void testRealDump() throws Exception {
        String dataXName = OracleDSFactoryContainer.dataName.getName();
        IDataxProcessor processor = EasyMock.mock("dataxProcessor", IDataxProcessor.class);
        IDataxWriter dataxWriter = EasyMock.mock("dataXWriter", IDataxWriter.class);
        // IDataxReaderContext dataXContext = EasyMock.mock("dataxContext", IDataxReaderContext.class);

        SelectedTab stab = TestSelectedTab.createSelectedTab(EntityName.parse(OracleDSFactoryContainer.tab_full_types), dsFactory);
        //IDataxProcessor.TableMap tab = new IDataxProcessor.TableMap(stab);
        // EasyMock.expect(dataxWriter.getSubTask(Optional.of(tab))).andReturn(dataXContext);

        IDataxGlobalCfg globalCfg = EasyMock.mock("dataxGlobalCfg", IDataxGlobalCfg.class);

        EasyMock.expect(processor.getDataXGlobalCfg()).andReturn(globalCfg);

        /**============================================
         * replay
         ============================================*/
        EasyMock.replay(processor, dataxWriter, globalCfg);
//        DataXCfgGenerator dataProcessor = new DataXCfgGenerator(null, dataXName, processor) {
//            @Override
//            public String getTemplateContent() {
//                return DataXOracleReader.getDftTemplate();
//            }
//        };

        DataXOracleReader dataxReader = createReader(dataXName, stab);
        // TISTable t = new TISTable();
        // dataxReader.getSubTasks();

//        DataDumpers dataDumpers = dsFactory.getDataDumpers(t);
//        IDataSourceDumper dumper = null;
//        if (dataDumpers.dumpers.hasNext()) {
//            dumper = dataDumpers.dumpers.next();
//        }

        //   String jobName, SelectedTab tab, IDataSourceDumper dumper

        String cfgResult = ReaderTemplate.generateReaderCfg(processor, dataxReader, dataXName);
//        cfgResult = dataProcessor.generateDataxConfig(
//                dataxReader.createDataXReaderContext("jobName", stab, dumper)
//                , dataxWriter, dataxReader, Optional.of(tab));


        DataxReader.dataxReaderGetter = (name) -> {
            Assert.assertEquals(dataXName, name);
            return dataxReader;
        };

        File readContent = folder.newFile("readContent.txt");
        ReaderTemplate.realExecute(dataXName, Configuration.from(cfgResult), readContent, dataxReader);
        String content = FileUtils.readFileToString(readContent, TisUTF8.get());

        Assert.assertNotNull(content);
        System.out.println(content);

        Assert.assertEquals(IOUtils.loadResourceFromClasspath(
                TestDataXOracleReaderDump.class, "oracle_full_types_read_expect.txt")
                , content);

        EasyMock.verify(processor, dataxWriter, globalCfg);
    }

    protected DataXOracleReader createReader(String dataXName, SelectedTab stab) {

        DataXOracleReader dataxReader = new DataXOracleReader() {
            @Override
            public Class<?> getOwnerClass() {
                return DataXOracleReader.class;
            }

            @Override
            public OracleDataSourceFactory getDataSourceFactory() {
                return (OracleDataSourceFactory) dsFactory;
            }
        };
        dataxReader.selectedTabs = Collections.singletonList(stab);
        dataxReader.fetchSize = 2000;
        dataxReader.dataXName = dataXName;
        dataxReader.template = DataXOracleReader.getDftTemplate();

        return dataxReader;
    }

}
