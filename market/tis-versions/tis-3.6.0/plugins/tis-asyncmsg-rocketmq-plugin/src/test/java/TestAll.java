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
import com.qlangtech.async.message.client.consumer.TestRocketMQListenerFactory;
import com.qlangtech.tis.component.TestIncrComponent;
import com.qlangtech.tis.component.TestPlugin;
import com.qlangtech.tis.component.TestRockMqPluginValidate;
import com.qlangtech.tis.util.TestHeteroList;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/*
 * @create: 2020-01-15 14:20
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestAll extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(TestPlugin.class);
        suite.addTestSuite(TestIncrComponent.class);
        suite.addTestSuite(TestRockMqPluginValidate.class);
        suite.addTestSuite(TestHeteroList.class);
        suite.addTestSuite(TestRocketMQListenerFactory.class);
        return suite;
    }
}
