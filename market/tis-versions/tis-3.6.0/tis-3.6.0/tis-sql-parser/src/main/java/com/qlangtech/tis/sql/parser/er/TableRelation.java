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
import com.qlangtech.tis.sql.parser.TisGroupBy;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.sql.parser.meta.PrimaryLinkKey;
import com.qlangtech.tis.sql.parser.stream.generate.FlatTableRelation;
import com.qlangtech.tis.sql.parser.stream.generate.MergeData;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.visitor.FuncFormat;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 描述表之间的依赖关系
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TableRelation {

    private DependencyNode parent;

    private DependencyNode child;

    private List<JoinerKey> joinerKeys = Lists.newArrayList();

    private TabCardinality cardinality;

    private String id;

    public TableRelation(String id, DependencyNode parent, DependencyNode child, TabCardinality cardinality) {
        this.id = id;
        this.parent = parent;
        this.child = child;
        this.cardinality = cardinality;
        if (parent == null) {
            throw new IllegalArgumentException("param parent can not be null");
        }
        if (child == null) {
            throw new IllegalArgumentException("param child can not be null");
        }
        if (cardinality == null) {
            throw new IllegalArgumentException("param cardinality can not be null");
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TableRelation() {
    }

    public TableRelation addJoinerKey(JoinerKey joinerKey) {
        this.joinerKeys.add(joinerKey);
        return this;
    }

    public TableRelation addJoinerKey(String parentKey, String childKey) {
        return this.addJoinerKey(new JoinerKey(parentKey, childKey));
    }

    /**
     * 主外键相同的情况
     *
     * @param joinKey
     * @return
     */
    public TableRelation addJoinerKey(String joinKey) {
        return this.addJoinerKey(new JoinerKey(joinKey, joinKey));
    }

    public DependencyNode getParent() {
        return this.parent;
    }

    public DependencyNode getChild() {
        return this.child;
    }

    public String getCardinality() {
        return this.cardinality.getToken();
    }

    public void setCardinality(String val) {
        this.cardinality = TabCardinality.parse(val);
    }

    public boolean isCardinalityEqual(TabCardinality cardinality) {
        return cardinality == this.cardinality;
    }

    public void setParent(DependencyNode parent) {
        this.parent = parent;
    }

    public void setChild(DependencyNode child) {
        this.child = child;
    }

    public List<JoinerKey> getJoinerKeys() {
        return joinerKeys;
    }

    public void setJoinerKeys(List<JoinerKey> joinerKeys) {
        this.joinerKeys = joinerKeys;
    }

    public static class FinalLinkKey {

        final boolean success;

        // 两个rel之间是否是连通的
        // boolean breakthrough;
        public final String linkKeyName;
        //public final int tabRelsSize;
        public final FlatTableRelation interruptedTableRelation;

        public FinalLinkKey(String linkKeyName) {
            this(true, linkKeyName, null);
        }

        /**
         * @param success
         * @param linkKeyName
         * @param interruptedTableRelation 连接被中断的连接关系
         */
        public FinalLinkKey(boolean success, String linkKeyName, FlatTableRelation interruptedTableRelation) {
            this.success = success;
            this.linkKeyName = linkKeyName;
            this.interruptedTableRelation = interruptedTableRelation;
        }
    }

    /**
     * 生成主表的PK键 scala代码
     *
     * @param context
     * @return
     */
    public String createParentPKLiteria(MergeData context) {
        Stack<FlatTableRelation> unprocessed = context.getUnprocessedTableRelations();
        FlatTableRelation un = null;
        try {
            FlatTableRelation currentTableRelation = getCurrentTableRelation(true);
            PrimaryTableMeta primary = getPrimaryTableMeta(context, currentTableRelation.getHeaderEntity());
            LinkKeys pkJoinerKey = null;
            for (LinkKeys lk : currentTableRelation.getHeaderKeys()) {
                if (StringUtils.equals(lk.getHeadLinkKey(), primary.getDBPrimayKeyName().getName())) {
                    pkJoinerKey = lk;
                }
            }
            if (!unprocessed.empty()) {
                // 连接栈不为空
                un = unprocessed.pop();
                if (!unprocessed.empty()) {
                    // FIXME: 或许将来支持更加复杂的增量流式处理，可以支持外表连接
                    throw new IllegalStateException("un process rel can not exceed 2,but now size:"
                            + unprocessed.size() + "pre:" + un.toString() + ",others:"
                            + unprocessed.stream().map((r) -> r.toString()).collect(Collectors.joining(",")));
                }

                if (currentTableRelation.isLinkable(un)) {
                    return primary.createCompositePK(currentTableRelation, un) + "/*gencode1*/";
                } else {
                    // SqlTaskNodeMeta.DependencyNode headerNode = un.getHeader();
                    return createSelectParentByChild(context, currentTableRelation, un, primary);
                }
            } else {

                TableRelation.FinalLinkKey finalLinkKey = FlatTableRelation.getFinalLinkKey(primary.getDBPrimayKeyName().getName(), currentTableRelation);
                if (finalLinkKey.success) {
                    // 连接栈为空
                    return "return " + primary.createCompositePK(currentTableRelation) + "/*gencode2*/";
                } else {
                    // baisui 修改 2020/9/29
                    // 例如：orderDetail是主表，以orderid作为pk，外表totalpayinfo 为外表（连接键为: totalpay_id -> totalpay_id,所以连接过程会中断
                    return createSelectParentByChild(context, currentTableRelation, currentTableRelation, primary);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException("parent:" + this.getParent().parseEntityName() + ",child:" + this.getChild().parseEntityName(), e);
        } finally {
            context.getUnprocessedTableRelations().clear();
        }
    }

    // private static String createCompositePK(PrimaryTableMeta primary, LinkKeys pkJoinerKey, FlatTableRelation... tabRels) {
    // String pkGetterLiteria =
    // EntityName.createColValLiteria("columnMeta"
    // , FlatTableRelation.getFinalLinkKey(pkJoinerKey.getHeadLinkKey(), tabRels).linkKeyName, "row");
    //
    // return " return new CompositePK(" + pkGetterLiteria + " " + primary.createPKPlayloadParams(tabRels).toString() + ")";
    // }
    public FlatTableRelation getCurrentTableRelation(boolean child2Parent) {
        return new FlatTableRelation(this, child2Parent);
    }

    /**
     * 通过子记录查询父记录
     *
     * @param context
     */
    public String createSelectParentByChild(
            MergeData context, FlatTableRelation currentTableRelation, FlatTableRelation nextRelation, PrimaryTableMeta primary) {

        String methodToken = nextRelation.getJoinerKeysQueryMethodToken();

        context.addGlobalScript(methodToken
                , nextRelation.buildQueryHeaderByTailerInfo(
                        currentTableRelation.getTailerKeys().stream().map((rr) -> rr.getHeadLinkKey()).collect(Collectors.toSet())));

        FuncFormat f = nextRelation.buildInvokeQueryHeaderByTailerInfo();
        // 执行处理结果
        FuncFormat p = nextRelation.buildInvokeQueryHeaderByTailerInfoResultProcess(primary, currentTableRelation);
        f.appendLine(p);
        return f.toString();
    }


    /**
     * 当子表为主表的时候
     *
     * @param context
     * @return
     */
    public String createChildPKLiteria(MergeData context) {
        try {
            DependencyNode child = this.getChild();
            EntityName parentEntity = this.getParent().parseEntityName();
            EntityName childEntity = child.parseEntityName();
            PrimaryTableMeta primary = getPrimaryTableMeta(context, childEntity);
            // 主索引连接键
            // JoinerKey joinerKey = getPrimaryJoinerKey(primary, false);
            // primary.getPrimaryKeyName();
            // joinerKey.getParentKey();
            // 先查数据库，将子表的记录都查出来
            // child.getName();
            FuncFormat r = new FuncFormat();
            r.startLine("// create by TableRelation.createChildPKLiteria()");
            r.startLine("val primaryColMeta = tabColumnMetaMap.get(\"" + primary.getTabName() + "\")");
            r.startLine("if (primaryColMeta == null) {");
            r.startLine("    throw new IllegalStateException(\"tableName: " + primary.getTabName() + " is not exist in tabColumnMetaMap\")");
            r.startLine("}");
            r.startLine("val pcol = primaryColMeta.getColMeta(\"" + primary.getDBPrimayKeyName().getName() + "\")");
            FlatTableRelation currentTableRelation = this.getCurrentTableRelation(false);
            final String methodToken = currentTableRelation.getJoinerKeysQueryMethodToken();
            context.addGlobalScript(methodToken, currentTableRelation.buildQueryHeaderByTailerInfo(Collections.emptySet()));
            // // 定义结果集对象
            // r.startLine(childEntity.buildDefineRowMapListLiteria());
            // // 创建查询对象
            // r.startLine(childEntity.buildDefineCriteriaEqualLiteria());
            //
            // //<< 设置查询条件
            Stack<FlatTableRelation> unprocessedTableRelationStack = context.getUnprocessedTableRelations();
            String paramsList = null;
            if (!unprocessedTableRelationStack.empty()) {
                FlatTableRelation unprocessedTableRelation = unprocessedTableRelationStack.pop();
                // paramsList = unprocessedTableRelation.getTailerKeys().stream().map((m) -> {
                // TisGroupBy.TisColumn col = new TisGroupBy.TisColumn(m.getHeadLinkKey());
                // r.appendLine(col.buildDefineParam());
                // return col.getJavaVarName();
                // }).collect(Collectors.joining(","));
                paramsList = unprocessedTableRelation.getRel().getJoinerKeys().stream().map((m) -> {
                    TisGroupBy.TisColumn col = new TisGroupBy.TisColumn(m.getParentKey());
                    r.appendLine(col.buildDefineParam());
                    return col.getJavaVarName();
                }).collect(Collectors.joining(","));
            } else {
                paramsList = this.getJoinerKeys().stream().map((m) -> {
                    TisGroupBy.TisColumn col = new TisGroupBy.TisColumn(m.getParentKey());
                    r.appendLine(col.buildDefineParam());
                    return col.getJavaVarName();
                }).collect(Collectors.joining(","));
            }
            boolean isMulti = (this.cardinality == TabCardinality.ONE_N);
            // 执行调用方法
            r.startLine("val " + (isMulti ? childEntity.entities() + ": List[RowMap] " : childEntity.getJavaEntityName() + ": RowMap ") + " = this.query" + childEntity.javaPropTableName() + "By" + parentEntity.javaPropTableName() + "(" + paramsList + ") ");
            if (isMulti) {
                // 遍历结果集
                r.buildRowMapTraverseLiteria(childEntity, (mm) -> {
                    mm.startLine("pushPojo2Queue(" + primary.createCompositePK("primaryColMeta", "r") + ", row)");
                });
            } else {
                r.methodBody("if(" + childEntity.getJavaEntityName() + " != null)", (mm) -> {
                    // mm.startLine("return new CompositePK(" + childEntity.getJavaEntityName() + ", pcol /**子表列*/, sharedId)");
                    mm.startLine("return " + primary.createCompositePK("primaryColMeta", childEntity.getJavaEntityName()));
                });
            }
            r.startLine(" null /*gencode20200730*/");
            return r.toString();
        } finally {
            context.getUnprocessedTableRelations().clear();
        }
    }

    /**
     * 取得主键
     *
     * @param primary
     * @param parentKey
     * @return
     */
    private JoinerKey getPrimaryJoinerKey(PrimaryTableMeta primary, boolean parentKey) {
        final String compareTabName = (parentKey ? this.parent.getName() : this.child.getName());
        if (!StringUtils.equals(primary.getTabName(), compareTabName)) {
            throw new IllegalStateException("primary.getTabName:" + primary.getTabName() + " shall equal tabname:" + compareTabName);
        }
        PrimaryLinkKey dbPrimayKey = primary.getDBPrimayKeyName();
        Optional<JoinerKey> joinerKey = this.getJoinerKeys().stream().filter((r) -> dbPrimayKey.getName().equals(parentKey ? r.getParentKey() : r.getChildKey())).findFirst();
        if (!joinerKey.isPresent()) {
            throw new IllegalStateException("primary table:" + primary + " can not find joiner Keys in :" + linkKeysToString());
        }
        return joinerKey.get();
    }

    private String linkKeysToString() {
        return this.getJoinerKeys().stream().map((r) -> r.toString()).collect(Collectors.joining(","));
    }

    private PrimaryTableMeta getPrimaryTableMeta(MergeData context, final EntityName tab) {
        Set<PrimaryTableMeta> primaryTabs = context.getPrimaryTableNames();
        // SqlTaskNodeMeta.DependencyNode parent = this.getParent();
        Optional<PrimaryTableMeta> primary = primaryTabs.stream().filter((r) -> StringUtils.equals(tab.getTabName(), r.getTabName())).findFirst();
        if (!primary.isPresent()) {
            throw new IllegalStateException("table:" + tab.getTabName() + " is not one of primary tab");
        }
        return primary.get();
    }

    @Override
    public String toString() {
        return "parent:" + this.getParent().parseEntityName() + ",child:" + this.getChild().parseEntityName() + ",linkKeys:" + this.linkKeysToString();
    }
}
