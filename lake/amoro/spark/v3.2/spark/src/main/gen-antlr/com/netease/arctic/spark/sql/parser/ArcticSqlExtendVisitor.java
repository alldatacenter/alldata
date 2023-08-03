package com.netease.arctic.spark.sql.parser;// Generated from com/netease/arctic/spark/sql/parser/ArcticSqlExtend.g4 by ANTLR 4.7.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ArcticSqlExtendParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ArcticSqlExtendVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#extendStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtendStatement(ArcticSqlExtendParser.ExtendStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTableWithPk}
	 * labeled alternative in {@link ArcticSqlExtendParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableWithPk(ArcticSqlExtendParser.CreateTableWithPkContext ctx);
	/**
	 * Visit a parse tree produced by the {@code explain}
	 * labeled alternative in {@link ArcticSqlExtendParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain(ArcticSqlExtendParser.ExplainContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#createTableHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableHeader(ArcticSqlExtendParser.CreateTableHeaderContext ctx);
	/**
	 * Visit a parse tree produced by the {@code colListWithPk}
	 * labeled alternative in {@link ArcticSqlExtendParser#colListAndPk}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColListWithPk(ArcticSqlExtendParser.ColListWithPkContext ctx);
	/**
	 * Visit a parse tree produced by the {@code colListOnlyPk}
	 * labeled alternative in {@link ArcticSqlExtendParser#colListAndPk}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColListOnlyPk(ArcticSqlExtendParser.ColListOnlyPkContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#primarySpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimarySpec(ArcticSqlExtendParser.PrimarySpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#bucketSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBucketSpec(ArcticSqlExtendParser.BucketSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#skewSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkewSpec(ArcticSqlExtendParser.SkewSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#locationSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocationSpec(ArcticSqlExtendParser.LocationSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#commentSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentSpec(ArcticSqlExtendParser.CommentSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(ArcticSqlExtendParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#ctes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCtes(ArcticSqlExtendParser.CtesContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#namedQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedQuery(ArcticSqlExtendParser.NamedQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#tableProvider}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableProvider(ArcticSqlExtendParser.TableProviderContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#createTableClauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableClauses(ArcticSqlExtendParser.CreateTableClausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#tablePropertyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyList(ArcticSqlExtendParser.TablePropertyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#tableProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableProperty(ArcticSqlExtendParser.TablePropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyKey(ArcticSqlExtendParser.TablePropertyKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyValue(ArcticSqlExtendParser.TablePropertyValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#constantList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantList(ArcticSqlExtendParser.ConstantListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#nestedConstantList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNestedConstantList(ArcticSqlExtendParser.NestedConstantListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#createFileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateFileFormat(ArcticSqlExtendParser.CreateFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticSqlExtendParser#fileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableFileFormat(ArcticSqlExtendParser.TableFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticSqlExtendParser#fileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericFileFormat(ArcticSqlExtendParser.GenericFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#storageHandler}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorageHandler(ArcticSqlExtendParser.StorageHandlerContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#queryOrganization}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryOrganization(ArcticSqlExtendParser.QueryOrganizationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryTermDefault(ArcticSqlExtendParser.QueryTermDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetOperation(ArcticSqlExtendParser.SetOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryPrimaryDefault(ArcticSqlExtendParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromStmt(ArcticSqlExtendParser.FromStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable(ArcticSqlExtendParser.TableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTableDefault1(ArcticSqlExtendParser.InlineTableDefault1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery(ArcticSqlExtendParser.SubqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#sortItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortItem(ArcticSqlExtendParser.SortItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#fromStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromStatement(ArcticSqlExtendParser.FromStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#fromStatementBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromStatementBody(ArcticSqlExtendParser.FromStatementBodyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link ArcticSqlExtendParser#querySpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransformQuerySpecification(ArcticSqlExtendParser.TransformQuerySpecificationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link ArcticSqlExtendParser#querySpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegularQuerySpecification(ArcticSqlExtendParser.RegularQuerySpecificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#transformClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransformClause(ArcticSqlExtendParser.TransformClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#selectClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectClause(ArcticSqlExtendParser.SelectClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#whereClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhereClause(ArcticSqlExtendParser.WhereClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#havingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHavingClause(ArcticSqlExtendParser.HavingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#hint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHint(ArcticSqlExtendParser.HintContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#hintStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHintStatement(ArcticSqlExtendParser.HintStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#fromClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromClause(ArcticSqlExtendParser.FromClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#aggregationClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregationClause(ArcticSqlExtendParser.AggregationClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#groupByClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupByClause(ArcticSqlExtendParser.GroupByClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingAnalytics(ArcticSqlExtendParser.GroupingAnalyticsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#groupingElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingElement(ArcticSqlExtendParser.GroupingElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#groupingSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingSet(ArcticSqlExtendParser.GroupingSetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#pivotClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivotClause(ArcticSqlExtendParser.PivotClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#pivotColumn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivotColumn(ArcticSqlExtendParser.PivotColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#pivotValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivotValue(ArcticSqlExtendParser.PivotValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#lateralView}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLateralView(ArcticSqlExtendParser.LateralViewContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#setQuantifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetQuantifier(ArcticSqlExtendParser.SetQuantifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#relation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelation(ArcticSqlExtendParser.RelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#joinRelation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinRelation(ArcticSqlExtendParser.JoinRelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#joinType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinType(ArcticSqlExtendParser.JoinTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#joinCriteria}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinCriteria(ArcticSqlExtendParser.JoinCriteriaContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#sample}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSample(ArcticSqlExtendParser.SampleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByPercentile(ArcticSqlExtendParser.SampleByPercentileContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByRows(ArcticSqlExtendParser.SampleByRowsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByBucket(ArcticSqlExtendParser.SampleByBucketContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByBytes(ArcticSqlExtendParser.SampleByBytesContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#identifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierList(ArcticSqlExtendParser.IdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#identifierSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierSeq(ArcticSqlExtendParser.IdentifierSeqContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderedIdentifierList(ArcticSqlExtendParser.OrderedIdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderedIdentifier(ArcticSqlExtendParser.OrderedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableName(ArcticSqlExtendParser.TableNameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedQuery(ArcticSqlExtendParser.AliasedQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedRelation(ArcticSqlExtendParser.AliasedRelationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTableDefault2(ArcticSqlExtendParser.InlineTableDefault2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableValuedFunction(ArcticSqlExtendParser.TableValuedFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#inlineTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTable(ArcticSqlExtendParser.InlineTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#functionTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTable(ArcticSqlExtendParser.FunctionTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#tableAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableAlias(ArcticSqlExtendParser.TableAliasContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticSqlExtendParser#rowFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowFormatSerde(ArcticSqlExtendParser.RowFormatSerdeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticSqlExtendParser#rowFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowFormatDelimited(ArcticSqlExtendParser.RowFormatDelimitedContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipartIdentifier(ArcticSqlExtendParser.MultipartIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#namedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedExpression(ArcticSqlExtendParser.NamedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedExpressionSeq(ArcticSqlExtendParser.NamedExpressionSeqContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#partitionFieldList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionFieldList(ArcticSqlExtendParser.PartitionFieldListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link ArcticSqlExtendParser#partitionField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionTransform(ArcticSqlExtendParser.PartitionTransformContext ctx);
	/**
	 * Visit a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link ArcticSqlExtendParser#partitionField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionColumn(ArcticSqlExtendParser.PartitionColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link ArcticSqlExtendParser#transform}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentityTransform(ArcticSqlExtendParser.IdentityTransformContext ctx);
	/**
	 * Visit a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link ArcticSqlExtendParser#transform}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApplyTransform(ArcticSqlExtendParser.ApplyTransformContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#transformArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransformArgument(ArcticSqlExtendParser.TransformArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(ArcticSqlExtendParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#expressionSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionSeq(ArcticSqlExtendParser.ExpressionSeqContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalNot(ArcticSqlExtendParser.LogicalNotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicated(ArcticSqlExtendParser.PredicatedContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExists(ArcticSqlExtendParser.ExistsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalBinary(ArcticSqlExtendParser.LogicalBinaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicate(ArcticSqlExtendParser.PredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueExpressionDefault(ArcticSqlExtendParser.ValueExpressionDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison(ArcticSqlExtendParser.ComparisonContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticBinary(ArcticSqlExtendParser.ArithmeticBinaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticUnary(ArcticSqlExtendParser.ArithmeticUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct(ArcticSqlExtendParser.StructContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDereference(ArcticSqlExtendParser.DereferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleCase(ArcticSqlExtendParser.SimpleCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCurrentLike(ArcticSqlExtendParser.CurrentLikeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnReference(ArcticSqlExtendParser.ColumnReferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowConstructor(ArcticSqlExtendParser.RowConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLast(ArcticSqlExtendParser.LastContext ctx);
	/**
	 * Visit a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar(ArcticSqlExtendParser.StarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverlay(ArcticSqlExtendParser.OverlayContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscript(ArcticSqlExtendParser.SubscriptContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubqueryExpression(ArcticSqlExtendParser.SubqueryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code substring}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubstring(ArcticSqlExtendParser.SubstringContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast(ArcticSqlExtendParser.CastContext ctx);
	/**
	 * Visit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDefault(ArcticSqlExtendParser.ConstantDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda(ArcticSqlExtendParser.LambdaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpression(ArcticSqlExtendParser.ParenthesizedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtract(ArcticSqlExtendParser.ExtractContext ctx);
	/**
	 * Visit a parse tree produced by the {@code trim}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrim(ArcticSqlExtendParser.TrimContext ctx);
	/**
	 * Visit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(ArcticSqlExtendParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearchedCase(ArcticSqlExtendParser.SearchedCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPosition(ArcticSqlExtendParser.PositionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFirst(ArcticSqlExtendParser.FirstContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullLiteral(ArcticSqlExtendParser.NullLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalLiteral(ArcticSqlExtendParser.IntervalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeConstructor(ArcticSqlExtendParser.TypeConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(ArcticSqlExtendParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(ArcticSqlExtendParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(ArcticSqlExtendParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOperator(ArcticSqlExtendParser.ComparisonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#booleanValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanValue(ArcticSqlExtendParser.BooleanValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#interval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval(ArcticSqlExtendParser.IntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorCapturingMultiUnitsInterval(ArcticSqlExtendParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiUnitsInterval(ArcticSqlExtendParser.MultiUnitsIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorCapturingUnitToUnitInterval(ArcticSqlExtendParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitToUnitInterval(ArcticSqlExtendParser.UnitToUnitIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#intervalValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalValue(ArcticSqlExtendParser.IntervalValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#colPosition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColPosition(ArcticSqlExtendParser.ColPositionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexDataType(ArcticSqlExtendParser.ComplexDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYearMonthIntervalDataType(ArcticSqlExtendParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDayTimeIntervalDataType(ArcticSqlExtendParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveDataType(ArcticSqlExtendParser.PrimitiveDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#colTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColTypeList(ArcticSqlExtendParser.ColTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#colType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColType(ArcticSqlExtendParser.ColTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#complexColTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexColTypeList(ArcticSqlExtendParser.ComplexColTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#complexColType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexColType(ArcticSqlExtendParser.ComplexColTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#whenClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenClause(ArcticSqlExtendParser.WhenClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#windowClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowClause(ArcticSqlExtendParser.WindowClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#namedWindow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedWindow(ArcticSqlExtendParser.NamedWindowContext ctx);
	/**
	 * Visit a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticSqlExtendParser#windowSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowRef(ArcticSqlExtendParser.WindowRefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticSqlExtendParser#windowSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowDef(ArcticSqlExtendParser.WindowDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#windowFrame}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowFrame(ArcticSqlExtendParser.WindowFrameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#frameBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrameBound(ArcticSqlExtendParser.FrameBoundContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#functionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionName(ArcticSqlExtendParser.FunctionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(ArcticSqlExtendParser.QualifiedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorCapturingIdentifier(ArcticSqlExtendParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticSqlExtendParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorIdent(ArcticSqlExtendParser.ErrorIdentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticSqlExtendParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRealIdent(ArcticSqlExtendParser.RealIdentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(ArcticSqlExtendParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSqlExtendParser#strictIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnquotedIdentifier(ArcticSqlExtendParser.UnquotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSqlExtendParser#strictIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifierAlternative(ArcticSqlExtendParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifier(ArcticSqlExtendParser.QuotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExponentLiteral(ArcticSqlExtendParser.ExponentLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecimalLiteral(ArcticSqlExtendParser.DecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLegacyDecimalLiteral(ArcticSqlExtendParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerLiteral(ArcticSqlExtendParser.IntegerLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigIntLiteral(ArcticSqlExtendParser.BigIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSmallIntLiteral(ArcticSqlExtendParser.SmallIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTinyIntLiteral(ArcticSqlExtendParser.TinyIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleLiteral(ArcticSqlExtendParser.DoubleLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatLiteral(ArcticSqlExtendParser.FloatLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigDecimalLiteral(ArcticSqlExtendParser.BigDecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnsiNonReserved(ArcticSqlExtendParser.AnsiNonReservedContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#strictNonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStrictNonReserved(ArcticSqlExtendParser.StrictNonReservedContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlExtendParser#nonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonReserved(ArcticSqlExtendParser.NonReservedContext ctx);
}
