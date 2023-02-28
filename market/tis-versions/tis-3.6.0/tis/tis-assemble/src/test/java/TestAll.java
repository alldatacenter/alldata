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

import com.qlangtech.tis.exec.TestActionInvocation;
import com.qlangtech.tis.exec.datax.TestDataXExecuteInterceptor;
import com.qlangtech.tis.flume.TestTisPropertiesFileConfigurationProvider;
import com.qlangtech.tis.full.dump.TestDefaultChainContext;
import com.qlangtech.tis.fullbuild.servlet.TestTisServlet;
import com.qlangtech.tis.fullbuild.taskflow.TestReactor;
import com.qlangtech.tis.log.TestRealtimeLoggerCollectorAppender;

import com.qlangtech.tis.order.center.TestIndexSwapTaskflowLauncherWithDataXTrigger;
import com.qlangtech.tis.rpc.server.TestIncrStatusServer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestAll extends TestCase {
    static {
        System.setProperty("logback.configurationFile", "src/main/resources/logback-assemble.xml");
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(TestDefaultChainContext.class);
        suite.addTestSuite(TestTisServlet.class);
        suite.addTestSuite(TestActionInvocation.class);
        suite.addTestSuite(TestReactor.class);
      //  suite.addTestSuite(TestIndexSwapTaskflowLauncher.class);
       // suite.addTestSuite(TestIndexSwapTaskflowLauncherWithSingleTableIndexBuild.class);
        suite.addTestSuite(TestIndexSwapTaskflowLauncherWithDataXTrigger.class);
        suite.addTestSuite(TestIncrStatusServer.class);
        suite.addTestSuite(TestRealtimeLoggerCollectorAppender.class);
        suite.addTestSuite(TestDataXExecuteInterceptor.class);
        suite.addTestSuite(TestTisPropertiesFileConfigurationProvider.class);
        return suite;
    }
}
