// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.postgre.antlr4;


import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link PostgreSqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface PostgreSqlParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#root}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoot(PostgreSqlParser.RootContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#plsqlroot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlsqlroot(PostgreSqlParser.PlsqlrootContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmtblock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmtblock(PostgreSqlParser.StmtblockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmtmulti}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmtmulti(PostgreSqlParser.StmtmultiContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(PostgreSqlParser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#plsqlconsolecommand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlsqlconsolecommand(PostgreSqlParser.PlsqlconsolecommandContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#callstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallstmt(PostgreSqlParser.CallstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createrolestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreaterolestmt(PostgreSqlParser.CreaterolestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_with}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_with(PostgreSqlParser.Opt_withContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optrolelist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptrolelist(PostgreSqlParser.OptrolelistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alteroptrolelist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlteroptrolelist(PostgreSqlParser.AlteroptrolelistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alteroptroleelem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlteroptroleelem(PostgreSqlParser.AlteroptroleelemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createoptroleelem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateoptroleelem(PostgreSqlParser.CreateoptroleelemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createuserstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateuserstmt(PostgreSqlParser.CreateuserstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterrolestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterrolestmt(PostgreSqlParser.AlterrolestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_in_database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_in_database(PostgreSqlParser.Opt_in_databaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterrolesetstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterrolesetstmt(PostgreSqlParser.AlterrolesetstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#droprolestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDroprolestmt(PostgreSqlParser.DroprolestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#creategroupstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreategroupstmt(PostgreSqlParser.CreategroupstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altergroupstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltergroupstmt(PostgreSqlParser.AltergroupstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#add_drop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_drop(PostgreSqlParser.Add_dropContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createschemastmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateschemastmt(PostgreSqlParser.CreateschemastmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optschemaname}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptschemaname(PostgreSqlParser.OptschemanameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optschemaeltlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptschemaeltlist(PostgreSqlParser.OptschemaeltlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#schema_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_stmt(PostgreSqlParser.Schema_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#variablesetstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariablesetstmt(PostgreSqlParser.VariablesetstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#set_rest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_rest(PostgreSqlParser.Set_restContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#generic_set}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_set(PostgreSqlParser.Generic_setContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#set_rest_more}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_rest_more(PostgreSqlParser.Set_rest_moreContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#var_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_name(PostgreSqlParser.Var_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#var_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_list(PostgreSqlParser.Var_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#var_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_value(PostgreSqlParser.Var_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#iso_level}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIso_level(PostgreSqlParser.Iso_levelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_boolean_or_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_boolean_or_string(PostgreSqlParser.Opt_boolean_or_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#zone_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitZone_value(PostgreSqlParser.Zone_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_encoding}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_encoding(PostgreSqlParser.Opt_encodingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#nonreservedword_or_sconst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonreservedword_or_sconst(PostgreSqlParser.Nonreservedword_or_sconstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#variableresetstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableresetstmt(PostgreSqlParser.VariableresetstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reset_rest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReset_rest(PostgreSqlParser.Reset_restContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#generic_reset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_reset(PostgreSqlParser.Generic_resetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#setresetclause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetresetclause(PostgreSqlParser.SetresetclauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#functionsetresetclause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionsetresetclause(PostgreSqlParser.FunctionsetresetclauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#variableshowstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableshowstmt(PostgreSqlParser.VariableshowstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constraintssetstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraintssetstmt(PostgreSqlParser.ConstraintssetstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constraints_set_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraints_set_list(PostgreSqlParser.Constraints_set_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constraints_set_mode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraints_set_mode(PostgreSqlParser.Constraints_set_modeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#checkpointstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCheckpointstmt(PostgreSqlParser.CheckpointstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#discardstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDiscardstmt(PostgreSqlParser.DiscardstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altertablestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltertablestmt(PostgreSqlParser.AltertablestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_table_cmds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_cmds(PostgreSqlParser.Alter_table_cmdsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#partition_cmd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_cmd(PostgreSqlParser.Partition_cmdContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#index_partition_cmd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_partition_cmd(PostgreSqlParser.Index_partition_cmdContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_table_cmd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_cmd(PostgreSqlParser.Alter_table_cmdContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_column_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_column_default(PostgreSqlParser.Alter_column_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_drop_behavior}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_drop_behavior(PostgreSqlParser.Opt_drop_behaviorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_collate_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_collate_clause(PostgreSqlParser.Opt_collate_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_using}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_using(PostgreSqlParser.Alter_usingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#replica_identity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplica_identity(PostgreSqlParser.Replica_identityContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reloptions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReloptions(PostgreSqlParser.ReloptionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_reloptions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_reloptions(PostgreSqlParser.Opt_reloptionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reloption_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReloption_list(PostgreSqlParser.Reloption_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reloption_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReloption_elem(PostgreSqlParser.Reloption_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_identity_column_option_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_identity_column_option_list(PostgreSqlParser.Alter_identity_column_option_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_identity_column_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_identity_column_option(PostgreSqlParser.Alter_identity_column_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#partitionboundspec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionboundspec(PostgreSqlParser.PartitionboundspecContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#hash_partbound_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_partbound_elem(PostgreSqlParser.Hash_partbound_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#hash_partbound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_partbound(PostgreSqlParser.Hash_partboundContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altercompositetypestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltercompositetypestmt(PostgreSqlParser.AltercompositetypestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_type_cmds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_type_cmds(PostgreSqlParser.Alter_type_cmdsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_type_cmd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_type_cmd(PostgreSqlParser.Alter_type_cmdContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#closeportalstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCloseportalstmt(PostgreSqlParser.CloseportalstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copystmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopystmt(PostgreSqlParser.CopystmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_from}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_from(PostgreSqlParser.Copy_fromContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_program(PostgreSqlParser.Opt_programContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_file_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_file_name(PostgreSqlParser.Copy_file_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_options(PostgreSqlParser.Copy_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_opt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_opt_list(PostgreSqlParser.Copy_opt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_opt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_opt_item(PostgreSqlParser.Copy_opt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_binary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_binary(PostgreSqlParser.Opt_binaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_delimiter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_delimiter(PostgreSqlParser.Copy_delimiterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_using}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_using(PostgreSqlParser.Opt_usingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_generic_opt_list(PostgreSqlParser.Copy_generic_opt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_generic_opt_elem(PostgreSqlParser.Copy_generic_opt_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_generic_opt_arg(PostgreSqlParser.Copy_generic_opt_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_arg_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_generic_opt_arg_list(PostgreSqlParser.Copy_generic_opt_arg_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#copy_generic_opt_arg_list_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_generic_opt_arg_list_item(PostgreSqlParser.Copy_generic_opt_arg_list_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatestmt(PostgreSqlParser.CreatestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opttemp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpttemp(PostgreSqlParser.OpttempContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opttableelementlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpttableelementlist(PostgreSqlParser.OpttableelementlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opttypedtableelementlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpttypedtableelementlist(PostgreSqlParser.OpttypedtableelementlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#tableelementlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableelementlist(PostgreSqlParser.TableelementlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#typedtableelementlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypedtableelementlist(PostgreSqlParser.TypedtableelementlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#tableelement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableelement(PostgreSqlParser.TableelementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#typedtableelement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypedtableelement(PostgreSqlParser.TypedtableelementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#columnDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnDef(PostgreSqlParser.ColumnDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#columnOptions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnOptions(PostgreSqlParser.ColumnOptionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#colquallist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColquallist(PostgreSqlParser.ColquallistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#colconstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColconstraint(PostgreSqlParser.ColconstraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#colconstraintelem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColconstraintelem(PostgreSqlParser.ColconstraintelemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#generated_when}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenerated_when(PostgreSqlParser.Generated_whenContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constraintattr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraintattr(PostgreSqlParser.ConstraintattrContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#tablelikeclause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablelikeclause(PostgreSqlParser.TablelikeclauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#tablelikeoptionlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablelikeoptionlist(PostgreSqlParser.TablelikeoptionlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#tablelikeoption}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablelikeoption(PostgreSqlParser.TablelikeoptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#tableconstraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableconstraint(PostgreSqlParser.TableconstraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constraintelem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraintelem(PostgreSqlParser.ConstraintelemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_no_inherit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_no_inherit(PostgreSqlParser.Opt_no_inheritContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_column_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_column_list(PostgreSqlParser.Opt_column_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#columnlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnlist(PostgreSqlParser.ColumnlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#columnElem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnElem(PostgreSqlParser.ColumnElemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_c_include}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_c_include(PostgreSqlParser.Opt_c_includeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#key_match}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey_match(PostgreSqlParser.Key_matchContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#exclusionconstraintlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExclusionconstraintlist(PostgreSqlParser.ExclusionconstraintlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#exclusionconstraintelem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExclusionconstraintelem(PostgreSqlParser.ExclusionconstraintelemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#exclusionwhereclause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExclusionwhereclause(PostgreSqlParser.ExclusionwhereclauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#key_actions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey_actions(PostgreSqlParser.Key_actionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#key_update}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey_update(PostgreSqlParser.Key_updateContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#key_delete}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey_delete(PostgreSqlParser.Key_deleteContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#key_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey_action(PostgreSqlParser.Key_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optinherit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptinherit(PostgreSqlParser.OptinheritContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optpartitionspec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptpartitionspec(PostgreSqlParser.OptpartitionspecContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#partitionspec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitionspec(PostgreSqlParser.PartitionspecContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#part_params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPart_params(PostgreSqlParser.Part_paramsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#part_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPart_elem(PostgreSqlParser.Part_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#table_access_method_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_access_method_clause(PostgreSqlParser.Table_access_method_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optwith}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptwith(PostgreSqlParser.OptwithContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#oncommitoption}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOncommitoption(PostgreSqlParser.OncommitoptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opttablespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpttablespace(PostgreSqlParser.OpttablespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optconstablespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptconstablespace(PostgreSqlParser.OptconstablespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#existingindex}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExistingindex(PostgreSqlParser.ExistingindexContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createstatsstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatestatsstmt(PostgreSqlParser.CreatestatsstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterstatsstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterstatsstmt(PostgreSqlParser.AlterstatsstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createasstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateasstmt(PostgreSqlParser.CreateasstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#create_as_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_as_target(PostgreSqlParser.Create_as_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_with_data}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_with_data(PostgreSqlParser.Opt_with_dataContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#creatematviewstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatematviewstmt(PostgreSqlParser.CreatematviewstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#create_mv_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_mv_target(PostgreSqlParser.Create_mv_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optnolog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptnolog(PostgreSqlParser.OptnologContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#refreshmatviewstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefreshmatviewstmt(PostgreSqlParser.RefreshmatviewstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createseqstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateseqstmt(PostgreSqlParser.CreateseqstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterseqstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterseqstmt(PostgreSqlParser.AlterseqstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optseqoptlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptseqoptlist(PostgreSqlParser.OptseqoptlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optparenthesizedseqoptlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptparenthesizedseqoptlist(PostgreSqlParser.OptparenthesizedseqoptlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#seqoptlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeqoptlist(PostgreSqlParser.SeqoptlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#seqoptelem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeqoptelem(PostgreSqlParser.SeqoptelemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_by}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_by(PostgreSqlParser.Opt_byContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#numericonly}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericonly(PostgreSqlParser.NumericonlyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#numericonly_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericonly_list(PostgreSqlParser.Numericonly_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createplangstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateplangstmt(PostgreSqlParser.CreateplangstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_trusted}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_trusted(PostgreSqlParser.Opt_trustedContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#handler_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHandler_name(PostgreSqlParser.Handler_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_inline_handler}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_inline_handler(PostgreSqlParser.Opt_inline_handlerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#validator_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValidator_clause(PostgreSqlParser.Validator_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_validator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_validator(PostgreSqlParser.Opt_validatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_procedural}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_procedural(PostgreSqlParser.Opt_proceduralContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createtablespacestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatetablespacestmt(PostgreSqlParser.CreatetablespacestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opttablespaceowner}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpttablespaceowner(PostgreSqlParser.OpttablespaceownerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#droptablespacestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDroptablespacestmt(PostgreSqlParser.DroptablespacestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createextensionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateextensionstmt(PostgreSqlParser.CreateextensionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#create_extension_opt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_extension_opt_list(PostgreSqlParser.Create_extension_opt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#create_extension_opt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_extension_opt_item(PostgreSqlParser.Create_extension_opt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterextensionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterextensionstmt(PostgreSqlParser.AlterextensionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_extension_opt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_extension_opt_list(PostgreSqlParser.Alter_extension_opt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_extension_opt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_extension_opt_item(PostgreSqlParser.Alter_extension_opt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterextensioncontentsstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterextensioncontentsstmt(PostgreSqlParser.AlterextensioncontentsstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createfdwstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatefdwstmt(PostgreSqlParser.CreatefdwstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#fdw_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFdw_option(PostgreSqlParser.Fdw_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#fdw_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFdw_options(PostgreSqlParser.Fdw_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_fdw_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_fdw_options(PostgreSqlParser.Opt_fdw_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterfdwstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterfdwstmt(PostgreSqlParser.AlterfdwstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#create_generic_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_generic_options(PostgreSqlParser.Create_generic_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#generic_option_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_option_list(PostgreSqlParser.Generic_option_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_generic_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_generic_options(PostgreSqlParser.Alter_generic_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_generic_option_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_generic_option_list(PostgreSqlParser.Alter_generic_option_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alter_generic_option_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_generic_option_elem(PostgreSqlParser.Alter_generic_option_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#generic_option_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_option_elem(PostgreSqlParser.Generic_option_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#generic_option_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_option_name(PostgreSqlParser.Generic_option_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#generic_option_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_option_arg(PostgreSqlParser.Generic_option_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createforeignserverstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateforeignserverstmt(PostgreSqlParser.CreateforeignserverstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_type(PostgreSqlParser.Opt_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#foreign_server_version}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeign_server_version(PostgreSqlParser.Foreign_server_versionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_foreign_server_version}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_foreign_server_version(PostgreSqlParser.Opt_foreign_server_versionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterforeignserverstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterforeignserverstmt(PostgreSqlParser.AlterforeignserverstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createforeigntablestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateforeigntablestmt(PostgreSqlParser.CreateforeigntablestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#importforeignschemastmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportforeignschemastmt(PostgreSqlParser.ImportforeignschemastmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#import_qualification_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_qualification_type(PostgreSqlParser.Import_qualification_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#import_qualification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_qualification(PostgreSqlParser.Import_qualificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createusermappingstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateusermappingstmt(PostgreSqlParser.CreateusermappingstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#auth_ident}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuth_ident(PostgreSqlParser.Auth_identContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dropusermappingstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropusermappingstmt(PostgreSqlParser.DropusermappingstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterusermappingstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterusermappingstmt(PostgreSqlParser.AlterusermappingstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createpolicystmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatepolicystmt(PostgreSqlParser.CreatepolicystmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterpolicystmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterpolicystmt(PostgreSqlParser.AlterpolicystmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rowsecurityoptionalexpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowsecurityoptionalexpr(PostgreSqlParser.RowsecurityoptionalexprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rowsecurityoptionalwithcheck}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowsecurityoptionalwithcheck(PostgreSqlParser.RowsecurityoptionalwithcheckContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rowsecuritydefaulttorole}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowsecuritydefaulttorole(PostgreSqlParser.RowsecuritydefaulttoroleContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rowsecurityoptionaltorole}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowsecurityoptionaltorole(PostgreSqlParser.RowsecurityoptionaltoroleContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rowsecuritydefaultpermissive}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowsecuritydefaultpermissive(PostgreSqlParser.RowsecuritydefaultpermissiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rowsecuritydefaultforcmd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowsecuritydefaultforcmd(PostgreSqlParser.RowsecuritydefaultforcmdContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#row_security_cmd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRow_security_cmd(PostgreSqlParser.Row_security_cmdContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createamstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateamstmt(PostgreSqlParser.CreateamstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#am_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAm_type(PostgreSqlParser.Am_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createtrigstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatetrigstmt(PostgreSqlParser.CreatetrigstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggeractiontime}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggeractiontime(PostgreSqlParser.TriggeractiontimeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggerevents}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggerevents(PostgreSqlParser.TriggereventsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggeroneevent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggeroneevent(PostgreSqlParser.TriggeroneeventContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggerreferencing}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggerreferencing(PostgreSqlParser.TriggerreferencingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggertransitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggertransitions(PostgreSqlParser.TriggertransitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggertransition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggertransition(PostgreSqlParser.TriggertransitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#transitionoldornew}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransitionoldornew(PostgreSqlParser.TransitionoldornewContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#transitionrowortable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransitionrowortable(PostgreSqlParser.TransitionrowortableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#transitionrelname}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransitionrelname(PostgreSqlParser.TransitionrelnameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggerforspec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggerforspec(PostgreSqlParser.TriggerforspecContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggerforopteach}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggerforopteach(PostgreSqlParser.TriggerforopteachContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggerfortype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggerfortype(PostgreSqlParser.TriggerfortypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggerwhen}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggerwhen(PostgreSqlParser.TriggerwhenContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#function_or_procedure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_or_procedure(PostgreSqlParser.Function_or_procedureContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggerfuncargs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggerfuncargs(PostgreSqlParser.TriggerfuncargsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#triggerfuncarg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTriggerfuncarg(PostgreSqlParser.TriggerfuncargContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#optconstrfromtable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptconstrfromtable(PostgreSqlParser.OptconstrfromtableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constraintattributespec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraintattributespec(PostgreSqlParser.ConstraintattributespecContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constraintattributeElem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraintattributeElem(PostgreSqlParser.ConstraintattributeElemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createeventtrigstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateeventtrigstmt(PostgreSqlParser.CreateeventtrigstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#event_trigger_when_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvent_trigger_when_list(PostgreSqlParser.Event_trigger_when_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#event_trigger_when_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvent_trigger_when_item(PostgreSqlParser.Event_trigger_when_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#event_trigger_value_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvent_trigger_value_list(PostgreSqlParser.Event_trigger_value_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altereventtrigstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltereventtrigstmt(PostgreSqlParser.AltereventtrigstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#enable_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnable_trigger(PostgreSqlParser.Enable_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createassertionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateassertionstmt(PostgreSqlParser.CreateassertionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#definestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefinestmt(PostgreSqlParser.DefinestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefinition(PostgreSqlParser.DefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#def_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDef_list(PostgreSqlParser.Def_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#def_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDef_elem(PostgreSqlParser.Def_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#def_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDef_arg(PostgreSqlParser.Def_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#old_aggr_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOld_aggr_definition(PostgreSqlParser.Old_aggr_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#old_aggr_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOld_aggr_list(PostgreSqlParser.Old_aggr_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#old_aggr_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOld_aggr_elem(PostgreSqlParser.Old_aggr_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_enum_val_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_enum_val_list(PostgreSqlParser.Opt_enum_val_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#enum_val_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_val_list(PostgreSqlParser.Enum_val_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterenumstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterenumstmt(PostgreSqlParser.AlterenumstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_if_not_exists}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_if_not_exists(PostgreSqlParser.Opt_if_not_existsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createopclassstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateopclassstmt(PostgreSqlParser.CreateopclassstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opclass_item_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpclass_item_list(PostgreSqlParser.Opclass_item_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opclass_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpclass_item(PostgreSqlParser.Opclass_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_default(PostgreSqlParser.Opt_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_opfamily}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_opfamily(PostgreSqlParser.Opt_opfamilyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opclass_purpose}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpclass_purpose(PostgreSqlParser.Opclass_purposeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_recheck}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_recheck(PostgreSqlParser.Opt_recheckContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createopfamilystmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateopfamilystmt(PostgreSqlParser.CreateopfamilystmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alteropfamilystmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlteropfamilystmt(PostgreSqlParser.AlteropfamilystmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opclass_drop_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpclass_drop_list(PostgreSqlParser.Opclass_drop_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opclass_drop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpclass_drop(PostgreSqlParser.Opclass_dropContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dropopclassstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropopclassstmt(PostgreSqlParser.DropopclassstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dropopfamilystmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropopfamilystmt(PostgreSqlParser.DropopfamilystmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dropownedstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropownedstmt(PostgreSqlParser.DropownedstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reassignownedstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReassignownedstmt(PostgreSqlParser.ReassignownedstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dropstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropstmt(PostgreSqlParser.DropstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#object_type_any_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_type_any_name(PostgreSqlParser.Object_type_any_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#object_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_type_name(PostgreSqlParser.Object_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#drop_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_type_name(PostgreSqlParser.Drop_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#object_type_name_on_any_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_type_name_on_any_name(PostgreSqlParser.Object_type_name_on_any_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#any_name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAny_name_list(PostgreSqlParser.Any_name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#any_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAny_name(PostgreSqlParser.Any_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#attrs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttrs(PostgreSqlParser.AttrsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#type_name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_name_list(PostgreSqlParser.Type_name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#truncatestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTruncatestmt(PostgreSqlParser.TruncatestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_restart_seqs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_restart_seqs(PostgreSqlParser.Opt_restart_seqsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#commentstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentstmt(PostgreSqlParser.CommentstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#comment_text}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment_text(PostgreSqlParser.Comment_textContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#seclabelstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeclabelstmt(PostgreSqlParser.SeclabelstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_provider}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_provider(PostgreSqlParser.Opt_providerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#security_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSecurity_label(PostgreSqlParser.Security_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#fetchstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetchstmt(PostgreSqlParser.FetchstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#fetch_args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetch_args(PostgreSqlParser.Fetch_argsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#from_in}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_in(PostgreSqlParser.From_inContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_from_in}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_from_in(PostgreSqlParser.Opt_from_inContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#grantstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrantstmt(PostgreSqlParser.GrantstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#revokestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRevokestmt(PostgreSqlParser.RevokestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#privileges}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrivileges(PostgreSqlParser.PrivilegesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#privilege_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrivilege_list(PostgreSqlParser.Privilege_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#privilege}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrivilege(PostgreSqlParser.PrivilegeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#privilege_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrivilege_target(PostgreSqlParser.Privilege_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#grantee_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrantee_list(PostgreSqlParser.Grantee_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#grantee}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrantee(PostgreSqlParser.GranteeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_grant_grant_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_grant_grant_option(PostgreSqlParser.Opt_grant_grant_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#grantrolestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrantrolestmt(PostgreSqlParser.GrantrolestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#revokerolestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRevokerolestmt(PostgreSqlParser.RevokerolestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_grant_admin_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_grant_admin_option(PostgreSqlParser.Opt_grant_admin_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_granted_by}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_granted_by(PostgreSqlParser.Opt_granted_byContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterdefaultprivilegesstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterdefaultprivilegesstmt(PostgreSqlParser.AlterdefaultprivilegesstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#defacloptionlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefacloptionlist(PostgreSqlParser.DefacloptionlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#defacloption}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefacloption(PostgreSqlParser.DefacloptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#defaclaction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaclaction(PostgreSqlParser.DefaclactionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#defacl_privilege_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefacl_privilege_target(PostgreSqlParser.Defacl_privilege_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#indexstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexstmt(PostgreSqlParser.IndexstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_unique}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_unique(PostgreSqlParser.Opt_uniqueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_concurrently}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_concurrently(PostgreSqlParser.Opt_concurrentlyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_index_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_index_name(PostgreSqlParser.Opt_index_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#access_method_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccess_method_clause(PostgreSqlParser.Access_method_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#index_params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_params(PostgreSqlParser.Index_paramsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#index_elem_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_elem_options(PostgreSqlParser.Index_elem_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#index_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_elem(PostgreSqlParser.Index_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_include}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_include(PostgreSqlParser.Opt_includeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#index_including_params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_including_params(PostgreSqlParser.Index_including_paramsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_collate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_collate(PostgreSqlParser.Opt_collateContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_class(PostgreSqlParser.Opt_classContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_asc_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_asc_desc(PostgreSqlParser.Opt_asc_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_nulls_order}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_nulls_order(PostgreSqlParser.Opt_nulls_orderContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createfunctionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatefunctionstmt(PostgreSqlParser.CreatefunctionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_or_replace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_or_replace(PostgreSqlParser.Opt_or_replaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_args(PostgreSqlParser.Func_argsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_args_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_args_list(PostgreSqlParser.Func_args_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#function_with_argtypes_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_with_argtypes_list(PostgreSqlParser.Function_with_argtypes_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#function_with_argtypes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_with_argtypes(PostgreSqlParser.Function_with_argtypesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_args_with_defaults}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_args_with_defaults(PostgreSqlParser.Func_args_with_defaultsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_args_with_defaults_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_args_with_defaults_list(PostgreSqlParser.Func_args_with_defaults_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_arg(PostgreSqlParser.Func_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#arg_class}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArg_class(PostgreSqlParser.Arg_classContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#param_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam_name(PostgreSqlParser.Param_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_return}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_return(PostgreSqlParser.Func_returnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_type(PostgreSqlParser.Func_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_arg_with_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_arg_with_default(PostgreSqlParser.Func_arg_with_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#aggr_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggr_arg(PostgreSqlParser.Aggr_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#aggr_args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggr_args(PostgreSqlParser.Aggr_argsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#aggr_args_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggr_args_list(PostgreSqlParser.Aggr_args_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#aggregate_with_argtypes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregate_with_argtypes(PostgreSqlParser.Aggregate_with_argtypesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#aggregate_with_argtypes_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregate_with_argtypes_list(PostgreSqlParser.Aggregate_with_argtypes_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createfunc_opt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatefunc_opt_list(PostgreSqlParser.Createfunc_opt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#common_func_opt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommon_func_opt_item(PostgreSqlParser.Common_func_opt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createfunc_opt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatefunc_opt_item(PostgreSqlParser.Createfunc_opt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_as}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_as(PostgreSqlParser.Func_asContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#transform_type_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransform_type_list(PostgreSqlParser.Transform_type_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_definition(PostgreSqlParser.Opt_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#table_func_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_func_column(PostgreSqlParser.Table_func_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#table_func_column_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_func_column_list(PostgreSqlParser.Table_func_column_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterfunctionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterfunctionstmt(PostgreSqlParser.AlterfunctionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterfunc_opt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterfunc_opt_list(PostgreSqlParser.Alterfunc_opt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_restrict}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_restrict(PostgreSqlParser.Opt_restrictContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#removefuncstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRemovefuncstmt(PostgreSqlParser.RemovefuncstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#removeaggrstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRemoveaggrstmt(PostgreSqlParser.RemoveaggrstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#removeoperstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRemoveoperstmt(PostgreSqlParser.RemoveoperstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#oper_argtypes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOper_argtypes(PostgreSqlParser.Oper_argtypesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#any_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAny_operator(PostgreSqlParser.Any_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#operator_with_argtypes_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_with_argtypes_list(PostgreSqlParser.Operator_with_argtypes_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#operator_with_argtypes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_with_argtypes(PostgreSqlParser.Operator_with_argtypesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dostmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDostmt(PostgreSqlParser.DostmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dostmt_opt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDostmt_opt_list(PostgreSqlParser.Dostmt_opt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dostmt_opt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDostmt_opt_item(PostgreSqlParser.Dostmt_opt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createcaststmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatecaststmt(PostgreSqlParser.CreatecaststmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#cast_context}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast_context(PostgreSqlParser.Cast_contextContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dropcaststmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropcaststmt(PostgreSqlParser.DropcaststmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_if_exists}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_if_exists(PostgreSqlParser.Opt_if_existsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createtransformstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatetransformstmt(PostgreSqlParser.CreatetransformstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#transform_element_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransform_element_list(PostgreSqlParser.Transform_element_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#droptransformstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDroptransformstmt(PostgreSqlParser.DroptransformstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reindexstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReindexstmt(PostgreSqlParser.ReindexstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reindex_target_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReindex_target_type(PostgreSqlParser.Reindex_target_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reindex_target_multitable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReindex_target_multitable(PostgreSqlParser.Reindex_target_multitableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reindex_option_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReindex_option_list(PostgreSqlParser.Reindex_option_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reindex_option_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReindex_option_elem(PostgreSqlParser.Reindex_option_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altertblspcstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltertblspcstmt(PostgreSqlParser.AltertblspcstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#renamestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRenamestmt(PostgreSqlParser.RenamestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_column(PostgreSqlParser.Opt_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_set_data}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_set_data(PostgreSqlParser.Opt_set_dataContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterobjectdependsstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterobjectdependsstmt(PostgreSqlParser.AlterobjectdependsstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_no}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_no(PostgreSqlParser.Opt_noContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterobjectschemastmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterobjectschemastmt(PostgreSqlParser.AlterobjectschemastmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alteroperatorstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlteroperatorstmt(PostgreSqlParser.AlteroperatorstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#operator_def_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_def_list(PostgreSqlParser.Operator_def_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#operator_def_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_def_elem(PostgreSqlParser.Operator_def_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#operator_def_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_def_arg(PostgreSqlParser.Operator_def_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altertypestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltertypestmt(PostgreSqlParser.AltertypestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterownerstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterownerstmt(PostgreSqlParser.AlterownerstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createpublicationstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatepublicationstmt(PostgreSqlParser.CreatepublicationstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_publication_for_tables}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_publication_for_tables(PostgreSqlParser.Opt_publication_for_tablesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#publication_for_tables}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPublication_for_tables(PostgreSqlParser.Publication_for_tablesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterpublicationstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterpublicationstmt(PostgreSqlParser.AlterpublicationstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createsubscriptionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatesubscriptionstmt(PostgreSqlParser.CreatesubscriptionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#publication_name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPublication_name_list(PostgreSqlParser.Publication_name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#publication_name_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPublication_name_item(PostgreSqlParser.Publication_name_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altersubscriptionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltersubscriptionstmt(PostgreSqlParser.AltersubscriptionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dropsubscriptionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropsubscriptionstmt(PostgreSqlParser.DropsubscriptionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rulestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRulestmt(PostgreSqlParser.RulestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#ruleactionlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRuleactionlist(PostgreSqlParser.RuleactionlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#ruleactionmulti}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRuleactionmulti(PostgreSqlParser.RuleactionmultiContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#ruleactionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRuleactionstmt(PostgreSqlParser.RuleactionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#ruleactionstmtOrEmpty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRuleactionstmtOrEmpty(PostgreSqlParser.RuleactionstmtOrEmptyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#event}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEvent(PostgreSqlParser.EventContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_instead}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_instead(PostgreSqlParser.Opt_insteadContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#notifystmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotifystmt(PostgreSqlParser.NotifystmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#notify_payload}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotify_payload(PostgreSqlParser.Notify_payloadContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#listenstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListenstmt(PostgreSqlParser.ListenstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#unlistenstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnlistenstmt(PostgreSqlParser.UnlistenstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#transactionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransactionstmt(PostgreSqlParser.TransactionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_transaction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_transaction(PostgreSqlParser.Opt_transactionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#transaction_mode_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransaction_mode_item(PostgreSqlParser.Transaction_mode_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#transaction_mode_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransaction_mode_list(PostgreSqlParser.Transaction_mode_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#transaction_mode_list_or_empty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransaction_mode_list_or_empty(PostgreSqlParser.Transaction_mode_list_or_emptyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_transaction_chain}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_transaction_chain(PostgreSqlParser.Opt_transaction_chainContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#viewstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitViewstmt(PostgreSqlParser.ViewstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_check_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_check_option(PostgreSqlParser.Opt_check_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#loadstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoadstmt(PostgreSqlParser.LoadstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createdbstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatedbstmt(PostgreSqlParser.CreatedbstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createdb_opt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatedb_opt_list(PostgreSqlParser.Createdb_opt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createdb_opt_items}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatedb_opt_items(PostgreSqlParser.Createdb_opt_itemsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createdb_opt_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatedb_opt_item(PostgreSqlParser.Createdb_opt_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createdb_opt_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatedb_opt_name(PostgreSqlParser.Createdb_opt_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_equal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_equal(PostgreSqlParser.Opt_equalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterdatabasestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterdatabasestmt(PostgreSqlParser.AlterdatabasestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterdatabasesetstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterdatabasesetstmt(PostgreSqlParser.AlterdatabasesetstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#dropdbstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDropdbstmt(PostgreSqlParser.DropdbstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#drop_option_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_option_list(PostgreSqlParser.Drop_option_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#drop_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_option(PostgreSqlParser.Drop_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altercollationstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltercollationstmt(PostgreSqlParser.AltercollationstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altersystemstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltersystemstmt(PostgreSqlParser.AltersystemstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createdomainstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatedomainstmt(PostgreSqlParser.CreatedomainstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alterdomainstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlterdomainstmt(PostgreSqlParser.AlterdomainstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_as}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_as(PostgreSqlParser.Opt_asContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altertsdictionarystmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltertsdictionarystmt(PostgreSqlParser.AltertsdictionarystmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#altertsconfigurationstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAltertsconfigurationstmt(PostgreSqlParser.AltertsconfigurationstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#any_with}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAny_with(PostgreSqlParser.Any_withContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#createconversionstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateconversionstmt(PostgreSqlParser.CreateconversionstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#clusterstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClusterstmt(PostgreSqlParser.ClusterstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#cluster_index_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCluster_index_specification(PostgreSqlParser.Cluster_index_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#vacuumstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVacuumstmt(PostgreSqlParser.VacuumstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#analyzestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyzestmt(PostgreSqlParser.AnalyzestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVac_analyze_option_list(PostgreSqlParser.Vac_analyze_option_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#analyze_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyze_keyword(PostgreSqlParser.Analyze_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVac_analyze_option_elem(PostgreSqlParser.Vac_analyze_option_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVac_analyze_option_name(PostgreSqlParser.Vac_analyze_option_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#vac_analyze_option_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVac_analyze_option_arg(PostgreSqlParser.Vac_analyze_option_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_analyze}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_analyze(PostgreSqlParser.Opt_analyzeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_verbose}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_verbose(PostgreSqlParser.Opt_verboseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_full}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_full(PostgreSqlParser.Opt_fullContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_freeze}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_freeze(PostgreSqlParser.Opt_freezeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_name_list(PostgreSqlParser.Opt_name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#vacuum_relation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVacuum_relation(PostgreSqlParser.Vacuum_relationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#vacuum_relation_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVacuum_relation_list(PostgreSqlParser.Vacuum_relation_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_vacuum_relation_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_vacuum_relation_list(PostgreSqlParser.Opt_vacuum_relation_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#explainstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplainstmt(PostgreSqlParser.ExplainstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#explainablestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplainablestmt(PostgreSqlParser.ExplainablestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#explain_option_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain_option_list(PostgreSqlParser.Explain_option_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#explain_option_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain_option_elem(PostgreSqlParser.Explain_option_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#explain_option_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain_option_name(PostgreSqlParser.Explain_option_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#explain_option_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain_option_arg(PostgreSqlParser.Explain_option_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#preparestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPreparestmt(PostgreSqlParser.PreparestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#prep_type_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrep_type_clause(PostgreSqlParser.Prep_type_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#preparablestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPreparablestmt(PostgreSqlParser.PreparablestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#executestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecutestmt(PostgreSqlParser.ExecutestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#execute_param_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_param_clause(PostgreSqlParser.Execute_param_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#deallocatestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeallocatestmt(PostgreSqlParser.DeallocatestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#insertstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertstmt(PostgreSqlParser.InsertstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#insert_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_target(PostgreSqlParser.Insert_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#insert_rest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_rest(PostgreSqlParser.Insert_restContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#override_kind}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverride_kind(PostgreSqlParser.Override_kindContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#insert_column_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_column_list(PostgreSqlParser.Insert_column_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#insert_column_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_column_item(PostgreSqlParser.Insert_column_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_on_conflict}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_on_conflict(PostgreSqlParser.Opt_on_conflictContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_conf_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_conf_expr(PostgreSqlParser.Opt_conf_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#returning_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturning_clause(PostgreSqlParser.Returning_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#mergestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMergestmt(PostgreSqlParser.MergestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_insert_clause(PostgreSqlParser.Merge_insert_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#merge_update_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_update_clause(PostgreSqlParser.Merge_update_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#merge_delete_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_delete_clause(PostgreSqlParser.Merge_delete_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#deletestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeletestmt(PostgreSqlParser.DeletestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#using_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_clause(PostgreSqlParser.Using_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#lockstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLockstmt(PostgreSqlParser.LockstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_lock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_lock(PostgreSqlParser.Opt_lockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#lock_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLock_type(PostgreSqlParser.Lock_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_nowait}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_nowait(PostgreSqlParser.Opt_nowaitContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_nowait_or_skip}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_nowait_or_skip(PostgreSqlParser.Opt_nowait_or_skipContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#updatestmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdatestmt(PostgreSqlParser.UpdatestmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#set_clause_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_clause_list(PostgreSqlParser.Set_clause_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#set_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_clause(PostgreSqlParser.Set_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#set_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_target(PostgreSqlParser.Set_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#set_target_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_target_list(PostgreSqlParser.Set_target_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#declarecursorstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarecursorstmt(PostgreSqlParser.DeclarecursorstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#cursor_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_name(PostgreSqlParser.Cursor_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#cursor_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_options(PostgreSqlParser.Cursor_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_hold}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_hold(PostgreSqlParser.Opt_holdContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#selectstmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectstmt(PostgreSqlParser.SelectstmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#select_with_parens}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_with_parens(PostgreSqlParser.Select_with_parensContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#select_no_parens}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_no_parens(PostgreSqlParser.Select_no_parensContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#select_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_clause(PostgreSqlParser.Select_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#simple_select}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_select(PostgreSqlParser.Simple_selectContext ctx);
	/**
	 * Visit a parse tree produced by the {@code union}
	 * labeled alternative in {@link PostgreSqlParser#set_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnion(PostgreSqlParser.UnionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code intersect}
	 * labeled alternative in {@link PostgreSqlParser#set_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntersect(PostgreSqlParser.IntersectContext ctx);
	/**
	 * Visit a parse tree produced by the {@code except}
	 * labeled alternative in {@link PostgreSqlParser#set_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExcept(PostgreSqlParser.ExceptContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#set_operator_with_all_or_distinct}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_operator_with_all_or_distinct(PostgreSqlParser.Set_operator_with_all_or_distinctContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#with_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_clause(PostgreSqlParser.With_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#cte_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCte_list(PostgreSqlParser.Cte_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#common_table_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommon_table_expr(PostgreSqlParser.Common_table_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_materialized}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_materialized(PostgreSqlParser.Opt_materializedContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_with_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_with_clause(PostgreSqlParser.Opt_with_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#into_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInto_clause(PostgreSqlParser.Into_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_strict}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_strict(PostgreSqlParser.Opt_strictContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opttempTableName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpttempTableName(PostgreSqlParser.OpttempTableNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_table(PostgreSqlParser.Opt_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#all_or_distinct}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAll_or_distinct(PostgreSqlParser.All_or_distinctContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#distinct_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDistinct_clause(PostgreSqlParser.Distinct_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_all_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_all_clause(PostgreSqlParser.Opt_all_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_sort_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_sort_clause(PostgreSqlParser.Opt_sort_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#sort_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSort_clause(PostgreSqlParser.Sort_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#sortby_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortby_list(PostgreSqlParser.Sortby_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#sortby}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortby(PostgreSqlParser.SortbyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#select_limit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_limit(PostgreSqlParser.Select_limitContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_select_limit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_select_limit(PostgreSqlParser.Opt_select_limitContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#limit_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLimit_clause(PostgreSqlParser.Limit_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#offset_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOffset_clause(PostgreSqlParser.Offset_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#select_limit_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_limit_value(PostgreSqlParser.Select_limit_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#select_offset_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_offset_value(PostgreSqlParser.Select_offset_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#select_fetch_first_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_fetch_first_value(PostgreSqlParser.Select_fetch_first_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#i_or_f_const}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitI_or_f_const(PostgreSqlParser.I_or_f_constContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#row_or_rows}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRow_or_rows(PostgreSqlParser.Row_or_rowsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#first_or_next}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFirst_or_next(PostgreSqlParser.First_or_nextContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#group_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_clause(PostgreSqlParser.Group_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#group_by_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_by_list(PostgreSqlParser.Group_by_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#group_by_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_by_item(PostgreSqlParser.Group_by_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#empty_grouping_set}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmpty_grouping_set(PostgreSqlParser.Empty_grouping_setContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rollup_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollup_clause(PostgreSqlParser.Rollup_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#cube_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCube_clause(PostgreSqlParser.Cube_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrouping_sets_clause(PostgreSqlParser.Grouping_sets_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#having_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHaving_clause(PostgreSqlParser.Having_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#for_locking_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_locking_clause(PostgreSqlParser.For_locking_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_for_locking_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_for_locking_clause(PostgreSqlParser.Opt_for_locking_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#for_locking_items}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_locking_items(PostgreSqlParser.For_locking_itemsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#for_locking_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_locking_item(PostgreSqlParser.For_locking_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#for_locking_strength}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_locking_strength(PostgreSqlParser.For_locking_strengthContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#locked_rels_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocked_rels_list(PostgreSqlParser.Locked_rels_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#values_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValues_clause(PostgreSqlParser.Values_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#from_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_clause(PostgreSqlParser.From_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#from_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_list(PostgreSqlParser.From_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#non_ansi_join}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_ansi_join(PostgreSqlParser.Non_ansi_joinContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#table_ref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref(PostgreSqlParser.Table_refContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#alias_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlias_clause(PostgreSqlParser.Alias_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_alias_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_alias_clause(PostgreSqlParser.Opt_alias_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#table_alias_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_alias_clause(PostgreSqlParser.Table_alias_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_alias_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_alias_clause(PostgreSqlParser.Func_alias_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#join_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoin_type(PostgreSqlParser.Join_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#join_qual}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoin_qual(PostgreSqlParser.Join_qualContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#relation_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelation_expr(PostgreSqlParser.Relation_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#relation_expr_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelation_expr_list(PostgreSqlParser.Relation_expr_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#relation_expr_opt_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelation_expr_opt_alias(PostgreSqlParser.Relation_expr_opt_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#tablesample_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablesample_clause(PostgreSqlParser.Tablesample_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_repeatable_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_repeatable_clause(PostgreSqlParser.Opt_repeatable_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_table(PostgreSqlParser.Func_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rowsfrom_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowsfrom_item(PostgreSqlParser.Rowsfrom_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rowsfrom_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRowsfrom_list(PostgreSqlParser.Rowsfrom_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_col_def_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_col_def_list(PostgreSqlParser.Opt_col_def_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_ordinality}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_ordinality(PostgreSqlParser.Opt_ordinalityContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#where_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhere_clause(PostgreSqlParser.Where_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#where_or_current_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhere_or_current_clause(PostgreSqlParser.Where_or_current_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opttablefuncelementlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpttablefuncelementlist(PostgreSqlParser.OpttablefuncelementlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#tablefuncelementlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablefuncelementlist(PostgreSqlParser.TablefuncelementlistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#tablefuncelement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablefuncelement(PostgreSqlParser.TablefuncelementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xmltable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltable(PostgreSqlParser.XmltableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xmltable_column_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltable_column_list(PostgreSqlParser.Xmltable_column_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xmltable_column_el}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltable_column_el(PostgreSqlParser.Xmltable_column_elContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xmltable_column_option_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltable_column_option_list(PostgreSqlParser.Xmltable_column_option_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xmltable_column_option_el}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltable_column_option_el(PostgreSqlParser.Xmltable_column_option_elContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xml_namespace_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_namespace_list(PostgreSqlParser.Xml_namespace_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xml_namespace_el}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_namespace_el(PostgreSqlParser.Xml_namespace_elContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#typename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypename(PostgreSqlParser.TypenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_array_bounds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_array_bounds(PostgreSqlParser.Opt_array_boundsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#simpletypename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpletypename(PostgreSqlParser.SimpletypenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#consttypename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConsttypename(PostgreSqlParser.ConsttypenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#generictype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenerictype(PostgreSqlParser.GenerictypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_type_modifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_type_modifiers(PostgreSqlParser.Opt_type_modifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#numeric}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric(PostgreSqlParser.NumericContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_float}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_float(PostgreSqlParser.Opt_floatContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#bit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBit(PostgreSqlParser.BitContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constbit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstbit(PostgreSqlParser.ConstbitContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#bitwithlength}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwithlength(PostgreSqlParser.BitwithlengthContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#bitwithoutlength}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwithoutlength(PostgreSqlParser.BitwithoutlengthContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#character}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharacter(PostgreSqlParser.CharacterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constcharacter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstcharacter(PostgreSqlParser.ConstcharacterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#character_c}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharacter_c(PostgreSqlParser.Character_cContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_varying}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_varying(PostgreSqlParser.Opt_varyingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constdatetime}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstdatetime(PostgreSqlParser.ConstdatetimeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#constinterval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstinterval(PostgreSqlParser.ConstintervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_timezone}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_timezone(PostgreSqlParser.Opt_timezoneContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_interval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_interval(PostgreSqlParser.Opt_intervalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#interval_second}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval_second(PostgreSqlParser.Interval_secondContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_escape}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_escape(PostgreSqlParser.Opt_escapeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr(PostgreSqlParser.A_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_qual}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_qual(PostgreSqlParser.A_expr_qualContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_lessless}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_lessless(PostgreSqlParser.A_expr_lesslessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_or(PostgreSqlParser.A_expr_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_and}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_and(PostgreSqlParser.A_expr_andContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_between}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_between(PostgreSqlParser.A_expr_betweenContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_in}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_in(PostgreSqlParser.A_expr_inContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_unary_not}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_unary_not(PostgreSqlParser.A_expr_unary_notContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_isnull}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_isnull(PostgreSqlParser.A_expr_isnullContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_is_not}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_is_not(PostgreSqlParser.A_expr_is_notContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_compare}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_compare(PostgreSqlParser.A_expr_compareContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_like}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_like(PostgreSqlParser.A_expr_likeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_qual_op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_qual_op(PostgreSqlParser.A_expr_qual_opContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_unary_qualop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_unary_qualop(PostgreSqlParser.A_expr_unary_qualopContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_add}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_add(PostgreSqlParser.A_expr_addContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_mul}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_mul(PostgreSqlParser.A_expr_mulContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_caret}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_caret(PostgreSqlParser.A_expr_caretContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_unary_sign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_unary_sign(PostgreSqlParser.A_expr_unary_signContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_at_time_zone}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_at_time_zone(PostgreSqlParser.A_expr_at_time_zoneContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_collate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_collate(PostgreSqlParser.A_expr_collateContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#a_expr_typecast}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA_expr_typecast(PostgreSqlParser.A_expr_typecastContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#b_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitB_expr(PostgreSqlParser.B_exprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code c_expr_exists}
	 * labeled alternative in {@link PostgreSqlParser#c_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_expr_exists(PostgreSqlParser.C_expr_existsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code c_expr_expr}
	 * labeled alternative in {@link PostgreSqlParser#c_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_expr_expr(PostgreSqlParser.C_expr_exprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code c_expr_case}
	 * labeled alternative in {@link PostgreSqlParser#c_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_expr_case(PostgreSqlParser.C_expr_caseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#plsqlvariablename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlsqlvariablename(PostgreSqlParser.PlsqlvariablenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_application}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_application(PostgreSqlParser.Func_applicationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_expr(PostgreSqlParser.Func_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_expr_windowless}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_expr_windowless(PostgreSqlParser.Func_expr_windowlessContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_expr_common_subexpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_expr_common_subexpr(PostgreSqlParser.Func_expr_common_subexprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xml_root_version}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_root_version(PostgreSqlParser.Xml_root_versionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_xml_root_standalone}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_xml_root_standalone(PostgreSqlParser.Opt_xml_root_standaloneContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xml_attributes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_attributes(PostgreSqlParser.Xml_attributesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xml_attribute_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_attribute_list(PostgreSqlParser.Xml_attribute_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xml_attribute_el}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_attribute_el(PostgreSqlParser.Xml_attribute_elContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#document_or_content}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDocument_or_content(PostgreSqlParser.Document_or_contentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xml_whitespace_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_whitespace_option(PostgreSqlParser.Xml_whitespace_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xmlexists_argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlexists_argument(PostgreSqlParser.Xmlexists_argumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xml_passing_mech}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_passing_mech(PostgreSqlParser.Xml_passing_mechContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#within_group_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithin_group_clause(PostgreSqlParser.Within_group_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#filter_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilter_clause(PostgreSqlParser.Filter_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#window_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindow_clause(PostgreSqlParser.Window_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#window_definition_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindow_definition_list(PostgreSqlParser.Window_definition_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#window_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindow_definition(PostgreSqlParser.Window_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#over_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOver_clause(PostgreSqlParser.Over_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#window_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindow_specification(PostgreSqlParser.Window_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_existing_window_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_existing_window_name(PostgreSqlParser.Opt_existing_window_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_partition_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_partition_clause(PostgreSqlParser.Opt_partition_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_frame_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_frame_clause(PostgreSqlParser.Opt_frame_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#frame_extent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrame_extent(PostgreSqlParser.Frame_extentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#frame_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrame_bound(PostgreSqlParser.Frame_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_window_exclusion_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_window_exclusion_clause(PostgreSqlParser.Opt_window_exclusion_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#row}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRow(PostgreSqlParser.RowContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#explicit_row}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicit_row(PostgreSqlParser.Explicit_rowContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#implicit_row}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImplicit_row(PostgreSqlParser.Implicit_rowContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#sub_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSub_type(PostgreSqlParser.Sub_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#all_op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAll_op(PostgreSqlParser.All_opContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#mathop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMathop(PostgreSqlParser.MathopContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#qual_op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQual_op(PostgreSqlParser.Qual_opContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#qual_all_op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQual_all_op(PostgreSqlParser.Qual_all_opContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#subquery_Op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery_Op(PostgreSqlParser.Subquery_OpContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#expr_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_list(PostgreSqlParser.Expr_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_arg_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_arg_list(PostgreSqlParser.Func_arg_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_arg_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_arg_expr(PostgreSqlParser.Func_arg_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#type_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_list(PostgreSqlParser.Type_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#array_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_expr(PostgreSqlParser.Array_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#array_expr_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_expr_list(PostgreSqlParser.Array_expr_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#extract_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtract_list(PostgreSqlParser.Extract_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#extract_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtract_arg(PostgreSqlParser.Extract_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#unicode_normal_form}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnicode_normal_form(PostgreSqlParser.Unicode_normal_formContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#overlay_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverlay_list(PostgreSqlParser.Overlay_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#position_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPosition_list(PostgreSqlParser.Position_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#substr_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubstr_list(PostgreSqlParser.Substr_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#trim_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrim_list(PostgreSqlParser.Trim_listContext ctx);
	/**
	 * Visit a parse tree produced by the {@code in_expr_select}
	 * labeled alternative in {@link PostgreSqlParser#in_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIn_expr_select(PostgreSqlParser.In_expr_selectContext ctx);
	/**
	 * Visit a parse tree produced by the {@code in_expr_list}
	 * labeled alternative in {@link PostgreSqlParser#in_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIn_expr_list(PostgreSqlParser.In_expr_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#case_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_expr(PostgreSqlParser.Case_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#when_clause_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhen_clause_list(PostgreSqlParser.When_clause_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#when_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhen_clause(PostgreSqlParser.When_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#case_default}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_default(PostgreSqlParser.Case_defaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#case_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_arg(PostgreSqlParser.Case_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#columnref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnref(PostgreSqlParser.ColumnrefContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#indirection_el}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndirection_el(PostgreSqlParser.Indirection_elContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_slice_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_slice_bound(PostgreSqlParser.Opt_slice_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#indirection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndirection(PostgreSqlParser.IndirectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_indirection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_indirection(PostgreSqlParser.Opt_indirectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_target_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_target_list(PostgreSqlParser.Opt_target_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#target_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTarget_list(PostgreSqlParser.Target_listContext ctx);
	/**
	 * Visit a parse tree produced by the {@code target_label}
	 * labeled alternative in {@link PostgreSqlParser#target_el}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTarget_label(PostgreSqlParser.Target_labelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code target_star}
	 * labeled alternative in {@link PostgreSqlParser#target_el}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTarget_star(PostgreSqlParser.Target_starContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#qualified_name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualified_name_list(PostgreSqlParser.Qualified_name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#qualified_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualified_name(PostgreSqlParser.Qualified_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#name_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName_list(PostgreSqlParser.Name_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName(PostgreSqlParser.NameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#attr_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttr_name(PostgreSqlParser.Attr_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#file_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFile_name(PostgreSqlParser.File_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#func_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_name(PostgreSqlParser.Func_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#aexprconst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAexprconst(PostgreSqlParser.AexprconstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#xconst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXconst(PostgreSqlParser.XconstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#bconst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBconst(PostgreSqlParser.BconstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#fconst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFconst(PostgreSqlParser.FconstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#iconst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIconst(PostgreSqlParser.IconstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#sconst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSconst(PostgreSqlParser.SconstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#anysconst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnysconst(PostgreSqlParser.AnysconstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_uescape}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_uescape(PostgreSqlParser.Opt_uescapeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#signediconst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignediconst(PostgreSqlParser.SignediconstContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#roleid}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoleid(PostgreSqlParser.RoleidContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#rolespec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRolespec(PostgreSqlParser.RolespecContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#role_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRole_list(PostgreSqlParser.Role_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#colid}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColid(PostgreSqlParser.ColidContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#table_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_alias(PostgreSqlParser.Table_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#type_function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_function_name(PostgreSqlParser.Type_function_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#nonreservedword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonreservedword(PostgreSqlParser.NonreservedwordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#collabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollabel(PostgreSqlParser.CollabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(PostgreSqlParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#plsqlidentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlsqlidentifier(PostgreSqlParser.PlsqlidentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#unreserved_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnreserved_keyword(PostgreSqlParser.Unreserved_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#col_name_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCol_name_keyword(PostgreSqlParser.Col_name_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#type_func_name_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_func_name_keyword(PostgreSqlParser.Type_func_name_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#reserved_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReserved_keyword(PostgreSqlParser.Reserved_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#builtin_function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBuiltin_function_name(PostgreSqlParser.Builtin_function_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#pl_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPl_function(PostgreSqlParser.Pl_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#comp_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComp_options(PostgreSqlParser.Comp_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#comp_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComp_option(PostgreSqlParser.Comp_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#sharp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSharp(PostgreSqlParser.SharpContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#option_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOption_value(PostgreSqlParser.Option_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_semi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_semi(PostgreSqlParser.Opt_semiContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#pl_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPl_block(PostgreSqlParser.Pl_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_sect}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_sect(PostgreSqlParser.Decl_sectContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_start(PostgreSqlParser.Decl_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_stmts}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_stmts(PostgreSqlParser.Decl_stmtsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#label_decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel_decl(PostgreSqlParser.Label_declContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_stmt(PostgreSqlParser.Decl_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_statement(PostgreSqlParser.Decl_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_scrollable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_scrollable(PostgreSqlParser.Opt_scrollableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_cursor_query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_cursor_query(PostgreSqlParser.Decl_cursor_queryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_cursor_args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_cursor_args(PostgreSqlParser.Decl_cursor_argsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_cursor_arglist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_cursor_arglist(PostgreSqlParser.Decl_cursor_arglistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_cursor_arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_cursor_arg(PostgreSqlParser.Decl_cursor_argContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_is_for}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_is_for(PostgreSqlParser.Decl_is_forContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_aliasitem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_aliasitem(PostgreSqlParser.Decl_aliasitemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_varname}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_varname(PostgreSqlParser.Decl_varnameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_const}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_const(PostgreSqlParser.Decl_constContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_datatype(PostgreSqlParser.Decl_datatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_collate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_collate(PostgreSqlParser.Decl_collateContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_notnull}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_notnull(PostgreSqlParser.Decl_notnullContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_defval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_defval(PostgreSqlParser.Decl_defvalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#decl_defkey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl_defkey(PostgreSqlParser.Decl_defkeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#assign_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssign_operator(PostgreSqlParser.Assign_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#proc_sect}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_sect(PostgreSqlParser.Proc_sectContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#proc_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_stmt(PostgreSqlParser.Proc_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_perform}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_perform(PostgreSqlParser.Stmt_performContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_call(PostgreSqlParser.Stmt_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_expr_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_expr_list(PostgreSqlParser.Opt_expr_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_assign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_assign(PostgreSqlParser.Stmt_assignContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_getdiag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_getdiag(PostgreSqlParser.Stmt_getdiagContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#getdiag_area_opt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetdiag_area_opt(PostgreSqlParser.Getdiag_area_optContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#getdiag_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetdiag_list(PostgreSqlParser.Getdiag_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#getdiag_list_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetdiag_list_item(PostgreSqlParser.Getdiag_list_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#getdiag_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetdiag_item(PostgreSqlParser.Getdiag_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#getdiag_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetdiag_target(PostgreSqlParser.Getdiag_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#assign_var}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssign_var(PostgreSqlParser.Assign_varContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_if}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_if(PostgreSqlParser.Stmt_ifContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_elsifs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_elsifs(PostgreSqlParser.Stmt_elsifsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_else}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_else(PostgreSqlParser.Stmt_elseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_case}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_case(PostgreSqlParser.Stmt_caseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_expr_until_when}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_expr_until_when(PostgreSqlParser.Opt_expr_until_whenContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#case_when_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_when_list(PostgreSqlParser.Case_when_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#case_when}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_when(PostgreSqlParser.Case_whenContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_case_else}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_case_else(PostgreSqlParser.Opt_case_elseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_loop(PostgreSqlParser.Stmt_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_while}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_while(PostgreSqlParser.Stmt_whileContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_for}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_for(PostgreSqlParser.Stmt_forContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#for_control}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_control(PostgreSqlParser.For_controlContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_for_using_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_for_using_expression(PostgreSqlParser.Opt_for_using_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_cursor_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_cursor_parameters(PostgreSqlParser.Opt_cursor_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_reverse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_reverse(PostgreSqlParser.Opt_reverseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_by_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_by_expression(PostgreSqlParser.Opt_by_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#for_variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_variable(PostgreSqlParser.For_variableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_foreach_a}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_foreach_a(PostgreSqlParser.Stmt_foreach_aContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#foreach_slice}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeach_slice(PostgreSqlParser.Foreach_sliceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_exit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_exit(PostgreSqlParser.Stmt_exitContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#exit_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExit_type(PostgreSqlParser.Exit_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_return}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_return(PostgreSqlParser.Stmt_returnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_return_result}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_return_result(PostgreSqlParser.Opt_return_resultContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_raise}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_raise(PostgreSqlParser.Stmt_raiseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_stmt_raise_level}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_stmt_raise_level(PostgreSqlParser.Opt_stmt_raise_levelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_raise_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_raise_list(PostgreSqlParser.Opt_raise_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_raise_using}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_raise_using(PostgreSqlParser.Opt_raise_usingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_raise_using_elem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_raise_using_elem(PostgreSqlParser.Opt_raise_using_elemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_raise_using_elem_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_raise_using_elem_list(PostgreSqlParser.Opt_raise_using_elem_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_assert}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_assert(PostgreSqlParser.Stmt_assertContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_stmt_assert_message}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_stmt_assert_message(PostgreSqlParser.Opt_stmt_assert_messageContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#loop_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_body(PostgreSqlParser.Loop_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_execsql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_execsql(PostgreSqlParser.Stmt_execsqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_dynexecute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_dynexecute(PostgreSqlParser.Stmt_dynexecuteContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_execute_using}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_execute_using(PostgreSqlParser.Opt_execute_usingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_execute_using_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_execute_using_list(PostgreSqlParser.Opt_execute_using_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_execute_into}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_execute_into(PostgreSqlParser.Opt_execute_intoContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_open}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_open(PostgreSqlParser.Stmt_openContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_open_bound_list_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_open_bound_list_item(PostgreSqlParser.Opt_open_bound_list_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_open_bound_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_open_bound_list(PostgreSqlParser.Opt_open_bound_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_open_using}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_open_using(PostgreSqlParser.Opt_open_usingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_scroll_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_scroll_option(PostgreSqlParser.Opt_scroll_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_scroll_option_no}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_scroll_option_no(PostgreSqlParser.Opt_scroll_option_noContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_fetch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_fetch(PostgreSqlParser.Stmt_fetchContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#into_target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInto_target(PostgreSqlParser.Into_targetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_cursor_from}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_cursor_from(PostgreSqlParser.Opt_cursor_fromContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_fetch_direction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_fetch_direction(PostgreSqlParser.Opt_fetch_directionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_move}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_move(PostgreSqlParser.Stmt_moveContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_close}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_close(PostgreSqlParser.Stmt_closeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_null}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_null(PostgreSqlParser.Stmt_nullContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_commit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_commit(PostgreSqlParser.Stmt_commitContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_rollback}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_rollback(PostgreSqlParser.Stmt_rollbackContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#plsql_opt_transaction_chain}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlsql_opt_transaction_chain(PostgreSqlParser.Plsql_opt_transaction_chainContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#stmt_set}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_set(PostgreSqlParser.Stmt_setContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#cursor_variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_variable(PostgreSqlParser.Cursor_variableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#exception_sect}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_sect(PostgreSqlParser.Exception_sectContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#proc_exceptions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_exceptions(PostgreSqlParser.Proc_exceptionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#proc_exception}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_exception(PostgreSqlParser.Proc_exceptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#proc_conditions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_conditions(PostgreSqlParser.Proc_conditionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#proc_condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_condition(PostgreSqlParser.Proc_conditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_block_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_block_label(PostgreSqlParser.Opt_block_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_loop_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_loop_label(PostgreSqlParser.Opt_loop_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_label(PostgreSqlParser.Opt_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_exitcond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_exitcond(PostgreSqlParser.Opt_exitcondContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#any_identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAny_identifier(PostgreSqlParser.Any_identifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#plsql_unreserved_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlsql_unreserved_keyword(PostgreSqlParser.Plsql_unreserved_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#sql_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_expression(PostgreSqlParser.Sql_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#expr_until_then}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_until_then(PostgreSqlParser.Expr_until_thenContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#expr_until_semi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_until_semi(PostgreSqlParser.Expr_until_semiContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#expr_until_rightbracket}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_until_rightbracket(PostgreSqlParser.Expr_until_rightbracketContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#expr_until_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr_until_loop(PostgreSqlParser.Expr_until_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#make_execsql_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMake_execsql_stmt(PostgreSqlParser.Make_execsql_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PostgreSqlParser#opt_returning_clause_into}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpt_returning_clause_into(PostgreSqlParser.Opt_returning_clause_intoContext ctx);
}