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

import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.hdfs.impl.HdfsFileSystemFactory;
import com.qlangtech.tis.hdfs.test.HdfsFileSystemFactoryTestUtils;
import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.test.BasicTest;
import junit.framework.TestCase;

import java.io.File;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 20:02
 **/
public class TestDataXHdfsReader extends BasicTest {

    public void testDescriptorsJSONGenerate() {
        PluginDesc.testDescGenerate(DataXHdfsReader.class, "hdfs-datax-reader-descriptor.json");
    }

    //    public void test() {
//        ContextDesc.descBuild(DataXHdfsReader.class, true);
//    }
    String dataXName = "testDataXName";

    public void testTemplateGenerate() throws Exception {


        DataXHdfsReader dataxReader = createHdfsReader(dataXName);


        ReaderTemplate.validateDataXReader("hdfs-datax-reader-assert.json", dataXName, dataxReader);


        dataxReader.compress = null;
        dataxReader.csvReaderConfig = null;
        dataxReader.fieldDelimiter = "\\t";


        ReaderTemplate.validateDataXReader("hdfs-datax-reader-assert-without-option-val.json", dataXName, dataxReader);
    }

    protected DataXHdfsReader createHdfsReader(String dataXName) {
        final HdfsFileSystemFactory fsFactory = HdfsFileSystemFactoryTestUtils.getFileSystemFactory();

        DataXHdfsReader dataxReader = new DataXHdfsReader() {
            @Override
            public HdfsFileSystemFactory getFs() {
                return fsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXHdfsReader.class;
            }
        };
        dataxReader.dataXName = dataXName;
        dataxReader.template = DataXHdfsReader.getDftTemplate();
        dataxReader.column = "[\n" +
                "      {\n" +
                "        \"index\": 0,\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"index\": 1,\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"index\": 2,\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"index\": 3,\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"index\": 4,\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    ]";
        dataxReader.fsName = "default";
        dataxReader.compress = "gzip";
        dataxReader.csvReaderConfig = "{\n" +
                "        \"safetySwitch\": false,\n" +
                "        \"skipEmptyRecords\": false,\n" +
                "        \"useTextQualifier\": false\n" +
                "}";
        dataxReader.fieldDelimiter = ",";
        dataxReader.encoding = "utf-8";
        dataxReader.nullFormat = "\\\\N";
        dataxReader.fileType = "text";
        dataxReader.path = "tis/order/*";
        return dataxReader;
    }

    public void testRealDump() throws Exception {
        DataXHdfsReader dataxReader = createHdfsReader(dataXName);
        DataxReader.dataxReaderGetter = (name) -> {
            TestCase.assertEquals(dataXName, name);
            return dataxReader;
        };

        File rcontent = new File("hdfs-datax-reader-content.txt");
        ReaderTemplate.realExecute("hdfs-datax-reader-assert-without-option-val.json", rcontent, dataxReader);
    }
}
