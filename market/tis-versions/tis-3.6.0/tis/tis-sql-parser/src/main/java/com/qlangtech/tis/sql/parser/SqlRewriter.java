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

import com.facebook.presto.sql.tree.*;
import com.google.common.collect.Lists;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.sql.parser.SqlFormatter.Formatter;
import com.qlangtech.tis.sql.parser.SqlStringBuilder.RewriteProcessContext;
import com.qlangtech.tis.sql.parser.er.IPrimaryTabFinder;
import com.qlangtech.tis.sql.parser.er.TableMeta;
import com.qlangtech.tis.sql.parser.exception.TisSqlFormatException;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.qlangtech.tis.sql.parser.ExpressionFormatter.formatExpression;

/**
 * 重写sql
 * <ul>
 *     <li>添加上表的pt，pmod约束</li>
 *     <li>对于某些列添加上field processor函数</li>
 * </ul>
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年8月27日
 */
public class SqlRewriter extends Formatter {

    public static final String ERROR_WithoutDefinePrimaryTableShareKey = "please check the Er rule of dataflow whether has set 'shardKey' or not";


    private final TabPartitions tabPartition;

    final List<AliasTable> waitProcessAliasTabsSet = Lists.newArrayList();

    private AliasTable primayTable;

    // 是否是数据流的最终节点
    private final boolean isFinal;

    private final IPrimaryTabFinder erRules;

    private final IJoinTaskContext joinContext;

    // private final SqlTaskNodeMeta nodeMeta;
    // 主表
    public AliasTable getPrimayTable() {
        return this.primayTable;
    }

    @Override
    protected void setPrimayTable(AliasTable t) {
        this.primayTable = t;
    }

    public SqlRewriter(SqlStringBuilder builder, Map<IDumpTable, ITabPartition> tabPartition, IPrimaryTabFinder erRules
            , Optional<List<Expression>> parameters, boolean isFinal, IJoinTaskContext joinContext) {
        this(builder, new TabPartitions(tabPartition), erRules, parameters, isFinal, joinContext);
    }

    public SqlRewriter(SqlStringBuilder builder, TabPartitions tabPartition, IPrimaryTabFinder erRules
            , Optional<List<Expression>> parameters, boolean isFinal, IJoinTaskContext joinContext) {
        super(builder, erRules.getTabFieldProcessorMap(), parameters);
        this.erRules = erRules;
        this.tabPartition = tabPartition;
        this.isFinal = isFinal;
        Objects.requireNonNull(joinContext, "param joinContext can not be null");
        Objects.requireNonNull(joinContext.getExecutePhaseRange(), "executePhaseRange can not be null");
        this.joinContext = joinContext;
    }

    @Override
    protected String createPtPmodCols(AliasTable a) {
        if (isFinal) {
            Optional<TableMeta> ptab = this.erRules.getPrimaryTab(a.getTable());
            StringBuffer result = new StringBuffer(a.getAlias() + "." + IDumpTable.PARTITION_PT + ",");

            // 如果当前是索引构建的场景下，需要校验是否已经设置分区键，这个判断非常重要2021/2/7，这个校验在最开始的点击触发按钮的时候也要校验
            if (joinContext.getExecutePhaseRange().contains(FullbuildPhase.BUILD)
                    && !TableMeta.hasValidPrimayTableSharedKey(ptab)) {
                throw new IllegalStateException(ERROR_WithoutDefinePrimaryTableShareKey);
            }

            if (ptab.isPresent()) {
                TableMeta tabMeta = ptab.get();
                String shardKey = tabMeta.getSharedKey();
                if (StringUtils.isEmpty(shardKey)) {
                    throw new IllegalStateException(tabMeta.toString() + " has not set 'shardKey' ");
                }
                try {
                    Integer shardCount = joinContext.getIndexShardCount();
                    result.append("abs( hash( cast( ").append(a.getAlias()).append(".")
                            .append(shardKey).append(" as string)) % ").append(shardCount).append(" ) AS ").append(IDumpTable.PARTITION_PMOD);
                } catch (Exception e) {
                    throw new RuntimeException(tabMeta.toString(), e);
                }
                // 说明是主表,这个函数是Hive专用的
                // return a.getAlias() + "." + IDumpTable.PARTITION_PT + "," + "abs(pmod( hash( cast( " + a.getAlias() + "." + shardKey + " as string) ) , " + shardCount + ")) AS " + IDumpTable.PARTITION_PMOD;
            } else {
                // return a.getAlias() + "." + IDumpTable.PARTITION_PT + "," + a.getAlias() + "." + IDumpTable.PARTITION_PMOD;
                result.append(a.getAlias()).append(".").append(IDumpTable.PARTITION_PMOD);
            }
            return result.toString();
        } else {
            return super.createPtPmodCols(a);
        }
    }

