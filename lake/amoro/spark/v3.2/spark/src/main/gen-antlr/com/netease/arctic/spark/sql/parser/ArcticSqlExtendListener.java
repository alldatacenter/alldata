package com.netease.arctic.spark.sql.parser;// Generated from com/netease/arctic/spark/sql/parser/ArcticSqlExtend.g4 by ANTLR 4.7.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ArcticSqlExtendParser}.
 */
public interface ArcticSqlExtendListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#extendStatement}.
	 * @param ctx the parse tree
	 */
	void enterExtendStatement(ArcticSqlExtendParser.ExtendStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#extendStatement}.
	 * @param ctx the parse tree
	 */
	void exitExtendStatement(ArcticSqlExtendParser.ExtendStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTableWithPk}
	 * labeled alternative in {@link ArcticSqlExtendParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableWithPk(ArcticSqlExtendParser.CreateTableWithPkContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTableWithPk}
	 * labeled alternative in {@link ArcticSqlExtendParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableWithPk(ArcticSqlExtendParser.CreateTableWithPkContext ctx);
	/**
	 * Enter a parse tree produced by the {@code explain}
	 * labeled alternative in {@link ArcticSqlExtendParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterExplain(ArcticSqlExtendParser.ExplainContext ctx);
	/**
	 * Exit a parse tree produced by the {@code explain}
	 * labeled alternative in {@link ArcticSqlExtendParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitExplain(ArcticSqlExtendParser.ExplainContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#createTableHeader}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableHeader(ArcticSqlExtendParser.CreateTableHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#createTableHeader}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableHeader(ArcticSqlExtendParser.CreateTableHeaderContext ctx);
	/**
	 * Enter a parse tree produced by the {@code colListWithPk}
	 * labeled alternative in {@link ArcticSqlExtendParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void enterColListWithPk(ArcticSqlExtendParser.ColListWithPkContext ctx);
	/**
	 * Exit a parse tree produced by the {@code colListWithPk}
	 * labeled alternative in {@link ArcticSqlExtendParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void exitColListWithPk(ArcticSqlExtendParser.ColListWithPkContext ctx);
	/**
	 * Enter a parse tree produced by the {@code colListOnlyPk}
	 * labeled alternative in {@link ArcticSqlExtendParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void enterColListOnlyPk(ArcticSqlExtendParser.ColListOnlyPkContext ctx);
	/**
	 * Exit a parse tree produced by the {@code colListOnlyPk}
	 * labeled alternative in {@link ArcticSqlExtendParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void exitColListOnlyPk(ArcticSqlExtendParser.ColListOnlyPkContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#primarySpec}.
	 * @param ctx the parse tree
	 */
	void enterPrimarySpec(ArcticSqlExtendParser.PrimarySpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#primarySpec}.
	 * @param ctx the parse tree
	 */
	void exitPrimarySpec(ArcticSqlExtendParser.PrimarySpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#bucketSpec}.
	 * @param ctx the parse tree
	 */
	void enterBucketSpec(ArcticSqlExtendParser.BucketSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#bucketSpec}.
	 * @param ctx the parse tree
	 */
	void exitBucketSpec(ArcticSqlExtendParser.BucketSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#skewSpec}.
	 * @param ctx the parse tree
	 */
	void enterSkewSpec(ArcticSqlExtendParser.SkewSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#skewSpec}.
	 * @param ctx the parse tree
	 */
	void exitSkewSpec(ArcticSqlExtendParser.SkewSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#locationSpec}.
	 * @param ctx the parse tree
	 */
	void enterLocationSpec(ArcticSqlExtendParser.LocationSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#locationSpec}.
	 * @param ctx the parse tree
	 */
	void exitLocationSpec(ArcticSqlExtendParser.LocationSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#commentSpec}.
	 * @param ctx the parse tree
	 */
	void enterCommentSpec(ArcticSqlExtendParser.CommentSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#commentSpec}.
	 * @param ctx the parse tree
	 */
	void exitCommentSpec(ArcticSqlExtendParser.CommentSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(ArcticSqlExtendParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(ArcticSqlExtendParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#ctes}.
	 * @param ctx the parse tree
	 */
	void enterCtes(ArcticSqlExtendParser.CtesContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#ctes}.
	 * @param ctx the parse tree
	 */
	void exitCtes(ArcticSqlExtendParser.CtesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void enterNamedQuery(ArcticSqlExtendParser.NamedQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void exitNamedQuery(ArcticSqlExtendParser.NamedQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#tableProvider}.
	 * @param ctx the parse tree
	 */
	void enterTableProvider(ArcticSqlExtendParser.TableProviderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#tableProvider}.
	 * @param ctx the parse tree
	 */
	void exitTableProvider(ArcticSqlExtendParser.TableProviderContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#createTableClauses}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableClauses(ArcticSqlExtendParser.CreateTableClausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#createTableClauses}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableClauses(ArcticSqlExtendParser.CreateTableClausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#tablePropertyList}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyList(ArcticSqlExtendParser.TablePropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#tablePropertyList}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyList(ArcticSqlExtendParser.TablePropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#tableProperty}.
	 * @param ctx the parse tree
	 */
	void enterTableProperty(ArcticSqlExtendParser.TablePropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#tableProperty}.
	 * @param ctx the parse tree
	 */
	void exitTableProperty(ArcticSqlExtendParser.TablePropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyKey(ArcticSqlExtendParser.TablePropertyKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyKey(ArcticSqlExtendParser.TablePropertyKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyValue(ArcticSqlExtendParser.TablePropertyValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyValue(ArcticSqlExtendParser.TablePropertyValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#constantList}.
	 * @param ctx the parse tree
	 */
	void enterConstantList(ArcticSqlExtendParser.ConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#constantList}.
	 * @param ctx the parse tree
	 */
	void exitConstantList(ArcticSqlExtendParser.ConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#nestedConstantList}.
	 * @param ctx the parse tree
	 */
	void enterNestedConstantList(ArcticSqlExtendParser.NestedConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#nestedConstantList}.
	 * @param ctx the parse tree
	 */
	void exitNestedConstantList(ArcticSqlExtendParser.NestedConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#createFileFormat}.
	 * @param ctx the parse tree
	 */
	void enterCreateFileFormat(ArcticSqlExtendParser.CreateFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#createFileFormat}.
	 * @param ctx the parse tree
	 */
	void exitCreateFileFormat(ArcticSqlExtendParser.CreateFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticSqlExtendParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterTableFileFormat(ArcticSqlExtendParser.TableFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticSqlExtendParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitTableFileFormat(ArcticSqlExtendParser.TableFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticSqlExtendParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterGenericFileFormat(ArcticSqlExtendParser.GenericFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticSqlExtendParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitGenericFileFormat(ArcticSqlExtendParser.GenericFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#storageHandler}.
	 * @param ctx the parse tree
	 */
	void enterStorageHandler(ArcticSqlExtendParser.StorageHandlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#storageHandler}.
	 * @param ctx the parse tree
	 */
	void exitStorageHandler(ArcticSqlExtendParser.StorageHandlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#queryOrganization}.
	 * @param ctx the parse tree
	 */
	void enterQueryOrganization(ArcticSqlExtendParser.QueryOrganizationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#queryOrganization}.
	 * @param ctx the parse tree
	 */
	void exitQueryOrganization(ArcticSqlExtendParser.QueryOrganizationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterQueryTermDefault(ArcticSqlExtendParser.QueryTermDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitQueryTermDefault(ArcticSqlExtendParser.QueryTermDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterSetOperation(ArcticSqlExtendParser.SetOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitSetOperation(ArcticSqlExtendParser.SetOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterQueryPrimaryDefault(ArcticSqlExtendParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitQueryPrimaryDefault(ArcticSqlExtendParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterFromStmt(ArcticSqlExtendParser.FromStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitFromStmt(ArcticSqlExtendParser.FromStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTable(ArcticSqlExtendParser.TableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTable(ArcticSqlExtendParser.TableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTableDefault1(ArcticSqlExtendParser.InlineTableDefault1Context ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTableDefault1(ArcticSqlExtendParser.InlineTableDefault1Context ctx);
	/**
	 * Enter a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(ArcticSqlExtendParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticSqlExtendParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(ArcticSqlExtendParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void enterSortItem(ArcticSqlExtendParser.SortItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void exitSortItem(ArcticSqlExtendParser.SortItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#fromStatement}.
	 * @param ctx the parse tree
	 */
	void enterFromStatement(ArcticSqlExtendParser.FromStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#fromStatement}.
	 * @param ctx the parse tree
	 */
	void exitFromStatement(ArcticSqlExtendParser.FromStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#fromStatementBody}.
	 * @param ctx the parse tree
	 */
	void enterFromStatementBody(ArcticSqlExtendParser.FromStatementBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#fromStatementBody}.
	 * @param ctx the parse tree
	 */
	void exitFromStatementBody(ArcticSqlExtendParser.FromStatementBodyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link ArcticSqlExtendParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterTransformQuerySpecification(ArcticSqlExtendParser.TransformQuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link ArcticSqlExtendParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitTransformQuerySpecification(ArcticSqlExtendParser.TransformQuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link ArcticSqlExtendParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterRegularQuerySpecification(ArcticSqlExtendParser.RegularQuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link ArcticSqlExtendParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitRegularQuerySpecification(ArcticSqlExtendParser.RegularQuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#transformClause}.
	 * @param ctx the parse tree
	 */
	void enterTransformClause(ArcticSqlExtendParser.TransformClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#transformClause}.
	 * @param ctx the parse tree
	 */
	void exitTransformClause(ArcticSqlExtendParser.TransformClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(ArcticSqlExtendParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(ArcticSqlExtendParser.SelectClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void enterWhereClause(ArcticSqlExtendParser.WhereClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void exitWhereClause(ArcticSqlExtendParser.WhereClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void enterHavingClause(ArcticSqlExtendParser.HavingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void exitHavingClause(ArcticSqlExtendParser.HavingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#hint}.
	 * @param ctx the parse tree
	 */
	void enterHint(ArcticSqlExtendParser.HintContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#hint}.
	 * @param ctx the parse tree
	 */
	void exitHint(ArcticSqlExtendParser.HintContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#hintStatement}.
	 * @param ctx the parse tree
	 */
	void enterHintStatement(ArcticSqlExtendParser.HintStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#hintStatement}.
	 * @param ctx the parse tree
	 */
	void exitHintStatement(ArcticSqlExtendParser.HintStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFromClause(ArcticSqlExtendParser.FromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFromClause(ArcticSqlExtendParser.FromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#aggregationClause}.
	 * @param ctx the parse tree
	 */
	void enterAggregationClause(ArcticSqlExtendParser.AggregationClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#aggregationClause}.
	 * @param ctx the parse tree
	 */
	void exitAggregationClause(ArcticSqlExtendParser.AggregationClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#groupByClause}.
	 * @param ctx the parse tree
	 */
	void enterGroupByClause(ArcticSqlExtendParser.GroupByClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#groupByClause}.
	 * @param ctx the parse tree
	 */
	void exitGroupByClause(ArcticSqlExtendParser.GroupByClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 */
	void enterGroupingAnalytics(ArcticSqlExtendParser.GroupingAnalyticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 */
	void exitGroupingAnalytics(ArcticSqlExtendParser.GroupingAnalyticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterGroupingElement(ArcticSqlExtendParser.GroupingElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitGroupingElement(ArcticSqlExtendParser.GroupingElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void enterGroupingSet(ArcticSqlExtendParser.GroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void exitGroupingSet(ArcticSqlExtendParser.GroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#pivotClause}.
	 * @param ctx the parse tree
	 */
	void enterPivotClause(ArcticSqlExtendParser.PivotClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#pivotClause}.
	 * @param ctx the parse tree
	 */
	void exitPivotClause(ArcticSqlExtendParser.PivotClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#pivotColumn}.
	 * @param ctx the parse tree
	 */
	void enterPivotColumn(ArcticSqlExtendParser.PivotColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#pivotColumn}.
	 * @param ctx the parse tree
	 */
	void exitPivotColumn(ArcticSqlExtendParser.PivotColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#pivotValue}.
	 * @param ctx the parse tree
	 */
	void enterPivotValue(ArcticSqlExtendParser.PivotValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#pivotValue}.
	 * @param ctx the parse tree
	 */
	void exitPivotValue(ArcticSqlExtendParser.PivotValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#lateralView}.
	 * @param ctx the parse tree
	 */
	void enterLateralView(ArcticSqlExtendParser.LateralViewContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#lateralView}.
	 * @param ctx the parse tree
	 */
	void exitLateralView(ArcticSqlExtendParser.LateralViewContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterSetQuantifier(ArcticSqlExtendParser.SetQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitSetQuantifier(ArcticSqlExtendParser.SetQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterRelation(ArcticSqlExtendParser.RelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitRelation(ArcticSqlExtendParser.RelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void enterJoinRelation(ArcticSqlExtendParser.JoinRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void exitJoinRelation(ArcticSqlExtendParser.JoinRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#joinType}.
	 * @param ctx the parse tree
	 */
	void enterJoinType(ArcticSqlExtendParser.JoinTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#joinType}.
	 * @param ctx the parse tree
	 */
	void exitJoinType(ArcticSqlExtendParser.JoinTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void enterJoinCriteria(ArcticSqlExtendParser.JoinCriteriaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void exitJoinCriteria(ArcticSqlExtendParser.JoinCriteriaContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#sample}.
	 * @param ctx the parse tree
	 */
	void enterSample(ArcticSqlExtendParser.SampleContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#sample}.
	 * @param ctx the parse tree
	 */
	void exitSample(ArcticSqlExtendParser.SampleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByPercentile(ArcticSqlExtendParser.SampleByPercentileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByPercentile(ArcticSqlExtendParser.SampleByPercentileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByRows(ArcticSqlExtendParser.SampleByRowsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByRows(ArcticSqlExtendParser.SampleByRowsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByBucket(ArcticSqlExtendParser.SampleByBucketContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByBucket(ArcticSqlExtendParser.SampleByBucketContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByBytes(ArcticSqlExtendParser.SampleByBytesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticSqlExtendParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByBytes(ArcticSqlExtendParser.SampleByBytesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierList(ArcticSqlExtendParser.IdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierList(ArcticSqlExtendParser.IdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierSeq(ArcticSqlExtendParser.IdentifierSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierSeq(ArcticSqlExtendParser.IdentifierSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 */
	void enterOrderedIdentifierList(ArcticSqlExtendParser.OrderedIdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 */
	void exitOrderedIdentifierList(ArcticSqlExtendParser.OrderedIdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterOrderedIdentifier(ArcticSqlExtendParser.OrderedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitOrderedIdentifier(ArcticSqlExtendParser.OrderedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableName(ArcticSqlExtendParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableName(ArcticSqlExtendParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterAliasedQuery(ArcticSqlExtendParser.AliasedQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitAliasedQuery(ArcticSqlExtendParser.AliasedQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterAliasedRelation(ArcticSqlExtendParser.AliasedRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitAliasedRelation(ArcticSqlExtendParser.AliasedRelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTableDefault2(ArcticSqlExtendParser.InlineTableDefault2Context ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTableDefault2(ArcticSqlExtendParser.InlineTableDefault2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableValuedFunction(ArcticSqlExtendParser.TableValuedFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticSqlExtendParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableValuedFunction(ArcticSqlExtendParser.TableValuedFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void enterInlineTable(ArcticSqlExtendParser.InlineTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void exitInlineTable(ArcticSqlExtendParser.InlineTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#functionTable}.
	 * @param ctx the parse tree
	 */
	void enterFunctionTable(ArcticSqlExtendParser.FunctionTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#functionTable}.
	 * @param ctx the parse tree
	 */
	void exitFunctionTable(ArcticSqlExtendParser.FunctionTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#tableAlias}.
	 * @param ctx the parse tree
	 */
	void enterTableAlias(ArcticSqlExtendParser.TableAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#tableAlias}.
	 * @param ctx the parse tree
	 */
	void exitTableAlias(ArcticSqlExtendParser.TableAliasContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticSqlExtendParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void enterRowFormatSerde(ArcticSqlExtendParser.RowFormatSerdeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticSqlExtendParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void exitRowFormatSerde(ArcticSqlExtendParser.RowFormatSerdeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticSqlExtendParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void enterRowFormatDelimited(ArcticSqlExtendParser.RowFormatDelimitedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticSqlExtendParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void exitRowFormatDelimited(ArcticSqlExtendParser.RowFormatDelimitedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifier(ArcticSqlExtendParser.MultipartIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifier(ArcticSqlExtendParser.MultipartIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#namedExpression}.
	 * @param ctx the parse tree
	 */
	void enterNamedExpression(ArcticSqlExtendParser.NamedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#namedExpression}.
	 * @param ctx the parse tree
	 */
	void exitNamedExpression(ArcticSqlExtendParser.NamedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 */
	void enterNamedExpressionSeq(ArcticSqlExtendParser.NamedExpressionSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 */
	void exitNamedExpressionSeq(ArcticSqlExtendParser.NamedExpressionSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#partitionFieldList}.
	 * @param ctx the parse tree
	 */
	void enterPartitionFieldList(ArcticSqlExtendParser.PartitionFieldListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#partitionFieldList}.
	 * @param ctx the parse tree
	 */
	void exitPartitionFieldList(ArcticSqlExtendParser.PartitionFieldListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link ArcticSqlExtendParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void enterPartitionTransform(ArcticSqlExtendParser.PartitionTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link ArcticSqlExtendParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void exitPartitionTransform(ArcticSqlExtendParser.PartitionTransformContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link ArcticSqlExtendParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void enterPartitionColumn(ArcticSqlExtendParser.PartitionColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link ArcticSqlExtendParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void exitPartitionColumn(ArcticSqlExtendParser.PartitionColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link ArcticSqlExtendParser#transform}.
	 * @param ctx the parse tree
	 */
	void enterIdentityTransform(ArcticSqlExtendParser.IdentityTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link ArcticSqlExtendParser#transform}.
	 * @param ctx the parse tree
	 */
	void exitIdentityTransform(ArcticSqlExtendParser.IdentityTransformContext ctx);
	/**
	 * Enter a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link ArcticSqlExtendParser#transform}.
	 * @param ctx the parse tree
	 */
	void enterApplyTransform(ArcticSqlExtendParser.ApplyTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link ArcticSqlExtendParser#transform}.
	 * @param ctx the parse tree
	 */
	void exitApplyTransform(ArcticSqlExtendParser.ApplyTransformContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#transformArgument}.
	 * @param ctx the parse tree
	 */
	void enterTransformArgument(ArcticSqlExtendParser.TransformArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#transformArgument}.
	 * @param ctx the parse tree
	 */
	void exitTransformArgument(ArcticSqlExtendParser.TransformArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ArcticSqlExtendParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ArcticSqlExtendParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#expressionSeq}.
	 * @param ctx the parse tree
	 */
	void enterExpressionSeq(ArcticSqlExtendParser.ExpressionSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#expressionSeq}.
	 * @param ctx the parse tree
	 */
	void exitExpressionSeq(ArcticSqlExtendParser.ExpressionSeqContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalNot(ArcticSqlExtendParser.LogicalNotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalNot(ArcticSqlExtendParser.LogicalNotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterPredicated(ArcticSqlExtendParser.PredicatedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitPredicated(ArcticSqlExtendParser.PredicatedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterExists(ArcticSqlExtendParser.ExistsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitExists(ArcticSqlExtendParser.ExistsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalBinary(ArcticSqlExtendParser.LogicalBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticSqlExtendParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalBinary(ArcticSqlExtendParser.LogicalBinaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(ArcticSqlExtendParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(ArcticSqlExtendParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterValueExpressionDefault(ArcticSqlExtendParser.ValueExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitValueExpressionDefault(ArcticSqlExtendParser.ValueExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterComparison(ArcticSqlExtendParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitComparison(ArcticSqlExtendParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticBinary(ArcticSqlExtendParser.ArithmeticBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticBinary(ArcticSqlExtendParser.ArithmeticBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticUnary(ArcticSqlExtendParser.ArithmeticUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticSqlExtendParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticUnary(ArcticSqlExtendParser.ArithmeticUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStruct(ArcticSqlExtendParser.StructContext ctx);
	/**
	 * Exit a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStruct(ArcticSqlExtendParser.StructContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterDereference(ArcticSqlExtendParser.DereferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitDereference(ArcticSqlExtendParser.DereferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCase(ArcticSqlExtendParser.SimpleCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCase(ArcticSqlExtendParser.SimpleCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCurrentLike(ArcticSqlExtendParser.CurrentLikeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCurrentLike(ArcticSqlExtendParser.CurrentLikeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterColumnReference(ArcticSqlExtendParser.ColumnReferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitColumnReference(ArcticSqlExtendParser.ColumnReferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterRowConstructor(ArcticSqlExtendParser.RowConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitRowConstructor(ArcticSqlExtendParser.RowConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLast(ArcticSqlExtendParser.LastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLast(ArcticSqlExtendParser.LastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStar(ArcticSqlExtendParser.StarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStar(ArcticSqlExtendParser.StarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterOverlay(ArcticSqlExtendParser.OverlayContext ctx);
	/**
	 * Exit a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitOverlay(ArcticSqlExtendParser.OverlayContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(ArcticSqlExtendParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(ArcticSqlExtendParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryExpression(ArcticSqlExtendParser.SubqueryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryExpression(ArcticSqlExtendParser.SubqueryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code substring}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubstring(ArcticSqlExtendParser.SubstringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code substring}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubstring(ArcticSqlExtendParser.SubstringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCast(ArcticSqlExtendParser.CastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCast(ArcticSqlExtendParser.CastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantDefault(ArcticSqlExtendParser.ConstantDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantDefault(ArcticSqlExtendParser.ConstantDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambda(ArcticSqlExtendParser.LambdaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambda(ArcticSqlExtendParser.LambdaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(ArcticSqlExtendParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(ArcticSqlExtendParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code extract}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterExtract(ArcticSqlExtendParser.ExtractContext ctx);
	/**
	 * Exit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitExtract(ArcticSqlExtendParser.ExtractContext ctx);
	/**
	 * Enter a parse tree produced by the {@code trim}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTrim(ArcticSqlExtendParser.TrimContext ctx);
	/**
	 * Exit a parse tree produced by the {@code trim}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTrim(ArcticSqlExtendParser.TrimContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(ArcticSqlExtendParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(ArcticSqlExtendParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSearchedCase(ArcticSqlExtendParser.SearchedCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSearchedCase(ArcticSqlExtendParser.SearchedCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPosition(ArcticSqlExtendParser.PositionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPosition(ArcticSqlExtendParser.PositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFirst(ArcticSqlExtendParser.FirstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticSqlExtendParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFirst(ArcticSqlExtendParser.FirstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNullLiteral(ArcticSqlExtendParser.NullLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNullLiteral(ArcticSqlExtendParser.NullLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterIntervalLiteral(ArcticSqlExtendParser.IntervalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitIntervalLiteral(ArcticSqlExtendParser.IntervalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterTypeConstructor(ArcticSqlExtendParser.TypeConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitTypeConstructor(ArcticSqlExtendParser.TypeConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(ArcticSqlExtendParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(ArcticSqlExtendParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(ArcticSqlExtendParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(ArcticSqlExtendParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(ArcticSqlExtendParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(ArcticSqlExtendParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(ArcticSqlExtendParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(ArcticSqlExtendParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(ArcticSqlExtendParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(ArcticSqlExtendParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#interval}.
	 * @param ctx the parse tree
	 */
	void enterInterval(ArcticSqlExtendParser.IntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#interval}.
	 * @param ctx the parse tree
	 */
	void exitInterval(ArcticSqlExtendParser.IntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingMultiUnitsInterval(ArcticSqlExtendParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingMultiUnitsInterval(ArcticSqlExtendParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void enterMultiUnitsInterval(ArcticSqlExtendParser.MultiUnitsIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void exitMultiUnitsInterval(ArcticSqlExtendParser.MultiUnitsIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingUnitToUnitInterval(ArcticSqlExtendParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingUnitToUnitInterval(ArcticSqlExtendParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void enterUnitToUnitInterval(ArcticSqlExtendParser.UnitToUnitIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void exitUnitToUnitInterval(ArcticSqlExtendParser.UnitToUnitIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#intervalValue}.
	 * @param ctx the parse tree
	 */
	void enterIntervalValue(ArcticSqlExtendParser.IntervalValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#intervalValue}.
	 * @param ctx the parse tree
	 */
	void exitIntervalValue(ArcticSqlExtendParser.IntervalValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#colPosition}.
	 * @param ctx the parse tree
	 */
	void enterColPosition(ArcticSqlExtendParser.ColPositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#colPosition}.
	 * @param ctx the parse tree
	 */
	void exitColPosition(ArcticSqlExtendParser.ColPositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterComplexDataType(ArcticSqlExtendParser.ComplexDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitComplexDataType(ArcticSqlExtendParser.ComplexDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterYearMonthIntervalDataType(ArcticSqlExtendParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitYearMonthIntervalDataType(ArcticSqlExtendParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDayTimeIntervalDataType(ArcticSqlExtendParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDayTimeIntervalDataType(ArcticSqlExtendParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveDataType(ArcticSqlExtendParser.PrimitiveDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticSqlExtendParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveDataType(ArcticSqlExtendParser.PrimitiveDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void enterColTypeList(ArcticSqlExtendParser.ColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void exitColTypeList(ArcticSqlExtendParser.ColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#colType}.
	 * @param ctx the parse tree
	 */
	void enterColType(ArcticSqlExtendParser.ColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#colType}.
	 * @param ctx the parse tree
	 */
	void exitColType(ArcticSqlExtendParser.ColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#complexColTypeList}.
	 * @param ctx the parse tree
	 */
	void enterComplexColTypeList(ArcticSqlExtendParser.ComplexColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#complexColTypeList}.
	 * @param ctx the parse tree
	 */
	void exitComplexColTypeList(ArcticSqlExtendParser.ComplexColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#complexColType}.
	 * @param ctx the parse tree
	 */
	void enterComplexColType(ArcticSqlExtendParser.ComplexColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#complexColType}.
	 * @param ctx the parse tree
	 */
	void exitComplexColType(ArcticSqlExtendParser.ComplexColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(ArcticSqlExtendParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(ArcticSqlExtendParser.WhenClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#windowClause}.
	 * @param ctx the parse tree
	 */
	void enterWindowClause(ArcticSqlExtendParser.WindowClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#windowClause}.
	 * @param ctx the parse tree
	 */
	void exitWindowClause(ArcticSqlExtendParser.WindowClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#namedWindow}.
	 * @param ctx the parse tree
	 */
	void enterNamedWindow(ArcticSqlExtendParser.NamedWindowContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#namedWindow}.
	 * @param ctx the parse tree
	 */
	void exitNamedWindow(ArcticSqlExtendParser.NamedWindowContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticSqlExtendParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void enterWindowRef(ArcticSqlExtendParser.WindowRefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticSqlExtendParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void exitWindowRef(ArcticSqlExtendParser.WindowRefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticSqlExtendParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void enterWindowDef(ArcticSqlExtendParser.WindowDefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticSqlExtendParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void exitWindowDef(ArcticSqlExtendParser.WindowDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void enterWindowFrame(ArcticSqlExtendParser.WindowFrameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void exitWindowFrame(ArcticSqlExtendParser.WindowFrameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterFrameBound(ArcticSqlExtendParser.FrameBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitFrameBound(ArcticSqlExtendParser.FrameBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#functionName}.
	 * @param ctx the parse tree
	 */
	void enterFunctionName(ArcticSqlExtendParser.FunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#functionName}.
	 * @param ctx the parse tree
	 */
	void exitFunctionName(ArcticSqlExtendParser.FunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(ArcticSqlExtendParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(ArcticSqlExtendParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingIdentifier(ArcticSqlExtendParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingIdentifier(ArcticSqlExtendParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticSqlExtendParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void enterErrorIdent(ArcticSqlExtendParser.ErrorIdentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticSqlExtendParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void exitErrorIdent(ArcticSqlExtendParser.ErrorIdentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticSqlExtendParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void enterRealIdent(ArcticSqlExtendParser.RealIdentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticSqlExtendParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void exitRealIdent(ArcticSqlExtendParser.RealIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(ArcticSqlExtendParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(ArcticSqlExtendParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSqlExtendParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedIdentifier(ArcticSqlExtendParser.UnquotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSqlExtendParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedIdentifier(ArcticSqlExtendParser.UnquotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSqlExtendParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifierAlternative(ArcticSqlExtendParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSqlExtendParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifierAlternative(ArcticSqlExtendParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifier(ArcticSqlExtendParser.QuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifier(ArcticSqlExtendParser.QuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void enterExponentLiteral(ArcticSqlExtendParser.ExponentLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void exitExponentLiteral(ArcticSqlExtendParser.ExponentLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDecimalLiteral(ArcticSqlExtendParser.DecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDecimalLiteral(ArcticSqlExtendParser.DecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void enterLegacyDecimalLiteral(ArcticSqlExtendParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void exitLegacyDecimalLiteral(ArcticSqlExtendParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(ArcticSqlExtendParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(ArcticSqlExtendParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigIntLiteral(ArcticSqlExtendParser.BigIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigIntLiteral(ArcticSqlExtendParser.BigIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void enterSmallIntLiteral(ArcticSqlExtendParser.SmallIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void exitSmallIntLiteral(ArcticSqlExtendParser.SmallIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void enterTinyIntLiteral(ArcticSqlExtendParser.TinyIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void exitTinyIntLiteral(ArcticSqlExtendParser.TinyIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(ArcticSqlExtendParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(ArcticSqlExtendParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void enterFloatLiteral(ArcticSqlExtendParser.FloatLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void exitFloatLiteral(ArcticSqlExtendParser.FloatLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigDecimalLiteral(ArcticSqlExtendParser.BigDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticSqlExtendParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigDecimalLiteral(ArcticSqlExtendParser.BigDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 */
	void enterAnsiNonReserved(ArcticSqlExtendParser.AnsiNonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 */
	void exitAnsiNonReserved(ArcticSqlExtendParser.AnsiNonReservedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#strictNonReserved}.
	 * @param ctx the parse tree
	 */
	void enterStrictNonReserved(ArcticSqlExtendParser.StrictNonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#strictNonReserved}.
	 * @param ctx the parse tree
	 */
	void exitStrictNonReserved(ArcticSqlExtendParser.StrictNonReservedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlExtendParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void enterNonReserved(ArcticSqlExtendParser.NonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlExtendParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void exitNonReserved(ArcticSqlExtendParser.NonReservedContext ctx);
}
