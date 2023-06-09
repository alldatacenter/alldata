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

import com.qlangtech.tis.exec.TestActionInvocation;
import com.qlangtech.tis.exec.datax.TestDataXExecuteInterceptor;
import com.qlangtech.tis.exec.datax.TestDataXExecuteInterceptorForMethodBuildTaskTriggers;
import com.qlangtech.tis.full.dump.TestDefaultChainContext;
import com.qlangtech.tis.fullbuild.servlet.TestTisServlet;
import com.qlangtech.tis.fullbuild.taskflow.TestReactor;
import com.qlangtech.tis.log.TestRealtimeLoggerCollectorAppender;
import com.qlangtech.tis.order.center.TestIndexSwapTaskflowLauncherWithDataXTrigger;
import com.qlangtech.tis.rpc.server.TestIncrStatusServer;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */


@RunWith(Suite.class)
@Suite.SuiteClasses(
        {TestDataXExecuteInterceptorForMethodBuildTaskTriggers.class
                , TestDataXExecuteInterceptor.class
                , TestRealtimeLoggerCollectorAppender.class
                , TestIncrStatusServer.class
                , TestDefaultChainContext.class
                , TestTisServlet.class
                , TestActionInvocation.class
                , TestReactor.class
                , TestIndexSwapTaskflowLauncherWithDataXTrigger.class})
public class TestAll  //extends TestCase
{
    static {
        System.setProperty("logback.configurationFile", "src/main/resources/logback-assemble.xml");
    }

//    public static Test suite() {
//        TestSuite suite = new TestSuite();
//        suite.addTestSuite();
//        suite.addTestSuite();
//        suite.addTestSuite();
//        suite.addTestSuite();
//      //  suite.addTestSuite(TestIndexSwapTaskflowLauncher.class);
//       // suite.addTestSuite(TestIndexSwapTaskflowLauncherWithSingleTableIndexBuild.class);
//        suite.addTestSuite();
//        suite.addTestSuite();
//        suite.addTestSuite();
//        suite.addTestSuite();
//        suite.addTestSuite();
//        return suite;
//    }
}
