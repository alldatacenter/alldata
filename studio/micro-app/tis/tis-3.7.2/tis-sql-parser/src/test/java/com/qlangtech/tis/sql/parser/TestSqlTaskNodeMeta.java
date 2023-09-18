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
package com.qlangtech.tis.sql.parser;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.sql.parser.er.ERRules;
import com.qlangtech.tis.sql.parser.exception.TisSqlFormatException;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.meta.Position;
import com.qlangtech.tis.sql.parser.supplyGoods.TestSupplyGoodsParse;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestSqlTaskNodeMeta extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CenterResource.setNotFetchFromCenterRepository();
        System.clearProperty(Config.KEY_DATA_DIR);
        SqlTaskNode.parent = new File(Config.getMetaCfgDir(), IFullBuildContext.NAME_DATAFLOW_DIR);
    }

    private final File parent = new File("./src/main/resources/totalpay");


    public void testValidateSql() {

        List<DependencyNode> dependencyNodes = Lists.newArrayList();

        // 这个sql语句有错误，需要校验成错误，抛异常
        Optional<TisSqlFormatException> err = SqlTaskNodeMeta.validateSql("    SELECT g.id,g.entity_id,g.commodity_id,gg.goods_id\n" +
                "     FROM commodity_goods g", dependencyNodes);

        assertTrue(err.isPresent());

        assertEquals("base ref:gg can not find relevant table entity in map,mapSize:1,exist:[g:commodity_goods],位置，行:1,列:44", err.get().summary());
    }

    /**
     * dependencyNodes 集合中的表不在sql的FROM部分中，校验需要失败
     */
    public void testValidateSql2() {

        List<DependencyNode> dependencyNodes = Lists.newArrayList();

        // 这个sql语句有错误，需要校验成错误，抛异常
        Optional<TisSqlFormatException> err = SqlTaskNodeMeta.validateSql("    SELECT g.id,g.entity_id,g.commodity_id,g.goods_id\n" +
                "     FROM commodity_goods g", dependencyNodes);

        assertTrue(err.isPresent());

        assertEquals("commodity_goods can not find tab in[]", err.get().summary());

        dependencyNodes.add(DependencyNode.create("123456", "commodity_goods", NodeType.DUMP));
        err = SqlTaskNodeMeta.validateSql("    SELECT g.id,g.entity_id,g.commodity_id,g.goods_id\n" +
                "     FROM commodity_goods g", dependencyNodes);
        assertFalse(err.isPresent());
    }

    public void testGetColMetaGetterSql() throws Exception {
        SqlTaskNodeMeta taskNodeMeta = new SqlTaskNodeMeta();
        taskNodeMeta.setSql(TestSqlRewriter.getScriptContent("supply_goods_rewrite_origin.sql"));
        TabPartitions dumpPartition = createTabPartition();
        ISqlTask.RewriteSql colMetaGetterSql = taskNodeMeta.getColMetaGetterSql(dumpPartition);

        System.out.println(colMetaGetterSql.rewriteSql);

        assertEquals(TestSqlRewriter.getScriptContent("supply_goods_rewrite_result_col_meta_get.sql"), colMetaGetterSql.rewriteSql);

    }


    public void testGetRewriteSqlUnusingJoin() throws Exception {
        SqlTaskNodeMeta taskNodeMeta = new SqlTaskNodeMeta();

        taskNodeMeta.setSql(TestSqlRewriter.getScriptContent("supply_goods_rewrite_unusing_join_origin.sql"));
        TabPartitions dumpPartition = createTabPartition();

        IJoinTaskContext joinTaskContext = EasyMock.createMock("joinTaskContext", IJoinTaskContext.class);
        EasyMock.expect(joinTaskContext.getExecutePhaseRange()).andReturn(ExecutePhaseRange.fullRange()).times(2);
        EasyMock.expect(joinTaskContext.getIndexShardCount()).andReturn(1).times(1);
        Optional<ERRules> erRule = ERRules.getErRule(TestSupplyGoodsParse.topologyName);
        assertTrue(erRule.isPresent());
        EasyMock.replay(joinTaskContext);

        ISqlTask.RewriteSql rewriteSql = taskNodeMeta.getRewriteSql(
                "supply_goods", dumpPartition, () -> erRule.get(), joinTaskContext, true);

        assertNotNull(rewriteSql);
        assertEquals(TestSqlRewriter.getScriptContent("supply_goods_rewrite_unusing_join_result.sql"), rewriteSql.rewriteSql);
        System.out.println(rewriteSql.rewriteSql);
        EasyMock.verify(joinTaskContext);
    }



    public void testGetRewriteSql() throws Exception {
        SqlTaskNodeMeta taskNodeMeta = new SqlTaskNodeMeta();

        taskNodeMeta.setSql(TestSqlRewriter.getScriptContent("supply_goods_rewrite_origin.sql"));
        TabPartitions dumpPartition = createTabPartition();

        IJoinTaskContext joinTaskContext = EasyMock.createMock("joinTaskContext", IJoinTaskContext.class);
        EasyMock.expect(joinTaskContext.getExecutePhaseRange()).andReturn(ExecutePhaseRange.fullRange()).times(2);
        EasyMock.expect(joinTaskContext.getIndexShardCount()).andReturn(1).times(1);
        Optional<ERRules> erRule = ERRules.getErRule(TestSupplyGoodsParse.topologyName);
        assertTrue(erRule.isPresent());
        EasyMock.replay(joinTaskContext);

        ISqlTask.RewriteSql rewriteSql = taskNodeMeta.getRewriteSql(
                "supply_goods", dumpPartition, () -> erRule.get(), joinTaskContext, true);

        assertNotNull(rewriteSql);
        assertEquals(TestSqlRewriter.getScriptContent("supply_goods_rewrite_result.txt"), rewriteSql.rewriteSql);
        System.out.println(rewriteSql.rewriteSql);
        EasyMock.verify(joinTaskContext);
    }

    private TabPartitions createTabPartition() {
        Map<IDumpTable, ITabPartition> dumpPartition = Maps.newHashMap();
        String pt = "20200703113848";
        dumpPartition.put(EntityName.parse("scmdb.warehouse_goods"), () -> pt);
        dumpPartition.put(EntityName.parse("tis.stock_info_collapse"), () -> pt);
        dumpPartition.put(EntityName.parse("scmdb.supplier_goods"), () -> pt);
        dumpPartition.put(EntityName.parse("tis.warehouse_collapse"), () -> pt);
        dumpPartition.put(EntityName.parse("tis.supplier_collapse"), () -> pt);
        dumpPartition.put(EntityName.parse("scmdb.goods"), () -> pt);
        dumpPartition.put(EntityName.parse("scmdb.stock_info"), () -> pt);
        dumpPartition.put(EntityName.parse("scmdb.category"), () -> pt);
        dumpPartition.put(EntityName.parse("scmdb.goods_sync_shop"), () -> pt);
        return new TabPartitions(dumpPartition); // dumpPartition;
    }

    public void testBigTextSerialize() {
        MySqlContent content = new MySqlContent();
        content.setContent(SqlTaskNodeMeta.processBigContent(TestSqlRewriter.getScriptContent("totalpay_summary_assert.txt")));
        StringWriter writer = new StringWriter();
        SqlTaskNodeMeta.yaml.get().addTypeDescription(new TypeDescription(MySqlContent.class, Tag.MAP));
        SqlTaskNodeMeta.yaml.get().dump(content, writer);
        System.out.println();
        System.out.println(writer);
        content = SqlTaskNodeMeta.yaml.get().loadAs(writer.toString(), MySqlContent.class);
        Assert.assertNotNull(content);
        Assert.assertNotNull(content.getContent());
        System.out.println(content.getContent());
    }

    public static class MySqlContent {

        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

//    public void testSerializeAndDeserialize() throws Exception {
//        File testDir = new File("./src/test/resources/test");
//        String topologyName = "totalpay";
//        final File dataflowDir = new File(testDir, "dataflow/" + topologyName);
//        FileUtils.forceMkdir(dataflowDir);
//
//        Config.setDataDir(testDir.getAbsolutePath());
//        SqlTaskNode.parent = new File(Config.getMetaCfgDir(), SqlTaskNode.NAME_DATAFLOW_DIR);
//
//        SqlDataFlowTopology topology = new SqlDataFlowTopology();
//        SqlTaskNodeMeta.TopologyProfile tprofile = new SqlTaskNodeMeta.TopologyProfile();
//        tprofile.setTimestamp(20210820112059l);
//        tprofile.setDataflowId(1);
//        tprofile.setName(topologyName);
//        topology.setProfile(tprofile);
//
//        List<DependencyNode> ns = Lists.newArrayList();
//        DependencyNode dep1 = new DependencyNode();
//        dep1.setDbid("123");
//        dep1.setTabid("765");
//        dep1.setDbName("order");
//        dep1.setExtraSql("select * from USER u  \ninner join Profile p on (u.userid = p.userid)");
//        dep1.setId("43");
//        dep1.setName("orderinfo");
//        dep1.setType(NodeType.DUMP.getType());
//        ns.add(dep1);
//        // topology.addDumpTab(ns);
//        DependencyNode dep2 = new DependencyNode();
//        dep2.setDbid("124");
//        dep2.setTabid("883");
//        dep2.setDbName("order");
//        dep2.setExtraSql("SELECT * FROM UUUSER u  \nINNER JOIN Profile p ON (u.userid = p.userid)");
//        dep2.setId("433");
//        dep2.setName("totalpay");
//        dep2.setType(NodeType.DUMP.getType());
//        ns.add(dep2);
//        topology.addDumpTab(ns);
//        SqlTaskNodeMeta processMeta = new SqlTaskNodeMeta();
//        processMeta.setExportName("test_baisui");
//        processMeta.setId("12312hgj1h1232134j");
//        Position pos = new Position();
//        pos.setX(123);
//        pos.setY(321);
//        processMeta.setPosition(pos);
//        processMeta.setSql("select a,b,c from baisui_table where 1=1");
//        processMeta.setType(NodeType.JOINER_SQL.name());
//        DependencyNode dependency = new DependencyNode();
//        dependency.setId("22334467");
//        dependency.setName("baisui_xx");
//        processMeta.setDependencies(Collections.singletonList(dependency));
//        topology.addNodeMeta(processMeta);
//        SqlTaskNodeMeta.persistence(topology, dataflowDir);
//        // 反序列化
//        SqlDataFlowTopology restore = SqlTaskNodeMeta.getSqlDataFlowTopology(topologyName);
//        Assert.assertNotNull(restore);
//        List<DependencyNode> dumpNodes = restore.getDumpNodes();
//        Assert.assertEquals(2, dumpNodes.size());
//        for (DependencyNode dump : dumpNodes) {
//            if (dump.getId().equals(dep1.getId())) {
//                assertDependencyNodeEqual(dep1, dump);
//            } else if (dump.getId().equals(dep2.getId())) {
//                assertDependencyNodeEqual(dep2, dump);
//            } else {
//                throw new IllegalStateException("node:" + dump.getId() + " is illegal");
//            }
//        }
//        List<SqlTaskNodeMeta> metas = restore.getNodeMetas();
//        Assert.assertEquals(1, metas.size());
//        SqlTaskNodeMeta m = metas.get(0);
//        List<DependencyNode> single = m.getDependencies();
//        Assert.assertEquals(1, single.size());
//        DependencyNode s = single.get(0);
//        Assert.assertEquals(dependency.getId(), s.getId());
//        Assert.assertEquals(dependency.getName(), s.getName());
//        // FileUtils.forceDelete(dataflowDir);
//    }

    private void assertDependencyNodeEqual(DependencyNode expect, DependencyNode actual) {
        assertEquals(expect.getDbid(), actual.getDbid());
        assertEquals(expect.getName(), actual.getName());
        assertEquals(expect.getDbName(), actual.getDbName());
        assertEquals(expect.getExtraSql(), actual.getExtraSql());
        assertEquals(expect.getId(), actual.getId());
        assertEquals(expect.getType(), actual.getType());
        assertEquals(expect.parseNodeType(), actual.parseNodeType());
    }

    public void testDescrialize() throws Exception {
        File f = new File(parent, "card_expense_relative.yaml");
        SqlTaskNodeMeta sqlNodeMeta = SqlTaskNodeMeta.deserializeTaskNode(f);
        Assert.assertNotNull(sqlNodeMeta);
        Position pos = sqlNodeMeta.getPosition();
        Assert.assertNotNull(pos);
        Assert.assertTrue(pos.getX() > 0);
        Assert.assertTrue(pos.getY() > 0);
        Assert.assertEquals("card_expense_relative", sqlNodeMeta.getExportName());
        String sqlContent = sqlNodeMeta.getSql();
        System.out.println(sqlContent);
        Assert.assertNotNull(sqlContent);
        List<DependencyNode> required = sqlNodeMeta.getDependencies();
        Assert.assertEquals(2, required.size());
        Assert.assertEquals(1059, sqlNodeMeta.getPosition().getX());
        Assert.assertEquals(264, sqlNodeMeta.getPosition().getY());
        Assert.assertEquals(NodeType.JOINER_SQL, sqlNodeMeta.getNodeType());
        Assert.assertEquals("6e7b9c50-0fba-8a19-f029-d973e5a833c7", sqlNodeMeta.getId());

    }

    File tmp_group_specialfee = new File(parent, "tmp_group_specialfee.yaml");

    public void testTmpGroupSpecialfee() throws Exception {
        SqlTaskNodeMeta sqlNodeMeta = SqlTaskNodeMeta.deserializeTaskNode(tmp_group_specialfee);
        Assert.assertNotNull(sqlNodeMeta);
        Assert.assertEquals("tmp_group_specialfee", sqlNodeMeta.getExportName());
        String sqlContent = sqlNodeMeta.getSql();
        System.out.println(sqlContent);
    }
    // public void testJsonSerialize() throws Exception {
    // SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology(tmp_group_specialfee.getParentFile());
    //
    // String jsonContent = com.alibaba.fastjson.JSON.toJSONString(topology,
    // SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat);
    //
    // System.out.println(jsonContent);
    //
    // Assert.assertEquals(11, topology.getDumpNodes().size());
    //
    // }
}
