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
package com.qlangtech.tis.sql.parser.er;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Lists;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.DBIdentity;
import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import com.qlangtech.tis.plugin.ds.PostedDSProp;
import com.qlangtech.tis.sql.parser.SqlTaskNode;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.sql.parser.meta.PrimaryLinkKey;
import com.qlangtech.tis.sql.parser.meta.TabExtraMeta;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ERRules implements IPrimaryTabFinder, IERRules {

    public static final String ER_RULES_FILE_NAME = "er_rules.yaml";

    private static final Yaml yaml;
    // 类似flink的时间处理
    private TimeCharacteristic timeCharacteristic = TimeCharacteristic.EventTime;

    private List<TableRelation> relationList = Lists.newArrayList();

    private List<DependencyNode> dumpNodes = Lists.newArrayList();

    private Map<String, DependencyNode> /*** TODO 先用String，将来再改成EntityName */
            dumpNodesMap;

    private List<String> ignoreIncrTriggerEntities;

    private List<PrimaryTableMeta> primaryTabs;

    private List<TabFieldProcessor> processors = null;

    private Map<EntityName, TabFieldProcessor> processorMap;

    static {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(4);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        // dumperOptions.setAnchorGenerator((n) -> "b");
        // dumperOptions.setAnchorGenerator();
        // dumperOptions.setAnchorGenerator(null);
        dumperOptions.setPrettyFlow(false);
        dumperOptions.setSplitLines(true);
        dumperOptions.setLineBreak(DumperOptions.LineBreak.UNIX);
        dumperOptions.setWidth(1000000);
        yaml = new Yaml(new Constructor(), new Representer() {

            @Override
            protected Node representScalar(Tag tag, String value, DumperOptions.ScalarStyle style) {
                // 大文本实用block
                if (Tag.STR == tag && value.length() > 100) {
                    style = DumperOptions.ScalarStyle.FOLDED;
                }
                return super.representScalar(tag, value, style);
            }

            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                if (propertyValue == null) {
                    return null;
                }
                return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
            }
        }, dumperOptions);
        yaml.addTypeDescription(new TypeDescription(TableRelation.class, Tag.MAP, TableRelation.class));
        yaml.addTypeDescription(new TypeDescription(ERRules.class, Tag.MAP, ERRules.class));
        yaml.addTypeDescription(new TypeDescription(DependencyNode.class, Tag.MAP, DependencyNode.class));
    }

    public static void createErRule(String topologyName, DependencyNode node, ColumnMetaData pkMeta) throws Exception {
        /***********************************************************
         * 设置TabExtraMeta
         **********************************************************/
        Objects.requireNonNull(pkMeta, "param pkMeta can not be null");
        node.setExtraSql(null);
        TabExtraMeta extraMeta = new TabExtraMeta();
        extraMeta.setSharedKey(pkMeta.getKey());
        extraMeta.setMonitorTrigger(true);
        List<PrimaryLinkKey> primaryIndexColumnName = Lists.newArrayList();
        PrimaryLinkKey pk = new PrimaryLinkKey();
        pk.setName(pkMeta.getKey());
        pk.setPk(true);
        primaryIndexColumnName.add(pk);
        extraMeta.setPrimaryIndexColumnNames(primaryIndexColumnName);
        extraMeta.setPrimaryIndexTab(true);
        node.setExtraMeta(extraMeta);
        ERRules erRules = new ERRules();
        erRules.addDumpNode(node);
        erRules.setTimeCharacteristic(TimeCharacteristic.ProcessTime);
        write(topologyName, erRules);
    }

    /**
     * 使用默认DumpNode创建ERRule并且持久化
     *
     * @param topology
     * @throws Exception
     */
    public static void createDefaultErRule(SqlTaskNodeMeta.SqlDataFlowTopology topology) throws Exception {
        // 还没有定义erRule
        DependencyNode dumpNode = topology.getFirstDumpNode();
        DataSourceFactory dsStore = TIS.getDataBasePlugin(new PostedDSProp(DBIdentity.parseId(dumpNode.getDbName())));
        List<ColumnMetaData> cols = dsStore.getTableMetadata(false, dumpNode.parseEntityName());
        //String topologyName, DependencyNode node, TargetColumnMeta targetColMetas
        Optional<ColumnMetaData> firstPK = cols.stream().filter((col) -> col.isPk()).findFirst();
        if (!firstPK.isPresent()) {
            throw new IllegalStateException("table:" + dumpNode.parseEntityName() + " can not find relevant PK cols");
        }
        createErRule(topology.getName(), dumpNode, firstPK.get());
    }

