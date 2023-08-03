// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.spark.sparksql.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SparkSqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SparkSqlParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleStatement(SparkSqlParser.SingleStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleExpression(SparkSqlParser.SingleExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleTableIdentifier(SparkSqlParser.SingleTableIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#singleMultipartIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleMultipartIdentifier(SparkSqlParser.SingleMultipartIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleFunctionIdentifier(SparkSqlParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#singleDataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleDataType(SparkSqlParser.SingleDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#singleTableSchema}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleTableSchema(SparkSqlParser.SingleTableSchemaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementDefault(SparkSqlParser.StatementDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dmlStatement}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDmlStatement(SparkSqlParser.DmlStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code use}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUse(SparkSqlParser.UseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code useNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUseNamespace(SparkSqlParser.UseNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setCatalog}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetCatalog(SparkSqlParser.SetCatalogContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateNamespace(SparkSqlParser.CreateNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setNamespaceProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetNamespaceProperties(SparkSqlParser.SetNamespacePropertiesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setNamespaceLocation}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetNamespaceLocation(SparkSqlParser.SetNamespaceLocationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropNamespace(SparkSqlParser.DropNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showNamespaces}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowNamespaces(SparkSqlParser.ShowNamespacesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTable(SparkSqlParser.CreateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTableLike}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableLike(SparkSqlParser.CreateTableLikeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code replaceTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceTable(SparkSqlParser.ReplaceTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyze(SparkSqlParser.AnalyzeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code analyzeTables}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyzeTables(SparkSqlParser.AnalyzeTablesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addTableColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddTableColumns(SparkSqlParser.AddTableColumnsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code renameTableColumn}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameTableColumn(SparkSqlParser.RenameTableColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropTableColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropTableColumns(SparkSqlParser.DropTableColumnsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameTable(SparkSqlParser.RenameTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code touchTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTouchTable(SparkSqlParser.TouchTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setTableProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetTableProperties(SparkSqlParser.SetTablePropertiesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unsetTableProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsetTableProperties(SparkSqlParser.UnsetTablePropertiesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterTableAlterColumn}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterTableAlterColumn(SparkSqlParser.AlterTableAlterColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code hiveChangeColumn}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHiveChangeColumn(SparkSqlParser.HiveChangeColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code hiveReplaceColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHiveReplaceColumns(SparkSqlParser.HiveReplaceColumnsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setTableSerDe}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetTableSerDe(SparkSqlParser.SetTableSerDeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addTablePartition}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddTablePartition(SparkSqlParser.AddTablePartitionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code renameTablePartition}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameTablePartition(SparkSqlParser.RenameTablePartitionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropTablePartitions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropTablePartitions(SparkSqlParser.DropTablePartitionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setTableLocation}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetTableLocation(SparkSqlParser.SetTableLocationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code recoverPartitions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecoverPartitions(SparkSqlParser.RecoverPartitionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropTable(SparkSqlParser.DropTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropView(SparkSqlParser.DropViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createView}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateView(SparkSqlParser.CreateViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTempViewUsing}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTempViewUsing(SparkSqlParser.CreateTempViewUsingContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterViewQuery}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterViewQuery(SparkSqlParser.AlterViewQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateFunction(SparkSqlParser.CreateFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropFunction(SparkSqlParser.DropFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code explain}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain(SparkSqlParser.ExplainContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowTables(SparkSqlParser.ShowTablesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showTableExtended}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowTableExtended(SparkSqlParser.ShowTableExtendedContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showTblProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowTblProperties(SparkSqlParser.ShowTblPropertiesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowColumns(SparkSqlParser.ShowColumnsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showViews}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowViews(SparkSqlParser.ShowViewsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showPartitions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowPartitions(SparkSqlParser.ShowPartitionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowFunctions(SparkSqlParser.ShowFunctionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowCreateTable(SparkSqlParser.ShowCreateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showCurrentNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowCurrentNamespace(SparkSqlParser.ShowCurrentNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showCatalogs}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowCatalogs(SparkSqlParser.ShowCatalogsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code describeFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeFunction(SparkSqlParser.DescribeFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code describeNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeNamespace(SparkSqlParser.DescribeNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code describeRelation}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeRelation(SparkSqlParser.DescribeRelationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code describeQuery}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeQuery(SparkSqlParser.DescribeQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code commentNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentNamespace(SparkSqlParser.CommentNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code commentTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentTable(SparkSqlParser.CommentTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code refreshTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefreshTable(SparkSqlParser.RefreshTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code refreshFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefreshFunction(SparkSqlParser.RefreshFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code refreshResource}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefreshResource(SparkSqlParser.RefreshResourceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cacheTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCacheTable(SparkSqlParser.CacheTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code uncacheTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUncacheTable(SparkSqlParser.UncacheTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code clearCache}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClearCache(SparkSqlParser.ClearCacheContext ctx);
	/**
	 * Visit a parse tree produced by the {@code loadData}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoadData(SparkSqlParser.LoadDataContext ctx);
	/**
	 * Visit a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTruncateTable(SparkSqlParser.TruncateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code repairTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRepairTable(SparkSqlParser.RepairTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code manageResource}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitManageResource(SparkSqlParser.ManageResourceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code failNativeCommand}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFailNativeCommand(SparkSqlParser.FailNativeCommandContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setTimeZone}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetTimeZone(SparkSqlParser.SetTimeZoneContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setQuotedConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetQuotedConfiguration(SparkSqlParser.SetQuotedConfigurationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetConfiguration(SparkSqlParser.SetConfigurationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code resetQuotedConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetQuotedConfiguration(SparkSqlParser.ResetQuotedConfigurationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code resetConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetConfiguration(SparkSqlParser.ResetConfigurationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createIndex}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateIndex(SparkSqlParser.CreateIndexContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropIndex}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropIndex(SparkSqlParser.DropIndexContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mergeFile}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeFile(SparkSqlParser.MergeFileContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createFileView}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateFileView(SparkSqlParser.CreateFileViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exportTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExportTable(SparkSqlParser.ExportTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code datatunnelExpr}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatunnelExpr(SparkSqlParser.DatatunnelExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code datatunnelHelp}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatunnelHelp(SparkSqlParser.DatatunnelHelpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code callHelp}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallHelp(SparkSqlParser.CallHelpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code call}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCall(SparkSqlParser.CallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sync}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSync(SparkSqlParser.SyncContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#timezone}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimezone(SparkSqlParser.TimezoneContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#configKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfigKey(SparkSqlParser.ConfigKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#configValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfigValue(SparkSqlParser.ConfigValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#callArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallArgument(SparkSqlParser.CallArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#callHelpExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallHelpExpr(SparkSqlParser.CallHelpExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#dtPropertyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtPropertyList(SparkSqlParser.DtPropertyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#columnDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnDef(SparkSqlParser.ColumnDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#dtProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtProperty(SparkSqlParser.DtPropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#dtColProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtColProperty(SparkSqlParser.DtColPropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#dtPropertyKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtPropertyKey(SparkSqlParser.DtPropertyKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#dtPropertyValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtPropertyValue(SparkSqlParser.DtPropertyValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#createFileViewClauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateFileViewClauses(SparkSqlParser.CreateFileViewClausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#exportTableClauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExportTableClauses(SparkSqlParser.ExportTableClausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsupportedHiveNativeCommands(SparkSqlParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#createTableHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableHeader(SparkSqlParser.CreateTableHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#replaceTableHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceTableHeader(SparkSqlParser.ReplaceTableHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#bucketSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBucketSpec(SparkSqlParser.BucketSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#skewSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkewSpec(SparkSqlParser.SkewSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#locationSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocationSpec(SparkSqlParser.LocationSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#commentSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentSpec(SparkSqlParser.CommentSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(SparkSqlParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertOverwriteTable(SparkSqlParser.InsertOverwriteTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertIntoTable(SparkSqlParser.InsertIntoTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertIntoReplaceWhere}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertIntoReplaceWhere(SparkSqlParser.InsertIntoReplaceWhereContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertOverwriteHiveDir(SparkSqlParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertOverwriteDir(SparkSqlParser.InsertOverwriteDirContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionSpecLocation(SparkSqlParser.PartitionSpecLocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#partitionSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionSpec(SparkSqlParser.PartitionSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#partitionVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionVal(SparkSqlParser.PartitionValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#namespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace(SparkSqlParser.NamespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#namespaces}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespaces(SparkSqlParser.NamespacesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#describeFuncName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeFuncName(SparkSqlParser.DescribeFuncNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#describeColName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeColName(SparkSqlParser.DescribeColNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#ctes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCtes(SparkSqlParser.CtesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#namedQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedQuery(SparkSqlParser.NamedQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#tableProvider}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableProvider(SparkSqlParser.TableProviderContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#createTableClauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableClauses(SparkSqlParser.CreateTableClausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#propertyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyList(SparkSqlParser.PropertyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#property}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProperty(SparkSqlParser.PropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#propertyKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyKey(SparkSqlParser.PropertyKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#propertyValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyValue(SparkSqlParser.PropertyValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#constantList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantList(SparkSqlParser.ConstantListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#nestedConstantList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNestedConstantList(SparkSqlParser.NestedConstantListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#createFileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateFileFormat(SparkSqlParser.CreateFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link SparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableFileFormat(SparkSqlParser.TableFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link SparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericFileFormat(SparkSqlParser.GenericFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#storageHandler}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorageHandler(SparkSqlParser.StorageHandlerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#resource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource(SparkSqlParser.ResourceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleInsertQuery(SparkSqlParser.SingleInsertQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiInsertQuery(SparkSqlParser.MultiInsertQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code deleteFromTable}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeleteFromTable(SparkSqlParser.DeleteFromTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code updateTable}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdateTable(SparkSqlParser.UpdateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mergeIntoTable}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeIntoTable(SparkSqlParser.MergeIntoTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#queryOrganization}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryOrganization(SparkSqlParser.QueryOrganizationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiInsertQueryBody(SparkSqlParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link SparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryTermDefault(SparkSqlParser.QueryTermDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link SparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetOperation(SparkSqlParser.SetOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryPrimaryDefault(SparkSqlParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromStmt(SparkSqlParser.FromStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable(SparkSqlParser.TableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTableDefault1(SparkSqlParser.InlineTableDefault1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery(SparkSqlParser.SubqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#sortItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortItem(SparkSqlParser.SortItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#fromStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromStatement(SparkSqlParser.FromStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#fromStatementBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromStatementBody(SparkSqlParser.FromStatementBodyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link SparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransformQuerySpecification(SparkSqlParser.TransformQuerySpecificationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link SparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegularQuerySpecification(SparkSqlParser.RegularQuerySpecificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#transformClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransformClause(SparkSqlParser.TransformClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#selectClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectClause(SparkSqlParser.SelectClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#setClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetClause(SparkSqlParser.SetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#matchedClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatchedClause(SparkSqlParser.MatchedClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#notMatchedClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotMatchedClause(SparkSqlParser.NotMatchedClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#notMatchedBySourceClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotMatchedBySourceClause(SparkSqlParser.NotMatchedBySourceClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#matchedAction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatchedAction(SparkSqlParser.MatchedActionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#notMatchedAction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotMatchedAction(SparkSqlParser.NotMatchedActionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#notMatchedBySourceAction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotMatchedBySourceAction(SparkSqlParser.NotMatchedBySourceActionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#assignmentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentList(SparkSqlParser.AssignmentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(SparkSqlParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#whereClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhereClause(SparkSqlParser.WhereClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#havingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHavingClause(SparkSqlParser.HavingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#hint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHint(SparkSqlParser.HintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#hintStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHintStatement(SparkSqlParser.HintStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#fromClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromClause(SparkSqlParser.FromClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#temporalClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemporalClause(SparkSqlParser.TemporalClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#aggregationClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregationClause(SparkSqlParser.AggregationClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#groupByClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupByClause(SparkSqlParser.GroupByClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingAnalytics(SparkSqlParser.GroupingAnalyticsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#groupingElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingElement(SparkSqlParser.GroupingElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#groupingSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingSet(SparkSqlParser.GroupingSetContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#pivotClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivotClause(SparkSqlParser.PivotClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#pivotColumn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivotColumn(SparkSqlParser.PivotColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#pivotValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivotValue(SparkSqlParser.PivotValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotClause(SparkSqlParser.UnpivotClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotNullClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotNullClause(SparkSqlParser.UnpivotNullClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotOperator(SparkSqlParser.UnpivotOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotSingleValueColumnClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotSingleValueColumnClause(SparkSqlParser.UnpivotSingleValueColumnClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotMultiValueColumnClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotMultiValueColumnClause(SparkSqlParser.UnpivotMultiValueColumnClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotColumnSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotColumnSet(SparkSqlParser.UnpivotColumnSetContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotValueColumn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotValueColumn(SparkSqlParser.UnpivotValueColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotNameColumn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotNameColumn(SparkSqlParser.UnpivotNameColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotColumnAndAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotColumnAndAlias(SparkSqlParser.UnpivotColumnAndAliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotColumn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotColumn(SparkSqlParser.UnpivotColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unpivotAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivotAlias(SparkSqlParser.UnpivotAliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#lateralView}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLateralView(SparkSqlParser.LateralViewContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetQuantifier(SparkSqlParser.SetQuantifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#relation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelation(SparkSqlParser.RelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#relationExtension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationExtension(SparkSqlParser.RelationExtensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#joinRelation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinRelation(SparkSqlParser.JoinRelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#joinType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinType(SparkSqlParser.JoinTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#joinCriteria}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinCriteria(SparkSqlParser.JoinCriteriaContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#sample}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSample(SparkSqlParser.SampleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByPercentile(SparkSqlParser.SampleByPercentileContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByRows(SparkSqlParser.SampleByRowsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByBucket(SparkSqlParser.SampleByBucketContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByBytes(SparkSqlParser.SampleByBytesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#identifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierList(SparkSqlParser.IdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierSeq(SparkSqlParser.IdentifierSeqContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderedIdentifierList(SparkSqlParser.OrderedIdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderedIdentifier(SparkSqlParser.OrderedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#identifierCommentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierCommentList(SparkSqlParser.IdentifierCommentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#identifierComment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierComment(SparkSqlParser.IdentifierCommentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableName(SparkSqlParser.TableNameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedQuery(SparkSqlParser.AliasedQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedRelation(SparkSqlParser.AliasedRelationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTableDefault2(SparkSqlParser.InlineTableDefault2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableValuedFunction(SparkSqlParser.TableValuedFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#inlineTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTable(SparkSqlParser.InlineTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#functionTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTable(SparkSqlParser.FunctionTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#tableAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableAlias(SparkSqlParser.TableAliasContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link SparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowFormatSerde(SparkSqlParser.RowFormatSerdeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link SparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowFormatDelimited(SparkSqlParser.RowFormatDelimitedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#multipartIdentifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipartIdentifierList(SparkSqlParser.MultipartIdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipartIdentifier(SparkSqlParser.MultipartIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#multipartIdentifierPropertyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipartIdentifierPropertyList(SparkSqlParser.MultipartIdentifierPropertyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#multipartIdentifierProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipartIdentifierProperty(SparkSqlParser.MultipartIdentifierPropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableIdentifier(SparkSqlParser.TableIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#functionIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionIdentifier(SparkSqlParser.FunctionIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#namedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedExpression(SparkSqlParser.NamedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedExpressionSeq(SparkSqlParser.NamedExpressionSeqContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#partitionFieldList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionFieldList(SparkSqlParser.PartitionFieldListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link SparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionTransform(SparkSqlParser.PartitionTransformContext ctx);
	/**
	 * Visit a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link SparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionColumn(SparkSqlParser.PartitionColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link SparkSqlParser#transform}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentityTransform(SparkSqlParser.IdentityTransformContext ctx);
	/**
	 * Visit a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link SparkSqlParser#transform}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApplyTransform(SparkSqlParser.ApplyTransformContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#transformArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransformArgument(SparkSqlParser.TransformArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(SparkSqlParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#expressionSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionSeq(SparkSqlParser.ExpressionSeqContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalNot(SparkSqlParser.LogicalNotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicated(SparkSqlParser.PredicatedContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExists(SparkSqlParser.ExistsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalBinary(SparkSqlParser.LogicalBinaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicate(SparkSqlParser.PredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueExpressionDefault(SparkSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison(SparkSqlParser.ComparisonContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticBinary(SparkSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticUnary(SparkSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#datetimeUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatetimeUnit(SparkSqlParser.DatetimeUnitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code struct}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct(SparkSqlParser.StructContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDereference(SparkSqlParser.DereferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code timestampadd}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimestampadd(SparkSqlParser.TimestampaddContext ctx);
	/**
	 * Visit a parse tree produced by the {@code substring}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubstring(SparkSqlParser.SubstringContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast(SparkSqlParser.CastContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda(SparkSqlParser.LambdaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpression(SparkSqlParser.ParenthesizedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code any_value}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAny_value(SparkSqlParser.Any_valueContext ctx);
	/**
	 * Visit a parse tree produced by the {@code trim}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrim(SparkSqlParser.TrimContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleCase(SparkSqlParser.SimpleCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCurrentLike(SparkSqlParser.CurrentLikeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnReference(SparkSqlParser.ColumnReferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowConstructor(SparkSqlParser.RowConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code last}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLast(SparkSqlParser.LastContext ctx);
	/**
	 * Visit a parse tree produced by the {@code star}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar(SparkSqlParser.StarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverlay(SparkSqlParser.OverlayContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscript(SparkSqlParser.SubscriptContext ctx);
	/**
	 * Visit a parse tree produced by the {@code timestampdiff}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimestampdiff(SparkSqlParser.TimestampdiffContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubqueryExpression(SparkSqlParser.SubqueryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDefault(SparkSqlParser.ConstantDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtract(SparkSqlParser.ExtractContext ctx);
	/**
	 * Visit a parse tree produced by the {@code percentile}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPercentile(SparkSqlParser.PercentileContext ctx);
	/**
	 * Visit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(SparkSqlParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearchedCase(SparkSqlParser.SearchedCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code position}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPosition(SparkSqlParser.PositionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code first}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFirst(SparkSqlParser.FirstContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullLiteral(SparkSqlParser.NullLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parameterLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterLiteral(SparkSqlParser.ParameterLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalLiteral(SparkSqlParser.IntervalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeConstructor(SparkSqlParser.TypeConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(SparkSqlParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(SparkSqlParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(SparkSqlParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOperator(SparkSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticOperator(SparkSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#predicateOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicateOperator(SparkSqlParser.PredicateOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanValue(SparkSqlParser.BooleanValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#interval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval(SparkSqlParser.IntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorCapturingMultiUnitsInterval(SparkSqlParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiUnitsInterval(SparkSqlParser.MultiUnitsIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorCapturingUnitToUnitInterval(SparkSqlParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitToUnitInterval(SparkSqlParser.UnitToUnitIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#intervalValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalValue(SparkSqlParser.IntervalValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unitInMultiUnits}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitInMultiUnits(SparkSqlParser.UnitInMultiUnitsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#unitInUnitToUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitInUnitToUnit(SparkSqlParser.UnitInUnitToUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#colPosition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColPosition(SparkSqlParser.ColPositionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexDataType(SparkSqlParser.ComplexDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYearMonthIntervalDataType(SparkSqlParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDayTimeIntervalDataType(SparkSqlParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveDataType(SparkSqlParser.PrimitiveDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#qualifiedColTypeWithPositionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedColTypeWithPositionList(SparkSqlParser.QualifiedColTypeWithPositionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#qualifiedColTypeWithPosition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedColTypeWithPosition(SparkSqlParser.QualifiedColTypeWithPositionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#defaultExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultExpression(SparkSqlParser.DefaultExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColTypeList(SparkSqlParser.ColTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#colType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColType(SparkSqlParser.ColTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#createOrReplaceTableColTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateOrReplaceTableColTypeList(SparkSqlParser.CreateOrReplaceTableColTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#createOrReplaceTableColType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateOrReplaceTableColType(SparkSqlParser.CreateOrReplaceTableColTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#colDefinitionOption}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColDefinitionOption(SparkSqlParser.ColDefinitionOptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#generationExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenerationExpression(SparkSqlParser.GenerationExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#complexColTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexColTypeList(SparkSqlParser.ComplexColTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#complexColType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexColType(SparkSqlParser.ComplexColTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#whenClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenClause(SparkSqlParser.WhenClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#windowClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowClause(SparkSqlParser.WindowClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#namedWindow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedWindow(SparkSqlParser.NamedWindowContext ctx);
	/**
	 * Visit a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link SparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowRef(SparkSqlParser.WindowRefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link SparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowDef(SparkSqlParser.WindowDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#windowFrame}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowFrame(SparkSqlParser.WindowFrameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#frameBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrameBound(SparkSqlParser.FrameBoundContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedNameList(SparkSqlParser.QualifiedNameListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#functionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionName(SparkSqlParser.FunctionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(SparkSqlParser.QualifiedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorCapturingIdentifier(SparkSqlParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link SparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorIdent(SparkSqlParser.ErrorIdentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link SparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRealIdent(SparkSqlParser.RealIdentContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(SparkSqlParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link SparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnquotedIdentifier(SparkSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link SparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifierAlternative(SparkSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifier(SparkSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#backQuotedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBackQuotedIdentifier(SparkSqlParser.BackQuotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExponentLiteral(SparkSqlParser.ExponentLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecimalLiteral(SparkSqlParser.DecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLegacyDecimalLiteral(SparkSqlParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerLiteral(SparkSqlParser.IntegerLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigIntLiteral(SparkSqlParser.BigIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSmallIntLiteral(SparkSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTinyIntLiteral(SparkSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleLiteral(SparkSqlParser.DoubleLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatLiteral(SparkSqlParser.FloatLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigDecimalLiteral(SparkSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#alterColumnAction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterColumnAction(SparkSqlParser.AlterColumnActionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#stringLit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLit(SparkSqlParser.StringLitContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#comment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment(SparkSqlParser.CommentContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#version}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVersion(SparkSqlParser.VersionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnsiNonReserved(SparkSqlParser.AnsiNonReservedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#strictNonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStrictNonReserved(SparkSqlParser.StrictNonReservedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkSqlParser#nonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonReserved(SparkSqlParser.NonReservedContext ctx);
}