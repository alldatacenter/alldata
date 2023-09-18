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

import com.google.common.collect.Maps;
import com.qlangtech.tis.config.hive.meta.IHiveMetaStore;
import com.qlangtech.tis.datax.Delimiter;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.exec.ExecChainContextUtils;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.hdfs.impl.HdfsFileSystemFactory;
import com.qlangtech.tis.hdfs.test.HdfsFileSystemFactoryTestUtils;
import com.qlangtech.tis.hive.Hiveserver2DataSourceFactory;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.common.WriterJson;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.impl.TextFSFormat;
import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import com.qlangtech.tis.sql.parser.TabPartitions;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-20 09:24
 **/
public class TestDataXHiveWriterDump {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();
    private static long currentTimeStamp;

    @BeforeClass
    public static void initialize() {
        currentTimeStamp = TimeFormat.getCurrentTimeStamp();// String.valueOf();
        System.setProperty(DataxUtils.EXEC_TIMESTAMP, String.valueOf(currentTimeStamp));
    }

    @Test
    public void testDataDump() throws Exception {

        HdfsFileSystemFactory hdfsFileSystemFactory = HdfsFileSystemFactoryTestUtils.getFileSystemFactory();
        IDataxProcessor.TableMap applicationTab = TestDataXHiveWriter.getApplicationTab();

        final String hiveDbName = "default";

        final DataXHiveWriter dataxWriter = new DataXHiveWriter() {
            @Override
            public Hiveserver2DataSourceFactory getDataSourceFactory() {
                Hiveserver2DataSourceFactory hiveDS = new Hiveserver2DataSourceFactory();
                hiveDS.name = "hive200";
                hiveDS.dbName = hiveDbName;
                hiveDS.metaStoreUrls = "thrift://192.168.28.200:9083";
                hiveDS.hiveAddress = "192.168.28.200:10000";
                hiveDS.userToken = new com.qlangtech.tis.config.hive.impl.OffHiveUserToken();
                return hiveDS;
            }

            @Override
            public FileSystemFactory getFs() {
                return hdfsFileSystemFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXHiveWriter.class;
            }
        };
        Hiveserver2DataSourceFactory ds = dataxWriter.getDataSourceFactory();
        try (IHiveMetaStore metaStoreClient = ds.createMetaStoreClient()) {
            metaStoreClient.dropTable(ds.dbName, applicationTab.getTo());

        }

        TextFSFormat txtFormat = new TextFSFormat();
        txtFormat.fieldDelimiter = Delimiter.Char001.token;
        dataxWriter.fileType = txtFormat;
        dataxWriter.dataXName = TestDataXHiveWriter.mysql2hiveDataXName;
        dataxWriter.partitionRetainNum = 1;
        dataxWriter.partitionFormat = (TimeFormat.yyyyMMddHHmmss.name());

        DataxWriter.dataxWriterGetter = (name) -> {
            Assert.assertEquals(TestDataXHiveWriter.mysql2hiveDataXName, name);
            return dataxWriter;
        };
        TabPartitions partitions = new TabPartitions(Maps.newHashMap());

        IExecChainContext execContext = EasyMock.mock("execContext", IExecChainContext.class);

        EasyMock.expect(execContext.getAttribute(ExecChainContextUtils.PARTITION_DATA_PARAMS))
                .andReturn(partitions);

        IDataxProcessor processor = EasyMock.mock("processor", IDataxProcessor.class);
        File ddlDir = folder.newFolder("ddlDir");

        CreateTableSqlBuilder.CreateDDL createDDL = dataxWriter.generateCreateDDL(applicationTab);
        Assert.assertNotNull("createDDL can not be null", createDDL);

        FileUtils.write(new File(ddlDir, applicationTab.getTo() + IDataxProcessor.DATAX_CREATE_DDL_FILE_NAME_SUFFIX)
                , createDDL.getDDLScript(), TisUTF8.get());

        EasyMock.expect(processor.getDataxCreateDDLDir(null)).andReturn(ddlDir);

        EasyMock.expect(execContext.getProcessor()).andReturn(processor);

        EasyMock.expect(execContext.getPartitionTimestampWithMillis()).andReturn(currentTimeStamp).anyTimes();

        IRemoteTaskTrigger preExec = dataxWriter.createPreExecuteTask(execContext, applicationTab.getSourceTab());

        EasyMock.replay(execContext, processor);

        preExec.run();

        WriterTemplate.realExecuteDump(TestDataXHiveWriter.mysql2hiveDataXName
                , WriterJson.path("hive-datax-writer-assert-without-option-val.json"), dataxWriter);

        IRemoteTaskTrigger postExec = dataxWriter.createPostTask(execContext, applicationTab.getSourceTab(), null);
        postExec.run();

        Assert.assertEquals(1, partitions.size());
        Optional<TabPartitions.DumpTabPartition> applicationPS
                = partitions.findTablePartition(hiveDbName, applicationTab.getTo());
        final String pt = TimeFormat.yyyyMMddHHmmss.format(currentTimeStamp);
        Assert.assertTrue(applicationPS.isPresent());
        Assert.assertEquals(pt, applicationPS.get().pt.getPt());

        try (DataSourceMeta.JDBCConnection connection = dataxWriter.getConnection()) {
            connection.query("select count(1) from " + applicationTab.getTo()
                            + " where " + IDumpTable.PARTITION_PT + "='" + pt + "'"
                    , (result) -> {
                        int rowCount = result.getInt(1);
                        Assert.assertEquals("table:" + applicationTab.getTo() + ",pt:" + IDumpTable.PARTITION_PT, 30, rowCount);
                        return false;
                    });
        }


        EasyMock.verify(execContext, processor);
    }

}
