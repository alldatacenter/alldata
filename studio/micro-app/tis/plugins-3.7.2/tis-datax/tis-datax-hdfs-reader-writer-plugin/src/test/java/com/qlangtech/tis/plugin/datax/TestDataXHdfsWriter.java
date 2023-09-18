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
import com.qlangtech.tis.datax.Delimiter;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.fs.IPathInfo;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fs.TISFSDataOutputStream;
import com.qlangtech.tis.hdfs.impl.HdfsFileSystemFactory;
import com.qlangtech.tis.hdfs.impl.HdfsPath;
import com.qlangtech.tis.hdfs.test.HdfsFileSystemFactoryTestUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.common.WriterJson;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.impl.TextFSFormat;
import com.qlangtech.tis.plugin.test.BasicTest;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXHdfsWriter extends BasicTest {
    private static final Logger logger = LoggerFactory.getLogger(TestDataXHdfsWriter.class);

    public void testGetDftTemplate() {
        String dftTemplate = DataXHdfsWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXHdfsWriter.class);
        assertTrue(extraProps.isPresent());
    }

    public void testDescriptorsJSONGenerate() {
        DataXHdfsWriter writer = new DataXHdfsWriter();
        DescriptorsJSON descJson = new DescriptorsJSON(writer.getDescriptor());
        JSONObject desc = descJson.getDescriptorsJSON();
        System.out.println(JsonUtil.toString(desc));

        JsonUtil.assertJSONEqual(TestDataXHdfsWriter.class, "desc-json/datax-writer-hdfs.json", desc, (m, e, a) -> {
            assertEquals(m, e, a);
        });
    }

    private final String mysql2hdfsDataXName = "mysql2hdfs";
    private static final String hdfsRelativePath = "tis/order";

    public void testConfigGenerate() throws Exception {


        DataXHdfsWriter hdfsWriter = new DataXHdfsWriter();
        hdfsWriter.dataXName = mysql2hdfsDataXName;
        hdfsWriter.fsName = "hdfs1";
        TextFSFormat textFormat = new TextFSFormat();
        textFormat.fieldDelimiter = Delimiter.Tab.token;
        hdfsWriter.fileType = textFormat;
        hdfsWriter.writeMode = "nonConflict";
      //  hdfsWriter.fieldDelimiter = "\t";
        hdfsWriter.compress = "gzip";
        hdfsWriter.encoding = "utf-8";
        hdfsWriter.template = DataXHdfsWriter.getDftTemplate();
        hdfsWriter.path = hdfsRelativePath;


        IDataxProcessor.TableMap tableMap = WriterTemplate.createCustomer_order_relationTableMap();


        WriterTemplate.valiateCfgGenerate("hdfs-datax-writer-assert.json", hdfsWriter, tableMap);


        hdfsWriter.compress = null;
        hdfsWriter.encoding = null;

        WriterTemplate.valiateCfgGenerate("hdfs-datax-writer-assert-without-option-val.json", hdfsWriter, tableMap);
    }

    //@Test
    public void testdataDump() throws Exception {

        //  final DataxWriter dataxWriter = DataxWriter.load(null, mysql2hdfsDataXName);


        HdfsFileSystemFactory fsFactory = HdfsFileSystemFactoryTestUtils.getFileSystemFactory();
        ITISFileSystem fileSystem = fsFactory.getFileSystem();
//        assertNotNull("fileSystem can not be null", fileSystem);

//        new Path(fsFactory.rootDir
//                , this.cfg.getNecessaryValue(Key.PATH, HdfsWriterErrorCode.REQUIRED_VALUE));
//
//        fileSystem.getPath("");

        HdfsPath p = new HdfsPath(fsFactory.rootDir + "/tis/order");


        HdfsPath subWriterPath = new HdfsPath(p, "test");

        try (TISFSDataOutputStream outputStream = fileSystem.create(subWriterPath, true)) {
            org.apache.commons.io.IOUtils.write(IOUtils.loadResourceFromClasspath(DataXHdfsWriter.class
                    , "hdfs-datax-writer-assert-without-option-val.json"), outputStream, TisUTF8.get());
        }
        System.out.println("write file success");

        List<IPathInfo> iPathInfos = fileSystem.listChildren(p);
        for (IPathInfo child : iPathInfos) {
            fileSystem.delete(child.getPath(), true);
        }

        final DataXHdfsWriter hdfsWriter = new DataXHdfsWriter() {
            @Override
            public FileSystemFactory getFs() {
                return fsFactory;
            }

            @Override
            public Class<?> getOwnerClass() {
                return DataXHdfsWriter.class;
            }
        };


        DataxWriter.dataxWriterGetter = (name) -> {
            assertEquals("mysql2hdfs", name);
            return hdfsWriter;
        };

//        IPath path = fileSystem.getPath(fileSystem.getPath(fileSystem.getRootDir()), hdfsRelativePath);
//        System.out.println("clear path:" + path);
//        fileSystem.delete(path, true);
//
        WriterTemplate.realExecuteDump(WriterJson.path("hdfs-datax-writer-assert-without-option-val.json"), hdfsWriter);
    }

}
