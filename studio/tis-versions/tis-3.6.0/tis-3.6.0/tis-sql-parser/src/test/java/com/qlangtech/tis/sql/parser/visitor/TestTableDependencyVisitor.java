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
package com.qlangtech.tis.sql.parser.visitor;

import java.util.List;
import java.util.Optional;
import com.qlangtech.tis.sql.parser.SqlTaskNode;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
import com.facebook.presto.sql.tree.Query;
import com.qlangtech.tis.common.utils.Assert;
import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestTableDependencyVisitor extends TestCase {

    public void testDependencies() throws Exception {
        List<SqlTaskNode> taskNodes = SqlTaskNodeMeta.getSqlDataFlowTopology("totalpay").parseTaskNodes();
        String order_customers = "order_customers";
        Optional<SqlTaskNode> taskNode = taskNodes.stream().filter((r) -> order_customers.equals(r.getExportName())).findFirst();
        Assert.assertTrue(order_customers + " shall be exist", taskNode.isPresent());
        TableDependencyVisitor dependenciesVisitor = TableDependencyVisitor.create();
        Query query = SqlTaskNode.parseQuery(taskNode.get().getContent());
        dependenciesVisitor.process(query, null);
        // for (String dependency : dependenciesVisitor.getTabDependencies()) {
        // System.out.println(dependency);
        // }
        // Assert.assertTrue(dependenciesVisitor.getTabDependencies().size() > 0);
        Assert.assertEquals(1, dependenciesVisitor.getTabDependencies().size());
        Assert.assertTrue(dependenciesVisitor.getTabDependencies().contains("instance"));
    }
}