    @Override
    protected Void visitSingleColumn(SingleColumn node, Integer indent) {
        return super.visitSingleColumn(node, indent);
    }

    @Override
    protected List<AliasTable> getWaitProcessAliasTabsSet() {
        return this.waitProcessAliasTabsSet;
    }

    @Override
    protected Void visitTable(Table tab, Integer indent) {
        String tableName = String.valueOf(tab.getName());
        QualifiedName tabName = tab.getName();
        Map.Entry<IDumpTable, ITabPartition> findTab = parseDumpTable(tabName);
        waitProcessAliasTabsSet.add(new AliasTable(tableName, findTab.getKey(), findTab.getValue()));
        processTable(findTab);
        return null;
    }

    private void processTable(Map.Entry<IDumpTable, ITabPartition> findTab) {
        IDumpTable t = findTab.getKey();
        this.builder.append(t.getDbName()).append(".").append(t.getTableName());
    }

    @Override
    protected Void visitAliasedRelation(AliasedRelation node, Integer indent) {
        if (node.getRelation() instanceof Table) {
            Table tab = (Table) node.getRelation();
            Map.Entry<IDumpTable, ITabPartition> dumpTable = parseDumpTable(tab.getName());
            waitProcessAliasTabsSet.add(new AliasTable(node.getAlias().getValue(), dumpTable.getKey(), dumpTable.getValue()));
            processTable(dumpTable);
            builder.append(' ').append(formatExpression(node.getAlias(), parameters));
            SqlFormatter.appendAliasColumns(builder, node.getColumnNames());
            return null;
        } else if (node.getRelation() instanceof TableSubquery) {
            SqlRewriter w = new SqlRewriter(new SqlStringBuilder(), this.tabPartition, this.erRules, this.parameters, false, joinContext);
            w.process(node.getRelation(), 0);
            Optional<AliasTable> subTable = w.getWaitProcessAliasTabsSet().stream().findFirst();
            if (!subTable.isPresent()) {
                throw new IllegalStateException("subtable:" + node.getAlias().getValue() + " can not find subtable");
            }
            // 为了重写select部分的pt,pmod部分，需要把该别名记录下来
            waitProcessAliasTabsSet.add(new AliasTable(node.getAlias().getValue(), subTable.get()));
            return super.visitAliasedRelation(node, indent);
        } else {
            throw new UnsupportedOperationException();
        }
        // process(node.getRelation(), indent);
        // builder.append(' ').append(formatExpression(node.getAlias(), parameters));
        // SqlFormatter.appendAliasColumns(builder, node.getColumnNames());
        // return null;//
        // return super.visitAliasedRelation(node, indent);
    }

    @Override
    protected Void visitLogicalBinaryExpression(LogicalBinaryExpression node, Integer context) {
        if (SqlStringBuilder.isInRewriteProcess()) {
            this.process(node.getLeft(), context);
            this.process(node.getRight(), context);
        }
        return super.visitLogicalBinaryExpression(node, context);
    }

