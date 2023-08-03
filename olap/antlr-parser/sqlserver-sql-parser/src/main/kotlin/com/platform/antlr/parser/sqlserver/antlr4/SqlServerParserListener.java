// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.sqlserver.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SqlServerParser}.
 */
public interface SqlServerParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#tsql_file}.
	 * @param ctx the parse tree
	 */
	void enterTsql_file(SqlServerParser.Tsql_fileContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#tsql_file}.
	 * @param ctx the parse tree
	 */
	void exitTsql_file(SqlServerParser.Tsql_fileContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#batch}.
	 * @param ctx the parse tree
	 */
	void enterBatch(SqlServerParser.BatchContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#batch}.
	 * @param ctx the parse tree
	 */
	void exitBatch(SqlServerParser.BatchContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#batch_level_statement}.
	 * @param ctx the parse tree
	 */
	void enterBatch_level_statement(SqlServerParser.Batch_level_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#batch_level_statement}.
	 * @param ctx the parse tree
	 */
	void exitBatch_level_statement(SqlServerParser.Batch_level_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#sql_clauses}.
	 * @param ctx the parse tree
	 */
	void enterSql_clauses(SqlServerParser.Sql_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#sql_clauses}.
	 * @param ctx the parse tree
	 */
	void exitSql_clauses(SqlServerParser.Sql_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dml_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_clause(SqlServerParser.Dml_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dml_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_clause(SqlServerParser.Dml_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#ddl_clause}.
	 * @param ctx the parse tree
	 */
	void enterDdl_clause(SqlServerParser.Ddl_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#ddl_clause}.
	 * @param ctx the parse tree
	 */
	void exitDdl_clause(SqlServerParser.Ddl_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#backup_statement}.
	 * @param ctx the parse tree
	 */
	void enterBackup_statement(SqlServerParser.Backup_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#backup_statement}.
	 * @param ctx the parse tree
	 */
	void exitBackup_statement(SqlServerParser.Backup_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#cfl_statement}.
	 * @param ctx the parse tree
	 */
	void enterCfl_statement(SqlServerParser.Cfl_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#cfl_statement}.
	 * @param ctx the parse tree
	 */
	void exitCfl_statement(SqlServerParser.Cfl_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#block_statement}.
	 * @param ctx the parse tree
	 */
	void enterBlock_statement(SqlServerParser.Block_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#block_statement}.
	 * @param ctx the parse tree
	 */
	void exitBlock_statement(SqlServerParser.Block_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#break_statement}.
	 * @param ctx the parse tree
	 */
	void enterBreak_statement(SqlServerParser.Break_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#break_statement}.
	 * @param ctx the parse tree
	 */
	void exitBreak_statement(SqlServerParser.Break_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#continue_statement}.
	 * @param ctx the parse tree
	 */
	void enterContinue_statement(SqlServerParser.Continue_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#continue_statement}.
	 * @param ctx the parse tree
	 */
	void exitContinue_statement(SqlServerParser.Continue_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#goto_statement}.
	 * @param ctx the parse tree
	 */
	void enterGoto_statement(SqlServerParser.Goto_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#goto_statement}.
	 * @param ctx the parse tree
	 */
	void exitGoto_statement(SqlServerParser.Goto_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void enterReturn_statement(SqlServerParser.Return_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void exitReturn_statement(SqlServerParser.Return_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void enterIf_statement(SqlServerParser.If_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void exitIf_statement(SqlServerParser.If_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#throw_statement}.
	 * @param ctx the parse tree
	 */
	void enterThrow_statement(SqlServerParser.Throw_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#throw_statement}.
	 * @param ctx the parse tree
	 */
	void exitThrow_statement(SqlServerParser.Throw_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#throw_error_number}.
	 * @param ctx the parse tree
	 */
	void enterThrow_error_number(SqlServerParser.Throw_error_numberContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#throw_error_number}.
	 * @param ctx the parse tree
	 */
	void exitThrow_error_number(SqlServerParser.Throw_error_numberContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#throw_message}.
	 * @param ctx the parse tree
	 */
	void enterThrow_message(SqlServerParser.Throw_messageContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#throw_message}.
	 * @param ctx the parse tree
	 */
	void exitThrow_message(SqlServerParser.Throw_messageContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#throw_state}.
	 * @param ctx the parse tree
	 */
	void enterThrow_state(SqlServerParser.Throw_stateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#throw_state}.
	 * @param ctx the parse tree
	 */
	void exitThrow_state(SqlServerParser.Throw_stateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#try_catch_statement}.
	 * @param ctx the parse tree
	 */
	void enterTry_catch_statement(SqlServerParser.Try_catch_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#try_catch_statement}.
	 * @param ctx the parse tree
	 */
	void exitTry_catch_statement(SqlServerParser.Try_catch_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#waitfor_statement}.
	 * @param ctx the parse tree
	 */
	void enterWaitfor_statement(SqlServerParser.Waitfor_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#waitfor_statement}.
	 * @param ctx the parse tree
	 */
	void exitWaitfor_statement(SqlServerParser.Waitfor_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#while_statement}.
	 * @param ctx the parse tree
	 */
	void enterWhile_statement(SqlServerParser.While_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#while_statement}.
	 * @param ctx the parse tree
	 */
	void exitWhile_statement(SqlServerParser.While_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#print_statement}.
	 * @param ctx the parse tree
	 */
	void enterPrint_statement(SqlServerParser.Print_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#print_statement}.
	 * @param ctx the parse tree
	 */
	void exitPrint_statement(SqlServerParser.Print_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#raiseerror_statement}.
	 * @param ctx the parse tree
	 */
	void enterRaiseerror_statement(SqlServerParser.Raiseerror_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#raiseerror_statement}.
	 * @param ctx the parse tree
	 */
	void exitRaiseerror_statement(SqlServerParser.Raiseerror_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#empty_statement}.
	 * @param ctx the parse tree
	 */
	void enterEmpty_statement(SqlServerParser.Empty_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#empty_statement}.
	 * @param ctx the parse tree
	 */
	void exitEmpty_statement(SqlServerParser.Empty_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#another_statement}.
	 * @param ctx the parse tree
	 */
	void enterAnother_statement(SqlServerParser.Another_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#another_statement}.
	 * @param ctx the parse tree
	 */
	void exitAnother_statement(SqlServerParser.Another_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_application_role}.
	 * @param ctx the parse tree
	 */
	void enterAlter_application_role(SqlServerParser.Alter_application_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_application_role}.
	 * @param ctx the parse tree
	 */
	void exitAlter_application_role(SqlServerParser.Alter_application_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_xml_schema_collection}.
	 * @param ctx the parse tree
	 */
	void enterAlter_xml_schema_collection(SqlServerParser.Alter_xml_schema_collectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_xml_schema_collection}.
	 * @param ctx the parse tree
	 */
	void exitAlter_xml_schema_collection(SqlServerParser.Alter_xml_schema_collectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_application_role}.
	 * @param ctx the parse tree
	 */
	void enterCreate_application_role(SqlServerParser.Create_application_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_application_role}.
	 * @param ctx the parse tree
	 */
	void exitCreate_application_role(SqlServerParser.Create_application_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_aggregate}.
	 * @param ctx the parse tree
	 */
	void enterDrop_aggregate(SqlServerParser.Drop_aggregateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_aggregate}.
	 * @param ctx the parse tree
	 */
	void exitDrop_aggregate(SqlServerParser.Drop_aggregateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_application_role}.
	 * @param ctx the parse tree
	 */
	void enterDrop_application_role(SqlServerParser.Drop_application_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_application_role}.
	 * @param ctx the parse tree
	 */
	void exitDrop_application_role(SqlServerParser.Drop_application_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly(SqlServerParser.Alter_assemblyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly(SqlServerParser.Alter_assemblyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_start}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_start(SqlServerParser.Alter_assembly_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_start}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_start(SqlServerParser.Alter_assembly_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_clause(SqlServerParser.Alter_assembly_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_clause(SqlServerParser.Alter_assembly_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_from_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_from_clause(SqlServerParser.Alter_assembly_from_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_from_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_from_clause(SqlServerParser.Alter_assembly_from_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_from_clause_start}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_from_clause_start(SqlServerParser.Alter_assembly_from_clause_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_from_clause_start}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_from_clause_start(SqlServerParser.Alter_assembly_from_clause_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_drop_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_drop_clause(SqlServerParser.Alter_assembly_drop_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_drop_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_drop_clause(SqlServerParser.Alter_assembly_drop_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_drop_multiple_files}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_drop_multiple_files(SqlServerParser.Alter_assembly_drop_multiple_filesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_drop_multiple_files}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_drop_multiple_files(SqlServerParser.Alter_assembly_drop_multiple_filesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_drop}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_drop(SqlServerParser.Alter_assembly_dropContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_drop}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_drop(SqlServerParser.Alter_assembly_dropContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_add_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_add_clause(SqlServerParser.Alter_assembly_add_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_add_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_add_clause(SqlServerParser.Alter_assembly_add_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_asssembly_add_clause_start}.
	 * @param ctx the parse tree
	 */
	void enterAlter_asssembly_add_clause_start(SqlServerParser.Alter_asssembly_add_clause_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_asssembly_add_clause_start}.
	 * @param ctx the parse tree
	 */
	void exitAlter_asssembly_add_clause_start(SqlServerParser.Alter_asssembly_add_clause_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_client_file_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_client_file_clause(SqlServerParser.Alter_assembly_client_file_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_client_file_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_client_file_clause(SqlServerParser.Alter_assembly_client_file_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_file_name}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_file_name(SqlServerParser.Alter_assembly_file_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_file_name}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_file_name(SqlServerParser.Alter_assembly_file_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_file_bits}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_file_bits(SqlServerParser.Alter_assembly_file_bitsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_file_bits}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_file_bits(SqlServerParser.Alter_assembly_file_bitsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_as}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_as(SqlServerParser.Alter_assembly_asContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_as}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_as(SqlServerParser.Alter_assembly_asContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_with_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_with_clause(SqlServerParser.Alter_assembly_with_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_with_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_with_clause(SqlServerParser.Alter_assembly_with_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_assembly_with}.
	 * @param ctx the parse tree
	 */
	void enterAlter_assembly_with(SqlServerParser.Alter_assembly_withContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_assembly_with}.
	 * @param ctx the parse tree
	 */
	void exitAlter_assembly_with(SqlServerParser.Alter_assembly_withContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#client_assembly_specifier}.
	 * @param ctx the parse tree
	 */
	void enterClient_assembly_specifier(SqlServerParser.Client_assembly_specifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#client_assembly_specifier}.
	 * @param ctx the parse tree
	 */
	void exitClient_assembly_specifier(SqlServerParser.Client_assembly_specifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#assembly_option}.
	 * @param ctx the parse tree
	 */
	void enterAssembly_option(SqlServerParser.Assembly_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#assembly_option}.
	 * @param ctx the parse tree
	 */
	void exitAssembly_option(SqlServerParser.Assembly_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#network_file_share}.
	 * @param ctx the parse tree
	 */
	void enterNetwork_file_share(SqlServerParser.Network_file_shareContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#network_file_share}.
	 * @param ctx the parse tree
	 */
	void exitNetwork_file_share(SqlServerParser.Network_file_shareContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#network_computer}.
	 * @param ctx the parse tree
	 */
	void enterNetwork_computer(SqlServerParser.Network_computerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#network_computer}.
	 * @param ctx the parse tree
	 */
	void exitNetwork_computer(SqlServerParser.Network_computerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#network_file_start}.
	 * @param ctx the parse tree
	 */
	void enterNetwork_file_start(SqlServerParser.Network_file_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#network_file_start}.
	 * @param ctx the parse tree
	 */
	void exitNetwork_file_start(SqlServerParser.Network_file_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#file_path}.
	 * @param ctx the parse tree
	 */
	void enterFile_path(SqlServerParser.File_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#file_path}.
	 * @param ctx the parse tree
	 */
	void exitFile_path(SqlServerParser.File_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#file_directory_path_separator}.
	 * @param ctx the parse tree
	 */
	void enterFile_directory_path_separator(SqlServerParser.File_directory_path_separatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#file_directory_path_separator}.
	 * @param ctx the parse tree
	 */
	void exitFile_directory_path_separator(SqlServerParser.File_directory_path_separatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#local_file}.
	 * @param ctx the parse tree
	 */
	void enterLocal_file(SqlServerParser.Local_fileContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#local_file}.
	 * @param ctx the parse tree
	 */
	void exitLocal_file(SqlServerParser.Local_fileContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#local_drive}.
	 * @param ctx the parse tree
	 */
	void enterLocal_drive(SqlServerParser.Local_driveContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#local_drive}.
	 * @param ctx the parse tree
	 */
	void exitLocal_drive(SqlServerParser.Local_driveContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#multiple_local_files}.
	 * @param ctx the parse tree
	 */
	void enterMultiple_local_files(SqlServerParser.Multiple_local_filesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#multiple_local_files}.
	 * @param ctx the parse tree
	 */
	void exitMultiple_local_files(SqlServerParser.Multiple_local_filesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#multiple_local_file_start}.
	 * @param ctx the parse tree
	 */
	void enterMultiple_local_file_start(SqlServerParser.Multiple_local_file_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#multiple_local_file_start}.
	 * @param ctx the parse tree
	 */
	void exitMultiple_local_file_start(SqlServerParser.Multiple_local_file_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_assembly}.
	 * @param ctx the parse tree
	 */
	void enterCreate_assembly(SqlServerParser.Create_assemblyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_assembly}.
	 * @param ctx the parse tree
	 */
	void exitCreate_assembly(SqlServerParser.Create_assemblyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_assembly}.
	 * @param ctx the parse tree
	 */
	void enterDrop_assembly(SqlServerParser.Drop_assemblyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_assembly}.
	 * @param ctx the parse tree
	 */
	void exitDrop_assembly(SqlServerParser.Drop_assemblyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_asymmetric_key}.
	 * @param ctx the parse tree
	 */
	void enterAlter_asymmetric_key(SqlServerParser.Alter_asymmetric_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_asymmetric_key}.
	 * @param ctx the parse tree
	 */
	void exitAlter_asymmetric_key(SqlServerParser.Alter_asymmetric_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_asymmetric_key_start}.
	 * @param ctx the parse tree
	 */
	void enterAlter_asymmetric_key_start(SqlServerParser.Alter_asymmetric_key_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_asymmetric_key_start}.
	 * @param ctx the parse tree
	 */
	void exitAlter_asymmetric_key_start(SqlServerParser.Alter_asymmetric_key_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#asymmetric_key_option}.
	 * @param ctx the parse tree
	 */
	void enterAsymmetric_key_option(SqlServerParser.Asymmetric_key_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#asymmetric_key_option}.
	 * @param ctx the parse tree
	 */
	void exitAsymmetric_key_option(SqlServerParser.Asymmetric_key_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#asymmetric_key_option_start}.
	 * @param ctx the parse tree
	 */
	void enterAsymmetric_key_option_start(SqlServerParser.Asymmetric_key_option_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#asymmetric_key_option_start}.
	 * @param ctx the parse tree
	 */
	void exitAsymmetric_key_option_start(SqlServerParser.Asymmetric_key_option_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#asymmetric_key_password_change_option}.
	 * @param ctx the parse tree
	 */
	void enterAsymmetric_key_password_change_option(SqlServerParser.Asymmetric_key_password_change_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#asymmetric_key_password_change_option}.
	 * @param ctx the parse tree
	 */
	void exitAsymmetric_key_password_change_option(SqlServerParser.Asymmetric_key_password_change_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_asymmetric_key}.
	 * @param ctx the parse tree
	 */
	void enterCreate_asymmetric_key(SqlServerParser.Create_asymmetric_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_asymmetric_key}.
	 * @param ctx the parse tree
	 */
	void exitCreate_asymmetric_key(SqlServerParser.Create_asymmetric_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_asymmetric_key}.
	 * @param ctx the parse tree
	 */
	void enterDrop_asymmetric_key(SqlServerParser.Drop_asymmetric_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_asymmetric_key}.
	 * @param ctx the parse tree
	 */
	void exitDrop_asymmetric_key(SqlServerParser.Drop_asymmetric_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_authorization}.
	 * @param ctx the parse tree
	 */
	void enterAlter_authorization(SqlServerParser.Alter_authorizationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_authorization}.
	 * @param ctx the parse tree
	 */
	void exitAlter_authorization(SqlServerParser.Alter_authorizationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#authorization_grantee}.
	 * @param ctx the parse tree
	 */
	void enterAuthorization_grantee(SqlServerParser.Authorization_granteeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#authorization_grantee}.
	 * @param ctx the parse tree
	 */
	void exitAuthorization_grantee(SqlServerParser.Authorization_granteeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#entity_to}.
	 * @param ctx the parse tree
	 */
	void enterEntity_to(SqlServerParser.Entity_toContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#entity_to}.
	 * @param ctx the parse tree
	 */
	void exitEntity_to(SqlServerParser.Entity_toContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#colon_colon}.
	 * @param ctx the parse tree
	 */
	void enterColon_colon(SqlServerParser.Colon_colonContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#colon_colon}.
	 * @param ctx the parse tree
	 */
	void exitColon_colon(SqlServerParser.Colon_colonContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_authorization_start}.
	 * @param ctx the parse tree
	 */
	void enterAlter_authorization_start(SqlServerParser.Alter_authorization_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_authorization_start}.
	 * @param ctx the parse tree
	 */
	void exitAlter_authorization_start(SqlServerParser.Alter_authorization_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_authorization_for_sql_database}.
	 * @param ctx the parse tree
	 */
	void enterAlter_authorization_for_sql_database(SqlServerParser.Alter_authorization_for_sql_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_authorization_for_sql_database}.
	 * @param ctx the parse tree
	 */
	void exitAlter_authorization_for_sql_database(SqlServerParser.Alter_authorization_for_sql_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_authorization_for_azure_dw}.
	 * @param ctx the parse tree
	 */
	void enterAlter_authorization_for_azure_dw(SqlServerParser.Alter_authorization_for_azure_dwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_authorization_for_azure_dw}.
	 * @param ctx the parse tree
	 */
	void exitAlter_authorization_for_azure_dw(SqlServerParser.Alter_authorization_for_azure_dwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_authorization_for_parallel_dw}.
	 * @param ctx the parse tree
	 */
	void enterAlter_authorization_for_parallel_dw(SqlServerParser.Alter_authorization_for_parallel_dwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_authorization_for_parallel_dw}.
	 * @param ctx the parse tree
	 */
	void exitAlter_authorization_for_parallel_dw(SqlServerParser.Alter_authorization_for_parallel_dwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#class_type}.
	 * @param ctx the parse tree
	 */
	void enterClass_type(SqlServerParser.Class_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#class_type}.
	 * @param ctx the parse tree
	 */
	void exitClass_type(SqlServerParser.Class_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#class_type_for_sql_database}.
	 * @param ctx the parse tree
	 */
	void enterClass_type_for_sql_database(SqlServerParser.Class_type_for_sql_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#class_type_for_sql_database}.
	 * @param ctx the parse tree
	 */
	void exitClass_type_for_sql_database(SqlServerParser.Class_type_for_sql_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#class_type_for_azure_dw}.
	 * @param ctx the parse tree
	 */
	void enterClass_type_for_azure_dw(SqlServerParser.Class_type_for_azure_dwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#class_type_for_azure_dw}.
	 * @param ctx the parse tree
	 */
	void exitClass_type_for_azure_dw(SqlServerParser.Class_type_for_azure_dwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#class_type_for_parallel_dw}.
	 * @param ctx the parse tree
	 */
	void enterClass_type_for_parallel_dw(SqlServerParser.Class_type_for_parallel_dwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#class_type_for_parallel_dw}.
	 * @param ctx the parse tree
	 */
	void exitClass_type_for_parallel_dw(SqlServerParser.Class_type_for_parallel_dwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#class_type_for_grant}.
	 * @param ctx the parse tree
	 */
	void enterClass_type_for_grant(SqlServerParser.Class_type_for_grantContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#class_type_for_grant}.
	 * @param ctx the parse tree
	 */
	void exitClass_type_for_grant(SqlServerParser.Class_type_for_grantContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_availability_group}.
	 * @param ctx the parse tree
	 */
	void enterDrop_availability_group(SqlServerParser.Drop_availability_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_availability_group}.
	 * @param ctx the parse tree
	 */
	void exitDrop_availability_group(SqlServerParser.Drop_availability_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_availability_group}.
	 * @param ctx the parse tree
	 */
	void enterAlter_availability_group(SqlServerParser.Alter_availability_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_availability_group}.
	 * @param ctx the parse tree
	 */
	void exitAlter_availability_group(SqlServerParser.Alter_availability_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_availability_group_start}.
	 * @param ctx the parse tree
	 */
	void enterAlter_availability_group_start(SqlServerParser.Alter_availability_group_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_availability_group_start}.
	 * @param ctx the parse tree
	 */
	void exitAlter_availability_group_start(SqlServerParser.Alter_availability_group_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_availability_group_options}.
	 * @param ctx the parse tree
	 */
	void enterAlter_availability_group_options(SqlServerParser.Alter_availability_group_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_availability_group_options}.
	 * @param ctx the parse tree
	 */
	void exitAlter_availability_group_options(SqlServerParser.Alter_availability_group_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#ip_v4_failover}.
	 * @param ctx the parse tree
	 */
	void enterIp_v4_failover(SqlServerParser.Ip_v4_failoverContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#ip_v4_failover}.
	 * @param ctx the parse tree
	 */
	void exitIp_v4_failover(SqlServerParser.Ip_v4_failoverContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#ip_v6_failover}.
	 * @param ctx the parse tree
	 */
	void enterIp_v6_failover(SqlServerParser.Ip_v6_failoverContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#ip_v6_failover}.
	 * @param ctx the parse tree
	 */
	void exitIp_v6_failover(SqlServerParser.Ip_v6_failoverContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_or_alter_broker_priority}.
	 * @param ctx the parse tree
	 */
	void enterCreate_or_alter_broker_priority(SqlServerParser.Create_or_alter_broker_priorityContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_or_alter_broker_priority}.
	 * @param ctx the parse tree
	 */
	void exitCreate_or_alter_broker_priority(SqlServerParser.Create_or_alter_broker_priorityContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_broker_priority}.
	 * @param ctx the parse tree
	 */
	void enterDrop_broker_priority(SqlServerParser.Drop_broker_priorityContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_broker_priority}.
	 * @param ctx the parse tree
	 */
	void exitDrop_broker_priority(SqlServerParser.Drop_broker_priorityContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_certificate}.
	 * @param ctx the parse tree
	 */
	void enterAlter_certificate(SqlServerParser.Alter_certificateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_certificate}.
	 * @param ctx the parse tree
	 */
	void exitAlter_certificate(SqlServerParser.Alter_certificateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_column_encryption_key}.
	 * @param ctx the parse tree
	 */
	void enterAlter_column_encryption_key(SqlServerParser.Alter_column_encryption_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_column_encryption_key}.
	 * @param ctx the parse tree
	 */
	void exitAlter_column_encryption_key(SqlServerParser.Alter_column_encryption_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_column_encryption_key}.
	 * @param ctx the parse tree
	 */
	void enterCreate_column_encryption_key(SqlServerParser.Create_column_encryption_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_column_encryption_key}.
	 * @param ctx the parse tree
	 */
	void exitCreate_column_encryption_key(SqlServerParser.Create_column_encryption_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_certificate}.
	 * @param ctx the parse tree
	 */
	void enterDrop_certificate(SqlServerParser.Drop_certificateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_certificate}.
	 * @param ctx the parse tree
	 */
	void exitDrop_certificate(SqlServerParser.Drop_certificateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_column_encryption_key}.
	 * @param ctx the parse tree
	 */
	void enterDrop_column_encryption_key(SqlServerParser.Drop_column_encryption_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_column_encryption_key}.
	 * @param ctx the parse tree
	 */
	void exitDrop_column_encryption_key(SqlServerParser.Drop_column_encryption_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_column_master_key}.
	 * @param ctx the parse tree
	 */
	void enterDrop_column_master_key(SqlServerParser.Drop_column_master_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_column_master_key}.
	 * @param ctx the parse tree
	 */
	void exitDrop_column_master_key(SqlServerParser.Drop_column_master_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_contract}.
	 * @param ctx the parse tree
	 */
	void enterDrop_contract(SqlServerParser.Drop_contractContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_contract}.
	 * @param ctx the parse tree
	 */
	void exitDrop_contract(SqlServerParser.Drop_contractContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_credential}.
	 * @param ctx the parse tree
	 */
	void enterDrop_credential(SqlServerParser.Drop_credentialContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_credential}.
	 * @param ctx the parse tree
	 */
	void exitDrop_credential(SqlServerParser.Drop_credentialContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_cryptograhic_provider}.
	 * @param ctx the parse tree
	 */
	void enterDrop_cryptograhic_provider(SqlServerParser.Drop_cryptograhic_providerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_cryptograhic_provider}.
	 * @param ctx the parse tree
	 */
	void exitDrop_cryptograhic_provider(SqlServerParser.Drop_cryptograhic_providerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_database}.
	 * @param ctx the parse tree
	 */
	void enterDrop_database(SqlServerParser.Drop_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_database}.
	 * @param ctx the parse tree
	 */
	void exitDrop_database(SqlServerParser.Drop_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_database_audit_specification}.
	 * @param ctx the parse tree
	 */
	void enterDrop_database_audit_specification(SqlServerParser.Drop_database_audit_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_database_audit_specification}.
	 * @param ctx the parse tree
	 */
	void exitDrop_database_audit_specification(SqlServerParser.Drop_database_audit_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_database_encryption_key}.
	 * @param ctx the parse tree
	 */
	void enterDrop_database_encryption_key(SqlServerParser.Drop_database_encryption_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_database_encryption_key}.
	 * @param ctx the parse tree
	 */
	void exitDrop_database_encryption_key(SqlServerParser.Drop_database_encryption_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_database_scoped_credential}.
	 * @param ctx the parse tree
	 */
	void enterDrop_database_scoped_credential(SqlServerParser.Drop_database_scoped_credentialContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_database_scoped_credential}.
	 * @param ctx the parse tree
	 */
	void exitDrop_database_scoped_credential(SqlServerParser.Drop_database_scoped_credentialContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_default}.
	 * @param ctx the parse tree
	 */
	void enterDrop_default(SqlServerParser.Drop_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_default}.
	 * @param ctx the parse tree
	 */
	void exitDrop_default(SqlServerParser.Drop_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_endpoint}.
	 * @param ctx the parse tree
	 */
	void enterDrop_endpoint(SqlServerParser.Drop_endpointContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_endpoint}.
	 * @param ctx the parse tree
	 */
	void exitDrop_endpoint(SqlServerParser.Drop_endpointContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_external_data_source}.
	 * @param ctx the parse tree
	 */
	void enterDrop_external_data_source(SqlServerParser.Drop_external_data_sourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_external_data_source}.
	 * @param ctx the parse tree
	 */
	void exitDrop_external_data_source(SqlServerParser.Drop_external_data_sourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_external_file_format}.
	 * @param ctx the parse tree
	 */
	void enterDrop_external_file_format(SqlServerParser.Drop_external_file_formatContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_external_file_format}.
	 * @param ctx the parse tree
	 */
	void exitDrop_external_file_format(SqlServerParser.Drop_external_file_formatContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_external_library}.
	 * @param ctx the parse tree
	 */
	void enterDrop_external_library(SqlServerParser.Drop_external_libraryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_external_library}.
	 * @param ctx the parse tree
	 */
	void exitDrop_external_library(SqlServerParser.Drop_external_libraryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_external_resource_pool}.
	 * @param ctx the parse tree
	 */
	void enterDrop_external_resource_pool(SqlServerParser.Drop_external_resource_poolContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_external_resource_pool}.
	 * @param ctx the parse tree
	 */
	void exitDrop_external_resource_pool(SqlServerParser.Drop_external_resource_poolContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_external_table}.
	 * @param ctx the parse tree
	 */
	void enterDrop_external_table(SqlServerParser.Drop_external_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_external_table}.
	 * @param ctx the parse tree
	 */
	void exitDrop_external_table(SqlServerParser.Drop_external_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_event_notifications}.
	 * @param ctx the parse tree
	 */
	void enterDrop_event_notifications(SqlServerParser.Drop_event_notificationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_event_notifications}.
	 * @param ctx the parse tree
	 */
	void exitDrop_event_notifications(SqlServerParser.Drop_event_notificationsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_event_session}.
	 * @param ctx the parse tree
	 */
	void enterDrop_event_session(SqlServerParser.Drop_event_sessionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_event_session}.
	 * @param ctx the parse tree
	 */
	void exitDrop_event_session(SqlServerParser.Drop_event_sessionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_fulltext_catalog}.
	 * @param ctx the parse tree
	 */
	void enterDrop_fulltext_catalog(SqlServerParser.Drop_fulltext_catalogContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_fulltext_catalog}.
	 * @param ctx the parse tree
	 */
	void exitDrop_fulltext_catalog(SqlServerParser.Drop_fulltext_catalogContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_fulltext_index}.
	 * @param ctx the parse tree
	 */
	void enterDrop_fulltext_index(SqlServerParser.Drop_fulltext_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_fulltext_index}.
	 * @param ctx the parse tree
	 */
	void exitDrop_fulltext_index(SqlServerParser.Drop_fulltext_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_fulltext_stoplist}.
	 * @param ctx the parse tree
	 */
	void enterDrop_fulltext_stoplist(SqlServerParser.Drop_fulltext_stoplistContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_fulltext_stoplist}.
	 * @param ctx the parse tree
	 */
	void exitDrop_fulltext_stoplist(SqlServerParser.Drop_fulltext_stoplistContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_login}.
	 * @param ctx the parse tree
	 */
	void enterDrop_login(SqlServerParser.Drop_loginContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_login}.
	 * @param ctx the parse tree
	 */
	void exitDrop_login(SqlServerParser.Drop_loginContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_master_key}.
	 * @param ctx the parse tree
	 */
	void enterDrop_master_key(SqlServerParser.Drop_master_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_master_key}.
	 * @param ctx the parse tree
	 */
	void exitDrop_master_key(SqlServerParser.Drop_master_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_message_type}.
	 * @param ctx the parse tree
	 */
	void enterDrop_message_type(SqlServerParser.Drop_message_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_message_type}.
	 * @param ctx the parse tree
	 */
	void exitDrop_message_type(SqlServerParser.Drop_message_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_partition_function}.
	 * @param ctx the parse tree
	 */
	void enterDrop_partition_function(SqlServerParser.Drop_partition_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_partition_function}.
	 * @param ctx the parse tree
	 */
	void exitDrop_partition_function(SqlServerParser.Drop_partition_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_partition_scheme}.
	 * @param ctx the parse tree
	 */
	void enterDrop_partition_scheme(SqlServerParser.Drop_partition_schemeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_partition_scheme}.
	 * @param ctx the parse tree
	 */
	void exitDrop_partition_scheme(SqlServerParser.Drop_partition_schemeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_queue}.
	 * @param ctx the parse tree
	 */
	void enterDrop_queue(SqlServerParser.Drop_queueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_queue}.
	 * @param ctx the parse tree
	 */
	void exitDrop_queue(SqlServerParser.Drop_queueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_remote_service_binding}.
	 * @param ctx the parse tree
	 */
	void enterDrop_remote_service_binding(SqlServerParser.Drop_remote_service_bindingContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_remote_service_binding}.
	 * @param ctx the parse tree
	 */
	void exitDrop_remote_service_binding(SqlServerParser.Drop_remote_service_bindingContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_resource_pool}.
	 * @param ctx the parse tree
	 */
	void enterDrop_resource_pool(SqlServerParser.Drop_resource_poolContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_resource_pool}.
	 * @param ctx the parse tree
	 */
	void exitDrop_resource_pool(SqlServerParser.Drop_resource_poolContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_db_role}.
	 * @param ctx the parse tree
	 */
	void enterDrop_db_role(SqlServerParser.Drop_db_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_db_role}.
	 * @param ctx the parse tree
	 */
	void exitDrop_db_role(SqlServerParser.Drop_db_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_route}.
	 * @param ctx the parse tree
	 */
	void enterDrop_route(SqlServerParser.Drop_routeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_route}.
	 * @param ctx the parse tree
	 */
	void exitDrop_route(SqlServerParser.Drop_routeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_rule}.
	 * @param ctx the parse tree
	 */
	void enterDrop_rule(SqlServerParser.Drop_ruleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_rule}.
	 * @param ctx the parse tree
	 */
	void exitDrop_rule(SqlServerParser.Drop_ruleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_schema}.
	 * @param ctx the parse tree
	 */
	void enterDrop_schema(SqlServerParser.Drop_schemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_schema}.
	 * @param ctx the parse tree
	 */
	void exitDrop_schema(SqlServerParser.Drop_schemaContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_search_property_list}.
	 * @param ctx the parse tree
	 */
	void enterDrop_search_property_list(SqlServerParser.Drop_search_property_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_search_property_list}.
	 * @param ctx the parse tree
	 */
	void exitDrop_search_property_list(SqlServerParser.Drop_search_property_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_security_policy}.
	 * @param ctx the parse tree
	 */
	void enterDrop_security_policy(SqlServerParser.Drop_security_policyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_security_policy}.
	 * @param ctx the parse tree
	 */
	void exitDrop_security_policy(SqlServerParser.Drop_security_policyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_sequence}.
	 * @param ctx the parse tree
	 */
	void enterDrop_sequence(SqlServerParser.Drop_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_sequence}.
	 * @param ctx the parse tree
	 */
	void exitDrop_sequence(SqlServerParser.Drop_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_server_audit}.
	 * @param ctx the parse tree
	 */
	void enterDrop_server_audit(SqlServerParser.Drop_server_auditContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_server_audit}.
	 * @param ctx the parse tree
	 */
	void exitDrop_server_audit(SqlServerParser.Drop_server_auditContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_server_audit_specification}.
	 * @param ctx the parse tree
	 */
	void enterDrop_server_audit_specification(SqlServerParser.Drop_server_audit_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_server_audit_specification}.
	 * @param ctx the parse tree
	 */
	void exitDrop_server_audit_specification(SqlServerParser.Drop_server_audit_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_server_role}.
	 * @param ctx the parse tree
	 */
	void enterDrop_server_role(SqlServerParser.Drop_server_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_server_role}.
	 * @param ctx the parse tree
	 */
	void exitDrop_server_role(SqlServerParser.Drop_server_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_service}.
	 * @param ctx the parse tree
	 */
	void enterDrop_service(SqlServerParser.Drop_serviceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_service}.
	 * @param ctx the parse tree
	 */
	void exitDrop_service(SqlServerParser.Drop_serviceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_signature}.
	 * @param ctx the parse tree
	 */
	void enterDrop_signature(SqlServerParser.Drop_signatureContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_signature}.
	 * @param ctx the parse tree
	 */
	void exitDrop_signature(SqlServerParser.Drop_signatureContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_statistics_name_azure_dw_and_pdw}.
	 * @param ctx the parse tree
	 */
	void enterDrop_statistics_name_azure_dw_and_pdw(SqlServerParser.Drop_statistics_name_azure_dw_and_pdwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_statistics_name_azure_dw_and_pdw}.
	 * @param ctx the parse tree
	 */
	void exitDrop_statistics_name_azure_dw_and_pdw(SqlServerParser.Drop_statistics_name_azure_dw_and_pdwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_symmetric_key}.
	 * @param ctx the parse tree
	 */
	void enterDrop_symmetric_key(SqlServerParser.Drop_symmetric_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_symmetric_key}.
	 * @param ctx the parse tree
	 */
	void exitDrop_symmetric_key(SqlServerParser.Drop_symmetric_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_synonym}.
	 * @param ctx the parse tree
	 */
	void enterDrop_synonym(SqlServerParser.Drop_synonymContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_synonym}.
	 * @param ctx the parse tree
	 */
	void exitDrop_synonym(SqlServerParser.Drop_synonymContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_user}.
	 * @param ctx the parse tree
	 */
	void enterDrop_user(SqlServerParser.Drop_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_user}.
	 * @param ctx the parse tree
	 */
	void exitDrop_user(SqlServerParser.Drop_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_workload_group}.
	 * @param ctx the parse tree
	 */
	void enterDrop_workload_group(SqlServerParser.Drop_workload_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_workload_group}.
	 * @param ctx the parse tree
	 */
	void exitDrop_workload_group(SqlServerParser.Drop_workload_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_xml_schema_collection}.
	 * @param ctx the parse tree
	 */
	void enterDrop_xml_schema_collection(SqlServerParser.Drop_xml_schema_collectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_xml_schema_collection}.
	 * @param ctx the parse tree
	 */
	void exitDrop_xml_schema_collection(SqlServerParser.Drop_xml_schema_collectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#disable_trigger}.
	 * @param ctx the parse tree
	 */
	void enterDisable_trigger(SqlServerParser.Disable_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#disable_trigger}.
	 * @param ctx the parse tree
	 */
	void exitDisable_trigger(SqlServerParser.Disable_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#enable_trigger}.
	 * @param ctx the parse tree
	 */
	void enterEnable_trigger(SqlServerParser.Enable_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#enable_trigger}.
	 * @param ctx the parse tree
	 */
	void exitEnable_trigger(SqlServerParser.Enable_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#lock_table}.
	 * @param ctx the parse tree
	 */
	void enterLock_table(SqlServerParser.Lock_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#lock_table}.
	 * @param ctx the parse tree
	 */
	void exitLock_table(SqlServerParser.Lock_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#truncate_table}.
	 * @param ctx the parse tree
	 */
	void enterTruncate_table(SqlServerParser.Truncate_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#truncate_table}.
	 * @param ctx the parse tree
	 */
	void exitTruncate_table(SqlServerParser.Truncate_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_column_master_key}.
	 * @param ctx the parse tree
	 */
	void enterCreate_column_master_key(SqlServerParser.Create_column_master_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_column_master_key}.
	 * @param ctx the parse tree
	 */
	void exitCreate_column_master_key(SqlServerParser.Create_column_master_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_credential}.
	 * @param ctx the parse tree
	 */
	void enterAlter_credential(SqlServerParser.Alter_credentialContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_credential}.
	 * @param ctx the parse tree
	 */
	void exitAlter_credential(SqlServerParser.Alter_credentialContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_credential}.
	 * @param ctx the parse tree
	 */
	void enterCreate_credential(SqlServerParser.Create_credentialContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_credential}.
	 * @param ctx the parse tree
	 */
	void exitCreate_credential(SqlServerParser.Create_credentialContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_cryptographic_provider}.
	 * @param ctx the parse tree
	 */
	void enterAlter_cryptographic_provider(SqlServerParser.Alter_cryptographic_providerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_cryptographic_provider}.
	 * @param ctx the parse tree
	 */
	void exitAlter_cryptographic_provider(SqlServerParser.Alter_cryptographic_providerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_cryptographic_provider}.
	 * @param ctx the parse tree
	 */
	void enterCreate_cryptographic_provider(SqlServerParser.Create_cryptographic_providerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_cryptographic_provider}.
	 * @param ctx the parse tree
	 */
	void exitCreate_cryptographic_provider(SqlServerParser.Create_cryptographic_providerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_endpoint}.
	 * @param ctx the parse tree
	 */
	void enterCreate_endpoint(SqlServerParser.Create_endpointContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_endpoint}.
	 * @param ctx the parse tree
	 */
	void exitCreate_endpoint(SqlServerParser.Create_endpointContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#endpoint_encryption_alogorithm_clause}.
	 * @param ctx the parse tree
	 */
	void enterEndpoint_encryption_alogorithm_clause(SqlServerParser.Endpoint_encryption_alogorithm_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#endpoint_encryption_alogorithm_clause}.
	 * @param ctx the parse tree
	 */
	void exitEndpoint_encryption_alogorithm_clause(SqlServerParser.Endpoint_encryption_alogorithm_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#endpoint_authentication_clause}.
	 * @param ctx the parse tree
	 */
	void enterEndpoint_authentication_clause(SqlServerParser.Endpoint_authentication_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#endpoint_authentication_clause}.
	 * @param ctx the parse tree
	 */
	void exitEndpoint_authentication_clause(SqlServerParser.Endpoint_authentication_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#endpoint_listener_clause}.
	 * @param ctx the parse tree
	 */
	void enterEndpoint_listener_clause(SqlServerParser.Endpoint_listener_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#endpoint_listener_clause}.
	 * @param ctx the parse tree
	 */
	void exitEndpoint_listener_clause(SqlServerParser.Endpoint_listener_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_event_notification}.
	 * @param ctx the parse tree
	 */
	void enterCreate_event_notification(SqlServerParser.Create_event_notificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_event_notification}.
	 * @param ctx the parse tree
	 */
	void exitCreate_event_notification(SqlServerParser.Create_event_notificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_or_alter_event_session}.
	 * @param ctx the parse tree
	 */
	void enterCreate_or_alter_event_session(SqlServerParser.Create_or_alter_event_sessionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_or_alter_event_session}.
	 * @param ctx the parse tree
	 */
	void exitCreate_or_alter_event_session(SqlServerParser.Create_or_alter_event_sessionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#event_session_predicate_expression}.
	 * @param ctx the parse tree
	 */
	void enterEvent_session_predicate_expression(SqlServerParser.Event_session_predicate_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#event_session_predicate_expression}.
	 * @param ctx the parse tree
	 */
	void exitEvent_session_predicate_expression(SqlServerParser.Event_session_predicate_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#event_session_predicate_factor}.
	 * @param ctx the parse tree
	 */
	void enterEvent_session_predicate_factor(SqlServerParser.Event_session_predicate_factorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#event_session_predicate_factor}.
	 * @param ctx the parse tree
	 */
	void exitEvent_session_predicate_factor(SqlServerParser.Event_session_predicate_factorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#event_session_predicate_leaf}.
	 * @param ctx the parse tree
	 */
	void enterEvent_session_predicate_leaf(SqlServerParser.Event_session_predicate_leafContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#event_session_predicate_leaf}.
	 * @param ctx the parse tree
	 */
	void exitEvent_session_predicate_leaf(SqlServerParser.Event_session_predicate_leafContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_external_data_source}.
	 * @param ctx the parse tree
	 */
	void enterAlter_external_data_source(SqlServerParser.Alter_external_data_sourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_external_data_source}.
	 * @param ctx the parse tree
	 */
	void exitAlter_external_data_source(SqlServerParser.Alter_external_data_sourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_external_library}.
	 * @param ctx the parse tree
	 */
	void enterAlter_external_library(SqlServerParser.Alter_external_libraryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_external_library}.
	 * @param ctx the parse tree
	 */
	void exitAlter_external_library(SqlServerParser.Alter_external_libraryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_external_library}.
	 * @param ctx the parse tree
	 */
	void enterCreate_external_library(SqlServerParser.Create_external_libraryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_external_library}.
	 * @param ctx the parse tree
	 */
	void exitCreate_external_library(SqlServerParser.Create_external_libraryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_external_resource_pool}.
	 * @param ctx the parse tree
	 */
	void enterAlter_external_resource_pool(SqlServerParser.Alter_external_resource_poolContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_external_resource_pool}.
	 * @param ctx the parse tree
	 */
	void exitAlter_external_resource_pool(SqlServerParser.Alter_external_resource_poolContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_external_resource_pool}.
	 * @param ctx the parse tree
	 */
	void enterCreate_external_resource_pool(SqlServerParser.Create_external_resource_poolContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_external_resource_pool}.
	 * @param ctx the parse tree
	 */
	void exitCreate_external_resource_pool(SqlServerParser.Create_external_resource_poolContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_fulltext_catalog}.
	 * @param ctx the parse tree
	 */
	void enterAlter_fulltext_catalog(SqlServerParser.Alter_fulltext_catalogContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_fulltext_catalog}.
	 * @param ctx the parse tree
	 */
	void exitAlter_fulltext_catalog(SqlServerParser.Alter_fulltext_catalogContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_fulltext_catalog}.
	 * @param ctx the parse tree
	 */
	void enterCreate_fulltext_catalog(SqlServerParser.Create_fulltext_catalogContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_fulltext_catalog}.
	 * @param ctx the parse tree
	 */
	void exitCreate_fulltext_catalog(SqlServerParser.Create_fulltext_catalogContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_fulltext_stoplist}.
	 * @param ctx the parse tree
	 */
	void enterAlter_fulltext_stoplist(SqlServerParser.Alter_fulltext_stoplistContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_fulltext_stoplist}.
	 * @param ctx the parse tree
	 */
	void exitAlter_fulltext_stoplist(SqlServerParser.Alter_fulltext_stoplistContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_fulltext_stoplist}.
	 * @param ctx the parse tree
	 */
	void enterCreate_fulltext_stoplist(SqlServerParser.Create_fulltext_stoplistContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_fulltext_stoplist}.
	 * @param ctx the parse tree
	 */
	void exitCreate_fulltext_stoplist(SqlServerParser.Create_fulltext_stoplistContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_login_sql_server}.
	 * @param ctx the parse tree
	 */
	void enterAlter_login_sql_server(SqlServerParser.Alter_login_sql_serverContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_login_sql_server}.
	 * @param ctx the parse tree
	 */
	void exitAlter_login_sql_server(SqlServerParser.Alter_login_sql_serverContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_login_sql_server}.
	 * @param ctx the parse tree
	 */
	void enterCreate_login_sql_server(SqlServerParser.Create_login_sql_serverContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_login_sql_server}.
	 * @param ctx the parse tree
	 */
	void exitCreate_login_sql_server(SqlServerParser.Create_login_sql_serverContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_login_azure_sql}.
	 * @param ctx the parse tree
	 */
	void enterAlter_login_azure_sql(SqlServerParser.Alter_login_azure_sqlContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_login_azure_sql}.
	 * @param ctx the parse tree
	 */
	void exitAlter_login_azure_sql(SqlServerParser.Alter_login_azure_sqlContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_login_azure_sql}.
	 * @param ctx the parse tree
	 */
	void enterCreate_login_azure_sql(SqlServerParser.Create_login_azure_sqlContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_login_azure_sql}.
	 * @param ctx the parse tree
	 */
	void exitCreate_login_azure_sql(SqlServerParser.Create_login_azure_sqlContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_login_azure_sql_dw_and_pdw}.
	 * @param ctx the parse tree
	 */
	void enterAlter_login_azure_sql_dw_and_pdw(SqlServerParser.Alter_login_azure_sql_dw_and_pdwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_login_azure_sql_dw_and_pdw}.
	 * @param ctx the parse tree
	 */
	void exitAlter_login_azure_sql_dw_and_pdw(SqlServerParser.Alter_login_azure_sql_dw_and_pdwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_login_pdw}.
	 * @param ctx the parse tree
	 */
	void enterCreate_login_pdw(SqlServerParser.Create_login_pdwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_login_pdw}.
	 * @param ctx the parse tree
	 */
	void exitCreate_login_pdw(SqlServerParser.Create_login_pdwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_master_key_sql_server}.
	 * @param ctx the parse tree
	 */
	void enterAlter_master_key_sql_server(SqlServerParser.Alter_master_key_sql_serverContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_master_key_sql_server}.
	 * @param ctx the parse tree
	 */
	void exitAlter_master_key_sql_server(SqlServerParser.Alter_master_key_sql_serverContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_master_key_sql_server}.
	 * @param ctx the parse tree
	 */
	void enterCreate_master_key_sql_server(SqlServerParser.Create_master_key_sql_serverContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_master_key_sql_server}.
	 * @param ctx the parse tree
	 */
	void exitCreate_master_key_sql_server(SqlServerParser.Create_master_key_sql_serverContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_master_key_azure_sql}.
	 * @param ctx the parse tree
	 */
	void enterAlter_master_key_azure_sql(SqlServerParser.Alter_master_key_azure_sqlContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_master_key_azure_sql}.
	 * @param ctx the parse tree
	 */
	void exitAlter_master_key_azure_sql(SqlServerParser.Alter_master_key_azure_sqlContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_master_key_azure_sql}.
	 * @param ctx the parse tree
	 */
	void enterCreate_master_key_azure_sql(SqlServerParser.Create_master_key_azure_sqlContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_master_key_azure_sql}.
	 * @param ctx the parse tree
	 */
	void exitCreate_master_key_azure_sql(SqlServerParser.Create_master_key_azure_sqlContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_message_type}.
	 * @param ctx the parse tree
	 */
	void enterAlter_message_type(SqlServerParser.Alter_message_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_message_type}.
	 * @param ctx the parse tree
	 */
	void exitAlter_message_type(SqlServerParser.Alter_message_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_partition_function}.
	 * @param ctx the parse tree
	 */
	void enterAlter_partition_function(SqlServerParser.Alter_partition_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_partition_function}.
	 * @param ctx the parse tree
	 */
	void exitAlter_partition_function(SqlServerParser.Alter_partition_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_partition_scheme}.
	 * @param ctx the parse tree
	 */
	void enterAlter_partition_scheme(SqlServerParser.Alter_partition_schemeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_partition_scheme}.
	 * @param ctx the parse tree
	 */
	void exitAlter_partition_scheme(SqlServerParser.Alter_partition_schemeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_remote_service_binding}.
	 * @param ctx the parse tree
	 */
	void enterAlter_remote_service_binding(SqlServerParser.Alter_remote_service_bindingContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_remote_service_binding}.
	 * @param ctx the parse tree
	 */
	void exitAlter_remote_service_binding(SqlServerParser.Alter_remote_service_bindingContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_remote_service_binding}.
	 * @param ctx the parse tree
	 */
	void enterCreate_remote_service_binding(SqlServerParser.Create_remote_service_bindingContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_remote_service_binding}.
	 * @param ctx the parse tree
	 */
	void exitCreate_remote_service_binding(SqlServerParser.Create_remote_service_bindingContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_resource_pool}.
	 * @param ctx the parse tree
	 */
	void enterCreate_resource_pool(SqlServerParser.Create_resource_poolContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_resource_pool}.
	 * @param ctx the parse tree
	 */
	void exitCreate_resource_pool(SqlServerParser.Create_resource_poolContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_resource_governor}.
	 * @param ctx the parse tree
	 */
	void enterAlter_resource_governor(SqlServerParser.Alter_resource_governorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_resource_governor}.
	 * @param ctx the parse tree
	 */
	void exitAlter_resource_governor(SqlServerParser.Alter_resource_governorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_database_audit_specification}.
	 * @param ctx the parse tree
	 */
	void enterAlter_database_audit_specification(SqlServerParser.Alter_database_audit_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_database_audit_specification}.
	 * @param ctx the parse tree
	 */
	void exitAlter_database_audit_specification(SqlServerParser.Alter_database_audit_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#audit_action_spec_group}.
	 * @param ctx the parse tree
	 */
	void enterAudit_action_spec_group(SqlServerParser.Audit_action_spec_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#audit_action_spec_group}.
	 * @param ctx the parse tree
	 */
	void exitAudit_action_spec_group(SqlServerParser.Audit_action_spec_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#audit_action_specification}.
	 * @param ctx the parse tree
	 */
	void enterAudit_action_specification(SqlServerParser.Audit_action_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#audit_action_specification}.
	 * @param ctx the parse tree
	 */
	void exitAudit_action_specification(SqlServerParser.Audit_action_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#action_specification}.
	 * @param ctx the parse tree
	 */
	void enterAction_specification(SqlServerParser.Action_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#action_specification}.
	 * @param ctx the parse tree
	 */
	void exitAction_specification(SqlServerParser.Action_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#audit_class_name}.
	 * @param ctx the parse tree
	 */
	void enterAudit_class_name(SqlServerParser.Audit_class_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#audit_class_name}.
	 * @param ctx the parse tree
	 */
	void exitAudit_class_name(SqlServerParser.Audit_class_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#audit_securable}.
	 * @param ctx the parse tree
	 */
	void enterAudit_securable(SqlServerParser.Audit_securableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#audit_securable}.
	 * @param ctx the parse tree
	 */
	void exitAudit_securable(SqlServerParser.Audit_securableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_db_role}.
	 * @param ctx the parse tree
	 */
	void enterAlter_db_role(SqlServerParser.Alter_db_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_db_role}.
	 * @param ctx the parse tree
	 */
	void exitAlter_db_role(SqlServerParser.Alter_db_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_database_audit_specification}.
	 * @param ctx the parse tree
	 */
	void enterCreate_database_audit_specification(SqlServerParser.Create_database_audit_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_database_audit_specification}.
	 * @param ctx the parse tree
	 */
	void exitCreate_database_audit_specification(SqlServerParser.Create_database_audit_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_db_role}.
	 * @param ctx the parse tree
	 */
	void enterCreate_db_role(SqlServerParser.Create_db_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_db_role}.
	 * @param ctx the parse tree
	 */
	void exitCreate_db_role(SqlServerParser.Create_db_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_route}.
	 * @param ctx the parse tree
	 */
	void enterCreate_route(SqlServerParser.Create_routeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_route}.
	 * @param ctx the parse tree
	 */
	void exitCreate_route(SqlServerParser.Create_routeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_rule}.
	 * @param ctx the parse tree
	 */
	void enterCreate_rule(SqlServerParser.Create_ruleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_rule}.
	 * @param ctx the parse tree
	 */
	void exitCreate_rule(SqlServerParser.Create_ruleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_schema_sql}.
	 * @param ctx the parse tree
	 */
	void enterAlter_schema_sql(SqlServerParser.Alter_schema_sqlContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_schema_sql}.
	 * @param ctx the parse tree
	 */
	void exitAlter_schema_sql(SqlServerParser.Alter_schema_sqlContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_schema}.
	 * @param ctx the parse tree
	 */
	void enterCreate_schema(SqlServerParser.Create_schemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_schema}.
	 * @param ctx the parse tree
	 */
	void exitCreate_schema(SqlServerParser.Create_schemaContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_schema_azure_sql_dw_and_pdw}.
	 * @param ctx the parse tree
	 */
	void enterCreate_schema_azure_sql_dw_and_pdw(SqlServerParser.Create_schema_azure_sql_dw_and_pdwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_schema_azure_sql_dw_and_pdw}.
	 * @param ctx the parse tree
	 */
	void exitCreate_schema_azure_sql_dw_and_pdw(SqlServerParser.Create_schema_azure_sql_dw_and_pdwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_schema_azure_sql_dw_and_pdw}.
	 * @param ctx the parse tree
	 */
	void enterAlter_schema_azure_sql_dw_and_pdw(SqlServerParser.Alter_schema_azure_sql_dw_and_pdwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_schema_azure_sql_dw_and_pdw}.
	 * @param ctx the parse tree
	 */
	void exitAlter_schema_azure_sql_dw_and_pdw(SqlServerParser.Alter_schema_azure_sql_dw_and_pdwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_search_property_list}.
	 * @param ctx the parse tree
	 */
	void enterCreate_search_property_list(SqlServerParser.Create_search_property_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_search_property_list}.
	 * @param ctx the parse tree
	 */
	void exitCreate_search_property_list(SqlServerParser.Create_search_property_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_security_policy}.
	 * @param ctx the parse tree
	 */
	void enterCreate_security_policy(SqlServerParser.Create_security_policyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_security_policy}.
	 * @param ctx the parse tree
	 */
	void exitCreate_security_policy(SqlServerParser.Create_security_policyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_sequence}.
	 * @param ctx the parse tree
	 */
	void enterAlter_sequence(SqlServerParser.Alter_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_sequence}.
	 * @param ctx the parse tree
	 */
	void exitAlter_sequence(SqlServerParser.Alter_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_sequence}.
	 * @param ctx the parse tree
	 */
	void enterCreate_sequence(SqlServerParser.Create_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_sequence}.
	 * @param ctx the parse tree
	 */
	void exitCreate_sequence(SqlServerParser.Create_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_server_audit}.
	 * @param ctx the parse tree
	 */
	void enterAlter_server_audit(SqlServerParser.Alter_server_auditContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_server_audit}.
	 * @param ctx the parse tree
	 */
	void exitAlter_server_audit(SqlServerParser.Alter_server_auditContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_server_audit}.
	 * @param ctx the parse tree
	 */
	void enterCreate_server_audit(SqlServerParser.Create_server_auditContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_server_audit}.
	 * @param ctx the parse tree
	 */
	void exitCreate_server_audit(SqlServerParser.Create_server_auditContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_server_audit_specification}.
	 * @param ctx the parse tree
	 */
	void enterAlter_server_audit_specification(SqlServerParser.Alter_server_audit_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_server_audit_specification}.
	 * @param ctx the parse tree
	 */
	void exitAlter_server_audit_specification(SqlServerParser.Alter_server_audit_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_server_audit_specification}.
	 * @param ctx the parse tree
	 */
	void enterCreate_server_audit_specification(SqlServerParser.Create_server_audit_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_server_audit_specification}.
	 * @param ctx the parse tree
	 */
	void exitCreate_server_audit_specification(SqlServerParser.Create_server_audit_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_server_configuration}.
	 * @param ctx the parse tree
	 */
	void enterAlter_server_configuration(SqlServerParser.Alter_server_configurationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_server_configuration}.
	 * @param ctx the parse tree
	 */
	void exitAlter_server_configuration(SqlServerParser.Alter_server_configurationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_server_role}.
	 * @param ctx the parse tree
	 */
	void enterAlter_server_role(SqlServerParser.Alter_server_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_server_role}.
	 * @param ctx the parse tree
	 */
	void exitAlter_server_role(SqlServerParser.Alter_server_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_server_role}.
	 * @param ctx the parse tree
	 */
	void enterCreate_server_role(SqlServerParser.Create_server_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_server_role}.
	 * @param ctx the parse tree
	 */
	void exitCreate_server_role(SqlServerParser.Create_server_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_server_role_pdw}.
	 * @param ctx the parse tree
	 */
	void enterAlter_server_role_pdw(SqlServerParser.Alter_server_role_pdwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_server_role_pdw}.
	 * @param ctx the parse tree
	 */
	void exitAlter_server_role_pdw(SqlServerParser.Alter_server_role_pdwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_service}.
	 * @param ctx the parse tree
	 */
	void enterAlter_service(SqlServerParser.Alter_serviceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_service}.
	 * @param ctx the parse tree
	 */
	void exitAlter_service(SqlServerParser.Alter_serviceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#opt_arg_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_arg_clause(SqlServerParser.Opt_arg_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#opt_arg_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_arg_clause(SqlServerParser.Opt_arg_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_service}.
	 * @param ctx the parse tree
	 */
	void enterCreate_service(SqlServerParser.Create_serviceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_service}.
	 * @param ctx the parse tree
	 */
	void exitCreate_service(SqlServerParser.Create_serviceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_service_master_key}.
	 * @param ctx the parse tree
	 */
	void enterAlter_service_master_key(SqlServerParser.Alter_service_master_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_service_master_key}.
	 * @param ctx the parse tree
	 */
	void exitAlter_service_master_key(SqlServerParser.Alter_service_master_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_symmetric_key}.
	 * @param ctx the parse tree
	 */
	void enterAlter_symmetric_key(SqlServerParser.Alter_symmetric_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_symmetric_key}.
	 * @param ctx the parse tree
	 */
	void exitAlter_symmetric_key(SqlServerParser.Alter_symmetric_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_synonym}.
	 * @param ctx the parse tree
	 */
	void enterCreate_synonym(SqlServerParser.Create_synonymContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_synonym}.
	 * @param ctx the parse tree
	 */
	void exitCreate_synonym(SqlServerParser.Create_synonymContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_user}.
	 * @param ctx the parse tree
	 */
	void enterAlter_user(SqlServerParser.Alter_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_user}.
	 * @param ctx the parse tree
	 */
	void exitAlter_user(SqlServerParser.Alter_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_user}.
	 * @param ctx the parse tree
	 */
	void enterCreate_user(SqlServerParser.Create_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_user}.
	 * @param ctx the parse tree
	 */
	void exitCreate_user(SqlServerParser.Create_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_user_azure_sql_dw}.
	 * @param ctx the parse tree
	 */
	void enterCreate_user_azure_sql_dw(SqlServerParser.Create_user_azure_sql_dwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_user_azure_sql_dw}.
	 * @param ctx the parse tree
	 */
	void exitCreate_user_azure_sql_dw(SqlServerParser.Create_user_azure_sql_dwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_user_azure_sql}.
	 * @param ctx the parse tree
	 */
	void enterAlter_user_azure_sql(SqlServerParser.Alter_user_azure_sqlContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_user_azure_sql}.
	 * @param ctx the parse tree
	 */
	void exitAlter_user_azure_sql(SqlServerParser.Alter_user_azure_sqlContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_workload_group}.
	 * @param ctx the parse tree
	 */
	void enterAlter_workload_group(SqlServerParser.Alter_workload_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_workload_group}.
	 * @param ctx the parse tree
	 */
	void exitAlter_workload_group(SqlServerParser.Alter_workload_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_workload_group}.
	 * @param ctx the parse tree
	 */
	void enterCreate_workload_group(SqlServerParser.Create_workload_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_workload_group}.
	 * @param ctx the parse tree
	 */
	void exitCreate_workload_group(SqlServerParser.Create_workload_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_xml_schema_collection}.
	 * @param ctx the parse tree
	 */
	void enterCreate_xml_schema_collection(SqlServerParser.Create_xml_schema_collectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_xml_schema_collection}.
	 * @param ctx the parse tree
	 */
	void exitCreate_xml_schema_collection(SqlServerParser.Create_xml_schema_collectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_partition_function}.
	 * @param ctx the parse tree
	 */
	void enterCreate_partition_function(SqlServerParser.Create_partition_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_partition_function}.
	 * @param ctx the parse tree
	 */
	void exitCreate_partition_function(SqlServerParser.Create_partition_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_partition_scheme}.
	 * @param ctx the parse tree
	 */
	void enterCreate_partition_scheme(SqlServerParser.Create_partition_schemeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_partition_scheme}.
	 * @param ctx the parse tree
	 */
	void exitCreate_partition_scheme(SqlServerParser.Create_partition_schemeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_queue}.
	 * @param ctx the parse tree
	 */
	void enterCreate_queue(SqlServerParser.Create_queueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_queue}.
	 * @param ctx the parse tree
	 */
	void exitCreate_queue(SqlServerParser.Create_queueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#queue_settings}.
	 * @param ctx the parse tree
	 */
	void enterQueue_settings(SqlServerParser.Queue_settingsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#queue_settings}.
	 * @param ctx the parse tree
	 */
	void exitQueue_settings(SqlServerParser.Queue_settingsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_queue}.
	 * @param ctx the parse tree
	 */
	void enterAlter_queue(SqlServerParser.Alter_queueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_queue}.
	 * @param ctx the parse tree
	 */
	void exitAlter_queue(SqlServerParser.Alter_queueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#queue_action}.
	 * @param ctx the parse tree
	 */
	void enterQueue_action(SqlServerParser.Queue_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#queue_action}.
	 * @param ctx the parse tree
	 */
	void exitQueue_action(SqlServerParser.Queue_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#queue_rebuild_options}.
	 * @param ctx the parse tree
	 */
	void enterQueue_rebuild_options(SqlServerParser.Queue_rebuild_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#queue_rebuild_options}.
	 * @param ctx the parse tree
	 */
	void exitQueue_rebuild_options(SqlServerParser.Queue_rebuild_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_contract}.
	 * @param ctx the parse tree
	 */
	void enterCreate_contract(SqlServerParser.Create_contractContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_contract}.
	 * @param ctx the parse tree
	 */
	void exitCreate_contract(SqlServerParser.Create_contractContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#conversation_statement}.
	 * @param ctx the parse tree
	 */
	void enterConversation_statement(SqlServerParser.Conversation_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#conversation_statement}.
	 * @param ctx the parse tree
	 */
	void exitConversation_statement(SqlServerParser.Conversation_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#message_statement}.
	 * @param ctx the parse tree
	 */
	void enterMessage_statement(SqlServerParser.Message_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#message_statement}.
	 * @param ctx the parse tree
	 */
	void exitMessage_statement(SqlServerParser.Message_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#merge_statement}.
	 * @param ctx the parse tree
	 */
	void enterMerge_statement(SqlServerParser.Merge_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#merge_statement}.
	 * @param ctx the parse tree
	 */
	void exitMerge_statement(SqlServerParser.Merge_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#when_matches}.
	 * @param ctx the parse tree
	 */
	void enterWhen_matches(SqlServerParser.When_matchesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#when_matches}.
	 * @param ctx the parse tree
	 */
	void exitWhen_matches(SqlServerParser.When_matchesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#merge_matched}.
	 * @param ctx the parse tree
	 */
	void enterMerge_matched(SqlServerParser.Merge_matchedContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#merge_matched}.
	 * @param ctx the parse tree
	 */
	void exitMerge_matched(SqlServerParser.Merge_matchedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#merge_not_matched}.
	 * @param ctx the parse tree
	 */
	void enterMerge_not_matched(SqlServerParser.Merge_not_matchedContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#merge_not_matched}.
	 * @param ctx the parse tree
	 */
	void exitMerge_not_matched(SqlServerParser.Merge_not_matchedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#delete_statement}.
	 * @param ctx the parse tree
	 */
	void enterDelete_statement(SqlServerParser.Delete_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#delete_statement}.
	 * @param ctx the parse tree
	 */
	void exitDelete_statement(SqlServerParser.Delete_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#delete_statement_from}.
	 * @param ctx the parse tree
	 */
	void enterDelete_statement_from(SqlServerParser.Delete_statement_fromContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#delete_statement_from}.
	 * @param ctx the parse tree
	 */
	void exitDelete_statement_from(SqlServerParser.Delete_statement_fromContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#insert_statement}.
	 * @param ctx the parse tree
	 */
	void enterInsert_statement(SqlServerParser.Insert_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#insert_statement}.
	 * @param ctx the parse tree
	 */
	void exitInsert_statement(SqlServerParser.Insert_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#insert_statement_value}.
	 * @param ctx the parse tree
	 */
	void enterInsert_statement_value(SqlServerParser.Insert_statement_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#insert_statement_value}.
	 * @param ctx the parse tree
	 */
	void exitInsert_statement_value(SqlServerParser.Insert_statement_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#receive_statement}.
	 * @param ctx the parse tree
	 */
	void enterReceive_statement(SqlServerParser.Receive_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#receive_statement}.
	 * @param ctx the parse tree
	 */
	void exitReceive_statement(SqlServerParser.Receive_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#select_statement_standalone}.
	 * @param ctx the parse tree
	 */
	void enterSelect_statement_standalone(SqlServerParser.Select_statement_standaloneContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#select_statement_standalone}.
	 * @param ctx the parse tree
	 */
	void exitSelect_statement_standalone(SqlServerParser.Select_statement_standaloneContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#select_statement}.
	 * @param ctx the parse tree
	 */
	void enterSelect_statement(SqlServerParser.Select_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#select_statement}.
	 * @param ctx the parse tree
	 */
	void exitSelect_statement(SqlServerParser.Select_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#time}.
	 * @param ctx the parse tree
	 */
	void enterTime(SqlServerParser.TimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#time}.
	 * @param ctx the parse tree
	 */
	void exitTime(SqlServerParser.TimeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#update_statement}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_statement(SqlServerParser.Update_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#update_statement}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_statement(SqlServerParser.Update_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#output_clause}.
	 * @param ctx the parse tree
	 */
	void enterOutput_clause(SqlServerParser.Output_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#output_clause}.
	 * @param ctx the parse tree
	 */
	void exitOutput_clause(SqlServerParser.Output_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#output_dml_list_elem}.
	 * @param ctx the parse tree
	 */
	void enterOutput_dml_list_elem(SqlServerParser.Output_dml_list_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#output_dml_list_elem}.
	 * @param ctx the parse tree
	 */
	void exitOutput_dml_list_elem(SqlServerParser.Output_dml_list_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_database}.
	 * @param ctx the parse tree
	 */
	void enterCreate_database(SqlServerParser.Create_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_database}.
	 * @param ctx the parse tree
	 */
	void exitCreate_database(SqlServerParser.Create_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_index}.
	 * @param ctx the parse tree
	 */
	void enterCreate_index(SqlServerParser.Create_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_index}.
	 * @param ctx the parse tree
	 */
	void exitCreate_index(SqlServerParser.Create_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_index_options}.
	 * @param ctx the parse tree
	 */
	void enterCreate_index_options(SqlServerParser.Create_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_index_options}.
	 * @param ctx the parse tree
	 */
	void exitCreate_index_options(SqlServerParser.Create_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#relational_index_option}.
	 * @param ctx the parse tree
	 */
	void enterRelational_index_option(SqlServerParser.Relational_index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#relational_index_option}.
	 * @param ctx the parse tree
	 */
	void exitRelational_index_option(SqlServerParser.Relational_index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_index}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index(SqlServerParser.Alter_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_index}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index(SqlServerParser.Alter_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#resumable_index_options}.
	 * @param ctx the parse tree
	 */
	void enterResumable_index_options(SqlServerParser.Resumable_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#resumable_index_options}.
	 * @param ctx the parse tree
	 */
	void exitResumable_index_options(SqlServerParser.Resumable_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#resumable_index_option}.
	 * @param ctx the parse tree
	 */
	void enterResumable_index_option(SqlServerParser.Resumable_index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#resumable_index_option}.
	 * @param ctx the parse tree
	 */
	void exitResumable_index_option(SqlServerParser.Resumable_index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#reorganize_partition}.
	 * @param ctx the parse tree
	 */
	void enterReorganize_partition(SqlServerParser.Reorganize_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#reorganize_partition}.
	 * @param ctx the parse tree
	 */
	void exitReorganize_partition(SqlServerParser.Reorganize_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#reorganize_options}.
	 * @param ctx the parse tree
	 */
	void enterReorganize_options(SqlServerParser.Reorganize_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#reorganize_options}.
	 * @param ctx the parse tree
	 */
	void exitReorganize_options(SqlServerParser.Reorganize_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#reorganize_option}.
	 * @param ctx the parse tree
	 */
	void enterReorganize_option(SqlServerParser.Reorganize_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#reorganize_option}.
	 * @param ctx the parse tree
	 */
	void exitReorganize_option(SqlServerParser.Reorganize_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#set_index_options}.
	 * @param ctx the parse tree
	 */
	void enterSet_index_options(SqlServerParser.Set_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#set_index_options}.
	 * @param ctx the parse tree
	 */
	void exitSet_index_options(SqlServerParser.Set_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#set_index_option}.
	 * @param ctx the parse tree
	 */
	void enterSet_index_option(SqlServerParser.Set_index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#set_index_option}.
	 * @param ctx the parse tree
	 */
	void exitSet_index_option(SqlServerParser.Set_index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#rebuild_partition}.
	 * @param ctx the parse tree
	 */
	void enterRebuild_partition(SqlServerParser.Rebuild_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#rebuild_partition}.
	 * @param ctx the parse tree
	 */
	void exitRebuild_partition(SqlServerParser.Rebuild_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#rebuild_index_options}.
	 * @param ctx the parse tree
	 */
	void enterRebuild_index_options(SqlServerParser.Rebuild_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#rebuild_index_options}.
	 * @param ctx the parse tree
	 */
	void exitRebuild_index_options(SqlServerParser.Rebuild_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#rebuild_index_option}.
	 * @param ctx the parse tree
	 */
	void enterRebuild_index_option(SqlServerParser.Rebuild_index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#rebuild_index_option}.
	 * @param ctx the parse tree
	 */
	void exitRebuild_index_option(SqlServerParser.Rebuild_index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#single_partition_rebuild_index_options}.
	 * @param ctx the parse tree
	 */
	void enterSingle_partition_rebuild_index_options(SqlServerParser.Single_partition_rebuild_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#single_partition_rebuild_index_options}.
	 * @param ctx the parse tree
	 */
	void exitSingle_partition_rebuild_index_options(SqlServerParser.Single_partition_rebuild_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#single_partition_rebuild_index_option}.
	 * @param ctx the parse tree
	 */
	void enterSingle_partition_rebuild_index_option(SqlServerParser.Single_partition_rebuild_index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#single_partition_rebuild_index_option}.
	 * @param ctx the parse tree
	 */
	void exitSingle_partition_rebuild_index_option(SqlServerParser.Single_partition_rebuild_index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#on_partitions}.
	 * @param ctx the parse tree
	 */
	void enterOn_partitions(SqlServerParser.On_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#on_partitions}.
	 * @param ctx the parse tree
	 */
	void exitOn_partitions(SqlServerParser.On_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_columnstore_index}.
	 * @param ctx the parse tree
	 */
	void enterCreate_columnstore_index(SqlServerParser.Create_columnstore_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_columnstore_index}.
	 * @param ctx the parse tree
	 */
	void exitCreate_columnstore_index(SqlServerParser.Create_columnstore_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_columnstore_index_options}.
	 * @param ctx the parse tree
	 */
	void enterCreate_columnstore_index_options(SqlServerParser.Create_columnstore_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_columnstore_index_options}.
	 * @param ctx the parse tree
	 */
	void exitCreate_columnstore_index_options(SqlServerParser.Create_columnstore_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#columnstore_index_option}.
	 * @param ctx the parse tree
	 */
	void enterColumnstore_index_option(SqlServerParser.Columnstore_index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#columnstore_index_option}.
	 * @param ctx the parse tree
	 */
	void exitColumnstore_index_option(SqlServerParser.Columnstore_index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_nonclustered_columnstore_index}.
	 * @param ctx the parse tree
	 */
	void enterCreate_nonclustered_columnstore_index(SqlServerParser.Create_nonclustered_columnstore_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_nonclustered_columnstore_index}.
	 * @param ctx the parse tree
	 */
	void exitCreate_nonclustered_columnstore_index(SqlServerParser.Create_nonclustered_columnstore_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_xml_index}.
	 * @param ctx the parse tree
	 */
	void enterCreate_xml_index(SqlServerParser.Create_xml_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_xml_index}.
	 * @param ctx the parse tree
	 */
	void exitCreate_xml_index(SqlServerParser.Create_xml_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#xml_index_options}.
	 * @param ctx the parse tree
	 */
	void enterXml_index_options(SqlServerParser.Xml_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#xml_index_options}.
	 * @param ctx the parse tree
	 */
	void exitXml_index_options(SqlServerParser.Xml_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#xml_index_option}.
	 * @param ctx the parse tree
	 */
	void enterXml_index_option(SqlServerParser.Xml_index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#xml_index_option}.
	 * @param ctx the parse tree
	 */
	void exitXml_index_option(SqlServerParser.Xml_index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_or_alter_procedure}.
	 * @param ctx the parse tree
	 */
	void enterCreate_or_alter_procedure(SqlServerParser.Create_or_alter_procedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_or_alter_procedure}.
	 * @param ctx the parse tree
	 */
	void exitCreate_or_alter_procedure(SqlServerParser.Create_or_alter_procedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#as_external_name}.
	 * @param ctx the parse tree
	 */
	void enterAs_external_name(SqlServerParser.As_external_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#as_external_name}.
	 * @param ctx the parse tree
	 */
	void exitAs_external_name(SqlServerParser.As_external_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_or_alter_trigger}.
	 * @param ctx the parse tree
	 */
	void enterCreate_or_alter_trigger(SqlServerParser.Create_or_alter_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_or_alter_trigger}.
	 * @param ctx the parse tree
	 */
	void exitCreate_or_alter_trigger(SqlServerParser.Create_or_alter_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_or_alter_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterCreate_or_alter_dml_trigger(SqlServerParser.Create_or_alter_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_or_alter_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitCreate_or_alter_dml_trigger(SqlServerParser.Create_or_alter_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dml_trigger_option}.
	 * @param ctx the parse tree
	 */
	void enterDml_trigger_option(SqlServerParser.Dml_trigger_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dml_trigger_option}.
	 * @param ctx the parse tree
	 */
	void exitDml_trigger_option(SqlServerParser.Dml_trigger_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dml_trigger_operation}.
	 * @param ctx the parse tree
	 */
	void enterDml_trigger_operation(SqlServerParser.Dml_trigger_operationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dml_trigger_operation}.
	 * @param ctx the parse tree
	 */
	void exitDml_trigger_operation(SqlServerParser.Dml_trigger_operationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_or_alter_ddl_trigger}.
	 * @param ctx the parse tree
	 */
	void enterCreate_or_alter_ddl_trigger(SqlServerParser.Create_or_alter_ddl_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_or_alter_ddl_trigger}.
	 * @param ctx the parse tree
	 */
	void exitCreate_or_alter_ddl_trigger(SqlServerParser.Create_or_alter_ddl_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#ddl_trigger_operation}.
	 * @param ctx the parse tree
	 */
	void enterDdl_trigger_operation(SqlServerParser.Ddl_trigger_operationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#ddl_trigger_operation}.
	 * @param ctx the parse tree
	 */
	void exitDdl_trigger_operation(SqlServerParser.Ddl_trigger_operationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_or_alter_function}.
	 * @param ctx the parse tree
	 */
	void enterCreate_or_alter_function(SqlServerParser.Create_or_alter_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_or_alter_function}.
	 * @param ctx the parse tree
	 */
	void exitCreate_or_alter_function(SqlServerParser.Create_or_alter_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#func_body_returns_select}.
	 * @param ctx the parse tree
	 */
	void enterFunc_body_returns_select(SqlServerParser.Func_body_returns_selectContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#func_body_returns_select}.
	 * @param ctx the parse tree
	 */
	void exitFunc_body_returns_select(SqlServerParser.Func_body_returns_selectContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#func_body_returns_table}.
	 * @param ctx the parse tree
	 */
	void enterFunc_body_returns_table(SqlServerParser.Func_body_returns_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#func_body_returns_table}.
	 * @param ctx the parse tree
	 */
	void exitFunc_body_returns_table(SqlServerParser.Func_body_returns_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#func_body_returns_scalar}.
	 * @param ctx the parse tree
	 */
	void enterFunc_body_returns_scalar(SqlServerParser.Func_body_returns_scalarContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#func_body_returns_scalar}.
	 * @param ctx the parse tree
	 */
	void exitFunc_body_returns_scalar(SqlServerParser.Func_body_returns_scalarContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#procedure_param_default_value}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_param_default_value(SqlServerParser.Procedure_param_default_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#procedure_param_default_value}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_param_default_value(SqlServerParser.Procedure_param_default_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#procedure_param}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_param(SqlServerParser.Procedure_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#procedure_param}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_param(SqlServerParser.Procedure_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#procedure_option}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_option(SqlServerParser.Procedure_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#procedure_option}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_option(SqlServerParser.Procedure_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#function_option}.
	 * @param ctx the parse tree
	 */
	void enterFunction_option(SqlServerParser.Function_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#function_option}.
	 * @param ctx the parse tree
	 */
	void exitFunction_option(SqlServerParser.Function_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_statistics}.
	 * @param ctx the parse tree
	 */
	void enterCreate_statistics(SqlServerParser.Create_statisticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_statistics}.
	 * @param ctx the parse tree
	 */
	void exitCreate_statistics(SqlServerParser.Create_statisticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#update_statistics}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_statistics(SqlServerParser.Update_statisticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#update_statistics}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_statistics(SqlServerParser.Update_statisticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#update_statistics_options}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_statistics_options(SqlServerParser.Update_statistics_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#update_statistics_options}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_statistics_options(SqlServerParser.Update_statistics_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#update_statistics_option}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_statistics_option(SqlServerParser.Update_statistics_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#update_statistics_option}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_statistics_option(SqlServerParser.Update_statistics_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_table}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table(SqlServerParser.Create_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_table}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table(SqlServerParser.Create_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_indices}.
	 * @param ctx the parse tree
	 */
	void enterTable_indices(SqlServerParser.Table_indicesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_indices}.
	 * @param ctx the parse tree
	 */
	void exitTable_indices(SqlServerParser.Table_indicesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_options}.
	 * @param ctx the parse tree
	 */
	void enterTable_options(SqlServerParser.Table_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_options}.
	 * @param ctx the parse tree
	 */
	void exitTable_options(SqlServerParser.Table_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_option}.
	 * @param ctx the parse tree
	 */
	void enterTable_option(SqlServerParser.Table_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_option}.
	 * @param ctx the parse tree
	 */
	void exitTable_option(SqlServerParser.Table_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_table_index_options}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_index_options(SqlServerParser.Create_table_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_table_index_options}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_index_options(SqlServerParser.Create_table_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_table_index_option}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table_index_option(SqlServerParser.Create_table_index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_table_index_option}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table_index_option(SqlServerParser.Create_table_index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_view}.
	 * @param ctx the parse tree
	 */
	void enterCreate_view(SqlServerParser.Create_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_view}.
	 * @param ctx the parse tree
	 */
	void exitCreate_view(SqlServerParser.Create_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#view_attribute}.
	 * @param ctx the parse tree
	 */
	void enterView_attribute(SqlServerParser.View_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#view_attribute}.
	 * @param ctx the parse tree
	 */
	void exitView_attribute(SqlServerParser.View_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_table}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table(SqlServerParser.Alter_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_table}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table(SqlServerParser.Alter_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#switch_partition}.
	 * @param ctx the parse tree
	 */
	void enterSwitch_partition(SqlServerParser.Switch_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#switch_partition}.
	 * @param ctx the parse tree
	 */
	void exitSwitch_partition(SqlServerParser.Switch_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#low_priority_lock_wait}.
	 * @param ctx the parse tree
	 */
	void enterLow_priority_lock_wait(SqlServerParser.Low_priority_lock_waitContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#low_priority_lock_wait}.
	 * @param ctx the parse tree
	 */
	void exitLow_priority_lock_wait(SqlServerParser.Low_priority_lock_waitContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_database}.
	 * @param ctx the parse tree
	 */
	void enterAlter_database(SqlServerParser.Alter_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_database}.
	 * @param ctx the parse tree
	 */
	void exitAlter_database(SqlServerParser.Alter_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#add_or_modify_files}.
	 * @param ctx the parse tree
	 */
	void enterAdd_or_modify_files(SqlServerParser.Add_or_modify_filesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#add_or_modify_files}.
	 * @param ctx the parse tree
	 */
	void exitAdd_or_modify_files(SqlServerParser.Add_or_modify_filesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#filespec}.
	 * @param ctx the parse tree
	 */
	void enterFilespec(SqlServerParser.FilespecContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#filespec}.
	 * @param ctx the parse tree
	 */
	void exitFilespec(SqlServerParser.FilespecContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#add_or_modify_filegroups}.
	 * @param ctx the parse tree
	 */
	void enterAdd_or_modify_filegroups(SqlServerParser.Add_or_modify_filegroupsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#add_or_modify_filegroups}.
	 * @param ctx the parse tree
	 */
	void exitAdd_or_modify_filegroups(SqlServerParser.Add_or_modify_filegroupsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#filegroup_updatability_option}.
	 * @param ctx the parse tree
	 */
	void enterFilegroup_updatability_option(SqlServerParser.Filegroup_updatability_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#filegroup_updatability_option}.
	 * @param ctx the parse tree
	 */
	void exitFilegroup_updatability_option(SqlServerParser.Filegroup_updatability_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#database_optionspec}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_optionspec(SqlServerParser.Database_optionspecContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#database_optionspec}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_optionspec(SqlServerParser.Database_optionspecContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#auto_option}.
	 * @param ctx the parse tree
	 */
	void enterAuto_option(SqlServerParser.Auto_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#auto_option}.
	 * @param ctx the parse tree
	 */
	void exitAuto_option(SqlServerParser.Auto_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#change_tracking_option}.
	 * @param ctx the parse tree
	 */
	void enterChange_tracking_option(SqlServerParser.Change_tracking_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#change_tracking_option}.
	 * @param ctx the parse tree
	 */
	void exitChange_tracking_option(SqlServerParser.Change_tracking_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#change_tracking_option_list}.
	 * @param ctx the parse tree
	 */
	void enterChange_tracking_option_list(SqlServerParser.Change_tracking_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#change_tracking_option_list}.
	 * @param ctx the parse tree
	 */
	void exitChange_tracking_option_list(SqlServerParser.Change_tracking_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#containment_option}.
	 * @param ctx the parse tree
	 */
	void enterContainment_option(SqlServerParser.Containment_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#containment_option}.
	 * @param ctx the parse tree
	 */
	void exitContainment_option(SqlServerParser.Containment_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#cursor_option}.
	 * @param ctx the parse tree
	 */
	void enterCursor_option(SqlServerParser.Cursor_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#cursor_option}.
	 * @param ctx the parse tree
	 */
	void exitCursor_option(SqlServerParser.Cursor_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_endpoint}.
	 * @param ctx the parse tree
	 */
	void enterAlter_endpoint(SqlServerParser.Alter_endpointContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_endpoint}.
	 * @param ctx the parse tree
	 */
	void exitAlter_endpoint(SqlServerParser.Alter_endpointContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#database_mirroring_option}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_mirroring_option(SqlServerParser.Database_mirroring_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#database_mirroring_option}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_mirroring_option(SqlServerParser.Database_mirroring_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#mirroring_set_option}.
	 * @param ctx the parse tree
	 */
	void enterMirroring_set_option(SqlServerParser.Mirroring_set_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#mirroring_set_option}.
	 * @param ctx the parse tree
	 */
	void exitMirroring_set_option(SqlServerParser.Mirroring_set_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#mirroring_partner}.
	 * @param ctx the parse tree
	 */
	void enterMirroring_partner(SqlServerParser.Mirroring_partnerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#mirroring_partner}.
	 * @param ctx the parse tree
	 */
	void exitMirroring_partner(SqlServerParser.Mirroring_partnerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#mirroring_witness}.
	 * @param ctx the parse tree
	 */
	void enterMirroring_witness(SqlServerParser.Mirroring_witnessContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#mirroring_witness}.
	 * @param ctx the parse tree
	 */
	void exitMirroring_witness(SqlServerParser.Mirroring_witnessContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#witness_partner_equal}.
	 * @param ctx the parse tree
	 */
	void enterWitness_partner_equal(SqlServerParser.Witness_partner_equalContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#witness_partner_equal}.
	 * @param ctx the parse tree
	 */
	void exitWitness_partner_equal(SqlServerParser.Witness_partner_equalContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#partner_option}.
	 * @param ctx the parse tree
	 */
	void enterPartner_option(SqlServerParser.Partner_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#partner_option}.
	 * @param ctx the parse tree
	 */
	void exitPartner_option(SqlServerParser.Partner_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#witness_option}.
	 * @param ctx the parse tree
	 */
	void enterWitness_option(SqlServerParser.Witness_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#witness_option}.
	 * @param ctx the parse tree
	 */
	void exitWitness_option(SqlServerParser.Witness_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#witness_server}.
	 * @param ctx the parse tree
	 */
	void enterWitness_server(SqlServerParser.Witness_serverContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#witness_server}.
	 * @param ctx the parse tree
	 */
	void exitWitness_server(SqlServerParser.Witness_serverContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#partner_server}.
	 * @param ctx the parse tree
	 */
	void enterPartner_server(SqlServerParser.Partner_serverContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#partner_server}.
	 * @param ctx the parse tree
	 */
	void exitPartner_server(SqlServerParser.Partner_serverContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#mirroring_host_port_seperator}.
	 * @param ctx the parse tree
	 */
	void enterMirroring_host_port_seperator(SqlServerParser.Mirroring_host_port_seperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#mirroring_host_port_seperator}.
	 * @param ctx the parse tree
	 */
	void exitMirroring_host_port_seperator(SqlServerParser.Mirroring_host_port_seperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#partner_server_tcp_prefix}.
	 * @param ctx the parse tree
	 */
	void enterPartner_server_tcp_prefix(SqlServerParser.Partner_server_tcp_prefixContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#partner_server_tcp_prefix}.
	 * @param ctx the parse tree
	 */
	void exitPartner_server_tcp_prefix(SqlServerParser.Partner_server_tcp_prefixContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#port_number}.
	 * @param ctx the parse tree
	 */
	void enterPort_number(SqlServerParser.Port_numberContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#port_number}.
	 * @param ctx the parse tree
	 */
	void exitPort_number(SqlServerParser.Port_numberContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#host}.
	 * @param ctx the parse tree
	 */
	void enterHost(SqlServerParser.HostContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#host}.
	 * @param ctx the parse tree
	 */
	void exitHost(SqlServerParser.HostContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#date_correlation_optimization_option}.
	 * @param ctx the parse tree
	 */
	void enterDate_correlation_optimization_option(SqlServerParser.Date_correlation_optimization_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#date_correlation_optimization_option}.
	 * @param ctx the parse tree
	 */
	void exitDate_correlation_optimization_option(SqlServerParser.Date_correlation_optimization_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#db_encryption_option}.
	 * @param ctx the parse tree
	 */
	void enterDb_encryption_option(SqlServerParser.Db_encryption_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#db_encryption_option}.
	 * @param ctx the parse tree
	 */
	void exitDb_encryption_option(SqlServerParser.Db_encryption_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#db_state_option}.
	 * @param ctx the parse tree
	 */
	void enterDb_state_option(SqlServerParser.Db_state_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#db_state_option}.
	 * @param ctx the parse tree
	 */
	void exitDb_state_option(SqlServerParser.Db_state_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#db_update_option}.
	 * @param ctx the parse tree
	 */
	void enterDb_update_option(SqlServerParser.Db_update_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#db_update_option}.
	 * @param ctx the parse tree
	 */
	void exitDb_update_option(SqlServerParser.Db_update_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#db_user_access_option}.
	 * @param ctx the parse tree
	 */
	void enterDb_user_access_option(SqlServerParser.Db_user_access_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#db_user_access_option}.
	 * @param ctx the parse tree
	 */
	void exitDb_user_access_option(SqlServerParser.Db_user_access_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#delayed_durability_option}.
	 * @param ctx the parse tree
	 */
	void enterDelayed_durability_option(SqlServerParser.Delayed_durability_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#delayed_durability_option}.
	 * @param ctx the parse tree
	 */
	void exitDelayed_durability_option(SqlServerParser.Delayed_durability_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#external_access_option}.
	 * @param ctx the parse tree
	 */
	void enterExternal_access_option(SqlServerParser.External_access_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#external_access_option}.
	 * @param ctx the parse tree
	 */
	void exitExternal_access_option(SqlServerParser.External_access_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#hadr_options}.
	 * @param ctx the parse tree
	 */
	void enterHadr_options(SqlServerParser.Hadr_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#hadr_options}.
	 * @param ctx the parse tree
	 */
	void exitHadr_options(SqlServerParser.Hadr_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#mixed_page_allocation_option}.
	 * @param ctx the parse tree
	 */
	void enterMixed_page_allocation_option(SqlServerParser.Mixed_page_allocation_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#mixed_page_allocation_option}.
	 * @param ctx the parse tree
	 */
	void exitMixed_page_allocation_option(SqlServerParser.Mixed_page_allocation_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#parameterization_option}.
	 * @param ctx the parse tree
	 */
	void enterParameterization_option(SqlServerParser.Parameterization_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#parameterization_option}.
	 * @param ctx the parse tree
	 */
	void exitParameterization_option(SqlServerParser.Parameterization_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#recovery_option}.
	 * @param ctx the parse tree
	 */
	void enterRecovery_option(SqlServerParser.Recovery_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#recovery_option}.
	 * @param ctx the parse tree
	 */
	void exitRecovery_option(SqlServerParser.Recovery_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#service_broker_option}.
	 * @param ctx the parse tree
	 */
	void enterService_broker_option(SqlServerParser.Service_broker_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#service_broker_option}.
	 * @param ctx the parse tree
	 */
	void exitService_broker_option(SqlServerParser.Service_broker_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#snapshot_option}.
	 * @param ctx the parse tree
	 */
	void enterSnapshot_option(SqlServerParser.Snapshot_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#snapshot_option}.
	 * @param ctx the parse tree
	 */
	void exitSnapshot_option(SqlServerParser.Snapshot_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#sql_option}.
	 * @param ctx the parse tree
	 */
	void enterSql_option(SqlServerParser.Sql_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#sql_option}.
	 * @param ctx the parse tree
	 */
	void exitSql_option(SqlServerParser.Sql_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#target_recovery_time_option}.
	 * @param ctx the parse tree
	 */
	void enterTarget_recovery_time_option(SqlServerParser.Target_recovery_time_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#target_recovery_time_option}.
	 * @param ctx the parse tree
	 */
	void exitTarget_recovery_time_option(SqlServerParser.Target_recovery_time_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#termination}.
	 * @param ctx the parse tree
	 */
	void enterTermination(SqlServerParser.TerminationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#termination}.
	 * @param ctx the parse tree
	 */
	void exitTermination(SqlServerParser.TerminationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_index}.
	 * @param ctx the parse tree
	 */
	void enterDrop_index(SqlServerParser.Drop_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_index}.
	 * @param ctx the parse tree
	 */
	void exitDrop_index(SqlServerParser.Drop_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_relational_or_xml_or_spatial_index}.
	 * @param ctx the parse tree
	 */
	void enterDrop_relational_or_xml_or_spatial_index(SqlServerParser.Drop_relational_or_xml_or_spatial_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_relational_or_xml_or_spatial_index}.
	 * @param ctx the parse tree
	 */
	void exitDrop_relational_or_xml_or_spatial_index(SqlServerParser.Drop_relational_or_xml_or_spatial_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_backward_compatible_index}.
	 * @param ctx the parse tree
	 */
	void enterDrop_backward_compatible_index(SqlServerParser.Drop_backward_compatible_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_backward_compatible_index}.
	 * @param ctx the parse tree
	 */
	void exitDrop_backward_compatible_index(SqlServerParser.Drop_backward_compatible_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_procedure}.
	 * @param ctx the parse tree
	 */
	void enterDrop_procedure(SqlServerParser.Drop_procedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_procedure}.
	 * @param ctx the parse tree
	 */
	void exitDrop_procedure(SqlServerParser.Drop_procedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_trigger}.
	 * @param ctx the parse tree
	 */
	void enterDrop_trigger(SqlServerParser.Drop_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_trigger}.
	 * @param ctx the parse tree
	 */
	void exitDrop_trigger(SqlServerParser.Drop_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterDrop_dml_trigger(SqlServerParser.Drop_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitDrop_dml_trigger(SqlServerParser.Drop_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_ddl_trigger}.
	 * @param ctx the parse tree
	 */
	void enterDrop_ddl_trigger(SqlServerParser.Drop_ddl_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_ddl_trigger}.
	 * @param ctx the parse tree
	 */
	void exitDrop_ddl_trigger(SqlServerParser.Drop_ddl_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_function}.
	 * @param ctx the parse tree
	 */
	void enterDrop_function(SqlServerParser.Drop_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_function}.
	 * @param ctx the parse tree
	 */
	void exitDrop_function(SqlServerParser.Drop_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_statistics}.
	 * @param ctx the parse tree
	 */
	void enterDrop_statistics(SqlServerParser.Drop_statisticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_statistics}.
	 * @param ctx the parse tree
	 */
	void exitDrop_statistics(SqlServerParser.Drop_statisticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_table}.
	 * @param ctx the parse tree
	 */
	void enterDrop_table(SqlServerParser.Drop_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_table}.
	 * @param ctx the parse tree
	 */
	void exitDrop_table(SqlServerParser.Drop_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_view}.
	 * @param ctx the parse tree
	 */
	void enterDrop_view(SqlServerParser.Drop_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_view}.
	 * @param ctx the parse tree
	 */
	void exitDrop_view(SqlServerParser.Drop_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_type}.
	 * @param ctx the parse tree
	 */
	void enterCreate_type(SqlServerParser.Create_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_type}.
	 * @param ctx the parse tree
	 */
	void exitCreate_type(SqlServerParser.Create_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#drop_type}.
	 * @param ctx the parse tree
	 */
	void enterDrop_type(SqlServerParser.Drop_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#drop_type}.
	 * @param ctx the parse tree
	 */
	void exitDrop_type(SqlServerParser.Drop_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#rowset_function_limited}.
	 * @param ctx the parse tree
	 */
	void enterRowset_function_limited(SqlServerParser.Rowset_function_limitedContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#rowset_function_limited}.
	 * @param ctx the parse tree
	 */
	void exitRowset_function_limited(SqlServerParser.Rowset_function_limitedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#openquery}.
	 * @param ctx the parse tree
	 */
	void enterOpenquery(SqlServerParser.OpenqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#openquery}.
	 * @param ctx the parse tree
	 */
	void exitOpenquery(SqlServerParser.OpenqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#opendatasource}.
	 * @param ctx the parse tree
	 */
	void enterOpendatasource(SqlServerParser.OpendatasourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#opendatasource}.
	 * @param ctx the parse tree
	 */
	void exitOpendatasource(SqlServerParser.OpendatasourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#declare_statement}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_statement(SqlServerParser.Declare_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#declare_statement}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_statement(SqlServerParser.Declare_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#xml_declaration}.
	 * @param ctx the parse tree
	 */
	void enterXml_declaration(SqlServerParser.Xml_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#xml_declaration}.
	 * @param ctx the parse tree
	 */
	void exitXml_declaration(SqlServerParser.Xml_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#cursor_statement}.
	 * @param ctx the parse tree
	 */
	void enterCursor_statement(SqlServerParser.Cursor_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#cursor_statement}.
	 * @param ctx the parse tree
	 */
	void exitCursor_statement(SqlServerParser.Cursor_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#backup_database}.
	 * @param ctx the parse tree
	 */
	void enterBackup_database(SqlServerParser.Backup_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#backup_database}.
	 * @param ctx the parse tree
	 */
	void exitBackup_database(SqlServerParser.Backup_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#backup_log}.
	 * @param ctx the parse tree
	 */
	void enterBackup_log(SqlServerParser.Backup_logContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#backup_log}.
	 * @param ctx the parse tree
	 */
	void exitBackup_log(SqlServerParser.Backup_logContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#backup_certificate}.
	 * @param ctx the parse tree
	 */
	void enterBackup_certificate(SqlServerParser.Backup_certificateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#backup_certificate}.
	 * @param ctx the parse tree
	 */
	void exitBackup_certificate(SqlServerParser.Backup_certificateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#backup_master_key}.
	 * @param ctx the parse tree
	 */
	void enterBackup_master_key(SqlServerParser.Backup_master_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#backup_master_key}.
	 * @param ctx the parse tree
	 */
	void exitBackup_master_key(SqlServerParser.Backup_master_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#backup_service_master_key}.
	 * @param ctx the parse tree
	 */
	void enterBackup_service_master_key(SqlServerParser.Backup_service_master_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#backup_service_master_key}.
	 * @param ctx the parse tree
	 */
	void exitBackup_service_master_key(SqlServerParser.Backup_service_master_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#kill_statement}.
	 * @param ctx the parse tree
	 */
	void enterKill_statement(SqlServerParser.Kill_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#kill_statement}.
	 * @param ctx the parse tree
	 */
	void exitKill_statement(SqlServerParser.Kill_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#kill_process}.
	 * @param ctx the parse tree
	 */
	void enterKill_process(SqlServerParser.Kill_processContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#kill_process}.
	 * @param ctx the parse tree
	 */
	void exitKill_process(SqlServerParser.Kill_processContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#kill_query_notification}.
	 * @param ctx the parse tree
	 */
	void enterKill_query_notification(SqlServerParser.Kill_query_notificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#kill_query_notification}.
	 * @param ctx the parse tree
	 */
	void exitKill_query_notification(SqlServerParser.Kill_query_notificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#kill_stats_job}.
	 * @param ctx the parse tree
	 */
	void enterKill_stats_job(SqlServerParser.Kill_stats_jobContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#kill_stats_job}.
	 * @param ctx the parse tree
	 */
	void exitKill_stats_job(SqlServerParser.Kill_stats_jobContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#execute_statement}.
	 * @param ctx the parse tree
	 */
	void enterExecute_statement(SqlServerParser.Execute_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#execute_statement}.
	 * @param ctx the parse tree
	 */
	void exitExecute_statement(SqlServerParser.Execute_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#execute_body_batch}.
	 * @param ctx the parse tree
	 */
	void enterExecute_body_batch(SqlServerParser.Execute_body_batchContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#execute_body_batch}.
	 * @param ctx the parse tree
	 */
	void exitExecute_body_batch(SqlServerParser.Execute_body_batchContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#execute_body}.
	 * @param ctx the parse tree
	 */
	void enterExecute_body(SqlServerParser.Execute_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#execute_body}.
	 * @param ctx the parse tree
	 */
	void exitExecute_body(SqlServerParser.Execute_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#execute_statement_arg}.
	 * @param ctx the parse tree
	 */
	void enterExecute_statement_arg(SqlServerParser.Execute_statement_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#execute_statement_arg}.
	 * @param ctx the parse tree
	 */
	void exitExecute_statement_arg(SqlServerParser.Execute_statement_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#execute_statement_arg_named}.
	 * @param ctx the parse tree
	 */
	void enterExecute_statement_arg_named(SqlServerParser.Execute_statement_arg_namedContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#execute_statement_arg_named}.
	 * @param ctx the parse tree
	 */
	void exitExecute_statement_arg_named(SqlServerParser.Execute_statement_arg_namedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#execute_statement_arg_unnamed}.
	 * @param ctx the parse tree
	 */
	void enterExecute_statement_arg_unnamed(SqlServerParser.Execute_statement_arg_unnamedContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#execute_statement_arg_unnamed}.
	 * @param ctx the parse tree
	 */
	void exitExecute_statement_arg_unnamed(SqlServerParser.Execute_statement_arg_unnamedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#execute_parameter}.
	 * @param ctx the parse tree
	 */
	void enterExecute_parameter(SqlServerParser.Execute_parameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#execute_parameter}.
	 * @param ctx the parse tree
	 */
	void exitExecute_parameter(SqlServerParser.Execute_parameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#execute_var_string}.
	 * @param ctx the parse tree
	 */
	void enterExecute_var_string(SqlServerParser.Execute_var_stringContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#execute_var_string}.
	 * @param ctx the parse tree
	 */
	void exitExecute_var_string(SqlServerParser.Execute_var_stringContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#security_statement}.
	 * @param ctx the parse tree
	 */
	void enterSecurity_statement(SqlServerParser.Security_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#security_statement}.
	 * @param ctx the parse tree
	 */
	void exitSecurity_statement(SqlServerParser.Security_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#principal_id}.
	 * @param ctx the parse tree
	 */
	void enterPrincipal_id(SqlServerParser.Principal_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#principal_id}.
	 * @param ctx the parse tree
	 */
	void exitPrincipal_id(SqlServerParser.Principal_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_certificate}.
	 * @param ctx the parse tree
	 */
	void enterCreate_certificate(SqlServerParser.Create_certificateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_certificate}.
	 * @param ctx the parse tree
	 */
	void exitCreate_certificate(SqlServerParser.Create_certificateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#existing_keys}.
	 * @param ctx the parse tree
	 */
	void enterExisting_keys(SqlServerParser.Existing_keysContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#existing_keys}.
	 * @param ctx the parse tree
	 */
	void exitExisting_keys(SqlServerParser.Existing_keysContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#private_key_options}.
	 * @param ctx the parse tree
	 */
	void enterPrivate_key_options(SqlServerParser.Private_key_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#private_key_options}.
	 * @param ctx the parse tree
	 */
	void exitPrivate_key_options(SqlServerParser.Private_key_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#generate_new_keys}.
	 * @param ctx the parse tree
	 */
	void enterGenerate_new_keys(SqlServerParser.Generate_new_keysContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#generate_new_keys}.
	 * @param ctx the parse tree
	 */
	void exitGenerate_new_keys(SqlServerParser.Generate_new_keysContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#date_options}.
	 * @param ctx the parse tree
	 */
	void enterDate_options(SqlServerParser.Date_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#date_options}.
	 * @param ctx the parse tree
	 */
	void exitDate_options(SqlServerParser.Date_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#open_key}.
	 * @param ctx the parse tree
	 */
	void enterOpen_key(SqlServerParser.Open_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#open_key}.
	 * @param ctx the parse tree
	 */
	void exitOpen_key(SqlServerParser.Open_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#close_key}.
	 * @param ctx the parse tree
	 */
	void enterClose_key(SqlServerParser.Close_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#close_key}.
	 * @param ctx the parse tree
	 */
	void exitClose_key(SqlServerParser.Close_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_key}.
	 * @param ctx the parse tree
	 */
	void enterCreate_key(SqlServerParser.Create_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_key}.
	 * @param ctx the parse tree
	 */
	void exitCreate_key(SqlServerParser.Create_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#key_options}.
	 * @param ctx the parse tree
	 */
	void enterKey_options(SqlServerParser.Key_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#key_options}.
	 * @param ctx the parse tree
	 */
	void exitKey_options(SqlServerParser.Key_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#algorithm}.
	 * @param ctx the parse tree
	 */
	void enterAlgorithm(SqlServerParser.AlgorithmContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#algorithm}.
	 * @param ctx the parse tree
	 */
	void exitAlgorithm(SqlServerParser.AlgorithmContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#encryption_mechanism}.
	 * @param ctx the parse tree
	 */
	void enterEncryption_mechanism(SqlServerParser.Encryption_mechanismContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#encryption_mechanism}.
	 * @param ctx the parse tree
	 */
	void exitEncryption_mechanism(SqlServerParser.Encryption_mechanismContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#decryption_mechanism}.
	 * @param ctx the parse tree
	 */
	void enterDecryption_mechanism(SqlServerParser.Decryption_mechanismContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#decryption_mechanism}.
	 * @param ctx the parse tree
	 */
	void exitDecryption_mechanism(SqlServerParser.Decryption_mechanismContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#grant_permission}.
	 * @param ctx the parse tree
	 */
	void enterGrant_permission(SqlServerParser.Grant_permissionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#grant_permission}.
	 * @param ctx the parse tree
	 */
	void exitGrant_permission(SqlServerParser.Grant_permissionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#set_statement}.
	 * @param ctx the parse tree
	 */
	void enterSet_statement(SqlServerParser.Set_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#set_statement}.
	 * @param ctx the parse tree
	 */
	void exitSet_statement(SqlServerParser.Set_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#transaction_statement}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_statement(SqlServerParser.Transaction_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#transaction_statement}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_statement(SqlServerParser.Transaction_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#go_statement}.
	 * @param ctx the parse tree
	 */
	void enterGo_statement(SqlServerParser.Go_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#go_statement}.
	 * @param ctx the parse tree
	 */
	void exitGo_statement(SqlServerParser.Go_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#use_statement}.
	 * @param ctx the parse tree
	 */
	void enterUse_statement(SqlServerParser.Use_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#use_statement}.
	 * @param ctx the parse tree
	 */
	void exitUse_statement(SqlServerParser.Use_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#setuser_statement}.
	 * @param ctx the parse tree
	 */
	void enterSetuser_statement(SqlServerParser.Setuser_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#setuser_statement}.
	 * @param ctx the parse tree
	 */
	void exitSetuser_statement(SqlServerParser.Setuser_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#reconfigure_statement}.
	 * @param ctx the parse tree
	 */
	void enterReconfigure_statement(SqlServerParser.Reconfigure_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#reconfigure_statement}.
	 * @param ctx the parse tree
	 */
	void exitReconfigure_statement(SqlServerParser.Reconfigure_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#shutdown_statement}.
	 * @param ctx the parse tree
	 */
	void enterShutdown_statement(SqlServerParser.Shutdown_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#shutdown_statement}.
	 * @param ctx the parse tree
	 */
	void exitShutdown_statement(SqlServerParser.Shutdown_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#checkpoint_statement}.
	 * @param ctx the parse tree
	 */
	void enterCheckpoint_statement(SqlServerParser.Checkpoint_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#checkpoint_statement}.
	 * @param ctx the parse tree
	 */
	void exitCheckpoint_statement(SqlServerParser.Checkpoint_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_checkalloc_option}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_checkalloc_option(SqlServerParser.Dbcc_checkalloc_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_checkalloc_option}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_checkalloc_option(SqlServerParser.Dbcc_checkalloc_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_checkalloc}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_checkalloc(SqlServerParser.Dbcc_checkallocContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_checkalloc}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_checkalloc(SqlServerParser.Dbcc_checkallocContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_checkcatalog}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_checkcatalog(SqlServerParser.Dbcc_checkcatalogContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_checkcatalog}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_checkcatalog(SqlServerParser.Dbcc_checkcatalogContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_checkconstraints_option}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_checkconstraints_option(SqlServerParser.Dbcc_checkconstraints_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_checkconstraints_option}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_checkconstraints_option(SqlServerParser.Dbcc_checkconstraints_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_checkconstraints}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_checkconstraints(SqlServerParser.Dbcc_checkconstraintsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_checkconstraints}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_checkconstraints(SqlServerParser.Dbcc_checkconstraintsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_checkdb_table_option}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_checkdb_table_option(SqlServerParser.Dbcc_checkdb_table_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_checkdb_table_option}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_checkdb_table_option(SqlServerParser.Dbcc_checkdb_table_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_checkdb}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_checkdb(SqlServerParser.Dbcc_checkdbContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_checkdb}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_checkdb(SqlServerParser.Dbcc_checkdbContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_checkfilegroup_option}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_checkfilegroup_option(SqlServerParser.Dbcc_checkfilegroup_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_checkfilegroup_option}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_checkfilegroup_option(SqlServerParser.Dbcc_checkfilegroup_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_checkfilegroup}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_checkfilegroup(SqlServerParser.Dbcc_checkfilegroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_checkfilegroup}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_checkfilegroup(SqlServerParser.Dbcc_checkfilegroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_checktable}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_checktable(SqlServerParser.Dbcc_checktableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_checktable}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_checktable(SqlServerParser.Dbcc_checktableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_cleantable}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_cleantable(SqlServerParser.Dbcc_cleantableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_cleantable}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_cleantable(SqlServerParser.Dbcc_cleantableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_clonedatabase_option}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_clonedatabase_option(SqlServerParser.Dbcc_clonedatabase_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_clonedatabase_option}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_clonedatabase_option(SqlServerParser.Dbcc_clonedatabase_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_clonedatabase}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_clonedatabase(SqlServerParser.Dbcc_clonedatabaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_clonedatabase}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_clonedatabase(SqlServerParser.Dbcc_clonedatabaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_pdw_showspaceused}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_pdw_showspaceused(SqlServerParser.Dbcc_pdw_showspaceusedContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_pdw_showspaceused}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_pdw_showspaceused(SqlServerParser.Dbcc_pdw_showspaceusedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_proccache}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_proccache(SqlServerParser.Dbcc_proccacheContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_proccache}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_proccache(SqlServerParser.Dbcc_proccacheContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_showcontig_option}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_showcontig_option(SqlServerParser.Dbcc_showcontig_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_showcontig_option}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_showcontig_option(SqlServerParser.Dbcc_showcontig_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_showcontig}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_showcontig(SqlServerParser.Dbcc_showcontigContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_showcontig}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_showcontig(SqlServerParser.Dbcc_showcontigContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_shrinklog}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_shrinklog(SqlServerParser.Dbcc_shrinklogContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_shrinklog}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_shrinklog(SqlServerParser.Dbcc_shrinklogContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_dbreindex}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_dbreindex(SqlServerParser.Dbcc_dbreindexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_dbreindex}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_dbreindex(SqlServerParser.Dbcc_dbreindexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_dll_free}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_dll_free(SqlServerParser.Dbcc_dll_freeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_dll_free}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_dll_free(SqlServerParser.Dbcc_dll_freeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_dropcleanbuffers}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_dropcleanbuffers(SqlServerParser.Dbcc_dropcleanbuffersContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_dropcleanbuffers}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_dropcleanbuffers(SqlServerParser.Dbcc_dropcleanbuffersContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dbcc_clause}.
	 * @param ctx the parse tree
	 */
	void enterDbcc_clause(SqlServerParser.Dbcc_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dbcc_clause}.
	 * @param ctx the parse tree
	 */
	void exitDbcc_clause(SqlServerParser.Dbcc_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#execute_clause}.
	 * @param ctx the parse tree
	 */
	void enterExecute_clause(SqlServerParser.Execute_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#execute_clause}.
	 * @param ctx the parse tree
	 */
	void exitExecute_clause(SqlServerParser.Execute_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#declare_local}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_local(SqlServerParser.Declare_localContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#declare_local}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_local(SqlServerParser.Declare_localContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_type_definition}.
	 * @param ctx the parse tree
	 */
	void enterTable_type_definition(SqlServerParser.Table_type_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_type_definition}.
	 * @param ctx the parse tree
	 */
	void exitTable_type_definition(SqlServerParser.Table_type_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_type_indices}.
	 * @param ctx the parse tree
	 */
	void enterTable_type_indices(SqlServerParser.Table_type_indicesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_type_indices}.
	 * @param ctx the parse tree
	 */
	void exitTable_type_indices(SqlServerParser.Table_type_indicesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#xml_type_definition}.
	 * @param ctx the parse tree
	 */
	void enterXml_type_definition(SqlServerParser.Xml_type_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#xml_type_definition}.
	 * @param ctx the parse tree
	 */
	void exitXml_type_definition(SqlServerParser.Xml_type_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#xml_schema_collection}.
	 * @param ctx the parse tree
	 */
	void enterXml_schema_collection(SqlServerParser.Xml_schema_collectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#xml_schema_collection}.
	 * @param ctx the parse tree
	 */
	void exitXml_schema_collection(SqlServerParser.Xml_schema_collectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_def_table_constraints}.
	 * @param ctx the parse tree
	 */
	void enterColumn_def_table_constraints(SqlServerParser.Column_def_table_constraintsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_def_table_constraints}.
	 * @param ctx the parse tree
	 */
	void exitColumn_def_table_constraints(SqlServerParser.Column_def_table_constraintsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_def_table_constraint}.
	 * @param ctx the parse tree
	 */
	void enterColumn_def_table_constraint(SqlServerParser.Column_def_table_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_def_table_constraint}.
	 * @param ctx the parse tree
	 */
	void exitColumn_def_table_constraint(SqlServerParser.Column_def_table_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_definition}.
	 * @param ctx the parse tree
	 */
	void enterColumn_definition(SqlServerParser.Column_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_definition}.
	 * @param ctx the parse tree
	 */
	void exitColumn_definition(SqlServerParser.Column_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_definition_element}.
	 * @param ctx the parse tree
	 */
	void enterColumn_definition_element(SqlServerParser.Column_definition_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_definition_element}.
	 * @param ctx the parse tree
	 */
	void exitColumn_definition_element(SqlServerParser.Column_definition_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_modifier}.
	 * @param ctx the parse tree
	 */
	void enterColumn_modifier(SqlServerParser.Column_modifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_modifier}.
	 * @param ctx the parse tree
	 */
	void exitColumn_modifier(SqlServerParser.Column_modifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#materialized_column_definition}.
	 * @param ctx the parse tree
	 */
	void enterMaterialized_column_definition(SqlServerParser.Materialized_column_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#materialized_column_definition}.
	 * @param ctx the parse tree
	 */
	void exitMaterialized_column_definition(SqlServerParser.Materialized_column_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_constraint}.
	 * @param ctx the parse tree
	 */
	void enterColumn_constraint(SqlServerParser.Column_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_constraint}.
	 * @param ctx the parse tree
	 */
	void exitColumn_constraint(SqlServerParser.Column_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_index}.
	 * @param ctx the parse tree
	 */
	void enterColumn_index(SqlServerParser.Column_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_index}.
	 * @param ctx the parse tree
	 */
	void exitColumn_index(SqlServerParser.Column_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#on_partition_or_filegroup}.
	 * @param ctx the parse tree
	 */
	void enterOn_partition_or_filegroup(SqlServerParser.On_partition_or_filegroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#on_partition_or_filegroup}.
	 * @param ctx the parse tree
	 */
	void exitOn_partition_or_filegroup(SqlServerParser.On_partition_or_filegroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_constraint}.
	 * @param ctx the parse tree
	 */
	void enterTable_constraint(SqlServerParser.Table_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_constraint}.
	 * @param ctx the parse tree
	 */
	void exitTable_constraint(SqlServerParser.Table_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#connection_node}.
	 * @param ctx the parse tree
	 */
	void enterConnection_node(SqlServerParser.Connection_nodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#connection_node}.
	 * @param ctx the parse tree
	 */
	void exitConnection_node(SqlServerParser.Connection_nodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#primary_key_options}.
	 * @param ctx the parse tree
	 */
	void enterPrimary_key_options(SqlServerParser.Primary_key_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#primary_key_options}.
	 * @param ctx the parse tree
	 */
	void exitPrimary_key_options(SqlServerParser.Primary_key_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#foreign_key_options}.
	 * @param ctx the parse tree
	 */
	void enterForeign_key_options(SqlServerParser.Foreign_key_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#foreign_key_options}.
	 * @param ctx the parse tree
	 */
	void exitForeign_key_options(SqlServerParser.Foreign_key_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#check_constraint}.
	 * @param ctx the parse tree
	 */
	void enterCheck_constraint(SqlServerParser.Check_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#check_constraint}.
	 * @param ctx the parse tree
	 */
	void exitCheck_constraint(SqlServerParser.Check_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#on_delete}.
	 * @param ctx the parse tree
	 */
	void enterOn_delete(SqlServerParser.On_deleteContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#on_delete}.
	 * @param ctx the parse tree
	 */
	void exitOn_delete(SqlServerParser.On_deleteContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#on_update}.
	 * @param ctx the parse tree
	 */
	void enterOn_update(SqlServerParser.On_updateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#on_update}.
	 * @param ctx the parse tree
	 */
	void exitOn_update(SqlServerParser.On_updateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_table_index_options}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_index_options(SqlServerParser.Alter_table_index_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_table_index_options}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_index_options(SqlServerParser.Alter_table_index_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#alter_table_index_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_index_option(SqlServerParser.Alter_table_index_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#alter_table_index_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_index_option(SqlServerParser.Alter_table_index_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#declare_cursor}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_cursor(SqlServerParser.Declare_cursorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#declare_cursor}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_cursor(SqlServerParser.Declare_cursorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#declare_set_cursor_common}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_set_cursor_common(SqlServerParser.Declare_set_cursor_commonContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#declare_set_cursor_common}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_set_cursor_common(SqlServerParser.Declare_set_cursor_commonContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#declare_set_cursor_common_partial}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_set_cursor_common_partial(SqlServerParser.Declare_set_cursor_common_partialContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#declare_set_cursor_common_partial}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_set_cursor_common_partial(SqlServerParser.Declare_set_cursor_common_partialContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#fetch_cursor}.
	 * @param ctx the parse tree
	 */
	void enterFetch_cursor(SqlServerParser.Fetch_cursorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#fetch_cursor}.
	 * @param ctx the parse tree
	 */
	void exitFetch_cursor(SqlServerParser.Fetch_cursorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#set_special}.
	 * @param ctx the parse tree
	 */
	void enterSet_special(SqlServerParser.Set_specialContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#set_special}.
	 * @param ctx the parse tree
	 */
	void exitSet_special(SqlServerParser.Set_specialContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#special_list}.
	 * @param ctx the parse tree
	 */
	void enterSpecial_list(SqlServerParser.Special_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#special_list}.
	 * @param ctx the parse tree
	 */
	void exitSpecial_list(SqlServerParser.Special_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#constant_LOCAL_ID}.
	 * @param ctx the parse tree
	 */
	void enterConstant_LOCAL_ID(SqlServerParser.Constant_LOCAL_IDContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#constant_LOCAL_ID}.
	 * @param ctx the parse tree
	 */
	void exitConstant_LOCAL_ID(SqlServerParser.Constant_LOCAL_IDContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(SqlServerParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(SqlServerParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(SqlServerParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(SqlServerParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#time_zone}.
	 * @param ctx the parse tree
	 */
	void enterTime_zone(SqlServerParser.Time_zoneContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#time_zone}.
	 * @param ctx the parse tree
	 */
	void exitTime_zone(SqlServerParser.Time_zoneContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#primitive_expression}.
	 * @param ctx the parse tree
	 */
	void enterPrimitive_expression(SqlServerParser.Primitive_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#primitive_expression}.
	 * @param ctx the parse tree
	 */
	void exitPrimitive_expression(SqlServerParser.Primitive_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#case_expression}.
	 * @param ctx the parse tree
	 */
	void enterCase_expression(SqlServerParser.Case_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#case_expression}.
	 * @param ctx the parse tree
	 */
	void exitCase_expression(SqlServerParser.Case_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#unary_operator_expression}.
	 * @param ctx the parse tree
	 */
	void enterUnary_operator_expression(SqlServerParser.Unary_operator_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#unary_operator_expression}.
	 * @param ctx the parse tree
	 */
	void exitUnary_operator_expression(SqlServerParser.Unary_operator_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#bracket_expression}.
	 * @param ctx the parse tree
	 */
	void enterBracket_expression(SqlServerParser.Bracket_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#bracket_expression}.
	 * @param ctx the parse tree
	 */
	void exitBracket_expression(SqlServerParser.Bracket_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#subquery}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(SqlServerParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#subquery}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(SqlServerParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#with_expression}.
	 * @param ctx the parse tree
	 */
	void enterWith_expression(SqlServerParser.With_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#with_expression}.
	 * @param ctx the parse tree
	 */
	void exitWith_expression(SqlServerParser.With_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#common_table_expression}.
	 * @param ctx the parse tree
	 */
	void enterCommon_table_expression(SqlServerParser.Common_table_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#common_table_expression}.
	 * @param ctx the parse tree
	 */
	void exitCommon_table_expression(SqlServerParser.Common_table_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#update_elem}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_elem(SqlServerParser.Update_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#update_elem}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_elem(SqlServerParser.Update_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#update_elem_merge}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_elem_merge(SqlServerParser.Update_elem_mergeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#update_elem_merge}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_elem_merge(SqlServerParser.Update_elem_mergeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#search_condition}.
	 * @param ctx the parse tree
	 */
	void enterSearch_condition(SqlServerParser.Search_conditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#search_condition}.
	 * @param ctx the parse tree
	 */
	void exitSearch_condition(SqlServerParser.Search_conditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(SqlServerParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(SqlServerParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#query_expression}.
	 * @param ctx the parse tree
	 */
	void enterQuery_expression(SqlServerParser.Query_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#query_expression}.
	 * @param ctx the parse tree
	 */
	void exitQuery_expression(SqlServerParser.Query_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#sql_union}.
	 * @param ctx the parse tree
	 */
	void enterSql_union(SqlServerParser.Sql_unionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#sql_union}.
	 * @param ctx the parse tree
	 */
	void exitSql_union(SqlServerParser.Sql_unionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#query_specification}.
	 * @param ctx the parse tree
	 */
	void enterQuery_specification(SqlServerParser.Query_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#query_specification}.
	 * @param ctx the parse tree
	 */
	void exitQuery_specification(SqlServerParser.Query_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#top_clause}.
	 * @param ctx the parse tree
	 */
	void enterTop_clause(SqlServerParser.Top_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#top_clause}.
	 * @param ctx the parse tree
	 */
	void exitTop_clause(SqlServerParser.Top_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#top_percent}.
	 * @param ctx the parse tree
	 */
	void enterTop_percent(SqlServerParser.Top_percentContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#top_percent}.
	 * @param ctx the parse tree
	 */
	void exitTop_percent(SqlServerParser.Top_percentContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#top_count}.
	 * @param ctx the parse tree
	 */
	void enterTop_count(SqlServerParser.Top_countContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#top_count}.
	 * @param ctx the parse tree
	 */
	void exitTop_count(SqlServerParser.Top_countContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#order_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by_clause(SqlServerParser.Order_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#order_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by_clause(SqlServerParser.Order_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#select_order_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterSelect_order_by_clause(SqlServerParser.Select_order_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#select_order_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitSelect_order_by_clause(SqlServerParser.Select_order_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#for_clause}.
	 * @param ctx the parse tree
	 */
	void enterFor_clause(SqlServerParser.For_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#for_clause}.
	 * @param ctx the parse tree
	 */
	void exitFor_clause(SqlServerParser.For_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#xml_common_directives}.
	 * @param ctx the parse tree
	 */
	void enterXml_common_directives(SqlServerParser.Xml_common_directivesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#xml_common_directives}.
	 * @param ctx the parse tree
	 */
	void exitXml_common_directives(SqlServerParser.Xml_common_directivesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#order_by_expression}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by_expression(SqlServerParser.Order_by_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#order_by_expression}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by_expression(SqlServerParser.Order_by_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#grouping_sets_item}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets_item(SqlServerParser.Grouping_sets_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#grouping_sets_item}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets_item(SqlServerParser.Grouping_sets_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#group_by_item}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_item(SqlServerParser.Group_by_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#group_by_item}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_item(SqlServerParser.Group_by_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#option_clause}.
	 * @param ctx the parse tree
	 */
	void enterOption_clause(SqlServerParser.Option_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#option_clause}.
	 * @param ctx the parse tree
	 */
	void exitOption_clause(SqlServerParser.Option_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#option}.
	 * @param ctx the parse tree
	 */
	void enterOption(SqlServerParser.OptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#option}.
	 * @param ctx the parse tree
	 */
	void exitOption(SqlServerParser.OptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#optimize_for_arg}.
	 * @param ctx the parse tree
	 */
	void enterOptimize_for_arg(SqlServerParser.Optimize_for_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#optimize_for_arg}.
	 * @param ctx the parse tree
	 */
	void exitOptimize_for_arg(SqlServerParser.Optimize_for_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#select_list}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list(SqlServerParser.Select_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#select_list}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list(SqlServerParser.Select_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#udt_method_arguments}.
	 * @param ctx the parse tree
	 */
	void enterUdt_method_arguments(SqlServerParser.Udt_method_argumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#udt_method_arguments}.
	 * @param ctx the parse tree
	 */
	void exitUdt_method_arguments(SqlServerParser.Udt_method_argumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#asterisk}.
	 * @param ctx the parse tree
	 */
	void enterAsterisk(SqlServerParser.AsteriskContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#asterisk}.
	 * @param ctx the parse tree
	 */
	void exitAsterisk(SqlServerParser.AsteriskContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#udt_elem}.
	 * @param ctx the parse tree
	 */
	void enterUdt_elem(SqlServerParser.Udt_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#udt_elem}.
	 * @param ctx the parse tree
	 */
	void exitUdt_elem(SqlServerParser.Udt_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#expression_elem}.
	 * @param ctx the parse tree
	 */
	void enterExpression_elem(SqlServerParser.Expression_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#expression_elem}.
	 * @param ctx the parse tree
	 */
	void exitExpression_elem(SqlServerParser.Expression_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#select_list_elem}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list_elem(SqlServerParser.Select_list_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#select_list_elem}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list_elem(SqlServerParser.Select_list_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_sources}.
	 * @param ctx the parse tree
	 */
	void enterTable_sources(SqlServerParser.Table_sourcesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_sources}.
	 * @param ctx the parse tree
	 */
	void exitTable_sources(SqlServerParser.Table_sourcesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#non_ansi_join}.
	 * @param ctx the parse tree
	 */
	void enterNon_ansi_join(SqlServerParser.Non_ansi_joinContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#non_ansi_join}.
	 * @param ctx the parse tree
	 */
	void exitNon_ansi_join(SqlServerParser.Non_ansi_joinContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_source}.
	 * @param ctx the parse tree
	 */
	void enterTable_source(SqlServerParser.Table_sourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_source}.
	 * @param ctx the parse tree
	 */
	void exitTable_source(SqlServerParser.Table_sourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_source_item}.
	 * @param ctx the parse tree
	 */
	void enterTable_source_item(SqlServerParser.Table_source_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_source_item}.
	 * @param ctx the parse tree
	 */
	void exitTable_source_item(SqlServerParser.Table_source_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#open_xml}.
	 * @param ctx the parse tree
	 */
	void enterOpen_xml(SqlServerParser.Open_xmlContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#open_xml}.
	 * @param ctx the parse tree
	 */
	void exitOpen_xml(SqlServerParser.Open_xmlContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#open_json}.
	 * @param ctx the parse tree
	 */
	void enterOpen_json(SqlServerParser.Open_jsonContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#open_json}.
	 * @param ctx the parse tree
	 */
	void exitOpen_json(SqlServerParser.Open_jsonContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#json_declaration}.
	 * @param ctx the parse tree
	 */
	void enterJson_declaration(SqlServerParser.Json_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#json_declaration}.
	 * @param ctx the parse tree
	 */
	void exitJson_declaration(SqlServerParser.Json_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#json_column_declaration}.
	 * @param ctx the parse tree
	 */
	void enterJson_column_declaration(SqlServerParser.Json_column_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#json_column_declaration}.
	 * @param ctx the parse tree
	 */
	void exitJson_column_declaration(SqlServerParser.Json_column_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#schema_declaration}.
	 * @param ctx the parse tree
	 */
	void enterSchema_declaration(SqlServerParser.Schema_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#schema_declaration}.
	 * @param ctx the parse tree
	 */
	void exitSchema_declaration(SqlServerParser.Schema_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_declaration}.
	 * @param ctx the parse tree
	 */
	void enterColumn_declaration(SqlServerParser.Column_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_declaration}.
	 * @param ctx the parse tree
	 */
	void exitColumn_declaration(SqlServerParser.Column_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#change_table}.
	 * @param ctx the parse tree
	 */
	void enterChange_table(SqlServerParser.Change_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#change_table}.
	 * @param ctx the parse tree
	 */
	void exitChange_table(SqlServerParser.Change_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#change_table_changes}.
	 * @param ctx the parse tree
	 */
	void enterChange_table_changes(SqlServerParser.Change_table_changesContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#change_table_changes}.
	 * @param ctx the parse tree
	 */
	void exitChange_table_changes(SqlServerParser.Change_table_changesContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#change_table_version}.
	 * @param ctx the parse tree
	 */
	void enterChange_table_version(SqlServerParser.Change_table_versionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#change_table_version}.
	 * @param ctx the parse tree
	 */
	void exitChange_table_version(SqlServerParser.Change_table_versionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#join_part}.
	 * @param ctx the parse tree
	 */
	void enterJoin_part(SqlServerParser.Join_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#join_part}.
	 * @param ctx the parse tree
	 */
	void exitJoin_part(SqlServerParser.Join_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#join_on}.
	 * @param ctx the parse tree
	 */
	void enterJoin_on(SqlServerParser.Join_onContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#join_on}.
	 * @param ctx the parse tree
	 */
	void exitJoin_on(SqlServerParser.Join_onContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#cross_join}.
	 * @param ctx the parse tree
	 */
	void enterCross_join(SqlServerParser.Cross_joinContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#cross_join}.
	 * @param ctx the parse tree
	 */
	void exitCross_join(SqlServerParser.Cross_joinContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#apply_}.
	 * @param ctx the parse tree
	 */
	void enterApply_(SqlServerParser.Apply_Context ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#apply_}.
	 * @param ctx the parse tree
	 */
	void exitApply_(SqlServerParser.Apply_Context ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#pivot}.
	 * @param ctx the parse tree
	 */
	void enterPivot(SqlServerParser.PivotContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#pivot}.
	 * @param ctx the parse tree
	 */
	void exitPivot(SqlServerParser.PivotContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#unpivot}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot(SqlServerParser.UnpivotContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#unpivot}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot(SqlServerParser.UnpivotContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#pivot_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_clause(SqlServerParser.Pivot_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#pivot_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_clause(SqlServerParser.Pivot_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#unpivot_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_clause(SqlServerParser.Unpivot_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#unpivot_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_clause(SqlServerParser.Unpivot_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#full_column_name_list}.
	 * @param ctx the parse tree
	 */
	void enterFull_column_name_list(SqlServerParser.Full_column_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#full_column_name_list}.
	 * @param ctx the parse tree
	 */
	void exitFull_column_name_list(SqlServerParser.Full_column_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#rowset_function}.
	 * @param ctx the parse tree
	 */
	void enterRowset_function(SqlServerParser.Rowset_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#rowset_function}.
	 * @param ctx the parse tree
	 */
	void exitRowset_function(SqlServerParser.Rowset_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#bulk_option}.
	 * @param ctx the parse tree
	 */
	void enterBulk_option(SqlServerParser.Bulk_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#bulk_option}.
	 * @param ctx the parse tree
	 */
	void exitBulk_option(SqlServerParser.Bulk_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#derived_table}.
	 * @param ctx the parse tree
	 */
	void enterDerived_table(SqlServerParser.Derived_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#derived_table}.
	 * @param ctx the parse tree
	 */
	void exitDerived_table(SqlServerParser.Derived_tableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RANKING_WINDOWED_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterRANKING_WINDOWED_FUNC(SqlServerParser.RANKING_WINDOWED_FUNCContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RANKING_WINDOWED_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitRANKING_WINDOWED_FUNC(SqlServerParser.RANKING_WINDOWED_FUNCContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AGGREGATE_WINDOWED_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterAGGREGATE_WINDOWED_FUNC(SqlServerParser.AGGREGATE_WINDOWED_FUNCContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AGGREGATE_WINDOWED_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitAGGREGATE_WINDOWED_FUNC(SqlServerParser.AGGREGATE_WINDOWED_FUNCContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ANALYTIC_WINDOWED_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterANALYTIC_WINDOWED_FUNC(SqlServerParser.ANALYTIC_WINDOWED_FUNCContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ANALYTIC_WINDOWED_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitANALYTIC_WINDOWED_FUNC(SqlServerParser.ANALYTIC_WINDOWED_FUNCContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BUILT_IN_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterBUILT_IN_FUNC(SqlServerParser.BUILT_IN_FUNCContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BUILT_IN_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitBUILT_IN_FUNC(SqlServerParser.BUILT_IN_FUNCContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SCALAR_FUNCTION}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterSCALAR_FUNCTION(SqlServerParser.SCALAR_FUNCTIONContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SCALAR_FUNCTION}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitSCALAR_FUNCTION(SqlServerParser.SCALAR_FUNCTIONContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FREE_TEXT}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterFREE_TEXT(SqlServerParser.FREE_TEXTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FREE_TEXT}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitFREE_TEXT(SqlServerParser.FREE_TEXTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PARTITION_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterPARTITION_FUNC(SqlServerParser.PARTITION_FUNCContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PARTITION_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitPARTITION_FUNC(SqlServerParser.PARTITION_FUNCContext ctx);
	/**
	 * Enter a parse tree produced by the {@code HIERARCHYID_METHOD}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterHIERARCHYID_METHOD(SqlServerParser.HIERARCHYID_METHODContext ctx);
	/**
	 * Exit a parse tree produced by the {@code HIERARCHYID_METHOD}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitHIERARCHYID_METHOD(SqlServerParser.HIERARCHYID_METHODContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#partition_function}.
	 * @param ctx the parse tree
	 */
	void enterPartition_function(SqlServerParser.Partition_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#partition_function}.
	 * @param ctx the parse tree
	 */
	void exitPartition_function(SqlServerParser.Partition_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#freetext_function}.
	 * @param ctx the parse tree
	 */
	void enterFreetext_function(SqlServerParser.Freetext_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#freetext_function}.
	 * @param ctx the parse tree
	 */
	void exitFreetext_function(SqlServerParser.Freetext_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#freetext_predicate}.
	 * @param ctx the parse tree
	 */
	void enterFreetext_predicate(SqlServerParser.Freetext_predicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#freetext_predicate}.
	 * @param ctx the parse tree
	 */
	void exitFreetext_predicate(SqlServerParser.Freetext_predicateContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#json_key_value}.
	 * @param ctx the parse tree
	 */
	void enterJson_key_value(SqlServerParser.Json_key_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#json_key_value}.
	 * @param ctx the parse tree
	 */
	void exitJson_key_value(SqlServerParser.Json_key_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#json_null_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_null_clause(SqlServerParser.Json_null_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#json_null_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_null_clause(SqlServerParser.Json_null_clauseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code APP_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterAPP_NAME(SqlServerParser.APP_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code APP_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitAPP_NAME(SqlServerParser.APP_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code APPLOCK_MODE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterAPPLOCK_MODE(SqlServerParser.APPLOCK_MODEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code APPLOCK_MODE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitAPPLOCK_MODE(SqlServerParser.APPLOCK_MODEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code APPLOCK_TEST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterAPPLOCK_TEST(SqlServerParser.APPLOCK_TESTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code APPLOCK_TEST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitAPPLOCK_TEST(SqlServerParser.APPLOCK_TESTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ASSEMBLYPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterASSEMBLYPROPERTY(SqlServerParser.ASSEMBLYPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ASSEMBLYPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitASSEMBLYPROPERTY(SqlServerParser.ASSEMBLYPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code COL_LENGTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCOL_LENGTH(SqlServerParser.COL_LENGTHContext ctx);
	/**
	 * Exit a parse tree produced by the {@code COL_LENGTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCOL_LENGTH(SqlServerParser.COL_LENGTHContext ctx);
	/**
	 * Enter a parse tree produced by the {@code COL_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCOL_NAME(SqlServerParser.COL_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code COL_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCOL_NAME(SqlServerParser.COL_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code COLUMNPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCOLUMNPROPERTY(SqlServerParser.COLUMNPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code COLUMNPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCOLUMNPROPERTY(SqlServerParser.COLUMNPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATABASEPROPERTYEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATABASEPROPERTYEX(SqlServerParser.DATABASEPROPERTYEXContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATABASEPROPERTYEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATABASEPROPERTYEX(SqlServerParser.DATABASEPROPERTYEXContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DB_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDB_ID(SqlServerParser.DB_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DB_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDB_ID(SqlServerParser.DB_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DB_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDB_NAME(SqlServerParser.DB_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DB_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDB_NAME(SqlServerParser.DB_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FILE_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFILE_ID(SqlServerParser.FILE_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FILE_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFILE_ID(SqlServerParser.FILE_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FILE_IDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFILE_IDEX(SqlServerParser.FILE_IDEXContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FILE_IDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFILE_IDEX(SqlServerParser.FILE_IDEXContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FILE_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFILE_NAME(SqlServerParser.FILE_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FILE_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFILE_NAME(SqlServerParser.FILE_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FILEGROUP_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFILEGROUP_ID(SqlServerParser.FILEGROUP_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FILEGROUP_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFILEGROUP_ID(SqlServerParser.FILEGROUP_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FILEGROUP_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFILEGROUP_NAME(SqlServerParser.FILEGROUP_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FILEGROUP_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFILEGROUP_NAME(SqlServerParser.FILEGROUP_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FILEGROUPPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFILEGROUPPROPERTY(SqlServerParser.FILEGROUPPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FILEGROUPPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFILEGROUPPROPERTY(SqlServerParser.FILEGROUPPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FILEPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFILEPROPERTY(SqlServerParser.FILEPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FILEPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFILEPROPERTY(SqlServerParser.FILEPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FILEPROPERTYEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFILEPROPERTYEX(SqlServerParser.FILEPROPERTYEXContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FILEPROPERTYEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFILEPROPERTYEX(SqlServerParser.FILEPROPERTYEXContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FULLTEXTCATALOGPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFULLTEXTCATALOGPROPERTY(SqlServerParser.FULLTEXTCATALOGPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FULLTEXTCATALOGPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFULLTEXTCATALOGPROPERTY(SqlServerParser.FULLTEXTCATALOGPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FULLTEXTSERVICEPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFULLTEXTSERVICEPROPERTY(SqlServerParser.FULLTEXTSERVICEPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FULLTEXTSERVICEPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFULLTEXTSERVICEPROPERTY(SqlServerParser.FULLTEXTSERVICEPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code INDEX_COL}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterINDEX_COL(SqlServerParser.INDEX_COLContext ctx);
	/**
	 * Exit a parse tree produced by the {@code INDEX_COL}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitINDEX_COL(SqlServerParser.INDEX_COLContext ctx);
	/**
	 * Enter a parse tree produced by the {@code INDEXKEY_PROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterINDEXKEY_PROPERTY(SqlServerParser.INDEXKEY_PROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code INDEXKEY_PROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitINDEXKEY_PROPERTY(SqlServerParser.INDEXKEY_PROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code INDEXPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterINDEXPROPERTY(SqlServerParser.INDEXPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code INDEXPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitINDEXPROPERTY(SqlServerParser.INDEXPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NEXT_VALUE_FOR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterNEXT_VALUE_FOR(SqlServerParser.NEXT_VALUE_FORContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NEXT_VALUE_FOR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitNEXT_VALUE_FOR(SqlServerParser.NEXT_VALUE_FORContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OBJECT_DEFINITION}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterOBJECT_DEFINITION(SqlServerParser.OBJECT_DEFINITIONContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OBJECT_DEFINITION}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitOBJECT_DEFINITION(SqlServerParser.OBJECT_DEFINITIONContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OBJECT_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterOBJECT_ID(SqlServerParser.OBJECT_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OBJECT_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitOBJECT_ID(SqlServerParser.OBJECT_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OBJECT_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterOBJECT_NAME(SqlServerParser.OBJECT_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OBJECT_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitOBJECT_NAME(SqlServerParser.OBJECT_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OBJECT_SCHEMA_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterOBJECT_SCHEMA_NAME(SqlServerParser.OBJECT_SCHEMA_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OBJECT_SCHEMA_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitOBJECT_SCHEMA_NAME(SqlServerParser.OBJECT_SCHEMA_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OBJECTPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterOBJECTPROPERTY(SqlServerParser.OBJECTPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OBJECTPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitOBJECTPROPERTY(SqlServerParser.OBJECTPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OBJECTPROPERTYEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterOBJECTPROPERTYEX(SqlServerParser.OBJECTPROPERTYEXContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OBJECTPROPERTYEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitOBJECTPROPERTYEX(SqlServerParser.OBJECTPROPERTYEXContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ORIGINAL_DB_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterORIGINAL_DB_NAME(SqlServerParser.ORIGINAL_DB_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ORIGINAL_DB_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitORIGINAL_DB_NAME(SqlServerParser.ORIGINAL_DB_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PARSENAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterPARSENAME(SqlServerParser.PARSENAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PARSENAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitPARSENAME(SqlServerParser.PARSENAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SCHEMA_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSCHEMA_ID(SqlServerParser.SCHEMA_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SCHEMA_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSCHEMA_ID(SqlServerParser.SCHEMA_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SCHEMA_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSCHEMA_NAME(SqlServerParser.SCHEMA_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SCHEMA_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSCHEMA_NAME(SqlServerParser.SCHEMA_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SCOPE_IDENTITY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSCOPE_IDENTITY(SqlServerParser.SCOPE_IDENTITYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SCOPE_IDENTITY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSCOPE_IDENTITY(SqlServerParser.SCOPE_IDENTITYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SERVERPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSERVERPROPERTY(SqlServerParser.SERVERPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SERVERPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSERVERPROPERTY(SqlServerParser.SERVERPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code STATS_DATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSTATS_DATE(SqlServerParser.STATS_DATEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code STATS_DATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSTATS_DATE(SqlServerParser.STATS_DATEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TYPE_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterTYPE_ID(SqlServerParser.TYPE_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TYPE_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitTYPE_ID(SqlServerParser.TYPE_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TYPE_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterTYPE_NAME(SqlServerParser.TYPE_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TYPE_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitTYPE_NAME(SqlServerParser.TYPE_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TYPEPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterTYPEPROPERTY(SqlServerParser.TYPEPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TYPEPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitTYPEPROPERTY(SqlServerParser.TYPEPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ASCII}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterASCII(SqlServerParser.ASCIIContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ASCII}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitASCII(SqlServerParser.ASCIIContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CHAR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCHAR(SqlServerParser.CHARContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CHAR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCHAR(SqlServerParser.CHARContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CHARINDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCHARINDEX(SqlServerParser.CHARINDEXContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CHARINDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCHARINDEX(SqlServerParser.CHARINDEXContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CONCAT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCONCAT(SqlServerParser.CONCATContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CONCAT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCONCAT(SqlServerParser.CONCATContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CONCAT_WS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCONCAT_WS(SqlServerParser.CONCAT_WSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CONCAT_WS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCONCAT_WS(SqlServerParser.CONCAT_WSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DIFFERENCE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDIFFERENCE(SqlServerParser.DIFFERENCEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DIFFERENCE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDIFFERENCE(SqlServerParser.DIFFERENCEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FORMAT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFORMAT(SqlServerParser.FORMATContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FORMAT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFORMAT(SqlServerParser.FORMATContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LEFT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterLEFT(SqlServerParser.LEFTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LEFT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitLEFT(SqlServerParser.LEFTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LEN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterLEN(SqlServerParser.LENContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LEN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitLEN(SqlServerParser.LENContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LOWER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterLOWER(SqlServerParser.LOWERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LOWER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitLOWER(SqlServerParser.LOWERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LTRIM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterLTRIM(SqlServerParser.LTRIMContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LTRIM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitLTRIM(SqlServerParser.LTRIMContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NCHAR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterNCHAR(SqlServerParser.NCHARContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NCHAR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitNCHAR(SqlServerParser.NCHARContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PATINDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterPATINDEX(SqlServerParser.PATINDEXContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PATINDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitPATINDEX(SqlServerParser.PATINDEXContext ctx);
	/**
	 * Enter a parse tree produced by the {@code QUOTENAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterQUOTENAME(SqlServerParser.QUOTENAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code QUOTENAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitQUOTENAME(SqlServerParser.QUOTENAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code REPLACE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterREPLACE(SqlServerParser.REPLACEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code REPLACE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitREPLACE(SqlServerParser.REPLACEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code REPLICATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterREPLICATE(SqlServerParser.REPLICATEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code REPLICATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitREPLICATE(SqlServerParser.REPLICATEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code REVERSE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterREVERSE(SqlServerParser.REVERSEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code REVERSE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitREVERSE(SqlServerParser.REVERSEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RIGHT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterRIGHT(SqlServerParser.RIGHTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RIGHT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitRIGHT(SqlServerParser.RIGHTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RTRIM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterRTRIM(SqlServerParser.RTRIMContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RTRIM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitRTRIM(SqlServerParser.RTRIMContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SOUNDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSOUNDEX(SqlServerParser.SOUNDEXContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SOUNDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSOUNDEX(SqlServerParser.SOUNDEXContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SPACE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSPACE(SqlServerParser.SPACEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SPACE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSPACE(SqlServerParser.SPACEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code STR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSTR(SqlServerParser.STRContext ctx);
	/**
	 * Exit a parse tree produced by the {@code STR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSTR(SqlServerParser.STRContext ctx);
	/**
	 * Enter a parse tree produced by the {@code STRINGAGG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSTRINGAGG(SqlServerParser.STRINGAGGContext ctx);
	/**
	 * Exit a parse tree produced by the {@code STRINGAGG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSTRINGAGG(SqlServerParser.STRINGAGGContext ctx);
	/**
	 * Enter a parse tree produced by the {@code STRING_ESCAPE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSTRING_ESCAPE(SqlServerParser.STRING_ESCAPEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code STRING_ESCAPE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSTRING_ESCAPE(SqlServerParser.STRING_ESCAPEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code STUFF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSTUFF(SqlServerParser.STUFFContext ctx);
	/**
	 * Exit a parse tree produced by the {@code STUFF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSTUFF(SqlServerParser.STUFFContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SUBSTRING}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSUBSTRING(SqlServerParser.SUBSTRINGContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SUBSTRING}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSUBSTRING(SqlServerParser.SUBSTRINGContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TRANSLATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterTRANSLATE(SqlServerParser.TRANSLATEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TRANSLATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitTRANSLATE(SqlServerParser.TRANSLATEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TRIM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterTRIM(SqlServerParser.TRIMContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TRIM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitTRIM(SqlServerParser.TRIMContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UNICODE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterUNICODE(SqlServerParser.UNICODEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UNICODE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitUNICODE(SqlServerParser.UNICODEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UPPER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterUPPER(SqlServerParser.UPPERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UPPER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitUPPER(SqlServerParser.UPPERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BINARY_CHECKSUM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterBINARY_CHECKSUM(SqlServerParser.BINARY_CHECKSUMContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BINARY_CHECKSUM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitBINARY_CHECKSUM(SqlServerParser.BINARY_CHECKSUMContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CHECKSUM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCHECKSUM(SqlServerParser.CHECKSUMContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CHECKSUM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCHECKSUM(SqlServerParser.CHECKSUMContext ctx);
	/**
	 * Enter a parse tree produced by the {@code COMPRESS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCOMPRESS(SqlServerParser.COMPRESSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code COMPRESS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCOMPRESS(SqlServerParser.COMPRESSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CONNECTIONPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCONNECTIONPROPERTY(SqlServerParser.CONNECTIONPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CONNECTIONPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCONNECTIONPROPERTY(SqlServerParser.CONNECTIONPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CONTEXT_INFO}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCONTEXT_INFO(SqlServerParser.CONTEXT_INFOContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CONTEXT_INFO}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCONTEXT_INFO(SqlServerParser.CONTEXT_INFOContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CURRENT_REQUEST_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCURRENT_REQUEST_ID(SqlServerParser.CURRENT_REQUEST_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CURRENT_REQUEST_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCURRENT_REQUEST_ID(SqlServerParser.CURRENT_REQUEST_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CURRENT_TRANSACTION_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCURRENT_TRANSACTION_ID(SqlServerParser.CURRENT_TRANSACTION_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CURRENT_TRANSACTION_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCURRENT_TRANSACTION_ID(SqlServerParser.CURRENT_TRANSACTION_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DECOMPRESS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDECOMPRESS(SqlServerParser.DECOMPRESSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DECOMPRESS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDECOMPRESS(SqlServerParser.DECOMPRESSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ERROR_LINE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterERROR_LINE(SqlServerParser.ERROR_LINEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ERROR_LINE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitERROR_LINE(SqlServerParser.ERROR_LINEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ERROR_MESSAGE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterERROR_MESSAGE(SqlServerParser.ERROR_MESSAGEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ERROR_MESSAGE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitERROR_MESSAGE(SqlServerParser.ERROR_MESSAGEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ERROR_NUMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterERROR_NUMBER(SqlServerParser.ERROR_NUMBERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ERROR_NUMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitERROR_NUMBER(SqlServerParser.ERROR_NUMBERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ERROR_PROCEDURE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterERROR_PROCEDURE(SqlServerParser.ERROR_PROCEDUREContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ERROR_PROCEDURE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitERROR_PROCEDURE(SqlServerParser.ERROR_PROCEDUREContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ERROR_SEVERITY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterERROR_SEVERITY(SqlServerParser.ERROR_SEVERITYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ERROR_SEVERITY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitERROR_SEVERITY(SqlServerParser.ERROR_SEVERITYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ERROR_STATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterERROR_STATE(SqlServerParser.ERROR_STATEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ERROR_STATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitERROR_STATE(SqlServerParser.ERROR_STATEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FORMATMESSAGE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFORMATMESSAGE(SqlServerParser.FORMATMESSAGEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FORMATMESSAGE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFORMATMESSAGE(SqlServerParser.FORMATMESSAGEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code GET_FILESTREAM_TRANSACTION_CONTEXT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterGET_FILESTREAM_TRANSACTION_CONTEXT(SqlServerParser.GET_FILESTREAM_TRANSACTION_CONTEXTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code GET_FILESTREAM_TRANSACTION_CONTEXT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitGET_FILESTREAM_TRANSACTION_CONTEXT(SqlServerParser.GET_FILESTREAM_TRANSACTION_CONTEXTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code GETANSINULL}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterGETANSINULL(SqlServerParser.GETANSINULLContext ctx);
	/**
	 * Exit a parse tree produced by the {@code GETANSINULL}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitGETANSINULL(SqlServerParser.GETANSINULLContext ctx);
	/**
	 * Enter a parse tree produced by the {@code HOST_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterHOST_ID(SqlServerParser.HOST_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code HOST_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitHOST_ID(SqlServerParser.HOST_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code HOST_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterHOST_NAME(SqlServerParser.HOST_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code HOST_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitHOST_NAME(SqlServerParser.HOST_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ISNULL}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterISNULL(SqlServerParser.ISNULLContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ISNULL}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitISNULL(SqlServerParser.ISNULLContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ISNUMERIC}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterISNUMERIC(SqlServerParser.ISNUMERICContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ISNUMERIC}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitISNUMERIC(SqlServerParser.ISNUMERICContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MIN_ACTIVE_ROWVERSION}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterMIN_ACTIVE_ROWVERSION(SqlServerParser.MIN_ACTIVE_ROWVERSIONContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MIN_ACTIVE_ROWVERSION}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitMIN_ACTIVE_ROWVERSION(SqlServerParser.MIN_ACTIVE_ROWVERSIONContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NEWID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterNEWID(SqlServerParser.NEWIDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NEWID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitNEWID(SqlServerParser.NEWIDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NEWSEQUENTIALID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterNEWSEQUENTIALID(SqlServerParser.NEWSEQUENTIALIDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NEWSEQUENTIALID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitNEWSEQUENTIALID(SqlServerParser.NEWSEQUENTIALIDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ROWCOUNT_BIG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterROWCOUNT_BIG(SqlServerParser.ROWCOUNT_BIGContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ROWCOUNT_BIG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitROWCOUNT_BIG(SqlServerParser.ROWCOUNT_BIGContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SESSION_CONTEXT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSESSION_CONTEXT(SqlServerParser.SESSION_CONTEXTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SESSION_CONTEXT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSESSION_CONTEXT(SqlServerParser.SESSION_CONTEXTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code XACT_STATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterXACT_STATE(SqlServerParser.XACT_STATEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code XACT_STATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitXACT_STATE(SqlServerParser.XACT_STATEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CAST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCAST(SqlServerParser.CASTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CAST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCAST(SqlServerParser.CASTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TRY_CAST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterTRY_CAST(SqlServerParser.TRY_CASTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TRY_CAST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitTRY_CAST(SqlServerParser.TRY_CASTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CONVERT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCONVERT(SqlServerParser.CONVERTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CONVERT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCONVERT(SqlServerParser.CONVERTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code COALESCE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCOALESCE(SqlServerParser.COALESCEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code COALESCE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCOALESCE(SqlServerParser.COALESCEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CURSOR_ROWS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCURSOR_ROWS(SqlServerParser.CURSOR_ROWSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CURSOR_ROWS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCURSOR_ROWS(SqlServerParser.CURSOR_ROWSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FETCH_STATUS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFETCH_STATUS(SqlServerParser.FETCH_STATUSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FETCH_STATUS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFETCH_STATUS(SqlServerParser.FETCH_STATUSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CURSOR_STATUS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCURSOR_STATUS(SqlServerParser.CURSOR_STATUSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CURSOR_STATUS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCURSOR_STATUS(SqlServerParser.CURSOR_STATUSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CERT_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCERT_ID(SqlServerParser.CERT_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CERT_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCERT_ID(SqlServerParser.CERT_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATALENGTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATALENGTH(SqlServerParser.DATALENGTHContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATALENGTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATALENGTH(SqlServerParser.DATALENGTHContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IDENT_CURRENT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterIDENT_CURRENT(SqlServerParser.IDENT_CURRENTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IDENT_CURRENT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitIDENT_CURRENT(SqlServerParser.IDENT_CURRENTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IDENT_INCR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterIDENT_INCR(SqlServerParser.IDENT_INCRContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IDENT_INCR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitIDENT_INCR(SqlServerParser.IDENT_INCRContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IDENT_SEED}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterIDENT_SEED(SqlServerParser.IDENT_SEEDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IDENT_SEED}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitIDENT_SEED(SqlServerParser.IDENT_SEEDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IDENTITY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterIDENTITY(SqlServerParser.IDENTITYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IDENTITY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitIDENTITY(SqlServerParser.IDENTITYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SQL_VARIANT_PROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSQL_VARIANT_PROPERTY(SqlServerParser.SQL_VARIANT_PROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SQL_VARIANT_PROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSQL_VARIANT_PROPERTY(SqlServerParser.SQL_VARIANT_PROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CURRENT_DATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCURRENT_DATE(SqlServerParser.CURRENT_DATEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CURRENT_DATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCURRENT_DATE(SqlServerParser.CURRENT_DATEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CURRENT_TIMESTAMP}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCURRENT_TIMESTAMP(SqlServerParser.CURRENT_TIMESTAMPContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CURRENT_TIMESTAMP}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCURRENT_TIMESTAMP(SqlServerParser.CURRENT_TIMESTAMPContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CURRENT_TIMEZONE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCURRENT_TIMEZONE(SqlServerParser.CURRENT_TIMEZONEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CURRENT_TIMEZONE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCURRENT_TIMEZONE(SqlServerParser.CURRENT_TIMEZONEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CURRENT_TIMEZONE_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCURRENT_TIMEZONE_ID(SqlServerParser.CURRENT_TIMEZONE_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CURRENT_TIMEZONE_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCURRENT_TIMEZONE_ID(SqlServerParser.CURRENT_TIMEZONE_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATE_BUCKET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATE_BUCKET(SqlServerParser.DATE_BUCKETContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATE_BUCKET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATE_BUCKET(SqlServerParser.DATE_BUCKETContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATEADD}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATEADD(SqlServerParser.DATEADDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATEADD}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATEADD(SqlServerParser.DATEADDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATEDIFF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATEDIFF(SqlServerParser.DATEDIFFContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATEDIFF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATEDIFF(SqlServerParser.DATEDIFFContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATEDIFF_BIG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATEDIFF_BIG(SqlServerParser.DATEDIFF_BIGContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATEDIFF_BIG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATEDIFF_BIG(SqlServerParser.DATEDIFF_BIGContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATEFROMPARTS(SqlServerParser.DATEFROMPARTSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATEFROMPARTS(SqlServerParser.DATEFROMPARTSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATENAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATENAME(SqlServerParser.DATENAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATENAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATENAME(SqlServerParser.DATENAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATEPART}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATEPART(SqlServerParser.DATEPARTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATEPART}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATEPART(SqlServerParser.DATEPARTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATETIME2FROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATETIME2FROMPARTS(SqlServerParser.DATETIME2FROMPARTSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATETIME2FROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATETIME2FROMPARTS(SqlServerParser.DATETIME2FROMPARTSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATETIMEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATETIMEFROMPARTS(SqlServerParser.DATETIMEFROMPARTSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATETIMEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATETIMEFROMPARTS(SqlServerParser.DATETIMEFROMPARTSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATETIMEOFFSETFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATETIMEOFFSETFROMPARTS(SqlServerParser.DATETIMEOFFSETFROMPARTSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATETIMEOFFSETFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATETIMEOFFSETFROMPARTS(SqlServerParser.DATETIMEOFFSETFROMPARTSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATETRUNC}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATETRUNC(SqlServerParser.DATETRUNCContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATETRUNC}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATETRUNC(SqlServerParser.DATETRUNCContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DAY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDAY(SqlServerParser.DAYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DAY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDAY(SqlServerParser.DAYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code EOMONTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterEOMONTH(SqlServerParser.EOMONTHContext ctx);
	/**
	 * Exit a parse tree produced by the {@code EOMONTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitEOMONTH(SqlServerParser.EOMONTHContext ctx);
	/**
	 * Enter a parse tree produced by the {@code GETDATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterGETDATE(SqlServerParser.GETDATEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code GETDATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitGETDATE(SqlServerParser.GETDATEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code GETUTCDATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterGETUTCDATE(SqlServerParser.GETUTCDATEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code GETUTCDATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitGETUTCDATE(SqlServerParser.GETUTCDATEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ISDATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterISDATE(SqlServerParser.ISDATEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ISDATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitISDATE(SqlServerParser.ISDATEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MONTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterMONTH(SqlServerParser.MONTHContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MONTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitMONTH(SqlServerParser.MONTHContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SMALLDATETIMEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSMALLDATETIMEFROMPARTS(SqlServerParser.SMALLDATETIMEFROMPARTSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SMALLDATETIMEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSMALLDATETIMEFROMPARTS(SqlServerParser.SMALLDATETIMEFROMPARTSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SWITCHOFFSET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSWITCHOFFSET(SqlServerParser.SWITCHOFFSETContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SWITCHOFFSET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSWITCHOFFSET(SqlServerParser.SWITCHOFFSETContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SYSDATETIME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSYSDATETIME(SqlServerParser.SYSDATETIMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SYSDATETIME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSYSDATETIME(SqlServerParser.SYSDATETIMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SYSDATETIMEOFFSET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSYSDATETIMEOFFSET(SqlServerParser.SYSDATETIMEOFFSETContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SYSDATETIMEOFFSET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSYSDATETIMEOFFSET(SqlServerParser.SYSDATETIMEOFFSETContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SYSUTCDATETIME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSYSUTCDATETIME(SqlServerParser.SYSUTCDATETIMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SYSUTCDATETIME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSYSUTCDATETIME(SqlServerParser.SYSUTCDATETIMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TIMEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterTIMEFROMPARTS(SqlServerParser.TIMEFROMPARTSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TIMEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitTIMEFROMPARTS(SqlServerParser.TIMEFROMPARTSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TODATETIMEOFFSET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterTODATETIMEOFFSET(SqlServerParser.TODATETIMEOFFSETContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TODATETIMEOFFSET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitTODATETIMEOFFSET(SqlServerParser.TODATETIMEOFFSETContext ctx);
	/**
	 * Enter a parse tree produced by the {@code YEAR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterYEAR(SqlServerParser.YEARContext ctx);
	/**
	 * Exit a parse tree produced by the {@code YEAR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitYEAR(SqlServerParser.YEARContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NULLIF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterNULLIF(SqlServerParser.NULLIFContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NULLIF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitNULLIF(SqlServerParser.NULLIFContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PARSE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterPARSE(SqlServerParser.PARSEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PARSE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitPARSE(SqlServerParser.PARSEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code XML_DATA_TYPE_FUNC}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterXML_DATA_TYPE_FUNC(SqlServerParser.XML_DATA_TYPE_FUNCContext ctx);
	/**
	 * Exit a parse tree produced by the {@code XML_DATA_TYPE_FUNC}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitXML_DATA_TYPE_FUNC(SqlServerParser.XML_DATA_TYPE_FUNCContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IIF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterIIF(SqlServerParser.IIFContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IIF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitIIF(SqlServerParser.IIFContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ISJSON}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterISJSON(SqlServerParser.ISJSONContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ISJSON}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitISJSON(SqlServerParser.ISJSONContext ctx);
	/**
	 * Enter a parse tree produced by the {@code JSON_OBJECT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterJSON_OBJECT(SqlServerParser.JSON_OBJECTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code JSON_OBJECT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitJSON_OBJECT(SqlServerParser.JSON_OBJECTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code JSON_ARRAY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterJSON_ARRAY(SqlServerParser.JSON_ARRAYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code JSON_ARRAY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitJSON_ARRAY(SqlServerParser.JSON_ARRAYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code JSON_VALUE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterJSON_VALUE(SqlServerParser.JSON_VALUEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code JSON_VALUE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitJSON_VALUE(SqlServerParser.JSON_VALUEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code JSON_QUERY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterJSON_QUERY(SqlServerParser.JSON_QUERYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code JSON_QUERY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitJSON_QUERY(SqlServerParser.JSON_QUERYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code JSON_MODIFY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterJSON_MODIFY(SqlServerParser.JSON_MODIFYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code JSON_MODIFY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitJSON_MODIFY(SqlServerParser.JSON_MODIFYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code JSON_PATH_EXISTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterJSON_PATH_EXISTS(SqlServerParser.JSON_PATH_EXISTSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code JSON_PATH_EXISTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitJSON_PATH_EXISTS(SqlServerParser.JSON_PATH_EXISTSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ABS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterABS(SqlServerParser.ABSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ABS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitABS(SqlServerParser.ABSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ACOS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterACOS(SqlServerParser.ACOSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ACOS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitACOS(SqlServerParser.ACOSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ASIN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterASIN(SqlServerParser.ASINContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ASIN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitASIN(SqlServerParser.ASINContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ATAN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterATAN(SqlServerParser.ATANContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ATAN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitATAN(SqlServerParser.ATANContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ATN2}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterATN2(SqlServerParser.ATN2Context ctx);
	/**
	 * Exit a parse tree produced by the {@code ATN2}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitATN2(SqlServerParser.ATN2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code CEILING}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCEILING(SqlServerParser.CEILINGContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CEILING}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCEILING(SqlServerParser.CEILINGContext ctx);
	/**
	 * Enter a parse tree produced by the {@code COS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCOS(SqlServerParser.COSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code COS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCOS(SqlServerParser.COSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code COT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCOT(SqlServerParser.COTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code COT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCOT(SqlServerParser.COTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DEGREES}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDEGREES(SqlServerParser.DEGREESContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DEGREES}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDEGREES(SqlServerParser.DEGREESContext ctx);
	/**
	 * Enter a parse tree produced by the {@code EXP}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterEXP(SqlServerParser.EXPContext ctx);
	/**
	 * Exit a parse tree produced by the {@code EXP}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitEXP(SqlServerParser.EXPContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FLOOR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterFLOOR(SqlServerParser.FLOORContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FLOOR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitFLOOR(SqlServerParser.FLOORContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LOG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterLOG(SqlServerParser.LOGContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LOG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitLOG(SqlServerParser.LOGContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LOG10}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterLOG10(SqlServerParser.LOG10Context ctx);
	/**
	 * Exit a parse tree produced by the {@code LOG10}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitLOG10(SqlServerParser.LOG10Context ctx);
	/**
	 * Enter a parse tree produced by the {@code PI}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterPI(SqlServerParser.PIContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PI}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitPI(SqlServerParser.PIContext ctx);
	/**
	 * Enter a parse tree produced by the {@code POWER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterPOWER(SqlServerParser.POWERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code POWER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitPOWER(SqlServerParser.POWERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RADIANS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterRADIANS(SqlServerParser.RADIANSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RADIANS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitRADIANS(SqlServerParser.RADIANSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RAND}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterRAND(SqlServerParser.RANDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RAND}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitRAND(SqlServerParser.RANDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ROUND}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterROUND(SqlServerParser.ROUNDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ROUND}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitROUND(SqlServerParser.ROUNDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MATH_SIGN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterMATH_SIGN(SqlServerParser.MATH_SIGNContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MATH_SIGN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitMATH_SIGN(SqlServerParser.MATH_SIGNContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SIN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSIN(SqlServerParser.SINContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SIN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSIN(SqlServerParser.SINContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SQRT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSQRT(SqlServerParser.SQRTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SQRT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSQRT(SqlServerParser.SQRTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SQUARE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSQUARE(SqlServerParser.SQUAREContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SQUARE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSQUARE(SqlServerParser.SQUAREContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TAN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterTAN(SqlServerParser.TANContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TAN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitTAN(SqlServerParser.TANContext ctx);
	/**
	 * Enter a parse tree produced by the {@code GREATEST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterGREATEST(SqlServerParser.GREATESTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code GREATEST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitGREATEST(SqlServerParser.GREATESTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LEAST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterLEAST(SqlServerParser.LEASTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LEAST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitLEAST(SqlServerParser.LEASTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CERTENCODED}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCERTENCODED(SqlServerParser.CERTENCODEDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CERTENCODED}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCERTENCODED(SqlServerParser.CERTENCODEDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CERTPRIVATEKEY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCERTPRIVATEKEY(SqlServerParser.CERTPRIVATEKEYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CERTPRIVATEKEY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCERTPRIVATEKEY(SqlServerParser.CERTPRIVATEKEYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CURRENT_USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterCURRENT_USER(SqlServerParser.CURRENT_USERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CURRENT_USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitCURRENT_USER(SqlServerParser.CURRENT_USERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DATABASE_PRINCIPAL_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterDATABASE_PRINCIPAL_ID(SqlServerParser.DATABASE_PRINCIPAL_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DATABASE_PRINCIPAL_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitDATABASE_PRINCIPAL_ID(SqlServerParser.DATABASE_PRINCIPAL_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code HAS_DBACCESS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterHAS_DBACCESS(SqlServerParser.HAS_DBACCESSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code HAS_DBACCESS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitHAS_DBACCESS(SqlServerParser.HAS_DBACCESSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code HAS_PERMS_BY_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterHAS_PERMS_BY_NAME(SqlServerParser.HAS_PERMS_BY_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code HAS_PERMS_BY_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitHAS_PERMS_BY_NAME(SqlServerParser.HAS_PERMS_BY_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IS_MEMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterIS_MEMBER(SqlServerParser.IS_MEMBERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IS_MEMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitIS_MEMBER(SqlServerParser.IS_MEMBERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IS_ROLEMEMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterIS_ROLEMEMBER(SqlServerParser.IS_ROLEMEMBERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IS_ROLEMEMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitIS_ROLEMEMBER(SqlServerParser.IS_ROLEMEMBERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IS_SRVROLEMEMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterIS_SRVROLEMEMBER(SqlServerParser.IS_SRVROLEMEMBERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IS_SRVROLEMEMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitIS_SRVROLEMEMBER(SqlServerParser.IS_SRVROLEMEMBERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LOGINPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterLOGINPROPERTY(SqlServerParser.LOGINPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LOGINPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitLOGINPROPERTY(SqlServerParser.LOGINPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ORIGINAL_LOGIN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterORIGINAL_LOGIN(SqlServerParser.ORIGINAL_LOGINContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ORIGINAL_LOGIN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitORIGINAL_LOGIN(SqlServerParser.ORIGINAL_LOGINContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PERMISSIONS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterPERMISSIONS(SqlServerParser.PERMISSIONSContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PERMISSIONS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitPERMISSIONS(SqlServerParser.PERMISSIONSContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PWDENCRYPT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterPWDENCRYPT(SqlServerParser.PWDENCRYPTContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PWDENCRYPT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitPWDENCRYPT(SqlServerParser.PWDENCRYPTContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PWDCOMPARE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterPWDCOMPARE(SqlServerParser.PWDCOMPAREContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PWDCOMPARE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitPWDCOMPARE(SqlServerParser.PWDCOMPAREContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SESSION_USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSESSION_USER(SqlServerParser.SESSION_USERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SESSION_USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSESSION_USER(SqlServerParser.SESSION_USERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SESSIONPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSESSIONPROPERTY(SqlServerParser.SESSIONPROPERTYContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SESSIONPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSESSIONPROPERTY(SqlServerParser.SESSIONPROPERTYContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SUSER_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSUSER_ID(SqlServerParser.SUSER_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SUSER_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSUSER_ID(SqlServerParser.SUSER_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SUSER_SNAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSUSER_SNAME(SqlServerParser.SUSER_SNAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SUSER_SNAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSUSER_SNAME(SqlServerParser.SUSER_SNAMEContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SUSER_SID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSUSER_SID(SqlServerParser.SUSER_SIDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SUSER_SID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSUSER_SID(SqlServerParser.SUSER_SIDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SYSTEM_USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterSYSTEM_USER(SqlServerParser.SYSTEM_USERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SYSTEM_USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitSYSTEM_USER(SqlServerParser.SYSTEM_USERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterUSER(SqlServerParser.USERContext ctx);
	/**
	 * Exit a parse tree produced by the {@code USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitUSER(SqlServerParser.USERContext ctx);
	/**
	 * Enter a parse tree produced by the {@code USER_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterUSER_ID(SqlServerParser.USER_IDContext ctx);
	/**
	 * Exit a parse tree produced by the {@code USER_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitUSER_ID(SqlServerParser.USER_IDContext ctx);
	/**
	 * Enter a parse tree produced by the {@code USER_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void enterUSER_NAME(SqlServerParser.USER_NAMEContext ctx);
	/**
	 * Exit a parse tree produced by the {@code USER_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 */
	void exitUSER_NAME(SqlServerParser.USER_NAMEContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#xml_data_type_methods}.
	 * @param ctx the parse tree
	 */
	void enterXml_data_type_methods(SqlServerParser.Xml_data_type_methodsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#xml_data_type_methods}.
	 * @param ctx the parse tree
	 */
	void exitXml_data_type_methods(SqlServerParser.Xml_data_type_methodsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dateparts_9}.
	 * @param ctx the parse tree
	 */
	void enterDateparts_9(SqlServerParser.Dateparts_9Context ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dateparts_9}.
	 * @param ctx the parse tree
	 */
	void exitDateparts_9(SqlServerParser.Dateparts_9Context ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dateparts_12}.
	 * @param ctx the parse tree
	 */
	void enterDateparts_12(SqlServerParser.Dateparts_12Context ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dateparts_12}.
	 * @param ctx the parse tree
	 */
	void exitDateparts_12(SqlServerParser.Dateparts_12Context ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dateparts_15}.
	 * @param ctx the parse tree
	 */
	void enterDateparts_15(SqlServerParser.Dateparts_15Context ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dateparts_15}.
	 * @param ctx the parse tree
	 */
	void exitDateparts_15(SqlServerParser.Dateparts_15Context ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#dateparts_datetrunc}.
	 * @param ctx the parse tree
	 */
	void enterDateparts_datetrunc(SqlServerParser.Dateparts_datetruncContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#dateparts_datetrunc}.
	 * @param ctx the parse tree
	 */
	void exitDateparts_datetrunc(SqlServerParser.Dateparts_datetruncContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#value_method}.
	 * @param ctx the parse tree
	 */
	void enterValue_method(SqlServerParser.Value_methodContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#value_method}.
	 * @param ctx the parse tree
	 */
	void exitValue_method(SqlServerParser.Value_methodContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#value_call}.
	 * @param ctx the parse tree
	 */
	void enterValue_call(SqlServerParser.Value_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#value_call}.
	 * @param ctx the parse tree
	 */
	void exitValue_call(SqlServerParser.Value_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#query_method}.
	 * @param ctx the parse tree
	 */
	void enterQuery_method(SqlServerParser.Query_methodContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#query_method}.
	 * @param ctx the parse tree
	 */
	void exitQuery_method(SqlServerParser.Query_methodContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#query_call}.
	 * @param ctx the parse tree
	 */
	void enterQuery_call(SqlServerParser.Query_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#query_call}.
	 * @param ctx the parse tree
	 */
	void exitQuery_call(SqlServerParser.Query_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#exist_method}.
	 * @param ctx the parse tree
	 */
	void enterExist_method(SqlServerParser.Exist_methodContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#exist_method}.
	 * @param ctx the parse tree
	 */
	void exitExist_method(SqlServerParser.Exist_methodContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#exist_call}.
	 * @param ctx the parse tree
	 */
	void enterExist_call(SqlServerParser.Exist_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#exist_call}.
	 * @param ctx the parse tree
	 */
	void exitExist_call(SqlServerParser.Exist_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#modify_method}.
	 * @param ctx the parse tree
	 */
	void enterModify_method(SqlServerParser.Modify_methodContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#modify_method}.
	 * @param ctx the parse tree
	 */
	void exitModify_method(SqlServerParser.Modify_methodContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#modify_call}.
	 * @param ctx the parse tree
	 */
	void enterModify_call(SqlServerParser.Modify_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#modify_call}.
	 * @param ctx the parse tree
	 */
	void exitModify_call(SqlServerParser.Modify_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#hierarchyid_call}.
	 * @param ctx the parse tree
	 */
	void enterHierarchyid_call(SqlServerParser.Hierarchyid_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#hierarchyid_call}.
	 * @param ctx the parse tree
	 */
	void exitHierarchyid_call(SqlServerParser.Hierarchyid_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#hierarchyid_static_method}.
	 * @param ctx the parse tree
	 */
	void enterHierarchyid_static_method(SqlServerParser.Hierarchyid_static_methodContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#hierarchyid_static_method}.
	 * @param ctx the parse tree
	 */
	void exitHierarchyid_static_method(SqlServerParser.Hierarchyid_static_methodContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#nodes_method}.
	 * @param ctx the parse tree
	 */
	void enterNodes_method(SqlServerParser.Nodes_methodContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#nodes_method}.
	 * @param ctx the parse tree
	 */
	void exitNodes_method(SqlServerParser.Nodes_methodContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#switch_section}.
	 * @param ctx the parse tree
	 */
	void enterSwitch_section(SqlServerParser.Switch_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#switch_section}.
	 * @param ctx the parse tree
	 */
	void exitSwitch_section(SqlServerParser.Switch_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#switch_search_condition_section}.
	 * @param ctx the parse tree
	 */
	void enterSwitch_search_condition_section(SqlServerParser.Switch_search_condition_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#switch_search_condition_section}.
	 * @param ctx the parse tree
	 */
	void exitSwitch_search_condition_section(SqlServerParser.Switch_search_condition_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#as_column_alias}.
	 * @param ctx the parse tree
	 */
	void enterAs_column_alias(SqlServerParser.As_column_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#as_column_alias}.
	 * @param ctx the parse tree
	 */
	void exitAs_column_alias(SqlServerParser.As_column_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#as_table_alias}.
	 * @param ctx the parse tree
	 */
	void enterAs_table_alias(SqlServerParser.As_table_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#as_table_alias}.
	 * @param ctx the parse tree
	 */
	void exitAs_table_alias(SqlServerParser.As_table_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_alias}.
	 * @param ctx the parse tree
	 */
	void enterTable_alias(SqlServerParser.Table_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_alias}.
	 * @param ctx the parse tree
	 */
	void exitTable_alias(SqlServerParser.Table_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#with_table_hints}.
	 * @param ctx the parse tree
	 */
	void enterWith_table_hints(SqlServerParser.With_table_hintsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#with_table_hints}.
	 * @param ctx the parse tree
	 */
	void exitWith_table_hints(SqlServerParser.With_table_hintsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#deprecated_table_hint}.
	 * @param ctx the parse tree
	 */
	void enterDeprecated_table_hint(SqlServerParser.Deprecated_table_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#deprecated_table_hint}.
	 * @param ctx the parse tree
	 */
	void exitDeprecated_table_hint(SqlServerParser.Deprecated_table_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#sybase_legacy_hints}.
	 * @param ctx the parse tree
	 */
	void enterSybase_legacy_hints(SqlServerParser.Sybase_legacy_hintsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#sybase_legacy_hints}.
	 * @param ctx the parse tree
	 */
	void exitSybase_legacy_hints(SqlServerParser.Sybase_legacy_hintsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#sybase_legacy_hint}.
	 * @param ctx the parse tree
	 */
	void enterSybase_legacy_hint(SqlServerParser.Sybase_legacy_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#sybase_legacy_hint}.
	 * @param ctx the parse tree
	 */
	void exitSybase_legacy_hint(SqlServerParser.Sybase_legacy_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_hint}.
	 * @param ctx the parse tree
	 */
	void enterTable_hint(SqlServerParser.Table_hintContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_hint}.
	 * @param ctx the parse tree
	 */
	void exitTable_hint(SqlServerParser.Table_hintContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#index_value}.
	 * @param ctx the parse tree
	 */
	void enterIndex_value(SqlServerParser.Index_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#index_value}.
	 * @param ctx the parse tree
	 */
	void exitIndex_value(SqlServerParser.Index_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_alias_list}.
	 * @param ctx the parse tree
	 */
	void enterColumn_alias_list(SqlServerParser.Column_alias_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_alias_list}.
	 * @param ctx the parse tree
	 */
	void exitColumn_alias_list(SqlServerParser.Column_alias_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_alias}.
	 * @param ctx the parse tree
	 */
	void enterColumn_alias(SqlServerParser.Column_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_alias}.
	 * @param ctx the parse tree
	 */
	void exitColumn_alias(SqlServerParser.Column_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_value_constructor}.
	 * @param ctx the parse tree
	 */
	void enterTable_value_constructor(SqlServerParser.Table_value_constructorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_value_constructor}.
	 * @param ctx the parse tree
	 */
	void exitTable_value_constructor(SqlServerParser.Table_value_constructorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#expression_list_}.
	 * @param ctx the parse tree
	 */
	void enterExpression_list_(SqlServerParser.Expression_list_Context ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#expression_list_}.
	 * @param ctx the parse tree
	 */
	void exitExpression_list_(SqlServerParser.Expression_list_Context ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#ranking_windowed_function}.
	 * @param ctx the parse tree
	 */
	void enterRanking_windowed_function(SqlServerParser.Ranking_windowed_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#ranking_windowed_function}.
	 * @param ctx the parse tree
	 */
	void exitRanking_windowed_function(SqlServerParser.Ranking_windowed_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#aggregate_windowed_function}.
	 * @param ctx the parse tree
	 */
	void enterAggregate_windowed_function(SqlServerParser.Aggregate_windowed_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#aggregate_windowed_function}.
	 * @param ctx the parse tree
	 */
	void exitAggregate_windowed_function(SqlServerParser.Aggregate_windowed_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#analytic_windowed_function}.
	 * @param ctx the parse tree
	 */
	void enterAnalytic_windowed_function(SqlServerParser.Analytic_windowed_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#analytic_windowed_function}.
	 * @param ctx the parse tree
	 */
	void exitAnalytic_windowed_function(SqlServerParser.Analytic_windowed_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#all_distinct_expression}.
	 * @param ctx the parse tree
	 */
	void enterAll_distinct_expression(SqlServerParser.All_distinct_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#all_distinct_expression}.
	 * @param ctx the parse tree
	 */
	void exitAll_distinct_expression(SqlServerParser.All_distinct_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#over_clause}.
	 * @param ctx the parse tree
	 */
	void enterOver_clause(SqlServerParser.Over_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#over_clause}.
	 * @param ctx the parse tree
	 */
	void exitOver_clause(SqlServerParser.Over_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#row_or_range_clause}.
	 * @param ctx the parse tree
	 */
	void enterRow_or_range_clause(SqlServerParser.Row_or_range_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#row_or_range_clause}.
	 * @param ctx the parse tree
	 */
	void exitRow_or_range_clause(SqlServerParser.Row_or_range_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#window_frame_extent}.
	 * @param ctx the parse tree
	 */
	void enterWindow_frame_extent(SqlServerParser.Window_frame_extentContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#window_frame_extent}.
	 * @param ctx the parse tree
	 */
	void exitWindow_frame_extent(SqlServerParser.Window_frame_extentContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#window_frame_bound}.
	 * @param ctx the parse tree
	 */
	void enterWindow_frame_bound(SqlServerParser.Window_frame_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#window_frame_bound}.
	 * @param ctx the parse tree
	 */
	void exitWindow_frame_bound(SqlServerParser.Window_frame_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#window_frame_preceding}.
	 * @param ctx the parse tree
	 */
	void enterWindow_frame_preceding(SqlServerParser.Window_frame_precedingContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#window_frame_preceding}.
	 * @param ctx the parse tree
	 */
	void exitWindow_frame_preceding(SqlServerParser.Window_frame_precedingContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#window_frame_following}.
	 * @param ctx the parse tree
	 */
	void enterWindow_frame_following(SqlServerParser.Window_frame_followingContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#window_frame_following}.
	 * @param ctx the parse tree
	 */
	void exitWindow_frame_following(SqlServerParser.Window_frame_followingContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#create_database_option}.
	 * @param ctx the parse tree
	 */
	void enterCreate_database_option(SqlServerParser.Create_database_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#create_database_option}.
	 * @param ctx the parse tree
	 */
	void exitCreate_database_option(SqlServerParser.Create_database_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#database_filestream_option}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_filestream_option(SqlServerParser.Database_filestream_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#database_filestream_option}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_filestream_option(SqlServerParser.Database_filestream_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#database_file_spec}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_file_spec(SqlServerParser.Database_file_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#database_file_spec}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_file_spec(SqlServerParser.Database_file_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#file_group}.
	 * @param ctx the parse tree
	 */
	void enterFile_group(SqlServerParser.File_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#file_group}.
	 * @param ctx the parse tree
	 */
	void exitFile_group(SqlServerParser.File_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#file_spec}.
	 * @param ctx the parse tree
	 */
	void enterFile_spec(SqlServerParser.File_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#file_spec}.
	 * @param ctx the parse tree
	 */
	void exitFile_spec(SqlServerParser.File_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#entity_name}.
	 * @param ctx the parse tree
	 */
	void enterEntity_name(SqlServerParser.Entity_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#entity_name}.
	 * @param ctx the parse tree
	 */
	void exitEntity_name(SqlServerParser.Entity_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#entity_name_for_azure_dw}.
	 * @param ctx the parse tree
	 */
	void enterEntity_name_for_azure_dw(SqlServerParser.Entity_name_for_azure_dwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#entity_name_for_azure_dw}.
	 * @param ctx the parse tree
	 */
	void exitEntity_name_for_azure_dw(SqlServerParser.Entity_name_for_azure_dwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#entity_name_for_parallel_dw}.
	 * @param ctx the parse tree
	 */
	void enterEntity_name_for_parallel_dw(SqlServerParser.Entity_name_for_parallel_dwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#entity_name_for_parallel_dw}.
	 * @param ctx the parse tree
	 */
	void exitEntity_name_for_parallel_dw(SqlServerParser.Entity_name_for_parallel_dwContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#full_table_name}.
	 * @param ctx the parse tree
	 */
	void enterFull_table_name(SqlServerParser.Full_table_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#full_table_name}.
	 * @param ctx the parse tree
	 */
	void exitFull_table_name(SqlServerParser.Full_table_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#table_name}.
	 * @param ctx the parse tree
	 */
	void enterTable_name(SqlServerParser.Table_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#table_name}.
	 * @param ctx the parse tree
	 */
	void exitTable_name(SqlServerParser.Table_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#simple_name}.
	 * @param ctx the parse tree
	 */
	void enterSimple_name(SqlServerParser.Simple_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#simple_name}.
	 * @param ctx the parse tree
	 */
	void exitSimple_name(SqlServerParser.Simple_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#func_proc_name_schema}.
	 * @param ctx the parse tree
	 */
	void enterFunc_proc_name_schema(SqlServerParser.Func_proc_name_schemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#func_proc_name_schema}.
	 * @param ctx the parse tree
	 */
	void exitFunc_proc_name_schema(SqlServerParser.Func_proc_name_schemaContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#func_proc_name_database_schema}.
	 * @param ctx the parse tree
	 */
	void enterFunc_proc_name_database_schema(SqlServerParser.Func_proc_name_database_schemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#func_proc_name_database_schema}.
	 * @param ctx the parse tree
	 */
	void exitFunc_proc_name_database_schema(SqlServerParser.Func_proc_name_database_schemaContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#func_proc_name_server_database_schema}.
	 * @param ctx the parse tree
	 */
	void enterFunc_proc_name_server_database_schema(SqlServerParser.Func_proc_name_server_database_schemaContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#func_proc_name_server_database_schema}.
	 * @param ctx the parse tree
	 */
	void exitFunc_proc_name_server_database_schema(SqlServerParser.Func_proc_name_server_database_schemaContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#ddl_object}.
	 * @param ctx the parse tree
	 */
	void enterDdl_object(SqlServerParser.Ddl_objectContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#ddl_object}.
	 * @param ctx the parse tree
	 */
	void exitDdl_object(SqlServerParser.Ddl_objectContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#full_column_name}.
	 * @param ctx the parse tree
	 */
	void enterFull_column_name(SqlServerParser.Full_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#full_column_name}.
	 * @param ctx the parse tree
	 */
	void exitFull_column_name(SqlServerParser.Full_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_name_list_with_order}.
	 * @param ctx the parse tree
	 */
	void enterColumn_name_list_with_order(SqlServerParser.Column_name_list_with_orderContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_name_list_with_order}.
	 * @param ctx the parse tree
	 */
	void exitColumn_name_list_with_order(SqlServerParser.Column_name_list_with_orderContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#insert_column_name_list}.
	 * @param ctx the parse tree
	 */
	void enterInsert_column_name_list(SqlServerParser.Insert_column_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#insert_column_name_list}.
	 * @param ctx the parse tree
	 */
	void exitInsert_column_name_list(SqlServerParser.Insert_column_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#insert_column_id}.
	 * @param ctx the parse tree
	 */
	void enterInsert_column_id(SqlServerParser.Insert_column_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#insert_column_id}.
	 * @param ctx the parse tree
	 */
	void exitInsert_column_id(SqlServerParser.Insert_column_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#column_name_list}.
	 * @param ctx the parse tree
	 */
	void enterColumn_name_list(SqlServerParser.Column_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#column_name_list}.
	 * @param ctx the parse tree
	 */
	void exitColumn_name_list(SqlServerParser.Column_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#cursor_name}.
	 * @param ctx the parse tree
	 */
	void enterCursor_name(SqlServerParser.Cursor_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#cursor_name}.
	 * @param ctx the parse tree
	 */
	void exitCursor_name(SqlServerParser.Cursor_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#on_off}.
	 * @param ctx the parse tree
	 */
	void enterOn_off(SqlServerParser.On_offContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#on_off}.
	 * @param ctx the parse tree
	 */
	void exitOn_off(SqlServerParser.On_offContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#clustered}.
	 * @param ctx the parse tree
	 */
	void enterClustered(SqlServerParser.ClusteredContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#clustered}.
	 * @param ctx the parse tree
	 */
	void exitClustered(SqlServerParser.ClusteredContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#null_notnull}.
	 * @param ctx the parse tree
	 */
	void enterNull_notnull(SqlServerParser.Null_notnullContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#null_notnull}.
	 * @param ctx the parse tree
	 */
	void exitNull_notnull(SqlServerParser.Null_notnullContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#scalar_function_name}.
	 * @param ctx the parse tree
	 */
	void enterScalar_function_name(SqlServerParser.Scalar_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#scalar_function_name}.
	 * @param ctx the parse tree
	 */
	void exitScalar_function_name(SqlServerParser.Scalar_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#begin_conversation_timer}.
	 * @param ctx the parse tree
	 */
	void enterBegin_conversation_timer(SqlServerParser.Begin_conversation_timerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#begin_conversation_timer}.
	 * @param ctx the parse tree
	 */
	void exitBegin_conversation_timer(SqlServerParser.Begin_conversation_timerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#begin_conversation_dialog}.
	 * @param ctx the parse tree
	 */
	void enterBegin_conversation_dialog(SqlServerParser.Begin_conversation_dialogContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#begin_conversation_dialog}.
	 * @param ctx the parse tree
	 */
	void exitBegin_conversation_dialog(SqlServerParser.Begin_conversation_dialogContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#contract_name}.
	 * @param ctx the parse tree
	 */
	void enterContract_name(SqlServerParser.Contract_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#contract_name}.
	 * @param ctx the parse tree
	 */
	void exitContract_name(SqlServerParser.Contract_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#service_name}.
	 * @param ctx the parse tree
	 */
	void enterService_name(SqlServerParser.Service_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#service_name}.
	 * @param ctx the parse tree
	 */
	void exitService_name(SqlServerParser.Service_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#end_conversation}.
	 * @param ctx the parse tree
	 */
	void enterEnd_conversation(SqlServerParser.End_conversationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#end_conversation}.
	 * @param ctx the parse tree
	 */
	void exitEnd_conversation(SqlServerParser.End_conversationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#waitfor_conversation}.
	 * @param ctx the parse tree
	 */
	void enterWaitfor_conversation(SqlServerParser.Waitfor_conversationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#waitfor_conversation}.
	 * @param ctx the parse tree
	 */
	void exitWaitfor_conversation(SqlServerParser.Waitfor_conversationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#get_conversation}.
	 * @param ctx the parse tree
	 */
	void enterGet_conversation(SqlServerParser.Get_conversationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#get_conversation}.
	 * @param ctx the parse tree
	 */
	void exitGet_conversation(SqlServerParser.Get_conversationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#queue_id}.
	 * @param ctx the parse tree
	 */
	void enterQueue_id(SqlServerParser.Queue_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#queue_id}.
	 * @param ctx the parse tree
	 */
	void exitQueue_id(SqlServerParser.Queue_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#send_conversation}.
	 * @param ctx the parse tree
	 */
	void enterSend_conversation(SqlServerParser.Send_conversationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#send_conversation}.
	 * @param ctx the parse tree
	 */
	void exitSend_conversation(SqlServerParser.Send_conversationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#data_type}.
	 * @param ctx the parse tree
	 */
	void enterData_type(SqlServerParser.Data_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#data_type}.
	 * @param ctx the parse tree
	 */
	void exitData_type(SqlServerParser.Data_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(SqlServerParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(SqlServerParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#primitive_constant}.
	 * @param ctx the parse tree
	 */
	void enterPrimitive_constant(SqlServerParser.Primitive_constantContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#primitive_constant}.
	 * @param ctx the parse tree
	 */
	void exitPrimitive_constant(SqlServerParser.Primitive_constantContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#keyword}.
	 * @param ctx the parse tree
	 */
	void enterKeyword(SqlServerParser.KeywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#keyword}.
	 * @param ctx the parse tree
	 */
	void exitKeyword(SqlServerParser.KeywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#id_}.
	 * @param ctx the parse tree
	 */
	void enterId_(SqlServerParser.Id_Context ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#id_}.
	 * @param ctx the parse tree
	 */
	void exitId_(SqlServerParser.Id_Context ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#simple_id}.
	 * @param ctx the parse tree
	 */
	void enterSimple_id(SqlServerParser.Simple_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#simple_id}.
	 * @param ctx the parse tree
	 */
	void exitSimple_id(SqlServerParser.Simple_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#id_or_string}.
	 * @param ctx the parse tree
	 */
	void enterId_or_string(SqlServerParser.Id_or_stringContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#id_or_string}.
	 * @param ctx the parse tree
	 */
	void exitId_or_string(SqlServerParser.Id_or_stringContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#comparison_operator}.
	 * @param ctx the parse tree
	 */
	void enterComparison_operator(SqlServerParser.Comparison_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#comparison_operator}.
	 * @param ctx the parse tree
	 */
	void exitComparison_operator(SqlServerParser.Comparison_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#assignment_operator}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_operator(SqlServerParser.Assignment_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#assignment_operator}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_operator(SqlServerParser.Assignment_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SqlServerParser#file_size}.
	 * @param ctx the parse tree
	 */
	void enterFile_size(SqlServerParser.File_sizeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SqlServerParser#file_size}.
	 * @param ctx the parse tree
	 */
	void exitFile_size(SqlServerParser.File_sizeContext ctx);
}