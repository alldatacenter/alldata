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

import com.alibaba.datax.plugin.unstructuredstorage.Compress;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsColMeta;
import com.google.common.collect.Lists;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.plugin.SetPluginsResult;
import com.qlangtech.tis.plugin.common.WriterJson;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.format.FileFormat;
import com.qlangtech.tis.plugin.datax.meta.NoneMetaDataWriter;
import com.qlangtech.tis.plugin.datax.server.FTPServer;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import org.apache.commons.collections.CollectionUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-15 11:54
 **/
public class TestDataXFtpWriterReal {
    static FTPContainer ftpContainer;

    static final String FTP_PATH = "/path1";

    @BeforeClass
    public static void initialize() {
        ftpContainer = new FTPContainer();
        ftpContainer.start();


    }

    @AfterClass
    public static void testTerminate() {
        ftpContainer.close();
    }


    @Test
    public void testRealDump() throws Exception {

        final String targetTableName = "customer_order_relation";
        String testDataXName = "mysql_ftp_for_test";

        final DataXFtpWriter writer = getFTPWriter();
       // final FTPServer ftpServer = FtpWriterUtils.createFtpServer(ftpContainer);
         final FTPServer ftpServer = FtpWriterUtils.createFtpServer(null);
        SetPluginsResult saveResult = ParamsConfig.getTargetPluginStore(FTPServer.FTP_SERVER)
                .setPlugins(null, Optional.empty(), Lists.newArrayList(new Descriptor.ParseDescribable(ftpServer)));
        Assert.assertTrue(saveResult.success);
        Assert.assertNotNull(FTPServer.getServer(FtpWriterUtils.ftpLink));


//        ftpServer.port = ftpContainer.getPort21();
//        ftpServer.connectPattern = "PASV"; // PORT
//        ftpServer.username = FTPContainer.USER_NAME;
//        ftpServer.password = FTPContainer.PASSWORD;
//        ftpServer.protocol = "ftp";
//        ftpServer.timeout = 1000;
        // writer.dataXName = testDataXName;
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
//        CreateTableSqlBuilder.CreateDDL ddl = writer.generateCreateDDL(tabMap);


        WriterJson wjson = WriterJson.content(WriterTemplate.cfgGenerate(writer, tabMap));
        ftpServer.useFtpHelper((helper) -> {
            helper.mkDirRecursive(FTP_PATH);
            Set<String> allFileExists = helper.getAllFilesInDir(FTP_PATH, "test");
            Assert.assertTrue(CollectionUtils.isEmpty(allFileExists));
            return null;
        });
        /**
         * ==============================================================================
         * ******** 开始执行
         * ==============================================================================
         */
        WriterTemplate.realExecuteDump(testDataXName, wjson, writer);

        ftpServer.useFtpHelper((helper) -> {
            Assert.assertTrue(FTP_PATH + " must be exist", helper.isDirExist(FTP_PATH));
            HashSet<String> importFiles = helper.getListFiles(FTP_PATH + "/*", 0, 100);
            // HashSet<String> importFiles = ftpHelper.getListFiles("path1/path2/*", 0, 1);
            Assert.assertEquals("importFiles size ", 1, importFiles.size());
            return null;
        });


    }

    private DataXFtpWriter getFTPWriter() {
        DataXFtpWriter writer = new DataXFtpWriter();
        writer.path = FTP_PATH;
        writer.writeMode = "truncate";
        writer.compress = Compress.noCompress.token;
        writer.template = DataXFtpWriter.getDftTemplate();
        FileFormat txtFormat = FtpWriterUtils.createTextFormat();
        writer.fileFormat = txtFormat;
        writer.writeMetaData = new NoneMetaDataWriter();
        // FTPServer ftpServer = FtpWriterUtils.createFtpServer();
        writer.linker = FtpWriterUtils.ftpLink; //ftpServer;
        DataxWriter.dataxWriterGetter = (name) -> writer;
        return writer;
    }
}
