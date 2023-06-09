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

import com.qlangtech.tis.sql.parser.*;
import com.qlangtech.tis.sql.parser.er.TestERRules;
import com.qlangtech.tis.sql.parser.shop.TestShopTopologyParse;
import com.qlangtech.tis.sql.parser.stream.generate.TestFlatTableRelation;
import com.qlangtech.tis.sql.parser.supplyGoods.TestSupplyGoodsParse;
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
        suite.addTestSuite(TestTopologyDir.class);
        suite.addTestSuite(TestShopTopologyParse.class);
        suite.addTestSuite(TestSupplyGoodsParse.class);
        suite.addTestSuite(TestFlatTableRelation.class);
        suite.addTestSuite(TestSqlDataFlowTopology.class);
        suite.addTestSuite(TestSqlRewriter.class);
        suite.addTestSuite(TestSqlTaskNodeMeta.class);
        suite.addTestSuite(TestSqlTaskNode.class);
//        suite.addTestSuite(TestStreamComponentCodeGenerator.class);
//        suite.addTestSuite(TestS4EmployeeStreamComponentCodeGenerator.class);

        // suite.addTestSuite(TestMqConfigMeta.class);
        suite.addTestSuite(TestDBNode.class);
        suite.addTestSuite(TestERRules.class);
        return suite;
    }
}
