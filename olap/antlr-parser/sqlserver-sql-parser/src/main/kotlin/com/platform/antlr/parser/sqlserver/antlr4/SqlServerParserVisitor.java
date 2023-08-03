// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.sqlserver.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SqlServerParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SqlServerParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#tsql_file}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTsql_file(SqlServerParser.Tsql_fileContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#batch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBatch(SqlServerParser.BatchContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#batch_level_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBatch_level_statement(SqlServerParser.Batch_level_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#sql_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_clauses(SqlServerParser.Sql_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dml_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_clause(SqlServerParser.Dml_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#ddl_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDdl_clause(SqlServerParser.Ddl_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#backup_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBackup_statement(SqlServerParser.Backup_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#cfl_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCfl_statement(SqlServerParser.Cfl_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#block_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock_statement(SqlServerParser.Block_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#break_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreak_statement(SqlServerParser.Break_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#continue_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinue_statement(SqlServerParser.Continue_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#goto_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGoto_statement(SqlServerParser.Goto_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#return_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_statement(SqlServerParser.Return_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#if_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_statement(SqlServerParser.If_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#throw_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrow_statement(SqlServerParser.Throw_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#throw_error_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrow_error_number(SqlServerParser.Throw_error_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#throw_message}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrow_message(SqlServerParser.Throw_messageContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#throw_state}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrow_state(SqlServerParser.Throw_stateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#try_catch_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTry_catch_statement(SqlServerParser.Try_catch_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#waitfor_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWaitfor_statement(SqlServerParser.Waitfor_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#while_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile_statement(SqlServerParser.While_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#print_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrint_statement(SqlServerParser.Print_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#raiseerror_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRaiseerror_statement(SqlServerParser.Raiseerror_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#empty_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmpty_statement(SqlServerParser.Empty_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#another_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnother_statement(SqlServerParser.Another_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_application_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_application_role(SqlServerParser.Alter_application_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_xml_schema_collection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_xml_schema_collection(SqlServerParser.Alter_xml_schema_collectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_application_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_application_role(SqlServerParser.Create_application_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_aggregate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_aggregate(SqlServerParser.Drop_aggregateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_application_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_application_role(SqlServerParser.Drop_application_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly(SqlServerParser.Alter_assemblyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_start(SqlServerParser.Alter_assembly_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_clause(SqlServerParser.Alter_assembly_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_from_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_from_clause(SqlServerParser.Alter_assembly_from_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_from_clause_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_from_clause_start(SqlServerParser.Alter_assembly_from_clause_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_drop_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_drop_clause(SqlServerParser.Alter_assembly_drop_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_drop_multiple_files}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_drop_multiple_files(SqlServerParser.Alter_assembly_drop_multiple_filesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_drop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_drop(SqlServerParser.Alter_assembly_dropContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_add_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_add_clause(SqlServerParser.Alter_assembly_add_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_asssembly_add_clause_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_asssembly_add_clause_start(SqlServerParser.Alter_asssembly_add_clause_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_client_file_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_client_file_clause(SqlServerParser.Alter_assembly_client_file_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_file_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_file_name(SqlServerParser.Alter_assembly_file_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_file_bits}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_file_bits(SqlServerParser.Alter_assembly_file_bitsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_as}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_as(SqlServerParser.Alter_assembly_asContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_with_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_with_clause(SqlServerParser.Alter_assembly_with_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_assembly_with}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_assembly_with(SqlServerParser.Alter_assembly_withContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#client_assembly_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClient_assembly_specifier(SqlServerParser.Client_assembly_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#assembly_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssembly_option(SqlServerParser.Assembly_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#network_file_share}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNetwork_file_share(SqlServerParser.Network_file_shareContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#network_computer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNetwork_computer(SqlServerParser.Network_computerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#network_file_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNetwork_file_start(SqlServerParser.Network_file_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#file_path}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFile_path(SqlServerParser.File_pathContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#file_directory_path_separator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFile_directory_path_separator(SqlServerParser.File_directory_path_separatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#local_file}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_file(SqlServerParser.Local_fileContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#local_drive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_drive(SqlServerParser.Local_driveContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#multiple_local_files}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiple_local_files(SqlServerParser.Multiple_local_filesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#multiple_local_file_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiple_local_file_start(SqlServerParser.Multiple_local_file_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_assembly}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_assembly(SqlServerParser.Create_assemblyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_assembly}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_assembly(SqlServerParser.Drop_assemblyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_asymmetric_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_asymmetric_key(SqlServerParser.Alter_asymmetric_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_asymmetric_key_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_asymmetric_key_start(SqlServerParser.Alter_asymmetric_key_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#asymmetric_key_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsymmetric_key_option(SqlServerParser.Asymmetric_key_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#asymmetric_key_option_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsymmetric_key_option_start(SqlServerParser.Asymmetric_key_option_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#asymmetric_key_password_change_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsymmetric_key_password_change_option(SqlServerParser.Asymmetric_key_password_change_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_asymmetric_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_asymmetric_key(SqlServerParser.Create_asymmetric_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_asymmetric_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_asymmetric_key(SqlServerParser.Drop_asymmetric_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_authorization}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_authorization(SqlServerParser.Alter_authorizationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#authorization_grantee}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuthorization_grantee(SqlServerParser.Authorization_granteeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#entity_to}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntity_to(SqlServerParser.Entity_toContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#colon_colon}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColon_colon(SqlServerParser.Colon_colonContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_authorization_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_authorization_start(SqlServerParser.Alter_authorization_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_authorization_for_sql_database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_authorization_for_sql_database(SqlServerParser.Alter_authorization_for_sql_databaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_authorization_for_azure_dw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_authorization_for_azure_dw(SqlServerParser.Alter_authorization_for_azure_dwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_authorization_for_parallel_dw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_authorization_for_parallel_dw(SqlServerParser.Alter_authorization_for_parallel_dwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#class_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_type(SqlServerParser.Class_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#class_type_for_sql_database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_type_for_sql_database(SqlServerParser.Class_type_for_sql_databaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#class_type_for_azure_dw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_type_for_azure_dw(SqlServerParser.Class_type_for_azure_dwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#class_type_for_parallel_dw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_type_for_parallel_dw(SqlServerParser.Class_type_for_parallel_dwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#class_type_for_grant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_type_for_grant(SqlServerParser.Class_type_for_grantContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_availability_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_availability_group(SqlServerParser.Drop_availability_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_availability_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_availability_group(SqlServerParser.Alter_availability_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_availability_group_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_availability_group_start(SqlServerParser.Alter_availability_group_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_availability_group_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_availability_group_options(SqlServerParser.Alter_availability_group_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#ip_v4_failover}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIp_v4_failover(SqlServerParser.Ip_v4_failoverContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#ip_v6_failover}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIp_v6_failover(SqlServerParser.Ip_v6_failoverContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_or_alter_broker_priority}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_or_alter_broker_priority(SqlServerParser.Create_or_alter_broker_priorityContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_broker_priority}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_broker_priority(SqlServerParser.Drop_broker_priorityContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_certificate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_certificate(SqlServerParser.Alter_certificateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_column_encryption_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_column_encryption_key(SqlServerParser.Alter_column_encryption_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_column_encryption_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_column_encryption_key(SqlServerParser.Create_column_encryption_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_certificate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_certificate(SqlServerParser.Drop_certificateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_column_encryption_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_column_encryption_key(SqlServerParser.Drop_column_encryption_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_column_master_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_column_master_key(SqlServerParser.Drop_column_master_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_contract}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_contract(SqlServerParser.Drop_contractContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_credential}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_credential(SqlServerParser.Drop_credentialContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_cryptograhic_provider}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_cryptograhic_provider(SqlServerParser.Drop_cryptograhic_providerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_database(SqlServerParser.Drop_databaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_database_audit_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_database_audit_specification(SqlServerParser.Drop_database_audit_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_database_encryption_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_database_encryption_key(SqlServerParser.Drop_database_encryption_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_database_scoped_credential}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_database_scoped_credential(SqlServerParser.Drop_database_scoped_credentialContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_default(SqlServerParser.Drop_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_endpoint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_endpoint(SqlServerParser.Drop_endpointContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_external_data_source}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_external_data_source(SqlServerParser.Drop_external_data_sourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_external_file_format}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_external_file_format(SqlServerParser.Drop_external_file_formatContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_external_library}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_external_library(SqlServerParser.Drop_external_libraryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_external_resource_pool}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_external_resource_pool(SqlServerParser.Drop_external_resource_poolContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_external_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_external_table(SqlServerParser.Drop_external_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_event_notifications}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_event_notifications(SqlServerParser.Drop_event_notificationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_event_session}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_event_session(SqlServerParser.Drop_event_sessionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_fulltext_catalog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_fulltext_catalog(SqlServerParser.Drop_fulltext_catalogContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_fulltext_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_fulltext_index(SqlServerParser.Drop_fulltext_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_fulltext_stoplist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_fulltext_stoplist(SqlServerParser.Drop_fulltext_stoplistContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_login}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_login(SqlServerParser.Drop_loginContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_master_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_master_key(SqlServerParser.Drop_master_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_message_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_message_type(SqlServerParser.Drop_message_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_partition_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_partition_function(SqlServerParser.Drop_partition_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_partition_scheme}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_partition_scheme(SqlServerParser.Drop_partition_schemeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_queue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_queue(SqlServerParser.Drop_queueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_remote_service_binding}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_remote_service_binding(SqlServerParser.Drop_remote_service_bindingContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_resource_pool}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_resource_pool(SqlServerParser.Drop_resource_poolContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_db_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_db_role(SqlServerParser.Drop_db_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_route}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_route(SqlServerParser.Drop_routeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_rule}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_rule(SqlServerParser.Drop_ruleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_schema}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_schema(SqlServerParser.Drop_schemaContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_search_property_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_search_property_list(SqlServerParser.Drop_search_property_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_security_policy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_security_policy(SqlServerParser.Drop_security_policyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_sequence(SqlServerParser.Drop_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_server_audit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_server_audit(SqlServerParser.Drop_server_auditContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_server_audit_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_server_audit_specification(SqlServerParser.Drop_server_audit_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_server_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_server_role(SqlServerParser.Drop_server_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_service}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_service(SqlServerParser.Drop_serviceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_signature}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_signature(SqlServerParser.Drop_signatureContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_statistics_name_azure_dw_and_pdw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_statistics_name_azure_dw_and_pdw(SqlServerParser.Drop_statistics_name_azure_dw_and_pdwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_symmetric_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_symmetric_key(SqlServerParser.Drop_symmetric_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_synonym}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_synonym(SqlServerParser.Drop_synonymContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_user}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_user(SqlServerParser.Drop_userContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_workload_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_workload_group(SqlServerParser.Drop_workload_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_xml_schema_collection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_xml_schema_collection(SqlServerParser.Drop_xml_schema_collectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#disable_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisable_trigger(SqlServerParser.Disable_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#enable_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnable_trigger(SqlServerParser.Enable_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#lock_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLock_table(SqlServerParser.Lock_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#truncate_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTruncate_table(SqlServerParser.Truncate_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_column_master_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_column_master_key(SqlServerParser.Create_column_master_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_credential}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_credential(SqlServerParser.Alter_credentialContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_credential}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_credential(SqlServerParser.Create_credentialContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_cryptographic_provider}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_cryptographic_provider(SqlServerParser.Alter_cryptographic_providerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_cryptographic_provider}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_cryptographic_provider(SqlServerParser.Create_cryptographic_providerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_endpoint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_endpoint(SqlServerParser.Create_endpointContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#endpoint_encryption_alogorithm_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndpoint_encryption_alogorithm_clause(SqlServerParser.Endpoint_encryption_alogorithm_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#endpoint_authentication_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndpoint_authentication_clause(SqlServerParser.Endpoint_authentication_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#endpoint_listener_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndpoint_listener_clause(SqlServerParser.Endpoint_listener_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_event_notification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_event_notification(SqlServerParser.Create_event_notificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_or_alter_event_session}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_or_alter_event_session(SqlServerParser.Create_or_alter_event_sessionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#event_session_predicate_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvent_session_predicate_expression(SqlServerParser.Event_session_predicate_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#event_session_predicate_factor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvent_session_predicate_factor(SqlServerParser.Event_session_predicate_factorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#event_session_predicate_leaf}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvent_session_predicate_leaf(SqlServerParser.Event_session_predicate_leafContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_external_data_source}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_external_data_source(SqlServerParser.Alter_external_data_sourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_external_library}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_external_library(SqlServerParser.Alter_external_libraryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_external_library}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_external_library(SqlServerParser.Create_external_libraryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_external_resource_pool}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_external_resource_pool(SqlServerParser.Alter_external_resource_poolContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_external_resource_pool}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_external_resource_pool(SqlServerParser.Create_external_resource_poolContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_fulltext_catalog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_fulltext_catalog(SqlServerParser.Alter_fulltext_catalogContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_fulltext_catalog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_fulltext_catalog(SqlServerParser.Create_fulltext_catalogContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_fulltext_stoplist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_fulltext_stoplist(SqlServerParser.Alter_fulltext_stoplistContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_fulltext_stoplist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_fulltext_stoplist(SqlServerParser.Create_fulltext_stoplistContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_login_sql_server}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_login_sql_server(SqlServerParser.Alter_login_sql_serverContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_login_sql_server}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_login_sql_server(SqlServerParser.Create_login_sql_serverContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_login_azure_sql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_login_azure_sql(SqlServerParser.Alter_login_azure_sqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_login_azure_sql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_login_azure_sql(SqlServerParser.Create_login_azure_sqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_login_azure_sql_dw_and_pdw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_login_azure_sql_dw_and_pdw(SqlServerParser.Alter_login_azure_sql_dw_and_pdwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_login_pdw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_login_pdw(SqlServerParser.Create_login_pdwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_master_key_sql_server}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_master_key_sql_server(SqlServerParser.Alter_master_key_sql_serverContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_master_key_sql_server}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_master_key_sql_server(SqlServerParser.Create_master_key_sql_serverContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_master_key_azure_sql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_master_key_azure_sql(SqlServerParser.Alter_master_key_azure_sqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_master_key_azure_sql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_master_key_azure_sql(SqlServerParser.Create_master_key_azure_sqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_message_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_message_type(SqlServerParser.Alter_message_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_partition_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_partition_function(SqlServerParser.Alter_partition_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_partition_scheme}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_partition_scheme(SqlServerParser.Alter_partition_schemeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_remote_service_binding}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_remote_service_binding(SqlServerParser.Alter_remote_service_bindingContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_remote_service_binding}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_remote_service_binding(SqlServerParser.Create_remote_service_bindingContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_resource_pool}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_resource_pool(SqlServerParser.Create_resource_poolContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_resource_governor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_resource_governor(SqlServerParser.Alter_resource_governorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_database_audit_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_database_audit_specification(SqlServerParser.Alter_database_audit_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#audit_action_spec_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_action_spec_group(SqlServerParser.Audit_action_spec_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#audit_action_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_action_specification(SqlServerParser.Audit_action_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#action_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAction_specification(SqlServerParser.Action_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#audit_class_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_class_name(SqlServerParser.Audit_class_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#audit_securable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_securable(SqlServerParser.Audit_securableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_db_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_db_role(SqlServerParser.Alter_db_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_database_audit_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_database_audit_specification(SqlServerParser.Create_database_audit_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_db_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_db_role(SqlServerParser.Create_db_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_route}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_route(SqlServerParser.Create_routeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_rule}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_rule(SqlServerParser.Create_ruleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_schema_sql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_schema_sql(SqlServerParser.Alter_schema_sqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_schema}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_schema(SqlServerParser.Create_schemaContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_schema_azure_sql_dw_and_pdw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_schema_azure_sql_dw_and_pdw(SqlServerParser.Create_schema_azure_sql_dw_and_pdwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_schema_azure_sql_dw_and_pdw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_schema_azure_sql_dw_and_pdw(SqlServerParser.Alter_schema_azure_sql_dw_and_pdwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_search_property_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_search_property_list(SqlServerParser.Create_search_property_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_security_policy}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_security_policy(SqlServerParser.Create_security_policyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_sequence(SqlServerParser.Alter_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_sequence(SqlServerParser.Create_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_server_audit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_server_audit(SqlServerParser.Alter_server_auditContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_server_audit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_server_audit(SqlServerParser.Create_server_auditContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_server_audit_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_server_audit_specification(SqlServerParser.Alter_server_audit_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_server_audit_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_server_audit_specification(SqlServerParser.Create_server_audit_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_server_configuration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_server_configuration(SqlServerParser.Alter_server_configurationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_server_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_server_role(SqlServerParser.Alter_server_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_server_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_server_role(SqlServerParser.Create_server_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_server_role_pdw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_server_role_pdw(SqlServerParser.Alter_server_role_pdwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_service}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_service(SqlServerParser.Alter_serviceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#opt_arg_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_arg_clause(SqlServerParser.Opt_arg_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_service}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_service(SqlServerParser.Create_serviceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_service_master_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_service_master_key(SqlServerParser.Alter_service_master_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_symmetric_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_symmetric_key(SqlServerParser.Alter_symmetric_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_synonym}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_synonym(SqlServerParser.Create_synonymContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_user}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_user(SqlServerParser.Alter_userContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_user}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_user(SqlServerParser.Create_userContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_user_azure_sql_dw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_user_azure_sql_dw(SqlServerParser.Create_user_azure_sql_dwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_user_azure_sql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_user_azure_sql(SqlServerParser.Alter_user_azure_sqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_workload_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_workload_group(SqlServerParser.Alter_workload_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_workload_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_workload_group(SqlServerParser.Create_workload_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_xml_schema_collection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_xml_schema_collection(SqlServerParser.Create_xml_schema_collectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_partition_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_partition_function(SqlServerParser.Create_partition_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_partition_scheme}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_partition_scheme(SqlServerParser.Create_partition_schemeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_queue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_queue(SqlServerParser.Create_queueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#queue_settings}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueue_settings(SqlServerParser.Queue_settingsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_queue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_queue(SqlServerParser.Alter_queueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#queue_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueue_action(SqlServerParser.Queue_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#queue_rebuild_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueue_rebuild_options(SqlServerParser.Queue_rebuild_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_contract}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_contract(SqlServerParser.Create_contractContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#conversation_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConversation_statement(SqlServerParser.Conversation_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#message_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMessage_statement(SqlServerParser.Message_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#merge_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_statement(SqlServerParser.Merge_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#when_matches}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhen_matches(SqlServerParser.When_matchesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#merge_matched}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_matched(SqlServerParser.Merge_matchedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#merge_not_matched}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_not_matched(SqlServerParser.Merge_not_matchedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#delete_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_statement(SqlServerParser.Delete_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#delete_statement_from}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_statement_from(SqlServerParser.Delete_statement_fromContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#insert_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_statement(SqlServerParser.Insert_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#insert_statement_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_statement_value(SqlServerParser.Insert_statement_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#receive_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReceive_statement(SqlServerParser.Receive_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#select_statement_standalone}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_statement_standalone(SqlServerParser.Select_statement_standaloneContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#select_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_statement(SqlServerParser.Select_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#time}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTime(SqlServerParser.TimeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#update_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_statement(SqlServerParser.Update_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#output_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutput_clause(SqlServerParser.Output_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#output_dml_list_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutput_dml_list_elem(SqlServerParser.Output_dml_list_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_database(SqlServerParser.Create_databaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index(SqlServerParser.Create_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_index_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index_options(SqlServerParser.Create_index_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#relational_index_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_index_option(SqlServerParser.Relational_index_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_index(SqlServerParser.Alter_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#resumable_index_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResumable_index_options(SqlServerParser.Resumable_index_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#resumable_index_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResumable_index_option(SqlServerParser.Resumable_index_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#reorganize_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReorganize_partition(SqlServerParser.Reorganize_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#reorganize_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReorganize_options(SqlServerParser.Reorganize_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#reorganize_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReorganize_option(SqlServerParser.Reorganize_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#set_index_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_index_options(SqlServerParser.Set_index_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#set_index_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_index_option(SqlServerParser.Set_index_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#rebuild_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRebuild_partition(SqlServerParser.Rebuild_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#rebuild_index_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRebuild_index_options(SqlServerParser.Rebuild_index_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#rebuild_index_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRebuild_index_option(SqlServerParser.Rebuild_index_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#single_partition_rebuild_index_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_partition_rebuild_index_options(SqlServerParser.Single_partition_rebuild_index_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#single_partition_rebuild_index_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_partition_rebuild_index_option(SqlServerParser.Single_partition_rebuild_index_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#on_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_partitions(SqlServerParser.On_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_columnstore_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_columnstore_index(SqlServerParser.Create_columnstore_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_columnstore_index_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_columnstore_index_options(SqlServerParser.Create_columnstore_index_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#columnstore_index_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnstore_index_option(SqlServerParser.Columnstore_index_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_nonclustered_columnstore_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_nonclustered_columnstore_index(SqlServerParser.Create_nonclustered_columnstore_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_xml_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_xml_index(SqlServerParser.Create_xml_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#xml_index_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_index_options(SqlServerParser.Xml_index_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#xml_index_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_index_option(SqlServerParser.Xml_index_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_or_alter_procedure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_or_alter_procedure(SqlServerParser.Create_or_alter_procedureContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#as_external_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAs_external_name(SqlServerParser.As_external_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_or_alter_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_or_alter_trigger(SqlServerParser.Create_or_alter_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_or_alter_dml_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_or_alter_dml_trigger(SqlServerParser.Create_or_alter_dml_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dml_trigger_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_trigger_option(SqlServerParser.Dml_trigger_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dml_trigger_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_trigger_operation(SqlServerParser.Dml_trigger_operationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_or_alter_ddl_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_or_alter_ddl_trigger(SqlServerParser.Create_or_alter_ddl_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#ddl_trigger_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDdl_trigger_operation(SqlServerParser.Ddl_trigger_operationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_or_alter_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_or_alter_function(SqlServerParser.Create_or_alter_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#func_body_returns_select}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_body_returns_select(SqlServerParser.Func_body_returns_selectContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#func_body_returns_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_body_returns_table(SqlServerParser.Func_body_returns_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#func_body_returns_scalar}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_body_returns_scalar(SqlServerParser.Func_body_returns_scalarContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#procedure_param_default_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_param_default_value(SqlServerParser.Procedure_param_default_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#procedure_param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_param(SqlServerParser.Procedure_paramContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#procedure_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_option(SqlServerParser.Procedure_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#function_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_option(SqlServerParser.Function_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_statistics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_statistics(SqlServerParser.Create_statisticsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#update_statistics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_statistics(SqlServerParser.Update_statisticsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#update_statistics_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_statistics_options(SqlServerParser.Update_statistics_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#update_statistics_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_statistics_option(SqlServerParser.Update_statistics_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table(SqlServerParser.Create_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_indices}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_indices(SqlServerParser.Table_indicesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_options(SqlServerParser.Table_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_option(SqlServerParser.Table_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_table_index_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_index_options(SqlServerParser.Create_table_index_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_table_index_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_index_option(SqlServerParser.Create_table_index_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_view(SqlServerParser.Create_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#view_attribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitView_attribute(SqlServerParser.View_attributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table(SqlServerParser.Alter_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#switch_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_partition(SqlServerParser.Switch_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#low_priority_lock_wait}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLow_priority_lock_wait(SqlServerParser.Low_priority_lock_waitContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_database(SqlServerParser.Alter_databaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#add_or_modify_files}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_or_modify_files(SqlServerParser.Add_or_modify_filesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#filespec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilespec(SqlServerParser.FilespecContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#add_or_modify_filegroups}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_or_modify_filegroups(SqlServerParser.Add_or_modify_filegroupsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#filegroup_updatability_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilegroup_updatability_option(SqlServerParser.Filegroup_updatability_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#database_optionspec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatabase_optionspec(SqlServerParser.Database_optionspecContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#auto_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuto_option(SqlServerParser.Auto_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#change_tracking_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChange_tracking_option(SqlServerParser.Change_tracking_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#change_tracking_option_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChange_tracking_option_list(SqlServerParser.Change_tracking_option_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#containment_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainment_option(SqlServerParser.Containment_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#cursor_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_option(SqlServerParser.Cursor_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_endpoint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_endpoint(SqlServerParser.Alter_endpointContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#database_mirroring_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatabase_mirroring_option(SqlServerParser.Database_mirroring_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#mirroring_set_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMirroring_set_option(SqlServerParser.Mirroring_set_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#mirroring_partner}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMirroring_partner(SqlServerParser.Mirroring_partnerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#mirroring_witness}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMirroring_witness(SqlServerParser.Mirroring_witnessContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#witness_partner_equal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWitness_partner_equal(SqlServerParser.Witness_partner_equalContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#partner_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartner_option(SqlServerParser.Partner_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#witness_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWitness_option(SqlServerParser.Witness_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#witness_server}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWitness_server(SqlServerParser.Witness_serverContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#partner_server}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartner_server(SqlServerParser.Partner_serverContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#mirroring_host_port_seperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMirroring_host_port_seperator(SqlServerParser.Mirroring_host_port_seperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#partner_server_tcp_prefix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartner_server_tcp_prefix(SqlServerParser.Partner_server_tcp_prefixContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#port_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPort_number(SqlServerParser.Port_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#host}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHost(SqlServerParser.HostContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#date_correlation_optimization_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDate_correlation_optimization_option(SqlServerParser.Date_correlation_optimization_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#db_encryption_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDb_encryption_option(SqlServerParser.Db_encryption_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#db_state_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDb_state_option(SqlServerParser.Db_state_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#db_update_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDb_update_option(SqlServerParser.Db_update_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#db_user_access_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDb_user_access_option(SqlServerParser.Db_user_access_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#delayed_durability_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelayed_durability_option(SqlServerParser.Delayed_durability_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#external_access_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternal_access_option(SqlServerParser.External_access_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#hadr_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHadr_options(SqlServerParser.Hadr_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#mixed_page_allocation_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMixed_page_allocation_option(SqlServerParser.Mixed_page_allocation_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#parameterization_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterization_option(SqlServerParser.Parameterization_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#recovery_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecovery_option(SqlServerParser.Recovery_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#service_broker_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitService_broker_option(SqlServerParser.Service_broker_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#snapshot_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSnapshot_option(SqlServerParser.Snapshot_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#sql_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_option(SqlServerParser.Sql_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#target_recovery_time_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTarget_recovery_time_option(SqlServerParser.Target_recovery_time_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#termination}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTermination(SqlServerParser.TerminationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_index(SqlServerParser.Drop_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_relational_or_xml_or_spatial_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_relational_or_xml_or_spatial_index(SqlServerParser.Drop_relational_or_xml_or_spatial_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_backward_compatible_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_backward_compatible_index(SqlServerParser.Drop_backward_compatible_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_procedure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_procedure(SqlServerParser.Drop_procedureContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_trigger(SqlServerParser.Drop_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_dml_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_dml_trigger(SqlServerParser.Drop_dml_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_ddl_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_ddl_trigger(SqlServerParser.Drop_ddl_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_function(SqlServerParser.Drop_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_statistics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_statistics(SqlServerParser.Drop_statisticsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_table(SqlServerParser.Drop_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_view(SqlServerParser.Drop_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_type(SqlServerParser.Create_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#drop_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_type(SqlServerParser.Drop_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#rowset_function_limited}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowset_function_limited(SqlServerParser.Rowset_function_limitedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#openquery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpenquery(SqlServerParser.OpenqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#opendatasource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpendatasource(SqlServerParser.OpendatasourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#declare_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_statement(SqlServerParser.Declare_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#xml_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_declaration(SqlServerParser.Xml_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#cursor_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_statement(SqlServerParser.Cursor_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#backup_database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBackup_database(SqlServerParser.Backup_databaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#backup_log}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBackup_log(SqlServerParser.Backup_logContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#backup_certificate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBackup_certificate(SqlServerParser.Backup_certificateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#backup_master_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBackup_master_key(SqlServerParser.Backup_master_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#backup_service_master_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBackup_service_master_key(SqlServerParser.Backup_service_master_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#kill_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKill_statement(SqlServerParser.Kill_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#kill_process}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKill_process(SqlServerParser.Kill_processContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#kill_query_notification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKill_query_notification(SqlServerParser.Kill_query_notificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#kill_stats_job}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKill_stats_job(SqlServerParser.Kill_stats_jobContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#execute_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_statement(SqlServerParser.Execute_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#execute_body_batch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_body_batch(SqlServerParser.Execute_body_batchContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#execute_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_body(SqlServerParser.Execute_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#execute_statement_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_statement_arg(SqlServerParser.Execute_statement_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#execute_statement_arg_named}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_statement_arg_named(SqlServerParser.Execute_statement_arg_namedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#execute_statement_arg_unnamed}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_statement_arg_unnamed(SqlServerParser.Execute_statement_arg_unnamedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#execute_parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_parameter(SqlServerParser.Execute_parameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#execute_var_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_var_string(SqlServerParser.Execute_var_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#security_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSecurity_statement(SqlServerParser.Security_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#principal_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrincipal_id(SqlServerParser.Principal_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_certificate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_certificate(SqlServerParser.Create_certificateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#existing_keys}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExisting_keys(SqlServerParser.Existing_keysContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#private_key_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrivate_key_options(SqlServerParser.Private_key_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#generate_new_keys}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenerate_new_keys(SqlServerParser.Generate_new_keysContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#date_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDate_options(SqlServerParser.Date_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#open_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_key(SqlServerParser.Open_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#close_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClose_key(SqlServerParser.Close_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_key(SqlServerParser.Create_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#key_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey_options(SqlServerParser.Key_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#algorithm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlgorithm(SqlServerParser.AlgorithmContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#encryption_mechanism}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEncryption_mechanism(SqlServerParser.Encryption_mechanismContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#decryption_mechanism}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecryption_mechanism(SqlServerParser.Decryption_mechanismContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#grant_permission}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant_permission(SqlServerParser.Grant_permissionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#set_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_statement(SqlServerParser.Set_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#transaction_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransaction_statement(SqlServerParser.Transaction_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#go_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGo_statement(SqlServerParser.Go_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#use_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUse_statement(SqlServerParser.Use_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#setuser_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetuser_statement(SqlServerParser.Setuser_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#reconfigure_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReconfigure_statement(SqlServerParser.Reconfigure_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#shutdown_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShutdown_statement(SqlServerParser.Shutdown_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#checkpoint_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCheckpoint_statement(SqlServerParser.Checkpoint_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_checkalloc_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_checkalloc_option(SqlServerParser.Dbcc_checkalloc_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_checkalloc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_checkalloc(SqlServerParser.Dbcc_checkallocContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_checkcatalog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_checkcatalog(SqlServerParser.Dbcc_checkcatalogContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_checkconstraints_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_checkconstraints_option(SqlServerParser.Dbcc_checkconstraints_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_checkconstraints}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_checkconstraints(SqlServerParser.Dbcc_checkconstraintsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_checkdb_table_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_checkdb_table_option(SqlServerParser.Dbcc_checkdb_table_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_checkdb}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_checkdb(SqlServerParser.Dbcc_checkdbContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_checkfilegroup_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_checkfilegroup_option(SqlServerParser.Dbcc_checkfilegroup_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_checkfilegroup}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_checkfilegroup(SqlServerParser.Dbcc_checkfilegroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_checktable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_checktable(SqlServerParser.Dbcc_checktableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_cleantable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_cleantable(SqlServerParser.Dbcc_cleantableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_clonedatabase_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_clonedatabase_option(SqlServerParser.Dbcc_clonedatabase_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_clonedatabase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_clonedatabase(SqlServerParser.Dbcc_clonedatabaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_pdw_showspaceused}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_pdw_showspaceused(SqlServerParser.Dbcc_pdw_showspaceusedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_proccache}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_proccache(SqlServerParser.Dbcc_proccacheContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_showcontig_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_showcontig_option(SqlServerParser.Dbcc_showcontig_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_showcontig}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_showcontig(SqlServerParser.Dbcc_showcontigContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_shrinklog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_shrinklog(SqlServerParser.Dbcc_shrinklogContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_dbreindex}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_dbreindex(SqlServerParser.Dbcc_dbreindexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_dll_free}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_dll_free(SqlServerParser.Dbcc_dll_freeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_dropcleanbuffers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_dropcleanbuffers(SqlServerParser.Dbcc_dropcleanbuffersContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dbcc_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbcc_clause(SqlServerParser.Dbcc_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#execute_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_clause(SqlServerParser.Execute_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#declare_local}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_local(SqlServerParser.Declare_localContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_type_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_type_definition(SqlServerParser.Table_type_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_type_indices}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_type_indices(SqlServerParser.Table_type_indicesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#xml_type_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_type_definition(SqlServerParser.Xml_type_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#xml_schema_collection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_schema_collection(SqlServerParser.Xml_schema_collectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_def_table_constraints}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_def_table_constraints(SqlServerParser.Column_def_table_constraintsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_def_table_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_def_table_constraint(SqlServerParser.Column_def_table_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_definition(SqlServerParser.Column_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_definition_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_definition_element(SqlServerParser.Column_definition_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_modifier(SqlServerParser.Column_modifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#materialized_column_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaterialized_column_definition(SqlServerParser.Materialized_column_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_constraint(SqlServerParser.Column_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_index(SqlServerParser.Column_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#on_partition_or_filegroup}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_partition_or_filegroup(SqlServerParser.On_partition_or_filegroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_constraint(SqlServerParser.Table_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#connection_node}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConnection_node(SqlServerParser.Connection_nodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#primary_key_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary_key_options(SqlServerParser.Primary_key_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#foreign_key_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeign_key_options(SqlServerParser.Foreign_key_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#check_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCheck_constraint(SqlServerParser.Check_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#on_delete}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_delete(SqlServerParser.On_deleteContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#on_update}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_update(SqlServerParser.On_updateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_table_index_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_index_options(SqlServerParser.Alter_table_index_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#alter_table_index_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_index_option(SqlServerParser.Alter_table_index_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#declare_cursor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_cursor(SqlServerParser.Declare_cursorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#declare_set_cursor_common}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_set_cursor_common(SqlServerParser.Declare_set_cursor_commonContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#declare_set_cursor_common_partial}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_set_cursor_common_partial(SqlServerParser.Declare_set_cursor_common_partialContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#fetch_cursor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetch_cursor(SqlServerParser.Fetch_cursorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#set_special}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_special(SqlServerParser.Set_specialContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#special_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecial_list(SqlServerParser.Special_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#constant_LOCAL_ID}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant_LOCAL_ID(SqlServerParser.Constant_LOCAL_IDContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(SqlServerParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(SqlServerParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#time_zone}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTime_zone(SqlServerParser.Time_zoneContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#primitive_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitive_expression(SqlServerParser.Primitive_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#case_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_expression(SqlServerParser.Case_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#unary_operator_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_operator_expression(SqlServerParser.Unary_operator_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#bracket_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracket_expression(SqlServerParser.Bracket_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#subquery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery(SqlServerParser.SubqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#with_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_expression(SqlServerParser.With_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#common_table_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommon_table_expression(SqlServerParser.Common_table_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#update_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_elem(SqlServerParser.Update_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#update_elem_merge}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_elem_merge(SqlServerParser.Update_elem_mergeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#search_condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearch_condition(SqlServerParser.Search_conditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicate(SqlServerParser.PredicateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#query_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_expression(SqlServerParser.Query_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#sql_union}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_union(SqlServerParser.Sql_unionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#query_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_specification(SqlServerParser.Query_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#top_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTop_clause(SqlServerParser.Top_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#top_percent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTop_percent(SqlServerParser.Top_percentContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#top_count}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTop_count(SqlServerParser.Top_countContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#order_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrder_by_clause(SqlServerParser.Order_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#select_order_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_order_by_clause(SqlServerParser.Select_order_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#for_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_clause(SqlServerParser.For_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#xml_common_directives}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_common_directives(SqlServerParser.Xml_common_directivesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#order_by_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrder_by_expression(SqlServerParser.Order_by_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#grouping_sets_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrouping_sets_item(SqlServerParser.Grouping_sets_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#group_by_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_by_item(SqlServerParser.Group_by_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#option_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOption_clause(SqlServerParser.Option_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOption(SqlServerParser.OptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#optimize_for_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptimize_for_arg(SqlServerParser.Optimize_for_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#select_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list(SqlServerParser.Select_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#udt_method_arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUdt_method_arguments(SqlServerParser.Udt_method_argumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#asterisk}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAsterisk(SqlServerParser.AsteriskContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#udt_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUdt_elem(SqlServerParser.Udt_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#expression_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_elem(SqlServerParser.Expression_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#select_list_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list_elem(SqlServerParser.Select_list_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_sources}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_sources(SqlServerParser.Table_sourcesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#non_ansi_join}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_ansi_join(SqlServerParser.Non_ansi_joinContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_source}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_source(SqlServerParser.Table_sourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_source_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_source_item(SqlServerParser.Table_source_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#open_xml}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_xml(SqlServerParser.Open_xmlContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#open_json}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_json(SqlServerParser.Open_jsonContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#json_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJson_declaration(SqlServerParser.Json_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#json_column_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJson_column_declaration(SqlServerParser.Json_column_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#schema_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_declaration(SqlServerParser.Schema_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_declaration(SqlServerParser.Column_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#change_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChange_table(SqlServerParser.Change_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#change_table_changes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChange_table_changes(SqlServerParser.Change_table_changesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#change_table_version}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChange_table_version(SqlServerParser.Change_table_versionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#join_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoin_part(SqlServerParser.Join_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#join_on}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoin_on(SqlServerParser.Join_onContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#cross_join}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCross_join(SqlServerParser.Cross_joinContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#apply_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApply_(SqlServerParser.Apply_Context ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#pivot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot(SqlServerParser.PivotContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#unpivot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivot(SqlServerParser.UnpivotContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#pivot_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_clause(SqlServerParser.Pivot_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#unpivot_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivot_clause(SqlServerParser.Unpivot_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#full_column_name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFull_column_name_list(SqlServerParser.Full_column_name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#rowset_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowset_function(SqlServerParser.Rowset_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#bulk_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBulk_option(SqlServerParser.Bulk_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#derived_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDerived_table(SqlServerParser.Derived_tableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RANKING_WINDOWED_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRANKING_WINDOWED_FUNC(SqlServerParser.RANKING_WINDOWED_FUNCContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AGGREGATE_WINDOWED_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAGGREGATE_WINDOWED_FUNC(SqlServerParser.AGGREGATE_WINDOWED_FUNCContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ANALYTIC_WINDOWED_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitANALYTIC_WINDOWED_FUNC(SqlServerParser.ANALYTIC_WINDOWED_FUNCContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BUILT_IN_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBUILT_IN_FUNC(SqlServerParser.BUILT_IN_FUNCContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SCALAR_FUNCTION}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSCALAR_FUNCTION(SqlServerParser.SCALAR_FUNCTIONContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FREE_TEXT}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFREE_TEXT(SqlServerParser.FREE_TEXTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PARTITION_FUNC}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPARTITION_FUNC(SqlServerParser.PARTITION_FUNCContext ctx);
	/**
	 * Visit a parse tree produced by the {@code HIERARCHYID_METHOD}
	 * labeled alternative in {@link SqlServerParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHIERARCHYID_METHOD(SqlServerParser.HIERARCHYID_METHODContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#partition_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_function(SqlServerParser.Partition_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#freetext_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFreetext_function(SqlServerParser.Freetext_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#freetext_predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFreetext_predicate(SqlServerParser.Freetext_predicateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#json_key_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJson_key_value(SqlServerParser.Json_key_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#json_null_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJson_null_clause(SqlServerParser.Json_null_clauseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code APP_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAPP_NAME(SqlServerParser.APP_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code APPLOCK_MODE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAPPLOCK_MODE(SqlServerParser.APPLOCK_MODEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code APPLOCK_TEST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAPPLOCK_TEST(SqlServerParser.APPLOCK_TESTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ASSEMBLYPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitASSEMBLYPROPERTY(SqlServerParser.ASSEMBLYPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code COL_LENGTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCOL_LENGTH(SqlServerParser.COL_LENGTHContext ctx);
	/**
	 * Visit a parse tree produced by the {@code COL_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCOL_NAME(SqlServerParser.COL_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code COLUMNPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCOLUMNPROPERTY(SqlServerParser.COLUMNPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATABASEPROPERTYEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATABASEPROPERTYEX(SqlServerParser.DATABASEPROPERTYEXContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DB_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDB_ID(SqlServerParser.DB_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DB_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDB_NAME(SqlServerParser.DB_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FILE_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFILE_ID(SqlServerParser.FILE_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FILE_IDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFILE_IDEX(SqlServerParser.FILE_IDEXContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FILE_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFILE_NAME(SqlServerParser.FILE_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FILEGROUP_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFILEGROUP_ID(SqlServerParser.FILEGROUP_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FILEGROUP_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFILEGROUP_NAME(SqlServerParser.FILEGROUP_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FILEGROUPPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFILEGROUPPROPERTY(SqlServerParser.FILEGROUPPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FILEPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFILEPROPERTY(SqlServerParser.FILEPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FILEPROPERTYEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFILEPROPERTYEX(SqlServerParser.FILEPROPERTYEXContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FULLTEXTCATALOGPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFULLTEXTCATALOGPROPERTY(SqlServerParser.FULLTEXTCATALOGPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FULLTEXTSERVICEPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFULLTEXTSERVICEPROPERTY(SqlServerParser.FULLTEXTSERVICEPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code INDEX_COL}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitINDEX_COL(SqlServerParser.INDEX_COLContext ctx);
	/**
	 * Visit a parse tree produced by the {@code INDEXKEY_PROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitINDEXKEY_PROPERTY(SqlServerParser.INDEXKEY_PROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code INDEXPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitINDEXPROPERTY(SqlServerParser.INDEXPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NEXT_VALUE_FOR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNEXT_VALUE_FOR(SqlServerParser.NEXT_VALUE_FORContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OBJECT_DEFINITION}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOBJECT_DEFINITION(SqlServerParser.OBJECT_DEFINITIONContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OBJECT_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOBJECT_ID(SqlServerParser.OBJECT_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OBJECT_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOBJECT_NAME(SqlServerParser.OBJECT_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OBJECT_SCHEMA_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOBJECT_SCHEMA_NAME(SqlServerParser.OBJECT_SCHEMA_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OBJECTPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOBJECTPROPERTY(SqlServerParser.OBJECTPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OBJECTPROPERTYEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOBJECTPROPERTYEX(SqlServerParser.OBJECTPROPERTYEXContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ORIGINAL_DB_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitORIGINAL_DB_NAME(SqlServerParser.ORIGINAL_DB_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PARSENAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPARSENAME(SqlServerParser.PARSENAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SCHEMA_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSCHEMA_ID(SqlServerParser.SCHEMA_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SCHEMA_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSCHEMA_NAME(SqlServerParser.SCHEMA_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SCOPE_IDENTITY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSCOPE_IDENTITY(SqlServerParser.SCOPE_IDENTITYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SERVERPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSERVERPROPERTY(SqlServerParser.SERVERPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code STATS_DATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSTATS_DATE(SqlServerParser.STATS_DATEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TYPE_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTYPE_ID(SqlServerParser.TYPE_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TYPE_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTYPE_NAME(SqlServerParser.TYPE_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TYPEPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTYPEPROPERTY(SqlServerParser.TYPEPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ASCII}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitASCII(SqlServerParser.ASCIIContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CHAR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCHAR(SqlServerParser.CHARContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CHARINDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCHARINDEX(SqlServerParser.CHARINDEXContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CONCAT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCONCAT(SqlServerParser.CONCATContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CONCAT_WS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCONCAT_WS(SqlServerParser.CONCAT_WSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DIFFERENCE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDIFFERENCE(SqlServerParser.DIFFERENCEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FORMAT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFORMAT(SqlServerParser.FORMATContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LEFT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLEFT(SqlServerParser.LEFTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LEN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLEN(SqlServerParser.LENContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LOWER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLOWER(SqlServerParser.LOWERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LTRIM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLTRIM(SqlServerParser.LTRIMContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NCHAR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNCHAR(SqlServerParser.NCHARContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PATINDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPATINDEX(SqlServerParser.PATINDEXContext ctx);
	/**
	 * Visit a parse tree produced by the {@code QUOTENAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQUOTENAME(SqlServerParser.QUOTENAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code REPLACE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitREPLACE(SqlServerParser.REPLACEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code REPLICATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitREPLICATE(SqlServerParser.REPLICATEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code REVERSE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitREVERSE(SqlServerParser.REVERSEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RIGHT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRIGHT(SqlServerParser.RIGHTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RTRIM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRTRIM(SqlServerParser.RTRIMContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SOUNDEX}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSOUNDEX(SqlServerParser.SOUNDEXContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SPACE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSPACE(SqlServerParser.SPACEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code STR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSTR(SqlServerParser.STRContext ctx);
	/**
	 * Visit a parse tree produced by the {@code STRINGAGG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSTRINGAGG(SqlServerParser.STRINGAGGContext ctx);
	/**
	 * Visit a parse tree produced by the {@code STRING_ESCAPE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSTRING_ESCAPE(SqlServerParser.STRING_ESCAPEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code STUFF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSTUFF(SqlServerParser.STUFFContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SUBSTRING}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSUBSTRING(SqlServerParser.SUBSTRINGContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TRANSLATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTRANSLATE(SqlServerParser.TRANSLATEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TRIM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTRIM(SqlServerParser.TRIMContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UNICODE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUNICODE(SqlServerParser.UNICODEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UPPER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUPPER(SqlServerParser.UPPERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BINARY_CHECKSUM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBINARY_CHECKSUM(SqlServerParser.BINARY_CHECKSUMContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CHECKSUM}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCHECKSUM(SqlServerParser.CHECKSUMContext ctx);
	/**
	 * Visit a parse tree produced by the {@code COMPRESS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCOMPRESS(SqlServerParser.COMPRESSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CONNECTIONPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCONNECTIONPROPERTY(SqlServerParser.CONNECTIONPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CONTEXT_INFO}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCONTEXT_INFO(SqlServerParser.CONTEXT_INFOContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CURRENT_REQUEST_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCURRENT_REQUEST_ID(SqlServerParser.CURRENT_REQUEST_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CURRENT_TRANSACTION_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCURRENT_TRANSACTION_ID(SqlServerParser.CURRENT_TRANSACTION_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DECOMPRESS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDECOMPRESS(SqlServerParser.DECOMPRESSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ERROR_LINE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitERROR_LINE(SqlServerParser.ERROR_LINEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ERROR_MESSAGE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitERROR_MESSAGE(SqlServerParser.ERROR_MESSAGEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ERROR_NUMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitERROR_NUMBER(SqlServerParser.ERROR_NUMBERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ERROR_PROCEDURE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitERROR_PROCEDURE(SqlServerParser.ERROR_PROCEDUREContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ERROR_SEVERITY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitERROR_SEVERITY(SqlServerParser.ERROR_SEVERITYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ERROR_STATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitERROR_STATE(SqlServerParser.ERROR_STATEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FORMATMESSAGE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFORMATMESSAGE(SqlServerParser.FORMATMESSAGEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GET_FILESTREAM_TRANSACTION_CONTEXT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGET_FILESTREAM_TRANSACTION_CONTEXT(SqlServerParser.GET_FILESTREAM_TRANSACTION_CONTEXTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GETANSINULL}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGETANSINULL(SqlServerParser.GETANSINULLContext ctx);
	/**
	 * Visit a parse tree produced by the {@code HOST_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHOST_ID(SqlServerParser.HOST_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code HOST_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHOST_NAME(SqlServerParser.HOST_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ISNULL}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitISNULL(SqlServerParser.ISNULLContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ISNUMERIC}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitISNUMERIC(SqlServerParser.ISNUMERICContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MIN_ACTIVE_ROWVERSION}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMIN_ACTIVE_ROWVERSION(SqlServerParser.MIN_ACTIVE_ROWVERSIONContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NEWID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNEWID(SqlServerParser.NEWIDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NEWSEQUENTIALID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNEWSEQUENTIALID(SqlServerParser.NEWSEQUENTIALIDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ROWCOUNT_BIG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitROWCOUNT_BIG(SqlServerParser.ROWCOUNT_BIGContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SESSION_CONTEXT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSESSION_CONTEXT(SqlServerParser.SESSION_CONTEXTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code XACT_STATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXACT_STATE(SqlServerParser.XACT_STATEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CAST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCAST(SqlServerParser.CASTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TRY_CAST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTRY_CAST(SqlServerParser.TRY_CASTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CONVERT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCONVERT(SqlServerParser.CONVERTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code COALESCE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCOALESCE(SqlServerParser.COALESCEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CURSOR_ROWS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCURSOR_ROWS(SqlServerParser.CURSOR_ROWSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FETCH_STATUS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFETCH_STATUS(SqlServerParser.FETCH_STATUSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CURSOR_STATUS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCURSOR_STATUS(SqlServerParser.CURSOR_STATUSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CERT_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCERT_ID(SqlServerParser.CERT_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATALENGTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATALENGTH(SqlServerParser.DATALENGTHContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IDENT_CURRENT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIDENT_CURRENT(SqlServerParser.IDENT_CURRENTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IDENT_INCR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIDENT_INCR(SqlServerParser.IDENT_INCRContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IDENT_SEED}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIDENT_SEED(SqlServerParser.IDENT_SEEDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IDENTITY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIDENTITY(SqlServerParser.IDENTITYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SQL_VARIANT_PROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSQL_VARIANT_PROPERTY(SqlServerParser.SQL_VARIANT_PROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CURRENT_DATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCURRENT_DATE(SqlServerParser.CURRENT_DATEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CURRENT_TIMESTAMP}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCURRENT_TIMESTAMP(SqlServerParser.CURRENT_TIMESTAMPContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CURRENT_TIMEZONE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCURRENT_TIMEZONE(SqlServerParser.CURRENT_TIMEZONEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CURRENT_TIMEZONE_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCURRENT_TIMEZONE_ID(SqlServerParser.CURRENT_TIMEZONE_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATE_BUCKET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATE_BUCKET(SqlServerParser.DATE_BUCKETContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATEADD}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATEADD(SqlServerParser.DATEADDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATEDIFF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATEDIFF(SqlServerParser.DATEDIFFContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATEDIFF_BIG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATEDIFF_BIG(SqlServerParser.DATEDIFF_BIGContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATEFROMPARTS(SqlServerParser.DATEFROMPARTSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATENAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATENAME(SqlServerParser.DATENAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATEPART}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATEPART(SqlServerParser.DATEPARTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATETIME2FROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATETIME2FROMPARTS(SqlServerParser.DATETIME2FROMPARTSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATETIMEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATETIMEFROMPARTS(SqlServerParser.DATETIMEFROMPARTSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATETIMEOFFSETFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATETIMEOFFSETFROMPARTS(SqlServerParser.DATETIMEOFFSETFROMPARTSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATETRUNC}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATETRUNC(SqlServerParser.DATETRUNCContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DAY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDAY(SqlServerParser.DAYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EOMONTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEOMONTH(SqlServerParser.EOMONTHContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GETDATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGETDATE(SqlServerParser.GETDATEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GETUTCDATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGETUTCDATE(SqlServerParser.GETUTCDATEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ISDATE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitISDATE(SqlServerParser.ISDATEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MONTH}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMONTH(SqlServerParser.MONTHContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SMALLDATETIMEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSMALLDATETIMEFROMPARTS(SqlServerParser.SMALLDATETIMEFROMPARTSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SWITCHOFFSET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSWITCHOFFSET(SqlServerParser.SWITCHOFFSETContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SYSDATETIME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSYSDATETIME(SqlServerParser.SYSDATETIMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SYSDATETIMEOFFSET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSYSDATETIMEOFFSET(SqlServerParser.SYSDATETIMEOFFSETContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SYSUTCDATETIME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSYSUTCDATETIME(SqlServerParser.SYSUTCDATETIMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TIMEFROMPARTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTIMEFROMPARTS(SqlServerParser.TIMEFROMPARTSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TODATETIMEOFFSET}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTODATETIMEOFFSET(SqlServerParser.TODATETIMEOFFSETContext ctx);
	/**
	 * Visit a parse tree produced by the {@code YEAR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitYEAR(SqlServerParser.YEARContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NULLIF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNULLIF(SqlServerParser.NULLIFContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PARSE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPARSE(SqlServerParser.PARSEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code XML_DATA_TYPE_FUNC}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXML_DATA_TYPE_FUNC(SqlServerParser.XML_DATA_TYPE_FUNCContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IIF}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIIF(SqlServerParser.IIFContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ISJSON}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitISJSON(SqlServerParser.ISJSONContext ctx);
	/**
	 * Visit a parse tree produced by the {@code JSON_OBJECT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJSON_OBJECT(SqlServerParser.JSON_OBJECTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code JSON_ARRAY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJSON_ARRAY(SqlServerParser.JSON_ARRAYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code JSON_VALUE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJSON_VALUE(SqlServerParser.JSON_VALUEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code JSON_QUERY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJSON_QUERY(SqlServerParser.JSON_QUERYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code JSON_MODIFY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJSON_MODIFY(SqlServerParser.JSON_MODIFYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code JSON_PATH_EXISTS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJSON_PATH_EXISTS(SqlServerParser.JSON_PATH_EXISTSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ABS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitABS(SqlServerParser.ABSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ACOS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitACOS(SqlServerParser.ACOSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ASIN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitASIN(SqlServerParser.ASINContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ATAN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitATAN(SqlServerParser.ATANContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ATN2}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitATN2(SqlServerParser.ATN2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code CEILING}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCEILING(SqlServerParser.CEILINGContext ctx);
	/**
	 * Visit a parse tree produced by the {@code COS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCOS(SqlServerParser.COSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code COT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCOT(SqlServerParser.COTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DEGREES}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDEGREES(SqlServerParser.DEGREESContext ctx);
	/**
	 * Visit a parse tree produced by the {@code EXP}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEXP(SqlServerParser.EXPContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FLOOR}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFLOOR(SqlServerParser.FLOORContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LOG}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLOG(SqlServerParser.LOGContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LOG10}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLOG10(SqlServerParser.LOG10Context ctx);
	/**
	 * Visit a parse tree produced by the {@code PI}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPI(SqlServerParser.PIContext ctx);
	/**
	 * Visit a parse tree produced by the {@code POWER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPOWER(SqlServerParser.POWERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RADIANS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRADIANS(SqlServerParser.RADIANSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RAND}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRAND(SqlServerParser.RANDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ROUND}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitROUND(SqlServerParser.ROUNDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MATH_SIGN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMATH_SIGN(SqlServerParser.MATH_SIGNContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SIN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSIN(SqlServerParser.SINContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SQRT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSQRT(SqlServerParser.SQRTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SQUARE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSQUARE(SqlServerParser.SQUAREContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TAN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTAN(SqlServerParser.TANContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GREATEST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGREATEST(SqlServerParser.GREATESTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LEAST}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLEAST(SqlServerParser.LEASTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CERTENCODED}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCERTENCODED(SqlServerParser.CERTENCODEDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CERTPRIVATEKEY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCERTPRIVATEKEY(SqlServerParser.CERTPRIVATEKEYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CURRENT_USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCURRENT_USER(SqlServerParser.CURRENT_USERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DATABASE_PRINCIPAL_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDATABASE_PRINCIPAL_ID(SqlServerParser.DATABASE_PRINCIPAL_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code HAS_DBACCESS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHAS_DBACCESS(SqlServerParser.HAS_DBACCESSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code HAS_PERMS_BY_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHAS_PERMS_BY_NAME(SqlServerParser.HAS_PERMS_BY_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IS_MEMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIS_MEMBER(SqlServerParser.IS_MEMBERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IS_ROLEMEMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIS_ROLEMEMBER(SqlServerParser.IS_ROLEMEMBERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IS_SRVROLEMEMBER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIS_SRVROLEMEMBER(SqlServerParser.IS_SRVROLEMEMBERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LOGINPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLOGINPROPERTY(SqlServerParser.LOGINPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ORIGINAL_LOGIN}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitORIGINAL_LOGIN(SqlServerParser.ORIGINAL_LOGINContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PERMISSIONS}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPERMISSIONS(SqlServerParser.PERMISSIONSContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PWDENCRYPT}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPWDENCRYPT(SqlServerParser.PWDENCRYPTContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PWDCOMPARE}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPWDCOMPARE(SqlServerParser.PWDCOMPAREContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SESSION_USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSESSION_USER(SqlServerParser.SESSION_USERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SESSIONPROPERTY}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSESSIONPROPERTY(SqlServerParser.SESSIONPROPERTYContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SUSER_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSUSER_ID(SqlServerParser.SUSER_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SUSER_SNAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSUSER_SNAME(SqlServerParser.SUSER_SNAMEContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SUSER_SID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSUSER_SID(SqlServerParser.SUSER_SIDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SYSTEM_USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSYSTEM_USER(SqlServerParser.SYSTEM_USERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code USER}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUSER(SqlServerParser.USERContext ctx);
	/**
	 * Visit a parse tree produced by the {@code USER_ID}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUSER_ID(SqlServerParser.USER_IDContext ctx);
	/**
	 * Visit a parse tree produced by the {@code USER_NAME}
	 * labeled alternative in {@link SqlServerParser#built_in_functions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUSER_NAME(SqlServerParser.USER_NAMEContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#xml_data_type_methods}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_data_type_methods(SqlServerParser.Xml_data_type_methodsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dateparts_9}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateparts_9(SqlServerParser.Dateparts_9Context ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dateparts_12}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateparts_12(SqlServerParser.Dateparts_12Context ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dateparts_15}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateparts_15(SqlServerParser.Dateparts_15Context ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#dateparts_datetrunc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateparts_datetrunc(SqlServerParser.Dateparts_datetruncContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#value_method}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue_method(SqlServerParser.Value_methodContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#value_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue_call(SqlServerParser.Value_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#query_method}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_method(SqlServerParser.Query_methodContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#query_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_call(SqlServerParser.Query_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#exist_method}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExist_method(SqlServerParser.Exist_methodContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#exist_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExist_call(SqlServerParser.Exist_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#modify_method}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_method(SqlServerParser.Modify_methodContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#modify_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_call(SqlServerParser.Modify_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#hierarchyid_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHierarchyid_call(SqlServerParser.Hierarchyid_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#hierarchyid_static_method}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHierarchyid_static_method(SqlServerParser.Hierarchyid_static_methodContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#nodes_method}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNodes_method(SqlServerParser.Nodes_methodContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#switch_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_section(SqlServerParser.Switch_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#switch_search_condition_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_search_condition_section(SqlServerParser.Switch_search_condition_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#as_column_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAs_column_alias(SqlServerParser.As_column_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#as_table_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAs_table_alias(SqlServerParser.As_table_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_alias(SqlServerParser.Table_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#with_table_hints}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_table_hints(SqlServerParser.With_table_hintsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#deprecated_table_hint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeprecated_table_hint(SqlServerParser.Deprecated_table_hintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#sybase_legacy_hints}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSybase_legacy_hints(SqlServerParser.Sybase_legacy_hintsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#sybase_legacy_hint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSybase_legacy_hint(SqlServerParser.Sybase_legacy_hintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_hint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_hint(SqlServerParser.Table_hintContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#index_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_value(SqlServerParser.Index_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_alias_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_alias_list(SqlServerParser.Column_alias_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_alias(SqlServerParser.Column_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_value_constructor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_value_constructor(SqlServerParser.Table_value_constructorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#expression_list_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_list_(SqlServerParser.Expression_list_Context ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#ranking_windowed_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRanking_windowed_function(SqlServerParser.Ranking_windowed_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#aggregate_windowed_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregate_windowed_function(SqlServerParser.Aggregate_windowed_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#analytic_windowed_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalytic_windowed_function(SqlServerParser.Analytic_windowed_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#all_distinct_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAll_distinct_expression(SqlServerParser.All_distinct_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#over_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOver_clause(SqlServerParser.Over_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#row_or_range_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRow_or_range_clause(SqlServerParser.Row_or_range_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#window_frame_extent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindow_frame_extent(SqlServerParser.Window_frame_extentContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#window_frame_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindow_frame_bound(SqlServerParser.Window_frame_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#window_frame_preceding}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindow_frame_preceding(SqlServerParser.Window_frame_precedingContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#window_frame_following}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindow_frame_following(SqlServerParser.Window_frame_followingContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#create_database_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_database_option(SqlServerParser.Create_database_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#database_filestream_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatabase_filestream_option(SqlServerParser.Database_filestream_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#database_file_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatabase_file_spec(SqlServerParser.Database_file_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#file_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFile_group(SqlServerParser.File_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#file_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFile_spec(SqlServerParser.File_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#entity_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntity_name(SqlServerParser.Entity_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#entity_name_for_azure_dw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntity_name_for_azure_dw(SqlServerParser.Entity_name_for_azure_dwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#entity_name_for_parallel_dw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntity_name_for_parallel_dw(SqlServerParser.Entity_name_for_parallel_dwContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#full_table_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFull_table_name(SqlServerParser.Full_table_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#table_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_name(SqlServerParser.Table_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#simple_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_name(SqlServerParser.Simple_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#func_proc_name_schema}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_proc_name_schema(SqlServerParser.Func_proc_name_schemaContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#func_proc_name_database_schema}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_proc_name_database_schema(SqlServerParser.Func_proc_name_database_schemaContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#func_proc_name_server_database_schema}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_proc_name_server_database_schema(SqlServerParser.Func_proc_name_server_database_schemaContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#ddl_object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDdl_object(SqlServerParser.Ddl_objectContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#full_column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFull_column_name(SqlServerParser.Full_column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_name_list_with_order}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_name_list_with_order(SqlServerParser.Column_name_list_with_orderContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#insert_column_name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_column_name_list(SqlServerParser.Insert_column_name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#insert_column_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_column_id(SqlServerParser.Insert_column_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#column_name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_name_list(SqlServerParser.Column_name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#cursor_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_name(SqlServerParser.Cursor_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#on_off}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_off(SqlServerParser.On_offContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#clustered}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClustered(SqlServerParser.ClusteredContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#null_notnull}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNull_notnull(SqlServerParser.Null_notnullContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#scalar_function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalar_function_name(SqlServerParser.Scalar_function_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#begin_conversation_timer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBegin_conversation_timer(SqlServerParser.Begin_conversation_timerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#begin_conversation_dialog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBegin_conversation_dialog(SqlServerParser.Begin_conversation_dialogContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#contract_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContract_name(SqlServerParser.Contract_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#service_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitService_name(SqlServerParser.Service_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#end_conversation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnd_conversation(SqlServerParser.End_conversationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#waitfor_conversation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWaitfor_conversation(SqlServerParser.Waitfor_conversationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#get_conversation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGet_conversation(SqlServerParser.Get_conversationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#queue_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQueue_id(SqlServerParser.Queue_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#send_conversation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSend_conversation(SqlServerParser.Send_conversationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#data_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData_type(SqlServerParser.Data_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(SqlServerParser.ConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#primitive_constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitive_constant(SqlServerParser.Primitive_constantContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword(SqlServerParser.KeywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#id_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId_(SqlServerParser.Id_Context ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#simple_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_id(SqlServerParser.Simple_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#id_or_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId_or_string(SqlServerParser.Id_or_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#comparison_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison_operator(SqlServerParser.Comparison_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#assignment_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_operator(SqlServerParser.Assignment_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlServerParser#file_size}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFile_size(SqlServerParser.File_sizeContext ctx);
}