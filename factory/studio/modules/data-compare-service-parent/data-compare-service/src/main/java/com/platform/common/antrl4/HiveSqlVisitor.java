// Generated from HiveSql.g4 by ANTLR 4.7.2

package com.platform.common.antrl4;
 
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link HiveSqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface HiveSqlVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(HiveSqlParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(HiveSqlParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#begin_end_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBegin_end_block(HiveSqlParser.Begin_end_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#single_block_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_block_stmt(HiveSqlParser.Single_block_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#block_end}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock_end(HiveSqlParser.Block_endContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#proc_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_block(HiveSqlParser.Proc_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(HiveSqlParser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#semicolon_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSemicolon_stmt(HiveSqlParser.Semicolon_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#exception_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_block(HiveSqlParser.Exception_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#exception_block_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_block_item(HiveSqlParser.Exception_block_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#null_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNull_stmt(HiveSqlParser.Null_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_stmt(HiveSqlParser.Expr_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#assignment_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_stmt(HiveSqlParser.Assignment_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#assignment_stmt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_stmt_item(HiveSqlParser.Assignment_stmt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#assignment_stmt_single_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_stmt_single_item(HiveSqlParser.Assignment_stmt_single_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#assignment_stmt_multiple_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_stmt_multiple_item(HiveSqlParser.Assignment_stmt_multiple_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#assignment_stmt_select_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_stmt_select_item(HiveSqlParser.Assignment_stmt_select_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#allocate_cursor_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllocate_cursor_stmt(HiveSqlParser.Allocate_cursor_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#associate_locator_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssociate_locator_stmt(HiveSqlParser.Associate_locator_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#begin_transaction_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBegin_transaction_stmt(HiveSqlParser.Begin_transaction_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#break_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreak_stmt(HiveSqlParser.Break_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#call_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCall_stmt(HiveSqlParser.Call_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#declare_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_stmt(HiveSqlParser.Declare_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#declare_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_block(HiveSqlParser.Declare_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#declare_block_inplace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_block_inplace(HiveSqlParser.Declare_block_inplaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#declare_stmt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_stmt_item(HiveSqlParser.Declare_stmt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#declare_var_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_var_item(HiveSqlParser.Declare_var_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#declare_condition_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_condition_item(HiveSqlParser.Declare_condition_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#declare_cursor_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_cursor_item(HiveSqlParser.Declare_cursor_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#cursor_with_return}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_with_return(HiveSqlParser.Cursor_with_returnContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#cursor_without_return}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_without_return(HiveSqlParser.Cursor_without_returnContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#declare_handler_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_handler_item(HiveSqlParser.Declare_handler_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#declare_temporary_table_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_temporary_table_item(HiveSqlParser.Declare_temporary_table_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_stmt(HiveSqlParser.Create_table_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_local_temp_table_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_local_temp_table_stmt(HiveSqlParser.Create_local_temp_table_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_definition(HiveSqlParser.Create_table_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_columns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_columns(HiveSqlParser.Create_table_columnsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_columns_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_columns_item(HiveSqlParser.Create_table_columns_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#column_comment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_comment(HiveSqlParser.Column_commentContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_name(HiveSqlParser.Column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_column_inline_cons}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_column_inline_cons(HiveSqlParser.Create_table_column_inline_consContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_column_cons}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_column_cons(HiveSqlParser.Create_table_column_consContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_fk_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_fk_action(HiveSqlParser.Create_table_fk_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_preoptions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_preoptions(HiveSqlParser.Create_table_preoptionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_preoptions_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_preoptions_item(HiveSqlParser.Create_table_preoptions_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_preoptions_td_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_preoptions_td_item(HiveSqlParser.Create_table_preoptions_td_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_options(HiveSqlParser.Create_table_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_options_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_options_item(HiveSqlParser.Create_table_options_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_options_ora_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_options_ora_item(HiveSqlParser.Create_table_options_ora_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_options_db2_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_options_db2_item(HiveSqlParser.Create_table_options_db2_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_options_td_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_options_td_item(HiveSqlParser.Create_table_options_td_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_options_hive_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_options_hive_item(HiveSqlParser.Create_table_options_hive_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_hive_row_format}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_hive_row_format(HiveSqlParser.Create_table_hive_row_formatContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_hive_row_format_fields}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_hive_row_format_fields(HiveSqlParser.Create_table_hive_row_format_fieldsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_options_mssql_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_options_mssql_item(HiveSqlParser.Create_table_options_mssql_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_table_options_mysql_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_options_mysql_item(HiveSqlParser.Create_table_options_mysql_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#alter_table_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_stmt(HiveSqlParser.Alter_table_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#alter_table_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_item(HiveSqlParser.Alter_table_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#alter_table_add_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_add_constraint(HiveSqlParser.Alter_table_add_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#alter_table_add_constraint_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_add_constraint_item(HiveSqlParser.Alter_table_add_constraint_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#dtype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtype(HiveSqlParser.DtypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#dtype_len}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtype_len(HiveSqlParser.Dtype_lenContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#dtype_attr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtype_attr(HiveSqlParser.Dtype_attrContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#dtype_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtype_default(HiveSqlParser.Dtype_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_database_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_database_stmt(HiveSqlParser.Create_database_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_database_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_database_option(HiveSqlParser.Create_database_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_function_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_function_stmt(HiveSqlParser.Create_function_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_function_return}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_function_return(HiveSqlParser.Create_function_returnContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_package_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_package_stmt(HiveSqlParser.Create_package_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#package_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_spec(HiveSqlParser.Package_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#package_spec_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_spec_item(HiveSqlParser.Package_spec_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_package_body_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_package_body_stmt(HiveSqlParser.Create_package_body_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#package_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_body(HiveSqlParser.Package_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#package_body_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_body_item(HiveSqlParser.Package_body_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_procedure_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_procedure_stmt(HiveSqlParser.Create_procedure_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_routine_params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_routine_params(HiveSqlParser.Create_routine_paramsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_routine_param_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_routine_param_item(HiveSqlParser.Create_routine_param_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_routine_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_routine_options(HiveSqlParser.Create_routine_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_routine_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_routine_option(HiveSqlParser.Create_routine_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#drop_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_stmt(HiveSqlParser.Drop_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#end_transaction_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnd_transaction_stmt(HiveSqlParser.End_transaction_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#exec_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExec_stmt(HiveSqlParser.Exec_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#if_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_stmt(HiveSqlParser.If_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#if_plsql_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_plsql_stmt(HiveSqlParser.If_plsql_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#if_tsql_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_tsql_stmt(HiveSqlParser.If_tsql_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#if_bteq_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_bteq_stmt(HiveSqlParser.If_bteq_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#elseif_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseif_block(HiveSqlParser.Elseif_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#else_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElse_block(HiveSqlParser.Else_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#include_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInclude_stmt(HiveSqlParser.Include_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#insert_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_stmt(HiveSqlParser.Insert_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#insert_stmt_cols}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_stmt_cols(HiveSqlParser.Insert_stmt_colsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#insert_stmt_rows}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_stmt_rows(HiveSqlParser.Insert_stmt_rowsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#insert_stmt_row}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_stmt_row(HiveSqlParser.Insert_stmt_rowContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#insert_directory_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_directory_stmt(HiveSqlParser.Insert_directory_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#exit_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExit_stmt(HiveSqlParser.Exit_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#get_diag_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGet_diag_stmt(HiveSqlParser.Get_diag_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#get_diag_stmt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGet_diag_stmt_item(HiveSqlParser.Get_diag_stmt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#get_diag_stmt_exception_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGet_diag_stmt_exception_item(HiveSqlParser.Get_diag_stmt_exception_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#get_diag_stmt_rowcount_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGet_diag_stmt_rowcount_item(HiveSqlParser.Get_diag_stmt_rowcount_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#grant_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant_stmt(HiveSqlParser.Grant_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#grant_stmt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant_stmt_item(HiveSqlParser.Grant_stmt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#leave_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLeave_stmt(HiveSqlParser.Leave_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#map_object_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMap_object_stmt(HiveSqlParser.Map_object_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#open_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_stmt(HiveSqlParser.Open_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#fetch_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetch_stmt(HiveSqlParser.Fetch_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#collect_stats_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollect_stats_stmt(HiveSqlParser.Collect_stats_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#collect_stats_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollect_stats_clause(HiveSqlParser.Collect_stats_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#close_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClose_stmt(HiveSqlParser.Close_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#cmp_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCmp_stmt(HiveSqlParser.Cmp_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#cmp_source}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCmp_source(HiveSqlParser.Cmp_sourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#copy_from_local_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_from_local_stmt(HiveSqlParser.Copy_from_local_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#copy_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_stmt(HiveSqlParser.Copy_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#copy_source}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_source(HiveSqlParser.Copy_sourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#copy_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_target(HiveSqlParser.Copy_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#copy_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_option(HiveSqlParser.Copy_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#copy_file_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_file_option(HiveSqlParser.Copy_file_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#commit_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommit_stmt(HiveSqlParser.Commit_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_index_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index_stmt(HiveSqlParser.Create_index_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#create_index_col}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index_col(HiveSqlParser.Create_index_colContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#index_storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_storage_clause(HiveSqlParser.Index_storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#index_mssql_storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_mssql_storage_clause(HiveSqlParser.Index_mssql_storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#print_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrint_stmt(HiveSqlParser.Print_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#quit_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuit_stmt(HiveSqlParser.Quit_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#raise_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRaise_stmt(HiveSqlParser.Raise_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#resignal_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResignal_stmt(HiveSqlParser.Resignal_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#return_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_stmt(HiveSqlParser.Return_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#rollback_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollback_stmt(HiveSqlParser.Rollback_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#set_session_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_session_option(HiveSqlParser.Set_session_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#set_current_schema_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_current_schema_option(HiveSqlParser.Set_current_schema_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#set_mssql_session_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_mssql_session_option(HiveSqlParser.Set_mssql_session_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#set_teradata_session_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_teradata_session_option(HiveSqlParser.Set_teradata_session_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#signal_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignal_stmt(HiveSqlParser.Signal_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#summary_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSummary_stmt(HiveSqlParser.Summary_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#truncate_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTruncate_stmt(HiveSqlParser.Truncate_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#use_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUse_stmt(HiveSqlParser.Use_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#values_into_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValues_into_stmt(HiveSqlParser.Values_into_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#while_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile_stmt(HiveSqlParser.While_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#for_cursor_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_cursor_stmt(HiveSqlParser.For_cursor_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#for_range_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_range_stmt(HiveSqlParser.For_range_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel(HiveSqlParser.LabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#using_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_clause(HiveSqlParser.Using_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#select_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_stmt(HiveSqlParser.Select_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#cte_select_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCte_select_stmt(HiveSqlParser.Cte_select_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#cte_select_stmt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCte_select_stmt_item(HiveSqlParser.Cte_select_stmt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#cte_select_cols}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCte_select_cols(HiveSqlParser.Cte_select_colsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#fullselect_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFullselect_stmt(HiveSqlParser.Fullselect_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#fullselect_stmt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFullselect_stmt_item(HiveSqlParser.Fullselect_stmt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#fullselect_set_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFullselect_set_clause(HiveSqlParser.Fullselect_set_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#subselect_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubselect_stmt(HiveSqlParser.Subselect_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#select_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list(HiveSqlParser.Select_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#select_list_set}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list_set(HiveSqlParser.Select_list_setContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#select_list_limit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list_limit(HiveSqlParser.Select_list_limitContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#select_list_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list_item(HiveSqlParser.Select_list_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#select_list_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list_alias(HiveSqlParser.Select_list_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#select_list_asterisk}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list_asterisk(HiveSqlParser.Select_list_asteriskContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#into_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInto_clause(HiveSqlParser.Into_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#from_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_clause(HiveSqlParser.From_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#from_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_table_clause(HiveSqlParser.From_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#from_table_name_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_table_name_clause(HiveSqlParser.From_table_name_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#from_subselect_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_subselect_clause(HiveSqlParser.From_subselect_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#from_join_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_join_clause(HiveSqlParser.From_join_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#from_join_type_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_join_type_clause(HiveSqlParser.From_join_type_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#from_table_values_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_table_values_clause(HiveSqlParser.From_table_values_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#from_table_values_row}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_table_values_row(HiveSqlParser.From_table_values_rowContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#from_alias_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_alias_clause(HiveSqlParser.From_alias_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#table_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_name(HiveSqlParser.Table_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#where_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhere_clause(HiveSqlParser.Where_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#group_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_by_clause(HiveSqlParser.Group_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#having_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHaving_clause(HiveSqlParser.Having_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#qualify_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualify_clause(HiveSqlParser.Qualify_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#order_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrder_by_clause(HiveSqlParser.Order_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#select_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_options(HiveSqlParser.Select_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#select_options_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_options_item(HiveSqlParser.Select_options_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#update_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_stmt(HiveSqlParser.Update_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#update_assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_assignment(HiveSqlParser.Update_assignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#update_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_table(HiveSqlParser.Update_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#update_upsert}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_upsert(HiveSqlParser.Update_upsertContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#merge_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_stmt(HiveSqlParser.Merge_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#merge_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_table(HiveSqlParser.Merge_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#merge_condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_condition(HiveSqlParser.Merge_conditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#merge_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_action(HiveSqlParser.Merge_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#delete_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_stmt(HiveSqlParser.Delete_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#delete_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_alias(HiveSqlParser.Delete_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#describe_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescribe_stmt(HiveSqlParser.Describe_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#bool_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_expr(HiveSqlParser.Bool_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#bool_expr_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_expr_atom(HiveSqlParser.Bool_expr_atomContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#bool_expr_unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_expr_unary(HiveSqlParser.Bool_expr_unaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#bool_expr_single_in}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_expr_single_in(HiveSqlParser.Bool_expr_single_inContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#bool_expr_multi_in}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_expr_multi_in(HiveSqlParser.Bool_expr_multi_inContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#bool_expr_binary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_expr_binary(HiveSqlParser.Bool_expr_binaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#bool_expr_logical_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_expr_logical_operator(HiveSqlParser.Bool_expr_logical_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#bool_expr_binary_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_expr_binary_operator(HiveSqlParser.Bool_expr_binary_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(HiveSqlParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_atom(HiveSqlParser.Expr_atomContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_interval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_interval(HiveSqlParser.Expr_intervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#interval_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval_item(HiveSqlParser.Interval_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_concat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_concat(HiveSqlParser.Expr_concatContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_concat_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_concat_item(HiveSqlParser.Expr_concat_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_case}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_case(HiveSqlParser.Expr_caseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_case_simple}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_case_simple(HiveSqlParser.Expr_case_simpleContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_case_searched}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_case_searched(HiveSqlParser.Expr_case_searchedContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_cursor_attribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_cursor_attribute(HiveSqlParser.Expr_cursor_attributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_agg_window_func}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_agg_window_func(HiveSqlParser.Expr_agg_window_funcContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_func_all_distinct}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_func_all_distinct(HiveSqlParser.Expr_func_all_distinctContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_func_over_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_func_over_clause(HiveSqlParser.Expr_func_over_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_func_partition_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_func_partition_by_clause(HiveSqlParser.Expr_func_partition_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_spec_func}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_spec_func(HiveSqlParser.Expr_spec_funcContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_func}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_func(HiveSqlParser.Expr_funcContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_func_params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_func_params(HiveSqlParser.Expr_func_paramsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#func_param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_param(HiveSqlParser.Func_paramContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_select}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_select(HiveSqlParser.Expr_selectContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#expr_file}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_file(HiveSqlParser.Expr_fileContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#hive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHive(HiveSqlParser.HiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#hive_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHive_item(HiveSqlParser.Hive_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#host}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHost(HiveSqlParser.HostContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#host_cmd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHost_cmd(HiveSqlParser.Host_cmdContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#host_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHost_stmt(HiveSqlParser.Host_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#file_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFile_name(HiveSqlParser.File_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#date_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDate_literal(HiveSqlParser.Date_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#timestamp_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimestamp_literal(HiveSqlParser.Timestamp_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#ident}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdent(HiveSqlParser.IdentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code single_quotedString}
	 * labeled alternative in {@link HiveSqlParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_quotedString(HiveSqlParser.Single_quotedStringContext ctx);
	/**
	 * Visit a parse tree produced by the {@code double_quotedString}
	 * labeled alternative in {@link HiveSqlParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDouble_quotedString(HiveSqlParser.Double_quotedStringContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#int_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInt_number(HiveSqlParser.Int_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#dec_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDec_number(HiveSqlParser.Dec_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#bool_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_literal(HiveSqlParser.Bool_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#null_const}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNull_const(HiveSqlParser.Null_constContext ctx);
	/**
	 * Visit a parse tree produced by {@link HiveSqlParser#non_reserved_words}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_reserved_words(HiveSqlParser.Non_reserved_wordsContext ctx);
}