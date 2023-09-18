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

import com.qlangtech.tis.extension.util.PluginExtraProps;
import com.qlangtech.tis.plugin.common.ReaderTemplate;
import com.qlangtech.tis.plugin.test.BasicTest;

import java.util.Optional;
import java.util.regex.Matcher;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-08 11:35
 **/
public class TestDataXOssReader extends BasicTest {

    public void testFieldPattern() {
        Matcher matcher = DataXOssReader.PATTERN_OSS_OBJECT_NAME.matcher("bazhen/*");
        assertTrue(matcher.matches());

        matcher = DataXOssReader.PATTERN_OSS_OBJECT_NAME.matcher("bazhen");
        assertTrue(matcher.matches());

        matcher = DataXOssReader.PATTERN_OSS_OBJECT_NAME.matcher("bazhen/dddd123");
        assertTrue(matcher.matches());

        matcher = DataXOssReader.PATTERN_OSS_OBJECT_NAME.matcher("/bazhen123/dddd");
        assertFalse(matcher.matches());

        matcher = DataXOssReader.PATTERN_OSS_OBJECT_NAME.matcher("/bazhen123/dddd/");
        assertFalse(matcher.matches());

        matcher = DataXOssReader.pattern_oss_bucket.matcher("tisrelease");
        assertTrue(matcher.matches());

        matcher = DataXOssReader.pattern_oss_bucket.matcher("tis-release");
        assertTrue(matcher.matches());
    }

    public void testGetDftTemplate() {
        String dftTemplate = DataXOssReader.getDftTemplate();
        assertNotNull("dftTemplate can not be null", dftTemplate);
    }

    public void testPluginExtraPropsLoad() throws Exception {
        Optional<PluginExtraProps> extraProps = PluginExtraProps.load(DataXOssReader.class);
        assertTrue(extraProps.isPresent());
    }

    public void testTempateGenerate() throws Exception {

        final String dataXName = "testDataXName";

//        String tab = "\\t";
//
//        System.out.println("tab:" + StringEscapeUtils.unescapeJava(tab).length());
        DataXOssReader ossReader = new DataXOssReader();
        ossReader.endpoint = "aliyun-bj-endpoint";
        ossReader.bucket = "testBucket";
        ossReader.object = "tis/mytable/*";
        ossReader.template = DataXOssReader.getDftTemplate();
        ossReader.column = "[{type:\"string\",index:0},{type:\"string\",index:1},{type:\"string\",value:\"test\"}]";
        ossReader.encoding = "utf8";
        ossReader.fieldDelimiter = "\t";
        ossReader.compress = "zip";

        ossReader.nullFormat = "\\N";
        ossReader.skipHeader = true;
        ossReader.csvReaderConfig = "{\n" +
                "        \"safetySwitch\": false,\n" +
                "        \"skipEmptyRecords\": false,\n" +
                "        \"useTextQualifier\": false\n" +
                "}";
        ReaderTemplate.validateDataXReader("oss-datax-reader-assert.json", dataXName, ossReader);

        ossReader.encoding = null;
        ossReader.compress = null;
        ossReader.nullFormat = null;
        ossReader.skipHeader = null;
        ossReader.csvReaderConfig = "{}";
        ReaderTemplate.validateDataXReader("oss-datax-reader-assert-without-option-val.json", dataXName, ossReader);
    }

}
