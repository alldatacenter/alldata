// Generated from HiveSql.g4 by ANTLR 4.7.2

package com.platform.common.antrl4;
 
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link HiveSqlParser}.
 */
public interface HiveSqlListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(HiveSqlParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(HiveSqlParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(HiveSqlParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(HiveSqlParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#begin_end_block}.
	 * @param ctx the parse tree
	 */
	void enterBegin_end_block(HiveSqlParser.Begin_end_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#begin_end_block}.
	 * @param ctx the parse tree
	 */
	void exitBegin_end_block(HiveSqlParser.Begin_end_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#single_block_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSingle_block_stmt(HiveSqlParser.Single_block_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#single_block_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSingle_block_stmt(HiveSqlParser.Single_block_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#block_end}.
	 * @param ctx the parse tree
	 */
	void enterBlock_end(HiveSqlParser.Block_endContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#block_end}.
	 * @param ctx the parse tree
	 */
	void exitBlock_end(HiveSqlParser.Block_endContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#proc_block}.
	 * @param ctx the parse tree
	 */
	void enterProc_block(HiveSqlParser.Proc_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#proc_block}.
	 * @param ctx the parse tree
	 */
	void exitProc_block(HiveSqlParser.Proc_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(HiveSqlParser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(HiveSqlParser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#semicolon_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSemicolon_stmt(HiveSqlParser.Semicolon_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#semicolon_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSemicolon_stmt(HiveSqlParser.Semicolon_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#exception_block}.
	 * @param ctx the parse tree
	 */
	void enterException_block(HiveSqlParser.Exception_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#exception_block}.
	 * @param ctx the parse tree
	 */
	void exitException_block(HiveSqlParser.Exception_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#exception_block_item}.
	 * @param ctx the parse tree
	 */
	void enterException_block_item(HiveSqlParser.Exception_block_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#exception_block_item}.
	 * @param ctx the parse tree
	 */
	void exitException_block_item(HiveSqlParser.Exception_block_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#null_stmt}.
	 * @param ctx the parse tree
	 */
	void enterNull_stmt(HiveSqlParser.Null_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#null_stmt}.
	 * @param ctx the parse tree
	 */
	void exitNull_stmt(HiveSqlParser.Null_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_stmt}.
	 * @param ctx the parse tree
	 */
	void enterExpr_stmt(HiveSqlParser.Expr_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_stmt}.
	 * @param ctx the parse tree
	 */
	void exitExpr_stmt(HiveSqlParser.Expr_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#assignment_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_stmt(HiveSqlParser.Assignment_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#assignment_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_stmt(HiveSqlParser.Assignment_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#assignment_stmt_item}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_stmt_item(HiveSqlParser.Assignment_stmt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#assignment_stmt_item}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_stmt_item(HiveSqlParser.Assignment_stmt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#assignment_stmt_single_item}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_stmt_single_item(HiveSqlParser.Assignment_stmt_single_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#assignment_stmt_single_item}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_stmt_single_item(HiveSqlParser.Assignment_stmt_single_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#assignment_stmt_multiple_item}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_stmt_multiple_item(HiveSqlParser.Assignment_stmt_multiple_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#assignment_stmt_multiple_item}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_stmt_multiple_item(HiveSqlParser.Assignment_stmt_multiple_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#assignment_stmt_select_item}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_stmt_select_item(HiveSqlParser.Assignment_stmt_select_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#assignment_stmt_select_item}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_stmt_select_item(HiveSqlParser.Assignment_stmt_select_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#allocate_cursor_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAllocate_cursor_stmt(HiveSqlParser.Allocate_cursor_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#allocate_cursor_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAllocate_cursor_stmt(HiveSqlParser.Allocate_cursor_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#associate_locator_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAssociate_locator_stmt(HiveSqlParser.Associate_locator_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#associate_locator_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAssociate_locator_stmt(HiveSqlParser.Associate_locator_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#begin_transaction_stmt}.
	 * @param ctx the parse tree
	 */
	void enterBegin_transaction_stmt(HiveSqlParser.Begin_transaction_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#begin_transaction_stmt}.
	 * @param ctx the parse tree
	 */
	void exitBegin_transaction_stmt(HiveSqlParser.Begin_transaction_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#break_stmt}.
	 * @param ctx the parse tree
	 */
	void enterBreak_stmt(HiveSqlParser.Break_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#break_stmt}.
	 * @param ctx the parse tree
	 */
	void exitBreak_stmt(HiveSqlParser.Break_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#call_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCall_stmt(HiveSqlParser.Call_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#call_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCall_stmt(HiveSqlParser.Call_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#declare_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_stmt(HiveSqlParser.Declare_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#declare_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_stmt(HiveSqlParser.Declare_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#declare_block}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_block(HiveSqlParser.Declare_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#declare_block}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_block(HiveSqlParser.Declare_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#declare_block_inplace}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_block_inplace(HiveSqlParser.Declare_block_inplaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#declare_block_inplace}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_block_inplace(HiveSqlParser.Declare_block_inplaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#declare_stmt_item}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_stmt_item(HiveSqlParser.Declare_stmt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#declare_stmt_item}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_stmt_item(HiveSqlParser.Declare_stmt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#declare_var_item}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_var_item(HiveSqlParser.Declare_var_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#declare_var_item}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_var_item(HiveSqlParser.Declare_var_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#declare_condition_item}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_condition_item(HiveSqlParser.Declare_condition_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#declare_condition_item}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_condition_item(HiveSqlParser.Declare_condition_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#declare_cursor_item}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_cursor_item(HiveSqlParser.Declare_cursor_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#declare_cursor_item}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_cursor_item(HiveSqlParser.Declare_cursor_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#cursor_with_return}.
	 * @param ctx the parse tree
	 */
	void enterCursor_with_return(HiveSqlParser.Cursor_with_returnContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#cursor_with_return}.
	 * @param ctx the parse tree
	 */
	void exitCursor_with_return(HiveSqlParser.Cursor_with_returnContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#cursor_without_return}.
	 * @param ctx the parse tree
	 */
	void enterCursor_without_return(HiveSqlParser.Cursor_without_returnContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#cursor_without_return}.
	 * @param ctx the parse tree
	 */
	void exitCursor_without_return(HiveSqlParser.Cursor_without_returnContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#declare_handler_item}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_handler_item(HiveSqlParser.Declare_handler_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#declare_handler_item}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_handler_item(HiveSqlParser.Declare_handler_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#declare_temporary_table_item}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_temporary_table_item(HiveSqlParser.Declare_temporary_table_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#declare_temporary_table_item}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_temporary_table_item(HiveSqlParser.Declare_temporary_table_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_stmt(HiveSqlParser.Create_table_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_stmt(HiveSqlParser.Create_table_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_local_temp_table_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_local_temp_table_stmt(HiveSqlParser.Create_local_temp_table_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_local_temp_table_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_local_temp_table_stmt(HiveSqlParser.Create_local_temp_table_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_definition}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_definition(HiveSqlParser.Create_table_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_definition}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_definition(HiveSqlParser.Create_table_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_columns}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_columns(HiveSqlParser.Create_table_columnsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_columns}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_columns(HiveSqlParser.Create_table_columnsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_columns_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_columns_item(HiveSqlParser.Create_table_columns_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_columns_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_columns_item(HiveSqlParser.Create_table_columns_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#column_comment}.
	 * @param ctx the parse tree
	 */
	void enterColumn_comment(HiveSqlParser.Column_commentContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#column_comment}.
	 * @param ctx the parse tree
	 */
	void exitColumn_comment(HiveSqlParser.Column_commentContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#column_name}.
	 * @param ctx the parse tree
	 */
	void enterColumn_name(HiveSqlParser.Column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#column_name}.
	 * @param ctx the parse tree
	 */
	void exitColumn_name(HiveSqlParser.Column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_column_inline_cons}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_column_inline_cons(HiveSqlParser.Create_table_column_inline_consContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_column_inline_cons}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_column_inline_cons(HiveSqlParser.Create_table_column_inline_consContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_column_cons}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_column_cons(HiveSqlParser.Create_table_column_consContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_column_cons}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_column_cons(HiveSqlParser.Create_table_column_consContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_fk_action}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_fk_action(HiveSqlParser.Create_table_fk_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_fk_action}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_fk_action(HiveSqlParser.Create_table_fk_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_preoptions}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_preoptions(HiveSqlParser.Create_table_preoptionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_preoptions}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_preoptions(HiveSqlParser.Create_table_preoptionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_preoptions_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_preoptions_item(HiveSqlParser.Create_table_preoptions_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_preoptions_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_preoptions_item(HiveSqlParser.Create_table_preoptions_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_preoptions_td_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_preoptions_td_item(HiveSqlParser.Create_table_preoptions_td_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_preoptions_td_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_preoptions_td_item(HiveSqlParser.Create_table_preoptions_td_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_options}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_options(HiveSqlParser.Create_table_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_options}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_options(HiveSqlParser.Create_table_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_options_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_options_item(HiveSqlParser.Create_table_options_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_options_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_options_item(HiveSqlParser.Create_table_options_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_options_ora_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_options_ora_item(HiveSqlParser.Create_table_options_ora_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_options_ora_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_options_ora_item(HiveSqlParser.Create_table_options_ora_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_options_db2_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_options_db2_item(HiveSqlParser.Create_table_options_db2_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_options_db2_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_options_db2_item(HiveSqlParser.Create_table_options_db2_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_options_td_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_options_td_item(HiveSqlParser.Create_table_options_td_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_options_td_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_options_td_item(HiveSqlParser.Create_table_options_td_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_options_hive_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_options_hive_item(HiveSqlParser.Create_table_options_hive_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_options_hive_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_options_hive_item(HiveSqlParser.Create_table_options_hive_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_hive_row_format}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_hive_row_format(HiveSqlParser.Create_table_hive_row_formatContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_hive_row_format}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_hive_row_format(HiveSqlParser.Create_table_hive_row_formatContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_hive_row_format_fields}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_hive_row_format_fields(HiveSqlParser.Create_table_hive_row_format_fieldsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_hive_row_format_fields}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_hive_row_format_fields(HiveSqlParser.Create_table_hive_row_format_fieldsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_options_mssql_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_options_mssql_item(HiveSqlParser.Create_table_options_mssql_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_options_mssql_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_options_mssql_item(HiveSqlParser.Create_table_options_mssql_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_table_options_mysql_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_options_mysql_item(HiveSqlParser.Create_table_options_mysql_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_table_options_mysql_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_options_mysql_item(HiveSqlParser.Create_table_options_mysql_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#alter_table_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_stmt(HiveSqlParser.Alter_table_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#alter_table_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_stmt(HiveSqlParser.Alter_table_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#alter_table_item}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_item(HiveSqlParser.Alter_table_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#alter_table_item}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_item(HiveSqlParser.Alter_table_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#alter_table_add_constraint}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_add_constraint(HiveSqlParser.Alter_table_add_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#alter_table_add_constraint}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_add_constraint(HiveSqlParser.Alter_table_add_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#alter_table_add_constraint_item}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_add_constraint_item(HiveSqlParser.Alter_table_add_constraint_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#alter_table_add_constraint_item}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_add_constraint_item(HiveSqlParser.Alter_table_add_constraint_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#dtype}.
	 * @param ctx the parse tree
	 */
	void enterDtype(HiveSqlParser.DtypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#dtype}.
	 * @param ctx the parse tree
	 */
	void exitDtype(HiveSqlParser.DtypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#dtype_len}.
	 * @param ctx the parse tree
	 */
	void enterDtype_len(HiveSqlParser.Dtype_lenContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#dtype_len}.
	 * @param ctx the parse tree
	 */
	void exitDtype_len(HiveSqlParser.Dtype_lenContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#dtype_attr}.
	 * @param ctx the parse tree
	 */
	void enterDtype_attr(HiveSqlParser.Dtype_attrContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#dtype_attr}.
	 * @param ctx the parse tree
	 */
	void exitDtype_attr(HiveSqlParser.Dtype_attrContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#dtype_default}.
	 * @param ctx the parse tree
	 */
	void enterDtype_default(HiveSqlParser.Dtype_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#dtype_default}.
	 * @param ctx the parse tree
	 */
	void exitDtype_default(HiveSqlParser.Dtype_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_database_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_database_stmt(HiveSqlParser.Create_database_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_database_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_database_stmt(HiveSqlParser.Create_database_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_database_option}.
	 * @param ctx the parse tree
	 */
	void enterCreate_database_option(HiveSqlParser.Create_database_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_database_option}.
	 * @param ctx the parse tree
	 */
	void exitCreate_database_option(HiveSqlParser.Create_database_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_function_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_function_stmt(HiveSqlParser.Create_function_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_function_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_function_stmt(HiveSqlParser.Create_function_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_function_return}.
	 * @param ctx the parse tree
	 */
	void enterCreate_function_return(HiveSqlParser.Create_function_returnContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_function_return}.
	 * @param ctx the parse tree
	 */
	void exitCreate_function_return(HiveSqlParser.Create_function_returnContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_package_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_package_stmt(HiveSqlParser.Create_package_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_package_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_package_stmt(HiveSqlParser.Create_package_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#package_spec}.
	 * @param ctx the parse tree
	 */
	void enterPackage_spec(HiveSqlParser.Package_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#package_spec}.
	 * @param ctx the parse tree
	 */
	void exitPackage_spec(HiveSqlParser.Package_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#package_spec_item}.
	 * @param ctx the parse tree
	 */
	void enterPackage_spec_item(HiveSqlParser.Package_spec_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#package_spec_item}.
	 * @param ctx the parse tree
	 */
	void exitPackage_spec_item(HiveSqlParser.Package_spec_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_package_body_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_package_body_stmt(HiveSqlParser.Create_package_body_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_package_body_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_package_body_stmt(HiveSqlParser.Create_package_body_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#package_body}.
	 * @param ctx the parse tree
	 */
	void enterPackage_body(HiveSqlParser.Package_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#package_body}.
	 * @param ctx the parse tree
	 */
	void exitPackage_body(HiveSqlParser.Package_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#package_body_item}.
	 * @param ctx the parse tree
	 */
	void enterPackage_body_item(HiveSqlParser.Package_body_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#package_body_item}.
	 * @param ctx the parse tree
	 */
	void exitPackage_body_item(HiveSqlParser.Package_body_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_procedure_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_procedure_stmt(HiveSqlParser.Create_procedure_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_procedure_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_procedure_stmt(HiveSqlParser.Create_procedure_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_routine_params}.
	 * @param ctx the parse tree
	 */
	void enterCreate_routine_params(HiveSqlParser.Create_routine_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_routine_params}.
	 * @param ctx the parse tree
	 */
	void exitCreate_routine_params(HiveSqlParser.Create_routine_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_routine_param_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_routine_param_item(HiveSqlParser.Create_routine_param_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_routine_param_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_routine_param_item(HiveSqlParser.Create_routine_param_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_routine_options}.
	 * @param ctx the parse tree
	 */
	void enterCreate_routine_options(HiveSqlParser.Create_routine_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_routine_options}.
	 * @param ctx the parse tree
	 */
	void exitCreate_routine_options(HiveSqlParser.Create_routine_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_routine_option}.
	 * @param ctx the parse tree
	 */
	void enterCreate_routine_option(HiveSqlParser.Create_routine_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_routine_option}.
	 * @param ctx the parse tree
	 */
	void exitCreate_routine_option(HiveSqlParser.Create_routine_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#drop_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDrop_stmt(HiveSqlParser.Drop_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#drop_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDrop_stmt(HiveSqlParser.Drop_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#end_transaction_stmt}.
	 * @param ctx the parse tree
	 */
	void enterEnd_transaction_stmt(HiveSqlParser.End_transaction_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#end_transaction_stmt}.
	 * @param ctx the parse tree
	 */
	void exitEnd_transaction_stmt(HiveSqlParser.End_transaction_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#exec_stmt}.
	 * @param ctx the parse tree
	 */
	void enterExec_stmt(HiveSqlParser.Exec_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#exec_stmt}.
	 * @param ctx the parse tree
	 */
	void exitExec_stmt(HiveSqlParser.Exec_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#if_stmt}.
	 * @param ctx the parse tree
	 */
	void enterIf_stmt(HiveSqlParser.If_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#if_stmt}.
	 * @param ctx the parse tree
	 */
	void exitIf_stmt(HiveSqlParser.If_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#if_plsql_stmt}.
	 * @param ctx the parse tree
	 */
	void enterIf_plsql_stmt(HiveSqlParser.If_plsql_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#if_plsql_stmt}.
	 * @param ctx the parse tree
	 */
	void exitIf_plsql_stmt(HiveSqlParser.If_plsql_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#if_tsql_stmt}.
	 * @param ctx the parse tree
	 */
	void enterIf_tsql_stmt(HiveSqlParser.If_tsql_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#if_tsql_stmt}.
	 * @param ctx the parse tree
	 */
	void exitIf_tsql_stmt(HiveSqlParser.If_tsql_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#if_bteq_stmt}.
	 * @param ctx the parse tree
	 */
	void enterIf_bteq_stmt(HiveSqlParser.If_bteq_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#if_bteq_stmt}.
	 * @param ctx the parse tree
	 */
	void exitIf_bteq_stmt(HiveSqlParser.If_bteq_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#elseif_block}.
	 * @param ctx the parse tree
	 */
	void enterElseif_block(HiveSqlParser.Elseif_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#elseif_block}.
	 * @param ctx the parse tree
	 */
	void exitElseif_block(HiveSqlParser.Elseif_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#else_block}.
	 * @param ctx the parse tree
	 */
	void enterElse_block(HiveSqlParser.Else_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#else_block}.
	 * @param ctx the parse tree
	 */
	void exitElse_block(HiveSqlParser.Else_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#include_stmt}.
	 * @param ctx the parse tree
	 */
	void enterInclude_stmt(HiveSqlParser.Include_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#include_stmt}.
	 * @param ctx the parse tree
	 */
	void exitInclude_stmt(HiveSqlParser.Include_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#insert_stmt}.
	 * @param ctx the parse tree
	 */
	void enterInsert_stmt(HiveSqlParser.Insert_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#insert_stmt}.
	 * @param ctx the parse tree
	 */
	void exitInsert_stmt(HiveSqlParser.Insert_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#insert_stmt_cols}.
	 * @param ctx the parse tree
	 */
	void enterInsert_stmt_cols(HiveSqlParser.Insert_stmt_colsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#insert_stmt_cols}.
	 * @param ctx the parse tree
	 */
	void exitInsert_stmt_cols(HiveSqlParser.Insert_stmt_colsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#insert_stmt_rows}.
	 * @param ctx the parse tree
	 */
	void enterInsert_stmt_rows(HiveSqlParser.Insert_stmt_rowsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#insert_stmt_rows}.
	 * @param ctx the parse tree
	 */
	void exitInsert_stmt_rows(HiveSqlParser.Insert_stmt_rowsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#insert_stmt_row}.
	 * @param ctx the parse tree
	 */
	void enterInsert_stmt_row(HiveSqlParser.Insert_stmt_rowContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#insert_stmt_row}.
	 * @param ctx the parse tree
	 */
	void exitInsert_stmt_row(HiveSqlParser.Insert_stmt_rowContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#insert_directory_stmt}.
	 * @param ctx the parse tree
	 */
	void enterInsert_directory_stmt(HiveSqlParser.Insert_directory_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#insert_directory_stmt}.
	 * @param ctx the parse tree
	 */
	void exitInsert_directory_stmt(HiveSqlParser.Insert_directory_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#exit_stmt}.
	 * @param ctx the parse tree
	 */
	void enterExit_stmt(HiveSqlParser.Exit_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#exit_stmt}.
	 * @param ctx the parse tree
	 */
	void exitExit_stmt(HiveSqlParser.Exit_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#get_diag_stmt}.
	 * @param ctx the parse tree
	 */
	void enterGet_diag_stmt(HiveSqlParser.Get_diag_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#get_diag_stmt}.
	 * @param ctx the parse tree
	 */
	void exitGet_diag_stmt(HiveSqlParser.Get_diag_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#get_diag_stmt_item}.
	 * @param ctx the parse tree
	 */
	void enterGet_diag_stmt_item(HiveSqlParser.Get_diag_stmt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#get_diag_stmt_item}.
	 * @param ctx the parse tree
	 */
	void exitGet_diag_stmt_item(HiveSqlParser.Get_diag_stmt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#get_diag_stmt_exception_item}.
	 * @param ctx the parse tree
	 */
	void enterGet_diag_stmt_exception_item(HiveSqlParser.Get_diag_stmt_exception_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#get_diag_stmt_exception_item}.
	 * @param ctx the parse tree
	 */
	void exitGet_diag_stmt_exception_item(HiveSqlParser.Get_diag_stmt_exception_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#get_diag_stmt_rowcount_item}.
	 * @param ctx the parse tree
	 */
	void enterGet_diag_stmt_rowcount_item(HiveSqlParser.Get_diag_stmt_rowcount_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#get_diag_stmt_rowcount_item}.
	 * @param ctx the parse tree
	 */
	void exitGet_diag_stmt_rowcount_item(HiveSqlParser.Get_diag_stmt_rowcount_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#grant_stmt}.
	 * @param ctx the parse tree
	 */
	void enterGrant_stmt(HiveSqlParser.Grant_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#grant_stmt}.
	 * @param ctx the parse tree
	 */
	void exitGrant_stmt(HiveSqlParser.Grant_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#grant_stmt_item}.
	 * @param ctx the parse tree
	 */
	void enterGrant_stmt_item(HiveSqlParser.Grant_stmt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#grant_stmt_item}.
	 * @param ctx the parse tree
	 */
	void exitGrant_stmt_item(HiveSqlParser.Grant_stmt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#leave_stmt}.
	 * @param ctx the parse tree
	 */
	void enterLeave_stmt(HiveSqlParser.Leave_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#leave_stmt}.
	 * @param ctx the parse tree
	 */
	void exitLeave_stmt(HiveSqlParser.Leave_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#map_object_stmt}.
	 * @param ctx the parse tree
	 */
	void enterMap_object_stmt(HiveSqlParser.Map_object_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#map_object_stmt}.
	 * @param ctx the parse tree
	 */
	void exitMap_object_stmt(HiveSqlParser.Map_object_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#open_stmt}.
	 * @param ctx the parse tree
	 */
	void enterOpen_stmt(HiveSqlParser.Open_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#open_stmt}.
	 * @param ctx the parse tree
	 */
	void exitOpen_stmt(HiveSqlParser.Open_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#fetch_stmt}.
	 * @param ctx the parse tree
	 */
	void enterFetch_stmt(HiveSqlParser.Fetch_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#fetch_stmt}.
	 * @param ctx the parse tree
	 */
	void exitFetch_stmt(HiveSqlParser.Fetch_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#collect_stats_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCollect_stats_stmt(HiveSqlParser.Collect_stats_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#collect_stats_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCollect_stats_stmt(HiveSqlParser.Collect_stats_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#collect_stats_clause}.
	 * @param ctx the parse tree
	 */
	void enterCollect_stats_clause(HiveSqlParser.Collect_stats_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#collect_stats_clause}.
	 * @param ctx the parse tree
	 */
	void exitCollect_stats_clause(HiveSqlParser.Collect_stats_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#close_stmt}.
	 * @param ctx the parse tree
	 */
	void enterClose_stmt(HiveSqlParser.Close_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#close_stmt}.
	 * @param ctx the parse tree
	 */
	void exitClose_stmt(HiveSqlParser.Close_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#cmp_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCmp_stmt(HiveSqlParser.Cmp_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#cmp_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCmp_stmt(HiveSqlParser.Cmp_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#cmp_source}.
	 * @param ctx the parse tree
	 */
	void enterCmp_source(HiveSqlParser.Cmp_sourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#cmp_source}.
	 * @param ctx the parse tree
	 */
	void exitCmp_source(HiveSqlParser.Cmp_sourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#copy_from_local_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCopy_from_local_stmt(HiveSqlParser.Copy_from_local_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#copy_from_local_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCopy_from_local_stmt(HiveSqlParser.Copy_from_local_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#copy_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCopy_stmt(HiveSqlParser.Copy_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#copy_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCopy_stmt(HiveSqlParser.Copy_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#copy_source}.
	 * @param ctx the parse tree
	 */
	void enterCopy_source(HiveSqlParser.Copy_sourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#copy_source}.
	 * @param ctx the parse tree
	 */
	void exitCopy_source(HiveSqlParser.Copy_sourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#copy_target}.
	 * @param ctx the parse tree
	 */
	void enterCopy_target(HiveSqlParser.Copy_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#copy_target}.
	 * @param ctx the parse tree
	 */
	void exitCopy_target(HiveSqlParser.Copy_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#copy_option}.
	 * @param ctx the parse tree
	 */
	void enterCopy_option(HiveSqlParser.Copy_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#copy_option}.
	 * @param ctx the parse tree
	 */
	void exitCopy_option(HiveSqlParser.Copy_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#copy_file_option}.
	 * @param ctx the parse tree
	 */
	void enterCopy_file_option(HiveSqlParser.Copy_file_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#copy_file_option}.
	 * @param ctx the parse tree
	 */
	void exitCopy_file_option(HiveSqlParser.Copy_file_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#commit_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCommit_stmt(HiveSqlParser.Commit_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#commit_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCommit_stmt(HiveSqlParser.Commit_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_index_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCreate_index_stmt(HiveSqlParser.Create_index_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_index_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCreate_index_stmt(HiveSqlParser.Create_index_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#create_index_col}.
	 * @param ctx the parse tree
	 */
	void enterCreate_index_col(HiveSqlParser.Create_index_colContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#create_index_col}.
	 * @param ctx the parse tree
	 */
	void exitCreate_index_col(HiveSqlParser.Create_index_colContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#index_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_storage_clause(HiveSqlParser.Index_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#index_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_storage_clause(HiveSqlParser.Index_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#index_mssql_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_mssql_storage_clause(HiveSqlParser.Index_mssql_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#index_mssql_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_mssql_storage_clause(HiveSqlParser.Index_mssql_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#print_stmt}.
	 * @param ctx the parse tree
	 */
	void enterPrint_stmt(HiveSqlParser.Print_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#print_stmt}.
	 * @param ctx the parse tree
	 */
	void exitPrint_stmt(HiveSqlParser.Print_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#quit_stmt}.
	 * @param ctx the parse tree
	 */
	void enterQuit_stmt(HiveSqlParser.Quit_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#quit_stmt}.
	 * @param ctx the parse tree
	 */
	void exitQuit_stmt(HiveSqlParser.Quit_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#raise_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRaise_stmt(HiveSqlParser.Raise_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#raise_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRaise_stmt(HiveSqlParser.Raise_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#resignal_stmt}.
	 * @param ctx the parse tree
	 */
	void enterResignal_stmt(HiveSqlParser.Resignal_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#resignal_stmt}.
	 * @param ctx the parse tree
	 */
	void exitResignal_stmt(HiveSqlParser.Resignal_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#return_stmt}.
	 * @param ctx the parse tree
	 */
	void enterReturn_stmt(HiveSqlParser.Return_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#return_stmt}.
	 * @param ctx the parse tree
	 */
	void exitReturn_stmt(HiveSqlParser.Return_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#rollback_stmt}.
	 * @param ctx the parse tree
	 */
	void enterRollback_stmt(HiveSqlParser.Rollback_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#rollback_stmt}.
	 * @param ctx the parse tree
	 */
	void exitRollback_stmt(HiveSqlParser.Rollback_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#set_session_option}.
	 * @param ctx the parse tree
	 */
	void enterSet_session_option(HiveSqlParser.Set_session_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#set_session_option}.
	 * @param ctx the parse tree
	 */
	void exitSet_session_option(HiveSqlParser.Set_session_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#set_current_schema_option}.
	 * @param ctx the parse tree
	 */
	void enterSet_current_schema_option(HiveSqlParser.Set_current_schema_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#set_current_schema_option}.
	 * @param ctx the parse tree
	 */
	void exitSet_current_schema_option(HiveSqlParser.Set_current_schema_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#set_mssql_session_option}.
	 * @param ctx the parse tree
	 */
	void enterSet_mssql_session_option(HiveSqlParser.Set_mssql_session_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#set_mssql_session_option}.
	 * @param ctx the parse tree
	 */
	void exitSet_mssql_session_option(HiveSqlParser.Set_mssql_session_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#set_teradata_session_option}.
	 * @param ctx the parse tree
	 */
	void enterSet_teradata_session_option(HiveSqlParser.Set_teradata_session_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#set_teradata_session_option}.
	 * @param ctx the parse tree
	 */
	void exitSet_teradata_session_option(HiveSqlParser.Set_teradata_session_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#signal_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSignal_stmt(HiveSqlParser.Signal_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#signal_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSignal_stmt(HiveSqlParser.Signal_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#summary_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSummary_stmt(HiveSqlParser.Summary_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#summary_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSummary_stmt(HiveSqlParser.Summary_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#truncate_stmt}.
	 * @param ctx the parse tree
	 */
	void enterTruncate_stmt(HiveSqlParser.Truncate_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#truncate_stmt}.
	 * @param ctx the parse tree
	 */
	void exitTruncate_stmt(HiveSqlParser.Truncate_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#use_stmt}.
	 * @param ctx the parse tree
	 */
	void enterUse_stmt(HiveSqlParser.Use_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#use_stmt}.
	 * @param ctx the parse tree
	 */
	void exitUse_stmt(HiveSqlParser.Use_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#values_into_stmt}.
	 * @param ctx the parse tree
	 */
	void enterValues_into_stmt(HiveSqlParser.Values_into_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#values_into_stmt}.
	 * @param ctx the parse tree
	 */
	void exitValues_into_stmt(HiveSqlParser.Values_into_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#while_stmt}.
	 * @param ctx the parse tree
	 */
	void enterWhile_stmt(HiveSqlParser.While_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#while_stmt}.
	 * @param ctx the parse tree
	 */
	void exitWhile_stmt(HiveSqlParser.While_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#for_cursor_stmt}.
	 * @param ctx the parse tree
	 */
	void enterFor_cursor_stmt(HiveSqlParser.For_cursor_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#for_cursor_stmt}.
	 * @param ctx the parse tree
	 */
	void exitFor_cursor_stmt(HiveSqlParser.For_cursor_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#for_range_stmt}.
	 * @param ctx the parse tree
	 */
	void enterFor_range_stmt(HiveSqlParser.For_range_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#for_range_stmt}.
	 * @param ctx the parse tree
	 */
	void exitFor_range_stmt(HiveSqlParser.For_range_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#label}.
	 * @param ctx the parse tree
	 */
	void enterLabel(HiveSqlParser.LabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#label}.
	 * @param ctx the parse tree
	 */
	void exitLabel(HiveSqlParser.LabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#using_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_clause(HiveSqlParser.Using_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#using_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_clause(HiveSqlParser.Using_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#select_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSelect_stmt(HiveSqlParser.Select_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#select_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSelect_stmt(HiveSqlParser.Select_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#cte_select_stmt}.
	 * @param ctx the parse tree
	 */
	void enterCte_select_stmt(HiveSqlParser.Cte_select_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#cte_select_stmt}.
	 * @param ctx the parse tree
	 */
	void exitCte_select_stmt(HiveSqlParser.Cte_select_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#cte_select_stmt_item}.
	 * @param ctx the parse tree
	 */
	void enterCte_select_stmt_item(HiveSqlParser.Cte_select_stmt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#cte_select_stmt_item}.
	 * @param ctx the parse tree
	 */
	void exitCte_select_stmt_item(HiveSqlParser.Cte_select_stmt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#cte_select_cols}.
	 * @param ctx the parse tree
	 */
	void enterCte_select_cols(HiveSqlParser.Cte_select_colsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#cte_select_cols}.
	 * @param ctx the parse tree
	 */
	void exitCte_select_cols(HiveSqlParser.Cte_select_colsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#fullselect_stmt}.
	 * @param ctx the parse tree
	 */
	void enterFullselect_stmt(HiveSqlParser.Fullselect_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#fullselect_stmt}.
	 * @param ctx the parse tree
	 */
	void exitFullselect_stmt(HiveSqlParser.Fullselect_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#fullselect_stmt_item}.
	 * @param ctx the parse tree
	 */
	void enterFullselect_stmt_item(HiveSqlParser.Fullselect_stmt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#fullselect_stmt_item}.
	 * @param ctx the parse tree
	 */
	void exitFullselect_stmt_item(HiveSqlParser.Fullselect_stmt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#fullselect_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterFullselect_set_clause(HiveSqlParser.Fullselect_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#fullselect_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitFullselect_set_clause(HiveSqlParser.Fullselect_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#subselect_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSubselect_stmt(HiveSqlParser.Subselect_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#subselect_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSubselect_stmt(HiveSqlParser.Subselect_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#select_list}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list(HiveSqlParser.Select_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#select_list}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list(HiveSqlParser.Select_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#select_list_set}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list_set(HiveSqlParser.Select_list_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#select_list_set}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list_set(HiveSqlParser.Select_list_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#select_list_limit}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list_limit(HiveSqlParser.Select_list_limitContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#select_list_limit}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list_limit(HiveSqlParser.Select_list_limitContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#select_list_item}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list_item(HiveSqlParser.Select_list_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#select_list_item}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list_item(HiveSqlParser.Select_list_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#select_list_alias}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list_alias(HiveSqlParser.Select_list_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#select_list_alias}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list_alias(HiveSqlParser.Select_list_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#select_list_asterisk}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list_asterisk(HiveSqlParser.Select_list_asteriskContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#select_list_asterisk}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list_asterisk(HiveSqlParser.Select_list_asteriskContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void enterInto_clause(HiveSqlParser.Into_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void exitInto_clause(HiveSqlParser.Into_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#from_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_clause(HiveSqlParser.From_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#from_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_clause(HiveSqlParser.From_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#from_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_table_clause(HiveSqlParser.From_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#from_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_table_clause(HiveSqlParser.From_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#from_table_name_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_table_name_clause(HiveSqlParser.From_table_name_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#from_table_name_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_table_name_clause(HiveSqlParser.From_table_name_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#from_subselect_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_subselect_clause(HiveSqlParser.From_subselect_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#from_subselect_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_subselect_clause(HiveSqlParser.From_subselect_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#from_join_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_join_clause(HiveSqlParser.From_join_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#from_join_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_join_clause(HiveSqlParser.From_join_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#from_join_type_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_join_type_clause(HiveSqlParser.From_join_type_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#from_join_type_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_join_type_clause(HiveSqlParser.From_join_type_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#from_table_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_table_values_clause(HiveSqlParser.From_table_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#from_table_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_table_values_clause(HiveSqlParser.From_table_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#from_table_values_row}.
	 * @param ctx the parse tree
	 */
	void enterFrom_table_values_row(HiveSqlParser.From_table_values_rowContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#from_table_values_row}.
	 * @param ctx the parse tree
	 */
	void exitFrom_table_values_row(HiveSqlParser.From_table_values_rowContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#from_alias_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_alias_clause(HiveSqlParser.From_alias_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#from_alias_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_alias_clause(HiveSqlParser.From_alias_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#table_name}.
	 * @param ctx the parse tree
	 */
	void enterTable_name(HiveSqlParser.Table_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#table_name}.
	 * @param ctx the parse tree
	 */
	void exitTable_name(HiveSqlParser.Table_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#where_clause}.
	 * @param ctx the parse tree
	 */
	void enterWhere_clause(HiveSqlParser.Where_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#where_clause}.
	 * @param ctx the parse tree
	 */
	void exitWhere_clause(HiveSqlParser.Where_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#group_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_clause(HiveSqlParser.Group_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#group_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_clause(HiveSqlParser.Group_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#having_clause}.
	 * @param ctx the parse tree
	 */
	void enterHaving_clause(HiveSqlParser.Having_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#having_clause}.
	 * @param ctx the parse tree
	 */
	void exitHaving_clause(HiveSqlParser.Having_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#qualify_clause}.
	 * @param ctx the parse tree
	 */
	void enterQualify_clause(HiveSqlParser.Qualify_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#qualify_clause}.
	 * @param ctx the parse tree
	 */
	void exitQualify_clause(HiveSqlParser.Qualify_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#order_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by_clause(HiveSqlParser.Order_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#order_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by_clause(HiveSqlParser.Order_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#select_options}.
	 * @param ctx the parse tree
	 */
	void enterSelect_options(HiveSqlParser.Select_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#select_options}.
	 * @param ctx the parse tree
	 */
	void exitSelect_options(HiveSqlParser.Select_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#select_options_item}.
	 * @param ctx the parse tree
	 */
	void enterSelect_options_item(HiveSqlParser.Select_options_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#select_options_item}.
	 * @param ctx the parse tree
	 */
	void exitSelect_options_item(HiveSqlParser.Select_options_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#update_stmt}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_stmt(HiveSqlParser.Update_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#update_stmt}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_stmt(HiveSqlParser.Update_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#update_assignment}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_assignment(HiveSqlParser.Update_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#update_assignment}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_assignment(HiveSqlParser.Update_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#update_table}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_table(HiveSqlParser.Update_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#update_table}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_table(HiveSqlParser.Update_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#update_upsert}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_upsert(HiveSqlParser.Update_upsertContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#update_upsert}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_upsert(HiveSqlParser.Update_upsertContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#merge_stmt}.
	 * @param ctx the parse tree
	 */
	void enterMerge_stmt(HiveSqlParser.Merge_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#merge_stmt}.
	 * @param ctx the parse tree
	 */
	void exitMerge_stmt(HiveSqlParser.Merge_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#merge_table}.
	 * @param ctx the parse tree
	 */
	void enterMerge_table(HiveSqlParser.Merge_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#merge_table}.
	 * @param ctx the parse tree
	 */
	void exitMerge_table(HiveSqlParser.Merge_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#merge_condition}.
	 * @param ctx the parse tree
	 */
	void enterMerge_condition(HiveSqlParser.Merge_conditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#merge_condition}.
	 * @param ctx the parse tree
	 */
	void exitMerge_condition(HiveSqlParser.Merge_conditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#merge_action}.
	 * @param ctx the parse tree
	 */
	void enterMerge_action(HiveSqlParser.Merge_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#merge_action}.
	 * @param ctx the parse tree
	 */
	void exitMerge_action(HiveSqlParser.Merge_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#delete_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDelete_stmt(HiveSqlParser.Delete_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#delete_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDelete_stmt(HiveSqlParser.Delete_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#delete_alias}.
	 * @param ctx the parse tree
	 */
	void enterDelete_alias(HiveSqlParser.Delete_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#delete_alias}.
	 * @param ctx the parse tree
	 */
	void exitDelete_alias(HiveSqlParser.Delete_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#describe_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDescribe_stmt(HiveSqlParser.Describe_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#describe_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDescribe_stmt(HiveSqlParser.Describe_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#bool_expr}.
	 * @param ctx the parse tree
	 */
	void enterBool_expr(HiveSqlParser.Bool_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#bool_expr}.
	 * @param ctx the parse tree
	 */
	void exitBool_expr(HiveSqlParser.Bool_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#bool_expr_atom}.
	 * @param ctx the parse tree
	 */
	void enterBool_expr_atom(HiveSqlParser.Bool_expr_atomContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#bool_expr_atom}.
	 * @param ctx the parse tree
	 */
	void exitBool_expr_atom(HiveSqlParser.Bool_expr_atomContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#bool_expr_unary}.
	 * @param ctx the parse tree
	 */
	void enterBool_expr_unary(HiveSqlParser.Bool_expr_unaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#bool_expr_unary}.
	 * @param ctx the parse tree
	 */
	void exitBool_expr_unary(HiveSqlParser.Bool_expr_unaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#bool_expr_single_in}.
	 * @param ctx the parse tree
	 */
	void enterBool_expr_single_in(HiveSqlParser.Bool_expr_single_inContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#bool_expr_single_in}.
	 * @param ctx the parse tree
	 */
	void exitBool_expr_single_in(HiveSqlParser.Bool_expr_single_inContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#bool_expr_multi_in}.
	 * @param ctx the parse tree
	 */
	void enterBool_expr_multi_in(HiveSqlParser.Bool_expr_multi_inContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#bool_expr_multi_in}.
	 * @param ctx the parse tree
	 */
	void exitBool_expr_multi_in(HiveSqlParser.Bool_expr_multi_inContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#bool_expr_binary}.
	 * @param ctx the parse tree
	 */
	void enterBool_expr_binary(HiveSqlParser.Bool_expr_binaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#bool_expr_binary}.
	 * @param ctx the parse tree
	 */
	void exitBool_expr_binary(HiveSqlParser.Bool_expr_binaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#bool_expr_logical_operator}.
	 * @param ctx the parse tree
	 */
	void enterBool_expr_logical_operator(HiveSqlParser.Bool_expr_logical_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#bool_expr_logical_operator}.
	 * @param ctx the parse tree
	 */
	void exitBool_expr_logical_operator(HiveSqlParser.Bool_expr_logical_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#bool_expr_binary_operator}.
	 * @param ctx the parse tree
	 */
	void enterBool_expr_binary_operator(HiveSqlParser.Bool_expr_binary_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#bool_expr_binary_operator}.
	 * @param ctx the parse tree
	 */
	void exitBool_expr_binary_operator(HiveSqlParser.Bool_expr_binary_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(HiveSqlParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(HiveSqlParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_atom}.
	 * @param ctx the parse tree
	 */
	void enterExpr_atom(HiveSqlParser.Expr_atomContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_atom}.
	 * @param ctx the parse tree
	 */
	void exitExpr_atom(HiveSqlParser.Expr_atomContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_interval}.
	 * @param ctx the parse tree
	 */
	void enterExpr_interval(HiveSqlParser.Expr_intervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_interval}.
	 * @param ctx the parse tree
	 */
	void exitExpr_interval(HiveSqlParser.Expr_intervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#interval_item}.
	 * @param ctx the parse tree
	 */
	void enterInterval_item(HiveSqlParser.Interval_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#interval_item}.
	 * @param ctx the parse tree
	 */
	void exitInterval_item(HiveSqlParser.Interval_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_concat}.
	 * @param ctx the parse tree
	 */
	void enterExpr_concat(HiveSqlParser.Expr_concatContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_concat}.
	 * @param ctx the parse tree
	 */
	void exitExpr_concat(HiveSqlParser.Expr_concatContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_concat_item}.
	 * @param ctx the parse tree
	 */
	void enterExpr_concat_item(HiveSqlParser.Expr_concat_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_concat_item}.
	 * @param ctx the parse tree
	 */
	void exitExpr_concat_item(HiveSqlParser.Expr_concat_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_case}.
	 * @param ctx the parse tree
	 */
	void enterExpr_case(HiveSqlParser.Expr_caseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_case}.
	 * @param ctx the parse tree
	 */
	void exitExpr_case(HiveSqlParser.Expr_caseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_case_simple}.
	 * @param ctx the parse tree
	 */
	void enterExpr_case_simple(HiveSqlParser.Expr_case_simpleContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_case_simple}.
	 * @param ctx the parse tree
	 */
	void exitExpr_case_simple(HiveSqlParser.Expr_case_simpleContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_case_searched}.
	 * @param ctx the parse tree
	 */
	void enterExpr_case_searched(HiveSqlParser.Expr_case_searchedContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_case_searched}.
	 * @param ctx the parse tree
	 */
	void exitExpr_case_searched(HiveSqlParser.Expr_case_searchedContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_cursor_attribute}.
	 * @param ctx the parse tree
	 */
	void enterExpr_cursor_attribute(HiveSqlParser.Expr_cursor_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_cursor_attribute}.
	 * @param ctx the parse tree
	 */
	void exitExpr_cursor_attribute(HiveSqlParser.Expr_cursor_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_agg_window_func}.
	 * @param ctx the parse tree
	 */
	void enterExpr_agg_window_func(HiveSqlParser.Expr_agg_window_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_agg_window_func}.
	 * @param ctx the parse tree
	 */
	void exitExpr_agg_window_func(HiveSqlParser.Expr_agg_window_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_func_all_distinct}.
	 * @param ctx the parse tree
	 */
	void enterExpr_func_all_distinct(HiveSqlParser.Expr_func_all_distinctContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_func_all_distinct}.
	 * @param ctx the parse tree
	 */
	void exitExpr_func_all_distinct(HiveSqlParser.Expr_func_all_distinctContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_func_over_clause}.
	 * @param ctx the parse tree
	 */
	void enterExpr_func_over_clause(HiveSqlParser.Expr_func_over_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_func_over_clause}.
	 * @param ctx the parse tree
	 */
	void exitExpr_func_over_clause(HiveSqlParser.Expr_func_over_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_func_partition_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterExpr_func_partition_by_clause(HiveSqlParser.Expr_func_partition_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_func_partition_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitExpr_func_partition_by_clause(HiveSqlParser.Expr_func_partition_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_spec_func}.
	 * @param ctx the parse tree
	 */
	void enterExpr_spec_func(HiveSqlParser.Expr_spec_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_spec_func}.
	 * @param ctx the parse tree
	 */
	void exitExpr_spec_func(HiveSqlParser.Expr_spec_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_func}.
	 * @param ctx the parse tree
	 */
	void enterExpr_func(HiveSqlParser.Expr_funcContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_func}.
	 * @param ctx the parse tree
	 */
	void exitExpr_func(HiveSqlParser.Expr_funcContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_func_params}.
	 * @param ctx the parse tree
	 */
	void enterExpr_func_params(HiveSqlParser.Expr_func_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_func_params}.
	 * @param ctx the parse tree
	 */
	void exitExpr_func_params(HiveSqlParser.Expr_func_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#func_param}.
	 * @param ctx the parse tree
	 */
	void enterFunc_param(HiveSqlParser.Func_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#func_param}.
	 * @param ctx the parse tree
	 */
	void exitFunc_param(HiveSqlParser.Func_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_select}.
	 * @param ctx the parse tree
	 */
	void enterExpr_select(HiveSqlParser.Expr_selectContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_select}.
	 * @param ctx the parse tree
	 */
	void exitExpr_select(HiveSqlParser.Expr_selectContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#expr_file}.
	 * @param ctx the parse tree
	 */
	void enterExpr_file(HiveSqlParser.Expr_fileContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#expr_file}.
	 * @param ctx the parse tree
	 */
	void exitExpr_file(HiveSqlParser.Expr_fileContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#hive}.
	 * @param ctx the parse tree
	 */
	void enterHive(HiveSqlParser.HiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#hive}.
	 * @param ctx the parse tree
	 */
	void exitHive(HiveSqlParser.HiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#hive_item}.
	 * @param ctx the parse tree
	 */
	void enterHive_item(HiveSqlParser.Hive_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#hive_item}.
	 * @param ctx the parse tree
	 */
	void exitHive_item(HiveSqlParser.Hive_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#host}.
	 * @param ctx the parse tree
	 */
	void enterHost(HiveSqlParser.HostContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#host}.
	 * @param ctx the parse tree
	 */
	void exitHost(HiveSqlParser.HostContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#host_cmd}.
	 * @param ctx the parse tree
	 */
	void enterHost_cmd(HiveSqlParser.Host_cmdContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#host_cmd}.
	 * @param ctx the parse tree
	 */
	void exitHost_cmd(HiveSqlParser.Host_cmdContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#host_stmt}.
	 * @param ctx the parse tree
	 */
	void enterHost_stmt(HiveSqlParser.Host_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#host_stmt}.
	 * @param ctx the parse tree
	 */
	void exitHost_stmt(HiveSqlParser.Host_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#file_name}.
	 * @param ctx the parse tree
	 */
	void enterFile_name(HiveSqlParser.File_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#file_name}.
	 * @param ctx the parse tree
	 */
	void exitFile_name(HiveSqlParser.File_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#date_literal}.
	 * @param ctx the parse tree
	 */
	void enterDate_literal(HiveSqlParser.Date_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#date_literal}.
	 * @param ctx the parse tree
	 */
	void exitDate_literal(HiveSqlParser.Date_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#timestamp_literal}.
	 * @param ctx the parse tree
	 */
	void enterTimestamp_literal(HiveSqlParser.Timestamp_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#timestamp_literal}.
	 * @param ctx the parse tree
	 */
	void exitTimestamp_literal(HiveSqlParser.Timestamp_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#ident}.
	 * @param ctx the parse tree
	 */
	void enterIdent(HiveSqlParser.IdentContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#ident}.
	 * @param ctx the parse tree
	 */
	void exitIdent(HiveSqlParser.IdentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code single_quotedString}
	 * labeled alternative in {@link HiveSqlParser#string}.
	 * @param ctx the parse tree
	 */
	void enterSingle_quotedString(HiveSqlParser.Single_quotedStringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code single_quotedString}
	 * labeled alternative in {@link HiveSqlParser#string}.
	 * @param ctx the parse tree
	 */
	void exitSingle_quotedString(HiveSqlParser.Single_quotedStringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code double_quotedString}
	 * labeled alternative in {@link HiveSqlParser#string}.
	 * @param ctx the parse tree
	 */
	void enterDouble_quotedString(HiveSqlParser.Double_quotedStringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code double_quotedString}
	 * labeled alternative in {@link HiveSqlParser#string}.
	 * @param ctx the parse tree
	 */
	void exitDouble_quotedString(HiveSqlParser.Double_quotedStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#int_number}.
	 * @param ctx the parse tree
	 */
	void enterInt_number(HiveSqlParser.Int_numberContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#int_number}.
	 * @param ctx the parse tree
	 */
	void exitInt_number(HiveSqlParser.Int_numberContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#dec_number}.
	 * @param ctx the parse tree
	 */
	void enterDec_number(HiveSqlParser.Dec_numberContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#dec_number}.
	 * @param ctx the parse tree
	 */
	void exitDec_number(HiveSqlParser.Dec_numberContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#bool_literal}.
	 * @param ctx the parse tree
	 */
	void enterBool_literal(HiveSqlParser.Bool_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#bool_literal}.
	 * @param ctx the parse tree
	 */
	void exitBool_literal(HiveSqlParser.Bool_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#null_const}.
	 * @param ctx the parse tree
	 */
	void enterNull_const(HiveSqlParser.Null_constContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#null_const}.
	 * @param ctx the parse tree
	 */
	void exitNull_const(HiveSqlParser.Null_constContext ctx);
	/**
	 * Enter a parse tree produced by {@link HiveSqlParser#non_reserved_words}.
	 * @param ctx the parse tree
	 */
	void enterNon_reserved_words(HiveSqlParser.Non_reserved_wordsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HiveSqlParser#non_reserved_words}.
	 * @param ctx the parse tree
	 */
	void exitNon_reserved_words(HiveSqlParser.Non_reserved_wordsContext ctx);
}