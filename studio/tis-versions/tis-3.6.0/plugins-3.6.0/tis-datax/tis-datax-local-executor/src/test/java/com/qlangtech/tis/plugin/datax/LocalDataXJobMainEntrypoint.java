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

import com.qlangtech.tis.datax.CuratorDataXTaskMessage;
import com.qlangtech.tis.datax.DataXJobSingleProcessorExecutor;
import com.qlangtech.tis.manage.common.Config;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-08-29 13:49
 **/
public class LocalDataXJobMainEntrypoint {

    public static final int testAllRows = 98765;

    static int executeCount = 0;

    public static void main(String[] args) {
        executeCount++;
        //System.out.println("===============hello" + args[0] + "\n" + args[1] + "\n" + args[2] + "\n" + args[3]);
        if (6 != args.length) {
            throw new AssertionError("6 != args.length");
        }

        if (Boolean.parseBoolean(System.getProperty("env_props"))) {
            throw new IllegalStateException("env_props must be false");
        }

        //  CenterResource.notFetchFromCenterRepository()
        if (!Boolean.parseBoolean(System.getProperty("notFetchFromCenterRepository"))) {
            throw new IllegalStateException("must be notFetchFromCenterRepository");
        }

        assertEquals(String.valueOf(TestLocalDataXJobSubmit.TaskId), args[0]);
        assertEquals(TestLocalDataXJobSubmit.dataXfileName, args[1]);// = "customer_order_relation_0.json";
        assertEquals(TestLocalDataXJobSubmit.dataXName, args[2]);//= "baisuitestTestcase";
        assertEquals(TestLocalDataXJobSubmit.statusCollectorHost, args[3]);// = "127.0.0.1:3489";
        assertEquals("local", args[4]);
        assertEquals(String.valueOf(testAllRows), args[5]);

        assertEquals(Config.SYSTEM_KEY_LOGBACK_PATH_VALUE, System.getProperty(Config.SYSTEM_KEY_LOGBACK_PATH_KEY));
    }

    private static void assertEquals(String expect, String actual) {
        if (!expect.equals(actual)) {
            throw new AssertionError("is not equal,expect:" + expect + ",actual:" + actual);
        }
    }

}
