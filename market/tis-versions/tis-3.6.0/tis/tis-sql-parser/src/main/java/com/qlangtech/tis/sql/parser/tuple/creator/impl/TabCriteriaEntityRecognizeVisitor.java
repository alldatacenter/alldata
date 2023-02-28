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
package com.qlangtech.tis.sql.parser.tuple.creator.impl;

import java.util.Optional;
import com.qlangtech.tis.sql.parser.IDumpNodeMapContext;
import com.qlangtech.tis.sql.parser.meta.NodeType;
import com.qlangtech.tis.sql.parser.SqlTaskNode;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IDataTupleCreatorVisitor;
import com.qlangtech.tis.sql.parser.visitor.TableDependencyVisitor;
import com.qlangtech.tis.sql.parser.visitor.TableReferenceVisitor.TabCriteria;
import com.facebook.presto.sql.SqlFormatter;
import com.facebook.presto.sql.tree.ComparisonExpression;
import com.facebook.presto.sql.tree.JoinOn;
import com.facebook.presto.sql.tree.LogicalBinaryExpression;
import com.facebook.presto.sql.tree.Query;
import com.facebook.presto.sql.tree.TableSubquery;

/**
 * 需要将实体别名识别成真实名称
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年5月13日
 */
public class TabCriteriaEntityRecognizeVisitor implements IDataTupleCreatorVisitor {

    private final TabCriteria tabCriteria;

    private final IDumpNodeMapContext dumpNodsContext;

    private TableSubquery subQuery;

    public TabCriteriaEntityRecognizeVisitor(TabCriteria tabCriteria, IDumpNodeMapContext dumpNodsContext) {
        super();
        this.tabCriteria = tabCriteria;
        this.dumpNodsContext = dumpNodsContext;
    }

    @Override
    public void visit(FunctionDataTupleCreator function) {
    // Expression expression = function.getExpression();
    // Map<ColName, IDataTupleCreator> params = function.getParams();
    }

    @Override
    public void visit(TableTupleCreator tableTuple) {
        TabCriteria tableCriteria = tabCriteria;
        // join 部分逻辑处理
        if (!tableCriteria.isPrimary()) {
            JoinOn joinOn = tableCriteria.getJoinOn();
            ComparisonExpression compare = null;
            LogicalBinaryExpression logic = null;
            if (joinOn.getExpression() instanceof ComparisonExpression) {
                // (tp.totalpay_id = o.totalpay_id)
                compare = (ComparisonExpression) joinOn.getExpression();
            } else if (joinOn.getExpression() instanceof LogicalBinaryExpression) {
                // ((tp.card_id = cc.id) AND (tp.entity_id = cc.entity_id))
                logic = (LogicalBinaryExpression) joinOn.getExpression();
            }
        }
        if (this.subQuery != null) {
            // 是像 totalpay/order_customers.txt 文件中那样有内嵌子查询的
            SqlTaskNode subqueryTaskNode = new SqlTaskNode(EntityName.parse(tableCriteria.getName()), NodeType.JOINER_SQL, this.dumpNodsContext);
            // Map<ColName, ValueOperator> columnTracer = Maps.newHashMap();
            // Rewriter rewriter = Rewriter.create(columnTracer);
            subqueryTaskNode.setContent(SqlFormatter.formatSql(this.subQuery.getQuery().getQueryBody(), Optional.empty()));
            TableDependencyVisitor dependenciesVisitor = TableDependencyVisitor.create();
            Query query = SqlTaskNode.parseQuery(subqueryTaskNode.getContent());
            dependenciesVisitor.process(query, null);
            dependenciesVisitor.getTabDependencies().stream().forEach((table) -> {
                // ;
                //
                // List<TableTupleCreator> tables = SqlTaskNode.dumpNodes.get(table);
                // if (tables.size() != 1) {
                // throw new IllegalStateException("table:" + table + " relevant tab has more than 1 match");
                // }
                // tables.get(0).getEntityName();
                EntityName firstMatch = dumpNodsContext.accurateMatch(table);
                subqueryTaskNode.addRequired(firstMatch, new SqlTaskNode(firstMatch, NodeType.DUMP, this.dumpNodsContext));
            });
            final TableTupleCreator tupleCreator = subqueryTaskNode.parse(true);
            tableTuple.setColsRefs(tupleCreator.getColsRefs());
            tableTuple.setRealEntityName(tupleCreator.getEntityName());
        // tableTuple.setEntityRef(new EntitiyRef(tableCriteria.getName(),
        // subqueryTaskNode));
        } else {
            // tableTuple.setEntityRef(new EntitiyRef(tableCriteria.getName()));
            // List<TableTupleCreator> tabs = null;
            EntityName ename = this.dumpNodsContext.nullableMatch(tableCriteria.getName());
            if (ename != null) {
                tableTuple.setNodetype(NodeType.DUMP);
                tableTuple.setRealEntityName(ename);
            } else {
                tableTuple.setRealEntityName(EntityName.parse(tableCriteria.getName()));
            }
        // if ((tabs = SqlTaskNode.dumpNodes.get(tableCriteria.getName())) != null) {
        // tableTuple.setNodetype(NodeType.DUMP);
        //
        // if (tabs.size() != 1) {
        // throw new IllegalStateException(
        // "tabname:" + tableCriteria.getName() + " relevant tab size shall be 1 but " + tabs.size());
        // } else {
        // tableTuple.setRealEntityName(tabs.get(0).getEntityName());
        // }
        // } else {
        // tableTuple.setRealEntityName(EntityName.parse(tableCriteria.getName()));
        // }
        }
    }

    public TableSubquery getSubQuery() {
        return this.subQuery;
    }

    public void setSubQuery(TableSubquery subQuery) {
        this.subQuery = subQuery;
    }
}
