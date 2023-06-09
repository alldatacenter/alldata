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

package com.qlangtech.tis.plugins.incr.flink.connector.hudi.streamscript;

import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.fs.IPath;
import com.qlangtech.tis.hdfs.impl.HdfsPath;
import com.qlangtech.tis.hdfs.test.HdfsFileSystemFactoryTestUtils;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.datax.hudi.HudiTest;
import com.qlangtech.tis.plugin.datax.hudi.partition.HudiTablePartition;
import com.qlangtech.tis.plugins.incr.flink.connector.hudi.HudiSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.scripttype.IStreamScriptType;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-25 11:15
 **/
public class BaiscFlinkStreamScriptCreator {
    public static final String targetTableName = "customer_order_relation";
    public static final String hudi_datax_writer_assert_without_optional = "hudi-datax-writer-assert-without-optional.json";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void startClazz() {
        CenterResource.setNotFetchFromCenterRepository();
    }

    protected void validateGenerateScript(IStreamScriptType scriptType, HudiTablePartition partition, MergeDataProcessor mergeDataProcessor) throws IOException {
        String child = "default/customer_order_relation";
        String dataDir = "hudi";
        IPath rootPath = new HdfsPath(HdfsFileSystemFactoryTestUtils.DEFAULT_HDFS_ADDRESS + "/user/admin");
        IPath tabPath = new HdfsPath(rootPath, child);
        // IPath hudiDataPath = new HdfsPath(tabPath, dataDir);
//        EasyMock.expect(fs.getPath(rootPath, child)).andReturn(tabPath);
//        EasyMock.expect(fs.getPath(tabPath, dataDir)).andReturn(hudiDataPath);
//        EasyMock.expect(fs.getRootDir()).andReturn(rootPath);
        // EasyMock.expect(fsFactory.getFileSystem()).andReturn(fs);
        // HudiTest forTest = createDataXWriter(Optional.of(fsFactory));
        DataxProcessor dataXProcessor = EasyMock.mock("dataXProcessor", DataxProcessor.class);
        String dataXCfgFile = targetTableName + "_0" + IDataxProcessor.DATAX_CREATE_DATAX_CFG_FILE_NAME_SUFFIX;
        File dataXCfg = folder.newFile(dataXCfgFile);


        //   List<File> dataXFiles = Lists.newArrayList(dataXCfg);

        final HudiTest hudiTest = HudiTest.createDataXWriter(Optional.empty(), partition);

        DataxWriter.dataxWriterGetter = (name) -> {
            return hudiTest.writer;
        };

        FileUtils.writeStringToFile(dataXCfg
                , "{job:{content:[{\"writer\":"
                        + IOUtils.loadResourceFromClasspath(
                        hudiTest.writer.getClass(), hudi_datax_writer_assert_without_optional)
                        + "}]}}"
                , TisUTF8.get());


        IDataxReader dataXReader = EasyMock.createMock("dataXReader", IDataxReader.class);


        EasyMock.expect(dataXReader.getSelectedTabs()).andReturn(Collections.singletonList(hudiTest.tab));

        EasyMock.expect(dataXProcessor.getReader(null)).andReturn(dataXReader);

       // EasyMock.expect(dataXProcessor.getDataxCfgFileNames(null)).andReturn(Collections.singletonList(dataXCfg));

        DataxProcessor.processorGetter = (dataXName) -> {
            Assert.assertEquals(HdfsFileSystemFactoryTestUtils.testDataXName.getName(), dataXName);
            return dataXProcessor;
        };
        EasyMock.replay(dataXProcessor, dataXReader);

        HudiSinkFactory sinkFactory = new HudiSinkFactory();
      //  sinkFactory.dumpTimeStamp = String.valueOf(HudiWriter.timestamp);
        sinkFactory.scriptType = scriptType;
        sinkFactory.currentLimit = 2000;
        //String groupName, String keyVal, Class<T> pluginClass
        sinkFactory.setKey(new KeyedPluginStore.Key(null, HdfsFileSystemFactoryTestUtils.testDataXName.getName(), null));


        mergeDataProcessor.processMergeData((IStreamIncrGenerateStrategy.AdapterStreamTemplateData) sinkFactory.decorateMergeData(
                new TestStreamTemplateData(HdfsFileSystemFactoryTestUtils.testDataXName, targetTableName)));


        EasyMock.verify(dataXProcessor, dataXReader);
    }


    interface MergeDataProcessor {
        public void processMergeData(IStreamIncrGenerateStrategy.AdapterStreamTemplateData mergeData);
    }

}

