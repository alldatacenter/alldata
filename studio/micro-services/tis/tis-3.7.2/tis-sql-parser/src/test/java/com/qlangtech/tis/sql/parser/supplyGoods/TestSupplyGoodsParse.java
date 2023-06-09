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
package com.qlangtech.tis.sql.parser.supplyGoods;

import com.qlangtech.tis.sql.parser.SqlTaskBaseTestCase;
import com.qlangtech.tis.sql.parser.SqlTaskNode;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.ColRef;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-07-01 13:46
 */
public class TestSupplyGoodsParse extends SqlTaskBaseTestCase {

    public static final String topologyName = "supply_goods";

    public void testTopologyParse() throws Exception {
        // this.parseSqlTaskNode()
        List<SqlTaskNode> processNodes = this.parseTopologySqlTaskNodes(topologyName);
        assertTrue(processNodes.size() > 0);
        assertEquals(4, processNodes.size());
        SqlTaskNodeMeta.SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology(topologyName);
        TableTupleCreator tableTupleCreator = topology.parseFinalSqlTaskNode();
        ColRef.ListMap cols = tableTupleCreator.getColsRefs().getColRefMap();
        assertTrue(cols.size() > 0);
        assertEquals("parse from suppyGoods cols size", 58, cols.size());
        assertNotNull("tableTupleCreator", tableTupleCreator);
    }
}
