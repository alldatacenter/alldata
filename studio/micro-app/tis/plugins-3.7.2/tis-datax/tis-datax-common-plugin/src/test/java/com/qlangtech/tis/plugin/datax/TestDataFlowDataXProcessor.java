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

import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.manage.common.CenterResource;
import junit.framework.TestCase;
import org.junit.Assert;

import java.io.File;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-03 13:42
 **/
public class TestDataFlowDataXProcessor extends TestCase {

    //    @BeforeClass
//    public static void init() {
//
//    }
    private static final String DataFlowName = "tttt7";
    DataFlowDataXProcessor dataXProcessor;

    @Override
    protected void setUp() throws Exception {
        CenterResource.setNotFetchFromCenterRepository();
        this.dataXProcessor = new DataFlowDataXProcessor();
        dataXProcessor.name = DataFlowName;
        dataXProcessor.globalCfg = "test";
    }

    public void testGetReaders() {

        Assert.assertTrue(dataXProcessor.isWriterSupportMultiTableInReader(null));
        Assert.assertTrue(dataXProcessor.isRDBMS2RDBMS(null));
        Assert.assertFalse(dataXProcessor.isRDBMS2UnStructed(null));


        List<IDataxReader> readers = dataXProcessor.getReaders(null);
        assertTrue(readers.size() > 0);
    }

    // @Test
    public void testGenerateDataXCfgs() throws Exception {

        DataXCfgGenerator gen = new DataXCfgGenerator(null, DataFlowName, dataXProcessor);

        File dataxCfgDir = dataXProcessor.getDataxCfgDir(null);

        DataXCfgGenerator.GenerateCfgs generateCfgs = gen.startGenerateCfg(dataxCfgDir);

        List<DataXCfgGenerator.DataXCfgFile> cfgFiles = generateCfgs.getDataXCfgFiles();
        Assert.assertEquals(2, cfgFiles.size());
        for (DataXCfgGenerator.DataXCfgFile f : cfgFiles) {
            File target = f.getFile();
            Assert.assertTrue(target.getAbsolutePath(), target.exists());
        }
    }
}
