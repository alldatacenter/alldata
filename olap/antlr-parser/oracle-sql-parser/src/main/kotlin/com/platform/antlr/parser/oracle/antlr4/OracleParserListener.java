// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.oracle.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link OracleParser}.
 */
public interface OracleParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_script}.
	 * @param ctx the parse tree
	 */
	void enterSql_script(OracleParser.Sql_scriptContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_script}.
	 * @param ctx the parse tree
	 */
	void exitSql_script(OracleParser.Sql_scriptContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unit_statement}.
	 * @param ctx the parse tree
	 */
	void enterUnit_statement(OracleParser.Unit_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unit_statement}.
	 * @param ctx the parse tree
	 */
	void exitUnit_statement(OracleParser.Unit_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_diskgroup}.
	 * @param ctx the parse tree
	 */
	void enterAlter_diskgroup(OracleParser.Alter_diskgroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_diskgroup}.
	 * @param ctx the parse tree
	 */
	void exitAlter_diskgroup(OracleParser.Alter_diskgroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_disk_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_disk_clause(OracleParser.Add_disk_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_disk_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_disk_clause(OracleParser.Add_disk_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_disk_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_disk_clause(OracleParser.Drop_disk_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_disk_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_disk_clause(OracleParser.Drop_disk_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#resize_disk_clause}.
	 * @param ctx the parse tree
	 */
	void enterResize_disk_clause(OracleParser.Resize_disk_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#resize_disk_clause}.
	 * @param ctx the parse tree
	 */
	void exitResize_disk_clause(OracleParser.Resize_disk_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#replace_disk_clause}.
	 * @param ctx the parse tree
	 */
	void enterReplace_disk_clause(OracleParser.Replace_disk_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#replace_disk_clause}.
	 * @param ctx the parse tree
	 */
	void exitReplace_disk_clause(OracleParser.Replace_disk_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#wait_nowait}.
	 * @param ctx the parse tree
	 */
	void enterWait_nowait(OracleParser.Wait_nowaitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#wait_nowait}.
	 * @param ctx the parse tree
	 */
	void exitWait_nowait(OracleParser.Wait_nowaitContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rename_disk_clause}.
	 * @param ctx the parse tree
	 */
	void enterRename_disk_clause(OracleParser.Rename_disk_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rename_disk_clause}.
	 * @param ctx the parse tree
	 */
	void exitRename_disk_clause(OracleParser.Rename_disk_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#disk_online_clause}.
	 * @param ctx the parse tree
	 */
	void enterDisk_online_clause(OracleParser.Disk_online_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#disk_online_clause}.
	 * @param ctx the parse tree
	 */
	void exitDisk_online_clause(OracleParser.Disk_online_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#disk_offline_clause}.
	 * @param ctx the parse tree
	 */
	void enterDisk_offline_clause(OracleParser.Disk_offline_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#disk_offline_clause}.
	 * @param ctx the parse tree
	 */
	void exitDisk_offline_clause(OracleParser.Disk_offline_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#timeout_clause}.
	 * @param ctx the parse tree
	 */
	void enterTimeout_clause(OracleParser.Timeout_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#timeout_clause}.
	 * @param ctx the parse tree
	 */
	void exitTimeout_clause(OracleParser.Timeout_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rebalance_diskgroup_clause}.
	 * @param ctx the parse tree
	 */
	void enterRebalance_diskgroup_clause(OracleParser.Rebalance_diskgroup_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rebalance_diskgroup_clause}.
	 * @param ctx the parse tree
	 */
	void exitRebalance_diskgroup_clause(OracleParser.Rebalance_diskgroup_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#phase}.
	 * @param ctx the parse tree
	 */
	void enterPhase(OracleParser.PhaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#phase}.
	 * @param ctx the parse tree
	 */
	void exitPhase(OracleParser.PhaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#check_diskgroup_clause}.
	 * @param ctx the parse tree
	 */
	void enterCheck_diskgroup_clause(OracleParser.Check_diskgroup_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#check_diskgroup_clause}.
	 * @param ctx the parse tree
	 */
	void exitCheck_diskgroup_clause(OracleParser.Check_diskgroup_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#diskgroup_template_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDiskgroup_template_clauses(OracleParser.Diskgroup_template_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#diskgroup_template_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDiskgroup_template_clauses(OracleParser.Diskgroup_template_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#qualified_template_clause}.
	 * @param ctx the parse tree
	 */
	void enterQualified_template_clause(OracleParser.Qualified_template_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#qualified_template_clause}.
	 * @param ctx the parse tree
	 */
	void exitQualified_template_clause(OracleParser.Qualified_template_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#redundancy_clause}.
	 * @param ctx the parse tree
	 */
	void enterRedundancy_clause(OracleParser.Redundancy_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#redundancy_clause}.
	 * @param ctx the parse tree
	 */
	void exitRedundancy_clause(OracleParser.Redundancy_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#striping_clause}.
	 * @param ctx the parse tree
	 */
	void enterStriping_clause(OracleParser.Striping_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#striping_clause}.
	 * @param ctx the parse tree
	 */
	void exitStriping_clause(OracleParser.Striping_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#force_noforce}.
	 * @param ctx the parse tree
	 */
	void enterForce_noforce(OracleParser.Force_noforceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#force_noforce}.
	 * @param ctx the parse tree
	 */
	void exitForce_noforce(OracleParser.Force_noforceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#diskgroup_directory_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDiskgroup_directory_clauses(OracleParser.Diskgroup_directory_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#diskgroup_directory_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDiskgroup_directory_clauses(OracleParser.Diskgroup_directory_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dir_name}.
	 * @param ctx the parse tree
	 */
	void enterDir_name(OracleParser.Dir_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dir_name}.
	 * @param ctx the parse tree
	 */
	void exitDir_name(OracleParser.Dir_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#diskgroup_alias_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDiskgroup_alias_clauses(OracleParser.Diskgroup_alias_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#diskgroup_alias_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDiskgroup_alias_clauses(OracleParser.Diskgroup_alias_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#diskgroup_volume_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDiskgroup_volume_clauses(OracleParser.Diskgroup_volume_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#diskgroup_volume_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDiskgroup_volume_clauses(OracleParser.Diskgroup_volume_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_volume_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_volume_clause(OracleParser.Add_volume_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_volume_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_volume_clause(OracleParser.Add_volume_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_volume_clause}.
	 * @param ctx the parse tree
	 */
	void enterModify_volume_clause(OracleParser.Modify_volume_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_volume_clause}.
	 * @param ctx the parse tree
	 */
	void exitModify_volume_clause(OracleParser.Modify_volume_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#diskgroup_attributes}.
	 * @param ctx the parse tree
	 */
	void enterDiskgroup_attributes(OracleParser.Diskgroup_attributesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#diskgroup_attributes}.
	 * @param ctx the parse tree
	 */
	void exitDiskgroup_attributes(OracleParser.Diskgroup_attributesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_diskgroup_file}.
	 * @param ctx the parse tree
	 */
	void enterModify_diskgroup_file(OracleParser.Modify_diskgroup_fileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_diskgroup_file}.
	 * @param ctx the parse tree
	 */
	void exitModify_diskgroup_file(OracleParser.Modify_diskgroup_fileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#disk_region_clause}.
	 * @param ctx the parse tree
	 */
	void enterDisk_region_clause(OracleParser.Disk_region_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#disk_region_clause}.
	 * @param ctx the parse tree
	 */
	void exitDisk_region_clause(OracleParser.Disk_region_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_diskgroup_file_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_diskgroup_file_clause(OracleParser.Drop_diskgroup_file_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_diskgroup_file_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_diskgroup_file_clause(OracleParser.Drop_diskgroup_file_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#convert_redundancy_clause}.
	 * @param ctx the parse tree
	 */
	void enterConvert_redundancy_clause(OracleParser.Convert_redundancy_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#convert_redundancy_clause}.
	 * @param ctx the parse tree
	 */
	void exitConvert_redundancy_clause(OracleParser.Convert_redundancy_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#usergroup_clauses}.
	 * @param ctx the parse tree
	 */
	void enterUsergroup_clauses(OracleParser.Usergroup_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#usergroup_clauses}.
	 * @param ctx the parse tree
	 */
	void exitUsergroup_clauses(OracleParser.Usergroup_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_clauses}.
	 * @param ctx the parse tree
	 */
	void enterUser_clauses(OracleParser.User_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_clauses}.
	 * @param ctx the parse tree
	 */
	void exitUser_clauses(OracleParser.User_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#file_permissions_clause}.
	 * @param ctx the parse tree
	 */
	void enterFile_permissions_clause(OracleParser.File_permissions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#file_permissions_clause}.
	 * @param ctx the parse tree
	 */
	void exitFile_permissions_clause(OracleParser.File_permissions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#file_owner_clause}.
	 * @param ctx the parse tree
	 */
	void enterFile_owner_clause(OracleParser.File_owner_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#file_owner_clause}.
	 * @param ctx the parse tree
	 */
	void exitFile_owner_clause(OracleParser.File_owner_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#scrub_clause}.
	 * @param ctx the parse tree
	 */
	void enterScrub_clause(OracleParser.Scrub_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#scrub_clause}.
	 * @param ctx the parse tree
	 */
	void exitScrub_clause(OracleParser.Scrub_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#quotagroup_clauses}.
	 * @param ctx the parse tree
	 */
	void enterQuotagroup_clauses(OracleParser.Quotagroup_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#quotagroup_clauses}.
	 * @param ctx the parse tree
	 */
	void exitQuotagroup_clauses(OracleParser.Quotagroup_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#property_name}.
	 * @param ctx the parse tree
	 */
	void enterProperty_name(OracleParser.Property_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#property_name}.
	 * @param ctx the parse tree
	 */
	void exitProperty_name(OracleParser.Property_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#property_value}.
	 * @param ctx the parse tree
	 */
	void enterProperty_value(OracleParser.Property_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#property_value}.
	 * @param ctx the parse tree
	 */
	void exitProperty_value(OracleParser.Property_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#filegroup_clauses}.
	 * @param ctx the parse tree
	 */
	void enterFilegroup_clauses(OracleParser.Filegroup_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#filegroup_clauses}.
	 * @param ctx the parse tree
	 */
	void exitFilegroup_clauses(OracleParser.Filegroup_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_filegroup_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_filegroup_clause(OracleParser.Add_filegroup_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_filegroup_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_filegroup_clause(OracleParser.Add_filegroup_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_filegroup_clause}.
	 * @param ctx the parse tree
	 */
	void enterModify_filegroup_clause(OracleParser.Modify_filegroup_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_filegroup_clause}.
	 * @param ctx the parse tree
	 */
	void exitModify_filegroup_clause(OracleParser.Modify_filegroup_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#move_to_filegroup_clause}.
	 * @param ctx the parse tree
	 */
	void enterMove_to_filegroup_clause(OracleParser.Move_to_filegroup_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#move_to_filegroup_clause}.
	 * @param ctx the parse tree
	 */
	void exitMove_to_filegroup_clause(OracleParser.Move_to_filegroup_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_filegroup_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_filegroup_clause(OracleParser.Drop_filegroup_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_filegroup_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_filegroup_clause(OracleParser.Drop_filegroup_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#quorum_regular}.
	 * @param ctx the parse tree
	 */
	void enterQuorum_regular(OracleParser.Quorum_regularContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#quorum_regular}.
	 * @param ctx the parse tree
	 */
	void exitQuorum_regular(OracleParser.Quorum_regularContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#undrop_disk_clause}.
	 * @param ctx the parse tree
	 */
	void enterUndrop_disk_clause(OracleParser.Undrop_disk_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#undrop_disk_clause}.
	 * @param ctx the parse tree
	 */
	void exitUndrop_disk_clause(OracleParser.Undrop_disk_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#diskgroup_availability}.
	 * @param ctx the parse tree
	 */
	void enterDiskgroup_availability(OracleParser.Diskgroup_availabilityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#diskgroup_availability}.
	 * @param ctx the parse tree
	 */
	void exitDiskgroup_availability(OracleParser.Diskgroup_availabilityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#enable_disable_volume}.
	 * @param ctx the parse tree
	 */
	void enterEnable_disable_volume(OracleParser.Enable_disable_volumeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#enable_disable_volume}.
	 * @param ctx the parse tree
	 */
	void exitEnable_disable_volume(OracleParser.Enable_disable_volumeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_function}.
	 * @param ctx the parse tree
	 */
	void enterDrop_function(OracleParser.Drop_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_function}.
	 * @param ctx the parse tree
	 */
	void exitDrop_function(OracleParser.Drop_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_flashback_archive}.
	 * @param ctx the parse tree
	 */
	void enterAlter_flashback_archive(OracleParser.Alter_flashback_archiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_flashback_archive}.
	 * @param ctx the parse tree
	 */
	void exitAlter_flashback_archive(OracleParser.Alter_flashback_archiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_hierarchy}.
	 * @param ctx the parse tree
	 */
	void enterAlter_hierarchy(OracleParser.Alter_hierarchyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_hierarchy}.
	 * @param ctx the parse tree
	 */
	void exitAlter_hierarchy(OracleParser.Alter_hierarchyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_function}.
	 * @param ctx the parse tree
	 */
	void enterAlter_function(OracleParser.Alter_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_function}.
	 * @param ctx the parse tree
	 */
	void exitAlter_function(OracleParser.Alter_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_java}.
	 * @param ctx the parse tree
	 */
	void enterAlter_java(OracleParser.Alter_javaContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_java}.
	 * @param ctx the parse tree
	 */
	void exitAlter_java(OracleParser.Alter_javaContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#match_string}.
	 * @param ctx the parse tree
	 */
	void enterMatch_string(OracleParser.Match_stringContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#match_string}.
	 * @param ctx the parse tree
	 */
	void exitMatch_string(OracleParser.Match_stringContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_function_body}.
	 * @param ctx the parse tree
	 */
	void enterCreate_function_body(OracleParser.Create_function_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_function_body}.
	 * @param ctx the parse tree
	 */
	void exitCreate_function_body(OracleParser.Create_function_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_macro_body}.
	 * @param ctx the parse tree
	 */
	void enterSql_macro_body(OracleParser.Sql_macro_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_macro_body}.
	 * @param ctx the parse tree
	 */
	void exitSql_macro_body(OracleParser.Sql_macro_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parallel_enable_clause}.
	 * @param ctx the parse tree
	 */
	void enterParallel_enable_clause(OracleParser.Parallel_enable_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parallel_enable_clause}.
	 * @param ctx the parse tree
	 */
	void exitParallel_enable_clause(OracleParser.Parallel_enable_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartition_by_clause(OracleParser.Partition_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartition_by_clause(OracleParser.Partition_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#result_cache_clause}.
	 * @param ctx the parse tree
	 */
	void enterResult_cache_clause(OracleParser.Result_cache_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#result_cache_clause}.
	 * @param ctx the parse tree
	 */
	void exitResult_cache_clause(OracleParser.Result_cache_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#relies_on_part}.
	 * @param ctx the parse tree
	 */
	void enterRelies_on_part(OracleParser.Relies_on_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#relies_on_part}.
	 * @param ctx the parse tree
	 */
	void exitRelies_on_part(OracleParser.Relies_on_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#streaming_clause}.
	 * @param ctx the parse tree
	 */
	void enterStreaming_clause(OracleParser.Streaming_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#streaming_clause}.
	 * @param ctx the parse tree
	 */
	void exitStreaming_clause(OracleParser.Streaming_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_outline}.
	 * @param ctx the parse tree
	 */
	void enterAlter_outline(OracleParser.Alter_outlineContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_outline}.
	 * @param ctx the parse tree
	 */
	void exitAlter_outline(OracleParser.Alter_outlineContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#outline_options}.
	 * @param ctx the parse tree
	 */
	void enterOutline_options(OracleParser.Outline_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#outline_options}.
	 * @param ctx the parse tree
	 */
	void exitOutline_options(OracleParser.Outline_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_lockdown_profile}.
	 * @param ctx the parse tree
	 */
	void enterAlter_lockdown_profile(OracleParser.Alter_lockdown_profileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_lockdown_profile}.
	 * @param ctx the parse tree
	 */
	void exitAlter_lockdown_profile(OracleParser.Alter_lockdown_profileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lockdown_feature}.
	 * @param ctx the parse tree
	 */
	void enterLockdown_feature(OracleParser.Lockdown_featureContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lockdown_feature}.
	 * @param ctx the parse tree
	 */
	void exitLockdown_feature(OracleParser.Lockdown_featureContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lockdown_options}.
	 * @param ctx the parse tree
	 */
	void enterLockdown_options(OracleParser.Lockdown_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lockdown_options}.
	 * @param ctx the parse tree
	 */
	void exitLockdown_options(OracleParser.Lockdown_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lockdown_statements}.
	 * @param ctx the parse tree
	 */
	void enterLockdown_statements(OracleParser.Lockdown_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lockdown_statements}.
	 * @param ctx the parse tree
	 */
	void exitLockdown_statements(OracleParser.Lockdown_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#statement_clauses}.
	 * @param ctx the parse tree
	 */
	void enterStatement_clauses(OracleParser.Statement_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#statement_clauses}.
	 * @param ctx the parse tree
	 */
	void exitStatement_clauses(OracleParser.Statement_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#clause_options}.
	 * @param ctx the parse tree
	 */
	void enterClause_options(OracleParser.Clause_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#clause_options}.
	 * @param ctx the parse tree
	 */
	void exitClause_options(OracleParser.Clause_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#option_values}.
	 * @param ctx the parse tree
	 */
	void enterOption_values(OracleParser.Option_valuesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#option_values}.
	 * @param ctx the parse tree
	 */
	void exitOption_values(OracleParser.Option_valuesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#string_list}.
	 * @param ctx the parse tree
	 */
	void enterString_list(OracleParser.String_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#string_list}.
	 * @param ctx the parse tree
	 */
	void exitString_list(OracleParser.String_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#disable_enable}.
	 * @param ctx the parse tree
	 */
	void enterDisable_enable(OracleParser.Disable_enableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#disable_enable}.
	 * @param ctx the parse tree
	 */
	void exitDisable_enable(OracleParser.Disable_enableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_lockdown_profile}.
	 * @param ctx the parse tree
	 */
	void enterDrop_lockdown_profile(OracleParser.Drop_lockdown_profileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_lockdown_profile}.
	 * @param ctx the parse tree
	 */
	void exitDrop_lockdown_profile(OracleParser.Drop_lockdown_profileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_package}.
	 * @param ctx the parse tree
	 */
	void enterDrop_package(OracleParser.Drop_packageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_package}.
	 * @param ctx the parse tree
	 */
	void exitDrop_package(OracleParser.Drop_packageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_package}.
	 * @param ctx the parse tree
	 */
	void enterAlter_package(OracleParser.Alter_packageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_package}.
	 * @param ctx the parse tree
	 */
	void exitAlter_package(OracleParser.Alter_packageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_package}.
	 * @param ctx the parse tree
	 */
	void enterCreate_package(OracleParser.Create_packageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_package}.
	 * @param ctx the parse tree
	 */
	void exitCreate_package(OracleParser.Create_packageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_package_body}.
	 * @param ctx the parse tree
	 */
	void enterCreate_package_body(OracleParser.Create_package_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_package_body}.
	 * @param ctx the parse tree
	 */
	void exitCreate_package_body(OracleParser.Create_package_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#package_obj_spec}.
	 * @param ctx the parse tree
	 */
	void enterPackage_obj_spec(OracleParser.Package_obj_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#package_obj_spec}.
	 * @param ctx the parse tree
	 */
	void exitPackage_obj_spec(OracleParser.Package_obj_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#procedure_spec}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_spec(OracleParser.Procedure_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#procedure_spec}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_spec(OracleParser.Procedure_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_spec}.
	 * @param ctx the parse tree
	 */
	void enterFunction_spec(OracleParser.Function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_spec}.
	 * @param ctx the parse tree
	 */
	void exitFunction_spec(OracleParser.Function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#package_obj_body}.
	 * @param ctx the parse tree
	 */
	void enterPackage_obj_body(OracleParser.Package_obj_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#package_obj_body}.
	 * @param ctx the parse tree
	 */
	void exitPackage_obj_body(OracleParser.Package_obj_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_pmem_filestore}.
	 * @param ctx the parse tree
	 */
	void enterAlter_pmem_filestore(OracleParser.Alter_pmem_filestoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_pmem_filestore}.
	 * @param ctx the parse tree
	 */
	void exitAlter_pmem_filestore(OracleParser.Alter_pmem_filestoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_pmem_filestore}.
	 * @param ctx the parse tree
	 */
	void enterDrop_pmem_filestore(OracleParser.Drop_pmem_filestoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_pmem_filestore}.
	 * @param ctx the parse tree
	 */
	void exitDrop_pmem_filestore(OracleParser.Drop_pmem_filestoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_procedure}.
	 * @param ctx the parse tree
	 */
	void enterDrop_procedure(OracleParser.Drop_procedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_procedure}.
	 * @param ctx the parse tree
	 */
	void exitDrop_procedure(OracleParser.Drop_procedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_procedure}.
	 * @param ctx the parse tree
	 */
	void enterAlter_procedure(OracleParser.Alter_procedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_procedure}.
	 * @param ctx the parse tree
	 */
	void exitAlter_procedure(OracleParser.Alter_procedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_body}.
	 * @param ctx the parse tree
	 */
	void enterFunction_body(OracleParser.Function_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_body}.
	 * @param ctx the parse tree
	 */
	void exitFunction_body(OracleParser.Function_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#procedure_body}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_body(OracleParser.Procedure_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#procedure_body}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_body(OracleParser.Procedure_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_procedure_body}.
	 * @param ctx the parse tree
	 */
	void enterCreate_procedure_body(OracleParser.Create_procedure_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_procedure_body}.
	 * @param ctx the parse tree
	 */
	void exitCreate_procedure_body(OracleParser.Create_procedure_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_resource_cost}.
	 * @param ctx the parse tree
	 */
	void enterAlter_resource_cost(OracleParser.Alter_resource_costContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_resource_cost}.
	 * @param ctx the parse tree
	 */
	void exitAlter_resource_cost(OracleParser.Alter_resource_costContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_outline}.
	 * @param ctx the parse tree
	 */
	void enterDrop_outline(OracleParser.Drop_outlineContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_outline}.
	 * @param ctx the parse tree
	 */
	void exitDrop_outline(OracleParser.Drop_outlineContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_rollback_segment}.
	 * @param ctx the parse tree
	 */
	void enterAlter_rollback_segment(OracleParser.Alter_rollback_segmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_rollback_segment}.
	 * @param ctx the parse tree
	 */
	void exitAlter_rollback_segment(OracleParser.Alter_rollback_segmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_restore_point}.
	 * @param ctx the parse tree
	 */
	void enterDrop_restore_point(OracleParser.Drop_restore_pointContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_restore_point}.
	 * @param ctx the parse tree
	 */
	void exitDrop_restore_point(OracleParser.Drop_restore_pointContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_rollback_segment}.
	 * @param ctx the parse tree
	 */
	void enterDrop_rollback_segment(OracleParser.Drop_rollback_segmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_rollback_segment}.
	 * @param ctx the parse tree
	 */
	void exitDrop_rollback_segment(OracleParser.Drop_rollback_segmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_role}.
	 * @param ctx the parse tree
	 */
	void enterDrop_role(OracleParser.Drop_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_role}.
	 * @param ctx the parse tree
	 */
	void exitDrop_role(OracleParser.Drop_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_pmem_filestore}.
	 * @param ctx the parse tree
	 */
	void enterCreate_pmem_filestore(OracleParser.Create_pmem_filestoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_pmem_filestore}.
	 * @param ctx the parse tree
	 */
	void exitCreate_pmem_filestore(OracleParser.Create_pmem_filestoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pmem_filestore_options}.
	 * @param ctx the parse tree
	 */
	void enterPmem_filestore_options(OracleParser.Pmem_filestore_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pmem_filestore_options}.
	 * @param ctx the parse tree
	 */
	void exitPmem_filestore_options(OracleParser.Pmem_filestore_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#file_path}.
	 * @param ctx the parse tree
	 */
	void enterFile_path(OracleParser.File_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#file_path}.
	 * @param ctx the parse tree
	 */
	void exitFile_path(OracleParser.File_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_rollback_segment}.
	 * @param ctx the parse tree
	 */
	void enterCreate_rollback_segment(OracleParser.Create_rollback_segmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_rollback_segment}.
	 * @param ctx the parse tree
	 */
	void exitCreate_rollback_segment(OracleParser.Create_rollback_segmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_trigger}.
	 * @param ctx the parse tree
	 */
	void enterDrop_trigger(OracleParser.Drop_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_trigger}.
	 * @param ctx the parse tree
	 */
	void exitDrop_trigger(OracleParser.Drop_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_trigger}.
	 * @param ctx the parse tree
	 */
	void enterAlter_trigger(OracleParser.Alter_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_trigger}.
	 * @param ctx the parse tree
	 */
	void exitAlter_trigger(OracleParser.Alter_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_trigger}.
	 * @param ctx the parse tree
	 */
	void enterCreate_trigger(OracleParser.Create_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_trigger}.
	 * @param ctx the parse tree
	 */
	void exitCreate_trigger(OracleParser.Create_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trigger_follows_clause}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_follows_clause(OracleParser.Trigger_follows_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trigger_follows_clause}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_follows_clause(OracleParser.Trigger_follows_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trigger_when_clause}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_when_clause(OracleParser.Trigger_when_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trigger_when_clause}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_when_clause(OracleParser.Trigger_when_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#simple_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterSimple_dml_trigger(OracleParser.Simple_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#simple_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitSimple_dml_trigger(OracleParser.Simple_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#for_each_row}.
	 * @param ctx the parse tree
	 */
	void enterFor_each_row(OracleParser.For_each_rowContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#for_each_row}.
	 * @param ctx the parse tree
	 */
	void exitFor_each_row(OracleParser.For_each_rowContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compound_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterCompound_dml_trigger(OracleParser.Compound_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compound_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitCompound_dml_trigger(OracleParser.Compound_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#non_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterNon_dml_trigger(OracleParser.Non_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#non_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitNon_dml_trigger(OracleParser.Non_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trigger_body}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_body(OracleParser.Trigger_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trigger_body}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_body(OracleParser.Trigger_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#routine_clause}.
	 * @param ctx the parse tree
	 */
	void enterRoutine_clause(OracleParser.Routine_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#routine_clause}.
	 * @param ctx the parse tree
	 */
	void exitRoutine_clause(OracleParser.Routine_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compound_trigger_block}.
	 * @param ctx the parse tree
	 */
	void enterCompound_trigger_block(OracleParser.Compound_trigger_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compound_trigger_block}.
	 * @param ctx the parse tree
	 */
	void exitCompound_trigger_block(OracleParser.Compound_trigger_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#timing_point_section}.
	 * @param ctx the parse tree
	 */
	void enterTiming_point_section(OracleParser.Timing_point_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#timing_point_section}.
	 * @param ctx the parse tree
	 */
	void exitTiming_point_section(OracleParser.Timing_point_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#non_dml_event}.
	 * @param ctx the parse tree
	 */
	void enterNon_dml_event(OracleParser.Non_dml_eventContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#non_dml_event}.
	 * @param ctx the parse tree
	 */
	void exitNon_dml_event(OracleParser.Non_dml_eventContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dml_event_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_event_clause(OracleParser.Dml_event_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dml_event_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_event_clause(OracleParser.Dml_event_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dml_event_element}.
	 * @param ctx the parse tree
	 */
	void enterDml_event_element(OracleParser.Dml_event_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dml_event_element}.
	 * @param ctx the parse tree
	 */
	void exitDml_event_element(OracleParser.Dml_event_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dml_event_nested_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_event_nested_clause(OracleParser.Dml_event_nested_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dml_event_nested_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_event_nested_clause(OracleParser.Dml_event_nested_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#referencing_clause}.
	 * @param ctx the parse tree
	 */
	void enterReferencing_clause(OracleParser.Referencing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#referencing_clause}.
	 * @param ctx the parse tree
	 */
	void exitReferencing_clause(OracleParser.Referencing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#referencing_element}.
	 * @param ctx the parse tree
	 */
	void enterReferencing_element(OracleParser.Referencing_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#referencing_element}.
	 * @param ctx the parse tree
	 */
	void exitReferencing_element(OracleParser.Referencing_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_type}.
	 * @param ctx the parse tree
	 */
	void enterDrop_type(OracleParser.Drop_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_type}.
	 * @param ctx the parse tree
	 */
	void exitDrop_type(OracleParser.Drop_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_type}.
	 * @param ctx the parse tree
	 */
	void enterAlter_type(OracleParser.Alter_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_type}.
	 * @param ctx the parse tree
	 */
	void exitAlter_type(OracleParser.Alter_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compile_type_clause}.
	 * @param ctx the parse tree
	 */
	void enterCompile_type_clause(OracleParser.Compile_type_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compile_type_clause}.
	 * @param ctx the parse tree
	 */
	void exitCompile_type_clause(OracleParser.Compile_type_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#replace_type_clause}.
	 * @param ctx the parse tree
	 */
	void enterReplace_type_clause(OracleParser.Replace_type_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#replace_type_clause}.
	 * @param ctx the parse tree
	 */
	void exitReplace_type_clause(OracleParser.Replace_type_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_method_spec}.
	 * @param ctx the parse tree
	 */
	void enterAlter_method_spec(OracleParser.Alter_method_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_method_spec}.
	 * @param ctx the parse tree
	 */
	void exitAlter_method_spec(OracleParser.Alter_method_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_method_element}.
	 * @param ctx the parse tree
	 */
	void enterAlter_method_element(OracleParser.Alter_method_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_method_element}.
	 * @param ctx the parse tree
	 */
	void exitAlter_method_element(OracleParser.Alter_method_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_attribute_definition}.
	 * @param ctx the parse tree
	 */
	void enterAlter_attribute_definition(OracleParser.Alter_attribute_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_attribute_definition}.
	 * @param ctx the parse tree
	 */
	void exitAlter_attribute_definition(OracleParser.Alter_attribute_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#attribute_definition}.
	 * @param ctx the parse tree
	 */
	void enterAttribute_definition(OracleParser.Attribute_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#attribute_definition}.
	 * @param ctx the parse tree
	 */
	void exitAttribute_definition(OracleParser.Attribute_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_collection_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAlter_collection_clauses(OracleParser.Alter_collection_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_collection_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAlter_collection_clauses(OracleParser.Alter_collection_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dependent_handling_clause}.
	 * @param ctx the parse tree
	 */
	void enterDependent_handling_clause(OracleParser.Dependent_handling_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dependent_handling_clause}.
	 * @param ctx the parse tree
	 */
	void exitDependent_handling_clause(OracleParser.Dependent_handling_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dependent_exceptions_part}.
	 * @param ctx the parse tree
	 */
	void enterDependent_exceptions_part(OracleParser.Dependent_exceptions_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dependent_exceptions_part}.
	 * @param ctx the parse tree
	 */
	void exitDependent_exceptions_part(OracleParser.Dependent_exceptions_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_type}.
	 * @param ctx the parse tree
	 */
	void enterCreate_type(OracleParser.Create_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_type}.
	 * @param ctx the parse tree
	 */
	void exitCreate_type(OracleParser.Create_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_definition}.
	 * @param ctx the parse tree
	 */
	void enterType_definition(OracleParser.Type_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_definition}.
	 * @param ctx the parse tree
	 */
	void exitType_definition(OracleParser.Type_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_type_def}.
	 * @param ctx the parse tree
	 */
	void enterObject_type_def(OracleParser.Object_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_type_def}.
	 * @param ctx the parse tree
	 */
	void exitObject_type_def(OracleParser.Object_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_as_part}.
	 * @param ctx the parse tree
	 */
	void enterObject_as_part(OracleParser.Object_as_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_as_part}.
	 * @param ctx the parse tree
	 */
	void exitObject_as_part(OracleParser.Object_as_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_under_part}.
	 * @param ctx the parse tree
	 */
	void enterObject_under_part(OracleParser.Object_under_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_under_part}.
	 * @param ctx the parse tree
	 */
	void exitObject_under_part(OracleParser.Object_under_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#nested_table_type_def}.
	 * @param ctx the parse tree
	 */
	void enterNested_table_type_def(OracleParser.Nested_table_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#nested_table_type_def}.
	 * @param ctx the parse tree
	 */
	void exitNested_table_type_def(OracleParser.Nested_table_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sqlj_object_type}.
	 * @param ctx the parse tree
	 */
	void enterSqlj_object_type(OracleParser.Sqlj_object_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sqlj_object_type}.
	 * @param ctx the parse tree
	 */
	void exitSqlj_object_type(OracleParser.Sqlj_object_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_body}.
	 * @param ctx the parse tree
	 */
	void enterType_body(OracleParser.Type_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_body}.
	 * @param ctx the parse tree
	 */
	void exitType_body(OracleParser.Type_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_body_elements}.
	 * @param ctx the parse tree
	 */
	void enterType_body_elements(OracleParser.Type_body_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_body_elements}.
	 * @param ctx the parse tree
	 */
	void exitType_body_elements(OracleParser.Type_body_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#map_order_func_declaration}.
	 * @param ctx the parse tree
	 */
	void enterMap_order_func_declaration(OracleParser.Map_order_func_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#map_order_func_declaration}.
	 * @param ctx the parse tree
	 */
	void exitMap_order_func_declaration(OracleParser.Map_order_func_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subprog_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void enterSubprog_decl_in_type(OracleParser.Subprog_decl_in_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subprog_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void exitSubprog_decl_in_type(OracleParser.Subprog_decl_in_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#proc_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void enterProc_decl_in_type(OracleParser.Proc_decl_in_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#proc_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void exitProc_decl_in_type(OracleParser.Proc_decl_in_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#func_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void enterFunc_decl_in_type(OracleParser.Func_decl_in_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#func_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void exitFunc_decl_in_type(OracleParser.Func_decl_in_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constructor_declaration}.
	 * @param ctx the parse tree
	 */
	void enterConstructor_declaration(OracleParser.Constructor_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constructor_declaration}.
	 * @param ctx the parse tree
	 */
	void exitConstructor_declaration(OracleParser.Constructor_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modifier_clause}.
	 * @param ctx the parse tree
	 */
	void enterModifier_clause(OracleParser.Modifier_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modifier_clause}.
	 * @param ctx the parse tree
	 */
	void exitModifier_clause(OracleParser.Modifier_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_member_spec}.
	 * @param ctx the parse tree
	 */
	void enterObject_member_spec(OracleParser.Object_member_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_member_spec}.
	 * @param ctx the parse tree
	 */
	void exitObject_member_spec(OracleParser.Object_member_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sqlj_object_type_attr}.
	 * @param ctx the parse tree
	 */
	void enterSqlj_object_type_attr(OracleParser.Sqlj_object_type_attrContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sqlj_object_type_attr}.
	 * @param ctx the parse tree
	 */
	void exitSqlj_object_type_attr(OracleParser.Sqlj_object_type_attrContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#element_spec}.
	 * @param ctx the parse tree
	 */
	void enterElement_spec(OracleParser.Element_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#element_spec}.
	 * @param ctx the parse tree
	 */
	void exitElement_spec(OracleParser.Element_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#element_spec_options}.
	 * @param ctx the parse tree
	 */
	void enterElement_spec_options(OracleParser.Element_spec_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#element_spec_options}.
	 * @param ctx the parse tree
	 */
	void exitElement_spec_options(OracleParser.Element_spec_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void enterSubprogram_spec(OracleParser.Subprogram_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void exitSubprogram_spec(OracleParser.Subprogram_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#overriding_subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void enterOverriding_subprogram_spec(OracleParser.Overriding_subprogram_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#overriding_subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void exitOverriding_subprogram_spec(OracleParser.Overriding_subprogram_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#overriding_function_spec}.
	 * @param ctx the parse tree
	 */
	void enterOverriding_function_spec(OracleParser.Overriding_function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#overriding_function_spec}.
	 * @param ctx the parse tree
	 */
	void exitOverriding_function_spec(OracleParser.Overriding_function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_procedure_spec}.
	 * @param ctx the parse tree
	 */
	void enterType_procedure_spec(OracleParser.Type_procedure_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_procedure_spec}.
	 * @param ctx the parse tree
	 */
	void exitType_procedure_spec(OracleParser.Type_procedure_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_function_spec}.
	 * @param ctx the parse tree
	 */
	void enterType_function_spec(OracleParser.Type_function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_function_spec}.
	 * @param ctx the parse tree
	 */
	void exitType_function_spec(OracleParser.Type_function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constructor_spec}.
	 * @param ctx the parse tree
	 */
	void enterConstructor_spec(OracleParser.Constructor_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constructor_spec}.
	 * @param ctx the parse tree
	 */
	void exitConstructor_spec(OracleParser.Constructor_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#map_order_function_spec}.
	 * @param ctx the parse tree
	 */
	void enterMap_order_function_spec(OracleParser.Map_order_function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#map_order_function_spec}.
	 * @param ctx the parse tree
	 */
	void exitMap_order_function_spec(OracleParser.Map_order_function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pragma_clause}.
	 * @param ctx the parse tree
	 */
	void enterPragma_clause(OracleParser.Pragma_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pragma_clause}.
	 * @param ctx the parse tree
	 */
	void exitPragma_clause(OracleParser.Pragma_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pragma_elements}.
	 * @param ctx the parse tree
	 */
	void enterPragma_elements(OracleParser.Pragma_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pragma_elements}.
	 * @param ctx the parse tree
	 */
	void exitPragma_elements(OracleParser.Pragma_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_elements_parameter}.
	 * @param ctx the parse tree
	 */
	void enterType_elements_parameter(OracleParser.Type_elements_parameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_elements_parameter}.
	 * @param ctx the parse tree
	 */
	void exitType_elements_parameter(OracleParser.Type_elements_parameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_sequence}.
	 * @param ctx the parse tree
	 */
	void enterDrop_sequence(OracleParser.Drop_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_sequence}.
	 * @param ctx the parse tree
	 */
	void exitDrop_sequence(OracleParser.Drop_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_sequence}.
	 * @param ctx the parse tree
	 */
	void enterAlter_sequence(OracleParser.Alter_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_sequence}.
	 * @param ctx the parse tree
	 */
	void exitAlter_sequence(OracleParser.Alter_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_session}.
	 * @param ctx the parse tree
	 */
	void enterAlter_session(OracleParser.Alter_sessionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_session}.
	 * @param ctx the parse tree
	 */
	void exitAlter_session(OracleParser.Alter_sessionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_session_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_session_set_clause(OracleParser.Alter_session_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_session_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_session_set_clause(OracleParser.Alter_session_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_sequence}.
	 * @param ctx the parse tree
	 */
	void enterCreate_sequence(OracleParser.Create_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_sequence}.
	 * @param ctx the parse tree
	 */
	void exitCreate_sequence(OracleParser.Create_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sequence_spec}.
	 * @param ctx the parse tree
	 */
	void enterSequence_spec(OracleParser.Sequence_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sequence_spec}.
	 * @param ctx the parse tree
	 */
	void exitSequence_spec(OracleParser.Sequence_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sequence_start_clause}.
	 * @param ctx the parse tree
	 */
	void enterSequence_start_clause(OracleParser.Sequence_start_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sequence_start_clause}.
	 * @param ctx the parse tree
	 */
	void exitSequence_start_clause(OracleParser.Sequence_start_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_analytic_view}.
	 * @param ctx the parse tree
	 */
	void enterCreate_analytic_view(OracleParser.Create_analytic_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_analytic_view}.
	 * @param ctx the parse tree
	 */
	void exitCreate_analytic_view(OracleParser.Create_analytic_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#classification_clause}.
	 * @param ctx the parse tree
	 */
	void enterClassification_clause(OracleParser.Classification_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#classification_clause}.
	 * @param ctx the parse tree
	 */
	void exitClassification_clause(OracleParser.Classification_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#caption_clause}.
	 * @param ctx the parse tree
	 */
	void enterCaption_clause(OracleParser.Caption_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#caption_clause}.
	 * @param ctx the parse tree
	 */
	void exitCaption_clause(OracleParser.Caption_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#description_clause}.
	 * @param ctx the parse tree
	 */
	void enterDescription_clause(OracleParser.Description_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#description_clause}.
	 * @param ctx the parse tree
	 */
	void exitDescription_clause(OracleParser.Description_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#classification_item}.
	 * @param ctx the parse tree
	 */
	void enterClassification_item(OracleParser.Classification_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#classification_item}.
	 * @param ctx the parse tree
	 */
	void exitClassification_item(OracleParser.Classification_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#language}.
	 * @param ctx the parse tree
	 */
	void enterLanguage(OracleParser.LanguageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#language}.
	 * @param ctx the parse tree
	 */
	void exitLanguage(OracleParser.LanguageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cav_using_clause}.
	 * @param ctx the parse tree
	 */
	void enterCav_using_clause(OracleParser.Cav_using_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cav_using_clause}.
	 * @param ctx the parse tree
	 */
	void exitCav_using_clause(OracleParser.Cav_using_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dim_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterDim_by_clause(OracleParser.Dim_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dim_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitDim_by_clause(OracleParser.Dim_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dim_key}.
	 * @param ctx the parse tree
	 */
	void enterDim_key(OracleParser.Dim_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dim_key}.
	 * @param ctx the parse tree
	 */
	void exitDim_key(OracleParser.Dim_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dim_ref}.
	 * @param ctx the parse tree
	 */
	void enterDim_ref(OracleParser.Dim_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dim_ref}.
	 * @param ctx the parse tree
	 */
	void exitDim_ref(OracleParser.Dim_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hier_ref}.
	 * @param ctx the parse tree
	 */
	void enterHier_ref(OracleParser.Hier_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hier_ref}.
	 * @param ctx the parse tree
	 */
	void exitHier_ref(OracleParser.Hier_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#measures_clause}.
	 * @param ctx the parse tree
	 */
	void enterMeasures_clause(OracleParser.Measures_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#measures_clause}.
	 * @param ctx the parse tree
	 */
	void exitMeasures_clause(OracleParser.Measures_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#av_measure}.
	 * @param ctx the parse tree
	 */
	void enterAv_measure(OracleParser.Av_measureContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#av_measure}.
	 * @param ctx the parse tree
	 */
	void exitAv_measure(OracleParser.Av_measureContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#base_meas_clause}.
	 * @param ctx the parse tree
	 */
	void enterBase_meas_clause(OracleParser.Base_meas_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#base_meas_clause}.
	 * @param ctx the parse tree
	 */
	void exitBase_meas_clause(OracleParser.Base_meas_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#meas_aggregate_clause}.
	 * @param ctx the parse tree
	 */
	void enterMeas_aggregate_clause(OracleParser.Meas_aggregate_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#meas_aggregate_clause}.
	 * @param ctx the parse tree
	 */
	void exitMeas_aggregate_clause(OracleParser.Meas_aggregate_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#calc_meas_clause}.
	 * @param ctx the parse tree
	 */
	void enterCalc_meas_clause(OracleParser.Calc_meas_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#calc_meas_clause}.
	 * @param ctx the parse tree
	 */
	void exitCalc_meas_clause(OracleParser.Calc_meas_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_measure_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_measure_clause(OracleParser.Default_measure_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_measure_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_measure_clause(OracleParser.Default_measure_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_aggregate_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_aggregate_clause(OracleParser.Default_aggregate_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_aggregate_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_aggregate_clause(OracleParser.Default_aggregate_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cache_clause}.
	 * @param ctx the parse tree
	 */
	void enterCache_clause(OracleParser.Cache_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cache_clause}.
	 * @param ctx the parse tree
	 */
	void exitCache_clause(OracleParser.Cache_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cache_specification}.
	 * @param ctx the parse tree
	 */
	void enterCache_specification(OracleParser.Cache_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cache_specification}.
	 * @param ctx the parse tree
	 */
	void exitCache_specification(OracleParser.Cache_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#levels_clause}.
	 * @param ctx the parse tree
	 */
	void enterLevels_clause(OracleParser.Levels_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#levels_clause}.
	 * @param ctx the parse tree
	 */
	void exitLevels_clause(OracleParser.Levels_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#level_specification}.
	 * @param ctx the parse tree
	 */
	void enterLevel_specification(OracleParser.Level_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#level_specification}.
	 * @param ctx the parse tree
	 */
	void exitLevel_specification(OracleParser.Level_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#level_group_type}.
	 * @param ctx the parse tree
	 */
	void enterLevel_group_type(OracleParser.Level_group_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#level_group_type}.
	 * @param ctx the parse tree
	 */
	void exitLevel_group_type(OracleParser.Level_group_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#fact_columns_clause}.
	 * @param ctx the parse tree
	 */
	void enterFact_columns_clause(OracleParser.Fact_columns_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#fact_columns_clause}.
	 * @param ctx the parse tree
	 */
	void exitFact_columns_clause(OracleParser.Fact_columns_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#qry_transform_clause}.
	 * @param ctx the parse tree
	 */
	void enterQry_transform_clause(OracleParser.Qry_transform_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#qry_transform_clause}.
	 * @param ctx the parse tree
	 */
	void exitQry_transform_clause(OracleParser.Qry_transform_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_attribute_dimension}.
	 * @param ctx the parse tree
	 */
	void enterCreate_attribute_dimension(OracleParser.Create_attribute_dimensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_attribute_dimension}.
	 * @param ctx the parse tree
	 */
	void exitCreate_attribute_dimension(OracleParser.Create_attribute_dimensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ad_using_clause}.
	 * @param ctx the parse tree
	 */
	void enterAd_using_clause(OracleParser.Ad_using_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ad_using_clause}.
	 * @param ctx the parse tree
	 */
	void exitAd_using_clause(OracleParser.Ad_using_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#source_clause}.
	 * @param ctx the parse tree
	 */
	void enterSource_clause(OracleParser.Source_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#source_clause}.
	 * @param ctx the parse tree
	 */
	void exitSource_clause(OracleParser.Source_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#join_path_clause}.
	 * @param ctx the parse tree
	 */
	void enterJoin_path_clause(OracleParser.Join_path_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#join_path_clause}.
	 * @param ctx the parse tree
	 */
	void exitJoin_path_clause(OracleParser.Join_path_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#join_condition}.
	 * @param ctx the parse tree
	 */
	void enterJoin_condition(OracleParser.Join_conditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#join_condition}.
	 * @param ctx the parse tree
	 */
	void exitJoin_condition(OracleParser.Join_conditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#join_condition_item}.
	 * @param ctx the parse tree
	 */
	void enterJoin_condition_item(OracleParser.Join_condition_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#join_condition_item}.
	 * @param ctx the parse tree
	 */
	void exitJoin_condition_item(OracleParser.Join_condition_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterAttributes_clause(OracleParser.Attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitAttributes_clause(OracleParser.Attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ad_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterAd_attributes_clause(OracleParser.Ad_attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ad_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitAd_attributes_clause(OracleParser.Ad_attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ad_level_clause}.
	 * @param ctx the parse tree
	 */
	void enterAd_level_clause(OracleParser.Ad_level_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ad_level_clause}.
	 * @param ctx the parse tree
	 */
	void exitAd_level_clause(OracleParser.Ad_level_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#key_clause}.
	 * @param ctx the parse tree
	 */
	void enterKey_clause(OracleParser.Key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#key_clause}.
	 * @param ctx the parse tree
	 */
	void exitKey_clause(OracleParser.Key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alternate_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlternate_key_clause(OracleParser.Alternate_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alternate_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlternate_key_clause(OracleParser.Alternate_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dim_order_clause}.
	 * @param ctx the parse tree
	 */
	void enterDim_order_clause(OracleParser.Dim_order_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dim_order_clause}.
	 * @param ctx the parse tree
	 */
	void exitDim_order_clause(OracleParser.Dim_order_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#all_clause}.
	 * @param ctx the parse tree
	 */
	void enterAll_clause(OracleParser.All_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#all_clause}.
	 * @param ctx the parse tree
	 */
	void exitAll_clause(OracleParser.All_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_audit_policy}.
	 * @param ctx the parse tree
	 */
	void enterCreate_audit_policy(OracleParser.Create_audit_policyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_audit_policy}.
	 * @param ctx the parse tree
	 */
	void exitCreate_audit_policy(OracleParser.Create_audit_policyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#privilege_audit_clause}.
	 * @param ctx the parse tree
	 */
	void enterPrivilege_audit_clause(OracleParser.Privilege_audit_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#privilege_audit_clause}.
	 * @param ctx the parse tree
	 */
	void exitPrivilege_audit_clause(OracleParser.Privilege_audit_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#action_audit_clause}.
	 * @param ctx the parse tree
	 */
	void enterAction_audit_clause(OracleParser.Action_audit_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#action_audit_clause}.
	 * @param ctx the parse tree
	 */
	void exitAction_audit_clause(OracleParser.Action_audit_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#standard_actions}.
	 * @param ctx the parse tree
	 */
	void enterStandard_actions(OracleParser.Standard_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#standard_actions}.
	 * @param ctx the parse tree
	 */
	void exitStandard_actions(OracleParser.Standard_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#actions_clause}.
	 * @param ctx the parse tree
	 */
	void enterActions_clause(OracleParser.Actions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#actions_clause}.
	 * @param ctx the parse tree
	 */
	void exitActions_clause(OracleParser.Actions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_action}.
	 * @param ctx the parse tree
	 */
	void enterObject_action(OracleParser.Object_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_action}.
	 * @param ctx the parse tree
	 */
	void exitObject_action(OracleParser.Object_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#system_action}.
	 * @param ctx the parse tree
	 */
	void enterSystem_action(OracleParser.System_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#system_action}.
	 * @param ctx the parse tree
	 */
	void exitSystem_action(OracleParser.System_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#component_actions}.
	 * @param ctx the parse tree
	 */
	void enterComponent_actions(OracleParser.Component_actionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#component_actions}.
	 * @param ctx the parse tree
	 */
	void exitComponent_actions(OracleParser.Component_actionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#component_action}.
	 * @param ctx the parse tree
	 */
	void enterComponent_action(OracleParser.Component_actionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#component_action}.
	 * @param ctx the parse tree
	 */
	void exitComponent_action(OracleParser.Component_actionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#role_audit_clause}.
	 * @param ctx the parse tree
	 */
	void enterRole_audit_clause(OracleParser.Role_audit_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#role_audit_clause}.
	 * @param ctx the parse tree
	 */
	void exitRole_audit_clause(OracleParser.Role_audit_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_controlfile}.
	 * @param ctx the parse tree
	 */
	void enterCreate_controlfile(OracleParser.Create_controlfileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_controlfile}.
	 * @param ctx the parse tree
	 */
	void exitCreate_controlfile(OracleParser.Create_controlfileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#controlfile_options}.
	 * @param ctx the parse tree
	 */
	void enterControlfile_options(OracleParser.Controlfile_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#controlfile_options}.
	 * @param ctx the parse tree
	 */
	void exitControlfile_options(OracleParser.Controlfile_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logfile_clause}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_clause(OracleParser.Logfile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logfile_clause}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_clause(OracleParser.Logfile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#character_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterCharacter_set_clause(OracleParser.Character_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#character_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitCharacter_set_clause(OracleParser.Character_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#file_specification}.
	 * @param ctx the parse tree
	 */
	void enterFile_specification(OracleParser.File_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#file_specification}.
	 * @param ctx the parse tree
	 */
	void exitFile_specification(OracleParser.File_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_diskgroup}.
	 * @param ctx the parse tree
	 */
	void enterCreate_diskgroup(OracleParser.Create_diskgroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_diskgroup}.
	 * @param ctx the parse tree
	 */
	void exitCreate_diskgroup(OracleParser.Create_diskgroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#qualified_disk_clause}.
	 * @param ctx the parse tree
	 */
	void enterQualified_disk_clause(OracleParser.Qualified_disk_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#qualified_disk_clause}.
	 * @param ctx the parse tree
	 */
	void exitQualified_disk_clause(OracleParser.Qualified_disk_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_edition}.
	 * @param ctx the parse tree
	 */
	void enterCreate_edition(OracleParser.Create_editionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_edition}.
	 * @param ctx the parse tree
	 */
	void exitCreate_edition(OracleParser.Create_editionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_flashback_archive}.
	 * @param ctx the parse tree
	 */
	void enterCreate_flashback_archive(OracleParser.Create_flashback_archiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_flashback_archive}.
	 * @param ctx the parse tree
	 */
	void exitCreate_flashback_archive(OracleParser.Create_flashback_archiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#flashback_archive_quota}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_archive_quota(OracleParser.Flashback_archive_quotaContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#flashback_archive_quota}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_archive_quota(OracleParser.Flashback_archive_quotaContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#flashback_archive_retention}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_archive_retention(OracleParser.Flashback_archive_retentionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#flashback_archive_retention}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_archive_retention(OracleParser.Flashback_archive_retentionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_hierarchy}.
	 * @param ctx the parse tree
	 */
	void enterCreate_hierarchy(OracleParser.Create_hierarchyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_hierarchy}.
	 * @param ctx the parse tree
	 */
	void exitCreate_hierarchy(OracleParser.Create_hierarchyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hier_using_clause}.
	 * @param ctx the parse tree
	 */
	void enterHier_using_clause(OracleParser.Hier_using_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hier_using_clause}.
	 * @param ctx the parse tree
	 */
	void exitHier_using_clause(OracleParser.Hier_using_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#level_hier_clause}.
	 * @param ctx the parse tree
	 */
	void enterLevel_hier_clause(OracleParser.Level_hier_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#level_hier_clause}.
	 * @param ctx the parse tree
	 */
	void exitLevel_hier_clause(OracleParser.Level_hier_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hier_attrs_clause}.
	 * @param ctx the parse tree
	 */
	void enterHier_attrs_clause(OracleParser.Hier_attrs_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hier_attrs_clause}.
	 * @param ctx the parse tree
	 */
	void exitHier_attrs_clause(OracleParser.Hier_attrs_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hier_attr_clause}.
	 * @param ctx the parse tree
	 */
	void enterHier_attr_clause(OracleParser.Hier_attr_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hier_attr_clause}.
	 * @param ctx the parse tree
	 */
	void exitHier_attr_clause(OracleParser.Hier_attr_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hier_attr_name}.
	 * @param ctx the parse tree
	 */
	void enterHier_attr_name(OracleParser.Hier_attr_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hier_attr_name}.
	 * @param ctx the parse tree
	 */
	void exitHier_attr_name(OracleParser.Hier_attr_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_index}.
	 * @param ctx the parse tree
	 */
	void enterCreate_index(OracleParser.Create_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_index}.
	 * @param ctx the parse tree
	 */
	void exitCreate_index(OracleParser.Create_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cluster_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterCluster_index_clause(OracleParser.Cluster_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cluster_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitCluster_index_clause(OracleParser.Cluster_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cluster_name}.
	 * @param ctx the parse tree
	 */
	void enterCluster_name(OracleParser.Cluster_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cluster_name}.
	 * @param ctx the parse tree
	 */
	void exitCluster_name(OracleParser.Cluster_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterTable_index_clause(OracleParser.Table_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitTable_index_clause(OracleParser.Table_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#bitmap_join_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterBitmap_join_index_clause(OracleParser.Bitmap_join_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#bitmap_join_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitBitmap_join_index_clause(OracleParser.Bitmap_join_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_expr}.
	 * @param ctx the parse tree
	 */
	void enterIndex_expr(OracleParser.Index_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_expr}.
	 * @param ctx the parse tree
	 */
	void exitIndex_expr(OracleParser.Index_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_properties}.
	 * @param ctx the parse tree
	 */
	void enterIndex_properties(OracleParser.Index_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_properties}.
	 * @param ctx the parse tree
	 */
	void exitIndex_properties(OracleParser.Index_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterDomain_index_clause(OracleParser.Domain_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitDomain_index_clause(OracleParser.Domain_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#local_domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterLocal_domain_index_clause(OracleParser.Local_domain_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#local_domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitLocal_domain_index_clause(OracleParser.Local_domain_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void enterXmlindex_clause(OracleParser.Xmlindex_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void exitXmlindex_clause(OracleParser.Xmlindex_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#local_xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void enterLocal_xmlindex_clause(OracleParser.Local_xmlindex_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#local_xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void exitLocal_xmlindex_clause(OracleParser.Local_xmlindex_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#global_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_partitioned_index(OracleParser.Global_partitioned_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#global_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_partitioned_index(OracleParser.Global_partitioned_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_partitioning_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_partitioning_clause(OracleParser.Index_partitioning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_partitioning_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_partitioning_clause(OracleParser.Index_partitioning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#local_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void enterLocal_partitioned_index(OracleParser.Local_partitioned_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#local_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void exitLocal_partitioned_index(OracleParser.Local_partitioned_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_range_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_range_partitioned_table(OracleParser.On_range_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_range_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_range_partitioned_table(OracleParser.On_range_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_list_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_list_partitioned_table(OracleParser.On_list_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_list_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_list_partitioned_table(OracleParser.On_list_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterPartitioned_table(OracleParser.Partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitPartitioned_table(OracleParser.Partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_hash_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_hash_partitioned_table(OracleParser.On_hash_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_hash_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_hash_partitioned_table(OracleParser.On_hash_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_hash_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_hash_partitioned_clause(OracleParser.On_hash_partitioned_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_hash_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_hash_partitioned_clause(OracleParser.On_hash_partitioned_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_comp_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_comp_partitioned_table(OracleParser.On_comp_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_comp_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_comp_partitioned_table(OracleParser.On_comp_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_comp_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_comp_partitioned_clause(OracleParser.On_comp_partitioned_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_comp_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_comp_partitioned_clause(OracleParser.On_comp_partitioned_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_subpartition_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_subpartition_clause(OracleParser.Index_subpartition_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_subpartition_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_subpartition_clause(OracleParser.Index_subpartition_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_subpartition_subclause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_subpartition_subclause(OracleParser.Index_subpartition_subclauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_subpartition_subclause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_subpartition_subclause(OracleParser.Index_subpartition_subclauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#odci_parameters}.
	 * @param ctx the parse tree
	 */
	void enterOdci_parameters(OracleParser.Odci_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#odci_parameters}.
	 * @param ctx the parse tree
	 */
	void exitOdci_parameters(OracleParser.Odci_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#indextype}.
	 * @param ctx the parse tree
	 */
	void enterIndextype(OracleParser.IndextypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#indextype}.
	 * @param ctx the parse tree
	 */
	void exitIndextype(OracleParser.IndextypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_index}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index(OracleParser.Alter_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_index}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index(OracleParser.Alter_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_index_ops_set1}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_ops_set1(OracleParser.Alter_index_ops_set1Context ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_index_ops_set1}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_ops_set1(OracleParser.Alter_index_ops_set1Context ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_index_ops_set2}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_ops_set2(OracleParser.Alter_index_ops_set2Context ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_index_ops_set2}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_ops_set2(OracleParser.Alter_index_ops_set2Context ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#visible_or_invisible}.
	 * @param ctx the parse tree
	 */
	void enterVisible_or_invisible(OracleParser.Visible_or_invisibleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#visible_or_invisible}.
	 * @param ctx the parse tree
	 */
	void exitVisible_or_invisible(OracleParser.Visible_or_invisibleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#monitoring_nomonitoring}.
	 * @param ctx the parse tree
	 */
	void enterMonitoring_nomonitoring(OracleParser.Monitoring_nomonitoringContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#monitoring_nomonitoring}.
	 * @param ctx the parse tree
	 */
	void exitMonitoring_nomonitoring(OracleParser.Monitoring_nomonitoringContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rebuild_clause}.
	 * @param ctx the parse tree
	 */
	void enterRebuild_clause(OracleParser.Rebuild_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rebuild_clause}.
	 * @param ctx the parse tree
	 */
	void exitRebuild_clause(OracleParser.Rebuild_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_index_partitioning}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_partitioning(OracleParser.Alter_index_partitioningContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_index_partitioning}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_partitioning(OracleParser.Alter_index_partitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_index_default_attrs}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_default_attrs(OracleParser.Modify_index_default_attrsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_index_default_attrs}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_default_attrs(OracleParser.Modify_index_default_attrsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_hash_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterAdd_hash_index_partition(OracleParser.Add_hash_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_hash_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitAdd_hash_index_partition(OracleParser.Add_hash_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#coalesce_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterCoalesce_index_partition(OracleParser.Coalesce_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#coalesce_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitCoalesce_index_partition(OracleParser.Coalesce_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_partition(OracleParser.Modify_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_partition(OracleParser.Modify_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_index_partitions_ops}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_partitions_ops(OracleParser.Modify_index_partitions_opsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_index_partitions_ops}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_partitions_ops(OracleParser.Modify_index_partitions_opsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rename_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterRename_index_partition(OracleParser.Rename_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rename_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitRename_index_partition(OracleParser.Rename_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterDrop_index_partition(OracleParser.Drop_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitDrop_index_partition(OracleParser.Drop_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#split_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterSplit_index_partition(OracleParser.Split_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#split_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitSplit_index_partition(OracleParser.Split_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_partition_description}.
	 * @param ctx the parse tree
	 */
	void enterIndex_partition_description(OracleParser.Index_partition_descriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_partition_description}.
	 * @param ctx the parse tree
	 */
	void exitIndex_partition_description(OracleParser.Index_partition_descriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_index_subpartition}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_subpartition(OracleParser.Modify_index_subpartitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_index_subpartition}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_subpartition(OracleParser.Modify_index_subpartitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_name_old}.
	 * @param ctx the parse tree
	 */
	void enterPartition_name_old(OracleParser.Partition_name_oldContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_name_old}.
	 * @param ctx the parse tree
	 */
	void exitPartition_name_old(OracleParser.Partition_name_oldContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_partition_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_partition_name(OracleParser.New_partition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_partition_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_partition_name(OracleParser.New_partition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_index_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_index_name(OracleParser.New_index_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_index_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_index_name(OracleParser.New_index_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_inmemory_join_group}.
	 * @param ctx the parse tree
	 */
	void enterAlter_inmemory_join_group(OracleParser.Alter_inmemory_join_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_inmemory_join_group}.
	 * @param ctx the parse tree
	 */
	void exitAlter_inmemory_join_group(OracleParser.Alter_inmemory_join_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_user}.
	 * @param ctx the parse tree
	 */
	void enterCreate_user(OracleParser.Create_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_user}.
	 * @param ctx the parse tree
	 */
	void exitCreate_user(OracleParser.Create_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_user}.
	 * @param ctx the parse tree
	 */
	void enterAlter_user(OracleParser.Alter_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_user}.
	 * @param ctx the parse tree
	 */
	void exitAlter_user(OracleParser.Alter_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_user}.
	 * @param ctx the parse tree
	 */
	void enterDrop_user(OracleParser.Drop_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_user}.
	 * @param ctx the parse tree
	 */
	void exitDrop_user(OracleParser.Drop_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_identified_by}.
	 * @param ctx the parse tree
	 */
	void enterAlter_identified_by(OracleParser.Alter_identified_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_identified_by}.
	 * @param ctx the parse tree
	 */
	void exitAlter_identified_by(OracleParser.Alter_identified_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identified_by}.
	 * @param ctx the parse tree
	 */
	void enterIdentified_by(OracleParser.Identified_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identified_by}.
	 * @param ctx the parse tree
	 */
	void exitIdentified_by(OracleParser.Identified_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identified_other_clause}.
	 * @param ctx the parse tree
	 */
	void enterIdentified_other_clause(OracleParser.Identified_other_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identified_other_clause}.
	 * @param ctx the parse tree
	 */
	void exitIdentified_other_clause(OracleParser.Identified_other_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_tablespace_clause(OracleParser.User_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_tablespace_clause(OracleParser.User_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#quota_clause}.
	 * @param ctx the parse tree
	 */
	void enterQuota_clause(OracleParser.Quota_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#quota_clause}.
	 * @param ctx the parse tree
	 */
	void exitQuota_clause(OracleParser.Quota_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#profile_clause}.
	 * @param ctx the parse tree
	 */
	void enterProfile_clause(OracleParser.Profile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#profile_clause}.
	 * @param ctx the parse tree
	 */
	void exitProfile_clause(OracleParser.Profile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#role_clause}.
	 * @param ctx the parse tree
	 */
	void enterRole_clause(OracleParser.Role_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#role_clause}.
	 * @param ctx the parse tree
	 */
	void exitRole_clause(OracleParser.Role_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_default_role_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_default_role_clause(OracleParser.User_default_role_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_default_role_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_default_role_clause(OracleParser.User_default_role_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#password_expire_clause}.
	 * @param ctx the parse tree
	 */
	void enterPassword_expire_clause(OracleParser.Password_expire_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#password_expire_clause}.
	 * @param ctx the parse tree
	 */
	void exitPassword_expire_clause(OracleParser.Password_expire_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_lock_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_lock_clause(OracleParser.User_lock_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_lock_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_lock_clause(OracleParser.User_lock_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_editions_clause(OracleParser.User_editions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_editions_clause(OracleParser.User_editions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_user_editions_clause(OracleParser.Alter_user_editions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_user_editions_clause(OracleParser.Alter_user_editions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#proxy_clause}.
	 * @param ctx the parse tree
	 */
	void enterProxy_clause(OracleParser.Proxy_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#proxy_clause}.
	 * @param ctx the parse tree
	 */
	void exitProxy_clause(OracleParser.Proxy_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#container_names}.
	 * @param ctx the parse tree
	 */
	void enterContainer_names(OracleParser.Container_namesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#container_names}.
	 * @param ctx the parse tree
	 */
	void exitContainer_names(OracleParser.Container_namesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_container_data}.
	 * @param ctx the parse tree
	 */
	void enterSet_container_data(OracleParser.Set_container_dataContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_container_data}.
	 * @param ctx the parse tree
	 */
	void exitSet_container_data(OracleParser.Set_container_dataContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_rem_container_data}.
	 * @param ctx the parse tree
	 */
	void enterAdd_rem_container_data(OracleParser.Add_rem_container_dataContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_rem_container_data}.
	 * @param ctx the parse tree
	 */
	void exitAdd_rem_container_data(OracleParser.Add_rem_container_dataContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#container_data_clause}.
	 * @param ctx the parse tree
	 */
	void enterContainer_data_clause(OracleParser.Container_data_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#container_data_clause}.
	 * @param ctx the parse tree
	 */
	void exitContainer_data_clause(OracleParser.Container_data_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#administer_key_management}.
	 * @param ctx the parse tree
	 */
	void enterAdminister_key_management(OracleParser.Administer_key_managementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#administer_key_management}.
	 * @param ctx the parse tree
	 */
	void exitAdminister_key_management(OracleParser.Administer_key_managementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#keystore_management_clauses}.
	 * @param ctx the parse tree
	 */
	void enterKeystore_management_clauses(OracleParser.Keystore_management_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#keystore_management_clauses}.
	 * @param ctx the parse tree
	 */
	void exitKeystore_management_clauses(OracleParser.Keystore_management_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_keystore}.
	 * @param ctx the parse tree
	 */
	void enterCreate_keystore(OracleParser.Create_keystoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_keystore}.
	 * @param ctx the parse tree
	 */
	void exitCreate_keystore(OracleParser.Create_keystoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#open_keystore}.
	 * @param ctx the parse tree
	 */
	void enterOpen_keystore(OracleParser.Open_keystoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#open_keystore}.
	 * @param ctx the parse tree
	 */
	void exitOpen_keystore(OracleParser.Open_keystoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#force_keystore}.
	 * @param ctx the parse tree
	 */
	void enterForce_keystore(OracleParser.Force_keystoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#force_keystore}.
	 * @param ctx the parse tree
	 */
	void exitForce_keystore(OracleParser.Force_keystoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#close_keystore}.
	 * @param ctx the parse tree
	 */
	void enterClose_keystore(OracleParser.Close_keystoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#close_keystore}.
	 * @param ctx the parse tree
	 */
	void exitClose_keystore(OracleParser.Close_keystoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#backup_keystore}.
	 * @param ctx the parse tree
	 */
	void enterBackup_keystore(OracleParser.Backup_keystoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#backup_keystore}.
	 * @param ctx the parse tree
	 */
	void exitBackup_keystore(OracleParser.Backup_keystoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_keystore_password}.
	 * @param ctx the parse tree
	 */
	void enterAlter_keystore_password(OracleParser.Alter_keystore_passwordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_keystore_password}.
	 * @param ctx the parse tree
	 */
	void exitAlter_keystore_password(OracleParser.Alter_keystore_passwordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_into_new_keystore}.
	 * @param ctx the parse tree
	 */
	void enterMerge_into_new_keystore(OracleParser.Merge_into_new_keystoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_into_new_keystore}.
	 * @param ctx the parse tree
	 */
	void exitMerge_into_new_keystore(OracleParser.Merge_into_new_keystoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_into_existing_keystore}.
	 * @param ctx the parse tree
	 */
	void enterMerge_into_existing_keystore(OracleParser.Merge_into_existing_keystoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_into_existing_keystore}.
	 * @param ctx the parse tree
	 */
	void exitMerge_into_existing_keystore(OracleParser.Merge_into_existing_keystoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#isolate_keystore}.
	 * @param ctx the parse tree
	 */
	void enterIsolate_keystore(OracleParser.Isolate_keystoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#isolate_keystore}.
	 * @param ctx the parse tree
	 */
	void exitIsolate_keystore(OracleParser.Isolate_keystoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unite_keystore}.
	 * @param ctx the parse tree
	 */
	void enterUnite_keystore(OracleParser.Unite_keystoreContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unite_keystore}.
	 * @param ctx the parse tree
	 */
	void exitUnite_keystore(OracleParser.Unite_keystoreContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#key_management_clauses}.
	 * @param ctx the parse tree
	 */
	void enterKey_management_clauses(OracleParser.Key_management_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#key_management_clauses}.
	 * @param ctx the parse tree
	 */
	void exitKey_management_clauses(OracleParser.Key_management_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_key}.
	 * @param ctx the parse tree
	 */
	void enterSet_key(OracleParser.Set_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_key}.
	 * @param ctx the parse tree
	 */
	void exitSet_key(OracleParser.Set_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_key}.
	 * @param ctx the parse tree
	 */
	void enterCreate_key(OracleParser.Create_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_key}.
	 * @param ctx the parse tree
	 */
	void exitCreate_key(OracleParser.Create_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#mkid}.
	 * @param ctx the parse tree
	 */
	void enterMkid(OracleParser.MkidContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#mkid}.
	 * @param ctx the parse tree
	 */
	void exitMkid(OracleParser.MkidContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#mk}.
	 * @param ctx the parse tree
	 */
	void enterMk(OracleParser.MkContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#mk}.
	 * @param ctx the parse tree
	 */
	void exitMk(OracleParser.MkContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#use_key}.
	 * @param ctx the parse tree
	 */
	void enterUse_key(OracleParser.Use_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#use_key}.
	 * @param ctx the parse tree
	 */
	void exitUse_key(OracleParser.Use_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_key_tag}.
	 * @param ctx the parse tree
	 */
	void enterSet_key_tag(OracleParser.Set_key_tagContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_key_tag}.
	 * @param ctx the parse tree
	 */
	void exitSet_key_tag(OracleParser.Set_key_tagContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#export_keys}.
	 * @param ctx the parse tree
	 */
	void enterExport_keys(OracleParser.Export_keysContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#export_keys}.
	 * @param ctx the parse tree
	 */
	void exitExport_keys(OracleParser.Export_keysContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#import_keys}.
	 * @param ctx the parse tree
	 */
	void enterImport_keys(OracleParser.Import_keysContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#import_keys}.
	 * @param ctx the parse tree
	 */
	void exitImport_keys(OracleParser.Import_keysContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#migrate_keys}.
	 * @param ctx the parse tree
	 */
	void enterMigrate_keys(OracleParser.Migrate_keysContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#migrate_keys}.
	 * @param ctx the parse tree
	 */
	void exitMigrate_keys(OracleParser.Migrate_keysContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#reverse_migrate_keys}.
	 * @param ctx the parse tree
	 */
	void enterReverse_migrate_keys(OracleParser.Reverse_migrate_keysContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#reverse_migrate_keys}.
	 * @param ctx the parse tree
	 */
	void exitReverse_migrate_keys(OracleParser.Reverse_migrate_keysContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#move_keys}.
	 * @param ctx the parse tree
	 */
	void enterMove_keys(OracleParser.Move_keysContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#move_keys}.
	 * @param ctx the parse tree
	 */
	void exitMove_keys(OracleParser.Move_keysContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identified_by_store}.
	 * @param ctx the parse tree
	 */
	void enterIdentified_by_store(OracleParser.Identified_by_storeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identified_by_store}.
	 * @param ctx the parse tree
	 */
	void exitIdentified_by_store(OracleParser.Identified_by_storeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_algorithm_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_algorithm_clause(OracleParser.Using_algorithm_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_algorithm_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_algorithm_clause(OracleParser.Using_algorithm_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_tag_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_tag_clause(OracleParser.Using_tag_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_tag_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_tag_clause(OracleParser.Using_tag_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#secret_management_clauses}.
	 * @param ctx the parse tree
	 */
	void enterSecret_management_clauses(OracleParser.Secret_management_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#secret_management_clauses}.
	 * @param ctx the parse tree
	 */
	void exitSecret_management_clauses(OracleParser.Secret_management_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_update_secret}.
	 * @param ctx the parse tree
	 */
	void enterAdd_update_secret(OracleParser.Add_update_secretContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_update_secret}.
	 * @param ctx the parse tree
	 */
	void exitAdd_update_secret(OracleParser.Add_update_secretContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#delete_secret}.
	 * @param ctx the parse tree
	 */
	void enterDelete_secret(OracleParser.Delete_secretContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#delete_secret}.
	 * @param ctx the parse tree
	 */
	void exitDelete_secret(OracleParser.Delete_secretContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_update_secret_seps}.
	 * @param ctx the parse tree
	 */
	void enterAdd_update_secret_seps(OracleParser.Add_update_secret_sepsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_update_secret_seps}.
	 * @param ctx the parse tree
	 */
	void exitAdd_update_secret_seps(OracleParser.Add_update_secret_sepsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#delete_secret_seps}.
	 * @param ctx the parse tree
	 */
	void enterDelete_secret_seps(OracleParser.Delete_secret_sepsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#delete_secret_seps}.
	 * @param ctx the parse tree
	 */
	void exitDelete_secret_seps(OracleParser.Delete_secret_sepsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#zero_downtime_software_patching_clauses}.
	 * @param ctx the parse tree
	 */
	void enterZero_downtime_software_patching_clauses(OracleParser.Zero_downtime_software_patching_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#zero_downtime_software_patching_clauses}.
	 * @param ctx the parse tree
	 */
	void exitZero_downtime_software_patching_clauses(OracleParser.Zero_downtime_software_patching_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#with_backup_clause}.
	 * @param ctx the parse tree
	 */
	void enterWith_backup_clause(OracleParser.With_backup_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#with_backup_clause}.
	 * @param ctx the parse tree
	 */
	void exitWith_backup_clause(OracleParser.With_backup_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identified_by_password_clause}.
	 * @param ctx the parse tree
	 */
	void enterIdentified_by_password_clause(OracleParser.Identified_by_password_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identified_by_password_clause}.
	 * @param ctx the parse tree
	 */
	void exitIdentified_by_password_clause(OracleParser.Identified_by_password_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#keystore_password}.
	 * @param ctx the parse tree
	 */
	void enterKeystore_password(OracleParser.Keystore_passwordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#keystore_password}.
	 * @param ctx the parse tree
	 */
	void exitKeystore_password(OracleParser.Keystore_passwordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#path}.
	 * @param ctx the parse tree
	 */
	void enterPath(OracleParser.PathContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#path}.
	 * @param ctx the parse tree
	 */
	void exitPath(OracleParser.PathContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#secret}.
	 * @param ctx the parse tree
	 */
	void enterSecret(OracleParser.SecretContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#secret}.
	 * @param ctx the parse tree
	 */
	void exitSecret(OracleParser.SecretContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#analyze}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze(OracleParser.AnalyzeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#analyze}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze(OracleParser.AnalyzeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_extention_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartition_extention_clause(OracleParser.Partition_extention_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_extention_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartition_extention_clause(OracleParser.Partition_extention_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#validation_clauses}.
	 * @param ctx the parse tree
	 */
	void enterValidation_clauses(OracleParser.Validation_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#validation_clauses}.
	 * @param ctx the parse tree
	 */
	void exitValidation_clauses(OracleParser.Validation_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compute_clauses}.
	 * @param ctx the parse tree
	 */
	void enterCompute_clauses(OracleParser.Compute_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compute_clauses}.
	 * @param ctx the parse tree
	 */
	void exitCompute_clauses(OracleParser.Compute_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#for_clause}.
	 * @param ctx the parse tree
	 */
	void enterFor_clause(OracleParser.For_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#for_clause}.
	 * @param ctx the parse tree
	 */
	void exitFor_clause(OracleParser.For_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#online_or_offline}.
	 * @param ctx the parse tree
	 */
	void enterOnline_or_offline(OracleParser.Online_or_offlineContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#online_or_offline}.
	 * @param ctx the parse tree
	 */
	void exitOnline_or_offline(OracleParser.Online_or_offlineContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#into_clause1}.
	 * @param ctx the parse tree
	 */
	void enterInto_clause1(OracleParser.Into_clause1Context ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#into_clause1}.
	 * @param ctx the parse tree
	 */
	void exitInto_clause1(OracleParser.Into_clause1Context ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_key_value}.
	 * @param ctx the parse tree
	 */
	void enterPartition_key_value(OracleParser.Partition_key_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_key_value}.
	 * @param ctx the parse tree
	 */
	void exitPartition_key_value(OracleParser.Partition_key_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_key_value}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_key_value(OracleParser.Subpartition_key_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_key_value}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_key_value(OracleParser.Subpartition_key_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#associate_statistics}.
	 * @param ctx the parse tree
	 */
	void enterAssociate_statistics(OracleParser.Associate_statisticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#associate_statistics}.
	 * @param ctx the parse tree
	 */
	void exitAssociate_statistics(OracleParser.Associate_statisticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_association}.
	 * @param ctx the parse tree
	 */
	void enterColumn_association(OracleParser.Column_associationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_association}.
	 * @param ctx the parse tree
	 */
	void exitColumn_association(OracleParser.Column_associationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_association}.
	 * @param ctx the parse tree
	 */
	void enterFunction_association(OracleParser.Function_associationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_association}.
	 * @param ctx the parse tree
	 */
	void exitFunction_association(OracleParser.Function_associationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#indextype_name}.
	 * @param ctx the parse tree
	 */
	void enterIndextype_name(OracleParser.Indextype_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#indextype_name}.
	 * @param ctx the parse tree
	 */
	void exitIndextype_name(OracleParser.Indextype_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_statistics_type}.
	 * @param ctx the parse tree
	 */
	void enterUsing_statistics_type(OracleParser.Using_statistics_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_statistics_type}.
	 * @param ctx the parse tree
	 */
	void exitUsing_statistics_type(OracleParser.Using_statistics_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#statistics_type_name}.
	 * @param ctx the parse tree
	 */
	void enterStatistics_type_name(OracleParser.Statistics_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#statistics_type_name}.
	 * @param ctx the parse tree
	 */
	void exitStatistics_type_name(OracleParser.Statistics_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_cost_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_cost_clause(OracleParser.Default_cost_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_cost_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_cost_clause(OracleParser.Default_cost_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cpu_cost}.
	 * @param ctx the parse tree
	 */
	void enterCpu_cost(OracleParser.Cpu_costContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cpu_cost}.
	 * @param ctx the parse tree
	 */
	void exitCpu_cost(OracleParser.Cpu_costContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#io_cost}.
	 * @param ctx the parse tree
	 */
	void enterIo_cost(OracleParser.Io_costContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#io_cost}.
	 * @param ctx the parse tree
	 */
	void exitIo_cost(OracleParser.Io_costContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#network_cost}.
	 * @param ctx the parse tree
	 */
	void enterNetwork_cost(OracleParser.Network_costContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#network_cost}.
	 * @param ctx the parse tree
	 */
	void exitNetwork_cost(OracleParser.Network_costContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_selectivity_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_selectivity_clause(OracleParser.Default_selectivity_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_selectivity_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_selectivity_clause(OracleParser.Default_selectivity_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_selectivity}.
	 * @param ctx the parse tree
	 */
	void enterDefault_selectivity(OracleParser.Default_selectivityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_selectivity}.
	 * @param ctx the parse tree
	 */
	void exitDefault_selectivity(OracleParser.Default_selectivityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#storage_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterStorage_table_clause(OracleParser.Storage_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#storage_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitStorage_table_clause(OracleParser.Storage_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unified_auditing}.
	 * @param ctx the parse tree
	 */
	void enterUnified_auditing(OracleParser.Unified_auditingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unified_auditing}.
	 * @param ctx the parse tree
	 */
	void exitUnified_auditing(OracleParser.Unified_auditingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#policy_name}.
	 * @param ctx the parse tree
	 */
	void enterPolicy_name(OracleParser.Policy_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#policy_name}.
	 * @param ctx the parse tree
	 */
	void exitPolicy_name(OracleParser.Policy_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_traditional}.
	 * @param ctx the parse tree
	 */
	void enterAudit_traditional(OracleParser.Audit_traditionalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_traditional}.
	 * @param ctx the parse tree
	 */
	void exitAudit_traditional(OracleParser.Audit_traditionalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_direct_path}.
	 * @param ctx the parse tree
	 */
	void enterAudit_direct_path(OracleParser.Audit_direct_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_direct_path}.
	 * @param ctx the parse tree
	 */
	void exitAudit_direct_path(OracleParser.Audit_direct_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_container_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_container_clause(OracleParser.Audit_container_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_container_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_container_clause(OracleParser.Audit_container_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_operation_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_operation_clause(OracleParser.Audit_operation_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_operation_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_operation_clause(OracleParser.Audit_operation_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#auditing_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterAuditing_by_clause(OracleParser.Auditing_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#auditing_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitAuditing_by_clause(OracleParser.Auditing_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_user}.
	 * @param ctx the parse tree
	 */
	void enterAudit_user(OracleParser.Audit_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_user}.
	 * @param ctx the parse tree
	 */
	void exitAudit_user(OracleParser.Audit_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_schema_object_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_schema_object_clause(OracleParser.Audit_schema_object_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_schema_object_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_schema_object_clause(OracleParser.Audit_schema_object_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_operation}.
	 * @param ctx the parse tree
	 */
	void enterSql_operation(OracleParser.Sql_operationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_operation}.
	 * @param ctx the parse tree
	 */
	void exitSql_operation(OracleParser.Sql_operationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#auditing_on_clause}.
	 * @param ctx the parse tree
	 */
	void enterAuditing_on_clause(OracleParser.Auditing_on_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#auditing_on_clause}.
	 * @param ctx the parse tree
	 */
	void exitAuditing_on_clause(OracleParser.Auditing_on_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_name}.
	 * @param ctx the parse tree
	 */
	void enterModel_name(OracleParser.Model_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_name}.
	 * @param ctx the parse tree
	 */
	void exitModel_name(OracleParser.Model_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_name}.
	 * @param ctx the parse tree
	 */
	void enterObject_name(OracleParser.Object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_name}.
	 * @param ctx the parse tree
	 */
	void exitObject_name(OracleParser.Object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#profile_name}.
	 * @param ctx the parse tree
	 */
	void enterProfile_name(OracleParser.Profile_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#profile_name}.
	 * @param ctx the parse tree
	 */
	void exitProfile_name(OracleParser.Profile_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_statement_shortcut}.
	 * @param ctx the parse tree
	 */
	void enterSql_statement_shortcut(OracleParser.Sql_statement_shortcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_statement_shortcut}.
	 * @param ctx the parse tree
	 */
	void exitSql_statement_shortcut(OracleParser.Sql_statement_shortcutContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_index}.
	 * @param ctx the parse tree
	 */
	void enterDrop_index(OracleParser.Drop_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_index}.
	 * @param ctx the parse tree
	 */
	void exitDrop_index(OracleParser.Drop_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#disassociate_statistics}.
	 * @param ctx the parse tree
	 */
	void enterDisassociate_statistics(OracleParser.Disassociate_statisticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#disassociate_statistics}.
	 * @param ctx the parse tree
	 */
	void exitDisassociate_statistics(OracleParser.Disassociate_statisticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_indextype}.
	 * @param ctx the parse tree
	 */
	void enterDrop_indextype(OracleParser.Drop_indextypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_indextype}.
	 * @param ctx the parse tree
	 */
	void exitDrop_indextype(OracleParser.Drop_indextypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_inmemory_join_group}.
	 * @param ctx the parse tree
	 */
	void enterDrop_inmemory_join_group(OracleParser.Drop_inmemory_join_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_inmemory_join_group}.
	 * @param ctx the parse tree
	 */
	void exitDrop_inmemory_join_group(OracleParser.Drop_inmemory_join_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#flashback_table}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_table(OracleParser.Flashback_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#flashback_table}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_table(OracleParser.Flashback_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#restore_point}.
	 * @param ctx the parse tree
	 */
	void enterRestore_point(OracleParser.Restore_pointContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#restore_point}.
	 * @param ctx the parse tree
	 */
	void exitRestore_point(OracleParser.Restore_pointContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#purge_statement}.
	 * @param ctx the parse tree
	 */
	void enterPurge_statement(OracleParser.Purge_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#purge_statement}.
	 * @param ctx the parse tree
	 */
	void exitPurge_statement(OracleParser.Purge_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#noaudit_statement}.
	 * @param ctx the parse tree
	 */
	void enterNoaudit_statement(OracleParser.Noaudit_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#noaudit_statement}.
	 * @param ctx the parse tree
	 */
	void exitNoaudit_statement(OracleParser.Noaudit_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rename_object}.
	 * @param ctx the parse tree
	 */
	void enterRename_object(OracleParser.Rename_objectContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rename_object}.
	 * @param ctx the parse tree
	 */
	void exitRename_object(OracleParser.Rename_objectContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#grant_statement}.
	 * @param ctx the parse tree
	 */
	void enterGrant_statement(OracleParser.Grant_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#grant_statement}.
	 * @param ctx the parse tree
	 */
	void exitGrant_statement(OracleParser.Grant_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#container_clause}.
	 * @param ctx the parse tree
	 */
	void enterContainer_clause(OracleParser.Container_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#container_clause}.
	 * @param ctx the parse tree
	 */
	void exitContainer_clause(OracleParser.Container_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#revoke_statement}.
	 * @param ctx the parse tree
	 */
	void enterRevoke_statement(OracleParser.Revoke_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#revoke_statement}.
	 * @param ctx the parse tree
	 */
	void exitRevoke_statement(OracleParser.Revoke_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#revoke_system_privilege}.
	 * @param ctx the parse tree
	 */
	void enterRevoke_system_privilege(OracleParser.Revoke_system_privilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#revoke_system_privilege}.
	 * @param ctx the parse tree
	 */
	void exitRevoke_system_privilege(OracleParser.Revoke_system_privilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#revokee_clause}.
	 * @param ctx the parse tree
	 */
	void enterRevokee_clause(OracleParser.Revokee_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#revokee_clause}.
	 * @param ctx the parse tree
	 */
	void exitRevokee_clause(OracleParser.Revokee_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#revoke_object_privileges}.
	 * @param ctx the parse tree
	 */
	void enterRevoke_object_privileges(OracleParser.Revoke_object_privilegesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#revoke_object_privileges}.
	 * @param ctx the parse tree
	 */
	void exitRevoke_object_privileges(OracleParser.Revoke_object_privilegesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_object_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_object_clause(OracleParser.On_object_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_object_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_object_clause(OracleParser.On_object_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#revoke_roles_from_programs}.
	 * @param ctx the parse tree
	 */
	void enterRevoke_roles_from_programs(OracleParser.Revoke_roles_from_programsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#revoke_roles_from_programs}.
	 * @param ctx the parse tree
	 */
	void exitRevoke_roles_from_programs(OracleParser.Revoke_roles_from_programsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#program_unit}.
	 * @param ctx the parse tree
	 */
	void enterProgram_unit(OracleParser.Program_unitContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#program_unit}.
	 * @param ctx the parse tree
	 */
	void exitProgram_unit(OracleParser.Program_unitContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_dimension}.
	 * @param ctx the parse tree
	 */
	void enterCreate_dimension(OracleParser.Create_dimensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_dimension}.
	 * @param ctx the parse tree
	 */
	void exitCreate_dimension(OracleParser.Create_dimensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_directory}.
	 * @param ctx the parse tree
	 */
	void enterCreate_directory(OracleParser.Create_directoryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_directory}.
	 * @param ctx the parse tree
	 */
	void exitCreate_directory(OracleParser.Create_directoryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#directory_name}.
	 * @param ctx the parse tree
	 */
	void enterDirectory_name(OracleParser.Directory_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#directory_name}.
	 * @param ctx the parse tree
	 */
	void exitDirectory_name(OracleParser.Directory_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#directory_path}.
	 * @param ctx the parse tree
	 */
	void enterDirectory_path(OracleParser.Directory_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#directory_path}.
	 * @param ctx the parse tree
	 */
	void exitDirectory_path(OracleParser.Directory_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_inmemory_join_group}.
	 * @param ctx the parse tree
	 */
	void enterCreate_inmemory_join_group(OracleParser.Create_inmemory_join_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_inmemory_join_group}.
	 * @param ctx the parse tree
	 */
	void exitCreate_inmemory_join_group(OracleParser.Create_inmemory_join_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_hierarchy}.
	 * @param ctx the parse tree
	 */
	void enterDrop_hierarchy(OracleParser.Drop_hierarchyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_hierarchy}.
	 * @param ctx the parse tree
	 */
	void exitDrop_hierarchy(OracleParser.Drop_hierarchyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_library}.
	 * @param ctx the parse tree
	 */
	void enterAlter_library(OracleParser.Alter_libraryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_library}.
	 * @param ctx the parse tree
	 */
	void exitAlter_library(OracleParser.Alter_libraryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_java}.
	 * @param ctx the parse tree
	 */
	void enterDrop_java(OracleParser.Drop_javaContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_java}.
	 * @param ctx the parse tree
	 */
	void exitDrop_java(OracleParser.Drop_javaContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_library}.
	 * @param ctx the parse tree
	 */
	void enterDrop_library(OracleParser.Drop_libraryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_library}.
	 * @param ctx the parse tree
	 */
	void exitDrop_library(OracleParser.Drop_libraryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_java}.
	 * @param ctx the parse tree
	 */
	void enterCreate_java(OracleParser.Create_javaContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_java}.
	 * @param ctx the parse tree
	 */
	void exitCreate_java(OracleParser.Create_javaContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_library}.
	 * @param ctx the parse tree
	 */
	void enterCreate_library(OracleParser.Create_libraryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_library}.
	 * @param ctx the parse tree
	 */
	void exitCreate_library(OracleParser.Create_libraryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#plsql_library_source}.
	 * @param ctx the parse tree
	 */
	void enterPlsql_library_source(OracleParser.Plsql_library_sourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#plsql_library_source}.
	 * @param ctx the parse tree
	 */
	void exitPlsql_library_source(OracleParser.Plsql_library_sourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#credential_name}.
	 * @param ctx the parse tree
	 */
	void enterCredential_name(OracleParser.Credential_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#credential_name}.
	 * @param ctx the parse tree
	 */
	void exitCredential_name(OracleParser.Credential_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#library_editionable}.
	 * @param ctx the parse tree
	 */
	void enterLibrary_editionable(OracleParser.Library_editionableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#library_editionable}.
	 * @param ctx the parse tree
	 */
	void exitLibrary_editionable(OracleParser.Library_editionableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#library_debug}.
	 * @param ctx the parse tree
	 */
	void enterLibrary_debug(OracleParser.Library_debugContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#library_debug}.
	 * @param ctx the parse tree
	 */
	void exitLibrary_debug(OracleParser.Library_debugContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compiler_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void enterCompiler_parameters_clause(OracleParser.Compiler_parameters_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compiler_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void exitCompiler_parameters_clause(OracleParser.Compiler_parameters_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parameter_value}.
	 * @param ctx the parse tree
	 */
	void enterParameter_value(OracleParser.Parameter_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parameter_value}.
	 * @param ctx the parse tree
	 */
	void exitParameter_value(OracleParser.Parameter_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#library_name}.
	 * @param ctx the parse tree
	 */
	void enterLibrary_name(OracleParser.Library_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#library_name}.
	 * @param ctx the parse tree
	 */
	void exitLibrary_name(OracleParser.Library_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_dimension}.
	 * @param ctx the parse tree
	 */
	void enterAlter_dimension(OracleParser.Alter_dimensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_dimension}.
	 * @param ctx the parse tree
	 */
	void exitAlter_dimension(OracleParser.Alter_dimensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#level_clause}.
	 * @param ctx the parse tree
	 */
	void enterLevel_clause(OracleParser.Level_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#level_clause}.
	 * @param ctx the parse tree
	 */
	void exitLevel_clause(OracleParser.Level_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hierarchy_clause}.
	 * @param ctx the parse tree
	 */
	void enterHierarchy_clause(OracleParser.Hierarchy_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hierarchy_clause}.
	 * @param ctx the parse tree
	 */
	void exitHierarchy_clause(OracleParser.Hierarchy_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dimension_join_clause}.
	 * @param ctx the parse tree
	 */
	void enterDimension_join_clause(OracleParser.Dimension_join_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dimension_join_clause}.
	 * @param ctx the parse tree
	 */
	void exitDimension_join_clause(OracleParser.Dimension_join_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#attribute_clause}.
	 * @param ctx the parse tree
	 */
	void enterAttribute_clause(OracleParser.Attribute_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#attribute_clause}.
	 * @param ctx the parse tree
	 */
	void exitAttribute_clause(OracleParser.Attribute_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#extended_attribute_clause}.
	 * @param ctx the parse tree
	 */
	void enterExtended_attribute_clause(OracleParser.Extended_attribute_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#extended_attribute_clause}.
	 * @param ctx the parse tree
	 */
	void exitExtended_attribute_clause(OracleParser.Extended_attribute_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_one_or_more_sub_clause}.
	 * @param ctx the parse tree
	 */
	void enterColumn_one_or_more_sub_clause(OracleParser.Column_one_or_more_sub_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_one_or_more_sub_clause}.
	 * @param ctx the parse tree
	 */
	void exitColumn_one_or_more_sub_clause(OracleParser.Column_one_or_more_sub_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_view}.
	 * @param ctx the parse tree
	 */
	void enterAlter_view(OracleParser.Alter_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_view}.
	 * @param ctx the parse tree
	 */
	void exitAlter_view(OracleParser.Alter_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_view_editionable}.
	 * @param ctx the parse tree
	 */
	void enterAlter_view_editionable(OracleParser.Alter_view_editionableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_view_editionable}.
	 * @param ctx the parse tree
	 */
	void exitAlter_view_editionable(OracleParser.Alter_view_editionableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_view}.
	 * @param ctx the parse tree
	 */
	void enterCreate_view(OracleParser.Create_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_view}.
	 * @param ctx the parse tree
	 */
	void exitCreate_view(OracleParser.Create_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#editioning_clause}.
	 * @param ctx the parse tree
	 */
	void enterEditioning_clause(OracleParser.Editioning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#editioning_clause}.
	 * @param ctx the parse tree
	 */
	void exitEditioning_clause(OracleParser.Editioning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#view_options}.
	 * @param ctx the parse tree
	 */
	void enterView_options(OracleParser.View_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#view_options}.
	 * @param ctx the parse tree
	 */
	void exitView_options(OracleParser.View_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#view_alias_constraint}.
	 * @param ctx the parse tree
	 */
	void enterView_alias_constraint(OracleParser.View_alias_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#view_alias_constraint}.
	 * @param ctx the parse tree
	 */
	void exitView_alias_constraint(OracleParser.View_alias_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_view_clause}.
	 * @param ctx the parse tree
	 */
	void enterObject_view_clause(OracleParser.Object_view_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_view_clause}.
	 * @param ctx the parse tree
	 */
	void exitObject_view_clause(OracleParser.Object_view_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inline_constraint}.
	 * @param ctx the parse tree
	 */
	void enterInline_constraint(OracleParser.Inline_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inline_constraint}.
	 * @param ctx the parse tree
	 */
	void exitInline_constraint(OracleParser.Inline_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inline_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void enterInline_ref_constraint(OracleParser.Inline_ref_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inline_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void exitInline_ref_constraint(OracleParser.Inline_ref_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#out_of_line_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_ref_constraint(OracleParser.Out_of_line_ref_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#out_of_line_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_ref_constraint(OracleParser.Out_of_line_ref_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#out_of_line_constraint}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_constraint(OracleParser.Out_of_line_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#out_of_line_constraint}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_constraint(OracleParser.Out_of_line_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constraint_state}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_state(OracleParser.Constraint_stateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constraint_state}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_state(OracleParser.Constraint_stateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltype_view_clause}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_view_clause(OracleParser.Xmltype_view_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltype_view_clause}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_view_clause(OracleParser.Xmltype_view_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_schema_spec}.
	 * @param ctx the parse tree
	 */
	void enterXml_schema_spec(OracleParser.Xml_schema_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_schema_spec}.
	 * @param ctx the parse tree
	 */
	void exitXml_schema_spec(OracleParser.Xml_schema_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_schema_url}.
	 * @param ctx the parse tree
	 */
	void enterXml_schema_url(OracleParser.Xml_schema_urlContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_schema_url}.
	 * @param ctx the parse tree
	 */
	void exitXml_schema_url(OracleParser.Xml_schema_urlContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#element}.
	 * @param ctx the parse tree
	 */
	void enterElement(OracleParser.ElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#element}.
	 * @param ctx the parse tree
	 */
	void exitElement(OracleParser.ElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablespace(OracleParser.Alter_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablespace(OracleParser.Alter_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datafile_tempfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDatafile_tempfile_clauses(OracleParser.Datafile_tempfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datafile_tempfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDatafile_tempfile_clauses(OracleParser.Datafile_tempfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_logging_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_logging_clauses(OracleParser.Tablespace_logging_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_logging_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_logging_clauses(OracleParser.Tablespace_logging_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_group_clause}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_group_clause(OracleParser.Tablespace_group_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_group_clause}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_group_clause(OracleParser.Tablespace_group_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_group_name}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_group_name(OracleParser.Tablespace_group_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_group_name}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_group_name(OracleParser.Tablespace_group_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_state_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_state_clauses(OracleParser.Tablespace_state_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_state_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_state_clauses(OracleParser.Tablespace_state_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#flashback_mode_clause}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_mode_clause(OracleParser.Flashback_mode_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#flashback_mode_clause}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_mode_clause(OracleParser.Flashback_mode_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_tablespace_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_tablespace_name(OracleParser.New_tablespace_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_tablespace_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_tablespace_name(OracleParser.New_tablespace_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterCreate_tablespace(OracleParser.Create_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitCreate_tablespace(OracleParser.Create_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#permanent_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterPermanent_tablespace_clause(OracleParser.Permanent_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#permanent_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitPermanent_tablespace_clause(OracleParser.Permanent_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_encryption_spec}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_encryption_spec(OracleParser.Tablespace_encryption_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_encryption_spec}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_encryption_spec(OracleParser.Tablespace_encryption_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logging_clause}.
	 * @param ctx the parse tree
	 */
	void enterLogging_clause(OracleParser.Logging_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logging_clause}.
	 * @param ctx the parse tree
	 */
	void exitLogging_clause(OracleParser.Logging_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#extent_management_clause}.
	 * @param ctx the parse tree
	 */
	void enterExtent_management_clause(OracleParser.Extent_management_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#extent_management_clause}.
	 * @param ctx the parse tree
	 */
	void exitExtent_management_clause(OracleParser.Extent_management_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#segment_management_clause}.
	 * @param ctx the parse tree
	 */
	void enterSegment_management_clause(OracleParser.Segment_management_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#segment_management_clause}.
	 * @param ctx the parse tree
	 */
	void exitSegment_management_clause(OracleParser.Segment_management_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#temporary_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterTemporary_tablespace_clause(OracleParser.Temporary_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#temporary_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitTemporary_tablespace_clause(OracleParser.Temporary_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#undo_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterUndo_tablespace_clause(OracleParser.Undo_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#undo_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitUndo_tablespace_clause(OracleParser.Undo_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_retention_clause}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_retention_clause(OracleParser.Tablespace_retention_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_retention_clause}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_retention_clause(OracleParser.Tablespace_retention_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_tablespace_set}.
	 * @param ctx the parse tree
	 */
	void enterCreate_tablespace_set(OracleParser.Create_tablespace_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_tablespace_set}.
	 * @param ctx the parse tree
	 */
	void exitCreate_tablespace_set(OracleParser.Create_tablespace_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#permanent_tablespace_attrs}.
	 * @param ctx the parse tree
	 */
	void enterPermanent_tablespace_attrs(OracleParser.Permanent_tablespace_attrsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#permanent_tablespace_attrs}.
	 * @param ctx the parse tree
	 */
	void exitPermanent_tablespace_attrs(OracleParser.Permanent_tablespace_attrsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_encryption_clause}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_encryption_clause(OracleParser.Tablespace_encryption_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_encryption_clause}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_encryption_clause(OracleParser.Tablespace_encryption_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_tablespace_params}.
	 * @param ctx the parse tree
	 */
	void enterDefault_tablespace_params(OracleParser.Default_tablespace_paramsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_tablespace_params}.
	 * @param ctx the parse tree
	 */
	void exitDefault_tablespace_params(OracleParser.Default_tablespace_paramsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_table_compression}.
	 * @param ctx the parse tree
	 */
	void enterDefault_table_compression(OracleParser.Default_table_compressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_table_compression}.
	 * @param ctx the parse tree
	 */
	void exitDefault_table_compression(OracleParser.Default_table_compressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#low_high}.
	 * @param ctx the parse tree
	 */
	void enterLow_high(OracleParser.Low_highContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#low_high}.
	 * @param ctx the parse tree
	 */
	void exitLow_high(OracleParser.Low_highContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_index_compression}.
	 * @param ctx the parse tree
	 */
	void enterDefault_index_compression(OracleParser.Default_index_compressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_index_compression}.
	 * @param ctx the parse tree
	 */
	void exitDefault_index_compression(OracleParser.Default_index_compressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inmmemory_clause}.
	 * @param ctx the parse tree
	 */
	void enterInmmemory_clause(OracleParser.Inmmemory_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inmmemory_clause}.
	 * @param ctx the parse tree
	 */
	void exitInmmemory_clause(OracleParser.Inmmemory_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datafile_specification}.
	 * @param ctx the parse tree
	 */
	void enterDatafile_specification(OracleParser.Datafile_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datafile_specification}.
	 * @param ctx the parse tree
	 */
	void exitDatafile_specification(OracleParser.Datafile_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tempfile_specification}.
	 * @param ctx the parse tree
	 */
	void enterTempfile_specification(OracleParser.Tempfile_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tempfile_specification}.
	 * @param ctx the parse tree
	 */
	void exitTempfile_specification(OracleParser.Tempfile_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datafile_tempfile_spec}.
	 * @param ctx the parse tree
	 */
	void enterDatafile_tempfile_spec(OracleParser.Datafile_tempfile_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datafile_tempfile_spec}.
	 * @param ctx the parse tree
	 */
	void exitDatafile_tempfile_spec(OracleParser.Datafile_tempfile_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#redo_log_file_spec}.
	 * @param ctx the parse tree
	 */
	void enterRedo_log_file_spec(OracleParser.Redo_log_file_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#redo_log_file_spec}.
	 * @param ctx the parse tree
	 */
	void exitRedo_log_file_spec(OracleParser.Redo_log_file_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#autoextend_clause}.
	 * @param ctx the parse tree
	 */
	void enterAutoextend_clause(OracleParser.Autoextend_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#autoextend_clause}.
	 * @param ctx the parse tree
	 */
	void exitAutoextend_clause(OracleParser.Autoextend_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#maxsize_clause}.
	 * @param ctx the parse tree
	 */
	void enterMaxsize_clause(OracleParser.Maxsize_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#maxsize_clause}.
	 * @param ctx the parse tree
	 */
	void exitMaxsize_clause(OracleParser.Maxsize_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#build_clause}.
	 * @param ctx the parse tree
	 */
	void enterBuild_clause(OracleParser.Build_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#build_clause}.
	 * @param ctx the parse tree
	 */
	void exitBuild_clause(OracleParser.Build_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parallel_clause}.
	 * @param ctx the parse tree
	 */
	void enterParallel_clause(OracleParser.Parallel_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parallel_clause}.
	 * @param ctx the parse tree
	 */
	void exitParallel_clause(OracleParser.Parallel_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_materialized_view}.
	 * @param ctx the parse tree
	 */
	void enterAlter_materialized_view(OracleParser.Alter_materialized_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_materialized_view}.
	 * @param ctx the parse tree
	 */
	void exitAlter_materialized_view(OracleParser.Alter_materialized_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_mv_option1}.
	 * @param ctx the parse tree
	 */
	void enterAlter_mv_option1(OracleParser.Alter_mv_option1Context ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_mv_option1}.
	 * @param ctx the parse tree
	 */
	void exitAlter_mv_option1(OracleParser.Alter_mv_option1Context ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void enterAlter_mv_refresh(OracleParser.Alter_mv_refreshContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void exitAlter_mv_refresh(OracleParser.Alter_mv_refreshContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rollback_segment}.
	 * @param ctx the parse tree
	 */
	void enterRollback_segment(OracleParser.Rollback_segmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rollback_segment}.
	 * @param ctx the parse tree
	 */
	void exitRollback_segment(OracleParser.Rollback_segmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_mv_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterModify_mv_column_clause(OracleParser.Modify_mv_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_mv_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitModify_mv_column_clause(OracleParser.Modify_mv_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void enterAlter_materialized_view_log(OracleParser.Alter_materialized_view_logContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void exitAlter_materialized_view_log(OracleParser.Alter_materialized_view_logContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_mv_log_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_mv_log_column_clause(OracleParser.Add_mv_log_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_mv_log_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_mv_log_column_clause(OracleParser.Add_mv_log_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#move_mv_log_clause}.
	 * @param ctx the parse tree
	 */
	void enterMove_mv_log_clause(OracleParser.Move_mv_log_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#move_mv_log_clause}.
	 * @param ctx the parse tree
	 */
	void exitMove_mv_log_clause(OracleParser.Move_mv_log_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#mv_log_augmentation}.
	 * @param ctx the parse tree
	 */
	void enterMv_log_augmentation(OracleParser.Mv_log_augmentationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#mv_log_augmentation}.
	 * @param ctx the parse tree
	 */
	void exitMv_log_augmentation(OracleParser.Mv_log_augmentationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datetime_expr}.
	 * @param ctx the parse tree
	 */
	void enterDatetime_expr(OracleParser.Datetime_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datetime_expr}.
	 * @param ctx the parse tree
	 */
	void exitDatetime_expr(OracleParser.Datetime_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#interval_expr}.
	 * @param ctx the parse tree
	 */
	void enterInterval_expr(OracleParser.Interval_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#interval_expr}.
	 * @param ctx the parse tree
	 */
	void exitInterval_expr(OracleParser.Interval_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#synchronous_or_asynchronous}.
	 * @param ctx the parse tree
	 */
	void enterSynchronous_or_asynchronous(OracleParser.Synchronous_or_asynchronousContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#synchronous_or_asynchronous}.
	 * @param ctx the parse tree
	 */
	void exitSynchronous_or_asynchronous(OracleParser.Synchronous_or_asynchronousContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#including_or_excluding}.
	 * @param ctx the parse tree
	 */
	void enterIncluding_or_excluding(OracleParser.Including_or_excludingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#including_or_excluding}.
	 * @param ctx the parse tree
	 */
	void exitIncluding_or_excluding(OracleParser.Including_or_excludingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void enterCreate_materialized_view_log(OracleParser.Create_materialized_view_logContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void exitCreate_materialized_view_log(OracleParser.Create_materialized_view_logContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterNew_values_clause(OracleParser.New_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitNew_values_clause(OracleParser.New_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#mv_log_purge_clause}.
	 * @param ctx the parse tree
	 */
	void enterMv_log_purge_clause(OracleParser.Mv_log_purge_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#mv_log_purge_clause}.
	 * @param ctx the parse tree
	 */
	void exitMv_log_purge_clause(OracleParser.Mv_log_purge_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_materialized_zonemap}.
	 * @param ctx the parse tree
	 */
	void enterCreate_materialized_zonemap(OracleParser.Create_materialized_zonemapContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_materialized_zonemap}.
	 * @param ctx the parse tree
	 */
	void exitCreate_materialized_zonemap(OracleParser.Create_materialized_zonemapContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_materialized_zonemap}.
	 * @param ctx the parse tree
	 */
	void enterAlter_materialized_zonemap(OracleParser.Alter_materialized_zonemapContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_materialized_zonemap}.
	 * @param ctx the parse tree
	 */
	void exitAlter_materialized_zonemap(OracleParser.Alter_materialized_zonemapContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_materialized_zonemap}.
	 * @param ctx the parse tree
	 */
	void enterDrop_materialized_zonemap(OracleParser.Drop_materialized_zonemapContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_materialized_zonemap}.
	 * @param ctx the parse tree
	 */
	void exitDrop_materialized_zonemap(OracleParser.Drop_materialized_zonemapContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#zonemap_refresh_clause}.
	 * @param ctx the parse tree
	 */
	void enterZonemap_refresh_clause(OracleParser.Zonemap_refresh_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#zonemap_refresh_clause}.
	 * @param ctx the parse tree
	 */
	void exitZonemap_refresh_clause(OracleParser.Zonemap_refresh_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#zonemap_attributes}.
	 * @param ctx the parse tree
	 */
	void enterZonemap_attributes(OracleParser.Zonemap_attributesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#zonemap_attributes}.
	 * @param ctx the parse tree
	 */
	void exitZonemap_attributes(OracleParser.Zonemap_attributesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#zonemap_name}.
	 * @param ctx the parse tree
	 */
	void enterZonemap_name(OracleParser.Zonemap_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#zonemap_name}.
	 * @param ctx the parse tree
	 */
	void exitZonemap_name(OracleParser.Zonemap_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#operator_name}.
	 * @param ctx the parse tree
	 */
	void enterOperator_name(OracleParser.Operator_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#operator_name}.
	 * @param ctx the parse tree
	 */
	void exitOperator_name(OracleParser.Operator_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#operator_function_name}.
	 * @param ctx the parse tree
	 */
	void enterOperator_function_name(OracleParser.Operator_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#operator_function_name}.
	 * @param ctx the parse tree
	 */
	void exitOperator_function_name(OracleParser.Operator_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_zonemap_on_table}.
	 * @param ctx the parse tree
	 */
	void enterCreate_zonemap_on_table(OracleParser.Create_zonemap_on_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_zonemap_on_table}.
	 * @param ctx the parse tree
	 */
	void exitCreate_zonemap_on_table(OracleParser.Create_zonemap_on_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_zonemap_as_subquery}.
	 * @param ctx the parse tree
	 */
	void enterCreate_zonemap_as_subquery(OracleParser.Create_zonemap_as_subqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_zonemap_as_subquery}.
	 * @param ctx the parse tree
	 */
	void exitCreate_zonemap_as_subquery(OracleParser.Create_zonemap_as_subqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_operator}.
	 * @param ctx the parse tree
	 */
	void enterAlter_operator(OracleParser.Alter_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_operator}.
	 * @param ctx the parse tree
	 */
	void exitAlter_operator(OracleParser.Alter_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_operator}.
	 * @param ctx the parse tree
	 */
	void enterDrop_operator(OracleParser.Drop_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_operator}.
	 * @param ctx the parse tree
	 */
	void exitDrop_operator(OracleParser.Drop_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_operator}.
	 * @param ctx the parse tree
	 */
	void enterCreate_operator(OracleParser.Create_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_operator}.
	 * @param ctx the parse tree
	 */
	void exitCreate_operator(OracleParser.Create_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#binding_clause}.
	 * @param ctx the parse tree
	 */
	void enterBinding_clause(OracleParser.Binding_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#binding_clause}.
	 * @param ctx the parse tree
	 */
	void exitBinding_clause(OracleParser.Binding_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_binding_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_binding_clause(OracleParser.Add_binding_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_binding_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_binding_clause(OracleParser.Add_binding_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#implementation_clause}.
	 * @param ctx the parse tree
	 */
	void enterImplementation_clause(OracleParser.Implementation_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#implementation_clause}.
	 * @param ctx the parse tree
	 */
	void exitImplementation_clause(OracleParser.Implementation_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#primary_operator_list}.
	 * @param ctx the parse tree
	 */
	void enterPrimary_operator_list(OracleParser.Primary_operator_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#primary_operator_list}.
	 * @param ctx the parse tree
	 */
	void exitPrimary_operator_list(OracleParser.Primary_operator_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#primary_operator_item}.
	 * @param ctx the parse tree
	 */
	void enterPrimary_operator_item(OracleParser.Primary_operator_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#primary_operator_item}.
	 * @param ctx the parse tree
	 */
	void exitPrimary_operator_item(OracleParser.Primary_operator_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#operator_context_clause}.
	 * @param ctx the parse tree
	 */
	void enterOperator_context_clause(OracleParser.Operator_context_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#operator_context_clause}.
	 * @param ctx the parse tree
	 */
	void exitOperator_context_clause(OracleParser.Operator_context_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_function_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_function_clause(OracleParser.Using_function_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_function_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_function_clause(OracleParser.Using_function_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_binding_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_binding_clause(OracleParser.Drop_binding_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_binding_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_binding_clause(OracleParser.Drop_binding_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_materialized_view}.
	 * @param ctx the parse tree
	 */
	void enterCreate_materialized_view(OracleParser.Create_materialized_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_materialized_view}.
	 * @param ctx the parse tree
	 */
	void exitCreate_materialized_view(OracleParser.Create_materialized_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#scoped_table_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void enterScoped_table_ref_constraint(OracleParser.Scoped_table_ref_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#scoped_table_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void exitScoped_table_ref_constraint(OracleParser.Scoped_table_ref_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#mv_column_alias}.
	 * @param ctx the parse tree
	 */
	void enterMv_column_alias(OracleParser.Mv_column_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#mv_column_alias}.
	 * @param ctx the parse tree
	 */
	void exitMv_column_alias(OracleParser.Mv_column_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void enterCreate_mv_refresh(OracleParser.Create_mv_refreshContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void exitCreate_mv_refresh(OracleParser.Create_mv_refreshContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_materialized_view}.
	 * @param ctx the parse tree
	 */
	void enterDrop_materialized_view(OracleParser.Drop_materialized_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_materialized_view}.
	 * @param ctx the parse tree
	 */
	void exitDrop_materialized_view(OracleParser.Drop_materialized_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_context}.
	 * @param ctx the parse tree
	 */
	void enterCreate_context(OracleParser.Create_contextContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_context}.
	 * @param ctx the parse tree
	 */
	void exitCreate_context(OracleParser.Create_contextContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#oracle_namespace}.
	 * @param ctx the parse tree
	 */
	void enterOracle_namespace(OracleParser.Oracle_namespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#oracle_namespace}.
	 * @param ctx the parse tree
	 */
	void exitOracle_namespace(OracleParser.Oracle_namespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_cluster}.
	 * @param ctx the parse tree
	 */
	void enterCreate_cluster(OracleParser.Create_clusterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_cluster}.
	 * @param ctx the parse tree
	 */
	void exitCreate_cluster(OracleParser.Create_clusterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_profile}.
	 * @param ctx the parse tree
	 */
	void enterCreate_profile(OracleParser.Create_profileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_profile}.
	 * @param ctx the parse tree
	 */
	void exitCreate_profile(OracleParser.Create_profileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#resource_parameters}.
	 * @param ctx the parse tree
	 */
	void enterResource_parameters(OracleParser.Resource_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#resource_parameters}.
	 * @param ctx the parse tree
	 */
	void exitResource_parameters(OracleParser.Resource_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#password_parameters}.
	 * @param ctx the parse tree
	 */
	void enterPassword_parameters(OracleParser.Password_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#password_parameters}.
	 * @param ctx the parse tree
	 */
	void exitPassword_parameters(OracleParser.Password_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_lockdown_profile}.
	 * @param ctx the parse tree
	 */
	void enterCreate_lockdown_profile(OracleParser.Create_lockdown_profileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_lockdown_profile}.
	 * @param ctx the parse tree
	 */
	void exitCreate_lockdown_profile(OracleParser.Create_lockdown_profileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#static_base_profile}.
	 * @param ctx the parse tree
	 */
	void enterStatic_base_profile(OracleParser.Static_base_profileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#static_base_profile}.
	 * @param ctx the parse tree
	 */
	void exitStatic_base_profile(OracleParser.Static_base_profileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dynamic_base_profile}.
	 * @param ctx the parse tree
	 */
	void enterDynamic_base_profile(OracleParser.Dynamic_base_profileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dynamic_base_profile}.
	 * @param ctx the parse tree
	 */
	void exitDynamic_base_profile(OracleParser.Dynamic_base_profileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_outline}.
	 * @param ctx the parse tree
	 */
	void enterCreate_outline(OracleParser.Create_outlineContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_outline}.
	 * @param ctx the parse tree
	 */
	void exitCreate_outline(OracleParser.Create_outlineContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_restore_point}.
	 * @param ctx the parse tree
	 */
	void enterCreate_restore_point(OracleParser.Create_restore_pointContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_restore_point}.
	 * @param ctx the parse tree
	 */
	void exitCreate_restore_point(OracleParser.Create_restore_pointContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_role}.
	 * @param ctx the parse tree
	 */
	void enterCreate_role(OracleParser.Create_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_role}.
	 * @param ctx the parse tree
	 */
	void exitCreate_role(OracleParser.Create_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_table}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table(OracleParser.Create_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_table}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table(OracleParser.Create_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltype_table}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_table(OracleParser.Xmltype_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltype_table}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_table(OracleParser.Xmltype_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltype_virtual_columns}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_virtual_columns(OracleParser.Xmltype_virtual_columnsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltype_virtual_columns}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_virtual_columns(OracleParser.Xmltype_virtual_columnsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltype_column_properties}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_column_properties(OracleParser.Xmltype_column_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltype_column_properties}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_column_properties(OracleParser.Xmltype_column_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltype_storage}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_storage(OracleParser.Xmltype_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltype_storage}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_storage(OracleParser.Xmltype_storageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlschema_spec}.
	 * @param ctx the parse tree
	 */
	void enterXmlschema_spec(OracleParser.Xmlschema_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlschema_spec}.
	 * @param ctx the parse tree
	 */
	void exitXmlschema_spec(OracleParser.Xmlschema_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_table}.
	 * @param ctx the parse tree
	 */
	void enterObject_table(OracleParser.Object_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_table}.
	 * @param ctx the parse tree
	 */
	void exitObject_table(OracleParser.Object_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_type}.
	 * @param ctx the parse tree
	 */
	void enterObject_type(OracleParser.Object_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_type}.
	 * @param ctx the parse tree
	 */
	void exitObject_type(OracleParser.Object_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#oid_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterOid_index_clause(OracleParser.Oid_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#oid_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitOid_index_clause(OracleParser.Oid_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#oid_clause}.
	 * @param ctx the parse tree
	 */
	void enterOid_clause(OracleParser.Oid_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#oid_clause}.
	 * @param ctx the parse tree
	 */
	void exitOid_clause(OracleParser.Oid_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_properties}.
	 * @param ctx the parse tree
	 */
	void enterObject_properties(OracleParser.Object_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_properties}.
	 * @param ctx the parse tree
	 */
	void exitObject_properties(OracleParser.Object_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_table_substitution}.
	 * @param ctx the parse tree
	 */
	void enterObject_table_substitution(OracleParser.Object_table_substitutionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_table_substitution}.
	 * @param ctx the parse tree
	 */
	void exitObject_table_substitution(OracleParser.Object_table_substitutionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#relational_table}.
	 * @param ctx the parse tree
	 */
	void enterRelational_table(OracleParser.Relational_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#relational_table}.
	 * @param ctx the parse tree
	 */
	void exitRelational_table(OracleParser.Relational_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#immutable_table_clauses}.
	 * @param ctx the parse tree
	 */
	void enterImmutable_table_clauses(OracleParser.Immutable_table_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#immutable_table_clauses}.
	 * @param ctx the parse tree
	 */
	void exitImmutable_table_clauses(OracleParser.Immutable_table_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#immutable_table_no_drop_clause}.
	 * @param ctx the parse tree
	 */
	void enterImmutable_table_no_drop_clause(OracleParser.Immutable_table_no_drop_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#immutable_table_no_drop_clause}.
	 * @param ctx the parse tree
	 */
	void exitImmutable_table_no_drop_clause(OracleParser.Immutable_table_no_drop_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#immutable_table_no_delete_clause}.
	 * @param ctx the parse tree
	 */
	void enterImmutable_table_no_delete_clause(OracleParser.Immutable_table_no_delete_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#immutable_table_no_delete_clause}.
	 * @param ctx the parse tree
	 */
	void exitImmutable_table_no_delete_clause(OracleParser.Immutable_table_no_delete_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#blockchain_table_clauses}.
	 * @param ctx the parse tree
	 */
	void enterBlockchain_table_clauses(OracleParser.Blockchain_table_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#blockchain_table_clauses}.
	 * @param ctx the parse tree
	 */
	void exitBlockchain_table_clauses(OracleParser.Blockchain_table_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#blockchain_drop_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterBlockchain_drop_table_clause(OracleParser.Blockchain_drop_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#blockchain_drop_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitBlockchain_drop_table_clause(OracleParser.Blockchain_drop_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#blockchain_row_retention_clause}.
	 * @param ctx the parse tree
	 */
	void enterBlockchain_row_retention_clause(OracleParser.Blockchain_row_retention_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#blockchain_row_retention_clause}.
	 * @param ctx the parse tree
	 */
	void exitBlockchain_row_retention_clause(OracleParser.Blockchain_row_retention_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#blockchain_hash_and_data_format_clause}.
	 * @param ctx the parse tree
	 */
	void enterBlockchain_hash_and_data_format_clause(OracleParser.Blockchain_hash_and_data_format_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#blockchain_hash_and_data_format_clause}.
	 * @param ctx the parse tree
	 */
	void exitBlockchain_hash_and_data_format_clause(OracleParser.Blockchain_hash_and_data_format_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#collation_name}.
	 * @param ctx the parse tree
	 */
	void enterCollation_name(OracleParser.Collation_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#collation_name}.
	 * @param ctx the parse tree
	 */
	void exitCollation_name(OracleParser.Collation_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_properties}.
	 * @param ctx the parse tree
	 */
	void enterTable_properties(OracleParser.Table_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_properties}.
	 * @param ctx the parse tree
	 */
	void exitTable_properties(OracleParser.Table_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#read_only_clause}.
	 * @param ctx the parse tree
	 */
	void enterRead_only_clause(OracleParser.Read_only_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#read_only_clause}.
	 * @param ctx the parse tree
	 */
	void exitRead_only_clause(OracleParser.Read_only_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#indexing_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndexing_clause(OracleParser.Indexing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#indexing_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndexing_clause(OracleParser.Indexing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#attribute_clustering_clause}.
	 * @param ctx the parse tree
	 */
	void enterAttribute_clustering_clause(OracleParser.Attribute_clustering_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#attribute_clustering_clause}.
	 * @param ctx the parse tree
	 */
	void exitAttribute_clustering_clause(OracleParser.Attribute_clustering_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#clustering_join}.
	 * @param ctx the parse tree
	 */
	void enterClustering_join(OracleParser.Clustering_joinContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#clustering_join}.
	 * @param ctx the parse tree
	 */
	void exitClustering_join(OracleParser.Clustering_joinContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#clustering_join_item}.
	 * @param ctx the parse tree
	 */
	void enterClustering_join_item(OracleParser.Clustering_join_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#clustering_join_item}.
	 * @param ctx the parse tree
	 */
	void exitClustering_join_item(OracleParser.Clustering_join_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#equijoin_condition}.
	 * @param ctx the parse tree
	 */
	void enterEquijoin_condition(OracleParser.Equijoin_conditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#equijoin_condition}.
	 * @param ctx the parse tree
	 */
	void exitEquijoin_condition(OracleParser.Equijoin_conditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cluster_clause}.
	 * @param ctx the parse tree
	 */
	void enterCluster_clause(OracleParser.Cluster_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cluster_clause}.
	 * @param ctx the parse tree
	 */
	void exitCluster_clause(OracleParser.Cluster_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#clustering_columns}.
	 * @param ctx the parse tree
	 */
	void enterClustering_columns(OracleParser.Clustering_columnsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#clustering_columns}.
	 * @param ctx the parse tree
	 */
	void exitClustering_columns(OracleParser.Clustering_columnsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#clustering_column_group}.
	 * @param ctx the parse tree
	 */
	void enterClustering_column_group(OracleParser.Clustering_column_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#clustering_column_group}.
	 * @param ctx the parse tree
	 */
	void exitClustering_column_group(OracleParser.Clustering_column_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#yes_no}.
	 * @param ctx the parse tree
	 */
	void enterYes_no(OracleParser.Yes_noContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#yes_no}.
	 * @param ctx the parse tree
	 */
	void exitYes_no(OracleParser.Yes_noContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#zonemap_clause}.
	 * @param ctx the parse tree
	 */
	void enterZonemap_clause(OracleParser.Zonemap_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#zonemap_clause}.
	 * @param ctx the parse tree
	 */
	void exitZonemap_clause(OracleParser.Zonemap_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logical_replication_clause}.
	 * @param ctx the parse tree
	 */
	void enterLogical_replication_clause(OracleParser.Logical_replication_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logical_replication_clause}.
	 * @param ctx the parse tree
	 */
	void exitLogical_replication_clause(OracleParser.Logical_replication_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_name}.
	 * @param ctx the parse tree
	 */
	void enterTable_name(OracleParser.Table_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_name}.
	 * @param ctx the parse tree
	 */
	void exitTable_name(OracleParser.Table_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#relational_property}.
	 * @param ctx the parse tree
	 */
	void enterRelational_property(OracleParser.Relational_propertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#relational_property}.
	 * @param ctx the parse tree
	 */
	void exitRelational_property(OracleParser.Relational_propertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_partitioning_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTable_partitioning_clauses(OracleParser.Table_partitioning_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_partitioning_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTable_partitioning_clauses(OracleParser.Table_partitioning_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#range_partitions}.
	 * @param ctx the parse tree
	 */
	void enterRange_partitions(OracleParser.Range_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#range_partitions}.
	 * @param ctx the parse tree
	 */
	void exitRange_partitions(OracleParser.Range_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#list_partitions}.
	 * @param ctx the parse tree
	 */
	void enterList_partitions(OracleParser.List_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#list_partitions}.
	 * @param ctx the parse tree
	 */
	void exitList_partitions(OracleParser.List_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hash_partitions}.
	 * @param ctx the parse tree
	 */
	void enterHash_partitions(OracleParser.Hash_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hash_partitions}.
	 * @param ctx the parse tree
	 */
	void exitHash_partitions(OracleParser.Hash_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#individual_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void enterIndividual_hash_partitions(OracleParser.Individual_hash_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#individual_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void exitIndividual_hash_partitions(OracleParser.Individual_hash_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hash_partitions_by_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_partitions_by_quantity(OracleParser.Hash_partitions_by_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hash_partitions_by_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_partitions_by_quantity(OracleParser.Hash_partitions_by_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hash_partition_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_partition_quantity(OracleParser.Hash_partition_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hash_partition_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_partition_quantity(OracleParser.Hash_partition_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#composite_range_partitions}.
	 * @param ctx the parse tree
	 */
	void enterComposite_range_partitions(OracleParser.Composite_range_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#composite_range_partitions}.
	 * @param ctx the parse tree
	 */
	void exitComposite_range_partitions(OracleParser.Composite_range_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#composite_list_partitions}.
	 * @param ctx the parse tree
	 */
	void enterComposite_list_partitions(OracleParser.Composite_list_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#composite_list_partitions}.
	 * @param ctx the parse tree
	 */
	void exitComposite_list_partitions(OracleParser.Composite_list_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#composite_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void enterComposite_hash_partitions(OracleParser.Composite_hash_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#composite_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void exitComposite_hash_partitions(OracleParser.Composite_hash_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#reference_partitioning}.
	 * @param ctx the parse tree
	 */
	void enterReference_partitioning(OracleParser.Reference_partitioningContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#reference_partitioning}.
	 * @param ctx the parse tree
	 */
	void exitReference_partitioning(OracleParser.Reference_partitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#reference_partition_desc}.
	 * @param ctx the parse tree
	 */
	void enterReference_partition_desc(OracleParser.Reference_partition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#reference_partition_desc}.
	 * @param ctx the parse tree
	 */
	void exitReference_partition_desc(OracleParser.Reference_partition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#system_partitioning}.
	 * @param ctx the parse tree
	 */
	void enterSystem_partitioning(OracleParser.System_partitioningContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#system_partitioning}.
	 * @param ctx the parse tree
	 */
	void exitSystem_partitioning(OracleParser.System_partitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#range_partition_desc}.
	 * @param ctx the parse tree
	 */
	void enterRange_partition_desc(OracleParser.Range_partition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#range_partition_desc}.
	 * @param ctx the parse tree
	 */
	void exitRange_partition_desc(OracleParser.Range_partition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#list_partition_desc}.
	 * @param ctx the parse tree
	 */
	void enterList_partition_desc(OracleParser.List_partition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#list_partition_desc}.
	 * @param ctx the parse tree
	 */
	void exitList_partition_desc(OracleParser.List_partition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_template}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_template(OracleParser.Subpartition_templateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_template}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_template(OracleParser.Subpartition_templateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hash_subpartition_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_subpartition_quantity(OracleParser.Hash_subpartition_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hash_subpartition_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_subpartition_quantity(OracleParser.Hash_subpartition_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_by_range}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_by_range(OracleParser.Subpartition_by_rangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_by_range}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_by_range(OracleParser.Subpartition_by_rangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_by_list}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_by_list(OracleParser.Subpartition_by_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_by_list}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_by_list(OracleParser.Subpartition_by_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_by_hash}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_by_hash(OracleParser.Subpartition_by_hashContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_by_hash}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_by_hash(OracleParser.Subpartition_by_hashContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_name}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_name(OracleParser.Subpartition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_name}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_name(OracleParser.Subpartition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#range_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void enterRange_subpartition_desc(OracleParser.Range_subpartition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#range_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void exitRange_subpartition_desc(OracleParser.Range_subpartition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#list_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void enterList_subpartition_desc(OracleParser.List_subpartition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#list_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void exitList_subpartition_desc(OracleParser.List_subpartition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#individual_hash_subparts}.
	 * @param ctx the parse tree
	 */
	void enterIndividual_hash_subparts(OracleParser.Individual_hash_subpartsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#individual_hash_subparts}.
	 * @param ctx the parse tree
	 */
	void exitIndividual_hash_subparts(OracleParser.Individual_hash_subpartsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hash_subparts_by_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_subparts_by_quantity(OracleParser.Hash_subparts_by_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hash_subparts_by_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_subparts_by_quantity(OracleParser.Hash_subparts_by_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#range_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterRange_values_clause(OracleParser.Range_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#range_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitRange_values_clause(OracleParser.Range_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#list_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterList_values_clause(OracleParser.List_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#list_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitList_values_clause(OracleParser.List_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_partition_description}.
	 * @param ctx the parse tree
	 */
	void enterTable_partition_description(OracleParser.Table_partition_descriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_partition_description}.
	 * @param ctx the parse tree
	 */
	void exitTable_partition_description(OracleParser.Table_partition_descriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partitioning_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartitioning_storage_clause(OracleParser.Partitioning_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partitioning_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartitioning_storage_clause(OracleParser.Partitioning_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_partitioning_storage}.
	 * @param ctx the parse tree
	 */
	void enterLob_partitioning_storage(OracleParser.Lob_partitioning_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_partitioning_storage}.
	 * @param ctx the parse tree
	 */
	void exitLob_partitioning_storage(OracleParser.Lob_partitioning_storageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datatype_null_enable}.
	 * @param ctx the parse tree
	 */
	void enterDatatype_null_enable(OracleParser.Datatype_null_enableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datatype_null_enable}.
	 * @param ctx the parse tree
	 */
	void exitDatatype_null_enable(OracleParser.Datatype_null_enableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#size_clause}.
	 * @param ctx the parse tree
	 */
	void enterSize_clause(OracleParser.Size_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#size_clause}.
	 * @param ctx the parse tree
	 */
	void exitSize_clause(OracleParser.Size_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_compression}.
	 * @param ctx the parse tree
	 */
	void enterTable_compression(OracleParser.Table_compressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_compression}.
	 * @param ctx the parse tree
	 */
	void exitTable_compression(OracleParser.Table_compressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inmemory_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterInmemory_table_clause(OracleParser.Inmemory_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inmemory_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitInmemory_table_clause(OracleParser.Inmemory_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inmemory_attributes}.
	 * @param ctx the parse tree
	 */
	void enterInmemory_attributes(OracleParser.Inmemory_attributesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inmemory_attributes}.
	 * @param ctx the parse tree
	 */
	void exitInmemory_attributes(OracleParser.Inmemory_attributesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inmemory_memcompress}.
	 * @param ctx the parse tree
	 */
	void enterInmemory_memcompress(OracleParser.Inmemory_memcompressContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inmemory_memcompress}.
	 * @param ctx the parse tree
	 */
	void exitInmemory_memcompress(OracleParser.Inmemory_memcompressContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inmemory_priority}.
	 * @param ctx the parse tree
	 */
	void enterInmemory_priority(OracleParser.Inmemory_priorityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inmemory_priority}.
	 * @param ctx the parse tree
	 */
	void exitInmemory_priority(OracleParser.Inmemory_priorityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inmemory_distribute}.
	 * @param ctx the parse tree
	 */
	void enterInmemory_distribute(OracleParser.Inmemory_distributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inmemory_distribute}.
	 * @param ctx the parse tree
	 */
	void exitInmemory_distribute(OracleParser.Inmemory_distributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inmemory_duplicate}.
	 * @param ctx the parse tree
	 */
	void enterInmemory_duplicate(OracleParser.Inmemory_duplicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inmemory_duplicate}.
	 * @param ctx the parse tree
	 */
	void exitInmemory_duplicate(OracleParser.Inmemory_duplicateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inmemory_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterInmemory_column_clause(OracleParser.Inmemory_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inmemory_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitInmemory_column_clause(OracleParser.Inmemory_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#physical_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterPhysical_attributes_clause(OracleParser.Physical_attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#physical_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitPhysical_attributes_clause(OracleParser.Physical_attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterStorage_clause(OracleParser.Storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitStorage_clause(OracleParser.Storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#deferred_segment_creation}.
	 * @param ctx the parse tree
	 */
	void enterDeferred_segment_creation(OracleParser.Deferred_segment_creationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#deferred_segment_creation}.
	 * @param ctx the parse tree
	 */
	void exitDeferred_segment_creation(OracleParser.Deferred_segment_creationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#segment_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterSegment_attributes_clause(OracleParser.Segment_attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#segment_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitSegment_attributes_clause(OracleParser.Segment_attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#physical_properties}.
	 * @param ctx the parse tree
	 */
	void enterPhysical_properties(OracleParser.Physical_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#physical_properties}.
	 * @param ctx the parse tree
	 */
	void exitPhysical_properties(OracleParser.Physical_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ilm_clause}.
	 * @param ctx the parse tree
	 */
	void enterIlm_clause(OracleParser.Ilm_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ilm_clause}.
	 * @param ctx the parse tree
	 */
	void exitIlm_clause(OracleParser.Ilm_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ilm_policy_clause}.
	 * @param ctx the parse tree
	 */
	void enterIlm_policy_clause(OracleParser.Ilm_policy_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ilm_policy_clause}.
	 * @param ctx the parse tree
	 */
	void exitIlm_policy_clause(OracleParser.Ilm_policy_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ilm_compression_policy}.
	 * @param ctx the parse tree
	 */
	void enterIlm_compression_policy(OracleParser.Ilm_compression_policyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ilm_compression_policy}.
	 * @param ctx the parse tree
	 */
	void exitIlm_compression_policy(OracleParser.Ilm_compression_policyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ilm_tiering_policy}.
	 * @param ctx the parse tree
	 */
	void enterIlm_tiering_policy(OracleParser.Ilm_tiering_policyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ilm_tiering_policy}.
	 * @param ctx the parse tree
	 */
	void exitIlm_tiering_policy(OracleParser.Ilm_tiering_policyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ilm_after_on}.
	 * @param ctx the parse tree
	 */
	void enterIlm_after_on(OracleParser.Ilm_after_onContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ilm_after_on}.
	 * @param ctx the parse tree
	 */
	void exitIlm_after_on(OracleParser.Ilm_after_onContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#segment_group}.
	 * @param ctx the parse tree
	 */
	void enterSegment_group(OracleParser.Segment_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#segment_group}.
	 * @param ctx the parse tree
	 */
	void exitSegment_group(OracleParser.Segment_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ilm_inmemory_policy}.
	 * @param ctx the parse tree
	 */
	void enterIlm_inmemory_policy(OracleParser.Ilm_inmemory_policyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ilm_inmemory_policy}.
	 * @param ctx the parse tree
	 */
	void exitIlm_inmemory_policy(OracleParser.Ilm_inmemory_policyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ilm_time_period}.
	 * @param ctx the parse tree
	 */
	void enterIlm_time_period(OracleParser.Ilm_time_periodContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ilm_time_period}.
	 * @param ctx the parse tree
	 */
	void exitIlm_time_period(OracleParser.Ilm_time_periodContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#heap_org_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterHeap_org_table_clause(OracleParser.Heap_org_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#heap_org_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitHeap_org_table_clause(OracleParser.Heap_org_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#external_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterExternal_table_clause(OracleParser.External_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#external_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitExternal_table_clause(OracleParser.External_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#access_driver_type}.
	 * @param ctx the parse tree
	 */
	void enterAccess_driver_type(OracleParser.Access_driver_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#access_driver_type}.
	 * @param ctx the parse tree
	 */
	void exitAccess_driver_type(OracleParser.Access_driver_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#external_table_data_props}.
	 * @param ctx the parse tree
	 */
	void enterExternal_table_data_props(OracleParser.External_table_data_propsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#external_table_data_props}.
	 * @param ctx the parse tree
	 */
	void exitExternal_table_data_props(OracleParser.External_table_data_propsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#opaque_format_spec}.
	 * @param ctx the parse tree
	 */
	void enterOpaque_format_spec(OracleParser.Opaque_format_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#opaque_format_spec}.
	 * @param ctx the parse tree
	 */
	void exitOpaque_format_spec(OracleParser.Opaque_format_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#row_movement_clause}.
	 * @param ctx the parse tree
	 */
	void enterRow_movement_clause(OracleParser.Row_movement_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#row_movement_clause}.
	 * @param ctx the parse tree
	 */
	void exitRow_movement_clause(OracleParser.Row_movement_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#flashback_archive_clause}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_archive_clause(OracleParser.Flashback_archive_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#flashback_archive_clause}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_archive_clause(OracleParser.Flashback_archive_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#log_grp}.
	 * @param ctx the parse tree
	 */
	void enterLog_grp(OracleParser.Log_grpContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#log_grp}.
	 * @param ctx the parse tree
	 */
	void exitLog_grp(OracleParser.Log_grpContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_table_logging}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_table_logging(OracleParser.Supplemental_table_loggingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_table_logging}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_table_logging(OracleParser.Supplemental_table_loggingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_log_grp_clause}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_log_grp_clause(OracleParser.Supplemental_log_grp_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_log_grp_clause}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_log_grp_clause(OracleParser.Supplemental_log_grp_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_id_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_id_key_clause(OracleParser.Supplemental_id_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_id_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_id_key_clause(OracleParser.Supplemental_id_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#allocate_extent_clause}.
	 * @param ctx the parse tree
	 */
	void enterAllocate_extent_clause(OracleParser.Allocate_extent_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#allocate_extent_clause}.
	 * @param ctx the parse tree
	 */
	void exitAllocate_extent_clause(OracleParser.Allocate_extent_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#deallocate_unused_clause}.
	 * @param ctx the parse tree
	 */
	void enterDeallocate_unused_clause(OracleParser.Deallocate_unused_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#deallocate_unused_clause}.
	 * @param ctx the parse tree
	 */
	void exitDeallocate_unused_clause(OracleParser.Deallocate_unused_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#shrink_clause}.
	 * @param ctx the parse tree
	 */
	void enterShrink_clause(OracleParser.Shrink_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#shrink_clause}.
	 * @param ctx the parse tree
	 */
	void exitShrink_clause(OracleParser.Shrink_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#records_per_block_clause}.
	 * @param ctx the parse tree
	 */
	void enterRecords_per_block_clause(OracleParser.Records_per_block_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#records_per_block_clause}.
	 * @param ctx the parse tree
	 */
	void exitRecords_per_block_clause(OracleParser.Records_per_block_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#upgrade_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterUpgrade_table_clause(OracleParser.Upgrade_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#upgrade_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitUpgrade_table_clause(OracleParser.Upgrade_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#truncate_table}.
	 * @param ctx the parse tree
	 */
	void enterTruncate_table(OracleParser.Truncate_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#truncate_table}.
	 * @param ctx the parse tree
	 */
	void exitTruncate_table(OracleParser.Truncate_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_table}.
	 * @param ctx the parse tree
	 */
	void enterDrop_table(OracleParser.Drop_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_table}.
	 * @param ctx the parse tree
	 */
	void exitDrop_table(OracleParser.Drop_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterDrop_tablespace(OracleParser.Drop_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitDrop_tablespace(OracleParser.Drop_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_tablespace_set}.
	 * @param ctx the parse tree
	 */
	void enterDrop_tablespace_set(OracleParser.Drop_tablespace_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_tablespace_set}.
	 * @param ctx the parse tree
	 */
	void exitDrop_tablespace_set(OracleParser.Drop_tablespace_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#including_contents_clause}.
	 * @param ctx the parse tree
	 */
	void enterIncluding_contents_clause(OracleParser.Including_contents_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#including_contents_clause}.
	 * @param ctx the parse tree
	 */
	void exitIncluding_contents_clause(OracleParser.Including_contents_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_view}.
	 * @param ctx the parse tree
	 */
	void enterDrop_view(OracleParser.Drop_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_view}.
	 * @param ctx the parse tree
	 */
	void exitDrop_view(OracleParser.Drop_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#comment_on_column}.
	 * @param ctx the parse tree
	 */
	void enterComment_on_column(OracleParser.Comment_on_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#comment_on_column}.
	 * @param ctx the parse tree
	 */
	void exitComment_on_column(OracleParser.Comment_on_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#enable_or_disable}.
	 * @param ctx the parse tree
	 */
	void enterEnable_or_disable(OracleParser.Enable_or_disableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#enable_or_disable}.
	 * @param ctx the parse tree
	 */
	void exitEnable_or_disable(OracleParser.Enable_or_disableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#allow_or_disallow}.
	 * @param ctx the parse tree
	 */
	void enterAllow_or_disallow(OracleParser.Allow_or_disallowContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#allow_or_disallow}.
	 * @param ctx the parse tree
	 */
	void exitAllow_or_disallow(OracleParser.Allow_or_disallowContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_synonym}.
	 * @param ctx the parse tree
	 */
	void enterAlter_synonym(OracleParser.Alter_synonymContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_synonym}.
	 * @param ctx the parse tree
	 */
	void exitAlter_synonym(OracleParser.Alter_synonymContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_synonym}.
	 * @param ctx the parse tree
	 */
	void enterCreate_synonym(OracleParser.Create_synonymContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_synonym}.
	 * @param ctx the parse tree
	 */
	void exitCreate_synonym(OracleParser.Create_synonymContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_synonym}.
	 * @param ctx the parse tree
	 */
	void enterDrop_synonym(OracleParser.Drop_synonymContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_synonym}.
	 * @param ctx the parse tree
	 */
	void exitDrop_synonym(OracleParser.Drop_synonymContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_spfile}.
	 * @param ctx the parse tree
	 */
	void enterCreate_spfile(OracleParser.Create_spfileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_spfile}.
	 * @param ctx the parse tree
	 */
	void exitCreate_spfile(OracleParser.Create_spfileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#spfile_name}.
	 * @param ctx the parse tree
	 */
	void enterSpfile_name(OracleParser.Spfile_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#spfile_name}.
	 * @param ctx the parse tree
	 */
	void exitSpfile_name(OracleParser.Spfile_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pfile_name}.
	 * @param ctx the parse tree
	 */
	void enterPfile_name(OracleParser.Pfile_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pfile_name}.
	 * @param ctx the parse tree
	 */
	void exitPfile_name(OracleParser.Pfile_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#comment_on_table}.
	 * @param ctx the parse tree
	 */
	void enterComment_on_table(OracleParser.Comment_on_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#comment_on_table}.
	 * @param ctx the parse tree
	 */
	void exitComment_on_table(OracleParser.Comment_on_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#comment_on_materialized}.
	 * @param ctx the parse tree
	 */
	void enterComment_on_materialized(OracleParser.Comment_on_materializedContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#comment_on_materialized}.
	 * @param ctx the parse tree
	 */
	void exitComment_on_materialized(OracleParser.Comment_on_materializedContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_analytic_view}.
	 * @param ctx the parse tree
	 */
	void enterAlter_analytic_view(OracleParser.Alter_analytic_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_analytic_view}.
	 * @param ctx the parse tree
	 */
	void exitAlter_analytic_view(OracleParser.Alter_analytic_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_add_cache_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_add_cache_clause(OracleParser.Alter_add_cache_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_add_cache_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_add_cache_clause(OracleParser.Alter_add_cache_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#levels_item}.
	 * @param ctx the parse tree
	 */
	void enterLevels_item(OracleParser.Levels_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#levels_item}.
	 * @param ctx the parse tree
	 */
	void exitLevels_item(OracleParser.Levels_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#measure_list}.
	 * @param ctx the parse tree
	 */
	void enterMeasure_list(OracleParser.Measure_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#measure_list}.
	 * @param ctx the parse tree
	 */
	void exitMeasure_list(OracleParser.Measure_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_drop_cache_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_drop_cache_clause(OracleParser.Alter_drop_cache_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_drop_cache_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_drop_cache_clause(OracleParser.Alter_drop_cache_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_attribute_dimension}.
	 * @param ctx the parse tree
	 */
	void enterAlter_attribute_dimension(OracleParser.Alter_attribute_dimensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_attribute_dimension}.
	 * @param ctx the parse tree
	 */
	void exitAlter_attribute_dimension(OracleParser.Alter_attribute_dimensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_audit_policy}.
	 * @param ctx the parse tree
	 */
	void enterAlter_audit_policy(OracleParser.Alter_audit_policyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_audit_policy}.
	 * @param ctx the parse tree
	 */
	void exitAlter_audit_policy(OracleParser.Alter_audit_policyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_cluster}.
	 * @param ctx the parse tree
	 */
	void enterAlter_cluster(OracleParser.Alter_clusterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_cluster}.
	 * @param ctx the parse tree
	 */
	void exitAlter_cluster(OracleParser.Alter_clusterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_analytic_view}.
	 * @param ctx the parse tree
	 */
	void enterDrop_analytic_view(OracleParser.Drop_analytic_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_analytic_view}.
	 * @param ctx the parse tree
	 */
	void exitDrop_analytic_view(OracleParser.Drop_analytic_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_attribute_dimension}.
	 * @param ctx the parse tree
	 */
	void enterDrop_attribute_dimension(OracleParser.Drop_attribute_dimensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_attribute_dimension}.
	 * @param ctx the parse tree
	 */
	void exitDrop_attribute_dimension(OracleParser.Drop_attribute_dimensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_audit_policy}.
	 * @param ctx the parse tree
	 */
	void enterDrop_audit_policy(OracleParser.Drop_audit_policyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_audit_policy}.
	 * @param ctx the parse tree
	 */
	void exitDrop_audit_policy(OracleParser.Drop_audit_policyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_flashback_archive}.
	 * @param ctx the parse tree
	 */
	void enterDrop_flashback_archive(OracleParser.Drop_flashback_archiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_flashback_archive}.
	 * @param ctx the parse tree
	 */
	void exitDrop_flashback_archive(OracleParser.Drop_flashback_archiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_cluster}.
	 * @param ctx the parse tree
	 */
	void enterDrop_cluster(OracleParser.Drop_clusterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_cluster}.
	 * @param ctx the parse tree
	 */
	void exitDrop_cluster(OracleParser.Drop_clusterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_context}.
	 * @param ctx the parse tree
	 */
	void enterDrop_context(OracleParser.Drop_contextContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_context}.
	 * @param ctx the parse tree
	 */
	void exitDrop_context(OracleParser.Drop_contextContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_directory}.
	 * @param ctx the parse tree
	 */
	void enterDrop_directory(OracleParser.Drop_directoryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_directory}.
	 * @param ctx the parse tree
	 */
	void exitDrop_directory(OracleParser.Drop_directoryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_diskgroup}.
	 * @param ctx the parse tree
	 */
	void enterDrop_diskgroup(OracleParser.Drop_diskgroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_diskgroup}.
	 * @param ctx the parse tree
	 */
	void exitDrop_diskgroup(OracleParser.Drop_diskgroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_edition}.
	 * @param ctx the parse tree
	 */
	void enterDrop_edition(OracleParser.Drop_editionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_edition}.
	 * @param ctx the parse tree
	 */
	void exitDrop_edition(OracleParser.Drop_editionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#truncate_cluster}.
	 * @param ctx the parse tree
	 */
	void enterTruncate_cluster(OracleParser.Truncate_clusterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#truncate_cluster}.
	 * @param ctx the parse tree
	 */
	void exitTruncate_cluster(OracleParser.Truncate_clusterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cache_or_nocache}.
	 * @param ctx the parse tree
	 */
	void enterCache_or_nocache(OracleParser.Cache_or_nocacheContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cache_or_nocache}.
	 * @param ctx the parse tree
	 */
	void exitCache_or_nocache(OracleParser.Cache_or_nocacheContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#database_name}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_name(OracleParser.Database_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#database_name}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_name(OracleParser.Database_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_database}.
	 * @param ctx the parse tree
	 */
	void enterAlter_database(OracleParser.Alter_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_database}.
	 * @param ctx the parse tree
	 */
	void exitAlter_database(OracleParser.Alter_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#database_clause}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_clause(OracleParser.Database_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#database_clause}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_clause(OracleParser.Database_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#startup_clauses}.
	 * @param ctx the parse tree
	 */
	void enterStartup_clauses(OracleParser.Startup_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#startup_clauses}.
	 * @param ctx the parse tree
	 */
	void exitStartup_clauses(OracleParser.Startup_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#resetlogs_or_noresetlogs}.
	 * @param ctx the parse tree
	 */
	void enterResetlogs_or_noresetlogs(OracleParser.Resetlogs_or_noresetlogsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#resetlogs_or_noresetlogs}.
	 * @param ctx the parse tree
	 */
	void exitResetlogs_or_noresetlogs(OracleParser.Resetlogs_or_noresetlogsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#upgrade_or_downgrade}.
	 * @param ctx the parse tree
	 */
	void enterUpgrade_or_downgrade(OracleParser.Upgrade_or_downgradeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#upgrade_or_downgrade}.
	 * @param ctx the parse tree
	 */
	void exitUpgrade_or_downgrade(OracleParser.Upgrade_or_downgradeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#recovery_clauses}.
	 * @param ctx the parse tree
	 */
	void enterRecovery_clauses(OracleParser.Recovery_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#recovery_clauses}.
	 * @param ctx the parse tree
	 */
	void exitRecovery_clauses(OracleParser.Recovery_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#begin_or_end}.
	 * @param ctx the parse tree
	 */
	void enterBegin_or_end(OracleParser.Begin_or_endContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#begin_or_end}.
	 * @param ctx the parse tree
	 */
	void exitBegin_or_end(OracleParser.Begin_or_endContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#general_recovery}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_recovery(OracleParser.General_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#general_recovery}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_recovery(OracleParser.General_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#full_database_recovery}.
	 * @param ctx the parse tree
	 */
	void enterFull_database_recovery(OracleParser.Full_database_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#full_database_recovery}.
	 * @param ctx the parse tree
	 */
	void exitFull_database_recovery(OracleParser.Full_database_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partial_database_recovery}.
	 * @param ctx the parse tree
	 */
	void enterPartial_database_recovery(OracleParser.Partial_database_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partial_database_recovery}.
	 * @param ctx the parse tree
	 */
	void exitPartial_database_recovery(OracleParser.Partial_database_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partial_database_recovery_10g}.
	 * @param ctx the parse tree
	 */
	void enterPartial_database_recovery_10g(OracleParser.Partial_database_recovery_10gContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partial_database_recovery_10g}.
	 * @param ctx the parse tree
	 */
	void exitPartial_database_recovery_10g(OracleParser.Partial_database_recovery_10gContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#managed_standby_recovery}.
	 * @param ctx the parse tree
	 */
	void enterManaged_standby_recovery(OracleParser.Managed_standby_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#managed_standby_recovery}.
	 * @param ctx the parse tree
	 */
	void exitManaged_standby_recovery(OracleParser.Managed_standby_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#db_name}.
	 * @param ctx the parse tree
	 */
	void enterDb_name(OracleParser.Db_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#db_name}.
	 * @param ctx the parse tree
	 */
	void exitDb_name(OracleParser.Db_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#database_file_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_file_clauses(OracleParser.Database_file_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#database_file_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_file_clauses(OracleParser.Database_file_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void enterCreate_datafile_clause(OracleParser.Create_datafile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void exitCreate_datafile_clause(OracleParser.Create_datafile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_datafile_clause(OracleParser.Alter_datafile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_datafile_clause(OracleParser.Alter_datafile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_tempfile_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tempfile_clause(OracleParser.Alter_tempfile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_tempfile_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tempfile_clause(OracleParser.Alter_tempfile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#move_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void enterMove_datafile_clause(OracleParser.Move_datafile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#move_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void exitMove_datafile_clause(OracleParser.Move_datafile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_clauses(OracleParser.Logfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_clauses(OracleParser.Logfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAdd_logfile_clauses(OracleParser.Add_logfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAdd_logfile_clauses(OracleParser.Add_logfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#group_redo_logfile}.
	 * @param ctx the parse tree
	 */
	void enterGroup_redo_logfile(OracleParser.Group_redo_logfileContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#group_redo_logfile}.
	 * @param ctx the parse tree
	 */
	void exitGroup_redo_logfile(OracleParser.Group_redo_logfileContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDrop_logfile_clauses(OracleParser.Drop_logfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDrop_logfile_clauses(OracleParser.Drop_logfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#switch_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void enterSwitch_logfile_clause(OracleParser.Switch_logfile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#switch_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void exitSwitch_logfile_clause(OracleParser.Switch_logfile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_db_logging}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_db_logging(OracleParser.Supplemental_db_loggingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_db_logging}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_db_logging(OracleParser.Supplemental_db_loggingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_or_drop}.
	 * @param ctx the parse tree
	 */
	void enterAdd_or_drop(OracleParser.Add_or_dropContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_or_drop}.
	 * @param ctx the parse tree
	 */
	void exitAdd_or_drop(OracleParser.Add_or_dropContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_plsql_clause}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_plsql_clause(OracleParser.Supplemental_plsql_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_plsql_clause}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_plsql_clause(OracleParser.Supplemental_plsql_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logfile_descriptor}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_descriptor(OracleParser.Logfile_descriptorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logfile_descriptor}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_descriptor(OracleParser.Logfile_descriptorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#controlfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterControlfile_clauses(OracleParser.Controlfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#controlfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitControlfile_clauses(OracleParser.Controlfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trace_file_clause}.
	 * @param ctx the parse tree
	 */
	void enterTrace_file_clause(OracleParser.Trace_file_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trace_file_clause}.
	 * @param ctx the parse tree
	 */
	void exitTrace_file_clause(OracleParser.Trace_file_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#standby_database_clauses}.
	 * @param ctx the parse tree
	 */
	void enterStandby_database_clauses(OracleParser.Standby_database_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#standby_database_clauses}.
	 * @param ctx the parse tree
	 */
	void exitStandby_database_clauses(OracleParser.Standby_database_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#activate_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void enterActivate_standby_db_clause(OracleParser.Activate_standby_db_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#activate_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void exitActivate_standby_db_clause(OracleParser.Activate_standby_db_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#maximize_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void enterMaximize_standby_db_clause(OracleParser.Maximize_standby_db_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#maximize_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void exitMaximize_standby_db_clause(OracleParser.Maximize_standby_db_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#register_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void enterRegister_logfile_clause(OracleParser.Register_logfile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#register_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void exitRegister_logfile_clause(OracleParser.Register_logfile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#commit_switchover_clause}.
	 * @param ctx the parse tree
	 */
	void enterCommit_switchover_clause(OracleParser.Commit_switchover_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#commit_switchover_clause}.
	 * @param ctx the parse tree
	 */
	void exitCommit_switchover_clause(OracleParser.Commit_switchover_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#start_standby_clause}.
	 * @param ctx the parse tree
	 */
	void enterStart_standby_clause(OracleParser.Start_standby_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#start_standby_clause}.
	 * @param ctx the parse tree
	 */
	void exitStart_standby_clause(OracleParser.Start_standby_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#stop_standby_clause}.
	 * @param ctx the parse tree
	 */
	void enterStop_standby_clause(OracleParser.Stop_standby_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#stop_standby_clause}.
	 * @param ctx the parse tree
	 */
	void exitStop_standby_clause(OracleParser.Stop_standby_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#convert_database_clause}.
	 * @param ctx the parse tree
	 */
	void enterConvert_database_clause(OracleParser.Convert_database_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#convert_database_clause}.
	 * @param ctx the parse tree
	 */
	void exitConvert_database_clause(OracleParser.Convert_database_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_settings_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_settings_clause(OracleParser.Default_settings_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_settings_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_settings_clause(OracleParser.Default_settings_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_time_zone_clause}.
	 * @param ctx the parse tree
	 */
	void enterSet_time_zone_clause(OracleParser.Set_time_zone_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_time_zone_clause}.
	 * @param ctx the parse tree
	 */
	void exitSet_time_zone_clause(OracleParser.Set_time_zone_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#instance_clauses}.
	 * @param ctx the parse tree
	 */
	void enterInstance_clauses(OracleParser.Instance_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#instance_clauses}.
	 * @param ctx the parse tree
	 */
	void exitInstance_clauses(OracleParser.Instance_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#security_clause}.
	 * @param ctx the parse tree
	 */
	void enterSecurity_clause(OracleParser.Security_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#security_clause}.
	 * @param ctx the parse tree
	 */
	void exitSecurity_clause(OracleParser.Security_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#domain}.
	 * @param ctx the parse tree
	 */
	void enterDomain(OracleParser.DomainContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#domain}.
	 * @param ctx the parse tree
	 */
	void exitDomain(OracleParser.DomainContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#database}.
	 * @param ctx the parse tree
	 */
	void enterDatabase(OracleParser.DatabaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#database}.
	 * @param ctx the parse tree
	 */
	void exitDatabase(OracleParser.DatabaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#edition_name}.
	 * @param ctx the parse tree
	 */
	void enterEdition_name(OracleParser.Edition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#edition_name}.
	 * @param ctx the parse tree
	 */
	void exitEdition_name(OracleParser.Edition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#filenumber}.
	 * @param ctx the parse tree
	 */
	void enterFilenumber(OracleParser.FilenumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#filenumber}.
	 * @param ctx the parse tree
	 */
	void exitFilenumber(OracleParser.FilenumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#filename}.
	 * @param ctx the parse tree
	 */
	void enterFilename(OracleParser.FilenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#filename}.
	 * @param ctx the parse tree
	 */
	void exitFilename(OracleParser.FilenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#prepare_clause}.
	 * @param ctx the parse tree
	 */
	void enterPrepare_clause(OracleParser.Prepare_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#prepare_clause}.
	 * @param ctx the parse tree
	 */
	void exitPrepare_clause(OracleParser.Prepare_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_mirror_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_mirror_clause(OracleParser.Drop_mirror_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_mirror_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_mirror_clause(OracleParser.Drop_mirror_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lost_write_protection}.
	 * @param ctx the parse tree
	 */
	void enterLost_write_protection(OracleParser.Lost_write_protectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lost_write_protection}.
	 * @param ctx the parse tree
	 */
	void exitLost_write_protection(OracleParser.Lost_write_protectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cdb_fleet_clauses}.
	 * @param ctx the parse tree
	 */
	void enterCdb_fleet_clauses(OracleParser.Cdb_fleet_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cdb_fleet_clauses}.
	 * @param ctx the parse tree
	 */
	void exitCdb_fleet_clauses(OracleParser.Cdb_fleet_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lead_cdb_clause}.
	 * @param ctx the parse tree
	 */
	void enterLead_cdb_clause(OracleParser.Lead_cdb_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lead_cdb_clause}.
	 * @param ctx the parse tree
	 */
	void exitLead_cdb_clause(OracleParser.Lead_cdb_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lead_cdb_uri_clause}.
	 * @param ctx the parse tree
	 */
	void enterLead_cdb_uri_clause(OracleParser.Lead_cdb_uri_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lead_cdb_uri_clause}.
	 * @param ctx the parse tree
	 */
	void exitLead_cdb_uri_clause(OracleParser.Lead_cdb_uri_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#property_clauses}.
	 * @param ctx the parse tree
	 */
	void enterProperty_clauses(OracleParser.Property_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#property_clauses}.
	 * @param ctx the parse tree
	 */
	void exitProperty_clauses(OracleParser.Property_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#replay_upgrade_clauses}.
	 * @param ctx the parse tree
	 */
	void enterReplay_upgrade_clauses(OracleParser.Replay_upgrade_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#replay_upgrade_clauses}.
	 * @param ctx the parse tree
	 */
	void exitReplay_upgrade_clauses(OracleParser.Replay_upgrade_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_database_link}.
	 * @param ctx the parse tree
	 */
	void enterAlter_database_link(OracleParser.Alter_database_linkContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_database_link}.
	 * @param ctx the parse tree
	 */
	void exitAlter_database_link(OracleParser.Alter_database_linkContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#password_value}.
	 * @param ctx the parse tree
	 */
	void enterPassword_value(OracleParser.Password_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#password_value}.
	 * @param ctx the parse tree
	 */
	void exitPassword_value(OracleParser.Password_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#link_authentication}.
	 * @param ctx the parse tree
	 */
	void enterLink_authentication(OracleParser.Link_authenticationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#link_authentication}.
	 * @param ctx the parse tree
	 */
	void exitLink_authentication(OracleParser.Link_authenticationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_database}.
	 * @param ctx the parse tree
	 */
	void enterCreate_database(OracleParser.Create_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_database}.
	 * @param ctx the parse tree
	 */
	void exitCreate_database(OracleParser.Create_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#database_logging_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_logging_clauses(OracleParser.Database_logging_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#database_logging_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_logging_clauses(OracleParser.Database_logging_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#database_logging_sub_clause}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_logging_sub_clause(OracleParser.Database_logging_sub_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#database_logging_sub_clause}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_logging_sub_clause(OracleParser.Database_logging_sub_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_clauses(OracleParser.Tablespace_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_clauses(OracleParser.Tablespace_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#enable_pluggable_database}.
	 * @param ctx the parse tree
	 */
	void enterEnable_pluggable_database(OracleParser.Enable_pluggable_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#enable_pluggable_database}.
	 * @param ctx the parse tree
	 */
	void exitEnable_pluggable_database(OracleParser.Enable_pluggable_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#file_name_convert}.
	 * @param ctx the parse tree
	 */
	void enterFile_name_convert(OracleParser.File_name_convertContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#file_name_convert}.
	 * @param ctx the parse tree
	 */
	void exitFile_name_convert(OracleParser.File_name_convertContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#filename_convert_sub_clause}.
	 * @param ctx the parse tree
	 */
	void enterFilename_convert_sub_clause(OracleParser.Filename_convert_sub_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#filename_convert_sub_clause}.
	 * @param ctx the parse tree
	 */
	void exitFilename_convert_sub_clause(OracleParser.Filename_convert_sub_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_datafile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_datafile_clauses(OracleParser.Tablespace_datafile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_datafile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_datafile_clauses(OracleParser.Tablespace_datafile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#undo_mode_clause}.
	 * @param ctx the parse tree
	 */
	void enterUndo_mode_clause(OracleParser.Undo_mode_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#undo_mode_clause}.
	 * @param ctx the parse tree
	 */
	void exitUndo_mode_clause(OracleParser.Undo_mode_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterDefault_tablespace(OracleParser.Default_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitDefault_tablespace(OracleParser.Default_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_temp_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterDefault_temp_tablespace(OracleParser.Default_temp_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_temp_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitDefault_temp_tablespace(OracleParser.Default_temp_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#undo_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterUndo_tablespace(OracleParser.Undo_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#undo_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitUndo_tablespace(OracleParser.Undo_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_database}.
	 * @param ctx the parse tree
	 */
	void enterDrop_database(OracleParser.Drop_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_database}.
	 * @param ctx the parse tree
	 */
	void exitDrop_database(OracleParser.Drop_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_database_link}.
	 * @param ctx the parse tree
	 */
	void enterCreate_database_link(OracleParser.Create_database_linkContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_database_link}.
	 * @param ctx the parse tree
	 */
	void exitCreate_database_link(OracleParser.Create_database_linkContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dblink}.
	 * @param ctx the parse tree
	 */
	void enterDblink(OracleParser.DblinkContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dblink}.
	 * @param ctx the parse tree
	 */
	void exitDblink(OracleParser.DblinkContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_database_link}.
	 * @param ctx the parse tree
	 */
	void enterDrop_database_link(OracleParser.Drop_database_linkContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_database_link}.
	 * @param ctx the parse tree
	 */
	void exitDrop_database_link(OracleParser.Drop_database_linkContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_tablespace_set}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablespace_set(OracleParser.Alter_tablespace_setContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_tablespace_set}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablespace_set(OracleParser.Alter_tablespace_setContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_tablespace_attrs}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablespace_attrs(OracleParser.Alter_tablespace_attrsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_tablespace_attrs}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablespace_attrs(OracleParser.Alter_tablespace_attrsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_tablespace_encryption}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablespace_encryption(OracleParser.Alter_tablespace_encryptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_tablespace_encryption}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablespace_encryption(OracleParser.Alter_tablespace_encryptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ts_file_name_convert}.
	 * @param ctx the parse tree
	 */
	void enterTs_file_name_convert(OracleParser.Ts_file_name_convertContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ts_file_name_convert}.
	 * @param ctx the parse tree
	 */
	void exitTs_file_name_convert(OracleParser.Ts_file_name_convertContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_role}.
	 * @param ctx the parse tree
	 */
	void enterAlter_role(OracleParser.Alter_roleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_role}.
	 * @param ctx the parse tree
	 */
	void exitAlter_role(OracleParser.Alter_roleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#role_identified_clause}.
	 * @param ctx the parse tree
	 */
	void enterRole_identified_clause(OracleParser.Role_identified_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#role_identified_clause}.
	 * @param ctx the parse tree
	 */
	void exitRole_identified_clause(OracleParser.Role_identified_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_table}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table(OracleParser.Alter_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_table}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table(OracleParser.Alter_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#memoptimize_read_write_clause}.
	 * @param ctx the parse tree
	 */
	void enterMemoptimize_read_write_clause(OracleParser.Memoptimize_read_write_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#memoptimize_read_write_clause}.
	 * @param ctx the parse tree
	 */
	void exitMemoptimize_read_write_clause(OracleParser.Memoptimize_read_write_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_table_properties}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_properties(OracleParser.Alter_table_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_table_properties}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_properties(OracleParser.Alter_table_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_table_partitioning}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_partitioning(OracleParser.Alter_table_partitioningContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_table_partitioning}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_partitioning(OracleParser.Alter_table_partitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_table_partition}.
	 * @param ctx the parse tree
	 */
	void enterAdd_table_partition(OracleParser.Add_table_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_table_partition}.
	 * @param ctx the parse tree
	 */
	void exitAdd_table_partition(OracleParser.Add_table_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_table_partition}.
	 * @param ctx the parse tree
	 */
	void enterDrop_table_partition(OracleParser.Drop_table_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_table_partition}.
	 * @param ctx the parse tree
	 */
	void exitDrop_table_partition(OracleParser.Drop_table_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_table_partition}.
	 * @param ctx the parse tree
	 */
	void enterMerge_table_partition(OracleParser.Merge_table_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_table_partition}.
	 * @param ctx the parse tree
	 */
	void exitMerge_table_partition(OracleParser.Merge_table_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_table_partition}.
	 * @param ctx the parse tree
	 */
	void enterModify_table_partition(OracleParser.Modify_table_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_table_partition}.
	 * @param ctx the parse tree
	 */
	void exitModify_table_partition(OracleParser.Modify_table_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#split_table_partition}.
	 * @param ctx the parse tree
	 */
	void enterSplit_table_partition(OracleParser.Split_table_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#split_table_partition}.
	 * @param ctx the parse tree
	 */
	void exitSplit_table_partition(OracleParser.Split_table_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#truncate_table_partition}.
	 * @param ctx the parse tree
	 */
	void enterTruncate_table_partition(OracleParser.Truncate_table_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#truncate_table_partition}.
	 * @param ctx the parse tree
	 */
	void exitTruncate_table_partition(OracleParser.Truncate_table_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exchange_table_partition}.
	 * @param ctx the parse tree
	 */
	void enterExchange_table_partition(OracleParser.Exchange_table_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exchange_table_partition}.
	 * @param ctx the parse tree
	 */
	void exitExchange_table_partition(OracleParser.Exchange_table_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#coalesce_table_partition}.
	 * @param ctx the parse tree
	 */
	void enterCoalesce_table_partition(OracleParser.Coalesce_table_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#coalesce_table_partition}.
	 * @param ctx the parse tree
	 */
	void exitCoalesce_table_partition(OracleParser.Coalesce_table_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_interval_partition}.
	 * @param ctx the parse tree
	 */
	void enterAlter_interval_partition(OracleParser.Alter_interval_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_interval_partition}.
	 * @param ctx the parse tree
	 */
	void exitAlter_interval_partition(OracleParser.Alter_interval_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_extended_names}.
	 * @param ctx the parse tree
	 */
	void enterPartition_extended_names(OracleParser.Partition_extended_namesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_extended_names}.
	 * @param ctx the parse tree
	 */
	void exitPartition_extended_names(OracleParser.Partition_extended_namesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_extended_names}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_extended_names(OracleParser.Subpartition_extended_namesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_extended_names}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_extended_names(OracleParser.Subpartition_extended_namesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_table_properties_1}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_properties_1(OracleParser.Alter_table_properties_1Context ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_table_properties_1}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_properties_1(OracleParser.Alter_table_properties_1Context ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_iot_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAlter_iot_clauses(OracleParser.Alter_iot_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_iot_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAlter_iot_clauses(OracleParser.Alter_iot_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_mapping_table_clause(OracleParser.Alter_mapping_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_mapping_table_clause(OracleParser.Alter_mapping_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_overflow_clause(OracleParser.Alter_overflow_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_overflow_clause(OracleParser.Alter_overflow_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_overflow_clause(OracleParser.Add_overflow_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_overflow_clause(OracleParser.Add_overflow_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#update_index_clauses}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_index_clauses(OracleParser.Update_index_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#update_index_clauses}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_index_clauses(OracleParser.Update_index_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#update_global_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_global_index_clause(OracleParser.Update_global_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#update_global_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_global_index_clause(OracleParser.Update_global_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#update_all_indexes_clause}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_all_indexes_clause(OracleParser.Update_all_indexes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#update_all_indexes_clause}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_all_indexes_clause(OracleParser.Update_all_indexes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#update_all_indexes_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_all_indexes_index_clause(OracleParser.Update_all_indexes_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#update_all_indexes_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_all_indexes_index_clause(OracleParser.Update_all_indexes_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#update_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_index_partition(OracleParser.Update_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#update_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_index_partition(OracleParser.Update_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#update_index_subpartition}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_index_subpartition(OracleParser.Update_index_subpartitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#update_index_subpartition}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_index_subpartition(OracleParser.Update_index_subpartitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#enable_disable_clause}.
	 * @param ctx the parse tree
	 */
	void enterEnable_disable_clause(OracleParser.Enable_disable_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#enable_disable_clause}.
	 * @param ctx the parse tree
	 */
	void exitEnable_disable_clause(OracleParser.Enable_disable_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_index_clause(OracleParser.Using_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_index_clause(OracleParser.Using_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_attributes}.
	 * @param ctx the parse tree
	 */
	void enterIndex_attributes(OracleParser.Index_attributesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_attributes}.
	 * @param ctx the parse tree
	 */
	void exitIndex_attributes(OracleParser.Index_attributesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sort_or_nosort}.
	 * @param ctx the parse tree
	 */
	void enterSort_or_nosort(OracleParser.Sort_or_nosortContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sort_or_nosort}.
	 * @param ctx the parse tree
	 */
	void exitSort_or_nosort(OracleParser.Sort_or_nosortContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exceptions_clause}.
	 * @param ctx the parse tree
	 */
	void enterExceptions_clause(OracleParser.Exceptions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exceptions_clause}.
	 * @param ctx the parse tree
	 */
	void exitExceptions_clause(OracleParser.Exceptions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#move_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterMove_table_clause(OracleParser.Move_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#move_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitMove_table_clause(OracleParser.Move_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_org_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_org_table_clause(OracleParser.Index_org_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_org_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_org_table_clause(OracleParser.Index_org_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterMapping_table_clause(OracleParser.Mapping_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitMapping_table_clause(OracleParser.Mapping_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#key_compression}.
	 * @param ctx the parse tree
	 */
	void enterKey_compression(OracleParser.Key_compressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#key_compression}.
	 * @param ctx the parse tree
	 */
	void exitKey_compression(OracleParser.Key_compressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_org_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_org_overflow_clause(OracleParser.Index_org_overflow_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_org_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_org_overflow_clause(OracleParser.Index_org_overflow_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterColumn_clauses(OracleParser.Column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitColumn_clauses(OracleParser.Column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_collection_retrieval}.
	 * @param ctx the parse tree
	 */
	void enterModify_collection_retrieval(OracleParser.Modify_collection_retrievalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_collection_retrieval}.
	 * @param ctx the parse tree
	 */
	void exitModify_collection_retrieval(OracleParser.Modify_collection_retrievalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#collection_item}.
	 * @param ctx the parse tree
	 */
	void enterCollection_item(OracleParser.Collection_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#collection_item}.
	 * @param ctx the parse tree
	 */
	void exitCollection_item(OracleParser.Collection_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rename_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterRename_column_clause(OracleParser.Rename_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rename_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitRename_column_clause(OracleParser.Rename_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#old_column_name}.
	 * @param ctx the parse tree
	 */
	void enterOld_column_name(OracleParser.Old_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#old_column_name}.
	 * @param ctx the parse tree
	 */
	void exitOld_column_name(OracleParser.Old_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_column_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_column_name(OracleParser.New_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_column_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_column_name(OracleParser.New_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_modify_drop_column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAdd_modify_drop_column_clauses(OracleParser.Add_modify_drop_column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_modify_drop_column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAdd_modify_drop_column_clauses(OracleParser.Add_modify_drop_column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_column_clause(OracleParser.Drop_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_column_clause(OracleParser.Drop_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterModify_column_clauses(OracleParser.Modify_column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitModify_column_clauses(OracleParser.Modify_column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterModify_col_properties(OracleParser.Modify_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitModify_col_properties(OracleParser.Modify_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_col_substitutable}.
	 * @param ctx the parse tree
	 */
	void enterModify_col_substitutable(OracleParser.Modify_col_substitutableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_col_substitutable}.
	 * @param ctx the parse tree
	 */
	void exitModify_col_substitutable(OracleParser.Modify_col_substitutableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_column_clause(OracleParser.Add_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_column_clause(OracleParser.Add_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterAlter_varray_col_properties(OracleParser.Alter_varray_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitAlter_varray_col_properties(OracleParser.Alter_varray_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterVarray_col_properties(OracleParser.Varray_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitVarray_col_properties(OracleParser.Varray_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#varray_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterVarray_storage_clause(OracleParser.Varray_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#varray_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitVarray_storage_clause(OracleParser.Varray_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_segname}.
	 * @param ctx the parse tree
	 */
	void enterLob_segname(OracleParser.Lob_segnameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_segname}.
	 * @param ctx the parse tree
	 */
	void exitLob_segname(OracleParser.Lob_segnameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_item}.
	 * @param ctx the parse tree
	 */
	void enterLob_item(OracleParser.Lob_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_item}.
	 * @param ctx the parse tree
	 */
	void exitLob_item(OracleParser.Lob_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_storage_parameters}.
	 * @param ctx the parse tree
	 */
	void enterLob_storage_parameters(OracleParser.Lob_storage_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_storage_parameters}.
	 * @param ctx the parse tree
	 */
	void exitLob_storage_parameters(OracleParser.Lob_storage_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_storage_clause(OracleParser.Lob_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_storage_clause(OracleParser.Lob_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterModify_lob_storage_clause(OracleParser.Modify_lob_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitModify_lob_storage_clause(OracleParser.Modify_lob_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_lob_parameters}.
	 * @param ctx the parse tree
	 */
	void enterModify_lob_parameters(OracleParser.Modify_lob_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_lob_parameters}.
	 * @param ctx the parse tree
	 */
	void exitModify_lob_parameters(OracleParser.Modify_lob_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_parameters}.
	 * @param ctx the parse tree
	 */
	void enterLob_parameters(OracleParser.Lob_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_parameters}.
	 * @param ctx the parse tree
	 */
	void exitLob_parameters(OracleParser.Lob_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_deduplicate_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_deduplicate_clause(OracleParser.Lob_deduplicate_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_deduplicate_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_deduplicate_clause(OracleParser.Lob_deduplicate_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_compression_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_compression_clause(OracleParser.Lob_compression_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_compression_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_compression_clause(OracleParser.Lob_compression_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_retention_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_retention_clause(OracleParser.Lob_retention_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_retention_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_retention_clause(OracleParser.Lob_retention_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#encryption_spec}.
	 * @param ctx the parse tree
	 */
	void enterEncryption_spec(OracleParser.Encryption_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#encryption_spec}.
	 * @param ctx the parse tree
	 */
	void exitEncryption_spec(OracleParser.Encryption_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace}.
	 * @param ctx the parse tree
	 */
	void enterTablespace(OracleParser.TablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace}.
	 * @param ctx the parse tree
	 */
	void exitTablespace(OracleParser.TablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#varray_item}.
	 * @param ctx the parse tree
	 */
	void enterVarray_item(OracleParser.Varray_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#varray_item}.
	 * @param ctx the parse tree
	 */
	void exitVarray_item(OracleParser.Varray_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_properties}.
	 * @param ctx the parse tree
	 */
	void enterColumn_properties(OracleParser.Column_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_properties}.
	 * @param ctx the parse tree
	 */
	void exitColumn_properties(OracleParser.Column_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_partition_storage}.
	 * @param ctx the parse tree
	 */
	void enterLob_partition_storage(OracleParser.Lob_partition_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_partition_storage}.
	 * @param ctx the parse tree
	 */
	void exitLob_partition_storage(OracleParser.Lob_partition_storageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#period_definition}.
	 * @param ctx the parse tree
	 */
	void enterPeriod_definition(OracleParser.Period_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#period_definition}.
	 * @param ctx the parse tree
	 */
	void exitPeriod_definition(OracleParser.Period_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#start_time_column}.
	 * @param ctx the parse tree
	 */
	void enterStart_time_column(OracleParser.Start_time_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#start_time_column}.
	 * @param ctx the parse tree
	 */
	void exitStart_time_column(OracleParser.Start_time_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#end_time_column}.
	 * @param ctx the parse tree
	 */
	void enterEnd_time_column(OracleParser.End_time_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#end_time_column}.
	 * @param ctx the parse tree
	 */
	void exitEnd_time_column(OracleParser.End_time_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_definition}.
	 * @param ctx the parse tree
	 */
	void enterColumn_definition(OracleParser.Column_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_definition}.
	 * @param ctx the parse tree
	 */
	void exitColumn_definition(OracleParser.Column_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_collation_name}.
	 * @param ctx the parse tree
	 */
	void enterColumn_collation_name(OracleParser.Column_collation_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_collation_name}.
	 * @param ctx the parse tree
	 */
	void exitColumn_collation_name(OracleParser.Column_collation_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identity_clause}.
	 * @param ctx the parse tree
	 */
	void enterIdentity_clause(OracleParser.Identity_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identity_clause}.
	 * @param ctx the parse tree
	 */
	void exitIdentity_clause(OracleParser.Identity_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identity_options_parentheses}.
	 * @param ctx the parse tree
	 */
	void enterIdentity_options_parentheses(OracleParser.Identity_options_parenthesesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identity_options_parentheses}.
	 * @param ctx the parse tree
	 */
	void exitIdentity_options_parentheses(OracleParser.Identity_options_parenthesesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identity_options}.
	 * @param ctx the parse tree
	 */
	void enterIdentity_options(OracleParser.Identity_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identity_options}.
	 * @param ctx the parse tree
	 */
	void exitIdentity_options(OracleParser.Identity_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#virtual_column_definition}.
	 * @param ctx the parse tree
	 */
	void enterVirtual_column_definition(OracleParser.Virtual_column_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#virtual_column_definition}.
	 * @param ctx the parse tree
	 */
	void exitVirtual_column_definition(OracleParser.Virtual_column_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#autogenerated_sequence_definition}.
	 * @param ctx the parse tree
	 */
	void enterAutogenerated_sequence_definition(OracleParser.Autogenerated_sequence_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#autogenerated_sequence_definition}.
	 * @param ctx the parse tree
	 */
	void exitAutogenerated_sequence_definition(OracleParser.Autogenerated_sequence_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#evaluation_edition_clause}.
	 * @param ctx the parse tree
	 */
	void enterEvaluation_edition_clause(OracleParser.Evaluation_edition_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#evaluation_edition_clause}.
	 * @param ctx the parse tree
	 */
	void exitEvaluation_edition_clause(OracleParser.Evaluation_edition_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#out_of_line_part_storage}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_part_storage(OracleParser.Out_of_line_part_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#out_of_line_part_storage}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_part_storage(OracleParser.Out_of_line_part_storageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#nested_table_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterNested_table_col_properties(OracleParser.Nested_table_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#nested_table_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitNested_table_col_properties(OracleParser.Nested_table_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#nested_item}.
	 * @param ctx the parse tree
	 */
	void enterNested_item(OracleParser.Nested_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#nested_item}.
	 * @param ctx the parse tree
	 */
	void exitNested_item(OracleParser.Nested_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#substitutable_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterSubstitutable_column_clause(OracleParser.Substitutable_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#substitutable_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitSubstitutable_column_clause(OracleParser.Substitutable_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_name}.
	 * @param ctx the parse tree
	 */
	void enterPartition_name(OracleParser.Partition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_name}.
	 * @param ctx the parse tree
	 */
	void exitPartition_name(OracleParser.Partition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_logging_props}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_logging_props(OracleParser.Supplemental_logging_propsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_logging_props}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_logging_props(OracleParser.Supplemental_logging_propsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_or_attribute}.
	 * @param ctx the parse tree
	 */
	void enterColumn_or_attribute(OracleParser.Column_or_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_or_attribute}.
	 * @param ctx the parse tree
	 */
	void exitColumn_or_attribute(OracleParser.Column_or_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_type_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterObject_type_col_properties(OracleParser.Object_type_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_type_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitObject_type_col_properties(OracleParser.Object_type_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constraint_clauses}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_clauses(OracleParser.Constraint_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constraint_clauses}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_clauses(OracleParser.Constraint_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#old_constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterOld_constraint_name(OracleParser.Old_constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#old_constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitOld_constraint_name(OracleParser.Old_constraint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_constraint_name(OracleParser.New_constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_constraint_name(OracleParser.New_constraint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_constraint_clause(OracleParser.Drop_constraint_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_constraint_clause(OracleParser.Drop_constraint_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_primary_key_or_unique_or_generic_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_primary_key_or_unique_or_generic_clause(OracleParser.Drop_primary_key_or_unique_or_generic_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_primary_key_or_unique_or_generic_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_primary_key_or_unique_or_generic_clause(OracleParser.Drop_primary_key_or_unique_or_generic_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_constraint}.
	 * @param ctx the parse tree
	 */
	void enterAdd_constraint(OracleParser.Add_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_constraint}.
	 * @param ctx the parse tree
	 */
	void exitAdd_constraint(OracleParser.Add_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_constraint_clause(OracleParser.Add_constraint_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_constraint_clause(OracleParser.Add_constraint_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#check_constraint}.
	 * @param ctx the parse tree
	 */
	void enterCheck_constraint(OracleParser.Check_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#check_constraint}.
	 * @param ctx the parse tree
	 */
	void exitCheck_constraint(OracleParser.Check_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_constraint}.
	 * @param ctx the parse tree
	 */
	void enterDrop_constraint(OracleParser.Drop_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_constraint}.
	 * @param ctx the parse tree
	 */
	void exitDrop_constraint(OracleParser.Drop_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#enable_constraint}.
	 * @param ctx the parse tree
	 */
	void enterEnable_constraint(OracleParser.Enable_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#enable_constraint}.
	 * @param ctx the parse tree
	 */
	void exitEnable_constraint(OracleParser.Enable_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#disable_constraint}.
	 * @param ctx the parse tree
	 */
	void enterDisable_constraint(OracleParser.Disable_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#disable_constraint}.
	 * @param ctx the parse tree
	 */
	void exitDisable_constraint(OracleParser.Disable_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#foreign_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterForeign_key_clause(OracleParser.Foreign_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#foreign_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitForeign_key_clause(OracleParser.Foreign_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#references_clause}.
	 * @param ctx the parse tree
	 */
	void enterReferences_clause(OracleParser.References_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#references_clause}.
	 * @param ctx the parse tree
	 */
	void exitReferences_clause(OracleParser.References_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_delete_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_delete_clause(OracleParser.On_delete_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_delete_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_delete_clause(OracleParser.On_delete_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unique_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnique_key_clause(OracleParser.Unique_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unique_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnique_key_clause(OracleParser.Unique_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#primary_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterPrimary_key_clause(OracleParser.Primary_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#primary_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitPrimary_key_clause(OracleParser.Primary_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#anonymous_block}.
	 * @param ctx the parse tree
	 */
	void enterAnonymous_block(OracleParser.Anonymous_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#anonymous_block}.
	 * @param ctx the parse tree
	 */
	void exitAnonymous_block(OracleParser.Anonymous_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#invoker_rights_clause}.
	 * @param ctx the parse tree
	 */
	void enterInvoker_rights_clause(OracleParser.Invoker_rights_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#invoker_rights_clause}.
	 * @param ctx the parse tree
	 */
	void exitInvoker_rights_clause(OracleParser.Invoker_rights_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#call_spec}.
	 * @param ctx the parse tree
	 */
	void enterCall_spec(OracleParser.Call_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#call_spec}.
	 * @param ctx the parse tree
	 */
	void exitCall_spec(OracleParser.Call_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#java_spec}.
	 * @param ctx the parse tree
	 */
	void enterJava_spec(OracleParser.Java_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#java_spec}.
	 * @param ctx the parse tree
	 */
	void exitJava_spec(OracleParser.Java_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#c_spec}.
	 * @param ctx the parse tree
	 */
	void enterC_spec(OracleParser.C_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#c_spec}.
	 * @param ctx the parse tree
	 */
	void exitC_spec(OracleParser.C_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#c_agent_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterC_agent_in_clause(OracleParser.C_agent_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#c_agent_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitC_agent_in_clause(OracleParser.C_agent_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#c_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void enterC_parameters_clause(OracleParser.C_parameters_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#c_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void exitC_parameters_clause(OracleParser.C_parameters_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(OracleParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(OracleParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_value_part}.
	 * @param ctx the parse tree
	 */
	void enterDefault_value_part(OracleParser.Default_value_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_value_part}.
	 * @param ctx the parse tree
	 */
	void exitDefault_value_part(OracleParser.Default_value_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#seq_of_declare_specs}.
	 * @param ctx the parse tree
	 */
	void enterSeq_of_declare_specs(OracleParser.Seq_of_declare_specsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#seq_of_declare_specs}.
	 * @param ctx the parse tree
	 */
	void exitSeq_of_declare_specs(OracleParser.Seq_of_declare_specsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#declare_spec}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_spec(OracleParser.Declare_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#declare_spec}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_spec(OracleParser.Declare_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void enterVariable_declaration(OracleParser.Variable_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void exitVariable_declaration(OracleParser.Variable_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subtype_declaration}.
	 * @param ctx the parse tree
	 */
	void enterSubtype_declaration(OracleParser.Subtype_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subtype_declaration}.
	 * @param ctx the parse tree
	 */
	void exitSubtype_declaration(OracleParser.Subtype_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cursor_declaration}.
	 * @param ctx the parse tree
	 */
	void enterCursor_declaration(OracleParser.Cursor_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cursor_declaration}.
	 * @param ctx the parse tree
	 */
	void exitCursor_declaration(OracleParser.Cursor_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parameter_spec}.
	 * @param ctx the parse tree
	 */
	void enterParameter_spec(OracleParser.Parameter_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parameter_spec}.
	 * @param ctx the parse tree
	 */
	void exitParameter_spec(OracleParser.Parameter_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exception_declaration}.
	 * @param ctx the parse tree
	 */
	void enterException_declaration(OracleParser.Exception_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exception_declaration}.
	 * @param ctx the parse tree
	 */
	void exitException_declaration(OracleParser.Exception_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pragma_declaration}.
	 * @param ctx the parse tree
	 */
	void enterPragma_declaration(OracleParser.Pragma_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pragma_declaration}.
	 * @param ctx the parse tree
	 */
	void exitPragma_declaration(OracleParser.Pragma_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#record_type_def}.
	 * @param ctx the parse tree
	 */
	void enterRecord_type_def(OracleParser.Record_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#record_type_def}.
	 * @param ctx the parse tree
	 */
	void exitRecord_type_def(OracleParser.Record_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#field_spec}.
	 * @param ctx the parse tree
	 */
	void enterField_spec(OracleParser.Field_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#field_spec}.
	 * @param ctx the parse tree
	 */
	void exitField_spec(OracleParser.Field_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ref_cursor_type_def}.
	 * @param ctx the parse tree
	 */
	void enterRef_cursor_type_def(OracleParser.Ref_cursor_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ref_cursor_type_def}.
	 * @param ctx the parse tree
	 */
	void exitRef_cursor_type_def(OracleParser.Ref_cursor_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_declaration}.
	 * @param ctx the parse tree
	 */
	void enterType_declaration(OracleParser.Type_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_declaration}.
	 * @param ctx the parse tree
	 */
	void exitType_declaration(OracleParser.Type_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_type_def}.
	 * @param ctx the parse tree
	 */
	void enterTable_type_def(OracleParser.Table_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_type_def}.
	 * @param ctx the parse tree
	 */
	void exitTable_type_def(OracleParser.Table_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_indexed_by_part}.
	 * @param ctx the parse tree
	 */
	void enterTable_indexed_by_part(OracleParser.Table_indexed_by_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_indexed_by_part}.
	 * @param ctx the parse tree
	 */
	void exitTable_indexed_by_part(OracleParser.Table_indexed_by_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#varray_type_def}.
	 * @param ctx the parse tree
	 */
	void enterVarray_type_def(OracleParser.Varray_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#varray_type_def}.
	 * @param ctx the parse tree
	 */
	void exitVarray_type_def(OracleParser.Varray_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#seq_of_statements}.
	 * @param ctx the parse tree
	 */
	void enterSeq_of_statements(OracleParser.Seq_of_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#seq_of_statements}.
	 * @param ctx the parse tree
	 */
	void exitSeq_of_statements(OracleParser.Seq_of_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#label_declaration}.
	 * @param ctx the parse tree
	 */
	void enterLabel_declaration(OracleParser.Label_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#label_declaration}.
	 * @param ctx the parse tree
	 */
	void exitLabel_declaration(OracleParser.Label_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(OracleParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(OracleParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#swallow_to_semi}.
	 * @param ctx the parse tree
	 */
	void enterSwallow_to_semi(OracleParser.Swallow_to_semiContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#swallow_to_semi}.
	 * @param ctx the parse tree
	 */
	void exitSwallow_to_semi(OracleParser.Swallow_to_semiContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_statement(OracleParser.Assignment_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_statement(OracleParser.Assignment_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#continue_statement}.
	 * @param ctx the parse tree
	 */
	void enterContinue_statement(OracleParser.Continue_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#continue_statement}.
	 * @param ctx the parse tree
	 */
	void exitContinue_statement(OracleParser.Continue_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exit_statement}.
	 * @param ctx the parse tree
	 */
	void enterExit_statement(OracleParser.Exit_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exit_statement}.
	 * @param ctx the parse tree
	 */
	void exitExit_statement(OracleParser.Exit_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#goto_statement}.
	 * @param ctx the parse tree
	 */
	void enterGoto_statement(OracleParser.Goto_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#goto_statement}.
	 * @param ctx the parse tree
	 */
	void exitGoto_statement(OracleParser.Goto_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void enterIf_statement(OracleParser.If_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void exitIf_statement(OracleParser.If_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#elsif_part}.
	 * @param ctx the parse tree
	 */
	void enterElsif_part(OracleParser.Elsif_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#elsif_part}.
	 * @param ctx the parse tree
	 */
	void exitElsif_part(OracleParser.Elsif_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#else_part}.
	 * @param ctx the parse tree
	 */
	void enterElse_part(OracleParser.Else_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#else_part}.
	 * @param ctx the parse tree
	 */
	void exitElse_part(OracleParser.Else_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void enterLoop_statement(OracleParser.Loop_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void exitLoop_statement(OracleParser.Loop_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cursor_loop_param}.
	 * @param ctx the parse tree
	 */
	void enterCursor_loop_param(OracleParser.Cursor_loop_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cursor_loop_param}.
	 * @param ctx the parse tree
	 */
	void exitCursor_loop_param(OracleParser.Cursor_loop_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#forall_statement}.
	 * @param ctx the parse tree
	 */
	void enterForall_statement(OracleParser.Forall_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#forall_statement}.
	 * @param ctx the parse tree
	 */
	void exitForall_statement(OracleParser.Forall_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#bounds_clause}.
	 * @param ctx the parse tree
	 */
	void enterBounds_clause(OracleParser.Bounds_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#bounds_clause}.
	 * @param ctx the parse tree
	 */
	void exitBounds_clause(OracleParser.Bounds_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#between_bound}.
	 * @param ctx the parse tree
	 */
	void enterBetween_bound(OracleParser.Between_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#between_bound}.
	 * @param ctx the parse tree
	 */
	void exitBetween_bound(OracleParser.Between_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lower_bound}.
	 * @param ctx the parse tree
	 */
	void enterLower_bound(OracleParser.Lower_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lower_bound}.
	 * @param ctx the parse tree
	 */
	void exitLower_bound(OracleParser.Lower_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#upper_bound}.
	 * @param ctx the parse tree
	 */
	void enterUpper_bound(OracleParser.Upper_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#upper_bound}.
	 * @param ctx the parse tree
	 */
	void exitUpper_bound(OracleParser.Upper_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#null_statement}.
	 * @param ctx the parse tree
	 */
	void enterNull_statement(OracleParser.Null_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#null_statement}.
	 * @param ctx the parse tree
	 */
	void exitNull_statement(OracleParser.Null_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#raise_statement}.
	 * @param ctx the parse tree
	 */
	void enterRaise_statement(OracleParser.Raise_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#raise_statement}.
	 * @param ctx the parse tree
	 */
	void exitRaise_statement(OracleParser.Raise_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void enterReturn_statement(OracleParser.Return_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void exitReturn_statement(OracleParser.Return_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#call_statement}.
	 * @param ctx the parse tree
	 */
	void enterCall_statement(OracleParser.Call_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#call_statement}.
	 * @param ctx the parse tree
	 */
	void exitCall_statement(OracleParser.Call_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pipe_row_statement}.
	 * @param ctx the parse tree
	 */
	void enterPipe_row_statement(OracleParser.Pipe_row_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pipe_row_statement}.
	 * @param ctx the parse tree
	 */
	void exitPipe_row_statement(OracleParser.Pipe_row_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#body}.
	 * @param ctx the parse tree
	 */
	void enterBody(OracleParser.BodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#body}.
	 * @param ctx the parse tree
	 */
	void exitBody(OracleParser.BodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exception_handler}.
	 * @param ctx the parse tree
	 */
	void enterException_handler(OracleParser.Exception_handlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exception_handler}.
	 * @param ctx the parse tree
	 */
	void exitException_handler(OracleParser.Exception_handlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trigger_block}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_block(OracleParser.Trigger_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trigger_block}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_block(OracleParser.Trigger_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(OracleParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(OracleParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_statement}.
	 * @param ctx the parse tree
	 */
	void enterSql_statement(OracleParser.Sql_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_statement}.
	 * @param ctx the parse tree
	 */
	void exitSql_statement(OracleParser.Sql_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#execute_immediate}.
	 * @param ctx the parse tree
	 */
	void enterExecute_immediate(OracleParser.Execute_immediateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#execute_immediate}.
	 * @param ctx the parse tree
	 */
	void exitExecute_immediate(OracleParser.Execute_immediateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dynamic_returning_clause}.
	 * @param ctx the parse tree
	 */
	void enterDynamic_returning_clause(OracleParser.Dynamic_returning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dynamic_returning_clause}.
	 * @param ctx the parse tree
	 */
	void exitDynamic_returning_clause(OracleParser.Dynamic_returning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#data_manipulation_language_statements}.
	 * @param ctx the parse tree
	 */
	void enterData_manipulation_language_statements(OracleParser.Data_manipulation_language_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#data_manipulation_language_statements}.
	 * @param ctx the parse tree
	 */
	void exitData_manipulation_language_statements(OracleParser.Data_manipulation_language_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cursor_manipulation_statements}.
	 * @param ctx the parse tree
	 */
	void enterCursor_manipulation_statements(OracleParser.Cursor_manipulation_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cursor_manipulation_statements}.
	 * @param ctx the parse tree
	 */
	void exitCursor_manipulation_statements(OracleParser.Cursor_manipulation_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#close_statement}.
	 * @param ctx the parse tree
	 */
	void enterClose_statement(OracleParser.Close_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#close_statement}.
	 * @param ctx the parse tree
	 */
	void exitClose_statement(OracleParser.Close_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#open_statement}.
	 * @param ctx the parse tree
	 */
	void enterOpen_statement(OracleParser.Open_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#open_statement}.
	 * @param ctx the parse tree
	 */
	void exitOpen_statement(OracleParser.Open_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#fetch_statement}.
	 * @param ctx the parse tree
	 */
	void enterFetch_statement(OracleParser.Fetch_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#fetch_statement}.
	 * @param ctx the parse tree
	 */
	void exitFetch_statement(OracleParser.Fetch_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#open_for_statement}.
	 * @param ctx the parse tree
	 */
	void enterOpen_for_statement(OracleParser.Open_for_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#open_for_statement}.
	 * @param ctx the parse tree
	 */
	void exitOpen_for_statement(OracleParser.Open_for_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#transaction_control_statements}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_control_statements(OracleParser.Transaction_control_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#transaction_control_statements}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_control_statements(OracleParser.Transaction_control_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_transaction_command}.
	 * @param ctx the parse tree
	 */
	void enterSet_transaction_command(OracleParser.Set_transaction_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_transaction_command}.
	 * @param ctx the parse tree
	 */
	void exitSet_transaction_command(OracleParser.Set_transaction_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_constraint_command}.
	 * @param ctx the parse tree
	 */
	void enterSet_constraint_command(OracleParser.Set_constraint_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_constraint_command}.
	 * @param ctx the parse tree
	 */
	void exitSet_constraint_command(OracleParser.Set_constraint_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#commit_statement}.
	 * @param ctx the parse tree
	 */
	void enterCommit_statement(OracleParser.Commit_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#commit_statement}.
	 * @param ctx the parse tree
	 */
	void exitCommit_statement(OracleParser.Commit_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#write_clause}.
	 * @param ctx the parse tree
	 */
	void enterWrite_clause(OracleParser.Write_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#write_clause}.
	 * @param ctx the parse tree
	 */
	void exitWrite_clause(OracleParser.Write_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rollback_statement}.
	 * @param ctx the parse tree
	 */
	void enterRollback_statement(OracleParser.Rollback_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rollback_statement}.
	 * @param ctx the parse tree
	 */
	void exitRollback_statement(OracleParser.Rollback_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#savepoint_statement}.
	 * @param ctx the parse tree
	 */
	void enterSavepoint_statement(OracleParser.Savepoint_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#savepoint_statement}.
	 * @param ctx the parse tree
	 */
	void exitSavepoint_statement(OracleParser.Savepoint_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#explain_statement}.
	 * @param ctx the parse tree
	 */
	void enterExplain_statement(OracleParser.Explain_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#explain_statement}.
	 * @param ctx the parse tree
	 */
	void exitExplain_statement(OracleParser.Explain_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#select_only_statement}.
	 * @param ctx the parse tree
	 */
	void enterSelect_only_statement(OracleParser.Select_only_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#select_only_statement}.
	 * @param ctx the parse tree
	 */
	void exitSelect_only_statement(OracleParser.Select_only_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#select_statement}.
	 * @param ctx the parse tree
	 */
	void enterSelect_statement(OracleParser.Select_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#select_statement}.
	 * @param ctx the parse tree
	 */
	void exitSelect_statement(OracleParser.Select_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subquery_factoring_clause}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_factoring_clause(OracleParser.Subquery_factoring_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subquery_factoring_clause}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_factoring_clause(OracleParser.Subquery_factoring_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#factoring_element}.
	 * @param ctx the parse tree
	 */
	void enterFactoring_element(OracleParser.Factoring_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#factoring_element}.
	 * @param ctx the parse tree
	 */
	void exitFactoring_element(OracleParser.Factoring_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#search_clause}.
	 * @param ctx the parse tree
	 */
	void enterSearch_clause(OracleParser.Search_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#search_clause}.
	 * @param ctx the parse tree
	 */
	void exitSearch_clause(OracleParser.Search_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cycle_clause}.
	 * @param ctx the parse tree
	 */
	void enterCycle_clause(OracleParser.Cycle_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cycle_clause}.
	 * @param ctx the parse tree
	 */
	void exitCycle_clause(OracleParser.Cycle_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subquery}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(OracleParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subquery}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(OracleParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subquery_basic_elements}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_basic_elements(OracleParser.Subquery_basic_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subquery_basic_elements}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_basic_elements(OracleParser.Subquery_basic_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subquery_operation_part}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_operation_part(OracleParser.Subquery_operation_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subquery_operation_part}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_operation_part(OracleParser.Subquery_operation_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#query_block}.
	 * @param ctx the parse tree
	 */
	void enterQuery_block(OracleParser.Query_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#query_block}.
	 * @param ctx the parse tree
	 */
	void exitQuery_block(OracleParser.Query_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#selected_list}.
	 * @param ctx the parse tree
	 */
	void enterSelected_list(OracleParser.Selected_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#selected_list}.
	 * @param ctx the parse tree
	 */
	void exitSelected_list(OracleParser.Selected_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#from_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_clause(OracleParser.From_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#from_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_clause(OracleParser.From_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#select_list_elements}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list_elements(OracleParser.Select_list_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#select_list_elements}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list_elements(OracleParser.Select_list_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_ref_list}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_list(OracleParser.Table_ref_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_ref_list}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_list(OracleParser.Table_ref_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_ref}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref(OracleParser.Table_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_ref}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref(OracleParser.Table_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_ref_aux}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux(OracleParser.Table_ref_auxContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_ref_aux}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux(OracleParser.Table_ref_auxContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table_ref_aux_internal_one}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux_internal_one(OracleParser.Table_ref_aux_internal_oneContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table_ref_aux_internal_one}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux_internal_one(OracleParser.Table_ref_aux_internal_oneContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table_ref_aux_internal_two}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux_internal_two(OracleParser.Table_ref_aux_internal_twoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table_ref_aux_internal_two}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux_internal_two(OracleParser.Table_ref_aux_internal_twoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table_ref_aux_internal_three}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux_internal_three(OracleParser.Table_ref_aux_internal_threeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table_ref_aux_internal_three}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux_internal_three(OracleParser.Table_ref_aux_internal_threeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#join_clause}.
	 * @param ctx the parse tree
	 */
	void enterJoin_clause(OracleParser.Join_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#join_clause}.
	 * @param ctx the parse tree
	 */
	void exitJoin_clause(OracleParser.Join_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#join_on_part}.
	 * @param ctx the parse tree
	 */
	void enterJoin_on_part(OracleParser.Join_on_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#join_on_part}.
	 * @param ctx the parse tree
	 */
	void exitJoin_on_part(OracleParser.Join_on_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#join_using_part}.
	 * @param ctx the parse tree
	 */
	void enterJoin_using_part(OracleParser.Join_using_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#join_using_part}.
	 * @param ctx the parse tree
	 */
	void exitJoin_using_part(OracleParser.Join_using_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#outer_join_type}.
	 * @param ctx the parse tree
	 */
	void enterOuter_join_type(OracleParser.Outer_join_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#outer_join_type}.
	 * @param ctx the parse tree
	 */
	void exitOuter_join_type(OracleParser.Outer_join_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#query_partition_clause}.
	 * @param ctx the parse tree
	 */
	void enterQuery_partition_clause(OracleParser.Query_partition_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#query_partition_clause}.
	 * @param ctx the parse tree
	 */
	void exitQuery_partition_clause(OracleParser.Query_partition_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#flashback_query_clause}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_query_clause(OracleParser.Flashback_query_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#flashback_query_clause}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_query_clause(OracleParser.Flashback_query_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_clause(OracleParser.Pivot_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_clause(OracleParser.Pivot_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_element}.
	 * @param ctx the parse tree
	 */
	void enterPivot_element(OracleParser.Pivot_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_element}.
	 * @param ctx the parse tree
	 */
	void exitPivot_element(OracleParser.Pivot_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_for_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_for_clause(OracleParser.Pivot_for_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_for_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_for_clause(OracleParser.Pivot_for_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_in_clause(OracleParser.Pivot_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_in_clause(OracleParser.Pivot_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_in_clause_element}.
	 * @param ctx the parse tree
	 */
	void enterPivot_in_clause_element(OracleParser.Pivot_in_clause_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_in_clause_element}.
	 * @param ctx the parse tree
	 */
	void exitPivot_in_clause_element(OracleParser.Pivot_in_clause_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_in_clause_elements}.
	 * @param ctx the parse tree
	 */
	void enterPivot_in_clause_elements(OracleParser.Pivot_in_clause_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_in_clause_elements}.
	 * @param ctx the parse tree
	 */
	void exitPivot_in_clause_elements(OracleParser.Pivot_in_clause_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unpivot_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_clause(OracleParser.Unpivot_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unpivot_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_clause(OracleParser.Unpivot_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unpivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_in_clause(OracleParser.Unpivot_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unpivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_in_clause(OracleParser.Unpivot_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unpivot_in_elements}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_in_elements(OracleParser.Unpivot_in_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unpivot_in_elements}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_in_elements(OracleParser.Unpivot_in_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hierarchical_query_clause}.
	 * @param ctx the parse tree
	 */
	void enterHierarchical_query_clause(OracleParser.Hierarchical_query_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hierarchical_query_clause}.
	 * @param ctx the parse tree
	 */
	void exitHierarchical_query_clause(OracleParser.Hierarchical_query_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#start_part}.
	 * @param ctx the parse tree
	 */
	void enterStart_part(OracleParser.Start_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#start_part}.
	 * @param ctx the parse tree
	 */
	void exitStart_part(OracleParser.Start_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#group_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_clause(OracleParser.Group_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#group_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_clause(OracleParser.Group_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#group_by_elements}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_elements(OracleParser.Group_by_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#group_by_elements}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_elements(OracleParser.Group_by_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rollup_cube_clause}.
	 * @param ctx the parse tree
	 */
	void enterRollup_cube_clause(OracleParser.Rollup_cube_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rollup_cube_clause}.
	 * @param ctx the parse tree
	 */
	void exitRollup_cube_clause(OracleParser.Rollup_cube_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets_clause(OracleParser.Grouping_sets_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets_clause(OracleParser.Grouping_sets_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#grouping_sets_elements}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets_elements(OracleParser.Grouping_sets_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#grouping_sets_elements}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets_elements(OracleParser.Grouping_sets_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#having_clause}.
	 * @param ctx the parse tree
	 */
	void enterHaving_clause(OracleParser.Having_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#having_clause}.
	 * @param ctx the parse tree
	 */
	void exitHaving_clause(OracleParser.Having_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_clause}.
	 * @param ctx the parse tree
	 */
	void enterModel_clause(OracleParser.Model_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_clause}.
	 * @param ctx the parse tree
	 */
	void exitModel_clause(OracleParser.Model_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cell_reference_options}.
	 * @param ctx the parse tree
	 */
	void enterCell_reference_options(OracleParser.Cell_reference_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cell_reference_options}.
	 * @param ctx the parse tree
	 */
	void exitCell_reference_options(OracleParser.Cell_reference_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#return_rows_clause}.
	 * @param ctx the parse tree
	 */
	void enterReturn_rows_clause(OracleParser.Return_rows_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#return_rows_clause}.
	 * @param ctx the parse tree
	 */
	void exitReturn_rows_clause(OracleParser.Return_rows_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#reference_model}.
	 * @param ctx the parse tree
	 */
	void enterReference_model(OracleParser.Reference_modelContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#reference_model}.
	 * @param ctx the parse tree
	 */
	void exitReference_model(OracleParser.Reference_modelContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#main_model}.
	 * @param ctx the parse tree
	 */
	void enterMain_model(OracleParser.Main_modelContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#main_model}.
	 * @param ctx the parse tree
	 */
	void exitMain_model(OracleParser.Main_modelContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterModel_column_clauses(OracleParser.Model_column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitModel_column_clauses(OracleParser.Model_column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_column_partition_part}.
	 * @param ctx the parse tree
	 */
	void enterModel_column_partition_part(OracleParser.Model_column_partition_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_column_partition_part}.
	 * @param ctx the parse tree
	 */
	void exitModel_column_partition_part(OracleParser.Model_column_partition_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_column_list}.
	 * @param ctx the parse tree
	 */
	void enterModel_column_list(OracleParser.Model_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_column_list}.
	 * @param ctx the parse tree
	 */
	void exitModel_column_list(OracleParser.Model_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_column}.
	 * @param ctx the parse tree
	 */
	void enterModel_column(OracleParser.Model_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_column}.
	 * @param ctx the parse tree
	 */
	void exitModel_column(OracleParser.Model_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_rules_clause}.
	 * @param ctx the parse tree
	 */
	void enterModel_rules_clause(OracleParser.Model_rules_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_rules_clause}.
	 * @param ctx the parse tree
	 */
	void exitModel_rules_clause(OracleParser.Model_rules_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_rules_part}.
	 * @param ctx the parse tree
	 */
	void enterModel_rules_part(OracleParser.Model_rules_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_rules_part}.
	 * @param ctx the parse tree
	 */
	void exitModel_rules_part(OracleParser.Model_rules_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_rules_element}.
	 * @param ctx the parse tree
	 */
	void enterModel_rules_element(OracleParser.Model_rules_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_rules_element}.
	 * @param ctx the parse tree
	 */
	void exitModel_rules_element(OracleParser.Model_rules_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cell_assignment}.
	 * @param ctx the parse tree
	 */
	void enterCell_assignment(OracleParser.Cell_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cell_assignment}.
	 * @param ctx the parse tree
	 */
	void exitCell_assignment(OracleParser.Cell_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_iterate_clause}.
	 * @param ctx the parse tree
	 */
	void enterModel_iterate_clause(OracleParser.Model_iterate_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_iterate_clause}.
	 * @param ctx the parse tree
	 */
	void exitModel_iterate_clause(OracleParser.Model_iterate_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#until_part}.
	 * @param ctx the parse tree
	 */
	void enterUntil_part(OracleParser.Until_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#until_part}.
	 * @param ctx the parse tree
	 */
	void exitUntil_part(OracleParser.Until_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#order_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by_clause(OracleParser.Order_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#order_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by_clause(OracleParser.Order_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#order_by_elements}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by_elements(OracleParser.Order_by_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#order_by_elements}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by_elements(OracleParser.Order_by_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#offset_clause}.
	 * @param ctx the parse tree
	 */
	void enterOffset_clause(OracleParser.Offset_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#offset_clause}.
	 * @param ctx the parse tree
	 */
	void exitOffset_clause(OracleParser.Offset_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#fetch_clause}.
	 * @param ctx the parse tree
	 */
	void enterFetch_clause(OracleParser.Fetch_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#fetch_clause}.
	 * @param ctx the parse tree
	 */
	void exitFetch_clause(OracleParser.Fetch_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#for_update_clause}.
	 * @param ctx the parse tree
	 */
	void enterFor_update_clause(OracleParser.For_update_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#for_update_clause}.
	 * @param ctx the parse tree
	 */
	void exitFor_update_clause(OracleParser.For_update_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#for_update_of_part}.
	 * @param ctx the parse tree
	 */
	void enterFor_update_of_part(OracleParser.For_update_of_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#for_update_of_part}.
	 * @param ctx the parse tree
	 */
	void exitFor_update_of_part(OracleParser.For_update_of_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#for_update_options}.
	 * @param ctx the parse tree
	 */
	void enterFor_update_options(OracleParser.For_update_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#for_update_options}.
	 * @param ctx the parse tree
	 */
	void exitFor_update_options(OracleParser.For_update_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#update_statement}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_statement(OracleParser.Update_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#update_statement}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_statement(OracleParser.Update_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#update_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_set_clause(OracleParser.Update_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#update_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_set_clause(OracleParser.Update_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_based_update_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterColumn_based_update_set_clause(OracleParser.Column_based_update_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_based_update_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitColumn_based_update_set_clause(OracleParser.Column_based_update_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#delete_statement}.
	 * @param ctx the parse tree
	 */
	void enterDelete_statement(OracleParser.Delete_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#delete_statement}.
	 * @param ctx the parse tree
	 */
	void exitDelete_statement(OracleParser.Delete_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#insert_statement}.
	 * @param ctx the parse tree
	 */
	void enterInsert_statement(OracleParser.Insert_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#insert_statement}.
	 * @param ctx the parse tree
	 */
	void exitInsert_statement(OracleParser.Insert_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#single_table_insert}.
	 * @param ctx the parse tree
	 */
	void enterSingle_table_insert(OracleParser.Single_table_insertContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#single_table_insert}.
	 * @param ctx the parse tree
	 */
	void exitSingle_table_insert(OracleParser.Single_table_insertContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#multi_table_insert}.
	 * @param ctx the parse tree
	 */
	void enterMulti_table_insert(OracleParser.Multi_table_insertContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#multi_table_insert}.
	 * @param ctx the parse tree
	 */
	void exitMulti_table_insert(OracleParser.Multi_table_insertContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#multi_table_element}.
	 * @param ctx the parse tree
	 */
	void enterMulti_table_element(OracleParser.Multi_table_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#multi_table_element}.
	 * @param ctx the parse tree
	 */
	void exitMulti_table_element(OracleParser.Multi_table_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#conditional_insert_clause}.
	 * @param ctx the parse tree
	 */
	void enterConditional_insert_clause(OracleParser.Conditional_insert_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#conditional_insert_clause}.
	 * @param ctx the parse tree
	 */
	void exitConditional_insert_clause(OracleParser.Conditional_insert_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#conditional_insert_when_part}.
	 * @param ctx the parse tree
	 */
	void enterConditional_insert_when_part(OracleParser.Conditional_insert_when_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#conditional_insert_when_part}.
	 * @param ctx the parse tree
	 */
	void exitConditional_insert_when_part(OracleParser.Conditional_insert_when_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#conditional_insert_else_part}.
	 * @param ctx the parse tree
	 */
	void enterConditional_insert_else_part(OracleParser.Conditional_insert_else_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#conditional_insert_else_part}.
	 * @param ctx the parse tree
	 */
	void exitConditional_insert_else_part(OracleParser.Conditional_insert_else_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#insert_into_clause}.
	 * @param ctx the parse tree
	 */
	void enterInsert_into_clause(OracleParser.Insert_into_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#insert_into_clause}.
	 * @param ctx the parse tree
	 */
	void exitInsert_into_clause(OracleParser.Insert_into_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#values_clause}.
	 * @param ctx the parse tree
	 */
	void enterValues_clause(OracleParser.Values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#values_clause}.
	 * @param ctx the parse tree
	 */
	void exitValues_clause(OracleParser.Values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_statement}.
	 * @param ctx the parse tree
	 */
	void enterMerge_statement(OracleParser.Merge_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_statement}.
	 * @param ctx the parse tree
	 */
	void exitMerge_statement(OracleParser.Merge_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_update_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_update_clause(OracleParser.Merge_update_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_update_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_update_clause(OracleParser.Merge_update_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_element}.
	 * @param ctx the parse tree
	 */
	void enterMerge_element(OracleParser.Merge_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_element}.
	 * @param ctx the parse tree
	 */
	void exitMerge_element(OracleParser.Merge_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_update_delete_part}.
	 * @param ctx the parse tree
	 */
	void enterMerge_update_delete_part(OracleParser.Merge_update_delete_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_update_delete_part}.
	 * @param ctx the parse tree
	 */
	void exitMerge_update_delete_part(OracleParser.Merge_update_delete_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_insert_clause(OracleParser.Merge_insert_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_insert_clause(OracleParser.Merge_insert_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#selected_tableview}.
	 * @param ctx the parse tree
	 */
	void enterSelected_tableview(OracleParser.Selected_tableviewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#selected_tableview}.
	 * @param ctx the parse tree
	 */
	void exitSelected_tableview(OracleParser.Selected_tableviewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lock_table_statement}.
	 * @param ctx the parse tree
	 */
	void enterLock_table_statement(OracleParser.Lock_table_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lock_table_statement}.
	 * @param ctx the parse tree
	 */
	void exitLock_table_statement(OracleParser.Lock_table_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#wait_nowait_part}.
	 * @param ctx the parse tree
	 */
	void enterWait_nowait_part(OracleParser.Wait_nowait_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#wait_nowait_part}.
	 * @param ctx the parse tree
	 */
	void exitWait_nowait_part(OracleParser.Wait_nowait_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lock_table_element}.
	 * @param ctx the parse tree
	 */
	void enterLock_table_element(OracleParser.Lock_table_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lock_table_element}.
	 * @param ctx the parse tree
	 */
	void exitLock_table_element(OracleParser.Lock_table_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lock_mode}.
	 * @param ctx the parse tree
	 */
	void enterLock_mode(OracleParser.Lock_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lock_mode}.
	 * @param ctx the parse tree
	 */
	void exitLock_mode(OracleParser.Lock_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#general_table_ref}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_table_ref(OracleParser.General_table_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#general_table_ref}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_table_ref(OracleParser.General_table_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#static_returning_clause}.
	 * @param ctx the parse tree
	 */
	void enterStatic_returning_clause(OracleParser.Static_returning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#static_returning_clause}.
	 * @param ctx the parse tree
	 */
	void exitStatic_returning_clause(OracleParser.Static_returning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#error_logging_clause}.
	 * @param ctx the parse tree
	 */
	void enterError_logging_clause(OracleParser.Error_logging_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#error_logging_clause}.
	 * @param ctx the parse tree
	 */
	void exitError_logging_clause(OracleParser.Error_logging_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#error_logging_into_part}.
	 * @param ctx the parse tree
	 */
	void enterError_logging_into_part(OracleParser.Error_logging_into_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#error_logging_into_part}.
	 * @param ctx the parse tree
	 */
	void exitError_logging_into_part(OracleParser.Error_logging_into_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#error_logging_reject_part}.
	 * @param ctx the parse tree
	 */
	void enterError_logging_reject_part(OracleParser.Error_logging_reject_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#error_logging_reject_part}.
	 * @param ctx the parse tree
	 */
	void exitError_logging_reject_part(OracleParser.Error_logging_reject_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dml_table_expression_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_table_expression_clause(OracleParser.Dml_table_expression_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dml_table_expression_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_table_expression_clause(OracleParser.Dml_table_expression_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_collection_expression}.
	 * @param ctx the parse tree
	 */
	void enterTable_collection_expression(OracleParser.Table_collection_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_collection_expression}.
	 * @param ctx the parse tree
	 */
	void exitTable_collection_expression(OracleParser.Table_collection_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subquery_restriction_clause}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_restriction_clause(OracleParser.Subquery_restriction_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subquery_restriction_clause}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_restriction_clause(OracleParser.Subquery_restriction_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sample_clause}.
	 * @param ctx the parse tree
	 */
	void enterSample_clause(OracleParser.Sample_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sample_clause}.
	 * @param ctx the parse tree
	 */
	void exitSample_clause(OracleParser.Sample_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#seed_part}.
	 * @param ctx the parse tree
	 */
	void enterSeed_part(OracleParser.Seed_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#seed_part}.
	 * @param ctx the parse tree
	 */
	void exitSeed_part(OracleParser.Seed_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(OracleParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(OracleParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_condition}.
	 * @param ctx the parse tree
	 */
	void enterJson_condition(OracleParser.Json_conditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_condition}.
	 * @param ctx the parse tree
	 */
	void exitJson_condition(OracleParser.Json_conditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#expressions}.
	 * @param ctx the parse tree
	 */
	void enterExpressions(OracleParser.ExpressionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#expressions}.
	 * @param ctx the parse tree
	 */
	void exitExpressions(OracleParser.ExpressionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(OracleParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(OracleParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cursor_expression}.
	 * @param ctx the parse tree
	 */
	void enterCursor_expression(OracleParser.Cursor_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cursor_expression}.
	 * @param ctx the parse tree
	 */
	void exitCursor_expression(OracleParser.Cursor_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logical_expression}.
	 * @param ctx the parse tree
	 */
	void enterLogical_expression(OracleParser.Logical_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logical_expression}.
	 * @param ctx the parse tree
	 */
	void exitLogical_expression(OracleParser.Logical_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unary_logical_expression}.
	 * @param ctx the parse tree
	 */
	void enterUnary_logical_expression(OracleParser.Unary_logical_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unary_logical_expression}.
	 * @param ctx the parse tree
	 */
	void exitUnary_logical_expression(OracleParser.Unary_logical_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logical_operation}.
	 * @param ctx the parse tree
	 */
	void enterLogical_operation(OracleParser.Logical_operationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logical_operation}.
	 * @param ctx the parse tree
	 */
	void exitLogical_operation(OracleParser.Logical_operationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#multiset_expression}.
	 * @param ctx the parse tree
	 */
	void enterMultiset_expression(OracleParser.Multiset_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#multiset_expression}.
	 * @param ctx the parse tree
	 */
	void exitMultiset_expression(OracleParser.Multiset_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#relational_expression}.
	 * @param ctx the parse tree
	 */
	void enterRelational_expression(OracleParser.Relational_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#relational_expression}.
	 * @param ctx the parse tree
	 */
	void exitRelational_expression(OracleParser.Relational_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compound_expression}.
	 * @param ctx the parse tree
	 */
	void enterCompound_expression(OracleParser.Compound_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compound_expression}.
	 * @param ctx the parse tree
	 */
	void exitCompound_expression(OracleParser.Compound_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#relational_operator}.
	 * @param ctx the parse tree
	 */
	void enterRelational_operator(OracleParser.Relational_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#relational_operator}.
	 * @param ctx the parse tree
	 */
	void exitRelational_operator(OracleParser.Relational_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#in_elements}.
	 * @param ctx the parse tree
	 */
	void enterIn_elements(OracleParser.In_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#in_elements}.
	 * @param ctx the parse tree
	 */
	void exitIn_elements(OracleParser.In_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#between_elements}.
	 * @param ctx the parse tree
	 */
	void enterBetween_elements(OracleParser.Between_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#between_elements}.
	 * @param ctx the parse tree
	 */
	void exitBetween_elements(OracleParser.Between_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#concatenation}.
	 * @param ctx the parse tree
	 */
	void enterConcatenation(OracleParser.ConcatenationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#concatenation}.
	 * @param ctx the parse tree
	 */
	void exitConcatenation(OracleParser.ConcatenationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#interval_expression}.
	 * @param ctx the parse tree
	 */
	void enterInterval_expression(OracleParser.Interval_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#interval_expression}.
	 * @param ctx the parse tree
	 */
	void exitInterval_expression(OracleParser.Interval_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_expression}.
	 * @param ctx the parse tree
	 */
	void enterModel_expression(OracleParser.Model_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_expression}.
	 * @param ctx the parse tree
	 */
	void exitModel_expression(OracleParser.Model_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_expression_element}.
	 * @param ctx the parse tree
	 */
	void enterModel_expression_element(OracleParser.Model_expression_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_expression_element}.
	 * @param ctx the parse tree
	 */
	void exitModel_expression_element(OracleParser.Model_expression_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#single_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void enterSingle_column_for_loop(OracleParser.Single_column_for_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#single_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void exitSingle_column_for_loop(OracleParser.Single_column_for_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#multi_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void enterMulti_column_for_loop(OracleParser.Multi_column_for_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#multi_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void exitMulti_column_for_loop(OracleParser.Multi_column_for_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unary_expression}.
	 * @param ctx the parse tree
	 */
	void enterUnary_expression(OracleParser.Unary_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unary_expression}.
	 * @param ctx the parse tree
	 */
	void exitUnary_expression(OracleParser.Unary_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#case_statement}.
	 * @param ctx the parse tree
	 */
	void enterCase_statement(OracleParser.Case_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#case_statement}.
	 * @param ctx the parse tree
	 */
	void exitCase_statement(OracleParser.Case_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#simple_case_statement}.
	 * @param ctx the parse tree
	 */
	void enterSimple_case_statement(OracleParser.Simple_case_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#simple_case_statement}.
	 * @param ctx the parse tree
	 */
	void exitSimple_case_statement(OracleParser.Simple_case_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#simple_case_when_part}.
	 * @param ctx the parse tree
	 */
	void enterSimple_case_when_part(OracleParser.Simple_case_when_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#simple_case_when_part}.
	 * @param ctx the parse tree
	 */
	void exitSimple_case_when_part(OracleParser.Simple_case_when_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#searched_case_statement}.
	 * @param ctx the parse tree
	 */
	void enterSearched_case_statement(OracleParser.Searched_case_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#searched_case_statement}.
	 * @param ctx the parse tree
	 */
	void exitSearched_case_statement(OracleParser.Searched_case_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#searched_case_when_part}.
	 * @param ctx the parse tree
	 */
	void enterSearched_case_when_part(OracleParser.Searched_case_when_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#searched_case_when_part}.
	 * @param ctx the parse tree
	 */
	void exitSearched_case_when_part(OracleParser.Searched_case_when_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#case_else_part}.
	 * @param ctx the parse tree
	 */
	void enterCase_else_part(OracleParser.Case_else_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#case_else_part}.
	 * @param ctx the parse tree
	 */
	void exitCase_else_part(OracleParser.Case_else_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterAtom(OracleParser.AtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitAtom(OracleParser.AtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#quantified_expression}.
	 * @param ctx the parse tree
	 */
	void enterQuantified_expression(OracleParser.Quantified_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#quantified_expression}.
	 * @param ctx the parse tree
	 */
	void exitQuantified_expression(OracleParser.Quantified_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#string_function}.
	 * @param ctx the parse tree
	 */
	void enterString_function(OracleParser.String_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#string_function}.
	 * @param ctx the parse tree
	 */
	void exitString_function(OracleParser.String_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#standard_function}.
	 * @param ctx the parse tree
	 */
	void enterStandard_function(OracleParser.Standard_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#standard_function}.
	 * @param ctx the parse tree
	 */
	void exitStandard_function(OracleParser.Standard_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_function}.
	 * @param ctx the parse tree
	 */
	void enterJson_function(OracleParser.Json_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_function}.
	 * @param ctx the parse tree
	 */
	void exitJson_function(OracleParser.Json_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_object_content}.
	 * @param ctx the parse tree
	 */
	void enterJson_object_content(OracleParser.Json_object_contentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_object_content}.
	 * @param ctx the parse tree
	 */
	void exitJson_object_content(OracleParser.Json_object_contentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_object_entry}.
	 * @param ctx the parse tree
	 */
	void enterJson_object_entry(OracleParser.Json_object_entryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_object_entry}.
	 * @param ctx the parse tree
	 */
	void exitJson_object_entry(OracleParser.Json_object_entryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_table_clause(OracleParser.Json_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_table_clause(OracleParser.Json_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_array_element}.
	 * @param ctx the parse tree
	 */
	void enterJson_array_element(OracleParser.Json_array_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_array_element}.
	 * @param ctx the parse tree
	 */
	void exitJson_array_element(OracleParser.Json_array_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_on_null_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_on_null_clause(OracleParser.Json_on_null_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_on_null_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_on_null_clause(OracleParser.Json_on_null_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_return_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_return_clause(OracleParser.Json_return_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_return_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_return_clause(OracleParser.Json_return_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_transform_op}.
	 * @param ctx the parse tree
	 */
	void enterJson_transform_op(OracleParser.Json_transform_opContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_transform_op}.
	 * @param ctx the parse tree
	 */
	void exitJson_transform_op(OracleParser.Json_transform_opContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_column_clause(OracleParser.Json_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_column_clause(OracleParser.Json_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_column_definition}.
	 * @param ctx the parse tree
	 */
	void enterJson_column_definition(OracleParser.Json_column_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_column_definition}.
	 * @param ctx the parse tree
	 */
	void exitJson_column_definition(OracleParser.Json_column_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_query_returning_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_query_returning_clause(OracleParser.Json_query_returning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_query_returning_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_query_returning_clause(OracleParser.Json_query_returning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_query_return_type}.
	 * @param ctx the parse tree
	 */
	void enterJson_query_return_type(OracleParser.Json_query_return_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_query_return_type}.
	 * @param ctx the parse tree
	 */
	void exitJson_query_return_type(OracleParser.Json_query_return_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_query_wrapper_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_query_wrapper_clause(OracleParser.Json_query_wrapper_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_query_wrapper_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_query_wrapper_clause(OracleParser.Json_query_wrapper_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_query_on_error_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_query_on_error_clause(OracleParser.Json_query_on_error_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_query_on_error_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_query_on_error_clause(OracleParser.Json_query_on_error_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_query_on_empty_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_query_on_empty_clause(OracleParser.Json_query_on_empty_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_query_on_empty_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_query_on_empty_clause(OracleParser.Json_query_on_empty_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_value_return_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_return_clause(OracleParser.Json_value_return_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_value_return_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_return_clause(OracleParser.Json_value_return_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_value_return_type}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_return_type(OracleParser.Json_value_return_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_value_return_type}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_return_type(OracleParser.Json_value_return_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#json_value_on_mismatch_clause}.
	 * @param ctx the parse tree
	 */
	void enterJson_value_on_mismatch_clause(OracleParser.Json_value_on_mismatch_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#json_value_on_mismatch_clause}.
	 * @param ctx the parse tree
	 */
	void exitJson_value_on_mismatch_clause(OracleParser.Json_value_on_mismatch_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(OracleParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(OracleParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#numeric_function_wrapper}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_function_wrapper(OracleParser.Numeric_function_wrapperContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#numeric_function_wrapper}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_function_wrapper(OracleParser.Numeric_function_wrapperContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#numeric_function}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_function(OracleParser.Numeric_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#numeric_function}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_function(OracleParser.Numeric_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#listagg_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void enterListagg_overflow_clause(OracleParser.Listagg_overflow_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#listagg_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void exitListagg_overflow_clause(OracleParser.Listagg_overflow_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#other_function}.
	 * @param ctx the parse tree
	 */
	void enterOther_function(OracleParser.Other_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#other_function}.
	 * @param ctx the parse tree
	 */
	void exitOther_function(OracleParser.Other_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void enterOver_clause_keyword(OracleParser.Over_clause_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void exitOver_clause_keyword(OracleParser.Over_clause_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#within_or_over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void enterWithin_or_over_clause_keyword(OracleParser.Within_or_over_clause_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#within_or_over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void exitWithin_or_over_clause_keyword(OracleParser.Within_or_over_clause_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#standard_prediction_function_keyword}.
	 * @param ctx the parse tree
	 */
	void enterStandard_prediction_function_keyword(OracleParser.Standard_prediction_function_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#standard_prediction_function_keyword}.
	 * @param ctx the parse tree
	 */
	void exitStandard_prediction_function_keyword(OracleParser.Standard_prediction_function_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#over_clause}.
	 * @param ctx the parse tree
	 */
	void enterOver_clause(OracleParser.Over_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#over_clause}.
	 * @param ctx the parse tree
	 */
	void exitOver_clause(OracleParser.Over_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#windowing_clause}.
	 * @param ctx the parse tree
	 */
	void enterWindowing_clause(OracleParser.Windowing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#windowing_clause}.
	 * @param ctx the parse tree
	 */
	void exitWindowing_clause(OracleParser.Windowing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#windowing_type}.
	 * @param ctx the parse tree
	 */
	void enterWindowing_type(OracleParser.Windowing_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#windowing_type}.
	 * @param ctx the parse tree
	 */
	void exitWindowing_type(OracleParser.Windowing_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#windowing_elements}.
	 * @param ctx the parse tree
	 */
	void enterWindowing_elements(OracleParser.Windowing_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#windowing_elements}.
	 * @param ctx the parse tree
	 */
	void exitWindowing_elements(OracleParser.Windowing_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_clause(OracleParser.Using_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_clause(OracleParser.Using_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_element}.
	 * @param ctx the parse tree
	 */
	void enterUsing_element(OracleParser.Using_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_element}.
	 * @param ctx the parse tree
	 */
	void exitUsing_element(OracleParser.Using_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#collect_order_by_part}.
	 * @param ctx the parse tree
	 */
	void enterCollect_order_by_part(OracleParser.Collect_order_by_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#collect_order_by_part}.
	 * @param ctx the parse tree
	 */
	void exitCollect_order_by_part(OracleParser.Collect_order_by_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#within_or_over_part}.
	 * @param ctx the parse tree
	 */
	void enterWithin_or_over_part(OracleParser.Within_or_over_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#within_or_over_part}.
	 * @param ctx the parse tree
	 */
	void exitWithin_or_over_part(OracleParser.Within_or_over_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cost_matrix_clause}.
	 * @param ctx the parse tree
	 */
	void enterCost_matrix_clause(OracleParser.Cost_matrix_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cost_matrix_clause}.
	 * @param ctx the parse tree
	 */
	void exitCost_matrix_clause(OracleParser.Cost_matrix_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_passing_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_passing_clause(OracleParser.Xml_passing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_passing_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_passing_clause(OracleParser.Xml_passing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_attributes_clause(OracleParser.Xml_attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_attributes_clause(OracleParser.Xml_attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_namespaces_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_namespaces_clause(OracleParser.Xml_namespaces_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_namespaces_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_namespaces_clause(OracleParser.Xml_namespaces_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_table_column}.
	 * @param ctx the parse tree
	 */
	void enterXml_table_column(OracleParser.Xml_table_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_table_column}.
	 * @param ctx the parse tree
	 */
	void exitXml_table_column(OracleParser.Xml_table_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_general_default_part}.
	 * @param ctx the parse tree
	 */
	void enterXml_general_default_part(OracleParser.Xml_general_default_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_general_default_part}.
	 * @param ctx the parse tree
	 */
	void exitXml_general_default_part(OracleParser.Xml_general_default_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_multiuse_expression_element}.
	 * @param ctx the parse tree
	 */
	void enterXml_multiuse_expression_element(OracleParser.Xml_multiuse_expression_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_multiuse_expression_element}.
	 * @param ctx the parse tree
	 */
	void exitXml_multiuse_expression_element(OracleParser.Xml_multiuse_expression_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlroot_param_version_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlroot_param_version_part(OracleParser.Xmlroot_param_version_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlroot_param_version_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlroot_param_version_part(OracleParser.Xmlroot_param_version_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlroot_param_standalone_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlroot_param_standalone_part(OracleParser.Xmlroot_param_standalone_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlroot_param_standalone_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlroot_param_standalone_part(OracleParser.Xmlroot_param_standalone_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlserialize_param_enconding_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlserialize_param_enconding_part(OracleParser.Xmlserialize_param_enconding_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlserialize_param_enconding_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlserialize_param_enconding_part(OracleParser.Xmlserialize_param_enconding_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlserialize_param_version_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlserialize_param_version_part(OracleParser.Xmlserialize_param_version_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlserialize_param_version_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlserialize_param_version_part(OracleParser.Xmlserialize_param_version_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlserialize_param_ident_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlserialize_param_ident_part(OracleParser.Xmlserialize_param_ident_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlserialize_param_ident_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlserialize_param_ident_part(OracleParser.Xmlserialize_param_ident_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_plus_command}.
	 * @param ctx the parse tree
	 */
	void enterSql_plus_command(OracleParser.Sql_plus_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_plus_command}.
	 * @param ctx the parse tree
	 */
	void exitSql_plus_command(OracleParser.Sql_plus_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#whenever_command}.
	 * @param ctx the parse tree
	 */
	void enterWhenever_command(OracleParser.Whenever_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#whenever_command}.
	 * @param ctx the parse tree
	 */
	void exitWhenever_command(OracleParser.Whenever_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_command}.
	 * @param ctx the parse tree
	 */
	void enterSet_command(OracleParser.Set_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_command}.
	 * @param ctx the parse tree
	 */
	void exitSet_command(OracleParser.Set_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#timing_command}.
	 * @param ctx the parse tree
	 */
	void enterTiming_command(OracleParser.Timing_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#timing_command}.
	 * @param ctx the parse tree
	 */
	void exitTiming_command(OracleParser.Timing_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_extension_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartition_extension_clause(OracleParser.Partition_extension_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_extension_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartition_extension_clause(OracleParser.Partition_extension_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_alias}.
	 * @param ctx the parse tree
	 */
	void enterColumn_alias(OracleParser.Column_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_alias}.
	 * @param ctx the parse tree
	 */
	void exitColumn_alias(OracleParser.Column_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_alias}.
	 * @param ctx the parse tree
	 */
	void enterTable_alias(OracleParser.Table_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_alias}.
	 * @param ctx the parse tree
	 */
	void exitTable_alias(OracleParser.Table_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#where_clause}.
	 * @param ctx the parse tree
	 */
	void enterWhere_clause(OracleParser.Where_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#where_clause}.
	 * @param ctx the parse tree
	 */
	void exitWhere_clause(OracleParser.Where_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#quantitative_where_stmt}.
	 * @param ctx the parse tree
	 */
	void enterQuantitative_where_stmt(OracleParser.Quantitative_where_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#quantitative_where_stmt}.
	 * @param ctx the parse tree
	 */
	void exitQuantitative_where_stmt(OracleParser.Quantitative_where_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void enterInto_clause(OracleParser.Into_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void exitInto_clause(OracleParser.Into_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_column_name}.
	 * @param ctx the parse tree
	 */
	void enterXml_column_name(OracleParser.Xml_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_column_name}.
	 * @param ctx the parse tree
	 */
	void exitXml_column_name(OracleParser.Xml_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cost_class_name}.
	 * @param ctx the parse tree
	 */
	void enterCost_class_name(OracleParser.Cost_class_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cost_class_name}.
	 * @param ctx the parse tree
	 */
	void exitCost_class_name(OracleParser.Cost_class_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#attribute_name}.
	 * @param ctx the parse tree
	 */
	void enterAttribute_name(OracleParser.Attribute_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#attribute_name}.
	 * @param ctx the parse tree
	 */
	void exitAttribute_name(OracleParser.Attribute_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#savepoint_name}.
	 * @param ctx the parse tree
	 */
	void enterSavepoint_name(OracleParser.Savepoint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#savepoint_name}.
	 * @param ctx the parse tree
	 */
	void exitSavepoint_name(OracleParser.Savepoint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rollback_segment_name}.
	 * @param ctx the parse tree
	 */
	void enterRollback_segment_name(OracleParser.Rollback_segment_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rollback_segment_name}.
	 * @param ctx the parse tree
	 */
	void exitRollback_segment_name(OracleParser.Rollback_segment_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_var_name}.
	 * @param ctx the parse tree
	 */
	void enterTable_var_name(OracleParser.Table_var_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_var_name}.
	 * @param ctx the parse tree
	 */
	void exitTable_var_name(OracleParser.Table_var_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#schema_name}.
	 * @param ctx the parse tree
	 */
	void enterSchema_name(OracleParser.Schema_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#schema_name}.
	 * @param ctx the parse tree
	 */
	void exitSchema_name(OracleParser.Schema_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#routine_name}.
	 * @param ctx the parse tree
	 */
	void enterRoutine_name(OracleParser.Routine_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#routine_name}.
	 * @param ctx the parse tree
	 */
	void exitRoutine_name(OracleParser.Routine_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#package_name}.
	 * @param ctx the parse tree
	 */
	void enterPackage_name(OracleParser.Package_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#package_name}.
	 * @param ctx the parse tree
	 */
	void exitPackage_name(OracleParser.Package_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#implementation_type_name}.
	 * @param ctx the parse tree
	 */
	void enterImplementation_type_name(OracleParser.Implementation_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#implementation_type_name}.
	 * @param ctx the parse tree
	 */
	void exitImplementation_type_name(OracleParser.Implementation_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parameter_name}.
	 * @param ctx the parse tree
	 */
	void enterParameter_name(OracleParser.Parameter_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parameter_name}.
	 * @param ctx the parse tree
	 */
	void exitParameter_name(OracleParser.Parameter_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#reference_model_name}.
	 * @param ctx the parse tree
	 */
	void enterReference_model_name(OracleParser.Reference_model_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#reference_model_name}.
	 * @param ctx the parse tree
	 */
	void exitReference_model_name(OracleParser.Reference_model_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#main_model_name}.
	 * @param ctx the parse tree
	 */
	void enterMain_model_name(OracleParser.Main_model_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#main_model_name}.
	 * @param ctx the parse tree
	 */
	void exitMain_model_name(OracleParser.Main_model_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#container_tableview_name}.
	 * @param ctx the parse tree
	 */
	void enterContainer_tableview_name(OracleParser.Container_tableview_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#container_tableview_name}.
	 * @param ctx the parse tree
	 */
	void exitContainer_tableview_name(OracleParser.Container_tableview_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#aggregate_function_name}.
	 * @param ctx the parse tree
	 */
	void enterAggregate_function_name(OracleParser.Aggregate_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#aggregate_function_name}.
	 * @param ctx the parse tree
	 */
	void exitAggregate_function_name(OracleParser.Aggregate_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#query_name}.
	 * @param ctx the parse tree
	 */
	void enterQuery_name(OracleParser.Query_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#query_name}.
	 * @param ctx the parse tree
	 */
	void exitQuery_name(OracleParser.Query_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#grantee_name}.
	 * @param ctx the parse tree
	 */
	void enterGrantee_name(OracleParser.Grantee_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#grantee_name}.
	 * @param ctx the parse tree
	 */
	void exitGrantee_name(OracleParser.Grantee_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#role_name}.
	 * @param ctx the parse tree
	 */
	void enterRole_name(OracleParser.Role_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#role_name}.
	 * @param ctx the parse tree
	 */
	void exitRole_name(OracleParser.Role_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_name(OracleParser.Constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_name(OracleParser.Constraint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#label_name}.
	 * @param ctx the parse tree
	 */
	void enterLabel_name(OracleParser.Label_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#label_name}.
	 * @param ctx the parse tree
	 */
	void exitLabel_name(OracleParser.Label_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_name}.
	 * @param ctx the parse tree
	 */
	void enterType_name(OracleParser.Type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_name}.
	 * @param ctx the parse tree
	 */
	void exitType_name(OracleParser.Type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sequence_name}.
	 * @param ctx the parse tree
	 */
	void enterSequence_name(OracleParser.Sequence_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sequence_name}.
	 * @param ctx the parse tree
	 */
	void exitSequence_name(OracleParser.Sequence_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exception_name}.
	 * @param ctx the parse tree
	 */
	void enterException_name(OracleParser.Exception_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exception_name}.
	 * @param ctx the parse tree
	 */
	void exitException_name(OracleParser.Exception_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_name}.
	 * @param ctx the parse tree
	 */
	void enterFunction_name(OracleParser.Function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_name}.
	 * @param ctx the parse tree
	 */
	void exitFunction_name(OracleParser.Function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#procedure_name}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_name(OracleParser.Procedure_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#procedure_name}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_name(OracleParser.Procedure_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trigger_name}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_name(OracleParser.Trigger_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trigger_name}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_name(OracleParser.Trigger_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#variable_name}.
	 * @param ctx the parse tree
	 */
	void enterVariable_name(OracleParser.Variable_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#variable_name}.
	 * @param ctx the parse tree
	 */
	void exitVariable_name(OracleParser.Variable_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_name}.
	 * @param ctx the parse tree
	 */
	void enterIndex_name(OracleParser.Index_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_name}.
	 * @param ctx the parse tree
	 */
	void exitIndex_name(OracleParser.Index_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cursor_name}.
	 * @param ctx the parse tree
	 */
	void enterCursor_name(OracleParser.Cursor_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cursor_name}.
	 * @param ctx the parse tree
	 */
	void exitCursor_name(OracleParser.Cursor_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#record_name}.
	 * @param ctx the parse tree
	 */
	void enterRecord_name(OracleParser.Record_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#record_name}.
	 * @param ctx the parse tree
	 */
	void exitRecord_name(OracleParser.Record_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#collection_name}.
	 * @param ctx the parse tree
	 */
	void enterCollection_name(OracleParser.Collection_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#collection_name}.
	 * @param ctx the parse tree
	 */
	void exitCollection_name(OracleParser.Collection_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#link_name}.
	 * @param ctx the parse tree
	 */
	void enterLink_name(OracleParser.Link_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#link_name}.
	 * @param ctx the parse tree
	 */
	void exitLink_name(OracleParser.Link_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_name}.
	 * @param ctx the parse tree
	 */
	void enterColumn_name(OracleParser.Column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_name}.
	 * @param ctx the parse tree
	 */
	void exitColumn_name(OracleParser.Column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tableview_name}.
	 * @param ctx the parse tree
	 */
	void enterTableview_name(OracleParser.Tableview_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tableview_name}.
	 * @param ctx the parse tree
	 */
	void exitTableview_name(OracleParser.Tableview_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltable}.
	 * @param ctx the parse tree
	 */
	void enterXmltable(OracleParser.XmltableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltable}.
	 * @param ctx the parse tree
	 */
	void exitXmltable(OracleParser.XmltableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#char_set_name}.
	 * @param ctx the parse tree
	 */
	void enterChar_set_name(OracleParser.Char_set_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#char_set_name}.
	 * @param ctx the parse tree
	 */
	void exitChar_set_name(OracleParser.Char_set_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#synonym_name}.
	 * @param ctx the parse tree
	 */
	void enterSynonym_name(OracleParser.Synonym_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#synonym_name}.
	 * @param ctx the parse tree
	 */
	void exitSynonym_name(OracleParser.Synonym_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#schema_object_name}.
	 * @param ctx the parse tree
	 */
	void enterSchema_object_name(OracleParser.Schema_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#schema_object_name}.
	 * @param ctx the parse tree
	 */
	void exitSchema_object_name(OracleParser.Schema_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dir_object_name}.
	 * @param ctx the parse tree
	 */
	void enterDir_object_name(OracleParser.Dir_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dir_object_name}.
	 * @param ctx the parse tree
	 */
	void exitDir_object_name(OracleParser.Dir_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_object_name}.
	 * @param ctx the parse tree
	 */
	void enterUser_object_name(OracleParser.User_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_object_name}.
	 * @param ctx the parse tree
	 */
	void exitUser_object_name(OracleParser.User_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#grant_object_name}.
	 * @param ctx the parse tree
	 */
	void enterGrant_object_name(OracleParser.Grant_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#grant_object_name}.
	 * @param ctx the parse tree
	 */
	void exitGrant_object_name(OracleParser.Grant_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_list}.
	 * @param ctx the parse tree
	 */
	void enterColumn_list(OracleParser.Column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_list}.
	 * @param ctx the parse tree
	 */
	void exitColumn_list(OracleParser.Column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#paren_column_list}.
	 * @param ctx the parse tree
	 */
	void enterParen_column_list(OracleParser.Paren_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#paren_column_list}.
	 * @param ctx the parse tree
	 */
	void exitParen_column_list(OracleParser.Paren_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#keep_clause}.
	 * @param ctx the parse tree
	 */
	void enterKeep_clause(OracleParser.Keep_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#keep_clause}.
	 * @param ctx the parse tree
	 */
	void exitKeep_clause(OracleParser.Keep_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_argument}.
	 * @param ctx the parse tree
	 */
	void enterFunction_argument(OracleParser.Function_argumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_argument}.
	 * @param ctx the parse tree
	 */
	void exitFunction_argument(OracleParser.Function_argumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_argument_analytic}.
	 * @param ctx the parse tree
	 */
	void enterFunction_argument_analytic(OracleParser.Function_argument_analyticContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_argument_analytic}.
	 * @param ctx the parse tree
	 */
	void exitFunction_argument_analytic(OracleParser.Function_argument_analyticContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_argument_modeling}.
	 * @param ctx the parse tree
	 */
	void enterFunction_argument_modeling(OracleParser.Function_argument_modelingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_argument_modeling}.
	 * @param ctx the parse tree
	 */
	void exitFunction_argument_modeling(OracleParser.Function_argument_modelingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#respect_or_ignore_nulls}.
	 * @param ctx the parse tree
	 */
	void enterRespect_or_ignore_nulls(OracleParser.Respect_or_ignore_nullsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#respect_or_ignore_nulls}.
	 * @param ctx the parse tree
	 */
	void exitRespect_or_ignore_nulls(OracleParser.Respect_or_ignore_nullsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(OracleParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(OracleParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_spec}.
	 * @param ctx the parse tree
	 */
	void enterType_spec(OracleParser.Type_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_spec}.
	 * @param ctx the parse tree
	 */
	void exitType_spec(OracleParser.Type_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datatype}.
	 * @param ctx the parse tree
	 */
	void enterDatatype(OracleParser.DatatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datatype}.
	 * @param ctx the parse tree
	 */
	void exitDatatype(OracleParser.DatatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#precision_part}.
	 * @param ctx the parse tree
	 */
	void enterPrecision_part(OracleParser.Precision_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#precision_part}.
	 * @param ctx the parse tree
	 */
	void exitPrecision_part(OracleParser.Precision_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#native_datatype_element}.
	 * @param ctx the parse tree
	 */
	void enterNative_datatype_element(OracleParser.Native_datatype_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#native_datatype_element}.
	 * @param ctx the parse tree
	 */
	void exitNative_datatype_element(OracleParser.Native_datatype_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#bind_variable}.
	 * @param ctx the parse tree
	 */
	void enterBind_variable(OracleParser.Bind_variableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#bind_variable}.
	 * @param ctx the parse tree
	 */
	void exitBind_variable(OracleParser.Bind_variableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#general_element}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_element(OracleParser.General_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#general_element}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_element(OracleParser.General_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#general_element_part}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_element_part(OracleParser.General_element_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#general_element_part}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_element_part(OracleParser.General_element_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_element}.
	 * @param ctx the parse tree
	 */
	void enterTable_element(OracleParser.Table_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_element}.
	 * @param ctx the parse tree
	 */
	void exitTable_element(OracleParser.Table_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_privilege}.
	 * @param ctx the parse tree
	 */
	void enterObject_privilege(OracleParser.Object_privilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_privilege}.
	 * @param ctx the parse tree
	 */
	void exitObject_privilege(OracleParser.Object_privilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#system_privilege}.
	 * @param ctx the parse tree
	 */
	void enterSystem_privilege(OracleParser.System_privilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#system_privilege}.
	 * @param ctx the parse tree
	 */
	void exitSystem_privilege(OracleParser.System_privilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(OracleParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(OracleParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#numeric}.
	 * @param ctx the parse tree
	 */
	void enterNumeric(OracleParser.NumericContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#numeric}.
	 * @param ctx the parse tree
	 */
	void exitNumeric(OracleParser.NumericContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#numeric_negative}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_negative(OracleParser.Numeric_negativeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#numeric_negative}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_negative(OracleParser.Numeric_negativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#quoted_string}.
	 * @param ctx the parse tree
	 */
	void enterQuoted_string(OracleParser.Quoted_stringContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#quoted_string}.
	 * @param ctx the parse tree
	 */
	void exitQuoted_string(OracleParser.Quoted_stringContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(OracleParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(OracleParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#id_expression}.
	 * @param ctx the parse tree
	 */
	void enterId_expression(OracleParser.Id_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#id_expression}.
	 * @param ctx the parse tree
	 */
	void exitId_expression(OracleParser.Id_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#outer_join_sign}.
	 * @param ctx the parse tree
	 */
	void enterOuter_join_sign(OracleParser.Outer_join_signContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#outer_join_sign}.
	 * @param ctx the parse tree
	 */
	void exitOuter_join_sign(OracleParser.Outer_join_signContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#regular_id}.
	 * @param ctx the parse tree
	 */
	void enterRegular_id(OracleParser.Regular_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#regular_id}.
	 * @param ctx the parse tree
	 */
	void exitRegular_id(OracleParser.Regular_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#non_reserved_keywords_in_12c}.
	 * @param ctx the parse tree
	 */
	void enterNon_reserved_keywords_in_12c(OracleParser.Non_reserved_keywords_in_12cContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#non_reserved_keywords_in_12c}.
	 * @param ctx the parse tree
	 */
	void exitNon_reserved_keywords_in_12c(OracleParser.Non_reserved_keywords_in_12cContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#non_reserved_keywords_pre12c}.
	 * @param ctx the parse tree
	 */
	void enterNon_reserved_keywords_pre12c(OracleParser.Non_reserved_keywords_pre12cContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#non_reserved_keywords_pre12c}.
	 * @param ctx the parse tree
	 */
	void exitNon_reserved_keywords_pre12c(OracleParser.Non_reserved_keywords_pre12cContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#string_function_name}.
	 * @param ctx the parse tree
	 */
	void enterString_function_name(OracleParser.String_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#string_function_name}.
	 * @param ctx the parse tree
	 */
	void exitString_function_name(OracleParser.String_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#numeric_function_name}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_function_name(OracleParser.Numeric_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#numeric_function_name}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_function_name(OracleParser.Numeric_function_nameContext ctx);
}