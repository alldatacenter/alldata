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
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.format.CSVFormat;
import com.qlangtech.tis.plugin.datax.format.TextFormat;
import com.qlangtech.tis.plugin.datax.server.FTPServer;
import com.qlangtech.tis.plugin.datax.test.TestSelectedTabs;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXFtpWriter {
    @Test
    public void testGetDftTemplate() {
        String dftTemplate = DataXFtpWriter.getDftTemplate();
        Assert.assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    @Test
    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXFtpWriter.class);
        Assert.assertTrue(extraProps.isPresent());
    }

    @Test
    public void testDescGenerate() {
        PluginDesc.testDescGenerate(DataXFtpWriter.class, "ftp-datax-writer-descriptor.json");
    }

    @Test
    public void testTempateGenerate() throws Exception {

        //    ContextDesc.descBuild(DataXFtpWriter.class, false);

        DataXFtpWriter dataXWriter = new DataXFtpWriter();
        dataXWriter.template = DataXFtpWriter.getDftTemplate();
        dataXWriter.compress = Compress.noCompress.token;
        FTPServer ftpServer = FtpWriterUtils.createFtpServer(null);
        // dataXWriter.protocol = "ftp";
        /// dataXWriter.host = "192.168.28.201";
        //dataXWriter.port = 21;
        // dataXWriter.timeout = 33333;
//        dataXWriter.username = "test";
//        dataXWriter.password = "test";
        dataXWriter.linker = null; //ftpServer;
        dataXWriter.path = "/tmp/data/";
        // dataXWriter.fileName = "yixiao";
        dataXWriter.writeMode = "truncate";
        //  dataXWriter.fieldDelimiter = ",";
        dataXWriter.encoding = "utf-8";
        dataXWriter.nullFormat = "\\\\N";
        // dataXWriter.fileFormat = "text";
        // dataXWriter.suffix = "xxxx";
        // dataXWriter.header = true;

        DataxWriter.dataxWriterGetter = (name) -> dataXWriter;

        TextFormat tformat = FtpWriterUtils.createTextFormat();
        dataXWriter.fileFormat = tformat;

        IDataxProcessor.TableMap tableMap = TestSelectedTabs.createTableMapper().get();

        WriterTemplate.valiateCfgGenerate("ftp-datax-writer-assert.json", dataXWriter, tableMap);

        ftpServer.port = null;
        ftpServer.timeout = null;
        // dataXWriter.port = null;
        // dataXWriter.timeout = null;
        CSVFormat csvFormat = FtpWriterUtils.createCsvFormat();
        // dataXWriter.fieldDelimiter = null;
        dataXWriter.fileFormat = csvFormat;
        dataXWriter.encoding = null;
        dataXWriter.nullFormat = null;
        dataXWriter.dateFormat = null;
        //dataXWriter.fileFormat = null;
        //dataXWriter.suffix = null;
        //dataXWriter.header = false;

        WriterTemplate.valiateCfgGenerate("ftp-datax-writer-assert-without-option-val.json", dataXWriter, tableMap);
    }


//    public void testRealDump() throws Exception {
//        DataXFtpWriter dataXWriter = new DataXFtpWriter();
//        WriterTemplate.realExecuteDump("ftp-datax-writer.json", dataXWriter);
//    }
}
