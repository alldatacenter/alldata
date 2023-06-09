// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.presto.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PrestoSqlBaseParser}.
 */
public interface PrestoSqlBaseListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void enterSingleStatement(PrestoSqlBaseParser.SingleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void exitSingleStatement(PrestoSqlBaseParser.SingleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#standaloneExpression}.
	 * @param ctx the parse tree
	 */
	void enterStandaloneExpression(PrestoSqlBaseParser.StandaloneExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#standaloneExpression}.
	 * @param ctx the parse tree
	 */
	void exitStandaloneExpression(PrestoSqlBaseParser.StandaloneExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#standaloneRoutineBody}.
	 * @param ctx the parse tree
	 */
	void enterStandaloneRoutineBody(PrestoSqlBaseParser.StandaloneRoutineBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#standaloneRoutineBody}.
	 * @param ctx the parse tree
	 */
	void exitStandaloneRoutineBody(PrestoSqlBaseParser.StandaloneRoutineBodyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatementDefault(PrestoSqlBaseParser.StatementDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code statementDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatementDefault(PrestoSqlBaseParser.StatementDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code use}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterUse(PrestoSqlBaseParser.UseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code use}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitUse(PrestoSqlBaseParser.UseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createSchema}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateSchema(PrestoSqlBaseParser.CreateSchemaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createSchema}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateSchema(PrestoSqlBaseParser.CreateSchemaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropSchema}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropSchema(PrestoSqlBaseParser.DropSchemaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropSchema}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropSchema(PrestoSqlBaseParser.DropSchemaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameSchema}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameSchema(PrestoSqlBaseParser.RenameSchemaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameSchema}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameSchema(PrestoSqlBaseParser.RenameSchemaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTableAsSelect}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableAsSelect(PrestoSqlBaseParser.CreateTableAsSelectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTableAsSelect}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableAsSelect(PrestoSqlBaseParser.CreateTableAsSelectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTable(PrestoSqlBaseParser.CreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTable(PrestoSqlBaseParser.CreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropTable(PrestoSqlBaseParser.DropTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropTable(PrestoSqlBaseParser.DropTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code insertInto}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterInsertInto(PrestoSqlBaseParser.InsertIntoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code insertInto}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitInsertInto(PrestoSqlBaseParser.InsertIntoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code delete}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDelete(PrestoSqlBaseParser.DeleteContext ctx);
	/**
	 * Exit a parse tree produced by the {@code delete}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDelete(PrestoSqlBaseParser.DeleteContext ctx);
	/**
	 * Enter a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterTruncateTable(PrestoSqlBaseParser.TruncateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code truncateTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitTruncateTable(PrestoSqlBaseParser.TruncateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameTable(PrestoSqlBaseParser.RenameTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameTable(PrestoSqlBaseParser.RenameTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code renameColumn}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRenameColumn(PrestoSqlBaseParser.RenameColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code renameColumn}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRenameColumn(PrestoSqlBaseParser.RenameColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropColumn}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropColumn(PrestoSqlBaseParser.DropColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropColumn}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropColumn(PrestoSqlBaseParser.DropColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addColumn}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAddColumn(PrestoSqlBaseParser.AddColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addColumn}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAddColumn(PrestoSqlBaseParser.AddColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze(PrestoSqlBaseParser.AnalyzeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code analyze}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze(PrestoSqlBaseParser.AnalyzeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createType}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateType(PrestoSqlBaseParser.CreateTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createType}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateType(PrestoSqlBaseParser.CreateTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateView(PrestoSqlBaseParser.CreateViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateView(PrestoSqlBaseParser.CreateViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropView(PrestoSqlBaseParser.DropViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropView(PrestoSqlBaseParser.DropViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateMaterializedView(PrestoSqlBaseParser.CreateMaterializedViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateMaterializedView(PrestoSqlBaseParser.CreateMaterializedViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropMaterializedView(PrestoSqlBaseParser.DropMaterializedViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropMaterializedView(PrestoSqlBaseParser.DropMaterializedViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code refreshMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshMaterializedView(PrestoSqlBaseParser.RefreshMaterializedViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code refreshMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshMaterializedView(PrestoSqlBaseParser.RefreshMaterializedViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateFunction(PrestoSqlBaseParser.CreateFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateFunction(PrestoSqlBaseParser.CreateFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAlterFunction(PrestoSqlBaseParser.AlterFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAlterFunction(PrestoSqlBaseParser.AlterFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropFunction(PrestoSqlBaseParser.DropFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropFunction(PrestoSqlBaseParser.DropFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code call}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCall(PrestoSqlBaseParser.CallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code call}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCall(PrestoSqlBaseParser.CallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createRole}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateRole(PrestoSqlBaseParser.CreateRoleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createRole}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateRole(PrestoSqlBaseParser.CreateRoleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dropRole}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDropRole(PrestoSqlBaseParser.DropRoleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dropRole}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDropRole(PrestoSqlBaseParser.DropRoleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grantRoles}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterGrantRoles(PrestoSqlBaseParser.GrantRolesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grantRoles}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitGrantRoles(PrestoSqlBaseParser.GrantRolesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revokeRoles}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRevokeRoles(PrestoSqlBaseParser.RevokeRolesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revokeRoles}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRevokeRoles(PrestoSqlBaseParser.RevokeRolesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setRole}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetRole(PrestoSqlBaseParser.SetRoleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setRole}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetRole(PrestoSqlBaseParser.SetRoleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grant}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterGrant(PrestoSqlBaseParser.GrantContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grant}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitGrant(PrestoSqlBaseParser.GrantContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revoke}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRevoke(PrestoSqlBaseParser.RevokeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revoke}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRevoke(PrestoSqlBaseParser.RevokeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showGrants}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowGrants(PrestoSqlBaseParser.ShowGrantsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showGrants}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowGrants(PrestoSqlBaseParser.ShowGrantsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code explain}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterExplain(PrestoSqlBaseParser.ExplainContext ctx);
	/**
	 * Exit a parse tree produced by the {@code explain}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitExplain(PrestoSqlBaseParser.ExplainContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateTable(PrestoSqlBaseParser.ShowCreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateTable(PrestoSqlBaseParser.ShowCreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateView(PrestoSqlBaseParser.ShowCreateViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateView(PrestoSqlBaseParser.ShowCreateViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateMaterializedView(PrestoSqlBaseParser.ShowCreateMaterializedViewContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateMaterializedView}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateMaterializedView(PrestoSqlBaseParser.ShowCreateMaterializedViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateFunction(PrestoSqlBaseParser.ShowCreateFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateFunction(PrestoSqlBaseParser.ShowCreateFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowTables(PrestoSqlBaseParser.ShowTablesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showTables}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowTables(PrestoSqlBaseParser.ShowTablesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showSchemas}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowSchemas(PrestoSqlBaseParser.ShowSchemasContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showSchemas}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowSchemas(PrestoSqlBaseParser.ShowSchemasContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCatalogs}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowCatalogs(PrestoSqlBaseParser.ShowCatalogsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCatalogs}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowCatalogs(PrestoSqlBaseParser.ShowCatalogsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowColumns(PrestoSqlBaseParser.ShowColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowColumns(PrestoSqlBaseParser.ShowColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showStats}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowStats(PrestoSqlBaseParser.ShowStatsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showStats}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowStats(PrestoSqlBaseParser.ShowStatsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showStatsForQuery}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowStatsForQuery(PrestoSqlBaseParser.ShowStatsForQueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showStatsForQuery}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowStatsForQuery(PrestoSqlBaseParser.ShowStatsForQueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showRoles}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowRoles(PrestoSqlBaseParser.ShowRolesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showRoles}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowRoles(PrestoSqlBaseParser.ShowRolesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showRoleGrants}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowRoleGrants(PrestoSqlBaseParser.ShowRoleGrantsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showRoleGrants}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowRoleGrants(PrestoSqlBaseParser.ShowRoleGrantsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowFunctions(PrestoSqlBaseParser.ShowFunctionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showFunctions}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowFunctions(PrestoSqlBaseParser.ShowFunctionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showSession}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterShowSession(PrestoSqlBaseParser.ShowSessionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showSession}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitShowSession(PrestoSqlBaseParser.ShowSessionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setSession}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSetSession(PrestoSqlBaseParser.SetSessionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setSession}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSetSession(PrestoSqlBaseParser.SetSessionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resetSession}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterResetSession(PrestoSqlBaseParser.ResetSessionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resetSession}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitResetSession(PrestoSqlBaseParser.ResetSessionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code startTransaction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStartTransaction(PrestoSqlBaseParser.StartTransactionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code startTransaction}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStartTransaction(PrestoSqlBaseParser.StartTransactionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code commit}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCommit(PrestoSqlBaseParser.CommitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code commit}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCommit(PrestoSqlBaseParser.CommitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rollback}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterRollback(PrestoSqlBaseParser.RollbackContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rollback}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitRollback(PrestoSqlBaseParser.RollbackContext ctx);
	/**
	 * Enter a parse tree produced by the {@code prepare}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterPrepare(PrestoSqlBaseParser.PrepareContext ctx);
	/**
	 * Exit a parse tree produced by the {@code prepare}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitPrepare(PrestoSqlBaseParser.PrepareContext ctx);
	/**
	 * Enter a parse tree produced by the {@code deallocate}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDeallocate(PrestoSqlBaseParser.DeallocateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code deallocate}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDeallocate(PrestoSqlBaseParser.DeallocateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code execute}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterExecute(PrestoSqlBaseParser.ExecuteContext ctx);
	/**
	 * Exit a parse tree produced by the {@code execute}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitExecute(PrestoSqlBaseParser.ExecuteContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeInput}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeInput(PrestoSqlBaseParser.DescribeInputContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeInput}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeInput(PrestoSqlBaseParser.DescribeInputContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeOutput}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDescribeOutput(PrestoSqlBaseParser.DescribeOutputContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeOutput}
	 * labeled alternative in {@link PrestoSqlBaseParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDescribeOutput(PrestoSqlBaseParser.DescribeOutputContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(PrestoSqlBaseParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(PrestoSqlBaseParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#with}.
	 * @param ctx the parse tree
	 */
	void enterWith(PrestoSqlBaseParser.WithContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#with}.
	 * @param ctx the parse tree
	 */
	void exitWith(PrestoSqlBaseParser.WithContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#tableElement}.
	 * @param ctx the parse tree
	 */
	void enterTableElement(PrestoSqlBaseParser.TableElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#tableElement}.
	 * @param ctx the parse tree
	 */
	void exitTableElement(PrestoSqlBaseParser.TableElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#columnDefinition}.
	 * @param ctx the parse tree
	 */
	void enterColumnDefinition(PrestoSqlBaseParser.ColumnDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#columnDefinition}.
	 * @param ctx the parse tree
	 */
	void exitColumnDefinition(PrestoSqlBaseParser.ColumnDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#likeClause}.
	 * @param ctx the parse tree
	 */
	void enterLikeClause(PrestoSqlBaseParser.LikeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#likeClause}.
	 * @param ctx the parse tree
	 */
	void exitLikeClause(PrestoSqlBaseParser.LikeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#properties}.
	 * @param ctx the parse tree
	 */
	void enterProperties(PrestoSqlBaseParser.PropertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#properties}.
	 * @param ctx the parse tree
	 */
	void exitProperties(PrestoSqlBaseParser.PropertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#property}.
	 * @param ctx the parse tree
	 */
	void enterProperty(PrestoSqlBaseParser.PropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#property}.
	 * @param ctx the parse tree
	 */
	void exitProperty(PrestoSqlBaseParser.PropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#sqlParameterDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterSqlParameterDeclaration(PrestoSqlBaseParser.SqlParameterDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#sqlParameterDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitSqlParameterDeclaration(PrestoSqlBaseParser.SqlParameterDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#routineCharacteristics}.
	 * @param ctx the parse tree
	 */
	void enterRoutineCharacteristics(PrestoSqlBaseParser.RoutineCharacteristicsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#routineCharacteristics}.
	 * @param ctx the parse tree
	 */
	void exitRoutineCharacteristics(PrestoSqlBaseParser.RoutineCharacteristicsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#routineCharacteristic}.
	 * @param ctx the parse tree
	 */
	void enterRoutineCharacteristic(PrestoSqlBaseParser.RoutineCharacteristicContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#routineCharacteristic}.
	 * @param ctx the parse tree
	 */
	void exitRoutineCharacteristic(PrestoSqlBaseParser.RoutineCharacteristicContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#alterRoutineCharacteristics}.
	 * @param ctx the parse tree
	 */
	void enterAlterRoutineCharacteristics(PrestoSqlBaseParser.AlterRoutineCharacteristicsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#alterRoutineCharacteristics}.
	 * @param ctx the parse tree
	 */
	void exitAlterRoutineCharacteristics(PrestoSqlBaseParser.AlterRoutineCharacteristicsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#alterRoutineCharacteristic}.
	 * @param ctx the parse tree
	 */
	void enterAlterRoutineCharacteristic(PrestoSqlBaseParser.AlterRoutineCharacteristicContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#alterRoutineCharacteristic}.
	 * @param ctx the parse tree
	 */
	void exitAlterRoutineCharacteristic(PrestoSqlBaseParser.AlterRoutineCharacteristicContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void enterRoutineBody(PrestoSqlBaseParser.RoutineBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void exitRoutineBody(PrestoSqlBaseParser.RoutineBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(PrestoSqlBaseParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(PrestoSqlBaseParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#externalBodyReference}.
	 * @param ctx the parse tree
	 */
	void enterExternalBodyReference(PrestoSqlBaseParser.ExternalBodyReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#externalBodyReference}.
	 * @param ctx the parse tree
	 */
	void exitExternalBodyReference(PrestoSqlBaseParser.ExternalBodyReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#language}.
	 * @param ctx the parse tree
	 */
	void enterLanguage(PrestoSqlBaseParser.LanguageContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#language}.
	 * @param ctx the parse tree
	 */
	void exitLanguage(PrestoSqlBaseParser.LanguageContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#determinism}.
	 * @param ctx the parse tree
	 */
	void enterDeterminism(PrestoSqlBaseParser.DeterminismContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#determinism}.
	 * @param ctx the parse tree
	 */
	void exitDeterminism(PrestoSqlBaseParser.DeterminismContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#nullCallClause}.
	 * @param ctx the parse tree
	 */
	void enterNullCallClause(PrestoSqlBaseParser.NullCallClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#nullCallClause}.
	 * @param ctx the parse tree
	 */
	void exitNullCallClause(PrestoSqlBaseParser.NullCallClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#externalRoutineName}.
	 * @param ctx the parse tree
	 */
	void enterExternalRoutineName(PrestoSqlBaseParser.ExternalRoutineNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#externalRoutineName}.
	 * @param ctx the parse tree
	 */
	void exitExternalRoutineName(PrestoSqlBaseParser.ExternalRoutineNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#queryNoWith}.
	 * @param ctx the parse tree
	 */
	void enterQueryNoWith(PrestoSqlBaseParser.QueryNoWithContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#queryNoWith}.
	 * @param ctx the parse tree
	 */
	void exitQueryNoWith(PrestoSqlBaseParser.QueryNoWithContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterQueryTermDefault(PrestoSqlBaseParser.QueryTermDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryTermDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitQueryTermDefault(PrestoSqlBaseParser.QueryTermDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void enterSetOperation(PrestoSqlBaseParser.SetOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryTerm}.
	 * @param ctx the parse tree
	 */
	void exitSetOperation(PrestoSqlBaseParser.SetOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterQueryPrimaryDefault(PrestoSqlBaseParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitQueryPrimaryDefault(PrestoSqlBaseParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTable(PrestoSqlBaseParser.TableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTable(PrestoSqlBaseParser.TableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTable(PrestoSqlBaseParser.InlineTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTable}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTable(PrestoSqlBaseParser.InlineTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(PrestoSqlBaseParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subquery}
	 * labeled alternative in {@link PrestoSqlBaseParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(PrestoSqlBaseParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void enterSortItem(PrestoSqlBaseParser.SortItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void exitSortItem(PrestoSqlBaseParser.SortItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterQuerySpecification(PrestoSqlBaseParser.QuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitQuerySpecification(PrestoSqlBaseParser.QuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#groupBy}.
	 * @param ctx the parse tree
	 */
	void enterGroupBy(PrestoSqlBaseParser.GroupByContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#groupBy}.
	 * @param ctx the parse tree
	 */
	void exitGroupBy(PrestoSqlBaseParser.GroupByContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleGroupingSet}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterSingleGroupingSet(PrestoSqlBaseParser.SingleGroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleGroupingSet}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitSingleGroupingSet(PrestoSqlBaseParser.SingleGroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rollup}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterRollup(PrestoSqlBaseParser.RollupContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rollup}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitRollup(PrestoSqlBaseParser.RollupContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cube}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterCube(PrestoSqlBaseParser.CubeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cube}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitCube(PrestoSqlBaseParser.CubeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multipleGroupingSets}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterMultipleGroupingSets(PrestoSqlBaseParser.MultipleGroupingSetsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multipleGroupingSets}
	 * labeled alternative in {@link PrestoSqlBaseParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitMultipleGroupingSets(PrestoSqlBaseParser.MultipleGroupingSetsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void enterGroupingSet(PrestoSqlBaseParser.GroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void exitGroupingSet(PrestoSqlBaseParser.GroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void enterNamedQuery(PrestoSqlBaseParser.NamedQueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#namedQuery}.
	 * @param ctx the parse tree
	 */
	void exitNamedQuery(PrestoSqlBaseParser.NamedQueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterSetQuantifier(PrestoSqlBaseParser.SetQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitSetQuantifier(PrestoSqlBaseParser.SetQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectSingle}
	 * labeled alternative in {@link PrestoSqlBaseParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void enterSelectSingle(PrestoSqlBaseParser.SelectSingleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectSingle}
	 * labeled alternative in {@link PrestoSqlBaseParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void exitSelectSingle(PrestoSqlBaseParser.SelectSingleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectAll}
	 * labeled alternative in {@link PrestoSqlBaseParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void enterSelectAll(PrestoSqlBaseParser.SelectAllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectAll}
	 * labeled alternative in {@link PrestoSqlBaseParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void exitSelectAll(PrestoSqlBaseParser.SelectAllContext ctx);
	/**
	 * Enter a parse tree produced by the {@code relationDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterRelationDefault(PrestoSqlBaseParser.RelationDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code relationDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitRelationDefault(PrestoSqlBaseParser.RelationDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code joinRelation}
	 * labeled alternative in {@link PrestoSqlBaseParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterJoinRelation(PrestoSqlBaseParser.JoinRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code joinRelation}
	 * labeled alternative in {@link PrestoSqlBaseParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitJoinRelation(PrestoSqlBaseParser.JoinRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#joinType}.
	 * @param ctx the parse tree
	 */
	void enterJoinType(PrestoSqlBaseParser.JoinTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#joinType}.
	 * @param ctx the parse tree
	 */
	void exitJoinType(PrestoSqlBaseParser.JoinTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void enterJoinCriteria(PrestoSqlBaseParser.JoinCriteriaContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void exitJoinCriteria(PrestoSqlBaseParser.JoinCriteriaContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#sampledRelation}.
	 * @param ctx the parse tree
	 */
	void enterSampledRelation(PrestoSqlBaseParser.SampledRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#sampledRelation}.
	 * @param ctx the parse tree
	 */
	void exitSampledRelation(PrestoSqlBaseParser.SampledRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#sampleType}.
	 * @param ctx the parse tree
	 */
	void enterSampleType(PrestoSqlBaseParser.SampleTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#sampleType}.
	 * @param ctx the parse tree
	 */
	void exitSampleType(PrestoSqlBaseParser.SampleTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#aliasedRelation}.
	 * @param ctx the parse tree
	 */
	void enterAliasedRelation(PrestoSqlBaseParser.AliasedRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#aliasedRelation}.
	 * @param ctx the parse tree
	 */
	void exitAliasedRelation(PrestoSqlBaseParser.AliasedRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#columnAliases}.
	 * @param ctx the parse tree
	 */
	void enterColumnAliases(PrestoSqlBaseParser.ColumnAliasesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#columnAliases}.
	 * @param ctx the parse tree
	 */
	void exitColumnAliases(PrestoSqlBaseParser.ColumnAliasesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableName(PrestoSqlBaseParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableName}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableName(PrestoSqlBaseParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryRelation}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryRelation(PrestoSqlBaseParser.SubqueryRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryRelation}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryRelation(PrestoSqlBaseParser.SubqueryRelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unnest}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterUnnest(PrestoSqlBaseParser.UnnestContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unnest}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitUnnest(PrestoSqlBaseParser.UnnestContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lateral}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterLateral(PrestoSqlBaseParser.LateralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lateral}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitLateral(PrestoSqlBaseParser.LateralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedRelation}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedRelation(PrestoSqlBaseParser.ParenthesizedRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedRelation}
	 * labeled alternative in {@link PrestoSqlBaseParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedRelation(PrestoSqlBaseParser.ParenthesizedRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(PrestoSqlBaseParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(PrestoSqlBaseParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link PrestoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalNot(PrestoSqlBaseParser.LogicalNotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link PrestoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalNot(PrestoSqlBaseParser.LogicalNotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link PrestoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterPredicated(PrestoSqlBaseParser.PredicatedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link PrestoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitPredicated(PrestoSqlBaseParser.PredicatedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link PrestoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalBinary(PrestoSqlBaseParser.LogicalBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link PrestoSqlBaseParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalBinary(PrestoSqlBaseParser.LogicalBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterComparison(PrestoSqlBaseParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitComparison(PrestoSqlBaseParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quantifiedComparison}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterQuantifiedComparison(PrestoSqlBaseParser.QuantifiedComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quantifiedComparison}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitQuantifiedComparison(PrestoSqlBaseParser.QuantifiedComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code between}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterBetween(PrestoSqlBaseParser.BetweenContext ctx);
	/**
	 * Exit a parse tree produced by the {@code between}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitBetween(PrestoSqlBaseParser.BetweenContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inList}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterInList(PrestoSqlBaseParser.InListContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inList}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitInList(PrestoSqlBaseParser.InListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inSubquery}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterInSubquery(PrestoSqlBaseParser.InSubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inSubquery}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitInSubquery(PrestoSqlBaseParser.InSubqueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code like}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterLike(PrestoSqlBaseParser.LikeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code like}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitLike(PrestoSqlBaseParser.LikeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullPredicate}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterNullPredicate(PrestoSqlBaseParser.NullPredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullPredicate}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitNullPredicate(PrestoSqlBaseParser.NullPredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code distinctFrom}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterDistinctFrom(PrestoSqlBaseParser.DistinctFromContext ctx);
	/**
	 * Exit a parse tree produced by the {@code distinctFrom}
	 * labeled alternative in {@link PrestoSqlBaseParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitDistinctFrom(PrestoSqlBaseParser.DistinctFromContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterValueExpressionDefault(PrestoSqlBaseParser.ValueExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitValueExpressionDefault(PrestoSqlBaseParser.ValueExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code concatenation}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterConcatenation(PrestoSqlBaseParser.ConcatenationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code concatenation}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitConcatenation(PrestoSqlBaseParser.ConcatenationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticBinary(PrestoSqlBaseParser.ArithmeticBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticBinary(PrestoSqlBaseParser.ArithmeticBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticUnary(PrestoSqlBaseParser.ArithmeticUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticUnary(PrestoSqlBaseParser.ArithmeticUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code atTimeZone}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterAtTimeZone(PrestoSqlBaseParser.AtTimeZoneContext ctx);
	/**
	 * Exit a parse tree produced by the {@code atTimeZone}
	 * labeled alternative in {@link PrestoSqlBaseParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitAtTimeZone(PrestoSqlBaseParser.AtTimeZoneContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterDereference(PrestoSqlBaseParser.DereferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitDereference(PrestoSqlBaseParser.DereferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTypeConstructor(PrestoSqlBaseParser.TypeConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTypeConstructor(PrestoSqlBaseParser.TypeConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code specialDateTimeFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSpecialDateTimeFunction(PrestoSqlBaseParser.SpecialDateTimeFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code specialDateTimeFunction}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSpecialDateTimeFunction(PrestoSqlBaseParser.SpecialDateTimeFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code substring}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubstring(PrestoSqlBaseParser.SubstringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code substring}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubstring(PrestoSqlBaseParser.SubstringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cast}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCast(PrestoSqlBaseParser.CastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCast(PrestoSqlBaseParser.CastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambda(PrestoSqlBaseParser.LambdaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambda(PrestoSqlBaseParser.LambdaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(PrestoSqlBaseParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(PrestoSqlBaseParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parameter}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParameter(PrestoSqlBaseParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parameter}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParameter(PrestoSqlBaseParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code normalize}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterNormalize(PrestoSqlBaseParser.NormalizeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code normalize}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitNormalize(PrestoSqlBaseParser.NormalizeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterIntervalLiteral(PrestoSqlBaseParser.IntervalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitIntervalLiteral(PrestoSqlBaseParser.IntervalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(PrestoSqlBaseParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(PrestoSqlBaseParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(PrestoSqlBaseParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(PrestoSqlBaseParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCase(PrestoSqlBaseParser.SimpleCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCase(PrestoSqlBaseParser.SimpleCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterColumnReference(PrestoSqlBaseParser.ColumnReferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitColumnReference(PrestoSqlBaseParser.ColumnReferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterNullLiteral(PrestoSqlBaseParser.NullLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitNullLiteral(PrestoSqlBaseParser.NullLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterRowConstructor(PrestoSqlBaseParser.RowConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rowConstructor}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitRowConstructor(PrestoSqlBaseParser.RowConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(PrestoSqlBaseParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(PrestoSqlBaseParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryExpression(PrestoSqlBaseParser.SubqueryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryExpression(PrestoSqlBaseParser.SubqueryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterBinaryLiteral(PrestoSqlBaseParser.BinaryLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitBinaryLiteral(PrestoSqlBaseParser.BinaryLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentUser}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCurrentUser(PrestoSqlBaseParser.CurrentUserContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentUser}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCurrentUser(PrestoSqlBaseParser.CurrentUserContext ctx);
	/**
	 * Enter a parse tree produced by the {@code extract}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterExtract(PrestoSqlBaseParser.ExtractContext ctx);
	/**
	 * Exit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitExtract(PrestoSqlBaseParser.ExtractContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(PrestoSqlBaseParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(PrestoSqlBaseParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arrayConstructor}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterArrayConstructor(PrestoSqlBaseParser.ArrayConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arrayConstructor}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitArrayConstructor(PrestoSqlBaseParser.ArrayConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(PrestoSqlBaseParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(PrestoSqlBaseParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exists}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterExists(PrestoSqlBaseParser.ExistsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitExists(PrestoSqlBaseParser.ExistsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code position}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPosition(PrestoSqlBaseParser.PositionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code position}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPosition(PrestoSqlBaseParser.PositionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSearchedCase(PrestoSqlBaseParser.SearchedCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSearchedCase(PrestoSqlBaseParser.SearchedCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code groupingOperation}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterGroupingOperation(PrestoSqlBaseParser.GroupingOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code groupingOperation}
	 * labeled alternative in {@link PrestoSqlBaseParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitGroupingOperation(PrestoSqlBaseParser.GroupingOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code basicStringLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#string}.
	 * @param ctx the parse tree
	 */
	void enterBasicStringLiteral(PrestoSqlBaseParser.BasicStringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code basicStringLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#string}.
	 * @param ctx the parse tree
	 */
	void exitBasicStringLiteral(PrestoSqlBaseParser.BasicStringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unicodeStringLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#string}.
	 * @param ctx the parse tree
	 */
	void enterUnicodeStringLiteral(PrestoSqlBaseParser.UnicodeStringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unicodeStringLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#string}.
	 * @param ctx the parse tree
	 */
	void exitUnicodeStringLiteral(PrestoSqlBaseParser.UnicodeStringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#nullTreatment}.
	 * @param ctx the parse tree
	 */
	void enterNullTreatment(PrestoSqlBaseParser.NullTreatmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#nullTreatment}.
	 * @param ctx the parse tree
	 */
	void exitNullTreatment(PrestoSqlBaseParser.NullTreatmentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timeZoneInterval}
	 * labeled alternative in {@link PrestoSqlBaseParser#timeZoneSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterTimeZoneInterval(PrestoSqlBaseParser.TimeZoneIntervalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timeZoneInterval}
	 * labeled alternative in {@link PrestoSqlBaseParser#timeZoneSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitTimeZoneInterval(PrestoSqlBaseParser.TimeZoneIntervalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timeZoneString}
	 * labeled alternative in {@link PrestoSqlBaseParser#timeZoneSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterTimeZoneString(PrestoSqlBaseParser.TimeZoneStringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timeZoneString}
	 * labeled alternative in {@link PrestoSqlBaseParser#timeZoneSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitTimeZoneString(PrestoSqlBaseParser.TimeZoneStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(PrestoSqlBaseParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(PrestoSqlBaseParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#comparisonQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterComparisonQuantifier(PrestoSqlBaseParser.ComparisonQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#comparisonQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitComparisonQuantifier(PrestoSqlBaseParser.ComparisonQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(PrestoSqlBaseParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(PrestoSqlBaseParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#interval}.
	 * @param ctx the parse tree
	 */
	void enterInterval(PrestoSqlBaseParser.IntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#interval}.
	 * @param ctx the parse tree
	 */
	void exitInterval(PrestoSqlBaseParser.IntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#intervalField}.
	 * @param ctx the parse tree
	 */
	void enterIntervalField(PrestoSqlBaseParser.IntervalFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#intervalField}.
	 * @param ctx the parse tree
	 */
	void exitIntervalField(PrestoSqlBaseParser.IntervalFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#normalForm}.
	 * @param ctx the parse tree
	 */
	void enterNormalForm(PrestoSqlBaseParser.NormalFormContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#normalForm}.
	 * @param ctx the parse tree
	 */
	void exitNormalForm(PrestoSqlBaseParser.NormalFormContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#types}.
	 * @param ctx the parse tree
	 */
	void enterTypes(PrestoSqlBaseParser.TypesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#types}.
	 * @param ctx the parse tree
	 */
	void exitTypes(PrestoSqlBaseParser.TypesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(PrestoSqlBaseParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(PrestoSqlBaseParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(PrestoSqlBaseParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(PrestoSqlBaseParser.TypeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#baseType}.
	 * @param ctx the parse tree
	 */
	void enterBaseType(PrestoSqlBaseParser.BaseTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#baseType}.
	 * @param ctx the parse tree
	 */
	void exitBaseType(PrestoSqlBaseParser.BaseTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(PrestoSqlBaseParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(PrestoSqlBaseParser.WhenClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterFilter(PrestoSqlBaseParser.FilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitFilter(PrestoSqlBaseParser.FilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#over}.
	 * @param ctx the parse tree
	 */
	void enterOver(PrestoSqlBaseParser.OverContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#over}.
	 * @param ctx the parse tree
	 */
	void exitOver(PrestoSqlBaseParser.OverContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void enterWindowFrame(PrestoSqlBaseParser.WindowFrameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void exitWindowFrame(PrestoSqlBaseParser.WindowFrameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unboundedFrame}
	 * labeled alternative in {@link PrestoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterUnboundedFrame(PrestoSqlBaseParser.UnboundedFrameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unboundedFrame}
	 * labeled alternative in {@link PrestoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitUnboundedFrame(PrestoSqlBaseParser.UnboundedFrameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentRowBound}
	 * labeled alternative in {@link PrestoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterCurrentRowBound(PrestoSqlBaseParser.CurrentRowBoundContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentRowBound}
	 * labeled alternative in {@link PrestoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitCurrentRowBound(PrestoSqlBaseParser.CurrentRowBoundContext ctx);
	/**
	 * Enter a parse tree produced by the {@code boundedFrame}
	 * labeled alternative in {@link PrestoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterBoundedFrame(PrestoSqlBaseParser.BoundedFrameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code boundedFrame}
	 * labeled alternative in {@link PrestoSqlBaseParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitBoundedFrame(PrestoSqlBaseParser.BoundedFrameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code explainFormat}
	 * labeled alternative in {@link PrestoSqlBaseParser#explainOption}.
	 * @param ctx the parse tree
	 */
	void enterExplainFormat(PrestoSqlBaseParser.ExplainFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code explainFormat}
	 * labeled alternative in {@link PrestoSqlBaseParser#explainOption}.
	 * @param ctx the parse tree
	 */
	void exitExplainFormat(PrestoSqlBaseParser.ExplainFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code explainType}
	 * labeled alternative in {@link PrestoSqlBaseParser#explainOption}.
	 * @param ctx the parse tree
	 */
	void enterExplainType(PrestoSqlBaseParser.ExplainTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code explainType}
	 * labeled alternative in {@link PrestoSqlBaseParser#explainOption}.
	 * @param ctx the parse tree
	 */
	void exitExplainType(PrestoSqlBaseParser.ExplainTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isolationLevel}
	 * labeled alternative in {@link PrestoSqlBaseParser#transactionMode}.
	 * @param ctx the parse tree
	 */
	void enterIsolationLevel(PrestoSqlBaseParser.IsolationLevelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isolationLevel}
	 * labeled alternative in {@link PrestoSqlBaseParser#transactionMode}.
	 * @param ctx the parse tree
	 */
	void exitIsolationLevel(PrestoSqlBaseParser.IsolationLevelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code transactionAccessMode}
	 * labeled alternative in {@link PrestoSqlBaseParser#transactionMode}.
	 * @param ctx the parse tree
	 */
	void enterTransactionAccessMode(PrestoSqlBaseParser.TransactionAccessModeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code transactionAccessMode}
	 * labeled alternative in {@link PrestoSqlBaseParser#transactionMode}.
	 * @param ctx the parse tree
	 */
	void exitTransactionAccessMode(PrestoSqlBaseParser.TransactionAccessModeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code readUncommitted}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void enterReadUncommitted(PrestoSqlBaseParser.ReadUncommittedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code readUncommitted}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void exitReadUncommitted(PrestoSqlBaseParser.ReadUncommittedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code readCommitted}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void enterReadCommitted(PrestoSqlBaseParser.ReadCommittedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code readCommitted}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void exitReadCommitted(PrestoSqlBaseParser.ReadCommittedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code repeatableRead}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void enterRepeatableRead(PrestoSqlBaseParser.RepeatableReadContext ctx);
	/**
	 * Exit a parse tree produced by the {@code repeatableRead}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void exitRepeatableRead(PrestoSqlBaseParser.RepeatableReadContext ctx);
	/**
	 * Enter a parse tree produced by the {@code serializable}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void enterSerializable(PrestoSqlBaseParser.SerializableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code serializable}
	 * labeled alternative in {@link PrestoSqlBaseParser#levelOfIsolation}.
	 * @param ctx the parse tree
	 */
	void exitSerializable(PrestoSqlBaseParser.SerializableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code positionalArgument}
	 * labeled alternative in {@link PrestoSqlBaseParser#callArgument}.
	 * @param ctx the parse tree
	 */
	void enterPositionalArgument(PrestoSqlBaseParser.PositionalArgumentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code positionalArgument}
	 * labeled alternative in {@link PrestoSqlBaseParser#callArgument}.
	 * @param ctx the parse tree
	 */
	void exitPositionalArgument(PrestoSqlBaseParser.PositionalArgumentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code namedArgument}
	 * labeled alternative in {@link PrestoSqlBaseParser#callArgument}.
	 * @param ctx the parse tree
	 */
	void enterNamedArgument(PrestoSqlBaseParser.NamedArgumentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code namedArgument}
	 * labeled alternative in {@link PrestoSqlBaseParser#callArgument}.
	 * @param ctx the parse tree
	 */
	void exitNamedArgument(PrestoSqlBaseParser.NamedArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#privilege}.
	 * @param ctx the parse tree
	 */
	void enterPrivilege(PrestoSqlBaseParser.PrivilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#privilege}.
	 * @param ctx the parse tree
	 */
	void exitPrivilege(PrestoSqlBaseParser.PrivilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(PrestoSqlBaseParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(PrestoSqlBaseParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentUserGrantor}
	 * labeled alternative in {@link PrestoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void enterCurrentUserGrantor(PrestoSqlBaseParser.CurrentUserGrantorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentUserGrantor}
	 * labeled alternative in {@link PrestoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void exitCurrentUserGrantor(PrestoSqlBaseParser.CurrentUserGrantorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentRoleGrantor}
	 * labeled alternative in {@link PrestoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void enterCurrentRoleGrantor(PrestoSqlBaseParser.CurrentRoleGrantorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentRoleGrantor}
	 * labeled alternative in {@link PrestoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void exitCurrentRoleGrantor(PrestoSqlBaseParser.CurrentRoleGrantorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code specifiedPrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void enterSpecifiedPrincipal(PrestoSqlBaseParser.SpecifiedPrincipalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code specifiedPrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#grantor}.
	 * @param ctx the parse tree
	 */
	void exitSpecifiedPrincipal(PrestoSqlBaseParser.SpecifiedPrincipalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code userPrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void enterUserPrincipal(PrestoSqlBaseParser.UserPrincipalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code userPrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void exitUserPrincipal(PrestoSqlBaseParser.UserPrincipalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rolePrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void enterRolePrincipal(PrestoSqlBaseParser.RolePrincipalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rolePrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void exitRolePrincipal(PrestoSqlBaseParser.RolePrincipalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unspecifiedPrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void enterUnspecifiedPrincipal(PrestoSqlBaseParser.UnspecifiedPrincipalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unspecifiedPrincipal}
	 * labeled alternative in {@link PrestoSqlBaseParser#principal}.
	 * @param ctx the parse tree
	 */
	void exitUnspecifiedPrincipal(PrestoSqlBaseParser.UnspecifiedPrincipalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#roles}.
	 * @param ctx the parse tree
	 */
	void enterRoles(PrestoSqlBaseParser.RolesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#roles}.
	 * @param ctx the parse tree
	 */
	void exitRoles(PrestoSqlBaseParser.RolesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedIdentifier(PrestoSqlBaseParser.UnquotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedIdentifier(PrestoSqlBaseParser.UnquotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quotedIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifier(PrestoSqlBaseParser.QuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quotedIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifier(PrestoSqlBaseParser.QuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code backQuotedIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterBackQuotedIdentifier(PrestoSqlBaseParser.BackQuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code backQuotedIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitBackQuotedIdentifier(PrestoSqlBaseParser.BackQuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code digitIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterDigitIdentifier(PrestoSqlBaseParser.DigitIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code digitIdentifier}
	 * labeled alternative in {@link PrestoSqlBaseParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitDigitIdentifier(PrestoSqlBaseParser.DigitIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDecimalLiteral(PrestoSqlBaseParser.DecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decimalLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDecimalLiteral(PrestoSqlBaseParser.DecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(PrestoSqlBaseParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(PrestoSqlBaseParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(PrestoSqlBaseParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link PrestoSqlBaseParser#number}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(PrestoSqlBaseParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PrestoSqlBaseParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void enterNonReserved(PrestoSqlBaseParser.NonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link PrestoSqlBaseParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void exitNonReserved(PrestoSqlBaseParser.NonReservedContext ctx);
}