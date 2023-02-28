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

import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.config.hive.IHiveConnGetter;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.hdfs.impl.HdfsFileSystemFactory;
import com.qlangtech.tis.hdfs.test.HdfsFileSystemFactoryTestUtils;
import com.qlangtech.tis.hive.DefaultHiveConnGetter;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.common.WriterJson;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.test.BasicTest;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-27 15:17
 **/
public class TestDataXHiveWriter extends BasicTest {

    public void testGetDftTemplate() {
        String dftTemplate = DataXHiveWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXHiveWriter.class);
        assertTrue(extraProps.isPresent());
    }

    public void testDescriptorsJSONGenerate() {
        DataXHiveWriter writer = new DataXHiveWriter();
        DescriptorsJSON descJson = new DescriptorsJSON(writer.getDescriptor());

        JSONObject desc = descJson.getDescriptorsJSON();
        System.out.println(JsonUtil.toString(desc));

        JsonUtil.assertJSONEqual(TestDataXHiveWriter.class, "desc-json/datax-writer-hive.json", desc, (m, e, a) -> {
            assertEquals(m, e, a);
        });
    }

    String mysql2hiveDataXName = "mysql2hive";

    public void testConfigGenerate() throws Exception {


        DataXHiveWriter hiveWriter = new DataXHiveWriter();
        hiveWriter.dataXName = mysql2hiveDataXName;
        hiveWriter.fsName = "hdfs1";
        hiveWriter.fileType = "text";
        hiveWriter.tabPrefix = "ods_";
        hiveWriter.writeMode = "nonConflict";
        hiveWriter.fieldDelimiter = "\t";
        hiveWriter.compress = "gzip";
        hiveWriter.encoding = "utf-8";
        hiveWriter.template = DataXHiveWriter.getDftTemplate();
        hiveWriter.partitionRetainNum = 2;
        hiveWriter.partitionFormat = "yyyyMMdd";


        IDataxProcessor.TableMap tableMap = WriterTemplate.createCustomer_order_relationTableMap();


        WriterTemplate.valiateCfgGenerate("hive-datax-writer-assert.json", hiveWriter, tableMap);


        hiveWriter.compress = null;
        hiveWriter.encoding = null;

        WriterTemplate.valiateCfgGenerate("hive-datax-writer-assert-without-option-val.json", hiveWriter, tableMap);

    }


    public void testDataDump() throws Exception {

        HdfsFileSystemFactory hdfsFileSystemFactory = HdfsFileSystemFactoryTestUtils.getFileSystemFactory();

        final DefaultHiveConnGetter hiveConnGetter = new DefaultHiveConnGetter();
        hiveConnGetter.dbName = "tis";
        hiveConnGetter.hiveAddress = "192.168.28.200:10000";

        final DataXHiveWriter dataxWriter = new DataXHiveWriter() {

            @Override
            public IHiveConnGetter getHiveConnGetter() {
                return hiveConnGetter;
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

        DataxWriter.dataxWriterGetter = (name) -> {
            assertEquals("mysql2hive", name);
            return dataxWriter;
        };

        WriterTemplate.realExecuteDump(WriterJson.path("hive-datax-writer-assert-without-option-val.json"), dataxWriter);
    }


}
