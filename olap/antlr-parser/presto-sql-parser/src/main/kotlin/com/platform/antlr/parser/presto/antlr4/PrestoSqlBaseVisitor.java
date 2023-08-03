// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.presto.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link PrestoSqlBaseParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface PrestoSqlBaseVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#singleStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleStatement(PrestoSqlBaseParser.SingleStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#standaloneExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStandaloneExpression(PrestoSqlBaseParser.StandaloneExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#standaloneRoutineBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStandaloneRoutineBody(PrestoSqlBaseParser.StandaloneRoutineBodyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementDefault(PrestoSqlBaseParser.StatementDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code use}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUse(PrestoSqlBaseParser.UseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createSchema}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateSchema(PrestoSqlBaseParser.CreateSchemaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropSchema}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropSchema(PrestoSqlBaseParser.DropSchemaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code renameSchema}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameSchema(PrestoSqlBaseParser.RenameSchemaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTableAsSelect}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTableAsSelect(PrestoSqlBaseParser.CreateTableAsSelectContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTable(PrestoSqlBaseParser.CreateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropTable(PrestoSqlBaseParser.DropTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code insertInto}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertInto(PrestoSqlBaseParser.InsertIntoContext ctx);
	/**
	 * Visit a parse tree produced by the {@code delete}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete(PrestoSqlBaseParser.DeleteContext ctx);
	/**
	 * Visit a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTruncateTable(PrestoSqlBaseParser.TruncateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameTable(PrestoSqlBaseParser.RenameTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code renameColumn}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenameColumn(PrestoSqlBaseParser.RenameColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropColumn}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropColumn(PrestoSqlBaseParser.DropColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addColumn}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddColumn(PrestoSqlBaseParser.AddColumnContext ctx);
	/**
	 * Visit a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyze(PrestoSqlBaseParser.AnalyzeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createType}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateType(PrestoSqlBaseParser.CreateTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateView(PrestoSqlBaseParser.CreateViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropView(PrestoSqlBaseParser.DropViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateMaterializedView(PrestoSqlBaseParser.CreateMaterializedViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropMaterializedView(PrestoSqlBaseParser.DropMaterializedViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code refreshMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefreshMaterializedView(PrestoSqlBaseParser.RefreshMaterializedViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateFunction(PrestoSqlBaseParser.CreateFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code alterFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterFunction(PrestoSqlBaseParser.AlterFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropFunction(PrestoSqlBaseParser.DropFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code call}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCall(PrestoSqlBaseParser.CallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createRole}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateRole(PrestoSqlBaseParser.CreateRoleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dropRole}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropRole(PrestoSqlBaseParser.DropRoleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code grantRoles}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrantRoles(PrestoSqlBaseParser.GrantRolesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code revokeRoles}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRevokeRoles(PrestoSqlBaseParser.RevokeRolesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setRole}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetRole(PrestoSqlBaseParser.SetRoleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code grant}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant(PrestoSqlBaseParser.GrantContext ctx);
	/**
	 * Visit a parse tree produced by the {@code revoke}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRevoke(PrestoSqlBaseParser.RevokeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showGrants}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowGrants(PrestoSqlBaseParser.ShowGrantsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code explain}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain(PrestoSqlBaseParser.ExplainContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowCreateTable(PrestoSqlBaseParser.ShowCreateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showCreateView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowCreateView(PrestoSqlBaseParser.ShowCreateViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showCreateMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowCreateMaterializedView(PrestoSqlBaseParser.ShowCreateMaterializedViewContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showCreateFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowCreateFunction(PrestoSqlBaseParser.ShowCreateFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowTables(PrestoSqlBaseParser.ShowTablesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showSchemas}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowSchemas(PrestoSqlBaseParser.ShowSchemasContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showCatalogs}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowCatalogs(PrestoSqlBaseParser.ShowCatalogsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowColumns(PrestoSqlBaseParser.ShowColumnsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showStats}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowStats(PrestoSqlBaseParser.ShowStatsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showStatsForQuery}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowStatsForQuery(PrestoSqlBaseParser.ShowStatsForQueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showRoles}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowRoles(PrestoSqlBaseParser.ShowRolesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showRoleGrants}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowRoleGrants(PrestoSqlBaseParser.ShowRoleGrantsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowFunctions(PrestoSqlBaseParser.ShowFunctionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code showSession}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowSession(PrestoSqlBaseParser.ShowSessionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setSession}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetSession(PrestoSqlBaseParser.SetSessionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code resetSession}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetSession(PrestoSqlBaseParser.ResetSessionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code startTransaction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartTransaction(PrestoSqlBaseParser.StartTransactionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code commit}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommit(PrestoSqlBaseParser.CommitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rollback}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollback(PrestoSqlBaseParser.RollbackContext ctx);
	/**
	 * Visit a parse tree produced by the {@code prepare}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrepare(PrestoSqlBaseParser.PrepareContext ctx);
	/**
	 * Visit a parse tree produced by the {@code deallocate}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeallocate(PrestoSqlBaseParser.DeallocateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code execute}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute(PrestoSqlBaseParser.ExecuteContext ctx);
	/**
	 * Visit a parse tree produced by the {@code describeInput}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeInput(PrestoSqlBaseParser.DescribeInputContext ctx);
	/**
	 * Visit a parse tree produced by the {@code describeOutput}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribeOutput(PrestoSqlBaseParser.DescribeOutputContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(PrestoSqlBaseParser.QueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#with}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith(PrestoSqlBaseParser.WithContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#tableElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableElement(PrestoSqlBaseParser.TableElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#columnDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnDefinition(PrestoSqlBaseParser.ColumnDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#likeClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLikeClause(PrestoSqlBaseParser.LikeClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProperties(PrestoSqlBaseParser.PropertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#property}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProperty(PrestoSqlBaseParser.PropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#sqlParameterDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSqlParameterDeclaration(PrestoSqlBaseParser.SqlParameterDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#routineCharacteristics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutineCharacteristics(PrestoSqlBaseParser.RoutineCharacteristicsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#routineCharacteristic}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutineCharacteristic(PrestoSqlBaseParser.RoutineCharacteristicContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#alterRoutineCharacteristics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterRoutineCharacteristics(PrestoSqlBaseParser.AlterRoutineCharacteristicsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#alterRoutineCharacteristic}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterRoutineCharacteristic(PrestoSqlBaseParser.AlterRoutineCharacteristicContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#routineBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutineBody(PrestoSqlBaseParser.RoutineBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#returnStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(PrestoSqlBaseParser.ReturnStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#externalBodyReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternalBodyReference(PrestoSqlBaseParser.ExternalBodyReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#language}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLanguage(PrestoSqlBaseParser.LanguageContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#determinism}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeterminism(PrestoSqlBaseParser.DeterminismContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#nullCallClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullCallClause(PrestoSqlBaseParser.NullCallClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#externalRoutineName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternalRoutineName(PrestoSqlBaseParser.ExternalRoutineNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#queryNoWith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryNoWith(PrestoSqlBaseParser.QueryNoWithContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryTermDefault(PrestoSqlBaseParser.QueryTermDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetOperation(PrestoSqlBaseParser.SetOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueryPrimaryDefault(PrestoSqlBaseParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable(PrestoSqlBaseParser.TableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inlineTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTable(PrestoSqlBaseParser.InlineTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery(PrestoSqlBaseParser.SubqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#sortItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortItem(PrestoSqlBaseParser.SortItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#querySpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuerySpecification(PrestoSqlBaseParser.QuerySpecificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#groupBy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupBy(PrestoSqlBaseParser.GroupByContext ctx);
	/**
	 * Visit a parse tree produced by the {@code singleGroupingSet}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleGroupingSet(PrestoSqlBaseParser.SingleGroupingSetContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rollup}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollup(PrestoSqlBaseParser.RollupContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cube}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCube(PrestoSqlBaseParser.CubeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code multipleGroupingSets}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipleGroupingSets(PrestoSqlBaseParser.MultipleGroupingSetsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#groupingSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingSet(PrestoSqlBaseParser.GroupingSetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#namedQuery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedQuery(PrestoSqlBaseParser.NamedQueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#setQuantifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetQuantifier(PrestoSqlBaseParser.SetQuantifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code selectSingle}
	 * labeled alternative in {@link PrestoSqlBaseParser#selectItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectSingle(PrestoSqlBaseParser.SelectSingleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code selectAll}
	 * labeled alternative in {@link PrestoSqlBaseParser#selectItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectAll(PrestoSqlBaseParser.SelectAllContext ctx);
	/**
	 * Visit a parse tree produced by the {@code relationDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#relation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationDefault(PrestoSqlBaseParser.RelationDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code joinRelation}
	 * labeled alternative in {@link PrestoSqlBaseParser#relation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinRelation(PrestoSqlBaseParser.JoinRelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#joinType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinType(PrestoSqlBaseParser.JoinTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#joinCriteria}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoinCriteria(PrestoSqlBaseParser.JoinCriteriaContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#sampledRelation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampledRelation(PrestoSqlBaseParser.SampledRelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#sampleType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSampleType(PrestoSqlBaseParser.SampleTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#aliasedRelation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasedRelation(PrestoSqlBaseParser.AliasedRelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#columnAliases}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnAliases(PrestoSqlBaseParser.ColumnAliasesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableName(PrestoSqlBaseParser.TableNameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subqueryRelation}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubqueryRelation(PrestoSqlBaseParser.SubqueryRelationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unnest}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnnest(PrestoSqlBaseParser.UnnestContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lateral}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLateral(PrestoSqlBaseParser.LateralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesizedRelation}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedRelation(PrestoSqlBaseParser.ParenthesizedRelationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(PrestoSqlBaseParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link PrestoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalNot(PrestoSqlBaseParser.LogicalNotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link PrestoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicated(PrestoSqlBaseParser.PredicatedContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link PrestoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalBinary(PrestoSqlBaseParser.LogicalBinaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison(PrestoSqlBaseParser.ComparisonContext ctx);
	/**
	 * Visit a parse tree produced by the {@code quantifiedComparison}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuantifiedComparison(PrestoSqlBaseParser.QuantifiedComparisonContext ctx);
	/**
	 * Visit a parse tree produced by the {@code between}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBetween(PrestoSqlBaseParser.BetweenContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inList}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInList(PrestoSqlBaseParser.InListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inSubquery}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInSubquery(PrestoSqlBaseParser.InSubqueryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code like}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLike(PrestoSqlBaseParser.LikeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nullPredicate}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullPredicate(PrestoSqlBaseParser.NullPredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code distinctFrom}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDistinctFrom(PrestoSqlBaseParser.DistinctFromContext ctx);
	/**
	 * Visit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueExpressionDefault(PrestoSqlBaseParser.ValueExpressionDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code concatenation}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConcatenation(PrestoSqlBaseParser.ConcatenationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticBinary(PrestoSqlBaseParser.ArithmeticBinaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticUnary(PrestoSqlBaseParser.ArithmeticUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code atTimeZone}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtTimeZone(PrestoSqlBaseParser.AtTimeZoneContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDereference(PrestoSqlBaseParser.DereferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeConstructor(PrestoSqlBaseParser.TypeConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code specialDateTimeFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecialDateTimeFunction(PrestoSqlBaseParser.SpecialDateTimeFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code substring}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubstring(PrestoSqlBaseParser.SubstringContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast(PrestoSqlBaseParser.CastContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambda(PrestoSqlBaseParser.LambdaContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpression(PrestoSqlBaseParser.ParenthesizedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parameter}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(PrestoSqlBaseParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by the {@code normalize}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNormalize(PrestoSqlBaseParser.NormalizeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalLiteral(PrestoSqlBaseParser.IntervalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(PrestoSqlBaseParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(PrestoSqlBaseParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleCase(PrestoSqlBaseParser.SimpleCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnReference(PrestoSqlBaseParser.ColumnReferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullLiteral(PrestoSqlBaseParser.NullLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowConstructor(PrestoSqlBaseParser.RowConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscript(PrestoSqlBaseParser.SubscriptContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubqueryExpression(PrestoSqlBaseParser.SubqueryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code binaryLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryLiteral(PrestoSqlBaseParser.BinaryLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code currentUser}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCurrentUser(PrestoSqlBaseParser.CurrentUserContext ctx);
	/**
	 * Visit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtract(PrestoSqlBaseParser.ExtractContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(PrestoSqlBaseParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arrayConstructor}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayConstructor(PrestoSqlBaseParser.ArrayConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(PrestoSqlBaseParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExists(PrestoSqlBaseParser.ExistsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code position}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPosition(PrestoSqlBaseParser.PositionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearchedCase(PrestoSqlBaseParser.SearchedCaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code groupingOperation}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupingOperation(PrestoSqlBaseParser.GroupingOperationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code basicStringLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBasicStringLiteral(PrestoSqlBaseParser.BasicStringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unicodeStringLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnicodeStringLiteral(PrestoSqlBaseParser.UnicodeStringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#nullTreatment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullTreatment(PrestoSqlBaseParser.NullTreatmentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code timeZoneInterval}
	 * labeled alternative in {@link PrestoSqlBaseParser#timeZoneSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimeZoneInterval(PrestoSqlBaseParser.TimeZoneIntervalContext ctx);
	/**
	 * Visit a parse tree produced by the {@code timeZoneString}
	 * labeled alternative in {@link PrestoSqlBaseParser#timeZoneSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimeZoneString(PrestoSqlBaseParser.TimeZoneStringContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOperator(PrestoSqlBaseParser.ComparisonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#comparisonQuantifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonQuantifier(PrestoSqlBaseParser.ComparisonQuantifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#booleanValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanValue(PrestoSqlBaseParser.BooleanValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#interval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval(PrestoSqlBaseParser.IntervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#intervalField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntervalField(PrestoSqlBaseParser.IntervalFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#normalForm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNormalForm(PrestoSqlBaseParser.NormalFormContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#types}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypes(PrestoSqlBaseParser.TypesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(PrestoSqlBaseParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(PrestoSqlBaseParser.TypeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#baseType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseType(PrestoSqlBaseParser.BaseTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#whenClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenClause(PrestoSqlBaseParser.WhenClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilter(PrestoSqlBaseParser.FilterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#over}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOver(PrestoSqlBaseParser.OverContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#windowFrame}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowFrame(PrestoSqlBaseParser.WindowFrameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unboundedFrame}
	 * labeled alternative in {@link PrestoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnboundedFrame(PrestoSqlBaseParser.UnboundedFrameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code currentRowBound}
	 * labeled alternative in {@link PrestoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCurrentRowBound(PrestoSqlBaseParser.CurrentRowBoundContext ctx);
	/**
	 * Visit a parse tree produced by the {@code boundedFrame}
	 * labeled alternative in {@link PrestoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoundedFrame(PrestoSqlBaseParser.BoundedFrameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code explainFormat}
	 * labeled alternative in {@link PrestoSqlBaseParser#explainOption}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplainFormat(PrestoSqlBaseParser.ExplainFormatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code explainType}
	 * labeled alternative in {@link PrestoSqlBaseParser#explainOption}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplainType(PrestoSqlBaseParser.ExplainTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code isolationLevel}
	 * labeled alternative in {@link PrestoSqlBaseParser#transactionMode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsolationLevel(PrestoSqlBaseParser.IsolationLevelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code transactionAccessMode}
	 * labeled alternative in {@link PrestoSqlBaseParser#transactionMode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransactionAccessMode(PrestoSqlBaseParser.TransactionAccessModeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code readUncommitted}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReadUncommitted(PrestoSqlBaseParser.ReadUncommittedContext ctx);
	/**
	 * Visit a parse tree produced by the {@code readCommitted}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReadCommitted(PrestoSqlBaseParser.ReadCommittedContext ctx);
	/**
	 * Visit a parse tree produced by the {@code repeatableRead}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRepeatableRead(PrestoSqlBaseParser.RepeatableReadContext ctx);
	/**
	 * Visit a parse tree produced by the {@code serializable}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSerializable(PrestoSqlBaseParser.SerializableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code positionalArgument}
	 * labeled alternative in {@link PrestoSqlBaseParser#callArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositionalArgument(PrestoSqlBaseParser.PositionalArgumentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code namedArgument}
	 * labeled alternative in {@link PrestoSqlBaseParser#callArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedArgument(PrestoSqlBaseParser.NamedArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#privilege}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrivilege(PrestoSqlBaseParser.PrivilegeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(PrestoSqlBaseParser.QualifiedNameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code currentUserGrantor}
	 * labeled alternative in {@link PrestoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCurrentUserGrantor(PrestoSqlBaseParser.CurrentUserGrantorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code currentRoleGrantor}
	 * labeled alternative in {@link PrestoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCurrentRoleGrantor(PrestoSqlBaseParser.CurrentRoleGrantorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code specifiedPrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecifiedPrincipal(PrestoSqlBaseParser.SpecifiedPrincipalContext ctx);
	/**
	 * Visit a parse tree produced by the {@code userPrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUserPrincipal(PrestoSqlBaseParser.UserPrincipalContext ctx);
	/**
	 * Visit a parse tree produced by the {@code rolePrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRolePrincipal(PrestoSqlBaseParser.RolePrincipalContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unspecifiedPrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnspecifiedPrincipal(PrestoSqlBaseParser.UnspecifiedPrincipalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#roles}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoles(PrestoSqlBaseParser.RolesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnquotedIdentifier(PrestoSqlBaseParser.UnquotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code quotedIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifier(PrestoSqlBaseParser.QuotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code backQuotedIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBackQuotedIdentifier(PrestoSqlBaseParser.BackQuotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code digitIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDigitIdentifier(PrestoSqlBaseParser.DigitIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecimalLiteral(PrestoSqlBaseParser.DecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleLiteral(PrestoSqlBaseParser.DoubleLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerLiteral(PrestoSqlBaseParser.IntegerLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link PrestoSqlBaseParser#nonReserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonReserved(PrestoSqlBaseParser.NonReservedContext ctx);
}