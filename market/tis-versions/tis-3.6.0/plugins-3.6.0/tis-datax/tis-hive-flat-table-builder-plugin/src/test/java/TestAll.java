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

import com.qlangtech.tis.dump.hive.TestHiveDBUtils;
import com.qlangtech.tis.dump.hive.TestHiveRemoveHistoryDataTask;
import com.qlangtech.tis.hive.TestDefaultHiveConnGetter;
import com.qlangtech.tis.hive.TestHiveInsertFromSelectParser;
import com.qlangtech.tis.plugin.datax.TestDataXHiveWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author: baisui 百岁
 * @create: 2020-06-03 14:09
 **/
public class TestAll extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        // suite.addTestSuite(TestHdfsFileSystemFactory.class);
        suite.addTestSuite(TestDefaultHiveConnGetter.class);
        suite.addTestSuite(TestHiveRemoveHistoryDataTask.class);
//        suite.addTestSuite(TestDataXHdfsReader.class);
//        suite.addTestSuite(TestDataXHdfsWriter.class);
        suite.addTestSuite(TestDataXHiveWriter.class);
        suite.addTestSuite(TestHiveInsertFromSelectParser.class);
        suite.addTestSuite(TestHiveDBUtils.class);
        // suite.addTestSuite(TestYarnTableDumpFactory.class);
        return suite;
    }
}
