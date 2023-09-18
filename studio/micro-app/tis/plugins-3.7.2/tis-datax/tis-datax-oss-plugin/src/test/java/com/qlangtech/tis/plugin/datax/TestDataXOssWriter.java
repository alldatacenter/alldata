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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.datax.IDataxGlobalCfg;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.test.BasicTest;
import com.qlangtech.tis.trigger.util.JsonUtil;
import org.easymock.EasyMock;
import org.junit.Assert;

import java.util.Optional;
import java.util.regex.Matcher;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:58
 **/
public class TestDataXOssWriter extends BasicTest {

    public void testFieldPattern() {
        Matcher matcher = DataXOssWriter.PATTERN_OSS_WRITER_OBJECT_NAME.matcher("test");
        assertTrue(matcher.matches());

        matcher = DataXOssWriter.PATTERN_OSS_WRITER_OBJECT_NAME.matcher("test/");
        assertFalse(matcher.matches());

        matcher = DataXOssWriter.PATTERN_OSS_WRITER_OBJECT_NAME.matcher("test/*");
        assertFalse(matcher.matches());

        matcher = DataXOssWriter.PATTERN_OSS_WRITER_OBJECT_NAME.matcher("test*");
        assertFalse(matcher.matches());
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXOssWriter.class);
        assertTrue(extraProps.isPresent());
    }

    public void testGetDftTemplate() {
        String dftTemplate = DataXOssWriter.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testTempateGenerate() throws Exception {
        IDataxProcessor processor = EasyMock.mock("dataxProcessor", IDataxProcessor.class);

        IDataxGlobalCfg dataxGlobalCfg = EasyMock.mock("dataxGlobalCfg", IDataxGlobalCfg.class);
        EasyMock.expect(processor.getDataXGlobalCfg()).andReturn(dataxGlobalCfg).anyTimes();
        //IDataxWriter dataxWriter = EasyMock.mock("dataxWriter", IDataxWriter.class);

        //  IDataxContext dataxContext = EasyMock.mock("dataxWriterContext", IDataxContext.class);
        // EasyMock.expect(dataxWriter.getSubTask(Optional.empty())).andReturn(dataxContext).anyTimes();

        DataXOssWriter ossWriter = new DataXOssWriter();
        ossWriter.endpoint = "aliyun-bj-endpoint";
        ossWriter.bucket = "testBucket";
        ossWriter.object = "tis/mytable/*";
        ossWriter.template = DataXOssWriter.getDftTemplate();
        ossWriter.header = "[\"name\",\"age\",\"degree\"]";
        ossWriter.encoding = "utf-8";
        ossWriter.fieldDelimiter = "\t";
        ossWriter.writeMode = "nonConflict";
        ossWriter.dateFormat = "yyyy-MM-dd";
        ossWriter.fileFormat = "csv";
        ossWriter.maxFileSize = 300;
        ossWriter.nullFormat = "\\\\N";
        EasyMock.expect(processor.getWriter(null)).andReturn(ossWriter).anyTimes();
        EasyMock.replay(processor, dataxGlobalCfg);

        valiateWriterCfgGenerate("oss-datax-writer-assert.json", processor, ossWriter);

        ossWriter.fieldDelimiter = null;
        ossWriter.encoding = null;
        ossWriter.nullFormat = null;
        ossWriter.dateFormat = null;
        ossWriter.fileFormat = null;
        ossWriter.header = null;
        ossWriter.maxFileSize = null;
        valiateWriterCfgGenerate("oss-datax-writer-assert-without-option-val.json", processor, ossWriter);

        EasyMock.verify(processor, dataxGlobalCfg);
    }


    private void valiateWriterCfgGenerate(String assertFileName, IDataxProcessor processor, DataXOssWriter ossWriter) throws Exception {

        MockDataxReaderContext mockReaderContext = new MockDataxReaderContext();

        DataXCfgGenerator dataProcessor = new DataXCfgGenerator(null, testDataXName, processor) {
        };

        DataXOssReader ossReader = new DataXOssReader();

        String readerCfg = dataProcessor.generateDataxConfig(mockReaderContext, ossWriter, ossReader, Optional.empty());
        assertNotNull(readerCfg);
        System.out.println(readerCfg);
        JsonUtil.assertJSONEqual(this.getClass(), assertFileName, readerCfg, (msg, expect, actual) -> {
            Assert.assertEquals(msg, expect, actual);
        });
    }
}