    @Override
    protected void processAppendPtWhere(Optional<Expression> where) {
        try {
            RewriteProcessContext processContext = new RewriteProcessContext();
            SqlStringBuilder.inRewriteProcess.set(processContext);
            if (where.isPresent()) {
                this.process(where.get(), MAGIC_TOKEN_JOINON_PROCESS);
                processPTRewrite(processContext);
            } else if (this.hasAnyUnprocessedAliasTabsSet()) {
                this.builder.appendIgnoreProcess(this.getWaitProcessAliasTabsSet().stream().filter((r) -> !r.isPtRewriterOver() && !r.isSubQueryTable()).map((r) -> {
                    r.setPtRewriter(true);
                    return r.getAliasPtCriteria();
                }).collect(Collectors.joining(" AND ")));
            }
        } finally {
            SqlStringBuilder.inRewriteProcess.set(null);
        }
    }

    // baisui add
    @Override
    protected void processJoinOn(JoinOn on) {
        // this.process(on.getExpression(), MAGIC_TOKEN_JOINON_PROCESS);
        try {
            RewriteProcessContext processContext = new RewriteProcessContext();
            SqlStringBuilder.inRewriteProcess.set(processContext);
            LogicalBinaryExpression logical = null;
            if (on.getExpression() instanceof LogicalBinaryExpression) {
                logical = (LogicalBinaryExpression) on.getExpression();
                this.process(logical.getLeft(), MAGIC_TOKEN_JOINON_PROCESS);
                this.process(logical.getRight(), MAGIC_TOKEN_JOINON_PROCESS);
                logical.getOperator();
            } else {
                this.process(on.getExpression(), MAGIC_TOKEN_JOINON_PROCESS);
            }
            this.processPTRewrite(processContext);
        } finally {
            SqlStringBuilder.inRewriteProcess.set(null);
        }
    }

    private void processPTRewrite(RewriteProcessContext processContext) {
        Optional<AliasTable> aliasOptional = null;
        AliasTable at = null;
        while (!processContext.tabAliasStack.isEmpty()) {
            final String alias = processContext.tabAliasStack.pop();
            aliasOptional = this.getWaitProcessAliasTabsSet().stream().filter((r) -> {
                return StringUtils.equals(r.getAlias(), alias) && !r.isPtRewriterOver() && !r.isSubQueryTable();
            }).findFirst();
            if (aliasOptional.isPresent()) {
                at = aliasOptional.get();
                this.builder.appendIgnoreProcess(" AND ").appendIgnoreProcess(at.getAlias()
                        + "." + IDumpTable.PARTITION_PT + "='").appendIgnoreProcess(at.getTabPartition()).appendIgnoreProcess("'");
                at.setPtRewriter(true);
            }
        }
    }

    private Map.Entry<IDumpTable, ITabPartition> parseDumpTable(QualifiedName tabName) {
        List<String> originalParts = tabName.getOriginalParts();
        Optional<Map.Entry<IDumpTable, ITabPartition>> find = null;
        if (originalParts.size() == 2) {

            find = tabPartition.findTablePartition(originalParts.get(0), originalParts.get(1));

//            find = tabPartition.entrySet().stream().filter((r) ->
//                    (StringUtils.equals(r.getKey().getDbName(), originalParts.get(0))
//                            && StringUtils.equals(r.getKey().getTableName(), originalParts.get(1)))).findFirst();
        } else if (originalParts.size() == 1) {
            final RewriterDumpTable tab = RewriterDumpTable.create(originalParts.get(0));
            //int[] count = new int[1];

            find = tabPartition.findTablePartition(tab.tabname);

//            Stream<Map.Entry<IDumpTable, ITabPartition>> findTabStream = tabPartition.entrySet().stream().filter((r) -> {
//                boolean match = StringUtils.equals(r.getKey().getTableName(), tab.tabname);
//                if (match) {
//                    count[0]++;
//                }
//                return match;
//            });
//            if (count[0] > 1) {
//                throw new IllegalStateException("tabname:" + tab.tabname + " has match more than 1 context tab:" + findTabStream.map((r) -> r.toString()).collect(Collectors.joining(",")));
//            }
            //  find = findTabStream.findFirst();
            // 重新rewrite表名称
        } else {
            throw new IllegalStateException("tabName:" + String.valueOf(tabName) + " is not illegal");
        }
        if (!find.isPresent()) {
            throw new TisSqlFormatException(tabName.toString() + " can not find tab in[" + tabPartition.joinFullNames() + "]", Optional.empty()); // IllegalStateException(tabName.toString() + " can not find tab in[" + tabPartition.joinFullNames() + "]");
        }
        Map.Entry<IDumpTable, ITabPartition> findTab = find.get();
        return findTab;
    }

