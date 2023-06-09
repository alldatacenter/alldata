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

import com.google.common.base.Joiner;
import com.qlangtech.tis.sql.parser.er.TestERRules;
import com.qlangtech.tis.sql.parser.tuple.creator.IEntityNameGetter;
import com.qlangtech.tis.sql.parser.tuple.creator.IValChain;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TaskNodeTraversesCreatorVisitor;

import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestValChainListTupleMap extends SqlTaskBaseTestCase {

    private static final String totalpay_summary = "totalpay_summary";

    public void generateCode() throws Exception {
        TableTupleCreator totalpaySummaryTuple = this.parseSqlTaskNode(totalpay_summary);
        TaskNodeTraversesCreatorVisitor visitor = new TaskNodeTraversesCreatorVisitor(TestERRules.getTotalpayErRules());
        totalpaySummaryTuple.accept(visitor);
        Map<IEntityNameGetter, List<IValChain>> tabTriggers = visitor.getTabTriggerLinker();
        for (Map.Entry<IEntityNameGetter, List<IValChain>> e : tabTriggers.entrySet()) {
            System.out.println(e.getKey().getEntityName());
            if ("payinfo".equals(e.getKey().getEntityName().getTabName()) || "card".equals(e.getKey().getEntityName().getTabName())) {
                System.out.println("====================================================");
                System.out.println(e.getKey().getEntityName().getTabName());
                for (IValChain chain : e.getValue()) {
                    System.out.println(Joiner.on("->").join(chain.mapChainValve((r) -> {
                        return r.getIdentityName();
                    }).iterator()));
                }
            }
        }
    }
}
