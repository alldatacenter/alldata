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

package com.qlangtech.tis.sql.parser.stream.generate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.tis.sql.parser.TisGroupBy;
import com.qlangtech.tis.sql.parser.er.*;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import com.qlangtech.tis.sql.parser.visitor.FuncFormat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-07 17:36
 **/
public class MapDataMethodCreator {
    // 需要聚合的表
    private final EntityName entityName;
    private final TisGroupBy groups;
    private final Set<String> relefantCols;
    private final IStreamIncrGenerateStrategy streamIncrGenerateStrategy;

    public MapDataMethodCreator(EntityName entityName, TisGroupBy groups, IStreamIncrGenerateStrategy streamIncrGenerateStrategy, Set<String> relefantCols) {
        super();
        this.entityName = entityName;
        this.groups = groups;
//            this.erRules = erRules;
        this.relefantCols = relefantCols;
        this.streamIncrGenerateStrategy = streamIncrGenerateStrategy;
    }

//        public final String capitalizeEntityName() {
//            return StringUtils.capitalize(entityName.getTabName());
//        }

    // 实体复数
//        public String entities() {
//            return this.entityName.getTabName() + "s";
//        }

    public String getMapDataMethodName() {
        return "map" + this.entityName.capitalizeEntityName() + "Data";
    }


    /**
     * @return
     */
    public String getGenerateMapDataMethodBody() {
        IERRules erRule = null; //streamIncrGenerateStrategy.getERRule();
        FuncFormat funcFormat = new FuncFormat();

        funcFormat.appendLine("val " + this.entityName.entities()
                + "ThreadLocal : ThreadLocal[Map[GroupKey, GroupValues]]  = addThreadLocalVal()");

        funcFormat.returnLine();

        // funcFormat.methodBody( //
        // "private Map<GroupKey, GroupValues> " + getMapDataMethodName() +
        // "(IRowValueGetter "
        // + this.entityName.getTabName() + ")",
        funcFormat.methodBody( //
                "private def " + getMapDataMethodName() + "( " + this.entityName.getJavaEntityName()
                        + " : IRowValueGetter) : scala.collection.mutable.Map[GroupKey, GroupValues] =",
                (r) -> {
                    r.startLine("var result :scala.collection.mutable.Map[GroupKey, GroupValues] = " + this.entityName.entities()
                            + "ThreadLocal.get()");

                    //r.startLine("var " + this.entityName.entities() + ": List[" + ROW_MAP_CLASS_NAME + "]  = null");

                    r.startLine(this.entityName.buildDefineRowMapListLiteria());

                    r.methodBody("if (result != null)", (m) -> {
                        m.appendLine(" return result");
                    });
                    if (groups.getGroups().size() < 1) {
                        throw new IllegalStateException("groups.getGroups().size() can not small than 1");
                    }
                    TableRelation parentRel = null;

                    Optional<PrimaryTableMeta> ptab = erRule.isPrimaryTable(this.entityName.getTabName());
                    if (ptab.isPresent()) {
                        // 如果聚合表本身就是主表的话，那它只需要查询自己就行了
                        PrimaryTableMeta p = ptab.get();
                        parentRel = new TableRelation();
//                            parentRel.setParent(null);
//                            parentRel.setChild(null);
                        parentRel.setCardinality(TabCardinality.ONE_N.getToken());
                        // List<JoinerKey> joinerKeys = Lists.newArrayList();
                        parentRel.setJoinerKeys(p.getPrimaryKeyNames().stream().map((rr) -> new JoinerKey(rr.getName(), rr.getName())).collect(Collectors.toList()));
                    } else {
                        Optional<TableRelation> firstParentRel = erRule.getFirstParent(this.entityName.getTabName());
                        if (!firstParentRel.isPresent()) {
                            throw new IllegalStateException("first parent table can not be null ,child table:" + this.entityName);
                        }
                        parentRel = firstParentRel.get();
                    }

                    if (!parentRel.isCardinalityEqual(TabCardinality.ONE_N)) {
                        throw new IllegalStateException("rel" + parentRel + " execute aggreate mush be an rel cardinality:" + TabCardinality.ONE_N);
                    }

                    List<TisGroupBy.TisColumn> linkKeys = Lists.newArrayList();

                    try {
                        TisGroupBy.TisColumn col = null;
                        for (LinkKeys linkKey : parentRel.getCurrentTableRelation(true).getTailerKeys()) {
                            col = new TisGroupBy.TisColumn(linkKey.getHeadLinkKey());
                            linkKeys.add(col);
                            r.appendLine("val " + col.getJavaVarName() + ":String = " + entityName.getJavaEntityName()
                                    + ".getColumn(\"" + col.getColname() + "\")");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(parentRel.toString(), e);
                    }


                    r.appendLine(this.entityName.buildDefineCriteriaEqualLiteria());

                    r.startLine(this.entityName.buildCreateCriteriaLiteria());


                    for (TisGroupBy.TisColumn g : linkKeys) {
                        r.append(g.buildPropCriteriaEqualLiteria());
                    }


                    // 外键查询键也要出现在select列中
                    final Set<String> selCols = Sets.newHashSet();
                    selCols.addAll(this.relefantCols);
                    this.groups.getGroups().stream().forEach((e) -> {
                        selCols.add(e.getColname());
                    });

                    r.startLine(this.entityName.buildAddSelectorColsLiteria(selCols));

                    r.startLine(this.entityName.buildExecuteQueryDAOLiteria());

                    r.startLine("result = scala.collection.mutable.Map[GroupKey, GroupValues]()");

                    r.startLine("var vals : Option[GroupValues] = null");
                    r.startLine("var groupKey: GroupKey = null");

                    r.buildRowMapTraverseLiteria(this.entityName, (m) -> {

                        m.startLine("groupKey = GroupKey.createCacheKey(") //
                                .append(groups.getGroups().stream()
                                        .map((rr) -> "\"" + rr.getColname() + "\",r.getColumn(\"" + (rr.getColname()) + "\")").collect(Collectors.joining(",")));
                        m.append(")").returnLine();

                        m.appendLine("vals = result.get(groupKey)");
                        m.appendLine("if (vals.isEmpty) {");

                        m.appendLine("  result +=(groupKey.clone() -> new GroupValues(r)) ");
                        m.appendLine("}else{");
                        m.appendLine(" vals.get.addVal(r)");
                        m.appendLine("}");

                    });


//                        r.methodBody("for ( ( r:" + ROW_MAP_CLASS_NAME + ") <- " + this.entityName.entities() + ".asScala)",
//                                (m) -> {
//
//                                    m.startLine("groupKey = GroupKey.createCacheKey(")
//                                            .append(Joiner.on(",")
//                                                    .join(groups.getGroups().stream()
//                                                            .map((rr) -> "r.getColumn(\"" + (rr.getColname()) + "\")")
//                                                            .iterator()));
//                                    m.append(")").returnLine();
//
//                                    m.appendLine("vals = result.get(groupKey)");
//                                    m.appendLine("if (vals.isEmpty) {");
//                                    // m.appendLine(" vals = new
//                                    // GroupValues();");
//                                    // m.appendLine("
//                                    // result.put(groupKey.clone(),
//                                    // vals);");
//                                    m.appendLine("  result +=(groupKey.clone() -> new GroupValues(r)) ");
//                                    m.appendLine("}else{");
//                                    m.appendLine(" vals.get.addVal(r)");
//                                    m.appendLine("}");
//
//                                });

                    r.appendLine(this.entityName.entities() + "ThreadLocal.set(result);");
                    r.appendLine("return result;");

                });

        return funcFormat.toString();
    }

}
