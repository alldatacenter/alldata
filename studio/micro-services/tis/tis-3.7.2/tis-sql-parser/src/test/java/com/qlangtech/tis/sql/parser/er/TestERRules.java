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
package com.qlangtech.tis.sql.parser.er;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
import com.qlangtech.tis.sql.parser.meta.*;
import junit.framework.TestCase;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestERRules extends TestCase {

    static Map<String, DependencyNode> nodes = Maps.newHashMap();

    private static final String topology_totalpay = "totalpay";

    public static ERRules totalpayErRules = new ERRules();


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CenterResource.setNotFetchFromCenterRepository();
        Config.setDataDir("./dataflow");
        try {
            SqlTaskNodeMeta.SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology(topology_totalpay);
            List<DependencyNode> dumpNodes = topology.getDumpNodes();
            dumpNodes.stream().forEach((r) -> {
                nodes.put(r.getName(), r);
                r.setExtraSql(null);
            });
            totalpayErRules = new ERRules();
            totalpayErRules.addRelation($("3c1df235-6ddd-aca4-c02f-c5685f3a9c11", "card", "totalpayinfo", TabCardinality.ONE_N).addJoinerKey("id", "card_id").addJoinerKey("entity_id"));
            totalpayErRules.addRelation($("8b9489a1-ca5f-a3a9-6a68-73e3b63ed709", "customer", "card", TabCardinality.ONE_N).addJoinerKey("id", "customer_id").addJoinerKey("entity_id"));
            totalpayErRules.addRelation($("f3b9f910-00fd-8f59-10c9-a1b453a99495", "totalpayinfo", "orderdetail", TabCardinality.ONE_ONE).addJoinerKey("totalpay_id", "totalpay_id").addJoinerKey("entity_id"));
            totalpayErRules.addRelation($("f94cf5c4-1ace-2fbf-6843-c3528c879156", "ent_expense", "ent_expense_order", TabCardinality.ONE_N).addJoinerKey("enterprise_id", "enterprise_id").addJoinerKey("expense_id"));
            totalpayErRules.addRelation($("ba8491ac-8fb3-483b-7c57-74f591148b35", "totalpayinfo", "ent_expense_order", TabCardinality.ONE_ONE).addJoinerKey("totalpay_id", "totalpay_id").addJoinerKey("entity_id"));
            totalpayErRules.addRelation($("572267ae-f3f6-cbf4-dcc2-25e89a3aceb3", "totalpayinfo", "payinfo", TabCardinality.ONE_N).addJoinerKey("totalpay_id", "totalpay_id").addJoinerKey("entity_id"));
            totalpayErRules.addRelation($("8079762a-7c5c-80fd-135d-8e1a9a621d5c", "orderdetail", "instancedetail", TabCardinality.ONE_N).addJoinerKey("order_id", "order_id").addJoinerKey("entity_id"));
            totalpayErRules.addRelation($("df26982d-4dd5-9bea-3439-68020cbc8b06", "totalpayinfo", "specialfee", TabCardinality.ONE_N).addJoinerKey("totalpay_id", "totalpay_id").addJoinerKey("entity_id"));
            totalpayErRules.addRelation($("7f0eb8d2-b9ad-9f34-683d-35f984ad60fc", "totalpayinfo", "servicebillinfo", TabCardinality.ONE_ONE).addJoinerKey("totalpay_id", "servicebill_id").addJoinerKey("entity_id", "entity_id"));
            totalpayErRules.addRelation($("a89f7bf6-01b4-e9b1-c604-05212d373b52", "orderdetail", "order_bill", TabCardinality.ONE_ONE).addJoinerKey("order_id", "order_id").addJoinerKey("entity_id", "entity_id"));
            totalpayErRules.addRelation($("f8f043a3-b463-5539-1e28-258d73256979", "orderdetail", "takeout_order_extra", TabCardinality.ONE_ONE).addJoinerKey("order_id").addJoinerKey("entity_id"));
            // totalpayErRules.addIgnoreIncrTriggerEntity("card");
            // totalpayErRules.addIgnoreIncrTriggerEntity("customer");
            // totalpayErRules.addIgnoreIncrTriggerEntity("mall_shop");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testERRulesParse() {
        ERRules erRule = getTotalpayErRules();
        Map<String, TableRelation> testMap = Maps.newHashMap();
        List<TableRelation> rList = erRule.getRelationList();
        for (TableRelation r : rList) {
            testMap.put(r.getId(), r);
            // System.out.println(r.getId() + ",p:" + r.getParent().getName() + ",c:" + r.getChild().getName());
        }
        ERRules example = totalpayErRules;
        // testRelation = null;
        for (TableRelation r : example.getRelationList()) {
            // testMap.put(r.getId(), r);
            TableRelation testRelation = testMap.get(r.getId());
            assertNotNull("id:" + r.getId(), testRelation);
            testRelationEqual(testRelation, r);
            testDependencyNodeEqual(testRelation.getChild(), r.getChild());
            testDependencyNodeEqual(testRelation.getParent(), r.getParent());
            assertEquals(testRelation.getCardinality(), r.getCardinality());
            assertTrue(testRelation.getJoinerKeys().size() > 0);
            assertEquals(r.getId(), r.getJoinerKeys().size(), testRelation.getJoinerKeys().size());
            r.getJoinerKeys().forEach((rr) -> {
                Optional<JoinerKey> find = testRelation.getJoinerKeys().stream().filter((tt) -> {
                    return StringUtils.equals(rr.getChildKey(), tt.getChildKey()) && StringUtils.equals(rr.getParentKey(), tt.getParentKey());
                }).findFirst();
                assertTrue(rr.toString(), find.isPresent());
            });
        }
        // Optional<String> first = null;
        // for (String ignoreTab : totalpayErRules.getIgnoreIncrTriggerEntities()) {
        // first = erRule.getIgnoreIncrTriggerEntities().stream().filter((r) -> StringUtils.equals(ignoreTab, r)).findFirst();
        // assertTrue("ignoreTab:" + ignoreTab + " shall exist", first.isPresent());
        // }
        final String totalpayinfo = "totalpayinfo";
        final String orderdetail = "orderdetail";
        List<String> expectIgnoreTab = Lists.newArrayList("card", "customer", "mall_shop", "takeout_order_extra");
        List<String> ignoreTabs = erRule.getIgnoreIncrTriggerEntities();
        assertEquals("actual:" + ignoreTabs.stream().collect(Collectors.joining(",")), 4, ignoreTabs.size());
        CollectionUtils.isEqualCollection(expectIgnoreTab, ignoreTabs);
        List<PrimaryTableMeta> primaryTabs = erRule.getPrimaryTabs();
        assertEquals(1, primaryTabs.size());
        PrimaryTableMeta primaryTableMeta = primaryTabs.get(0);
        assertEquals(totalpayinfo, primaryTableMeta.getTabName());
      //  assertEquals("totalpay_id", primaryTableMeta.getPrimaryKeyNames());
        Set<String> pks = Sets.newHashSet("totalpay_id", "entity_id");
        for (PrimaryLinkKey pk : primaryTableMeta.getPrimaryKeyNames()) {
            assertTrue(pk.getName() + " must contain in pks", pks.contains(pk.getName()));
        }

        List<TabFieldProcessor> tabFieldProcessors = erRule.getTabFieldProcessors();
        assertEquals(9, tabFieldProcessors.size());
        Map<String, TabFieldProcessor> collect = tabFieldProcessors.stream().collect(Collectors.toMap((r) -> r.getName(), (r) -> r));
        // TODO further assert for tabFieldProcessors
        TabFieldProcessor totalpayProcess = collect.get(totalpayinfo);
        assertNotNull(totalpayProcess);
        TabFieldProcessor orderProcess = collect.get(orderdetail);
        assertNotNull(orderProcess);
        // ignoreIncrTriggerEntities: ["card","customer","mall_shop"]
    }

    private void testDependencyNodeEqual(DependencyNode test, DependencyNode exampe) {
        assertEquals(exampe.getId(), test.getId());
        assertEquals(exampe.getPosition().getX(), test.getPosition().getX());
        assertEquals(exampe.getPosition().getY(), test.getPosition().getY());
        assertEquals(exampe.parseNodeType(), test.parseNodeType());
        assertEquals(exampe.getTabid(), test.getTabid());
        assertEquals(exampe.getDbid(), test.getDbid());
        assertEquals(exampe.getName(), test.getName());
    }

    private void testRelationEqual(TableRelation t, TableRelation example) {
        assertEquals(example.getId(), t.getId());
        assertEquals(example.getCardinality(), t.getCardinality());
    }

    public static ERRules getTotalpayErRules() {
        return ERRules.getErRule(topology_totalpay).get();
    }

    public void testRuleParse() throws Exception {
        ERRules erRules = new ERRules();
        erRules.addRelation($("1", "orderdetail", "totalpayinfo", TabCardinality.ONE_ONE).addJoinerKey("order_id", "order_id"));
        erRules.addRelation($("2", "ent_expense", "ent_expense_order", TabCardinality.ONE_N).addJoinerKey("enterprise_id", "enterprise_id").addJoinerKey("expense_id", "expense_id"));
        erRules.addRelation($("3", "totalpayinfo", "ent_expense_order", TabCardinality.ONE_ONE).addJoinerKey("totalpay_id", "totalpay_id"));
        erRules.addRelation($("3", "totalpayinfo", "payinfo", TabCardinality.ONE_N).addJoinerKey("totalpay_id", "totalpay_id").addJoinerKey("entity_id", "entity_id"));
        erRules.addRelation($("4", "orderdetail", "instancedetail", TabCardinality.ONE_N).addJoinerKey("order_id", "order_id").addJoinerKey("entity_id", "entity_id"));
        erRules.addRelation($("5", "totalpayinfo", "specialfee", TabCardinality.ONE_N).addJoinerKey("totalpay_id", "totalpay_id").addJoinerKey("entity_id", "entity_id"));
        erRules.addRelation($("6", "totalpayinfo", "servicebillinfo", TabCardinality.ONE_ONE).addJoinerKey("totalpay_id", "servicebill_id").addJoinerKey("entity_id", "entity_id"));
        erRules.addRelation($("7", "orderdetail", "order_bill", TabCardinality.ONE_ONE).addJoinerKey("order_id", "order_id").addJoinerKey("entity_id", "entity_id"));
        erRules.addRelation($("8", "orderdetail", "takeout_order_extra", TabCardinality.ONE_ONE).addJoinerKey("order_id").addJoinerKey("entity_id"));
        DependencyNode testNode = new DependencyNode();
        testNode.setName("testtable");
        Position testPost = new Position();
        testPost.setY(99);
        testPost.setX(99);
        testNode.setPosition(testPost);
        TabExtraMeta tabExtraMeta = new TabExtraMeta();
        tabExtraMeta.setMonitorTrigger(true);
        tabExtraMeta.setPrimaryIndexTab(true);
        ColumnTransfer colTransfer = new ColumnTransfer("testcol", "testTransfer", "testvalue");
        tabExtraMeta.addColumnTransfer(colTransfer);
        testNode.setExtraMeta(tabExtraMeta);
        erRules.addDumpNode(testNode);
        System.out.println(erRules.getRelationList().size());
        String serString = ERRules.serialize(erRules);
        System.out.println(serString);
        ERRules rules = ERRules.deserialize(serString);
        Optional<DependencyNode> findTestNode = rules.getDumpNodes().stream().filter((r) -> StringUtils.equals(testNode.getName(), r.getName())).findFirst();
        assertTrue(findTestNode.isPresent());
        testDependencyNodeEqual(findTestNode.get(), testNode);
        List<ColumnTransfer> colTransfers = findTestNode.get().getExtraMeta().getColTransfers();
        assertEquals(1, colTransfers.size());
        Assert.assertNotNull(rules);
    }

    static TableRelation $(String id, String parent, String child, TabCardinality c) {
        return new TableRelation(id, nodes.get(parent), nodes.get(child), c);
    }
}
