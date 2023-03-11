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

// Generated from com/netease/arctic/spark/sql/parser/ArcticExtendSparkSql.g4 by ANTLR 4.7
package com.netease.arctic.spark.sql.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ArcticExtendSparkSqlParser}.
 */
public interface ArcticExtendSparkSqlListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#arcticCommand}.
	 * @param ctx the parse tree
	 */
	void enterArcticCommand(ArcticExtendSparkSqlParser.ArcticCommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#arcticCommand}.
	 * @param ctx the parse tree
	 */
	void exitArcticCommand(ArcticExtendSparkSqlParser.ArcticCommandContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTableWithPk}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#arcticStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableWithPk(ArcticExtendSparkSqlParser.CreateTableWithPkContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTableWithPk}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#arcticStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableWithPk(ArcticExtendSparkSqlParser.CreateTableWithPkContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#createTableWithPrimaryKey}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableWithPrimaryKey(ArcticExtendSparkSqlParser.CreateTableWithPrimaryKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#createTableWithPrimaryKey}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableWithPrimaryKey(ArcticExtendSparkSqlParser.CreateTableWithPrimaryKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#primarySpec}.
	 * @param ctx the parse tree
	 */
	void enterPrimarySpec(ArcticExtendSparkSqlParser.PrimarySpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#primarySpec}.
	 * @param ctx the parse tree
	 */
	void exitPrimarySpec(ArcticExtendSparkSqlParser.PrimarySpecContext ctx);
	/**
	 * Enter a parse tree produced by the {@code colListWithPk}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void enterColListWithPk(ArcticExtendSparkSqlParser.ColListWithPkContext ctx);
	/**
	 * Exit a parse tree produced by the {@code colListWithPk}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void exitColListWithPk(ArcticExtendSparkSqlParser.ColListWithPkContext ctx);
	/**
	 * Enter a parse tree produced by the {@code colListOnlyPk}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void enterColListOnlyPk(ArcticExtendSparkSqlParser.ColListOnlyPkContext ctx);
	/**
	 * Exit a parse tree produced by the {@code colListOnlyPk}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 */
	void exitColListOnlyPk(ArcticExtendSparkSqlParser.ColListOnlyPkContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void enterSingleStatement(ArcticExtendSparkSqlParser.SingleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void exitSingleStatement(ArcticExtendSparkSqlParser.SingleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void enterSingleExpression(ArcticExtendSparkSqlParser.SingleExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleExpression}.
	 * @param ctx the parse tree
	 */
	void exitSingleExpression(ArcticExtendSparkSqlParser.SingleExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleTableIdentifier(ArcticExtendSparkSqlParser.SingleTableIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleTableIdentifier(ArcticExtendSparkSqlParser.SingleTableIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#singleMultipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleMultipartIdentifier(ArcticExtendSparkSqlParser.SingleMultipartIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleMultipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleMultipartIdentifier(ArcticExtendSparkSqlParser.SingleMultipartIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterSingleFunctionIdentifier(ArcticExtendSparkSqlParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitSingleFunctionIdentifier(ArcticExtendSparkSqlParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#singleDataType}.
	 * @param ctx the parse tree
	 */
	void enterSingleDataType(ArcticExtendSparkSqlParser.SingleDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleDataType}.
	 * @param ctx the parse tree
	 */
	void exitSingleDataType(ArcticExtendSparkSqlParser.SingleDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#singleTableSchema}.
	 * @param ctx the parse tree
	 */
	void enterSingleTableSchema(ArcticExtendSparkSqlParser.SingleTableSchemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleTableSchema}.
	 * @param ctx the parse tree
	 */
	void exitSingleTableSchema(ArcticExtendSparkSqlParser.SingleTableSchemaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatementDefault(ArcticExtendSparkSqlParser.StatementDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatementDefault(ArcticExtendSparkSqlParser.StatementDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dmlStatement}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDmlStatement(ArcticExtendSparkSqlParser.DmlStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dmlStatement}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDmlStatement(ArcticExtendSparkSqlParser.DmlStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code use}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUse(ArcticExtendSparkSqlParser.UseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code use}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUse(ArcticExtendSparkSqlParser.UseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateNamespace(ArcticExtendSparkSqlParser.CreateNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateNamespace(ArcticExtendSparkSqlParser.CreateNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNamespaceProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetNamespaceProperties(ArcticExtendSparkSqlParser.SetNamespacePropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNamespaceProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetNamespaceProperties(ArcticExtendSparkSqlParser.SetNamespacePropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNamespaceLocation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetNamespaceLocation(ArcticExtendSparkSqlParser.SetNamespaceLocationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNamespaceLocation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetNamespaceLocation(ArcticExtendSparkSqlParser.SetNamespaceLocationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropNamespace(ArcticExtendSparkSqlParser.DropNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropNamespace(ArcticExtendSparkSqlParser.DropNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showNamespaces}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowNamespaces(ArcticExtendSparkSqlParser.ShowNamespacesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showNamespaces}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowNamespaces(ArcticExtendSparkSqlParser.ShowNamespacesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTable(ArcticExtendSparkSqlParser.CreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTable(ArcticExtendSparkSqlParser.CreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTableLike}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableLike(ArcticExtendSparkSqlParser.CreateTableLikeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTableLike}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableLike(ArcticExtendSparkSqlParser.CreateTableLikeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code replaceTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterReplaceTable(ArcticExtendSparkSqlParser.ReplaceTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code replaceTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitReplaceTable(ArcticExtendSparkSqlParser.ReplaceTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze(ArcticExtendSparkSqlParser.AnalyzeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze(ArcticExtendSparkSqlParser.AnalyzeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code analyzeTables}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyzeTables(ArcticExtendSparkSqlParser.AnalyzeTablesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code analyzeTables}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyzeTables(ArcticExtendSparkSqlParser.AnalyzeTablesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addTableColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAddTableColumns(ArcticExtendSparkSqlParser.AddTableColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addTableColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAddTableColumns(ArcticExtendSparkSqlParser.AddTableColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTableColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTableColumn(ArcticExtendSparkSqlParser.RenameTableColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTableColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTableColumn(ArcticExtendSparkSqlParser.RenameTableColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTableColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTableColumns(ArcticExtendSparkSqlParser.DropTableColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTableColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTableColumns(ArcticExtendSparkSqlParser.DropTableColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTable(ArcticExtendSparkSqlParser.RenameTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTable(ArcticExtendSparkSqlParser.RenameTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTableProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTableProperties(ArcticExtendSparkSqlParser.SetTablePropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTableProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTableProperties(ArcticExtendSparkSqlParser.SetTablePropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unsetTableProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUnsetTableProperties(ArcticExtendSparkSqlParser.UnsetTablePropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unsetTableProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUnsetTableProperties(ArcticExtendSparkSqlParser.UnsetTablePropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterTableAlterColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterTableAlterColumn(ArcticExtendSparkSqlParser.AlterTableAlterColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterTableAlterColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterTableAlterColumn(ArcticExtendSparkSqlParser.AlterTableAlterColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hiveChangeColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterHiveChangeColumn(ArcticExtendSparkSqlParser.HiveChangeColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hiveChangeColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitHiveChangeColumn(ArcticExtendSparkSqlParser.HiveChangeColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hiveReplaceColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterHiveReplaceColumns(ArcticExtendSparkSqlParser.HiveReplaceColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hiveReplaceColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitHiveReplaceColumns(ArcticExtendSparkSqlParser.HiveReplaceColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTableSerDe}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTableSerDe(ArcticExtendSparkSqlParser.SetTableSerDeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTableSerDe}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTableSerDe(ArcticExtendSparkSqlParser.SetTableSerDeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addTablePartition}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAddTablePartition(ArcticExtendSparkSqlParser.AddTablePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addTablePartition}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAddTablePartition(ArcticExtendSparkSqlParser.AddTablePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTablePartition}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTablePartition(ArcticExtendSparkSqlParser.RenameTablePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTablePartition}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTablePartition(ArcticExtendSparkSqlParser.RenameTablePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTablePartitions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTablePartitions(ArcticExtendSparkSqlParser.DropTablePartitionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTablePartitions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTablePartitions(ArcticExtendSparkSqlParser.DropTablePartitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTableLocation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTableLocation(ArcticExtendSparkSqlParser.SetTableLocationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTableLocation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTableLocation(ArcticExtendSparkSqlParser.SetTableLocationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code recoverPartitions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRecoverPartitions(ArcticExtendSparkSqlParser.RecoverPartitionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code recoverPartitions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRecoverPartitions(ArcticExtendSparkSqlParser.RecoverPartitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTable(ArcticExtendSparkSqlParser.DropTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTable(ArcticExtendSparkSqlParser.DropTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropView(ArcticExtendSparkSqlParser.DropViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropView(ArcticExtendSparkSqlParser.DropViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createView}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateView(ArcticExtendSparkSqlParser.CreateViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createView}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateView(ArcticExtendSparkSqlParser.CreateViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTempViewUsing}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTempViewUsing(ArcticExtendSparkSqlParser.CreateTempViewUsingContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTempViewUsing}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTempViewUsing(ArcticExtendSparkSqlParser.CreateTempViewUsingContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterViewQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterViewQuery(ArcticExtendSparkSqlParser.AlterViewQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterViewQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterViewQuery(ArcticExtendSparkSqlParser.AlterViewQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateFunction(ArcticExtendSparkSqlParser.CreateFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateFunction(ArcticExtendSparkSqlParser.CreateFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropFunction(ArcticExtendSparkSqlParser.DropFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropFunction(ArcticExtendSparkSqlParser.DropFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code explain}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterExplain(ArcticExtendSparkSqlParser.ExplainContext ctx);
	/**
	 * Exit a parse tree produced by the {@code explain}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitExplain(ArcticExtendSparkSqlParser.ExplainContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTables(ArcticExtendSparkSqlParser.ShowTablesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTables(ArcticExtendSparkSqlParser.ShowTablesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTableExtended}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTableExtended(ArcticExtendSparkSqlParser.ShowTableExtendedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTableExtended}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTableExtended(ArcticExtendSparkSqlParser.ShowTableExtendedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTblProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTblProperties(ArcticExtendSparkSqlParser.ShowTblPropertiesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTblProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTblProperties(ArcticExtendSparkSqlParser.ShowTblPropertiesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowColumns(ArcticExtendSparkSqlParser.ShowColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowColumns(ArcticExtendSparkSqlParser.ShowColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showViews}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowViews(ArcticExtendSparkSqlParser.ShowViewsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showViews}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowViews(ArcticExtendSparkSqlParser.ShowViewsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showPartitions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowPartitions(ArcticExtendSparkSqlParser.ShowPartitionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showPartitions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowPartitions(ArcticExtendSparkSqlParser.ShowPartitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowFunctions(ArcticExtendSparkSqlParser.ShowFunctionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowFunctions(ArcticExtendSparkSqlParser.ShowFunctionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateTable(ArcticExtendSparkSqlParser.ShowCreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateTable(ArcticExtendSparkSqlParser.ShowCreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCurrentNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCurrentNamespace(ArcticExtendSparkSqlParser.ShowCurrentNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCurrentNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCurrentNamespace(ArcticExtendSparkSqlParser.ShowCurrentNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeFunction(ArcticExtendSparkSqlParser.DescribeFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeFunction(ArcticExtendSparkSqlParser.DescribeFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeNamespace(ArcticExtendSparkSqlParser.DescribeNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeNamespace(ArcticExtendSparkSqlParser.DescribeNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeRelation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeRelation(ArcticExtendSparkSqlParser.DescribeRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeRelation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeRelation(ArcticExtendSparkSqlParser.DescribeRelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeQuery(ArcticExtendSparkSqlParser.DescribeQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeQuery(ArcticExtendSparkSqlParser.DescribeQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code commentNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCommentNamespace(ArcticExtendSparkSqlParser.CommentNamespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code commentNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCommentNamespace(ArcticExtendSparkSqlParser.CommentNamespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code commentTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCommentTable(ArcticExtendSparkSqlParser.CommentTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code commentTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCommentTable(ArcticExtendSparkSqlParser.CommentTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshTable(ArcticExtendSparkSqlParser.RefreshTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshTable(ArcticExtendSparkSqlParser.RefreshTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshFunction(ArcticExtendSparkSqlParser.RefreshFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshFunction(ArcticExtendSparkSqlParser.RefreshFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshResource}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshResource(ArcticExtendSparkSqlParser.RefreshResourceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshResource}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshResource(ArcticExtendSparkSqlParser.RefreshResourceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cacheTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCacheTable(ArcticExtendSparkSqlParser.CacheTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cacheTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCacheTable(ArcticExtendSparkSqlParser.CacheTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uncacheTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUncacheTable(ArcticExtendSparkSqlParser.UncacheTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uncacheTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUncacheTable(ArcticExtendSparkSqlParser.UncacheTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code clearCache}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterClearCache(ArcticExtendSparkSqlParser.ClearCacheContext ctx);
	/**
	 * Exit a parse tree produced by the {@code clearCache}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitClearCache(ArcticExtendSparkSqlParser.ClearCacheContext ctx);
	/**
	 * Enter a parse tree produced by the {@code loadData}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterLoadData(ArcticExtendSparkSqlParser.LoadDataContext ctx);
	/**
	 * Exit a parse tree produced by the {@code loadData}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitLoadData(ArcticExtendSparkSqlParser.LoadDataContext ctx);
	/**
	 * Enter a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterTruncateTable(ArcticExtendSparkSqlParser.TruncateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitTruncateTable(ArcticExtendSparkSqlParser.TruncateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code repairTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRepairTable(ArcticExtendSparkSqlParser.RepairTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code repairTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRepairTable(ArcticExtendSparkSqlParser.RepairTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code manageResource}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterManageResource(ArcticExtendSparkSqlParser.ManageResourceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code manageResource}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitManageResource(ArcticExtendSparkSqlParser.ManageResourceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code failNativeCommand}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterFailNativeCommand(ArcticExtendSparkSqlParser.FailNativeCommandContext ctx);
	/**
	 * Exit a parse tree produced by the {@code failNativeCommand}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitFailNativeCommand(ArcticExtendSparkSqlParser.FailNativeCommandContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTimeZone}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetTimeZone(ArcticExtendSparkSqlParser.SetTimeZoneContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTimeZone}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetTimeZone(ArcticExtendSparkSqlParser.SetTimeZoneContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setQuotedConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetQuotedConfiguration(ArcticExtendSparkSqlParser.SetQuotedConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setQuotedConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetQuotedConfiguration(ArcticExtendSparkSqlParser.SetQuotedConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetConfiguration(ArcticExtendSparkSqlParser.SetConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetConfiguration(ArcticExtendSparkSqlParser.SetConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resetQuotedConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterResetQuotedConfiguration(ArcticExtendSparkSqlParser.ResetQuotedConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resetQuotedConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitResetQuotedConfiguration(ArcticExtendSparkSqlParser.ResetQuotedConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resetConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterResetConfiguration(ArcticExtendSparkSqlParser.ResetConfigurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resetConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitResetConfiguration(ArcticExtendSparkSqlParser.ResetConfigurationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#configKey}.
	 * @param ctx the parse tree
	 */
	void enterConfigKey(ArcticExtendSparkSqlParser.ConfigKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#configKey}.
	 * @param ctx the parse tree
	 */
	void exitConfigKey(ArcticExtendSparkSqlParser.ConfigKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#configValue}.
	 * @param ctx the parse tree
	 */
	void enterConfigValue(ArcticExtendSparkSqlParser.ConfigValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#configValue}.
	 * @param ctx the parse tree
	 */
	void exitConfigValue(ArcticExtendSparkSqlParser.ConfigValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 */
	void enterUnsupportedHiveNativeCommands(ArcticExtendSparkSqlParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 */
	void exitUnsupportedHiveNativeCommands(ArcticExtendSparkSqlParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#createTableHeader}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableHeader(ArcticExtendSparkSqlParser.CreateTableHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#createTableHeader}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableHeader(ArcticExtendSparkSqlParser.CreateTableHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#replaceTableHeader}.
	 * @param ctx the parse tree
	 */
	void enterReplaceTableHeader(ArcticExtendSparkSqlParser.ReplaceTableHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#replaceTableHeader}.
	 * @param ctx the parse tree
	 */
	void exitReplaceTableHeader(ArcticExtendSparkSqlParser.ReplaceTableHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#bucketSpec}.
	 * @param ctx the parse tree
	 */
	void enterBucketSpec(ArcticExtendSparkSqlParser.BucketSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#bucketSpec}.
	 * @param ctx the parse tree
	 */
	void exitBucketSpec(ArcticExtendSparkSqlParser.BucketSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#skewSpec}.
	 * @param ctx the parse tree
	 */
	void enterSkewSpec(ArcticExtendSparkSqlParser.SkewSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#skewSpec}.
	 * @param ctx the parse tree
	 */
	void exitSkewSpec(ArcticExtendSparkSqlParser.SkewSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#locationSpec}.
	 * @param ctx the parse tree
	 */
	void enterLocationSpec(ArcticExtendSparkSqlParser.LocationSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#locationSpec}.
	 * @param ctx the parse tree
	 */
	void exitLocationSpec(ArcticExtendSparkSqlParser.LocationSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#commentSpec}.
	 * @param ctx the parse tree
	 */
	void enterCommentSpec(ArcticExtendSparkSqlParser.CommentSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#commentSpec}.
	 * @param ctx the parse tree
	 */
	void exitCommentSpec(ArcticExtendSparkSqlParser.CommentSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(ArcticExtendSparkSqlParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(ArcticExtendSparkSqlParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteTable(ArcticExtendSparkSqlParser.InsertOverwriteTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteTable(ArcticExtendSparkSqlParser.InsertOverwriteTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertIntoTable(ArcticExtendSparkSqlParser.InsertIntoTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertIntoTable(ArcticExtendSparkSqlParser.InsertIntoTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteHiveDir(ArcticExtendSparkSqlParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteHiveDir(ArcticExtendSparkSqlParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void enterInsertOverwriteDir(ArcticExtendSparkSqlParser.InsertOverwriteDirContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 */
	void exitInsertOverwriteDir(ArcticExtendSparkSqlParser.InsertOverwriteDirContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 */
	void enterPartitionSpecLocation(ArcticExtendSparkSqlParser.PartitionSpecLocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 */
	void exitPartitionSpecLocation(ArcticExtendSparkSqlParser.PartitionSpecLocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionSpec}.
	 * @param ctx the parse tree
	 */
	void enterPartitionSpec(ArcticExtendSparkSqlParser.PartitionSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionSpec}.
	 * @param ctx the parse tree
	 */
	void exitPartitionSpec(ArcticExtendSparkSqlParser.PartitionSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionVal}.
	 * @param ctx the parse tree
	 */
	void enterPartitionVal(ArcticExtendSparkSqlParser.PartitionValContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionVal}.
	 * @param ctx the parse tree
	 */
	void exitPartitionVal(ArcticExtendSparkSqlParser.PartitionValContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#namespace}.
	 * @param ctx the parse tree
	 */
	void enterNamespace(ArcticExtendSparkSqlParser.NamespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#namespace}.
	 * @param ctx the parse tree
	 */
	void exitNamespace(ArcticExtendSparkSqlParser.NamespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#describeFuncName}.
	 * @param ctx the parse tree
	 */
	void enterDescribeFuncName(ArcticExtendSparkSqlParser.DescribeFuncNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#describeFuncName}.
	 * @param ctx the parse tree
	 */
	void exitDescribeFuncName(ArcticExtendSparkSqlParser.DescribeFuncNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#describeColName}.
	 * @param ctx the parse tree
	 */
	void enterDescribeColName(ArcticExtendSparkSqlParser.DescribeColNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#describeColName}.
	 * @param ctx the parse tree
	 */
	void exitDescribeColName(ArcticExtendSparkSqlParser.DescribeColNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#ctes}.
	 * @param ctx the parse tree
	 */
	void enterCtes(ArcticExtendSparkSqlParser.CtesContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#ctes}.
	 * @param ctx the parse tree
	 */
	void exitCtes(ArcticExtendSparkSqlParser.CtesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void enterNamedQuery(ArcticExtendSparkSqlParser.NamedQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void exitNamedQuery(ArcticExtendSparkSqlParser.NamedQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#tableProvider}.
	 * @param ctx the parse tree
	 */
	void enterTableProvider(ArcticExtendSparkSqlParser.TableProviderContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#tableProvider}.
	 * @param ctx the parse tree
	 */
	void exitTableProvider(ArcticExtendSparkSqlParser.TableProviderContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#createTableClauses}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableClauses(ArcticExtendSparkSqlParser.CreateTableClausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#createTableClauses}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableClauses(ArcticExtendSparkSqlParser.CreateTableClausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#tablePropertyList}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyList(ArcticExtendSparkSqlParser.TablePropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#tablePropertyList}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyList(ArcticExtendSparkSqlParser.TablePropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#tableProperty}.
	 * @param ctx the parse tree
	 */
	void enterTableProperty(ArcticExtendSparkSqlParser.TablePropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#tableProperty}.
	 * @param ctx the parse tree
	 */
	void exitTableProperty(ArcticExtendSparkSqlParser.TablePropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyKey(ArcticExtendSparkSqlParser.TablePropertyKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyKey(ArcticExtendSparkSqlParser.TablePropertyKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyValue(ArcticExtendSparkSqlParser.TablePropertyValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyValue(ArcticExtendSparkSqlParser.TablePropertyValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#constantList}.
	 * @param ctx the parse tree
	 */
	void enterConstantList(ArcticExtendSparkSqlParser.ConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#constantList}.
	 * @param ctx the parse tree
	 */
	void exitConstantList(ArcticExtendSparkSqlParser.ConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#nestedConstantList}.
	 * @param ctx the parse tree
	 */
	void enterNestedConstantList(ArcticExtendSparkSqlParser.NestedConstantListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#nestedConstantList}.
	 * @param ctx the parse tree
	 */
	void exitNestedConstantList(ArcticExtendSparkSqlParser.NestedConstantListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#createFileFormat}.
	 * @param ctx the parse tree
	 */
	void enterCreateFileFormat(ArcticExtendSparkSqlParser.CreateFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#createFileFormat}.
	 * @param ctx the parse tree
	 */
	void exitCreateFileFormat(ArcticExtendSparkSqlParser.CreateFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterTableFileFormat(ArcticExtendSparkSqlParser.TableFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitTableFileFormat(ArcticExtendSparkSqlParser.TableFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterGenericFileFormat(ArcticExtendSparkSqlParser.GenericFileFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitGenericFileFormat(ArcticExtendSparkSqlParser.GenericFileFormatContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#storageHandler}.
	 * @param ctx the parse tree
	 */
	void enterStorageHandler(ArcticExtendSparkSqlParser.StorageHandlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#storageHandler}.
	 * @param ctx the parse tree
	 */
	void exitStorageHandler(ArcticExtendSparkSqlParser.StorageHandlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(ArcticExtendSparkSqlParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(ArcticExtendSparkSqlParser.ResourceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterSingleInsertQuery(ArcticExtendSparkSqlParser.SingleInsertQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitSingleInsertQuery(ArcticExtendSparkSqlParser.SingleInsertQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterMultiInsertQuery(ArcticExtendSparkSqlParser.MultiInsertQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitMultiInsertQuery(ArcticExtendSparkSqlParser.MultiInsertQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code deleteFromTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterDeleteFromTable(ArcticExtendSparkSqlParser.DeleteFromTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code deleteFromTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitDeleteFromTable(ArcticExtendSparkSqlParser.DeleteFromTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code updateTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterUpdateTable(ArcticExtendSparkSqlParser.UpdateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code updateTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitUpdateTable(ArcticExtendSparkSqlParser.UpdateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mergeIntoTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void enterMergeIntoTable(ArcticExtendSparkSqlParser.MergeIntoTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mergeIntoTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 */
	void exitMergeIntoTable(ArcticExtendSparkSqlParser.MergeIntoTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#queryOrganization}.
	 * @param ctx the parse tree
	 */
	void enterQueryOrganization(ArcticExtendSparkSqlParser.QueryOrganizationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#queryOrganization}.
	 * @param ctx the parse tree
	 */
	void exitQueryOrganization(ArcticExtendSparkSqlParser.QueryOrganizationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 */
	void enterMultiInsertQueryBody(ArcticExtendSparkSqlParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 */
	void exitMultiInsertQueryBody(ArcticExtendSparkSqlParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterQueryTermDefault(ArcticExtendSparkSqlParser.QueryTermDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitQueryTermDefault(ArcticExtendSparkSqlParser.QueryTermDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterSetOperation(ArcticExtendSparkSqlParser.SetOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitSetOperation(ArcticExtendSparkSqlParser.SetOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterQueryPrimaryDefault(ArcticExtendSparkSqlParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitQueryPrimaryDefault(ArcticExtendSparkSqlParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterFromStmt(ArcticExtendSparkSqlParser.FromStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitFromStmt(ArcticExtendSparkSqlParser.FromStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTable(ArcticExtendSparkSqlParser.TableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTable(ArcticExtendSparkSqlParser.TableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTableDefault1(ArcticExtendSparkSqlParser.InlineTableDefault1Context ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTableDefault1(ArcticExtendSparkSqlParser.InlineTableDefault1Context ctx);
	/**
	 * Enter a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(ArcticExtendSparkSqlParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(ArcticExtendSparkSqlParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void enterSortItem(ArcticExtendSparkSqlParser.SortItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void exitSortItem(ArcticExtendSparkSqlParser.SortItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#fromStatement}.
	 * @param ctx the parse tree
	 */
	void enterFromStatement(ArcticExtendSparkSqlParser.FromStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#fromStatement}.
	 * @param ctx the parse tree
	 */
	void exitFromStatement(ArcticExtendSparkSqlParser.FromStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#fromStatementBody}.
	 * @param ctx the parse tree
	 */
	void enterFromStatementBody(ArcticExtendSparkSqlParser.FromStatementBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#fromStatementBody}.
	 * @param ctx the parse tree
	 */
	void exitFromStatementBody(ArcticExtendSparkSqlParser.FromStatementBodyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterTransformQuerySpecification(ArcticExtendSparkSqlParser.TransformQuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitTransformQuerySpecification(ArcticExtendSparkSqlParser.TransformQuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterRegularQuerySpecification(ArcticExtendSparkSqlParser.RegularQuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitRegularQuerySpecification(ArcticExtendSparkSqlParser.RegularQuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#transformClause}.
	 * @param ctx the parse tree
	 */
	void enterTransformClause(ArcticExtendSparkSqlParser.TransformClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#transformClause}.
	 * @param ctx the parse tree
	 */
	void exitTransformClause(ArcticExtendSparkSqlParser.TransformClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(ArcticExtendSparkSqlParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(ArcticExtendSparkSqlParser.SelectClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#setClause}.
	 * @param ctx the parse tree
	 */
	void enterSetClause(ArcticExtendSparkSqlParser.SetClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#setClause}.
	 * @param ctx the parse tree
	 */
	void exitSetClause(ArcticExtendSparkSqlParser.SetClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#matchedClause}.
	 * @param ctx the parse tree
	 */
	void enterMatchedClause(ArcticExtendSparkSqlParser.MatchedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#matchedClause}.
	 * @param ctx the parse tree
	 */
	void exitMatchedClause(ArcticExtendSparkSqlParser.MatchedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#notMatchedClause}.
	 * @param ctx the parse tree
	 */
	void enterNotMatchedClause(ArcticExtendSparkSqlParser.NotMatchedClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#notMatchedClause}.
	 * @param ctx the parse tree
	 */
	void exitNotMatchedClause(ArcticExtendSparkSqlParser.NotMatchedClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#matchedAction}.
	 * @param ctx the parse tree
	 */
	void enterMatchedAction(ArcticExtendSparkSqlParser.MatchedActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#matchedAction}.
	 * @param ctx the parse tree
	 */
	void exitMatchedAction(ArcticExtendSparkSqlParser.MatchedActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#notMatchedAction}.
	 * @param ctx the parse tree
	 */
	void enterNotMatchedAction(ArcticExtendSparkSqlParser.NotMatchedActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#notMatchedAction}.
	 * @param ctx the parse tree
	 */
	void exitNotMatchedAction(ArcticExtendSparkSqlParser.NotMatchedActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#assignmentList}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentList(ArcticExtendSparkSqlParser.AssignmentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#assignmentList}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentList(ArcticExtendSparkSqlParser.AssignmentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(ArcticExtendSparkSqlParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(ArcticExtendSparkSqlParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void enterWhereClause(ArcticExtendSparkSqlParser.WhereClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void exitWhereClause(ArcticExtendSparkSqlParser.WhereClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void enterHavingClause(ArcticExtendSparkSqlParser.HavingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void exitHavingClause(ArcticExtendSparkSqlParser.HavingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#hint}.
	 * @param ctx the parse tree
	 */
	void enterHint(ArcticExtendSparkSqlParser.HintContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#hint}.
	 * @param ctx the parse tree
	 */
	void exitHint(ArcticExtendSparkSqlParser.HintContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#hintStatement}.
	 * @param ctx the parse tree
	 */
	void enterHintStatement(ArcticExtendSparkSqlParser.HintStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#hintStatement}.
	 * @param ctx the parse tree
	 */
	void exitHintStatement(ArcticExtendSparkSqlParser.HintStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFromClause(ArcticExtendSparkSqlParser.FromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFromClause(ArcticExtendSparkSqlParser.FromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#aggregationClause}.
	 * @param ctx the parse tree
	 */
	void enterAggregationClause(ArcticExtendSparkSqlParser.AggregationClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#aggregationClause}.
	 * @param ctx the parse tree
	 */
	void exitAggregationClause(ArcticExtendSparkSqlParser.AggregationClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#groupByClause}.
	 * @param ctx the parse tree
	 */
	void enterGroupByClause(ArcticExtendSparkSqlParser.GroupByClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#groupByClause}.
	 * @param ctx the parse tree
	 */
	void exitGroupByClause(ArcticExtendSparkSqlParser.GroupByClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 */
	void enterGroupingAnalytics(ArcticExtendSparkSqlParser.GroupingAnalyticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 */
	void exitGroupingAnalytics(ArcticExtendSparkSqlParser.GroupingAnalyticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterGroupingElement(ArcticExtendSparkSqlParser.GroupingElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitGroupingElement(ArcticExtendSparkSqlParser.GroupingElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void enterGroupingSet(ArcticExtendSparkSqlParser.GroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void exitGroupingSet(ArcticExtendSparkSqlParser.GroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#pivotClause}.
	 * @param ctx the parse tree
	 */
	void enterPivotClause(ArcticExtendSparkSqlParser.PivotClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#pivotClause}.
	 * @param ctx the parse tree
	 */
	void exitPivotClause(ArcticExtendSparkSqlParser.PivotClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#pivotColumn}.
	 * @param ctx the parse tree
	 */
	void enterPivotColumn(ArcticExtendSparkSqlParser.PivotColumnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#pivotColumn}.
	 * @param ctx the parse tree
	 */
	void exitPivotColumn(ArcticExtendSparkSqlParser.PivotColumnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#pivotValue}.
	 * @param ctx the parse tree
	 */
	void enterPivotValue(ArcticExtendSparkSqlParser.PivotValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#pivotValue}.
	 * @param ctx the parse tree
	 */
	void exitPivotValue(ArcticExtendSparkSqlParser.PivotValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#lateralView}.
	 * @param ctx the parse tree
	 */
	void enterLateralView(ArcticExtendSparkSqlParser.LateralViewContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#lateralView}.
	 * @param ctx the parse tree
	 */
	void exitLateralView(ArcticExtendSparkSqlParser.LateralViewContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterSetQuantifier(ArcticExtendSparkSqlParser.SetQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitSetQuantifier(ArcticExtendSparkSqlParser.SetQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterRelation(ArcticExtendSparkSqlParser.RelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitRelation(ArcticExtendSparkSqlParser.RelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void enterJoinRelation(ArcticExtendSparkSqlParser.JoinRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void exitJoinRelation(ArcticExtendSparkSqlParser.JoinRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#joinType}.
	 * @param ctx the parse tree
	 */
	void enterJoinType(ArcticExtendSparkSqlParser.JoinTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#joinType}.
	 * @param ctx the parse tree
	 */
	void exitJoinType(ArcticExtendSparkSqlParser.JoinTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void enterJoinCriteria(ArcticExtendSparkSqlParser.JoinCriteriaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void exitJoinCriteria(ArcticExtendSparkSqlParser.JoinCriteriaContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#sample}.
	 * @param ctx the parse tree
	 */
	void enterSample(ArcticExtendSparkSqlParser.SampleContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#sample}.
	 * @param ctx the parse tree
	 */
	void exitSample(ArcticExtendSparkSqlParser.SampleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByPercentile(ArcticExtendSparkSqlParser.SampleByPercentileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByPercentile(ArcticExtendSparkSqlParser.SampleByPercentileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByRows(ArcticExtendSparkSqlParser.SampleByRowsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByRows(ArcticExtendSparkSqlParser.SampleByRowsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByBucket(ArcticExtendSparkSqlParser.SampleByBucketContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByBucket(ArcticExtendSparkSqlParser.SampleByBucketContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void enterSampleByBytes(ArcticExtendSparkSqlParser.SampleByBytesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 */
	void exitSampleByBytes(ArcticExtendSparkSqlParser.SampleByBytesContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierList(ArcticExtendSparkSqlParser.IdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierList(ArcticExtendSparkSqlParser.IdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierSeq(ArcticExtendSparkSqlParser.IdentifierSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierSeq(ArcticExtendSparkSqlParser.IdentifierSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 */
	void enterOrderedIdentifierList(ArcticExtendSparkSqlParser.OrderedIdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 */
	void exitOrderedIdentifierList(ArcticExtendSparkSqlParser.OrderedIdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterOrderedIdentifier(ArcticExtendSparkSqlParser.OrderedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitOrderedIdentifier(ArcticExtendSparkSqlParser.OrderedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierCommentList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierCommentList(ArcticExtendSparkSqlParser.IdentifierCommentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierCommentList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierCommentList(ArcticExtendSparkSqlParser.IdentifierCommentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierComment}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierComment(ArcticExtendSparkSqlParser.IdentifierCommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierComment}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierComment(ArcticExtendSparkSqlParser.IdentifierCommentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableName(ArcticExtendSparkSqlParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableName(ArcticExtendSparkSqlParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterAliasedQuery(ArcticExtendSparkSqlParser.AliasedQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitAliasedQuery(ArcticExtendSparkSqlParser.AliasedQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterAliasedRelation(ArcticExtendSparkSqlParser.AliasedRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitAliasedRelation(ArcticExtendSparkSqlParser.AliasedRelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTableDefault2(ArcticExtendSparkSqlParser.InlineTableDefault2Context ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTableDefault2(ArcticExtendSparkSqlParser.InlineTableDefault2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableValuedFunction(ArcticExtendSparkSqlParser.TableValuedFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableValuedFunction(ArcticExtendSparkSqlParser.TableValuedFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void enterInlineTable(ArcticExtendSparkSqlParser.InlineTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void exitInlineTable(ArcticExtendSparkSqlParser.InlineTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#functionTable}.
	 * @param ctx the parse tree
	 */
	void enterFunctionTable(ArcticExtendSparkSqlParser.FunctionTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#functionTable}.
	 * @param ctx the parse tree
	 */
	void exitFunctionTable(ArcticExtendSparkSqlParser.FunctionTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#tableAlias}.
	 * @param ctx the parse tree
	 */
	void enterTableAlias(ArcticExtendSparkSqlParser.TableAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#tableAlias}.
	 * @param ctx the parse tree
	 */
	void exitTableAlias(ArcticExtendSparkSqlParser.TableAliasContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void enterRowFormatSerde(ArcticExtendSparkSqlParser.RowFormatSerdeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void exitRowFormatSerde(ArcticExtendSparkSqlParser.RowFormatSerdeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void enterRowFormatDelimited(ArcticExtendSparkSqlParser.RowFormatDelimitedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 */
	void exitRowFormatDelimited(ArcticExtendSparkSqlParser.RowFormatDelimitedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#multipartIdentifierList}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifierList(ArcticExtendSparkSqlParser.MultipartIdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#multipartIdentifierList}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifierList(ArcticExtendSparkSqlParser.MultipartIdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifier(ArcticExtendSparkSqlParser.MultipartIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifier(ArcticExtendSparkSqlParser.MultipartIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterTableIdentifier(ArcticExtendSparkSqlParser.TableIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitTableIdentifier(ArcticExtendSparkSqlParser.TableIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#functionIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterFunctionIdentifier(ArcticExtendSparkSqlParser.FunctionIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#functionIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitFunctionIdentifier(ArcticExtendSparkSqlParser.FunctionIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#namedExpression}.
	 * @param ctx the parse tree
	 */
	void enterNamedExpression(ArcticExtendSparkSqlParser.NamedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#namedExpression}.
	 * @param ctx the parse tree
	 */
	void exitNamedExpression(ArcticExtendSparkSqlParser.NamedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 */
	void enterNamedExpressionSeq(ArcticExtendSparkSqlParser.NamedExpressionSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 */
	void exitNamedExpressionSeq(ArcticExtendSparkSqlParser.NamedExpressionSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionFieldList}.
	 * @param ctx the parse tree
	 */
	void enterPartitionFieldList(ArcticExtendSparkSqlParser.PartitionFieldListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionFieldList}.
	 * @param ctx the parse tree
	 */
	void exitPartitionFieldList(ArcticExtendSparkSqlParser.PartitionFieldListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void enterPartitionTransform(ArcticExtendSparkSqlParser.PartitionTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void exitPartitionTransform(ArcticExtendSparkSqlParser.PartitionTransformContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void enterPartitionColumn(ArcticExtendSparkSqlParser.PartitionColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 */
	void exitPartitionColumn(ArcticExtendSparkSqlParser.PartitionColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#transform}.
	 * @param ctx the parse tree
	 */
	void enterIdentityTransform(ArcticExtendSparkSqlParser.IdentityTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#transform}.
	 * @param ctx the parse tree
	 */
	void exitIdentityTransform(ArcticExtendSparkSqlParser.IdentityTransformContext ctx);
	/**
	 * Enter a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#transform}.
	 * @param ctx the parse tree
	 */
	void enterApplyTransform(ArcticExtendSparkSqlParser.ApplyTransformContext ctx);
	/**
	 * Exit a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#transform}.
	 * @param ctx the parse tree
	 */
	void exitApplyTransform(ArcticExtendSparkSqlParser.ApplyTransformContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#transformArgument}.
	 * @param ctx the parse tree
	 */
	void enterTransformArgument(ArcticExtendSparkSqlParser.TransformArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#transformArgument}.
	 * @param ctx the parse tree
	 */
	void exitTransformArgument(ArcticExtendSparkSqlParser.TransformArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ArcticExtendSparkSqlParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ArcticExtendSparkSqlParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#expressionSeq}.
	 * @param ctx the parse tree
	 */
	void enterExpressionSeq(ArcticExtendSparkSqlParser.ExpressionSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#expressionSeq}.
	 * @param ctx the parse tree
	 */
	void exitExpressionSeq(ArcticExtendSparkSqlParser.ExpressionSeqContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalNot(ArcticExtendSparkSqlParser.LogicalNotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalNot(ArcticExtendSparkSqlParser.LogicalNotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterPredicated(ArcticExtendSparkSqlParser.PredicatedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitPredicated(ArcticExtendSparkSqlParser.PredicatedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterExists(ArcticExtendSparkSqlParser.ExistsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitExists(ArcticExtendSparkSqlParser.ExistsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalBinary(ArcticExtendSparkSqlParser.LogicalBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalBinary(ArcticExtendSparkSqlParser.LogicalBinaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(ArcticExtendSparkSqlParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(ArcticExtendSparkSqlParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterValueExpressionDefault(ArcticExtendSparkSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitValueExpressionDefault(ArcticExtendSparkSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterComparison(ArcticExtendSparkSqlParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitComparison(ArcticExtendSparkSqlParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticBinary(ArcticExtendSparkSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticBinary(ArcticExtendSparkSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticUnary(ArcticExtendSparkSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticUnary(ArcticExtendSparkSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStruct(ArcticExtendSparkSqlParser.StructContext ctx);
	/**
	 * Exit a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStruct(ArcticExtendSparkSqlParser.StructContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterDereference(ArcticExtendSparkSqlParser.DereferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitDereference(ArcticExtendSparkSqlParser.DereferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCase(ArcticExtendSparkSqlParser.SimpleCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCase(ArcticExtendSparkSqlParser.SimpleCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCurrentLike(ArcticExtendSparkSqlParser.CurrentLikeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCurrentLike(ArcticExtendSparkSqlParser.CurrentLikeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterColumnReference(ArcticExtendSparkSqlParser.ColumnReferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitColumnReference(ArcticExtendSparkSqlParser.ColumnReferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterRowConstructor(ArcticExtendSparkSqlParser.RowConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitRowConstructor(ArcticExtendSparkSqlParser.RowConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLast(ArcticExtendSparkSqlParser.LastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLast(ArcticExtendSparkSqlParser.LastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStar(ArcticExtendSparkSqlParser.StarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStar(ArcticExtendSparkSqlParser.StarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterOverlay(ArcticExtendSparkSqlParser.OverlayContext ctx);
	/**
	 * Exit a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitOverlay(ArcticExtendSparkSqlParser.OverlayContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(ArcticExtendSparkSqlParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(ArcticExtendSparkSqlParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryExpression(ArcticExtendSparkSqlParser.SubqueryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryExpression(ArcticExtendSparkSqlParser.SubqueryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code substring}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubstring(ArcticExtendSparkSqlParser.SubstringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code substring}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubstring(ArcticExtendSparkSqlParser.SubstringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCast(ArcticExtendSparkSqlParser.CastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCast(ArcticExtendSparkSqlParser.CastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantDefault(ArcticExtendSparkSqlParser.ConstantDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantDefault(ArcticExtendSparkSqlParser.ConstantDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambda(ArcticExtendSparkSqlParser.LambdaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambda(ArcticExtendSparkSqlParser.LambdaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(ArcticExtendSparkSqlParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(ArcticExtendSparkSqlParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code extract}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterExtract(ArcticExtendSparkSqlParser.ExtractContext ctx);
	/**
	 * Exit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitExtract(ArcticExtendSparkSqlParser.ExtractContext ctx);
	/**
	 * Enter a parse tree produced by the {@code trim}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTrim(ArcticExtendSparkSqlParser.TrimContext ctx);
	/**
	 * Exit a parse tree produced by the {@code trim}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTrim(ArcticExtendSparkSqlParser.TrimContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(ArcticExtendSparkSqlParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(ArcticExtendSparkSqlParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSearchedCase(ArcticExtendSparkSqlParser.SearchedCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSearchedCase(ArcticExtendSparkSqlParser.SearchedCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPosition(ArcticExtendSparkSqlParser.PositionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPosition(ArcticExtendSparkSqlParser.PositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFirst(ArcticExtendSparkSqlParser.FirstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFirst(ArcticExtendSparkSqlParser.FirstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNullLiteral(ArcticExtendSparkSqlParser.NullLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNullLiteral(ArcticExtendSparkSqlParser.NullLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterIntervalLiteral(ArcticExtendSparkSqlParser.IntervalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitIntervalLiteral(ArcticExtendSparkSqlParser.IntervalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterTypeConstructor(ArcticExtendSparkSqlParser.TypeConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitTypeConstructor(ArcticExtendSparkSqlParser.TypeConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(ArcticExtendSparkSqlParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(ArcticExtendSparkSqlParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(ArcticExtendSparkSqlParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(ArcticExtendSparkSqlParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(ArcticExtendSparkSqlParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(ArcticExtendSparkSqlParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(ArcticExtendSparkSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(ArcticExtendSparkSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticOperator(ArcticExtendSparkSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticOperator(ArcticExtendSparkSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#predicateOperator}.
	 * @param ctx the parse tree
	 */
	void enterPredicateOperator(ArcticExtendSparkSqlParser.PredicateOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#predicateOperator}.
	 * @param ctx the parse tree
	 */
	void exitPredicateOperator(ArcticExtendSparkSqlParser.PredicateOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(ArcticExtendSparkSqlParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(ArcticExtendSparkSqlParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#interval}.
	 * @param ctx the parse tree
	 */
	void enterInterval(ArcticExtendSparkSqlParser.IntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#interval}.
	 * @param ctx the parse tree
	 */
	void exitInterval(ArcticExtendSparkSqlParser.IntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingMultiUnitsInterval(ArcticExtendSparkSqlParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingMultiUnitsInterval(ArcticExtendSparkSqlParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void enterMultiUnitsInterval(ArcticExtendSparkSqlParser.MultiUnitsIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 */
	void exitMultiUnitsInterval(ArcticExtendSparkSqlParser.MultiUnitsIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingUnitToUnitInterval(ArcticExtendSparkSqlParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingUnitToUnitInterval(ArcticExtendSparkSqlParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void enterUnitToUnitInterval(ArcticExtendSparkSqlParser.UnitToUnitIntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 */
	void exitUnitToUnitInterval(ArcticExtendSparkSqlParser.UnitToUnitIntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#intervalValue}.
	 * @param ctx the parse tree
	 */
	void enterIntervalValue(ArcticExtendSparkSqlParser.IntervalValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#intervalValue}.
	 * @param ctx the parse tree
	 */
	void exitIntervalValue(ArcticExtendSparkSqlParser.IntervalValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#colPosition}.
	 * @param ctx the parse tree
	 */
	void enterColPosition(ArcticExtendSparkSqlParser.ColPositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#colPosition}.
	 * @param ctx the parse tree
	 */
	void exitColPosition(ArcticExtendSparkSqlParser.ColPositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterComplexDataType(ArcticExtendSparkSqlParser.ComplexDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitComplexDataType(ArcticExtendSparkSqlParser.ComplexDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterYearMonthIntervalDataType(ArcticExtendSparkSqlParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitYearMonthIntervalDataType(ArcticExtendSparkSqlParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDayTimeIntervalDataType(ArcticExtendSparkSqlParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDayTimeIntervalDataType(ArcticExtendSparkSqlParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveDataType(ArcticExtendSparkSqlParser.PrimitiveDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveDataType(ArcticExtendSparkSqlParser.PrimitiveDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedColTypeWithPositionList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedColTypeWithPositionList(ArcticExtendSparkSqlParser.QualifiedColTypeWithPositionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedColTypeWithPositionList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedColTypeWithPositionList(ArcticExtendSparkSqlParser.QualifiedColTypeWithPositionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedColTypeWithPosition}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedColTypeWithPosition(ArcticExtendSparkSqlParser.QualifiedColTypeWithPositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedColTypeWithPosition}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedColTypeWithPosition(ArcticExtendSparkSqlParser.QualifiedColTypeWithPositionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void enterColTypeList(ArcticExtendSparkSqlParser.ColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void exitColTypeList(ArcticExtendSparkSqlParser.ColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#colType}.
	 * @param ctx the parse tree
	 */
	void enterColType(ArcticExtendSparkSqlParser.ColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#colType}.
	 * @param ctx the parse tree
	 */
	void exitColType(ArcticExtendSparkSqlParser.ColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#complexColTypeList}.
	 * @param ctx the parse tree
	 */
	void enterComplexColTypeList(ArcticExtendSparkSqlParser.ComplexColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#complexColTypeList}.
	 * @param ctx the parse tree
	 */
	void exitComplexColTypeList(ArcticExtendSparkSqlParser.ComplexColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#complexColType}.
	 * @param ctx the parse tree
	 */
	void enterComplexColType(ArcticExtendSparkSqlParser.ComplexColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#complexColType}.
	 * @param ctx the parse tree
	 */
	void exitComplexColType(ArcticExtendSparkSqlParser.ComplexColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(ArcticExtendSparkSqlParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(ArcticExtendSparkSqlParser.WhenClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#windowClause}.
	 * @param ctx the parse tree
	 */
	void enterWindowClause(ArcticExtendSparkSqlParser.WindowClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#windowClause}.
	 * @param ctx the parse tree
	 */
	void exitWindowClause(ArcticExtendSparkSqlParser.WindowClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#namedWindow}.
	 * @param ctx the parse tree
	 */
	void enterNamedWindow(ArcticExtendSparkSqlParser.NamedWindowContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#namedWindow}.
	 * @param ctx the parse tree
	 */
	void exitNamedWindow(ArcticExtendSparkSqlParser.NamedWindowContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void enterWindowRef(ArcticExtendSparkSqlParser.WindowRefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void exitWindowRef(ArcticExtendSparkSqlParser.WindowRefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void enterWindowDef(ArcticExtendSparkSqlParser.WindowDefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 */
	void exitWindowDef(ArcticExtendSparkSqlParser.WindowDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void enterWindowFrame(ArcticExtendSparkSqlParser.WindowFrameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void exitWindowFrame(ArcticExtendSparkSqlParser.WindowFrameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterFrameBound(ArcticExtendSparkSqlParser.FrameBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitFrameBound(ArcticExtendSparkSqlParser.FrameBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedNameList(ArcticExtendSparkSqlParser.QualifiedNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedNameList(ArcticExtendSparkSqlParser.QualifiedNameListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#functionName}.
	 * @param ctx the parse tree
	 */
	void enterFunctionName(ArcticExtendSparkSqlParser.FunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#functionName}.
	 * @param ctx the parse tree
	 */
	void exitFunctionName(ArcticExtendSparkSqlParser.FunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(ArcticExtendSparkSqlParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(ArcticExtendSparkSqlParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingIdentifier(ArcticExtendSparkSqlParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingIdentifier(ArcticExtendSparkSqlParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void enterErrorIdent(ArcticExtendSparkSqlParser.ErrorIdentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void exitErrorIdent(ArcticExtendSparkSqlParser.ErrorIdentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void enterRealIdent(ArcticExtendSparkSqlParser.RealIdentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void exitRealIdent(ArcticExtendSparkSqlParser.RealIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(ArcticExtendSparkSqlParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(ArcticExtendSparkSqlParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedIdentifier(ArcticExtendSparkSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedIdentifier(ArcticExtendSparkSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifierAlternative(ArcticExtendSparkSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifierAlternative(ArcticExtendSparkSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifier(ArcticExtendSparkSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifier(ArcticExtendSparkSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterExponentLiteral(ArcticExtendSparkSqlParser.ExponentLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitExponentLiteral(ArcticExtendSparkSqlParser.ExponentLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDecimalLiteral(ArcticExtendSparkSqlParser.DecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDecimalLiteral(ArcticExtendSparkSqlParser.DecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterLegacyDecimalLiteral(ArcticExtendSparkSqlParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitLegacyDecimalLiteral(ArcticExtendSparkSqlParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(ArcticExtendSparkSqlParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(ArcticExtendSparkSqlParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigIntLiteral(ArcticExtendSparkSqlParser.BigIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigIntLiteral(ArcticExtendSparkSqlParser.BigIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterSmallIntLiteral(ArcticExtendSparkSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitSmallIntLiteral(ArcticExtendSparkSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterTinyIntLiteral(ArcticExtendSparkSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitTinyIntLiteral(ArcticExtendSparkSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(ArcticExtendSparkSqlParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(ArcticExtendSparkSqlParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterFloatLiteral(ArcticExtendSparkSqlParser.FloatLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitFloatLiteral(ArcticExtendSparkSqlParser.FloatLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigDecimalLiteral(ArcticExtendSparkSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigDecimalLiteral(ArcticExtendSparkSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#alterColumnAction}.
	 * @param ctx the parse tree
	 */
	void enterAlterColumnAction(ArcticExtendSparkSqlParser.AlterColumnActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#alterColumnAction}.
	 * @param ctx the parse tree
	 */
	void exitAlterColumnAction(ArcticExtendSparkSqlParser.AlterColumnActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 */
	void enterAnsiNonReserved(ArcticExtendSparkSqlParser.AnsiNonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 */
	void exitAnsiNonReserved(ArcticExtendSparkSqlParser.AnsiNonReservedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#strictNonReserved}.
	 * @param ctx the parse tree
	 */
	void enterStrictNonReserved(ArcticExtendSparkSqlParser.StrictNonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#strictNonReserved}.
	 * @param ctx the parse tree
	 */
	void exitStrictNonReserved(ArcticExtendSparkSqlParser.StrictNonReservedContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticExtendSparkSqlParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void enterNonReserved(ArcticExtendSparkSqlParser.NonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticExtendSparkSqlParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void exitNonReserved(ArcticExtendSparkSqlParser.NonReservedContext ctx);
}