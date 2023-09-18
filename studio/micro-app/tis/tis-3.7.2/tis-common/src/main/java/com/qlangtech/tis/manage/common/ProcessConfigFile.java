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
package com.qlangtech.tis.manage.common;

import org.apache.commons.lang.StringUtils;
import com.qlangtech.tis.manage.common.ConfigFileContext.ContentProcess;
import com.qlangtech.tis.pubhook.common.ConfigConstant;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-4-26
 */
public class ProcessConfigFile {

    private final ConfigFileContext.ContentProcess process;

    private final SnapshotDomain domain;

    public ProcessConfigFile(ContentProcess process, SnapshotDomain domain) {
        super();
        this.process = process;
        this.domain = domain;
    }

    public void execute() {
        try {
            for (PropteryGetter getter : ConfigFileReader.getConfigList()) {
//                if (ConfigConstant.FILE_JAR.equals(getter.getFileName())) {
//                    continue;
//                }
                // 没有属性得到
                if (getter.getUploadResource(domain) == null) {
                    continue;
                }
                String md5 = getter.getMd5CodeValue(this.domain);
                if (StringUtils.isBlank(md5)) {
                    continue;
                }
                try {
                    Thread.sleep(500);
                } catch (Throwable e) {
                }
                process.execute(getter, getter.getContent(this.domain));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
