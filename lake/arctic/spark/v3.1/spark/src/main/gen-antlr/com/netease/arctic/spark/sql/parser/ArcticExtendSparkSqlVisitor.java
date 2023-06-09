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
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ArcticExtendSparkSqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ArcticExtendSparkSqlVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#arcticCommand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArcticCommand(ArcticExtendSparkSqlParser.ArcticCommandContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTableWithPk}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#arcticStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableWithPk(ArcticExtendSparkSqlParser.CreateTableWithPkContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#createTableWithPrimaryKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableWithPrimaryKey(ArcticExtendSparkSqlParser.CreateTableWithPrimaryKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#primarySpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimarySpec(ArcticExtendSparkSqlParser.PrimarySpecContext ctx);
	/**
	 * Visit a parse tree produced by the {@code colListWithPk}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColListWithPk(ArcticExtendSparkSqlParser.ColListWithPkContext ctx);
	/**
	 * Visit a parse tree produced by the {@code colListOnlyPk}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#colListAndPk}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColListOnlyPk(ArcticExtendSparkSqlParser.ColListOnlyPkContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleStatement(ArcticExtendSparkSqlParser.SingleStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleExpression(ArcticExtendSparkSqlParser.SingleExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleTableIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleTableIdentifier(ArcticExtendSparkSqlParser.SingleTableIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleMultipartIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleMultipartIdentifier(ArcticExtendSparkSqlParser.SingleMultipartIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleFunctionIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleFunctionIdentifier(ArcticExtendSparkSqlParser.SingleFunctionIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleDataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleDataType(ArcticExtendSparkSqlParser.SingleDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#singleTableSchema}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleTableSchema(ArcticExtendSparkSqlParser.SingleTableSchemaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementDefault(ArcticExtendSparkSqlParser.StatementDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dmlStatement}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDmlStatement(ArcticExtendSparkSqlParser.DmlStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code use}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUse(ArcticExtendSparkSqlParser.UseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateNamespace(ArcticExtendSparkSqlParser.CreateNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setNamespaceProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetNamespaceProperties(ArcticExtendSparkSqlParser.SetNamespacePropertiesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setNamespaceLocation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetNamespaceLocation(ArcticExtendSparkSqlParser.SetNamespaceLocationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropNamespace(ArcticExtendSparkSqlParser.DropNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showNamespaces}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowNamespaces(ArcticExtendSparkSqlParser.ShowNamespacesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTable(ArcticExtendSparkSqlParser.CreateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTableLike}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableLike(ArcticExtendSparkSqlParser.CreateTableLikeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code replaceTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceTable(ArcticExtendSparkSqlParser.ReplaceTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyze(ArcticExtendSparkSqlParser.AnalyzeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code analyzeTables}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyzeTables(ArcticExtendSparkSqlParser.AnalyzeTablesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addTableColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddTableColumns(ArcticExtendSparkSqlParser.AddTableColumnsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code renameTableColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameTableColumn(ArcticExtendSparkSqlParser.RenameTableColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropTableColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropTableColumns(ArcticExtendSparkSqlParser.DropTableColumnsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameTable(ArcticExtendSparkSqlParser.RenameTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setTableProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetTableProperties(ArcticExtendSparkSqlParser.SetTablePropertiesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unsetTableProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsetTableProperties(ArcticExtendSparkSqlParser.UnsetTablePropertiesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterTableAlterColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterTableAlterColumn(ArcticExtendSparkSqlParser.AlterTableAlterColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code hiveChangeColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHiveChangeColumn(ArcticExtendSparkSqlParser.HiveChangeColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code hiveReplaceColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHiveReplaceColumns(ArcticExtendSparkSqlParser.HiveReplaceColumnsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setTableSerDe}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetTableSerDe(ArcticExtendSparkSqlParser.SetTableSerDeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addTablePartition}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddTablePartition(ArcticExtendSparkSqlParser.AddTablePartitionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code renameTablePartition}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameTablePartition(ArcticExtendSparkSqlParser.RenameTablePartitionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropTablePartitions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropTablePartitions(ArcticExtendSparkSqlParser.DropTablePartitionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setTableLocation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetTableLocation(ArcticExtendSparkSqlParser.SetTableLocationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code recoverPartitions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecoverPartitions(ArcticExtendSparkSqlParser.RecoverPartitionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropTable(ArcticExtendSparkSqlParser.DropTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropView(ArcticExtendSparkSqlParser.DropViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createView}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateView(ArcticExtendSparkSqlParser.CreateViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTempViewUsing}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTempViewUsing(ArcticExtendSparkSqlParser.CreateTempViewUsingContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterViewQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterViewQuery(ArcticExtendSparkSqlParser.AlterViewQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateFunction(ArcticExtendSparkSqlParser.CreateFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropFunction(ArcticExtendSparkSqlParser.DropFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code explain}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain(ArcticExtendSparkSqlParser.ExplainContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowTables(ArcticExtendSparkSqlParser.ShowTablesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showTableExtended}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowTableExtended(ArcticExtendSparkSqlParser.ShowTableExtendedContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showTblProperties}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowTblProperties(ArcticExtendSparkSqlParser.ShowTblPropertiesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowColumns(ArcticExtendSparkSqlParser.ShowColumnsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showViews}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowViews(ArcticExtendSparkSqlParser.ShowViewsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showPartitions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowPartitions(ArcticExtendSparkSqlParser.ShowPartitionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowFunctions(ArcticExtendSparkSqlParser.ShowFunctionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowCreateTable(ArcticExtendSparkSqlParser.ShowCreateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showCurrentNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowCurrentNamespace(ArcticExtendSparkSqlParser.ShowCurrentNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code describeFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeFunction(ArcticExtendSparkSqlParser.DescribeFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code describeNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeNamespace(ArcticExtendSparkSqlParser.DescribeNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code describeRelation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeRelation(ArcticExtendSparkSqlParser.DescribeRelationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code describeQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeQuery(ArcticExtendSparkSqlParser.DescribeQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code commentNamespace}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentNamespace(ArcticExtendSparkSqlParser.CommentNamespaceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code commentTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentTable(ArcticExtendSparkSqlParser.CommentTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code refreshTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefreshTable(ArcticExtendSparkSqlParser.RefreshTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code refreshFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefreshFunction(ArcticExtendSparkSqlParser.RefreshFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code refreshResource}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefreshResource(ArcticExtendSparkSqlParser.RefreshResourceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cacheTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCacheTable(ArcticExtendSparkSqlParser.CacheTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code uncacheTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUncacheTable(ArcticExtendSparkSqlParser.UncacheTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code clearCache}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClearCache(ArcticExtendSparkSqlParser.ClearCacheContext ctx);
	/**
	 * Visit a parse tree produced by the {@code loadData}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoadData(ArcticExtendSparkSqlParser.LoadDataContext ctx);
	/**
	 * Visit a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTruncateTable(ArcticExtendSparkSqlParser.TruncateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code repairTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRepairTable(ArcticExtendSparkSqlParser.RepairTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code manageResource}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitManageResource(ArcticExtendSparkSqlParser.ManageResourceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code failNativeCommand}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFailNativeCommand(ArcticExtendSparkSqlParser.FailNativeCommandContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setTimeZone}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetTimeZone(ArcticExtendSparkSqlParser.SetTimeZoneContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setQuotedConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetQuotedConfiguration(ArcticExtendSparkSqlParser.SetQuotedConfigurationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetConfiguration(ArcticExtendSparkSqlParser.SetConfigurationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code resetQuotedConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetQuotedConfiguration(ArcticExtendSparkSqlParser.ResetQuotedConfigurationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code resetConfiguration}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetConfiguration(ArcticExtendSparkSqlParser.ResetConfigurationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#configKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfigKey(ArcticExtendSparkSqlParser.ConfigKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#configValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfigValue(ArcticExtendSparkSqlParser.ConfigValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#unsupportedHiveNativeCommands}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsupportedHiveNativeCommands(ArcticExtendSparkSqlParser.UnsupportedHiveNativeCommandsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#createTableHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableHeader(ArcticExtendSparkSqlParser.CreateTableHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#replaceTableHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceTableHeader(ArcticExtendSparkSqlParser.ReplaceTableHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#bucketSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBucketSpec(ArcticExtendSparkSqlParser.BucketSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#skewSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkewSpec(ArcticExtendSparkSqlParser.SkewSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#locationSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocationSpec(ArcticExtendSparkSqlParser.LocationSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#commentSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentSpec(ArcticExtendSparkSqlParser.CommentSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(ArcticExtendSparkSqlParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertOverwriteTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertOverwriteTable(ArcticExtendSparkSqlParser.InsertOverwriteTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertIntoTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertIntoTable(ArcticExtendSparkSqlParser.InsertIntoTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertOverwriteHiveDir}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertOverwriteHiveDir(ArcticExtendSparkSqlParser.InsertOverwriteHiveDirContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertOverwriteDir}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#insertInto}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertOverwriteDir(ArcticExtendSparkSqlParser.InsertOverwriteDirContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionSpecLocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionSpecLocation(ArcticExtendSparkSqlParser.PartitionSpecLocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionSpec(ArcticExtendSparkSqlParser.PartitionSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionVal(ArcticExtendSparkSqlParser.PartitionValContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#namespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace(ArcticExtendSparkSqlParser.NamespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#describeFuncName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeFuncName(ArcticExtendSparkSqlParser.DescribeFuncNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#describeColName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeColName(ArcticExtendSparkSqlParser.DescribeColNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#ctes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCtes(ArcticExtendSparkSqlParser.CtesContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#namedQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedQuery(ArcticExtendSparkSqlParser.NamedQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#tableProvider}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableProvider(ArcticExtendSparkSqlParser.TableProviderContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#createTableClauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableClauses(ArcticExtendSparkSqlParser.CreateTableClausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#tablePropertyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyList(ArcticExtendSparkSqlParser.TablePropertyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#tableProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableProperty(ArcticExtendSparkSqlParser.TablePropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyKey(ArcticExtendSparkSqlParser.TablePropertyKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyValue(ArcticExtendSparkSqlParser.TablePropertyValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#constantList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantList(ArcticExtendSparkSqlParser.ConstantListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#nestedConstantList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNestedConstantList(ArcticExtendSparkSqlParser.NestedConstantListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#createFileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateFileFormat(ArcticExtendSparkSqlParser.CreateFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tableFileFormat}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableFileFormat(ArcticExtendSparkSqlParser.TableFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code genericFileFormat}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#fileFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericFileFormat(ArcticExtendSparkSqlParser.GenericFileFormatContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#storageHandler}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorageHandler(ArcticExtendSparkSqlParser.StorageHandlerContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#resource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource(ArcticExtendSparkSqlParser.ResourceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code singleInsertQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleInsertQuery(ArcticExtendSparkSqlParser.SingleInsertQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code multiInsertQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiInsertQuery(ArcticExtendSparkSqlParser.MultiInsertQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code deleteFromTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeleteFromTable(ArcticExtendSparkSqlParser.DeleteFromTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code updateTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdateTable(ArcticExtendSparkSqlParser.UpdateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mergeIntoTable}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dmlStatementNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergeIntoTable(ArcticExtendSparkSqlParser.MergeIntoTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#queryOrganization}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryOrganization(ArcticExtendSparkSqlParser.QueryOrganizationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#multiInsertQueryBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiInsertQueryBody(ArcticExtendSparkSqlParser.MultiInsertQueryBodyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryTermDefault(ArcticExtendSparkSqlParser.QueryTermDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetOperation(ArcticExtendSparkSqlParser.SetOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryPrimaryDefault(ArcticExtendSparkSqlParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code fromStmt}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromStmt(ArcticExtendSparkSqlParser.FromStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable(ArcticExtendSparkSqlParser.TableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inlineTableDefault1}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTableDefault1(ArcticExtendSparkSqlParser.InlineTableDefault1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery(ArcticExtendSparkSqlParser.SubqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#sortItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortItem(ArcticExtendSparkSqlParser.SortItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#fromStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromStatement(ArcticExtendSparkSqlParser.FromStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#fromStatementBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromStatementBody(ArcticExtendSparkSqlParser.FromStatementBodyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code transformQuerySpecification}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransformQuerySpecification(ArcticExtendSparkSqlParser.TransformQuerySpecificationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code regularQuerySpecification}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#querySpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegularQuerySpecification(ArcticExtendSparkSqlParser.RegularQuerySpecificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#transformClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransformClause(ArcticExtendSparkSqlParser.TransformClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#selectClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectClause(ArcticExtendSparkSqlParser.SelectClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#setClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetClause(ArcticExtendSparkSqlParser.SetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#matchedClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatchedClause(ArcticExtendSparkSqlParser.MatchedClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#notMatchedClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotMatchedClause(ArcticExtendSparkSqlParser.NotMatchedClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#matchedAction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatchedAction(ArcticExtendSparkSqlParser.MatchedActionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#notMatchedAction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotMatchedAction(ArcticExtendSparkSqlParser.NotMatchedActionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#assignmentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentList(ArcticExtendSparkSqlParser.AssignmentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(ArcticExtendSparkSqlParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#whereClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhereClause(ArcticExtendSparkSqlParser.WhereClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#havingClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHavingClause(ArcticExtendSparkSqlParser.HavingClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#hint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHint(ArcticExtendSparkSqlParser.HintContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#hintStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHintStatement(ArcticExtendSparkSqlParser.HintStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#fromClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromClause(ArcticExtendSparkSqlParser.FromClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#aggregationClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregationClause(ArcticExtendSparkSqlParser.AggregationClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#groupByClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupByClause(ArcticExtendSparkSqlParser.GroupByClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#groupingAnalytics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingAnalytics(ArcticExtendSparkSqlParser.GroupingAnalyticsContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#groupingElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingElement(ArcticExtendSparkSqlParser.GroupingElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#groupingSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingSet(ArcticExtendSparkSqlParser.GroupingSetContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#pivotClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivotClause(ArcticExtendSparkSqlParser.PivotClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#pivotColumn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivotColumn(ArcticExtendSparkSqlParser.PivotColumnContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#pivotValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivotValue(ArcticExtendSparkSqlParser.PivotValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#lateralView}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLateralView(ArcticExtendSparkSqlParser.LateralViewContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetQuantifier(ArcticExtendSparkSqlParser.SetQuantifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#relation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelation(ArcticExtendSparkSqlParser.RelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#joinRelation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinRelation(ArcticExtendSparkSqlParser.JoinRelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#joinType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinType(ArcticExtendSparkSqlParser.JoinTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#joinCriteria}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinCriteria(ArcticExtendSparkSqlParser.JoinCriteriaContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#sample}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSample(ArcticExtendSparkSqlParser.SampleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByPercentile}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByPercentile(ArcticExtendSparkSqlParser.SampleByPercentileContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByRows}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByRows(ArcticExtendSparkSqlParser.SampleByRowsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByBucket}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByBucket(ArcticExtendSparkSqlParser.SampleByBucketContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sampleByBytes}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#sampleMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleByBytes(ArcticExtendSparkSqlParser.SampleByBytesContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierList(ArcticExtendSparkSqlParser.IdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierSeq(ArcticExtendSparkSqlParser.IdentifierSeqContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#orderedIdentifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderedIdentifierList(ArcticExtendSparkSqlParser.OrderedIdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#orderedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderedIdentifier(ArcticExtendSparkSqlParser.OrderedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierCommentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierCommentList(ArcticExtendSparkSqlParser.IdentifierCommentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#identifierComment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierComment(ArcticExtendSparkSqlParser.IdentifierCommentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableName(ArcticExtendSparkSqlParser.TableNameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aliasedQuery}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedQuery(ArcticExtendSparkSqlParser.AliasedQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code aliasedRelation}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedRelation(ArcticExtendSparkSqlParser.AliasedRelationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inlineTableDefault2}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTableDefault2(ArcticExtendSparkSqlParser.InlineTableDefault2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code tableValuedFunction}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableValuedFunction(ArcticExtendSparkSqlParser.TableValuedFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#inlineTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTable(ArcticExtendSparkSqlParser.InlineTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#functionTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionTable(ArcticExtendSparkSqlParser.FunctionTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#tableAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableAlias(ArcticExtendSparkSqlParser.TableAliasContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowFormatSerde}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowFormatSerde(ArcticExtendSparkSqlParser.RowFormatSerdeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowFormatDelimited}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#rowFormat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowFormatDelimited(ArcticExtendSparkSqlParser.RowFormatDelimitedContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#multipartIdentifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipartIdentifierList(ArcticExtendSparkSqlParser.MultipartIdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipartIdentifier(ArcticExtendSparkSqlParser.MultipartIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableIdentifier(ArcticExtendSparkSqlParser.TableIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#functionIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionIdentifier(ArcticExtendSparkSqlParser.FunctionIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#namedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedExpression(ArcticExtendSparkSqlParser.NamedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#namedExpressionSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedExpressionSeq(ArcticExtendSparkSqlParser.NamedExpressionSeqContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#partitionFieldList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionFieldList(ArcticExtendSparkSqlParser.PartitionFieldListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code partitionTransform}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionTransform(ArcticExtendSparkSqlParser.PartitionTransformContext ctx);
	/**
	 * Visit a parse tree produced by the {@code partitionColumn}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#partitionField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionColumn(ArcticExtendSparkSqlParser.PartitionColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code identityTransform}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#transform}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentityTransform(ArcticExtendSparkSqlParser.IdentityTransformContext ctx);
	/**
	 * Visit a parse tree produced by the {@code applyTransform}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#transform}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApplyTransform(ArcticExtendSparkSqlParser.ApplyTransformContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#transformArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransformArgument(ArcticExtendSparkSqlParser.TransformArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(ArcticExtendSparkSqlParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#expressionSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionSeq(ArcticExtendSparkSqlParser.ExpressionSeqContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalNot(ArcticExtendSparkSqlParser.LogicalNotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicated(ArcticExtendSparkSqlParser.PredicatedContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExists(ArcticExtendSparkSqlParser.ExistsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalBinary(ArcticExtendSparkSqlParser.LogicalBinaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicate(ArcticExtendSparkSqlParser.PredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueExpressionDefault(ArcticExtendSparkSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison(ArcticExtendSparkSqlParser.ComparisonContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticBinary(ArcticExtendSparkSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticUnary(ArcticExtendSparkSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code struct}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct(ArcticExtendSparkSqlParser.StructContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDereference(ArcticExtendSparkSqlParser.DereferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleCase(ArcticExtendSparkSqlParser.SimpleCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code currentLike}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCurrentLike(ArcticExtendSparkSqlParser.CurrentLikeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnReference(ArcticExtendSparkSqlParser.ColumnReferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowConstructor(ArcticExtendSparkSqlParser.RowConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code last}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLast(ArcticExtendSparkSqlParser.LastContext ctx);
	/**
	 * Visit a parse tree produced by the {@code star}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStar(ArcticExtendSparkSqlParser.StarContext ctx);
	/**
	 * Visit a parse tree produced by the {@code overlay}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverlay(ArcticExtendSparkSqlParser.OverlayContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscript(ArcticExtendSparkSqlParser.SubscriptContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubqueryExpression(ArcticExtendSparkSqlParser.SubqueryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code substring}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubstring(ArcticExtendSparkSqlParser.SubstringContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast(ArcticExtendSparkSqlParser.CastContext ctx);
	/**
	 * Visit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDefault(ArcticExtendSparkSqlParser.ConstantDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda(ArcticExtendSparkSqlParser.LambdaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpression(ArcticExtendSparkSqlParser.ParenthesizedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtract(ArcticExtendSparkSqlParser.ExtractContext ctx);
	/**
	 * Visit a parse tree produced by the {@code trim}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrim(ArcticExtendSparkSqlParser.TrimContext ctx);
	/**
	 * Visit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(ArcticExtendSparkSqlParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearchedCase(ArcticExtendSparkSqlParser.SearchedCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code position}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPosition(ArcticExtendSparkSqlParser.PositionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code first}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFirst(ArcticExtendSparkSqlParser.FirstContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullLiteral(ArcticExtendSparkSqlParser.NullLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalLiteral(ArcticExtendSparkSqlParser.IntervalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeConstructor(ArcticExtendSparkSqlParser.TypeConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(ArcticExtendSparkSqlParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(ArcticExtendSparkSqlParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(ArcticExtendSparkSqlParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOperator(ArcticExtendSparkSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticOperator(ArcticExtendSparkSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#predicateOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicateOperator(ArcticExtendSparkSqlParser.PredicateOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanValue(ArcticExtendSparkSqlParser.BooleanValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#interval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval(ArcticExtendSparkSqlParser.IntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#errorCapturingMultiUnitsInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorCapturingMultiUnitsInterval(ArcticExtendSparkSqlParser.ErrorCapturingMultiUnitsIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#multiUnitsInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiUnitsInterval(ArcticExtendSparkSqlParser.MultiUnitsIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#errorCapturingUnitToUnitInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorCapturingUnitToUnitInterval(ArcticExtendSparkSqlParser.ErrorCapturingUnitToUnitIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#unitToUnitInterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitToUnitInterval(ArcticExtendSparkSqlParser.UnitToUnitIntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#intervalValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalValue(ArcticExtendSparkSqlParser.IntervalValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#colPosition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColPosition(ArcticExtendSparkSqlParser.ColPositionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code complexDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexDataType(ArcticExtendSparkSqlParser.ComplexDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code yearMonthIntervalDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYearMonthIntervalDataType(ArcticExtendSparkSqlParser.YearMonthIntervalDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dayTimeIntervalDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDayTimeIntervalDataType(ArcticExtendSparkSqlParser.DayTimeIntervalDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code primitiveDataType}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveDataType(ArcticExtendSparkSqlParser.PrimitiveDataTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedColTypeWithPositionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedColTypeWithPositionList(ArcticExtendSparkSqlParser.QualifiedColTypeWithPositionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedColTypeWithPosition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedColTypeWithPosition(ArcticExtendSparkSqlParser.QualifiedColTypeWithPositionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColTypeList(ArcticExtendSparkSqlParser.ColTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#colType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColType(ArcticExtendSparkSqlParser.ColTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#complexColTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexColTypeList(ArcticExtendSparkSqlParser.ComplexColTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#complexColType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComplexColType(ArcticExtendSparkSqlParser.ComplexColTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#whenClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenClause(ArcticExtendSparkSqlParser.WhenClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#windowClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowClause(ArcticExtendSparkSqlParser.WindowClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#namedWindow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedWindow(ArcticExtendSparkSqlParser.NamedWindowContext ctx);
	/**
	 * Visit a parse tree produced by the {@code windowRef}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowRef(ArcticExtendSparkSqlParser.WindowRefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code windowDef}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#windowSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowDef(ArcticExtendSparkSqlParser.WindowDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#windowFrame}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowFrame(ArcticExtendSparkSqlParser.WindowFrameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#frameBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrameBound(ArcticExtendSparkSqlParser.FrameBoundContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedNameList(ArcticExtendSparkSqlParser.QualifiedNameListContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#functionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionName(ArcticExtendSparkSqlParser.FunctionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(ArcticExtendSparkSqlParser.QualifiedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorCapturingIdentifier(ArcticExtendSparkSqlParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorIdent(ArcticExtendSparkSqlParser.ErrorIdentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRealIdent(ArcticExtendSparkSqlParser.RealIdentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(ArcticExtendSparkSqlParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnquotedIdentifier(ArcticExtendSparkSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#strictIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifierAlternative(ArcticExtendSparkSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifier(ArcticExtendSparkSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exponentLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExponentLiteral(ArcticExtendSparkSqlParser.ExponentLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecimalLiteral(ArcticExtendSparkSqlParser.DecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLegacyDecimalLiteral(ArcticExtendSparkSqlParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerLiteral(ArcticExtendSparkSqlParser.IntegerLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigIntLiteral(ArcticExtendSparkSqlParser.BigIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSmallIntLiteral(ArcticExtendSparkSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTinyIntLiteral(ArcticExtendSparkSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleLiteral(ArcticExtendSparkSqlParser.DoubleLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatLiteral(ArcticExtendSparkSqlParser.FloatLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArcticExtendSparkSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigDecimalLiteral(ArcticExtendSparkSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#alterColumnAction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterColumnAction(ArcticExtendSparkSqlParser.AlterColumnActionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#ansiNonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnsiNonReserved(ArcticExtendSparkSqlParser.AnsiNonReservedContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#strictNonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStrictNonReserved(ArcticExtendSparkSqlParser.StrictNonReservedContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticExtendSparkSqlParser#nonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonReserved(ArcticExtendSparkSqlParser.NonReservedContext ctx);
}