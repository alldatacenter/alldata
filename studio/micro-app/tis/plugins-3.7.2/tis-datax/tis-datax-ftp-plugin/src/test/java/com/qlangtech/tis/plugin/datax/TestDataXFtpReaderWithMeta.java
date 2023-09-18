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

import com.alibaba.datax.plugin.ftp.common.FtpHelper;
import com.qlangtech.tis.plugin.datax.meta.MetaDataWriter;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-05 18:33
 **/
public class TestDataXFtpReaderWithMeta {

    @Test
    public void testFTP_FILE_PATTERN_recognize() {
        String tableName = "instancedetail";
        Matcher matcher = DataXFtpReaderWithMeta.FTP_FILE_PATTERN
                .matcher("/admin/order2/" + tableName + "/" + FtpHelper.KEY_META_FILE);

        Assert.assertTrue(matcher.matches());
        Assert.assertEquals(tableName, matcher.group(1));


        matcher = DataXFtpReaderWithMeta.FTP_FILE_PATTERN
                .matcher("/" + tableName + "/" + FtpHelper.KEY_META_FILE);

        Assert.assertTrue(matcher.matches());
        Assert.assertEquals(tableName, matcher.group(1));
    }
}
