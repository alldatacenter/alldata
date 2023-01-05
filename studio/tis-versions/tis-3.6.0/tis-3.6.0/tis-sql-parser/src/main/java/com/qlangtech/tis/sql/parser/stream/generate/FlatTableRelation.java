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

import com.qlangtech.tis.sql.parser.TisGroupBy;
import com.qlangtech.tis.sql.parser.er.LinkKeys;
import com.qlangtech.tis.sql.parser.er.PrimaryTableMeta;
import com.qlangtech.tis.sql.parser.er.TabCardinality;
import com.qlangtech.tis.sql.parser.er.TableRelation;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.visitor.FuncFormat;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class FlatTableRelation {

    private final TableRelation rel;

    private final boolean child2Parent;

    public FlatTableRelation(TableRelation rel, boolean child2Parent) {
        this.rel = rel;
        this.child2Parent = child2Parent;
    }

    public TableRelation getRel() {
        return this.rel;
    }

    public boolean isChild2Parent() {
        return this.child2Parent;
    }

    private String buildQueryHeaderByTailerInfoMethodName() {
        final EntityName tailerEntity = this.getTailerEntity();
        final EntityName headEntity = this.getHeaderEntity();
        return "query" + headEntity.javaPropTableName() + "By" + tailerEntity.javaPropTableName();
    }

    /**
     * 通过子表查询主表（head），生成查询方法
     *
     * @param extraHeaderColKeys 额外的col
     * @return
     */
    public FuncFormat buildQueryHeaderByTailerInfo(final Set<String> extraHeaderColKeys) {
        // final String paramsList = this.getJoinerKeys().stream().map((jk) -> {
        // TisGroupBy.TisColumn col = new TisGroupBy.TisColumn(jk.getChildKey());
        // return col.getJavaVarName() + " : String";
        // }).collect(Collectors.joining(","));
        // EntityName headerEntity = relation.getHeader().parseEntityName();
        final EntityName tailerEntity = this.getTailerEntity();
        final EntityName headEntity = this.getHeaderEntity();
        final String methodToken = "private def " + this.buildQueryHeaderByTailerInfoMethodName() + "(" + this.getJoinerKeysQueryMethodParamsLiteria() + ") : " + (this.isHeaderMulti() ? "List[RowMap]" : "RowMap") + " =";
        FuncFormat r = new FuncFormat();
        r.methodBody(methodToken, (m) -> {
            // 定义结果集对象
            m.startLine(headEntity.buildDefineRowMapListLiteria());
            // 创建查询对象
            m.startLine(headEntity.buildDefineCriteriaEqualLiteria());
            // << 设置查询条件
            // this.getJoinerKeys().stream().forEach((mm) -> {
            // TisGroupBy.TisColumn col = new TisGroupBy.TisColumn(mm.getChildKey());
            // m.appendLine(col.buildDefineParam());
            // });
            m.returnLine();
            m.startLine(headEntity.buildCreateCriteriaLiteria());
            this.getHeaderKeys().stream().forEach((mm) -> {
                TisGroupBy.TisColumn col = new TisGroupBy.TisColumn(mm.getHeadLinkKey());
                m.append(col.buildPropCriteriaEqualLiteria());
            });
            // 设置查询条件>>
            // 设置select的列
            Set<String> selCols = this.getHeaderKeys().stream().map((link) -> link.getHeadLinkKey()).collect(Collectors.toSet());
            selCols.addAll(extraHeaderColKeys);
            m.startLine(headEntity.buildAddSelectorColsLiteria(selCols));
            // 执行dao查询
            m.startLine(headEntity.buildExecuteQueryDAOLiteria());
            if (this.isHeaderMulti()) {
                m.startLine("return " + headEntity.entities());
            } else {
                m.methodBody("for ( ( r:RowMap) <- " + headEntity.entities() + ".asScala)", (rt) -> {
                    rt.startLine("return r");
                });
                m.startLine("null");
            }
        });
        return r;
    }

    /**
     * 执行调用通过子查询父表逻辑
     *
     * @return
     */
    public FuncFormat buildInvokeQueryHeaderByTailerInfo() {
        FuncFormat r = new FuncFormat();
        final String paramsList = this.getHeaderKeys().stream().map((m) -> {
            TisGroupBy.TisColumn col = new TisGroupBy.TisColumn(m.getHeadLinkKey());
            r.appendLine("val " + col.getJavaVarName() + " = row.getColumn(\"" + m.getTailerLinkKey() + "\")");
            return col.getJavaVarName();
        }).collect(Collectors.joining(","));
        EntityName headerEntity = this.getHeaderEntity();
        r.startLine("val " + (this.isHeaderMulti() ? headerEntity.entities() + ": List[RowMap] " : headerEntity.getJavaEntityName() + ": RowMap ") + " = this." + this.buildQueryHeaderByTailerInfoMethodName() + "(" + paramsList + ") ");
        return r;
    }

    /**
     * 创建查询结处理
     *
     * @param primary
     * @param preTableRelation 上一个表关联关系
     * @return
     */
    public FuncFormat buildInvokeQueryHeaderByTailerInfoResultProcess(PrimaryTableMeta primary, FlatTableRelation preTableRelation) {
        FuncFormat f = new FuncFormat();
        EntityName headerEntity = this.getHeaderEntity();
        f.appendLine("val " + headerEntity.getJavaEntityName() + "Meta : AliasList = tabColumnMetaMap.get(\"" + headerEntity.getTabName() + "\");");
        // TableRelation.FinalLinkKey finalLinkKey = getFinalLinkKey(primary.getDBPrimayKeyName().getName(), preTableRelation);
        // final String pkColGetter = headerEntity.createColGetterLiteria(finalLinkKey.linkKeyName);
        // headerEntity.getJavaEntityName() + "Meta.getColMeta(\"" + finalLinkKey.linkKeyName + "\")";
        final String createCompositePKLiteria
                = primary.createCompositePK(headerEntity.getJavaEntityName() + "Meta"
                , this.isHeaderMulti() ? "r" : headerEntity.getJavaEntityName(), true, preTableRelation);

        if (this.isHeaderMulti()) {
            // 遍历结果集
            f.buildRowMapTraverseLiteria(headerEntity, (mm) -> {
                mm.startLine("pushPojo2Queue(" + createCompositePKLiteria + ", row)").append("/*gencode3*/");
                // mm.startLine("pushPojo2Queue(new CompositePK("
                // + headerEntity.createColValLiteria(finalLinkKey.linkKeyName, "r") + ", r), row)");
            });
        } else {
            f.methodBody("if(" + headerEntity.getJavaEntityName() + " != null)", (mm) -> {
                mm.startLine("return " + createCompositePKLiteria).append("/*gencode4*/");
                // mm.startLine("return new CompositePK(" + headerEntity.createColValLiteria("dddddd")
                // + "," + headerEntity.getJavaEntityName() + ")").append("/*codegen4*/");
            });
        }
        f.startLine(" null");
        return f;
    }

    private String buidQueryHeaderByTailerInfoReturnValName() {
        return this.isHeaderMulti() ? this.getHeaderEntity().entities() : this.getHeaderEntity().getJavaEntityName();
    }

    /**
     * 是否能通过主外键相连
     *
     * @param next
     * @return
     */
    public boolean isLinkable(FlatTableRelation next) {
        DependencyNode currForeig = this.getTailer();
        DependencyNode nextPrimary = next.getHeader();
        EntityName currEntityName = currForeig.parseEntityName();
        EntityName nextEntityName = nextPrimary.parseEntityName();
        if (!currEntityName.equals(nextEntityName)) {
            // return false;
            throw new IllegalStateException("curr foreign:" + currEntityName + " next primary:" + nextEntityName + "  must be equal");
        }
        return CollectionUtils.isEqualCollection(this.getTailerKeys().stream().map((r) -> r.getHeadLinkKey()).collect(Collectors.toList()), next.getHeaderKeys().stream().map((r) -> r.getHeadLinkKey()).collect(Collectors.toList()));
    }

    public TableRelation.FinalLinkKey getFinalLinkKey(PrimaryTableMeta primary, FlatTableRelation next) {
        TableRelation.FinalLinkKey currFinalLinkKey = getFinalLinkKey(primary.getDBPrimayKeyName().getName(), this, next);
        return currFinalLinkKey;
    }

    /**
     * 多个表连接起来，tabRels数组的第一个元素为主索引表
     *
     * @param primaryColName
     * @param tabRels        数组长度可以为空
     * @return
     */
    public static TableRelation.FinalLinkKey getFinalLinkKey(String primaryColName, FlatTableRelation... tabRels) {
        for (FlatTableRelation tabRel : tabRels) {
            final String tmpPrimaryColName = primaryColName;
            Optional<LinkKeys> headerLinkKey = tabRel.getHeaderKeys().stream().filter((r) -> {
                return StringUtils.equals(r.getHeadLinkKey(), tmpPrimaryColName);
            }).findFirst();

            if (!headerLinkKey.isPresent()) {
//                throw new IllegalStateException("header:" + tabRel.getHeaderEntity() + ",tailer:" + tabRel.getTailerEntity()
//                        + " can not find key:" + primaryColName + ",cols:"
//                        + tabRel.getHeaderKeys().stream().map((r) -> "[" + r.getHeadLinkKey() + "->" + r.getTailerLinkKey() + "]")
//                        .collect(Collectors.joining(",")));
                // 例如：orderDetail是主表，以orderid作为pk，外表totalpayinfo 为外表（连接键为: totalpay_id -> totalpay_id,所以连接过程会中断
                return new TableRelation.FinalLinkKey(false, primaryColName, tabRel);
            }
            primaryColName = headerLinkKey.get().getTailerLinkKey();
        }
        return new TableRelation.FinalLinkKey(primaryColName);
    }


    /**
     * 取得索引主节点
     *
     * @return
     */
    private DependencyNode getHeader() {
        return this.child2Parent ? this.rel.getParent() : this.rel.getChild();
    }

    private EntityName headerEntity;

    private EntityName tailerEntity;

    public EntityName getHeaderEntity() {
        if (this.headerEntity == null) {
            this.headerEntity = this.getHeader().parseEntityName();
        }
        return this.headerEntity;
    }

    public EntityName getTailerEntity() {
        if (this.tailerEntity == null) {
            this.tailerEntity = this.getTailer().parseEntityName();
        }
        return this.tailerEntity;
    }

    @Override
    public String toString() {
        return "header:" + this.getHeaderEntity() + ",tailer:" + this.getTailerEntity();
    }

    /**
     * 头节点是否是多维的
     *
     * @return
     */
    public boolean isHeaderMulti() {
        if (this.rel.isCardinalityEqual(TabCardinality.ONE_ONE))
            return false;
        if (this.rel.isCardinalityEqual(TabCardinality.ONE_N)) {
            return !this.child2Parent;
        }
        throw new IllegalStateException("un recgnize carinality " + this.rel.getCardinality());
    }

    /**
     * 生成查询参数列表
     *
     * @return
     */
    public String getJoinerKeysQueryMethodParamsLiteria() {
        final String paramsList = this.getTailerKeys().stream().map((jk) -> {
            TisGroupBy.TisColumn col = new TisGroupBy.TisColumn(jk.getTailerLinkKey());
            return col.getJavaVarName() + " : String";
        }).collect(Collectors.joining(","));
        return paramsList;
    }

    public final String getJoinerKeysQueryMethodToken() {
        final String methodToken = "private def query" + this.getHeader().parseEntityName().javaPropTableName() + "By" + this.getTailer().parseEntityName().javaPropTableName() + "(" + this.getJoinerKeysQueryMethodParamsLiteria() + ") : " + (this.isHeaderMulti() ? "List[RowMap]" : "RowMap") + " =";
        return methodToken;
    }

    public List<LinkKeys> getHeaderKeys() {
        return getLinkKeys(!this.child2Parent);
    }

    public List<LinkKeys> getTailerKeys() {
        return getLinkKeys(this.child2Parent);
    }

    private List<LinkKeys> getLinkKeys(final boolean child2Parent) {
        return this.rel.getJoinerKeys().stream().map((r) -> {
            return child2Parent ? new LinkKeys(r.getChildKey(), r.getParentKey()) : new LinkKeys(r.getParentKey(), r.getChildKey());
        }).collect(Collectors.toList());
    }

    /**
     * 宽表的子表节点
     *
     * @return
     */
    private DependencyNode getTailer() {
        return this.child2Parent ? this.rel.getChild() : this.rel.getParent();
    }
}
