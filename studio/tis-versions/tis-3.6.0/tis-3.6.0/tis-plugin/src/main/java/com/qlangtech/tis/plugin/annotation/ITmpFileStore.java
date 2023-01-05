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

package com.qlangtech.tis.plugin.annotation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * 序列化时并不进行保存
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-05 11:34
 **/
public interface ITmpFileStore {
    public void setTmpeFile(TmpFile tmp);

    public String getStoreFileName();

    void save(File parentDir);

    public class TmpFile {
        public final File tmp;

        public TmpFile(File tmp) {
            this.tmp = tmp;
        }

        public void saveToDir(File dir, String fileName) {
            if (StringUtils.isEmpty(fileName)) {
                throw new IllegalArgumentException("param fileName can not be empty");
            }
            File target = new File(dir, fileName);
            if (!tmp.exists()) {
                throw new IllegalStateException("tmp file is not exist,path:" + tmp.getAbsolutePath());
            }
            try {
                FileUtils.copyFile(tmp, target);
                FileUtils.deleteQuietly(tmp);
            } catch (IOException e) {
                throw new RuntimeException(target.getAbsolutePath(), e);
            }
        }
    }
}