    public static class AliasTable implements IAliasTable {

        private final String alias;

        private final IDumpTable table;

        private final ITabPartition tabPartition;

        private boolean ptRewriter = false;

        private final boolean subQueryTable;

        // 在SQL WHERE部分添加PT,PMOD
        private boolean selectPtAppendProcess = false;

        // 嵌套关系的子
        private AliasTable child;

        public AliasTable(String alias, IDumpTable table, ITabPartition tabPartition, boolean subQueryTable) {
            super();
            this.table = table;
            this.alias = alias;
            if (tabPartition == null) {
                throw new IllegalStateException("alias:" + alias + ",table:" + table + " relevant partition can not be null");
            }
            this.tabPartition = tabPartition;
            this.subQueryTable = subQueryTable;
        }

        @Override
        public String getPt() {
            return this.getTabPartition();
        }

        public AliasTable(String alias, IDumpTable table, ITabPartition tabPartition) {
            this(alias, table, tabPartition, false);
        }

        public AliasTable(String alias, AliasTable child) {
            this(alias, null, () -> {
                throw new UnsupportedOperationException("alias:" + alias + " not not support pt");
            }, true);
            this.setPtRewriter(true);
            this.child = child;
        }

        public boolean isPtRewriterOver() {
            return ptRewriter;
        }

        public void setPtRewriter(boolean ptRewriter) {
            this.ptRewriter = ptRewriter;
        }

        public boolean isSelectPtAppendProcess() {
            return this.selectPtAppendProcess;
        }

        public void makeSelectPtAppendProcess() {
            this.selectPtAppendProcess = true;
        }

        public boolean isSubQueryTable() {
            return this.subQueryTable;
        }

        public String getAliasPtCriteria() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(this.getAlias()).append("." + IDumpTable.PARTITION_PT + "='").append(this.getTabPartition()).append("'");
            return buffer.toString();
        }

        public String getAlias() {
            return alias;
        }

        public IDumpTable getTable() {
            return table;
        }

        public String getTabPartition() {
            if (this.child != null) {
                return this.child.getTabPartition();
            }
            return this.tabPartition.getPt();
        }

        public AliasTable getChild() {
            return this.child;
        }

        @Override
        public String toString() {
            final StringBuffer buffer = new StringBuffer();
            buffer.append("alias:").append(this.getAlias());
            buffer.append(",hasPtAppend:").append(this.selectPtAppendProcess);
            buffer.append(",hasProcess:").append(this.selectPtAppendProcess);
            if (this.subQueryTable) {
                return buffer.toString();
            }
            buffer.append(",table:").append(this.getTable()).append(",pt:").append(this.getTabPartition());
            return buffer.toString();
        }
    }

    public static class RewriterDumpTable implements IDumpTable {

        private final String dbname;

        private final String tabname;

        private RewriterDumpTable(String dbname, String tabname) {
            super();
            this.dbname = dbname;
            this.tabname = tabname;
        }

       public static RewriterDumpTable create(String dbname, String tabname) {
            return new RewriterDumpTable(dbname, tabname);
        }

        static RewriterDumpTable create(String tabname) {
            return new RewriterDumpTable(IDumpTable.DEFAULT_DATABASE_NAME, tabname);
        }

        @Override
        public String getDbName() {
            return this.dbname;
        }

        @Override
        public String getTableName() {
            return this.tabname;
        }

        @Override
        public String getFullName() {
            return this.dbname + "." + this.tabname;
        }

        @Override
        public int hashCode() {
            return getFullName().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return Objects.equals(getFullName(), ((RewriterDumpTable) o).getFullName());
        }

        @Override
        public String toString() {
            return getFullName();
        }
    }
}
