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

import com.qlangtech.tis.datax.impl.TestDataxProcessor;
import com.qlangtech.tis.sql.parser.stream.generate.TestS4EmployeeStreamComponentCodeGenerator;
import com.qlangtech.tis.sql.parser.stream.generate.TestStreamComponentCodeGenerator;
import com.qlangtech.tis.sql.parser.stream.generate.TestTikvEmployee;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestAll_bak extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(TestS4EmployeeStreamComponentCodeGenerator.class);
        suite.addTestSuite(TestStreamComponentCodeGenerator.class);
        suite.addTestSuite(TestTikvEmployee.class);
        suite.addTestSuite(TestDataxProcessor.class);
        return suite;
    }
}
