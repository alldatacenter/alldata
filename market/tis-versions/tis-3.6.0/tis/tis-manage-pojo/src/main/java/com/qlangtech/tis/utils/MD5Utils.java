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

package com.qlangtech.tis.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-26 13:35
 **/
public class MD5Utils {
    public static String md5file(final File saveFile) throws IOException {
        //    InputStream savedStream = null;
        try (InputStream savedStream = new FileInputStream(saveFile)) {
            ;
            // 保存的文件的签名
            return md5file(IOUtils.toByteArray(savedStream));
        }
    }

    public static String md5file(byte[] content) {
        // try {
        return (DigestUtils.md5Hex(content));
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
    }
}
