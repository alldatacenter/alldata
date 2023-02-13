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
package com.qlangtech.tis.sql.parser;

import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta.SqlDataFlowTopology;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.List;
import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class SqlTaskBaseTestCase extends TestCase {

    @Override
    protected void setUp() throws Exception {
        CenterResource.setNotFetchFromCenterRepository();
    }

    protected TableTupleCreator parseSqlTaskNode(String mediaTableName) throws Exception {
        List<SqlTaskNode> taskNodes = parseTopologySqlTaskNodes("totalpay");
        SqlTaskNode task = null;
        Optional<SqlTaskNode> taskNode = taskNodes.stream().filter((r) -> mediaTableName.equals(r.getExportName().getTabName())).findFirst();
        Assert.assertTrue(mediaTableName + " shall be exist", taskNode.isPresent());
        // Map<ColName, ValueOperator> columnTracer = Maps.newHashMap();
        // Rewriter rewriter = Rewriter.create(columnTracer);
        task = taskNode.get();
        /**
         * *******************************
         * 开始解析
         * *******************************
         */
        return task.parse(true);
    }

    protected List<SqlTaskNode> parseTopologySqlTaskNodes(String topologyName) throws Exception {
        SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology(topologyName);
        // SqlTaskNode.parseTaskNodes(topology);
        List<SqlTaskNode> taskNodes = topology.parseTaskNodes();
        return taskNodes;
    }
}
