// Generated from /Users/jinsilei/arctic/arctic/spark/v2.3/spark/src/main/antlr4/com/netease/arctic/spark/sql/parser/ArcticSparkSql.g4 by ANTLR 4.7.2
package com.netease.arctic.spark.sql.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ArcticSparkSqlParser}.
 */
public interface ArcticSparkSqlListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void enterSingleStatement(ArcticSparkSqlParser.SingleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void exitSingleStatement(ArcticSparkSqlParser.SingleStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createArcticTable}
	 * labeled alternative in {@link ArcticSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateArcticTable(ArcticSparkSqlParser.CreateArcticTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createArcticTable}
	 * labeled alternative in {@link ArcticSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateArcticTable(ArcticSparkSqlParser.CreateArcticTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code passThrough}
	 * labeled alternative in {@link ArcticSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterPassThrough(ArcticSparkSqlParser.PassThroughContext ctx);
	/**
	 * Exit a parse tree produced by the {@code passThrough}
	 * labeled alternative in {@link ArcticSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitPassThrough(ArcticSparkSqlParser.PassThroughContext ctx);
	/**
	 * Enter a parse tree produced by the {@code colListWithPk}
	 * labeled alternative in {@link ArcticSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void enterColListWithPk(ArcticSparkSqlParser.ColListWithPkContext ctx);
	/**
	 * Exit a parse tree produced by the {@code colListWithPk}
	 * labeled alternative in {@link ArcticSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void exitColListWithPk(ArcticSparkSqlParser.ColListWithPkContext ctx);
	/**
	 * Enter a parse tree produced by the {@code colListOnlyPk}
	 * labeled alternative in {@link ArcticSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void enterColListOnlyPk(ArcticSparkSqlParser.ColListOnlyPkContext ctx);
	/**
	 * Exit a parse tree produced by the {@code colListOnlyPk}
	 * labeled alternative in {@link ArcticSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void exitColListOnlyPk(ArcticSparkSqlParser.ColListOnlyPkContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#primaryKey}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryKey(ArcticSparkSqlParser.PrimaryKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#primaryKey}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryKey(ArcticSparkSqlParser.PrimaryKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#partitionFieldList}.
	 * @param ctx the parse tree
	 */
	void enterPartitionFieldList(ArcticSparkSqlParser.PartitionFieldListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#partitionFieldList}.
	 * @param ctx the parse tree
	 */
	void exitPartitionFieldList(ArcticSparkSqlParser.PartitionFieldListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionColumnRef}
	 * labeled alternative in {@link ArcticSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void enterPartitionColumnRef(ArcticSparkSqlParser.PartitionColumnRefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionColumnRef}
	 * labeled alternative in {@link ArcticSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void exitPartitionColumnRef(ArcticSparkSqlParser.PartitionColumnRefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionColumnDefine}
	 * labeled alternative in {@link ArcticSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void enterPartitionColumnDefine(ArcticSparkSqlParser.PartitionColumnDefineContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionColumnDefine}
	 * labeled alternative in {@link ArcticSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void exitPartitionColumnDefine(ArcticSparkSqlParser.PartitionColumnDefineContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterSingleExpression(ArcticSparkSqlParser.SingleExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitSingleExpression(ArcticSparkSqlParser.SingleExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleTableIdentifier(ArcticSparkSqlParser.SingleTableIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleTableIdentifier(ArcticSparkSqlParser.SingleTableIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleFunctionIdentifier(ArcticSparkSqlParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleFunctionIdentifier(ArcticSparkSqlParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#singleDataType}.
	 * @param ctx the parse tree
	 */
	void enterSingleDataType(ArcticSparkSqlParser.SingleDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#singleDataType}.
	 * @param ctx the parse tree
	 */
	void exitSingleDataType(ArcticSparkSqlParser.SingleDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#singleTableSchema}.
	 * @param ctx the parse tree
	 */
	void enterSingleTableSchema(ArcticSparkSqlParser.SingleTableSchemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#singleTableSchema}.
	 * @param ctx the parse tree
	 */
	void exitSingleTableSchema(ArcticSparkSqlParser.SingleTableSchemaContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 */
	void enterUnsupportedHiveNativeCommands(ArcticSparkSqlParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 */
	void exitUnsupportedHiveNativeCommands(ArcticSparkSqlParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#createTableHeader}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableHeader(ArcticSparkSqlParser.CreateTableHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#createTableHeader}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableHeader(ArcticSparkSqlParser.CreateTableHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#bucketSpec}.
	 * @param ctx the parse tree
	 */
	void enterBucketSpec(ArcticSparkSqlParser.BucketSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#bucketSpec}.
	 * @param ctx the parse tree
	 */
	void exitBucketSpec(ArcticSparkSqlParser.BucketSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#skewSpec}.
	 * @param ctx the parse tree
	 */
	void enterSkewSpec(ArcticSparkSqlParser.SkewSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#skewSpec}.
	 * @param ctx the parse tree
	 */
	void exitSkewSpec(ArcticSparkSqlParser.SkewSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#locationSpec}.
	 * @param ctx the parse tree
	 */
	void enterLocationSpec(ArcticSparkSqlParser.LocationSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#locationSpec}.
	 * @param ctx the parse tree
	 */
	void exitLocationSpec(ArcticSparkSqlParser.LocationSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(ArcticSparkSqlParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(ArcticSparkSqlParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteTable(ArcticSparkSqlParser.InsertOverwriteTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteTable(ArcticSparkSqlParser.InsertOverwriteTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertIntoTable(ArcticSparkSqlParser.InsertIntoTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertIntoTable(ArcticSparkSqlParser.InsertIntoTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteHiveDir(ArcticSparkSqlParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteHiveDir(ArcticSparkSqlParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteDir(ArcticSparkSqlParser.InsertOverwriteDirContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link ArcticSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteDir(ArcticSparkSqlParser.InsertOverwriteDirContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 */
	void enterPartitionSpecLocation(ArcticSparkSqlParser.PartitionSpecLocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 */
	void exitPartitionSpecLocation(ArcticSparkSqlParser.PartitionSpecLocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#partitionSpec}.
	 * @param ctx the parse tree
	 */
	void enterPartitionSpec(ArcticSparkSqlParser.PartitionSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#partitionSpec}.
	 * @param ctx the parse tree
	 */
	void exitPartitionSpec(ArcticSparkSqlParser.PartitionSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#partitionVal}.
	 * @param ctx the parse tree
	 */
	void enterPartitionVal(ArcticSparkSqlParser.PartitionValContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#partitionVal}.
	 * @param ctx the parse tree
	 */
	void exitPartitionVal(ArcticSparkSqlParser.PartitionValContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#describeFuncName}.
	 * @param ctx the parse tree
	 */
	void enterDescribeFuncName(ArcticSparkSqlParser.DescribeFuncNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#describeFuncName}.
	 * @param ctx the parse tree
	 */
	void exitDescribeFuncName(ArcticSparkSqlParser.DescribeFuncNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#describeColName}.
	 * @param ctx the parse tree
	 */
	void enterDescribeColName(ArcticSparkSqlParser.DescribeColNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#describeColName}.
	 * @param ctx the parse tree
	 */
	void exitDescribeColName(ArcticSparkSqlParser.DescribeColNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#ctes}.
	 * @param ctx the parse tree
	 */
	void enterCtes(ArcticSparkSqlParser.CtesContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#ctes}.
	 * @param ctx the parse tree
	 */
	void exitCtes(ArcticSparkSqlParser.CtesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void enterNamedQuery(ArcticSparkSqlParser.NamedQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void exitNamedQuery(ArcticSparkSqlParser.NamedQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#tableProvider}.
	 * @param ctx the parse tree
	 */
	void enterTableProvider(ArcticSparkSqlParser.TableProviderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#tableProvider}.
	 * @param ctx the parse tree
	 */
	void exitTableProvider(ArcticSparkSqlParser.TableProviderContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#tablePropertyList}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyList(ArcticSparkSqlParser.TablePropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#tablePropertyList}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyList(ArcticSparkSqlParser.TablePropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#tableProperty}.
	 * @param ctx the parse tree
	 */
	void enterTableProperty(ArcticSparkSqlParser.TablePropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#tableProperty}.
	 * @param ctx the parse tree
	 */
	void exitTableProperty(ArcticSparkSqlParser.TablePropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyKey(ArcticSparkSqlParser.TablePropertyKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyKey(ArcticSparkSqlParser.TablePropertyKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyValue(ArcticSparkSqlParser.TablePropertyValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyValue(ArcticSparkSqlParser.TablePropertyValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#constantList}.
	 * @param ctx the parse tree
	 */
	void enterConstantList(ArcticSparkSqlParser.ConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#constantList}.
	 * @param ctx the parse tree
	 */
	void exitConstantList(ArcticSparkSqlParser.ConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#nestedConstantList}.
	 * @param ctx the parse tree
	 */
	void enterNestedConstantList(ArcticSparkSqlParser.NestedConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#nestedConstantList}.
	 * @param ctx the parse tree
	 */
	void exitNestedConstantList(ArcticSparkSqlParser.NestedConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#createFileFormat}.
	 * @param ctx the parse tree
	 */
	void enterCreateFileFormat(ArcticSparkSqlParser.CreateFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#createFileFormat}.
	 * @param ctx the parse tree
	 */
	void exitCreateFileFormat(ArcticSparkSqlParser.CreateFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterTableFileFormat(ArcticSparkSqlParser.TableFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitTableFileFormat(ArcticSparkSqlParser.TableFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterGenericFileFormat(ArcticSparkSqlParser.GenericFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitGenericFileFormat(ArcticSparkSqlParser.GenericFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#storageHandler}.
	 * @param ctx the parse tree
	 */
	void enterStorageHandler(ArcticSparkSqlParser.StorageHandlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#storageHandler}.
	 * @param ctx the parse tree
	 */
	void exitStorageHandler(ArcticSparkSqlParser.StorageHandlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(ArcticSparkSqlParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(ArcticSparkSqlParser.ResourceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryNoWith}.
	 * @param ctx the parse tree
	 */
	void enterSingleInsertQuery(ArcticSparkSqlParser.SingleInsertQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryNoWith}.
	 * @param ctx the parse tree
	 */
	void exitSingleInsertQuery(ArcticSparkSqlParser.SingleInsertQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryNoWith}.
	 * @param ctx the parse tree
	 */
	void enterMultiInsertQuery(ArcticSparkSqlParser.MultiInsertQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryNoWith}.
	 * @param ctx the parse tree
	 */
	void exitMultiInsertQuery(ArcticSparkSqlParser.MultiInsertQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#queryOrganization}.
	 * @param ctx the parse tree
	 */
	void enterQueryOrganization(ArcticSparkSqlParser.QueryOrganizationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#queryOrganization}.
	 * @param ctx the parse tree
	 */
	void exitQueryOrganization(ArcticSparkSqlParser.QueryOrganizationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 */
	void enterMultiInsertQueryBody(ArcticSparkSqlParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 */
	void exitMultiInsertQueryBody(ArcticSparkSqlParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterQueryTermDefault(ArcticSparkSqlParser.QueryTermDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitQueryTermDefault(ArcticSparkSqlParser.QueryTermDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterSetOperation(ArcticSparkSqlParser.SetOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitSetOperation(ArcticSparkSqlParser.SetOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterQueryPrimaryDefault(ArcticSparkSqlParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitQueryPrimaryDefault(ArcticSparkSqlParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTable(ArcticSparkSqlParser.TableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTable(ArcticSparkSqlParser.TableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTableDefault1(ArcticSparkSqlParser.InlineTableDefault1Context ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTableDefault1(ArcticSparkSqlParser.InlineTableDefault1Context ctx);
	/**
	 * Enter a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(ArcticSparkSqlParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(ArcticSparkSqlParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void enterSortItem(ArcticSparkSqlParser.SortItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void exitSortItem(ArcticSparkSqlParser.SortItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterQuerySpecification(ArcticSparkSqlParser.QuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitQuerySpecification(ArcticSparkSqlParser.QuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#hint}.
	 * @param ctx the parse tree
	 */
	void enterHint(ArcticSparkSqlParser.HintContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#hint}.
	 * @param ctx the parse tree
	 */
	void exitHint(ArcticSparkSqlParser.HintContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#hintStatement}.
	 * @param ctx the parse tree
	 */
	void enterHintStatement(ArcticSparkSqlParser.HintStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#hintStatement}.
	 * @param ctx the parse tree
	 */
	void exitHintStatement(ArcticSparkSqlParser.HintStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFromClause(ArcticSparkSqlParser.FromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFromClause(ArcticSparkSqlParser.FromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#aggregation}.
	 * @param ctx the parse tree
	 */
	void enterAggregation(ArcticSparkSqlParser.AggregationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#aggregation}.
	 * @param ctx the parse tree
	 */
	void exitAggregation(ArcticSparkSqlParser.AggregationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void enterGroupingSet(ArcticSparkSqlParser.GroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void exitGroupingSet(ArcticSparkSqlParser.GroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#lateralView}.
	 * @param ctx the parse tree
	 */
	void enterLateralView(ArcticSparkSqlParser.LateralViewContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#lateralView}.
	 * @param ctx the parse tree
	 */
	void exitLateralView(ArcticSparkSqlParser.LateralViewContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterSetQuantifier(ArcticSparkSqlParser.SetQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitSetQuantifier(ArcticSparkSqlParser.SetQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterRelation(ArcticSparkSqlParser.RelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitRelation(ArcticSparkSqlParser.RelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void enterJoinRelation(ArcticSparkSqlParser.JoinRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void exitJoinRelation(ArcticSparkSqlParser.JoinRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#joinType}.
	 * @param ctx the parse tree
	 */
	void enterJoinType(ArcticSparkSqlParser.JoinTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#joinType}.
	 * @param ctx the parse tree
	 */
	void exitJoinType(ArcticSparkSqlParser.JoinTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void enterJoinCriteria(ArcticSparkSqlParser.JoinCriteriaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void exitJoinCriteria(ArcticSparkSqlParser.JoinCriteriaContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#sample}.
	 * @param ctx the parse tree
	 */
	void enterSample(ArcticSparkSqlParser.SampleContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#sample}.
	 * @param ctx the parse tree
	 */
	void exitSample(ArcticSparkSqlParser.SampleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByPercentile(ArcticSparkSqlParser.SampleByPercentileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByPercentile(ArcticSparkSqlParser.SampleByPercentileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByRows(ArcticSparkSqlParser.SampleByRowsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByRows(ArcticSparkSqlParser.SampleByRowsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByBucket(ArcticSparkSqlParser.SampleByBucketContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByBucket(ArcticSparkSqlParser.SampleByBucketContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByBytes(ArcticSparkSqlParser.SampleByBytesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByBytes(ArcticSparkSqlParser.SampleByBytesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierList(ArcticSparkSqlParser.IdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierList(ArcticSparkSqlParser.IdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierSeq(ArcticSparkSqlParser.IdentifierSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierSeq(ArcticSparkSqlParser.IdentifierSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 */
	void enterOrderedIdentifierList(ArcticSparkSqlParser.OrderedIdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 */
	void exitOrderedIdentifierList(ArcticSparkSqlParser.OrderedIdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterOrderedIdentifier(ArcticSparkSqlParser.OrderedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitOrderedIdentifier(ArcticSparkSqlParser.OrderedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#identifierCommentList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierCommentList(ArcticSparkSqlParser.IdentifierCommentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#identifierCommentList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierCommentList(ArcticSparkSqlParser.IdentifierCommentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#identifierComment}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierComment(ArcticSparkSqlParser.IdentifierCommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#identifierComment}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierComment(ArcticSparkSqlParser.IdentifierCommentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableName(ArcticSparkSqlParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableName(ArcticSparkSqlParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterAliasedQuery(ArcticSparkSqlParser.AliasedQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitAliasedQuery(ArcticSparkSqlParser.AliasedQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterAliasedRelation(ArcticSparkSqlParser.AliasedRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitAliasedRelation(ArcticSparkSqlParser.AliasedRelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTableDefault2(ArcticSparkSqlParser.InlineTableDefault2Context ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTableDefault2(ArcticSparkSqlParser.InlineTableDefault2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableValuedFunction(ArcticSparkSqlParser.TableValuedFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableValuedFunction(ArcticSparkSqlParser.TableValuedFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void enterInlineTable(ArcticSparkSqlParser.InlineTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void exitInlineTable(ArcticSparkSqlParser.InlineTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#functionTable}.
	 * @param ctx the parse tree
	 */
	void enterFunctionTable(ArcticSparkSqlParser.FunctionTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#functionTable}.
	 * @param ctx the parse tree
	 */
	void exitFunctionTable(ArcticSparkSqlParser.FunctionTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#tableAlias}.
	 * @param ctx the parse tree
	 */
	void enterTableAlias(ArcticSparkSqlParser.TableAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#tableAlias}.
	 * @param ctx the parse tree
	 */
	void exitTableAlias(ArcticSparkSqlParser.TableAliasContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void enterRowFormatSerde(ArcticSparkSqlParser.RowFormatSerdeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void exitRowFormatSerde(ArcticSparkSqlParser.RowFormatSerdeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void enterRowFormatDelimited(ArcticSparkSqlParser.RowFormatDelimitedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void exitRowFormatDelimited(ArcticSparkSqlParser.RowFormatDelimitedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterTableIdentifier(ArcticSparkSqlParser.TableIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitTableIdentifier(ArcticSparkSqlParser.TableIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#functionIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterFunctionIdentifier(ArcticSparkSqlParser.FunctionIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#functionIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitFunctionIdentifier(ArcticSparkSqlParser.FunctionIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#namedExpression}.
	 * @param ctx the parse tree
	 */
	void enterNamedExpression(ArcticSparkSqlParser.NamedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#namedExpression}.
	 * @param ctx the parse tree
	 */
	void exitNamedExpression(ArcticSparkSqlParser.NamedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 */
	void enterNamedExpressionSeq(ArcticSparkSqlParser.NamedExpressionSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 */
	void exitNamedExpressionSeq(ArcticSparkSqlParser.NamedExpressionSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ArcticSparkSqlParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ArcticSparkSqlParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalNot(ArcticSparkSqlParser.LogicalNotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalNot(ArcticSparkSqlParser.LogicalNotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanDefault(ArcticSparkSqlParser.BooleanDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanDefault(ArcticSparkSqlParser.BooleanDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterExists(ArcticSparkSqlParser.ExistsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitExists(ArcticSparkSqlParser.ExistsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalBinary(ArcticSparkSqlParser.LogicalBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalBinary(ArcticSparkSqlParser.LogicalBinaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#predicated}.
	 * @param ctx the parse tree
	 */
	void enterPredicated(ArcticSparkSqlParser.PredicatedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#predicated}.
	 * @param ctx the parse tree
	 */
	void exitPredicated(ArcticSparkSqlParser.PredicatedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(ArcticSparkSqlParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(ArcticSparkSqlParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterValueExpressionDefault(ArcticSparkSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitValueExpressionDefault(ArcticSparkSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterComparison(ArcticSparkSqlParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitComparison(ArcticSparkSqlParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticBinary(ArcticSparkSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticBinary(ArcticSparkSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticUnary(ArcticSparkSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticUnary(ArcticSparkSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStruct(ArcticSparkSqlParser.StructContext ctx);
	/**
	 * Exit a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStruct(ArcticSparkSqlParser.StructContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterDereference(ArcticSparkSqlParser.DereferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitDereference(ArcticSparkSqlParser.DereferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCase(ArcticSparkSqlParser.SimpleCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCase(ArcticSparkSqlParser.SimpleCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterColumnReference(ArcticSparkSqlParser.ColumnReferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitColumnReference(ArcticSparkSqlParser.ColumnReferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterRowConstructor(ArcticSparkSqlParser.RowConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitRowConstructor(ArcticSparkSqlParser.RowConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLast(ArcticSparkSqlParser.LastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLast(ArcticSparkSqlParser.LastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStar(ArcticSparkSqlParser.StarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStar(ArcticSparkSqlParser.StarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(ArcticSparkSqlParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(ArcticSparkSqlParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryExpression(ArcticSparkSqlParser.SubqueryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryExpression(ArcticSparkSqlParser.SubqueryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCast(ArcticSparkSqlParser.CastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCast(ArcticSparkSqlParser.CastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantDefault(ArcticSparkSqlParser.ConstantDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantDefault(ArcticSparkSqlParser.ConstantDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(ArcticSparkSqlParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(ArcticSparkSqlParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(ArcticSparkSqlParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(ArcticSparkSqlParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSearchedCase(ArcticSparkSqlParser.SearchedCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSearchedCase(ArcticSparkSqlParser.SearchedCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPosition(ArcticSparkSqlParser.PositionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPosition(ArcticSparkSqlParser.PositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFirst(ArcticSparkSqlParser.FirstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFirst(ArcticSparkSqlParser.FirstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNullLiteral(ArcticSparkSqlParser.NullLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNullLiteral(ArcticSparkSqlParser.NullLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterIntervalLiteral(ArcticSparkSqlParser.IntervalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitIntervalLiteral(ArcticSparkSqlParser.IntervalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterTypeConstructor(ArcticSparkSqlParser.TypeConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitTypeConstructor(ArcticSparkSqlParser.TypeConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(ArcticSparkSqlParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(ArcticSparkSqlParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(ArcticSparkSqlParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(ArcticSparkSqlParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(ArcticSparkSqlParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(ArcticSparkSqlParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(ArcticSparkSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(ArcticSparkSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticOperator(ArcticSparkSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticOperator(ArcticSparkSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#predicateOperator}.
	 * @param ctx the parse tree
	 */
	void enterPredicateOperator(ArcticSparkSqlParser.PredicateOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#predicateOperator}.
	 * @param ctx the parse tree
	 */
	void exitPredicateOperator(ArcticSparkSqlParser.PredicateOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(ArcticSparkSqlParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(ArcticSparkSqlParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#interval}.
	 * @param ctx the parse tree
	 */
	void enterInterval(ArcticSparkSqlParser.IntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#interval}.
	 * @param ctx the parse tree
	 */
	void exitInterval(ArcticSparkSqlParser.IntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#intervalField}.
	 * @param ctx the parse tree
	 */
	void enterIntervalField(ArcticSparkSqlParser.IntervalFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#intervalField}.
	 * @param ctx the parse tree
	 */
	void exitIntervalField(ArcticSparkSqlParser.IntervalFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#intervalValue}.
	 * @param ctx the parse tree
	 */
	void enterIntervalValue(ArcticSparkSqlParser.IntervalValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#intervalValue}.
	 * @param ctx the parse tree
	 */
	void exitIntervalValue(ArcticSparkSqlParser.IntervalValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#colPosition}.
	 * @param ctx the parse tree
	 */
	void enterColPosition(ArcticSparkSqlParser.ColPositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#colPosition}.
	 * @param ctx the parse tree
	 */
	void exitColPosition(ArcticSparkSqlParser.ColPositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterComplexDataType(ArcticSparkSqlParser.ComplexDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitComplexDataType(ArcticSparkSqlParser.ComplexDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveDataType(ArcticSparkSqlParser.PrimitiveDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveDataType(ArcticSparkSqlParser.PrimitiveDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void enterColTypeList(ArcticSparkSqlParser.ColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void exitColTypeList(ArcticSparkSqlParser.ColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#colType}.
	 * @param ctx the parse tree
	 */
	void enterColType(ArcticSparkSqlParser.ColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#colType}.
	 * @param ctx the parse tree
	 */
	void exitColType(ArcticSparkSqlParser.ColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#complexColTypeList}.
	 * @param ctx the parse tree
	 */
	void enterComplexColTypeList(ArcticSparkSqlParser.ComplexColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#complexColTypeList}.
	 * @param ctx the parse tree
	 */
	void exitComplexColTypeList(ArcticSparkSqlParser.ComplexColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#complexColType}.
	 * @param ctx the parse tree
	 */
	void enterComplexColType(ArcticSparkSqlParser.ComplexColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#complexColType}.
	 * @param ctx the parse tree
	 */
	void exitComplexColType(ArcticSparkSqlParser.ComplexColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(ArcticSparkSqlParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(ArcticSparkSqlParser.WhenClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#windows}.
	 * @param ctx the parse tree
	 */
	void enterWindows(ArcticSparkSqlParser.WindowsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#windows}.
	 * @param ctx the parse tree
	 */
	void exitWindows(ArcticSparkSqlParser.WindowsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#namedWindow}.
	 * @param ctx the parse tree
	 */
	void enterNamedWindow(ArcticSparkSqlParser.NamedWindowContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#namedWindow}.
	 * @param ctx the parse tree
	 */
	void exitNamedWindow(ArcticSparkSqlParser.NamedWindowContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void enterWindowRef(ArcticSparkSqlParser.WindowRefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void exitWindowRef(ArcticSparkSqlParser.WindowRefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void enterWindowDef(ArcticSparkSqlParser.WindowDefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void exitWindowDef(ArcticSparkSqlParser.WindowDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void enterWindowFrame(ArcticSparkSqlParser.WindowFrameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void exitWindowFrame(ArcticSparkSqlParser.WindowFrameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterFrameBound(ArcticSparkSqlParser.FrameBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitFrameBound(ArcticSparkSqlParser.FrameBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(ArcticSparkSqlParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(ArcticSparkSqlParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(ArcticSparkSqlParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(ArcticSparkSqlParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedIdentifier(ArcticSparkSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedIdentifier(ArcticSparkSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifierAlternative(ArcticSparkSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifierAlternative(ArcticSparkSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifier(ArcticSparkSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifier(ArcticSparkSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDecimalLiteral(ArcticSparkSqlParser.DecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDecimalLiteral(ArcticSparkSqlParser.DecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(ArcticSparkSqlParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(ArcticSparkSqlParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigIntLiteral(ArcticSparkSqlParser.BigIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigIntLiteral(ArcticSparkSqlParser.BigIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterSmallIntLiteral(ArcticSparkSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitSmallIntLiteral(ArcticSparkSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterTinyIntLiteral(ArcticSparkSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitTinyIntLiteral(ArcticSparkSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(ArcticSparkSqlParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(ArcticSparkSqlParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigDecimalLiteral(ArcticSparkSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigDecimalLiteral(ArcticSparkSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSparkSqlParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void enterNonReserved(ArcticSparkSqlParser.NonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSparkSqlParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void exitNonReserved(ArcticSparkSqlParser.NonReservedContext ctx);
}