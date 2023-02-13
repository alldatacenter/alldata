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
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.datax.server.FTPServer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXFtpReader {
    @Test
    public void testGetDftTemplate() {
        String dftTemplate = DataXFtpReader.getDftTemplate();
        Assert.assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    @Test
    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXFtpReader.class);
        Assert.assertTrue(extraProps.isPresent());
    }

    @Test
    public void testDescGenerate() throws Exception {

        // ContextDesc.descBuild(DataXFtpReader.class, true);

        PluginDesc.testDescGenerate(DataXFtpReader.class, "ftp-datax-reader-descriptor.json");

    }

    @Test
    public void testTemplateGenerate() throws Exception {

        String dataXName = "test";

        DataXFtpReader reader = new DataXFtpReader();
        reader.compress = Compress.noCompress.token;
        reader.template = DataXFtpReader.getDftTemplate();
        reader.linker = FtpWriterUtils.createFtpServer();
//        reader.protocol = "ftp";
//        reader.host = "192.168.28.201";
//        reader.port = 21;
//        reader.timeout = 59999;
//        reader.connectPattern = "PASV";
//        reader.username = "test";
//        reader.password = "test";
        reader.path = "/home/hanfa.shf/ftpReaderTest/data";
        reader.column = " [{\n" +
                "    \"type\": \"long\",\n" +
                "    \"index\": 0    \n" +
                " },\n" +
                " {\n" +
                "    \"type\": \"string\",\n" +
                "    \"value\": \"alibaba\"  \n" +
                " }]";

        reader.fileFormat = FtpWriterUtils.createCsvFormat();

        // reader.fieldDelimiter = ",";
        reader.compress = "bzip2";
        reader.encoding = "utf-8";
        // reader.skipHeader = true;
        reader.nullFormat = "\\\\N";
        reader.maxTraversalLevel = "99";
//        reader.csvReaderConfig = "{\n" +
//                "        \"safetySwitch\": false,\n" +
//                "        \"skipEmptyRecords\": false,\n" +
//                "        \"useTextQualifier\": false\n" +
//                "}";

        ReaderTemplate.validateDataXReader("ftp-datax-reader-assert.json", dataXName, reader);


        FTPServer ftpServer = FtpWriterUtils.createFtpServer();
        ftpServer.port = null;
        ftpServer.timeout = null;
        ftpServer.connectPattern = null;
        reader.linker = ftpServer;
        reader.fileFormat = FtpWriterUtils.createTextFormat();
//        reader.port = null;
//        reader.timeout = null;
//        reader.connectPattern = null;
        reader.compress = null;
        reader.encoding = null;
        // reader.skipHeader = null;
        reader.nullFormat = null;
        reader.maxTraversalLevel = null;
        //reader.csvReaderConfig = null;
        // reader.fileFormat

        ReaderTemplate.validateDataXReader("ftp-datax-reader-assert-without-option-val.json", dataXName, reader);

    }
}
