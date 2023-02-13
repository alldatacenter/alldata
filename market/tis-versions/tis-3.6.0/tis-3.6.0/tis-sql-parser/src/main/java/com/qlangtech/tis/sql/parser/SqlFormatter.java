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
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.sql.parser.SqlRewriter.AliasTable;
import com.qlangtech.tis.sql.parser.SqlStringBuilder.RewriteProcessContext;
import com.qlangtech.tis.sql.parser.er.TabFieldProcessor;
import com.qlangtech.tis.sql.parser.exception.TisSqlFormatException;
import com.qlangtech.tis.sql.parser.meta.ColumnTransfer;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.facebook.presto.sql.TisExpressionFormatter.*;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.qlangtech.tis.sql.parser.ExpressionFormatter.formatExpression;
import static java.util.stream.Collectors.joining;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public final class SqlFormatter {

    private static final String INDENT = "   ";

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z_][a-z0-9_]*");

    private SqlFormatter() {
    }

    public static String formatSql(Node root, Optional<List<Expression>> parameters) {
        SqlStringBuilder builder = new SqlStringBuilder();
        new Formatter(builder, Collections.emptyMap(), parameters).process(root, 0);
        return builder.toString();
    }

    /**
     * 通过解析From，得到alias->EntityName的映射关系
     */
    public static class QueryFromTableFinder extends com.facebook.presto.sql.tree.AstVisitor<Void, Void> {

        private final Map<String, EntityName> /**
         * alias
         */
                aliasTableMap = Maps.newHashMap();

        public Map<String, /**
         * alias
         */
                EntityName> getAliasTableMap() {
            return aliasTableMap;
        }

        @Override
        protected Void visitJoin(Join node, Void context) {
            this.process(node.getLeft(), context);
            this.process(node.getRight(), context);
            return null;
            // return super.visitJoin(node, context);
        }

        @Override
        protected Void visitTable(Table node, Void context) {
            // return super.visitTable(node, context);
            EntityName en = EntityName.parse(node.getName().toString());
            aliasTableMap.put(en.getTabName(), en);
            return null;
        }

        @Override
        protected Void visitAliasedRelation(AliasedRelation node, Void context) {
            if (node.getRelation() instanceof Table) {
                Table table = (Table) node.getRelation();
                aliasTableMap.put(node.getAlias().getValue(), EntityName.parse(table.getName().toString()));
                return null;
            } else if (node.getRelation() instanceof TableSubquery) {
                aliasTableMap.put(node.getAlias().getValue(), EntityName.createSubQueryTable());
                return null;
            } else {
                return super.visitAliasedRelation(node, context);
            }
        }
    }

    public static class Formatter extends com.facebook.presto.sql.tree.AstVisitor<Void, Integer> {

        protected final SqlStringBuilder builder;

        protected final Optional<List<Expression>> parameters;

        protected final Map<EntityName, TabFieldProcessor> dumpNodeExtraMetaMap;

        public Formatter(SqlStringBuilder builder, final Map<EntityName, TabFieldProcessor> dumpNodeExtraMetaMap, Optional<List<Expression>> parameters) {
            this.builder = builder;
            this.parameters = parameters;
            Objects.requireNonNull(dumpNodeExtraMetaMap,"dumpNodeExtraMetaMap can not be null");
            this.dumpNodeExtraMetaMap = dumpNodeExtraMetaMap;
        }

        protected List<AliasTable> getWaitProcessAliasTabsSet() {
            return Collections.emptyList();
        }

        // private final boolean hasAnyUnprocessedSelectPtAppend() {
        // return this.getWaitProcessAliasTabsSet().stream().filter((r) ->
        // !r.isSelectPtAppendProcess()).count() > 0;
        // }
        protected boolean hasAnyUnprocessedAliasTabsSet() {
            return this.getWaitProcessAliasTabsSet().stream().filter((r) -> !r.isPtRewriterOver()).count() > 0;
        }

        private Optional<AliasTable> getFirstUnprocessedAliasTab() {
            Optional<AliasTable> unprocessTabAlias = this.getWaitProcessAliasTabsSet().stream().filter((r) -> {
                boolean result = !r.isSelectPtAppendProcess() && !r.isSubQueryTable();
                if (result) {
                    r.makeSelectPtAppendProcess();
                }
                return result;
            }).findFirst();
            if (unprocessTabAlias.isPresent()) {
                return unprocessTabAlias;
            }
            unprocessTabAlias = this.getWaitProcessAliasTabsSet().stream().filter((r) -> {
                boolean result = !r.isSelectPtAppendProcess() && r.isSubQueryTable();
                if (result) {
                    r.makeSelectPtAppendProcess();
                }
                return result;
            }).findFirst();
            // }
            return unprocessTabAlias;
        }

        @Override
        protected Void visitNode(Node node, Integer indent) {
            throw new UnsupportedOperationException("not yet implemented: " + node);
        }

        @Override
        protected Void visitExpression(Expression node, Integer indent) {
            if (indent != MAGIC_TOKEN_JOINON_PROCESS) {
                checkArgument(indent == 0, "visitExpression should only be called at root");
            }
            builder.append(formatExpression(node, parameters));
            return null;
        }

        @Override
        protected Void visitComparisonExpression(ComparisonExpression node, Integer context) {
            if (SqlStringBuilder.isInRewriteProcess()) {
                this.process(node.getLeft(), MAGIC_TOKEN_JOINON_PROCESS);
                this.process(node.getRight(), MAGIC_TOKEN_JOINON_PROCESS);
            }
            return super.visitComparisonExpression(node, context);
        }

        @Override
        protected Void visitDereferenceExpression(DereferenceExpression node, Integer context) {
            if (SqlStringBuilder.isInRewriteProcess()) {
                RewriteProcessContext pcontext = SqlStringBuilder.getProcessContext();
                pcontext.tabAliasStack.push(String.valueOf(node.getBase()));
            }
            // this.getWaitProcessAliasTabsSet()
            return super.visitDereferenceExpression(node, context);
        }

        // @Override
        // protected Void visitUnnest(Unnest node, Integer indent) {
        // builder.append("UNNEST(").append(node.getExpressions().stream()
        // .map(expression -> formatExpression(expression,
        // parameters)).collect(joining(", "))).append(")");
        // if (node.isWithOrdinality()) {
        // builder.append(" WITH ORDINALITY");
        // }
        // return null;
        // }
        // @Override
        // protected Void visitLateral(Lateral node, Integer indent) {
        // append(indent, "LATERAL (");
        // process(node.getQuery(), indent + 1);
        // append(indent, ")");
        // return null;
        // }
        // @Override
        // protected Void visitPrepare(Prepare node, Integer indent) {
        // append(indent, "PREPARE ");
        // builder.append(node.getName());
        // builder.append(" FROM");
        // builder.append("\n");
        // process(node.getStatement(), indent + 1);
        // return null;
        // }
        // @Override
        // protected Void visitDeallocate(Deallocate node, Integer indent) {
        // append(indent, "DEALLOCATE PREPARE ");
        // builder.append(node.getName());
        // return null;
        // }
        // @Override
        // protected Void visitExecute(Execute node, Integer indent) {
        // append(indent, "EXECUTE ");
        // builder.append(node.getName());
        // List<Expression> parameters = node.getParameters();
        // if (!parameters.isEmpty()) {
        // builder.append(" USING ");
        // Joiner.on(", ").appendTo(builder, parameters);
        // }
        // return null;
        // }
        // @Override
        // protected Void visitDescribeOutput(DescribeOutput node, Integer indent) {
        // append(indent, "DESCRIBE OUTPUT ");
        // builder.append(node.getName());
        // return null;
        // }
        // @Override
        // protected Void visitDescribeInput(DescribeInput node, Integer indent) {
        // append(indent, "DESCRIBE INPUT ");
        // builder.append(node.getName());
        // return null;
        // }
        @Override
        protected Void visitQuery(Query node, Integer indent) {
            if (node.getWith().isPresent()) {
                throw new UnsupportedOperationException(String.valueOf(node.getWith()));
                // With with = node.getWith().get();
                // append(indent, "WITH");
                // if (with.isRecursive()) {
                // builder.append(" RECURSIVE");
                // }
                // builder.append("\n ");
                // Iterator<WithQuery> queries = with.getQueries().iterator();
                // while (queries.hasNext()) {
                // WithQuery query = queries.next();
                // append(indent, formatExpression(query.getName(), parameters));
                // query.getColumnNames().ifPresent(columnNames -> appendAliasColumns(builder,
                // columnNames));
                // builder.append(" AS ");
                // process(new TableSubquery(query.getQuery()), indent);
                // builder.append('\n');
                // if (queries.hasNext()) {
                // builder.append(", ");
                // }
                // }
            }
            processRelation(node.getQueryBody(), indent);
            if (node.getOrderBy().isPresent()) {
                process(node.getOrderBy().get(), indent);
            }
            if (node.getLimit().isPresent()) {
                append(indent, "LIMIT " + node.getLimit().get()).append('\n');
            }
            return null;
        }

        /**
         * 设置参与join process的主表
         *
         * @param primaryTable
         */
        protected void setPrimayTable(AliasTable primaryTable) {
        }

        @Override
        protected Void visitQuerySpecification(QuerySpecification node, Integer indent) {
            Optional<Relation> from = node.getFrom();
            if (!from.isPresent()) {
                throw new IllegalStateException("querySpecification from mush present");
            }
            QueryFromTableFinder fromTableFinder = new QueryFromTableFinder();
            fromTableFinder.process(from.get());
            // fromTableFinder.getAliasTableMap();
            // this.process(node.getSelect(), indent);
            new TableColumnTransferRewritFormatter(this.builder, fromTableFinder, this.dumpNodeExtraMetaMap).process(node.getSelect(), indent);
            // Optional<AliasTable> firstTableCriteria = Optional.empty();
            final AliasTable[] firstTableCriteria = new AliasTable[1];
            Callable<String> appendPtPmod = () -> {
                if (firstTableCriteria[0] == null) {
                    throw new IllegalStateException("can not find firstTableCriteria " + node);
                }
                AliasTable a = firstTableCriteria[0];
                return createPtPmodCols(a) + "\n";
            };
            append(indent, ", ");
            this.builder.append(appendPtPmod);
            if (node.getFrom().isPresent()) {
                append(indent, "FROM");
                builder.append('\n');
                append(indent, "  ");
                process(node.getFrom().get(), indent);
            }
            Optional<AliasTable> optAlias = getFirstUnprocessedAliasTab();
            if (!optAlias.isPresent()) {
                throw new IllegalStateException("can not find firstTableCriteria " + node.getSelect());
            }
            firstTableCriteria[0] = optAlias.get();
            this.setPrimayTable(firstTableCriteria[0]);
            builder.append('\n');
            // ▼▼ baisui modify
            boolean hasWherePresent = false;
            if (node.getWhere().isPresent()) {
                append(indent, "WHERE " + formatExpression(node.getWhere().get(), parameters));
                hasWherePresent = true;
            }
            boolean hasPtRewritePresent = false;
            if (this.hasAnyUnprocessedAliasTabsSet()) {
                if (!hasWherePresent) {
                    append(indent, "WHERE ");
                }
                // import
                this.processAppendPtWhere(node.getWhere());
                hasPtRewritePresent = true;
            }
            if (hasWherePresent || hasPtRewritePresent) {
                append(indent, "\n");
            }
            // ▲▲ end 20190829
            if (node.getGroupBy().isPresent()) {
                append(indent, "GROUP BY " + (node.getGroupBy().get().isDistinct() ? " DISTINCT " : "") + formatGroupBy(node.getGroupBy().get().getGroupingElements()));
                //
                // .append('\n');
                builder.append(',').append(appendPtPmod);
            }
            if (node.getHaving().isPresent()) {
                append(indent, "HAVING " + formatExpression(node.getHaving().get(), parameters)).append('\n');
            }
            if (node.getOrderBy().isPresent()) {
                process(node.getOrderBy().get(), indent);
            }
            if (node.getLimit().isPresent()) {
                append(indent, "LIMIT " + node.getLimit().get()).append('\n');
            }
            return null;
        }

        protected String createPtPmodCols(AliasTable a) {
            return a.getAlias() + "." + IDumpTable.PARTITION_PT + "," + a.getAlias() + "." + IDumpTable.PARTITION_PMOD;
        }

        protected void processAppendPtWhere(Optional<Expression> where) {
        }

        @Override
        protected Void visitOrderBy(OrderBy node, Integer indent) {
            append(indent, formatOrderBy(node, parameters)).append('\n');
            return null;
        }

        @Override
        protected Void visitSelect(Select node, Integer indent) {
            // System.out.println("visit select");
            append(indent, "SELECT");
            if (node.isDistinct()) {
                builder.append(" DISTINCT");
            }
            if (node.getSelectItems().size() > 1) {
                boolean first = true;
                for (SelectItem item : node.getSelectItems()) {
                    builder.append("\n").append(indentString(indent)).append(first ? "  " : ", ");
                    process(item, indent);
                    first = false;
                }
            } else {
                builder.append(' ');
                process(getOnlyElement(node.getSelectItems()), indent);
            }
            builder.append('\n');
            return null;
        }

        @Override
        protected Void visitSingleColumn(SingleColumn node, Integer indent) {
            builder.append(formatExpression(node.getExpression(), parameters));
            if (node.getAlias().isPresent()) {
                builder.append(' ').append(formatExpression(node.getAlias().get(), parameters));
            }
            return null;
        }

        @Override
        protected Void visitAllColumns(AllColumns node, Integer context) {
            builder.append(node.toString());
            return null;
        }

        @Override
        protected Void visitTable(Table node, Integer indent) {
            builder.append(formatName(node.getName()));
            return null;
        }

        @Override
        protected Void visitJoin(Join node, Integer indent) {
            JoinCriteria criteria = node.getCriteria().orElse(null);
            String type = node.getType().toString();
            if (criteria instanceof NaturalJoin) {
                type = "NATURAL " + type;
            }
            // ▼▼▼ 奇怪源码中为什么要在这里将FROM 部分整个加一个括号,这样就导致在SELECT部分引用不到列的alias的reference了
            // if (node.getType() != Join.Type.IMPLICIT) {
            // builder.append('(');
            // }
            // ▲▲▲
            process(node.getLeft(), indent);
            builder.append('\n');
            if (node.getType() == Join.Type.IMPLICIT) {
                append(indent, ", ");
            } else {
                append(indent, type).append(" JOIN ");
            }
            process(node.getRight(), indent);
            if (node.getType() != Join.Type.CROSS && node.getType() != Join.Type.IMPLICIT) {
                if (criteria instanceof JoinUsing) {
                    JoinUsing using = (JoinUsing) criteria;
                    builder.append(" USING (").append(Joiner.on(", ").join(using.getColumns())).append(")");
                } else if (criteria instanceof JoinOn) {
                    JoinOn on = (JoinOn) criteria;
                    builder.append(" ON (").append(formatExpression(on.getExpression(), parameters));
                    this.processJoinOn(on);
                    builder.append(")");
                } else if (!(criteria instanceof NaturalJoin)) {
                    throw new UnsupportedOperationException("unknown join criteria: " + criteria);
                }
            }
            // ▲▲▲
            return null;
        }

        public static final int MAGIC_TOKEN_JOINON_PROCESS = 9527;

        // baisui add
        protected void processJoinOn(JoinOn on) {
        }

        @Override
        protected Void visitAliasedRelation(AliasedRelation node, Integer indent) {
            process(node.getRelation(), indent);
            builder.append(' ').append(formatExpression(node.getAlias(), parameters));
            appendAliasColumns(builder, node.getColumnNames());
            return null;
        }

        @Override
        protected Void visitSampledRelation(SampledRelation node, Integer indent) {
            process(node.getRelation(), indent);
            builder.append(" TABLESAMPLE ").append(node.getType()).append(" (").append(node.getSamplePercentage()).append(')');
            return null;
        }

        @Override
        protected Void visitValues(Values node, Integer indent) {
            builder.append(" VALUES ");
            boolean first = true;
            for (Expression row : node.getRows()) {
                builder.append("\n").append(indentString(indent)).append(first ? "  " : ", ");
                builder.append(formatExpression(row, parameters));
                first = false;
            }
            builder.append('\n');
            return null;
        }

        @Override
        protected Void visitTableSubquery(TableSubquery node, Integer indent) {
            builder.append('(').append('\n');
            process(node.getQuery(), indent + 1);
            append(indent, ") ");
            return null;
        }

        @Override
        protected Void visitUnion(Union node, Integer indent) {
            Iterator<Relation> relations = node.getRelations().iterator();
            while (relations.hasNext()) {
                processRelation(relations.next(), indent);
                if (relations.hasNext()) {
                    builder.append("UNION ");
                    if (!node.isDistinct()) {
                        builder.append("ALL ");
                    }
                }
            }
            return null;
        }

        @Override
        protected Void visitExcept(Except node, Integer indent) {
            processRelation(node.getLeft(), indent);
            builder.append("EXCEPT ");
            if (!node.isDistinct()) {
                builder.append("ALL ");
            }
            processRelation(node.getRight(), indent);
            return null;
        }

        @Override
        protected Void visitIntersect(Intersect node, Integer indent) {
            Iterator<Relation> relations = node.getRelations().iterator();
            while (relations.hasNext()) {
                processRelation(relations.next(), indent);
                if (relations.hasNext()) {
                    builder.append("INTERSECT ");
                    if (!node.isDistinct()) {
                        builder.append("ALL ");
                    }
                }
            }
            return null;
        }

        @Override
        protected Void visitCreateView(CreateView node, Integer indent) {
            builder.append("CREATE ");
            if (node.isReplace()) {
                builder.append("OR REPLACE ");
            }
            builder.append("VIEW ").append(formatName(node.getName())).append(" AS\n");
            process(node.getQuery(), indent);
            return null;
        }

        @Override
        protected Void visitDropView(DropView node, Integer context) {
            builder.append("DROP VIEW ");
            if (node.isExists()) {
                builder.append("IF EXISTS ");
            }
            builder.append(node.getName());
            return null;
        }

        @Override
        protected Void visitExplain(Explain node, Integer indent) {
            builder.append("EXPLAIN ");
            if (node.isAnalyze()) {
                builder.append("ANALYZE ");
            }
            List<String> options = new ArrayList<>();
            for (ExplainOption option : node.getOptions()) {
                if (option instanceof ExplainType) {
                    options.add("TYPE " + ((ExplainType) option).getType());
                } else if (option instanceof ExplainFormat) {
                    options.add("FORMAT " + ((ExplainFormat) option).getType());
                } else {
                    throw new UnsupportedOperationException("unhandled explain option: " + option);
                }
            }
            try {
                if (!options.isEmpty()) {
                    builder.append("(");
                    Joiner.on(", ").appendTo(builder, options);
                    builder.append(")");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            builder.append("\n");
            process(node.getStatement(), indent);
            return null;
        }

        @Override
        protected Void visitShowCatalogs(ShowCatalogs node, Integer context) {
            builder.append("SHOW CATALOGS");
            node.getLikePattern().ifPresent((value) -> builder.append(" LIKE ").append(formatStringLiteral(value)));
            return null;
        }

        // @Override
        // protected Void visitShowSchemas(ShowSchemas node, Integer context) {
        // builder.append("SHOW SCHEMAS");
        //
        // if (node.getCatalog().isPresent()) {
        // builder.append(" FROM ").append(node.getCatalog().get());
        // }
        //
        // node.getLikePattern().ifPresent((value) -> builder.append(" LIKE
        // ").append(formatStringLiteral(value)));
        //
        // node.getEscape().ifPresent((value) -> builder.append(" ESCAPE
        // ").append(formatStringLiteral(value)));
        //
        // return null;
        // }
        // @Override
        // protected Void visitShowTables(ShowTables node, Integer context) {
        // builder.append("SHOW TABLES");
        //
        // node.getSchema().ifPresent(value -> builder.append(" FROM
        // ").append(formatName(value)));
        //
        // node.getLikePattern().ifPresent(value -> builder.append(" LIKE
        // ").append(formatStringLiteral(value)));
        //
        // node.getEscape().ifPresent(value -> builder.append(" ESCAPE
        // ").append(formatStringLiteral(value)));
        //
        // return null;
        // }
        // @Override
        // protected Void visitShowCreate(ShowCreate node, Integer context) {
        // if (node.getType() == ShowCreate.Type.TABLE) {
        // builder.append("SHOW CREATE TABLE ").append(formatName(node.getName()));
        // } else if (node.getType() == ShowCreate.Type.VIEW) {
        // builder.append("SHOW CREATE VIEW ").append(formatName(node.getName()));
        // }
        //
        // return null;
        // }
        @Override
        protected Void visitShowColumns(ShowColumns node, Integer context) {
            builder.append("SHOW COLUMNS FROM ").append(formatName(node.getTable()));
            return null;
        }

        private String formatProperties(List<Property> properties) {
            if (properties.isEmpty()) {
                return "";
            }
            String propertyList = properties.stream().map(element -> INDENT + formatExpression(element.getName(), parameters) + " = " + formatExpression(element.getValue(), parameters)).collect(joining(",\n"));
            return "\nWITH (\n" + propertyList + "\n)";
        }

        private static String formatName(String name) {
            if (NAME_PATTERN.matcher(name).matches()) {
                return name;
            }
            return "\"" + name.replace("\"", "\"\"") + "\"";
        }

        private static String formatName(QualifiedName name) {
            return name.getOriginalParts().stream().map(Formatter::formatName).collect(joining("."));
        }

        @Override
        protected Void visitDropTable(DropTable node, Integer context) {
            builder.append("DROP TABLE ");
            if (node.isExists()) {
                builder.append("IF EXISTS ");
            }
            builder.append(node.getTableName());
            return null;
        }

        @Override
        protected Void visitRow(Row node, Integer indent) {
            builder.append("ROW(");
            boolean firstItem = true;
            for (Expression item : node.getItems()) {
                if (!firstItem) {
                    builder.append(", ");
                }
                process(item, indent);
                firstItem = false;
            }
            builder.append(")");
            return null;
        }

        @Override
        public Void visitSetPath(SetPath node, Integer indent) {
            builder.append("SET PATH ");
            builder.append(Joiner.on(", ").join(node.getPathSpecification().getPath()));
            return null;
        }

        private void processRelation(Relation relation, Integer indent) {
            // TODO: handle this properly
            if (relation instanceof Table) {
                builder.append("TABLE ").append(((Table) relation).getName()).append('\n');
            } else {
                process(relation, indent);
            }
        }

        private SqlStringBuilder append(int indent, String value) {
            return builder.append(indentString(indent)).append(value);
        }

        private SqlStringBuilder append(int indent, Object value) {
            return builder.append(indentString(indent)).append(value);
        }

        private static String indentString(int indent) {
            return Strings.repeat(INDENT, indent);
        }
    }

    /**
     * 依据ERRule中的transfer规则rewrite col
     */
    public static class TableColumnTransferRewritFormatter extends Formatter {

        private final Map<String, EntityName> /*alias*/
                aliasTableMap;

        public TableColumnTransferRewritFormatter(SqlStringBuilder builder, QueryFromTableFinder queryFromTableFinder
                , Map<EntityName, TabFieldProcessor> tableExtraMetaMap) {
            super(builder, tableExtraMetaMap, Optional.empty());
            this.aliasTableMap = queryFromTableFinder.getAliasTableMap();
        }

        @Override
        protected Void visitSingleColumn(SingleColumn node, Integer indent) {
            this.process(node.getExpression(), MAGIC_TOKEN_JOINON_PROCESS);
            // builder.append(formatExpression(node.getExpression(), parameters));
            if (node.getAlias().isPresent()) {
                builder.append(' ').append(formatExpression(node.getAlias().get(), parameters));
            }
            return null;
        }

        @Override
        protected Void visitDereferenceExpression(DereferenceExpression node, Integer context) {
            if (!(node.getBase() instanceof Identifier)) {
                return super.visitDereferenceExpression(node, context);
            }
            Identifier base = (Identifier) node.getBase();
            Identifier field = node.getField();
            EntityName e = aliasTableMap.get(base.getValue());
            if (e == null) {
                throw new TisSqlFormatException("base ref:" + base.getValue()
                        + " can not find relevant table entity in map,mapSize:"
                        + aliasTableMap.size() + ",exist:"
                        + aliasTableMap.entrySet().stream().map((entry) -> "[" + entry.getKey() + ":" + entry.getValue() + "]")
                        .collect(Collectors.joining(",")), node.getLocation());
            }
            // 找到别名依赖的
            Optional<Map.Entry<EntityName, TabFieldProcessor>> find = Optional.empty();
            if (!(e instanceof EntityName.SubTableQuery)) {
                find = dumpNodeExtraMetaMap.entrySet().stream().filter((entry) -> {
                    if (e.useDftDbName()) {
                        return StringUtils.equals(entry.getKey().getTabName(), e.getTabName());
                    } else {
                        return entry.getKey().equals(e);
                    }
                }).findFirst();
            }
            if (find.isPresent()) {
                // 找到对应的表
                Map.Entry<EntityName, TabFieldProcessor> tabExtraMeta = find.get();
                TabFieldProcessor extraMeta = tabExtraMeta.getValue();
                Map<String, ColumnTransfer> /**
                 * colKey
                 */
                        columnTransferMap = extraMeta.colTransfersMap();
                ColumnTransfer cTransfer = columnTransferMap.get(field.getValue());
                if (cTransfer != null) {
                    this.builder.append(SqlTaskNodeMeta.HiveColTransfer.instance.transfer(base.getValue(), field.getValue(), cTransfer));
                    // System.out.println("hello:" + cTransfer.getColKey() + "," + cTransfer.getTransfer() + "," + cTransfer.getParam());
                    return null;
                }
            }
            return super.visitDereferenceExpression(node, context);
            // return super.visitDereferenceExpression(node, context);
        }
        // @Override
        // protected Void visitSingleColumn(SingleColumn node, Integer indent) {
        //
        //
        // if (node.getAlias().isPresent()) {
        // Identifier alias = node.getAlias().get();
        // EntityName e = aliasTableMap.get(alias.getValue());
        // // 找到别名依赖的
        // Optional<Map.Entry<EntityName, SqlTaskNodeMeta.TabExtraMeta>> find
        // = dumpNodeDependency.entrySet().stream().filter((entry) -> {
        // if (e.useDftDbName()) {
        // return StringUtils.equals(entry.getKey().getTabName(), e.getTabName());
        // } else {
        // return entry.getKey().equals(e);
        // }
        // }).findFirst();
        //
        // if (find.isPresent()) {
        // Map.Entry<EntityName, SqlTaskNodeMeta.TabExtraMeta> tabExtraMeta = find.get();
        // SqlTaskNodeMeta.TabExtraMeta extraMeta = tabExtraMeta.getValue();
        //
        // Map<String /**colKey*/, SqlTaskNodeMeta.ColumnTransfer> columnTransferMap = extraMeta.fetchColTransfersMap();
        //
        //
        // }
        // }
        //
        // builder.append(formatExpression(node.getExpression(), parameters));
        // if (node.getAlias().isPresent()) {
        // Identifier alias = node.getAlias().get();
        // builder.append(' ').append(formatExpression(alias, parameters));
        // }
        //
        // return null;
        // //return super.visitSingleColumn(node, indent);
        // }
    }

    protected static void appendAliasColumns(SqlStringBuilder builder, List<Identifier> columns) {
        if ((columns != null) && (!columns.isEmpty())) {
            String formattedColumns = columns.stream().map(name -> formatExpression(name, Optional.empty())).collect(Collectors.joining(", "));
            builder.append(" (").append(formattedColumns).append(')');
        }
    }
}
