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

package com.qlangtech.tis.config;

import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.utils.MD5Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-15 17:55
 **/
public class Utils {
    private Utils() {

    }

    public static void setHadoopConfig2Local(File cfgDir, String fileName, String content) {
        if (StringUtils.isEmpty(content)) {
            throw new IllegalArgumentException("fileName:" + fileName + " relevant content can not be empty");
        }
        try {
            File ys = new File(cfgDir, fileName);
//           // String yarnConfigDir = System.getenv("YARN_CONF_DIR");
//            if (StringUtils.isEmpty(yarnConfigDir) || !yarnConfigDir.equals(cfgDir.getCanonicalPath())) {
//                throw new IllegalStateException("yarnConfigDir is illegal:" + yarnConfigDir
//                        + ",cfgDir:" + cfgDir.getCanonicalPath());
//            }
            if (!ys.exists()) {
                FileUtils.write(ys, content, TisUTF8.get(), false);
            } else if (!StringUtils.equals(MD5Utils.md5file(content.getBytes(TisUTF8.get())), MD5Utils.md5file(ys))) {
                // 先备份
                FileUtils.moveFile(ys, new File(cfgDir, fileName + "_bak_" + TimeFormat.yyyyMMddHHmmss.format(TimeFormat.getCurrentTimeStamp())));
                FileUtils.write(ys, content, TisUTF8.get(), false);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
