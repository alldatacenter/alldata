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
package com.qlangtech.tis;

import ch.qos.logback.classic.util.ContextInitializer;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.web.start.TisAppLaunch;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-08-27 11:48
 */
public abstract class BaseTestCase extends TestCase {

    static {
        System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "logback-assemble.xml");
        try {
            File logDir = new File("/tmp/logs/tis");
            File assembleLogdir = new File(logDir, "assemble");
            FileUtils.forceMkdir(logDir);
            FileUtils.cleanDirectory(logDir);
            System.setProperty(TisAppLaunch.KEY_LOG_DIR, logDir.getAbsolutePath());
            System.setProperty(TisAppLaunch.KEY_ASSEMBLE_TASK_DIR, assembleLogdir.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
