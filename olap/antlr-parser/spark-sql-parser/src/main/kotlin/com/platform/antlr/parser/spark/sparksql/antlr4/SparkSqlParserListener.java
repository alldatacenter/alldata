// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.spark.sparksql.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SparkSqlParser}.
 */
public interface SparkSqlParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void enterSingleStatement(SparkSqlParser.SingleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void exitSingleStatement(SparkSqlParser.SingleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterSingleExpression(SparkSqlParser.SingleExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitSingleExpression(SparkSqlParser.SingleExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleTableIdentifier(SparkSqlParser.SingleTableIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleTableIdentifier(SparkSqlParser.SingleTableIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#singleMultipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleMultipartIdentifier(SparkSqlParser.SingleMultipartIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#singleMultipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleMultipartIdentifier(SparkSqlParser.SingleMultipartIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleFunctionIdentifier(SparkSqlParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleFunctionIdentifier(SparkSqlParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#singleDataType}.
	 * @param ctx the parse tree
	 */
	void enterSingleDataType(SparkSqlParser.SingleDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#singleDataType}.
	 * @param ctx the parse tree
	 */
	void exitSingleDataType(SparkSqlParser.SingleDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#singleTableSchema}.
	 * @param ctx the parse tree
	 */
	void enterSingleTableSchema(SparkSqlParser.SingleTableSchemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#singleTableSchema}.
	 * @param ctx the parse tree
	 */
	void exitSingleTableSchema(SparkSqlParser.SingleTableSchemaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatementDefault(SparkSqlParser.StatementDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatementDefault(SparkSqlParser.StatementDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dmlStatement}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDmlStatement(SparkSqlParser.DmlStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dmlStatement}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDmlStatement(SparkSqlParser.DmlStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code use}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUse(SparkSqlParser.UseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code use}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUse(SparkSqlParser.UseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code useNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUseNamespace(SparkSqlParser.UseNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code useNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUseNamespace(SparkSqlParser.UseNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setCatalog}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetCatalog(SparkSqlParser.SetCatalogContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setCatalog}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetCatalog(SparkSqlParser.SetCatalogContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateNamespace(SparkSqlParser.CreateNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateNamespace(SparkSqlParser.CreateNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNamespaceProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetNamespaceProperties(SparkSqlParser.SetNamespacePropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNamespaceProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetNamespaceProperties(SparkSqlParser.SetNamespacePropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNamespaceLocation}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetNamespaceLocation(SparkSqlParser.SetNamespaceLocationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNamespaceLocation}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetNamespaceLocation(SparkSqlParser.SetNamespaceLocationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropNamespace(SparkSqlParser.DropNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropNamespace(SparkSqlParser.DropNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showNamespaces}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowNamespaces(SparkSqlParser.ShowNamespacesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showNamespaces}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowNamespaces(SparkSqlParser.ShowNamespacesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTable(SparkSqlParser.CreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTable(SparkSqlParser.CreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTableLike}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableLike(SparkSqlParser.CreateTableLikeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTableLike}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableLike(SparkSqlParser.CreateTableLikeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code replaceTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterReplaceTable(SparkSqlParser.ReplaceTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code replaceTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitReplaceTable(SparkSqlParser.ReplaceTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze(SparkSqlParser.AnalyzeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze(SparkSqlParser.AnalyzeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code analyzeTables}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyzeTables(SparkSqlParser.AnalyzeTablesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code analyzeTables}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyzeTables(SparkSqlParser.AnalyzeTablesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addTableColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAddTableColumns(SparkSqlParser.AddTableColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addTableColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAddTableColumns(SparkSqlParser.AddTableColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTableColumn}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTableColumn(SparkSqlParser.RenameTableColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTableColumn}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTableColumn(SparkSqlParser.RenameTableColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTableColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTableColumns(SparkSqlParser.DropTableColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTableColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTableColumns(SparkSqlParser.DropTableColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTable(SparkSqlParser.RenameTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTable(SparkSqlParser.RenameTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code touchTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterTouchTable(SparkSqlParser.TouchTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code touchTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitTouchTable(SparkSqlParser.TouchTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTableProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTableProperties(SparkSqlParser.SetTablePropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTableProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTableProperties(SparkSqlParser.SetTablePropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unsetTableProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUnsetTableProperties(SparkSqlParser.UnsetTablePropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unsetTableProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUnsetTableProperties(SparkSqlParser.UnsetTablePropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterTableAlterColumn}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterTableAlterColumn(SparkSqlParser.AlterTableAlterColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterTableAlterColumn}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterTableAlterColumn(SparkSqlParser.AlterTableAlterColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hiveChangeColumn}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterHiveChangeColumn(SparkSqlParser.HiveChangeColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hiveChangeColumn}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitHiveChangeColumn(SparkSqlParser.HiveChangeColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hiveReplaceColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterHiveReplaceColumns(SparkSqlParser.HiveReplaceColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hiveReplaceColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitHiveReplaceColumns(SparkSqlParser.HiveReplaceColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTableSerDe}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTableSerDe(SparkSqlParser.SetTableSerDeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTableSerDe}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTableSerDe(SparkSqlParser.SetTableSerDeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addTablePartition}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAddTablePartition(SparkSqlParser.AddTablePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addTablePartition}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAddTablePartition(SparkSqlParser.AddTablePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTablePartition}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTablePartition(SparkSqlParser.RenameTablePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTablePartition}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTablePartition(SparkSqlParser.RenameTablePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTablePartitions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTablePartitions(SparkSqlParser.DropTablePartitionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTablePartitions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTablePartitions(SparkSqlParser.DropTablePartitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTableLocation}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTableLocation(SparkSqlParser.SetTableLocationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTableLocation}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTableLocation(SparkSqlParser.SetTableLocationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code recoverPartitions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRecoverPartitions(SparkSqlParser.RecoverPartitionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code recoverPartitions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRecoverPartitions(SparkSqlParser.RecoverPartitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTable(SparkSqlParser.DropTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTable(SparkSqlParser.DropTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropView(SparkSqlParser.DropViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropView(SparkSqlParser.DropViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createView}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateView(SparkSqlParser.CreateViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createView}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateView(SparkSqlParser.CreateViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTempViewUsing}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTempViewUsing(SparkSqlParser.CreateTempViewUsingContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTempViewUsing}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTempViewUsing(SparkSqlParser.CreateTempViewUsingContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterViewQuery}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterViewQuery(SparkSqlParser.AlterViewQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterViewQuery}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterViewQuery(SparkSqlParser.AlterViewQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateFunction(SparkSqlParser.CreateFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateFunction(SparkSqlParser.CreateFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropFunction(SparkSqlParser.DropFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropFunction(SparkSqlParser.DropFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code explain}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterExplain(SparkSqlParser.ExplainContext ctx);
	/**
	 * Exit a parse tree produced by the {@code explain}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitExplain(SparkSqlParser.ExplainContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTables(SparkSqlParser.ShowTablesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTables(SparkSqlParser.ShowTablesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTableExtended}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTableExtended(SparkSqlParser.ShowTableExtendedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTableExtended}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTableExtended(SparkSqlParser.ShowTableExtendedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTblProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTblProperties(SparkSqlParser.ShowTblPropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTblProperties}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTblProperties(SparkSqlParser.ShowTblPropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowColumns(SparkSqlParser.ShowColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowColumns(SparkSqlParser.ShowColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showViews}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowViews(SparkSqlParser.ShowViewsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showViews}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowViews(SparkSqlParser.ShowViewsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showPartitions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowPartitions(SparkSqlParser.ShowPartitionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showPartitions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowPartitions(SparkSqlParser.ShowPartitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowFunctions(SparkSqlParser.ShowFunctionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowFunctions(SparkSqlParser.ShowFunctionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateTable(SparkSqlParser.ShowCreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateTable(SparkSqlParser.ShowCreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCurrentNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCurrentNamespace(SparkSqlParser.ShowCurrentNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCurrentNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCurrentNamespace(SparkSqlParser.ShowCurrentNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCatalogs}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCatalogs(SparkSqlParser.ShowCatalogsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCatalogs}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCatalogs(SparkSqlParser.ShowCatalogsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeFunction(SparkSqlParser.DescribeFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeFunction(SparkSqlParser.DescribeFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeNamespace(SparkSqlParser.DescribeNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeNamespace(SparkSqlParser.DescribeNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeRelation}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeRelation(SparkSqlParser.DescribeRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeRelation}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeRelation(SparkSqlParser.DescribeRelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeQuery}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeQuery(SparkSqlParser.DescribeQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeQuery}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeQuery(SparkSqlParser.DescribeQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code commentNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCommentNamespace(SparkSqlParser.CommentNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code commentNamespace}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCommentNamespace(SparkSqlParser.CommentNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code commentTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCommentTable(SparkSqlParser.CommentTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code commentTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCommentTable(SparkSqlParser.CommentTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshTable(SparkSqlParser.RefreshTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshTable(SparkSqlParser.RefreshTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshFunction(SparkSqlParser.RefreshFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshFunction}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshFunction(SparkSqlParser.RefreshFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshResource}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshResource(SparkSqlParser.RefreshResourceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshResource}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshResource(SparkSqlParser.RefreshResourceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cacheTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCacheTable(SparkSqlParser.CacheTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cacheTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCacheTable(SparkSqlParser.CacheTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uncacheTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUncacheTable(SparkSqlParser.UncacheTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uncacheTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUncacheTable(SparkSqlParser.UncacheTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code clearCache}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterClearCache(SparkSqlParser.ClearCacheContext ctx);
	/**
	 * Exit a parse tree produced by the {@code clearCache}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitClearCache(SparkSqlParser.ClearCacheContext ctx);
	/**
	 * Enter a parse tree produced by the {@code loadData}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterLoadData(SparkSqlParser.LoadDataContext ctx);
	/**
	 * Exit a parse tree produced by the {@code loadData}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitLoadData(SparkSqlParser.LoadDataContext ctx);
	/**
	 * Enter a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterTruncateTable(SparkSqlParser.TruncateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitTruncateTable(SparkSqlParser.TruncateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code repairTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRepairTable(SparkSqlParser.RepairTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code repairTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRepairTable(SparkSqlParser.RepairTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code manageResource}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterManageResource(SparkSqlParser.ManageResourceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code manageResource}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitManageResource(SparkSqlParser.ManageResourceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code failNativeCommand}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterFailNativeCommand(SparkSqlParser.FailNativeCommandContext ctx);
	/**
	 * Exit a parse tree produced by the {@code failNativeCommand}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitFailNativeCommand(SparkSqlParser.FailNativeCommandContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTimeZone}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTimeZone(SparkSqlParser.SetTimeZoneContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTimeZone}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTimeZone(SparkSqlParser.SetTimeZoneContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setQuotedConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetQuotedConfiguration(SparkSqlParser.SetQuotedConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setQuotedConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetQuotedConfiguration(SparkSqlParser.SetQuotedConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetConfiguration(SparkSqlParser.SetConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetConfiguration(SparkSqlParser.SetConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resetQuotedConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterResetQuotedConfiguration(SparkSqlParser.ResetQuotedConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resetQuotedConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitResetQuotedConfiguration(SparkSqlParser.ResetQuotedConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resetConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterResetConfiguration(SparkSqlParser.ResetConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resetConfiguration}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitResetConfiguration(SparkSqlParser.ResetConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createIndex}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateIndex(SparkSqlParser.CreateIndexContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createIndex}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateIndex(SparkSqlParser.CreateIndexContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropIndex}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropIndex(SparkSqlParser.DropIndexContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropIndex}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropIndex(SparkSqlParser.DropIndexContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mergeFile}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterMergeFile(SparkSqlParser.MergeFileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mergeFile}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitMergeFile(SparkSqlParser.MergeFileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createFileView}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateFileView(SparkSqlParser.CreateFileViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createFileView}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateFileView(SparkSqlParser.CreateFileViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exportTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterExportTable(SparkSqlParser.ExportTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exportTable}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitExportTable(SparkSqlParser.ExportTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code datatunnelExpr}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDatatunnelExpr(SparkSqlParser.DatatunnelExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code datatunnelExpr}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDatatunnelExpr(SparkSqlParser.DatatunnelExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code datatunnelHelp}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDatatunnelHelp(SparkSqlParser.DatatunnelHelpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code datatunnelHelp}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDatatunnelHelp(SparkSqlParser.DatatunnelHelpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code callHelp}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCallHelp(SparkSqlParser.CallHelpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code callHelp}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCallHelp(SparkSqlParser.CallHelpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code call}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCall(SparkSqlParser.CallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code call}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCall(SparkSqlParser.CallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sync}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSync(SparkSqlParser.SyncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sync}
	 * labeled alternative in {@link SparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSync(SparkSqlParser.SyncContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#timezone}.
	 * @param ctx the parse tree
	 */
	void enterTimezone(SparkSqlParser.TimezoneContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#timezone}.
	 * @param ctx the parse tree
	 */
	void exitTimezone(SparkSqlParser.TimezoneContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#configKey}.
	 * @param ctx the parse tree
	 */
	void enterConfigKey(SparkSqlParser.ConfigKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#configKey}.
	 * @param ctx the parse tree
	 */
	void exitConfigKey(SparkSqlParser.ConfigKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#configValue}.
	 * @param ctx the parse tree
	 */
	void enterConfigValue(SparkSqlParser.ConfigValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#configValue}.
	 * @param ctx the parse tree
	 */
	void exitConfigValue(SparkSqlParser.ConfigValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#callArgument}.
	 * @param ctx the parse tree
	 */
	void enterCallArgument(SparkSqlParser.CallArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#callArgument}.
	 * @param ctx the parse tree
	 */
	void exitCallArgument(SparkSqlParser.CallArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#callHelpExpr}.
	 * @param ctx the parse tree
	 */
	void enterCallHelpExpr(SparkSqlParser.CallHelpExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#callHelpExpr}.
	 * @param ctx the parse tree
	 */
	void exitCallHelpExpr(SparkSqlParser.CallHelpExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#dtPropertyList}.
	 * @param ctx the parse tree
	 */
	void enterDtPropertyList(SparkSqlParser.DtPropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#dtPropertyList}.
	 * @param ctx the parse tree
	 */
	void exitDtPropertyList(SparkSqlParser.DtPropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#columnDef}.
	 * @param ctx the parse tree
	 */
	void enterColumnDef(SparkSqlParser.ColumnDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#columnDef}.
	 * @param ctx the parse tree
	 */
	void exitColumnDef(SparkSqlParser.ColumnDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#dtProperty}.
	 * @param ctx the parse tree
	 */
	void enterDtProperty(SparkSqlParser.DtPropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#dtProperty}.
	 * @param ctx the parse tree
	 */
	void exitDtProperty(SparkSqlParser.DtPropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#dtColProperty}.
	 * @param ctx the parse tree
	 */
	void enterDtColProperty(SparkSqlParser.DtColPropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#dtColProperty}.
	 * @param ctx the parse tree
	 */
	void exitDtColProperty(SparkSqlParser.DtColPropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#dtPropertyKey}.
	 * @param ctx the parse tree
	 */
	void enterDtPropertyKey(SparkSqlParser.DtPropertyKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#dtPropertyKey}.
	 * @param ctx the parse tree
	 */
	void exitDtPropertyKey(SparkSqlParser.DtPropertyKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#dtPropertyValue}.
	 * @param ctx the parse tree
	 */
	void enterDtPropertyValue(SparkSqlParser.DtPropertyValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#dtPropertyValue}.
	 * @param ctx the parse tree
	 */
	void exitDtPropertyValue(SparkSqlParser.DtPropertyValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#createFileViewClauses}.
	 * @param ctx the parse tree
	 */
	void enterCreateFileViewClauses(SparkSqlParser.CreateFileViewClausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#createFileViewClauses}.
	 * @param ctx the parse tree
	 */
	void exitCreateFileViewClauses(SparkSqlParser.CreateFileViewClausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#exportTableClauses}.
	 * @param ctx the parse tree
	 */
	void enterExportTableClauses(SparkSqlParser.ExportTableClausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#exportTableClauses}.
	 * @param ctx the parse tree
	 */
	void exitExportTableClauses(SparkSqlParser.ExportTableClausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 */
	void enterUnsupportedHiveNativeCommands(SparkSqlParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 */
	void exitUnsupportedHiveNativeCommands(SparkSqlParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#createTableHeader}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableHeader(SparkSqlParser.CreateTableHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#createTableHeader}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableHeader(SparkSqlParser.CreateTableHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#replaceTableHeader}.
	 * @param ctx the parse tree
	 */
	void enterReplaceTableHeader(SparkSqlParser.ReplaceTableHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#replaceTableHeader}.
	 * @param ctx the parse tree
	 */
	void exitReplaceTableHeader(SparkSqlParser.ReplaceTableHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#bucketSpec}.
	 * @param ctx the parse tree
	 */
	void enterBucketSpec(SparkSqlParser.BucketSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#bucketSpec}.
	 * @param ctx the parse tree
	 */
	void exitBucketSpec(SparkSqlParser.BucketSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#skewSpec}.
	 * @param ctx the parse tree
	 */
	void enterSkewSpec(SparkSqlParser.SkewSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#skewSpec}.
	 * @param ctx the parse tree
	 */
	void exitSkewSpec(SparkSqlParser.SkewSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#locationSpec}.
	 * @param ctx the parse tree
	 */
	void enterLocationSpec(SparkSqlParser.LocationSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#locationSpec}.
	 * @param ctx the parse tree
	 */
	void exitLocationSpec(SparkSqlParser.LocationSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#commentSpec}.
	 * @param ctx the parse tree
	 */
	void enterCommentSpec(SparkSqlParser.CommentSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#commentSpec}.
	 * @param ctx the parse tree
	 */
	void exitCommentSpec(SparkSqlParser.CommentSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(SparkSqlParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(SparkSqlParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteTable(SparkSqlParser.InsertOverwriteTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteTable(SparkSqlParser.InsertOverwriteTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertIntoTable(SparkSqlParser.InsertIntoTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertIntoTable(SparkSqlParser.InsertIntoTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertIntoReplaceWhere}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertIntoReplaceWhere(SparkSqlParser.InsertIntoReplaceWhereContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertIntoReplaceWhere}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertIntoReplaceWhere(SparkSqlParser.InsertIntoReplaceWhereContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteHiveDir(SparkSqlParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteHiveDir(SparkSqlParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteDir(SparkSqlParser.InsertOverwriteDirContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link SparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteDir(SparkSqlParser.InsertOverwriteDirContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 */
	void enterPartitionSpecLocation(SparkSqlParser.PartitionSpecLocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 */
	void exitPartitionSpecLocation(SparkSqlParser.PartitionSpecLocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#partitionSpec}.
	 * @param ctx the parse tree
	 */
	void enterPartitionSpec(SparkSqlParser.PartitionSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#partitionSpec}.
	 * @param ctx the parse tree
	 */
	void exitPartitionSpec(SparkSqlParser.PartitionSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#partitionVal}.
	 * @param ctx the parse tree
	 */
	void enterPartitionVal(SparkSqlParser.PartitionValContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#partitionVal}.
	 * @param ctx the parse tree
	 */
	void exitPartitionVal(SparkSqlParser.PartitionValContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#namespace}.
	 * @param ctx the parse tree
	 */
	void enterNamespace(SparkSqlParser.NamespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#namespace}.
	 * @param ctx the parse tree
	 */
	void exitNamespace(SparkSqlParser.NamespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#namespaces}.
	 * @param ctx the parse tree
	 */
	void enterNamespaces(SparkSqlParser.NamespacesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#namespaces}.
	 * @param ctx the parse tree
	 */
	void exitNamespaces(SparkSqlParser.NamespacesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#describeFuncName}.
	 * @param ctx the parse tree
	 */
	void enterDescribeFuncName(SparkSqlParser.DescribeFuncNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#describeFuncName}.
	 * @param ctx the parse tree
	 */
	void exitDescribeFuncName(SparkSqlParser.DescribeFuncNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#describeColName}.
	 * @param ctx the parse tree
	 */
	void enterDescribeColName(SparkSqlParser.DescribeColNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#describeColName}.
	 * @param ctx the parse tree
	 */
	void exitDescribeColName(SparkSqlParser.DescribeColNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#ctes}.
	 * @param ctx the parse tree
	 */
	void enterCtes(SparkSqlParser.CtesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#ctes}.
	 * @param ctx the parse tree
	 */
	void exitCtes(SparkSqlParser.CtesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void enterNamedQuery(SparkSqlParser.NamedQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void exitNamedQuery(SparkSqlParser.NamedQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#tableProvider}.
	 * @param ctx the parse tree
	 */
	void enterTableProvider(SparkSqlParser.TableProviderContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#tableProvider}.
	 * @param ctx the parse tree
	 */
	void exitTableProvider(SparkSqlParser.TableProviderContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#createTableClauses}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableClauses(SparkSqlParser.CreateTableClausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#createTableClauses}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableClauses(SparkSqlParser.CreateTableClausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#propertyList}.
	 * @param ctx the parse tree
	 */
	void enterPropertyList(SparkSqlParser.PropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#propertyList}.
	 * @param ctx the parse tree
	 */
	void exitPropertyList(SparkSqlParser.PropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#property}.
	 * @param ctx the parse tree
	 */
	void enterProperty(SparkSqlParser.PropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#property}.
	 * @param ctx the parse tree
	 */
	void exitProperty(SparkSqlParser.PropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#propertyKey}.
	 * @param ctx the parse tree
	 */
	void enterPropertyKey(SparkSqlParser.PropertyKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#propertyKey}.
	 * @param ctx the parse tree
	 */
	void exitPropertyKey(SparkSqlParser.PropertyKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#propertyValue}.
	 * @param ctx the parse tree
	 */
	void enterPropertyValue(SparkSqlParser.PropertyValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#propertyValue}.
	 * @param ctx the parse tree
	 */
	void exitPropertyValue(SparkSqlParser.PropertyValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#constantList}.
	 * @param ctx the parse tree
	 */
	void enterConstantList(SparkSqlParser.ConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#constantList}.
	 * @param ctx the parse tree
	 */
	void exitConstantList(SparkSqlParser.ConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#nestedConstantList}.
	 * @param ctx the parse tree
	 */
	void enterNestedConstantList(SparkSqlParser.NestedConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#nestedConstantList}.
	 * @param ctx the parse tree
	 */
	void exitNestedConstantList(SparkSqlParser.NestedConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#createFileFormat}.
	 * @param ctx the parse tree
	 */
	void enterCreateFileFormat(SparkSqlParser.CreateFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#createFileFormat}.
	 * @param ctx the parse tree
	 */
	void exitCreateFileFormat(SparkSqlParser.CreateFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link SparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterTableFileFormat(SparkSqlParser.TableFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link SparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitTableFileFormat(SparkSqlParser.TableFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link SparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterGenericFileFormat(SparkSqlParser.GenericFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link SparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitGenericFileFormat(SparkSqlParser.GenericFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#storageHandler}.
	 * @param ctx the parse tree
	 */
	void enterStorageHandler(SparkSqlParser.StorageHandlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#storageHandler}.
	 * @param ctx the parse tree
	 */
	void exitStorageHandler(SparkSqlParser.StorageHandlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(SparkSqlParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(SparkSqlParser.ResourceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterSingleInsertQuery(SparkSqlParser.SingleInsertQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitSingleInsertQuery(SparkSqlParser.SingleInsertQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterMultiInsertQuery(SparkSqlParser.MultiInsertQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitMultiInsertQuery(SparkSqlParser.MultiInsertQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code deleteFromTable}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterDeleteFromTable(SparkSqlParser.DeleteFromTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code deleteFromTable}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitDeleteFromTable(SparkSqlParser.DeleteFromTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code updateTable}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterUpdateTable(SparkSqlParser.UpdateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code updateTable}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitUpdateTable(SparkSqlParser.UpdateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mergeIntoTable}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterMergeIntoTable(SparkSqlParser.MergeIntoTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mergeIntoTable}
	 * labeled alternative in {@link SparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitMergeIntoTable(SparkSqlParser.MergeIntoTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#queryOrganization}.
	 * @param ctx the parse tree
	 */
	void enterQueryOrganization(SparkSqlParser.QueryOrganizationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#queryOrganization}.
	 * @param ctx the parse tree
	 */
	void exitQueryOrganization(SparkSqlParser.QueryOrganizationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 */
	void enterMultiInsertQueryBody(SparkSqlParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 */
	void exitMultiInsertQueryBody(SparkSqlParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link SparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterQueryTermDefault(SparkSqlParser.QueryTermDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link SparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitQueryTermDefault(SparkSqlParser.QueryTermDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link SparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterSetOperation(SparkSqlParser.SetOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link SparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitSetOperation(SparkSqlParser.SetOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterQueryPrimaryDefault(SparkSqlParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitQueryPrimaryDefault(SparkSqlParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterFromStmt(SparkSqlParser.FromStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitFromStmt(SparkSqlParser.FromStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTable(SparkSqlParser.TableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTable(SparkSqlParser.TableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTableDefault1(SparkSqlParser.InlineTableDefault1Context ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTableDefault1(SparkSqlParser.InlineTableDefault1Context ctx);
	/**
	 * Enter a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(SparkSqlParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link SparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(SparkSqlParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void enterSortItem(SparkSqlParser.SortItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void exitSortItem(SparkSqlParser.SortItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#fromStatement}.
	 * @param ctx the parse tree
	 */
	void enterFromStatement(SparkSqlParser.FromStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#fromStatement}.
	 * @param ctx the parse tree
	 */
	void exitFromStatement(SparkSqlParser.FromStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#fromStatementBody}.
	 * @param ctx the parse tree
	 */
	void enterFromStatementBody(SparkSqlParser.FromStatementBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#fromStatementBody}.
	 * @param ctx the parse tree
	 */
	void exitFromStatementBody(SparkSqlParser.FromStatementBodyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link SparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterTransformQuerySpecification(SparkSqlParser.TransformQuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link SparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitTransformQuerySpecification(SparkSqlParser.TransformQuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link SparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterRegularQuerySpecification(SparkSqlParser.RegularQuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link SparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitRegularQuerySpecification(SparkSqlParser.RegularQuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#transformClause}.
	 * @param ctx the parse tree
	 */
	void enterTransformClause(SparkSqlParser.TransformClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#transformClause}.
	 * @param ctx the parse tree
	 */
	void exitTransformClause(SparkSqlParser.TransformClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(SparkSqlParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(SparkSqlParser.SelectClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#setClause}.
	 * @param ctx the parse tree
	 */
	void enterSetClause(SparkSqlParser.SetClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#setClause}.
	 * @param ctx the parse tree
	 */
	void exitSetClause(SparkSqlParser.SetClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#matchedClause}.
	 * @param ctx the parse tree
	 */
	void enterMatchedClause(SparkSqlParser.MatchedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#matchedClause}.
	 * @param ctx the parse tree
	 */
	void exitMatchedClause(SparkSqlParser.MatchedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#notMatchedClause}.
	 * @param ctx the parse tree
	 */
	void enterNotMatchedClause(SparkSqlParser.NotMatchedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#notMatchedClause}.
	 * @param ctx the parse tree
	 */
	void exitNotMatchedClause(SparkSqlParser.NotMatchedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#notMatchedBySourceClause}.
	 * @param ctx the parse tree
	 */
	void enterNotMatchedBySourceClause(SparkSqlParser.NotMatchedBySourceClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#notMatchedBySourceClause}.
	 * @param ctx the parse tree
	 */
	void exitNotMatchedBySourceClause(SparkSqlParser.NotMatchedBySourceClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#matchedAction}.
	 * @param ctx the parse tree
	 */
	void enterMatchedAction(SparkSqlParser.MatchedActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#matchedAction}.
	 * @param ctx the parse tree
	 */
	void exitMatchedAction(SparkSqlParser.MatchedActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#notMatchedAction}.
	 * @param ctx the parse tree
	 */
	void enterNotMatchedAction(SparkSqlParser.NotMatchedActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#notMatchedAction}.
	 * @param ctx the parse tree
	 */
	void exitNotMatchedAction(SparkSqlParser.NotMatchedActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#notMatchedBySourceAction}.
	 * @param ctx the parse tree
	 */
	void enterNotMatchedBySourceAction(SparkSqlParser.NotMatchedBySourceActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#notMatchedBySourceAction}.
	 * @param ctx the parse tree
	 */
	void exitNotMatchedBySourceAction(SparkSqlParser.NotMatchedBySourceActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#assignmentList}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentList(SparkSqlParser.AssignmentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#assignmentList}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentList(SparkSqlParser.AssignmentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(SparkSqlParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(SparkSqlParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void enterWhereClause(SparkSqlParser.WhereClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void exitWhereClause(SparkSqlParser.WhereClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void enterHavingClause(SparkSqlParser.HavingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void exitHavingClause(SparkSqlParser.HavingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#hint}.
	 * @param ctx the parse tree
	 */
	void enterHint(SparkSqlParser.HintContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#hint}.
	 * @param ctx the parse tree
	 */
	void exitHint(SparkSqlParser.HintContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#hintStatement}.
	 * @param ctx the parse tree
	 */
	void enterHintStatement(SparkSqlParser.HintStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#hintStatement}.
	 * @param ctx the parse tree
	 */
	void exitHintStatement(SparkSqlParser.HintStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFromClause(SparkSqlParser.FromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFromClause(SparkSqlParser.FromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#temporalClause}.
	 * @param ctx the parse tree
	 */
	void enterTemporalClause(SparkSqlParser.TemporalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#temporalClause}.
	 * @param ctx the parse tree
	 */
	void exitTemporalClause(SparkSqlParser.TemporalClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#aggregationClause}.
	 * @param ctx the parse tree
	 */
	void enterAggregationClause(SparkSqlParser.AggregationClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#aggregationClause}.
	 * @param ctx the parse tree
	 */
	void exitAggregationClause(SparkSqlParser.AggregationClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#groupByClause}.
	 * @param ctx the parse tree
	 */
	void enterGroupByClause(SparkSqlParser.GroupByClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#groupByClause}.
	 * @param ctx the parse tree
	 */
	void exitGroupByClause(SparkSqlParser.GroupByClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 */
	void enterGroupingAnalytics(SparkSqlParser.GroupingAnalyticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 */
	void exitGroupingAnalytics(SparkSqlParser.GroupingAnalyticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterGroupingElement(SparkSqlParser.GroupingElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitGroupingElement(SparkSqlParser.GroupingElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void enterGroupingSet(SparkSqlParser.GroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void exitGroupingSet(SparkSqlParser.GroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#pivotClause}.
	 * @param ctx the parse tree
	 */
	void enterPivotClause(SparkSqlParser.PivotClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#pivotClause}.
	 * @param ctx the parse tree
	 */
	void exitPivotClause(SparkSqlParser.PivotClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#pivotColumn}.
	 * @param ctx the parse tree
	 */
	void enterPivotColumn(SparkSqlParser.PivotColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#pivotColumn}.
	 * @param ctx the parse tree
	 */
	void exitPivotColumn(SparkSqlParser.PivotColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#pivotValue}.
	 * @param ctx the parse tree
	 */
	void enterPivotValue(SparkSqlParser.PivotValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#pivotValue}.
	 * @param ctx the parse tree
	 */
	void exitPivotValue(SparkSqlParser.PivotValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotClause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotClause(SparkSqlParser.UnpivotClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotClause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotClause(SparkSqlParser.UnpivotClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotNullClause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotNullClause(SparkSqlParser.UnpivotNullClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotNullClause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotNullClause(SparkSqlParser.UnpivotNullClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotOperator}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotOperator(SparkSqlParser.UnpivotOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotOperator}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotOperator(SparkSqlParser.UnpivotOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotSingleValueColumnClause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotSingleValueColumnClause(SparkSqlParser.UnpivotSingleValueColumnClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotSingleValueColumnClause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotSingleValueColumnClause(SparkSqlParser.UnpivotSingleValueColumnClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotMultiValueColumnClause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotMultiValueColumnClause(SparkSqlParser.UnpivotMultiValueColumnClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotMultiValueColumnClause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotMultiValueColumnClause(SparkSqlParser.UnpivotMultiValueColumnClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotColumnSet}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotColumnSet(SparkSqlParser.UnpivotColumnSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotColumnSet}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotColumnSet(SparkSqlParser.UnpivotColumnSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotValueColumn}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotValueColumn(SparkSqlParser.UnpivotValueColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotValueColumn}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotValueColumn(SparkSqlParser.UnpivotValueColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotNameColumn}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotNameColumn(SparkSqlParser.UnpivotNameColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotNameColumn}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotNameColumn(SparkSqlParser.UnpivotNameColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotColumnAndAlias}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotColumnAndAlias(SparkSqlParser.UnpivotColumnAndAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotColumnAndAlias}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotColumnAndAlias(SparkSqlParser.UnpivotColumnAndAliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotColumn}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotColumn(SparkSqlParser.UnpivotColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotColumn}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotColumn(SparkSqlParser.UnpivotColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unpivotAlias}.
	 * @param ctx the parse tree
	 */
	void enterUnpivotAlias(SparkSqlParser.UnpivotAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unpivotAlias}.
	 * @param ctx the parse tree
	 */
	void exitUnpivotAlias(SparkSqlParser.UnpivotAliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#lateralView}.
	 * @param ctx the parse tree
	 */
	void enterLateralView(SparkSqlParser.LateralViewContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#lateralView}.
	 * @param ctx the parse tree
	 */
	void exitLateralView(SparkSqlParser.LateralViewContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterSetQuantifier(SparkSqlParser.SetQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitSetQuantifier(SparkSqlParser.SetQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterRelation(SparkSqlParser.RelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitRelation(SparkSqlParser.RelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#relationExtension}.
	 * @param ctx the parse tree
	 */
	void enterRelationExtension(SparkSqlParser.RelationExtensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#relationExtension}.
	 * @param ctx the parse tree
	 */
	void exitRelationExtension(SparkSqlParser.RelationExtensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void enterJoinRelation(SparkSqlParser.JoinRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void exitJoinRelation(SparkSqlParser.JoinRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#joinType}.
	 * @param ctx the parse tree
	 */
	void enterJoinType(SparkSqlParser.JoinTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#joinType}.
	 * @param ctx the parse tree
	 */
	void exitJoinType(SparkSqlParser.JoinTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void enterJoinCriteria(SparkSqlParser.JoinCriteriaContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void exitJoinCriteria(SparkSqlParser.JoinCriteriaContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#sample}.
	 * @param ctx the parse tree
	 */
	void enterSample(SparkSqlParser.SampleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#sample}.
	 * @param ctx the parse tree
	 */
	void exitSample(SparkSqlParser.SampleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByPercentile(SparkSqlParser.SampleByPercentileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByPercentile(SparkSqlParser.SampleByPercentileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByRows(SparkSqlParser.SampleByRowsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByRows(SparkSqlParser.SampleByRowsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByBucket(SparkSqlParser.SampleByBucketContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByBucket(SparkSqlParser.SampleByBucketContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByBytes(SparkSqlParser.SampleByBytesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link SparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByBytes(SparkSqlParser.SampleByBytesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierList(SparkSqlParser.IdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierList(SparkSqlParser.IdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierSeq(SparkSqlParser.IdentifierSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierSeq(SparkSqlParser.IdentifierSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 */
	void enterOrderedIdentifierList(SparkSqlParser.OrderedIdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 */
	void exitOrderedIdentifierList(SparkSqlParser.OrderedIdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterOrderedIdentifier(SparkSqlParser.OrderedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitOrderedIdentifier(SparkSqlParser.OrderedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#identifierCommentList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierCommentList(SparkSqlParser.IdentifierCommentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#identifierCommentList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierCommentList(SparkSqlParser.IdentifierCommentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#identifierComment}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierComment(SparkSqlParser.IdentifierCommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#identifierComment}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierComment(SparkSqlParser.IdentifierCommentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableName(SparkSqlParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableName(SparkSqlParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterAliasedQuery(SparkSqlParser.AliasedQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitAliasedQuery(SparkSqlParser.AliasedQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterAliasedRelation(SparkSqlParser.AliasedRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitAliasedRelation(SparkSqlParser.AliasedRelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTableDefault2(SparkSqlParser.InlineTableDefault2Context ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTableDefault2(SparkSqlParser.InlineTableDefault2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableValuedFunction(SparkSqlParser.TableValuedFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link SparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableValuedFunction(SparkSqlParser.TableValuedFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void enterInlineTable(SparkSqlParser.InlineTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void exitInlineTable(SparkSqlParser.InlineTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#functionTable}.
	 * @param ctx the parse tree
	 */
	void enterFunctionTable(SparkSqlParser.FunctionTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#functionTable}.
	 * @param ctx the parse tree
	 */
	void exitFunctionTable(SparkSqlParser.FunctionTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#tableAlias}.
	 * @param ctx the parse tree
	 */
	void enterTableAlias(SparkSqlParser.TableAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#tableAlias}.
	 * @param ctx the parse tree
	 */
	void exitTableAlias(SparkSqlParser.TableAliasContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link SparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void enterRowFormatSerde(SparkSqlParser.RowFormatSerdeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link SparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void exitRowFormatSerde(SparkSqlParser.RowFormatSerdeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link SparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void enterRowFormatDelimited(SparkSqlParser.RowFormatDelimitedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link SparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void exitRowFormatDelimited(SparkSqlParser.RowFormatDelimitedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#multipartIdentifierList}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifierList(SparkSqlParser.MultipartIdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#multipartIdentifierList}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifierList(SparkSqlParser.MultipartIdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifier(SparkSqlParser.MultipartIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifier(SparkSqlParser.MultipartIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#multipartIdentifierPropertyList}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifierPropertyList(SparkSqlParser.MultipartIdentifierPropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#multipartIdentifierPropertyList}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifierPropertyList(SparkSqlParser.MultipartIdentifierPropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#multipartIdentifierProperty}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifierProperty(SparkSqlParser.MultipartIdentifierPropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#multipartIdentifierProperty}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifierProperty(SparkSqlParser.MultipartIdentifierPropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterTableIdentifier(SparkSqlParser.TableIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitTableIdentifier(SparkSqlParser.TableIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#functionIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterFunctionIdentifier(SparkSqlParser.FunctionIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#functionIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitFunctionIdentifier(SparkSqlParser.FunctionIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#namedExpression}.
	 * @param ctx the parse tree
	 */
	void enterNamedExpression(SparkSqlParser.NamedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#namedExpression}.
	 * @param ctx the parse tree
	 */
	void exitNamedExpression(SparkSqlParser.NamedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 */
	void enterNamedExpressionSeq(SparkSqlParser.NamedExpressionSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 */
	void exitNamedExpressionSeq(SparkSqlParser.NamedExpressionSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#partitionFieldList}.
	 * @param ctx the parse tree
	 */
	void enterPartitionFieldList(SparkSqlParser.PartitionFieldListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#partitionFieldList}.
	 * @param ctx the parse tree
	 */
	void exitPartitionFieldList(SparkSqlParser.PartitionFieldListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link SparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void enterPartitionTransform(SparkSqlParser.PartitionTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link SparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void exitPartitionTransform(SparkSqlParser.PartitionTransformContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link SparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void enterPartitionColumn(SparkSqlParser.PartitionColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link SparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void exitPartitionColumn(SparkSqlParser.PartitionColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link SparkSqlParser#transform}.
	 * @param ctx the parse tree
	 */
	void enterIdentityTransform(SparkSqlParser.IdentityTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link SparkSqlParser#transform}.
	 * @param ctx the parse tree
	 */
	void exitIdentityTransform(SparkSqlParser.IdentityTransformContext ctx);
	/**
	 * Enter a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link SparkSqlParser#transform}.
	 * @param ctx the parse tree
	 */
	void enterApplyTransform(SparkSqlParser.ApplyTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link SparkSqlParser#transform}.
	 * @param ctx the parse tree
	 */
	void exitApplyTransform(SparkSqlParser.ApplyTransformContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#transformArgument}.
	 * @param ctx the parse tree
	 */
	void enterTransformArgument(SparkSqlParser.TransformArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#transformArgument}.
	 * @param ctx the parse tree
	 */
	void exitTransformArgument(SparkSqlParser.TransformArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(SparkSqlParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(SparkSqlParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#expressionSeq}.
	 * @param ctx the parse tree
	 */
	void enterExpressionSeq(SparkSqlParser.ExpressionSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#expressionSeq}.
	 * @param ctx the parse tree
	 */
	void exitExpressionSeq(SparkSqlParser.ExpressionSeqContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalNot(SparkSqlParser.LogicalNotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalNot(SparkSqlParser.LogicalNotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterPredicated(SparkSqlParser.PredicatedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitPredicated(SparkSqlParser.PredicatedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exists}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterExists(SparkSqlParser.ExistsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitExists(SparkSqlParser.ExistsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalBinary(SparkSqlParser.LogicalBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link SparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalBinary(SparkSqlParser.LogicalBinaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(SparkSqlParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(SparkSqlParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterValueExpressionDefault(SparkSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitValueExpressionDefault(SparkSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterComparison(SparkSqlParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitComparison(SparkSqlParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticBinary(SparkSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticBinary(SparkSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticUnary(SparkSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link SparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticUnary(SparkSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#datetimeUnit}.
	 * @param ctx the parse tree
	 */
	void enterDatetimeUnit(SparkSqlParser.DatetimeUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#datetimeUnit}.
	 * @param ctx the parse tree
	 */
	void exitDatetimeUnit(SparkSqlParser.DatetimeUnitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code struct}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStruct(SparkSqlParser.StructContext ctx);
	/**
	 * Exit a parse tree produced by the {@code struct}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStruct(SparkSqlParser.StructContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterDereference(SparkSqlParser.DereferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitDereference(SparkSqlParser.DereferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timestampadd}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTimestampadd(SparkSqlParser.TimestampaddContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timestampadd}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTimestampadd(SparkSqlParser.TimestampaddContext ctx);
	/**
	 * Enter a parse tree produced by the {@code substring}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubstring(SparkSqlParser.SubstringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code substring}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubstring(SparkSqlParser.SubstringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cast}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCast(SparkSqlParser.CastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCast(SparkSqlParser.CastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambda(SparkSqlParser.LambdaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambda(SparkSqlParser.LambdaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(SparkSqlParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(SparkSqlParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code any_value}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterAny_value(SparkSqlParser.Any_valueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code any_value}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitAny_value(SparkSqlParser.Any_valueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code trim}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTrim(SparkSqlParser.TrimContext ctx);
	/**
	 * Exit a parse tree produced by the {@code trim}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTrim(SparkSqlParser.TrimContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCase(SparkSqlParser.SimpleCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCase(SparkSqlParser.SimpleCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCurrentLike(SparkSqlParser.CurrentLikeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCurrentLike(SparkSqlParser.CurrentLikeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterColumnReference(SparkSqlParser.ColumnReferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitColumnReference(SparkSqlParser.ColumnReferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterRowConstructor(SparkSqlParser.RowConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitRowConstructor(SparkSqlParser.RowConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code last}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLast(SparkSqlParser.LastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code last}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLast(SparkSqlParser.LastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code star}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStar(SparkSqlParser.StarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code star}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStar(SparkSqlParser.StarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterOverlay(SparkSqlParser.OverlayContext ctx);
	/**
	 * Exit a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitOverlay(SparkSqlParser.OverlayContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(SparkSqlParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(SparkSqlParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timestampdiff}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTimestampdiff(SparkSqlParser.TimestampdiffContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timestampdiff}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTimestampdiff(SparkSqlParser.TimestampdiffContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryExpression(SparkSqlParser.SubqueryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryExpression(SparkSqlParser.SubqueryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantDefault(SparkSqlParser.ConstantDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantDefault(SparkSqlParser.ConstantDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code extract}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterExtract(SparkSqlParser.ExtractContext ctx);
	/**
	 * Exit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitExtract(SparkSqlParser.ExtractContext ctx);
	/**
	 * Enter a parse tree produced by the {@code percentile}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPercentile(SparkSqlParser.PercentileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code percentile}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPercentile(SparkSqlParser.PercentileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(SparkSqlParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(SparkSqlParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSearchedCase(SparkSqlParser.SearchedCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSearchedCase(SparkSqlParser.SearchedCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code position}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPosition(SparkSqlParser.PositionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code position}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPosition(SparkSqlParser.PositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code first}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFirst(SparkSqlParser.FirstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code first}
	 * labeled alternative in {@link SparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFirst(SparkSqlParser.FirstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNullLiteral(SparkSqlParser.NullLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNullLiteral(SparkSqlParser.NullLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parameterLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterParameterLiteral(SparkSqlParser.ParameterLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parameterLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitParameterLiteral(SparkSqlParser.ParameterLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterIntervalLiteral(SparkSqlParser.IntervalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitIntervalLiteral(SparkSqlParser.IntervalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterTypeConstructor(SparkSqlParser.TypeConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitTypeConstructor(SparkSqlParser.TypeConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(SparkSqlParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(SparkSqlParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(SparkSqlParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(SparkSqlParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(SparkSqlParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link SparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(SparkSqlParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(SparkSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(SparkSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticOperator(SparkSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticOperator(SparkSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#predicateOperator}.
	 * @param ctx the parse tree
	 */
	void enterPredicateOperator(SparkSqlParser.PredicateOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#predicateOperator}.
	 * @param ctx the parse tree
	 */
	void exitPredicateOperator(SparkSqlParser.PredicateOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(SparkSqlParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(SparkSqlParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#interval}.
	 * @param ctx the parse tree
	 */
	void enterInterval(SparkSqlParser.IntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#interval}.
	 * @param ctx the parse tree
	 */
	void exitInterval(SparkSqlParser.IntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingMultiUnitsInterval(SparkSqlParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingMultiUnitsInterval(SparkSqlParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void enterMultiUnitsInterval(SparkSqlParser.MultiUnitsIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void exitMultiUnitsInterval(SparkSqlParser.MultiUnitsIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingUnitToUnitInterval(SparkSqlParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingUnitToUnitInterval(SparkSqlParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void enterUnitToUnitInterval(SparkSqlParser.UnitToUnitIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void exitUnitToUnitInterval(SparkSqlParser.UnitToUnitIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#intervalValue}.
	 * @param ctx the parse tree
	 */
	void enterIntervalValue(SparkSqlParser.IntervalValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#intervalValue}.
	 * @param ctx the parse tree
	 */
	void exitIntervalValue(SparkSqlParser.IntervalValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unitInMultiUnits}.
	 * @param ctx the parse tree
	 */
	void enterUnitInMultiUnits(SparkSqlParser.UnitInMultiUnitsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unitInMultiUnits}.
	 * @param ctx the parse tree
	 */
	void exitUnitInMultiUnits(SparkSqlParser.UnitInMultiUnitsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#unitInUnitToUnit}.
	 * @param ctx the parse tree
	 */
	void enterUnitInUnitToUnit(SparkSqlParser.UnitInUnitToUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#unitInUnitToUnit}.
	 * @param ctx the parse tree
	 */
	void exitUnitInUnitToUnit(SparkSqlParser.UnitInUnitToUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#colPosition}.
	 * @param ctx the parse tree
	 */
	void enterColPosition(SparkSqlParser.ColPositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#colPosition}.
	 * @param ctx the parse tree
	 */
	void exitColPosition(SparkSqlParser.ColPositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterComplexDataType(SparkSqlParser.ComplexDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitComplexDataType(SparkSqlParser.ComplexDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterYearMonthIntervalDataType(SparkSqlParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitYearMonthIntervalDataType(SparkSqlParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDayTimeIntervalDataType(SparkSqlParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDayTimeIntervalDataType(SparkSqlParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveDataType(SparkSqlParser.PrimitiveDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link SparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveDataType(SparkSqlParser.PrimitiveDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#qualifiedColTypeWithPositionList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedColTypeWithPositionList(SparkSqlParser.QualifiedColTypeWithPositionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#qualifiedColTypeWithPositionList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedColTypeWithPositionList(SparkSqlParser.QualifiedColTypeWithPositionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#qualifiedColTypeWithPosition}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedColTypeWithPosition(SparkSqlParser.QualifiedColTypeWithPositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#qualifiedColTypeWithPosition}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedColTypeWithPosition(SparkSqlParser.QualifiedColTypeWithPositionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#defaultExpression}.
	 * @param ctx the parse tree
	 */
	void enterDefaultExpression(SparkSqlParser.DefaultExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#defaultExpression}.
	 * @param ctx the parse tree
	 */
	void exitDefaultExpression(SparkSqlParser.DefaultExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void enterColTypeList(SparkSqlParser.ColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void exitColTypeList(SparkSqlParser.ColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#colType}.
	 * @param ctx the parse tree
	 */
	void enterColType(SparkSqlParser.ColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#colType}.
	 * @param ctx the parse tree
	 */
	void exitColType(SparkSqlParser.ColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#createOrReplaceTableColTypeList}.
	 * @param ctx the parse tree
	 */
	void enterCreateOrReplaceTableColTypeList(SparkSqlParser.CreateOrReplaceTableColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#createOrReplaceTableColTypeList}.
	 * @param ctx the parse tree
	 */
	void exitCreateOrReplaceTableColTypeList(SparkSqlParser.CreateOrReplaceTableColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#createOrReplaceTableColType}.
	 * @param ctx the parse tree
	 */
	void enterCreateOrReplaceTableColType(SparkSqlParser.CreateOrReplaceTableColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#createOrReplaceTableColType}.
	 * @param ctx the parse tree
	 */
	void exitCreateOrReplaceTableColType(SparkSqlParser.CreateOrReplaceTableColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#colDefinitionOption}.
	 * @param ctx the parse tree
	 */
	void enterColDefinitionOption(SparkSqlParser.ColDefinitionOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#colDefinitionOption}.
	 * @param ctx the parse tree
	 */
	void exitColDefinitionOption(SparkSqlParser.ColDefinitionOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#generationExpression}.
	 * @param ctx the parse tree
	 */
	void enterGenerationExpression(SparkSqlParser.GenerationExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#generationExpression}.
	 * @param ctx the parse tree
	 */
	void exitGenerationExpression(SparkSqlParser.GenerationExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#complexColTypeList}.
	 * @param ctx the parse tree
	 */
	void enterComplexColTypeList(SparkSqlParser.ComplexColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#complexColTypeList}.
	 * @param ctx the parse tree
	 */
	void exitComplexColTypeList(SparkSqlParser.ComplexColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#complexColType}.
	 * @param ctx the parse tree
	 */
	void enterComplexColType(SparkSqlParser.ComplexColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#complexColType}.
	 * @param ctx the parse tree
	 */
	void exitComplexColType(SparkSqlParser.ComplexColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(SparkSqlParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(SparkSqlParser.WhenClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#windowClause}.
	 * @param ctx the parse tree
	 */
	void enterWindowClause(SparkSqlParser.WindowClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#windowClause}.
	 * @param ctx the parse tree
	 */
	void exitWindowClause(SparkSqlParser.WindowClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#namedWindow}.
	 * @param ctx the parse tree
	 */
	void enterNamedWindow(SparkSqlParser.NamedWindowContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#namedWindow}.
	 * @param ctx the parse tree
	 */
	void exitNamedWindow(SparkSqlParser.NamedWindowContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link SparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void enterWindowRef(SparkSqlParser.WindowRefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link SparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void exitWindowRef(SparkSqlParser.WindowRefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link SparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void enterWindowDef(SparkSqlParser.WindowDefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link SparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void exitWindowDef(SparkSqlParser.WindowDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void enterWindowFrame(SparkSqlParser.WindowFrameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void exitWindowFrame(SparkSqlParser.WindowFrameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterFrameBound(SparkSqlParser.FrameBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitFrameBound(SparkSqlParser.FrameBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedNameList(SparkSqlParser.QualifiedNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedNameList(SparkSqlParser.QualifiedNameListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#functionName}.
	 * @param ctx the parse tree
	 */
	void enterFunctionName(SparkSqlParser.FunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#functionName}.
	 * @param ctx the parse tree
	 */
	void exitFunctionName(SparkSqlParser.FunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(SparkSqlParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(SparkSqlParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingIdentifier(SparkSqlParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingIdentifier(SparkSqlParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link SparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void enterErrorIdent(SparkSqlParser.ErrorIdentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link SparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void exitErrorIdent(SparkSqlParser.ErrorIdentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link SparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void enterRealIdent(SparkSqlParser.RealIdentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link SparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void exitRealIdent(SparkSqlParser.RealIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(SparkSqlParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(SparkSqlParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link SparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedIdentifier(SparkSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link SparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedIdentifier(SparkSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link SparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifierAlternative(SparkSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link SparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifierAlternative(SparkSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifier(SparkSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifier(SparkSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#backQuotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterBackQuotedIdentifier(SparkSqlParser.BackQuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#backQuotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitBackQuotedIdentifier(SparkSqlParser.BackQuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterExponentLiteral(SparkSqlParser.ExponentLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitExponentLiteral(SparkSqlParser.ExponentLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDecimalLiteral(SparkSqlParser.DecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDecimalLiteral(SparkSqlParser.DecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterLegacyDecimalLiteral(SparkSqlParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitLegacyDecimalLiteral(SparkSqlParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(SparkSqlParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(SparkSqlParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigIntLiteral(SparkSqlParser.BigIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigIntLiteral(SparkSqlParser.BigIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterSmallIntLiteral(SparkSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitSmallIntLiteral(SparkSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterTinyIntLiteral(SparkSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitTinyIntLiteral(SparkSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(SparkSqlParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(SparkSqlParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterFloatLiteral(SparkSqlParser.FloatLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitFloatLiteral(SparkSqlParser.FloatLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigDecimalLiteral(SparkSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link SparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigDecimalLiteral(SparkSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#alterColumnAction}.
	 * @param ctx the parse tree
	 */
	void enterAlterColumnAction(SparkSqlParser.AlterColumnActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#alterColumnAction}.
	 * @param ctx the parse tree
	 */
	void exitAlterColumnAction(SparkSqlParser.AlterColumnActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#stringLit}.
	 * @param ctx the parse tree
	 */
	void enterStringLit(SparkSqlParser.StringLitContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#stringLit}.
	 * @param ctx the parse tree
	 */
	void exitStringLit(SparkSqlParser.StringLitContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#comment}.
	 * @param ctx the parse tree
	 */
	void enterComment(SparkSqlParser.CommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#comment}.
	 * @param ctx the parse tree
	 */
	void exitComment(SparkSqlParser.CommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#version}.
	 * @param ctx the parse tree
	 */
	void enterVersion(SparkSqlParser.VersionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#version}.
	 * @param ctx the parse tree
	 */
	void exitVersion(SparkSqlParser.VersionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 */
	void enterAnsiNonReserved(SparkSqlParser.AnsiNonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 */
	void exitAnsiNonReserved(SparkSqlParser.AnsiNonReservedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#strictNonReserved}.
	 * @param ctx the parse tree
	 */
	void enterStrictNonReserved(SparkSqlParser.StrictNonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#strictNonReserved}.
	 * @param ctx the parse tree
	 */
	void exitStrictNonReserved(SparkSqlParser.StrictNonReservedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkSqlParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void enterNonReserved(SparkSqlParser.NonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkSqlParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void exitNonReserved(SparkSqlParser.NonReservedContext ctx);
}