//    @Override
//    public Optional<TableMeta> getPrimaryTab(EntityName entityName) {
//        return Optional.empty();
//    }

    private DependencyNode getDumpNode(EntityName tabName) {
        if (this.dumpNodesMap == null) {
            this.dumpNodesMap = this.dumpNodes.stream().collect(Collectors.toMap((r) -> r.getName(), (r) -> r));
        }
        DependencyNode result = this.dumpNodesMap.get(tabName.getTabName());
        if (result == null) {
            throw new IllegalStateException("can not find table:" + tabName + " in " + this.dumpNodes.stream().map((r) -> String.valueOf(r.parseEntityName())).collect(Collectors.joining(",")));
        }
        return result;
    }

    public static void write(String topology, ERRules erRules) throws Exception {
        File parent = new File(SqlTaskNode.parent, topology);
        FileUtils.forceMkdir(parent);
        FileUtils.write(new File(parent, ER_RULES_FILE_NAME), ERRules.serialize(erRules), TisUTF8.getName(), false);
    }

    /**
     * 规则已经存在
     *
     * @param topology
     * @return
     */
    public static boolean ruleExist(String topology) {
        return getErRuleFile(topology).exists();
    }

    private static File getErRuleFile(String topology) {
        final String topologyPath = topology + File.separator + ER_RULES_FILE_NAME;
        CenterResource.copyFromRemote2Local(IFullBuildContext.NAME_DATAFLOW_DIR + File.separator + topologyPath, true);
        return new File(SqlTaskNode.parent, topologyPath);
    }

    public static String serialize(ERRules rules) {
        synchronized (yaml) {
            return yaml.dump(rules);
        }

    }

    public void addDumpNode(DependencyNode node) {
        this.dumpNodes.add(node);
    }

    public List<DependencyNode> getDumpNodes() {
        return dumpNodes;
    }

    public void setDumpNodes(List<DependencyNode> dumpNodes) {
        this.dumpNodes = dumpNodes;
    }

    public static ERRules deserialize(String rulesStr) {
        synchronized (yaml) {
            return yaml.loadAs(rulesStr, ERRules.class);
        }
    }

    public static Optional<ERRules> getErRule(String topology) {
        if (!ruleExist(topology)) {
            return Optional.empty();
        }
        File erRuleFile = getErRuleFile(topology);
        try {
            return Optional.of(deserialize(FileUtils.readFileToString(erRuleFile, TisUTF8.get())));
        } catch (Exception e) {
            throw new RuntimeException("topology:" + topology + ",file path:" + erRuleFile.getAbsolutePath(), e);
        }
    }

    // 配置需要忽略执行增量的表的集合
    // private List<String> ignoreIncrTriggerEntities = Lists.newArrayList();
    public void setRelationList(List<TableRelation> relationList) {
        this.relationList = relationList;
    }

    public void addRelation(TableRelation relation) {
        this.relationList.add(relation);
    }

    public List<TableRelation> getRelationList() {
        return this.relationList;
    }


    /**
     * 取得表的所有主表信息
     *
     * @param
     * @return
     */
    public List<TableRelation> getAllParent(EntityName entityName) {
        List<TableRelation> parentRefs = Lists.newArrayList();
        DependencyNode child;
        TableRelation resultRel = null;
        for (TableRelation relation : this.getRelationList()) {
            child = relation.getChild();
            if (StringUtils.equals(child.getName(), entityName.getTabName())) {
                resultRel = relation;
                parentRefs.add(resultRel);
            }
        }
        return parentRefs;
    }

    public TimeCharacteristic getTimeCharacteristic() {
        return timeCharacteristic;
    }

    public void setTimeCharacteristic(TimeCharacteristic timeCharacteristic) {
        this.timeCharacteristic = timeCharacteristic;
    }

    /**
     * 是否是主表
     *
     * @param tabName
     * @return
     */
    public Optional<PrimaryTableMeta> isPrimaryTable(String tabName) {
        List<PrimaryTableMeta> primaryTableNames = this.getPrimaryTabs();
        Optional<PrimaryTableMeta> hasPrimaryParent = primaryTableNames.stream().filter((r) -> StringUtils.equals(tabName, r.getTabName())).findFirst();
        return hasPrimaryParent;
    }

    /**
     * 取得第一个父表关系, 一个表有多个父表，优先取为主索引表的父表
     *
     * @param tabName
     * @return
     */
    public Optional<TableRelation> getFirstParent(String tabName) {
        // List<PrimaryTableMeta> primaryTableNames = this.getPrimaryTabs();

        DependencyNode child;
        TableRelation resultRel = null;
        Optional<PrimaryTableMeta> hasPrimaryParent = null;
        for (TableRelation relation : this.getRelationList()) {
            child = relation.getChild();
            if (StringUtils.equals(child.getName(), tabName)) {
                resultRel = relation;
                // 判断parent表是否是主表？
                // 优先选取主索引表
                if (isPrimaryTable(relation.getParent().getName()).isPresent()) {
                    return Optional.of(relation);
                }
            }
        }
        if (resultRel != null) {
            return Optional.of(resultRel);
        }
        return Optional.empty();
    }

    /**
     * 取得子表引用集合
     *
     * @param tabName
     * @return
     */
    public List<TableRelation> getChildTabReference(EntityName tabName) {
        List<TableRelation> childRefs = Lists.newArrayList();
        for (TableRelation relation : this.getRelationList()) {
            if (StringUtils.equals(relation.getParent().getName(), tabName.getTabName())) {
                childRefs.add(relation);
            }
        }
        // 排序：要将子表为主表的实体排序在前面
        childRefs.sort((r1, r2) -> {
            Optional<TableMeta> r1p = getPrimaryTab(r1.getChild().parseEntityName());
            Optional<TableMeta> r2p = getPrimaryTab(r2.getChild().parseEntityName());
            return (r2p.isPresent() ? 1 : 0) - (r1p.isPresent() ? 1 : 0);
        });
        return childRefs;
    }

    // 添加增量忽略处理的表
    // public void addIgnoreIncrTriggerEntity(String tabName) {
    // this.ignoreIncrTriggerEntities.add(tabName);
    // }
    // 增量执行过程中某些维表不需要监听变更时间
    @JSONField(serialize = false)
    public List<String> getIgnoreIncrTriggerEntities() {
        if (ignoreIncrTriggerEntities == null) {
            ignoreIncrTriggerEntities = this.getDumpNodes().stream()
                    .filter((d) -> d.getExtraMeta() != null && !d.getExtraMeta().isMonitorTrigger())
                    .map((d) -> d.getName()).collect(Collectors.toList());
        }
        return this.ignoreIncrTriggerEntities;
    }

    @Override
    public boolean isTimestampVerColumn(EntityName tableName, String colName) {
        return StringUtils.equals(getTimestampVerColumn(tableName), colName);
    }

    public boolean hasSetTimestampVerColumn(EntityName tableName) {
        DependencyNode dumpNode = getDumpNode(tableName);
        TabExtraMeta extraMeta = dumpNode.getExtraMeta();
        return extraMeta == null && StringUtils.isNotEmpty(extraMeta.getTimeVerColName());
    }

    /**
     * 取得表的时间戳生成列
     *
     * @param tableName
     * @return
     */
    @Override
    public String getTimestampVerColumn(EntityName tableName) {
        if (this.isTriggerIgnore(tableName)) {
            throw new IllegalStateException("tab:" + tableName + " is not monitor in incr process");
        }
        DependencyNode dumpNode = getDumpNode(tableName);
        TabExtraMeta extraMeta = dumpNode.getExtraMeta();
        if (extraMeta == null || StringUtils.isEmpty(extraMeta.getTimeVerColName())) {
            throw new IllegalStateException("table:" + tableName + " can not find 'timeVerColName' prop");
        }
        return extraMeta.getTimeVerColName();
    }

    @JSONField(serialize = false)
    public List<TabFieldProcessor> getTabFieldProcessors() {
        if (processors == null) {
            this.processors = this.getDumpNodes().stream()
                    .filter((d) -> d.getExtraMeta() != null && d.getExtraMeta().getColTransfers().size() > 0)
                    .map((d) -> new TabFieldProcessor(d.parseEntityName(), d.getExtraMeta().getColTransfers()))
                    .collect(Collectors.toList());
        }
        return processors;
    }

    @JSONField(serialize = false)
    public Map<EntityName, TabFieldProcessor> getTabFieldProcessorMap() {
        if (processorMap == null) {
            processorMap = getTabFieldProcessors().stream().collect(Collectors.toMap((r) -> r.tabName, (r) -> r));
        }
        return processorMap;
    }

    /**
     * 索引主表，如果索引是UNION结构，则返回的集合的size则大于1
     *
     * @return
     */
    @JSONField(serialize = false)
    public List<PrimaryTableMeta> getPrimaryTabs() {
        if (primaryTabs == null) {
            primaryTabs = this.getDumpNodes().stream().filter(
                    (d) -> d.getExtraMeta() != null && d.getExtraMeta().isPrimaryIndexTab())
                    .map((d) -> new PrimaryTableMeta(d.getName(), d.getExtraMeta())).collect(Collectors.toList());
        }
        return primaryTabs;
    }

    @Override
    public Optional<TableMeta> getPrimaryTab(final IDumpTable entityName) {
        Optional<TableMeta> first = this.getPrimaryTabs().stream().filter(
                (p) -> StringUtils.equals(p.getTabName(), entityName.getTableName())).map((r) -> (TableMeta) r).findFirst();
        return first;
    }


    /**
     * 表实体在增量处理时候是否需要忽略
     *
     * @param entityName
     * @return
     */
    public boolean isTriggerIgnore(EntityName entityName) {
        return this.getIgnoreIncrTriggerEntities().contains(entityName.getTabName());
    }

    public static TableRelation $(String id, SqlTaskNodeMeta.SqlDataFlowTopology topology, String parent, String child, TabCardinality c) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("param id can not be null");
        }
        Map<String, DependencyNode> /**
         * table name
         */
                dumpNodesMap = topology.getDumpNodesMap();
        return new TableRelation(id, dumpNodesMap.get(parent), dumpNodesMap.get(child), c);
    }
}
