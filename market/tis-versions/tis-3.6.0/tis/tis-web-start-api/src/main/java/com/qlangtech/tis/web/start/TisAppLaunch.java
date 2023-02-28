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

package com.qlangtech.tis.web.start;

import java.io.File;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-07 17:27
 **/
public class TisAppLaunch {
    public static final String KEY_TIS_LAUNCH_PORT = "tis.launch.port";
    public static final String KEY_ASSEMBLE_TASK_DIR = "assemble.task.dir";
    public static final String KEY_LOG_DIR = "log.dir";
    private static boolean test = false;
    private final Integer launchPort;
    private static final String logDir;

    static {
        logDir = System.getProperty(KEY_LOG_DIR, "/opt/logs/tis");
        System.setProperty(TisAppLaunch.KEY_ASSEMBLE_TASK_DIR, logDir + "/assemble/task");
    }

    public static File getLogDir() {
        return new File(logDir);
    }

    public static TisAppLaunch instance;

    private TisAppLaunch() {
        if (logDir == null) {
            throw new IllegalStateException("logDir can not be null");
        }
        this.launchPort = Integer.parseInt(System.getProperty(KEY_TIS_LAUNCH_PORT, "8080"));
    }


    public static int getPort(TisSubModule context) {
        if (instance == null) {
            instance = new TisAppLaunch();
        }
        return instance.launchPort + context.portOffset;
    }

    public static final File getAssebleTaskDir() {
        return new File(System.getProperty(KEY_ASSEMBLE_TASK_DIR));
    }

    public static void setTest(boolean val) {
        test = val;
    }

    public static boolean isTestMock() {
        return test;
    }
}
