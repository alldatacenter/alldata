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

import com.qlangtech.tis.coredefine.module.action.TestPluginAction;
import com.qlangtech.tis.manage.servlet.TestIncrTagHeatBeatMonitor;
import com.qlangtech.tis.offline.module.action.TestOfflineDatasourceAction;
import com.qlangtech.tis.runtime.module.action.TestSchemaAction;
import com.qlangtech.tis.runtime.module.action.TestSysInitializeAction;
import com.qlangtech.tis.solrdao.TestSchemaResult;
import com.qlangtech.tis.util.TestPluginItems;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestAll extends TestCase {

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(TestPluginItems.class);
    suite.addTestSuite(TestIncrTagHeatBeatMonitor.class);
    // suite.addTestSuite(TestTISK8sDelegate.class);
    //suite.addTestSuite(TestCoreAction.class);

    suite.addTestSuite(TestSysInitializeAction.class);
    suite.addTestSuite(TestSchemaResult.class);
    suite.addTestSuite(TestSchemaAction.class);
    //suite.addTestSuite(TestCollectionAction.class);
    suite.addTestSuite(TestPluginAction.class);
    suite.addTestSuite(TestOfflineDatasourceAction.class);

    return suite;
  }
}
