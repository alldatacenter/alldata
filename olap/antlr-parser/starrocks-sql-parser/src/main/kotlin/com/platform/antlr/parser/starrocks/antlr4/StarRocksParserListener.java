// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.starrocks.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link StarRocksParser}.
 */
public interface StarRocksParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#sqlStatements}.
	 * @param ctx the parse tree
	 */
	void enterSqlStatements(StarRocksParser.SqlStatementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#sqlStatements}.
	 * @param ctx the parse tree
	 */
	void exitSqlStatements(StarRocksParser.SqlStatementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void enterSingleStatement(StarRocksParser.SingleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void exitSingleStatement(StarRocksParser.SingleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStatement(StarRocksParser.EmptyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStatement(StarRocksParser.EmptyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(StarRocksParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(StarRocksParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#useDatabaseStatement}.
	 * @param ctx the parse tree
	 */
	void enterUseDatabaseStatement(StarRocksParser.UseDatabaseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#useDatabaseStatement}.
	 * @param ctx the parse tree
	 */
	void exitUseDatabaseStatement(StarRocksParser.UseDatabaseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#useCatalogStatement}.
	 * @param ctx the parse tree
	 */
	void enterUseCatalogStatement(StarRocksParser.UseCatalogStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#useCatalogStatement}.
	 * @param ctx the parse tree
	 */
	void exitUseCatalogStatement(StarRocksParser.UseCatalogStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#setCatalogStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetCatalogStatement(StarRocksParser.SetCatalogStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#setCatalogStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetCatalogStatement(StarRocksParser.SetCatalogStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showDatabasesStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowDatabasesStatement(StarRocksParser.ShowDatabasesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showDatabasesStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowDatabasesStatement(StarRocksParser.ShowDatabasesStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterDbQuotaStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterDbQuotaStatement(StarRocksParser.AlterDbQuotaStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterDbQuotaStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterDbQuotaStatement(StarRocksParser.AlterDbQuotaStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createDbStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateDbStatement(StarRocksParser.CreateDbStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createDbStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateDbStatement(StarRocksParser.CreateDbStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropDbStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropDbStatement(StarRocksParser.DropDbStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropDbStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropDbStatement(StarRocksParser.DropDbStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showCreateDbStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateDbStatement(StarRocksParser.ShowCreateDbStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showCreateDbStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateDbStatement(StarRocksParser.ShowCreateDbStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterDatabaseRenameStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterDatabaseRenameStatement(StarRocksParser.AlterDatabaseRenameStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterDatabaseRenameStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterDatabaseRenameStatement(StarRocksParser.AlterDatabaseRenameStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#recoverDbStmt}.
	 * @param ctx the parse tree
	 */
	void enterRecoverDbStmt(StarRocksParser.RecoverDbStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#recoverDbStmt}.
	 * @param ctx the parse tree
	 */
	void exitRecoverDbStmt(StarRocksParser.RecoverDbStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showDataStmt}.
	 * @param ctx the parse tree
	 */
	void enterShowDataStmt(StarRocksParser.ShowDataStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showDataStmt}.
	 * @param ctx the parse tree
	 */
	void exitShowDataStmt(StarRocksParser.ShowDataStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableStatement(StarRocksParser.CreateTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableStatement(StarRocksParser.CreateTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#columnDesc}.
	 * @param ctx the parse tree
	 */
	void enterColumnDesc(StarRocksParser.ColumnDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#columnDesc}.
	 * @param ctx the parse tree
	 */
	void exitColumnDesc(StarRocksParser.ColumnDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#charsetName}.
	 * @param ctx the parse tree
	 */
	void enterCharsetName(StarRocksParser.CharsetNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#charsetName}.
	 * @param ctx the parse tree
	 */
	void exitCharsetName(StarRocksParser.CharsetNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#defaultDesc}.
	 * @param ctx the parse tree
	 */
	void enterDefaultDesc(StarRocksParser.DefaultDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#defaultDesc}.
	 * @param ctx the parse tree
	 */
	void exitDefaultDesc(StarRocksParser.DefaultDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#materializedColumnDesc}.
	 * @param ctx the parse tree
	 */
	void enterMaterializedColumnDesc(StarRocksParser.MaterializedColumnDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#materializedColumnDesc}.
	 * @param ctx the parse tree
	 */
	void exitMaterializedColumnDesc(StarRocksParser.MaterializedColumnDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#indexDesc}.
	 * @param ctx the parse tree
	 */
	void enterIndexDesc(StarRocksParser.IndexDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#indexDesc}.
	 * @param ctx the parse tree
	 */
	void exitIndexDesc(StarRocksParser.IndexDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#engineDesc}.
	 * @param ctx the parse tree
	 */
	void enterEngineDesc(StarRocksParser.EngineDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#engineDesc}.
	 * @param ctx the parse tree
	 */
	void exitEngineDesc(StarRocksParser.EngineDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#charsetDesc}.
	 * @param ctx the parse tree
	 */
	void enterCharsetDesc(StarRocksParser.CharsetDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#charsetDesc}.
	 * @param ctx the parse tree
	 */
	void exitCharsetDesc(StarRocksParser.CharsetDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#collateDesc}.
	 * @param ctx the parse tree
	 */
	void enterCollateDesc(StarRocksParser.CollateDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#collateDesc}.
	 * @param ctx the parse tree
	 */
	void exitCollateDesc(StarRocksParser.CollateDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#keyDesc}.
	 * @param ctx the parse tree
	 */
	void enterKeyDesc(StarRocksParser.KeyDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#keyDesc}.
	 * @param ctx the parse tree
	 */
	void exitKeyDesc(StarRocksParser.KeyDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#orderByDesc}.
	 * @param ctx the parse tree
	 */
	void enterOrderByDesc(StarRocksParser.OrderByDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#orderByDesc}.
	 * @param ctx the parse tree
	 */
	void exitOrderByDesc(StarRocksParser.OrderByDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#aggDesc}.
	 * @param ctx the parse tree
	 */
	void enterAggDesc(StarRocksParser.AggDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#aggDesc}.
	 * @param ctx the parse tree
	 */
	void exitAggDesc(StarRocksParser.AggDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#rollupDesc}.
	 * @param ctx the parse tree
	 */
	void enterRollupDesc(StarRocksParser.RollupDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#rollupDesc}.
	 * @param ctx the parse tree
	 */
	void exitRollupDesc(StarRocksParser.RollupDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#rollupItem}.
	 * @param ctx the parse tree
	 */
	void enterRollupItem(StarRocksParser.RollupItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#rollupItem}.
	 * @param ctx the parse tree
	 */
	void exitRollupItem(StarRocksParser.RollupItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dupKeys}.
	 * @param ctx the parse tree
	 */
	void enterDupKeys(StarRocksParser.DupKeysContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dupKeys}.
	 * @param ctx the parse tree
	 */
	void exitDupKeys(StarRocksParser.DupKeysContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#fromRollup}.
	 * @param ctx the parse tree
	 */
	void enterFromRollup(StarRocksParser.FromRollupContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#fromRollup}.
	 * @param ctx the parse tree
	 */
	void exitFromRollup(StarRocksParser.FromRollupContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createTemporaryTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTemporaryTableStatement(StarRocksParser.CreateTemporaryTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createTemporaryTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTemporaryTableStatement(StarRocksParser.CreateTemporaryTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createTableAsSelectStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableAsSelectStatement(StarRocksParser.CreateTableAsSelectStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createTableAsSelectStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableAsSelectStatement(StarRocksParser.CreateTableAsSelectStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropTableStatement(StarRocksParser.DropTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropTableStatement(StarRocksParser.DropTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterTableStatement(StarRocksParser.AlterTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterTableStatement(StarRocksParser.AlterTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createIndexStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateIndexStatement(StarRocksParser.CreateIndexStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createIndexStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateIndexStatement(StarRocksParser.CreateIndexStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropIndexStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropIndexStatement(StarRocksParser.DropIndexStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropIndexStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropIndexStatement(StarRocksParser.DropIndexStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#indexType}.
	 * @param ctx the parse tree
	 */
	void enterIndexType(StarRocksParser.IndexTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#indexType}.
	 * @param ctx the parse tree
	 */
	void exitIndexType(StarRocksParser.IndexTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowTableStatement(StarRocksParser.ShowTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowTableStatement(StarRocksParser.ShowTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showCreateTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateTableStatement(StarRocksParser.ShowCreateTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showCreateTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateTableStatement(StarRocksParser.ShowCreateTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showColumnStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowColumnStatement(StarRocksParser.ShowColumnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showColumnStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowColumnStatement(StarRocksParser.ShowColumnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showTableStatusStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowTableStatusStatement(StarRocksParser.ShowTableStatusStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showTableStatusStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowTableStatusStatement(StarRocksParser.ShowTableStatusStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#refreshTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshTableStatement(StarRocksParser.RefreshTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#refreshTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshTableStatement(StarRocksParser.RefreshTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showAlterStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowAlterStatement(StarRocksParser.ShowAlterStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showAlterStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowAlterStatement(StarRocksParser.ShowAlterStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#descTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterDescTableStatement(StarRocksParser.DescTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#descTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitDescTableStatement(StarRocksParser.DescTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createTableLikeStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableLikeStatement(StarRocksParser.CreateTableLikeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createTableLikeStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableLikeStatement(StarRocksParser.CreateTableLikeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showIndexStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowIndexStatement(StarRocksParser.ShowIndexStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showIndexStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowIndexStatement(StarRocksParser.ShowIndexStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#recoverTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterRecoverTableStatement(StarRocksParser.RecoverTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#recoverTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitRecoverTableStatement(StarRocksParser.RecoverTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#truncateTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterTruncateTableStatement(StarRocksParser.TruncateTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#truncateTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitTruncateTableStatement(StarRocksParser.TruncateTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#cancelAlterTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterCancelAlterTableStatement(StarRocksParser.CancelAlterTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#cancelAlterTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitCancelAlterTableStatement(StarRocksParser.CancelAlterTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showPartitionsStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowPartitionsStatement(StarRocksParser.ShowPartitionsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showPartitionsStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowPartitionsStatement(StarRocksParser.ShowPartitionsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#recoverPartitionStatement}.
	 * @param ctx the parse tree
	 */
	void enterRecoverPartitionStatement(StarRocksParser.RecoverPartitionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#recoverPartitionStatement}.
	 * @param ctx the parse tree
	 */
	void exitRecoverPartitionStatement(StarRocksParser.RecoverPartitionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createViewStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateViewStatement(StarRocksParser.CreateViewStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createViewStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateViewStatement(StarRocksParser.CreateViewStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterViewStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterViewStatement(StarRocksParser.AlterViewStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterViewStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterViewStatement(StarRocksParser.AlterViewStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropViewStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropViewStatement(StarRocksParser.DropViewStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropViewStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropViewStatement(StarRocksParser.DropViewStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#columnNameWithComment}.
	 * @param ctx the parse tree
	 */
	void enterColumnNameWithComment(StarRocksParser.ColumnNameWithCommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#columnNameWithComment}.
	 * @param ctx the parse tree
	 */
	void exitColumnNameWithComment(StarRocksParser.ColumnNameWithCommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#submitTaskStatement}.
	 * @param ctx the parse tree
	 */
	void enterSubmitTaskStatement(StarRocksParser.SubmitTaskStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#submitTaskStatement}.
	 * @param ctx the parse tree
	 */
	void exitSubmitTaskStatement(StarRocksParser.SubmitTaskStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createMaterializedViewStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateMaterializedViewStatement(StarRocksParser.CreateMaterializedViewStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createMaterializedViewStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateMaterializedViewStatement(StarRocksParser.CreateMaterializedViewStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#materializedViewDesc}.
	 * @param ctx the parse tree
	 */
	void enterMaterializedViewDesc(StarRocksParser.MaterializedViewDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#materializedViewDesc}.
	 * @param ctx the parse tree
	 */
	void exitMaterializedViewDesc(StarRocksParser.MaterializedViewDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showMaterializedViewsStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowMaterializedViewsStatement(StarRocksParser.ShowMaterializedViewsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showMaterializedViewsStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowMaterializedViewsStatement(StarRocksParser.ShowMaterializedViewsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropMaterializedViewStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropMaterializedViewStatement(StarRocksParser.DropMaterializedViewStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropMaterializedViewStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropMaterializedViewStatement(StarRocksParser.DropMaterializedViewStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterMaterializedViewStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterMaterializedViewStatement(StarRocksParser.AlterMaterializedViewStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterMaterializedViewStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterMaterializedViewStatement(StarRocksParser.AlterMaterializedViewStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#refreshMaterializedViewStatement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshMaterializedViewStatement(StarRocksParser.RefreshMaterializedViewStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#refreshMaterializedViewStatement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshMaterializedViewStatement(StarRocksParser.RefreshMaterializedViewStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#cancelRefreshMaterializedViewStatement}.
	 * @param ctx the parse tree
	 */
	void enterCancelRefreshMaterializedViewStatement(StarRocksParser.CancelRefreshMaterializedViewStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#cancelRefreshMaterializedViewStatement}.
	 * @param ctx the parse tree
	 */
	void exitCancelRefreshMaterializedViewStatement(StarRocksParser.CancelRefreshMaterializedViewStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#adminSetConfigStatement}.
	 * @param ctx the parse tree
	 */
	void enterAdminSetConfigStatement(StarRocksParser.AdminSetConfigStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#adminSetConfigStatement}.
	 * @param ctx the parse tree
	 */
	void exitAdminSetConfigStatement(StarRocksParser.AdminSetConfigStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#adminSetReplicaStatusStatement}.
	 * @param ctx the parse tree
	 */
	void enterAdminSetReplicaStatusStatement(StarRocksParser.AdminSetReplicaStatusStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#adminSetReplicaStatusStatement}.
	 * @param ctx the parse tree
	 */
	void exitAdminSetReplicaStatusStatement(StarRocksParser.AdminSetReplicaStatusStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#adminShowConfigStatement}.
	 * @param ctx the parse tree
	 */
	void enterAdminShowConfigStatement(StarRocksParser.AdminShowConfigStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#adminShowConfigStatement}.
	 * @param ctx the parse tree
	 */
	void exitAdminShowConfigStatement(StarRocksParser.AdminShowConfigStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#adminShowReplicaDistributionStatement}.
	 * @param ctx the parse tree
	 */
	void enterAdminShowReplicaDistributionStatement(StarRocksParser.AdminShowReplicaDistributionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#adminShowReplicaDistributionStatement}.
	 * @param ctx the parse tree
	 */
	void exitAdminShowReplicaDistributionStatement(StarRocksParser.AdminShowReplicaDistributionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#adminShowReplicaStatusStatement}.
	 * @param ctx the parse tree
	 */
	void enterAdminShowReplicaStatusStatement(StarRocksParser.AdminShowReplicaStatusStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#adminShowReplicaStatusStatement}.
	 * @param ctx the parse tree
	 */
	void exitAdminShowReplicaStatusStatement(StarRocksParser.AdminShowReplicaStatusStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#adminRepairTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterAdminRepairTableStatement(StarRocksParser.AdminRepairTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#adminRepairTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitAdminRepairTableStatement(StarRocksParser.AdminRepairTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#adminCancelRepairTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterAdminCancelRepairTableStatement(StarRocksParser.AdminCancelRepairTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#adminCancelRepairTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitAdminCancelRepairTableStatement(StarRocksParser.AdminCancelRepairTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#adminCheckTabletsStatement}.
	 * @param ctx the parse tree
	 */
	void enterAdminCheckTabletsStatement(StarRocksParser.AdminCheckTabletsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#adminCheckTabletsStatement}.
	 * @param ctx the parse tree
	 */
	void exitAdminCheckTabletsStatement(StarRocksParser.AdminCheckTabletsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#killStatement}.
	 * @param ctx the parse tree
	 */
	void enterKillStatement(StarRocksParser.KillStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#killStatement}.
	 * @param ctx the parse tree
	 */
	void exitKillStatement(StarRocksParser.KillStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#syncStatement}.
	 * @param ctx the parse tree
	 */
	void enterSyncStatement(StarRocksParser.SyncStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#syncStatement}.
	 * @param ctx the parse tree
	 */
	void exitSyncStatement(StarRocksParser.SyncStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterSystemStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterSystemStatement(StarRocksParser.AlterSystemStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterSystemStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterSystemStatement(StarRocksParser.AlterSystemStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#cancelAlterSystemStatement}.
	 * @param ctx the parse tree
	 */
	void enterCancelAlterSystemStatement(StarRocksParser.CancelAlterSystemStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#cancelAlterSystemStatement}.
	 * @param ctx the parse tree
	 */
	void exitCancelAlterSystemStatement(StarRocksParser.CancelAlterSystemStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showComputeNodesStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowComputeNodesStatement(StarRocksParser.ShowComputeNodesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showComputeNodesStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowComputeNodesStatement(StarRocksParser.ShowComputeNodesStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createExternalCatalogStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateExternalCatalogStatement(StarRocksParser.CreateExternalCatalogStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createExternalCatalogStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateExternalCatalogStatement(StarRocksParser.CreateExternalCatalogStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showCreateExternalCatalogStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateExternalCatalogStatement(StarRocksParser.ShowCreateExternalCatalogStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showCreateExternalCatalogStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateExternalCatalogStatement(StarRocksParser.ShowCreateExternalCatalogStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropExternalCatalogStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropExternalCatalogStatement(StarRocksParser.DropExternalCatalogStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropExternalCatalogStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropExternalCatalogStatement(StarRocksParser.DropExternalCatalogStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showCatalogsStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCatalogsStatement(StarRocksParser.ShowCatalogsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showCatalogsStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCatalogsStatement(StarRocksParser.ShowCatalogsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateWarehouseStatement(StarRocksParser.CreateWarehouseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateWarehouseStatement(StarRocksParser.CreateWarehouseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showWarehousesStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowWarehousesStatement(StarRocksParser.ShowWarehousesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showWarehousesStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowWarehousesStatement(StarRocksParser.ShowWarehousesStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropWarehouseStatement(StarRocksParser.DropWarehouseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropWarehouseStatement(StarRocksParser.DropWarehouseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterWarehouseStatement(StarRocksParser.AlterWarehouseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterWarehouseStatement(StarRocksParser.AlterWarehouseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showClustersStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowClustersStatement(StarRocksParser.ShowClustersStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showClustersStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowClustersStatement(StarRocksParser.ShowClustersStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#suspendWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void enterSuspendWarehouseStatement(StarRocksParser.SuspendWarehouseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#suspendWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void exitSuspendWarehouseStatement(StarRocksParser.SuspendWarehouseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#resumeWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void enterResumeWarehouseStatement(StarRocksParser.ResumeWarehouseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#resumeWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void exitResumeWarehouseStatement(StarRocksParser.ResumeWarehouseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createStorageVolumeStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateStorageVolumeStatement(StarRocksParser.CreateStorageVolumeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createStorageVolumeStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateStorageVolumeStatement(StarRocksParser.CreateStorageVolumeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#typeDesc}.
	 * @param ctx the parse tree
	 */
	void enterTypeDesc(StarRocksParser.TypeDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#typeDesc}.
	 * @param ctx the parse tree
	 */
	void exitTypeDesc(StarRocksParser.TypeDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#locationsDesc}.
	 * @param ctx the parse tree
	 */
	void enterLocationsDesc(StarRocksParser.LocationsDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#locationsDesc}.
	 * @param ctx the parse tree
	 */
	void exitLocationsDesc(StarRocksParser.LocationsDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showStorageVolumesStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowStorageVolumesStatement(StarRocksParser.ShowStorageVolumesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showStorageVolumesStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowStorageVolumesStatement(StarRocksParser.ShowStorageVolumesStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropStorageVolumeStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropStorageVolumeStatement(StarRocksParser.DropStorageVolumeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropStorageVolumeStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropStorageVolumeStatement(StarRocksParser.DropStorageVolumeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterStorageVolumeStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterStorageVolumeStatement(StarRocksParser.AlterStorageVolumeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterStorageVolumeStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterStorageVolumeStatement(StarRocksParser.AlterStorageVolumeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterStorageVolumeClause}.
	 * @param ctx the parse tree
	 */
	void enterAlterStorageVolumeClause(StarRocksParser.AlterStorageVolumeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterStorageVolumeClause}.
	 * @param ctx the parse tree
	 */
	void exitAlterStorageVolumeClause(StarRocksParser.AlterStorageVolumeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#modifyStorageVolumePropertiesClause}.
	 * @param ctx the parse tree
	 */
	void enterModifyStorageVolumePropertiesClause(StarRocksParser.ModifyStorageVolumePropertiesClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#modifyStorageVolumePropertiesClause}.
	 * @param ctx the parse tree
	 */
	void exitModifyStorageVolumePropertiesClause(StarRocksParser.ModifyStorageVolumePropertiesClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#modifyStorageVolumeCommentClause}.
	 * @param ctx the parse tree
	 */
	void enterModifyStorageVolumeCommentClause(StarRocksParser.ModifyStorageVolumeCommentClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#modifyStorageVolumeCommentClause}.
	 * @param ctx the parse tree
	 */
	void exitModifyStorageVolumeCommentClause(StarRocksParser.ModifyStorageVolumeCommentClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#descStorageVolumeStatement}.
	 * @param ctx the parse tree
	 */
	void enterDescStorageVolumeStatement(StarRocksParser.DescStorageVolumeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#descStorageVolumeStatement}.
	 * @param ctx the parse tree
	 */
	void exitDescStorageVolumeStatement(StarRocksParser.DescStorageVolumeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#setDefaultStorageVolumeStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetDefaultStorageVolumeStatement(StarRocksParser.SetDefaultStorageVolumeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#setDefaultStorageVolumeStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetDefaultStorageVolumeStatement(StarRocksParser.SetDefaultStorageVolumeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterClause}.
	 * @param ctx the parse tree
	 */
	void enterAlterClause(StarRocksParser.AlterClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterClause}.
	 * @param ctx the parse tree
	 */
	void exitAlterClause(StarRocksParser.AlterClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#addFrontendClause}.
	 * @param ctx the parse tree
	 */
	void enterAddFrontendClause(StarRocksParser.AddFrontendClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#addFrontendClause}.
	 * @param ctx the parse tree
	 */
	void exitAddFrontendClause(StarRocksParser.AddFrontendClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropFrontendClause}.
	 * @param ctx the parse tree
	 */
	void enterDropFrontendClause(StarRocksParser.DropFrontendClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropFrontendClause}.
	 * @param ctx the parse tree
	 */
	void exitDropFrontendClause(StarRocksParser.DropFrontendClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#modifyFrontendHostClause}.
	 * @param ctx the parse tree
	 */
	void enterModifyFrontendHostClause(StarRocksParser.ModifyFrontendHostClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#modifyFrontendHostClause}.
	 * @param ctx the parse tree
	 */
	void exitModifyFrontendHostClause(StarRocksParser.ModifyFrontendHostClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#addBackendClause}.
	 * @param ctx the parse tree
	 */
	void enterAddBackendClause(StarRocksParser.AddBackendClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#addBackendClause}.
	 * @param ctx the parse tree
	 */
	void exitAddBackendClause(StarRocksParser.AddBackendClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropBackendClause}.
	 * @param ctx the parse tree
	 */
	void enterDropBackendClause(StarRocksParser.DropBackendClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropBackendClause}.
	 * @param ctx the parse tree
	 */
	void exitDropBackendClause(StarRocksParser.DropBackendClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#decommissionBackendClause}.
	 * @param ctx the parse tree
	 */
	void enterDecommissionBackendClause(StarRocksParser.DecommissionBackendClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#decommissionBackendClause}.
	 * @param ctx the parse tree
	 */
	void exitDecommissionBackendClause(StarRocksParser.DecommissionBackendClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#modifyBackendHostClause}.
	 * @param ctx the parse tree
	 */
	void enterModifyBackendHostClause(StarRocksParser.ModifyBackendHostClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#modifyBackendHostClause}.
	 * @param ctx the parse tree
	 */
	void exitModifyBackendHostClause(StarRocksParser.ModifyBackendHostClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#addComputeNodeClause}.
	 * @param ctx the parse tree
	 */
	void enterAddComputeNodeClause(StarRocksParser.AddComputeNodeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#addComputeNodeClause}.
	 * @param ctx the parse tree
	 */
	void exitAddComputeNodeClause(StarRocksParser.AddComputeNodeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropComputeNodeClause}.
	 * @param ctx the parse tree
	 */
	void enterDropComputeNodeClause(StarRocksParser.DropComputeNodeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropComputeNodeClause}.
	 * @param ctx the parse tree
	 */
	void exitDropComputeNodeClause(StarRocksParser.DropComputeNodeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#modifyBrokerClause}.
	 * @param ctx the parse tree
	 */
	void enterModifyBrokerClause(StarRocksParser.ModifyBrokerClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#modifyBrokerClause}.
	 * @param ctx the parse tree
	 */
	void exitModifyBrokerClause(StarRocksParser.ModifyBrokerClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterLoadErrorUrlClause}.
	 * @param ctx the parse tree
	 */
	void enterAlterLoadErrorUrlClause(StarRocksParser.AlterLoadErrorUrlClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterLoadErrorUrlClause}.
	 * @param ctx the parse tree
	 */
	void exitAlterLoadErrorUrlClause(StarRocksParser.AlterLoadErrorUrlClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createImageClause}.
	 * @param ctx the parse tree
	 */
	void enterCreateImageClause(StarRocksParser.CreateImageClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createImageClause}.
	 * @param ctx the parse tree
	 */
	void exitCreateImageClause(StarRocksParser.CreateImageClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#cleanTabletSchedQClause}.
	 * @param ctx the parse tree
	 */
	void enterCleanTabletSchedQClause(StarRocksParser.CleanTabletSchedQClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#cleanTabletSchedQClause}.
	 * @param ctx the parse tree
	 */
	void exitCleanTabletSchedQClause(StarRocksParser.CleanTabletSchedQClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createIndexClause}.
	 * @param ctx the parse tree
	 */
	void enterCreateIndexClause(StarRocksParser.CreateIndexClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createIndexClause}.
	 * @param ctx the parse tree
	 */
	void exitCreateIndexClause(StarRocksParser.CreateIndexClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropIndexClause}.
	 * @param ctx the parse tree
	 */
	void enterDropIndexClause(StarRocksParser.DropIndexClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropIndexClause}.
	 * @param ctx the parse tree
	 */
	void exitDropIndexClause(StarRocksParser.DropIndexClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#tableRenameClause}.
	 * @param ctx the parse tree
	 */
	void enterTableRenameClause(StarRocksParser.TableRenameClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#tableRenameClause}.
	 * @param ctx the parse tree
	 */
	void exitTableRenameClause(StarRocksParser.TableRenameClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#swapTableClause}.
	 * @param ctx the parse tree
	 */
	void enterSwapTableClause(StarRocksParser.SwapTableClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#swapTableClause}.
	 * @param ctx the parse tree
	 */
	void exitSwapTableClause(StarRocksParser.SwapTableClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#modifyTablePropertiesClause}.
	 * @param ctx the parse tree
	 */
	void enterModifyTablePropertiesClause(StarRocksParser.ModifyTablePropertiesClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#modifyTablePropertiesClause}.
	 * @param ctx the parse tree
	 */
	void exitModifyTablePropertiesClause(StarRocksParser.ModifyTablePropertiesClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#modifyCommentClause}.
	 * @param ctx the parse tree
	 */
	void enterModifyCommentClause(StarRocksParser.ModifyCommentClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#modifyCommentClause}.
	 * @param ctx the parse tree
	 */
	void exitModifyCommentClause(StarRocksParser.ModifyCommentClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#addColumnClause}.
	 * @param ctx the parse tree
	 */
	void enterAddColumnClause(StarRocksParser.AddColumnClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#addColumnClause}.
	 * @param ctx the parse tree
	 */
	void exitAddColumnClause(StarRocksParser.AddColumnClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#addColumnsClause}.
	 * @param ctx the parse tree
	 */
	void enterAddColumnsClause(StarRocksParser.AddColumnsClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#addColumnsClause}.
	 * @param ctx the parse tree
	 */
	void exitAddColumnsClause(StarRocksParser.AddColumnsClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropColumnClause}.
	 * @param ctx the parse tree
	 */
	void enterDropColumnClause(StarRocksParser.DropColumnClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropColumnClause}.
	 * @param ctx the parse tree
	 */
	void exitDropColumnClause(StarRocksParser.DropColumnClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#modifyColumnClause}.
	 * @param ctx the parse tree
	 */
	void enterModifyColumnClause(StarRocksParser.ModifyColumnClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#modifyColumnClause}.
	 * @param ctx the parse tree
	 */
	void exitModifyColumnClause(StarRocksParser.ModifyColumnClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#columnRenameClause}.
	 * @param ctx the parse tree
	 */
	void enterColumnRenameClause(StarRocksParser.ColumnRenameClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#columnRenameClause}.
	 * @param ctx the parse tree
	 */
	void exitColumnRenameClause(StarRocksParser.ColumnRenameClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#reorderColumnsClause}.
	 * @param ctx the parse tree
	 */
	void enterReorderColumnsClause(StarRocksParser.ReorderColumnsClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#reorderColumnsClause}.
	 * @param ctx the parse tree
	 */
	void exitReorderColumnsClause(StarRocksParser.ReorderColumnsClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#rollupRenameClause}.
	 * @param ctx the parse tree
	 */
	void enterRollupRenameClause(StarRocksParser.RollupRenameClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#rollupRenameClause}.
	 * @param ctx the parse tree
	 */
	void exitRollupRenameClause(StarRocksParser.RollupRenameClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#compactionClause}.
	 * @param ctx the parse tree
	 */
	void enterCompactionClause(StarRocksParser.CompactionClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#compactionClause}.
	 * @param ctx the parse tree
	 */
	void exitCompactionClause(StarRocksParser.CompactionClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#addPartitionClause}.
	 * @param ctx the parse tree
	 */
	void enterAddPartitionClause(StarRocksParser.AddPartitionClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#addPartitionClause}.
	 * @param ctx the parse tree
	 */
	void exitAddPartitionClause(StarRocksParser.AddPartitionClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropPartitionClause}.
	 * @param ctx the parse tree
	 */
	void enterDropPartitionClause(StarRocksParser.DropPartitionClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropPartitionClause}.
	 * @param ctx the parse tree
	 */
	void exitDropPartitionClause(StarRocksParser.DropPartitionClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#truncatePartitionClause}.
	 * @param ctx the parse tree
	 */
	void enterTruncatePartitionClause(StarRocksParser.TruncatePartitionClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#truncatePartitionClause}.
	 * @param ctx the parse tree
	 */
	void exitTruncatePartitionClause(StarRocksParser.TruncatePartitionClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#modifyPartitionClause}.
	 * @param ctx the parse tree
	 */
	void enterModifyPartitionClause(StarRocksParser.ModifyPartitionClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#modifyPartitionClause}.
	 * @param ctx the parse tree
	 */
	void exitModifyPartitionClause(StarRocksParser.ModifyPartitionClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#replacePartitionClause}.
	 * @param ctx the parse tree
	 */
	void enterReplacePartitionClause(StarRocksParser.ReplacePartitionClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#replacePartitionClause}.
	 * @param ctx the parse tree
	 */
	void exitReplacePartitionClause(StarRocksParser.ReplacePartitionClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#partitionRenameClause}.
	 * @param ctx the parse tree
	 */
	void enterPartitionRenameClause(StarRocksParser.PartitionRenameClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#partitionRenameClause}.
	 * @param ctx the parse tree
	 */
	void exitPartitionRenameClause(StarRocksParser.PartitionRenameClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#insertStatement}.
	 * @param ctx the parse tree
	 */
	void enterInsertStatement(StarRocksParser.InsertStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#insertStatement}.
	 * @param ctx the parse tree
	 */
	void exitInsertStatement(StarRocksParser.InsertStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#updateStatement}.
	 * @param ctx the parse tree
	 */
	void enterUpdateStatement(StarRocksParser.UpdateStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#updateStatement}.
	 * @param ctx the parse tree
	 */
	void exitUpdateStatement(StarRocksParser.UpdateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#deleteStatement}.
	 * @param ctx the parse tree
	 */
	void enterDeleteStatement(StarRocksParser.DeleteStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#deleteStatement}.
	 * @param ctx the parse tree
	 */
	void exitDeleteStatement(StarRocksParser.DeleteStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateRoutineLoadStatement(StarRocksParser.CreateRoutineLoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateRoutineLoadStatement(StarRocksParser.CreateRoutineLoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterRoutineLoadStatement(StarRocksParser.AlterRoutineLoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterRoutineLoadStatement(StarRocksParser.AlterRoutineLoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dataSource}.
	 * @param ctx the parse tree
	 */
	void enterDataSource(StarRocksParser.DataSourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dataSource}.
	 * @param ctx the parse tree
	 */
	void exitDataSource(StarRocksParser.DataSourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#loadProperties}.
	 * @param ctx the parse tree
	 */
	void enterLoadProperties(StarRocksParser.LoadPropertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#loadProperties}.
	 * @param ctx the parse tree
	 */
	void exitLoadProperties(StarRocksParser.LoadPropertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#colSeparatorProperty}.
	 * @param ctx the parse tree
	 */
	void enterColSeparatorProperty(StarRocksParser.ColSeparatorPropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#colSeparatorProperty}.
	 * @param ctx the parse tree
	 */
	void exitColSeparatorProperty(StarRocksParser.ColSeparatorPropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#rowDelimiterProperty}.
	 * @param ctx the parse tree
	 */
	void enterRowDelimiterProperty(StarRocksParser.RowDelimiterPropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#rowDelimiterProperty}.
	 * @param ctx the parse tree
	 */
	void exitRowDelimiterProperty(StarRocksParser.RowDelimiterPropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#importColumns}.
	 * @param ctx the parse tree
	 */
	void enterImportColumns(StarRocksParser.ImportColumnsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#importColumns}.
	 * @param ctx the parse tree
	 */
	void exitImportColumns(StarRocksParser.ImportColumnsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#columnProperties}.
	 * @param ctx the parse tree
	 */
	void enterColumnProperties(StarRocksParser.ColumnPropertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#columnProperties}.
	 * @param ctx the parse tree
	 */
	void exitColumnProperties(StarRocksParser.ColumnPropertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#jobProperties}.
	 * @param ctx the parse tree
	 */
	void enterJobProperties(StarRocksParser.JobPropertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#jobProperties}.
	 * @param ctx the parse tree
	 */
	void exitJobProperties(StarRocksParser.JobPropertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dataSourceProperties}.
	 * @param ctx the parse tree
	 */
	void enterDataSourceProperties(StarRocksParser.DataSourcePropertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dataSourceProperties}.
	 * @param ctx the parse tree
	 */
	void exitDataSourceProperties(StarRocksParser.DataSourcePropertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#stopRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void enterStopRoutineLoadStatement(StarRocksParser.StopRoutineLoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#stopRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void exitStopRoutineLoadStatement(StarRocksParser.StopRoutineLoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#resumeRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void enterResumeRoutineLoadStatement(StarRocksParser.ResumeRoutineLoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#resumeRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void exitResumeRoutineLoadStatement(StarRocksParser.ResumeRoutineLoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#pauseRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void enterPauseRoutineLoadStatement(StarRocksParser.PauseRoutineLoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#pauseRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void exitPauseRoutineLoadStatement(StarRocksParser.PauseRoutineLoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowRoutineLoadStatement(StarRocksParser.ShowRoutineLoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showRoutineLoadStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowRoutineLoadStatement(StarRocksParser.ShowRoutineLoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showRoutineLoadTaskStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowRoutineLoadTaskStatement(StarRocksParser.ShowRoutineLoadTaskStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showRoutineLoadTaskStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowRoutineLoadTaskStatement(StarRocksParser.ShowRoutineLoadTaskStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showStreamLoadStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowStreamLoadStatement(StarRocksParser.ShowStreamLoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showStreamLoadStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowStreamLoadStatement(StarRocksParser.ShowStreamLoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#analyzeStatement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyzeStatement(StarRocksParser.AnalyzeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#analyzeStatement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyzeStatement(StarRocksParser.AnalyzeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropStatsStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropStatsStatement(StarRocksParser.DropStatsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropStatsStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropStatsStatement(StarRocksParser.DropStatsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#analyzeHistogramStatement}.
	 * @param ctx the parse tree
	 */
	void enterAnalyzeHistogramStatement(StarRocksParser.AnalyzeHistogramStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#analyzeHistogramStatement}.
	 * @param ctx the parse tree
	 */
	void exitAnalyzeHistogramStatement(StarRocksParser.AnalyzeHistogramStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropHistogramStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropHistogramStatement(StarRocksParser.DropHistogramStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropHistogramStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropHistogramStatement(StarRocksParser.DropHistogramStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createAnalyzeStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateAnalyzeStatement(StarRocksParser.CreateAnalyzeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createAnalyzeStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateAnalyzeStatement(StarRocksParser.CreateAnalyzeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropAnalyzeJobStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropAnalyzeJobStatement(StarRocksParser.DropAnalyzeJobStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropAnalyzeJobStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropAnalyzeJobStatement(StarRocksParser.DropAnalyzeJobStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showAnalyzeStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowAnalyzeStatement(StarRocksParser.ShowAnalyzeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showAnalyzeStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowAnalyzeStatement(StarRocksParser.ShowAnalyzeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showStatsMetaStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowStatsMetaStatement(StarRocksParser.ShowStatsMetaStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showStatsMetaStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowStatsMetaStatement(StarRocksParser.ShowStatsMetaStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showHistogramMetaStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowHistogramMetaStatement(StarRocksParser.ShowHistogramMetaStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showHistogramMetaStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowHistogramMetaStatement(StarRocksParser.ShowHistogramMetaStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#killAnalyzeStatement}.
	 * @param ctx the parse tree
	 */
	void enterKillAnalyzeStatement(StarRocksParser.KillAnalyzeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#killAnalyzeStatement}.
	 * @param ctx the parse tree
	 */
	void exitKillAnalyzeStatement(StarRocksParser.KillAnalyzeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createResourceGroupStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateResourceGroupStatement(StarRocksParser.CreateResourceGroupStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createResourceGroupStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateResourceGroupStatement(StarRocksParser.CreateResourceGroupStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropResourceGroupStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropResourceGroupStatement(StarRocksParser.DropResourceGroupStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropResourceGroupStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropResourceGroupStatement(StarRocksParser.DropResourceGroupStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterResourceGroupStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterResourceGroupStatement(StarRocksParser.AlterResourceGroupStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterResourceGroupStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterResourceGroupStatement(StarRocksParser.AlterResourceGroupStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showResourceGroupStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowResourceGroupStatement(StarRocksParser.ShowResourceGroupStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showResourceGroupStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowResourceGroupStatement(StarRocksParser.ShowResourceGroupStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createResourceStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateResourceStatement(StarRocksParser.CreateResourceStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createResourceStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateResourceStatement(StarRocksParser.CreateResourceStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterResourceStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterResourceStatement(StarRocksParser.AlterResourceStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterResourceStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterResourceStatement(StarRocksParser.AlterResourceStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropResourceStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropResourceStatement(StarRocksParser.DropResourceStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropResourceStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropResourceStatement(StarRocksParser.DropResourceStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showResourceStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowResourceStatement(StarRocksParser.ShowResourceStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showResourceStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowResourceStatement(StarRocksParser.ShowResourceStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#classifier}.
	 * @param ctx the parse tree
	 */
	void enterClassifier(StarRocksParser.ClassifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#classifier}.
	 * @param ctx the parse tree
	 */
	void exitClassifier(StarRocksParser.ClassifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showFunctionsStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowFunctionsStatement(StarRocksParser.ShowFunctionsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showFunctionsStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowFunctionsStatement(StarRocksParser.ShowFunctionsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropFunctionStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropFunctionStatement(StarRocksParser.DropFunctionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropFunctionStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropFunctionStatement(StarRocksParser.DropFunctionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createFunctionStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateFunctionStatement(StarRocksParser.CreateFunctionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createFunctionStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateFunctionStatement(StarRocksParser.CreateFunctionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#typeList}.
	 * @param ctx the parse tree
	 */
	void enterTypeList(StarRocksParser.TypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#typeList}.
	 * @param ctx the parse tree
	 */
	void exitTypeList(StarRocksParser.TypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#loadStatement}.
	 * @param ctx the parse tree
	 */
	void enterLoadStatement(StarRocksParser.LoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#loadStatement}.
	 * @param ctx the parse tree
	 */
	void exitLoadStatement(StarRocksParser.LoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#labelName}.
	 * @param ctx the parse tree
	 */
	void enterLabelName(StarRocksParser.LabelNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#labelName}.
	 * @param ctx the parse tree
	 */
	void exitLabelName(StarRocksParser.LabelNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dataDescList}.
	 * @param ctx the parse tree
	 */
	void enterDataDescList(StarRocksParser.DataDescListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dataDescList}.
	 * @param ctx the parse tree
	 */
	void exitDataDescList(StarRocksParser.DataDescListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dataDesc}.
	 * @param ctx the parse tree
	 */
	void enterDataDesc(StarRocksParser.DataDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dataDesc}.
	 * @param ctx the parse tree
	 */
	void exitDataDesc(StarRocksParser.DataDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#formatProps}.
	 * @param ctx the parse tree
	 */
	void enterFormatProps(StarRocksParser.FormatPropsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#formatProps}.
	 * @param ctx the parse tree
	 */
	void exitFormatProps(StarRocksParser.FormatPropsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#brokerDesc}.
	 * @param ctx the parse tree
	 */
	void enterBrokerDesc(StarRocksParser.BrokerDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#brokerDesc}.
	 * @param ctx the parse tree
	 */
	void exitBrokerDesc(StarRocksParser.BrokerDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#resourceDesc}.
	 * @param ctx the parse tree
	 */
	void enterResourceDesc(StarRocksParser.ResourceDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#resourceDesc}.
	 * @param ctx the parse tree
	 */
	void exitResourceDesc(StarRocksParser.ResourceDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showLoadStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowLoadStatement(StarRocksParser.ShowLoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showLoadStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowLoadStatement(StarRocksParser.ShowLoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showLoadWarningsStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowLoadWarningsStatement(StarRocksParser.ShowLoadWarningsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showLoadWarningsStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowLoadWarningsStatement(StarRocksParser.ShowLoadWarningsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#cancelLoadStatement}.
	 * @param ctx the parse tree
	 */
	void enterCancelLoadStatement(StarRocksParser.CancelLoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#cancelLoadStatement}.
	 * @param ctx the parse tree
	 */
	void exitCancelLoadStatement(StarRocksParser.CancelLoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterLoadStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterLoadStatement(StarRocksParser.AlterLoadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterLoadStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterLoadStatement(StarRocksParser.AlterLoadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showAuthorStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowAuthorStatement(StarRocksParser.ShowAuthorStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showAuthorStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowAuthorStatement(StarRocksParser.ShowAuthorStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showBackendsStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowBackendsStatement(StarRocksParser.ShowBackendsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showBackendsStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowBackendsStatement(StarRocksParser.ShowBackendsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showBrokerStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowBrokerStatement(StarRocksParser.ShowBrokerStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showBrokerStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowBrokerStatement(StarRocksParser.ShowBrokerStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showCharsetStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCharsetStatement(StarRocksParser.ShowCharsetStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showCharsetStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCharsetStatement(StarRocksParser.ShowCharsetStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showCollationStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCollationStatement(StarRocksParser.ShowCollationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showCollationStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCollationStatement(StarRocksParser.ShowCollationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showDeleteStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowDeleteStatement(StarRocksParser.ShowDeleteStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showDeleteStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowDeleteStatement(StarRocksParser.ShowDeleteStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showDynamicPartitionStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowDynamicPartitionStatement(StarRocksParser.ShowDynamicPartitionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showDynamicPartitionStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowDynamicPartitionStatement(StarRocksParser.ShowDynamicPartitionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showEventsStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowEventsStatement(StarRocksParser.ShowEventsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showEventsStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowEventsStatement(StarRocksParser.ShowEventsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showEnginesStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowEnginesStatement(StarRocksParser.ShowEnginesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showEnginesStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowEnginesStatement(StarRocksParser.ShowEnginesStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showFrontendsStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowFrontendsStatement(StarRocksParser.ShowFrontendsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showFrontendsStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowFrontendsStatement(StarRocksParser.ShowFrontendsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showPluginsStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowPluginsStatement(StarRocksParser.ShowPluginsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showPluginsStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowPluginsStatement(StarRocksParser.ShowPluginsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showRepositoriesStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowRepositoriesStatement(StarRocksParser.ShowRepositoriesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showRepositoriesStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowRepositoriesStatement(StarRocksParser.ShowRepositoriesStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showOpenTableStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowOpenTableStatement(StarRocksParser.ShowOpenTableStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showOpenTableStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowOpenTableStatement(StarRocksParser.ShowOpenTableStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showPrivilegesStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowPrivilegesStatement(StarRocksParser.ShowPrivilegesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showPrivilegesStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowPrivilegesStatement(StarRocksParser.ShowPrivilegesStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showProcedureStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowProcedureStatement(StarRocksParser.ShowProcedureStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showProcedureStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowProcedureStatement(StarRocksParser.ShowProcedureStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showProcStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowProcStatement(StarRocksParser.ShowProcStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showProcStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowProcStatement(StarRocksParser.ShowProcStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showProcesslistStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowProcesslistStatement(StarRocksParser.ShowProcesslistStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showProcesslistStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowProcesslistStatement(StarRocksParser.ShowProcesslistStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showStatusStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowStatusStatement(StarRocksParser.ShowStatusStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showStatusStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowStatusStatement(StarRocksParser.ShowStatusStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showTabletStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowTabletStatement(StarRocksParser.ShowTabletStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showTabletStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowTabletStatement(StarRocksParser.ShowTabletStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showTransactionStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowTransactionStatement(StarRocksParser.ShowTransactionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showTransactionStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowTransactionStatement(StarRocksParser.ShowTransactionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showTriggersStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowTriggersStatement(StarRocksParser.ShowTriggersStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showTriggersStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowTriggersStatement(StarRocksParser.ShowTriggersStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showUserPropertyStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowUserPropertyStatement(StarRocksParser.ShowUserPropertyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showUserPropertyStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowUserPropertyStatement(StarRocksParser.ShowUserPropertyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showVariablesStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowVariablesStatement(StarRocksParser.ShowVariablesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showVariablesStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowVariablesStatement(StarRocksParser.ShowVariablesStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showWarningStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowWarningStatement(StarRocksParser.ShowWarningStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showWarningStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowWarningStatement(StarRocksParser.ShowWarningStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#helpStatement}.
	 * @param ctx the parse tree
	 */
	void enterHelpStatement(StarRocksParser.HelpStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#helpStatement}.
	 * @param ctx the parse tree
	 */
	void exitHelpStatement(StarRocksParser.HelpStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createUserStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateUserStatement(StarRocksParser.CreateUserStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createUserStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateUserStatement(StarRocksParser.CreateUserStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropUserStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropUserStatement(StarRocksParser.DropUserStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropUserStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropUserStatement(StarRocksParser.DropUserStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterUserStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterUserStatement(StarRocksParser.AlterUserStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterUserStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterUserStatement(StarRocksParser.AlterUserStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showUserStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowUserStatement(StarRocksParser.ShowUserStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showUserStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowUserStatement(StarRocksParser.ShowUserStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAllAuthentication}
	 * labeled alternative in {@link StarRocksParser#showAuthenticationStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowAllAuthentication(StarRocksParser.ShowAllAuthenticationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAllAuthentication}
	 * labeled alternative in {@link StarRocksParser#showAuthenticationStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowAllAuthentication(StarRocksParser.ShowAllAuthenticationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showAuthenticationForUser}
	 * labeled alternative in {@link StarRocksParser#showAuthenticationStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowAuthenticationForUser(StarRocksParser.ShowAuthenticationForUserContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showAuthenticationForUser}
	 * labeled alternative in {@link StarRocksParser#showAuthenticationStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowAuthenticationForUser(StarRocksParser.ShowAuthenticationForUserContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#executeAsStatement}.
	 * @param ctx the parse tree
	 */
	void enterExecuteAsStatement(StarRocksParser.ExecuteAsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#executeAsStatement}.
	 * @param ctx the parse tree
	 */
	void exitExecuteAsStatement(StarRocksParser.ExecuteAsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createRoleStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateRoleStatement(StarRocksParser.CreateRoleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createRoleStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateRoleStatement(StarRocksParser.CreateRoleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropRoleStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropRoleStatement(StarRocksParser.DropRoleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropRoleStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropRoleStatement(StarRocksParser.DropRoleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showRolesStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowRolesStatement(StarRocksParser.ShowRolesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showRolesStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowRolesStatement(StarRocksParser.ShowRolesStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grantRoleToUser}
	 * labeled alternative in {@link StarRocksParser#grantRoleStatement}.
	 * @param ctx the parse tree
	 */
	void enterGrantRoleToUser(StarRocksParser.GrantRoleToUserContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grantRoleToUser}
	 * labeled alternative in {@link StarRocksParser#grantRoleStatement}.
	 * @param ctx the parse tree
	 */
	void exitGrantRoleToUser(StarRocksParser.GrantRoleToUserContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grantRoleToRole}
	 * labeled alternative in {@link StarRocksParser#grantRoleStatement}.
	 * @param ctx the parse tree
	 */
	void enterGrantRoleToRole(StarRocksParser.GrantRoleToRoleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grantRoleToRole}
	 * labeled alternative in {@link StarRocksParser#grantRoleStatement}.
	 * @param ctx the parse tree
	 */
	void exitGrantRoleToRole(StarRocksParser.GrantRoleToRoleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revokeRoleFromUser}
	 * labeled alternative in {@link StarRocksParser#revokeRoleStatement}.
	 * @param ctx the parse tree
	 */
	void enterRevokeRoleFromUser(StarRocksParser.RevokeRoleFromUserContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revokeRoleFromUser}
	 * labeled alternative in {@link StarRocksParser#revokeRoleStatement}.
	 * @param ctx the parse tree
	 */
	void exitRevokeRoleFromUser(StarRocksParser.RevokeRoleFromUserContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revokeRoleFromRole}
	 * labeled alternative in {@link StarRocksParser#revokeRoleStatement}.
	 * @param ctx the parse tree
	 */
	void enterRevokeRoleFromRole(StarRocksParser.RevokeRoleFromRoleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revokeRoleFromRole}
	 * labeled alternative in {@link StarRocksParser#revokeRoleStatement}.
	 * @param ctx the parse tree
	 */
	void exitRevokeRoleFromRole(StarRocksParser.RevokeRoleFromRoleContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#setRoleStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetRoleStatement(StarRocksParser.SetRoleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#setRoleStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetRoleStatement(StarRocksParser.SetRoleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#setDefaultRoleStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetDefaultRoleStatement(StarRocksParser.SetDefaultRoleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#setDefaultRoleStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetDefaultRoleStatement(StarRocksParser.SetDefaultRoleStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#grantRevokeClause}.
	 * @param ctx the parse tree
	 */
	void enterGrantRevokeClause(StarRocksParser.GrantRevokeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#grantRevokeClause}.
	 * @param ctx the parse tree
	 */
	void exitGrantRevokeClause(StarRocksParser.GrantRevokeClauseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grantOnUser}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterGrantOnUser(StarRocksParser.GrantOnUserContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grantOnUser}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitGrantOnUser(StarRocksParser.GrantOnUserContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grantOnTableBrief}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterGrantOnTableBrief(StarRocksParser.GrantOnTableBriefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grantOnTableBrief}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitGrantOnTableBrief(StarRocksParser.GrantOnTableBriefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grantOnFunc}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterGrantOnFunc(StarRocksParser.GrantOnFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grantOnFunc}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitGrantOnFunc(StarRocksParser.GrantOnFuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grantOnSystem}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterGrantOnSystem(StarRocksParser.GrantOnSystemContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grantOnSystem}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitGrantOnSystem(StarRocksParser.GrantOnSystemContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grantOnPrimaryObj}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterGrantOnPrimaryObj(StarRocksParser.GrantOnPrimaryObjContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grantOnPrimaryObj}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitGrantOnPrimaryObj(StarRocksParser.GrantOnPrimaryObjContext ctx);
	/**
	 * Enter a parse tree produced by the {@code grantOnAll}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterGrantOnAll(StarRocksParser.GrantOnAllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code grantOnAll}
	 * labeled alternative in {@link StarRocksParser#grantPrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitGrantOnAll(StarRocksParser.GrantOnAllContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revokeOnUser}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterRevokeOnUser(StarRocksParser.RevokeOnUserContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revokeOnUser}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitRevokeOnUser(StarRocksParser.RevokeOnUserContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revokeOnTableBrief}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterRevokeOnTableBrief(StarRocksParser.RevokeOnTableBriefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revokeOnTableBrief}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitRevokeOnTableBrief(StarRocksParser.RevokeOnTableBriefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revokeOnFunc}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterRevokeOnFunc(StarRocksParser.RevokeOnFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revokeOnFunc}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitRevokeOnFunc(StarRocksParser.RevokeOnFuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revokeOnSystem}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterRevokeOnSystem(StarRocksParser.RevokeOnSystemContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revokeOnSystem}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitRevokeOnSystem(StarRocksParser.RevokeOnSystemContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revokeOnPrimaryObj}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterRevokeOnPrimaryObj(StarRocksParser.RevokeOnPrimaryObjContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revokeOnPrimaryObj}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitRevokeOnPrimaryObj(StarRocksParser.RevokeOnPrimaryObjContext ctx);
	/**
	 * Enter a parse tree produced by the {@code revokeOnAll}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void enterRevokeOnAll(StarRocksParser.RevokeOnAllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code revokeOnAll}
	 * labeled alternative in {@link StarRocksParser#revokePrivilegeStatement}.
	 * @param ctx the parse tree
	 */
	void exitRevokeOnAll(StarRocksParser.RevokeOnAllContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showGrantsStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowGrantsStatement(StarRocksParser.ShowGrantsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showGrantsStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowGrantsStatement(StarRocksParser.ShowGrantsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createSecurityIntegrationStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateSecurityIntegrationStatement(StarRocksParser.CreateSecurityIntegrationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createSecurityIntegrationStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateSecurityIntegrationStatement(StarRocksParser.CreateSecurityIntegrationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterSecurityIntegrationStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterSecurityIntegrationStatement(StarRocksParser.AlterSecurityIntegrationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterSecurityIntegrationStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterSecurityIntegrationStatement(StarRocksParser.AlterSecurityIntegrationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropSecurityIntegrationStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropSecurityIntegrationStatement(StarRocksParser.DropSecurityIntegrationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropSecurityIntegrationStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropSecurityIntegrationStatement(StarRocksParser.DropSecurityIntegrationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showSecurityIntegrationStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowSecurityIntegrationStatement(StarRocksParser.ShowSecurityIntegrationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showSecurityIntegrationStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowSecurityIntegrationStatement(StarRocksParser.ShowSecurityIntegrationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showCreateSecurityIntegrationStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateSecurityIntegrationStatement(StarRocksParser.ShowCreateSecurityIntegrationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showCreateSecurityIntegrationStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateSecurityIntegrationStatement(StarRocksParser.ShowCreateSecurityIntegrationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createRoleMappingStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateRoleMappingStatement(StarRocksParser.CreateRoleMappingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createRoleMappingStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateRoleMappingStatement(StarRocksParser.CreateRoleMappingStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterRoleMappingStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterRoleMappingStatement(StarRocksParser.AlterRoleMappingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterRoleMappingStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterRoleMappingStatement(StarRocksParser.AlterRoleMappingStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropRoleMappingStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropRoleMappingStatement(StarRocksParser.DropRoleMappingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropRoleMappingStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropRoleMappingStatement(StarRocksParser.DropRoleMappingStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showRoleMappingStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowRoleMappingStatement(StarRocksParser.ShowRoleMappingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showRoleMappingStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowRoleMappingStatement(StarRocksParser.ShowRoleMappingStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#refreshRoleMappingStatement}.
	 * @param ctx the parse tree
	 */
	void enterRefreshRoleMappingStatement(StarRocksParser.RefreshRoleMappingStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#refreshRoleMappingStatement}.
	 * @param ctx the parse tree
	 */
	void exitRefreshRoleMappingStatement(StarRocksParser.RefreshRoleMappingStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code authWithoutPlugin}
	 * labeled alternative in {@link StarRocksParser#authOption}.
	 * @param ctx the parse tree
	 */
	void enterAuthWithoutPlugin(StarRocksParser.AuthWithoutPluginContext ctx);
	/**
	 * Exit a parse tree produced by the {@code authWithoutPlugin}
	 * labeled alternative in {@link StarRocksParser#authOption}.
	 * @param ctx the parse tree
	 */
	void exitAuthWithoutPlugin(StarRocksParser.AuthWithoutPluginContext ctx);
	/**
	 * Enter a parse tree produced by the {@code authWithPlugin}
	 * labeled alternative in {@link StarRocksParser#authOption}.
	 * @param ctx the parse tree
	 */
	void enterAuthWithPlugin(StarRocksParser.AuthWithPluginContext ctx);
	/**
	 * Exit a parse tree produced by the {@code authWithPlugin}
	 * labeled alternative in {@link StarRocksParser#authOption}.
	 * @param ctx the parse tree
	 */
	void exitAuthWithPlugin(StarRocksParser.AuthWithPluginContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#privObjectName}.
	 * @param ctx the parse tree
	 */
	void enterPrivObjectName(StarRocksParser.PrivObjectNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#privObjectName}.
	 * @param ctx the parse tree
	 */
	void exitPrivObjectName(StarRocksParser.PrivObjectNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#privObjectNameList}.
	 * @param ctx the parse tree
	 */
	void enterPrivObjectNameList(StarRocksParser.PrivObjectNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#privObjectNameList}.
	 * @param ctx the parse tree
	 */
	void exitPrivObjectNameList(StarRocksParser.PrivObjectNameListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#privFunctionObjectNameList}.
	 * @param ctx the parse tree
	 */
	void enterPrivFunctionObjectNameList(StarRocksParser.PrivFunctionObjectNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#privFunctionObjectNameList}.
	 * @param ctx the parse tree
	 */
	void exitPrivFunctionObjectNameList(StarRocksParser.PrivFunctionObjectNameListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#privilegeTypeList}.
	 * @param ctx the parse tree
	 */
	void enterPrivilegeTypeList(StarRocksParser.PrivilegeTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#privilegeTypeList}.
	 * @param ctx the parse tree
	 */
	void exitPrivilegeTypeList(StarRocksParser.PrivilegeTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#privilegeType}.
	 * @param ctx the parse tree
	 */
	void enterPrivilegeType(StarRocksParser.PrivilegeTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#privilegeType}.
	 * @param ctx the parse tree
	 */
	void exitPrivilegeType(StarRocksParser.PrivilegeTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#privObjectType}.
	 * @param ctx the parse tree
	 */
	void enterPrivObjectType(StarRocksParser.PrivObjectTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#privObjectType}.
	 * @param ctx the parse tree
	 */
	void exitPrivObjectType(StarRocksParser.PrivObjectTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#privObjectTypePlural}.
	 * @param ctx the parse tree
	 */
	void enterPrivObjectTypePlural(StarRocksParser.PrivObjectTypePluralContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#privObjectTypePlural}.
	 * @param ctx the parse tree
	 */
	void exitPrivObjectTypePlural(StarRocksParser.PrivObjectTypePluralContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createMaskingPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateMaskingPolicyStatement(StarRocksParser.CreateMaskingPolicyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createMaskingPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateMaskingPolicyStatement(StarRocksParser.CreateMaskingPolicyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropMaskingPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropMaskingPolicyStatement(StarRocksParser.DropMaskingPolicyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropMaskingPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropMaskingPolicyStatement(StarRocksParser.DropMaskingPolicyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterMaskingPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterMaskingPolicyStatement(StarRocksParser.AlterMaskingPolicyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterMaskingPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterMaskingPolicyStatement(StarRocksParser.AlterMaskingPolicyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showMaskingPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowMaskingPolicyStatement(StarRocksParser.ShowMaskingPolicyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showMaskingPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowMaskingPolicyStatement(StarRocksParser.ShowMaskingPolicyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showCreateMaskingPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateMaskingPolicyStatement(StarRocksParser.ShowCreateMaskingPolicyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showCreateMaskingPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateMaskingPolicyStatement(StarRocksParser.ShowCreateMaskingPolicyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createRowAccessPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateRowAccessPolicyStatement(StarRocksParser.CreateRowAccessPolicyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createRowAccessPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateRowAccessPolicyStatement(StarRocksParser.CreateRowAccessPolicyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropRowAccessPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropRowAccessPolicyStatement(StarRocksParser.DropRowAccessPolicyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropRowAccessPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropRowAccessPolicyStatement(StarRocksParser.DropRowAccessPolicyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#alterRowAccessPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void enterAlterRowAccessPolicyStatement(StarRocksParser.AlterRowAccessPolicyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#alterRowAccessPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void exitAlterRowAccessPolicyStatement(StarRocksParser.AlterRowAccessPolicyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showRowAccessPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowRowAccessPolicyStatement(StarRocksParser.ShowRowAccessPolicyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showRowAccessPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowRowAccessPolicyStatement(StarRocksParser.ShowRowAccessPolicyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showCreateRowAccessPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateRowAccessPolicyStatement(StarRocksParser.ShowCreateRowAccessPolicyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showCreateRowAccessPolicyStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateRowAccessPolicyStatement(StarRocksParser.ShowCreateRowAccessPolicyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#policySignature}.
	 * @param ctx the parse tree
	 */
	void enterPolicySignature(StarRocksParser.PolicySignatureContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#policySignature}.
	 * @param ctx the parse tree
	 */
	void exitPolicySignature(StarRocksParser.PolicySignatureContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#backupStatement}.
	 * @param ctx the parse tree
	 */
	void enterBackupStatement(StarRocksParser.BackupStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#backupStatement}.
	 * @param ctx the parse tree
	 */
	void exitBackupStatement(StarRocksParser.BackupStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#cancelBackupStatement}.
	 * @param ctx the parse tree
	 */
	void enterCancelBackupStatement(StarRocksParser.CancelBackupStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#cancelBackupStatement}.
	 * @param ctx the parse tree
	 */
	void exitCancelBackupStatement(StarRocksParser.CancelBackupStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showBackupStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowBackupStatement(StarRocksParser.ShowBackupStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showBackupStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowBackupStatement(StarRocksParser.ShowBackupStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#restoreStatement}.
	 * @param ctx the parse tree
	 */
	void enterRestoreStatement(StarRocksParser.RestoreStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#restoreStatement}.
	 * @param ctx the parse tree
	 */
	void exitRestoreStatement(StarRocksParser.RestoreStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#cancelRestoreStatement}.
	 * @param ctx the parse tree
	 */
	void enterCancelRestoreStatement(StarRocksParser.CancelRestoreStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#cancelRestoreStatement}.
	 * @param ctx the parse tree
	 */
	void exitCancelRestoreStatement(StarRocksParser.CancelRestoreStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showRestoreStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowRestoreStatement(StarRocksParser.ShowRestoreStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showRestoreStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowRestoreStatement(StarRocksParser.ShowRestoreStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showSnapshotStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowSnapshotStatement(StarRocksParser.ShowSnapshotStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showSnapshotStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowSnapshotStatement(StarRocksParser.ShowSnapshotStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createRepositoryStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateRepositoryStatement(StarRocksParser.CreateRepositoryStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createRepositoryStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateRepositoryStatement(StarRocksParser.CreateRepositoryStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropRepositoryStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropRepositoryStatement(StarRocksParser.DropRepositoryStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropRepositoryStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropRepositoryStatement(StarRocksParser.DropRepositoryStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#addSqlBlackListStatement}.
	 * @param ctx the parse tree
	 */
	void enterAddSqlBlackListStatement(StarRocksParser.AddSqlBlackListStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#addSqlBlackListStatement}.
	 * @param ctx the parse tree
	 */
	void exitAddSqlBlackListStatement(StarRocksParser.AddSqlBlackListStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#delSqlBlackListStatement}.
	 * @param ctx the parse tree
	 */
	void enterDelSqlBlackListStatement(StarRocksParser.DelSqlBlackListStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#delSqlBlackListStatement}.
	 * @param ctx the parse tree
	 */
	void exitDelSqlBlackListStatement(StarRocksParser.DelSqlBlackListStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showSqlBlackListStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowSqlBlackListStatement(StarRocksParser.ShowSqlBlackListStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showSqlBlackListStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowSqlBlackListStatement(StarRocksParser.ShowSqlBlackListStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showWhiteListStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowWhiteListStatement(StarRocksParser.ShowWhiteListStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showWhiteListStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowWhiteListStatement(StarRocksParser.ShowWhiteListStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#exportStatement}.
	 * @param ctx the parse tree
	 */
	void enterExportStatement(StarRocksParser.ExportStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#exportStatement}.
	 * @param ctx the parse tree
	 */
	void exitExportStatement(StarRocksParser.ExportStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#cancelExportStatement}.
	 * @param ctx the parse tree
	 */
	void enterCancelExportStatement(StarRocksParser.CancelExportStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#cancelExportStatement}.
	 * @param ctx the parse tree
	 */
	void exitCancelExportStatement(StarRocksParser.CancelExportStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showExportStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowExportStatement(StarRocksParser.ShowExportStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showExportStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowExportStatement(StarRocksParser.ShowExportStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#installPluginStatement}.
	 * @param ctx the parse tree
	 */
	void enterInstallPluginStatement(StarRocksParser.InstallPluginStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#installPluginStatement}.
	 * @param ctx the parse tree
	 */
	void exitInstallPluginStatement(StarRocksParser.InstallPluginStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#uninstallPluginStatement}.
	 * @param ctx the parse tree
	 */
	void enterUninstallPluginStatement(StarRocksParser.UninstallPluginStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#uninstallPluginStatement}.
	 * @param ctx the parse tree
	 */
	void exitUninstallPluginStatement(StarRocksParser.UninstallPluginStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#createFileStatement}.
	 * @param ctx the parse tree
	 */
	void enterCreateFileStatement(StarRocksParser.CreateFileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#createFileStatement}.
	 * @param ctx the parse tree
	 */
	void exitCreateFileStatement(StarRocksParser.CreateFileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#dropFileStatement}.
	 * @param ctx the parse tree
	 */
	void enterDropFileStatement(StarRocksParser.DropFileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#dropFileStatement}.
	 * @param ctx the parse tree
	 */
	void exitDropFileStatement(StarRocksParser.DropFileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#showSmallFilesStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowSmallFilesStatement(StarRocksParser.ShowSmallFilesStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#showSmallFilesStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowSmallFilesStatement(StarRocksParser.ShowSmallFilesStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetStatement(StarRocksParser.SetStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetStatement(StarRocksParser.SetStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNames}
	 * labeled alternative in {@link StarRocksParser#setVar}.
	 * @param ctx the parse tree
	 */
	void enterSetNames(StarRocksParser.SetNamesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNames}
	 * labeled alternative in {@link StarRocksParser#setVar}.
	 * @param ctx the parse tree
	 */
	void exitSetNames(StarRocksParser.SetNamesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setPassword}
	 * labeled alternative in {@link StarRocksParser#setVar}.
	 * @param ctx the parse tree
	 */
	void enterSetPassword(StarRocksParser.SetPasswordContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setPassword}
	 * labeled alternative in {@link StarRocksParser#setVar}.
	 * @param ctx the parse tree
	 */
	void exitSetPassword(StarRocksParser.SetPasswordContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setUserVar}
	 * labeled alternative in {@link StarRocksParser#setVar}.
	 * @param ctx the parse tree
	 */
	void enterSetUserVar(StarRocksParser.SetUserVarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setUserVar}
	 * labeled alternative in {@link StarRocksParser#setVar}.
	 * @param ctx the parse tree
	 */
	void exitSetUserVar(StarRocksParser.SetUserVarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setSystemVar}
	 * labeled alternative in {@link StarRocksParser#setVar}.
	 * @param ctx the parse tree
	 */
	void enterSetSystemVar(StarRocksParser.SetSystemVarContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setSystemVar}
	 * labeled alternative in {@link StarRocksParser#setVar}.
	 * @param ctx the parse tree
	 */
	void exitSetSystemVar(StarRocksParser.SetSystemVarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTransaction}
	 * labeled alternative in {@link StarRocksParser#setVar}.
	 * @param ctx the parse tree
	 */
	void enterSetTransaction(StarRocksParser.SetTransactionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTransaction}
	 * labeled alternative in {@link StarRocksParser#setVar}.
	 * @param ctx the parse tree
	 */
	void exitSetTransaction(StarRocksParser.SetTransactionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#transaction_characteristics}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_characteristics(StarRocksParser.Transaction_characteristicsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#transaction_characteristics}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_characteristics(StarRocksParser.Transaction_characteristicsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#transaction_access_mode}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_access_mode(StarRocksParser.Transaction_access_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#transaction_access_mode}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_access_mode(StarRocksParser.Transaction_access_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#isolation_level}.
	 * @param ctx the parse tree
	 */
	void enterIsolation_level(StarRocksParser.Isolation_levelContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#isolation_level}.
	 * @param ctx the parse tree
	 */
	void exitIsolation_level(StarRocksParser.Isolation_levelContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#isolation_types}.
	 * @param ctx the parse tree
	 */
	void enterIsolation_types(StarRocksParser.Isolation_typesContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#isolation_types}.
	 * @param ctx the parse tree
	 */
	void exitIsolation_types(StarRocksParser.Isolation_typesContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#setExprOrDefault}.
	 * @param ctx the parse tree
	 */
	void enterSetExprOrDefault(StarRocksParser.SetExprOrDefaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#setExprOrDefault}.
	 * @param ctx the parse tree
	 */
	void exitSetExprOrDefault(StarRocksParser.SetExprOrDefaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#setUserPropertyStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetUserPropertyStatement(StarRocksParser.SetUserPropertyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#setUserPropertyStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetUserPropertyStatement(StarRocksParser.SetUserPropertyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#roleList}.
	 * @param ctx the parse tree
	 */
	void enterRoleList(StarRocksParser.RoleListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#roleList}.
	 * @param ctx the parse tree
	 */
	void exitRoleList(StarRocksParser.RoleListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#setWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetWarehouseStatement(StarRocksParser.SetWarehouseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#setWarehouseStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetWarehouseStatement(StarRocksParser.SetWarehouseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#executeScriptStatement}.
	 * @param ctx the parse tree
	 */
	void enterExecuteScriptStatement(StarRocksParser.ExecuteScriptStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#executeScriptStatement}.
	 * @param ctx the parse tree
	 */
	void exitExecuteScriptStatement(StarRocksParser.ExecuteScriptStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#unsupportedStatement}.
	 * @param ctx the parse tree
	 */
	void enterUnsupportedStatement(StarRocksParser.UnsupportedStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#unsupportedStatement}.
	 * @param ctx the parse tree
	 */
	void exitUnsupportedStatement(StarRocksParser.UnsupportedStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#lock_item}.
	 * @param ctx the parse tree
	 */
	void enterLock_item(StarRocksParser.Lock_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#lock_item}.
	 * @param ctx the parse tree
	 */
	void exitLock_item(StarRocksParser.Lock_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#lock_type}.
	 * @param ctx the parse tree
	 */
	void enterLock_type(StarRocksParser.Lock_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#lock_type}.
	 * @param ctx the parse tree
	 */
	void exitLock_type(StarRocksParser.Lock_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#queryStatement}.
	 * @param ctx the parse tree
	 */
	void enterQueryStatement(StarRocksParser.QueryStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#queryStatement}.
	 * @param ctx the parse tree
	 */
	void exitQueryStatement(StarRocksParser.QueryStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#queryRelation}.
	 * @param ctx the parse tree
	 */
	void enterQueryRelation(StarRocksParser.QueryRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#queryRelation}.
	 * @param ctx the parse tree
	 */
	void exitQueryRelation(StarRocksParser.QueryRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#withClause}.
	 * @param ctx the parse tree
	 */
	void enterWithClause(StarRocksParser.WithClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#withClause}.
	 * @param ctx the parse tree
	 */
	void exitWithClause(StarRocksParser.WithClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#queryNoWith}.
	 * @param ctx the parse tree
	 */
	void enterQueryNoWith(StarRocksParser.QueryNoWithContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#queryNoWith}.
	 * @param ctx the parse tree
	 */
	void exitQueryNoWith(StarRocksParser.QueryNoWithContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#temporalClause}.
	 * @param ctx the parse tree
	 */
	void enterTemporalClause(StarRocksParser.TemporalClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#temporalClause}.
	 * @param ctx the parse tree
	 */
	void exitTemporalClause(StarRocksParser.TemporalClauseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryWithParentheses}
	 * labeled alternative in {@link StarRocksParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterQueryWithParentheses(StarRocksParser.QueryWithParenthesesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryWithParentheses}
	 * labeled alternative in {@link StarRocksParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitQueryWithParentheses(StarRocksParser.QueryWithParenthesesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link StarRocksParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSetOperation(StarRocksParser.SetOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setOperation}
	 * labeled alternative in {@link StarRocksParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSetOperation(StarRocksParser.SetOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link StarRocksParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void enterQueryPrimaryDefault(StarRocksParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryPrimaryDefault}
	 * labeled alternative in {@link StarRocksParser#queryPrimary}.
	 * @param ctx the parse tree
	 */
	void exitQueryPrimaryDefault(StarRocksParser.QueryPrimaryDefaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#subquery}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(StarRocksParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#subquery}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(StarRocksParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#rowConstructor}.
	 * @param ctx the parse tree
	 */
	void enterRowConstructor(StarRocksParser.RowConstructorContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#rowConstructor}.
	 * @param ctx the parse tree
	 */
	void exitRowConstructor(StarRocksParser.RowConstructorContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void enterSortItem(StarRocksParser.SortItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#sortItem}.
	 * @param ctx the parse tree
	 */
	void exitSortItem(StarRocksParser.SortItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#limitElement}.
	 * @param ctx the parse tree
	 */
	void enterLimitElement(StarRocksParser.LimitElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#limitElement}.
	 * @param ctx the parse tree
	 */
	void exitLimitElement(StarRocksParser.LimitElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterQuerySpecification(StarRocksParser.QuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitQuerySpecification(StarRocksParser.QuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code from}
	 * labeled alternative in {@link StarRocksParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFrom(StarRocksParser.FromContext ctx);
	/**
	 * Exit a parse tree produced by the {@code from}
	 * labeled alternative in {@link StarRocksParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFrom(StarRocksParser.FromContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dual}
	 * labeled alternative in {@link StarRocksParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterDual(StarRocksParser.DualContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dual}
	 * labeled alternative in {@link StarRocksParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitDual(StarRocksParser.DualContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rollup}
	 * labeled alternative in {@link StarRocksParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterRollup(StarRocksParser.RollupContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rollup}
	 * labeled alternative in {@link StarRocksParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitRollup(StarRocksParser.RollupContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cube}
	 * labeled alternative in {@link StarRocksParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterCube(StarRocksParser.CubeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cube}
	 * labeled alternative in {@link StarRocksParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitCube(StarRocksParser.CubeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multipleGroupingSets}
	 * labeled alternative in {@link StarRocksParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterMultipleGroupingSets(StarRocksParser.MultipleGroupingSetsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multipleGroupingSets}
	 * labeled alternative in {@link StarRocksParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitMultipleGroupingSets(StarRocksParser.MultipleGroupingSetsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleGroupingSet}
	 * labeled alternative in {@link StarRocksParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void enterSingleGroupingSet(StarRocksParser.SingleGroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleGroupingSet}
	 * labeled alternative in {@link StarRocksParser#groupingElement}.
	 * @param ctx the parse tree
	 */
	void exitSingleGroupingSet(StarRocksParser.SingleGroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void enterGroupingSet(StarRocksParser.GroupingSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#groupingSet}.
	 * @param ctx the parse tree
	 */
	void exitGroupingSet(StarRocksParser.GroupingSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#commonTableExpression}.
	 * @param ctx the parse tree
	 */
	void enterCommonTableExpression(StarRocksParser.CommonTableExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#commonTableExpression}.
	 * @param ctx the parse tree
	 */
	void exitCommonTableExpression(StarRocksParser.CommonTableExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterSetQuantifier(StarRocksParser.SetQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitSetQuantifier(StarRocksParser.SetQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectSingle}
	 * labeled alternative in {@link StarRocksParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void enterSelectSingle(StarRocksParser.SelectSingleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectSingle}
	 * labeled alternative in {@link StarRocksParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void exitSelectSingle(StarRocksParser.SelectSingleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectAll}
	 * labeled alternative in {@link StarRocksParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void enterSelectAll(StarRocksParser.SelectAllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectAll}
	 * labeled alternative in {@link StarRocksParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void exitSelectAll(StarRocksParser.SelectAllContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#relations}.
	 * @param ctx the parse tree
	 */
	void enterRelations(StarRocksParser.RelationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#relations}.
	 * @param ctx the parse tree
	 */
	void exitRelations(StarRocksParser.RelationsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#relation}.
	 * @param ctx the parse tree
	 */
	void enterRelation(StarRocksParser.RelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#relation}.
	 * @param ctx the parse tree
	 */
	void exitRelation(StarRocksParser.RelationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableAtom}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableAtom(StarRocksParser.TableAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableAtom}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableAtom(StarRocksParser.TableAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inlineTable}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterInlineTable(StarRocksParser.InlineTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inlineTable}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitInlineTable(StarRocksParser.InlineTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryWithAlias}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryWithAlias(StarRocksParser.SubqueryWithAliasContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryWithAlias}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryWithAlias(StarRocksParser.SubqueryWithAliasContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableFunction}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterTableFunction(StarRocksParser.TableFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableFunction}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitTableFunction(StarRocksParser.TableFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code normalizedTableFunction}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterNormalizedTableFunction(StarRocksParser.NormalizedTableFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code normalizedTableFunction}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitNormalizedTableFunction(StarRocksParser.NormalizedTableFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedRelation}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedRelation(StarRocksParser.ParenthesizedRelationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedRelation}
	 * labeled alternative in {@link StarRocksParser#relationPrimary}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedRelation(StarRocksParser.ParenthesizedRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void enterJoinRelation(StarRocksParser.JoinRelationContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#joinRelation}.
	 * @param ctx the parse tree
	 */
	void exitJoinRelation(StarRocksParser.JoinRelationContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#crossOrInnerJoinType}.
	 * @param ctx the parse tree
	 */
	void enterCrossOrInnerJoinType(StarRocksParser.CrossOrInnerJoinTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#crossOrInnerJoinType}.
	 * @param ctx the parse tree
	 */
	void exitCrossOrInnerJoinType(StarRocksParser.CrossOrInnerJoinTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#outerAndSemiJoinType}.
	 * @param ctx the parse tree
	 */
	void enterOuterAndSemiJoinType(StarRocksParser.OuterAndSemiJoinTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#outerAndSemiJoinType}.
	 * @param ctx the parse tree
	 */
	void exitOuterAndSemiJoinType(StarRocksParser.OuterAndSemiJoinTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#bracketHint}.
	 * @param ctx the parse tree
	 */
	void enterBracketHint(StarRocksParser.BracketHintContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#bracketHint}.
	 * @param ctx the parse tree
	 */
	void exitBracketHint(StarRocksParser.BracketHintContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#setVarHint}.
	 * @param ctx the parse tree
	 */
	void enterSetVarHint(StarRocksParser.SetVarHintContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#setVarHint}.
	 * @param ctx the parse tree
	 */
	void exitSetVarHint(StarRocksParser.SetVarHintContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#hintMap}.
	 * @param ctx the parse tree
	 */
	void enterHintMap(StarRocksParser.HintMapContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#hintMap}.
	 * @param ctx the parse tree
	 */
	void exitHintMap(StarRocksParser.HintMapContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void enterJoinCriteria(StarRocksParser.JoinCriteriaContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#joinCriteria}.
	 * @param ctx the parse tree
	 */
	void exitJoinCriteria(StarRocksParser.JoinCriteriaContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#columnAliases}.
	 * @param ctx the parse tree
	 */
	void enterColumnAliases(StarRocksParser.ColumnAliasesContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#columnAliases}.
	 * @param ctx the parse tree
	 */
	void exitColumnAliases(StarRocksParser.ColumnAliasesContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#partitionNames}.
	 * @param ctx the parse tree
	 */
	void enterPartitionNames(StarRocksParser.PartitionNamesContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#partitionNames}.
	 * @param ctx the parse tree
	 */
	void exitPartitionNames(StarRocksParser.PartitionNamesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code keyPartitionList}
	 * labeled alternative in {@link StarRocksParser#keyPartitions}.
	 * @param ctx the parse tree
	 */
	void enterKeyPartitionList(StarRocksParser.KeyPartitionListContext ctx);
	/**
	 * Exit a parse tree produced by the {@code keyPartitionList}
	 * labeled alternative in {@link StarRocksParser#keyPartitions}.
	 * @param ctx the parse tree
	 */
	void exitKeyPartitionList(StarRocksParser.KeyPartitionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#tabletList}.
	 * @param ctx the parse tree
	 */
	void enterTabletList(StarRocksParser.TabletListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#tabletList}.
	 * @param ctx the parse tree
	 */
	void exitTabletList(StarRocksParser.TabletListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#expressionsWithDefault}.
	 * @param ctx the parse tree
	 */
	void enterExpressionsWithDefault(StarRocksParser.ExpressionsWithDefaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#expressionsWithDefault}.
	 * @param ctx the parse tree
	 */
	void exitExpressionsWithDefault(StarRocksParser.ExpressionsWithDefaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#expressionOrDefault}.
	 * @param ctx the parse tree
	 */
	void enterExpressionOrDefault(StarRocksParser.ExpressionOrDefaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#expressionOrDefault}.
	 * @param ctx the parse tree
	 */
	void exitExpressionOrDefault(StarRocksParser.ExpressionOrDefaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#mapExpressionList}.
	 * @param ctx the parse tree
	 */
	void enterMapExpressionList(StarRocksParser.MapExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#mapExpressionList}.
	 * @param ctx the parse tree
	 */
	void exitMapExpressionList(StarRocksParser.MapExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#mapExpression}.
	 * @param ctx the parse tree
	 */
	void enterMapExpression(StarRocksParser.MapExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#mapExpression}.
	 * @param ctx the parse tree
	 */
	void exitMapExpression(StarRocksParser.MapExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#expressionSingleton}.
	 * @param ctx the parse tree
	 */
	void enterExpressionSingleton(StarRocksParser.ExpressionSingletonContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#expressionSingleton}.
	 * @param ctx the parse tree
	 */
	void exitExpressionSingleton(StarRocksParser.ExpressionSingletonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expressionDefault}
	 * labeled alternative in {@link StarRocksParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpressionDefault(StarRocksParser.ExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expressionDefault}
	 * labeled alternative in {@link StarRocksParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpressionDefault(StarRocksParser.ExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link StarRocksParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalNot(StarRocksParser.LogicalNotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link StarRocksParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalNot(StarRocksParser.LogicalNotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link StarRocksParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalBinary(StarRocksParser.LogicalBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link StarRocksParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalBinary(StarRocksParser.LogicalBinaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(StarRocksParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(StarRocksParser.ExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link StarRocksParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterComparison(StarRocksParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link StarRocksParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitComparison(StarRocksParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanExpressionDefault}
	 * labeled alternative in {@link StarRocksParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanExpressionDefault(StarRocksParser.BooleanExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanExpressionDefault}
	 * labeled alternative in {@link StarRocksParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanExpressionDefault(StarRocksParser.BooleanExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isNull}
	 * labeled alternative in {@link StarRocksParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterIsNull(StarRocksParser.IsNullContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isNull}
	 * labeled alternative in {@link StarRocksParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitIsNull(StarRocksParser.IsNullContext ctx);
	/**
	 * Enter a parse tree produced by the {@code scalarSubquery}
	 * labeled alternative in {@link StarRocksParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterScalarSubquery(StarRocksParser.ScalarSubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code scalarSubquery}
	 * labeled alternative in {@link StarRocksParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitScalarSubquery(StarRocksParser.ScalarSubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(StarRocksParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(StarRocksParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#tupleInSubquery}.
	 * @param ctx the parse tree
	 */
	void enterTupleInSubquery(StarRocksParser.TupleInSubqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#tupleInSubquery}.
	 * @param ctx the parse tree
	 */
	void exitTupleInSubquery(StarRocksParser.TupleInSubqueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inSubquery}
	 * labeled alternative in {@link StarRocksParser#predicateOperations}.
	 * @param ctx the parse tree
	 */
	void enterInSubquery(StarRocksParser.InSubqueryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inSubquery}
	 * labeled alternative in {@link StarRocksParser#predicateOperations}.
	 * @param ctx the parse tree
	 */
	void exitInSubquery(StarRocksParser.InSubqueryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inList}
	 * labeled alternative in {@link StarRocksParser#predicateOperations}.
	 * @param ctx the parse tree
	 */
	void enterInList(StarRocksParser.InListContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inList}
	 * labeled alternative in {@link StarRocksParser#predicateOperations}.
	 * @param ctx the parse tree
	 */
	void exitInList(StarRocksParser.InListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code between}
	 * labeled alternative in {@link StarRocksParser#predicateOperations}.
	 * @param ctx the parse tree
	 */
	void enterBetween(StarRocksParser.BetweenContext ctx);
	/**
	 * Exit a parse tree produced by the {@code between}
	 * labeled alternative in {@link StarRocksParser#predicateOperations}.
	 * @param ctx the parse tree
	 */
	void exitBetween(StarRocksParser.BetweenContext ctx);
	/**
	 * Enter a parse tree produced by the {@code like}
	 * labeled alternative in {@link StarRocksParser#predicateOperations}.
	 * @param ctx the parse tree
	 */
	void enterLike(StarRocksParser.LikeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code like}
	 * labeled alternative in {@link StarRocksParser#predicateOperations}.
	 * @param ctx the parse tree
	 */
	void exitLike(StarRocksParser.LikeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link StarRocksParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterValueExpressionDefault(StarRocksParser.ValueExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link StarRocksParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitValueExpressionDefault(StarRocksParser.ValueExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link StarRocksParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticBinary(StarRocksParser.ArithmeticBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link StarRocksParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticBinary(StarRocksParser.ArithmeticBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterDereference(StarRocksParser.DereferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dereference}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitDereference(StarRocksParser.DereferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCase(StarRocksParser.SimpleCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCase(StarRocksParser.SimpleCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arrowExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterArrowExpression(StarRocksParser.ArrowExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arrowExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitArrowExpression(StarRocksParser.ArrowExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code odbcFunctionCallExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterOdbcFunctionCallExpression(StarRocksParser.OdbcFunctionCallExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code odbcFunctionCallExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitOdbcFunctionCallExpression(StarRocksParser.OdbcFunctionCallExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnRef}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterColumnRef(StarRocksParser.ColumnRefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnRef}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitColumnRef(StarRocksParser.ColumnRefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code systemVariableExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSystemVariableExpression(StarRocksParser.SystemVariableExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code systemVariableExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSystemVariableExpression(StarRocksParser.SystemVariableExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code convert}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterConvert(StarRocksParser.ConvertContext ctx);
	/**
	 * Exit a parse tree produced by the {@code convert}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitConvert(StarRocksParser.ConvertContext ctx);
	/**
	 * Enter a parse tree produced by the {@code concat}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterConcat(StarRocksParser.ConcatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code concat}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitConcat(StarRocksParser.ConcatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryExpression(StarRocksParser.SubqueryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryExpression(StarRocksParser.SubqueryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lambdaFunctionExpr}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambdaFunctionExpr(StarRocksParser.LambdaFunctionExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lambdaFunctionExpr}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambdaFunctionExpr(StarRocksParser.LambdaFunctionExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code collectionSubscript}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCollectionSubscript(StarRocksParser.CollectionSubscriptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code collectionSubscript}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCollectionSubscript(StarRocksParser.CollectionSubscriptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code literal}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(StarRocksParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code literal}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(StarRocksParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cast}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCast(StarRocksParser.CastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCast(StarRocksParser.CastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code collate}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCollate(StarRocksParser.CollateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code collate}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCollate(StarRocksParser.CollateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(StarRocksParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(StarRocksParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code userVariableExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUserVariableExpression(StarRocksParser.UserVariableExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code userVariableExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUserVariableExpression(StarRocksParser.UserVariableExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arrayConstructor}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterArrayConstructor(StarRocksParser.ArrayConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arrayConstructor}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitArrayConstructor(StarRocksParser.ArrayConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mapConstructor}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterMapConstructor(StarRocksParser.MapConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mapConstructor}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitMapConstructor(StarRocksParser.MapConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arraySlice}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterArraySlice(StarRocksParser.ArraySliceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arraySlice}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitArraySlice(StarRocksParser.ArraySliceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCallExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallExpression(StarRocksParser.FunctionCallExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCallExpression}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallExpression(StarRocksParser.FunctionCallExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exists}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterExists(StarRocksParser.ExistsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exists}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitExists(StarRocksParser.ExistsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSearchedCase(StarRocksParser.SearchedCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSearchedCase(StarRocksParser.SearchedCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticUnary(StarRocksParser.ArithmeticUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link StarRocksParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticUnary(StarRocksParser.ArithmeticUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void enterNullLiteral(StarRocksParser.NullLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void exitNullLiteral(StarRocksParser.NullLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(StarRocksParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(StarRocksParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(StarRocksParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(StarRocksParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dateLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void enterDateLiteral(StarRocksParser.DateLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dateLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void exitDateLiteral(StarRocksParser.DateLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(StarRocksParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(StarRocksParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void enterIntervalLiteral(StarRocksParser.IntervalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void exitIntervalLiteral(StarRocksParser.IntervalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unitBoundaryLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnitBoundaryLiteral(StarRocksParser.UnitBoundaryLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unitBoundaryLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnitBoundaryLiteral(StarRocksParser.UnitBoundaryLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void enterBinaryLiteral(StarRocksParser.BinaryLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryLiteral}
	 * labeled alternative in {@link StarRocksParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void exitBinaryLiteral(StarRocksParser.BinaryLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code extract}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterExtract(StarRocksParser.ExtractContext ctx);
	/**
	 * Exit a parse tree produced by the {@code extract}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitExtract(StarRocksParser.ExtractContext ctx);
	/**
	 * Enter a parse tree produced by the {@code groupingOperation}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterGroupingOperation(StarRocksParser.GroupingOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code groupingOperation}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitGroupingOperation(StarRocksParser.GroupingOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code informationFunction}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterInformationFunction(StarRocksParser.InformationFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code informationFunction}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitInformationFunction(StarRocksParser.InformationFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code specialDateTime}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterSpecialDateTime(StarRocksParser.SpecialDateTimeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code specialDateTime}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitSpecialDateTime(StarRocksParser.SpecialDateTimeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code specialFunction}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterSpecialFunction(StarRocksParser.SpecialFunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code specialFunction}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitSpecialFunction(StarRocksParser.SpecialFunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aggregationFunctionCall}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterAggregationFunctionCall(StarRocksParser.AggregationFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aggregationFunctionCall}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitAggregationFunctionCall(StarRocksParser.AggregationFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code windowFunctionCall}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterWindowFunctionCall(StarRocksParser.WindowFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code windowFunctionCall}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitWindowFunctionCall(StarRocksParser.WindowFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleFunctionCall}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterSimpleFunctionCall(StarRocksParser.SimpleFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleFunctionCall}
	 * labeled alternative in {@link StarRocksParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitSimpleFunctionCall(StarRocksParser.SimpleFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#aggregationFunction}.
	 * @param ctx the parse tree
	 */
	void enterAggregationFunction(StarRocksParser.AggregationFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#aggregationFunction}.
	 * @param ctx the parse tree
	 */
	void exitAggregationFunction(StarRocksParser.AggregationFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#userVariable}.
	 * @param ctx the parse tree
	 */
	void enterUserVariable(StarRocksParser.UserVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#userVariable}.
	 * @param ctx the parse tree
	 */
	void exitUserVariable(StarRocksParser.UserVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#systemVariable}.
	 * @param ctx the parse tree
	 */
	void enterSystemVariable(StarRocksParser.SystemVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#systemVariable}.
	 * @param ctx the parse tree
	 */
	void exitSystemVariable(StarRocksParser.SystemVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#columnReference}.
	 * @param ctx the parse tree
	 */
	void enterColumnReference(StarRocksParser.ColumnReferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#columnReference}.
	 * @param ctx the parse tree
	 */
	void exitColumnReference(StarRocksParser.ColumnReferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#informationFunctionExpression}.
	 * @param ctx the parse tree
	 */
	void enterInformationFunctionExpression(StarRocksParser.InformationFunctionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#informationFunctionExpression}.
	 * @param ctx the parse tree
	 */
	void exitInformationFunctionExpression(StarRocksParser.InformationFunctionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#specialDateTimeExpression}.
	 * @param ctx the parse tree
	 */
	void enterSpecialDateTimeExpression(StarRocksParser.SpecialDateTimeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#specialDateTimeExpression}.
	 * @param ctx the parse tree
	 */
	void exitSpecialDateTimeExpression(StarRocksParser.SpecialDateTimeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#specialFunctionExpression}.
	 * @param ctx the parse tree
	 */
	void enterSpecialFunctionExpression(StarRocksParser.SpecialFunctionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#specialFunctionExpression}.
	 * @param ctx the parse tree
	 */
	void exitSpecialFunctionExpression(StarRocksParser.SpecialFunctionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#windowFunction}.
	 * @param ctx the parse tree
	 */
	void enterWindowFunction(StarRocksParser.WindowFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#windowFunction}.
	 * @param ctx the parse tree
	 */
	void exitWindowFunction(StarRocksParser.WindowFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(StarRocksParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(StarRocksParser.WhenClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#over}.
	 * @param ctx the parse tree
	 */
	void enterOver(StarRocksParser.OverContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#over}.
	 * @param ctx the parse tree
	 */
	void exitOver(StarRocksParser.OverContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#ignoreNulls}.
	 * @param ctx the parse tree
	 */
	void enterIgnoreNulls(StarRocksParser.IgnoreNullsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#ignoreNulls}.
	 * @param ctx the parse tree
	 */
	void exitIgnoreNulls(StarRocksParser.IgnoreNullsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void enterWindowFrame(StarRocksParser.WindowFrameContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#windowFrame}.
	 * @param ctx the parse tree
	 */
	void exitWindowFrame(StarRocksParser.WindowFrameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unboundedFrame}
	 * labeled alternative in {@link StarRocksParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterUnboundedFrame(StarRocksParser.UnboundedFrameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unboundedFrame}
	 * labeled alternative in {@link StarRocksParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitUnboundedFrame(StarRocksParser.UnboundedFrameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentRowBound}
	 * labeled alternative in {@link StarRocksParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterCurrentRowBound(StarRocksParser.CurrentRowBoundContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentRowBound}
	 * labeled alternative in {@link StarRocksParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitCurrentRowBound(StarRocksParser.CurrentRowBoundContext ctx);
	/**
	 * Enter a parse tree produced by the {@code boundedFrame}
	 * labeled alternative in {@link StarRocksParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void enterBoundedFrame(StarRocksParser.BoundedFrameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code boundedFrame}
	 * labeled alternative in {@link StarRocksParser#frameBound}.
	 * @param ctx the parse tree
	 */
	void exitBoundedFrame(StarRocksParser.BoundedFrameContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#tableDesc}.
	 * @param ctx the parse tree
	 */
	void enterTableDesc(StarRocksParser.TableDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#tableDesc}.
	 * @param ctx the parse tree
	 */
	void exitTableDesc(StarRocksParser.TableDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#restoreTableDesc}.
	 * @param ctx the parse tree
	 */
	void enterRestoreTableDesc(StarRocksParser.RestoreTableDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#restoreTableDesc}.
	 * @param ctx the parse tree
	 */
	void exitRestoreTableDesc(StarRocksParser.RestoreTableDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#explainDesc}.
	 * @param ctx the parse tree
	 */
	void enterExplainDesc(StarRocksParser.ExplainDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#explainDesc}.
	 * @param ctx the parse tree
	 */
	void exitExplainDesc(StarRocksParser.ExplainDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#optimizerTrace}.
	 * @param ctx the parse tree
	 */
	void enterOptimizerTrace(StarRocksParser.OptimizerTraceContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#optimizerTrace}.
	 * @param ctx the parse tree
	 */
	void exitOptimizerTrace(StarRocksParser.OptimizerTraceContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#partitionDesc}.
	 * @param ctx the parse tree
	 */
	void enterPartitionDesc(StarRocksParser.PartitionDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#partitionDesc}.
	 * @param ctx the parse tree
	 */
	void exitPartitionDesc(StarRocksParser.PartitionDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#listPartitionDesc}.
	 * @param ctx the parse tree
	 */
	void enterListPartitionDesc(StarRocksParser.ListPartitionDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#listPartitionDesc}.
	 * @param ctx the parse tree
	 */
	void exitListPartitionDesc(StarRocksParser.ListPartitionDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#singleItemListPartitionDesc}.
	 * @param ctx the parse tree
	 */
	void enterSingleItemListPartitionDesc(StarRocksParser.SingleItemListPartitionDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#singleItemListPartitionDesc}.
	 * @param ctx the parse tree
	 */
	void exitSingleItemListPartitionDesc(StarRocksParser.SingleItemListPartitionDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#multiItemListPartitionDesc}.
	 * @param ctx the parse tree
	 */
	void enterMultiItemListPartitionDesc(StarRocksParser.MultiItemListPartitionDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#multiItemListPartitionDesc}.
	 * @param ctx the parse tree
	 */
	void exitMultiItemListPartitionDesc(StarRocksParser.MultiItemListPartitionDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#stringList}.
	 * @param ctx the parse tree
	 */
	void enterStringList(StarRocksParser.StringListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#stringList}.
	 * @param ctx the parse tree
	 */
	void exitStringList(StarRocksParser.StringListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#rangePartitionDesc}.
	 * @param ctx the parse tree
	 */
	void enterRangePartitionDesc(StarRocksParser.RangePartitionDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#rangePartitionDesc}.
	 * @param ctx the parse tree
	 */
	void exitRangePartitionDesc(StarRocksParser.RangePartitionDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#singleRangePartition}.
	 * @param ctx the parse tree
	 */
	void enterSingleRangePartition(StarRocksParser.SingleRangePartitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#singleRangePartition}.
	 * @param ctx the parse tree
	 */
	void exitSingleRangePartition(StarRocksParser.SingleRangePartitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#multiRangePartition}.
	 * @param ctx the parse tree
	 */
	void enterMultiRangePartition(StarRocksParser.MultiRangePartitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#multiRangePartition}.
	 * @param ctx the parse tree
	 */
	void exitMultiRangePartition(StarRocksParser.MultiRangePartitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#partitionRangeDesc}.
	 * @param ctx the parse tree
	 */
	void enterPartitionRangeDesc(StarRocksParser.PartitionRangeDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#partitionRangeDesc}.
	 * @param ctx the parse tree
	 */
	void exitPartitionRangeDesc(StarRocksParser.PartitionRangeDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#partitionKeyDesc}.
	 * @param ctx the parse tree
	 */
	void enterPartitionKeyDesc(StarRocksParser.PartitionKeyDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#partitionKeyDesc}.
	 * @param ctx the parse tree
	 */
	void exitPartitionKeyDesc(StarRocksParser.PartitionKeyDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#partitionValueList}.
	 * @param ctx the parse tree
	 */
	void enterPartitionValueList(StarRocksParser.PartitionValueListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#partitionValueList}.
	 * @param ctx the parse tree
	 */
	void exitPartitionValueList(StarRocksParser.PartitionValueListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#keyPartition}.
	 * @param ctx the parse tree
	 */
	void enterKeyPartition(StarRocksParser.KeyPartitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#keyPartition}.
	 * @param ctx the parse tree
	 */
	void exitKeyPartition(StarRocksParser.KeyPartitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#partitionValue}.
	 * @param ctx the parse tree
	 */
	void enterPartitionValue(StarRocksParser.PartitionValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#partitionValue}.
	 * @param ctx the parse tree
	 */
	void exitPartitionValue(StarRocksParser.PartitionValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#distributionClause}.
	 * @param ctx the parse tree
	 */
	void enterDistributionClause(StarRocksParser.DistributionClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#distributionClause}.
	 * @param ctx the parse tree
	 */
	void exitDistributionClause(StarRocksParser.DistributionClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#distributionDesc}.
	 * @param ctx the parse tree
	 */
	void enterDistributionDesc(StarRocksParser.DistributionDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#distributionDesc}.
	 * @param ctx the parse tree
	 */
	void exitDistributionDesc(StarRocksParser.DistributionDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#refreshSchemeDesc}.
	 * @param ctx the parse tree
	 */
	void enterRefreshSchemeDesc(StarRocksParser.RefreshSchemeDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#refreshSchemeDesc}.
	 * @param ctx the parse tree
	 */
	void exitRefreshSchemeDesc(StarRocksParser.RefreshSchemeDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#properties}.
	 * @param ctx the parse tree
	 */
	void enterProperties(StarRocksParser.PropertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#properties}.
	 * @param ctx the parse tree
	 */
	void exitProperties(StarRocksParser.PropertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#extProperties}.
	 * @param ctx the parse tree
	 */
	void enterExtProperties(StarRocksParser.ExtPropertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#extProperties}.
	 * @param ctx the parse tree
	 */
	void exitExtProperties(StarRocksParser.ExtPropertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#propertyList}.
	 * @param ctx the parse tree
	 */
	void enterPropertyList(StarRocksParser.PropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#propertyList}.
	 * @param ctx the parse tree
	 */
	void exitPropertyList(StarRocksParser.PropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#userPropertyList}.
	 * @param ctx the parse tree
	 */
	void enterUserPropertyList(StarRocksParser.UserPropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#userPropertyList}.
	 * @param ctx the parse tree
	 */
	void exitUserPropertyList(StarRocksParser.UserPropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#property}.
	 * @param ctx the parse tree
	 */
	void enterProperty(StarRocksParser.PropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#property}.
	 * @param ctx the parse tree
	 */
	void exitProperty(StarRocksParser.PropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#varType}.
	 * @param ctx the parse tree
	 */
	void enterVarType(StarRocksParser.VarTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#varType}.
	 * @param ctx the parse tree
	 */
	void exitVarType(StarRocksParser.VarTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#comment}.
	 * @param ctx the parse tree
	 */
	void enterComment(StarRocksParser.CommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#comment}.
	 * @param ctx the parse tree
	 */
	void exitComment(StarRocksParser.CommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#outfile}.
	 * @param ctx the parse tree
	 */
	void enterOutfile(StarRocksParser.OutfileContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#outfile}.
	 * @param ctx the parse tree
	 */
	void exitOutfile(StarRocksParser.OutfileContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void enterFileFormat(StarRocksParser.FileFormatContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#fileFormat}.
	 * @param ctx the parse tree
	 */
	void exitFileFormat(StarRocksParser.FileFormatContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#string}.
	 * @param ctx the parse tree
	 */
	void enterString(StarRocksParser.StringContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#string}.
	 * @param ctx the parse tree
	 */
	void exitString(StarRocksParser.StringContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#binary}.
	 * @param ctx the parse tree
	 */
	void enterBinary(StarRocksParser.BinaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#binary}.
	 * @param ctx the parse tree
	 */
	void exitBinary(StarRocksParser.BinaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(StarRocksParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(StarRocksParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(StarRocksParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(StarRocksParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#interval}.
	 * @param ctx the parse tree
	 */
	void enterInterval(StarRocksParser.IntervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#interval}.
	 * @param ctx the parse tree
	 */
	void exitInterval(StarRocksParser.IntervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#unitIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterUnitIdentifier(StarRocksParser.UnitIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#unitIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitUnitIdentifier(StarRocksParser.UnitIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#unitBoundary}.
	 * @param ctx the parse tree
	 */
	void enterUnitBoundary(StarRocksParser.UnitBoundaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#unitBoundary}.
	 * @param ctx the parse tree
	 */
	void exitUnitBoundary(StarRocksParser.UnitBoundaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(StarRocksParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(StarRocksParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#arrayType}.
	 * @param ctx the parse tree
	 */
	void enterArrayType(StarRocksParser.ArrayTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#arrayType}.
	 * @param ctx the parse tree
	 */
	void exitArrayType(StarRocksParser.ArrayTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#mapType}.
	 * @param ctx the parse tree
	 */
	void enterMapType(StarRocksParser.MapTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#mapType}.
	 * @param ctx the parse tree
	 */
	void exitMapType(StarRocksParser.MapTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#subfieldDesc}.
	 * @param ctx the parse tree
	 */
	void enterSubfieldDesc(StarRocksParser.SubfieldDescContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#subfieldDesc}.
	 * @param ctx the parse tree
	 */
	void exitSubfieldDesc(StarRocksParser.SubfieldDescContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#subfieldDescs}.
	 * @param ctx the parse tree
	 */
	void enterSubfieldDescs(StarRocksParser.SubfieldDescsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#subfieldDescs}.
	 * @param ctx the parse tree
	 */
	void exitSubfieldDescs(StarRocksParser.SubfieldDescsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#structType}.
	 * @param ctx the parse tree
	 */
	void enterStructType(StarRocksParser.StructTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#structType}.
	 * @param ctx the parse tree
	 */
	void exitStructType(StarRocksParser.StructTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(StarRocksParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(StarRocksParser.TypeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#baseType}.
	 * @param ctx the parse tree
	 */
	void enterBaseType(StarRocksParser.BaseTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#baseType}.
	 * @param ctx the parse tree
	 */
	void exitBaseType(StarRocksParser.BaseTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#decimalType}.
	 * @param ctx the parse tree
	 */
	void enterDecimalType(StarRocksParser.DecimalTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#decimalType}.
	 * @param ctx the parse tree
	 */
	void exitDecimalType(StarRocksParser.DecimalTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(StarRocksParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(StarRocksParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link StarRocksParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedIdentifier(StarRocksParser.UnquotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link StarRocksParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedIdentifier(StarRocksParser.UnquotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code digitIdentifier}
	 * labeled alternative in {@link StarRocksParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterDigitIdentifier(StarRocksParser.DigitIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code digitIdentifier}
	 * labeled alternative in {@link StarRocksParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitDigitIdentifier(StarRocksParser.DigitIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code backQuotedIdentifier}
	 * labeled alternative in {@link StarRocksParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterBackQuotedIdentifier(StarRocksParser.BackQuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code backQuotedIdentifier}
	 * labeled alternative in {@link StarRocksParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitBackQuotedIdentifier(StarRocksParser.BackQuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierList(StarRocksParser.IdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierList(StarRocksParser.IdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#identifierOrString}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierOrString(StarRocksParser.IdentifierOrStringContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#identifierOrString}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierOrString(StarRocksParser.IdentifierOrStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#identifierOrStringList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierOrStringList(StarRocksParser.IdentifierOrStringListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#identifierOrStringList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierOrStringList(StarRocksParser.IdentifierOrStringListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#identifierOrStringOrStar}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierOrStringOrStar(StarRocksParser.IdentifierOrStringOrStarContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#identifierOrStringOrStar}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierOrStringOrStar(StarRocksParser.IdentifierOrStringOrStarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code userWithoutHost}
	 * labeled alternative in {@link StarRocksParser#user}.
	 * @param ctx the parse tree
	 */
	void enterUserWithoutHost(StarRocksParser.UserWithoutHostContext ctx);
	/**
	 * Exit a parse tree produced by the {@code userWithoutHost}
	 * labeled alternative in {@link StarRocksParser#user}.
	 * @param ctx the parse tree
	 */
	void exitUserWithoutHost(StarRocksParser.UserWithoutHostContext ctx);
	/**
	 * Enter a parse tree produced by the {@code userWithHost}
	 * labeled alternative in {@link StarRocksParser#user}.
	 * @param ctx the parse tree
	 */
	void enterUserWithHost(StarRocksParser.UserWithHostContext ctx);
	/**
	 * Exit a parse tree produced by the {@code userWithHost}
	 * labeled alternative in {@link StarRocksParser#user}.
	 * @param ctx the parse tree
	 */
	void exitUserWithHost(StarRocksParser.UserWithHostContext ctx);
	/**
	 * Enter a parse tree produced by the {@code userWithHostAndBlanket}
	 * labeled alternative in {@link StarRocksParser#user}.
	 * @param ctx the parse tree
	 */
	void enterUserWithHostAndBlanket(StarRocksParser.UserWithHostAndBlanketContext ctx);
	/**
	 * Exit a parse tree produced by the {@code userWithHostAndBlanket}
	 * labeled alternative in {@link StarRocksParser#user}.
	 * @param ctx the parse tree
	 */
	void exitUserWithHostAndBlanket(StarRocksParser.UserWithHostAndBlanketContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(StarRocksParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(StarRocksParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#assignmentList}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentList(StarRocksParser.AssignmentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#assignmentList}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentList(StarRocksParser.AssignmentListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decimalValue}
	 * labeled alternative in {@link StarRocksParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDecimalValue(StarRocksParser.DecimalValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decimalValue}
	 * labeled alternative in {@link StarRocksParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDecimalValue(StarRocksParser.DecimalValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleValue}
	 * labeled alternative in {@link StarRocksParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDoubleValue(StarRocksParser.DoubleValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleValue}
	 * labeled alternative in {@link StarRocksParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDoubleValue(StarRocksParser.DoubleValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerValue}
	 * labeled alternative in {@link StarRocksParser#number}.
	 * @param ctx the parse tree
	 */
	void enterIntegerValue(StarRocksParser.IntegerValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerValue}
	 * labeled alternative in {@link StarRocksParser#number}.
	 * @param ctx the parse tree
	 */
	void exitIntegerValue(StarRocksParser.IntegerValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link StarRocksParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void enterNonReserved(StarRocksParser.NonReservedContext ctx);
	/**
	 * Exit a parse tree produced by {@link StarRocksParser#nonReserved}.
	 * @param ctx the parse tree
	 */
	void exitNonReserved(StarRocksParser.NonReservedContext ctx);
}