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
package com.qlangtech.tis.sql.parser.visitor;

import com.facebook.presto.sql.tree.*;
import com.facebook.presto.sql.tree.Join.Type;
import com.facebook.presto.sql.tree.TISStackableAstVisitor.StackableAstVisitorContext;
import com.google.common.base.Joiner;
import com.qlangtech.tis.sql.parser.IDumpNodeMapContext;
import com.qlangtech.tis.sql.parser.NodeProcessResult;
import com.qlangtech.tis.sql.parser.TisGroupBy;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.ColRef;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.FunctionDataTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TabCriteriaEntityRecognizeVisitor;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import com.qlangtech.tis.sql.parser.utils.NodeUtils;
import org.apache.commons.lang.StringUtils;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年5月10日
 */
public class TableReferenceVisitor extends DefaultTraversalVisitor<NodeProcessResult<?>, TISStackableAstVisitor.StackableAstVisitorContext<Integer>> {

    private final ColRef colRef;

    private final IDumpNodeMapContext dumpNodsContext;

    public TableReferenceVisitor(ColRef colRef, IDumpNodeMapContext dumpNodsContext) {
        super();
        this.colRef = colRef;
        this.dumpNodsContext = dumpNodsContext;
    }

    @Override
    protected NodeProcessResult<?> visitJoin(Join node, StackableAstVisitorContext<Integer> context) {
        final Type type = node.getType();
        Relation left = node.getLeft();
        processLeftOrRightRelation(context, left, null, null);
        Relation right = node.getRight();
        Optional<JoinOn> joinOn = node.getCriteria().filter(criteria -> criteria instanceof JoinOn).map(criteria -> (JoinOn) criteria);
        processLeftOrRightRelation(context, right, joinOn, type);
        return null;
    }

    @Override
    protected NodeProcessResult<?> visitAliasedRelation(AliasedRelation node, StackableAstVisitorContext<Integer> context) {
        // 表引用名称
        final String alias = node.getAlias().getValue();
        IDataTupleCreator tupleCreator = getDataTupleCreatorByRef(alias);
        if (node.getRelation() instanceof Table) {
            processTableReferenceRecoginize(null, /* joinOn */
            null, /* jointype */
            (Table) node.getRelation(), tupleCreator, alias);
        } else if (node.getRelation() instanceof TableSubquery) {
            // this.process(node.getRelation(), context);
            processSubQuery(null, /* joinOn */
            null, /* jointype */
            node.getAlias(), tupleCreator, alias, (TableSubquery) node.getRelation());
        } else {
            NodeUtils.faild(node);
        }
        return null;
    // return super.visitAliasedRelation(node, context);
    }

    private IDataTupleCreator getDataTupleCreatorByRef(String alias) {
        IDataTupleCreator tupleCreator = colRef.getTupleCreator(alias);
        if (tupleCreator == null) {
            throw new IllegalStateException("alias:" + alias + ",in[" + Joiner.on(",").join(colRef.getBaseRefKeys()) + "] relevant tupleCreator shall not be null");
        }
        return tupleCreator;
    }

    // private NodeProcessResult<?> whenProcessJoinOn(String rightTab, JoinOn joinOn,
    // StackableAstVisitorContext<Integer> context) {
    // return this.process(joinOn.getExpression(), context);
    // }
    private void processLeftOrRightRelation(StackableAstVisitorContext<Integer> context, Relation rel, Optional<JoinOn> joinOn, Type jointype) {
        AliasedRelation aliasRel = null;
        Identifier a = null;
        IDataTupleCreator tupleCreator = null;
        if (rel instanceof Table) {
            Table tab = (Table) rel;
            tupleCreator = getDataTupleCreatorByRef(String.valueOf(tab.getName()));
            processTableReferenceRecoginize(joinOn, jointype, tab, tupleCreator, String.valueOf(tab.getName()));
        } else if (rel instanceof AliasedRelation) {
            aliasRel = (AliasedRelation) rel;
            a = aliasRel.getAlias();
            tupleCreator = getDataTupleCreatorByRef(aliasRel.getAlias().getValue());
            if (joinOn != null) {
                if (!joinOn.isPresent()) {
                    StreamTransformVisitor.faild(rel);
                }
            }
            if (aliasRel.getRelation() instanceof Table) {
                processTableReferenceRecoginize(joinOn, jointype, (Table) aliasRel.getRelation(), tupleCreator, a.getValue());
            } else if (aliasRel.getRelation() instanceof TableSubquery) {
                TableSubquery subQuery = (TableSubquery) aliasRel.getRelation();
                // example:
                // TableSubquery{Query{queryBody=QuerySpecification{select=Select{distinct=false,
                // selectItems=[i.order_id, "concat_ws"(',', "collect_set"("split"(i.batch_msg,
                // '[\\w\\W]*\\|')[1])) customer_ids]},
                // from=Optional[AliasedRelation{relation=Table{instance}, alias=i}],
                // where=(i.is_valid = 1), groupBy=Optional[GroupBy{isDistinct=false,
                // groupingElements=[SimpleGroupBy{columns=[order_id]}]}], having=null,
                // orderBy=Optional.empty, limit=null}, orderBy=Optional.empty}}
                // subQuery.getQuery();
                processSubQuery(joinOn, jointype, a, tupleCreator, a.getValue(), /* alias */
                subQuery);
            // this.process(aliasRel.getRelation(), context);
            } else {
                NodeUtils.faild(aliasRel.getRelation());
            }
        } else {
            this.process(rel, context);
        }
    }

