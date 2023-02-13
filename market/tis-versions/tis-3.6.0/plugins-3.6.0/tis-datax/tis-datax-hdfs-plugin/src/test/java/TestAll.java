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

import com.qlangtech.tis.kerberos.TestKerberosCfg;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

//import com.qlangtech.tis.plugin.datax.TestDataXHdfsReader;
//import com.qlangtech.tis.plugin.datax.TestDataXHdfsWriter;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-22 16:19
 **/
@RunWith(Suite.class)
@Suite.SuiteClasses({TestKerberosCfg.class})
public class TestAll {

//    public static Test suite() {
//        TestSuite suite = new TestSuite();
//        suite.addTestSuite(TestHdfsFileSystemFactory.class);


        //suite.addTestSuite(TestHiveRemoveHistoryDataTask.class);
//        suite.addTestSuite(TestDataXHdfsReader.class);
//        suite.addTestSuite(TestDataXHdfsWriter.class);

//        suite.addTestSuite(TestHiveInsertFromSelectParser.class);
//        suite.addTestSuite(TestHiveDBUtils.class);
        // suite.addTestSuite(TestYarnTableDumpFactory.class);
//        return suite;
//    }
}
