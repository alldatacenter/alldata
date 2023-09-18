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

package com.qlangtech.tis.web.start;

import java.io.File;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-07 17:27
 **/
public class TisAppLaunch {
    public static final String KEY_TIS_LAUNCH_PORT = "tis.launch.port";
    public static final String KEY_ASSEMBLE_TASK_DIR = "assemble.task.dir";
    public static final String KEY_LOG_DIR = "log.dir";
    private static boolean test = false;
    public  final Integer launchPort;
    private static final String logDir;

    // 运行模式
    private TisRunMode runMode;

    /**
     * Zeppelin 是否被激活
     */
    private final boolean zeppelinHomeSetted;
    private boolean zeppelinContextInitialized;
    private final File zeppelinHome;

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
        String zeppelinHome = System.getenv("ZEPPELIN_HOME");
        this.zeppelinHomeSetted = zeppelinHome != null;
        this.zeppelinHome = (this.zeppelinHomeSetted) ? new File(zeppelinHome) : null;
        if (this.zeppelinHomeSetted) {
            if (!this.zeppelinHome.exists() || !this.zeppelinHome.isDirectory()) {
                throw new IllegalStateException("zeppelinHome is not valid:" + this.zeppelinHome.getAbsolutePath());
            }
        }
    }

    public void setRunMode(TisRunMode runMode) {
        this.runMode = runMode;
    }

    public TisRunMode getRunMode() {
        if (this.runMode == null && isTestMock()) {
            this.runMode = TisRunMode.LocalTest;
        }
        return Objects.requireNonNull(this.runMode, "runMode can not be null");
    }

    public void setZeppelinContextInitialized() {
        this.zeppelinContextInitialized = true;
    }

    public boolean isZeppelinContextInitialized() {
        return zeppelinContextInitialized;
    }

    public File getZeppelinHome() {
        return this.zeppelinHome;
    }

    public boolean isZeppelinHomeSetted() {
        return this.zeppelinHomeSetted;
    }

    /**
     * zeppelin 在TIS中是否可用？
     *
     * @return
     */
    public boolean isZeppelinActivate() {


        return this.isZeppelinHomeSetted() && this.getRunMode().zeppelinContextInitialized.get();
    }

//    public static int getPort(TisSubModule context) {
//        TisAppLaunch i = get();
//        return i.launchPort + context.portOffset;
//    }

    public static TisAppLaunch get() {
        if (instance == null) {
            instance = new TisAppLaunch();
        }
        return instance;
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
