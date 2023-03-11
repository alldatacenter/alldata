// Generated from /Users/jinsilei/arctic/arctic/spark/v2.3/spark/src/main/antlr4/com/netease/arctic/spark/sql/parser/ArcticSparkSql.g4 by ANTLR 4.7.2
package com.netease.arctic.spark.sql.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ArcticSparkSqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ArcticSparkSqlVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleStatement(ArcticSparkSqlParser.SingleStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createArcticTable}
	 * labeled alternative in {@link ArcticSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateArcticTable(ArcticSparkSqlParser.CreateArcticTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code passThrough}
	 * labeled alternative in {@link ArcticSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPassThrough(ArcticSparkSqlParser.PassThroughContext ctx);
	/**
	 * Visit a parse tree produced by the {@code colListWithPk}
	 * labeled alternative in {@link ArcticSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColListWithPk(ArcticSparkSqlParser.ColListWithPkContext ctx);
	/**
	 * Visit a parse tree produced by the {@code colListOnlyPk}
	 * labeled alternative in {@link ArcticSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColListOnlyPk(ArcticSparkSqlParser.ColListOnlyPkContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#primaryKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryKey(ArcticSparkSqlParser.PrimaryKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#partitionFieldList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionFieldList(ArcticSparkSqlParser.PartitionFieldListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code partitionColumnRef}
	 * labeled alternative in {@link ArcticSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionColumnRef(ArcticSparkSqlParser.PartitionColumnRefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code partitionColumnDefine}
	 * labeled alternative in {@link ArcticSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionColumnDefine(ArcticSparkSqlParser.PartitionColumnDefineContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleExpression(ArcticSparkSqlParser.SingleExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleTableIdentifier(ArcticSparkSqlParser.SingleTableIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleFunctionIdentifier(ArcticSparkSqlParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#singleDataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleDataType(ArcticSparkSqlParser.SingleDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#singleTableSchema}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleTableSchema(ArcticSparkSqlParser.SingleTableSchemaContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsupportedHiveNativeCommands(ArcticSparkSqlParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#createTableHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableHeader(ArcticSparkSqlParser.CreateTableHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#bucketSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBucketSpec(ArcticSparkSqlParser.BucketSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#skewSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkewSpec(ArcticSparkSqlParser.SkewSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#locationSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocationSpec(ArcticSparkSqlParser.LocationSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(ArcticSparkSqlParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertOverwriteTable(ArcticSparkSqlParser.InsertOverwriteTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertIntoTable(ArcticSparkSqlParser.InsertIntoTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertOverwriteHiveDir(ArcticSparkSqlParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertOverwriteDir(ArcticSparkSqlParser.InsertOverwriteDirContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionSpecLocation(ArcticSparkSqlParser.PartitionSpecLocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#partitionSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionSpec(ArcticSparkSqlParser.PartitionSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#partitionVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionVal(ArcticSparkSqlParser.PartitionValContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#describeFuncName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeFuncName(ArcticSparkSqlParser.DescribeFuncNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#describeColName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeColName(ArcticSparkSqlParser.DescribeColNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#ctes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCtes(ArcticSparkSqlParser.CtesContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#namedQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedQuery(ArcticSparkSqlParser.NamedQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#tableProvider}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableProvider(ArcticSparkSqlParser.TableProviderContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#tablePropertyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyList(ArcticSparkSqlParser.TablePropertyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#tableProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableProperty(ArcticSparkSqlParser.TablePropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyKey(ArcticSparkSqlParser.TablePropertyKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyValue(ArcticSparkSqlParser.TablePropertyValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#constantList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantList(ArcticSparkSqlParser.ConstantListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#nestedConstantList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNestedConstantList(ArcticSparkSqlParser.NestedConstantListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#createFileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateFileFormat(ArcticSparkSqlParser.CreateFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableFileFormat(ArcticSparkSqlParser.TableFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericFileFormat(ArcticSparkSqlParser.GenericFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#storageHandler}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorageHandler(ArcticSparkSqlParser.StorageHandlerContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#resource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource(ArcticSparkSqlParser.ResourceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleInsertQuery(ArcticSparkSqlParser.SingleInsertQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiInsertQuery(ArcticSparkSqlParser.MultiInsertQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#queryOrganization}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryOrganization(ArcticSparkSqlParser.QueryOrganizationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiInsertQueryBody(ArcticSparkSqlParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryTermDefault(ArcticSparkSqlParser.QueryTermDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetOperation(ArcticSparkSqlParser.SetOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryPrimaryDefault(ArcticSparkSqlParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable(ArcticSparkSqlParser.TableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTableDefault1(ArcticSparkSqlParser.InlineTableDefault1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery(ArcticSparkSqlParser.SubqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#sortItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortItem(ArcticSparkSqlParser.SortItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuerySpecification(ArcticSparkSqlParser.QuerySpecificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#hint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHint(ArcticSparkSqlParser.HintContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#hintStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHintStatement(ArcticSparkSqlParser.HintStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#fromClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromClause(ArcticSparkSqlParser.FromClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#aggregation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregation(ArcticSparkSqlParser.AggregationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#groupingSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingSet(ArcticSparkSqlParser.GroupingSetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#lateralView}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLateralView(ArcticSparkSqlParser.LateralViewContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetQuantifier(ArcticSparkSqlParser.SetQuantifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#relation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelation(ArcticSparkSqlParser.RelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#joinRelation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinRelation(ArcticSparkSqlParser.JoinRelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#joinType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinType(ArcticSparkSqlParser.JoinTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#joinCriteria}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinCriteria(ArcticSparkSqlParser.JoinCriteriaContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#sample}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSample(ArcticSparkSqlParser.SampleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByPercentile(ArcticSparkSqlParser.SampleByPercentileContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByRows(ArcticSparkSqlParser.SampleByRowsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByBucket(ArcticSparkSqlParser.SampleByBucketContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByBytes(ArcticSparkSqlParser.SampleByBytesContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#identifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierList(ArcticSparkSqlParser.IdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierSeq(ArcticSparkSqlParser.IdentifierSeqContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderedIdentifierList(ArcticSparkSqlParser.OrderedIdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderedIdentifier(ArcticSparkSqlParser.OrderedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#identifierCommentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierCommentList(ArcticSparkSqlParser.IdentifierCommentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#identifierComment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierComment(ArcticSparkSqlParser.IdentifierCommentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableName(ArcticSparkSqlParser.TableNameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedQuery(ArcticSparkSqlParser.AliasedQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedRelation(ArcticSparkSqlParser.AliasedRelationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTableDefault2(ArcticSparkSqlParser.InlineTableDefault2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableValuedFunction(ArcticSparkSqlParser.TableValuedFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#inlineTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTable(ArcticSparkSqlParser.InlineTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#functionTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTable(ArcticSparkSqlParser.FunctionTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#tableAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableAlias(ArcticSparkSqlParser.TableAliasContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowFormatSerde(ArcticSparkSqlParser.RowFormatSerdeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowFormatDelimited(ArcticSparkSqlParser.RowFormatDelimitedContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableIdentifier(ArcticSparkSqlParser.TableIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#functionIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionIdentifier(ArcticSparkSqlParser.FunctionIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#namedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedExpression(ArcticSparkSqlParser.NamedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedExpressionSeq(ArcticSparkSqlParser.NamedExpressionSeqContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(ArcticSparkSqlParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalNot(ArcticSparkSqlParser.LogicalNotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanDefault(ArcticSparkSqlParser.BooleanDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExists(ArcticSparkSqlParser.ExistsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalBinary(ArcticSparkSqlParser.LogicalBinaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#predicated}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicated(ArcticSparkSqlParser.PredicatedContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicate(ArcticSparkSqlParser.PredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueExpressionDefault(ArcticSparkSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison(ArcticSparkSqlParser.ComparisonContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticBinary(ArcticSparkSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticUnary(ArcticSparkSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct(ArcticSparkSqlParser.StructContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDereference(ArcticSparkSqlParser.DereferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleCase(ArcticSparkSqlParser.SimpleCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnReference(ArcticSparkSqlParser.ColumnReferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowConstructor(ArcticSparkSqlParser.RowConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLast(ArcticSparkSqlParser.LastContext ctx);
	/**
	 * Visit a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar(ArcticSparkSqlParser.StarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscript(ArcticSparkSqlParser.SubscriptContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubqueryExpression(ArcticSparkSqlParser.SubqueryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast(ArcticSparkSqlParser.CastContext ctx);
	/**
	 * Visit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDefault(ArcticSparkSqlParser.ConstantDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpression(ArcticSparkSqlParser.ParenthesizedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(ArcticSparkSqlParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearchedCase(ArcticSparkSqlParser.SearchedCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPosition(ArcticSparkSqlParser.PositionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFirst(ArcticSparkSqlParser.FirstContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullLiteral(ArcticSparkSqlParser.NullLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalLiteral(ArcticSparkSqlParser.IntervalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeConstructor(ArcticSparkSqlParser.TypeConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(ArcticSparkSqlParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(ArcticSparkSqlParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(ArcticSparkSqlParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOperator(ArcticSparkSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticOperator(ArcticSparkSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#predicateOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicateOperator(ArcticSparkSqlParser.PredicateOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanValue(ArcticSparkSqlParser.BooleanValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#interval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval(ArcticSparkSqlParser.IntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#intervalField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalField(ArcticSparkSqlParser.IntervalFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#intervalValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalValue(ArcticSparkSqlParser.IntervalValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#colPosition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColPosition(ArcticSparkSqlParser.ColPositionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexDataType(ArcticSparkSqlParser.ComplexDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveDataType(ArcticSparkSqlParser.PrimitiveDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColTypeList(ArcticSparkSqlParser.ColTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#colType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColType(ArcticSparkSqlParser.ColTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#complexColTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexColTypeList(ArcticSparkSqlParser.ComplexColTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#complexColType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexColType(ArcticSparkSqlParser.ComplexColTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#whenClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenClause(ArcticSparkSqlParser.WhenClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#windows}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindows(ArcticSparkSqlParser.WindowsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#namedWindow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedWindow(ArcticSparkSqlParser.NamedWindowContext ctx);
	/**
	 * Visit a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowRef(ArcticSparkSqlParser.WindowRefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowDef(ArcticSparkSqlParser.WindowDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#windowFrame}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowFrame(ArcticSparkSqlParser.WindowFrameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#frameBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrameBound(ArcticSparkSqlParser.FrameBoundContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(ArcticSparkSqlParser.QualifiedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(ArcticSparkSqlParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnquotedIdentifier(ArcticSparkSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifierAlternative(ArcticSparkSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifier(ArcticSparkSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecimalLiteral(ArcticSparkSqlParser.DecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerLiteral(ArcticSparkSqlParser.IntegerLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigIntLiteral(ArcticSparkSqlParser.BigIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSmallIntLiteral(ArcticSparkSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTinyIntLiteral(ArcticSparkSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleLiteral(ArcticSparkSqlParser.DoubleLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigDecimalLiteral(ArcticSparkSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSparkSqlParser#nonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonReserved(ArcticSparkSqlParser.NonReservedContext ctx);
}