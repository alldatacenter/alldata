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

import com.qlangtech.tis.plugin.datax.TestDataXOracleReader;
import com.qlangtech.tis.plugin.datax.TestDataXOracleReaderDump;
import com.qlangtech.tis.plugin.datax.TestDataXOracleWriter;
import com.qlangtech.tis.plugin.ds.oracle.TestOracleDataSourceFactory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-09 09:22
 **/
@RunWith(Suite.class)
@Suite.SuiteClasses({TestDataXOracleReaderDump.class, TestDataXOracleReader.class
        , TestOracleDataSourceFactory.class, TestDataXOracleWriter.class})
public class TestAll //extends TestCase
{
//    public static Test suite() {
//        TestSuite suite = new TestSuite();
//        suite.addTestSuite(TestOracleDataSourceFactory.class);
//        suite.addTestSuite(TestDataXOracleReader.class);
//        suite.addTestSuite(TestDataXOracleWriter.class);
//        return suite;
//    }
}
