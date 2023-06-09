/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Generated from com/netease/arctic/spark/sql/parser/ArcticSqlCommand.g4 by ANTLR 4.7
package com.netease.arctic.spark.sql.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ArcticSqlCommandParser}.
 */
public interface ArcticSqlCommandListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#arcticCommand}.
	 * @param ctx the parse tree
	 */
	void enterArcticCommand(ArcticSqlCommandParser.ArcticCommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#arcticCommand}.
	 * @param ctx the parse tree
	 */
	void exitArcticCommand(ArcticSqlCommandParser.ArcticCommandContext ctx);
	/**
	 * Enter a parse tree produced by the {@code migrateStatement}
	 * labeled alternative in {@link ArcticSqlCommandParser#arcticStatement}.
	 * @param ctx the parse tree
	 */
	void enterMigrateStatement(ArcticSqlCommandParser.MigrateStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code migrateStatement}
	 * labeled alternative in {@link ArcticSqlCommandParser#arcticStatement}.
	 * @param ctx the parse tree
	 */
	void exitMigrateStatement(ArcticSqlCommandParser.MigrateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void enterSingleStatement(ArcticSqlCommandParser.SingleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void exitSingleStatement(ArcticSqlCommandParser.SingleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterSingleExpression(ArcticSqlCommandParser.SingleExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitSingleExpression(ArcticSqlCommandParser.SingleExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleTableIdentifier(ArcticSqlCommandParser.SingleTableIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleTableIdentifier(ArcticSqlCommandParser.SingleTableIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#singleMultipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleMultipartIdentifier(ArcticSqlCommandParser.SingleMultipartIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#singleMultipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleMultipartIdentifier(ArcticSqlCommandParser.SingleMultipartIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleFunctionIdentifier(ArcticSqlCommandParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleFunctionIdentifier(ArcticSqlCommandParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#singleDataType}.
	 * @param ctx the parse tree
	 */
	void enterSingleDataType(ArcticSqlCommandParser.SingleDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#singleDataType}.
	 * @param ctx the parse tree
	 */
	void exitSingleDataType(ArcticSqlCommandParser.SingleDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#singleTableSchema}.
	 * @param ctx the parse tree
	 */
	void enterSingleTableSchema(ArcticSqlCommandParser.SingleTableSchemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#singleTableSchema}.
	 * @param ctx the parse tree
	 */
	void exitSingleTableSchema(ArcticSqlCommandParser.SingleTableSchemaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatementDefault(ArcticSqlCommandParser.StatementDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatementDefault(ArcticSqlCommandParser.StatementDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dmlStatement}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDmlStatement(ArcticSqlCommandParser.DmlStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dmlStatement}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDmlStatement(ArcticSqlCommandParser.DmlStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code use}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUse(ArcticSqlCommandParser.UseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code use}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUse(ArcticSqlCommandParser.UseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createNamespace}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateNamespace(ArcticSqlCommandParser.CreateNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createNamespace}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateNamespace(ArcticSqlCommandParser.CreateNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNamespaceProperties}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetNamespaceProperties(ArcticSqlCommandParser.SetNamespacePropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNamespaceProperties}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetNamespaceProperties(ArcticSqlCommandParser.SetNamespacePropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNamespaceLocation}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetNamespaceLocation(ArcticSqlCommandParser.SetNamespaceLocationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNamespaceLocation}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetNamespaceLocation(ArcticSqlCommandParser.SetNamespaceLocationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropNamespace}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropNamespace(ArcticSqlCommandParser.DropNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropNamespace}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropNamespace(ArcticSqlCommandParser.DropNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showNamespaces}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowNamespaces(ArcticSqlCommandParser.ShowNamespacesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showNamespaces}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowNamespaces(ArcticSqlCommandParser.ShowNamespacesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTable(ArcticSqlCommandParser.CreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTable(ArcticSqlCommandParser.CreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTableLike}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableLike(ArcticSqlCommandParser.CreateTableLikeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTableLike}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableLike(ArcticSqlCommandParser.CreateTableLikeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code replaceTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterReplaceTable(ArcticSqlCommandParser.ReplaceTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code replaceTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitReplaceTable(ArcticSqlCommandParser.ReplaceTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze(ArcticSqlCommandParser.AnalyzeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze(ArcticSqlCommandParser.AnalyzeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code analyzeTables}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyzeTables(ArcticSqlCommandParser.AnalyzeTablesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code analyzeTables}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyzeTables(ArcticSqlCommandParser.AnalyzeTablesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addTableColumns}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAddTableColumns(ArcticSqlCommandParser.AddTableColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addTableColumns}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAddTableColumns(ArcticSqlCommandParser.AddTableColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTableColumn}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTableColumn(ArcticSqlCommandParser.RenameTableColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTableColumn}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTableColumn(ArcticSqlCommandParser.RenameTableColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTableColumns}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTableColumns(ArcticSqlCommandParser.DropTableColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTableColumns}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTableColumns(ArcticSqlCommandParser.DropTableColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTable(ArcticSqlCommandParser.RenameTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTable(ArcticSqlCommandParser.RenameTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTableProperties}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTableProperties(ArcticSqlCommandParser.SetTablePropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTableProperties}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTableProperties(ArcticSqlCommandParser.SetTablePropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unsetTableProperties}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUnsetTableProperties(ArcticSqlCommandParser.UnsetTablePropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unsetTableProperties}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUnsetTableProperties(ArcticSqlCommandParser.UnsetTablePropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterTableAlterColumn}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterTableAlterColumn(ArcticSqlCommandParser.AlterTableAlterColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterTableAlterColumn}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterTableAlterColumn(ArcticSqlCommandParser.AlterTableAlterColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hiveChangeColumn}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterHiveChangeColumn(ArcticSqlCommandParser.HiveChangeColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hiveChangeColumn}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitHiveChangeColumn(ArcticSqlCommandParser.HiveChangeColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hiveReplaceColumns}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterHiveReplaceColumns(ArcticSqlCommandParser.HiveReplaceColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hiveReplaceColumns}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitHiveReplaceColumns(ArcticSqlCommandParser.HiveReplaceColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTableSerDe}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTableSerDe(ArcticSqlCommandParser.SetTableSerDeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTableSerDe}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTableSerDe(ArcticSqlCommandParser.SetTableSerDeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addTablePartition}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAddTablePartition(ArcticSqlCommandParser.AddTablePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addTablePartition}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAddTablePartition(ArcticSqlCommandParser.AddTablePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTablePartition}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTablePartition(ArcticSqlCommandParser.RenameTablePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTablePartition}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTablePartition(ArcticSqlCommandParser.RenameTablePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTablePartitions}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTablePartitions(ArcticSqlCommandParser.DropTablePartitionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTablePartitions}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTablePartitions(ArcticSqlCommandParser.DropTablePartitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTableLocation}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTableLocation(ArcticSqlCommandParser.SetTableLocationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTableLocation}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTableLocation(ArcticSqlCommandParser.SetTableLocationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code recoverPartitions}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRecoverPartitions(ArcticSqlCommandParser.RecoverPartitionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code recoverPartitions}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRecoverPartitions(ArcticSqlCommandParser.RecoverPartitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTable(ArcticSqlCommandParser.DropTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTable(ArcticSqlCommandParser.DropTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropView(ArcticSqlCommandParser.DropViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropView(ArcticSqlCommandParser.DropViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createView}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateView(ArcticSqlCommandParser.CreateViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createView}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateView(ArcticSqlCommandParser.CreateViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTempViewUsing}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTempViewUsing(ArcticSqlCommandParser.CreateTempViewUsingContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTempViewUsing}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTempViewUsing(ArcticSqlCommandParser.CreateTempViewUsingContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterViewQuery}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterViewQuery(ArcticSqlCommandParser.AlterViewQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterViewQuery}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterViewQuery(ArcticSqlCommandParser.AlterViewQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateFunction(ArcticSqlCommandParser.CreateFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateFunction(ArcticSqlCommandParser.CreateFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropFunction(ArcticSqlCommandParser.DropFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropFunction(ArcticSqlCommandParser.DropFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code explain}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterExplain(ArcticSqlCommandParser.ExplainContext ctx);
	/**
	 * Exit a parse tree produced by the {@code explain}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitExplain(ArcticSqlCommandParser.ExplainContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTables(ArcticSqlCommandParser.ShowTablesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTables(ArcticSqlCommandParser.ShowTablesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTableExtended}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTableExtended(ArcticSqlCommandParser.ShowTableExtendedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTableExtended}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTableExtended(ArcticSqlCommandParser.ShowTableExtendedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTblProperties}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTblProperties(ArcticSqlCommandParser.ShowTblPropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTblProperties}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTblProperties(ArcticSqlCommandParser.ShowTblPropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowColumns(ArcticSqlCommandParser.ShowColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowColumns(ArcticSqlCommandParser.ShowColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showViews}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowViews(ArcticSqlCommandParser.ShowViewsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showViews}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowViews(ArcticSqlCommandParser.ShowViewsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showPartitions}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowPartitions(ArcticSqlCommandParser.ShowPartitionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showPartitions}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowPartitions(ArcticSqlCommandParser.ShowPartitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowFunctions(ArcticSqlCommandParser.ShowFunctionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowFunctions(ArcticSqlCommandParser.ShowFunctionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateTable(ArcticSqlCommandParser.ShowCreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateTable(ArcticSqlCommandParser.ShowCreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCurrentNamespace}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCurrentNamespace(ArcticSqlCommandParser.ShowCurrentNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCurrentNamespace}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCurrentNamespace(ArcticSqlCommandParser.ShowCurrentNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeFunction}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeFunction(ArcticSqlCommandParser.DescribeFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeFunction}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeFunction(ArcticSqlCommandParser.DescribeFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeNamespace}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeNamespace(ArcticSqlCommandParser.DescribeNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeNamespace}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeNamespace(ArcticSqlCommandParser.DescribeNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeRelation}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeRelation(ArcticSqlCommandParser.DescribeRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeRelation}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeRelation(ArcticSqlCommandParser.DescribeRelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeQuery}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeQuery(ArcticSqlCommandParser.DescribeQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeQuery}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeQuery(ArcticSqlCommandParser.DescribeQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code commentNamespace}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCommentNamespace(ArcticSqlCommandParser.CommentNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code commentNamespace}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCommentNamespace(ArcticSqlCommandParser.CommentNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code commentTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCommentTable(ArcticSqlCommandParser.CommentTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code commentTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCommentTable(ArcticSqlCommandParser.CommentTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshTable(ArcticSqlCommandParser.RefreshTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshTable(ArcticSqlCommandParser.RefreshTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshFunction}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshFunction(ArcticSqlCommandParser.RefreshFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshFunction}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshFunction(ArcticSqlCommandParser.RefreshFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshResource}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshResource(ArcticSqlCommandParser.RefreshResourceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshResource}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshResource(ArcticSqlCommandParser.RefreshResourceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cacheTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCacheTable(ArcticSqlCommandParser.CacheTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cacheTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCacheTable(ArcticSqlCommandParser.CacheTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uncacheTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUncacheTable(ArcticSqlCommandParser.UncacheTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uncacheTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUncacheTable(ArcticSqlCommandParser.UncacheTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code clearCache}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterClearCache(ArcticSqlCommandParser.ClearCacheContext ctx);
	/**
	 * Exit a parse tree produced by the {@code clearCache}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitClearCache(ArcticSqlCommandParser.ClearCacheContext ctx);
	/**
	 * Enter a parse tree produced by the {@code loadData}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterLoadData(ArcticSqlCommandParser.LoadDataContext ctx);
	/**
	 * Exit a parse tree produced by the {@code loadData}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitLoadData(ArcticSqlCommandParser.LoadDataContext ctx);
	/**
	 * Enter a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterTruncateTable(ArcticSqlCommandParser.TruncateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitTruncateTable(ArcticSqlCommandParser.TruncateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code repairTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRepairTable(ArcticSqlCommandParser.RepairTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code repairTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRepairTable(ArcticSqlCommandParser.RepairTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code manageResource}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterManageResource(ArcticSqlCommandParser.ManageResourceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code manageResource}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitManageResource(ArcticSqlCommandParser.ManageResourceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code failNativeCommand}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterFailNativeCommand(ArcticSqlCommandParser.FailNativeCommandContext ctx);
	/**
	 * Exit a parse tree produced by the {@code failNativeCommand}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitFailNativeCommand(ArcticSqlCommandParser.FailNativeCommandContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTimeZone}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTimeZone(ArcticSqlCommandParser.SetTimeZoneContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTimeZone}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTimeZone(ArcticSqlCommandParser.SetTimeZoneContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setQuotedConfiguration}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetQuotedConfiguration(ArcticSqlCommandParser.SetQuotedConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setQuotedConfiguration}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetQuotedConfiguration(ArcticSqlCommandParser.SetQuotedConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setConfiguration}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetConfiguration(ArcticSqlCommandParser.SetConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setConfiguration}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetConfiguration(ArcticSqlCommandParser.SetConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resetQuotedConfiguration}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterResetQuotedConfiguration(ArcticSqlCommandParser.ResetQuotedConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resetQuotedConfiguration}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitResetQuotedConfiguration(ArcticSqlCommandParser.ResetQuotedConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resetConfiguration}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterResetConfiguration(ArcticSqlCommandParser.ResetConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resetConfiguration}
	 * labeled alternative in {@link ArcticSqlCommandParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitResetConfiguration(ArcticSqlCommandParser.ResetConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#configKey}.
	 * @param ctx the parse tree
	 */
	void enterConfigKey(ArcticSqlCommandParser.ConfigKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#configKey}.
	 * @param ctx the parse tree
	 */
	void exitConfigKey(ArcticSqlCommandParser.ConfigKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#configValue}.
	 * @param ctx the parse tree
	 */
	void enterConfigValue(ArcticSqlCommandParser.ConfigValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#configValue}.
	 * @param ctx the parse tree
	 */
	void exitConfigValue(ArcticSqlCommandParser.ConfigValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 */
	void enterUnsupportedHiveNativeCommands(ArcticSqlCommandParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 */
	void exitUnsupportedHiveNativeCommands(ArcticSqlCommandParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#createTableHeader}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableHeader(ArcticSqlCommandParser.CreateTableHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#createTableHeader}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableHeader(ArcticSqlCommandParser.CreateTableHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#replaceTableHeader}.
	 * @param ctx the parse tree
	 */
	void enterReplaceTableHeader(ArcticSqlCommandParser.ReplaceTableHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#replaceTableHeader}.
	 * @param ctx the parse tree
	 */
	void exitReplaceTableHeader(ArcticSqlCommandParser.ReplaceTableHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#bucketSpec}.
	 * @param ctx the parse tree
	 */
	void enterBucketSpec(ArcticSqlCommandParser.BucketSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#bucketSpec}.
	 * @param ctx the parse tree
	 */
	void exitBucketSpec(ArcticSqlCommandParser.BucketSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#skewSpec}.
	 * @param ctx the parse tree
	 */
	void enterSkewSpec(ArcticSqlCommandParser.SkewSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#skewSpec}.
	 * @param ctx the parse tree
	 */
	void exitSkewSpec(ArcticSqlCommandParser.SkewSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#locationSpec}.
	 * @param ctx the parse tree
	 */
	void enterLocationSpec(ArcticSqlCommandParser.LocationSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#locationSpec}.
	 * @param ctx the parse tree
	 */
	void exitLocationSpec(ArcticSqlCommandParser.LocationSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#commentSpec}.
	 * @param ctx the parse tree
	 */
	void enterCommentSpec(ArcticSqlCommandParser.CommentSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#commentSpec}.
	 * @param ctx the parse tree
	 */
	void exitCommentSpec(ArcticSqlCommandParser.CommentSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(ArcticSqlCommandParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(ArcticSqlCommandParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteTable(ArcticSqlCommandParser.InsertOverwriteTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteTable(ArcticSqlCommandParser.InsertOverwriteTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertIntoTable(ArcticSqlCommandParser.InsertIntoTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertIntoTable(ArcticSqlCommandParser.InsertIntoTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link ArcticSqlCommandParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteHiveDir(ArcticSqlCommandParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link ArcticSqlCommandParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteHiveDir(ArcticSqlCommandParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link ArcticSqlCommandParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteDir(ArcticSqlCommandParser.InsertOverwriteDirContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link ArcticSqlCommandParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteDir(ArcticSqlCommandParser.InsertOverwriteDirContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 */
	void enterPartitionSpecLocation(ArcticSqlCommandParser.PartitionSpecLocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 */
	void exitPartitionSpecLocation(ArcticSqlCommandParser.PartitionSpecLocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#partitionSpec}.
	 * @param ctx the parse tree
	 */
	void enterPartitionSpec(ArcticSqlCommandParser.PartitionSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#partitionSpec}.
	 * @param ctx the parse tree
	 */
	void exitPartitionSpec(ArcticSqlCommandParser.PartitionSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#partitionVal}.
	 * @param ctx the parse tree
	 */
	void enterPartitionVal(ArcticSqlCommandParser.PartitionValContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#partitionVal}.
	 * @param ctx the parse tree
	 */
	void exitPartitionVal(ArcticSqlCommandParser.PartitionValContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#namespace}.
	 * @param ctx the parse tree
	 */
	void enterNamespace(ArcticSqlCommandParser.NamespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#namespace}.
	 * @param ctx the parse tree
	 */
	void exitNamespace(ArcticSqlCommandParser.NamespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#describeFuncName}.
	 * @param ctx the parse tree
	 */
	void enterDescribeFuncName(ArcticSqlCommandParser.DescribeFuncNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#describeFuncName}.
	 * @param ctx the parse tree
	 */
	void exitDescribeFuncName(ArcticSqlCommandParser.DescribeFuncNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#describeColName}.
	 * @param ctx the parse tree
	 */
	void enterDescribeColName(ArcticSqlCommandParser.DescribeColNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#describeColName}.
	 * @param ctx the parse tree
	 */
	void exitDescribeColName(ArcticSqlCommandParser.DescribeColNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#ctes}.
	 * @param ctx the parse tree
	 */
	void enterCtes(ArcticSqlCommandParser.CtesContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#ctes}.
	 * @param ctx the parse tree
	 */
	void exitCtes(ArcticSqlCommandParser.CtesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void enterNamedQuery(ArcticSqlCommandParser.NamedQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void exitNamedQuery(ArcticSqlCommandParser.NamedQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#tableProvider}.
	 * @param ctx the parse tree
	 */
	void enterTableProvider(ArcticSqlCommandParser.TableProviderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#tableProvider}.
	 * @param ctx the parse tree
	 */
	void exitTableProvider(ArcticSqlCommandParser.TableProviderContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#createTableClauses}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableClauses(ArcticSqlCommandParser.CreateTableClausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#createTableClauses}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableClauses(ArcticSqlCommandParser.CreateTableClausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#tablePropertyList}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyList(ArcticSqlCommandParser.TablePropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#tablePropertyList}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyList(ArcticSqlCommandParser.TablePropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#tableProperty}.
	 * @param ctx the parse tree
	 */
	void enterTableProperty(ArcticSqlCommandParser.TablePropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#tableProperty}.
	 * @param ctx the parse tree
	 */
	void exitTableProperty(ArcticSqlCommandParser.TablePropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyKey(ArcticSqlCommandParser.TablePropertyKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyKey(ArcticSqlCommandParser.TablePropertyKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyValue(ArcticSqlCommandParser.TablePropertyValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyValue(ArcticSqlCommandParser.TablePropertyValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#constantList}.
	 * @param ctx the parse tree
	 */
	void enterConstantList(ArcticSqlCommandParser.ConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#constantList}.
	 * @param ctx the parse tree
	 */
	void exitConstantList(ArcticSqlCommandParser.ConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#nestedConstantList}.
	 * @param ctx the parse tree
	 */
	void enterNestedConstantList(ArcticSqlCommandParser.NestedConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#nestedConstantList}.
	 * @param ctx the parse tree
	 */
	void exitNestedConstantList(ArcticSqlCommandParser.NestedConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#createFileFormat}.
	 * @param ctx the parse tree
	 */
	void enterCreateFileFormat(ArcticSqlCommandParser.CreateFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#createFileFormat}.
	 * @param ctx the parse tree
	 */
	void exitCreateFileFormat(ArcticSqlCommandParser.CreateFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticSqlCommandParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterTableFileFormat(ArcticSqlCommandParser.TableFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticSqlCommandParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitTableFileFormat(ArcticSqlCommandParser.TableFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticSqlCommandParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterGenericFileFormat(ArcticSqlCommandParser.GenericFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticSqlCommandParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitGenericFileFormat(ArcticSqlCommandParser.GenericFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#storageHandler}.
	 * @param ctx the parse tree
	 */
	void enterStorageHandler(ArcticSqlCommandParser.StorageHandlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#storageHandler}.
	 * @param ctx the parse tree
	 */
	void exitStorageHandler(ArcticSqlCommandParser.StorageHandlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(ArcticSqlCommandParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(ArcticSqlCommandParser.ResourceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link ArcticSqlCommandParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterSingleInsertQuery(ArcticSqlCommandParser.SingleInsertQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link ArcticSqlCommandParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitSingleInsertQuery(ArcticSqlCommandParser.SingleInsertQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link ArcticSqlCommandParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterMultiInsertQuery(ArcticSqlCommandParser.MultiInsertQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link ArcticSqlCommandParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitMultiInsertQuery(ArcticSqlCommandParser.MultiInsertQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code deleteFromTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterDeleteFromTable(ArcticSqlCommandParser.DeleteFromTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code deleteFromTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitDeleteFromTable(ArcticSqlCommandParser.DeleteFromTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code updateTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterUpdateTable(ArcticSqlCommandParser.UpdateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code updateTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitUpdateTable(ArcticSqlCommandParser.UpdateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mergeIntoTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterMergeIntoTable(ArcticSqlCommandParser.MergeIntoTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mergeIntoTable}
	 * labeled alternative in {@link ArcticSqlCommandParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitMergeIntoTable(ArcticSqlCommandParser.MergeIntoTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#queryOrganization}.
	 * @param ctx the parse tree
	 */
	void enterQueryOrganization(ArcticSqlCommandParser.QueryOrganizationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#queryOrganization}.
	 * @param ctx the parse tree
	 */
	void exitQueryOrganization(ArcticSqlCommandParser.QueryOrganizationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 */
	void enterMultiInsertQueryBody(ArcticSqlCommandParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 */
	void exitMultiInsertQueryBody(ArcticSqlCommandParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterQueryTermDefault(ArcticSqlCommandParser.QueryTermDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitQueryTermDefault(ArcticSqlCommandParser.QueryTermDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterSetOperation(ArcticSqlCommandParser.SetOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitSetOperation(ArcticSqlCommandParser.SetOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterQueryPrimaryDefault(ArcticSqlCommandParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitQueryPrimaryDefault(ArcticSqlCommandParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterFromStmt(ArcticSqlCommandParser.FromStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitFromStmt(ArcticSqlCommandParser.FromStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTable(ArcticSqlCommandParser.TableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTable(ArcticSqlCommandParser.TableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTableDefault1(ArcticSqlCommandParser.InlineTableDefault1Context ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTableDefault1(ArcticSqlCommandParser.InlineTableDefault1Context ctx);
	/**
	 * Enter a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(ArcticSqlCommandParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticSqlCommandParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(ArcticSqlCommandParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void enterSortItem(ArcticSqlCommandParser.SortItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void exitSortItem(ArcticSqlCommandParser.SortItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#fromStatement}.
	 * @param ctx the parse tree
	 */
	void enterFromStatement(ArcticSqlCommandParser.FromStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#fromStatement}.
	 * @param ctx the parse tree
	 */
	void exitFromStatement(ArcticSqlCommandParser.FromStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#fromStatementBody}.
	 * @param ctx the parse tree
	 */
	void enterFromStatementBody(ArcticSqlCommandParser.FromStatementBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#fromStatementBody}.
	 * @param ctx the parse tree
	 */
	void exitFromStatementBody(ArcticSqlCommandParser.FromStatementBodyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link ArcticSqlCommandParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterTransformQuerySpecification(ArcticSqlCommandParser.TransformQuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link ArcticSqlCommandParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitTransformQuerySpecification(ArcticSqlCommandParser.TransformQuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link ArcticSqlCommandParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterRegularQuerySpecification(ArcticSqlCommandParser.RegularQuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link ArcticSqlCommandParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitRegularQuerySpecification(ArcticSqlCommandParser.RegularQuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#transformClause}.
	 * @param ctx the parse tree
	 */
	void enterTransformClause(ArcticSqlCommandParser.TransformClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#transformClause}.
	 * @param ctx the parse tree
	 */
	void exitTransformClause(ArcticSqlCommandParser.TransformClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(ArcticSqlCommandParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(ArcticSqlCommandParser.SelectClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#setClause}.
	 * @param ctx the parse tree
	 */
	void enterSetClause(ArcticSqlCommandParser.SetClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#setClause}.
	 * @param ctx the parse tree
	 */
	void exitSetClause(ArcticSqlCommandParser.SetClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#matchedClause}.
	 * @param ctx the parse tree
	 */
	void enterMatchedClause(ArcticSqlCommandParser.MatchedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#matchedClause}.
	 * @param ctx the parse tree
	 */
	void exitMatchedClause(ArcticSqlCommandParser.MatchedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#notMatchedClause}.
	 * @param ctx the parse tree
	 */
	void enterNotMatchedClause(ArcticSqlCommandParser.NotMatchedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#notMatchedClause}.
	 * @param ctx the parse tree
	 */
	void exitNotMatchedClause(ArcticSqlCommandParser.NotMatchedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#matchedAction}.
	 * @param ctx the parse tree
	 */
	void enterMatchedAction(ArcticSqlCommandParser.MatchedActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#matchedAction}.
	 * @param ctx the parse tree
	 */
	void exitMatchedAction(ArcticSqlCommandParser.MatchedActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#notMatchedAction}.
	 * @param ctx the parse tree
	 */
	void enterNotMatchedAction(ArcticSqlCommandParser.NotMatchedActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#notMatchedAction}.
	 * @param ctx the parse tree
	 */
	void exitNotMatchedAction(ArcticSqlCommandParser.NotMatchedActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#assignmentList}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentList(ArcticSqlCommandParser.AssignmentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#assignmentList}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentList(ArcticSqlCommandParser.AssignmentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(ArcticSqlCommandParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(ArcticSqlCommandParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void enterWhereClause(ArcticSqlCommandParser.WhereClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void exitWhereClause(ArcticSqlCommandParser.WhereClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void enterHavingClause(ArcticSqlCommandParser.HavingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void exitHavingClause(ArcticSqlCommandParser.HavingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#hint}.
	 * @param ctx the parse tree
	 */
	void enterHint(ArcticSqlCommandParser.HintContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#hint}.
	 * @param ctx the parse tree
	 */
	void exitHint(ArcticSqlCommandParser.HintContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#hintStatement}.
	 * @param ctx the parse tree
	 */
	void enterHintStatement(ArcticSqlCommandParser.HintStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#hintStatement}.
	 * @param ctx the parse tree
	 */
	void exitHintStatement(ArcticSqlCommandParser.HintStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFromClause(ArcticSqlCommandParser.FromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFromClause(ArcticSqlCommandParser.FromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#aggregationClause}.
	 * @param ctx the parse tree
	 */
	void enterAggregationClause(ArcticSqlCommandParser.AggregationClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#aggregationClause}.
	 * @param ctx the parse tree
	 */
	void exitAggregationClause(ArcticSqlCommandParser.AggregationClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#groupByClause}.
	 * @param ctx the parse tree
	 */
	void enterGroupByClause(ArcticSqlCommandParser.GroupByClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#groupByClause}.
	 * @param ctx the parse tree
	 */
	void exitGroupByClause(ArcticSqlCommandParser.GroupByClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 */
	void enterGroupingAnalytics(ArcticSqlCommandParser.GroupingAnalyticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 */
	void exitGroupingAnalytics(ArcticSqlCommandParser.GroupingAnalyticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterGroupingElement(ArcticSqlCommandParser.GroupingElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitGroupingElement(ArcticSqlCommandParser.GroupingElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void enterGroupingSet(ArcticSqlCommandParser.GroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void exitGroupingSet(ArcticSqlCommandParser.GroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#pivotClause}.
	 * @param ctx the parse tree
	 */
	void enterPivotClause(ArcticSqlCommandParser.PivotClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#pivotClause}.
	 * @param ctx the parse tree
	 */
	void exitPivotClause(ArcticSqlCommandParser.PivotClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#pivotColumn}.
	 * @param ctx the parse tree
	 */
	void enterPivotColumn(ArcticSqlCommandParser.PivotColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#pivotColumn}.
	 * @param ctx the parse tree
	 */
	void exitPivotColumn(ArcticSqlCommandParser.PivotColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#pivotValue}.
	 * @param ctx the parse tree
	 */
	void enterPivotValue(ArcticSqlCommandParser.PivotValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#pivotValue}.
	 * @param ctx the parse tree
	 */
	void exitPivotValue(ArcticSqlCommandParser.PivotValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#lateralView}.
	 * @param ctx the parse tree
	 */
	void enterLateralView(ArcticSqlCommandParser.LateralViewContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#lateralView}.
	 * @param ctx the parse tree
	 */
	void exitLateralView(ArcticSqlCommandParser.LateralViewContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterSetQuantifier(ArcticSqlCommandParser.SetQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitSetQuantifier(ArcticSqlCommandParser.SetQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterRelation(ArcticSqlCommandParser.RelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitRelation(ArcticSqlCommandParser.RelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void enterJoinRelation(ArcticSqlCommandParser.JoinRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void exitJoinRelation(ArcticSqlCommandParser.JoinRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#joinType}.
	 * @param ctx the parse tree
	 */
	void enterJoinType(ArcticSqlCommandParser.JoinTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#joinType}.
	 * @param ctx the parse tree
	 */
	void exitJoinType(ArcticSqlCommandParser.JoinTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void enterJoinCriteria(ArcticSqlCommandParser.JoinCriteriaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void exitJoinCriteria(ArcticSqlCommandParser.JoinCriteriaContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#sample}.
	 * @param ctx the parse tree
	 */
	void enterSample(ArcticSqlCommandParser.SampleContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#sample}.
	 * @param ctx the parse tree
	 */
	void exitSample(ArcticSqlCommandParser.SampleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticSqlCommandParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByPercentile(ArcticSqlCommandParser.SampleByPercentileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticSqlCommandParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByPercentile(ArcticSqlCommandParser.SampleByPercentileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticSqlCommandParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByRows(ArcticSqlCommandParser.SampleByRowsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticSqlCommandParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByRows(ArcticSqlCommandParser.SampleByRowsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticSqlCommandParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByBucket(ArcticSqlCommandParser.SampleByBucketContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticSqlCommandParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByBucket(ArcticSqlCommandParser.SampleByBucketContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticSqlCommandParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByBytes(ArcticSqlCommandParser.SampleByBytesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticSqlCommandParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByBytes(ArcticSqlCommandParser.SampleByBytesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierList(ArcticSqlCommandParser.IdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierList(ArcticSqlCommandParser.IdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierSeq(ArcticSqlCommandParser.IdentifierSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierSeq(ArcticSqlCommandParser.IdentifierSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 */
	void enterOrderedIdentifierList(ArcticSqlCommandParser.OrderedIdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 */
	void exitOrderedIdentifierList(ArcticSqlCommandParser.OrderedIdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterOrderedIdentifier(ArcticSqlCommandParser.OrderedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitOrderedIdentifier(ArcticSqlCommandParser.OrderedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#identifierCommentList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierCommentList(ArcticSqlCommandParser.IdentifierCommentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#identifierCommentList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierCommentList(ArcticSqlCommandParser.IdentifierCommentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#identifierComment}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierComment(ArcticSqlCommandParser.IdentifierCommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#identifierComment}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierComment(ArcticSqlCommandParser.IdentifierCommentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticSqlCommandParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableName(ArcticSqlCommandParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticSqlCommandParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableName(ArcticSqlCommandParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticSqlCommandParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterAliasedQuery(ArcticSqlCommandParser.AliasedQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticSqlCommandParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitAliasedQuery(ArcticSqlCommandParser.AliasedQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticSqlCommandParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterAliasedRelation(ArcticSqlCommandParser.AliasedRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticSqlCommandParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitAliasedRelation(ArcticSqlCommandParser.AliasedRelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticSqlCommandParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTableDefault2(ArcticSqlCommandParser.InlineTableDefault2Context ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticSqlCommandParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTableDefault2(ArcticSqlCommandParser.InlineTableDefault2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticSqlCommandParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableValuedFunction(ArcticSqlCommandParser.TableValuedFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticSqlCommandParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableValuedFunction(ArcticSqlCommandParser.TableValuedFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void enterInlineTable(ArcticSqlCommandParser.InlineTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void exitInlineTable(ArcticSqlCommandParser.InlineTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#functionTable}.
	 * @param ctx the parse tree
	 */
	void enterFunctionTable(ArcticSqlCommandParser.FunctionTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#functionTable}.
	 * @param ctx the parse tree
	 */
	void exitFunctionTable(ArcticSqlCommandParser.FunctionTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#tableAlias}.
	 * @param ctx the parse tree
	 */
	void enterTableAlias(ArcticSqlCommandParser.TableAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#tableAlias}.
	 * @param ctx the parse tree
	 */
	void exitTableAlias(ArcticSqlCommandParser.TableAliasContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticSqlCommandParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void enterRowFormatSerde(ArcticSqlCommandParser.RowFormatSerdeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticSqlCommandParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void exitRowFormatSerde(ArcticSqlCommandParser.RowFormatSerdeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticSqlCommandParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void enterRowFormatDelimited(ArcticSqlCommandParser.RowFormatDelimitedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticSqlCommandParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void exitRowFormatDelimited(ArcticSqlCommandParser.RowFormatDelimitedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#multipartIdentifierList}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifierList(ArcticSqlCommandParser.MultipartIdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#multipartIdentifierList}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifierList(ArcticSqlCommandParser.MultipartIdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifier(ArcticSqlCommandParser.MultipartIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifier(ArcticSqlCommandParser.MultipartIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#tableIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterTableIdentifier(ArcticSqlCommandParser.TableIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#tableIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitTableIdentifier(ArcticSqlCommandParser.TableIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#functionIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterFunctionIdentifier(ArcticSqlCommandParser.FunctionIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#functionIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitFunctionIdentifier(ArcticSqlCommandParser.FunctionIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#namedExpression}.
	 * @param ctx the parse tree
	 */
	void enterNamedExpression(ArcticSqlCommandParser.NamedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#namedExpression}.
	 * @param ctx the parse tree
	 */
	void exitNamedExpression(ArcticSqlCommandParser.NamedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 */
	void enterNamedExpressionSeq(ArcticSqlCommandParser.NamedExpressionSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 */
	void exitNamedExpressionSeq(ArcticSqlCommandParser.NamedExpressionSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#partitionFieldList}.
	 * @param ctx the parse tree
	 */
	void enterPartitionFieldList(ArcticSqlCommandParser.PartitionFieldListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#partitionFieldList}.
	 * @param ctx the parse tree
	 */
	void exitPartitionFieldList(ArcticSqlCommandParser.PartitionFieldListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link ArcticSqlCommandParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void enterPartitionTransform(ArcticSqlCommandParser.PartitionTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link ArcticSqlCommandParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void exitPartitionTransform(ArcticSqlCommandParser.PartitionTransformContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link ArcticSqlCommandParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void enterPartitionColumn(ArcticSqlCommandParser.PartitionColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link ArcticSqlCommandParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void exitPartitionColumn(ArcticSqlCommandParser.PartitionColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link ArcticSqlCommandParser#transform}.
	 * @param ctx the parse tree
	 */
	void enterIdentityTransform(ArcticSqlCommandParser.IdentityTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link ArcticSqlCommandParser#transform}.
	 * @param ctx the parse tree
	 */
	void exitIdentityTransform(ArcticSqlCommandParser.IdentityTransformContext ctx);
	/**
	 * Enter a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link ArcticSqlCommandParser#transform}.
	 * @param ctx the parse tree
	 */
	void enterApplyTransform(ArcticSqlCommandParser.ApplyTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link ArcticSqlCommandParser#transform}.
	 * @param ctx the parse tree
	 */
	void exitApplyTransform(ArcticSqlCommandParser.ApplyTransformContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#transformArgument}.
	 * @param ctx the parse tree
	 */
	void enterTransformArgument(ArcticSqlCommandParser.TransformArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#transformArgument}.
	 * @param ctx the parse tree
	 */
	void exitTransformArgument(ArcticSqlCommandParser.TransformArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ArcticSqlCommandParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ArcticSqlCommandParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#expressionSeq}.
	 * @param ctx the parse tree
	 */
	void enterExpressionSeq(ArcticSqlCommandParser.ExpressionSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#expressionSeq}.
	 * @param ctx the parse tree
	 */
	void exitExpressionSeq(ArcticSqlCommandParser.ExpressionSeqContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticSqlCommandParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalNot(ArcticSqlCommandParser.LogicalNotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticSqlCommandParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalNot(ArcticSqlCommandParser.LogicalNotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link ArcticSqlCommandParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterPredicated(ArcticSqlCommandParser.PredicatedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link ArcticSqlCommandParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitPredicated(ArcticSqlCommandParser.PredicatedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticSqlCommandParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterExists(ArcticSqlCommandParser.ExistsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticSqlCommandParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitExists(ArcticSqlCommandParser.ExistsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticSqlCommandParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalBinary(ArcticSqlCommandParser.LogicalBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticSqlCommandParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalBinary(ArcticSqlCommandParser.LogicalBinaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(ArcticSqlCommandParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(ArcticSqlCommandParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticSqlCommandParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterValueExpressionDefault(ArcticSqlCommandParser.ValueExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticSqlCommandParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitValueExpressionDefault(ArcticSqlCommandParser.ValueExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticSqlCommandParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterComparison(ArcticSqlCommandParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticSqlCommandParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitComparison(ArcticSqlCommandParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticSqlCommandParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticBinary(ArcticSqlCommandParser.ArithmeticBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticSqlCommandParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticBinary(ArcticSqlCommandParser.ArithmeticBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticSqlCommandParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticUnary(ArcticSqlCommandParser.ArithmeticUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticSqlCommandParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticUnary(ArcticSqlCommandParser.ArithmeticUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStruct(ArcticSqlCommandParser.StructContext ctx);
	/**
	 * Exit a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStruct(ArcticSqlCommandParser.StructContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterDereference(ArcticSqlCommandParser.DereferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitDereference(ArcticSqlCommandParser.DereferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCase(ArcticSqlCommandParser.SimpleCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCase(ArcticSqlCommandParser.SimpleCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCurrentLike(ArcticSqlCommandParser.CurrentLikeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCurrentLike(ArcticSqlCommandParser.CurrentLikeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterColumnReference(ArcticSqlCommandParser.ColumnReferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitColumnReference(ArcticSqlCommandParser.ColumnReferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterRowConstructor(ArcticSqlCommandParser.RowConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitRowConstructor(ArcticSqlCommandParser.RowConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLast(ArcticSqlCommandParser.LastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLast(ArcticSqlCommandParser.LastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStar(ArcticSqlCommandParser.StarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStar(ArcticSqlCommandParser.StarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterOverlay(ArcticSqlCommandParser.OverlayContext ctx);
	/**
	 * Exit a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitOverlay(ArcticSqlCommandParser.OverlayContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(ArcticSqlCommandParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(ArcticSqlCommandParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryExpression(ArcticSqlCommandParser.SubqueryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryExpression(ArcticSqlCommandParser.SubqueryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code substring}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubstring(ArcticSqlCommandParser.SubstringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code substring}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubstring(ArcticSqlCommandParser.SubstringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCast(ArcticSqlCommandParser.CastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCast(ArcticSqlCommandParser.CastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantDefault(ArcticSqlCommandParser.ConstantDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantDefault(ArcticSqlCommandParser.ConstantDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambda(ArcticSqlCommandParser.LambdaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambda(ArcticSqlCommandParser.LambdaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(ArcticSqlCommandParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(ArcticSqlCommandParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code extract}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterExtract(ArcticSqlCommandParser.ExtractContext ctx);
	/**
	 * Exit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitExtract(ArcticSqlCommandParser.ExtractContext ctx);
	/**
	 * Enter a parse tree produced by the {@code trim}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTrim(ArcticSqlCommandParser.TrimContext ctx);
	/**
	 * Exit a parse tree produced by the {@code trim}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTrim(ArcticSqlCommandParser.TrimContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(ArcticSqlCommandParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(ArcticSqlCommandParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSearchedCase(ArcticSqlCommandParser.SearchedCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSearchedCase(ArcticSqlCommandParser.SearchedCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPosition(ArcticSqlCommandParser.PositionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPosition(ArcticSqlCommandParser.PositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFirst(ArcticSqlCommandParser.FirstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticSqlCommandParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFirst(ArcticSqlCommandParser.FirstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNullLiteral(ArcticSqlCommandParser.NullLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNullLiteral(ArcticSqlCommandParser.NullLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterIntervalLiteral(ArcticSqlCommandParser.IntervalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitIntervalLiteral(ArcticSqlCommandParser.IntervalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterTypeConstructor(ArcticSqlCommandParser.TypeConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitTypeConstructor(ArcticSqlCommandParser.TypeConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(ArcticSqlCommandParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(ArcticSqlCommandParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(ArcticSqlCommandParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(ArcticSqlCommandParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(ArcticSqlCommandParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(ArcticSqlCommandParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(ArcticSqlCommandParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(ArcticSqlCommandParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticOperator(ArcticSqlCommandParser.ArithmeticOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticOperator(ArcticSqlCommandParser.ArithmeticOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#predicateOperator}.
	 * @param ctx the parse tree
	 */
	void enterPredicateOperator(ArcticSqlCommandParser.PredicateOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#predicateOperator}.
	 * @param ctx the parse tree
	 */
	void exitPredicateOperator(ArcticSqlCommandParser.PredicateOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(ArcticSqlCommandParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(ArcticSqlCommandParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#interval}.
	 * @param ctx the parse tree
	 */
	void enterInterval(ArcticSqlCommandParser.IntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#interval}.
	 * @param ctx the parse tree
	 */
	void exitInterval(ArcticSqlCommandParser.IntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingMultiUnitsInterval(ArcticSqlCommandParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingMultiUnitsInterval(ArcticSqlCommandParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void enterMultiUnitsInterval(ArcticSqlCommandParser.MultiUnitsIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void exitMultiUnitsInterval(ArcticSqlCommandParser.MultiUnitsIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingUnitToUnitInterval(ArcticSqlCommandParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingUnitToUnitInterval(ArcticSqlCommandParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void enterUnitToUnitInterval(ArcticSqlCommandParser.UnitToUnitIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void exitUnitToUnitInterval(ArcticSqlCommandParser.UnitToUnitIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#intervalValue}.
	 * @param ctx the parse tree
	 */
	void enterIntervalValue(ArcticSqlCommandParser.IntervalValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#intervalValue}.
	 * @param ctx the parse tree
	 */
	void exitIntervalValue(ArcticSqlCommandParser.IntervalValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#colPosition}.
	 * @param ctx the parse tree
	 */
	void enterColPosition(ArcticSqlCommandParser.ColPositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#colPosition}.
	 * @param ctx the parse tree
	 */
	void exitColPosition(ArcticSqlCommandParser.ColPositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticSqlCommandParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterComplexDataType(ArcticSqlCommandParser.ComplexDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticSqlCommandParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitComplexDataType(ArcticSqlCommandParser.ComplexDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link ArcticSqlCommandParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterYearMonthIntervalDataType(ArcticSqlCommandParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link ArcticSqlCommandParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitYearMonthIntervalDataType(ArcticSqlCommandParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link ArcticSqlCommandParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDayTimeIntervalDataType(ArcticSqlCommandParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link ArcticSqlCommandParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDayTimeIntervalDataType(ArcticSqlCommandParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticSqlCommandParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveDataType(ArcticSqlCommandParser.PrimitiveDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticSqlCommandParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveDataType(ArcticSqlCommandParser.PrimitiveDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#qualifiedColTypeWithPositionList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedColTypeWithPositionList(ArcticSqlCommandParser.QualifiedColTypeWithPositionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#qualifiedColTypeWithPositionList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedColTypeWithPositionList(ArcticSqlCommandParser.QualifiedColTypeWithPositionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#qualifiedColTypeWithPosition}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedColTypeWithPosition(ArcticSqlCommandParser.QualifiedColTypeWithPositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#qualifiedColTypeWithPosition}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedColTypeWithPosition(ArcticSqlCommandParser.QualifiedColTypeWithPositionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void enterColTypeList(ArcticSqlCommandParser.ColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void exitColTypeList(ArcticSqlCommandParser.ColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#colType}.
	 * @param ctx the parse tree
	 */
	void enterColType(ArcticSqlCommandParser.ColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#colType}.
	 * @param ctx the parse tree
	 */
	void exitColType(ArcticSqlCommandParser.ColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#complexColTypeList}.
	 * @param ctx the parse tree
	 */
	void enterComplexColTypeList(ArcticSqlCommandParser.ComplexColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#complexColTypeList}.
	 * @param ctx the parse tree
	 */
	void exitComplexColTypeList(ArcticSqlCommandParser.ComplexColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#complexColType}.
	 * @param ctx the parse tree
	 */
	void enterComplexColType(ArcticSqlCommandParser.ComplexColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#complexColType}.
	 * @param ctx the parse tree
	 */
	void exitComplexColType(ArcticSqlCommandParser.ComplexColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(ArcticSqlCommandParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(ArcticSqlCommandParser.WhenClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#windowClause}.
	 * @param ctx the parse tree
	 */
	void enterWindowClause(ArcticSqlCommandParser.WindowClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#windowClause}.
	 * @param ctx the parse tree
	 */
	void exitWindowClause(ArcticSqlCommandParser.WindowClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#namedWindow}.
	 * @param ctx the parse tree
	 */
	void enterNamedWindow(ArcticSqlCommandParser.NamedWindowContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#namedWindow}.
	 * @param ctx the parse tree
	 */
	void exitNamedWindow(ArcticSqlCommandParser.NamedWindowContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticSqlCommandParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void enterWindowRef(ArcticSqlCommandParser.WindowRefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticSqlCommandParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void exitWindowRef(ArcticSqlCommandParser.WindowRefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticSqlCommandParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void enterWindowDef(ArcticSqlCommandParser.WindowDefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticSqlCommandParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void exitWindowDef(ArcticSqlCommandParser.WindowDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void enterWindowFrame(ArcticSqlCommandParser.WindowFrameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void exitWindowFrame(ArcticSqlCommandParser.WindowFrameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterFrameBound(ArcticSqlCommandParser.FrameBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitFrameBound(ArcticSqlCommandParser.FrameBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedNameList(ArcticSqlCommandParser.QualifiedNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedNameList(ArcticSqlCommandParser.QualifiedNameListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#functionName}.
	 * @param ctx the parse tree
	 */
	void enterFunctionName(ArcticSqlCommandParser.FunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#functionName}.
	 * @param ctx the parse tree
	 */
	void exitFunctionName(ArcticSqlCommandParser.FunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(ArcticSqlCommandParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(ArcticSqlCommandParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingIdentifier(ArcticSqlCommandParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingIdentifier(ArcticSqlCommandParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticSqlCommandParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void enterErrorIdent(ArcticSqlCommandParser.ErrorIdentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticSqlCommandParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void exitErrorIdent(ArcticSqlCommandParser.ErrorIdentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticSqlCommandParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void enterRealIdent(ArcticSqlCommandParser.RealIdentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticSqlCommandParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void exitRealIdent(ArcticSqlCommandParser.RealIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(ArcticSqlCommandParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(ArcticSqlCommandParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSqlCommandParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedIdentifier(ArcticSqlCommandParser.UnquotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSqlCommandParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedIdentifier(ArcticSqlCommandParser.UnquotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSqlCommandParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifierAlternative(ArcticSqlCommandParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSqlCommandParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifierAlternative(ArcticSqlCommandParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifier(ArcticSqlCommandParser.QuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifier(ArcticSqlCommandParser.QuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void enterExponentLiteral(ArcticSqlCommandParser.ExponentLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void exitExponentLiteral(ArcticSqlCommandParser.ExponentLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDecimalLiteral(ArcticSqlCommandParser.DecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDecimalLiteral(ArcticSqlCommandParser.DecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void enterLegacyDecimalLiteral(ArcticSqlCommandParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void exitLegacyDecimalLiteral(ArcticSqlCommandParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(ArcticSqlCommandParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(ArcticSqlCommandParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigIntLiteral(ArcticSqlCommandParser.BigIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigIntLiteral(ArcticSqlCommandParser.BigIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void enterSmallIntLiteral(ArcticSqlCommandParser.SmallIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void exitSmallIntLiteral(ArcticSqlCommandParser.SmallIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void enterTinyIntLiteral(ArcticSqlCommandParser.TinyIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void exitTinyIntLiteral(ArcticSqlCommandParser.TinyIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(ArcticSqlCommandParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(ArcticSqlCommandParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void enterFloatLiteral(ArcticSqlCommandParser.FloatLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void exitFloatLiteral(ArcticSqlCommandParser.FloatLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigDecimalLiteral(ArcticSqlCommandParser.BigDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticSqlCommandParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigDecimalLiteral(ArcticSqlCommandParser.BigDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#alterColumnAction}.
	 * @param ctx the parse tree
	 */
	void enterAlterColumnAction(ArcticSqlCommandParser.AlterColumnActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#alterColumnAction}.
	 * @param ctx the parse tree
	 */
	void exitAlterColumnAction(ArcticSqlCommandParser.AlterColumnActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 */
	void enterAnsiNonReserved(ArcticSqlCommandParser.AnsiNonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 */
	void exitAnsiNonReserved(ArcticSqlCommandParser.AnsiNonReservedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#strictNonReserved}.
	 * @param ctx the parse tree
	 */
	void enterStrictNonReserved(ArcticSqlCommandParser.StrictNonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#strictNonReserved}.
	 * @param ctx the parse tree
	 */
	void exitStrictNonReserved(ArcticSqlCommandParser.StrictNonReservedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void enterNonReserved(ArcticSqlCommandParser.NonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void exitNonReserved(ArcticSqlCommandParser.NonReservedContext ctx);
}