// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.trino.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TrinoSqlBaseParser}.
 */
public interface TrinoSqlBaseListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void enterSingleStatement(TrinoSqlBaseParser.SingleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void exitSingleStatement(TrinoSqlBaseParser.SingleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#standaloneExpression}.
	 * @param ctx the parse tree
	 */
	void enterStandaloneExpression(TrinoSqlBaseParser.StandaloneExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#standaloneExpression}.
	 * @param ctx the parse tree
	 */
	void exitStandaloneExpression(TrinoSqlBaseParser.StandaloneExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#standaloneRoutineBody}.
	 * @param ctx the parse tree
	 */
	void enterStandaloneRoutineBody(TrinoSqlBaseParser.StandaloneRoutineBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#standaloneRoutineBody}.
	 * @param ctx the parse tree
	 */
	void exitStandaloneRoutineBody(TrinoSqlBaseParser.StandaloneRoutineBodyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatementDefault(TrinoSqlBaseParser.StatementDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatementDefault(TrinoSqlBaseParser.StatementDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code use}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUse(TrinoSqlBaseParser.UseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code use}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUse(TrinoSqlBaseParser.UseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createSchema}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateSchema(TrinoSqlBaseParser.CreateSchemaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createSchema}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateSchema(TrinoSqlBaseParser.CreateSchemaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropSchema}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropSchema(TrinoSqlBaseParser.DropSchemaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropSchema}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropSchema(TrinoSqlBaseParser.DropSchemaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameSchema}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameSchema(TrinoSqlBaseParser.RenameSchemaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameSchema}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameSchema(TrinoSqlBaseParser.RenameSchemaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTableAsSelect}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableAsSelect(TrinoSqlBaseParser.CreateTableAsSelectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTableAsSelect}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableAsSelect(TrinoSqlBaseParser.CreateTableAsSelectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTable(TrinoSqlBaseParser.CreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTable(TrinoSqlBaseParser.CreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTable(TrinoSqlBaseParser.DropTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTable(TrinoSqlBaseParser.DropTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertInto}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterInsertInto(TrinoSqlBaseParser.InsertIntoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertInto}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitInsertInto(TrinoSqlBaseParser.InsertIntoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code delete}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDelete(TrinoSqlBaseParser.DeleteContext ctx);
	/**
	 * Exit a parse tree produced by the {@code delete}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDelete(TrinoSqlBaseParser.DeleteContext ctx);
	/**
	 * Enter a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterTruncateTable(TrinoSqlBaseParser.TruncateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitTruncateTable(TrinoSqlBaseParser.TruncateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTable(TrinoSqlBaseParser.RenameTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTable(TrinoSqlBaseParser.RenameTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameColumn}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameColumn(TrinoSqlBaseParser.RenameColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameColumn}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameColumn(TrinoSqlBaseParser.RenameColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropColumn}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropColumn(TrinoSqlBaseParser.DropColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropColumn}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropColumn(TrinoSqlBaseParser.DropColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addColumn}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAddColumn(TrinoSqlBaseParser.AddColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addColumn}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAddColumn(TrinoSqlBaseParser.AddColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze(TrinoSqlBaseParser.AnalyzeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze(TrinoSqlBaseParser.AnalyzeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createType}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateType(TrinoSqlBaseParser.CreateTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createType}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateType(TrinoSqlBaseParser.CreateTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateView(TrinoSqlBaseParser.CreateViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateView(TrinoSqlBaseParser.CreateViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropView(TrinoSqlBaseParser.DropViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropView(TrinoSqlBaseParser.DropViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createMaterializedView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateMaterializedView(TrinoSqlBaseParser.CreateMaterializedViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createMaterializedView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateMaterializedView(TrinoSqlBaseParser.CreateMaterializedViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropMaterializedView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropMaterializedView(TrinoSqlBaseParser.DropMaterializedViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropMaterializedView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropMaterializedView(TrinoSqlBaseParser.DropMaterializedViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshMaterializedView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshMaterializedView(TrinoSqlBaseParser.RefreshMaterializedViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshMaterializedView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshMaterializedView(TrinoSqlBaseParser.RefreshMaterializedViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateFunction(TrinoSqlBaseParser.CreateFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateFunction(TrinoSqlBaseParser.CreateFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterFunction}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterFunction(TrinoSqlBaseParser.AlterFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterFunction}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterFunction(TrinoSqlBaseParser.AlterFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropFunction(TrinoSqlBaseParser.DropFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropFunction(TrinoSqlBaseParser.DropFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code call}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCall(TrinoSqlBaseParser.CallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code call}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCall(TrinoSqlBaseParser.CallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createRole}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateRole(TrinoSqlBaseParser.CreateRoleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createRole}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateRole(TrinoSqlBaseParser.CreateRoleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropRole}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropRole(TrinoSqlBaseParser.DropRoleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropRole}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropRole(TrinoSqlBaseParser.DropRoleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grantRoles}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterGrantRoles(TrinoSqlBaseParser.GrantRolesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grantRoles}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitGrantRoles(TrinoSqlBaseParser.GrantRolesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revokeRoles}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRevokeRoles(TrinoSqlBaseParser.RevokeRolesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revokeRoles}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRevokeRoles(TrinoSqlBaseParser.RevokeRolesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setRole}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetRole(TrinoSqlBaseParser.SetRoleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setRole}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetRole(TrinoSqlBaseParser.SetRoleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grant}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterGrant(TrinoSqlBaseParser.GrantContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grant}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitGrant(TrinoSqlBaseParser.GrantContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revoke}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRevoke(TrinoSqlBaseParser.RevokeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revoke}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRevoke(TrinoSqlBaseParser.RevokeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showGrants}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowGrants(TrinoSqlBaseParser.ShowGrantsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showGrants}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowGrants(TrinoSqlBaseParser.ShowGrantsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code explain}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterExplain(TrinoSqlBaseParser.ExplainContext ctx);
	/**
	 * Exit a parse tree produced by the {@code explain}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitExplain(TrinoSqlBaseParser.ExplainContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateTable(TrinoSqlBaseParser.ShowCreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateTable(TrinoSqlBaseParser.ShowCreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateView(TrinoSqlBaseParser.ShowCreateViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateView(TrinoSqlBaseParser.ShowCreateViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateMaterializedView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateMaterializedView(TrinoSqlBaseParser.ShowCreateMaterializedViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateMaterializedView}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateMaterializedView(TrinoSqlBaseParser.ShowCreateMaterializedViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateFunction}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateFunction(TrinoSqlBaseParser.ShowCreateFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateFunction}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateFunction(TrinoSqlBaseParser.ShowCreateFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTables(TrinoSqlBaseParser.ShowTablesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTables(TrinoSqlBaseParser.ShowTablesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showSchemas}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowSchemas(TrinoSqlBaseParser.ShowSchemasContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showSchemas}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowSchemas(TrinoSqlBaseParser.ShowSchemasContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCatalogs}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCatalogs(TrinoSqlBaseParser.ShowCatalogsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCatalogs}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCatalogs(TrinoSqlBaseParser.ShowCatalogsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowColumns(TrinoSqlBaseParser.ShowColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowColumns(TrinoSqlBaseParser.ShowColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showStats}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowStats(TrinoSqlBaseParser.ShowStatsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showStats}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowStats(TrinoSqlBaseParser.ShowStatsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showStatsForQuery}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowStatsForQuery(TrinoSqlBaseParser.ShowStatsForQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showStatsForQuery}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowStatsForQuery(TrinoSqlBaseParser.ShowStatsForQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showRoles}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowRoles(TrinoSqlBaseParser.ShowRolesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showRoles}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowRoles(TrinoSqlBaseParser.ShowRolesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showRoleGrants}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowRoleGrants(TrinoSqlBaseParser.ShowRoleGrantsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showRoleGrants}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowRoleGrants(TrinoSqlBaseParser.ShowRoleGrantsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowFunctions(TrinoSqlBaseParser.ShowFunctionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowFunctions(TrinoSqlBaseParser.ShowFunctionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showSession}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowSession(TrinoSqlBaseParser.ShowSessionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showSession}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowSession(TrinoSqlBaseParser.ShowSessionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setSession}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetSession(TrinoSqlBaseParser.SetSessionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setSession}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetSession(TrinoSqlBaseParser.SetSessionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resetSession}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterResetSession(TrinoSqlBaseParser.ResetSessionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resetSession}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitResetSession(TrinoSqlBaseParser.ResetSessionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code startTransaction}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStartTransaction(TrinoSqlBaseParser.StartTransactionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code startTransaction}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStartTransaction(TrinoSqlBaseParser.StartTransactionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code commit}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCommit(TrinoSqlBaseParser.CommitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code commit}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCommit(TrinoSqlBaseParser.CommitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rollback}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRollback(TrinoSqlBaseParser.RollbackContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rollback}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRollback(TrinoSqlBaseParser.RollbackContext ctx);
	/**
	 * Enter a parse tree produced by the {@code prepare}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterPrepare(TrinoSqlBaseParser.PrepareContext ctx);
	/**
	 * Exit a parse tree produced by the {@code prepare}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitPrepare(TrinoSqlBaseParser.PrepareContext ctx);
	/**
	 * Enter a parse tree produced by the {@code deallocate}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDeallocate(TrinoSqlBaseParser.DeallocateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code deallocate}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDeallocate(TrinoSqlBaseParser.DeallocateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code execute}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterExecute(TrinoSqlBaseParser.ExecuteContext ctx);
	/**
	 * Exit a parse tree produced by the {@code execute}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitExecute(TrinoSqlBaseParser.ExecuteContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeInput}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeInput(TrinoSqlBaseParser.DescribeInputContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeInput}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeInput(TrinoSqlBaseParser.DescribeInputContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeOutput}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeOutput(TrinoSqlBaseParser.DescribeOutputContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeOutput}
	 * labeled alternative in {@link TrinoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeOutput(TrinoSqlBaseParser.DescribeOutputContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(TrinoSqlBaseParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(TrinoSqlBaseParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#with}.
	 * @param ctx the parse tree
	 */
	void enterWith(TrinoSqlBaseParser.WithContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#with}.
	 * @param ctx the parse tree
	 */
	void exitWith(TrinoSqlBaseParser.WithContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#tableElement}.
	 * @param ctx the parse tree
	 */
	void enterTableElement(TrinoSqlBaseParser.TableElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#tableElement}.
	 * @param ctx the parse tree
	 */
	void exitTableElement(TrinoSqlBaseParser.TableElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#columnDefinition}.
	 * @param ctx the parse tree
	 */
	void enterColumnDefinition(TrinoSqlBaseParser.ColumnDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#columnDefinition}.
	 * @param ctx the parse tree
	 */
	void exitColumnDefinition(TrinoSqlBaseParser.ColumnDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#likeClause}.
	 * @param ctx the parse tree
	 */
	void enterLikeClause(TrinoSqlBaseParser.LikeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#likeClause}.
	 * @param ctx the parse tree
	 */
	void exitLikeClause(TrinoSqlBaseParser.LikeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#properties}.
	 * @param ctx the parse tree
	 */
	void enterProperties(TrinoSqlBaseParser.PropertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#properties}.
	 * @param ctx the parse tree
	 */
	void exitProperties(TrinoSqlBaseParser.PropertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#property}.
	 * @param ctx the parse tree
	 */
	void enterProperty(TrinoSqlBaseParser.PropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#property}.
	 * @param ctx the parse tree
	 */
	void exitProperty(TrinoSqlBaseParser.PropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#sqlParameterDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterSqlParameterDeclaration(TrinoSqlBaseParser.SqlParameterDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#sqlParameterDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitSqlParameterDeclaration(TrinoSqlBaseParser.SqlParameterDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#routineCharacteristics}.
	 * @param ctx the parse tree
	 */
	void enterRoutineCharacteristics(TrinoSqlBaseParser.RoutineCharacteristicsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#routineCharacteristics}.
	 * @param ctx the parse tree
	 */
	void exitRoutineCharacteristics(TrinoSqlBaseParser.RoutineCharacteristicsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#routineCharacteristic}.
	 * @param ctx the parse tree
	 */
	void enterRoutineCharacteristic(TrinoSqlBaseParser.RoutineCharacteristicContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#routineCharacteristic}.
	 * @param ctx the parse tree
	 */
	void exitRoutineCharacteristic(TrinoSqlBaseParser.RoutineCharacteristicContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#alterRoutineCharacteristics}.
	 * @param ctx the parse tree
	 */
	void enterAlterRoutineCharacteristics(TrinoSqlBaseParser.AlterRoutineCharacteristicsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#alterRoutineCharacteristics}.
	 * @param ctx the parse tree
	 */
	void exitAlterRoutineCharacteristics(TrinoSqlBaseParser.AlterRoutineCharacteristicsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#alterRoutineCharacteristic}.
	 * @param ctx the parse tree
	 */
	void enterAlterRoutineCharacteristic(TrinoSqlBaseParser.AlterRoutineCharacteristicContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#alterRoutineCharacteristic}.
	 * @param ctx the parse tree
	 */
	void exitAlterRoutineCharacteristic(TrinoSqlBaseParser.AlterRoutineCharacteristicContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void enterRoutineBody(TrinoSqlBaseParser.RoutineBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void exitRoutineBody(TrinoSqlBaseParser.RoutineBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(TrinoSqlBaseParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(TrinoSqlBaseParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#externalBodyReference}.
	 * @param ctx the parse tree
	 */
	void enterExternalBodyReference(TrinoSqlBaseParser.ExternalBodyReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#externalBodyReference}.
	 * @param ctx the parse tree
	 */
	void exitExternalBodyReference(TrinoSqlBaseParser.ExternalBodyReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#language}.
	 * @param ctx the parse tree
	 */
	void enterLanguage(TrinoSqlBaseParser.LanguageContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#language}.
	 * @param ctx the parse tree
	 */
	void exitLanguage(TrinoSqlBaseParser.LanguageContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#determinism}.
	 * @param ctx the parse tree
	 */
	void enterDeterminism(TrinoSqlBaseParser.DeterminismContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#determinism}.
	 * @param ctx the parse tree
	 */
	void exitDeterminism(TrinoSqlBaseParser.DeterminismContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#nullCallClause}.
	 * @param ctx the parse tree
	 */
	void enterNullCallClause(TrinoSqlBaseParser.NullCallClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#nullCallClause}.
	 * @param ctx the parse tree
	 */
	void exitNullCallClause(TrinoSqlBaseParser.NullCallClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#externalRoutineName}.
	 * @param ctx the parse tree
	 */
	void enterExternalRoutineName(TrinoSqlBaseParser.ExternalRoutineNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#externalRoutineName}.
	 * @param ctx the parse tree
	 */
	void exitExternalRoutineName(TrinoSqlBaseParser.ExternalRoutineNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#queryNoWith}.
	 * @param ctx the parse tree
	 */
	void enterQueryNoWith(TrinoSqlBaseParser.QueryNoWithContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#queryNoWith}.
	 * @param ctx the parse tree
	 */
	void exitQueryNoWith(TrinoSqlBaseParser.QueryNoWithContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterQueryTermDefault(TrinoSqlBaseParser.QueryTermDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitQueryTermDefault(TrinoSqlBaseParser.QueryTermDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterSetOperation(TrinoSqlBaseParser.SetOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitSetOperation(TrinoSqlBaseParser.SetOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterQueryPrimaryDefault(TrinoSqlBaseParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitQueryPrimaryDefault(TrinoSqlBaseParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTable(TrinoSqlBaseParser.TableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTable(TrinoSqlBaseParser.TableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTable(TrinoSqlBaseParser.InlineTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTable}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTable(TrinoSqlBaseParser.InlineTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(TrinoSqlBaseParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link TrinoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(TrinoSqlBaseParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void enterSortItem(TrinoSqlBaseParser.SortItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void exitSortItem(TrinoSqlBaseParser.SortItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterQuerySpecification(TrinoSqlBaseParser.QuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitQuerySpecification(TrinoSqlBaseParser.QuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#groupBy}.
	 * @param ctx the parse tree
	 */
	void enterGroupBy(TrinoSqlBaseParser.GroupByContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#groupBy}.
	 * @param ctx the parse tree
	 */
	void exitGroupBy(TrinoSqlBaseParser.GroupByContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleGroupingSet}
	 * labeled alternative in {@link TrinoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterSingleGroupingSet(TrinoSqlBaseParser.SingleGroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleGroupingSet}
	 * labeled alternative in {@link TrinoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitSingleGroupingSet(TrinoSqlBaseParser.SingleGroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rollup}
	 * labeled alternative in {@link TrinoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterRollup(TrinoSqlBaseParser.RollupContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rollup}
	 * labeled alternative in {@link TrinoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitRollup(TrinoSqlBaseParser.RollupContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cube}
	 * labeled alternative in {@link TrinoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterCube(TrinoSqlBaseParser.CubeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cube}
	 * labeled alternative in {@link TrinoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitCube(TrinoSqlBaseParser.CubeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multipleGroupingSets}
	 * labeled alternative in {@link TrinoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterMultipleGroupingSets(TrinoSqlBaseParser.MultipleGroupingSetsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multipleGroupingSets}
	 * labeled alternative in {@link TrinoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitMultipleGroupingSets(TrinoSqlBaseParser.MultipleGroupingSetsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void enterGroupingSet(TrinoSqlBaseParser.GroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void exitGroupingSet(TrinoSqlBaseParser.GroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void enterNamedQuery(TrinoSqlBaseParser.NamedQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void exitNamedQuery(TrinoSqlBaseParser.NamedQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterSetQuantifier(TrinoSqlBaseParser.SetQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitSetQuantifier(TrinoSqlBaseParser.SetQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectSingle}
	 * labeled alternative in {@link TrinoSqlBaseParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void enterSelectSingle(TrinoSqlBaseParser.SelectSingleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectSingle}
	 * labeled alternative in {@link TrinoSqlBaseParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void exitSelectSingle(TrinoSqlBaseParser.SelectSingleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectAll}
	 * labeled alternative in {@link TrinoSqlBaseParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void enterSelectAll(TrinoSqlBaseParser.SelectAllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectAll}
	 * labeled alternative in {@link TrinoSqlBaseParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void exitSelectAll(TrinoSqlBaseParser.SelectAllContext ctx);
	/**
	 * Enter a parse tree produced by the {@code relationDefault}
	 * labeled alternative in {@link TrinoSqlBaseParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterRelationDefault(TrinoSqlBaseParser.RelationDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code relationDefault}
	 * labeled alternative in {@link TrinoSqlBaseParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitRelationDefault(TrinoSqlBaseParser.RelationDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code joinRelation}
	 * labeled alternative in {@link TrinoSqlBaseParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterJoinRelation(TrinoSqlBaseParser.JoinRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code joinRelation}
	 * labeled alternative in {@link TrinoSqlBaseParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitJoinRelation(TrinoSqlBaseParser.JoinRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#joinType}.
	 * @param ctx the parse tree
	 */
	void enterJoinType(TrinoSqlBaseParser.JoinTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#joinType}.
	 * @param ctx the parse tree
	 */
	void exitJoinType(TrinoSqlBaseParser.JoinTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void enterJoinCriteria(TrinoSqlBaseParser.JoinCriteriaContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void exitJoinCriteria(TrinoSqlBaseParser.JoinCriteriaContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#sampledRelation}.
	 * @param ctx the parse tree
	 */
	void enterSampledRelation(TrinoSqlBaseParser.SampledRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#sampledRelation}.
	 * @param ctx the parse tree
	 */
	void exitSampledRelation(TrinoSqlBaseParser.SampledRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#sampleType}.
	 * @param ctx the parse tree
	 */
	void enterSampleType(TrinoSqlBaseParser.SampleTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#sampleType}.
	 * @param ctx the parse tree
	 */
	void exitSampleType(TrinoSqlBaseParser.SampleTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#aliasedRelation}.
	 * @param ctx the parse tree
	 */
	void enterAliasedRelation(TrinoSqlBaseParser.AliasedRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#aliasedRelation}.
	 * @param ctx the parse tree
	 */
	void exitAliasedRelation(TrinoSqlBaseParser.AliasedRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#columnAliases}.
	 * @param ctx the parse tree
	 */
	void enterColumnAliases(TrinoSqlBaseParser.ColumnAliasesContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#columnAliases}.
	 * @param ctx the parse tree
	 */
	void exitColumnAliases(TrinoSqlBaseParser.ColumnAliasesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link TrinoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableName(TrinoSqlBaseParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link TrinoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableName(TrinoSqlBaseParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryRelation}
	 * labeled alternative in {@link TrinoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryRelation(TrinoSqlBaseParser.SubqueryRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryRelation}
	 * labeled alternative in {@link TrinoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryRelation(TrinoSqlBaseParser.SubqueryRelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unnest}
	 * labeled alternative in {@link TrinoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterUnnest(TrinoSqlBaseParser.UnnestContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unnest}
	 * labeled alternative in {@link TrinoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitUnnest(TrinoSqlBaseParser.UnnestContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lateral}
	 * labeled alternative in {@link TrinoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterLateral(TrinoSqlBaseParser.LateralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lateral}
	 * labeled alternative in {@link TrinoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitLateral(TrinoSqlBaseParser.LateralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedRelation}
	 * labeled alternative in {@link TrinoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedRelation(TrinoSqlBaseParser.ParenthesizedRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedRelation}
	 * labeled alternative in {@link TrinoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedRelation(TrinoSqlBaseParser.ParenthesizedRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(TrinoSqlBaseParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(TrinoSqlBaseParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link TrinoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalNot(TrinoSqlBaseParser.LogicalNotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link TrinoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalNot(TrinoSqlBaseParser.LogicalNotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link TrinoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterPredicated(TrinoSqlBaseParser.PredicatedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link TrinoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitPredicated(TrinoSqlBaseParser.PredicatedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link TrinoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalBinary(TrinoSqlBaseParser.LogicalBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link TrinoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalBinary(TrinoSqlBaseParser.LogicalBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterComparison(TrinoSqlBaseParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitComparison(TrinoSqlBaseParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quantifiedComparison}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterQuantifiedComparison(TrinoSqlBaseParser.QuantifiedComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quantifiedComparison}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitQuantifiedComparison(TrinoSqlBaseParser.QuantifiedComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code between}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterBetween(TrinoSqlBaseParser.BetweenContext ctx);
	/**
	 * Exit a parse tree produced by the {@code between}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitBetween(TrinoSqlBaseParser.BetweenContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inList}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterInList(TrinoSqlBaseParser.InListContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inList}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitInList(TrinoSqlBaseParser.InListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inSubquery}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterInSubquery(TrinoSqlBaseParser.InSubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inSubquery}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitInSubquery(TrinoSqlBaseParser.InSubqueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code like}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterLike(TrinoSqlBaseParser.LikeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code like}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitLike(TrinoSqlBaseParser.LikeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullPredicate}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterNullPredicate(TrinoSqlBaseParser.NullPredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullPredicate}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitNullPredicate(TrinoSqlBaseParser.NullPredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code distinctFrom}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterDistinctFrom(TrinoSqlBaseParser.DistinctFromContext ctx);
	/**
	 * Exit a parse tree produced by the {@code distinctFrom}
	 * labeled alternative in {@link TrinoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitDistinctFrom(TrinoSqlBaseParser.DistinctFromContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link TrinoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterValueExpressionDefault(TrinoSqlBaseParser.ValueExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link TrinoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitValueExpressionDefault(TrinoSqlBaseParser.ValueExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code concatenation}
	 * labeled alternative in {@link TrinoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterConcatenation(TrinoSqlBaseParser.ConcatenationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code concatenation}
	 * labeled alternative in {@link TrinoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitConcatenation(TrinoSqlBaseParser.ConcatenationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link TrinoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticBinary(TrinoSqlBaseParser.ArithmeticBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link TrinoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticBinary(TrinoSqlBaseParser.ArithmeticBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link TrinoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticUnary(TrinoSqlBaseParser.ArithmeticUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link TrinoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticUnary(TrinoSqlBaseParser.ArithmeticUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code atTimeZone}
	 * labeled alternative in {@link TrinoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterAtTimeZone(TrinoSqlBaseParser.AtTimeZoneContext ctx);
	/**
	 * Exit a parse tree produced by the {@code atTimeZone}
	 * labeled alternative in {@link TrinoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitAtTimeZone(TrinoSqlBaseParser.AtTimeZoneContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterDereference(TrinoSqlBaseParser.DereferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitDereference(TrinoSqlBaseParser.DereferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTypeConstructor(TrinoSqlBaseParser.TypeConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTypeConstructor(TrinoSqlBaseParser.TypeConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code specialDateTimeFunction}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSpecialDateTimeFunction(TrinoSqlBaseParser.SpecialDateTimeFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code specialDateTimeFunction}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSpecialDateTimeFunction(TrinoSqlBaseParser.SpecialDateTimeFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code substring}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubstring(TrinoSqlBaseParser.SubstringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code substring}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubstring(TrinoSqlBaseParser.SubstringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cast}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCast(TrinoSqlBaseParser.CastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCast(TrinoSqlBaseParser.CastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambda(TrinoSqlBaseParser.LambdaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambda(TrinoSqlBaseParser.LambdaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(TrinoSqlBaseParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(TrinoSqlBaseParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parameter}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParameter(TrinoSqlBaseParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parameter}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParameter(TrinoSqlBaseParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code normalize}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterNormalize(TrinoSqlBaseParser.NormalizeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code normalize}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitNormalize(TrinoSqlBaseParser.NormalizeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterIntervalLiteral(TrinoSqlBaseParser.IntervalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitIntervalLiteral(TrinoSqlBaseParser.IntervalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(TrinoSqlBaseParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(TrinoSqlBaseParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(TrinoSqlBaseParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(TrinoSqlBaseParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCase(TrinoSqlBaseParser.SimpleCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCase(TrinoSqlBaseParser.SimpleCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterColumnReference(TrinoSqlBaseParser.ColumnReferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitColumnReference(TrinoSqlBaseParser.ColumnReferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterNullLiteral(TrinoSqlBaseParser.NullLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitNullLiteral(TrinoSqlBaseParser.NullLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterRowConstructor(TrinoSqlBaseParser.RowConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitRowConstructor(TrinoSqlBaseParser.RowConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(TrinoSqlBaseParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(TrinoSqlBaseParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryExpression(TrinoSqlBaseParser.SubqueryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryExpression(TrinoSqlBaseParser.SubqueryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterBinaryLiteral(TrinoSqlBaseParser.BinaryLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitBinaryLiteral(TrinoSqlBaseParser.BinaryLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentUser}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCurrentUser(TrinoSqlBaseParser.CurrentUserContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentUser}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCurrentUser(TrinoSqlBaseParser.CurrentUserContext ctx);
	/**
	 * Enter a parse tree produced by the {@code extract}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterExtract(TrinoSqlBaseParser.ExtractContext ctx);
	/**
	 * Exit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitExtract(TrinoSqlBaseParser.ExtractContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(TrinoSqlBaseParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(TrinoSqlBaseParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arrayConstructor}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterArrayConstructor(TrinoSqlBaseParser.ArrayConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arrayConstructor}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitArrayConstructor(TrinoSqlBaseParser.ArrayConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(TrinoSqlBaseParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(TrinoSqlBaseParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exists}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterExists(TrinoSqlBaseParser.ExistsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitExists(TrinoSqlBaseParser.ExistsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code position}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPosition(TrinoSqlBaseParser.PositionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code position}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPosition(TrinoSqlBaseParser.PositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSearchedCase(TrinoSqlBaseParser.SearchedCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSearchedCase(TrinoSqlBaseParser.SearchedCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code groupingOperation}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterGroupingOperation(TrinoSqlBaseParser.GroupingOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code groupingOperation}
	 * labeled alternative in {@link TrinoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitGroupingOperation(TrinoSqlBaseParser.GroupingOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code basicStringLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#string}.
	 * @param ctx the parse tree
	 */
	void enterBasicStringLiteral(TrinoSqlBaseParser.BasicStringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code basicStringLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#string}.
	 * @param ctx the parse tree
	 */
	void exitBasicStringLiteral(TrinoSqlBaseParser.BasicStringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unicodeStringLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#string}.
	 * @param ctx the parse tree
	 */
	void enterUnicodeStringLiteral(TrinoSqlBaseParser.UnicodeStringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unicodeStringLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#string}.
	 * @param ctx the parse tree
	 */
	void exitUnicodeStringLiteral(TrinoSqlBaseParser.UnicodeStringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#nullTreatment}.
	 * @param ctx the parse tree
	 */
	void enterNullTreatment(TrinoSqlBaseParser.NullTreatmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#nullTreatment}.
	 * @param ctx the parse tree
	 */
	void exitNullTreatment(TrinoSqlBaseParser.NullTreatmentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timeZoneInterval}
	 * labeled alternative in {@link TrinoSqlBaseParser#timeZoneSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterTimeZoneInterval(TrinoSqlBaseParser.TimeZoneIntervalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timeZoneInterval}
	 * labeled alternative in {@link TrinoSqlBaseParser#timeZoneSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitTimeZoneInterval(TrinoSqlBaseParser.TimeZoneIntervalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timeZoneString}
	 * labeled alternative in {@link TrinoSqlBaseParser#timeZoneSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterTimeZoneString(TrinoSqlBaseParser.TimeZoneStringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timeZoneString}
	 * labeled alternative in {@link TrinoSqlBaseParser#timeZoneSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitTimeZoneString(TrinoSqlBaseParser.TimeZoneStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(TrinoSqlBaseParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(TrinoSqlBaseParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#comparisonQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterComparisonQuantifier(TrinoSqlBaseParser.ComparisonQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#comparisonQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitComparisonQuantifier(TrinoSqlBaseParser.ComparisonQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(TrinoSqlBaseParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(TrinoSqlBaseParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#interval}.
	 * @param ctx the parse tree
	 */
	void enterInterval(TrinoSqlBaseParser.IntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#interval}.
	 * @param ctx the parse tree
	 */
	void exitInterval(TrinoSqlBaseParser.IntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#intervalField}.
	 * @param ctx the parse tree
	 */
	void enterIntervalField(TrinoSqlBaseParser.IntervalFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#intervalField}.
	 * @param ctx the parse tree
	 */
	void exitIntervalField(TrinoSqlBaseParser.IntervalFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#normalForm}.
	 * @param ctx the parse tree
	 */
	void enterNormalForm(TrinoSqlBaseParser.NormalFormContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#normalForm}.
	 * @param ctx the parse tree
	 */
	void exitNormalForm(TrinoSqlBaseParser.NormalFormContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#types}.
	 * @param ctx the parse tree
	 */
	void enterTypes(TrinoSqlBaseParser.TypesContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#types}.
	 * @param ctx the parse tree
	 */
	void exitTypes(TrinoSqlBaseParser.TypesContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(TrinoSqlBaseParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(TrinoSqlBaseParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(TrinoSqlBaseParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(TrinoSqlBaseParser.TypeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#baseType}.
	 * @param ctx the parse tree
	 */
	void enterBaseType(TrinoSqlBaseParser.BaseTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#baseType}.
	 * @param ctx the parse tree
	 */
	void exitBaseType(TrinoSqlBaseParser.BaseTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(TrinoSqlBaseParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(TrinoSqlBaseParser.WhenClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterFilter(TrinoSqlBaseParser.FilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitFilter(TrinoSqlBaseParser.FilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#over}.
	 * @param ctx the parse tree
	 */
	void enterOver(TrinoSqlBaseParser.OverContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#over}.
	 * @param ctx the parse tree
	 */
	void exitOver(TrinoSqlBaseParser.OverContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void enterWindowFrame(TrinoSqlBaseParser.WindowFrameContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void exitWindowFrame(TrinoSqlBaseParser.WindowFrameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unboundedFrame}
	 * labeled alternative in {@link TrinoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterUnboundedFrame(TrinoSqlBaseParser.UnboundedFrameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unboundedFrame}
	 * labeled alternative in {@link TrinoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitUnboundedFrame(TrinoSqlBaseParser.UnboundedFrameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentRowBound}
	 * labeled alternative in {@link TrinoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterCurrentRowBound(TrinoSqlBaseParser.CurrentRowBoundContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentRowBound}
	 * labeled alternative in {@link TrinoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitCurrentRowBound(TrinoSqlBaseParser.CurrentRowBoundContext ctx);
	/**
	 * Enter a parse tree produced by the {@code boundedFrame}
	 * labeled alternative in {@link TrinoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterBoundedFrame(TrinoSqlBaseParser.BoundedFrameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code boundedFrame}
	 * labeled alternative in {@link TrinoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitBoundedFrame(TrinoSqlBaseParser.BoundedFrameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code explainFormat}
	 * labeled alternative in {@link TrinoSqlBaseParser#explainOption}.
	 * @param ctx the parse tree
	 */
	void enterExplainFormat(TrinoSqlBaseParser.ExplainFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code explainFormat}
	 * labeled alternative in {@link TrinoSqlBaseParser#explainOption}.
	 * @param ctx the parse tree
	 */
	void exitExplainFormat(TrinoSqlBaseParser.ExplainFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code explainType}
	 * labeled alternative in {@link TrinoSqlBaseParser#explainOption}.
	 * @param ctx the parse tree
	 */
	void enterExplainType(TrinoSqlBaseParser.ExplainTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code explainType}
	 * labeled alternative in {@link TrinoSqlBaseParser#explainOption}.
	 * @param ctx the parse tree
	 */
	void exitExplainType(TrinoSqlBaseParser.ExplainTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isolationLevel}
	 * labeled alternative in {@link TrinoSqlBaseParser#transactionMode}.
	 * @param ctx the parse tree
	 */
	void enterIsolationLevel(TrinoSqlBaseParser.IsolationLevelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isolationLevel}
	 * labeled alternative in {@link TrinoSqlBaseParser#transactionMode}.
	 * @param ctx the parse tree
	 */
	void exitIsolationLevel(TrinoSqlBaseParser.IsolationLevelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code transactionAccessMode}
	 * labeled alternative in {@link TrinoSqlBaseParser#transactionMode}.
	 * @param ctx the parse tree
	 */
	void enterTransactionAccessMode(TrinoSqlBaseParser.TransactionAccessModeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code transactionAccessMode}
	 * labeled alternative in {@link TrinoSqlBaseParser#transactionMode}.
	 * @param ctx the parse tree
	 */
	void exitTransactionAccessMode(TrinoSqlBaseParser.TransactionAccessModeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code readUncommitted}
	 * labeled alternative in {@link TrinoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void enterReadUncommitted(TrinoSqlBaseParser.ReadUncommittedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code readUncommitted}
	 * labeled alternative in {@link TrinoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void exitReadUncommitted(TrinoSqlBaseParser.ReadUncommittedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code readCommitted}
	 * labeled alternative in {@link TrinoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void enterReadCommitted(TrinoSqlBaseParser.ReadCommittedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code readCommitted}
	 * labeled alternative in {@link TrinoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void exitReadCommitted(TrinoSqlBaseParser.ReadCommittedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code repeatableRead}
	 * labeled alternative in {@link TrinoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void enterRepeatableRead(TrinoSqlBaseParser.RepeatableReadContext ctx);
	/**
	 * Exit a parse tree produced by the {@code repeatableRead}
	 * labeled alternative in {@link TrinoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void exitRepeatableRead(TrinoSqlBaseParser.RepeatableReadContext ctx);
	/**
	 * Enter a parse tree produced by the {@code serializable}
	 * labeled alternative in {@link TrinoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void enterSerializable(TrinoSqlBaseParser.SerializableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code serializable}
	 * labeled alternative in {@link TrinoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void exitSerializable(TrinoSqlBaseParser.SerializableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code positionalArgument}
	 * labeled alternative in {@link TrinoSqlBaseParser#callArgument}.
	 * @param ctx the parse tree
	 */
	void enterPositionalArgument(TrinoSqlBaseParser.PositionalArgumentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code positionalArgument}
	 * labeled alternative in {@link TrinoSqlBaseParser#callArgument}.
	 * @param ctx the parse tree
	 */
	void exitPositionalArgument(TrinoSqlBaseParser.PositionalArgumentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code namedArgument}
	 * labeled alternative in {@link TrinoSqlBaseParser#callArgument}.
	 * @param ctx the parse tree
	 */
	void enterNamedArgument(TrinoSqlBaseParser.NamedArgumentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code namedArgument}
	 * labeled alternative in {@link TrinoSqlBaseParser#callArgument}.
	 * @param ctx the parse tree
	 */
	void exitNamedArgument(TrinoSqlBaseParser.NamedArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#privilege}.
	 * @param ctx the parse tree
	 */
	void enterPrivilege(TrinoSqlBaseParser.PrivilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#privilege}.
	 * @param ctx the parse tree
	 */
	void exitPrivilege(TrinoSqlBaseParser.PrivilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(TrinoSqlBaseParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(TrinoSqlBaseParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentUserGrantor}
	 * labeled alternative in {@link TrinoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void enterCurrentUserGrantor(TrinoSqlBaseParser.CurrentUserGrantorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentUserGrantor}
	 * labeled alternative in {@link TrinoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void exitCurrentUserGrantor(TrinoSqlBaseParser.CurrentUserGrantorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentRoleGrantor}
	 * labeled alternative in {@link TrinoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void enterCurrentRoleGrantor(TrinoSqlBaseParser.CurrentRoleGrantorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentRoleGrantor}
	 * labeled alternative in {@link TrinoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void exitCurrentRoleGrantor(TrinoSqlBaseParser.CurrentRoleGrantorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code specifiedPrincipal}
	 * labeled alternative in {@link TrinoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void enterSpecifiedPrincipal(TrinoSqlBaseParser.SpecifiedPrincipalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code specifiedPrincipal}
	 * labeled alternative in {@link TrinoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void exitSpecifiedPrincipal(TrinoSqlBaseParser.SpecifiedPrincipalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code userPrincipal}
	 * labeled alternative in {@link TrinoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void enterUserPrincipal(TrinoSqlBaseParser.UserPrincipalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code userPrincipal}
	 * labeled alternative in {@link TrinoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void exitUserPrincipal(TrinoSqlBaseParser.UserPrincipalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rolePrincipal}
	 * labeled alternative in {@link TrinoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void enterRolePrincipal(TrinoSqlBaseParser.RolePrincipalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rolePrincipal}
	 * labeled alternative in {@link TrinoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void exitRolePrincipal(TrinoSqlBaseParser.RolePrincipalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unspecifiedPrincipal}
	 * labeled alternative in {@link TrinoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void enterUnspecifiedPrincipal(TrinoSqlBaseParser.UnspecifiedPrincipalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unspecifiedPrincipal}
	 * labeled alternative in {@link TrinoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void exitUnspecifiedPrincipal(TrinoSqlBaseParser.UnspecifiedPrincipalContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#roles}.
	 * @param ctx the parse tree
	 */
	void enterRoles(TrinoSqlBaseParser.RolesContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#roles}.
	 * @param ctx the parse tree
	 */
	void exitRoles(TrinoSqlBaseParser.RolesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link TrinoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedIdentifier(TrinoSqlBaseParser.UnquotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link TrinoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedIdentifier(TrinoSqlBaseParser.UnquotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quotedIdentifier}
	 * labeled alternative in {@link TrinoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifier(TrinoSqlBaseParser.QuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quotedIdentifier}
	 * labeled alternative in {@link TrinoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifier(TrinoSqlBaseParser.QuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code backQuotedIdentifier}
	 * labeled alternative in {@link TrinoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterBackQuotedIdentifier(TrinoSqlBaseParser.BackQuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code backQuotedIdentifier}
	 * labeled alternative in {@link TrinoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitBackQuotedIdentifier(TrinoSqlBaseParser.BackQuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code digitIdentifier}
	 * labeled alternative in {@link TrinoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterDigitIdentifier(TrinoSqlBaseParser.DigitIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code digitIdentifier}
	 * labeled alternative in {@link TrinoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitDigitIdentifier(TrinoSqlBaseParser.DigitIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDecimalLiteral(TrinoSqlBaseParser.DecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDecimalLiteral(TrinoSqlBaseParser.DecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(TrinoSqlBaseParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(TrinoSqlBaseParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(TrinoSqlBaseParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link TrinoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(TrinoSqlBaseParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link TrinoSqlBaseParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void enterNonReserved(TrinoSqlBaseParser.NonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link TrinoSqlBaseParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void exitNonReserved(TrinoSqlBaseParser.NonReservedContext ctx);
}