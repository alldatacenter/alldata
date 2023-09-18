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

package com.qlangtech.plugins.incr.flink.junit;

import com.qlangtech.plugins.incr.flink.TISFlinkClassLoaderFactory;
import com.qlangtech.tis.manage.common.Config;
import org.junit.rules.TestRule;
import org.junit.runner.Description;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-19 18:04
 **/
public class TISApplySkipFlinkClassloaderFactoryCreation implements TestRule {

    //    @ClassRule(order = 100)
//    public static TestRule name = new TestRule() {
    @Override
    public org.junit.runners.model.Statement apply(org.junit.runners.model.Statement base, Description description) {
        System.setProperty(TISFlinkClassLoaderFactory.SKIP_CLASSLOADER_FACTORY_CREATION, "true");
        System.setProperty(Config.SYSTEM_KEY_LOGBACK_PATH_KEY, "logback-test.xml");
        return base;
    }
//    };
}
