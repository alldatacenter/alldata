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
package com.qlangtech.tis.sql.parser.stream.generate;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.sql.parser.er.IERRules;
import com.qlangtech.tis.sql.parser.er.PrimaryTableMeta;
import com.qlangtech.tis.sql.parser.er.TabFieldProcessor;
import com.qlangtech.tis.sql.parser.er.TableRelation;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import com.qlangtech.tis.sql.parser.visitor.IBlockToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class MergeData implements IStreamIncrGenerateStrategy.IStreamTemplateData {

    private final String collection;
    private final IStreamIncrGenerateStrategy streamIncrGenerateStrategy;

    private final Map<EntityName, MapDataMethodCreator> mapDataMethodCreatorMap;

    //  private final FuncFormat aliasListBuilder;

    private final List<TableAlias> tabTriggers;

    private final List<FacadeContext> facadeContextList;

    private final Set<PrimaryTableMeta> primaryTableNames;
    private final IERRules erRules;

    private final Map<String, IBlockToString> /**
     * method token
     */
            globalScripts = Maps.newHashMap();

    // private static final Pattern PATTERN_COLLECTION_NAME = Pattern.compile("(search4)([^\\s]+)");


    /**
     * @param collection
     * @param mapDataMethodCreatorMap
     * @param tabTriggers
     * @param facadeContextList       索引主表集合，当索引为两个表union起来的时候Set中就存在多个实体
     */
    public MergeData(
            String collection, Map<EntityName, MapDataMethodCreator> mapDataMethodCreatorMap
            //, FuncFormat aliasListBuilder
            , List<TableAlias> tabTriggers
            , IERRules erRules
            , List<FacadeContext> facadeContextList
            , IStreamIncrGenerateStrategy streamIncrGenerateStrategy) {
        super();
        this.streamIncrGenerateStrategy = streamIncrGenerateStrategy;
        this.collection = collection;
        this.mapDataMethodCreatorMap = mapDataMethodCreatorMap;
        //  this.aliasListBuilder = aliasListBuilder;
        this.tabTriggers = tabTriggers;
        if (facadeContextList == null) {
            throw new IllegalArgumentException("param facadeContextList can not be null");
        }
        this.facadeContextList = facadeContextList;
        this.erRules = erRules;
        Objects.requireNonNull(erRules, "erRules can not be null");
        List<PrimaryTableMeta> primaryTabs = this.erRules.getPrimaryTabs();// erRules.getPrimaryTabs();
        // 索引的主索引表
        // Sets.newHashSet(new PrimaryTableMeta("totalpayinfo", "totalpay_id"));
        Set<PrimaryTableMeta> primaryTablesName = Sets.newHashSet(primaryTabs);
        // FIXME 暂时先用一下
        if (primaryTablesName.size() < 1) {
            throw new IllegalStateException("primaryTableName is not illegal");
        }
        this.primaryTableNames = primaryTablesName;
    }

    private final Stack<FlatTableRelation> unprocessedTableRelations = new Stack<>();

    public boolean isFacadeDAOSupport() {
        return !this.streamIncrGenerateStrategy.isExcludeFacadeDAOSupport();
    }

    /**
     * 将子表主表关系压入关系栈,通过velocit模版（parsePKGetter.vm）中调用
     *
     * @param rel
     * @param child2Parent 标示方向，由子表连接到主表
     */
    public void pushRel(TableRelation rel, boolean child2Parent) {
        System.out.println(rel.toString() + ",child2Parent" + child2Parent + "=====================");
        this.unprocessedTableRelations.push(new FlatTableRelation(rel, child2Parent));
    }

    public List<TabFieldProcessor> getTabFieldProcessors() {
        return this.erRules.getTabFieldProcessors();
    }

    public Stack<FlatTableRelation> getUnprocessedTableRelations() {
        return this.unprocessedTableRelations;
    }

    public void addGlobalScript(String methodToken, IBlockToString script) {
        this.globalScripts.put(methodToken, script);
    }

    public Collection<IBlockToString> getGlobalScripts() {
        return this.globalScripts.values();
    }

    public Optional<TableRelation> getFirstParent(String tabName) {
        return erRules.getFirstParent(tabName);
    }

    public List<TableRelation> getChildTabReference(String tabName) {
        return erRules.getChildTabReference(EntityName.parse(tabName));
    }

    public Set<PrimaryTableMeta> getPrimaryTableNames() {
        return this.primaryTableNames;
    }

    /**
     * 取得主索引表的数据库主键索引字段名称
     *
     * @param tabName 必须为 primary table
     * @return
     */
    public String getPTableDBPKName(String tabName) {
        Optional<PrimaryTableMeta> p = getPrimaryTableMetaOption(tabName);
        if (!p.isPresent()) {
            throw new IllegalStateException("tabName:" + tabName + " is not one of the primayTab");
        }
        PrimaryTableMeta ptabMeta = p.get();
        return ptabMeta.getDBPrimayKeyName().getName();
    }

    public String getPTableRouterKeysName(String tabName) {
        Optional<PrimaryTableMeta> p = getPrimaryTableMetaOption(tabName);
        if (!p.isPresent()) {
            throw new IllegalStateException("tabName:" + tabName + " is not one of the primayTab");
        }
        return p.get().createPKPlayloadParams().toString();
        // PrimaryTableMeta ptabMeta = p.get();
        // List<PrimaryTableMeta.PrimaryLinkKey> payloadRouterKeys = ptabMeta.getPayloadRouterKeys();
        // StringBuffer buffer = new StringBuffer();
        // for (PrimaryTableMeta.PrimaryLinkKey routerKey : payloadRouterKeys) {
        // buffer.append(",\"").append(routerKey.getName()).append("\",row.getColumn(\"").append(routerKey.getName()).append("\")");
        // }
        // return buffer.toString();
    }

    /**
     * 是否是索引宽表的主表
     *
     * @param tabName
     * @return
     */
    public boolean isPrimaryTable(String tabName) {
        Optional<PrimaryTableMeta> p = getPrimaryTableMetaOption(tabName);
        return p.isPresent();
    }

    private Optional<PrimaryTableMeta> getPrimaryTableMetaOption(String tabName) {
        return primaryTableNames.stream().filter((r) -> StringUtils.equals(r.getTabName(), tabName)).findFirst();
    }

    public List<FacadeContext> getFacadeContextList() {
        return this.facadeContextList;
    }

    @Override
    public List<TableAlias> getDumpTables() {
        if (CollectionUtils.isEmpty(this.tabTriggers)) {
            throw new IllegalStateException("tabTriggers can not be empty");
        }
        return this.tabTriggers;//.keySet().stream().map((r) -> r.getEntityName()).collect(Collectors.toList());
    }

    public String getSharedId(EntityName e) {
        Optional<PrimaryTableMeta> ptmeta = getPrimaryTableMetaOption(e.getTabName());
        if (ptmeta.isPresent()) {
            return ptmeta.get().getSharedKey();
        } else {
//        #set($parentTabRef=$config.getFirstParent($i.tabName))
//        #set($tabName=$i.tabName)
//        #* List<TableRelation> *#
//        #set($childTabRef=$config.getChildTabReference($i.tabName))
            Optional<TableRelation> firstParent = this.getFirstParent(e.getTabName());
            if (firstParent.isPresent()) {
                ptmeta = getPrimaryTableMetaOption(firstParent.get().getParent().getName());
                if (ptmeta.isPresent()) {
                    return ptmeta.get().getSharedKey();
                }
            }
            List<TableRelation> childTabRefs = this.getChildTabReference(e.getTabName());
            for (TableRelation childRef : childTabRefs) {
                ptmeta = getPrimaryTableMetaOption(childRef.getChild().getName());
                if (ptmeta.isPresent()) {
                    return ptmeta.get().getSharedKey();
                }
            }
        }

        throw new IllegalStateException("can not find shareId with table:" + e.getTabName());
    }


    public boolean isTriggerIgnore(EntityName entityName) {
        return this.erRules.isTriggerIgnore(entityName);
    }

//    public String getTableFocuseJoinerLiteria() {
//        return getDumpTables().stream().filter((e) -> !isTriggerIgnore(e)).map((e) -> "\"" + e.getTabName() + "\"").collect(Collectors.joining(","));
//    }

//    public String getColsMetaBuilderList() {
//
//        return this.aliasListBuilder.toString();
//    }

    public Collection<MapDataMethodCreator> getMapDataMethodCreatorList() {
        return this.mapDataMethodCreatorMap.values();
    }

    public Collection<EntityName> getEntitiesList() {
        return this.mapDataMethodCreatorMap.keySet();
    }

    public String getCollection() {
        return this.collection;
    }
}