    private void processSubQuery(Optional<JoinOn> joinOn, Type jointype, Identifier a, IDataTupleCreator tupleCreator, String tabRefName, TableSubquery subQuery) {
        final String tabName = "innertab_" + a.getValue();
        //
        TabCriteriaEntityRecognizeVisitor tabCriteriaEntityRecognizeVisitor = new TabCriteriaEntityRecognizeVisitor(new TabCriteria(tabName, joinOn == null ? null : joinOn.get(), joinOn == null, jointype), this.dumpNodsContext);
        tabCriteriaEntityRecognizeVisitor.setSubQuery(subQuery);
        tupleCreator.accept(tabCriteriaEntityRecognizeVisitor);
        this.setFuncGroupByTuple(tupleCreator, tabRefName);
    }

    /**
     * @param joinOn
     * @param jointype
     * @param table
     * @param tupleCreator
     * @param tabRefName   表引用名称
     */
    private void processTableReferenceRecoginize(Optional<JoinOn> joinOn, Type jointype, Table table, IDataTupleCreator tupleCreator, final String tabRefName) {
        String tabName = String.valueOf((table).getName());
        //
        TabCriteriaEntityRecognizeVisitor tabCriteriaEntityRecognizeVisitor = new //
        TabCriteriaEntityRecognizeVisitor(new TabCriteria(tabName, joinOn == null ? null : joinOn.get(), joinOn == null, jointype), this.dumpNodsContext);
        tupleCreator.accept(tabCriteriaEntityRecognizeVisitor);
        this.setFuncGroupByTuple(tupleCreator, tabRefName);
    }

    private void setFuncGroupByTuple(IDataTupleCreator tupleCreator, final String tabRefName) {
        Stream<FunctionDataTupleCreator> funcs = this.colRef.getColRefMap().values().stream().filter((r) -> r instanceof FunctionDataTupleCreator).map((r) -> (FunctionDataTupleCreator) r);
        funcs.forEach((f) -> {
            Optional<TisGroupBy> g = f.getGroupBy();
            if (g.isPresent()) {
                g.get().getGroups().forEach((group) -> {
                    TableTupleCreator tabTuple = null;
                    if (!group.isTupleSetted() && StringUtils.equals(group.getTabRef(), tabRefName)) {
                        if (!(tupleCreator instanceof TableTupleCreator)) {
                            throw new IllegalStateException("tupleCreator type shall be TableTupleCterator");
                        }
                        tabTuple = (TableTupleCreator) tupleCreator;
                        if (!tabRefName.equals(tabTuple.getMediaTabRef())) {
                            throw new IllegalStateException("tabRefName:" + tabRefName + ",tabTuple.getMediaTabRef:" + tabTuple.getMediaTabRef() + " shall be equal");
                        }
                        group.setTabTuple(tabTuple);
                    }
                });
            }
        });
    }

    public static class TabCriteria {

        private final String name;

        private final JoinOn joinOn;

        // 是否是主表
        private boolean primary;

        private Type jointype;

        public TabCriteria(String name, JoinOn joinOn, boolean primary, Type jointype) {
            super();
            this.name = name;
            this.joinOn = joinOn;
            this.primary = primary;
            this.jointype = jointype;
        }

        public Type getJointype() {
            return jointype;
        }

        public void setJointype(Type jointype) {
            this.jointype = jointype;
        }

        public boolean isPrimary() {
            return this.primary;
        }

        public void setPrimary(boolean primary) {
            this.primary = primary;
        }

        public String getName() {
            return this.name;
        }

        public JoinOn getJoinOn() {
            return this.joinOn;
        }
    }
}
