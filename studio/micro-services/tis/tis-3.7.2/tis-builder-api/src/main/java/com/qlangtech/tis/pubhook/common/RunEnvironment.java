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
package com.qlangtech.tis.pubhook.common;

import com.qlangtech.tis.manage.common.Config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-25
 */
public enum RunEnvironment {

    // ///////////
    DAILY("daily", (short) 0, "日常环境", () -> Config.getConfigRepositoryHost()),
    // //////////////////////
    ONLINE(//
            "online", //
            (short) 2, // /
            "线上环境", // /
            null);


    public static final String KEY_RUNTIME = "runtime";

    public static boolean isDevelopMode() {
        return (getSysRuntime() == DAILY);
    }

    public static void setSysRuntime(RunEnvironment runtime) {
        System.setProperty(RunEnvironment.KEY_RUNTIME, runtime.getKeyName());
    }

    public static boolean isOnlineMode() {
        return (getSysRuntime() == ONLINE);
    }

    // private static RunEnvironment runtime;
    public static RunEnvironment getSysRuntime() {
        return RunEnvironment.getEnum(Config.getInstance().getRuntime());
    }

    public static RunEnvironment getSysEnvironment() {
        return getSysRuntime();
    }

    private final Short id;

    private final String keyName;

    private final String describe;

    private final Callable<String> innerRepositoryURL;


    private RunEnvironment(String keyName, Short id, String describe, Callable<String> innerRepositoryURL) {
        this.id = id;
        this.keyName = keyName;
        this.describe = describe;
        this.innerRepositoryURL = innerRepositoryURL;
    }

    public String getInnerRepositoryURL() {
        try {
            return innerRepositoryURL.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public Short getId() {
        return id;
    }

    public String getDescribe() {
        return describe;
    }

    public String getKeyName() {
        return keyName;
    }

    public static RunEnvironment getEnum(String key) {
        EnumSet<RunEnvironment> all = EnumSet.allOf(RunEnvironment.class);
        for (RunEnvironment r : all) {
            if (r.getKeyName().equals(key)) {
                return r;
            }
        }
        throw new IllegalArgumentException("key:" + key + " is invalid");
    }

    public static RunEnvironment getEnum(short key) {
        EnumSet<RunEnvironment> all = EnumSet.allOf(RunEnvironment.class);
        for (RunEnvironment r : all) {
            if (r.getId() == key) {
                return r;
            }
        }
        throw new IllegalArgumentException("key:" + key + " is invalid");
    }

    private static List<RunEnvironment> environmentList = new ArrayList<RunEnvironment>();

    static {
        try {
            RunEnvironment[] fields = RunEnvironment.values();
            // Object o = null;
            for (RunEnvironment f : fields) {
                // o = f.get(null);
                // if (o instanceof RunEnvironment) {
                // environmentList.add(((RunEnvironment) o));
                // }
                environmentList.add(f);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<RunEnvironment> getRunEnvironmentList() {
        return environmentList;
    }

    public static void main(String[] arg) throws Exception {
        List<RunEnvironment> environmentList = RunEnvironment.getRunEnvironmentList();
        for (RunEnvironment envir : environmentList) {
            System.out.println(envir);
        }
    }
}
