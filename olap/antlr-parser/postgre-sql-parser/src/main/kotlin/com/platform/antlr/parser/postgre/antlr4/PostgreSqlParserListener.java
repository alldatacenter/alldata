// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.postgre.antlr4;


import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PostgreSqlParser}.
 */
public interface PostgreSqlParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#root}.
	 * @param ctx the parse tree
	 */
	void enterRoot(PostgreSqlParser.RootContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#root}.
	 * @param ctx the parse tree
	 */
	void exitRoot(PostgreSqlParser.RootContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#plsqlroot}.
	 * @param ctx the parse tree
	 */
	void enterPlsqlroot(PostgreSqlParser.PlsqlrootContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#plsqlroot}.
	 * @param ctx the parse tree
	 */
	void exitPlsqlroot(PostgreSqlParser.PlsqlrootContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmtblock}.
	 * @param ctx the parse tree
	 */
	void enterStmtblock(PostgreSqlParser.StmtblockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmtblock}.
	 * @param ctx the parse tree
	 */
	void exitStmtblock(PostgreSqlParser.StmtblockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmtmulti}.
	 * @param ctx the parse tree
	 */
	void enterStmtmulti(PostgreSqlParser.StmtmultiContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmtmulti}.
	 * @param ctx the parse tree
	 */
	void exitStmtmulti(PostgreSqlParser.StmtmultiContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(PostgreSqlParser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(PostgreSqlParser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#plsqlconsolecommand}.
	 * @param ctx the parse tree
	 */
	void enterPlsqlconsolecommand(PostgreSqlParser.PlsqlconsolecommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#plsqlconsolecommand}.
	 * @param ctx the parse tree
	 */
	void exitPlsqlconsolecommand(PostgreSqlParser.PlsqlconsolecommandContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#callstmt}.
	 * @param ctx the parse tree
	 */
	void enterCallstmt(PostgreSqlParser.CallstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#callstmt}.
	 * @param ctx the parse tree
	 */
	void exitCallstmt(PostgreSqlParser.CallstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createrolestmt}.
	 * @param ctx the parse tree
	 */
	void enterCreaterolestmt(PostgreSqlParser.CreaterolestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createrolestmt}.
	 * @param ctx the parse tree
	 */
	void exitCreaterolestmt(PostgreSqlParser.CreaterolestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_with}.
	 * @param ctx the parse tree
	 */
	void enterOpt_with(PostgreSqlParser.Opt_withContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_with}.
	 * @param ctx the parse tree
	 */
	void exitOpt_with(PostgreSqlParser.Opt_withContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optrolelist}.
	 * @param ctx the parse tree
	 */
	void enterOptrolelist(PostgreSqlParser.OptrolelistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optrolelist}.
	 * @param ctx the parse tree
	 */
	void exitOptrolelist(PostgreSqlParser.OptrolelistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alteroptrolelist}.
	 * @param ctx the parse tree
	 */
	void enterAlteroptrolelist(PostgreSqlParser.AlteroptrolelistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alteroptrolelist}.
	 * @param ctx the parse tree
	 */
	void exitAlteroptrolelist(PostgreSqlParser.AlteroptrolelistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alteroptroleelem}.
	 * @param ctx the parse tree
	 */
	void enterAlteroptroleelem(PostgreSqlParser.AlteroptroleelemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alteroptroleelem}.
	 * @param ctx the parse tree
	 */
	void exitAlteroptroleelem(PostgreSqlParser.AlteroptroleelemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createoptroleelem}.
	 * @param ctx the parse tree
	 */
	void enterCreateoptroleelem(PostgreSqlParser.CreateoptroleelemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createoptroleelem}.
	 * @param ctx the parse tree
	 */
	void exitCreateoptroleelem(PostgreSqlParser.CreateoptroleelemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createuserstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateuserstmt(PostgreSqlParser.CreateuserstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createuserstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateuserstmt(PostgreSqlParser.CreateuserstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterrolestmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterrolestmt(PostgreSqlParser.AlterrolestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterrolestmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterrolestmt(PostgreSqlParser.AlterrolestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_in_database}.
	 * @param ctx the parse tree
	 */
	void enterOpt_in_database(PostgreSqlParser.Opt_in_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_in_database}.
	 * @param ctx the parse tree
	 */
	void exitOpt_in_database(PostgreSqlParser.Opt_in_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterrolesetstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterrolesetstmt(PostgreSqlParser.AlterrolesetstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterrolesetstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterrolesetstmt(PostgreSqlParser.AlterrolesetstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#droprolestmt}.
	 * @param ctx the parse tree
	 */
	void enterDroprolestmt(PostgreSqlParser.DroprolestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#droprolestmt}.
	 * @param ctx the parse tree
	 */
	void exitDroprolestmt(PostgreSqlParser.DroprolestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#creategroupstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreategroupstmt(PostgreSqlParser.CreategroupstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#creategroupstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreategroupstmt(PostgreSqlParser.CreategroupstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altergroupstmt}.
	 * @param ctx the parse tree
	 */
	void enterAltergroupstmt(PostgreSqlParser.AltergroupstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altergroupstmt}.
	 * @param ctx the parse tree
	 */
	void exitAltergroupstmt(PostgreSqlParser.AltergroupstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#add_drop}.
	 * @param ctx the parse tree
	 */
	void enterAdd_drop(PostgreSqlParser.Add_dropContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#add_drop}.
	 * @param ctx the parse tree
	 */
	void exitAdd_drop(PostgreSqlParser.Add_dropContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createschemastmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateschemastmt(PostgreSqlParser.CreateschemastmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createschemastmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateschemastmt(PostgreSqlParser.CreateschemastmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optschemaname}.
	 * @param ctx the parse tree
	 */
	void enterOptschemaname(PostgreSqlParser.OptschemanameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optschemaname}.
	 * @param ctx the parse tree
	 */
	void exitOptschemaname(PostgreSqlParser.OptschemanameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optschemaeltlist}.
	 * @param ctx the parse tree
	 */
	void enterOptschemaeltlist(PostgreSqlParser.OptschemaeltlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optschemaeltlist}.
	 * @param ctx the parse tree
	 */
	void exitOptschemaeltlist(PostgreSqlParser.OptschemaeltlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#schema_stmt}.
	 * @param ctx the parse tree
	 */
	void enterSchema_stmt(PostgreSqlParser.Schema_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#schema_stmt}.
	 * @param ctx the parse tree
	 */
	void exitSchema_stmt(PostgreSqlParser.Schema_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#variablesetstmt}.
	 * @param ctx the parse tree
	 */
	void enterVariablesetstmt(PostgreSqlParser.VariablesetstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#variablesetstmt}.
	 * @param ctx the parse tree
	 */
	void exitVariablesetstmt(PostgreSqlParser.VariablesetstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#set_rest}.
	 * @param ctx the parse tree
	 */
	void enterSet_rest(PostgreSqlParser.Set_restContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#set_rest}.
	 * @param ctx the parse tree
	 */
	void exitSet_rest(PostgreSqlParser.Set_restContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#generic_set}.
	 * @param ctx the parse tree
	 */
	void enterGeneric_set(PostgreSqlParser.Generic_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#generic_set}.
	 * @param ctx the parse tree
	 */
	void exitGeneric_set(PostgreSqlParser.Generic_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#set_rest_more}.
	 * @param ctx the parse tree
	 */
	void enterSet_rest_more(PostgreSqlParser.Set_rest_moreContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#set_rest_more}.
	 * @param ctx the parse tree
	 */
	void exitSet_rest_more(PostgreSqlParser.Set_rest_moreContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#var_name}.
	 * @param ctx the parse tree
	 */
	void enterVar_name(PostgreSqlParser.Var_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#var_name}.
	 * @param ctx the parse tree
	 */
	void exitVar_name(PostgreSqlParser.Var_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#var_list}.
	 * @param ctx the parse tree
	 */
	void enterVar_list(PostgreSqlParser.Var_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#var_list}.
	 * @param ctx the parse tree
	 */
	void exitVar_list(PostgreSqlParser.Var_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#var_value}.
	 * @param ctx the parse tree
	 */
	void enterVar_value(PostgreSqlParser.Var_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#var_value}.
	 * @param ctx the parse tree
	 */
	void exitVar_value(PostgreSqlParser.Var_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#iso_level}.
	 * @param ctx the parse tree
	 */
	void enterIso_level(PostgreSqlParser.Iso_levelContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#iso_level}.
	 * @param ctx the parse tree
	 */
	void exitIso_level(PostgreSqlParser.Iso_levelContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_boolean_or_string}.
	 * @param ctx the parse tree
	 */
	void enterOpt_boolean_or_string(PostgreSqlParser.Opt_boolean_or_stringContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_boolean_or_string}.
	 * @param ctx the parse tree
	 */
	void exitOpt_boolean_or_string(PostgreSqlParser.Opt_boolean_or_stringContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#zone_value}.
	 * @param ctx the parse tree
	 */
	void enterZone_value(PostgreSqlParser.Zone_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#zone_value}.
	 * @param ctx the parse tree
	 */
	void exitZone_value(PostgreSqlParser.Zone_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_encoding}.
	 * @param ctx the parse tree
	 */
	void enterOpt_encoding(PostgreSqlParser.Opt_encodingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_encoding}.
	 * @param ctx the parse tree
	 */
	void exitOpt_encoding(PostgreSqlParser.Opt_encodingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#nonreservedword_or_sconst}.
	 * @param ctx the parse tree
	 */
	void enterNonreservedword_or_sconst(PostgreSqlParser.Nonreservedword_or_sconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#nonreservedword_or_sconst}.
	 * @param ctx the parse tree
	 */
	void exitNonreservedword_or_sconst(PostgreSqlParser.Nonreservedword_or_sconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#variableresetstmt}.
	 * @param ctx the parse tree
	 */
	void enterVariableresetstmt(PostgreSqlParser.VariableresetstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#variableresetstmt}.
	 * @param ctx the parse tree
	 */
	void exitVariableresetstmt(PostgreSqlParser.VariableresetstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reset_rest}.
	 * @param ctx the parse tree
	 */
	void enterReset_rest(PostgreSqlParser.Reset_restContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reset_rest}.
	 * @param ctx the parse tree
	 */
	void exitReset_rest(PostgreSqlParser.Reset_restContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#generic_reset}.
	 * @param ctx the parse tree
	 */
	void enterGeneric_reset(PostgreSqlParser.Generic_resetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#generic_reset}.
	 * @param ctx the parse tree
	 */
	void exitGeneric_reset(PostgreSqlParser.Generic_resetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#setresetclause}.
	 * @param ctx the parse tree
	 */
	void enterSetresetclause(PostgreSqlParser.SetresetclauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#setresetclause}.
	 * @param ctx the parse tree
	 */
	void exitSetresetclause(PostgreSqlParser.SetresetclauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#functionsetresetclause}.
	 * @param ctx the parse tree
	 */
	void enterFunctionsetresetclause(PostgreSqlParser.FunctionsetresetclauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#functionsetresetclause}.
	 * @param ctx the parse tree
	 */
	void exitFunctionsetresetclause(PostgreSqlParser.FunctionsetresetclauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#variableshowstmt}.
	 * @param ctx the parse tree
	 */
	void enterVariableshowstmt(PostgreSqlParser.VariableshowstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#variableshowstmt}.
	 * @param ctx the parse tree
	 */
	void exitVariableshowstmt(PostgreSqlParser.VariableshowstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constraintssetstmt}.
	 * @param ctx the parse tree
	 */
	void enterConstraintssetstmt(PostgreSqlParser.ConstraintssetstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constraintssetstmt}.
	 * @param ctx the parse tree
	 */
	void exitConstraintssetstmt(PostgreSqlParser.ConstraintssetstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constraints_set_list}.
	 * @param ctx the parse tree
	 */
	void enterConstraints_set_list(PostgreSqlParser.Constraints_set_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constraints_set_list}.
	 * @param ctx the parse tree
	 */
	void exitConstraints_set_list(PostgreSqlParser.Constraints_set_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constraints_set_mode}.
	 * @param ctx the parse tree
	 */
	void enterConstraints_set_mode(PostgreSqlParser.Constraints_set_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constraints_set_mode}.
	 * @param ctx the parse tree
	 */
	void exitConstraints_set_mode(PostgreSqlParser.Constraints_set_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#checkpointstmt}.
	 * @param ctx the parse tree
	 */
	void enterCheckpointstmt(PostgreSqlParser.CheckpointstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#checkpointstmt}.
	 * @param ctx the parse tree
	 */
	void exitCheckpointstmt(PostgreSqlParser.CheckpointstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#discardstmt}.
	 * @param ctx the parse tree
	 */
	void enterDiscardstmt(PostgreSqlParser.DiscardstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#discardstmt}.
	 * @param ctx the parse tree
	 */
	void exitDiscardstmt(PostgreSqlParser.DiscardstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altertablestmt}.
	 * @param ctx the parse tree
	 */
	void enterAltertablestmt(PostgreSqlParser.AltertablestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altertablestmt}.
	 * @param ctx the parse tree
	 */
	void exitAltertablestmt(PostgreSqlParser.AltertablestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_table_cmds}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_cmds(PostgreSqlParser.Alter_table_cmdsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_table_cmds}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_cmds(PostgreSqlParser.Alter_table_cmdsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#partition_cmd}.
	 * @param ctx the parse tree
	 */
	void enterPartition_cmd(PostgreSqlParser.Partition_cmdContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#partition_cmd}.
	 * @param ctx the parse tree
	 */
	void exitPartition_cmd(PostgreSqlParser.Partition_cmdContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#index_partition_cmd}.
	 * @param ctx the parse tree
	 */
	void enterIndex_partition_cmd(PostgreSqlParser.Index_partition_cmdContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#index_partition_cmd}.
	 * @param ctx the parse tree
	 */
	void exitIndex_partition_cmd(PostgreSqlParser.Index_partition_cmdContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_table_cmd}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_cmd(PostgreSqlParser.Alter_table_cmdContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_table_cmd}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_cmd(PostgreSqlParser.Alter_table_cmdContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_column_default}.
	 * @param ctx the parse tree
	 */
	void enterAlter_column_default(PostgreSqlParser.Alter_column_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_column_default}.
	 * @param ctx the parse tree
	 */
	void exitAlter_column_default(PostgreSqlParser.Alter_column_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_drop_behavior}.
	 * @param ctx the parse tree
	 */
	void enterOpt_drop_behavior(PostgreSqlParser.Opt_drop_behaviorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_drop_behavior}.
	 * @param ctx the parse tree
	 */
	void exitOpt_drop_behavior(PostgreSqlParser.Opt_drop_behaviorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_collate_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_collate_clause(PostgreSqlParser.Opt_collate_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_collate_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_collate_clause(PostgreSqlParser.Opt_collate_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_using}.
	 * @param ctx the parse tree
	 */
	void enterAlter_using(PostgreSqlParser.Alter_usingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_using}.
	 * @param ctx the parse tree
	 */
	void exitAlter_using(PostgreSqlParser.Alter_usingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#replica_identity}.
	 * @param ctx the parse tree
	 */
	void enterReplica_identity(PostgreSqlParser.Replica_identityContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#replica_identity}.
	 * @param ctx the parse tree
	 */
	void exitReplica_identity(PostgreSqlParser.Replica_identityContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reloptions}.
	 * @param ctx the parse tree
	 */
	void enterReloptions(PostgreSqlParser.ReloptionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reloptions}.
	 * @param ctx the parse tree
	 */
	void exitReloptions(PostgreSqlParser.ReloptionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_reloptions}.
	 * @param ctx the parse tree
	 */
	void enterOpt_reloptions(PostgreSqlParser.Opt_reloptionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_reloptions}.
	 * @param ctx the parse tree
	 */
	void exitOpt_reloptions(PostgreSqlParser.Opt_reloptionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reloption_list}.
	 * @param ctx the parse tree
	 */
	void enterReloption_list(PostgreSqlParser.Reloption_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reloption_list}.
	 * @param ctx the parse tree
	 */
	void exitReloption_list(PostgreSqlParser.Reloption_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reloption_elem}.
	 * @param ctx the parse tree
	 */
	void enterReloption_elem(PostgreSqlParser.Reloption_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reloption_elem}.
	 * @param ctx the parse tree
	 */
	void exitReloption_elem(PostgreSqlParser.Reloption_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_identity_column_option_list}.
	 * @param ctx the parse tree
	 */
	void enterAlter_identity_column_option_list(PostgreSqlParser.Alter_identity_column_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_identity_column_option_list}.
	 * @param ctx the parse tree
	 */
	void exitAlter_identity_column_option_list(PostgreSqlParser.Alter_identity_column_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_identity_column_option}.
	 * @param ctx the parse tree
	 */
	void enterAlter_identity_column_option(PostgreSqlParser.Alter_identity_column_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_identity_column_option}.
	 * @param ctx the parse tree
	 */
	void exitAlter_identity_column_option(PostgreSqlParser.Alter_identity_column_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#partitionboundspec}.
	 * @param ctx the parse tree
	 */
	void enterPartitionboundspec(PostgreSqlParser.PartitionboundspecContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#partitionboundspec}.
	 * @param ctx the parse tree
	 */
	void exitPartitionboundspec(PostgreSqlParser.PartitionboundspecContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#hash_partbound_elem}.
	 * @param ctx the parse tree
	 */
	void enterHash_partbound_elem(PostgreSqlParser.Hash_partbound_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#hash_partbound_elem}.
	 * @param ctx the parse tree
	 */
	void exitHash_partbound_elem(PostgreSqlParser.Hash_partbound_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#hash_partbound}.
	 * @param ctx the parse tree
	 */
	void enterHash_partbound(PostgreSqlParser.Hash_partboundContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#hash_partbound}.
	 * @param ctx the parse tree
	 */
	void exitHash_partbound(PostgreSqlParser.Hash_partboundContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altercompositetypestmt}.
	 * @param ctx the parse tree
	 */
	void enterAltercompositetypestmt(PostgreSqlParser.AltercompositetypestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altercompositetypestmt}.
	 * @param ctx the parse tree
	 */
	void exitAltercompositetypestmt(PostgreSqlParser.AltercompositetypestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_type_cmds}.
	 * @param ctx the parse tree
	 */
	void enterAlter_type_cmds(PostgreSqlParser.Alter_type_cmdsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_type_cmds}.
	 * @param ctx the parse tree
	 */
	void exitAlter_type_cmds(PostgreSqlParser.Alter_type_cmdsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_type_cmd}.
	 * @param ctx the parse tree
	 */
	void enterAlter_type_cmd(PostgreSqlParser.Alter_type_cmdContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_type_cmd}.
	 * @param ctx the parse tree
	 */
	void exitAlter_type_cmd(PostgreSqlParser.Alter_type_cmdContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#closeportalstmt}.
	 * @param ctx the parse tree
	 */
	void enterCloseportalstmt(PostgreSqlParser.CloseportalstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#closeportalstmt}.
	 * @param ctx the parse tree
	 */
	void exitCloseportalstmt(PostgreSqlParser.CloseportalstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copystmt}.
	 * @param ctx the parse tree
	 */
	void enterCopystmt(PostgreSqlParser.CopystmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copystmt}.
	 * @param ctx the parse tree
	 */
	void exitCopystmt(PostgreSqlParser.CopystmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_from}.
	 * @param ctx the parse tree
	 */
	void enterCopy_from(PostgreSqlParser.Copy_fromContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_from}.
	 * @param ctx the parse tree
	 */
	void exitCopy_from(PostgreSqlParser.Copy_fromContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_program}.
	 * @param ctx the parse tree
	 */
	void enterOpt_program(PostgreSqlParser.Opt_programContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_program}.
	 * @param ctx the parse tree
	 */
	void exitOpt_program(PostgreSqlParser.Opt_programContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_file_name}.
	 * @param ctx the parse tree
	 */
	void enterCopy_file_name(PostgreSqlParser.Copy_file_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_file_name}.
	 * @param ctx the parse tree
	 */
	void exitCopy_file_name(PostgreSqlParser.Copy_file_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_options}.
	 * @param ctx the parse tree
	 */
	void enterCopy_options(PostgreSqlParser.Copy_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_options}.
	 * @param ctx the parse tree
	 */
	void exitCopy_options(PostgreSqlParser.Copy_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_opt_list}.
	 * @param ctx the parse tree
	 */
	void enterCopy_opt_list(PostgreSqlParser.Copy_opt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_opt_list}.
	 * @param ctx the parse tree
	 */
	void exitCopy_opt_list(PostgreSqlParser.Copy_opt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_opt_item}.
	 * @param ctx the parse tree
	 */
	void enterCopy_opt_item(PostgreSqlParser.Copy_opt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_opt_item}.
	 * @param ctx the parse tree
	 */
	void exitCopy_opt_item(PostgreSqlParser.Copy_opt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_binary}.
	 * @param ctx the parse tree
	 */
	void enterOpt_binary(PostgreSqlParser.Opt_binaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_binary}.
	 * @param ctx the parse tree
	 */
	void exitOpt_binary(PostgreSqlParser.Opt_binaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_delimiter}.
	 * @param ctx the parse tree
	 */
	void enterCopy_delimiter(PostgreSqlParser.Copy_delimiterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_delimiter}.
	 * @param ctx the parse tree
	 */
	void exitCopy_delimiter(PostgreSqlParser.Copy_delimiterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_using}.
	 * @param ctx the parse tree
	 */
	void enterOpt_using(PostgreSqlParser.Opt_usingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_using}.
	 * @param ctx the parse tree
	 */
	void exitOpt_using(PostgreSqlParser.Opt_usingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_list}.
	 * @param ctx the parse tree
	 */
	void enterCopy_generic_opt_list(PostgreSqlParser.Copy_generic_opt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_list}.
	 * @param ctx the parse tree
	 */
	void exitCopy_generic_opt_list(PostgreSqlParser.Copy_generic_opt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_elem}.
	 * @param ctx the parse tree
	 */
	void enterCopy_generic_opt_elem(PostgreSqlParser.Copy_generic_opt_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_elem}.
	 * @param ctx the parse tree
	 */
	void exitCopy_generic_opt_elem(PostgreSqlParser.Copy_generic_opt_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_arg}.
	 * @param ctx the parse tree
	 */
	void enterCopy_generic_opt_arg(PostgreSqlParser.Copy_generic_opt_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_arg}.
	 * @param ctx the parse tree
	 */
	void exitCopy_generic_opt_arg(PostgreSqlParser.Copy_generic_opt_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_arg_list}.
	 * @param ctx the parse tree
	 */
	void enterCopy_generic_opt_arg_list(PostgreSqlParser.Copy_generic_opt_arg_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_arg_list}.
	 * @param ctx the parse tree
	 */
	void exitCopy_generic_opt_arg_list(PostgreSqlParser.Copy_generic_opt_arg_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_arg_list_item}.
	 * @param ctx the parse tree
	 */
	void enterCopy_generic_opt_arg_list_item(PostgreSqlParser.Copy_generic_opt_arg_list_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_arg_list_item}.
	 * @param ctx the parse tree
	 */
	void exitCopy_generic_opt_arg_list_item(PostgreSqlParser.Copy_generic_opt_arg_list_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatestmt(PostgreSqlParser.CreatestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatestmt(PostgreSqlParser.CreatestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opttemp}.
	 * @param ctx the parse tree
	 */
	void enterOpttemp(PostgreSqlParser.OpttempContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opttemp}.
	 * @param ctx the parse tree
	 */
	void exitOpttemp(PostgreSqlParser.OpttempContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opttableelementlist}.
	 * @param ctx the parse tree
	 */
	void enterOpttableelementlist(PostgreSqlParser.OpttableelementlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opttableelementlist}.
	 * @param ctx the parse tree
	 */
	void exitOpttableelementlist(PostgreSqlParser.OpttableelementlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opttypedtableelementlist}.
	 * @param ctx the parse tree
	 */
	void enterOpttypedtableelementlist(PostgreSqlParser.OpttypedtableelementlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opttypedtableelementlist}.
	 * @param ctx the parse tree
	 */
	void exitOpttypedtableelementlist(PostgreSqlParser.OpttypedtableelementlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#tableelementlist}.
	 * @param ctx the parse tree
	 */
	void enterTableelementlist(PostgreSqlParser.TableelementlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#tableelementlist}.
	 * @param ctx the parse tree
	 */
	void exitTableelementlist(PostgreSqlParser.TableelementlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#typedtableelementlist}.
	 * @param ctx the parse tree
	 */
	void enterTypedtableelementlist(PostgreSqlParser.TypedtableelementlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#typedtableelementlist}.
	 * @param ctx the parse tree
	 */
	void exitTypedtableelementlist(PostgreSqlParser.TypedtableelementlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#tableelement}.
	 * @param ctx the parse tree
	 */
	void enterTableelement(PostgreSqlParser.TableelementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#tableelement}.
	 * @param ctx the parse tree
	 */
	void exitTableelement(PostgreSqlParser.TableelementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#typedtableelement}.
	 * @param ctx the parse tree
	 */
	void enterTypedtableelement(PostgreSqlParser.TypedtableelementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#typedtableelement}.
	 * @param ctx the parse tree
	 */
	void exitTypedtableelement(PostgreSqlParser.TypedtableelementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#columnDef}.
	 * @param ctx the parse tree
	 */
	void enterColumnDef(PostgreSqlParser.ColumnDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#columnDef}.
	 * @param ctx the parse tree
	 */
	void exitColumnDef(PostgreSqlParser.ColumnDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#columnOptions}.
	 * @param ctx the parse tree
	 */
	void enterColumnOptions(PostgreSqlParser.ColumnOptionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#columnOptions}.
	 * @param ctx the parse tree
	 */
	void exitColumnOptions(PostgreSqlParser.ColumnOptionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#colquallist}.
	 * @param ctx the parse tree
	 */
	void enterColquallist(PostgreSqlParser.ColquallistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#colquallist}.
	 * @param ctx the parse tree
	 */
	void exitColquallist(PostgreSqlParser.ColquallistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#colconstraint}.
	 * @param ctx the parse tree
	 */
	void enterColconstraint(PostgreSqlParser.ColconstraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#colconstraint}.
	 * @param ctx the parse tree
	 */
	void exitColconstraint(PostgreSqlParser.ColconstraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#colconstraintelem}.
	 * @param ctx the parse tree
	 */
	void enterColconstraintelem(PostgreSqlParser.ColconstraintelemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#colconstraintelem}.
	 * @param ctx the parse tree
	 */
	void exitColconstraintelem(PostgreSqlParser.ColconstraintelemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#generated_when}.
	 * @param ctx the parse tree
	 */
	void enterGenerated_when(PostgreSqlParser.Generated_whenContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#generated_when}.
	 * @param ctx the parse tree
	 */
	void exitGenerated_when(PostgreSqlParser.Generated_whenContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constraintattr}.
	 * @param ctx the parse tree
	 */
	void enterConstraintattr(PostgreSqlParser.ConstraintattrContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constraintattr}.
	 * @param ctx the parse tree
	 */
	void exitConstraintattr(PostgreSqlParser.ConstraintattrContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#tablelikeclause}.
	 * @param ctx the parse tree
	 */
	void enterTablelikeclause(PostgreSqlParser.TablelikeclauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#tablelikeclause}.
	 * @param ctx the parse tree
	 */
	void exitTablelikeclause(PostgreSqlParser.TablelikeclauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#tablelikeoptionlist}.
	 * @param ctx the parse tree
	 */
	void enterTablelikeoptionlist(PostgreSqlParser.TablelikeoptionlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#tablelikeoptionlist}.
	 * @param ctx the parse tree
	 */
	void exitTablelikeoptionlist(PostgreSqlParser.TablelikeoptionlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#tablelikeoption}.
	 * @param ctx the parse tree
	 */
	void enterTablelikeoption(PostgreSqlParser.TablelikeoptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#tablelikeoption}.
	 * @param ctx the parse tree
	 */
	void exitTablelikeoption(PostgreSqlParser.TablelikeoptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#tableconstraint}.
	 * @param ctx the parse tree
	 */
	void enterTableconstraint(PostgreSqlParser.TableconstraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#tableconstraint}.
	 * @param ctx the parse tree
	 */
	void exitTableconstraint(PostgreSqlParser.TableconstraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constraintelem}.
	 * @param ctx the parse tree
	 */
	void enterConstraintelem(PostgreSqlParser.ConstraintelemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constraintelem}.
	 * @param ctx the parse tree
	 */
	void exitConstraintelem(PostgreSqlParser.ConstraintelemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_no_inherit}.
	 * @param ctx the parse tree
	 */
	void enterOpt_no_inherit(PostgreSqlParser.Opt_no_inheritContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_no_inherit}.
	 * @param ctx the parse tree
	 */
	void exitOpt_no_inherit(PostgreSqlParser.Opt_no_inheritContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_column_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_column_list(PostgreSqlParser.Opt_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_column_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_column_list(PostgreSqlParser.Opt_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#columnlist}.
	 * @param ctx the parse tree
	 */
	void enterColumnlist(PostgreSqlParser.ColumnlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#columnlist}.
	 * @param ctx the parse tree
	 */
	void exitColumnlist(PostgreSqlParser.ColumnlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#columnElem}.
	 * @param ctx the parse tree
	 */
	void enterColumnElem(PostgreSqlParser.ColumnElemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#columnElem}.
	 * @param ctx the parse tree
	 */
	void exitColumnElem(PostgreSqlParser.ColumnElemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_c_include}.
	 * @param ctx the parse tree
	 */
	void enterOpt_c_include(PostgreSqlParser.Opt_c_includeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_c_include}.
	 * @param ctx the parse tree
	 */
	void exitOpt_c_include(PostgreSqlParser.Opt_c_includeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#key_match}.
	 * @param ctx the parse tree
	 */
	void enterKey_match(PostgreSqlParser.Key_matchContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#key_match}.
	 * @param ctx the parse tree
	 */
	void exitKey_match(PostgreSqlParser.Key_matchContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#exclusionconstraintlist}.
	 * @param ctx the parse tree
	 */
	void enterExclusionconstraintlist(PostgreSqlParser.ExclusionconstraintlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#exclusionconstraintlist}.
	 * @param ctx the parse tree
	 */
	void exitExclusionconstraintlist(PostgreSqlParser.ExclusionconstraintlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#exclusionconstraintelem}.
	 * @param ctx the parse tree
	 */
	void enterExclusionconstraintelem(PostgreSqlParser.ExclusionconstraintelemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#exclusionconstraintelem}.
	 * @param ctx the parse tree
	 */
	void exitExclusionconstraintelem(PostgreSqlParser.ExclusionconstraintelemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#exclusionwhereclause}.
	 * @param ctx the parse tree
	 */
	void enterExclusionwhereclause(PostgreSqlParser.ExclusionwhereclauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#exclusionwhereclause}.
	 * @param ctx the parse tree
	 */
	void exitExclusionwhereclause(PostgreSqlParser.ExclusionwhereclauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#key_actions}.
	 * @param ctx the parse tree
	 */
	void enterKey_actions(PostgreSqlParser.Key_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#key_actions}.
	 * @param ctx the parse tree
	 */
	void exitKey_actions(PostgreSqlParser.Key_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#key_update}.
	 * @param ctx the parse tree
	 */
	void enterKey_update(PostgreSqlParser.Key_updateContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#key_update}.
	 * @param ctx the parse tree
	 */
	void exitKey_update(PostgreSqlParser.Key_updateContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#key_delete}.
	 * @param ctx the parse tree
	 */
	void enterKey_delete(PostgreSqlParser.Key_deleteContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#key_delete}.
	 * @param ctx the parse tree
	 */
	void exitKey_delete(PostgreSqlParser.Key_deleteContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#key_action}.
	 * @param ctx the parse tree
	 */
	void enterKey_action(PostgreSqlParser.Key_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#key_action}.
	 * @param ctx the parse tree
	 */
	void exitKey_action(PostgreSqlParser.Key_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optinherit}.
	 * @param ctx the parse tree
	 */
	void enterOptinherit(PostgreSqlParser.OptinheritContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optinherit}.
	 * @param ctx the parse tree
	 */
	void exitOptinherit(PostgreSqlParser.OptinheritContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optpartitionspec}.
	 * @param ctx the parse tree
	 */
	void enterOptpartitionspec(PostgreSqlParser.OptpartitionspecContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optpartitionspec}.
	 * @param ctx the parse tree
	 */
	void exitOptpartitionspec(PostgreSqlParser.OptpartitionspecContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#partitionspec}.
	 * @param ctx the parse tree
	 */
	void enterPartitionspec(PostgreSqlParser.PartitionspecContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#partitionspec}.
	 * @param ctx the parse tree
	 */
	void exitPartitionspec(PostgreSqlParser.PartitionspecContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#part_params}.
	 * @param ctx the parse tree
	 */
	void enterPart_params(PostgreSqlParser.Part_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#part_params}.
	 * @param ctx the parse tree
	 */
	void exitPart_params(PostgreSqlParser.Part_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#part_elem}.
	 * @param ctx the parse tree
	 */
	void enterPart_elem(PostgreSqlParser.Part_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#part_elem}.
	 * @param ctx the parse tree
	 */
	void exitPart_elem(PostgreSqlParser.Part_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#table_access_method_clause}.
	 * @param ctx the parse tree
	 */
	void enterTable_access_method_clause(PostgreSqlParser.Table_access_method_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#table_access_method_clause}.
	 * @param ctx the parse tree
	 */
	void exitTable_access_method_clause(PostgreSqlParser.Table_access_method_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optwith}.
	 * @param ctx the parse tree
	 */
	void enterOptwith(PostgreSqlParser.OptwithContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optwith}.
	 * @param ctx the parse tree
	 */
	void exitOptwith(PostgreSqlParser.OptwithContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#oncommitoption}.
	 * @param ctx the parse tree
	 */
	void enterOncommitoption(PostgreSqlParser.OncommitoptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#oncommitoption}.
	 * @param ctx the parse tree
	 */
	void exitOncommitoption(PostgreSqlParser.OncommitoptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opttablespace}.
	 * @param ctx the parse tree
	 */
	void enterOpttablespace(PostgreSqlParser.OpttablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opttablespace}.
	 * @param ctx the parse tree
	 */
	void exitOpttablespace(PostgreSqlParser.OpttablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optconstablespace}.
	 * @param ctx the parse tree
	 */
	void enterOptconstablespace(PostgreSqlParser.OptconstablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optconstablespace}.
	 * @param ctx the parse tree
	 */
	void exitOptconstablespace(PostgreSqlParser.OptconstablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#existingindex}.
	 * @param ctx the parse tree
	 */
	void enterExistingindex(PostgreSqlParser.ExistingindexContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#existingindex}.
	 * @param ctx the parse tree
	 */
	void exitExistingindex(PostgreSqlParser.ExistingindexContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createstatsstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatestatsstmt(PostgreSqlParser.CreatestatsstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createstatsstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatestatsstmt(PostgreSqlParser.CreatestatsstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterstatsstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterstatsstmt(PostgreSqlParser.AlterstatsstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterstatsstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterstatsstmt(PostgreSqlParser.AlterstatsstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createasstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateasstmt(PostgreSqlParser.CreateasstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createasstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateasstmt(PostgreSqlParser.CreateasstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#create_as_target}.
	 * @param ctx the parse tree
	 */
	void enterCreate_as_target(PostgreSqlParser.Create_as_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#create_as_target}.
	 * @param ctx the parse tree
	 */
	void exitCreate_as_target(PostgreSqlParser.Create_as_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_with_data}.
	 * @param ctx the parse tree
	 */
	void enterOpt_with_data(PostgreSqlParser.Opt_with_dataContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_with_data}.
	 * @param ctx the parse tree
	 */
	void exitOpt_with_data(PostgreSqlParser.Opt_with_dataContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#creatematviewstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatematviewstmt(PostgreSqlParser.CreatematviewstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#creatematviewstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatematviewstmt(PostgreSqlParser.CreatematviewstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#create_mv_target}.
	 * @param ctx the parse tree
	 */
	void enterCreate_mv_target(PostgreSqlParser.Create_mv_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#create_mv_target}.
	 * @param ctx the parse tree
	 */
	void exitCreate_mv_target(PostgreSqlParser.Create_mv_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optnolog}.
	 * @param ctx the parse tree
	 */
	void enterOptnolog(PostgreSqlParser.OptnologContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optnolog}.
	 * @param ctx the parse tree
	 */
	void exitOptnolog(PostgreSqlParser.OptnologContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#refreshmatviewstmt}.
	 * @param ctx the parse tree
	 */
	void enterRefreshmatviewstmt(PostgreSqlParser.RefreshmatviewstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#refreshmatviewstmt}.
	 * @param ctx the parse tree
	 */
	void exitRefreshmatviewstmt(PostgreSqlParser.RefreshmatviewstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createseqstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateseqstmt(PostgreSqlParser.CreateseqstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createseqstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateseqstmt(PostgreSqlParser.CreateseqstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterseqstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterseqstmt(PostgreSqlParser.AlterseqstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterseqstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterseqstmt(PostgreSqlParser.AlterseqstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optseqoptlist}.
	 * @param ctx the parse tree
	 */
	void enterOptseqoptlist(PostgreSqlParser.OptseqoptlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optseqoptlist}.
	 * @param ctx the parse tree
	 */
	void exitOptseqoptlist(PostgreSqlParser.OptseqoptlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optparenthesizedseqoptlist}.
	 * @param ctx the parse tree
	 */
	void enterOptparenthesizedseqoptlist(PostgreSqlParser.OptparenthesizedseqoptlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optparenthesizedseqoptlist}.
	 * @param ctx the parse tree
	 */
	void exitOptparenthesizedseqoptlist(PostgreSqlParser.OptparenthesizedseqoptlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#seqoptlist}.
	 * @param ctx the parse tree
	 */
	void enterSeqoptlist(PostgreSqlParser.SeqoptlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#seqoptlist}.
	 * @param ctx the parse tree
	 */
	void exitSeqoptlist(PostgreSqlParser.SeqoptlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#seqoptelem}.
	 * @param ctx the parse tree
	 */
	void enterSeqoptelem(PostgreSqlParser.SeqoptelemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#seqoptelem}.
	 * @param ctx the parse tree
	 */
	void exitSeqoptelem(PostgreSqlParser.SeqoptelemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_by}.
	 * @param ctx the parse tree
	 */
	void enterOpt_by(PostgreSqlParser.Opt_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_by}.
	 * @param ctx the parse tree
	 */
	void exitOpt_by(PostgreSqlParser.Opt_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#numericonly}.
	 * @param ctx the parse tree
	 */
	void enterNumericonly(PostgreSqlParser.NumericonlyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#numericonly}.
	 * @param ctx the parse tree
	 */
	void exitNumericonly(PostgreSqlParser.NumericonlyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#numericonly_list}.
	 * @param ctx the parse tree
	 */
	void enterNumericonly_list(PostgreSqlParser.Numericonly_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#numericonly_list}.
	 * @param ctx the parse tree
	 */
	void exitNumericonly_list(PostgreSqlParser.Numericonly_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createplangstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateplangstmt(PostgreSqlParser.CreateplangstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createplangstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateplangstmt(PostgreSqlParser.CreateplangstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_trusted}.
	 * @param ctx the parse tree
	 */
	void enterOpt_trusted(PostgreSqlParser.Opt_trustedContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_trusted}.
	 * @param ctx the parse tree
	 */
	void exitOpt_trusted(PostgreSqlParser.Opt_trustedContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#handler_name}.
	 * @param ctx the parse tree
	 */
	void enterHandler_name(PostgreSqlParser.Handler_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#handler_name}.
	 * @param ctx the parse tree
	 */
	void exitHandler_name(PostgreSqlParser.Handler_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_inline_handler}.
	 * @param ctx the parse tree
	 */
	void enterOpt_inline_handler(PostgreSqlParser.Opt_inline_handlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_inline_handler}.
	 * @param ctx the parse tree
	 */
	void exitOpt_inline_handler(PostgreSqlParser.Opt_inline_handlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#validator_clause}.
	 * @param ctx the parse tree
	 */
	void enterValidator_clause(PostgreSqlParser.Validator_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#validator_clause}.
	 * @param ctx the parse tree
	 */
	void exitValidator_clause(PostgreSqlParser.Validator_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_validator}.
	 * @param ctx the parse tree
	 */
	void enterOpt_validator(PostgreSqlParser.Opt_validatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_validator}.
	 * @param ctx the parse tree
	 */
	void exitOpt_validator(PostgreSqlParser.Opt_validatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_procedural}.
	 * @param ctx the parse tree
	 */
	void enterOpt_procedural(PostgreSqlParser.Opt_proceduralContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_procedural}.
	 * @param ctx the parse tree
	 */
	void exitOpt_procedural(PostgreSqlParser.Opt_proceduralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createtablespacestmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatetablespacestmt(PostgreSqlParser.CreatetablespacestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createtablespacestmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatetablespacestmt(PostgreSqlParser.CreatetablespacestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opttablespaceowner}.
	 * @param ctx the parse tree
	 */
	void enterOpttablespaceowner(PostgreSqlParser.OpttablespaceownerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opttablespaceowner}.
	 * @param ctx the parse tree
	 */
	void exitOpttablespaceowner(PostgreSqlParser.OpttablespaceownerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#droptablespacestmt}.
	 * @param ctx the parse tree
	 */
	void enterDroptablespacestmt(PostgreSqlParser.DroptablespacestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#droptablespacestmt}.
	 * @param ctx the parse tree
	 */
	void exitDroptablespacestmt(PostgreSqlParser.DroptablespacestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createextensionstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateextensionstmt(PostgreSqlParser.CreateextensionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createextensionstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateextensionstmt(PostgreSqlParser.CreateextensionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#create_extension_opt_list}.
	 * @param ctx the parse tree
	 */
	void enterCreate_extension_opt_list(PostgreSqlParser.Create_extension_opt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#create_extension_opt_list}.
	 * @param ctx the parse tree
	 */
	void exitCreate_extension_opt_list(PostgreSqlParser.Create_extension_opt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#create_extension_opt_item}.
	 * @param ctx the parse tree
	 */
	void enterCreate_extension_opt_item(PostgreSqlParser.Create_extension_opt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#create_extension_opt_item}.
	 * @param ctx the parse tree
	 */
	void exitCreate_extension_opt_item(PostgreSqlParser.Create_extension_opt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterextensionstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterextensionstmt(PostgreSqlParser.AlterextensionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterextensionstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterextensionstmt(PostgreSqlParser.AlterextensionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_extension_opt_list}.
	 * @param ctx the parse tree
	 */
	void enterAlter_extension_opt_list(PostgreSqlParser.Alter_extension_opt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_extension_opt_list}.
	 * @param ctx the parse tree
	 */
	void exitAlter_extension_opt_list(PostgreSqlParser.Alter_extension_opt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_extension_opt_item}.
	 * @param ctx the parse tree
	 */
	void enterAlter_extension_opt_item(PostgreSqlParser.Alter_extension_opt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_extension_opt_item}.
	 * @param ctx the parse tree
	 */
	void exitAlter_extension_opt_item(PostgreSqlParser.Alter_extension_opt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterextensioncontentsstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterextensioncontentsstmt(PostgreSqlParser.AlterextensioncontentsstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterextensioncontentsstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterextensioncontentsstmt(PostgreSqlParser.AlterextensioncontentsstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createfdwstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatefdwstmt(PostgreSqlParser.CreatefdwstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createfdwstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatefdwstmt(PostgreSqlParser.CreatefdwstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#fdw_option}.
	 * @param ctx the parse tree
	 */
	void enterFdw_option(PostgreSqlParser.Fdw_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#fdw_option}.
	 * @param ctx the parse tree
	 */
	void exitFdw_option(PostgreSqlParser.Fdw_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#fdw_options}.
	 * @param ctx the parse tree
	 */
	void enterFdw_options(PostgreSqlParser.Fdw_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#fdw_options}.
	 * @param ctx the parse tree
	 */
	void exitFdw_options(PostgreSqlParser.Fdw_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_fdw_options}.
	 * @param ctx the parse tree
	 */
	void enterOpt_fdw_options(PostgreSqlParser.Opt_fdw_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_fdw_options}.
	 * @param ctx the parse tree
	 */
	void exitOpt_fdw_options(PostgreSqlParser.Opt_fdw_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterfdwstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterfdwstmt(PostgreSqlParser.AlterfdwstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterfdwstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterfdwstmt(PostgreSqlParser.AlterfdwstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#create_generic_options}.
	 * @param ctx the parse tree
	 */
	void enterCreate_generic_options(PostgreSqlParser.Create_generic_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#create_generic_options}.
	 * @param ctx the parse tree
	 */
	void exitCreate_generic_options(PostgreSqlParser.Create_generic_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#generic_option_list}.
	 * @param ctx the parse tree
	 */
	void enterGeneric_option_list(PostgreSqlParser.Generic_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#generic_option_list}.
	 * @param ctx the parse tree
	 */
	void exitGeneric_option_list(PostgreSqlParser.Generic_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_generic_options}.
	 * @param ctx the parse tree
	 */
	void enterAlter_generic_options(PostgreSqlParser.Alter_generic_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_generic_options}.
	 * @param ctx the parse tree
	 */
	void exitAlter_generic_options(PostgreSqlParser.Alter_generic_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_generic_option_list}.
	 * @param ctx the parse tree
	 */
	void enterAlter_generic_option_list(PostgreSqlParser.Alter_generic_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_generic_option_list}.
	 * @param ctx the parse tree
	 */
	void exitAlter_generic_option_list(PostgreSqlParser.Alter_generic_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alter_generic_option_elem}.
	 * @param ctx the parse tree
	 */
	void enterAlter_generic_option_elem(PostgreSqlParser.Alter_generic_option_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alter_generic_option_elem}.
	 * @param ctx the parse tree
	 */
	void exitAlter_generic_option_elem(PostgreSqlParser.Alter_generic_option_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#generic_option_elem}.
	 * @param ctx the parse tree
	 */
	void enterGeneric_option_elem(PostgreSqlParser.Generic_option_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#generic_option_elem}.
	 * @param ctx the parse tree
	 */
	void exitGeneric_option_elem(PostgreSqlParser.Generic_option_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#generic_option_name}.
	 * @param ctx the parse tree
	 */
	void enterGeneric_option_name(PostgreSqlParser.Generic_option_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#generic_option_name}.
	 * @param ctx the parse tree
	 */
	void exitGeneric_option_name(PostgreSqlParser.Generic_option_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#generic_option_arg}.
	 * @param ctx the parse tree
	 */
	void enterGeneric_option_arg(PostgreSqlParser.Generic_option_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#generic_option_arg}.
	 * @param ctx the parse tree
	 */
	void exitGeneric_option_arg(PostgreSqlParser.Generic_option_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createforeignserverstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateforeignserverstmt(PostgreSqlParser.CreateforeignserverstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createforeignserverstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateforeignserverstmt(PostgreSqlParser.CreateforeignserverstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_type}.
	 * @param ctx the parse tree
	 */
	void enterOpt_type(PostgreSqlParser.Opt_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_type}.
	 * @param ctx the parse tree
	 */
	void exitOpt_type(PostgreSqlParser.Opt_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#foreign_server_version}.
	 * @param ctx the parse tree
	 */
	void enterForeign_server_version(PostgreSqlParser.Foreign_server_versionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#foreign_server_version}.
	 * @param ctx the parse tree
	 */
	void exitForeign_server_version(PostgreSqlParser.Foreign_server_versionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_foreign_server_version}.
	 * @param ctx the parse tree
	 */
	void enterOpt_foreign_server_version(PostgreSqlParser.Opt_foreign_server_versionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_foreign_server_version}.
	 * @param ctx the parse tree
	 */
	void exitOpt_foreign_server_version(PostgreSqlParser.Opt_foreign_server_versionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterforeignserverstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterforeignserverstmt(PostgreSqlParser.AlterforeignserverstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterforeignserverstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterforeignserverstmt(PostgreSqlParser.AlterforeignserverstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createforeigntablestmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateforeigntablestmt(PostgreSqlParser.CreateforeigntablestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createforeigntablestmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateforeigntablestmt(PostgreSqlParser.CreateforeigntablestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#importforeignschemastmt}.
	 * @param ctx the parse tree
	 */
	void enterImportforeignschemastmt(PostgreSqlParser.ImportforeignschemastmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#importforeignschemastmt}.
	 * @param ctx the parse tree
	 */
	void exitImportforeignschemastmt(PostgreSqlParser.ImportforeignschemastmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#import_qualification_type}.
	 * @param ctx the parse tree
	 */
	void enterImport_qualification_type(PostgreSqlParser.Import_qualification_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#import_qualification_type}.
	 * @param ctx the parse tree
	 */
	void exitImport_qualification_type(PostgreSqlParser.Import_qualification_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#import_qualification}.
	 * @param ctx the parse tree
	 */
	void enterImport_qualification(PostgreSqlParser.Import_qualificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#import_qualification}.
	 * @param ctx the parse tree
	 */
	void exitImport_qualification(PostgreSqlParser.Import_qualificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createusermappingstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateusermappingstmt(PostgreSqlParser.CreateusermappingstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createusermappingstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateusermappingstmt(PostgreSqlParser.CreateusermappingstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#auth_ident}.
	 * @param ctx the parse tree
	 */
	void enterAuth_ident(PostgreSqlParser.Auth_identContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#auth_ident}.
	 * @param ctx the parse tree
	 */
	void exitAuth_ident(PostgreSqlParser.Auth_identContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dropusermappingstmt}.
	 * @param ctx the parse tree
	 */
	void enterDropusermappingstmt(PostgreSqlParser.DropusermappingstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dropusermappingstmt}.
	 * @param ctx the parse tree
	 */
	void exitDropusermappingstmt(PostgreSqlParser.DropusermappingstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterusermappingstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterusermappingstmt(PostgreSqlParser.AlterusermappingstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterusermappingstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterusermappingstmt(PostgreSqlParser.AlterusermappingstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createpolicystmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatepolicystmt(PostgreSqlParser.CreatepolicystmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createpolicystmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatepolicystmt(PostgreSqlParser.CreatepolicystmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterpolicystmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterpolicystmt(PostgreSqlParser.AlterpolicystmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterpolicystmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterpolicystmt(PostgreSqlParser.AlterpolicystmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rowsecurityoptionalexpr}.
	 * @param ctx the parse tree
	 */
	void enterRowsecurityoptionalexpr(PostgreSqlParser.RowsecurityoptionalexprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rowsecurityoptionalexpr}.
	 * @param ctx the parse tree
	 */
	void exitRowsecurityoptionalexpr(PostgreSqlParser.RowsecurityoptionalexprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rowsecurityoptionalwithcheck}.
	 * @param ctx the parse tree
	 */
	void enterRowsecurityoptionalwithcheck(PostgreSqlParser.RowsecurityoptionalwithcheckContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rowsecurityoptionalwithcheck}.
	 * @param ctx the parse tree
	 */
	void exitRowsecurityoptionalwithcheck(PostgreSqlParser.RowsecurityoptionalwithcheckContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rowsecuritydefaulttorole}.
	 * @param ctx the parse tree
	 */
	void enterRowsecuritydefaulttorole(PostgreSqlParser.RowsecuritydefaulttoroleContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rowsecuritydefaulttorole}.
	 * @param ctx the parse tree
	 */
	void exitRowsecuritydefaulttorole(PostgreSqlParser.RowsecuritydefaulttoroleContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rowsecurityoptionaltorole}.
	 * @param ctx the parse tree
	 */
	void enterRowsecurityoptionaltorole(PostgreSqlParser.RowsecurityoptionaltoroleContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rowsecurityoptionaltorole}.
	 * @param ctx the parse tree
	 */
	void exitRowsecurityoptionaltorole(PostgreSqlParser.RowsecurityoptionaltoroleContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rowsecuritydefaultpermissive}.
	 * @param ctx the parse tree
	 */
	void enterRowsecuritydefaultpermissive(PostgreSqlParser.RowsecuritydefaultpermissiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rowsecuritydefaultpermissive}.
	 * @param ctx the parse tree
	 */
	void exitRowsecuritydefaultpermissive(PostgreSqlParser.RowsecuritydefaultpermissiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rowsecuritydefaultforcmd}.
	 * @param ctx the parse tree
	 */
	void enterRowsecuritydefaultforcmd(PostgreSqlParser.RowsecuritydefaultforcmdContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rowsecuritydefaultforcmd}.
	 * @param ctx the parse tree
	 */
	void exitRowsecuritydefaultforcmd(PostgreSqlParser.RowsecuritydefaultforcmdContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#row_security_cmd}.
	 * @param ctx the parse tree
	 */
	void enterRow_security_cmd(PostgreSqlParser.Row_security_cmdContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#row_security_cmd}.
	 * @param ctx the parse tree
	 */
	void exitRow_security_cmd(PostgreSqlParser.Row_security_cmdContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createamstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateamstmt(PostgreSqlParser.CreateamstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createamstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateamstmt(PostgreSqlParser.CreateamstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#am_type}.
	 * @param ctx the parse tree
	 */
	void enterAm_type(PostgreSqlParser.Am_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#am_type}.
	 * @param ctx the parse tree
	 */
	void exitAm_type(PostgreSqlParser.Am_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createtrigstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatetrigstmt(PostgreSqlParser.CreatetrigstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createtrigstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatetrigstmt(PostgreSqlParser.CreatetrigstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggeractiontime}.
	 * @param ctx the parse tree
	 */
	void enterTriggeractiontime(PostgreSqlParser.TriggeractiontimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggeractiontime}.
	 * @param ctx the parse tree
	 */
	void exitTriggeractiontime(PostgreSqlParser.TriggeractiontimeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggerevents}.
	 * @param ctx the parse tree
	 */
	void enterTriggerevents(PostgreSqlParser.TriggereventsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggerevents}.
	 * @param ctx the parse tree
	 */
	void exitTriggerevents(PostgreSqlParser.TriggereventsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggeroneevent}.
	 * @param ctx the parse tree
	 */
	void enterTriggeroneevent(PostgreSqlParser.TriggeroneeventContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggeroneevent}.
	 * @param ctx the parse tree
	 */
	void exitTriggeroneevent(PostgreSqlParser.TriggeroneeventContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggerreferencing}.
	 * @param ctx the parse tree
	 */
	void enterTriggerreferencing(PostgreSqlParser.TriggerreferencingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggerreferencing}.
	 * @param ctx the parse tree
	 */
	void exitTriggerreferencing(PostgreSqlParser.TriggerreferencingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggertransitions}.
	 * @param ctx the parse tree
	 */
	void enterTriggertransitions(PostgreSqlParser.TriggertransitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggertransitions}.
	 * @param ctx the parse tree
	 */
	void exitTriggertransitions(PostgreSqlParser.TriggertransitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggertransition}.
	 * @param ctx the parse tree
	 */
	void enterTriggertransition(PostgreSqlParser.TriggertransitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggertransition}.
	 * @param ctx the parse tree
	 */
	void exitTriggertransition(PostgreSqlParser.TriggertransitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#transitionoldornew}.
	 * @param ctx the parse tree
	 */
	void enterTransitionoldornew(PostgreSqlParser.TransitionoldornewContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#transitionoldornew}.
	 * @param ctx the parse tree
	 */
	void exitTransitionoldornew(PostgreSqlParser.TransitionoldornewContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#transitionrowortable}.
	 * @param ctx the parse tree
	 */
	void enterTransitionrowortable(PostgreSqlParser.TransitionrowortableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#transitionrowortable}.
	 * @param ctx the parse tree
	 */
	void exitTransitionrowortable(PostgreSqlParser.TransitionrowortableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#transitionrelname}.
	 * @param ctx the parse tree
	 */
	void enterTransitionrelname(PostgreSqlParser.TransitionrelnameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#transitionrelname}.
	 * @param ctx the parse tree
	 */
	void exitTransitionrelname(PostgreSqlParser.TransitionrelnameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggerforspec}.
	 * @param ctx the parse tree
	 */
	void enterTriggerforspec(PostgreSqlParser.TriggerforspecContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggerforspec}.
	 * @param ctx the parse tree
	 */
	void exitTriggerforspec(PostgreSqlParser.TriggerforspecContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggerforopteach}.
	 * @param ctx the parse tree
	 */
	void enterTriggerforopteach(PostgreSqlParser.TriggerforopteachContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggerforopteach}.
	 * @param ctx the parse tree
	 */
	void exitTriggerforopteach(PostgreSqlParser.TriggerforopteachContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggerfortype}.
	 * @param ctx the parse tree
	 */
	void enterTriggerfortype(PostgreSqlParser.TriggerfortypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggerfortype}.
	 * @param ctx the parse tree
	 */
	void exitTriggerfortype(PostgreSqlParser.TriggerfortypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggerwhen}.
	 * @param ctx the parse tree
	 */
	void enterTriggerwhen(PostgreSqlParser.TriggerwhenContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggerwhen}.
	 * @param ctx the parse tree
	 */
	void exitTriggerwhen(PostgreSqlParser.TriggerwhenContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#function_or_procedure}.
	 * @param ctx the parse tree
	 */
	void enterFunction_or_procedure(PostgreSqlParser.Function_or_procedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#function_or_procedure}.
	 * @param ctx the parse tree
	 */
	void exitFunction_or_procedure(PostgreSqlParser.Function_or_procedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggerfuncargs}.
	 * @param ctx the parse tree
	 */
	void enterTriggerfuncargs(PostgreSqlParser.TriggerfuncargsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggerfuncargs}.
	 * @param ctx the parse tree
	 */
	void exitTriggerfuncargs(PostgreSqlParser.TriggerfuncargsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#triggerfuncarg}.
	 * @param ctx the parse tree
	 */
	void enterTriggerfuncarg(PostgreSqlParser.TriggerfuncargContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#triggerfuncarg}.
	 * @param ctx the parse tree
	 */
	void exitTriggerfuncarg(PostgreSqlParser.TriggerfuncargContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#optconstrfromtable}.
	 * @param ctx the parse tree
	 */
	void enterOptconstrfromtable(PostgreSqlParser.OptconstrfromtableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#optconstrfromtable}.
	 * @param ctx the parse tree
	 */
	void exitOptconstrfromtable(PostgreSqlParser.OptconstrfromtableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constraintattributespec}.
	 * @param ctx the parse tree
	 */
	void enterConstraintattributespec(PostgreSqlParser.ConstraintattributespecContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constraintattributespec}.
	 * @param ctx the parse tree
	 */
	void exitConstraintattributespec(PostgreSqlParser.ConstraintattributespecContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constraintattributeElem}.
	 * @param ctx the parse tree
	 */
	void enterConstraintattributeElem(PostgreSqlParser.ConstraintattributeElemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constraintattributeElem}.
	 * @param ctx the parse tree
	 */
	void exitConstraintattributeElem(PostgreSqlParser.ConstraintattributeElemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createeventtrigstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateeventtrigstmt(PostgreSqlParser.CreateeventtrigstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createeventtrigstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateeventtrigstmt(PostgreSqlParser.CreateeventtrigstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#event_trigger_when_list}.
	 * @param ctx the parse tree
	 */
	void enterEvent_trigger_when_list(PostgreSqlParser.Event_trigger_when_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#event_trigger_when_list}.
	 * @param ctx the parse tree
	 */
	void exitEvent_trigger_when_list(PostgreSqlParser.Event_trigger_when_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#event_trigger_when_item}.
	 * @param ctx the parse tree
	 */
	void enterEvent_trigger_when_item(PostgreSqlParser.Event_trigger_when_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#event_trigger_when_item}.
	 * @param ctx the parse tree
	 */
	void exitEvent_trigger_when_item(PostgreSqlParser.Event_trigger_when_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#event_trigger_value_list}.
	 * @param ctx the parse tree
	 */
	void enterEvent_trigger_value_list(PostgreSqlParser.Event_trigger_value_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#event_trigger_value_list}.
	 * @param ctx the parse tree
	 */
	void exitEvent_trigger_value_list(PostgreSqlParser.Event_trigger_value_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altereventtrigstmt}.
	 * @param ctx the parse tree
	 */
	void enterAltereventtrigstmt(PostgreSqlParser.AltereventtrigstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altereventtrigstmt}.
	 * @param ctx the parse tree
	 */
	void exitAltereventtrigstmt(PostgreSqlParser.AltereventtrigstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#enable_trigger}.
	 * @param ctx the parse tree
	 */
	void enterEnable_trigger(PostgreSqlParser.Enable_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#enable_trigger}.
	 * @param ctx the parse tree
	 */
	void exitEnable_trigger(PostgreSqlParser.Enable_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createassertionstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateassertionstmt(PostgreSqlParser.CreateassertionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createassertionstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateassertionstmt(PostgreSqlParser.CreateassertionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#definestmt}.
	 * @param ctx the parse tree
	 */
	void enterDefinestmt(PostgreSqlParser.DefinestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#definestmt}.
	 * @param ctx the parse tree
	 */
	void exitDefinestmt(PostgreSqlParser.DefinestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#definition}.
	 * @param ctx the parse tree
	 */
	void enterDefinition(PostgreSqlParser.DefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#definition}.
	 * @param ctx the parse tree
	 */
	void exitDefinition(PostgreSqlParser.DefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#def_list}.
	 * @param ctx the parse tree
	 */
	void enterDef_list(PostgreSqlParser.Def_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#def_list}.
	 * @param ctx the parse tree
	 */
	void exitDef_list(PostgreSqlParser.Def_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#def_elem}.
	 * @param ctx the parse tree
	 */
	void enterDef_elem(PostgreSqlParser.Def_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#def_elem}.
	 * @param ctx the parse tree
	 */
	void exitDef_elem(PostgreSqlParser.Def_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#def_arg}.
	 * @param ctx the parse tree
	 */
	void enterDef_arg(PostgreSqlParser.Def_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#def_arg}.
	 * @param ctx the parse tree
	 */
	void exitDef_arg(PostgreSqlParser.Def_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#old_aggr_definition}.
	 * @param ctx the parse tree
	 */
	void enterOld_aggr_definition(PostgreSqlParser.Old_aggr_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#old_aggr_definition}.
	 * @param ctx the parse tree
	 */
	void exitOld_aggr_definition(PostgreSqlParser.Old_aggr_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#old_aggr_list}.
	 * @param ctx the parse tree
	 */
	void enterOld_aggr_list(PostgreSqlParser.Old_aggr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#old_aggr_list}.
	 * @param ctx the parse tree
	 */
	void exitOld_aggr_list(PostgreSqlParser.Old_aggr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#old_aggr_elem}.
	 * @param ctx the parse tree
	 */
	void enterOld_aggr_elem(PostgreSqlParser.Old_aggr_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#old_aggr_elem}.
	 * @param ctx the parse tree
	 */
	void exitOld_aggr_elem(PostgreSqlParser.Old_aggr_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_enum_val_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_enum_val_list(PostgreSqlParser.Opt_enum_val_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_enum_val_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_enum_val_list(PostgreSqlParser.Opt_enum_val_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#enum_val_list}.
	 * @param ctx the parse tree
	 */
	void enterEnum_val_list(PostgreSqlParser.Enum_val_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#enum_val_list}.
	 * @param ctx the parse tree
	 */
	void exitEnum_val_list(PostgreSqlParser.Enum_val_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterenumstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterenumstmt(PostgreSqlParser.AlterenumstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterenumstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterenumstmt(PostgreSqlParser.AlterenumstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_if_not_exists}.
	 * @param ctx the parse tree
	 */
	void enterOpt_if_not_exists(PostgreSqlParser.Opt_if_not_existsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_if_not_exists}.
	 * @param ctx the parse tree
	 */
	void exitOpt_if_not_exists(PostgreSqlParser.Opt_if_not_existsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createopclassstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateopclassstmt(PostgreSqlParser.CreateopclassstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createopclassstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateopclassstmt(PostgreSqlParser.CreateopclassstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opclass_item_list}.
	 * @param ctx the parse tree
	 */
	void enterOpclass_item_list(PostgreSqlParser.Opclass_item_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opclass_item_list}.
	 * @param ctx the parse tree
	 */
	void exitOpclass_item_list(PostgreSqlParser.Opclass_item_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opclass_item}.
	 * @param ctx the parse tree
	 */
	void enterOpclass_item(PostgreSqlParser.Opclass_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opclass_item}.
	 * @param ctx the parse tree
	 */
	void exitOpclass_item(PostgreSqlParser.Opclass_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_default}.
	 * @param ctx the parse tree
	 */
	void enterOpt_default(PostgreSqlParser.Opt_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_default}.
	 * @param ctx the parse tree
	 */
	void exitOpt_default(PostgreSqlParser.Opt_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_opfamily}.
	 * @param ctx the parse tree
	 */
	void enterOpt_opfamily(PostgreSqlParser.Opt_opfamilyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_opfamily}.
	 * @param ctx the parse tree
	 */
	void exitOpt_opfamily(PostgreSqlParser.Opt_opfamilyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opclass_purpose}.
	 * @param ctx the parse tree
	 */
	void enterOpclass_purpose(PostgreSqlParser.Opclass_purposeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opclass_purpose}.
	 * @param ctx the parse tree
	 */
	void exitOpclass_purpose(PostgreSqlParser.Opclass_purposeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_recheck}.
	 * @param ctx the parse tree
	 */
	void enterOpt_recheck(PostgreSqlParser.Opt_recheckContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_recheck}.
	 * @param ctx the parse tree
	 */
	void exitOpt_recheck(PostgreSqlParser.Opt_recheckContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createopfamilystmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateopfamilystmt(PostgreSqlParser.CreateopfamilystmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createopfamilystmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateopfamilystmt(PostgreSqlParser.CreateopfamilystmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alteropfamilystmt}.
	 * @param ctx the parse tree
	 */
	void enterAlteropfamilystmt(PostgreSqlParser.AlteropfamilystmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alteropfamilystmt}.
	 * @param ctx the parse tree
	 */
	void exitAlteropfamilystmt(PostgreSqlParser.AlteropfamilystmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opclass_drop_list}.
	 * @param ctx the parse tree
	 */
	void enterOpclass_drop_list(PostgreSqlParser.Opclass_drop_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opclass_drop_list}.
	 * @param ctx the parse tree
	 */
	void exitOpclass_drop_list(PostgreSqlParser.Opclass_drop_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opclass_drop}.
	 * @param ctx the parse tree
	 */
	void enterOpclass_drop(PostgreSqlParser.Opclass_dropContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opclass_drop}.
	 * @param ctx the parse tree
	 */
	void exitOpclass_drop(PostgreSqlParser.Opclass_dropContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dropopclassstmt}.
	 * @param ctx the parse tree
	 */
	void enterDropopclassstmt(PostgreSqlParser.DropopclassstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dropopclassstmt}.
	 * @param ctx the parse tree
	 */
	void exitDropopclassstmt(PostgreSqlParser.DropopclassstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dropopfamilystmt}.
	 * @param ctx the parse tree
	 */
	void enterDropopfamilystmt(PostgreSqlParser.DropopfamilystmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dropopfamilystmt}.
	 * @param ctx the parse tree
	 */
	void exitDropopfamilystmt(PostgreSqlParser.DropopfamilystmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dropownedstmt}.
	 * @param ctx the parse tree
	 */
	void enterDropownedstmt(PostgreSqlParser.DropownedstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dropownedstmt}.
	 * @param ctx the parse tree
	 */
	void exitDropownedstmt(PostgreSqlParser.DropownedstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reassignownedstmt}.
	 * @param ctx the parse tree
	 */
	void enterReassignownedstmt(PostgreSqlParser.ReassignownedstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reassignownedstmt}.
	 * @param ctx the parse tree
	 */
	void exitReassignownedstmt(PostgreSqlParser.ReassignownedstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dropstmt}.
	 * @param ctx the parse tree
	 */
	void enterDropstmt(PostgreSqlParser.DropstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dropstmt}.
	 * @param ctx the parse tree
	 */
	void exitDropstmt(PostgreSqlParser.DropstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#object_type_any_name}.
	 * @param ctx the parse tree
	 */
	void enterObject_type_any_name(PostgreSqlParser.Object_type_any_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#object_type_any_name}.
	 * @param ctx the parse tree
	 */
	void exitObject_type_any_name(PostgreSqlParser.Object_type_any_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#object_type_name}.
	 * @param ctx the parse tree
	 */
	void enterObject_type_name(PostgreSqlParser.Object_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#object_type_name}.
	 * @param ctx the parse tree
	 */
	void exitObject_type_name(PostgreSqlParser.Object_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#drop_type_name}.
	 * @param ctx the parse tree
	 */
	void enterDrop_type_name(PostgreSqlParser.Drop_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#drop_type_name}.
	 * @param ctx the parse tree
	 */
	void exitDrop_type_name(PostgreSqlParser.Drop_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#object_type_name_on_any_name}.
	 * @param ctx the parse tree
	 */
	void enterObject_type_name_on_any_name(PostgreSqlParser.Object_type_name_on_any_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#object_type_name_on_any_name}.
	 * @param ctx the parse tree
	 */
	void exitObject_type_name_on_any_name(PostgreSqlParser.Object_type_name_on_any_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#any_name_list}.
	 * @param ctx the parse tree
	 */
	void enterAny_name_list(PostgreSqlParser.Any_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#any_name_list}.
	 * @param ctx the parse tree
	 */
	void exitAny_name_list(PostgreSqlParser.Any_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#any_name}.
	 * @param ctx the parse tree
	 */
	void enterAny_name(PostgreSqlParser.Any_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#any_name}.
	 * @param ctx the parse tree
	 */
	void exitAny_name(PostgreSqlParser.Any_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#attrs}.
	 * @param ctx the parse tree
	 */
	void enterAttrs(PostgreSqlParser.AttrsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#attrs}.
	 * @param ctx the parse tree
	 */
	void exitAttrs(PostgreSqlParser.AttrsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#type_name_list}.
	 * @param ctx the parse tree
	 */
	void enterType_name_list(PostgreSqlParser.Type_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#type_name_list}.
	 * @param ctx the parse tree
	 */
	void exitType_name_list(PostgreSqlParser.Type_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#truncatestmt}.
	 * @param ctx the parse tree
	 */
	void enterTruncatestmt(PostgreSqlParser.TruncatestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#truncatestmt}.
	 * @param ctx the parse tree
	 */
	void exitTruncatestmt(PostgreSqlParser.TruncatestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_restart_seqs}.
	 * @param ctx the parse tree
	 */
	void enterOpt_restart_seqs(PostgreSqlParser.Opt_restart_seqsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_restart_seqs}.
	 * @param ctx the parse tree
	 */
	void exitOpt_restart_seqs(PostgreSqlParser.Opt_restart_seqsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#commentstmt}.
	 * @param ctx the parse tree
	 */
	void enterCommentstmt(PostgreSqlParser.CommentstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#commentstmt}.
	 * @param ctx the parse tree
	 */
	void exitCommentstmt(PostgreSqlParser.CommentstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#comment_text}.
	 * @param ctx the parse tree
	 */
	void enterComment_text(PostgreSqlParser.Comment_textContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#comment_text}.
	 * @param ctx the parse tree
	 */
	void exitComment_text(PostgreSqlParser.Comment_textContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#seclabelstmt}.
	 * @param ctx the parse tree
	 */
	void enterSeclabelstmt(PostgreSqlParser.SeclabelstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#seclabelstmt}.
	 * @param ctx the parse tree
	 */
	void exitSeclabelstmt(PostgreSqlParser.SeclabelstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_provider}.
	 * @param ctx the parse tree
	 */
	void enterOpt_provider(PostgreSqlParser.Opt_providerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_provider}.
	 * @param ctx the parse tree
	 */
	void exitOpt_provider(PostgreSqlParser.Opt_providerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#security_label}.
	 * @param ctx the parse tree
	 */
	void enterSecurity_label(PostgreSqlParser.Security_labelContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#security_label}.
	 * @param ctx the parse tree
	 */
	void exitSecurity_label(PostgreSqlParser.Security_labelContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#fetchstmt}.
	 * @param ctx the parse tree
	 */
	void enterFetchstmt(PostgreSqlParser.FetchstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#fetchstmt}.
	 * @param ctx the parse tree
	 */
	void exitFetchstmt(PostgreSqlParser.FetchstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#fetch_args}.
	 * @param ctx the parse tree
	 */
	void enterFetch_args(PostgreSqlParser.Fetch_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#fetch_args}.
	 * @param ctx the parse tree
	 */
	void exitFetch_args(PostgreSqlParser.Fetch_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#from_in}.
	 * @param ctx the parse tree
	 */
	void enterFrom_in(PostgreSqlParser.From_inContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#from_in}.
	 * @param ctx the parse tree
	 */
	void exitFrom_in(PostgreSqlParser.From_inContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_from_in}.
	 * @param ctx the parse tree
	 */
	void enterOpt_from_in(PostgreSqlParser.Opt_from_inContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_from_in}.
	 * @param ctx the parse tree
	 */
	void exitOpt_from_in(PostgreSqlParser.Opt_from_inContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#grantstmt}.
	 * @param ctx the parse tree
	 */
	void enterGrantstmt(PostgreSqlParser.GrantstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#grantstmt}.
	 * @param ctx the parse tree
	 */
	void exitGrantstmt(PostgreSqlParser.GrantstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#revokestmt}.
	 * @param ctx the parse tree
	 */
	void enterRevokestmt(PostgreSqlParser.RevokestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#revokestmt}.
	 * @param ctx the parse tree
	 */
	void exitRevokestmt(PostgreSqlParser.RevokestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#privileges}.
	 * @param ctx the parse tree
	 */
	void enterPrivileges(PostgreSqlParser.PrivilegesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#privileges}.
	 * @param ctx the parse tree
	 */
	void exitPrivileges(PostgreSqlParser.PrivilegesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#privilege_list}.
	 * @param ctx the parse tree
	 */
	void enterPrivilege_list(PostgreSqlParser.Privilege_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#privilege_list}.
	 * @param ctx the parse tree
	 */
	void exitPrivilege_list(PostgreSqlParser.Privilege_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#privilege}.
	 * @param ctx the parse tree
	 */
	void enterPrivilege(PostgreSqlParser.PrivilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#privilege}.
	 * @param ctx the parse tree
	 */
	void exitPrivilege(PostgreSqlParser.PrivilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#privilege_target}.
	 * @param ctx the parse tree
	 */
	void enterPrivilege_target(PostgreSqlParser.Privilege_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#privilege_target}.
	 * @param ctx the parse tree
	 */
	void exitPrivilege_target(PostgreSqlParser.Privilege_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#grantee_list}.
	 * @param ctx the parse tree
	 */
	void enterGrantee_list(PostgreSqlParser.Grantee_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#grantee_list}.
	 * @param ctx the parse tree
	 */
	void exitGrantee_list(PostgreSqlParser.Grantee_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#grantee}.
	 * @param ctx the parse tree
	 */
	void enterGrantee(PostgreSqlParser.GranteeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#grantee}.
	 * @param ctx the parse tree
	 */
	void exitGrantee(PostgreSqlParser.GranteeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_grant_grant_option}.
	 * @param ctx the parse tree
	 */
	void enterOpt_grant_grant_option(PostgreSqlParser.Opt_grant_grant_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_grant_grant_option}.
	 * @param ctx the parse tree
	 */
	void exitOpt_grant_grant_option(PostgreSqlParser.Opt_grant_grant_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#grantrolestmt}.
	 * @param ctx the parse tree
	 */
	void enterGrantrolestmt(PostgreSqlParser.GrantrolestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#grantrolestmt}.
	 * @param ctx the parse tree
	 */
	void exitGrantrolestmt(PostgreSqlParser.GrantrolestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#revokerolestmt}.
	 * @param ctx the parse tree
	 */
	void enterRevokerolestmt(PostgreSqlParser.RevokerolestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#revokerolestmt}.
	 * @param ctx the parse tree
	 */
	void exitRevokerolestmt(PostgreSqlParser.RevokerolestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_grant_admin_option}.
	 * @param ctx the parse tree
	 */
	void enterOpt_grant_admin_option(PostgreSqlParser.Opt_grant_admin_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_grant_admin_option}.
	 * @param ctx the parse tree
	 */
	void exitOpt_grant_admin_option(PostgreSqlParser.Opt_grant_admin_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_granted_by}.
	 * @param ctx the parse tree
	 */
	void enterOpt_granted_by(PostgreSqlParser.Opt_granted_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_granted_by}.
	 * @param ctx the parse tree
	 */
	void exitOpt_granted_by(PostgreSqlParser.Opt_granted_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterdefaultprivilegesstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterdefaultprivilegesstmt(PostgreSqlParser.AlterdefaultprivilegesstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterdefaultprivilegesstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterdefaultprivilegesstmt(PostgreSqlParser.AlterdefaultprivilegesstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#defacloptionlist}.
	 * @param ctx the parse tree
	 */
	void enterDefacloptionlist(PostgreSqlParser.DefacloptionlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#defacloptionlist}.
	 * @param ctx the parse tree
	 */
	void exitDefacloptionlist(PostgreSqlParser.DefacloptionlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#defacloption}.
	 * @param ctx the parse tree
	 */
	void enterDefacloption(PostgreSqlParser.DefacloptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#defacloption}.
	 * @param ctx the parse tree
	 */
	void exitDefacloption(PostgreSqlParser.DefacloptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#defaclaction}.
	 * @param ctx the parse tree
	 */
	void enterDefaclaction(PostgreSqlParser.DefaclactionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#defaclaction}.
	 * @param ctx the parse tree
	 */
	void exitDefaclaction(PostgreSqlParser.DefaclactionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#defacl_privilege_target}.
	 * @param ctx the parse tree
	 */
	void enterDefacl_privilege_target(PostgreSqlParser.Defacl_privilege_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#defacl_privilege_target}.
	 * @param ctx the parse tree
	 */
	void exitDefacl_privilege_target(PostgreSqlParser.Defacl_privilege_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#indexstmt}.
	 * @param ctx the parse tree
	 */
	void enterIndexstmt(PostgreSqlParser.IndexstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#indexstmt}.
	 * @param ctx the parse tree
	 */
	void exitIndexstmt(PostgreSqlParser.IndexstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_unique}.
	 * @param ctx the parse tree
	 */
	void enterOpt_unique(PostgreSqlParser.Opt_uniqueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_unique}.
	 * @param ctx the parse tree
	 */
	void exitOpt_unique(PostgreSqlParser.Opt_uniqueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_concurrently}.
	 * @param ctx the parse tree
	 */
	void enterOpt_concurrently(PostgreSqlParser.Opt_concurrentlyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_concurrently}.
	 * @param ctx the parse tree
	 */
	void exitOpt_concurrently(PostgreSqlParser.Opt_concurrentlyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_index_name}.
	 * @param ctx the parse tree
	 */
	void enterOpt_index_name(PostgreSqlParser.Opt_index_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_index_name}.
	 * @param ctx the parse tree
	 */
	void exitOpt_index_name(PostgreSqlParser.Opt_index_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#access_method_clause}.
	 * @param ctx the parse tree
	 */
	void enterAccess_method_clause(PostgreSqlParser.Access_method_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#access_method_clause}.
	 * @param ctx the parse tree
	 */
	void exitAccess_method_clause(PostgreSqlParser.Access_method_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#index_params}.
	 * @param ctx the parse tree
	 */
	void enterIndex_params(PostgreSqlParser.Index_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#index_params}.
	 * @param ctx the parse tree
	 */
	void exitIndex_params(PostgreSqlParser.Index_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#index_elem_options}.
	 * @param ctx the parse tree
	 */
	void enterIndex_elem_options(PostgreSqlParser.Index_elem_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#index_elem_options}.
	 * @param ctx the parse tree
	 */
	void exitIndex_elem_options(PostgreSqlParser.Index_elem_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#index_elem}.
	 * @param ctx the parse tree
	 */
	void enterIndex_elem(PostgreSqlParser.Index_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#index_elem}.
	 * @param ctx the parse tree
	 */
	void exitIndex_elem(PostgreSqlParser.Index_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_include}.
	 * @param ctx the parse tree
	 */
	void enterOpt_include(PostgreSqlParser.Opt_includeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_include}.
	 * @param ctx the parse tree
	 */
	void exitOpt_include(PostgreSqlParser.Opt_includeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#index_including_params}.
	 * @param ctx the parse tree
	 */
	void enterIndex_including_params(PostgreSqlParser.Index_including_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#index_including_params}.
	 * @param ctx the parse tree
	 */
	void exitIndex_including_params(PostgreSqlParser.Index_including_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_collate}.
	 * @param ctx the parse tree
	 */
	void enterOpt_collate(PostgreSqlParser.Opt_collateContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_collate}.
	 * @param ctx the parse tree
	 */
	void exitOpt_collate(PostgreSqlParser.Opt_collateContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_class}.
	 * @param ctx the parse tree
	 */
	void enterOpt_class(PostgreSqlParser.Opt_classContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_class}.
	 * @param ctx the parse tree
	 */
	void exitOpt_class(PostgreSqlParser.Opt_classContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_asc_desc}.
	 * @param ctx the parse tree
	 */
	void enterOpt_asc_desc(PostgreSqlParser.Opt_asc_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_asc_desc}.
	 * @param ctx the parse tree
	 */
	void exitOpt_asc_desc(PostgreSqlParser.Opt_asc_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_nulls_order}.
	 * @param ctx the parse tree
	 */
	void enterOpt_nulls_order(PostgreSqlParser.Opt_nulls_orderContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_nulls_order}.
	 * @param ctx the parse tree
	 */
	void exitOpt_nulls_order(PostgreSqlParser.Opt_nulls_orderContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createfunctionstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatefunctionstmt(PostgreSqlParser.CreatefunctionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createfunctionstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatefunctionstmt(PostgreSqlParser.CreatefunctionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_or_replace}.
	 * @param ctx the parse tree
	 */
	void enterOpt_or_replace(PostgreSqlParser.Opt_or_replaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_or_replace}.
	 * @param ctx the parse tree
	 */
	void exitOpt_or_replace(PostgreSqlParser.Opt_or_replaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_args}.
	 * @param ctx the parse tree
	 */
	void enterFunc_args(PostgreSqlParser.Func_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_args}.
	 * @param ctx the parse tree
	 */
	void exitFunc_args(PostgreSqlParser.Func_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_args_list}.
	 * @param ctx the parse tree
	 */
	void enterFunc_args_list(PostgreSqlParser.Func_args_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_args_list}.
	 * @param ctx the parse tree
	 */
	void exitFunc_args_list(PostgreSqlParser.Func_args_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#function_with_argtypes_list}.
	 * @param ctx the parse tree
	 */
	void enterFunction_with_argtypes_list(PostgreSqlParser.Function_with_argtypes_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#function_with_argtypes_list}.
	 * @param ctx the parse tree
	 */
	void exitFunction_with_argtypes_list(PostgreSqlParser.Function_with_argtypes_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#function_with_argtypes}.
	 * @param ctx the parse tree
	 */
	void enterFunction_with_argtypes(PostgreSqlParser.Function_with_argtypesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#function_with_argtypes}.
	 * @param ctx the parse tree
	 */
	void exitFunction_with_argtypes(PostgreSqlParser.Function_with_argtypesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_args_with_defaults}.
	 * @param ctx the parse tree
	 */
	void enterFunc_args_with_defaults(PostgreSqlParser.Func_args_with_defaultsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_args_with_defaults}.
	 * @param ctx the parse tree
	 */
	void exitFunc_args_with_defaults(PostgreSqlParser.Func_args_with_defaultsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_args_with_defaults_list}.
	 * @param ctx the parse tree
	 */
	void enterFunc_args_with_defaults_list(PostgreSqlParser.Func_args_with_defaults_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_args_with_defaults_list}.
	 * @param ctx the parse tree
	 */
	void exitFunc_args_with_defaults_list(PostgreSqlParser.Func_args_with_defaults_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_arg}.
	 * @param ctx the parse tree
	 */
	void enterFunc_arg(PostgreSqlParser.Func_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_arg}.
	 * @param ctx the parse tree
	 */
	void exitFunc_arg(PostgreSqlParser.Func_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#arg_class}.
	 * @param ctx the parse tree
	 */
	void enterArg_class(PostgreSqlParser.Arg_classContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#arg_class}.
	 * @param ctx the parse tree
	 */
	void exitArg_class(PostgreSqlParser.Arg_classContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#param_name}.
	 * @param ctx the parse tree
	 */
	void enterParam_name(PostgreSqlParser.Param_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#param_name}.
	 * @param ctx the parse tree
	 */
	void exitParam_name(PostgreSqlParser.Param_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_return}.
	 * @param ctx the parse tree
	 */
	void enterFunc_return(PostgreSqlParser.Func_returnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_return}.
	 * @param ctx the parse tree
	 */
	void exitFunc_return(PostgreSqlParser.Func_returnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_type}.
	 * @param ctx the parse tree
	 */
	void enterFunc_type(PostgreSqlParser.Func_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_type}.
	 * @param ctx the parse tree
	 */
	void exitFunc_type(PostgreSqlParser.Func_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_arg_with_default}.
	 * @param ctx the parse tree
	 */
	void enterFunc_arg_with_default(PostgreSqlParser.Func_arg_with_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_arg_with_default}.
	 * @param ctx the parse tree
	 */
	void exitFunc_arg_with_default(PostgreSqlParser.Func_arg_with_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#aggr_arg}.
	 * @param ctx the parse tree
	 */
	void enterAggr_arg(PostgreSqlParser.Aggr_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#aggr_arg}.
	 * @param ctx the parse tree
	 */
	void exitAggr_arg(PostgreSqlParser.Aggr_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#aggr_args}.
	 * @param ctx the parse tree
	 */
	void enterAggr_args(PostgreSqlParser.Aggr_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#aggr_args}.
	 * @param ctx the parse tree
	 */
	void exitAggr_args(PostgreSqlParser.Aggr_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#aggr_args_list}.
	 * @param ctx the parse tree
	 */
	void enterAggr_args_list(PostgreSqlParser.Aggr_args_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#aggr_args_list}.
	 * @param ctx the parse tree
	 */
	void exitAggr_args_list(PostgreSqlParser.Aggr_args_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#aggregate_with_argtypes}.
	 * @param ctx the parse tree
	 */
	void enterAggregate_with_argtypes(PostgreSqlParser.Aggregate_with_argtypesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#aggregate_with_argtypes}.
	 * @param ctx the parse tree
	 */
	void exitAggregate_with_argtypes(PostgreSqlParser.Aggregate_with_argtypesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#aggregate_with_argtypes_list}.
	 * @param ctx the parse tree
	 */
	void enterAggregate_with_argtypes_list(PostgreSqlParser.Aggregate_with_argtypes_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#aggregate_with_argtypes_list}.
	 * @param ctx the parse tree
	 */
	void exitAggregate_with_argtypes_list(PostgreSqlParser.Aggregate_with_argtypes_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createfunc_opt_list}.
	 * @param ctx the parse tree
	 */
	void enterCreatefunc_opt_list(PostgreSqlParser.Createfunc_opt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createfunc_opt_list}.
	 * @param ctx the parse tree
	 */
	void exitCreatefunc_opt_list(PostgreSqlParser.Createfunc_opt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#common_func_opt_item}.
	 * @param ctx the parse tree
	 */
	void enterCommon_func_opt_item(PostgreSqlParser.Common_func_opt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#common_func_opt_item}.
	 * @param ctx the parse tree
	 */
	void exitCommon_func_opt_item(PostgreSqlParser.Common_func_opt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createfunc_opt_item}.
	 * @param ctx the parse tree
	 */
	void enterCreatefunc_opt_item(PostgreSqlParser.Createfunc_opt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createfunc_opt_item}.
	 * @param ctx the parse tree
	 */
	void exitCreatefunc_opt_item(PostgreSqlParser.Createfunc_opt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_as}.
	 * @param ctx the parse tree
	 */
	void enterFunc_as(PostgreSqlParser.Func_asContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_as}.
	 * @param ctx the parse tree
	 */
	void exitFunc_as(PostgreSqlParser.Func_asContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#transform_type_list}.
	 * @param ctx the parse tree
	 */
	void enterTransform_type_list(PostgreSqlParser.Transform_type_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#transform_type_list}.
	 * @param ctx the parse tree
	 */
	void exitTransform_type_list(PostgreSqlParser.Transform_type_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_definition}.
	 * @param ctx the parse tree
	 */
	void enterOpt_definition(PostgreSqlParser.Opt_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_definition}.
	 * @param ctx the parse tree
	 */
	void exitOpt_definition(PostgreSqlParser.Opt_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#table_func_column}.
	 * @param ctx the parse tree
	 */
	void enterTable_func_column(PostgreSqlParser.Table_func_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#table_func_column}.
	 * @param ctx the parse tree
	 */
	void exitTable_func_column(PostgreSqlParser.Table_func_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#table_func_column_list}.
	 * @param ctx the parse tree
	 */
	void enterTable_func_column_list(PostgreSqlParser.Table_func_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#table_func_column_list}.
	 * @param ctx the parse tree
	 */
	void exitTable_func_column_list(PostgreSqlParser.Table_func_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterfunctionstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterfunctionstmt(PostgreSqlParser.AlterfunctionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterfunctionstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterfunctionstmt(PostgreSqlParser.AlterfunctionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterfunc_opt_list}.
	 * @param ctx the parse tree
	 */
	void enterAlterfunc_opt_list(PostgreSqlParser.Alterfunc_opt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterfunc_opt_list}.
	 * @param ctx the parse tree
	 */
	void exitAlterfunc_opt_list(PostgreSqlParser.Alterfunc_opt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_restrict}.
	 * @param ctx the parse tree
	 */
	void enterOpt_restrict(PostgreSqlParser.Opt_restrictContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_restrict}.
	 * @param ctx the parse tree
	 */
	void exitOpt_restrict(PostgreSqlParser.Opt_restrictContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#removefuncstmt}.
	 * @param ctx the parse tree
	 */
	void enterRemovefuncstmt(PostgreSqlParser.RemovefuncstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#removefuncstmt}.
	 * @param ctx the parse tree
	 */
	void exitRemovefuncstmt(PostgreSqlParser.RemovefuncstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#removeaggrstmt}.
	 * @param ctx the parse tree
	 */
	void enterRemoveaggrstmt(PostgreSqlParser.RemoveaggrstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#removeaggrstmt}.
	 * @param ctx the parse tree
	 */
	void exitRemoveaggrstmt(PostgreSqlParser.RemoveaggrstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#removeoperstmt}.
	 * @param ctx the parse tree
	 */
	void enterRemoveoperstmt(PostgreSqlParser.RemoveoperstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#removeoperstmt}.
	 * @param ctx the parse tree
	 */
	void exitRemoveoperstmt(PostgreSqlParser.RemoveoperstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#oper_argtypes}.
	 * @param ctx the parse tree
	 */
	void enterOper_argtypes(PostgreSqlParser.Oper_argtypesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#oper_argtypes}.
	 * @param ctx the parse tree
	 */
	void exitOper_argtypes(PostgreSqlParser.Oper_argtypesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#any_operator}.
	 * @param ctx the parse tree
	 */
	void enterAny_operator(PostgreSqlParser.Any_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#any_operator}.
	 * @param ctx the parse tree
	 */
	void exitAny_operator(PostgreSqlParser.Any_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#operator_with_argtypes_list}.
	 * @param ctx the parse tree
	 */
	void enterOperator_with_argtypes_list(PostgreSqlParser.Operator_with_argtypes_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#operator_with_argtypes_list}.
	 * @param ctx the parse tree
	 */
	void exitOperator_with_argtypes_list(PostgreSqlParser.Operator_with_argtypes_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#operator_with_argtypes}.
	 * @param ctx the parse tree
	 */
	void enterOperator_with_argtypes(PostgreSqlParser.Operator_with_argtypesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#operator_with_argtypes}.
	 * @param ctx the parse tree
	 */
	void exitOperator_with_argtypes(PostgreSqlParser.Operator_with_argtypesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dostmt}.
	 * @param ctx the parse tree
	 */
	void enterDostmt(PostgreSqlParser.DostmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dostmt}.
	 * @param ctx the parse tree
	 */
	void exitDostmt(PostgreSqlParser.DostmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dostmt_opt_list}.
	 * @param ctx the parse tree
	 */
	void enterDostmt_opt_list(PostgreSqlParser.Dostmt_opt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dostmt_opt_list}.
	 * @param ctx the parse tree
	 */
	void exitDostmt_opt_list(PostgreSqlParser.Dostmt_opt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dostmt_opt_item}.
	 * @param ctx the parse tree
	 */
	void enterDostmt_opt_item(PostgreSqlParser.Dostmt_opt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dostmt_opt_item}.
	 * @param ctx the parse tree
	 */
	void exitDostmt_opt_item(PostgreSqlParser.Dostmt_opt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createcaststmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatecaststmt(PostgreSqlParser.CreatecaststmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createcaststmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatecaststmt(PostgreSqlParser.CreatecaststmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#cast_context}.
	 * @param ctx the parse tree
	 */
	void enterCast_context(PostgreSqlParser.Cast_contextContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#cast_context}.
	 * @param ctx the parse tree
	 */
	void exitCast_context(PostgreSqlParser.Cast_contextContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dropcaststmt}.
	 * @param ctx the parse tree
	 */
	void enterDropcaststmt(PostgreSqlParser.DropcaststmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dropcaststmt}.
	 * @param ctx the parse tree
	 */
	void exitDropcaststmt(PostgreSqlParser.DropcaststmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_if_exists}.
	 * @param ctx the parse tree
	 */
	void enterOpt_if_exists(PostgreSqlParser.Opt_if_existsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_if_exists}.
	 * @param ctx the parse tree
	 */
	void exitOpt_if_exists(PostgreSqlParser.Opt_if_existsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createtransformstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatetransformstmt(PostgreSqlParser.CreatetransformstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createtransformstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatetransformstmt(PostgreSqlParser.CreatetransformstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#transform_element_list}.
	 * @param ctx the parse tree
	 */
	void enterTransform_element_list(PostgreSqlParser.Transform_element_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#transform_element_list}.
	 * @param ctx the parse tree
	 */
	void exitTransform_element_list(PostgreSqlParser.Transform_element_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#droptransformstmt}.
	 * @param ctx the parse tree
	 */
	void enterDroptransformstmt(PostgreSqlParser.DroptransformstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#droptransformstmt}.
	 * @param ctx the parse tree
	 */
	void exitDroptransformstmt(PostgreSqlParser.DroptransformstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reindexstmt}.
	 * @param ctx the parse tree
	 */
	void enterReindexstmt(PostgreSqlParser.ReindexstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reindexstmt}.
	 * @param ctx the parse tree
	 */
	void exitReindexstmt(PostgreSqlParser.ReindexstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reindex_target_type}.
	 * @param ctx the parse tree
	 */
	void enterReindex_target_type(PostgreSqlParser.Reindex_target_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reindex_target_type}.
	 * @param ctx the parse tree
	 */
	void exitReindex_target_type(PostgreSqlParser.Reindex_target_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reindex_target_multitable}.
	 * @param ctx the parse tree
	 */
	void enterReindex_target_multitable(PostgreSqlParser.Reindex_target_multitableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reindex_target_multitable}.
	 * @param ctx the parse tree
	 */
	void exitReindex_target_multitable(PostgreSqlParser.Reindex_target_multitableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reindex_option_list}.
	 * @param ctx the parse tree
	 */
	void enterReindex_option_list(PostgreSqlParser.Reindex_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reindex_option_list}.
	 * @param ctx the parse tree
	 */
	void exitReindex_option_list(PostgreSqlParser.Reindex_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reindex_option_elem}.
	 * @param ctx the parse tree
	 */
	void enterReindex_option_elem(PostgreSqlParser.Reindex_option_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reindex_option_elem}.
	 * @param ctx the parse tree
	 */
	void exitReindex_option_elem(PostgreSqlParser.Reindex_option_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altertblspcstmt}.
	 * @param ctx the parse tree
	 */
	void enterAltertblspcstmt(PostgreSqlParser.AltertblspcstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altertblspcstmt}.
	 * @param ctx the parse tree
	 */
	void exitAltertblspcstmt(PostgreSqlParser.AltertblspcstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#renamestmt}.
	 * @param ctx the parse tree
	 */
	void enterRenamestmt(PostgreSqlParser.RenamestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#renamestmt}.
	 * @param ctx the parse tree
	 */
	void exitRenamestmt(PostgreSqlParser.RenamestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_column}.
	 * @param ctx the parse tree
	 */
	void enterOpt_column(PostgreSqlParser.Opt_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_column}.
	 * @param ctx the parse tree
	 */
	void exitOpt_column(PostgreSqlParser.Opt_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_set_data}.
	 * @param ctx the parse tree
	 */
	void enterOpt_set_data(PostgreSqlParser.Opt_set_dataContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_set_data}.
	 * @param ctx the parse tree
	 */
	void exitOpt_set_data(PostgreSqlParser.Opt_set_dataContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterobjectdependsstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterobjectdependsstmt(PostgreSqlParser.AlterobjectdependsstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterobjectdependsstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterobjectdependsstmt(PostgreSqlParser.AlterobjectdependsstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_no}.
	 * @param ctx the parse tree
	 */
	void enterOpt_no(PostgreSqlParser.Opt_noContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_no}.
	 * @param ctx the parse tree
	 */
	void exitOpt_no(PostgreSqlParser.Opt_noContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterobjectschemastmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterobjectschemastmt(PostgreSqlParser.AlterobjectschemastmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterobjectschemastmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterobjectschemastmt(PostgreSqlParser.AlterobjectschemastmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alteroperatorstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlteroperatorstmt(PostgreSqlParser.AlteroperatorstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alteroperatorstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlteroperatorstmt(PostgreSqlParser.AlteroperatorstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#operator_def_list}.
	 * @param ctx the parse tree
	 */
	void enterOperator_def_list(PostgreSqlParser.Operator_def_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#operator_def_list}.
	 * @param ctx the parse tree
	 */
	void exitOperator_def_list(PostgreSqlParser.Operator_def_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#operator_def_elem}.
	 * @param ctx the parse tree
	 */
	void enterOperator_def_elem(PostgreSqlParser.Operator_def_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#operator_def_elem}.
	 * @param ctx the parse tree
	 */
	void exitOperator_def_elem(PostgreSqlParser.Operator_def_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#operator_def_arg}.
	 * @param ctx the parse tree
	 */
	void enterOperator_def_arg(PostgreSqlParser.Operator_def_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#operator_def_arg}.
	 * @param ctx the parse tree
	 */
	void exitOperator_def_arg(PostgreSqlParser.Operator_def_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altertypestmt}.
	 * @param ctx the parse tree
	 */
	void enterAltertypestmt(PostgreSqlParser.AltertypestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altertypestmt}.
	 * @param ctx the parse tree
	 */
	void exitAltertypestmt(PostgreSqlParser.AltertypestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterownerstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterownerstmt(PostgreSqlParser.AlterownerstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterownerstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterownerstmt(PostgreSqlParser.AlterownerstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createpublicationstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatepublicationstmt(PostgreSqlParser.CreatepublicationstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createpublicationstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatepublicationstmt(PostgreSqlParser.CreatepublicationstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_publication_for_tables}.
	 * @param ctx the parse tree
	 */
	void enterOpt_publication_for_tables(PostgreSqlParser.Opt_publication_for_tablesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_publication_for_tables}.
	 * @param ctx the parse tree
	 */
	void exitOpt_publication_for_tables(PostgreSqlParser.Opt_publication_for_tablesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#publication_for_tables}.
	 * @param ctx the parse tree
	 */
	void enterPublication_for_tables(PostgreSqlParser.Publication_for_tablesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#publication_for_tables}.
	 * @param ctx the parse tree
	 */
	void exitPublication_for_tables(PostgreSqlParser.Publication_for_tablesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterpublicationstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterpublicationstmt(PostgreSqlParser.AlterpublicationstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterpublicationstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterpublicationstmt(PostgreSqlParser.AlterpublicationstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createsubscriptionstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatesubscriptionstmt(PostgreSqlParser.CreatesubscriptionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createsubscriptionstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatesubscriptionstmt(PostgreSqlParser.CreatesubscriptionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#publication_name_list}.
	 * @param ctx the parse tree
	 */
	void enterPublication_name_list(PostgreSqlParser.Publication_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#publication_name_list}.
	 * @param ctx the parse tree
	 */
	void exitPublication_name_list(PostgreSqlParser.Publication_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#publication_name_item}.
	 * @param ctx the parse tree
	 */
	void enterPublication_name_item(PostgreSqlParser.Publication_name_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#publication_name_item}.
	 * @param ctx the parse tree
	 */
	void exitPublication_name_item(PostgreSqlParser.Publication_name_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altersubscriptionstmt}.
	 * @param ctx the parse tree
	 */
	void enterAltersubscriptionstmt(PostgreSqlParser.AltersubscriptionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altersubscriptionstmt}.
	 * @param ctx the parse tree
	 */
	void exitAltersubscriptionstmt(PostgreSqlParser.AltersubscriptionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dropsubscriptionstmt}.
	 * @param ctx the parse tree
	 */
	void enterDropsubscriptionstmt(PostgreSqlParser.DropsubscriptionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dropsubscriptionstmt}.
	 * @param ctx the parse tree
	 */
	void exitDropsubscriptionstmt(PostgreSqlParser.DropsubscriptionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rulestmt}.
	 * @param ctx the parse tree
	 */
	void enterRulestmt(PostgreSqlParser.RulestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rulestmt}.
	 * @param ctx the parse tree
	 */
	void exitRulestmt(PostgreSqlParser.RulestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#ruleactionlist}.
	 * @param ctx the parse tree
	 */
	void enterRuleactionlist(PostgreSqlParser.RuleactionlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#ruleactionlist}.
	 * @param ctx the parse tree
	 */
	void exitRuleactionlist(PostgreSqlParser.RuleactionlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#ruleactionmulti}.
	 * @param ctx the parse tree
	 */
	void enterRuleactionmulti(PostgreSqlParser.RuleactionmultiContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#ruleactionmulti}.
	 * @param ctx the parse tree
	 */
	void exitRuleactionmulti(PostgreSqlParser.RuleactionmultiContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#ruleactionstmt}.
	 * @param ctx the parse tree
	 */
	void enterRuleactionstmt(PostgreSqlParser.RuleactionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#ruleactionstmt}.
	 * @param ctx the parse tree
	 */
	void exitRuleactionstmt(PostgreSqlParser.RuleactionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#ruleactionstmtOrEmpty}.
	 * @param ctx the parse tree
	 */
	void enterRuleactionstmtOrEmpty(PostgreSqlParser.RuleactionstmtOrEmptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#ruleactionstmtOrEmpty}.
	 * @param ctx the parse tree
	 */
	void exitRuleactionstmtOrEmpty(PostgreSqlParser.RuleactionstmtOrEmptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#event}.
	 * @param ctx the parse tree
	 */
	void enterEvent(PostgreSqlParser.EventContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#event}.
	 * @param ctx the parse tree
	 */
	void exitEvent(PostgreSqlParser.EventContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_instead}.
	 * @param ctx the parse tree
	 */
	void enterOpt_instead(PostgreSqlParser.Opt_insteadContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_instead}.
	 * @param ctx the parse tree
	 */
	void exitOpt_instead(PostgreSqlParser.Opt_insteadContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#notifystmt}.
	 * @param ctx the parse tree
	 */
	void enterNotifystmt(PostgreSqlParser.NotifystmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#notifystmt}.
	 * @param ctx the parse tree
	 */
	void exitNotifystmt(PostgreSqlParser.NotifystmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#notify_payload}.
	 * @param ctx the parse tree
	 */
	void enterNotify_payload(PostgreSqlParser.Notify_payloadContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#notify_payload}.
	 * @param ctx the parse tree
	 */
	void exitNotify_payload(PostgreSqlParser.Notify_payloadContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#listenstmt}.
	 * @param ctx the parse tree
	 */
	void enterListenstmt(PostgreSqlParser.ListenstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#listenstmt}.
	 * @param ctx the parse tree
	 */
	void exitListenstmt(PostgreSqlParser.ListenstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#unlistenstmt}.
	 * @param ctx the parse tree
	 */
	void enterUnlistenstmt(PostgreSqlParser.UnlistenstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#unlistenstmt}.
	 * @param ctx the parse tree
	 */
	void exitUnlistenstmt(PostgreSqlParser.UnlistenstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#transactionstmt}.
	 * @param ctx the parse tree
	 */
	void enterTransactionstmt(PostgreSqlParser.TransactionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#transactionstmt}.
	 * @param ctx the parse tree
	 */
	void exitTransactionstmt(PostgreSqlParser.TransactionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_transaction}.
	 * @param ctx the parse tree
	 */
	void enterOpt_transaction(PostgreSqlParser.Opt_transactionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_transaction}.
	 * @param ctx the parse tree
	 */
	void exitOpt_transaction(PostgreSqlParser.Opt_transactionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#transaction_mode_item}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_mode_item(PostgreSqlParser.Transaction_mode_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#transaction_mode_item}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_mode_item(PostgreSqlParser.Transaction_mode_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#transaction_mode_list}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_mode_list(PostgreSqlParser.Transaction_mode_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#transaction_mode_list}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_mode_list(PostgreSqlParser.Transaction_mode_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#transaction_mode_list_or_empty}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_mode_list_or_empty(PostgreSqlParser.Transaction_mode_list_or_emptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#transaction_mode_list_or_empty}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_mode_list_or_empty(PostgreSqlParser.Transaction_mode_list_or_emptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_transaction_chain}.
	 * @param ctx the parse tree
	 */
	void enterOpt_transaction_chain(PostgreSqlParser.Opt_transaction_chainContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_transaction_chain}.
	 * @param ctx the parse tree
	 */
	void exitOpt_transaction_chain(PostgreSqlParser.Opt_transaction_chainContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#viewstmt}.
	 * @param ctx the parse tree
	 */
	void enterViewstmt(PostgreSqlParser.ViewstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#viewstmt}.
	 * @param ctx the parse tree
	 */
	void exitViewstmt(PostgreSqlParser.ViewstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_check_option}.
	 * @param ctx the parse tree
	 */
	void enterOpt_check_option(PostgreSqlParser.Opt_check_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_check_option}.
	 * @param ctx the parse tree
	 */
	void exitOpt_check_option(PostgreSqlParser.Opt_check_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#loadstmt}.
	 * @param ctx the parse tree
	 */
	void enterLoadstmt(PostgreSqlParser.LoadstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#loadstmt}.
	 * @param ctx the parse tree
	 */
	void exitLoadstmt(PostgreSqlParser.LoadstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createdbstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatedbstmt(PostgreSqlParser.CreatedbstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createdbstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatedbstmt(PostgreSqlParser.CreatedbstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createdb_opt_list}.
	 * @param ctx the parse tree
	 */
	void enterCreatedb_opt_list(PostgreSqlParser.Createdb_opt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createdb_opt_list}.
	 * @param ctx the parse tree
	 */
	void exitCreatedb_opt_list(PostgreSqlParser.Createdb_opt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createdb_opt_items}.
	 * @param ctx the parse tree
	 */
	void enterCreatedb_opt_items(PostgreSqlParser.Createdb_opt_itemsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createdb_opt_items}.
	 * @param ctx the parse tree
	 */
	void exitCreatedb_opt_items(PostgreSqlParser.Createdb_opt_itemsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createdb_opt_item}.
	 * @param ctx the parse tree
	 */
	void enterCreatedb_opt_item(PostgreSqlParser.Createdb_opt_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createdb_opt_item}.
	 * @param ctx the parse tree
	 */
	void exitCreatedb_opt_item(PostgreSqlParser.Createdb_opt_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createdb_opt_name}.
	 * @param ctx the parse tree
	 */
	void enterCreatedb_opt_name(PostgreSqlParser.Createdb_opt_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createdb_opt_name}.
	 * @param ctx the parse tree
	 */
	void exitCreatedb_opt_name(PostgreSqlParser.Createdb_opt_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_equal}.
	 * @param ctx the parse tree
	 */
	void enterOpt_equal(PostgreSqlParser.Opt_equalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_equal}.
	 * @param ctx the parse tree
	 */
	void exitOpt_equal(PostgreSqlParser.Opt_equalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterdatabasestmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterdatabasestmt(PostgreSqlParser.AlterdatabasestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterdatabasestmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterdatabasestmt(PostgreSqlParser.AlterdatabasestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterdatabasesetstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterdatabasesetstmt(PostgreSqlParser.AlterdatabasesetstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterdatabasesetstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterdatabasesetstmt(PostgreSqlParser.AlterdatabasesetstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#dropdbstmt}.
	 * @param ctx the parse tree
	 */
	void enterDropdbstmt(PostgreSqlParser.DropdbstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#dropdbstmt}.
	 * @param ctx the parse tree
	 */
	void exitDropdbstmt(PostgreSqlParser.DropdbstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#drop_option_list}.
	 * @param ctx the parse tree
	 */
	void enterDrop_option_list(PostgreSqlParser.Drop_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#drop_option_list}.
	 * @param ctx the parse tree
	 */
	void exitDrop_option_list(PostgreSqlParser.Drop_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#drop_option}.
	 * @param ctx the parse tree
	 */
	void enterDrop_option(PostgreSqlParser.Drop_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#drop_option}.
	 * @param ctx the parse tree
	 */
	void exitDrop_option(PostgreSqlParser.Drop_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altercollationstmt}.
	 * @param ctx the parse tree
	 */
	void enterAltercollationstmt(PostgreSqlParser.AltercollationstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altercollationstmt}.
	 * @param ctx the parse tree
	 */
	void exitAltercollationstmt(PostgreSqlParser.AltercollationstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altersystemstmt}.
	 * @param ctx the parse tree
	 */
	void enterAltersystemstmt(PostgreSqlParser.AltersystemstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altersystemstmt}.
	 * @param ctx the parse tree
	 */
	void exitAltersystemstmt(PostgreSqlParser.AltersystemstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createdomainstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreatedomainstmt(PostgreSqlParser.CreatedomainstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createdomainstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreatedomainstmt(PostgreSqlParser.CreatedomainstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alterdomainstmt}.
	 * @param ctx the parse tree
	 */
	void enterAlterdomainstmt(PostgreSqlParser.AlterdomainstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alterdomainstmt}.
	 * @param ctx the parse tree
	 */
	void exitAlterdomainstmt(PostgreSqlParser.AlterdomainstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_as}.
	 * @param ctx the parse tree
	 */
	void enterOpt_as(PostgreSqlParser.Opt_asContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_as}.
	 * @param ctx the parse tree
	 */
	void exitOpt_as(PostgreSqlParser.Opt_asContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altertsdictionarystmt}.
	 * @param ctx the parse tree
	 */
	void enterAltertsdictionarystmt(PostgreSqlParser.AltertsdictionarystmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altertsdictionarystmt}.
	 * @param ctx the parse tree
	 */
	void exitAltertsdictionarystmt(PostgreSqlParser.AltertsdictionarystmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#altertsconfigurationstmt}.
	 * @param ctx the parse tree
	 */
	void enterAltertsconfigurationstmt(PostgreSqlParser.AltertsconfigurationstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#altertsconfigurationstmt}.
	 * @param ctx the parse tree
	 */
	void exitAltertsconfigurationstmt(PostgreSqlParser.AltertsconfigurationstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#any_with}.
	 * @param ctx the parse tree
	 */
	void enterAny_with(PostgreSqlParser.Any_withContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#any_with}.
	 * @param ctx the parse tree
	 */
	void exitAny_with(PostgreSqlParser.Any_withContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#createconversionstmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateconversionstmt(PostgreSqlParser.CreateconversionstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#createconversionstmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateconversionstmt(PostgreSqlParser.CreateconversionstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#clusterstmt}.
	 * @param ctx the parse tree
	 */
	void enterClusterstmt(PostgreSqlParser.ClusterstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#clusterstmt}.
	 * @param ctx the parse tree
	 */
	void exitClusterstmt(PostgreSqlParser.ClusterstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#cluster_index_specification}.
	 * @param ctx the parse tree
	 */
	void enterCluster_index_specification(PostgreSqlParser.Cluster_index_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#cluster_index_specification}.
	 * @param ctx the parse tree
	 */
	void exitCluster_index_specification(PostgreSqlParser.Cluster_index_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#vacuumstmt}.
	 * @param ctx the parse tree
	 */
	void enterVacuumstmt(PostgreSqlParser.VacuumstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#vacuumstmt}.
	 * @param ctx the parse tree
	 */
	void exitVacuumstmt(PostgreSqlParser.VacuumstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#analyzestmt}.
	 * @param ctx the parse tree
	 */
	void enterAnalyzestmt(PostgreSqlParser.AnalyzestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#analyzestmt}.
	 * @param ctx the parse tree
	 */
	void exitAnalyzestmt(PostgreSqlParser.AnalyzestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_list}.
	 * @param ctx the parse tree
	 */
	void enterVac_analyze_option_list(PostgreSqlParser.Vac_analyze_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_list}.
	 * @param ctx the parse tree
	 */
	void exitVac_analyze_option_list(PostgreSqlParser.Vac_analyze_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#analyze_keyword}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze_keyword(PostgreSqlParser.Analyze_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#analyze_keyword}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze_keyword(PostgreSqlParser.Analyze_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_elem}.
	 * @param ctx the parse tree
	 */
	void enterVac_analyze_option_elem(PostgreSqlParser.Vac_analyze_option_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_elem}.
	 * @param ctx the parse tree
	 */
	void exitVac_analyze_option_elem(PostgreSqlParser.Vac_analyze_option_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_name}.
	 * @param ctx the parse tree
	 */
	void enterVac_analyze_option_name(PostgreSqlParser.Vac_analyze_option_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_name}.
	 * @param ctx the parse tree
	 */
	void exitVac_analyze_option_name(PostgreSqlParser.Vac_analyze_option_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_arg}.
	 * @param ctx the parse tree
	 */
	void enterVac_analyze_option_arg(PostgreSqlParser.Vac_analyze_option_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_arg}.
	 * @param ctx the parse tree
	 */
	void exitVac_analyze_option_arg(PostgreSqlParser.Vac_analyze_option_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_analyze}.
	 * @param ctx the parse tree
	 */
	void enterOpt_analyze(PostgreSqlParser.Opt_analyzeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_analyze}.
	 * @param ctx the parse tree
	 */
	void exitOpt_analyze(PostgreSqlParser.Opt_analyzeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_verbose}.
	 * @param ctx the parse tree
	 */
	void enterOpt_verbose(PostgreSqlParser.Opt_verboseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_verbose}.
	 * @param ctx the parse tree
	 */
	void exitOpt_verbose(PostgreSqlParser.Opt_verboseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_full}.
	 * @param ctx the parse tree
	 */
	void enterOpt_full(PostgreSqlParser.Opt_fullContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_full}.
	 * @param ctx the parse tree
	 */
	void exitOpt_full(PostgreSqlParser.Opt_fullContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_freeze}.
	 * @param ctx the parse tree
	 */
	void enterOpt_freeze(PostgreSqlParser.Opt_freezeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_freeze}.
	 * @param ctx the parse tree
	 */
	void exitOpt_freeze(PostgreSqlParser.Opt_freezeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_name_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_name_list(PostgreSqlParser.Opt_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_name_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_name_list(PostgreSqlParser.Opt_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#vacuum_relation}.
	 * @param ctx the parse tree
	 */
	void enterVacuum_relation(PostgreSqlParser.Vacuum_relationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#vacuum_relation}.
	 * @param ctx the parse tree
	 */
	void exitVacuum_relation(PostgreSqlParser.Vacuum_relationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#vacuum_relation_list}.
	 * @param ctx the parse tree
	 */
	void enterVacuum_relation_list(PostgreSqlParser.Vacuum_relation_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#vacuum_relation_list}.
	 * @param ctx the parse tree
	 */
	void exitVacuum_relation_list(PostgreSqlParser.Vacuum_relation_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_vacuum_relation_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_vacuum_relation_list(PostgreSqlParser.Opt_vacuum_relation_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_vacuum_relation_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_vacuum_relation_list(PostgreSqlParser.Opt_vacuum_relation_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#explainstmt}.
	 * @param ctx the parse tree
	 */
	void enterExplainstmt(PostgreSqlParser.ExplainstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#explainstmt}.
	 * @param ctx the parse tree
	 */
	void exitExplainstmt(PostgreSqlParser.ExplainstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#explainablestmt}.
	 * @param ctx the parse tree
	 */
	void enterExplainablestmt(PostgreSqlParser.ExplainablestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#explainablestmt}.
	 * @param ctx the parse tree
	 */
	void exitExplainablestmt(PostgreSqlParser.ExplainablestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#explain_option_list}.
	 * @param ctx the parse tree
	 */
	void enterExplain_option_list(PostgreSqlParser.Explain_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#explain_option_list}.
	 * @param ctx the parse tree
	 */
	void exitExplain_option_list(PostgreSqlParser.Explain_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#explain_option_elem}.
	 * @param ctx the parse tree
	 */
	void enterExplain_option_elem(PostgreSqlParser.Explain_option_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#explain_option_elem}.
	 * @param ctx the parse tree
	 */
	void exitExplain_option_elem(PostgreSqlParser.Explain_option_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#explain_option_name}.
	 * @param ctx the parse tree
	 */
	void enterExplain_option_name(PostgreSqlParser.Explain_option_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#explain_option_name}.
	 * @param ctx the parse tree
	 */
	void exitExplain_option_name(PostgreSqlParser.Explain_option_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#explain_option_arg}.
	 * @param ctx the parse tree
	 */
	void enterExplain_option_arg(PostgreSqlParser.Explain_option_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#explain_option_arg}.
	 * @param ctx the parse tree
	 */
	void exitExplain_option_arg(PostgreSqlParser.Explain_option_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#preparestmt}.
	 * @param ctx the parse tree
	 */
	void enterPreparestmt(PostgreSqlParser.PreparestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#preparestmt}.
	 * @param ctx the parse tree
	 */
	void exitPreparestmt(PostgreSqlParser.PreparestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#prep_type_clause}.
	 * @param ctx the parse tree
	 */
	void enterPrep_type_clause(PostgreSqlParser.Prep_type_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#prep_type_clause}.
	 * @param ctx the parse tree
	 */
	void exitPrep_type_clause(PostgreSqlParser.Prep_type_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#preparablestmt}.
	 * @param ctx the parse tree
	 */
	void enterPreparablestmt(PostgreSqlParser.PreparablestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#preparablestmt}.
	 * @param ctx the parse tree
	 */
	void exitPreparablestmt(PostgreSqlParser.PreparablestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#executestmt}.
	 * @param ctx the parse tree
	 */
	void enterExecutestmt(PostgreSqlParser.ExecutestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#executestmt}.
	 * @param ctx the parse tree
	 */
	void exitExecutestmt(PostgreSqlParser.ExecutestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#execute_param_clause}.
	 * @param ctx the parse tree
	 */
	void enterExecute_param_clause(PostgreSqlParser.Execute_param_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#execute_param_clause}.
	 * @param ctx the parse tree
	 */
	void exitExecute_param_clause(PostgreSqlParser.Execute_param_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#deallocatestmt}.
	 * @param ctx the parse tree
	 */
	void enterDeallocatestmt(PostgreSqlParser.DeallocatestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#deallocatestmt}.
	 * @param ctx the parse tree
	 */
	void exitDeallocatestmt(PostgreSqlParser.DeallocatestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#insertstmt}.
	 * @param ctx the parse tree
	 */
	void enterInsertstmt(PostgreSqlParser.InsertstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#insertstmt}.
	 * @param ctx the parse tree
	 */
	void exitInsertstmt(PostgreSqlParser.InsertstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#insert_target}.
	 * @param ctx the parse tree
	 */
	void enterInsert_target(PostgreSqlParser.Insert_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#insert_target}.
	 * @param ctx the parse tree
	 */
	void exitInsert_target(PostgreSqlParser.Insert_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#insert_rest}.
	 * @param ctx the parse tree
	 */
	void enterInsert_rest(PostgreSqlParser.Insert_restContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#insert_rest}.
	 * @param ctx the parse tree
	 */
	void exitInsert_rest(PostgreSqlParser.Insert_restContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#override_kind}.
	 * @param ctx the parse tree
	 */
	void enterOverride_kind(PostgreSqlParser.Override_kindContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#override_kind}.
	 * @param ctx the parse tree
	 */
	void exitOverride_kind(PostgreSqlParser.Override_kindContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#insert_column_list}.
	 * @param ctx the parse tree
	 */
	void enterInsert_column_list(PostgreSqlParser.Insert_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#insert_column_list}.
	 * @param ctx the parse tree
	 */
	void exitInsert_column_list(PostgreSqlParser.Insert_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#insert_column_item}.
	 * @param ctx the parse tree
	 */
	void enterInsert_column_item(PostgreSqlParser.Insert_column_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#insert_column_item}.
	 * @param ctx the parse tree
	 */
	void exitInsert_column_item(PostgreSqlParser.Insert_column_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_on_conflict}.
	 * @param ctx the parse tree
	 */
	void enterOpt_on_conflict(PostgreSqlParser.Opt_on_conflictContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_on_conflict}.
	 * @param ctx the parse tree
	 */
	void exitOpt_on_conflict(PostgreSqlParser.Opt_on_conflictContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_conf_expr}.
	 * @param ctx the parse tree
	 */
	void enterOpt_conf_expr(PostgreSqlParser.Opt_conf_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_conf_expr}.
	 * @param ctx the parse tree
	 */
	void exitOpt_conf_expr(PostgreSqlParser.Opt_conf_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#returning_clause}.
	 * @param ctx the parse tree
	 */
	void enterReturning_clause(PostgreSqlParser.Returning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#returning_clause}.
	 * @param ctx the parse tree
	 */
	void exitReturning_clause(PostgreSqlParser.Returning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#mergestmt}.
	 * @param ctx the parse tree
	 */
	void enterMergestmt(PostgreSqlParser.MergestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#mergestmt}.
	 * @param ctx the parse tree
	 */
	void exitMergestmt(PostgreSqlParser.MergestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_insert_clause(PostgreSqlParser.Merge_insert_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_insert_clause(PostgreSqlParser.Merge_insert_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#merge_update_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_update_clause(PostgreSqlParser.Merge_update_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#merge_update_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_update_clause(PostgreSqlParser.Merge_update_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#merge_delete_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_delete_clause(PostgreSqlParser.Merge_delete_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#merge_delete_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_delete_clause(PostgreSqlParser.Merge_delete_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#deletestmt}.
	 * @param ctx the parse tree
	 */
	void enterDeletestmt(PostgreSqlParser.DeletestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#deletestmt}.
	 * @param ctx the parse tree
	 */
	void exitDeletestmt(PostgreSqlParser.DeletestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#using_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_clause(PostgreSqlParser.Using_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#using_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_clause(PostgreSqlParser.Using_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#lockstmt}.
	 * @param ctx the parse tree
	 */
	void enterLockstmt(PostgreSqlParser.LockstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#lockstmt}.
	 * @param ctx the parse tree
	 */
	void exitLockstmt(PostgreSqlParser.LockstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_lock}.
	 * @param ctx the parse tree
	 */
	void enterOpt_lock(PostgreSqlParser.Opt_lockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_lock}.
	 * @param ctx the parse tree
	 */
	void exitOpt_lock(PostgreSqlParser.Opt_lockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#lock_type}.
	 * @param ctx the parse tree
	 */
	void enterLock_type(PostgreSqlParser.Lock_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#lock_type}.
	 * @param ctx the parse tree
	 */
	void exitLock_type(PostgreSqlParser.Lock_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_nowait}.
	 * @param ctx the parse tree
	 */
	void enterOpt_nowait(PostgreSqlParser.Opt_nowaitContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_nowait}.
	 * @param ctx the parse tree
	 */
	void exitOpt_nowait(PostgreSqlParser.Opt_nowaitContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_nowait_or_skip}.
	 * @param ctx the parse tree
	 */
	void enterOpt_nowait_or_skip(PostgreSqlParser.Opt_nowait_or_skipContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_nowait_or_skip}.
	 * @param ctx the parse tree
	 */
	void exitOpt_nowait_or_skip(PostgreSqlParser.Opt_nowait_or_skipContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#updatestmt}.
	 * @param ctx the parse tree
	 */
	void enterUpdatestmt(PostgreSqlParser.UpdatestmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#updatestmt}.
	 * @param ctx the parse tree
	 */
	void exitUpdatestmt(PostgreSqlParser.UpdatestmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#set_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterSet_clause_list(PostgreSqlParser.Set_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#set_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitSet_clause_list(PostgreSqlParser.Set_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#set_clause}.
	 * @param ctx the parse tree
	 */
	void enterSet_clause(PostgreSqlParser.Set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#set_clause}.
	 * @param ctx the parse tree
	 */
	void exitSet_clause(PostgreSqlParser.Set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#set_target}.
	 * @param ctx the parse tree
	 */
	void enterSet_target(PostgreSqlParser.Set_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#set_target}.
	 * @param ctx the parse tree
	 */
	void exitSet_target(PostgreSqlParser.Set_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#set_target_list}.
	 * @param ctx the parse tree
	 */
	void enterSet_target_list(PostgreSqlParser.Set_target_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#set_target_list}.
	 * @param ctx the parse tree
	 */
	void exitSet_target_list(PostgreSqlParser.Set_target_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#declarecursorstmt}.
	 * @param ctx the parse tree
	 */
	void enterDeclarecursorstmt(PostgreSqlParser.DeclarecursorstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#declarecursorstmt}.
	 * @param ctx the parse tree
	 */
	void exitDeclarecursorstmt(PostgreSqlParser.DeclarecursorstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#cursor_name}.
	 * @param ctx the parse tree
	 */
	void enterCursor_name(PostgreSqlParser.Cursor_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#cursor_name}.
	 * @param ctx the parse tree
	 */
	void exitCursor_name(PostgreSqlParser.Cursor_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#cursor_options}.
	 * @param ctx the parse tree
	 */
	void enterCursor_options(PostgreSqlParser.Cursor_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#cursor_options}.
	 * @param ctx the parse tree
	 */
	void exitCursor_options(PostgreSqlParser.Cursor_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_hold}.
	 * @param ctx the parse tree
	 */
	void enterOpt_hold(PostgreSqlParser.Opt_holdContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_hold}.
	 * @param ctx the parse tree
	 */
	void exitOpt_hold(PostgreSqlParser.Opt_holdContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#selectstmt}.
	 * @param ctx the parse tree
	 */
	void enterSelectstmt(PostgreSqlParser.SelectstmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#selectstmt}.
	 * @param ctx the parse tree
	 */
	void exitSelectstmt(PostgreSqlParser.SelectstmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#select_with_parens}.
	 * @param ctx the parse tree
	 */
	void enterSelect_with_parens(PostgreSqlParser.Select_with_parensContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#select_with_parens}.
	 * @param ctx the parse tree
	 */
	void exitSelect_with_parens(PostgreSqlParser.Select_with_parensContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#select_no_parens}.
	 * @param ctx the parse tree
	 */
	void enterSelect_no_parens(PostgreSqlParser.Select_no_parensContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#select_no_parens}.
	 * @param ctx the parse tree
	 */
	void exitSelect_no_parens(PostgreSqlParser.Select_no_parensContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#select_clause}.
	 * @param ctx the parse tree
	 */
	void enterSelect_clause(PostgreSqlParser.Select_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#select_clause}.
	 * @param ctx the parse tree
	 */
	void exitSelect_clause(PostgreSqlParser.Select_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#simple_select}.
	 * @param ctx the parse tree
	 */
	void enterSimple_select(PostgreSqlParser.Simple_selectContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#simple_select}.
	 * @param ctx the parse tree
	 */
	void exitSimple_select(PostgreSqlParser.Simple_selectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code union}
	 * labeled alternative in {@link PostgreSqlParser#set_operator}.
	 * @param ctx the parse tree
	 */
	void enterUnion(PostgreSqlParser.UnionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code union}
	 * labeled alternative in {@link PostgreSqlParser#set_operator}.
	 * @param ctx the parse tree
	 */
	void exitUnion(PostgreSqlParser.UnionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intersect}
	 * labeled alternative in {@link PostgreSqlParser#set_operator}.
	 * @param ctx the parse tree
	 */
	void enterIntersect(PostgreSqlParser.IntersectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intersect}
	 * labeled alternative in {@link PostgreSqlParser#set_operator}.
	 * @param ctx the parse tree
	 */
	void exitIntersect(PostgreSqlParser.IntersectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code except}
	 * labeled alternative in {@link PostgreSqlParser#set_operator}.
	 * @param ctx the parse tree
	 */
	void enterExcept(PostgreSqlParser.ExceptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code except}
	 * labeled alternative in {@link PostgreSqlParser#set_operator}.
	 * @param ctx the parse tree
	 */
	void exitExcept(PostgreSqlParser.ExceptContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#set_operator_with_all_or_distinct}.
	 * @param ctx the parse tree
	 */
	void enterSet_operator_with_all_or_distinct(PostgreSqlParser.Set_operator_with_all_or_distinctContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#set_operator_with_all_or_distinct}.
	 * @param ctx the parse tree
	 */
	void exitSet_operator_with_all_or_distinct(PostgreSqlParser.Set_operator_with_all_or_distinctContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#with_clause}.
	 * @param ctx the parse tree
	 */
	void enterWith_clause(PostgreSqlParser.With_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#with_clause}.
	 * @param ctx the parse tree
	 */
	void exitWith_clause(PostgreSqlParser.With_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#cte_list}.
	 * @param ctx the parse tree
	 */
	void enterCte_list(PostgreSqlParser.Cte_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#cte_list}.
	 * @param ctx the parse tree
	 */
	void exitCte_list(PostgreSqlParser.Cte_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#common_table_expr}.
	 * @param ctx the parse tree
	 */
	void enterCommon_table_expr(PostgreSqlParser.Common_table_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#common_table_expr}.
	 * @param ctx the parse tree
	 */
	void exitCommon_table_expr(PostgreSqlParser.Common_table_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_materialized}.
	 * @param ctx the parse tree
	 */
	void enterOpt_materialized(PostgreSqlParser.Opt_materializedContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_materialized}.
	 * @param ctx the parse tree
	 */
	void exitOpt_materialized(PostgreSqlParser.Opt_materializedContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_with_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_with_clause(PostgreSqlParser.Opt_with_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_with_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_with_clause(PostgreSqlParser.Opt_with_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void enterInto_clause(PostgreSqlParser.Into_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void exitInto_clause(PostgreSqlParser.Into_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_strict}.
	 * @param ctx the parse tree
	 */
	void enterOpt_strict(PostgreSqlParser.Opt_strictContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_strict}.
	 * @param ctx the parse tree
	 */
	void exitOpt_strict(PostgreSqlParser.Opt_strictContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opttempTableName}.
	 * @param ctx the parse tree
	 */
	void enterOpttempTableName(PostgreSqlParser.OpttempTableNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opttempTableName}.
	 * @param ctx the parse tree
	 */
	void exitOpttempTableName(PostgreSqlParser.OpttempTableNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_table}.
	 * @param ctx the parse tree
	 */
	void enterOpt_table(PostgreSqlParser.Opt_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_table}.
	 * @param ctx the parse tree
	 */
	void exitOpt_table(PostgreSqlParser.Opt_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#all_or_distinct}.
	 * @param ctx the parse tree
	 */
	void enterAll_or_distinct(PostgreSqlParser.All_or_distinctContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#all_or_distinct}.
	 * @param ctx the parse tree
	 */
	void exitAll_or_distinct(PostgreSqlParser.All_or_distinctContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#distinct_clause}.
	 * @param ctx the parse tree
	 */
	void enterDistinct_clause(PostgreSqlParser.Distinct_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#distinct_clause}.
	 * @param ctx the parse tree
	 */
	void exitDistinct_clause(PostgreSqlParser.Distinct_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_all_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_all_clause(PostgreSqlParser.Opt_all_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_all_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_all_clause(PostgreSqlParser.Opt_all_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_sort_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_sort_clause(PostgreSqlParser.Opt_sort_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_sort_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_sort_clause(PostgreSqlParser.Opt_sort_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#sort_clause}.
	 * @param ctx the parse tree
	 */
	void enterSort_clause(PostgreSqlParser.Sort_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#sort_clause}.
	 * @param ctx the parse tree
	 */
	void exitSort_clause(PostgreSqlParser.Sort_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#sortby_list}.
	 * @param ctx the parse tree
	 */
	void enterSortby_list(PostgreSqlParser.Sortby_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#sortby_list}.
	 * @param ctx the parse tree
	 */
	void exitSortby_list(PostgreSqlParser.Sortby_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#sortby}.
	 * @param ctx the parse tree
	 */
	void enterSortby(PostgreSqlParser.SortbyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#sortby}.
	 * @param ctx the parse tree
	 */
	void exitSortby(PostgreSqlParser.SortbyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#select_limit}.
	 * @param ctx the parse tree
	 */
	void enterSelect_limit(PostgreSqlParser.Select_limitContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#select_limit}.
	 * @param ctx the parse tree
	 */
	void exitSelect_limit(PostgreSqlParser.Select_limitContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_select_limit}.
	 * @param ctx the parse tree
	 */
	void enterOpt_select_limit(PostgreSqlParser.Opt_select_limitContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_select_limit}.
	 * @param ctx the parse tree
	 */
	void exitOpt_select_limit(PostgreSqlParser.Opt_select_limitContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#limit_clause}.
	 * @param ctx the parse tree
	 */
	void enterLimit_clause(PostgreSqlParser.Limit_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#limit_clause}.
	 * @param ctx the parse tree
	 */
	void exitLimit_clause(PostgreSqlParser.Limit_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#offset_clause}.
	 * @param ctx the parse tree
	 */
	void enterOffset_clause(PostgreSqlParser.Offset_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#offset_clause}.
	 * @param ctx the parse tree
	 */
	void exitOffset_clause(PostgreSqlParser.Offset_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#select_limit_value}.
	 * @param ctx the parse tree
	 */
	void enterSelect_limit_value(PostgreSqlParser.Select_limit_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#select_limit_value}.
	 * @param ctx the parse tree
	 */
	void exitSelect_limit_value(PostgreSqlParser.Select_limit_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#select_offset_value}.
	 * @param ctx the parse tree
	 */
	void enterSelect_offset_value(PostgreSqlParser.Select_offset_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#select_offset_value}.
	 * @param ctx the parse tree
	 */
	void exitSelect_offset_value(PostgreSqlParser.Select_offset_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#select_fetch_first_value}.
	 * @param ctx the parse tree
	 */
	void enterSelect_fetch_first_value(PostgreSqlParser.Select_fetch_first_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#select_fetch_first_value}.
	 * @param ctx the parse tree
	 */
	void exitSelect_fetch_first_value(PostgreSqlParser.Select_fetch_first_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#i_or_f_const}.
	 * @param ctx the parse tree
	 */
	void enterI_or_f_const(PostgreSqlParser.I_or_f_constContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#i_or_f_const}.
	 * @param ctx the parse tree
	 */
	void exitI_or_f_const(PostgreSqlParser.I_or_f_constContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#row_or_rows}.
	 * @param ctx the parse tree
	 */
	void enterRow_or_rows(PostgreSqlParser.Row_or_rowsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#row_or_rows}.
	 * @param ctx the parse tree
	 */
	void exitRow_or_rows(PostgreSqlParser.Row_or_rowsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#first_or_next}.
	 * @param ctx the parse tree
	 */
	void enterFirst_or_next(PostgreSqlParser.First_or_nextContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#first_or_next}.
	 * @param ctx the parse tree
	 */
	void exitFirst_or_next(PostgreSqlParser.First_or_nextContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#group_clause}.
	 * @param ctx the parse tree
	 */
	void enterGroup_clause(PostgreSqlParser.Group_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#group_clause}.
	 * @param ctx the parse tree
	 */
	void exitGroup_clause(PostgreSqlParser.Group_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#group_by_list}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_list(PostgreSqlParser.Group_by_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#group_by_list}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_list(PostgreSqlParser.Group_by_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#group_by_item}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_item(PostgreSqlParser.Group_by_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#group_by_item}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_item(PostgreSqlParser.Group_by_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#empty_grouping_set}.
	 * @param ctx the parse tree
	 */
	void enterEmpty_grouping_set(PostgreSqlParser.Empty_grouping_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#empty_grouping_set}.
	 * @param ctx the parse tree
	 */
	void exitEmpty_grouping_set(PostgreSqlParser.Empty_grouping_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rollup_clause}.
	 * @param ctx the parse tree
	 */
	void enterRollup_clause(PostgreSqlParser.Rollup_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rollup_clause}.
	 * @param ctx the parse tree
	 */
	void exitRollup_clause(PostgreSqlParser.Rollup_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#cube_clause}.
	 * @param ctx the parse tree
	 */
	void enterCube_clause(PostgreSqlParser.Cube_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#cube_clause}.
	 * @param ctx the parse tree
	 */
	void exitCube_clause(PostgreSqlParser.Cube_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets_clause(PostgreSqlParser.Grouping_sets_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets_clause(PostgreSqlParser.Grouping_sets_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#having_clause}.
	 * @param ctx the parse tree
	 */
	void enterHaving_clause(PostgreSqlParser.Having_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#having_clause}.
	 * @param ctx the parse tree
	 */
	void exitHaving_clause(PostgreSqlParser.Having_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#for_locking_clause}.
	 * @param ctx the parse tree
	 */
	void enterFor_locking_clause(PostgreSqlParser.For_locking_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#for_locking_clause}.
	 * @param ctx the parse tree
	 */
	void exitFor_locking_clause(PostgreSqlParser.For_locking_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_for_locking_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_for_locking_clause(PostgreSqlParser.Opt_for_locking_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_for_locking_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_for_locking_clause(PostgreSqlParser.Opt_for_locking_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#for_locking_items}.
	 * @param ctx the parse tree
	 */
	void enterFor_locking_items(PostgreSqlParser.For_locking_itemsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#for_locking_items}.
	 * @param ctx the parse tree
	 */
	void exitFor_locking_items(PostgreSqlParser.For_locking_itemsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#for_locking_item}.
	 * @param ctx the parse tree
	 */
	void enterFor_locking_item(PostgreSqlParser.For_locking_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#for_locking_item}.
	 * @param ctx the parse tree
	 */
	void exitFor_locking_item(PostgreSqlParser.For_locking_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#for_locking_strength}.
	 * @param ctx the parse tree
	 */
	void enterFor_locking_strength(PostgreSqlParser.For_locking_strengthContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#for_locking_strength}.
	 * @param ctx the parse tree
	 */
	void exitFor_locking_strength(PostgreSqlParser.For_locking_strengthContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#locked_rels_list}.
	 * @param ctx the parse tree
	 */
	void enterLocked_rels_list(PostgreSqlParser.Locked_rels_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#locked_rels_list}.
	 * @param ctx the parse tree
	 */
	void exitLocked_rels_list(PostgreSqlParser.Locked_rels_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#values_clause}.
	 * @param ctx the parse tree
	 */
	void enterValues_clause(PostgreSqlParser.Values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#values_clause}.
	 * @param ctx the parse tree
	 */
	void exitValues_clause(PostgreSqlParser.Values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#from_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_clause(PostgreSqlParser.From_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#from_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_clause(PostgreSqlParser.From_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#from_list}.
	 * @param ctx the parse tree
	 */
	void enterFrom_list(PostgreSqlParser.From_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#from_list}.
	 * @param ctx the parse tree
	 */
	void exitFrom_list(PostgreSqlParser.From_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#non_ansi_join}.
	 * @param ctx the parse tree
	 */
	void enterNon_ansi_join(PostgreSqlParser.Non_ansi_joinContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#non_ansi_join}.
	 * @param ctx the parse tree
	 */
	void exitNon_ansi_join(PostgreSqlParser.Non_ansi_joinContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#table_ref}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref(PostgreSqlParser.Table_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#table_ref}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref(PostgreSqlParser.Table_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#alias_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlias_clause(PostgreSqlParser.Alias_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#alias_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlias_clause(PostgreSqlParser.Alias_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_alias_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_alias_clause(PostgreSqlParser.Opt_alias_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_alias_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_alias_clause(PostgreSqlParser.Opt_alias_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#table_alias_clause}.
	 * @param ctx the parse tree
	 */
	void enterTable_alias_clause(PostgreSqlParser.Table_alias_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#table_alias_clause}.
	 * @param ctx the parse tree
	 */
	void exitTable_alias_clause(PostgreSqlParser.Table_alias_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_alias_clause}.
	 * @param ctx the parse tree
	 */
	void enterFunc_alias_clause(PostgreSqlParser.Func_alias_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_alias_clause}.
	 * @param ctx the parse tree
	 */
	void exitFunc_alias_clause(PostgreSqlParser.Func_alias_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#join_type}.
	 * @param ctx the parse tree
	 */
	void enterJoin_type(PostgreSqlParser.Join_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#join_type}.
	 * @param ctx the parse tree
	 */
	void exitJoin_type(PostgreSqlParser.Join_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#join_qual}.
	 * @param ctx the parse tree
	 */
	void enterJoin_qual(PostgreSqlParser.Join_qualContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#join_qual}.
	 * @param ctx the parse tree
	 */
	void exitJoin_qual(PostgreSqlParser.Join_qualContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#relation_expr}.
	 * @param ctx the parse tree
	 */
	void enterRelation_expr(PostgreSqlParser.Relation_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#relation_expr}.
	 * @param ctx the parse tree
	 */
	void exitRelation_expr(PostgreSqlParser.Relation_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#relation_expr_list}.
	 * @param ctx the parse tree
	 */
	void enterRelation_expr_list(PostgreSqlParser.Relation_expr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#relation_expr_list}.
	 * @param ctx the parse tree
	 */
	void exitRelation_expr_list(PostgreSqlParser.Relation_expr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#relation_expr_opt_alias}.
	 * @param ctx the parse tree
	 */
	void enterRelation_expr_opt_alias(PostgreSqlParser.Relation_expr_opt_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#relation_expr_opt_alias}.
	 * @param ctx the parse tree
	 */
	void exitRelation_expr_opt_alias(PostgreSqlParser.Relation_expr_opt_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#tablesample_clause}.
	 * @param ctx the parse tree
	 */
	void enterTablesample_clause(PostgreSqlParser.Tablesample_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#tablesample_clause}.
	 * @param ctx the parse tree
	 */
	void exitTablesample_clause(PostgreSqlParser.Tablesample_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_repeatable_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_repeatable_clause(PostgreSqlParser.Opt_repeatable_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_repeatable_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_repeatable_clause(PostgreSqlParser.Opt_repeatable_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_table}.
	 * @param ctx the parse tree
	 */
	void enterFunc_table(PostgreSqlParser.Func_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_table}.
	 * @param ctx the parse tree
	 */
	void exitFunc_table(PostgreSqlParser.Func_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rowsfrom_item}.
	 * @param ctx the parse tree
	 */
	void enterRowsfrom_item(PostgreSqlParser.Rowsfrom_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rowsfrom_item}.
	 * @param ctx the parse tree
	 */
	void exitRowsfrom_item(PostgreSqlParser.Rowsfrom_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rowsfrom_list}.
	 * @param ctx the parse tree
	 */
	void enterRowsfrom_list(PostgreSqlParser.Rowsfrom_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rowsfrom_list}.
	 * @param ctx the parse tree
	 */
	void exitRowsfrom_list(PostgreSqlParser.Rowsfrom_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_col_def_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_col_def_list(PostgreSqlParser.Opt_col_def_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_col_def_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_col_def_list(PostgreSqlParser.Opt_col_def_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_ordinality}.
	 * @param ctx the parse tree
	 */
	void enterOpt_ordinality(PostgreSqlParser.Opt_ordinalityContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_ordinality}.
	 * @param ctx the parse tree
	 */
	void exitOpt_ordinality(PostgreSqlParser.Opt_ordinalityContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#where_clause}.
	 * @param ctx the parse tree
	 */
	void enterWhere_clause(PostgreSqlParser.Where_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#where_clause}.
	 * @param ctx the parse tree
	 */
	void exitWhere_clause(PostgreSqlParser.Where_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#where_or_current_clause}.
	 * @param ctx the parse tree
	 */
	void enterWhere_or_current_clause(PostgreSqlParser.Where_or_current_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#where_or_current_clause}.
	 * @param ctx the parse tree
	 */
	void exitWhere_or_current_clause(PostgreSqlParser.Where_or_current_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opttablefuncelementlist}.
	 * @param ctx the parse tree
	 */
	void enterOpttablefuncelementlist(PostgreSqlParser.OpttablefuncelementlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opttablefuncelementlist}.
	 * @param ctx the parse tree
	 */
	void exitOpttablefuncelementlist(PostgreSqlParser.OpttablefuncelementlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#tablefuncelementlist}.
	 * @param ctx the parse tree
	 */
	void enterTablefuncelementlist(PostgreSqlParser.TablefuncelementlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#tablefuncelementlist}.
	 * @param ctx the parse tree
	 */
	void exitTablefuncelementlist(PostgreSqlParser.TablefuncelementlistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#tablefuncelement}.
	 * @param ctx the parse tree
	 */
	void enterTablefuncelement(PostgreSqlParser.TablefuncelementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#tablefuncelement}.
	 * @param ctx the parse tree
	 */
	void exitTablefuncelement(PostgreSqlParser.TablefuncelementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xmltable}.
	 * @param ctx the parse tree
	 */
	void enterXmltable(PostgreSqlParser.XmltableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xmltable}.
	 * @param ctx the parse tree
	 */
	void exitXmltable(PostgreSqlParser.XmltableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xmltable_column_list}.
	 * @param ctx the parse tree
	 */
	void enterXmltable_column_list(PostgreSqlParser.Xmltable_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xmltable_column_list}.
	 * @param ctx the parse tree
	 */
	void exitXmltable_column_list(PostgreSqlParser.Xmltable_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xmltable_column_el}.
	 * @param ctx the parse tree
	 */
	void enterXmltable_column_el(PostgreSqlParser.Xmltable_column_elContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xmltable_column_el}.
	 * @param ctx the parse tree
	 */
	void exitXmltable_column_el(PostgreSqlParser.Xmltable_column_elContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xmltable_column_option_list}.
	 * @param ctx the parse tree
	 */
	void enterXmltable_column_option_list(PostgreSqlParser.Xmltable_column_option_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xmltable_column_option_list}.
	 * @param ctx the parse tree
	 */
	void exitXmltable_column_option_list(PostgreSqlParser.Xmltable_column_option_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xmltable_column_option_el}.
	 * @param ctx the parse tree
	 */
	void enterXmltable_column_option_el(PostgreSqlParser.Xmltable_column_option_elContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xmltable_column_option_el}.
	 * @param ctx the parse tree
	 */
	void exitXmltable_column_option_el(PostgreSqlParser.Xmltable_column_option_elContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xml_namespace_list}.
	 * @param ctx the parse tree
	 */
	void enterXml_namespace_list(PostgreSqlParser.Xml_namespace_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xml_namespace_list}.
	 * @param ctx the parse tree
	 */
	void exitXml_namespace_list(PostgreSqlParser.Xml_namespace_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xml_namespace_el}.
	 * @param ctx the parse tree
	 */
	void enterXml_namespace_el(PostgreSqlParser.Xml_namespace_elContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xml_namespace_el}.
	 * @param ctx the parse tree
	 */
	void exitXml_namespace_el(PostgreSqlParser.Xml_namespace_elContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#typename}.
	 * @param ctx the parse tree
	 */
	void enterTypename(PostgreSqlParser.TypenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#typename}.
	 * @param ctx the parse tree
	 */
	void exitTypename(PostgreSqlParser.TypenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_array_bounds}.
	 * @param ctx the parse tree
	 */
	void enterOpt_array_bounds(PostgreSqlParser.Opt_array_boundsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_array_bounds}.
	 * @param ctx the parse tree
	 */
	void exitOpt_array_bounds(PostgreSqlParser.Opt_array_boundsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#simpletypename}.
	 * @param ctx the parse tree
	 */
	void enterSimpletypename(PostgreSqlParser.SimpletypenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#simpletypename}.
	 * @param ctx the parse tree
	 */
	void exitSimpletypename(PostgreSqlParser.SimpletypenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#consttypename}.
	 * @param ctx the parse tree
	 */
	void enterConsttypename(PostgreSqlParser.ConsttypenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#consttypename}.
	 * @param ctx the parse tree
	 */
	void exitConsttypename(PostgreSqlParser.ConsttypenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#generictype}.
	 * @param ctx the parse tree
	 */
	void enterGenerictype(PostgreSqlParser.GenerictypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#generictype}.
	 * @param ctx the parse tree
	 */
	void exitGenerictype(PostgreSqlParser.GenerictypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_type_modifiers}.
	 * @param ctx the parse tree
	 */
	void enterOpt_type_modifiers(PostgreSqlParser.Opt_type_modifiersContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_type_modifiers}.
	 * @param ctx the parse tree
	 */
	void exitOpt_type_modifiers(PostgreSqlParser.Opt_type_modifiersContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#numeric}.
	 * @param ctx the parse tree
	 */
	void enterNumeric(PostgreSqlParser.NumericContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#numeric}.
	 * @param ctx the parse tree
	 */
	void exitNumeric(PostgreSqlParser.NumericContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_float}.
	 * @param ctx the parse tree
	 */
	void enterOpt_float(PostgreSqlParser.Opt_floatContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_float}.
	 * @param ctx the parse tree
	 */
	void exitOpt_float(PostgreSqlParser.Opt_floatContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#bit}.
	 * @param ctx the parse tree
	 */
	void enterBit(PostgreSqlParser.BitContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#bit}.
	 * @param ctx the parse tree
	 */
	void exitBit(PostgreSqlParser.BitContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constbit}.
	 * @param ctx the parse tree
	 */
	void enterConstbit(PostgreSqlParser.ConstbitContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constbit}.
	 * @param ctx the parse tree
	 */
	void exitConstbit(PostgreSqlParser.ConstbitContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#bitwithlength}.
	 * @param ctx the parse tree
	 */
	void enterBitwithlength(PostgreSqlParser.BitwithlengthContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#bitwithlength}.
	 * @param ctx the parse tree
	 */
	void exitBitwithlength(PostgreSqlParser.BitwithlengthContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#bitwithoutlength}.
	 * @param ctx the parse tree
	 */
	void enterBitwithoutlength(PostgreSqlParser.BitwithoutlengthContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#bitwithoutlength}.
	 * @param ctx the parse tree
	 */
	void exitBitwithoutlength(PostgreSqlParser.BitwithoutlengthContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#character}.
	 * @param ctx the parse tree
	 */
	void enterCharacter(PostgreSqlParser.CharacterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#character}.
	 * @param ctx the parse tree
	 */
	void exitCharacter(PostgreSqlParser.CharacterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constcharacter}.
	 * @param ctx the parse tree
	 */
	void enterConstcharacter(PostgreSqlParser.ConstcharacterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constcharacter}.
	 * @param ctx the parse tree
	 */
	void exitConstcharacter(PostgreSqlParser.ConstcharacterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#character_c}.
	 * @param ctx the parse tree
	 */
	void enterCharacter_c(PostgreSqlParser.Character_cContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#character_c}.
	 * @param ctx the parse tree
	 */
	void exitCharacter_c(PostgreSqlParser.Character_cContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_varying}.
	 * @param ctx the parse tree
	 */
	void enterOpt_varying(PostgreSqlParser.Opt_varyingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_varying}.
	 * @param ctx the parse tree
	 */
	void exitOpt_varying(PostgreSqlParser.Opt_varyingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constdatetime}.
	 * @param ctx the parse tree
	 */
	void enterConstdatetime(PostgreSqlParser.ConstdatetimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constdatetime}.
	 * @param ctx the parse tree
	 */
	void exitConstdatetime(PostgreSqlParser.ConstdatetimeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#constinterval}.
	 * @param ctx the parse tree
	 */
	void enterConstinterval(PostgreSqlParser.ConstintervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#constinterval}.
	 * @param ctx the parse tree
	 */
	void exitConstinterval(PostgreSqlParser.ConstintervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_timezone}.
	 * @param ctx the parse tree
	 */
	void enterOpt_timezone(PostgreSqlParser.Opt_timezoneContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_timezone}.
	 * @param ctx the parse tree
	 */
	void exitOpt_timezone(PostgreSqlParser.Opt_timezoneContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_interval}.
	 * @param ctx the parse tree
	 */
	void enterOpt_interval(PostgreSqlParser.Opt_intervalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_interval}.
	 * @param ctx the parse tree
	 */
	void exitOpt_interval(PostgreSqlParser.Opt_intervalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#interval_second}.
	 * @param ctx the parse tree
	 */
	void enterInterval_second(PostgreSqlParser.Interval_secondContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#interval_second}.
	 * @param ctx the parse tree
	 */
	void exitInterval_second(PostgreSqlParser.Interval_secondContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_escape}.
	 * @param ctx the parse tree
	 */
	void enterOpt_escape(PostgreSqlParser.Opt_escapeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_escape}.
	 * @param ctx the parse tree
	 */
	void exitOpt_escape(PostgreSqlParser.Opt_escapeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr}.
	 * @param ctx the parse tree
	 */
	void enterA_expr(PostgreSqlParser.A_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr}.
	 * @param ctx the parse tree
	 */
	void exitA_expr(PostgreSqlParser.A_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_qual}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_qual(PostgreSqlParser.A_expr_qualContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_qual}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_qual(PostgreSqlParser.A_expr_qualContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_lessless}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_lessless(PostgreSqlParser.A_expr_lesslessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_lessless}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_lessless(PostgreSqlParser.A_expr_lesslessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_or}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_or(PostgreSqlParser.A_expr_orContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_or}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_or(PostgreSqlParser.A_expr_orContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_and}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_and(PostgreSqlParser.A_expr_andContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_and}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_and(PostgreSqlParser.A_expr_andContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_between}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_between(PostgreSqlParser.A_expr_betweenContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_between}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_between(PostgreSqlParser.A_expr_betweenContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_in}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_in(PostgreSqlParser.A_expr_inContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_in}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_in(PostgreSqlParser.A_expr_inContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_unary_not}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_unary_not(PostgreSqlParser.A_expr_unary_notContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_unary_not}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_unary_not(PostgreSqlParser.A_expr_unary_notContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_isnull}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_isnull(PostgreSqlParser.A_expr_isnullContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_isnull}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_isnull(PostgreSqlParser.A_expr_isnullContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_is_not}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_is_not(PostgreSqlParser.A_expr_is_notContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_is_not}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_is_not(PostgreSqlParser.A_expr_is_notContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_compare}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_compare(PostgreSqlParser.A_expr_compareContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_compare}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_compare(PostgreSqlParser.A_expr_compareContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_like}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_like(PostgreSqlParser.A_expr_likeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_like}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_like(PostgreSqlParser.A_expr_likeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_qual_op}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_qual_op(PostgreSqlParser.A_expr_qual_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_qual_op}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_qual_op(PostgreSqlParser.A_expr_qual_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_unary_qualop}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_unary_qualop(PostgreSqlParser.A_expr_unary_qualopContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_unary_qualop}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_unary_qualop(PostgreSqlParser.A_expr_unary_qualopContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_add}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_add(PostgreSqlParser.A_expr_addContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_add}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_add(PostgreSqlParser.A_expr_addContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_mul}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_mul(PostgreSqlParser.A_expr_mulContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_mul}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_mul(PostgreSqlParser.A_expr_mulContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_caret}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_caret(PostgreSqlParser.A_expr_caretContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_caret}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_caret(PostgreSqlParser.A_expr_caretContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_unary_sign}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_unary_sign(PostgreSqlParser.A_expr_unary_signContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_unary_sign}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_unary_sign(PostgreSqlParser.A_expr_unary_signContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_at_time_zone}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_at_time_zone(PostgreSqlParser.A_expr_at_time_zoneContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_at_time_zone}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_at_time_zone(PostgreSqlParser.A_expr_at_time_zoneContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_collate}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_collate(PostgreSqlParser.A_expr_collateContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_collate}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_collate(PostgreSqlParser.A_expr_collateContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#a_expr_typecast}.
	 * @param ctx the parse tree
	 */
	void enterA_expr_typecast(PostgreSqlParser.A_expr_typecastContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#a_expr_typecast}.
	 * @param ctx the parse tree
	 */
	void exitA_expr_typecast(PostgreSqlParser.A_expr_typecastContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#b_expr}.
	 * @param ctx the parse tree
	 */
	void enterB_expr(PostgreSqlParser.B_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#b_expr}.
	 * @param ctx the parse tree
	 */
	void exitB_expr(PostgreSqlParser.B_exprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code c_expr_exists}
	 * labeled alternative in {@link PostgreSqlParser#c_expr}.
	 * @param ctx the parse tree
	 */
	void enterC_expr_exists(PostgreSqlParser.C_expr_existsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code c_expr_exists}
	 * labeled alternative in {@link PostgreSqlParser#c_expr}.
	 * @param ctx the parse tree
	 */
	void exitC_expr_exists(PostgreSqlParser.C_expr_existsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code c_expr_expr}
	 * labeled alternative in {@link PostgreSqlParser#c_expr}.
	 * @param ctx the parse tree
	 */
	void enterC_expr_expr(PostgreSqlParser.C_expr_exprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code c_expr_expr}
	 * labeled alternative in {@link PostgreSqlParser#c_expr}.
	 * @param ctx the parse tree
	 */
	void exitC_expr_expr(PostgreSqlParser.C_expr_exprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code c_expr_case}
	 * labeled alternative in {@link PostgreSqlParser#c_expr}.
	 * @param ctx the parse tree
	 */
	void enterC_expr_case(PostgreSqlParser.C_expr_caseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code c_expr_case}
	 * labeled alternative in {@link PostgreSqlParser#c_expr}.
	 * @param ctx the parse tree
	 */
	void exitC_expr_case(PostgreSqlParser.C_expr_caseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#plsqlvariablename}.
	 * @param ctx the parse tree
	 */
	void enterPlsqlvariablename(PostgreSqlParser.PlsqlvariablenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#plsqlvariablename}.
	 * @param ctx the parse tree
	 */
	void exitPlsqlvariablename(PostgreSqlParser.PlsqlvariablenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_application}.
	 * @param ctx the parse tree
	 */
	void enterFunc_application(PostgreSqlParser.Func_applicationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_application}.
	 * @param ctx the parse tree
	 */
	void exitFunc_application(PostgreSqlParser.Func_applicationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_expr}.
	 * @param ctx the parse tree
	 */
	void enterFunc_expr(PostgreSqlParser.Func_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_expr}.
	 * @param ctx the parse tree
	 */
	void exitFunc_expr(PostgreSqlParser.Func_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_expr_windowless}.
	 * @param ctx the parse tree
	 */
	void enterFunc_expr_windowless(PostgreSqlParser.Func_expr_windowlessContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_expr_windowless}.
	 * @param ctx the parse tree
	 */
	void exitFunc_expr_windowless(PostgreSqlParser.Func_expr_windowlessContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_expr_common_subexpr}.
	 * @param ctx the parse tree
	 */
	void enterFunc_expr_common_subexpr(PostgreSqlParser.Func_expr_common_subexprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_expr_common_subexpr}.
	 * @param ctx the parse tree
	 */
	void exitFunc_expr_common_subexpr(PostgreSqlParser.Func_expr_common_subexprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xml_root_version}.
	 * @param ctx the parse tree
	 */
	void enterXml_root_version(PostgreSqlParser.Xml_root_versionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xml_root_version}.
	 * @param ctx the parse tree
	 */
	void exitXml_root_version(PostgreSqlParser.Xml_root_versionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_xml_root_standalone}.
	 * @param ctx the parse tree
	 */
	void enterOpt_xml_root_standalone(PostgreSqlParser.Opt_xml_root_standaloneContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_xml_root_standalone}.
	 * @param ctx the parse tree
	 */
	void exitOpt_xml_root_standalone(PostgreSqlParser.Opt_xml_root_standaloneContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xml_attributes}.
	 * @param ctx the parse tree
	 */
	void enterXml_attributes(PostgreSqlParser.Xml_attributesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xml_attributes}.
	 * @param ctx the parse tree
	 */
	void exitXml_attributes(PostgreSqlParser.Xml_attributesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xml_attribute_list}.
	 * @param ctx the parse tree
	 */
	void enterXml_attribute_list(PostgreSqlParser.Xml_attribute_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xml_attribute_list}.
	 * @param ctx the parse tree
	 */
	void exitXml_attribute_list(PostgreSqlParser.Xml_attribute_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xml_attribute_el}.
	 * @param ctx the parse tree
	 */
	void enterXml_attribute_el(PostgreSqlParser.Xml_attribute_elContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xml_attribute_el}.
	 * @param ctx the parse tree
	 */
	void exitXml_attribute_el(PostgreSqlParser.Xml_attribute_elContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#document_or_content}.
	 * @param ctx the parse tree
	 */
	void enterDocument_or_content(PostgreSqlParser.Document_or_contentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#document_or_content}.
	 * @param ctx the parse tree
	 */
	void exitDocument_or_content(PostgreSqlParser.Document_or_contentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xml_whitespace_option}.
	 * @param ctx the parse tree
	 */
	void enterXml_whitespace_option(PostgreSqlParser.Xml_whitespace_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xml_whitespace_option}.
	 * @param ctx the parse tree
	 */
	void exitXml_whitespace_option(PostgreSqlParser.Xml_whitespace_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xmlexists_argument}.
	 * @param ctx the parse tree
	 */
	void enterXmlexists_argument(PostgreSqlParser.Xmlexists_argumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xmlexists_argument}.
	 * @param ctx the parse tree
	 */
	void exitXmlexists_argument(PostgreSqlParser.Xmlexists_argumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xml_passing_mech}.
	 * @param ctx the parse tree
	 */
	void enterXml_passing_mech(PostgreSqlParser.Xml_passing_mechContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xml_passing_mech}.
	 * @param ctx the parse tree
	 */
	void exitXml_passing_mech(PostgreSqlParser.Xml_passing_mechContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#within_group_clause}.
	 * @param ctx the parse tree
	 */
	void enterWithin_group_clause(PostgreSqlParser.Within_group_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#within_group_clause}.
	 * @param ctx the parse tree
	 */
	void exitWithin_group_clause(PostgreSqlParser.Within_group_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#filter_clause}.
	 * @param ctx the parse tree
	 */
	void enterFilter_clause(PostgreSqlParser.Filter_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#filter_clause}.
	 * @param ctx the parse tree
	 */
	void exitFilter_clause(PostgreSqlParser.Filter_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#window_clause}.
	 * @param ctx the parse tree
	 */
	void enterWindow_clause(PostgreSqlParser.Window_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#window_clause}.
	 * @param ctx the parse tree
	 */
	void exitWindow_clause(PostgreSqlParser.Window_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#window_definition_list}.
	 * @param ctx the parse tree
	 */
	void enterWindow_definition_list(PostgreSqlParser.Window_definition_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#window_definition_list}.
	 * @param ctx the parse tree
	 */
	void exitWindow_definition_list(PostgreSqlParser.Window_definition_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#window_definition}.
	 * @param ctx the parse tree
	 */
	void enterWindow_definition(PostgreSqlParser.Window_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#window_definition}.
	 * @param ctx the parse tree
	 */
	void exitWindow_definition(PostgreSqlParser.Window_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#over_clause}.
	 * @param ctx the parse tree
	 */
	void enterOver_clause(PostgreSqlParser.Over_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#over_clause}.
	 * @param ctx the parse tree
	 */
	void exitOver_clause(PostgreSqlParser.Over_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#window_specification}.
	 * @param ctx the parse tree
	 */
	void enterWindow_specification(PostgreSqlParser.Window_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#window_specification}.
	 * @param ctx the parse tree
	 */
	void exitWindow_specification(PostgreSqlParser.Window_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_existing_window_name}.
	 * @param ctx the parse tree
	 */
	void enterOpt_existing_window_name(PostgreSqlParser.Opt_existing_window_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_existing_window_name}.
	 * @param ctx the parse tree
	 */
	void exitOpt_existing_window_name(PostgreSqlParser.Opt_existing_window_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_partition_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_partition_clause(PostgreSqlParser.Opt_partition_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_partition_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_partition_clause(PostgreSqlParser.Opt_partition_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_frame_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_frame_clause(PostgreSqlParser.Opt_frame_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_frame_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_frame_clause(PostgreSqlParser.Opt_frame_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#frame_extent}.
	 * @param ctx the parse tree
	 */
	void enterFrame_extent(PostgreSqlParser.Frame_extentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#frame_extent}.
	 * @param ctx the parse tree
	 */
	void exitFrame_extent(PostgreSqlParser.Frame_extentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#frame_bound}.
	 * @param ctx the parse tree
	 */
	void enterFrame_bound(PostgreSqlParser.Frame_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#frame_bound}.
	 * @param ctx the parse tree
	 */
	void exitFrame_bound(PostgreSqlParser.Frame_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_window_exclusion_clause}.
	 * @param ctx the parse tree
	 */
	void enterOpt_window_exclusion_clause(PostgreSqlParser.Opt_window_exclusion_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_window_exclusion_clause}.
	 * @param ctx the parse tree
	 */
	void exitOpt_window_exclusion_clause(PostgreSqlParser.Opt_window_exclusion_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#row}.
	 * @param ctx the parse tree
	 */
	void enterRow(PostgreSqlParser.RowContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#row}.
	 * @param ctx the parse tree
	 */
	void exitRow(PostgreSqlParser.RowContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#explicit_row}.
	 * @param ctx the parse tree
	 */
	void enterExplicit_row(PostgreSqlParser.Explicit_rowContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#explicit_row}.
	 * @param ctx the parse tree
	 */
	void exitExplicit_row(PostgreSqlParser.Explicit_rowContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#implicit_row}.
	 * @param ctx the parse tree
	 */
	void enterImplicit_row(PostgreSqlParser.Implicit_rowContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#implicit_row}.
	 * @param ctx the parse tree
	 */
	void exitImplicit_row(PostgreSqlParser.Implicit_rowContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#sub_type}.
	 * @param ctx the parse tree
	 */
	void enterSub_type(PostgreSqlParser.Sub_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#sub_type}.
	 * @param ctx the parse tree
	 */
	void exitSub_type(PostgreSqlParser.Sub_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#all_op}.
	 * @param ctx the parse tree
	 */
	void enterAll_op(PostgreSqlParser.All_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#all_op}.
	 * @param ctx the parse tree
	 */
	void exitAll_op(PostgreSqlParser.All_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#mathop}.
	 * @param ctx the parse tree
	 */
	void enterMathop(PostgreSqlParser.MathopContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#mathop}.
	 * @param ctx the parse tree
	 */
	void exitMathop(PostgreSqlParser.MathopContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#qual_op}.
	 * @param ctx the parse tree
	 */
	void enterQual_op(PostgreSqlParser.Qual_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#qual_op}.
	 * @param ctx the parse tree
	 */
	void exitQual_op(PostgreSqlParser.Qual_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#qual_all_op}.
	 * @param ctx the parse tree
	 */
	void enterQual_all_op(PostgreSqlParser.Qual_all_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#qual_all_op}.
	 * @param ctx the parse tree
	 */
	void exitQual_all_op(PostgreSqlParser.Qual_all_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#subquery_Op}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_Op(PostgreSqlParser.Subquery_OpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#subquery_Op}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_Op(PostgreSqlParser.Subquery_OpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#expr_list}.
	 * @param ctx the parse tree
	 */
	void enterExpr_list(PostgreSqlParser.Expr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#expr_list}.
	 * @param ctx the parse tree
	 */
	void exitExpr_list(PostgreSqlParser.Expr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_arg_list}.
	 * @param ctx the parse tree
	 */
	void enterFunc_arg_list(PostgreSqlParser.Func_arg_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_arg_list}.
	 * @param ctx the parse tree
	 */
	void exitFunc_arg_list(PostgreSqlParser.Func_arg_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_arg_expr}.
	 * @param ctx the parse tree
	 */
	void enterFunc_arg_expr(PostgreSqlParser.Func_arg_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_arg_expr}.
	 * @param ctx the parse tree
	 */
	void exitFunc_arg_expr(PostgreSqlParser.Func_arg_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#type_list}.
	 * @param ctx the parse tree
	 */
	void enterType_list(PostgreSqlParser.Type_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#type_list}.
	 * @param ctx the parse tree
	 */
	void exitType_list(PostgreSqlParser.Type_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#array_expr}.
	 * @param ctx the parse tree
	 */
	void enterArray_expr(PostgreSqlParser.Array_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#array_expr}.
	 * @param ctx the parse tree
	 */
	void exitArray_expr(PostgreSqlParser.Array_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#array_expr_list}.
	 * @param ctx the parse tree
	 */
	void enterArray_expr_list(PostgreSqlParser.Array_expr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#array_expr_list}.
	 * @param ctx the parse tree
	 */
	void exitArray_expr_list(PostgreSqlParser.Array_expr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#extract_list}.
	 * @param ctx the parse tree
	 */
	void enterExtract_list(PostgreSqlParser.Extract_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#extract_list}.
	 * @param ctx the parse tree
	 */
	void exitExtract_list(PostgreSqlParser.Extract_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#extract_arg}.
	 * @param ctx the parse tree
	 */
	void enterExtract_arg(PostgreSqlParser.Extract_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#extract_arg}.
	 * @param ctx the parse tree
	 */
	void exitExtract_arg(PostgreSqlParser.Extract_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#unicode_normal_form}.
	 * @param ctx the parse tree
	 */
	void enterUnicode_normal_form(PostgreSqlParser.Unicode_normal_formContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#unicode_normal_form}.
	 * @param ctx the parse tree
	 */
	void exitUnicode_normal_form(PostgreSqlParser.Unicode_normal_formContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#overlay_list}.
	 * @param ctx the parse tree
	 */
	void enterOverlay_list(PostgreSqlParser.Overlay_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#overlay_list}.
	 * @param ctx the parse tree
	 */
	void exitOverlay_list(PostgreSqlParser.Overlay_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#position_list}.
	 * @param ctx the parse tree
	 */
	void enterPosition_list(PostgreSqlParser.Position_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#position_list}.
	 * @param ctx the parse tree
	 */
	void exitPosition_list(PostgreSqlParser.Position_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#substr_list}.
	 * @param ctx the parse tree
	 */
	void enterSubstr_list(PostgreSqlParser.Substr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#substr_list}.
	 * @param ctx the parse tree
	 */
	void exitSubstr_list(PostgreSqlParser.Substr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#trim_list}.
	 * @param ctx the parse tree
	 */
	void enterTrim_list(PostgreSqlParser.Trim_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#trim_list}.
	 * @param ctx the parse tree
	 */
	void exitTrim_list(PostgreSqlParser.Trim_listContext ctx);
	/**
	 * Enter a parse tree produced by the {@code in_expr_select}
	 * labeled alternative in {@link PostgreSqlParser#in_expr}.
	 * @param ctx the parse tree
	 */
	void enterIn_expr_select(PostgreSqlParser.In_expr_selectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code in_expr_select}
	 * labeled alternative in {@link PostgreSqlParser#in_expr}.
	 * @param ctx the parse tree
	 */
	void exitIn_expr_select(PostgreSqlParser.In_expr_selectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code in_expr_list}
	 * labeled alternative in {@link PostgreSqlParser#in_expr}.
	 * @param ctx the parse tree
	 */
	void enterIn_expr_list(PostgreSqlParser.In_expr_listContext ctx);
	/**
	 * Exit a parse tree produced by the {@code in_expr_list}
	 * labeled alternative in {@link PostgreSqlParser#in_expr}.
	 * @param ctx the parse tree
	 */
	void exitIn_expr_list(PostgreSqlParser.In_expr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#case_expr}.
	 * @param ctx the parse tree
	 */
	void enterCase_expr(PostgreSqlParser.Case_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#case_expr}.
	 * @param ctx the parse tree
	 */
	void exitCase_expr(PostgreSqlParser.Case_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#when_clause_list}.
	 * @param ctx the parse tree
	 */
	void enterWhen_clause_list(PostgreSqlParser.When_clause_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#when_clause_list}.
	 * @param ctx the parse tree
	 */
	void exitWhen_clause_list(PostgreSqlParser.When_clause_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#when_clause}.
	 * @param ctx the parse tree
	 */
	void enterWhen_clause(PostgreSqlParser.When_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#when_clause}.
	 * @param ctx the parse tree
	 */
	void exitWhen_clause(PostgreSqlParser.When_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#case_default}.
	 * @param ctx the parse tree
	 */
	void enterCase_default(PostgreSqlParser.Case_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#case_default}.
	 * @param ctx the parse tree
	 */
	void exitCase_default(PostgreSqlParser.Case_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#case_arg}.
	 * @param ctx the parse tree
	 */
	void enterCase_arg(PostgreSqlParser.Case_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#case_arg}.
	 * @param ctx the parse tree
	 */
	void exitCase_arg(PostgreSqlParser.Case_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#columnref}.
	 * @param ctx the parse tree
	 */
	void enterColumnref(PostgreSqlParser.ColumnrefContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#columnref}.
	 * @param ctx the parse tree
	 */
	void exitColumnref(PostgreSqlParser.ColumnrefContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#indirection_el}.
	 * @param ctx the parse tree
	 */
	void enterIndirection_el(PostgreSqlParser.Indirection_elContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#indirection_el}.
	 * @param ctx the parse tree
	 */
	void exitIndirection_el(PostgreSqlParser.Indirection_elContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_slice_bound}.
	 * @param ctx the parse tree
	 */
	void enterOpt_slice_bound(PostgreSqlParser.Opt_slice_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_slice_bound}.
	 * @param ctx the parse tree
	 */
	void exitOpt_slice_bound(PostgreSqlParser.Opt_slice_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#indirection}.
	 * @param ctx the parse tree
	 */
	void enterIndirection(PostgreSqlParser.IndirectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#indirection}.
	 * @param ctx the parse tree
	 */
	void exitIndirection(PostgreSqlParser.IndirectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_indirection}.
	 * @param ctx the parse tree
	 */
	void enterOpt_indirection(PostgreSqlParser.Opt_indirectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_indirection}.
	 * @param ctx the parse tree
	 */
	void exitOpt_indirection(PostgreSqlParser.Opt_indirectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_target_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_target_list(PostgreSqlParser.Opt_target_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_target_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_target_list(PostgreSqlParser.Opt_target_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#target_list}.
	 * @param ctx the parse tree
	 */
	void enterTarget_list(PostgreSqlParser.Target_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#target_list}.
	 * @param ctx the parse tree
	 */
	void exitTarget_list(PostgreSqlParser.Target_listContext ctx);
	/**
	 * Enter a parse tree produced by the {@code target_label}
	 * labeled alternative in {@link PostgreSqlParser#target_el}.
	 * @param ctx the parse tree
	 */
	void enterTarget_label(PostgreSqlParser.Target_labelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code target_label}
	 * labeled alternative in {@link PostgreSqlParser#target_el}.
	 * @param ctx the parse tree
	 */
	void exitTarget_label(PostgreSqlParser.Target_labelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code target_star}
	 * labeled alternative in {@link PostgreSqlParser#target_el}.
	 * @param ctx the parse tree
	 */
	void enterTarget_star(PostgreSqlParser.Target_starContext ctx);
	/**
	 * Exit a parse tree produced by the {@code target_star}
	 * labeled alternative in {@link PostgreSqlParser#target_el}.
	 * @param ctx the parse tree
	 */
	void exitTarget_star(PostgreSqlParser.Target_starContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#qualified_name_list}.
	 * @param ctx the parse tree
	 */
	void enterQualified_name_list(PostgreSqlParser.Qualified_name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#qualified_name_list}.
	 * @param ctx the parse tree
	 */
	void exitQualified_name_list(PostgreSqlParser.Qualified_name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#qualified_name}.
	 * @param ctx the parse tree
	 */
	void enterQualified_name(PostgreSqlParser.Qualified_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#qualified_name}.
	 * @param ctx the parse tree
	 */
	void exitQualified_name(PostgreSqlParser.Qualified_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#name_list}.
	 * @param ctx the parse tree
	 */
	void enterName_list(PostgreSqlParser.Name_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#name_list}.
	 * @param ctx the parse tree
	 */
	void exitName_list(PostgreSqlParser.Name_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#name}.
	 * @param ctx the parse tree
	 */
	void enterName(PostgreSqlParser.NameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#name}.
	 * @param ctx the parse tree
	 */
	void exitName(PostgreSqlParser.NameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#attr_name}.
	 * @param ctx the parse tree
	 */
	void enterAttr_name(PostgreSqlParser.Attr_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#attr_name}.
	 * @param ctx the parse tree
	 */
	void exitAttr_name(PostgreSqlParser.Attr_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#file_name}.
	 * @param ctx the parse tree
	 */
	void enterFile_name(PostgreSqlParser.File_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#file_name}.
	 * @param ctx the parse tree
	 */
	void exitFile_name(PostgreSqlParser.File_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#func_name}.
	 * @param ctx the parse tree
	 */
	void enterFunc_name(PostgreSqlParser.Func_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#func_name}.
	 * @param ctx the parse tree
	 */
	void exitFunc_name(PostgreSqlParser.Func_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#aexprconst}.
	 * @param ctx the parse tree
	 */
	void enterAexprconst(PostgreSqlParser.AexprconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#aexprconst}.
	 * @param ctx the parse tree
	 */
	void exitAexprconst(PostgreSqlParser.AexprconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#xconst}.
	 * @param ctx the parse tree
	 */
	void enterXconst(PostgreSqlParser.XconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#xconst}.
	 * @param ctx the parse tree
	 */
	void exitXconst(PostgreSqlParser.XconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#bconst}.
	 * @param ctx the parse tree
	 */
	void enterBconst(PostgreSqlParser.BconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#bconst}.
	 * @param ctx the parse tree
	 */
	void exitBconst(PostgreSqlParser.BconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#fconst}.
	 * @param ctx the parse tree
	 */
	void enterFconst(PostgreSqlParser.FconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#fconst}.
	 * @param ctx the parse tree
	 */
	void exitFconst(PostgreSqlParser.FconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#iconst}.
	 * @param ctx the parse tree
	 */
	void enterIconst(PostgreSqlParser.IconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#iconst}.
	 * @param ctx the parse tree
	 */
	void exitIconst(PostgreSqlParser.IconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#sconst}.
	 * @param ctx the parse tree
	 */
	void enterSconst(PostgreSqlParser.SconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#sconst}.
	 * @param ctx the parse tree
	 */
	void exitSconst(PostgreSqlParser.SconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#anysconst}.
	 * @param ctx the parse tree
	 */
	void enterAnysconst(PostgreSqlParser.AnysconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#anysconst}.
	 * @param ctx the parse tree
	 */
	void exitAnysconst(PostgreSqlParser.AnysconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_uescape}.
	 * @param ctx the parse tree
	 */
	void enterOpt_uescape(PostgreSqlParser.Opt_uescapeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_uescape}.
	 * @param ctx the parse tree
	 */
	void exitOpt_uescape(PostgreSqlParser.Opt_uescapeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#signediconst}.
	 * @param ctx the parse tree
	 */
	void enterSignediconst(PostgreSqlParser.SignediconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#signediconst}.
	 * @param ctx the parse tree
	 */
	void exitSignediconst(PostgreSqlParser.SignediconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#roleid}.
	 * @param ctx the parse tree
	 */
	void enterRoleid(PostgreSqlParser.RoleidContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#roleid}.
	 * @param ctx the parse tree
	 */
	void exitRoleid(PostgreSqlParser.RoleidContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#rolespec}.
	 * @param ctx the parse tree
	 */
	void enterRolespec(PostgreSqlParser.RolespecContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#rolespec}.
	 * @param ctx the parse tree
	 */
	void exitRolespec(PostgreSqlParser.RolespecContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#role_list}.
	 * @param ctx the parse tree
	 */
	void enterRole_list(PostgreSqlParser.Role_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#role_list}.
	 * @param ctx the parse tree
	 */
	void exitRole_list(PostgreSqlParser.Role_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#colid}.
	 * @param ctx the parse tree
	 */
	void enterColid(PostgreSqlParser.ColidContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#colid}.
	 * @param ctx the parse tree
	 */
	void exitColid(PostgreSqlParser.ColidContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#table_alias}.
	 * @param ctx the parse tree
	 */
	void enterTable_alias(PostgreSqlParser.Table_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#table_alias}.
	 * @param ctx the parse tree
	 */
	void exitTable_alias(PostgreSqlParser.Table_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#type_function_name}.
	 * @param ctx the parse tree
	 */
	void enterType_function_name(PostgreSqlParser.Type_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#type_function_name}.
	 * @param ctx the parse tree
	 */
	void exitType_function_name(PostgreSqlParser.Type_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#nonreservedword}.
	 * @param ctx the parse tree
	 */
	void enterNonreservedword(PostgreSqlParser.NonreservedwordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#nonreservedword}.
	 * @param ctx the parse tree
	 */
	void exitNonreservedword(PostgreSqlParser.NonreservedwordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#collabel}.
	 * @param ctx the parse tree
	 */
	void enterCollabel(PostgreSqlParser.CollabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#collabel}.
	 * @param ctx the parse tree
	 */
	void exitCollabel(PostgreSqlParser.CollabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(PostgreSqlParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(PostgreSqlParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#plsqlidentifier}.
	 * @param ctx the parse tree
	 */
	void enterPlsqlidentifier(PostgreSqlParser.PlsqlidentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#plsqlidentifier}.
	 * @param ctx the parse tree
	 */
	void exitPlsqlidentifier(PostgreSqlParser.PlsqlidentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#unreserved_keyword}.
	 * @param ctx the parse tree
	 */
	void enterUnreserved_keyword(PostgreSqlParser.Unreserved_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#unreserved_keyword}.
	 * @param ctx the parse tree
	 */
	void exitUnreserved_keyword(PostgreSqlParser.Unreserved_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#col_name_keyword}.
	 * @param ctx the parse tree
	 */
	void enterCol_name_keyword(PostgreSqlParser.Col_name_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#col_name_keyword}.
	 * @param ctx the parse tree
	 */
	void exitCol_name_keyword(PostgreSqlParser.Col_name_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#type_func_name_keyword}.
	 * @param ctx the parse tree
	 */
	void enterType_func_name_keyword(PostgreSqlParser.Type_func_name_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#type_func_name_keyword}.
	 * @param ctx the parse tree
	 */
	void exitType_func_name_keyword(PostgreSqlParser.Type_func_name_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#reserved_keyword}.
	 * @param ctx the parse tree
	 */
	void enterReserved_keyword(PostgreSqlParser.Reserved_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#reserved_keyword}.
	 * @param ctx the parse tree
	 */
	void exitReserved_keyword(PostgreSqlParser.Reserved_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#builtin_function_name}.
	 * @param ctx the parse tree
	 */
	void enterBuiltin_function_name(PostgreSqlParser.Builtin_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#builtin_function_name}.
	 * @param ctx the parse tree
	 */
	void exitBuiltin_function_name(PostgreSqlParser.Builtin_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#pl_function}.
	 * @param ctx the parse tree
	 */
	void enterPl_function(PostgreSqlParser.Pl_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#pl_function}.
	 * @param ctx the parse tree
	 */
	void exitPl_function(PostgreSqlParser.Pl_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#comp_options}.
	 * @param ctx the parse tree
	 */
	void enterComp_options(PostgreSqlParser.Comp_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#comp_options}.
	 * @param ctx the parse tree
	 */
	void exitComp_options(PostgreSqlParser.Comp_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#comp_option}.
	 * @param ctx the parse tree
	 */
	void enterComp_option(PostgreSqlParser.Comp_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#comp_option}.
	 * @param ctx the parse tree
	 */
	void exitComp_option(PostgreSqlParser.Comp_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#sharp}.
	 * @param ctx the parse tree
	 */
	void enterSharp(PostgreSqlParser.SharpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#sharp}.
	 * @param ctx the parse tree
	 */
	void exitSharp(PostgreSqlParser.SharpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#option_value}.
	 * @param ctx the parse tree
	 */
	void enterOption_value(PostgreSqlParser.Option_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#option_value}.
	 * @param ctx the parse tree
	 */
	void exitOption_value(PostgreSqlParser.Option_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_semi}.
	 * @param ctx the parse tree
	 */
	void enterOpt_semi(PostgreSqlParser.Opt_semiContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_semi}.
	 * @param ctx the parse tree
	 */
	void exitOpt_semi(PostgreSqlParser.Opt_semiContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#pl_block}.
	 * @param ctx the parse tree
	 */
	void enterPl_block(PostgreSqlParser.Pl_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#pl_block}.
	 * @param ctx the parse tree
	 */
	void exitPl_block(PostgreSqlParser.Pl_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_sect}.
	 * @param ctx the parse tree
	 */
	void enterDecl_sect(PostgreSqlParser.Decl_sectContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_sect}.
	 * @param ctx the parse tree
	 */
	void exitDecl_sect(PostgreSqlParser.Decl_sectContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_start}.
	 * @param ctx the parse tree
	 */
	void enterDecl_start(PostgreSqlParser.Decl_startContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_start}.
	 * @param ctx the parse tree
	 */
	void exitDecl_start(PostgreSqlParser.Decl_startContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_stmts}.
	 * @param ctx the parse tree
	 */
	void enterDecl_stmts(PostgreSqlParser.Decl_stmtsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_stmts}.
	 * @param ctx the parse tree
	 */
	void exitDecl_stmts(PostgreSqlParser.Decl_stmtsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#label_decl}.
	 * @param ctx the parse tree
	 */
	void enterLabel_decl(PostgreSqlParser.Label_declContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#label_decl}.
	 * @param ctx the parse tree
	 */
	void exitLabel_decl(PostgreSqlParser.Label_declContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_stmt}.
	 * @param ctx the parse tree
	 */
	void enterDecl_stmt(PostgreSqlParser.Decl_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_stmt}.
	 * @param ctx the parse tree
	 */
	void exitDecl_stmt(PostgreSqlParser.Decl_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_statement}.
	 * @param ctx the parse tree
	 */
	void enterDecl_statement(PostgreSqlParser.Decl_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_statement}.
	 * @param ctx the parse tree
	 */
	void exitDecl_statement(PostgreSqlParser.Decl_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_scrollable}.
	 * @param ctx the parse tree
	 */
	void enterOpt_scrollable(PostgreSqlParser.Opt_scrollableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_scrollable}.
	 * @param ctx the parse tree
	 */
	void exitOpt_scrollable(PostgreSqlParser.Opt_scrollableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_cursor_query}.
	 * @param ctx the parse tree
	 */
	void enterDecl_cursor_query(PostgreSqlParser.Decl_cursor_queryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_cursor_query}.
	 * @param ctx the parse tree
	 */
	void exitDecl_cursor_query(PostgreSqlParser.Decl_cursor_queryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_cursor_args}.
	 * @param ctx the parse tree
	 */
	void enterDecl_cursor_args(PostgreSqlParser.Decl_cursor_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_cursor_args}.
	 * @param ctx the parse tree
	 */
	void exitDecl_cursor_args(PostgreSqlParser.Decl_cursor_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_cursor_arglist}.
	 * @param ctx the parse tree
	 */
	void enterDecl_cursor_arglist(PostgreSqlParser.Decl_cursor_arglistContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_cursor_arglist}.
	 * @param ctx the parse tree
	 */
	void exitDecl_cursor_arglist(PostgreSqlParser.Decl_cursor_arglistContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_cursor_arg}.
	 * @param ctx the parse tree
	 */
	void enterDecl_cursor_arg(PostgreSqlParser.Decl_cursor_argContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_cursor_arg}.
	 * @param ctx the parse tree
	 */
	void exitDecl_cursor_arg(PostgreSqlParser.Decl_cursor_argContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_is_for}.
	 * @param ctx the parse tree
	 */
	void enterDecl_is_for(PostgreSqlParser.Decl_is_forContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_is_for}.
	 * @param ctx the parse tree
	 */
	void exitDecl_is_for(PostgreSqlParser.Decl_is_forContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_aliasitem}.
	 * @param ctx the parse tree
	 */
	void enterDecl_aliasitem(PostgreSqlParser.Decl_aliasitemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_aliasitem}.
	 * @param ctx the parse tree
	 */
	void exitDecl_aliasitem(PostgreSqlParser.Decl_aliasitemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_varname}.
	 * @param ctx the parse tree
	 */
	void enterDecl_varname(PostgreSqlParser.Decl_varnameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_varname}.
	 * @param ctx the parse tree
	 */
	void exitDecl_varname(PostgreSqlParser.Decl_varnameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_const}.
	 * @param ctx the parse tree
	 */
	void enterDecl_const(PostgreSqlParser.Decl_constContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_const}.
	 * @param ctx the parse tree
	 */
	void exitDecl_const(PostgreSqlParser.Decl_constContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_datatype}.
	 * @param ctx the parse tree
	 */
	void enterDecl_datatype(PostgreSqlParser.Decl_datatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_datatype}.
	 * @param ctx the parse tree
	 */
	void exitDecl_datatype(PostgreSqlParser.Decl_datatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_collate}.
	 * @param ctx the parse tree
	 */
	void enterDecl_collate(PostgreSqlParser.Decl_collateContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_collate}.
	 * @param ctx the parse tree
	 */
	void exitDecl_collate(PostgreSqlParser.Decl_collateContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_notnull}.
	 * @param ctx the parse tree
	 */
	void enterDecl_notnull(PostgreSqlParser.Decl_notnullContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_notnull}.
	 * @param ctx the parse tree
	 */
	void exitDecl_notnull(PostgreSqlParser.Decl_notnullContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_defval}.
	 * @param ctx the parse tree
	 */
	void enterDecl_defval(PostgreSqlParser.Decl_defvalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_defval}.
	 * @param ctx the parse tree
	 */
	void exitDecl_defval(PostgreSqlParser.Decl_defvalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#decl_defkey}.
	 * @param ctx the parse tree
	 */
	void enterDecl_defkey(PostgreSqlParser.Decl_defkeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#decl_defkey}.
	 * @param ctx the parse tree
	 */
	void exitDecl_defkey(PostgreSqlParser.Decl_defkeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#assign_operator}.
	 * @param ctx the parse tree
	 */
	void enterAssign_operator(PostgreSqlParser.Assign_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#assign_operator}.
	 * @param ctx the parse tree
	 */
	void exitAssign_operator(PostgreSqlParser.Assign_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#proc_sect}.
	 * @param ctx the parse tree
	 */
	void enterProc_sect(PostgreSqlParser.Proc_sectContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#proc_sect}.
	 * @param ctx the parse tree
	 */
	void exitProc_sect(PostgreSqlParser.Proc_sectContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#proc_stmt}.
	 * @param ctx the parse tree
	 */
	void enterProc_stmt(PostgreSqlParser.Proc_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#proc_stmt}.
	 * @param ctx the parse tree
	 */
	void exitProc_stmt(PostgreSqlParser.Proc_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_perform}.
	 * @param ctx the parse tree
	 */
	void enterStmt_perform(PostgreSqlParser.Stmt_performContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_perform}.
	 * @param ctx the parse tree
	 */
	void exitStmt_perform(PostgreSqlParser.Stmt_performContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_call}.
	 * @param ctx the parse tree
	 */
	void enterStmt_call(PostgreSqlParser.Stmt_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_call}.
	 * @param ctx the parse tree
	 */
	void exitStmt_call(PostgreSqlParser.Stmt_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_expr_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_expr_list(PostgreSqlParser.Opt_expr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_expr_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_expr_list(PostgreSqlParser.Opt_expr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_assign}.
	 * @param ctx the parse tree
	 */
	void enterStmt_assign(PostgreSqlParser.Stmt_assignContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_assign}.
	 * @param ctx the parse tree
	 */
	void exitStmt_assign(PostgreSqlParser.Stmt_assignContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_getdiag}.
	 * @param ctx the parse tree
	 */
	void enterStmt_getdiag(PostgreSqlParser.Stmt_getdiagContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_getdiag}.
	 * @param ctx the parse tree
	 */
	void exitStmt_getdiag(PostgreSqlParser.Stmt_getdiagContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#getdiag_area_opt}.
	 * @param ctx the parse tree
	 */
	void enterGetdiag_area_opt(PostgreSqlParser.Getdiag_area_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#getdiag_area_opt}.
	 * @param ctx the parse tree
	 */
	void exitGetdiag_area_opt(PostgreSqlParser.Getdiag_area_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#getdiag_list}.
	 * @param ctx the parse tree
	 */
	void enterGetdiag_list(PostgreSqlParser.Getdiag_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#getdiag_list}.
	 * @param ctx the parse tree
	 */
	void exitGetdiag_list(PostgreSqlParser.Getdiag_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#getdiag_list_item}.
	 * @param ctx the parse tree
	 */
	void enterGetdiag_list_item(PostgreSqlParser.Getdiag_list_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#getdiag_list_item}.
	 * @param ctx the parse tree
	 */
	void exitGetdiag_list_item(PostgreSqlParser.Getdiag_list_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#getdiag_item}.
	 * @param ctx the parse tree
	 */
	void enterGetdiag_item(PostgreSqlParser.Getdiag_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#getdiag_item}.
	 * @param ctx the parse tree
	 */
	void exitGetdiag_item(PostgreSqlParser.Getdiag_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#getdiag_target}.
	 * @param ctx the parse tree
	 */
	void enterGetdiag_target(PostgreSqlParser.Getdiag_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#getdiag_target}.
	 * @param ctx the parse tree
	 */
	void exitGetdiag_target(PostgreSqlParser.Getdiag_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#assign_var}.
	 * @param ctx the parse tree
	 */
	void enterAssign_var(PostgreSqlParser.Assign_varContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#assign_var}.
	 * @param ctx the parse tree
	 */
	void exitAssign_var(PostgreSqlParser.Assign_varContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_if}.
	 * @param ctx the parse tree
	 */
	void enterStmt_if(PostgreSqlParser.Stmt_ifContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_if}.
	 * @param ctx the parse tree
	 */
	void exitStmt_if(PostgreSqlParser.Stmt_ifContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_elsifs}.
	 * @param ctx the parse tree
	 */
	void enterStmt_elsifs(PostgreSqlParser.Stmt_elsifsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_elsifs}.
	 * @param ctx the parse tree
	 */
	void exitStmt_elsifs(PostgreSqlParser.Stmt_elsifsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_else}.
	 * @param ctx the parse tree
	 */
	void enterStmt_else(PostgreSqlParser.Stmt_elseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_else}.
	 * @param ctx the parse tree
	 */
	void exitStmt_else(PostgreSqlParser.Stmt_elseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_case}.
	 * @param ctx the parse tree
	 */
	void enterStmt_case(PostgreSqlParser.Stmt_caseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_case}.
	 * @param ctx the parse tree
	 */
	void exitStmt_case(PostgreSqlParser.Stmt_caseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_expr_until_when}.
	 * @param ctx the parse tree
	 */
	void enterOpt_expr_until_when(PostgreSqlParser.Opt_expr_until_whenContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_expr_until_when}.
	 * @param ctx the parse tree
	 */
	void exitOpt_expr_until_when(PostgreSqlParser.Opt_expr_until_whenContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#case_when_list}.
	 * @param ctx the parse tree
	 */
	void enterCase_when_list(PostgreSqlParser.Case_when_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#case_when_list}.
	 * @param ctx the parse tree
	 */
	void exitCase_when_list(PostgreSqlParser.Case_when_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#case_when}.
	 * @param ctx the parse tree
	 */
	void enterCase_when(PostgreSqlParser.Case_whenContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#case_when}.
	 * @param ctx the parse tree
	 */
	void exitCase_when(PostgreSqlParser.Case_whenContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_case_else}.
	 * @param ctx the parse tree
	 */
	void enterOpt_case_else(PostgreSqlParser.Opt_case_elseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_case_else}.
	 * @param ctx the parse tree
	 */
	void exitOpt_case_else(PostgreSqlParser.Opt_case_elseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_loop}.
	 * @param ctx the parse tree
	 */
	void enterStmt_loop(PostgreSqlParser.Stmt_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_loop}.
	 * @param ctx the parse tree
	 */
	void exitStmt_loop(PostgreSqlParser.Stmt_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_while}.
	 * @param ctx the parse tree
	 */
	void enterStmt_while(PostgreSqlParser.Stmt_whileContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_while}.
	 * @param ctx the parse tree
	 */
	void exitStmt_while(PostgreSqlParser.Stmt_whileContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_for}.
	 * @param ctx the parse tree
	 */
	void enterStmt_for(PostgreSqlParser.Stmt_forContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_for}.
	 * @param ctx the parse tree
	 */
	void exitStmt_for(PostgreSqlParser.Stmt_forContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#for_control}.
	 * @param ctx the parse tree
	 */
	void enterFor_control(PostgreSqlParser.For_controlContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#for_control}.
	 * @param ctx the parse tree
	 */
	void exitFor_control(PostgreSqlParser.For_controlContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_for_using_expression}.
	 * @param ctx the parse tree
	 */
	void enterOpt_for_using_expression(PostgreSqlParser.Opt_for_using_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_for_using_expression}.
	 * @param ctx the parse tree
	 */
	void exitOpt_for_using_expression(PostgreSqlParser.Opt_for_using_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_cursor_parameters}.
	 * @param ctx the parse tree
	 */
	void enterOpt_cursor_parameters(PostgreSqlParser.Opt_cursor_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_cursor_parameters}.
	 * @param ctx the parse tree
	 */
	void exitOpt_cursor_parameters(PostgreSqlParser.Opt_cursor_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_reverse}.
	 * @param ctx the parse tree
	 */
	void enterOpt_reverse(PostgreSqlParser.Opt_reverseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_reverse}.
	 * @param ctx the parse tree
	 */
	void exitOpt_reverse(PostgreSqlParser.Opt_reverseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_by_expression}.
	 * @param ctx the parse tree
	 */
	void enterOpt_by_expression(PostgreSqlParser.Opt_by_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_by_expression}.
	 * @param ctx the parse tree
	 */
	void exitOpt_by_expression(PostgreSqlParser.Opt_by_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#for_variable}.
	 * @param ctx the parse tree
	 */
	void enterFor_variable(PostgreSqlParser.For_variableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#for_variable}.
	 * @param ctx the parse tree
	 */
	void exitFor_variable(PostgreSqlParser.For_variableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_foreach_a}.
	 * @param ctx the parse tree
	 */
	void enterStmt_foreach_a(PostgreSqlParser.Stmt_foreach_aContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_foreach_a}.
	 * @param ctx the parse tree
	 */
	void exitStmt_foreach_a(PostgreSqlParser.Stmt_foreach_aContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#foreach_slice}.
	 * @param ctx the parse tree
	 */
	void enterForeach_slice(PostgreSqlParser.Foreach_sliceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#foreach_slice}.
	 * @param ctx the parse tree
	 */
	void exitForeach_slice(PostgreSqlParser.Foreach_sliceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_exit}.
	 * @param ctx the parse tree
	 */
	void enterStmt_exit(PostgreSqlParser.Stmt_exitContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_exit}.
	 * @param ctx the parse tree
	 */
	void exitStmt_exit(PostgreSqlParser.Stmt_exitContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#exit_type}.
	 * @param ctx the parse tree
	 */
	void enterExit_type(PostgreSqlParser.Exit_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#exit_type}.
	 * @param ctx the parse tree
	 */
	void exitExit_type(PostgreSqlParser.Exit_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_return}.
	 * @param ctx the parse tree
	 */
	void enterStmt_return(PostgreSqlParser.Stmt_returnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_return}.
	 * @param ctx the parse tree
	 */
	void exitStmt_return(PostgreSqlParser.Stmt_returnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_return_result}.
	 * @param ctx the parse tree
	 */
	void enterOpt_return_result(PostgreSqlParser.Opt_return_resultContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_return_result}.
	 * @param ctx the parse tree
	 */
	void exitOpt_return_result(PostgreSqlParser.Opt_return_resultContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_raise}.
	 * @param ctx the parse tree
	 */
	void enterStmt_raise(PostgreSqlParser.Stmt_raiseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_raise}.
	 * @param ctx the parse tree
	 */
	void exitStmt_raise(PostgreSqlParser.Stmt_raiseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_stmt_raise_level}.
	 * @param ctx the parse tree
	 */
	void enterOpt_stmt_raise_level(PostgreSqlParser.Opt_stmt_raise_levelContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_stmt_raise_level}.
	 * @param ctx the parse tree
	 */
	void exitOpt_stmt_raise_level(PostgreSqlParser.Opt_stmt_raise_levelContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_raise_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_raise_list(PostgreSqlParser.Opt_raise_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_raise_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_raise_list(PostgreSqlParser.Opt_raise_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_raise_using}.
	 * @param ctx the parse tree
	 */
	void enterOpt_raise_using(PostgreSqlParser.Opt_raise_usingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_raise_using}.
	 * @param ctx the parse tree
	 */
	void exitOpt_raise_using(PostgreSqlParser.Opt_raise_usingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_raise_using_elem}.
	 * @param ctx the parse tree
	 */
	void enterOpt_raise_using_elem(PostgreSqlParser.Opt_raise_using_elemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_raise_using_elem}.
	 * @param ctx the parse tree
	 */
	void exitOpt_raise_using_elem(PostgreSqlParser.Opt_raise_using_elemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_raise_using_elem_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_raise_using_elem_list(PostgreSqlParser.Opt_raise_using_elem_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_raise_using_elem_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_raise_using_elem_list(PostgreSqlParser.Opt_raise_using_elem_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_assert}.
	 * @param ctx the parse tree
	 */
	void enterStmt_assert(PostgreSqlParser.Stmt_assertContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_assert}.
	 * @param ctx the parse tree
	 */
	void exitStmt_assert(PostgreSqlParser.Stmt_assertContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_stmt_assert_message}.
	 * @param ctx the parse tree
	 */
	void enterOpt_stmt_assert_message(PostgreSqlParser.Opt_stmt_assert_messageContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_stmt_assert_message}.
	 * @param ctx the parse tree
	 */
	void exitOpt_stmt_assert_message(PostgreSqlParser.Opt_stmt_assert_messageContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#loop_body}.
	 * @param ctx the parse tree
	 */
	void enterLoop_body(PostgreSqlParser.Loop_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#loop_body}.
	 * @param ctx the parse tree
	 */
	void exitLoop_body(PostgreSqlParser.Loop_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_execsql}.
	 * @param ctx the parse tree
	 */
	void enterStmt_execsql(PostgreSqlParser.Stmt_execsqlContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_execsql}.
	 * @param ctx the parse tree
	 */
	void exitStmt_execsql(PostgreSqlParser.Stmt_execsqlContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_dynexecute}.
	 * @param ctx the parse tree
	 */
	void enterStmt_dynexecute(PostgreSqlParser.Stmt_dynexecuteContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_dynexecute}.
	 * @param ctx the parse tree
	 */
	void exitStmt_dynexecute(PostgreSqlParser.Stmt_dynexecuteContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_execute_using}.
	 * @param ctx the parse tree
	 */
	void enterOpt_execute_using(PostgreSqlParser.Opt_execute_usingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_execute_using}.
	 * @param ctx the parse tree
	 */
	void exitOpt_execute_using(PostgreSqlParser.Opt_execute_usingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_execute_using_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_execute_using_list(PostgreSqlParser.Opt_execute_using_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_execute_using_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_execute_using_list(PostgreSqlParser.Opt_execute_using_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_execute_into}.
	 * @param ctx the parse tree
	 */
	void enterOpt_execute_into(PostgreSqlParser.Opt_execute_intoContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_execute_into}.
	 * @param ctx the parse tree
	 */
	void exitOpt_execute_into(PostgreSqlParser.Opt_execute_intoContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_open}.
	 * @param ctx the parse tree
	 */
	void enterStmt_open(PostgreSqlParser.Stmt_openContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_open}.
	 * @param ctx the parse tree
	 */
	void exitStmt_open(PostgreSqlParser.Stmt_openContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_open_bound_list_item}.
	 * @param ctx the parse tree
	 */
	void enterOpt_open_bound_list_item(PostgreSqlParser.Opt_open_bound_list_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_open_bound_list_item}.
	 * @param ctx the parse tree
	 */
	void exitOpt_open_bound_list_item(PostgreSqlParser.Opt_open_bound_list_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_open_bound_list}.
	 * @param ctx the parse tree
	 */
	void enterOpt_open_bound_list(PostgreSqlParser.Opt_open_bound_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_open_bound_list}.
	 * @param ctx the parse tree
	 */
	void exitOpt_open_bound_list(PostgreSqlParser.Opt_open_bound_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_open_using}.
	 * @param ctx the parse tree
	 */
	void enterOpt_open_using(PostgreSqlParser.Opt_open_usingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_open_using}.
	 * @param ctx the parse tree
	 */
	void exitOpt_open_using(PostgreSqlParser.Opt_open_usingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_scroll_option}.
	 * @param ctx the parse tree
	 */
	void enterOpt_scroll_option(PostgreSqlParser.Opt_scroll_optionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_scroll_option}.
	 * @param ctx the parse tree
	 */
	void exitOpt_scroll_option(PostgreSqlParser.Opt_scroll_optionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_scroll_option_no}.
	 * @param ctx the parse tree
	 */
	void enterOpt_scroll_option_no(PostgreSqlParser.Opt_scroll_option_noContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_scroll_option_no}.
	 * @param ctx the parse tree
	 */
	void exitOpt_scroll_option_no(PostgreSqlParser.Opt_scroll_option_noContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_fetch}.
	 * @param ctx the parse tree
	 */
	void enterStmt_fetch(PostgreSqlParser.Stmt_fetchContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_fetch}.
	 * @param ctx the parse tree
	 */
	void exitStmt_fetch(PostgreSqlParser.Stmt_fetchContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#into_target}.
	 * @param ctx the parse tree
	 */
	void enterInto_target(PostgreSqlParser.Into_targetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#into_target}.
	 * @param ctx the parse tree
	 */
	void exitInto_target(PostgreSqlParser.Into_targetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_cursor_from}.
	 * @param ctx the parse tree
	 */
	void enterOpt_cursor_from(PostgreSqlParser.Opt_cursor_fromContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_cursor_from}.
	 * @param ctx the parse tree
	 */
	void exitOpt_cursor_from(PostgreSqlParser.Opt_cursor_fromContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_fetch_direction}.
	 * @param ctx the parse tree
	 */
	void enterOpt_fetch_direction(PostgreSqlParser.Opt_fetch_directionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_fetch_direction}.
	 * @param ctx the parse tree
	 */
	void exitOpt_fetch_direction(PostgreSqlParser.Opt_fetch_directionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_move}.
	 * @param ctx the parse tree
	 */
	void enterStmt_move(PostgreSqlParser.Stmt_moveContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_move}.
	 * @param ctx the parse tree
	 */
	void exitStmt_move(PostgreSqlParser.Stmt_moveContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_close}.
	 * @param ctx the parse tree
	 */
	void enterStmt_close(PostgreSqlParser.Stmt_closeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_close}.
	 * @param ctx the parse tree
	 */
	void exitStmt_close(PostgreSqlParser.Stmt_closeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_null}.
	 * @param ctx the parse tree
	 */
	void enterStmt_null(PostgreSqlParser.Stmt_nullContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_null}.
	 * @param ctx the parse tree
	 */
	void exitStmt_null(PostgreSqlParser.Stmt_nullContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_commit}.
	 * @param ctx the parse tree
	 */
	void enterStmt_commit(PostgreSqlParser.Stmt_commitContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_commit}.
	 * @param ctx the parse tree
	 */
	void exitStmt_commit(PostgreSqlParser.Stmt_commitContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_rollback}.
	 * @param ctx the parse tree
	 */
	void enterStmt_rollback(PostgreSqlParser.Stmt_rollbackContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_rollback}.
	 * @param ctx the parse tree
	 */
	void exitStmt_rollback(PostgreSqlParser.Stmt_rollbackContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#plsql_opt_transaction_chain}.
	 * @param ctx the parse tree
	 */
	void enterPlsql_opt_transaction_chain(PostgreSqlParser.Plsql_opt_transaction_chainContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#plsql_opt_transaction_chain}.
	 * @param ctx the parse tree
	 */
	void exitPlsql_opt_transaction_chain(PostgreSqlParser.Plsql_opt_transaction_chainContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#stmt_set}.
	 * @param ctx the parse tree
	 */
	void enterStmt_set(PostgreSqlParser.Stmt_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#stmt_set}.
	 * @param ctx the parse tree
	 */
	void exitStmt_set(PostgreSqlParser.Stmt_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#cursor_variable}.
	 * @param ctx the parse tree
	 */
	void enterCursor_variable(PostgreSqlParser.Cursor_variableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#cursor_variable}.
	 * @param ctx the parse tree
	 */
	void exitCursor_variable(PostgreSqlParser.Cursor_variableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#exception_sect}.
	 * @param ctx the parse tree
	 */
	void enterException_sect(PostgreSqlParser.Exception_sectContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#exception_sect}.
	 * @param ctx the parse tree
	 */
	void exitException_sect(PostgreSqlParser.Exception_sectContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#proc_exceptions}.
	 * @param ctx the parse tree
	 */
	void enterProc_exceptions(PostgreSqlParser.Proc_exceptionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#proc_exceptions}.
	 * @param ctx the parse tree
	 */
	void exitProc_exceptions(PostgreSqlParser.Proc_exceptionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#proc_exception}.
	 * @param ctx the parse tree
	 */
	void enterProc_exception(PostgreSqlParser.Proc_exceptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#proc_exception}.
	 * @param ctx the parse tree
	 */
	void exitProc_exception(PostgreSqlParser.Proc_exceptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#proc_conditions}.
	 * @param ctx the parse tree
	 */
	void enterProc_conditions(PostgreSqlParser.Proc_conditionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#proc_conditions}.
	 * @param ctx the parse tree
	 */
	void exitProc_conditions(PostgreSqlParser.Proc_conditionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#proc_condition}.
	 * @param ctx the parse tree
	 */
	void enterProc_condition(PostgreSqlParser.Proc_conditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#proc_condition}.
	 * @param ctx the parse tree
	 */
	void exitProc_condition(PostgreSqlParser.Proc_conditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_block_label}.
	 * @param ctx the parse tree
	 */
	void enterOpt_block_label(PostgreSqlParser.Opt_block_labelContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_block_label}.
	 * @param ctx the parse tree
	 */
	void exitOpt_block_label(PostgreSqlParser.Opt_block_labelContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_loop_label}.
	 * @param ctx the parse tree
	 */
	void enterOpt_loop_label(PostgreSqlParser.Opt_loop_labelContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_loop_label}.
	 * @param ctx the parse tree
	 */
	void exitOpt_loop_label(PostgreSqlParser.Opt_loop_labelContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_label}.
	 * @param ctx the parse tree
	 */
	void enterOpt_label(PostgreSqlParser.Opt_labelContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_label}.
	 * @param ctx the parse tree
	 */
	void exitOpt_label(PostgreSqlParser.Opt_labelContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_exitcond}.
	 * @param ctx the parse tree
	 */
	void enterOpt_exitcond(PostgreSqlParser.Opt_exitcondContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_exitcond}.
	 * @param ctx the parse tree
	 */
	void exitOpt_exitcond(PostgreSqlParser.Opt_exitcondContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#any_identifier}.
	 * @param ctx the parse tree
	 */
	void enterAny_identifier(PostgreSqlParser.Any_identifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#any_identifier}.
	 * @param ctx the parse tree
	 */
	void exitAny_identifier(PostgreSqlParser.Any_identifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#plsql_unreserved_keyword}.
	 * @param ctx the parse tree
	 */
	void enterPlsql_unreserved_keyword(PostgreSqlParser.Plsql_unreserved_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#plsql_unreserved_keyword}.
	 * @param ctx the parse tree
	 */
	void exitPlsql_unreserved_keyword(PostgreSqlParser.Plsql_unreserved_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#sql_expression}.
	 * @param ctx the parse tree
	 */
	void enterSql_expression(PostgreSqlParser.Sql_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#sql_expression}.
	 * @param ctx the parse tree
	 */
	void exitSql_expression(PostgreSqlParser.Sql_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#expr_until_then}.
	 * @param ctx the parse tree
	 */
	void enterExpr_until_then(PostgreSqlParser.Expr_until_thenContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#expr_until_then}.
	 * @param ctx the parse tree
	 */
	void exitExpr_until_then(PostgreSqlParser.Expr_until_thenContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#expr_until_semi}.
	 * @param ctx the parse tree
	 */
	void enterExpr_until_semi(PostgreSqlParser.Expr_until_semiContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#expr_until_semi}.
	 * @param ctx the parse tree
	 */
	void exitExpr_until_semi(PostgreSqlParser.Expr_until_semiContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#expr_until_rightbracket}.
	 * @param ctx the parse tree
	 */
	void enterExpr_until_rightbracket(PostgreSqlParser.Expr_until_rightbracketContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#expr_until_rightbracket}.
	 * @param ctx the parse tree
	 */
	void exitExpr_until_rightbracket(PostgreSqlParser.Expr_until_rightbracketContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#expr_until_loop}.
	 * @param ctx the parse tree
	 */
	void enterExpr_until_loop(PostgreSqlParser.Expr_until_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#expr_until_loop}.
	 * @param ctx the parse tree
	 */
	void exitExpr_until_loop(PostgreSqlParser.Expr_until_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#make_execsql_stmt}.
	 * @param ctx the parse tree
	 */
	void enterMake_execsql_stmt(PostgreSqlParser.Make_execsql_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#make_execsql_stmt}.
	 * @param ctx the parse tree
	 */
	void exitMake_execsql_stmt(PostgreSqlParser.Make_execsql_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostgreSqlParser#opt_returning_clause_into}.
	 * @param ctx the parse tree
	 */
	void enterOpt_returning_clause_into(PostgreSqlParser.Opt_returning_clause_intoContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostgreSqlParser#opt_returning_clause_into}.
	 * @param ctx the parse tree
	 */
	void exitOpt_returning_clause_into(PostgreSqlParser.Opt_returning_clause_intoContext ctx);
}